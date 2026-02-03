package org.mbe.configSchedule.solver;

import com.google.ortools.sat.*;
import org.mbe.configSchedule.util.*;

import java.util.*;
import java.util.stream.Collectors;

public class ProblemSolver {

    private final BaseModel baseModel;
    private CpSolver makespanSolver;

    private SolverReturn result;

    public ProblemSolver(BaseModel baseModel) {
        this.baseModel = baseModel;
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

    public void findOptimalSolution(Set<ScheduleStructure> structureSets) {
        this.makespanSolver = new CpSolver();
        createSolverReturn(this.makespanSolver, structureSets);
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
        CpModel makespanModel = this.baseModel.getModel().getClone();
        makespanModel.getBuilder().setName("Makespan");
        makespanModel.minimize(this.baseModel.getMakespanVar());

        CpSolverStatus status = solver.solve(makespanModel);
        Schedule schedule;
        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
            schedule = createSchedule(this.baseModel, solver);
        } else {
            schedule = null;
        }
        this.result = new SolverReturn(solver.userTime(), status, schedule);
    }

    public static Schedule createSchedule(BaseModel baseModel, CpSolver solver) {
        Schedule schedule = new Schedule(solver.objectiveValue());

        for (TaskType taskType : baseModel.getAllTaskTypes().values()) {
            AssignedTask assignedTask = new AssignedTask(
                    (int) solver.value(taskType.getStart()),
                    (int) solver.value(taskType.getInterval().getSizeExpr()),
                    taskType.getTask());

            // Add task to machine's task list, if it is executed
            if (!taskType.getTask().isOptional() || solver.value(taskType.getActive()) == 1) {
                schedule.addTaskToMachine(assignedTask, taskType.getTask().getMachine());
            }
        }
        return schedule;
    }

    private void createSolverReturn(CpSolver solver, Set<ScheduleStructure> structureSet) {
        CpModel makespanModel = this.baseModel.getModel().getClone();
        makespanModel.getBuilder().setName("Makespan");
        makespanModel.minimize(this.baseModel.getMakespanVar());

        //Check known structures
        for(ScheduleStructure structure : structureSet)
        {
            CpModel clone = makespanModel.getClone();
            addStructureConstraints(clone,structure);
            CpSolverStatus status = solver.solve(clone); //check with extra constrains from previous configurations
            if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE)
            {
                Schedule schedule = createSchedule(this.baseModel, solver);
                this.result = new SolverReturn(solver.userTime(), status, schedule, structure);
                return;
            }
        }
        //No existing valid structures
        CpSolverStatus status = solver.solve(makespanModel);
        Schedule schedule = createSchedule(this.baseModel, solver);
        ScheduleStructure newStructure = new ScheduleStructure(schedule);
        this.result = new SolverReturn(solver.userTime(), status, schedule, newStructure);
    }

    /**
     * Adds the current structure to the model.
     *
     * @param model the model of the configuration
     * @param structure the structure which is added
     *
     */
    private void addStructureConstraints(CpModel model, ScheduleStructure structure)
    {
        Map<String,TaskType> taskByType = this.baseModel.getAllTaskTypes().values().stream().collect(Collectors.toMap(task -> task.getTask().getName(),type -> type));

        for (TaskType type : baseModel.getAllTaskTypes().values())
        {
            boolean inStructure = structure.getMachines().stream().anyMatch(machine -> structure.getTaskOrder(machine).stream().anyMatch(task -> task.getName().equals(type.getTask().getName())));
            model.addEquality(type.getActive(), inStructure ? 1 : 0);
        }

        for (Machine machine : structure.getMachines()) {
            List<Task> orderedTasks = structure.getTaskOrder(machine);

            for (int i = 0; i < orderedTasks.size() - 1; i++) {
                TaskType firstType = taskByType.get(orderedTasks.get(i).getName());
                TaskType secondType = taskByType.get(orderedTasks.get(i + 1).getName());

                if (firstType == null || secondType == null)
                {
                    continue;
                }

                model.addGreaterOrEqual(secondType.getStart(), firstType.getEnd());
            }
        }
    }

    /**
     * Adds the current structure to the model to check if its added still leads to a valid schedule.
     *
     * @param structure the structure for which to check if it covers the configuration/model
     *
     */
    public boolean isStructureFeasible(ScheduleStructure structure)
    {
        CpSolver solver = new CpSolver();
        CpModel clone = this.baseModel.getModel().getClone();
        addStructureConstraints(clone,structure);
        CpSolverStatus status = solver.solve(clone);
        return status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE;
    }
}
