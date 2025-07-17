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

    protected void setRandomDurations(Task task, int maxAmount, int minimum, int variability, List<Task> mandatoryTasksWithVarDuration) {
        // Should duration be variable?
        boolean variableDurationChance = this.random.nextBoolean();
        // 0 = Variable duration, 1 = static duration
        if (variableDurationChance) {
            int durationCount = this.random.nextInt(maxAmount) + 1;
            for (int i = 0; i < durationCount; i++) {
                task.addDuration(this.random.nextInt(variability) + minimum);
            }

            // Add tasks to mandatory tasks for the duration constraints
            mandatoryTasksWithVarDuration.add(task);
        } else {
            int duration = this.random.nextInt(variability) + minimum;
            task.setDurations(new int[] {duration});
        }
    }

    /**
     * Parses the given scheduling problem to the UVL-format
     *
     * @param sp SchedulingProblem-Object for which the UVL-file is to be created
     * @return String in the format of a UVL-file
     */
    protected abstract String parseToUVL(SchedulingProblem sp);

}
