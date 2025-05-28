package org.mbe.configSchedule;

import com.google.ortools.Loader;
import com.opencsv.CSVWriter;
import de.vill.model.FeatureModel;
import org.mbe.configSchedule.generator.SPGenerator;
import org.mbe.configSchedule.parser.UVLReader;
import org.mbe.configSchedule.solver.ConfigurationSolver;
import org.mbe.configSchedule.solver.ProblemSolver;
import org.mbe.configSchedule.util.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

// =======================================================================
// Basierend auf dem Jobshop-Problem-Beispiel von Google OR-Tools
// https://developers.google.com/optimization/scheduling/job_shop?hl=de
// 07.08.2024
// ========================================================================

public class Main {

    public enum SolveComplexity {
        FEASIBLE,
        OPTIMAL;
    }

    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException, ClassNotFoundException {
        Loader.loadNativeLibraries();
        PathPreferences prefs = new PathPreferences();

        if (!(args.length == 12 || args.length == 2 || args.length == 3 || args.length == 4)) {
            System.err.println("""
                    Wrong command or wrong amount of arguments\s
                    To generate a problem: generate <jobCount> <taskCount> <durationOutlier> \
                    <machineCount> <optionalCount> <altCount> <altGroupCount> <deadline> <durationConstraints> <maxDurationRequires> <name>\s
                    To solve a problem: solve [o or f] <name>""");
            return;
        }

        switch (args[0]) {
            case "generate" -> {
                // Parse some arguments to integers
                int[] intParameter = new int[10];
                for (int i = 1; i < args.length - 1; i++) {
                    intParameter[i - 1] = Integer.parseInt(args[i]);
                }
                SPGenerator spgenerator = new SPGenerator();
                spgenerator.generateProblem(intParameter[0], intParameter[1], intParameter[2], intParameter[3], intParameter[4], intParameter[5],
                        intParameter[6], intParameter[7], intParameter[8], intParameter[9], args[11]);

                System.out.printf("""
                        Problem saved as %1$s.uvl
                        To solve use: solve [o|f] %1$s.uvl
                        """, args[11]);
            }
            case "solve" -> {
                SolveComplexity solveMode;
                // set mode (search for feasible or optimal solution?)
                if ("o".equals(args[1])) {
                    solveMode = SolveComplexity.OPTIMAL;
                } else if ("f".equals(args[1])) {
                    solveMode = SolveComplexity.FEASIBLE;
                } else {
                    System.err.printf("""
                            Undefined argument "%s"
                            Please use "o" to search for an optimal schedule or "f" to search for a feasible schedule
                            """, args[1]);
                    break;
                }

                if (args.length == 3 && args[2].endsWith(".uvl")) {
                    solveFamilyBased(args[2], solveMode);
                } else if (args.length == 4) {
                    solveInstanceBased(args[2], args[3], solveMode);
                } else {
                    System.err.println("Incorrect amount or type of arguments.");
                }
            }
            case "solutionpath" -> {
                prefs.setSolutionSavePath(args[1]);
                System.out.printf("Path for saving solutions problems set to %s", args[1]);
            }
            case "problempath" -> {
                prefs.setProblemSavePath(args[1]);
                System.out.printf("Path for saving generated problems set to %s", args[1]);
            }
            case "get" -> {
                if (args[1].equals("solutionpath")) {
                    System.out.println(prefs.getSolutionSavePath());
                } else if (args[1].equals("problempath")) {
                    System.out.println(prefs.getProblemSavePath());
                }
            }
            case "delete" -> {
                if (args[1].equals("solutionpath")) {
                    prefs.removeSolutionSavePath();
                    System.out.println("Deleted path for saving solutions");
                } else if (args[1].equals("problempath")) {
                    prefs.removeProblemSavePath();
                    System.out.println("Deleted path for saving problems");
                }
            }
            default -> System.err.printf("""
                    Undefined command %s
                    "Use "generate" or "solve"
                    """, args[0]);
        }

    }

