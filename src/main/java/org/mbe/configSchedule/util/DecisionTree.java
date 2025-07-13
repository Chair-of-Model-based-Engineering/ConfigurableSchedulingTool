package org.mbe.configSchedule.util;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public record DecisionTree(double time, DecisionTree.TaskDecisions root) {
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

        public String outputString(String machineSep) {
            List<StringBuilder> output = new ArrayList<>();
            for (Machine machine : this.taskOrder.keySet()) {
                StringBuilder machineString = new StringBuilder();
                machineString.append(machine.getName()).append(": ");
                machineString.append(this.taskOrder.get(machine).stream().map(Task::getName).collect(Collectors.joining(", ")));
                output.add(machineString);
            }
            return String.join(machineSep, output);
        }

        public String outputString() {
            return outputString(System.lineSeparator());
        }
    }

    public static class TaskDecisions {
        private final Task task;
        private final List<Integer> decisionDurations = new ArrayList<>();
        // Each entry at index i corresponds to the interval of durations in decisionDurations i-1 to i
        private final List<SimpleSchedule> schedules = new ArrayList<>();

        private final Map<Integer, TaskDecisions> nextDecision = new HashMap<>();

        public TaskDecisions(Task task) {
            this.task = task;
        }

        public void addDecision(int decisionDuration, Schedule leftSchedule) {
            addDecision(
                    decisionDuration,
                    SimpleSchedule.fromSchedule(leftSchedule),
                    (i, simpleSchedule) -> SimpleSchedule.fromSchedule(leftSchedule).equals(simpleSchedule)
            );
        }

        public void addInfeasibleDecision(int decisionDuration) {
            addDecision(
                    decisionDuration,
                    null,
                    (i, _) -> this.schedules.get(i - 1) == null
            );
        }

        private void addDecision(int decisionDuration, SimpleSchedule simpleSchedule, BiPredicate<Integer, SimpleSchedule> shouldBeMerged) {
            int i = getDurationIndex(decisionDuration);
            if (!this.schedules.isEmpty() && shouldBeMerged.test(i, simpleSchedule)) {
                this.decisionDurations.set(i - 1, decisionDuration);
            } else {
                this.decisionDurations.add(i, decisionDuration);
                this.schedules.add(i, simpleSchedule);
            }
        }

        public void addNextLevel(int decisionDuration, TaskDecisions nextLevel) {
            this.nextDecision.put(decisionDuration, nextLevel);
        }

        public Task getTask() {
            return task;
        }

        public List<Integer> getDecisionDurations() {
            return decisionDurations;
        }

        public SimpleSchedule getScheduleForDuration(int duration) {
            return this.schedules.get(getDurationIndex(duration));
        }

        public TaskDecisions getNextLevelForDuration(int duration) {
            return this.nextDecision.get(duration);
        }

        public Collection<TaskDecisions> getNextLevels() {
            return this.nextDecision.values();
        }

        private int getDurationIndex(int duration) {
            int i = Collections.binarySearch(this.decisionDurations, duration);
            if (i < 0)
                i = -i - 1;
            return i;
        }

        String print(int indent) {
            StringBuilder out = new StringBuilder();
            String indention = " ".repeat(indent * 4);
            for (Integer decisionDuration : this.nextDecision.keySet()) {
                String scheduleString;
                try {
                    scheduleString = getScheduleForDuration(decisionDuration).outputString("; ");
                } catch (NullPointerException e) {
                    scheduleString = "infeasible";
                }
                out.append("%s- %d: %s (%s)%n".formatted(
                        indention,
                        decisionDuration,
                        getNextLevelForDuration(decisionDuration).getTask().getName(),
                        scheduleString
                ));
                out.append(getNextLevelForDuration(decisionDuration).print(indent + 1));
            }
            if (this.nextDecision.isEmpty()) {
                for (int i = 0; i < this.decisionDurations.size(); i++) {
                    SimpleSchedule schedule = this.schedules.get(i);
                    out.append("%s- %d: %s%n".formatted(
                            indention,
                            this.decisionDurations.get(i),
                            schedule != null ? schedule.outputString("; ") : "infeasible"
                    ));
                }
            }
            return out.toString();
        }
    }

    public void print() {
        System.out.println(this.root.getTask().getName());
        System.out.println(this.root.print(0));
    }
}
