package org.mbe.configSchedule.util;

/**
 * A class representing an element of a scheduling problem.
 */
public abstract class SpElement {
    /**
     * A class representing a specific duration of a {@link Task}.
     */
    static public class TaskDuration extends SpElement {
        private final Task task;
        private final int duration;

        public TaskDuration(Task task, int duration) {
            this.task = task;
            this.duration = duration;
        }

        public Task getTask() {
            return task;
        }

        public int getDuration() {
            return duration;
        }

        /**
         * @return The string for a task's duration feature.
         * @inheritDoc
         */
        @Override
        public String getName() {
            return String.format("\"d%s = %d\"", this.task.getName(), this.duration);
        }

        @Override
        public String toString() {
            return String.format("d(%s) = %d", this.task.getName(), this.duration);
        }
    }

    /**
     * Returns the name of the element in the configurable scheduling problem as present in the feature model.
     *
     * @return the feature's name.
     */
    public abstract String getName();
}
