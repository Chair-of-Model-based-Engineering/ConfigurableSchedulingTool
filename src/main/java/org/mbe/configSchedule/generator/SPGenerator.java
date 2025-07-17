package org.mbe.configSchedule.generator;

import org.mbe.configSchedule.util.Machine;
import org.mbe.configSchedule.util.SchedulingProblem;
import org.mbe.configSchedule.util.Task;

import java.util.List;
import java.util.Random;

public abstract class SPGenerator {
    protected final Random random = new Random();

    /**
     * Creates the machines for the problem genreated by the Problem Generator
     *
     * @param machineCount The number of machines (to be created)
     * @param machines     List of machines
     */
    protected void createMachines(int machineCount, List<Machine> machines) {
        int machineID = 1;
        for (int i = 0; i < machineCount; i++) {
            Machine machine = new Machine("m" + machineID, false);
            machineID++;
            machines.add(machine);
        }
    }

    /**
     * Generate durations for the given task.
     *
     * <p>There is a 50% chance for generating multiple durations or only a single one.
     *
     * @param task                   the task for which to generate durations.
     * @param maxAmount              the maximum amount of durations.
     * @param minimum                the minimum duration.
     * @param variability            the variability of the durations on top of the minimum amount.
     * @param unboundDurationsChance the inverse chance to generate unbound durations.
     */
    protected void setRandomDurations(Task task, int maxAmount, int minimum, int variability, int unboundDurationsChance) {
        boolean variableDurationChance = this.random.nextBoolean();
        if (variableDurationChance) {
            int durationCount = this.random.nextInt(1, maxAmount + 1);
            for (int i = 0; i < durationCount; i++) {
                task.addDuration(this.random.nextInt(minimum, variability + 1));
            }
        } else {
            int duration = this.random.nextInt(minimum, variability + 1);
            task.setDurations(new int[] {duration});
        }

        if (unboundDurationsChance > 0 && this.random.nextInt(unboundDurationsChance) == 0) {
            int lowerBound = this.random.nextInt(minimum, variability + 1);
            task.setUnboundDurations(lowerBound);
        }
    }

    protected Task createRandomTask(String name, boolean optional, List<Machine> machines, boolean isOutlier) {
        Task task = new Task();
        task.setName(name);
        task.setMachine(machines.get(this.random.nextInt(machines.size())));

        if (isOutlier) {
            setRandomDurations(task, 4, 6, 10, 4);
        } else {
            setRandomDurations(task, 4, 1, 5, 0);
        }

        task.setOptional(optional);
        return task;
    }

    /**
     * Parses the given scheduling problem to the UVL-format
     *
     * @param sp SchedulingProblem-Object for which the UVL-file is to be created
     * @return String in the format of a UVL-file
     */
    protected abstract String parseToUVL(SchedulingProblem sp);

}
