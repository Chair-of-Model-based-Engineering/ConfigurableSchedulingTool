package org.mbe.configSchedule.util;

import de.vill.model.FeatureModel;
import org.mbe.configSchedule.parser.UVLReader;

import java.util.*;
import javax.annotation.Nonnull;

public class SchedulingProblem {
    /**
     * A class representing an exclusion constraint inside a {@link SchedulingProblem}.
     *
     * <p>The constraint represents the Boolean formula {@code !(A & B & C & ... & N)} with {@code A = polarites[i] ? elements[i] : !elements[i]}.
     *
     * @param elements   List of elements in the constraint.
     * @param polarities List of the elements' polarities.
     */
    public record ExclusionConstraint(SpElement[] elements, boolean[] polarities) {
        /**
         * Shortcut constructor for binary constraints of the form {@code !(A & B)}.
         *
         * @param a         The first element in the constraint.
         * @param aPolarity The polarization of {@code a}.
         * @param b         The second element in the constraint.
         * @param bPolarity The polarization of {@code b}.
         */
        public ExclusionConstraint(SpElement a, boolean aPolarity, SpElement b, boolean bPolarity) {
            this(new SpElement[] {a, b}, new boolean[] {aPolarity, bPolarity});
        }

        @Override
        @Nonnull
        public String toString() {
            String[] literals = new String[this.elements.length];
            for (int i = 0; i < this.elements.length; i++) {
                SpElement element = this.elements[i];
                boolean polarity = this.polarities[i];
                literals[i] = (polarity ? "" : "!") + element.getName();
            }

            return "!(" + String.join(" & ", literals) + ")";
        }
    }

    private final String name;
    private final List<Machine> machines;
    private final Collection<Task> tasks;
    private final Map<Task, List<Task>> precedenceOrder;
    private final Set<ExclusionConstraint> exclusionConstraints;
    private final int deadline;

    /**
     * Creates new object of type SchedulingProblem.
     *
     * @param name            the name/identifier of the problem.
     * @param tasks           the tasks contained in the problem.
     * @param precedenceOrder the precedence order between tasks.
     * @param machines        a {@link List} of {@link Machine Machines}.
     * @param deadline        deadline of the scheduling problem.
     */
    public SchedulingProblem(String name, Collection<Task> tasks, Map<Task, List<Task>> precedenceOrder, List<Machine> machines, int deadline) {
        this(name, tasks, precedenceOrder, machines, deadline, Set.of());
    }

    /**
     * Creates new object of type SchedulingProblem.
     *
     * @param name            the name/identifier of the problem.
     * @param tasks           the tasks contained in the problem.
     * @param precedenceOrder the precedence order between tasks.
     * @param machines        a {@link List} of {@link Machine Machines}.
     * @param deadline        deadline of the scheduling problem.
     */
    public SchedulingProblem(String name, Collection<Task> tasks, Map<Task, List<Task>> precedenceOrder, List<Machine> machines, int deadline, Set<ExclusionConstraint> exclusionConstraints) {
        this.name = name;
        this.tasks = tasks;
        this.precedenceOrder = precedenceOrder;
        this.machines = machines;
        this.deadline = deadline;
        this.exclusionConstraints = exclusionConstraints;
    }

    /**
     * Copies a given {@code SchedulingProblem} with additional exclusion constraints.
     *
     * @param sp                   the scheduling problem to copy.
     * @param exclusionConstraints the additional exclusion constraints.
     * @see ExclusionConstraint
     */
    public SchedulingProblem(SchedulingProblem sp, Set<ExclusionConstraint> exclusionConstraints) {
        this(sp.name, sp.tasks, sp.precedenceOrder, sp.machines, sp.deadline, exclusionConstraints);
    }

    /**
     * Factory method for creating a SchedulingProblem from a {@link FeatureModel}.
     *
     * @param featureModel the feature model to parse.
     * @return the scheduling problem as described by the feature model.
     */
    public static SchedulingProblem fromFeatureModel(FeatureModel featureModel) {
        UVLReader uvlReader = new UVLReader(featureModel);
        String name = uvlReader.parseName();
        int deadline = uvlReader.parseDeadline();
        Collection<Task> tasks = uvlReader.parseTasks();
        Map<Task, List<Task>> precedenceOrder = uvlReader.parsePrecedenceOrder();
        List<Machine> machines = uvlReader.parseMachines();

        return new SchedulingProblem(name, tasks, precedenceOrder, machines, deadline);
    }

    /**
     * Get machines of scheduling problem.
     *
     * @return {@link List} of {@link Machine Machines}.
     */
    public List<Machine> getMachines() {
        return machines;
    }

    /**
     * Get deadline of scheduling problem.
     *
     * <p>If the deadline is at infinity, this method returns {@code -1}.
     * Only non-negative numbers (>=0) should be interpreted as actual deadlines.
     *
     * @return deadline as an integer.
     */
    public int getDeadline() {
        return deadline;
    }

    /**
     * Get the name of the scheduling problem.
     *
     * @return the scheduling problem's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the precedence order between the tasks.
     *
     * <p>The key is the dependent task and the value is the list of dependencies.
     *
     * @return the precedence order between the tasks.
     * @see #getTasks()
     */
    public Map<Task, List<Task>> getPrecedenceOrder() {
        return precedenceOrder;
    }

    /**
     * {@return all tasks of the scheduling problem}
     *
     * @see #getPrecedenceOrder()
     */
    public Collection<Task> getTasks() {
        return tasks;
    }

    /**
     * {@return all exclude edges}
     */
    public Set<ExclusionConstraint> getExclusionConstraints() {
        return exclusionConstraints;
    }
}
