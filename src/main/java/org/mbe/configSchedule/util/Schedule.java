package org.mbe.configSchedule.util;

import java.util.*;

/**
 * A schedule of a scheduling problem.
 */
public class Schedule {
    private final Map<Machine, List<AssignedTask>> assignedJobs = new HashMap<>();
    private final double makespan;

    public Schedule(double makespan) {
        this.makespan = makespan;
    }

    /**
     * Adds the given task to the timeline of the given machine.
     *
     * @param assignedTask the task to insert.
     * @param machine      the machine to which to add the task.
     */
    public void addTaskToMachine(AssignedTask assignedTask, Machine machine) {
        this.assignedJobs.computeIfAbsent(machine, (Machine _) -> new ArrayList<>());
        List<AssignedTask> machineTasks = this.assignedJobs.get(machine);
        if (!machineTasks.isEmpty()) {
            int i = Collections.binarySearch(machineTasks, assignedTask);
            if (i < 0)
                i = -i - 1;
            machineTasks.add(i, assignedTask);
        } else {
            machineTasks.add(assignedTask);
        }
    }

    /**
     * Returns all machines active in this schedule.
     *
     * @return all active machines.
     */
    public Set<Machine> getActiveMachines() {
        return this.assignedJobs.keySet();
    }

    /**
     * Return the tasks which are executed on the given machine.
     *
     * @param machine the machine for which to get tasks.
     * @return the tasks which are executed on the given machine.
     */
    public List<AssignedTask> getTasks(Machine machine) {
        return this.assignedJobs.get(machine);
    }

    /**
     * Return all tasks which are executed.
     *
     * @return all tasks in this schedule.
     */
    public List<AssignedTask> getTasks() {
        return this.assignedJobs.values().stream().flatMap(Collection::stream).toList();
    }

    /**
     * Returns the makespan of the schedule.
     *
     * @return the makespan of the schedule.
     */
    public double getMakespan() {
        return makespan;
    }

    /**
     * Generates a textual diagram of the schedule.
     *
     * @return a printable string describing the schedule.
     */
    public String generateOutputString() {
        int longestMachineName = this.getActiveMachines().stream()
                .mapToInt(m -> m.getName().length())
                .max()
                .orElse(0);

        // Create per machine output lines.
        StringBuilder output = new StringBuilder();
        for (Machine machine : this.getActiveMachines()) {
            StringBuilder solLineTasks = new StringBuilder("Machine " + machine.getName() + ": ");
            solLineTasks.append(" ".repeat(Math.max(0, longestMachineName - machine.getName().length())));
            StringBuilder solLine = new StringBuilder();
            solLine.append(" ".repeat(longestMachineName + 10)); // 10 = length of "Machine_: "

            if (this.getTasks(machine) != null) {
                for (AssignedTask assignedTask : this.getTasks(machine)) {
                    //String name = "job_" + assignedTask.jobID + "_task_" + assignedTask.taskID;
                    String name = assignedTask.getName();
                    // Add spaces to output to align columns.
                    solLineTasks.append(String.format("%-15s", name));

                    String solTmp = "[" + assignedTask.getStart() + "," + (assignedTask.getStart() + assignedTask.getDuration()) + "]";
                    // Add spaces to output to align columns.
                    solLine.append(String.format("%-15s", solTmp));
                }
            }
            output.append(solLineTasks).append(System.lineSeparator());
            output.append(solLine).append(System.lineSeparator());

        }
        return output.toString();
    }
}
