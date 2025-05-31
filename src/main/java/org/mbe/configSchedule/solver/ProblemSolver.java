package org.mbe.configSchedule.solver;

import com.google.ortools.sat.*;
import com.google.ortools.util.Domain;
import org.mbe.configSchedule.util.*;

import java.util.*;

public class ProblemSolver {

    private final CpModel model = new CpModel();
    // List<Integer> ist später {JobID, TaskID}, Map ist also <{JobID, TaskID}, TaskType-Objekt>
    // TaskID ist nur der Index der Task im Job, {JobID, TaskID} ist sozusagen die echte TaskID der Task
    private final Map<List<Integer>, TaskType> allTaskTypes = new HashMap<>();

    private final SchedulingProblem sp;

    public ProblemSolver(SchedulingProblem sp) {
        this.sp = sp;
        buildModel(sp);
    }

    public SolverReturn getFirstSolution() {
        CpSolver solver = new CpSolver();

        // Wenn auf Erfüllbarkeit geprüft wird, soll nach der ersten Lösung gestoppt werden
        solver.getParameters().setStopAfterFirstSolution(true);

        // Solven
        return getSolverReturn(sp, solver);
    }

    public SolverReturn getBestSolution() {
        CpSolver solver = new CpSolver();

        // Solven
        return getSolverReturn(sp, solver);
    }

