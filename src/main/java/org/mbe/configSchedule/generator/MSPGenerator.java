package org.mbe.configSchedule.generator;

import org.mbe.configSchedule.util.Machine;
import org.mbe.configSchedule.util.SchedulingProblem;
import org.mbe.configSchedule.util.Task;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

public class MSPGenerator extends SPGenerator {
    /**
     * Generates a machine scheduling problem with the parameters
     *
     * @param taskCount            Number of mandatory tasks
     * @param durationOutlierCount Number of duration Outliers (tasks that take longer than usual)
     * @param machineCount         Number of machines
     * @param optionalCount        Number of optional tasks
     * @param altCount             Number of alternative tasks
     * @param altGroupCount        Number of alternative task groups
     * @param deadline             Deadline
     * @param durationConstraints  Number of duration constraints
     * @param maxDurationRequires  Maximum number of duration constraints originating from one task
     * @param name                 Name of the problem
     * @return the generated scheduling problem.
     */
    public SchedulingProblem generateProblem(int taskCount,
                                int durationOutlierCount, int machineCount, int optionalCount,
                                int altCount, int altGroupCount, int deadline, int durationConstraints, int maxDurationRequires, String name) {
        List<Task> tasks = new ArrayList<>();
        Map<Task, List<Task>> precedenceOrder = new HashMap<>();

        List<Machine> machines = createMachines(machineCount);
        List<Task> mandatoryTasksWithVarDuration = new ArrayList<>();
        createMandatoryTasks(taskCount, durationOutlierCount, machines, mandatoryTasksWithVarDuration, tasks, precedenceOrder);
        List<Task> optionalTasks = createOptionalTasks(optionalCount, machines);
        List<Task[]> alternativeTaskGroups = createAlternativeTasks(altCount, altGroupCount, machines);

        createDurationConstraints(
                Stream.concat(optionalTasks.stream().map(t -> new Task[] {t}), alternativeTaskGroups.stream()).toList(),
                mandatoryTasksWithVarDuration, durationConstraints, maxDurationRequires
        );

        tasks.addAll(optionalTasks);
        tasks.addAll(alternativeTaskGroups.stream().flatMap(Arrays::stream).distinct().toList());

        return new SchedulingProblem(name, tasks, precedenceOrder, machines, deadline);
    }

    private void createMandatoryTasks(int taskCount,
                                      int durationOutlierCount,
                                      List<Machine> machines,
                                      List<Task> mandatoryTasksWithVarDuration,
                                      List<Task> tasks,
                                      Map<Task, List<Task>> precedenceOrder
    ) {
        for (int i = 0; i < taskCount; i++) {
            // If there are equal to or more durationOutlier than tasks to be created,
            // then the task has to be an outlier
            boolean onlyOutliersLeft = (taskCount - i) <= durationOutlierCount;
            boolean outLierChance = this.random.nextBoolean();
            boolean isOutlier = durationOutlierCount > 0 && (onlyOutliersLeft || outLierChance);
            durationOutlierCount--;

            Task task = createRandomTask("p" + (i + 1), false, machines, isOutlier);
            if (task.hasUncertainDurations() && !task.hasUnboundDurations()) {
                mandatoryTasksWithVarDuration.add(task);
            }

            List<Task> dependencies = new ArrayList<>();
            if (!tasks.isEmpty()) {
                int dependencyCount = this.random.nextInt(Math.min(4, tasks.size()));
                for (int j = 0; j < dependencyCount; j++) {
                    dependencies.add(tasks.get(this.random.nextInt(tasks.size())));
                }
            }

            precedenceOrder.put(task, dependencies);

            tasks.add(task);
        }
    }
}