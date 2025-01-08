package org.mbe.configSchedule;

import com.google.ortools.Loader;
import com.google.protobuf.MapEntry;
import com.opencsv.CSVWriter;
import de.vill.main.UVLModelFactory;
import de.vill.model.FeatureModel;
import org.mbe.configSchedule.generator.SPGenerator;
import org.mbe.configSchedule.parser.FMReader;
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

// =======================================================================
// Basierend auf dem Jobshop-Problem-Beispiel von Google OR-Tools
// https://developers.google.com/optimization/scheduling/job_shop?hl=de
// 07.08.2024
// ========================================================================

public class Main {

    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException, ClassNotFoundException {
        Loader.loadNativeLibraries();
        PathPreferences prefs = new PathPreferences();

        // 0 = Erfüllbarkeit prüfen, 1 = Optimum finden
        int mode = 1;
        // 0 = Variables FM, 1 = Mehrere Configs, 2 = "Zufälliges" Problem generieren
        int mode2 = 0;

        if (args.length == 12 || args.length == 2 || args.length == 3 || args.length == 4) {
            switch (args[0]) {

                // Wenn ein Problem durch den Generator erstellt werden soll
                case "generate":
                    // Dafür erst die benötigten Argumente in Integer parsen, ansonsten Exception
                    // try{
                    int[] intParameter = new int[10];
                    for (int i = 1; i < args.length - 1; i++) {
                        intParameter[i - 1] = Integer.parseInt(args[i]);
                    }
                    SPGenerator spgenerator = new SPGenerator();
                    spgenerator.generateProblem(intParameter[0], intParameter[1], intParameter[2], intParameter[3], intParameter[4], intParameter[5],
                            intParameter[6], intParameter[7], intParameter[8], intParameter[9], args[11]);

                    System.out.println("Problem saved as " + args[11] + ".txt \n" +
                            "To solve use: solve [o or f] " + args[11] + ".txt");
                    // }catch(Exception e) {
                    //    System.out.println("Error - Please make sure that all arguments besides <name> are integers");
                    // }
                    break;

                // Wenn ein Problem gelöst werden soll
                case "solve":
                    // Zuerst mode festlegen (ob nach optimalem oder feasible Schedule gesucht wird)
                    switch (args[1]) {
                        case "o":
                            mode = 1;
                            break;
                        case "f":
                            mode = 0;
                            break;
                        default:
                            System.out.println("Undefinded argument " + args[1] + "\n" +
                                    "Please use \"o\" to search for an optimal schedule or \"f\" to search for a feasible schedule");
                            break;
                    }

                    if (args.length == 3) {
                        // Wenn es ein durch den Generator erstelltes Problem ist
                        if (args[2].endsWith(".txt") || args[2].endsWith(".uvl")) {
                            //try {
                            Instant readStart = Instant.now();
                            SchedulingProblem sp = ReadProblemGen(args[2]);
                            //SchedulingProblem sp = ReadProblemUVL(args[2]);
                            Instant readEnd = Instant.now();

                            PrintProblem(sp);

                            Instant solveStart = Instant.now();
                            ProblemSolver problemSolver = new ProblemSolver(sp);
                            SolverReturn sr;
                            if (mode==0){
                                sr = problemSolver.getFirstSolution();
                            }else{
                                sr = problemSolver.getBestSolution();
                            }

                            Instant solveEnd = Instant.now();

                            long readTime = Duration.between(readStart, readEnd).toMillis();
                            long solveTime = Duration.between(solveStart, solveEnd).toMillis();
                            long combinedTime = readTime + solveTime;

                            if (sr != null) {
                                PrintSolution(sr);
                                System.out.println("Read Time: " + readTime + "ms, Solve Time: " + solveTime + "ms, Combined: " + combinedTime + "ms");
                                WriteCSV(sr, mode, args[2]);
                            } else {
                                System.out.println("No solution found");
                                System.out.println("Read Time: " + readTime + "ms, Solve Time: " + solveTime + "ms, Combined: " + combinedTime + "ms");
                            }
                            //} catch (Exception e) {
                            //    System.out.println("Error - Please make sure that a problem with the name " + args[2] + " exists");
                            //}
                        }
                        // Wenn es die Problemfamilie ist (XML-Datei)
                        else if (args[2].endsWith(".xml")) {
                            //try {
                            Instant readStart = Instant.now();
                            SchedulingProblem sp = FMReader.readFM(args[2]);
                            Instant readEnd = Instant.now();

                            PrintProblem(sp);

                            Instant solveStart = Instant.now();
                            ProblemSolver problemSolver = new ProblemSolver(sp);
                            SolverReturn sr;
                            if (mode==0){
                                sr = problemSolver.getFirstSolution();
                            }else{
                                sr = problemSolver.getBestSolution();
                            }

                            Instant solveEnd = Instant.now();

                            long readTime = Duration.between(readStart, readEnd).toMillis();
                            long solveTime = Duration.between(solveStart, solveEnd).toMillis();
                            long combinedTime = readTime + solveTime;

                            if (sr != null) {
                                PrintSolution(sr);
                                System.out.println("Read Time: " + readTime + "ms, Solve Time: " + solveTime + "ms, Combined: " + combinedTime + "ms");
                                WriteCSV(sr, mode, args[2]);
                            } else {
                                System.out.println("No solution found");
                                System.out.println("Read Time: " + readTime + "ms, Solve Time: " + solveTime + "ms, Combined: " + combinedTime + "ms");
                            }
                            //} catch (Exception e) {
                            //    System.out.println("Error - Please make sure that you entered the complete file-path and your model follows the correct structure");
                            //}
                        }
                        // Wenn über die Instanzmenge gesucht werden soll
                    } else if (args.length == 4) {
                        //try {
                        String modelPath = args[2];
                        SchedulingProblem sp = FMReader.readFM(modelPath);
                        PrintProblem(sp);
                        ConfigurationSolverReturn csr;
                        if (mode == 0){
                            csr = ConfigurationSolver.getFirst(args[3], modelPath);
                        }else{
                            csr = ConfigurationSolver.getBest(args[3], modelPath);
                        }

                        if (csr != null && csr.isHasSolution()) {
                            PrintSolution(csr.getSolverReturn());
                            System.out.println("Found solution in iteration " + csr.getIteration() + "\n" +
                                    "Read time: " + csr.getReadTime() + "ms, Solve time: " + csr.getTimeSolve() + "ms, Combined: " + csr.getNeededTime() + "ms");
                            WriteCSV(csr.getSolverReturn(), mode, args[2]);
                        } else {
                            System.out.println("No solution found");
                            System.out.println("Searched in " + (csr.getSearchedConfigs() - 1) + " configurations \n" +
                                    "Read time: " + csr.getReadTime() + "ms, Solve time: " + csr.getTimeSolve() + "ms, Combined: " + csr.getNeededTime() + "ms");
                        }
                        //} catch (Exception e) {
                        //    System.out.println("Error - Please make sure that you entered the complete path to the configurations-directory");
                        //}
                    }
                    break;

                case "solutionpath":
                    prefs.setSolutionSavePath(args[1]);
                    break;
                case "problempath":
                    prefs.setProblemSavePath(args[1]);
                    System.out.printf("Path for saving generated problems set to %s", args[1]);
                    break;
                case "get":
                    if (args[1].equals("solutionpath")) {
                        System.out.println(prefs.getSolutionSavePath());
                    } else if (args[1].equals("problempath")) {
                        System.out.println(prefs.getProblemSavePath());
                    }
                default:
                    System.out.println("Undefined command " + args[0] + "\n" +
                            "Use \"generate\" or \"solve\"");
            }
        } else {
            System.out.println("Wrong command or wrong amount of arguments \n" +
                    "To generate a problem: generate <jobCount> <taskCount> <durationOutlier> " +
                    "<machineCount> <optionalCount> <altCount> <altGroupCount> <deadline> <name> \n" +
                    "To solve a problem: solve [o or f] <name>");
        }

    }


