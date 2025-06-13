package org.mbe.configSchedule.solver;

import com.google.ortools.sat.*;
import com.google.ortools.util.Domain;
import org.mbe.configSchedule.util.*;

import java.util.*;

public class ProblemSolver {

    private final SchedulingProblem sp;
    // List<Integer> ist später {JobID, TaskID}, Map ist also <{JobID, TaskID}, TaskType-Objekt>
    // TaskID ist nur der Index der Task im Job, {JobID, TaskID} ist sozusagen die echte TaskID der Task
    private final Map<List<Integer>, TaskType> allTaskTypes = new HashMap<>();

    private final CpModel baseModel = new CpModel();
    private IntVar makespan;

    private CpSolver makespanSolver;

    private SolverReturn result;

    public ProblemSolver(SchedulingProblem sp) {
        this.sp = sp;
        buildModel();
    }

    /**
     * Solves the scheduling problem feasibly.
     *
     * <p>The result can be got with a call to {@link #getSolverReturn()}.
     */
    public void findFeasibleSolution() {
        this.makespanSolver = new CpSolver();
        this.makespanSolver.getParameters().setStopAfterFirstSolution(true);
        createSolverReturn(this.makespanSolver);
    }

    /**
     * Solves the scheduling problem optimally.
     *
     * <p>The result can be got with a call to {@link #getSolverReturn()}.
     */
    public void findOptimalSolution() {
        this.makespanSolver = new CpSolver();
        createSolverReturn(this.makespanSolver);
    }

    /**
     * Returns the {@link SolverReturn} containing all information from previous analyses calls.
     *
     * @return the result of previous analyses.
     */
    public SolverReturn getSolverReturn() {
        return this.result;
    }

