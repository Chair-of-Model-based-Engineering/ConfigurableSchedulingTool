package org.mbe.configSchedule.solver;

import com.google.ortools.sat.CpSolverStatus;
import org.mbe.configSchedule.parser.ConfigurationReader;
import org.mbe.configSchedule.util.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;


public class ConfigurationSolver {
    /**
     * Given a list of configurations, this method iterates over each configuration and calls the
     * {@link org.mbe.configSchedule.solver.ProblemSolver ProblemSolver} for each configuration separately
     * and returns the first feasible schedule.
     *
     * @param configDirectoryPath Path to the directory on which the configuration files are stored.
     * @param modelPath           Path to model of the scheduling problem.
     * @return Object of type {@link org.mbe.configSchedule.util.ConfigurationSolverReturn ConfigurationSolverReturn}
     * @throws XPathExpressionException
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public static ConfigurationSolverReturn getFirst(String configDirectoryPath, String modelPath) throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
        File directory = new File(configDirectoryPath);
        File[] directoryFiles = directory.listFiles();

        ConfigurationReader cReader = new ConfigurationReader();

        // mode = 0 -> feasible Schedule
        if (directoryFiles != null) {

            int iteration = 1;
            long sumTimeRead = 0;
            long sumTimeSolve = 0;

            for (File file : directoryFiles) {
                String filePath = file.getPath();

                Instant readStart = Instant.now();
                SchedulingProblem sp = cReader.ReadConfig(filePath, modelPath);
                Instant readEnd = Instant.now();

                Instant solveStart = Instant.now();

                ProblemSolver problemSolver = new ProblemSolver(new BaseModel(sp));
                problemSolver.findFeasibleSolution();
                SolverReturn sr = problemSolver.getSolverReturn();
                Instant solveEnd = Instant.now();

                long readTime = Duration.between(readStart, readEnd).toMillis();
                long solveTime = Duration.between(solveStart, solveEnd).toMillis();
                sumTimeRead += readTime;
                sumTimeSolve += solveTime;

                if (sr != null && (sr.getStatus() == CpSolverStatus.OPTIMAL || sr.getStatus() == CpSolverStatus.FEASIBLE)) {
                    ConfigurationSolverReturn csr = new ConfigurationSolverReturn(true, sr, sumTimeRead, sumTimeSolve, sumTimeRead + sumTimeSolve, iteration, iteration);
                    return csr;
                }
                iteration++;
            }
            ConfigurationSolverReturn csr = new ConfigurationSolverReturn(false, null, sumTimeRead, sumTimeSolve, sumTimeRead + sumTimeSolve, iteration, iteration);
            return csr;
        }
        return null;
    }

    /**
     * Given a list of configurations, this method iterates over each configuration and calls the
     * {@link org.mbe.configSchedule.solver.ProblemSolver ProblemSolver} for each configuration separately
     * and returns the optimal schedule.
     *
     * @param configDirectoryPath Path to the directory on which the configuration files are stored.
     * @param modelPath           Path to model of the scheduling problem.
     * @return Object of type {@link org.mbe.configSchedule.util.ConfigurationSolverReturn ConfigurationSolverReturn}
     * @throws XPathExpressionException
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public static ConfigurationSolverReturn getBest(String configDirectoryPath, String modelPath) throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
        File directory = new File(configDirectoryPath);
        File[] directoryFiles = directory.listFiles();

        ConfigurationReader cReader = new ConfigurationReader();
        if (directoryFiles != null) {
            int iteration = 1;
            Double bestResultTime = Double.POSITIVE_INFINITY;
            SolverReturn bestResult = new SolverReturn();
            long sumTimeRead = 0;
            long sumTimeSolve = 0;
            int bestIteration = -1;
            Set<ScheduleStructure> amountStructures = new HashSet<>();
            Map<ScheduleStructure, Set<Integer>> coverage = new HashMap<>();

            List<File> sortedFiles = sortFilesPerProblemSize(directoryFiles, cReader, modelPath); //Task amount sorted by small to big
            for (File file : sortedFiles) {
                String filePath = file.getPath();

                Instant readStart = Instant.now();
                SchedulingProblem sp = cReader.ReadConfig(filePath, modelPath);
                Instant readEnd = Instant.now();

                Instant solveStart = Instant.now();

                ProblemSolver problemSolver = new ProblemSolver(new BaseModel(sp));
                problemSolver.findOptimalSolution(amountStructures);
                SolverReturn sr = problemSolver.getSolverReturn();

                amountStructures.add(sr.getStructure());

                Instant solveEnd = Instant.now();

                if (sr.isOptimal()) {
                    @SuppressWarnings("OptionalGetWithoutIsPresent") // if the schedule is optimal, it necessarily has a schedule.
                    double makespan = sr.getSchedule().get().getMakespan();
                    if (makespan < bestResultTime) {
                        bestResultTime = makespan;
                        bestResult = sr;
                        bestIteration = iteration;
                    }
                }
                iteration++;

                long readTime = Duration.between(readStart, readEnd).toMillis();
                long solveTime = Duration.between(solveStart, solveEnd).toMillis();
                sumTimeRead += readTime;
                sumTimeSolve += solveTime;
            }

            for(ScheduleStructure structure : amountStructures)
            {
                for(File file : directoryFiles)
                {
                    String filePath = file.getPath();
                    SchedulingProblem sp = cReader.ReadConfig(filePath, modelPath);
                    ProblemSolver problemSolver = new ProblemSolver(new BaseModel(sp));
                    if(problemSolver.isStructureFeasible(structure))
                    {
                        coverage.computeIfAbsent(structure, files -> new HashSet<>()).add(Integer.parseInt(file.getName().replaceFirst("\\.[^.]+$", ""))); //removing the 0 at the start and .xml at the end for index
                    }
                }
            }

            Set<ScheduleStructure> uniqueStructures = new HashSet<>(amountStructures);

            if(amountStructures.size() > 1)
            {
                for(ScheduleStructure structure : amountStructures)
                {
                    for(ScheduleStructure otherStructure : amountStructures)
                    {
                        if(structure == otherStructure)
                        {
                            continue;
                        }
                        if(otherStructure.containsStructure(structure) && coverage.get(otherStructure).containsAll(coverage.get(structure)))
                        {
                            uniqueStructures.remove(structure);
                            break;
                        }
                    }
                }
            }

            ScheduleStructure generalizedStructure = ScheduleStructure.generalizeStructure(uniqueStructures.stream().toList());

            coverage.forEach((structure, file) -> {
                System.out.println("Files: " + file);
                System.out.println(structure + "Amount: " + file.size());
            });
            System.out.println("---------------Unique Structures-------------------");
            System.out.println(uniqueStructures);

            System.out.println("---------------Generalized Structure-------------------");
            System.out.println(generalizedStructure);

            Map<ScheduleStructure, Set<Integer>> finalCoverage = new HashMap<>();
            for(File file : directoryFiles)
            {
                String filePath = file.getPath();
                SchedulingProblem sp = cReader.ReadConfig(filePath, modelPath);
                ProblemSolver problemSolver = new ProblemSolver(new BaseModel(sp));
                if(problemSolver.isStructureFeasible(generalizedStructure))
                {
                    finalCoverage.computeIfAbsent(generalizedStructure, files -> new HashSet<>()).add(Integer.parseInt(file.getName().replaceFirst("\\.[^.]+$", ""))); //removing the 0 at the start and .xml at the end for index
                }
            }

            finalCoverage.forEach((structure, file) -> {
                System.out.println("Files: " + file);
                System.out.println(structure + "Amount: " + file.size());
                System.out.println();
            });



            if (bestIteration != -1) {
                ConfigurationSolverReturn csr = new ConfigurationSolverReturn(true, bestResult, sumTimeRead, sumTimeSolve, sumTimeRead + sumTimeSolve, bestIteration, iteration);
                return csr;
            } else {
                ConfigurationSolverReturn csr = new ConfigurationSolverReturn(false, bestResult, sumTimeRead, sumTimeSolve, sumTimeRead + sumTimeSolve, bestIteration, iteration);
                return csr;
            }
        }
        return null;
    }

    /**
     * Sorts the given File Array from small to big according to the amount of tasks in the SchedulingProblem
     *
     * @param directoryFiles The file-array of all configurations
     * @param modelPath  The file-path to the model-file
     * @return A List with the sorted files
     * @throws Exception                  if any Error occurs.
     */
    private static List<File> sortFilesPerProblemSize(File[] directoryFiles, ConfigurationReader cReader, String modelPath) {
        List<File> sortedFiles = Arrays.asList(directoryFiles);
        sortedFiles.sort((file1,file2) ->
        {
            try
            {
                SchedulingProblem problem1 = cReader.ReadConfig(file1.getPath(), modelPath);
                SchedulingProblem problem2 = cReader.ReadConfig(file2.getPath(), modelPath);
                return Integer.compare(problem1.getTasks().size(), problem2.getTasks().size());
            }
            catch(Exception exception)
            {
                exception.printStackTrace();
                return 0;
            }
        });
        return sortedFiles;
    }
}