    /**
     * Use a family-based approach to search for a solution to the given problem.
     * @param fileName  The name of the given problem.
     * @param solveMode The mode (feasible/optimal) of the search.
     * @throws IOException if the problem file cannot be read or the result file cannot be written.
     */
    private static void solveFamilyBased(String fileName, SolveComplexity solveMode) throws IOException {
        Instant readStart = Instant.now();
        SchedulingProblem sp = ReadProblemUVL(fileName);
        Instant readEnd = Instant.now();

        PrintProblem(sp);

        Instant solveStart = Instant.now();
        ProblemSolver problemSolver = new ProblemSolver(sp);
        SolverReturn sr;
        if (solveMode == SolveComplexity.FEASIBLE) {
            sr = problemSolver.getFirstSolution();
        } else {
            sr = problemSolver.getBestSolution();
        }

        Instant solveEnd = Instant.now();

        long readTime = Duration.between(readStart, readEnd).toMillis();
        long solveTime = Duration.between(solveStart, solveEnd).toMillis();
        long combinedTime = readTime + solveTime;

        if (sr != null) {
            PrintSolution(sr);
            System.out.println("Read Time: " + readTime + "ms, Solve Time: " + solveTime + "ms, Combined: " + combinedTime + "ms");
            WriteCSV(sr, solveMode, fileName);
        } else {
            System.out.println("No solution found");
            System.out.println("Read Time: " + readTime + "ms, Solve Time: " + solveTime + "ms, Combined: " + combinedTime + "ms");
        }
    }

    /**
     * Use an instance-based approach to search for a solution to the given problem.
     * @param modelPath     The path to the model describing the problem.
     * @param configDirPath The path to the directory containing valid instances/configurations to the problem.
     * @param solveMode The mode (feasible/optimal) of the search.
     * @throws IOException if the problem file cannot be read or the result file cannot be written.
     */
    private static void solveInstanceBased(String modelPath, String configDirPath, SolveComplexity solveMode) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        SchedulingProblem sp = ReadProblemUVL(modelPath);
        PrintProblem(sp);
        ConfigurationSolverReturn csr;
        if (solveMode == SolveComplexity.FEASIBLE) {
            csr = ConfigurationSolver.getFirst(configDirPath, modelPath);
        } else {
            csr = ConfigurationSolver.getBest(configDirPath, modelPath);
        }

