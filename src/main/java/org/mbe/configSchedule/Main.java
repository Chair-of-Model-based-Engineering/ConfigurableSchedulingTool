package org.mbe.configschedule;

import com.google.ortools.Loader;
import com.opencsv.CSVWriter;
import de.vill.model.FeatureModel;
import org.mbe.configschedule.generator.SPGenerator;
import org.mbe.configschedule.parser.FMReader;
import org.mbe.configschedule.parser.UVLReader;
import org.mbe.configschedule.solver.ProblemSolver;
import org.mbe.configschedule.util.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

// =======================================================================
// Basierend auf dem Jobshop-Problem-Beispiel von Google OR-Tools
// https://developers.google.com/optimization/scheduling/job_shop?hl=de
// 07.08.2024
// ========================================================================

public class Main {

    public static void main(String[] args){
        try{
            FeatureModel model = UVLReader.read("src/main/modelle/J2_T5_M1(extendedUVL).uvl");
            SchedulingProblem problem = new SchedulingProblem(model);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


        public static void tmain(String[] args) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException, ClassNotFoundException {

        Loader.loadNativeLibraries();

        // 0 = Erfüllbarkeit prüfen, 1 = Optimum finden
        int mode = 1;
        // 0 = Variables FM, 1 = Mehrere Configs, 2 = "Zufälliges" Problem generieren
        int mode2 = 0;

        ProblemSolver pSolver = new ProblemSolver();

        if(args.length == 10 || args.length == 3 || args.length == 4) {
            switch (args[0]) {

                // Wenn ein Problem durch den Generator erstellt werden soll
                case "generate":
                    // Dafür erst die benötigten Argumente in Integer parsen, ansonsten Exception
                    try{
                        int[] intParameter = new int[8];
                        for(int i = 1; i < args.length-1; i++) {
                            intParameter[i-1] = Integer.parseInt(args[i]);
                            System.out.println(intParameter[i-1]);
                        }
                        SPGenerator spgenerator = new SPGenerator();
                        spgenerator.generateProblem(intParameter[0], intParameter[1], intParameter[2], intParameter[3], intParameter[4], intParameter[5],
                                intParameter[6], intParameter[7], args[9]);

                        System.out.println("Problem saved as " + args[9] + ".txt \n" +
                                "To solve use: solve [o or f] " + args[9] + ".txt" );
                    }catch(Exception e) {
                        System.out.println("Error - Please make sure that all arguments besides <name> are integers");
                    }
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
                            System.out.println("Undefinded argument " + args[1] +"\n" +
                                    "Please use \"o\" to search for an optimal schedule or \"f\" to search for a feasible schedule");
                            break;
                    }

                    if(args.length == 3) {
                        // Wenn es ein durch den Generator erstelltes Problem ist
                        if(args[2].endsWith(".txt")) {
                            //try {
                                Instant readStart = Instant.now();
                                SchedulingProblem sp = ReadProblemGen(args[2]);
                                Instant readEnd = Instant.now();

                                PrintProblem(sp);

                                Instant solveStart = Instant.now();
                                SolverReturn sr = pSolver.solveProblem(mode, sp);
                                Instant solveEnd = Instant.now();

                                long readTime = Duration.between(readStart, readEnd).toMillis();
                                long solveTime = Duration.between(solveStart, solveEnd).toMillis();
                                long combinedTime = readTime + solveTime;

                                if(sr != null) {
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
                        else if(args[2].endsWith(".xml")) {
                            //try {
                                Instant readStart = Instant.now();
                                SchedulingProblem sp = FMReader.readFM(args[2]);
                                Instant readEnd = Instant.now();

                                PrintProblem(sp);

                                Instant solveStart = Instant.now();
                                SolverReturn sr = pSolver.solveProblem(mode, sp);
                                Instant solveEnd = Instant.now();

                                long readTime = Duration.between(readStart, readEnd).toMillis();
                                long solveTime = Duration.between(solveStart, solveEnd).toMillis();
                                long combinedTime = readTime + solveTime;

                                if(sr != null) {
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
                    } else if (args.length == 4){
                        try {
                            String modelPath = args[2];
                            SchedulingProblem sp = FMReader.readFM(modelPath);
                            PrintProblem(sp);
                            ConfigurationSolverReturn csr = pSolver.SolveConfigurations(mode, args[3], modelPath);
                            if(csr.isHasSolution()) {
                                PrintSolution(csr.getSolverReturn());
                                System.out.println("Found solution in iteration " + csr.getIteration() + "\n" +
                                        "Read time: " + csr.getReadTime() + "ms, Solve time: " + csr.getTimeSolve() + "ms, Combined: " + csr.getNeededTime() + "ms");
                                WriteCSV(csr.getSolverReturn(), mode, args[2]);
                            } else {
                                System.out.println("No solution found");
                                System.out.println("Searched in " + (csr.getSearchedConfigs() - 1) + " configurations \n" +
                                        "Read time: " + csr.getReadTime() + "ms, Solve time: " + csr.getTimeSolve() + "ms, Combined: " + csr.getNeededTime() + "ms");
                            }
                        } catch (Exception e) {
                            System.out.println("Error - Please make sure that you entered the complete path to the configurations-directory");
                        }
                    }
                    break;

                default: System.out.println("Undefined command " + args[0] + "\n" +
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
        FileInputStream fIn = new FileInputStream("src/main/probleme/" + name);
        ObjectInputStream oIn = new ObjectInputStream(fIn);
        SchedulingProblem sp = (SchedulingProblem) oIn.readObject();
        oIn.close();

        return sp;
    }


    public static void PrintProblem(SchedulingProblem sp) {
        System.out.println("*********************** \n" +
            "Scheduling problem:");
        System.out.println("Deadline: " + sp.getDeadline());
        int index = 0;
        for(List<Task> job : sp.getJobs()) {
            System.out.println("Job " + index + ": ");
            for(Task task : job) {
                System.out.println(task.getName() + ", d: [" + task.getDuration()[0] +"," + task.getDuration()[1] + "], m: " + task.getMachine() + ", o: " + task.isOptional()
                    + ", e: " + task.getExcludeTasks().toString());
            }
            index++;
        }

        System.out.println("*********************** \n");
    }


    public static void PrintSolution(SolverReturn sr) {
        System.out.println("Solution:");
        System.out.printf(sr.getOutput() + "\n");
        System.out.println("Schedule is " + sr.getStatus() + ", takes " + sr.getTime());
    }

    public static List<String[]> ConvertSRToStrings(SolverReturn sr) {
        Map<Machine, List<AssignedTask>> assignedJobs = new HashMap<>(sr.getAssignedJobs());
        List<String[]> resultString = new ArrayList<>();

        for(Machine m : assignedJobs.keySet()) {
            String[] mLine = new String[assignedJobs.get(m).size() + 1];
            String[] iLine = new String[assignedJobs.get(m).size() + 1];
            mLine[0] = m.getName();
            iLine[0] = m.getName() + "Intervals";

            int index = 1;
            for(AssignedTask t : assignedJobs.get(m)) {
                if(t.isActive()) {
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
        } else if(mode == 1) {
            fileName = fileName + "-optimum";
        }

        File file = new File("src/main/schedules/" + fileName);
        FileWriter outputFile = new FileWriter(file);
        CSVWriter csvWriter = new CSVWriter(outputFile);

        for(String[] line : data) {
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