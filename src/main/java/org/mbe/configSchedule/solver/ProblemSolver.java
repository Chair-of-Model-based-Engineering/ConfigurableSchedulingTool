package org.mbe.configSchedule.solver;

import com.google.ortools.sat.*;
import org.mbe.configSchedule.util.*;

import java.util.*;

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
            addStructureConstrains(clone,structure);
            CpSolverStatus status = solver.solve(clone); //check with extra constrains from previous configurations
            if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE)
            {
                Schedule schedule = createSchedule(this.baseModel, solver);
                if(structure.sameStructure(new ScheduleStructure(schedule)))
                {
                    this.result = new SolverReturn(solver.userTime(), status, schedule, structure);
                    return;
                }
            }
        }
        //No existing valid structures
        CpSolverStatus status = solver.solve(makespanModel);
        Schedule schedule = createSchedule(this.baseModel, solver);
        ScheduleStructure newStructure = new ScheduleStructure(schedule);
        this.result = new SolverReturn(solver.userTime(), status, schedule, newStructure);
    }

    private void addStructureConstrains(CpModel model, ScheduleStructure structure)
    {
        for(Machine machine : structure.getMachines())
        {
            List<Task> orderedTasks = structure.getTaskOrder(machine);

            for(int i = 0; i < orderedTasks.size() - 1; i++)
            {
                Task firstTask = orderedTasks.get(i);
                Task secondTask = orderedTasks.get(i + 1);

                TaskType firstType = null;
                TaskType secondType = null;

                for (TaskType type : this.baseModel.getAllTaskTypes().values()) {
                    if (type.getName().equals(firstTask.getName())) {
                        firstType = type;
                    }
                    if (type.getName().equals(secondTask.getName())) {
                        secondType = type;
                    }
                }

                if(firstType == null || secondType == null)
                {
                    // TODO: One of the getName()-calls definitely throws a NullPointerException before this one can be thrown
                    throw new NullPointerException("TaskType is null for either/both: " + firstType.getName() + "or/and " + secondType.getName());
                }

                IntVar endFirst = firstType.getEnd();
                IntVar secondStart = secondType.getStart();

                model.addGreaterOrEqual(secondStart,endFirst);
            }
        }
    }
}
