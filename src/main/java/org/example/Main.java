package org.example;
import static java.lang.Math.max;

import com.google.ortools.Loader;
import com.google.ortools.constraintsolver.Solver;
import com.google.ortools.sat.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;



public class Main {

    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {

        Loader.loadNativeLibraries();

        FMReader fmReader = new FMReader();

        String modelPath = "src/main/modelle/J3_T20_M3_A5.xml";
        String configFolderPath = "/home/max/eclipse-workspace/J3_T20_M3_A5/products";

        // 0 = Erfüllbarkeit prüfen, 1 = Optimum finden
        int mode = 1;
        // 0 = Variables FM, 1 = Mehrere Configs, 2 = "Zufälliges" Problem generieren
        int mode2 = 2;

        if(mode2 == 0) {
            fmReader.ReadFM(modelPath);
            final int deadline = fmReader.deadline;
            final List<List<Task>> alleJobs = new ArrayList<>(fmReader.jobs);
            final List<Machine> machines = new ArrayList<>(fmReader.machines);

            System.out.println("\n"
                    + "======================= \n"
                    + "Die Jobs sind: \n"
                    + "=======================");

            System.out.println("Deadline: " + deadline);
            for (int i = 0; i < alleJobs.size(); i++) {
                System.out.println("\n" + "Job " + i + ": ");
                for (Task task : alleJobs.get(i)) {
                    System.out.print(task.name + "  [" + task.duration[0] + "," + task.duration[1] + "]  " + task.machine + "  " + task.optional + "  |  ");
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

            SolverReturn result = solveProblem(mode, alleJobs, machines, deadline);
            System.out.println("\n" + "Result: " + result.time + ",  Status: " + result.status);
        } else if(mode2 == 1) {
            List<SolverReturn> results = new ArrayList<>();
            File directory = new File(configFolderPath);
            File[] directoryFiles = directory.listFiles();

            Double bestResult = Double.POSITIVE_INFINITY;
            String bestResultString = "";
            int iteration = 0;

            if(directoryFiles != null) {
                int index = 0;
                for(File config : directoryFiles) {
                    index++;
                    ConfigurationReader cReader = new ConfigurationReader();
                    String path = config.getPath();
                    cReader.ReadConfig(path, modelPath);
                    final int deadline = cReader.deadline;
                    final List<List<Task>> alleJobs = new ArrayList<>(cReader.jobs);
                    final List<Machine> machines = new ArrayList<>(cReader.machines);

                    System.out.println("\n"
                            + "======================= \n"
                            + "Die Jobs sind: \n"
                            + "=======================");

                    System.out.println("Deadline: " + deadline);
                    for (int i = 0; i < alleJobs.size(); i++) {
                        System.out.println("\n" + "Job " + i + ": ");
                        for (Task task : alleJobs.get(i)) {
                            System.out.print(task.name + "  [" + task.duration[0] + "," + task.duration[1] + "]  " + task.machine + "  " + task.optional + "  |  ");
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

                    SolverReturn result = solveProblem(mode, alleJobs, machines, deadline);

                    if(mode == 0 && (result.status == CpSolverStatus.FEASIBLE || result.status == CpSolverStatus.OPTIMAL)) {
                        bestResult = result.time;
                        bestResultString = result.output;
                        break;
                    }
                    if((result.status == CpSolverStatus.OPTIMAL || result.status == CpSolverStatus.FEASIBLE) && result.time < bestResult) {
                        bestResult = result.time;
                        bestResultString = result.output;
                        iteration = index;
                    }
                    System.out.println("\n" + "Result: " + result.time + ",  Status: " + result.status);
                }

                if(mode == 0) {
                    System.out.println("Feasible Lösung gefunden, die " + bestResult + " dauert nach " + index + " Iterationen.");
                    System.out.println(bestResultString);
                } else if(mode == 1) {
                    System.out.println("================ \n" + "Das beste Ergebnis ist in Iteration "+ iteration +": \n"
                            + bestResultString + "\n" + "Zeit: " + bestResult);
                }
            }
        } else if(mode2 == 2) {
            SPGenerator spg = new SPGenerator();
            SchedulingProblem sp = spg.generateProblem(5, 500, 100,
                    9, 6, 300, 20, 1100);
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
                    + "exclude: " + task.excludeTasks.toString() + " | ");
                }
            }
            System.out.println("\n" + "Auf den Maschinen: ");
            for (Machine machine : machines) {
                System.out.println(machine.id + "  " + machine.name + "  " + machine.optional);
            }

            SolverReturn result = solveProblem(mode, alleJobs, machines, deadline);
            System.out.println("\n" + "Result: " + result.time + ",  Status: " + result.status);

        }



    }


    public static SolverReturn solveProblem(int mode, List<List<Task>> alleJobs, List<Machine> machines, int deadline) {
        // Model
        CpModel model = new CpModel();

        // Für jede optionale Maschine wird eine neue BoolVar
        for (Machine machine : machines) {
            if (machine.optional) {
                machine.active = model.newBoolVar("Machine" + machine.id + "_active");
            }
        }

        // Setzt fest wie viele Maschinen es gibt
        int numMachines = machines.size();

        // Berechnet wie lange es dauern würde, wenn alle Tasks einzeln nacheinander laufen würden
        int maxDuration = 0;
        for (List<Task> job : alleJobs) {
            for (Task task : job) {
                maxDuration += task.duration[1];
            }
        }
        // System.out.println("Maximale Duration: " + maxDuration);

        // List<Integer> ist später {JobID, TaskID}, Map ist also <{JobID, TaskID}, TaskType-Objekt>
        Map<List<Integer>, TaskType> alleTasks = new HashMap<>();

        // Jede Maschine hat eine Liste mit Intervallen (Tasks) die sich nicht überlappen dürfen (kommt später)
        Map<Machine, List<IntervalVar>> machineToIntervals = new HashMap<>();

        // Liste von TaskTypes die einer exclude Constraint angehören, erst wenn alle Tasks erstellt wurden,
        // können die Constraints für die active BoolVars festgelegt werden
        List<TaskType> excludingTasks = new ArrayList<>();

        // Jede optionale Maschine hat eine Liste mit BoolVars, die dafür stehen,
        // ob die Tasks die auf ihr ausgeführt werden, auch ausgeführt werden
        // Wenn alle false sind, kann die Maschine "deaktiviert" werden
        Map<Machine, List<BoolVar>> optionalMachineTaskActives = new HashMap<>();
        for (Machine machine : machines) {
            if (machine.optional) {
                optionalMachineTaskActives.put(machine, new ArrayList<>());
            }
        }
        //System.out.println("Anzahl Maschinen: " + numMachines + ", davon sind " + optionalMachineTaskActives.size() + " optional");

        // Iteriert durch Jobs
        for (int jobID = 0; jobID < alleJobs.size(); ++jobID) {
            List<Task> job = alleJobs.get(jobID);
            // Iteriert durch Tasks des aktuellen Jobs
            for (int taskID = 0; taskID < job.size(); ++taskID) {
                Task task = job.get(taskID);
                String suffix = "_" + jobID + "_" + taskID;

                // Für jede Task wird ein TaskType erstellt
                TaskType taskType = new TaskType();
                taskType.name = task.name;
                //Tasks dürfen zwischen 0 und maxDuration starten und enden
                taskType.start = model.newIntVar(0, maxDuration, "start" + suffix);
                taskType.end = model.newIntVar(0, maxDuration, "end" + suffix);

                taskType.excludeTasks = task.excludeTasks;

                if (task.optional) {
                    BoolVar bool = model.newBoolVar(task.name + "_active");
                    taskType.active = bool;
                    taskType.interval = model.newOptionalIntervalVar(taskType.start,
                            model.newIntVar(task.duration[0], task.duration[1], task.name + "_duration"),
                            taskType.end,
                            taskType.active,
                            "interval_" + suffix);
                    if (task.excludeTasks.size() > 0) {
                        excludingTasks.add(taskType);
                    }

                    // Wenn die optionale Task auf einer optionalen Maschine ausgeführt wird,
                    // Wird ein BoolVar für die Maschine zu optionalMachineTaskActives
                    for (Machine machine : machines) {
                        if (task.machine == machine.id && machine.optional) {
                            optionalMachineTaskActives.get(machine).add(taskType.active);
                        }
                    }

                } else {
                    // Erstellt ein neues Interval mit (start, size, end, name)
                    taskType.interval = model.newIntervalVar(
                            taskType.start,
                            model.newIntVar(task.duration[0], task.duration[1], task.name + "_duration"),
                            taskType.end,
                            "interval" + suffix);

                    BoolVar active = model.newBoolVar(task.name + "_active");
                    if (task.excludeTasks.size() > 0) {
                        excludingTasks.add(taskType);
                    }
                    // Wenn die Task mandatory ist und auf einer optionalen Maschine ausgeführt wird,
                    // wird optionalMachineTaskActives der Maschine ein BoolVar hinzugefügt der immer true ist
                    // Dadurch kann die Maschine nicht mehr deaktiviert werden
                    model.addEquality(active, 1);
                    for (Machine machine : machines) {
                        if (task.machine == machine.id && machine.optional) {
                            optionalMachineTaskActives.get(machine).add(active);
                        }
                    }
                }

                // Packt die Task mit (jobID, taskID) in die AlleTasks Map
                List<Integer> key = Arrays.asList(jobID, taskID);
                alleTasks.put(key, taskType);

                // Die Maschine finden auf der die Task ausgeführt wird und ihr in
                // machineToIntervals das Interval der Task hinzufügen
                Machine machine = null;
                for (Machine machine1 : machines) {
                    if (machine1.id == task.machine) {
                        machine = machine1;
                    }
                }
                // Falls noch keine ArrayList für die Maschine vorhanden ist, wird eine neue erstellt
                machineToIntervals.computeIfAbsent(machine, (Machine m) -> new ArrayList<>());
                machineToIntervals.get(machine).add(taskType.interval);
            }
        }


        for (TaskType tt : excludingTasks) {
            // Für jeden TaskType in excludingTasks eine Liste mit den TTs active machen, die sie excluden will
            List<BoolVar> tasksToExclude = new ArrayList<>();
            for (TaskType tt2 : excludingTasks) {
                if (tt.excludeTasks.contains(tt2.name)) {
                    tasksToExclude.add(tt2.active);
                }
            }

            BoolVar atLeastOneActive = model.newBoolVar(tt.name + "_exclude_atLeastOneActive");

            model.addMaxEquality(atLeastOneActive, tasksToExclude);
            model.addEquality(tt.active, 1).onlyEnforceIf(atLeastOneActive.not());
            model.addEquality(tt.active, 0).onlyEnforceIf(atLeastOneActive);
        }

        // Neue BoolVar erstellen, die am Ende gleich dem Aktivstatus der Maschine sein soll
        // Wenn mindestens eine Task in optionalMachineTaskActives für die Maschine true ist,
        // dann soll die Maschine auch aktiv sein
        for (Machine machine : optionalMachineTaskActives.keySet()) {
            if (!optionalMachineTaskActives.get(machine).isEmpty()) {
                BoolVar atLeastOneActive = model.newBoolVar(machine.id + "_atLeastOneActiveTask");
                model.addMaxEquality(atLeastOneActive, optionalMachineTaskActives.get(machine));
                model.addEquality(machine.active, atLeastOneActive);
            } else {
                model.addEquality(machine.active, 0);
            }
        }


        // System.out.println("\n" + "Die folgenden Maschineen bekommen addNoOverlap");
        // Intervalle (Tasks) auf einer Maschine dürfen sich nicht überlappen
        for (Machine machine : machineToIntervals.keySet()) {
            // System.out.println("Machine_" + machine.id + " mit der Intervalsize " + machineToIntervals.get(machine).size());
            List<IntervalVar> list = machineToIntervals.get(machine);
            model.addNoOverlap(list);
        }

        // Tasks innerhalb eines Jobs dürfen nur nacheinander starten
        // Über alle Jobs
        for (int jobID = 0; jobID < alleJobs.size(); ++jobID) {
            List<Task> job = alleJobs.get(jobID);
            // Über alle Tasks
            for (int taskID = 0; taskID < job.size() - 1; ++taskID) {
                List<Integer> prevKey = Arrays.asList(jobID, taskID);
                List<Integer> nextKey = Arrays.asList(jobID, taskID + 1);
                // Einschränkung, dass Task 2 aus Job 1 erst nach Beendigung von Task 1 aus Job 1 startet (Beispiel)
                model.addGreaterOrEqual(alleTasks.get(nextKey).start, alleTasks.get(prevKey).end);
            }
        }


        // Zielvariable, nimmt am Ende die komplette Duration an und soll minimiert werden
        IntVar objVar = model.newIntVar(0, maxDuration, "makespan");

        // Map die für jede Task (TaskType) die Endzeit der Task beinhaltet
        Map<TaskType, IntVar> endTimes = new HashMap<>();
        for (int jobID = 0; jobID < alleJobs.size(); jobID++) {
            List<Task> job = alleJobs.get(jobID);
            for (int taskID = 0; taskID < job.size(); taskID++) {
                List<Integer> key = Arrays.asList(jobID, taskID);
                endTimes.put(alleTasks.get(key), alleTasks.get(key).end);
                //System.out.println(endTimes.size());
            }

        }

        // IntervalVars von optionalen Tasks, die nicht ausgeführt werden, werden nicht "performed",
        // sie exisitieren aber immernoch. Ihre lower und upper bounds, sowie noOverlaps werden ignoriert,
        // sodass sie eine duration von 0 haben und ganz am Anfang gestartet werden.
        // Da sie aber immernoch die letzte Task in einem Job sein können, und der Solver nicht denken soll,
        // dass das Ende des Jobs schon nach 0 Sekunden erreicht wird, erstellen wir (anders als vorher)
        // die Constraint, dass objVar == max(possibleMaxEndtimes).
        // Daher werden alle Endzeiten jeder Task possibleMaxEndtimes hinzugefügt
        // Falls eine Task inaktiv ist, ist die Endzeit 0, falls aktiv ist sie die tatsächliche Endzeit

        // Arrays mit jeweils allen BoolVars (bzw. den TaskTypes die die aktiv-BoolVars enthalten)
        // und Intvars von endTimes
        TaskType[] activeBools = endTimes.keySet().toArray(new TaskType[endTimes.size()]);
        IntVar[] endtimeInts = endTimes.values().toArray(new IntVar[endTimes.size()]);

        IntVar[] possibleMaxEndtimes = new IntVar[activeBools.length];

        for (int i = 0; i < activeBools.length; i++) {
            possibleMaxEndtimes[i] = model.newIntVar(0, maxDuration, "possibleMaxEndtime_" + i);
            //Falls es eine optionale Task ist (wenn es keine optionale ist, dann ist active = null)
            if (activeBools[i].active != null) {
                model.addEquality(possibleMaxEndtimes[i], endtimeInts[i]).onlyEnforceIf(activeBools[i].active);
                model.addEquality(possibleMaxEndtimes[i], 0).onlyEnforceIf(activeBools[i].active.not());
            } else {
                model.addEquality(possibleMaxEndtimes[i], endtimeInts[i]);
            }

        }

        // System.out.println("Die möglichen Zeiten die ausgewählt werden können wehe es steht 0 drinne :");
        for (IntVar time : possibleMaxEndtimes) {
            //System.out.println(time);
        }
        model.addLessOrEqual(objVar, deadline);
        model.addMaxEquality(objVar, possibleMaxEndtimes);


        CpSolver solver = new CpSolver();

        // Wenn auf Erfüllbarkeit geprüft wird, soll nach der ersten Lösung gestoppt werden
        if(mode == 0){
            solver.getParameters().setStopAfterFirstSolution(true);
        }
        model.minimize(objVar);
        CpSolverStatus status = solver.solve(model);


        //==============================================================================================================
        //Output
        //==============================================================================================================
        String outputString = "";
        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
            class AssignedTask {
                int jobID;
                int taskID;
                int start;
                int duration;
                String name;
                boolean isActive;

                // Constructor
                AssignedTask(int jobID, int taskID, int start, int duration, String name) {
                    this.jobID = jobID;
                    this.taskID = taskID;
                    this.start = start;
                    this.name = name;
                    this.duration = duration;
                }
            }

            class SortTasks implements Comparator<AssignedTask> {
                @Override
                public int compare(AssignedTask a, AssignedTask b) {
                    if (a.start != b.start) {
                        return a.start - b.start;
                    } else {
                        return a.duration - b.duration;
                    }
                }
            }
            System.out.println("Solution:");

            // Create one list of assigned tasks per machine.
            Map<Machine, List<AssignedTask>> assignedJobs = new HashMap<>();
            // Über jede Task iterieren
            for (int jobID = 0; jobID < alleJobs.size(); ++jobID) {
                List<Task> job = alleJobs.get(jobID);
                for (int taskID = 0; taskID < job.size(); ++taskID) {
                    Task task = job.get(taskID);
                    List<Integer> key = Arrays.asList(jobID, taskID);

                    AssignedTask assignedTask = new AssignedTask(
                            jobID,
                            taskID,
                            (int) solver.value(alleTasks.get(key).start),
                            (int) solver.value(alleTasks.get(key).interval.getSizeExpr()),
                            task.name);


                    if (task.optional) {
                        int active = (int) solver.value(alleTasks.get(key).active);
                        if (active == 1) {
                            assignedTask.isActive = true;
                        } else {
                            assignedTask.isActive = false;
                        }
                    } else {
                        assignedTask.isActive = true;
                    }

                    //assignedJobs.computeIfAbsent(task.machine, (Integer k) -> new ArrayList<>());
                    //assignedJobs.get(task.machine).add(assignedTask);
                    Machine machine = null;
                    for (Machine machine1 : machines) {
                        if (machine1.id == task.machine) {
                            machine = machine1;
                        }
                    }

                    assignedJobs.computeIfAbsent(machine, (Machine m) -> new ArrayList<>());
                    assignedJobs.get(machine).add(assignedTask);

                }
            }

            // Create per machine output lines.
            String output = "";
            for (Machine machine : machines) {
                if (!machine.optional || solver.value(machine.active) == 1) {
                    // Sort by starting time.
                    if(assignedJobs.get(machine) != null) {
                        Collections.sort(assignedJobs.get(machine), new SortTasks());
                    }
                    String solLineTasks = "Machine " + machine.id + ": ";
                    String solLine = "           ";

                    if(assignedJobs.get(machine) != null) {
                        for (AssignedTask assignedTask : assignedJobs.get(machine)) {
                            if (assignedTask.isActive) {
                                //String name = "job_" + assignedTask.jobID + "_task_" + assignedTask.taskID;
                                String name = assignedTask.name;
                                // Add spaces to output to align columns.
                                solLineTasks += String.format("%-15s", name);

                                String solTmp = "[" + assignedTask.start + "," + (assignedTask.start + assignedTask.duration) + "]";
                                // Add spaces to output to align columns.
                                solLine += String.format("%-15s", solTmp);
                            }

                        }
                    }
                    output += solLineTasks + "%n";
                    output += solLine + "%n";
                }

            }
            System.out.printf("Optimal Schedule Length: %f%n", solver.objectiveValue());
            System.out.printf(output);
            outputString = output;

            /*
            for (Machine machine : machines) {
                if (machine.optional) {
                    System.out.println("Machine_" + machine.id + " ist aktiv? " + solver.value(machine.active));
                } else {
                    System.out.println("Machine_" + machine.id + " ist aktiv? true");
                }
                if (assignedJobs.get(machine) != null) {
                    for (AssignedTask assignedTask : assignedJobs.get(machine)) {
                        System.out.println("Task " + assignedTask.taskID + " ist aktiv? " + assignedTask.isActive);
                        System.out.println("Task " + assignedTask.taskID + " startet bei " + assignedTask.start + " und dauert " + assignedTask.duration);
                    }
                }
            }

            for (Machine machine : optionalMachineTaskActives.keySet()) {
                System.out.println("\n" + "Machine_" + machine.id + "ist optional und enhält BoolVars:");
                for (BoolVar b : optionalMachineTaskActives.get(machine)) {
                    System.out.println(solver.value(b));
                }
            }

             */
        } else {
            System.out.println("No solution found.");
        }
        SolverReturn result = new SolverReturn(solver.objectiveValue(), status, outputString);
        return result;
    }
}