    public static SchedulingProblem ReadProblemGen(String name) throws IOException, ClassNotFoundException {
        //FileInputStream fIn = new FileInputStream("src/main/probleme/" + name);
        PathPreferences prefs = new PathPreferences();
        String path = prefs.getProblemSavePath();
        FileInputStream fIn = new FileInputStream(path + name);
        ObjectInputStream oIn = new ObjectInputStream(fIn);
        SchedulingProblem sp = (SchedulingProblem) oIn.readObject();
        oIn.close();

        return sp;
    }

    public static SchedulingProblem ReadProblemUVL(String name) throws IOException {
        PathPreferences prefs = new PathPreferences();
        String path = prefs.getProblemSavePath();
        Path filePath = Path.of(path + name);
        String problemUVL = Files.readString(filePath);

        System.out.println("\n" + problemUVL + "\n");
        UVLModelFactory modelFactory = new UVLModelFactory();
        FeatureModel fm = modelFactory.parse(problemUVL);

        SchedulingProblem sp = new SchedulingProblem(fm);
        return sp;
    }


    public static void PrintProblem(SchedulingProblem sp) {
        System.out.println("*********************** \n" +
                "Scheduling problem:");
        System.out.println("Deadline: " + sp.getDeadline());
        int index = 0;
        for (List<Task> job : sp.getJobs()) {
            System.out.println("Job " + index + ": ");
            for (Task task : job) {
                System.out.print(task.getName() + ", d: [" + task.getDuration()[0] + "," + task.getDuration()[1] + "], m: " + task.getMachine().getName() + ", o: " + task.isOptional()
                        + ", e: " + task.getExcludeTasks().toString() + ", d: ");
                for (Map.Entry<Integer, List<Task>> item : task.getDurationCons().entrySet()) {
                    System.out.print("[" + item.getKey() + "; ");
                    for (Task t : item.getValue()) {
                        System.out.print(t.getName() + ",");
                    }
                    System.out.print("], ");
                }
                System.out.println();
            }
            index++;
        }

        System.out.println("\n*********************** \n");
    }


    public static void PrintSolution(SolverReturn sr) {
        System.out.println("Solution:");
        System.out.printf(sr.getOutput() + "\n");
        System.out.println("Schedule is " + sr.getStatus() + ", takes " + sr.getTime());
    }

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

    public static void WriteCSV(SolverReturn sr, int mode, String ProblemPath) throws IOException {
        List<String[]> data = ConvertSRToStrings(sr);
        String fileName = ReadProblemName(ProblemPath);

        if (mode == 0) {
            fileName = fileName + "-feasible";
        } else if (mode == 1) {
            fileName = fileName + "-optimum";
        }

        //File file = new File("src/main/schedules/" + fileName);
        File file = new File("/home/max/Schreibtisch/Solver-Ergebnisse/" + fileName);
        FileWriter outputFile = new FileWriter(file);
        CSVWriter csvWriter = new CSVWriter(outputFile);

        for (String[] line : data) {
            csvWriter.writeNext(line);
        }

        csvWriter.close();

    }

    public static String ReadProblemName(String path) {
        String[] subStrings = path.split("/");
        String s = subStrings[subStrings.length - 1];

        String[] subStrings2 = s.split("\\.");
        String s2 = subStrings2[0];

        return s2;
    }
}