    private SolverReturn getSolverReturn(SchedulingProblem sp, CpSolver solver) {
        CpSolverStatus status = solver.solve(model);
        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
            Map<Machine, List<AssignedTask>> assignedJobs = getAssignedJobs(sp, solver);
            String outputString = generateOutputString(sp, solver, assignedJobs);
            return new SolverReturn(solver.objectiveValue(), status, outputString, assignedJobs);
        } else {
            return null;
        }
    }

    private String generateOutputString(SchedulingProblem sp, CpSolver solver, Map<Machine, List<AssignedTask>> assignedJobs) {
        class SortTasks implements Comparator<AssignedTask> {
            @Override
            public int compare(AssignedTask a, AssignedTask b) {
                if (a.getStart() != b.getStart()) {
                    return a.getStart() - b.getStart();
                } else {
                    return a.getDuration() - b.getDuration();
                }
            }
        }

        int longestMachineName = sp.getMachines().stream()
                .mapToInt(m -> m.getName().length())
                .max()
                .orElse(0);

        // Create per machine output lines.
        StringBuilder output = new StringBuilder();
        for (Machine machine : sp.getMachines()) {
            if (!machine.isOptional() || solver.value(machine.getActive()) == 1) {
                // Sort by starting time.
                if (assignedJobs.get(machine) != null) {
                    assignedJobs.get(machine).sort(new SortTasks());
                }

                StringBuilder solLineTasks = new StringBuilder("Machine " + machine.getName() + ": ");
                solLineTasks.append(" ".repeat(Math.max(0, longestMachineName - machine.getName().length())));
                StringBuilder solLine = new StringBuilder();
                solLine.append(" ".repeat(longestMachineName + 10)); // 10 = length of "Machine_: "

                if (assignedJobs.get(machine) != null) {
                    for (AssignedTask assignedTask : assignedJobs.get(machine)) {
                        if (assignedTask.isActive()) {
                            //String name = "job_" + assignedTask.jobID + "_task_" + assignedTask.taskID;
                            String name = assignedTask.getName();
                            // Add spaces to output to align columns.
                            solLineTasks.append(String.format("%-15s", name));

                            String solTmp = "[" + assignedTask.getStart() + "," + (assignedTask.getStart() + assignedTask.getDuration()) + "]";
                            // Add spaces to output to align columns.
                            solLine.append(String.format("%-15s", solTmp));
                        }

                    }
                }
                output.append(solLineTasks).append(System.lineSeparator());
                output.append(solLine).append(System.lineSeparator());
            }

        }
        return output.toString();
    }

    private Map<Machine, List<AssignedTask>> getAssignedJobs(SchedulingProblem sp, CpSolver solver) {
        Map<Machine, List<AssignedTask>> assignedJobs = new HashMap<>();

        //System.out.println("Solution:");

        // Create one list of assigned tasks per machine.
        //Map<Machine, List<AssignedTask>> assignedJobs = new HashMap<>();
        // Über jede Task iterieren
        for (int jobID = 0; jobID < sp.getJobs().size(); ++jobID) {
            List<Task> job = sp.getJobs().get(jobID);
            for (int taskID = 0; taskID < job.size(); ++taskID) {
                Task task = job.get(taskID);
                List<Integer> key = Arrays.asList(jobID, taskID);

                AssignedTask assignedTask = new AssignedTask(
                        jobID,
                        taskID,
                        (int) solver.value(allTaskTypes.get(key).getStart()),
                        (int) solver.value(allTaskTypes.get(key).getInterval().getSizeExpr()),
                        task.getName());


                if (task.isOptional()) {
                    int active = (int) solver.value(allTaskTypes.get(key).getActive());
                    assignedTask.setActive(active == 1);
                } else {
                    assignedTask.setActive(true);
                }

                Machine machine = task.getMachine();

                assignedJobs.computeIfAbsent(machine, (Machine _) -> new ArrayList<>());
                assignedJobs.get(machine).add(assignedTask);

            }
        }
        return assignedJobs;
    }

    private void buildModel(SchedulingProblem sp) {
        // Berechnet wie lange es dauern würde, wenn alle Tasks einzeln nacheinander laufen würden
        int maxDuration = sp.getJobs().stream().flatMap(Collection::stream).mapToInt(Task::getMaximumDuration).sum();

        // Map mit Tasknamen als Key, genutzt für die Duration Constraints
        Map<String, TaskType> nameToTaskType = new HashMap<>();

        // Jede Maschine hat eine Liste mit Intervallen (Tasks) die sich nicht überlappen dürfen
        Map<Machine, List<IntervalVar>> machineToIntervals = new HashMap<>();

        // Liste von TaskTypes die einer alterantive Task Gruppe angehören, erst wenn alle Tasks erstellt wurden,
        // können die Constraints für die active BoolVars festgelegt werden
        List<TaskType> excludingTasks = new ArrayList<>();

        List<TaskType> taskTypesWithDurationConstraints = new ArrayList<>();

        // Jede optionale Maschine hat eine Liste mit BoolVars, die dafür stehen,
        // ob die Tasks die auf ihr ausgeführt werden, auch aktiv sind
        // Wenn alle false sind, kann die Maschine "deaktiviert" werden
        Map<Machine, List<BoolVar>> optionalMachineTaskActives = new HashMap<>();
        for (Machine machine : sp.getMachines()) {
            if (machine.isOptional()) {
                optionalMachineTaskActives.put(machine, new ArrayList<>());
                machine.setActive(model.newBoolVar("Machine_" + machine.getName() + "_active"));
            }
        }

        buildTaskTypes(sp, maxDuration,
                optionalMachineTaskActives,
                excludingTasks,
                nameToTaskType,
                taskTypesWithDurationConstraints,
                machineToIntervals
        );
        buildDurationConstraints(taskTypesWithDurationConstraints, nameToTaskType);
        buildExcludeConstraints(excludingTasks, nameToTaskType);
        buildOptionalMachines(optionalMachineTaskActives);

        buildValidityConstraints(sp, machineToIntervals);

        // TODO: Is this even needed still?
        // IntervalVars von optionalen Tasks, die nicht ausgeführt werden, werden nicht "performed",
        // sie existieren aber immer noch. Ihre lower und upper bounds, sowie noOverlaps werden ignoriert,
        // sodass sie eine duration von 0 haben und ganz am Anfang gestartet werden.
        // Da sie aber immer noch die letzte Task in einem Job sein können, und der Solver nicht denken soll,
        // dass das Ende des Jobs schon nach 0 Sekunden erreicht wird, erstellen wir (anders als vorher)
        // die Constraint, dass objVar == max(possibleMaxEndtimes).
        // Daher werden alle Endzeiten jeder Task possibleMaxEndtimes hinzugefügt
        // Falls eine Task inaktiv ist, ist die Endzeit 0, falls aktiv ist sie die tatsächliche Endzeit
        List<IntVar> possibleMaxEndtimes = new ArrayList<>();
        for (TaskType taskType : allTaskTypes.values()) {
            IntVar possibleEndTime = model.newIntVar(0, maxDuration, "possibleMaxEndtime_" + taskType.getName());
            if (taskType.getTask().isOptional()) {
                model.addEquality(possibleEndTime, taskType.getEnd()).onlyEnforceIf(taskType.getActive());
                model.addEquality(possibleEndTime, 0).onlyEnforceIf(taskType.getActive().not());
            } else {
                model.addEquality(possibleEndTime, taskType.getEnd());
            }
            possibleMaxEndtimes.add(possibleEndTime);
        }

        // Zielvariable, nimmt am Ende die komplette Duration an und soll minimiert werden
        IntVar objVar = model.newIntVar(0, maxDuration, "makespan");

        // objVar darf höchstens so groß wie die Deadline sein
        // und soll den selben Wert wie die maximale Endtime haben (Endzeitpunkt des letzten Tasks)
        model.addLessOrEqual(objVar, sp.getDeadline());
        model.addMaxEquality(objVar, possibleMaxEndtimes);

        // objVar soll so klein wie möglich gehalten werden
        model.minimize(objVar);
    }

    private void buildValidityConstraints(SchedulingProblem sp, Map<Machine, List<IntervalVar>> machineToIntervals) {
        // Intervalle (Tasks) auf einer Maschine dürfen sich nicht überlappen
        for (List<IntervalVar> intervalsOnMachine : machineToIntervals.values()) {
            model.addNoOverlap(intervalsOnMachine);
        }

        // Tasks innerhalb eines Jobs dürfen nur nacheinander starten
        // Über alle Jobs
        for (int jobID = 0; jobID < sp.getJobs().size(); ++jobID) {
            List<Task> job = sp.getJobs().get(jobID);
            // Über alle Tasks
            for (int taskID = 0; taskID < job.size() - 1; ++taskID) {
                List<Integer> prevKey = List.of(jobID, taskID);
                List<Integer> nextKey = Arrays.asList(jobID, taskID + 1);
                // Einschränkung, dass Task 2 aus Job 1 erst nach Beendigung von Task 1 aus Job 1 startet (Beispiel)
                model.addGreaterOrEqual(allTaskTypes.get(nextKey).getStart(), allTaskTypes.get(prevKey).getEnd());
            }
        }
    }

    private void buildOptionalMachines(Map<Machine, List<BoolVar>> optionalMachineTaskActives) {
        // Neue BoolVar erstellen, die am Ende gleich dem Aktivstatus der Maschine sein soll.
        // Wenn mindestens ein Task in optionalMachineTaskActives für die Maschine true ist,
        // dann soll die Maschine auch aktiv sein
        for (Machine machine : optionalMachineTaskActives.keySet()) {
            if (!optionalMachineTaskActives.get(machine).isEmpty()) {
                BoolVar atLeastOneActive = model.newBoolVar(machine.getName() + "_atLeastOneActiveTask");
                model.addMaxEquality(atLeastOneActive, optionalMachineTaskActives.get(machine));
                model.addEquality(machine.getActive(), atLeastOneActive);
            } else {
                model.addEquality(machine.getActive(), model.falseLiteral());
            }
        }
    }

    private void buildExcludeConstraints(List<TaskType> excludingTasks, Map<String, TaskType> nameToTaskType) {
        // Für jeden Task in der excludingTasks-Liste wird eine BoolVar und eine Liste mit den actives der TaskTypes,
        // mit denen sie in einer alternativen Taskgruppe sind, erstellt.
        // Die BoolVar nimmt den maximalen Value der Liste an
        // Wenn der BoolVar = 0, dann muss task.active = 1
        // Wenn der BoolVar = 1, dann muss task.active = 0
        for (TaskType taskType : excludingTasks) {
            // Für jeden TaskType in excludingTasks eine Liste mit den TTs active machen, die sie excluden will
            List<BoolVar> tasksToExclude = new ArrayList<>();
            for (String excludeTaskName : taskType.getTask().getExcludeTasks()) {
                tasksToExclude.add(nameToTaskType.get(excludeTaskName).getActive());
            }

            BoolVar atLeastOneActive = model.newBoolVar(taskType.getName() + "_exclude_atLeastOneActive");

            model.addMaxEquality(atLeastOneActive, tasksToExclude);
            model.addEquality(taskType.getActive(), 1).onlyEnforceIf(atLeastOneActive.not());
            model.addEquality(taskType.getActive(), 0).onlyEnforceIf(atLeastOneActive);
        }
    }

    private void buildDurationConstraints(List<TaskType> taskTypesWithDurationConstraints, Map<String, TaskType> nameToTaskType) {
        // Für jede Task, die mindestens eine durationConstraint hat, werden Constraints dem Model hinzugefügt
        for (TaskType taskType : taskTypesWithDurationConstraints) {
            Map<Integer, List<Task>> durationCons = taskType.getTask().getDurationCons();
            for (Map.Entry<Integer, List<Task>> con : durationCons.entrySet()) {
                int durationValue = con.getKey();
                // BoolVar soll 1 annehmen, wenn alle benötigten Tasks für diese Duration aktiv sind, ansonsten 0
                BoolVar allRequiredTasksActive = model.newBoolVar(taskType.getName() + "_durationConstraints_" + con.getKey());

                // Liste mit den active-BoolVars der required TaskTypes
                List<BoolVar> requiredBoolVars = new ArrayList<>();
                for (Task requiredTask : con.getValue()) {
                    requiredBoolVars.add(nameToTaskType.get(requiredTask.getName()).getActive());
                }

                model.addMinEquality(allRequiredTasksActive, requiredBoolVars);

                // Falls eine der required Tasks nicht aktiv ist, darf die zugehörie Dauer auch nicht für
                // die Task gewählt werden
                model.addDifferent(taskType.getInterval().getSizeExpr(), durationValue).onlyEnforceIf(allRequiredTasksActive.not());
            }
        }
    }

    private void buildTaskTypes(
            SchedulingProblem sp,
            int maxDuration,
            Map<Machine, List<BoolVar>> optionalMachineTaskActives,
            List<TaskType> excludingTasks,
            Map<String, TaskType> nameToTaskType,
            List<TaskType> taskTypesWithDurationConstraints,
            Map<Machine, List<IntervalVar>> machineToIntervals) {
        for (int jobID = 0; jobID < sp.getJobs().size(); ++jobID) {
            List<Task> job = sp.getJobs().get(jobID);
            // Iteriert durch Tasks des aktuellen Jobs
            for (int taskID = 0; taskID < job.size(); ++taskID) {
                Task task = job.get(taskID);

                TaskType taskType = createTaskType(task, jobID + "_" + taskID, maxDuration);

                // Wenn der optionale Task auf einer optionalen Maschine ausgeführt wird,
                // so wird eine BoolVar für die Maschine zu optionalMachineTaskActives hinzugefügt
                if (task.getMachine().isOptional()) {
                    optionalMachineTaskActives.get(task.getMachine()).add(taskType.getActive());
                }

                if (!task.getExcludeTasks().isEmpty()) {
                    excludingTasks.add(taskType);
                }

                // Packt die Task mit (jobID, taskID) in die AlleTasks Map
                List<Integer> key = Arrays.asList(jobID, taskID);
                allTaskTypes.put(key, taskType);
                nameToTaskType.put(task.getName(), taskType);

                // Wenn die Task eine Duration hat, die nur gewählt werden darf, wenn eine andere Task ausgeführt wird
                if (task.getDurationCons() != null && !task.getDurationCons().isEmpty()) {
                    taskTypesWithDurationConstraints.add(taskType);
                }

                // Falls noch keine ArrayList für die Maschine vorhanden ist, wird eine neue erstellt
                machineToIntervals.computeIfAbsent(task.getMachine(), (Machine m) -> new ArrayList<>());
                machineToIntervals.get(task.getMachine()).add(taskType.getInterval());
            }
        }
    }

    /**
     * Create the {@link TaskType} for a task.
     *
     * @param task           the task for which to create the TaskType
     * @param taskIdentifier the identifier for the task.
     * @param maxDuration    the maximumDuration of the scheduling problem
     * @return a fitting TaskType
     */
    private TaskType createTaskType(Task task, String taskIdentifier, int maxDuration) {
        // Für jede Task wird ein TaskType erstellt
        TaskType taskType = new TaskType(task);
        //Tasks dürfen zwischen 0 und maxDuration starten und enden
        taskType.setStart(model.newIntVar(0, maxDuration, "start_" + taskIdentifier));
        taskType.setEnd(model.newIntVar(0, maxDuration, "end_" + taskIdentifier));

        long[] taskDurations = Arrays.stream(task.getDurations()).asLongStream().toArray();
        IntVar possibleDurations = model.newIntVarFromDomain(Domain.fromValues(taskDurations), task.getName() + "_duration");

        BoolVar taskActive = model.newBoolVar(task.getName() + "_active");
        taskType.setActive(taskActive);

        // Wenn eine Task optional ist, bekommt sie ein OptionalIntervalVar, sodass das Interval
        // nicht performed, wenn die Task nicht aktiv ist
        // dafür hat das Interval den Task.active als Literal
        IntervalVar durationIntervalVar;
        if (task.isOptional()) {
            durationIntervalVar = model.newOptionalIntervalVar(
                    taskType.getStart(),
                    possibleDurations,
                    taskType.getEnd(),
                    taskType.getActive(),
                    "interval_" + taskIdentifier
            );
        } else {
            // Wenn der Task nicht optional ist, wird ein normales IntervalVar erstellt, das immer performt.
            durationIntervalVar = model.newIntervalVar(
                    taskType.getStart(),
                    possibleDurations,
                    taskType.getEnd(),
                    "interval_" + taskIdentifier
            );

            // Mandatory Tasks müssen immer aktiv sein
            model.addEquality(taskType.getActive(), model.trueLiteral());
        }
        taskType.setInterval(durationIntervalVar);
        return taskType;
    }
}