        if (csr != null && csr.isHasSolution()) {
            PrintSolution(csr.getSolverReturn());
            System.out.printf("""
                    Found solution in iteration %s
                    "Read time: %d ms, Solve time: %d ms, Combined: %d ms
                    """, csr.getIteration(), csr.getReadTime(), csr.getTimeSolve(), csr.getNeededTime());
            WriteCSV(csr.getSolverReturn(), solveMode, modelPath);
        } else {
            System.out.printf("""
                    No solution found
                    Searched in %d configurations
                    "Read time: %d ms, Solve time: %d ms, Combined: %d ms
                    """, (csr.getSearchedConfigs() - 1), csr.getReadTime(), csr.getTimeSolve(), csr.getNeededTime());
        }
    }

    /**
     * Reads a problem from a file in, which contents are formatted as an uvl.
     * @param name          The name of the problem
     * @return              A {@link SchedulingProblem} with the properties of the input-file
     * @throws IOException if the input uvl file cannot be written.
     */
    public static SchedulingProblem ReadProblemUVL(String name) throws IOException {
        Path filePath;
        if(name.contains("/")) {
            filePath = Path.of(name);
        } else {
            PathPreferences prefs = new PathPreferences();
            filePath = Path.of(prefs.getProblemSavePath()).resolve(name);
        }

        System.out.printf("%nReading problem: %s%n", name);
        FeatureModel fm = UVLReader.read(filePath);
        return new SchedulingProblem(fm);
    }

    /**
     * Prints a problem
     * @param sp    The {@link SchedulingProblem} to be printed
     */
    public static void PrintProblem(SchedulingProblem sp) {
        System.out.printf("""
                ***********************
                Scheduling problem:
                Deadline: %d
                """, sp.getDeadline());
        int jobIndex = 0;
        for (List<Task> job : sp.getJobs()) {
            System.out.printf("Job %d: %n", jobIndex);
            for (Task task : job) {
                String taskDurationConsString = task.getDurationCons().entrySet().stream().map(entry -> {
                    String taskList = entry.getValue().stream().map(Task::getName).collect(Collectors.joining(", "));
                    return "%d -> {%s}".formatted(entry.getKey(), taskList);
                }).collect(Collectors.joining(", "));

                System.out.printf("%s => m: %s, o: %b, d: %s, e: %s, d: {%s}%n",
                        task.getName(),
                        task.getMachine().getName(),
                        task.isOptional(),
                        Arrays.toString(task.getDurations()),
                        task.getExcludeTasks().toString(),
                        taskDurationConsString
                );
            }
            jobIndex++;
        }

        System.out.printf("%n***********************%n%n");
    }

    /**
     * Prints a solution
     * @param sr    The {@link SolverReturn} to be printed
     */
    public static void PrintSolution(SolverReturn sr) {
        System.out.println("Solution:");
        System.out.println(sr.getOutput());
        System.out.println("Schedule is " + sr.getStatus() + ", takes " + sr.getTime());
    }

    /**
     * Converts a solution to a {@link List} of String-Arrays, which can be used to save the solution as a CSV-file.
     * @param sr    The solution in the form of a {@link SolverReturn}, which should be converted
     * @return      A {@link List} of {@link Arrays} of type {@link String}
     */
    public static List<String[]> ConvertSRToStrings(SolverReturn sr) {
        Map<Machine, List<AssignedTask>> assignedJobs = new HashMap<>(sr.getAssignedJobs());
        List<String[]> resultString = new ArrayList<>();

        for (Machine m : assignedJobs.keySet()) {
            String[] mLine = new String[assignedJobs.get(m).size() + 1];
            String[] iLine = new String[assignedJobs.get(m).size() + 1];
            mLine[0] = m.getName();
            iLine[0] = m.getName() + "Intervals";

            int index = 1;
            for (AssignedTask t : assignedJobs.get(m)) {
                if (t.isActive()) {
                    mLine[index] = t.getName();
                    iLine[index] = t.getStart() + "," + (t.getStart() + t.getDuration());
                    index++;
                }
            }

            resultString.add(mLine);
            resultString.add(iLine);
        }


        return resultString;
    }

    /**
     * Saves a solution to a CSV-file
     * @param sr            The solution in the form of a {@link SolverReturn}
     * @param mode          The mode (feasible/optimal) of the search
     * @param ProblemPath   The path of the corresponding problem
     * @throws IOException if the result csv file cannot be written.
     */
    public static void WriteCSV(SolverReturn sr, SolveComplexity mode, String ProblemPath) throws IOException {
        List<String[]> data = ConvertSRToStrings(sr);
        String fileName = ReadProblemName(ProblemPath);

        if (mode == SolveComplexity.FEASIBLE) {
            fileName = fileName + "-feasible";
        } else {
            fileName = fileName + "-optimum";
        }

        PathPreferences prefs = new PathPreferences();

        Path path = Path.of(prefs.getSolutionSavePath());
        if(!Files.exists(path)) {
            Files.createDirectories(path);
        }

        Path filePath = path.resolve(fileName);

        File file = new File(String.valueOf(filePath));
        FileWriter outputFile = new FileWriter(file);
        CSVWriter csvWriter = new CSVWriter(outputFile);

        for (String[] line : data) {
            csvWriter.writeNext(line);
        }

        csvWriter.close();

    }

    /**
     * Extracts the name of a problem from its file path
     * @param path  The path to the problem file
     * @return      The name of the problem
     */
    public static String ReadProblemName(String path) {
        String[] pathParts = path.split("/");
        String filename = pathParts[pathParts.length - 1];

        String[] filenameParts = filename.split("\\.");
        return filenameParts[0];
    }
}