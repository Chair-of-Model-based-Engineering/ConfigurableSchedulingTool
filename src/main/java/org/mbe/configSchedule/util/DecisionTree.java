package org.mbe.configSchedule.util;

import java.util.*;
import java.util.stream.Collectors;

public class DecisionTree {
    /**
     * Represents a schedule without times and only the order of tasks
     *
     * @see Schedule
     */
    public static class SimpleSchedule {
        private final Map<Machine, List<Task>> taskOrder;

        private SimpleSchedule(Map<Machine, List<Task>> taskOrder) {
            this.taskOrder = taskOrder;
        }

        static SimpleSchedule fromSchedule(Schedule schedule) {
            HashMap<Machine, List<Task>> taskOrder = new HashMap<>();
            for (Machine machine : schedule.getActiveMachines()) {
                taskOrder.put(machine, schedule.getTasks(machine).stream().map(AssignedTask::getTask).toList());
            }
            return new SimpleSchedule(taskOrder);
        }

        public Map<Machine, List<Task>> getTaskOrder() {
            return taskOrder;
        }

        public boolean equals(SimpleSchedule other) {
            return this.taskOrder.equals(other.taskOrder);
        }

        public String outputString() {
            StringBuilder output = new StringBuilder();
            for (Machine machine : this.taskOrder.keySet()) {
                output.append(machine.getName()).append(": ");
                output.append(this.taskOrder.get(machine).stream().map(Task::getName).collect(Collectors.joining(", ")));
                output.append(System.lineSeparator());
            }
            return output.toString();
        }
    }

    public static class TaskDecisions {
        private final Task task;
        private final List<Integer> decisionDurations = new ArrayList<>();
        // For nextSchedules and nextDecisions:
        // Each entry at index i corresponds to the interval of durations in decisionDurations i-1 to i
        private final List<SimpleSchedule> nextSchedules = new ArrayList<>();
        private final List<TaskDecisions> nextDecisions = new ArrayList<>();

        public TaskDecisions(Task task) {
            this.task = task;
        }

        public void addDecision(int decisionDuration, Schedule leftSchedule) {
            int i = Collections.binarySearch(this.decisionDurations, decisionDuration);
            if (i < 0)
                i = -i - 1;

            SimpleSchedule simpleSchedule = SimpleSchedule.fromSchedule(leftSchedule);

            if (!this.nextSchedules.isEmpty() && this.nextSchedules.get(i - 1).equals(simpleSchedule)) {
                this.decisionDurations.set(i - 1, decisionDuration);
            } else {

                this.decisionDurations.add(i , decisionDuration);
                this.nextSchedules.add(i, SimpleSchedule.fromSchedule(leftSchedule));
            }
        }
    }
}
