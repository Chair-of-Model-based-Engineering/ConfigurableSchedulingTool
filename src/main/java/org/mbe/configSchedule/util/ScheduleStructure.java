package org.mbe.configSchedule.util;
import java.util.*;

/**
 * The structure of a given schedule
 */
public class ScheduleStructure {

    private final Map<Machine,List<Task>> structure = new HashMap<>();

    public ScheduleStructure(Schedule schedule)
    {
        for (Machine machine : schedule.getActiveMachines()) {
            List<Task> taskOrder = schedule.getTasks(machine).stream()
                    .sorted(Comparator.comparingInt(AssignedTask::getStart))
                    .map(AssignedTask::getTask)
                    .toList();
            structure.put(machine, taskOrder);
        }
    }

    /**
     * Return the order of tasks for a specific machine
     *
     * @param machine the machine for which to get the order of tasks.
     * @return list which contains task order for given machine.
     */
    public List<Task> getTaskOrder(Machine machine)
    {
        if(this.structure.containsKey(machine))
        {
            return Collections.unmodifiableList(this.structure.get(machine));
        }
        return List.of();
    }

    /**
     * Return all machines of the structure
     *
     * @return a set of machines
     */
    public Set<Machine> getMachines()
    {
        return Collections.unmodifiableSet(this.structure.keySet());
    }

    /**
     * Generates a textual diagram of the structure of a given schedule.
     *
     * @return a printable string describing the structure.
     */
    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();

        output.append("--------------------------------------").append(System.lineSeparator());
        for (Machine machine : this.getMachines()) {
            output.append(machine.getName()).append(": ");

            List<Task> tasks = this.structure.get(machine);
            for (int i = 0; i < tasks.size(); i++) {
                output.append(tasks.get(i).getName());
                if (i < tasks.size() - 1) {
                    output.append(" -> ");
                }
            }
            output.append(System.lineSeparator());
        }
        output.append("--------------------------------------").append(System.lineSeparator());
        return output.toString();
    }
}