    private void createSolverReturn(CpSolver solver) {
        CpModel makespanModel = this.baseModel.getClone();
        makespanModel.minimize(this.makespan);

        CpSolverStatus status = solver.solve(makespanModel);
        Schedule schedule;
        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
            schedule = createSchedule(solver);
        } else {
            schedule = null;
        }
        this.result = new SolverReturn(solver.userTime(), status, schedule);
    }

    private Schedule createSchedule(CpSolver solver) {
        Schedule schedule = new Schedule(solver.objectiveValue());

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

                // Add task to machine's task list, if it is executed
                if (!task.isOptional() || solver.value(allTaskTypes.get(key).getActive()) == 1) {
                    schedule.addTaskToMachine(assignedTask, task.getMachine());
                }
            }
        }
        return schedule;
    }

    private void buildModel() {
        // Used as the upper bound for the domain of task durations.
        // A negative number as the deadline represents a deadline of infinity in which case we calculate the
        // upper bound for task durations as the sum of all task durations.
        // Adding 1 so tasks with unbound durations can assume an infeasible duration.
        int maxDuration = 1 + (sp.getDeadline() >= 0 ?
                sp.getDeadline() :
                sp.getJobs().stream().flatMap(Collection::stream).mapToInt(Task::getMaximumDuration).sum());

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
                machine.setActive(baseModel.newBoolVar("Machine_" + machine.getName() + "_active"));
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
        // die Constraint, dass makespan == max(possibleMaxEndtimes).
        // Daher werden alle Endzeiten jeder Task possibleMaxEndtimes hinzugefügt
        // Falls eine Task inaktiv ist, ist die Endzeit 0, falls aktiv ist sie die tatsächliche Endzeit
        List<IntVar> possibleMaxEndtimes = new ArrayList<>();
        for (TaskType taskType : allTaskTypes.values()) {
            IntVar possibleEndTime = baseModel.newIntVar(0, maxDuration, "possibleMaxEndtime_" + taskType.getName());
            if (taskType.getTask().isOptional()) {
                baseModel.addEquality(possibleEndTime, taskType.getEnd()).onlyEnforceIf(taskType.getActive());
                baseModel.addEquality(possibleEndTime, 0).onlyEnforceIf(taskType.getActive().not());
            } else {
                baseModel.addEquality(possibleEndTime, taskType.getEnd());
            }
            possibleMaxEndtimes.add(possibleEndTime);
        }

        // Zielvariable, nimmt am Ende die komplette Duration an und soll minimiert werden
        this.makespan = baseModel.newIntVar(0, maxDuration, "makespan");
        baseModel.addMaxEquality(this.makespan, possibleMaxEndtimes);

        if (sp.getDeadline() >= 0)
            baseModel.addLessOrEqual(this.makespan, sp.getDeadline());
    }

    private void buildValidityConstraints(SchedulingProblem sp, Map<Machine, List<IntervalVar>> machineToIntervals) {
        // Intervalle (Tasks) auf einer Maschine dürfen sich nicht überlappen
        for (List<IntervalVar> intervalsOnMachine : machineToIntervals.values()) {
            baseModel.addNoOverlap(intervalsOnMachine);
        }

        // Tasks innerhalb eines Jobs dürfen nur nacheinander starten.
        // NOTE: For tasks required by duration constraints, this is already handled below in buildDurationConstraints(...).
        for (int jobID = 0; jobID < sp.getJobs().size(); ++jobID) {
            List<Task> job = sp.getJobs().get(jobID);
            for (int taskID = 0; taskID < job.size() - 1; ++taskID) {
                List<Integer> prevKey = List.of(jobID, taskID);
                List<Integer> nextKey = Arrays.asList(jobID, taskID + 1);
                // Einschränkung, dass Task 2 aus Job 1 erst nach Beendigung von Task 1 aus Job 1 startet (Beispiel)
                baseModel.addGreaterOrEqual(allTaskTypes.get(nextKey).getStart(), allTaskTypes.get(prevKey).getEnd());
            }
        }
    }

    private void buildOptionalMachines(Map<Machine, List<BoolVar>> optionalMachineTaskActives) {
        // Neue BoolVar erstellen, die am Ende gleich dem Aktivstatus der Maschine sein soll.
        // Wenn mindestens ein Task in optionalMachineTaskActives für die Maschine true ist,
        // dann soll die Maschine auch aktiv sein
        for (Machine machine : optionalMachineTaskActives.keySet()) {
            if (!optionalMachineTaskActives.get(machine).isEmpty()) {
                BoolVar atLeastOneActive = baseModel.newBoolVar(machine.getName() + "_atLeastOneActiveTask");
                baseModel.addMaxEquality(atLeastOneActive, optionalMachineTaskActives.get(machine));
                baseModel.addEquality(machine.getActive(), atLeastOneActive);
            } else {
                baseModel.addEquality(machine.getActive(), baseModel.falseLiteral());
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

            BoolVar atLeastOneActive = baseModel.newBoolVar(taskType.getName() + "_exclude_atLeastOneActive");

            baseModel.addMaxEquality(atLeastOneActive, tasksToExclude);
            baseModel.addEquality(taskType.getActive(), 1).onlyEnforceIf(atLeastOneActive.not());
            baseModel.addEquality(taskType.getActive(), 0).onlyEnforceIf(atLeastOneActive);
        }
    }

    private void buildDurationConstraints(List<TaskType> taskTypesWithDurationConstraints, Map<String, TaskType> nameToTaskType) {
        // Für jede Task, die mindestens eine durationConstraint hat, werden Constraints dem Model hinzugefügt
        for (TaskType taskType : taskTypesWithDurationConstraints) {
            Map<Integer, List<Task>> durationCons = taskType.getTask().getDurationCons();
            for (Map.Entry<Integer, List<Task>> con : durationCons.entrySet()) {
                int durationValue = con.getKey();
                // BoolVar soll 1 annehmen, wenn alle benötigten Tasks für diese Duration aktiv sind, ansonsten 0
                BoolVar allRequiredTasksActive = baseModel.newBoolVar(taskType.getName() + "_durationConstraints_" + con.getKey());

                // Liste mit den active-BoolVars der required TaskTypes
                List<BoolVar> requiredBoolVars = new ArrayList<>();
                for (Task requiredTask : con.getValue()) {
                    TaskType requiredTaskType = nameToTaskType.get(requiredTask.getName());
                    requiredBoolVars.add(requiredTaskType.getActive());

                    // The dependent task should only start after all requirements are finished,
                    // but only if the duration of the duration constraint is actually selected.
                    // (taskType.selectedDuration == durationOfDurationConstraint) => (taskType.start >= requiredTask.end)
                    //
                    // The required task may still be executed as a requirement of a different task, but since this task
                    // did not select the duration of the duration constraint it is not dependent on the required task.
                    //
                    // This cannot be directly encoded for CP-SAT because we cannot set a boolean variable based on
                    // the equality of two other variables. Therefore, we need to use the channeling pattern as described here:
                    // https://github.com/google/or-tools/blob/stable/ortools/sat/docs/channeling.md#java-code
                    BoolVar constraintDurationSelected = baseModel.newBoolVar(taskType.getName() + "_constraintDurationSelected_" + con.getKey());

                    // Set `constraintDurationSelected = (task.selectedDuration == durationValue)`
                    LinearExpr sizeExpr = taskType.getInterval().getSizeExpr();
                    baseModel.addEquality(sizeExpr, durationValue).onlyEnforceIf(constraintDurationSelected);
                    baseModel.addDifferent(sizeExpr, durationValue).onlyEnforceIf(constraintDurationSelected.not());

                    baseModel.addGreaterOrEqual(taskType.getStart(), requiredTaskType.getEnd()).onlyEnforceIf(constraintDurationSelected);
                }

                baseModel.addMinEquality(allRequiredTasksActive, requiredBoolVars);

                // Falls eine der required Tasks nicht aktiv ist, darf die zugehörige Dauer auch nicht für
                // die Task gewählt werden
                baseModel.addDifferent(taskType.getInterval().getSizeExpr(), durationValue).onlyEnforceIf(allRequiredTasksActive.not());
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
        taskType.setStart(baseModel.newIntVar(0, maxDuration, "start_" + taskIdentifier));
        taskType.setEnd(baseModel.newIntVar(0, maxDuration, "end_" + taskIdentifier));

        long[] taskDurations = Arrays.stream(task.getDurations()).asLongStream().toArray();
        Domain durationsDomain = Domain.fromValues(taskDurations);
        Optional<Integer> unboundDurationsBound = task.getUnboundDurations();
        if (unboundDurationsBound.isPresent()) {
            Domain unboundDurationsDomain = Domain.fromFlatIntervals(new long[] {unboundDurationsBound.get(), maxDuration});
            durationsDomain = durationsDomain.unionWith(unboundDurationsDomain);
        }
        IntVar possibleDurations = baseModel.newIntVarFromDomain(durationsDomain, task.getName() + "_duration");

        BoolVar taskActive = baseModel.newBoolVar(task.getName() + "_active");
        taskType.setActive(taskActive);

        // Wenn eine Task optional ist, bekommt sie ein OptionalIntervalVar, sodass das Interval
        // nicht performed, wenn die Task nicht aktiv ist
        // dafür hat das Interval den Task.active als Literal
        IntervalVar durationIntervalVar;
        if (task.isOptional()) {
            durationIntervalVar = baseModel.newOptionalIntervalVar(
                    taskType.getStart(),
                    possibleDurations,
                    taskType.getEnd(),
                    taskType.getActive(),
                    "interval_" + taskIdentifier
            );
        } else {
            // Wenn der Task nicht optional ist, wird ein normales IntervalVar erstellt, das immer performt.
            durationIntervalVar = baseModel.newIntervalVar(
                    taskType.getStart(),
                    possibleDurations,
                    taskType.getEnd(),
                    "interval_" + taskIdentifier
            );

            // Mandatory Tasks müssen immer aktiv sein
            baseModel.addEquality(taskType.getActive(), baseModel.trueLiteral());
        }
        taskType.setInterval(durationIntervalVar);
        return taskType;
    }
}
