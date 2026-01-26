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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


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

            for (File file : directoryFiles) {
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

            System.out.println(amountStructures);
            System.out.println(amountStructures.size());
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
}
