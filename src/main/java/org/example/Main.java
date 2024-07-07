package org.example;
import static java.lang.Math.max;

import com.google.ortools.Loader;
import com.google.ortools.sat.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
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

    /*static {
        System.loadLibrary("jniortools");
    }*/

    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException {

        Loader.loadNativeLibraries();

        //CPU = 0, GPU = 1
        //Task = (Machine, Duration)
        //Job 1 = (0,3),(1,4),(1,2) - Job 2 = (1,2),(0,4)
        //final List<List<Task>> alleJobs =
        //      Arrays.asList(Arrays.asList(new Task(0,3, "p1"), new Task(1,4, "p2"), new Task(1,2, "p3")),
        //          Arrays.asList(new Task(1,2, "p4"), new Task(0,4, "p5")));

        /*
        ProblemGenerator pg = new ProblemGenerator();

        // Bei allen Tasks die Maschine um 1 verringern, da hier die niedrigste Maschine den index 0 haben muss,
        // beim auslesen ist die niedrigste aber 1
        for(int i = 0; i < pg.jobs.size(); i++) {
            for(int j = 0; j < pg.jobs.get(i).size(); j++) {
                pg.jobs.get(i).get(j).machine = pg.jobs.get(i).get(j).machine - 1;
            }
        }

        final List<List<Task>> alleJobs = pg.jobs;

        for(int i = 0; i < alleJobs.size(); i++) {
            System.out.println("Job " + i + ":  ");
            for(int j = 0; j < alleJobs.get(i).size(); j++) {
                System.out.print("Task " + j + ":  ");
                System.out.println(alleJobs.get(i).get(j).name + " d:" + alleJobs.get(i).get(j).duration
                        + "   m:" + alleJobs.get(i).get(j).machine);
            }
        } */


        /*
        final List<List<Task>> alleJobs =
                Arrays.asList(Arrays.asList(new Task(0, new int[]{2,2}, "p1", false),
                        new Task(0, new int[]{2,4}, "p2", false),
                        new Task(1, new int[]{2,3}, "p3", false),
                        new Task(0, new int[]{1,1}, "p4", false)),
                        Arrays.asList(new Task(1, new int[]{1,3}, "p6", false),
                                new Task(0, new int[]{3,3}, "p7", false)),
                        Arrays.asList(new Task(1, new int[]{2,2}, "p8", true)));

        final List<Machine> machines = Arrays.asList(new Machine(0, false), new Machine(1, true));
        */

        final List<List<Task>> alleJobs =
                Arrays.asList(Arrays.asList(new Task(0, new int[]{2,2}, "p1", true),
                        new Task(0, new int[]{5,5}, "p2", true)));

        final List<Machine> machines = Arrays.asList(new Machine(0, true),
                new Machine(1, true));




        // ==========================================================================
        // ==========================================================================

        // Model
        CpModel  model = new CpModel();

        // Für jede optionale Maschine wird eine neue BoolVar
        for(Machine machine : machines) {
            if(machine.optional) {
                machine.active = model.newBoolVar("Machine" + machine.id + "_active");
            }
        }

        // Setzt fest wie viele Maschinen es gibt
        int numMachines = machines.size();

        // Und packt sie in ein Array
        // final int[] allMachines = IntStream.range(0, numMachines).toArray();

        // Berechnet wie lange es dauern würde, wenn alle Tasks einzeln nacheinander laufen würden
        int maxDuration = 0;
        for (List<Task> job : alleJobs) {
            for (Task task : job) {
                maxDuration += task.duration[1];
            }
        }
        System.out.println("Maximale Duration: " + maxDuration);

        // List<Integer> ist später {JobID, TaskID}, Map ist also <{JobID, TaskID}, TaskType-Objekt>
        Map<List<Integer>, TaskType> alleTasks = new HashMap<>();

        // Jede Maschine hat eine Liste mit Intervallen (Tasks) die sich nicht überlappen dürfen (kommt später)
        Map<Machine, List<IntervalVar>> machineToIntervals = new HashMap<>();

        // Jede optionale Maschine hat eine Liste mit BoolVars, die dafür stehen,
        // ob die Tasks die auf ihr ausgeführt werden, auch ausgeführt werden
        // Wenn alle false sind, kann die Maschine "deaktiviert" werden
        Map<Machine, List<BoolVar>> optionalMachineTaskActives = new HashMap<>();
        for(Machine machine : machines) {
            if(machine.optional) {
                optionalMachineTaskActives.put(machine, new ArrayList<>());
            }
        }
        System.out.println("Anzahl Maschinen: " + numMachines + ", davon sind " + optionalMachineTaskActives.size() + " optional");

        // Iteriert durch Jobs
        for(int jobID = 0; jobID < alleJobs.size(); ++jobID) {
            List<Task> job = alleJobs.get(jobID);
            // Iteriert durch Tasks des aktuellen Jobs
            for(int taskID = 0; taskID < job.size(); ++taskID) {
                Task task = job.get(taskID);
                String suffix = "_" + jobID + "_" + taskID;

                // Für jede Task wird ein TaskType erstellt
                TaskType taskType = new TaskType();
                //Tasks dürfen zwischen 0 und maxDuration starten und enden
                taskType.start = model.newIntVar(0, maxDuration, "start" + suffix);
                taskType.end = model.newIntVar(0, maxDuration, "end" + suffix);

                if(task.optional) {
                    taskType.interval = model.newOptionalIntervalVar(taskType.start,
                            model.newIntVar(task.duration[0], task.duration[1], task.name + "_duration"),
                            taskType.end,
                            taskType.active = model.newBoolVar(task.name + "_active"),
                            "interval_" + suffix);

                    // Wenn die optionale Task auf einer optionalen Maschine ausgeführt wird,
                    // Wird ein BoolVar für die Maschine zu optionalMachineTaskActives
                    for(Machine machine : machines) {
                        if(task.machine == machine.id && machine.optional) {
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
                    // Wenn die Task mandatory ist und auf einer optionalen Maschine ausgeführt wird,
                    // wird optionalMachineTaskActives der Maschine ein BoolVar hinzugefügt der immer true ist
                    // Dadurch kann die Maschine nicht mehr deaktiviert werden
                    model.addEquality(active, 1);
                    for(Machine machine : machines) {
                        if(task.machine == machine.id && machine.optional) {
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
                for(Machine machine1 : machines) {
                    if(machine1.id == task.machine) {
                        machine = machine1;
                    }
                }
                // Falls noch keine ArrayList für die Maschine vorhanden ist, wird eine neue erstellt
                machineToIntervals.computeIfAbsent(machine, (Machine m) -> new ArrayList<>());
                machineToIntervals.get(machine).add(taskType.interval);
            }
        }


        // Neue BoolVar erstellen, die am Ende gleich dem Aktivstatus der Maschine sein soll
        // Wenn mindestens eine Task in optionalMachineTaskActives für die Maschine true ist,
        // dann soll die Maschine auch aktiv sein
        for(Machine machine : optionalMachineTaskActives.keySet()) {
            if(optionalMachineTaskActives.get(machine).size() > 0) {
                BoolVar atLeastOneActive = model.newBoolVar(machine.id + "_atLeastOneActiveTask");
                model.addMaxEquality(atLeastOneActive, optionalMachineTaskActives.get(machine));
                model.addEquality(machine.active, atLeastOneActive);
            } else {
                model.addEquality(machine.active, 0);
            }
        }


        System.out.println("\n" + "Die folgenden Maschineen bekommen addNoOverlap");
        // Intervalle (Tasks) auf einer Maschine dürfen sich nicht überlappen
        for (Machine machine : machineToIntervals.keySet()) {
            System.out.println("Machine_" + machine.id + " mit der Intervalsize " + machineToIntervals.get(machine).size());
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

        // Liste mit Endzeiten
        List<IntVar> ends = new ArrayList<>();

        // Über alle Jobs
        /*
        for (int jobID = 0; jobID < alleJobs.size(); ++jobID) {
            // Liste mit Tasks des aktuellen Jobs
            List<Task> job = alleJobs.get(jobID);
            // Liste key mit Einträgen aus JobID und der Jobsize - 1 (Wenn Jobsize = 5, dann ist in Index 4 die letzte Task)
            List<Integer> key = Arrays.asList(jobID, job.size() - 1);
            // Das Ende der Task zu den Endzeiten hinzufügen
            // Am Ende hat man also eine Liste mit allen Endzeiten der letzten Tasks der Jobs
            ends.add(alleTasks.get(key).end);
        }

         */

        // Map die für jede Task (TaskType) die Endzeit der Task beinhaltet
        Map<TaskType, IntVar> endTimes = new HashMap<>();
        for (int jobID = 0; jobID < alleJobs.size(); jobID++) {
            List<Task> job = alleJobs.get(jobID);
            for (int taskID = 0; taskID < job.size(); taskID++) {
                List<Integer> key = Arrays.asList(jobID, taskID);
                endTimes.put(alleTasks.get(key), alleTasks.get(key).end);
                System.out.println(endTimes.size());
            }

        }

        System.out.println(alleTasks);
        System.out.println(endTimes.size());
        for(TaskType key : endTimes.keySet()) {
            System.out.println(endTimes.get(key).getName());
        }
        // Es wird die größte Endzeit in ends (allerletzte Task die beendet wird) gesucht, die mit einer Zahl in objVar
        // (Spanne von 0 bis maxDuration) übereinstimmt
        //model.addMaxEquality(objVar, ends);
        // Diese Zahl, soll minimiert werden

        // IntVar maxFullDuration = model.newIntVar(0, maxDuration, "maximal full duration");


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

        for(int i = 0; i < activeBools.length; i++) {
            possibleMaxEndtimes[i] = model.newIntVar(0, maxDuration, "possibleMaxEndtime_" + i);
            // System.out.println("Task " + i + "  " + activeBools[i].active);

            //Falls es eine optionale Task ist (wenn es keine optionale ist, dann ist active = null)
            if(activeBools[i].active != null) {
                model.addEquality(possibleMaxEndtimes[i], endtimeInts[i]).onlyEnforceIf(activeBools[i].active);
                model.addEquality(possibleMaxEndtimes[i], 0).onlyEnforceIf(activeBools[i].active.not());
            } else {
                model.addEquality(possibleMaxEndtimes[i], endtimeInts[i]);
            }

        }

        System.out.println("Die möglichen Zeiten die ausgewählt werden können wehe es steht 0 drinne :");
        for(IntVar time : possibleMaxEndtimes) {
            System.out.println(time);
        }

        model.addMaxEquality(objVar, possibleMaxEndtimes);
        model.minimize(objVar);


        CpSolver solver = new CpSolver();
        CpSolverStatus status = solver.solve(model);


        //==============================================================================================================
        //Output
        //==============================================================================================================
        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
            class AssignedTask {
                int jobID;
                int taskID;
                int start;
                int duration;
                boolean isActive;

                // Constructor
                AssignedTask(int jobID, int taskID, int start, int duration) {
                    this.jobID = jobID;
                    this.taskID = taskID;
                    this.start = start;
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
                            (int) solver.value(alleTasks.get(key).interval.getSizeExpr()));


                    if(task.optional) {
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
                    for(Machine machine1 : machines) {
                        if(machine1.id == task.machine) {
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
                if(!machine.optional || solver.value(machine.active) == 1) {
                    // Sort by starting time.
                    Collections.sort(assignedJobs.get(machine), new SortTasks());
                    String solLineTasks = "Machine " + machine.id + ": ";
                    String solLine = "           ";

                    for (AssignedTask assignedTask : assignedJobs.get(machine)) {
                        if(assignedTask.isActive) {
                            String name = "job_" + assignedTask.jobID + "_task_" + assignedTask.taskID;
                            // Add spaces to output to align columns.
                            solLineTasks += String.format("%-15s", name);

                            String solTmp = "[" + assignedTask.start + "," + (assignedTask.start + assignedTask.duration) + "]";
                            // Add spaces to output to align columns.
                            solLine += String.format("%-15s", solTmp);
                        }

                    }
                    output += solLineTasks + "%n";
                    output += solLine + "%n";
                }

            }
            System.out.printf("Optimal Schedule Length: %f%n", solver.objectiveValue());
            System.out.printf(output);

            for(Machine machine : machines) {
                if(machine.optional) {
                    System.out.println("Machine_" + machine.id + " ist aktiv? " + solver.value(machine.active));
                } else {
                    System.out.println("Machine_" + machine.id + " ist aktiv? true");
                }
                if(assignedJobs.get(machine) != null) {
                    for (AssignedTask assignedTask : assignedJobs.get(machine)) {
                        System.out.println("Task " + assignedTask.taskID + " ist aktiv? " + assignedTask.isActive);
                        System.out.println("Task " + assignedTask.taskID + " startet bei " + assignedTask.start + " und dauert " + assignedTask.duration);
                    }
                }
            }

            for(Machine machine : optionalMachineTaskActives.keySet()) {
                System.out.println("\n" + "Machine_" + machine.id + "ist optional und enhält BoolVars:");
                for(BoolVar b : optionalMachineTaskActives.get(machine)) {
                    System.out.println(solver.value(b));
                }
            }
        } else {
            System.out.println("No solution found.");
            /*
            for(int i = 0; i < alleJobs.size(); i++) {
                for(int j = 0; j < alleJobs.get(i).size(); j++) {
                    List<Integer> key = Arrays.asList(i, j);
                    TaskType task = alleTasks.get(key);

                    System.out.println("Task_" + key + "  Start: " + solver.value(task.start)
                        + "    End: " + solver.value(task.end));
                }
            }
            */

        }
    }
}