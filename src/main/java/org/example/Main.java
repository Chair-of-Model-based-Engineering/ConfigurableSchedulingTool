package org.example;
import static java.lang.Math.max;

import com.google.ortools.Loader;
import com.google.ortools.constraintsolver.Solver;
import com.google.ortools.sat.*;
import com.opencsv.CSVWriter;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;

// =======================================================================
// Basierend auf dem Jobshop-Problem-Beispiel von Google OR-Tools
// https://developers.google.com/optimization/scheduling/job_shop?hl=de
// 07.08.2024
// ========================================================================

public class Main {

    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException, ClassNotFoundException {

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
                                FMReader fmReader = new FMReader();

                                Instant readStart = Instant.now();
                                SchedulingProblem sp = fmReader.ReadFM(args[2]);
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
                            FMReader fmReader = new FMReader();
                            SchedulingProblem sp = fmReader.ReadFM(modelPath);
                            PrintProblem(sp);
                            ConfigurationSolverReturn csr = pSolver.SolveConfigurations(mode, args[3], modelPath);
                            if(csr.hasSolution) {
                                PrintSolution(csr.solverReturn);
                                System.out.println("Found solution in iteration " + csr.iteration + "\n" +
                                        "Read time: " + csr.readTime + "ms, Solve time: " + csr.timeSolve + "ms, Combined: " + csr.neededTime + "ms");
                                WriteCSV(csr.solverReturn, mode, args[2]);
                            } else {
                                System.out.println("No solution found");
                                System.out.println("Searched in " + (csr.searchedConfigs - 1) + " configurations \n" +
                                        "Read time: " + csr.readTime + "ms, Solve time: " + csr.timeSolve + "ms, Combined: " + csr.neededTime + "ms");
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

        /*
        FMReader fmReader = new FMReader();

        String modelPath = "src/main/modelle/J2_T10_M2_O2_A4.xml";
        String configFolderPath = "src/main/modelle/J2_T5_M1Configs";


        // Falls mode2 = 2, kann man mit 0 das selbe Problem immer wieder lösen (um zu überprüfen ob die Lösungen
        // gleich sind), mit 1 nur einmal Lösen (um Zeit zu messen)
        int mode2repeat = 0;

        // Suche über Problemfamilie
        if(mode2 == 0) {
            Instant start = Instant.now();
            fmReader.ReadFM(modelPath);
            final int deadline = fmReader.deadline;
            final List<List<Task>> alleJobs = new ArrayList<>(fmReader.jobs);
            final List<Machine> machines = new ArrayList<>(fmReader.machines);
            Instant end = Instant.now();
            long readDuration = Duration.between(start, end).toMillis();

            System.out.println("\n"
                    + "======================= \n"
                    + "Die Jobs sind: \n"
                    + "=======================");

            System.out.println("Deadline: " + deadline);
            for (int i = 0; i < alleJobs.size(); i++) {
                System.out.println("\n" + "Job " + i + ": ");
                for (Task task : alleJobs.get(i)) {
                    System.out.print(task.name + "  [" + task.duration[0] + "," + task.duration[1] + "]  " + task.machine + "  " + task.optional + " " + task.excludeTasks.toString() + "  |  ");
                }
            }
            System.out.println("\n" + "Auf den Maschinen: ");
            for (Machine machine : machines) {
                System.out.println(machine.id + "  " + machine.name + "  " + machine.optional);
            }
            System.out.println("\n"
                    + "======================= \n"
                    + "Start Solver \n"
                    + "=======================");

            Instant solveStart = Instant.now();
            SolverReturn result = solveProblem(mode, alleJobs, machines, deadline);
            Instant solveEnd = Instant.now();
            long solveDuration = Duration.between(solveStart, solveEnd).toMillis();
            long combinedDuration = readDuration + solveDuration;
            System.out.println("\n" + "Result: " + result.time + ",  Status: " + result.status);
            System.out.println("Read Dauer: " + readDuration +",   Solve Dauer: " + solveDuration + ", \n kombiniert: " + combinedDuration);
            // Suche über mehrere Instanzen
        } else if(mode2 == 1) {
            List<SolverReturn> results = new ArrayList<>();
            File directory = new File(configFolderPath);
            File[] directoryFiles = directory.listFiles();

            Double bestResult = Double.POSITIVE_INFINITY;
            String bestResultString = "";
            int iteration = 0;

            long combinedDuration = 0;
            long readDuration = 0;
            long solveDuration = 0;

            if(directoryFiles != null) {
                int index = 0;
                for(File config : directoryFiles) {
                    index++;
                    Instant readStart = Instant.now();
                    ConfigurationReader cReader = new ConfigurationReader();
                    String path = config.getPath();
                    cReader.ReadConfig(path, modelPath);
                    final int deadline = cReader.deadline;
                    final List<List<Task>> alleJobs = new ArrayList<>(cReader.jobs);
                    final List<Machine> machines = new ArrayList<>(cReader.machines);
                    Instant readEnd = Instant.now();
                    long singleReadDuration = Duration.between(readStart, readEnd).toMillis();
                    readDuration = readDuration + singleReadDuration;

                    System.out.println("\n"
                            + "======================= \n"
                            + "Die Jobs sind: \n"
                            + "=======================");

                    System.out.println("Deadline: " + deadline);
                    for (int i = 0; i < alleJobs.size(); i++) {
                        System.out.println("\n" + "Job " + i + ": ");
                        for (Task task : alleJobs.get(i)) {
                            System.out.print(task.name + "  [" + task.duration[0] + "," + task.duration[1] + "]  " + task.machine + "  " + task.optional + " " + task.excludeTasks.toString() + "  |  ");
                        }
                    }
                    System.out.println("\n" + "Auf den Maschinen: ");
                    for (Machine machine : machines) {
                        System.out.println(machine.id + "  " + machine.name + "  " + machine.optional);
                    }
                    System.out.println("\n"
                            + "======================= \n"
                            + "Start Solver \n"
                            + "=======================");

                    Instant solveStart = Instant.now();
                    SolverReturn result = solveProblem(mode, alleJobs, machines, deadline);
                    Instant solveEnd = Instant.now();
                    long singleSolveDuration = Duration.between(solveStart, solveEnd).toMillis();
                    solveDuration = solveDuration + singleSolveDuration;
                    System.out.println("Read: " + singleReadDuration + ", Solve: " + singleSolveDuration);

                    // Wenn nur nach feasible Lösung gesucht wird, kann die Suche beendet werden wenn eine gefunden wurde
                    if(mode == 0 && (result.status == CpSolverStatus.FEASIBLE || result.status == CpSolverStatus.OPTIMAL)) {
                        bestResult = result.time;
                        bestResultString = result.output;
                        break;
                    }
                    // Wenn nach Optimum gesucht wird, Werte updaten
                    if((result.status == CpSolverStatus.OPTIMAL || result.status == CpSolverStatus.FEASIBLE) && result.time < bestResult) {
                        bestResult = result.time;
                        bestResultString = result.output;
                        iteration = index;
                    }

                    System.out.println("\n" + "Result: " + result.time + ",  Status: " + result.status);

                }

                combinedDuration = readDuration + solveDuration;

                // Erfüllbarkeit Output
                if(mode == 0) {
                    System.out.println("Feasible Lösung gefunden, die " + bestResult + " dauert nach " + index + " Iterationen.");
                    System.out.printf(bestResultString);
                    System.out.println("Read Dauer: " + readDuration + ",  Solve Duration: " + solveDuration
                            + ", \n Kombiniert: " + combinedDuration);

                    // Optimum Output
                } else if(mode == 1) {
                    System.out.println("================ \n" + "Das beste Ergebnis ist in Iteration "+ iteration +": \n"
                            + "Zeit: " + bestResult);
                    System.out.printf(bestResultString);
                    System.out.println("Read Dauer: " + readDuration + ",  Solve Duration: " + solveDuration
                            + ", \n Kombiniert: " + combinedDuration);
                }
            }
            // Zufällige Probleme erstellen
        } else if(mode2 == 2) {
            SPGenerator spg = new SPGenerator();
            spg.generateProblem(2, 5, 2,
                    2, 1, 2, 1, 50, "asdf");

            FileInputStream fIn = new FileInputStream("src/main/probleme/problem.txt");
            ObjectInputStream oIn = new ObjectInputStream(fIn);
            SchedulingProblem sp = (SchedulingProblem) oIn.readObject();
            oIn.close();

            final List<List<Task>> alleJobs = new ArrayList<>(sp.jobs);
            final List<Machine> machines = new ArrayList<>(sp.machines);
            final int deadline = sp.deadline;

            System.out.println("\n"
                    + "======================= \n"
                    + "Die Jobs sind: \n"
                    + "=======================");

            System.out.println("Deadline: " + deadline);
            for (int i = 0; i < alleJobs.size(); i++) {
                System.out.println("\n" + "Job " + i + ": ");
                for (Task task : alleJobs.get(i)) {
                    System.out.print(task.name + "  [" + task.duration[0] + "," + task.duration[1] + "]  " + task.machine + "  " + task.optional
                            + " exclude: " + task.excludeTasks.toString() + " | ");
                }
            }

            System.out.println("\n" + "Auf den Maschinen: ");
            for (Machine machine : machines) {
                System.out.println(machine.id + "  " + machine.name + "  " + machine.optional);
            }

            if(mode2repeat == 0) {
                mode = 0;
                System.out.println("Problem generiert, 0 eingeben um beliebige Lösung zu suchen, 1 für optimale Lösung. \n " +
                        "Mit anderer ZAHL abbrechen");
                Scanner scanner = new Scanner(System.in);
                int con = scanner.nextInt();
                while(con == 0) {
                    Instant start = Instant.now();
                    SolverReturn result = solveProblem(mode, alleJobs, machines, deadline);
                    Instant end = Instant.now();
                    System.out.println("\n" + "Result: " + result.time + ",  Status: " + result.status);
                    System.out.println("(Feasible) Verstrichene Zeit: " + Duration.between(start, end).toMillis());
                    con = scanner.nextInt();
                }
                while (con == 1) {
                    mode = 1;
                    Instant start = Instant.now();
                    SolverReturn result = solveProblem(mode, alleJobs, machines, deadline);
                    Instant end = Instant.now();
                    System.out.println("\n" + "Result: " + result.time + ",  Status: " + result.status);
                    System.out.println("(Optimum) Verstrichene Zeit: " + Duration.between(start, end).toMillis());
                    con = scanner.nextInt();
                }
            } else {
                SolverReturn result = solveProblem(mode, alleJobs, machines, deadline);
                System.out.println("\n" + "Result: " + result.time + ",  Status: " + result.status);
            }
        }

         */
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
        System.out.println("Deadline: " + sp.deadline);
        int index = 0;
        for(List<Task> job : sp.jobs) {
            System.out.println("Job " + index + ": ");
            for(Task task : job) {
                System.out.println(task.name + ", d: [" + task.duration[0] +"," + task.duration[1] + "], m: " + task.machine + ", o: " + task.optional
                    + ", e: " + task.excludeTasks.toString());
            }
            index++;
        }

        System.out.println("*********************** \n");
    }


    public static void PrintSolution(SolverReturn sr) {
        System.out.println("Solution:");
        System.out.printf(sr.output + "\n");
        System.out.println("Schedule is " + sr.status + ", takes " + sr.time);
    }

    public static List<String[]> ConvertSRToStrings(SolverReturn sr) {
        Map<Machine, List<AssignedTask>> assignedJobs = new HashMap<>(sr.assignedJobs);
        List<String[]> resultString = new ArrayList<>();

        for(Machine m : assignedJobs.keySet()) {
            String[] mLine = new String[assignedJobs.get(m).size() + 1];
            String[] iLine = new String[assignedJobs.get(m).size() + 1];
            mLine[0] = m.name;
            iLine[0] = m.name + "Intervals";

            int index = 1;
            for(AssignedTask t : assignedJobs.get(m)) {
                if(t.isActive) {
                    mLine[index] = t.name;
                    iLine[index] = t.start + "," + (t.start + t.duration);
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