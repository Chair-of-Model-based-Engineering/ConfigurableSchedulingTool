package org.mbe.configSchedule.parser;

import de.vill.main.UVLModelFactory;
import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import de.vill.model.Group;
import de.vill.model.constraint.*;
import org.mbe.configSchedule.util.Machine;
import org.mbe.configSchedule.util.Task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class UVLReader {

    private final FeatureModel featureModel;

    private final List<Machine> machines = new ArrayList<>();
    private final Map<String, Task> allTasks = new HashMap<>();

    /**
     * Constructs a new UVLReader for the given {@link FeatureModel}.
     *
     * @param featureModel the UVL feature model to read.
     */
    public UVLReader(FeatureModel featureModel) {
        this.featureModel = featureModel;
    }

    /**
     * Creates a feature model from the contents of the file provided by the path
     *
     * @param path File-path to the UVL-file
     * @return A FeatureModel-Object
     * @throws IOException
     */
    public static FeatureModel read(Path path) throws IOException {
        String content = new String(Files.readAllBytes(path));
        UVLModelFactory uvlModelFactory = new UVLModelFactory();
        return uvlModelFactory.parse(content);
    }

    /**
     * Writes the feature model to a file
     *
     * @param path         File-path to where the file should be written
     * @param featureModel The feature model to be written
     * @throws IOException
     */
    public static void write(String path, FeatureModel featureModel) throws IOException {
        String uvlModel = featureModel.toString();
        Path filePath = Paths.get(featureModel.getNamespace() + ".uvl");
        Files.write(filePath, uvlModel.getBytes());
    }

    /**
     * Parses the name from the feature model. The name is the value of the root feature.
     *
     * @return the feature model's name
     */
    public String parseName() {
        return this.featureModel.getRootFeature().getFeatureName();
    }

    /**
     * Parses the deadline from the feature model.
     *
     * <p>If the deadline cannot be parsed, because the value is malformed or the feature is missing,
     * the method returns {@code -1} representing a deadline of infinity.
     *
     * @return the deadline value or -1
     */
    public int parseDeadline() {
        int deadline;

        Optional<Feature> deadlineFeature = this.featureModel.getRootFeature().getChildren()
                .stream()
                .flatMap(c -> c.getFeatures().stream())
                .filter(c -> c.getFeatureName().contains("dl"))
                .findAny();
        if (deadlineFeature.isPresent()) {
            String deadlineFeatureName = deadlineFeature.get().getFeatureName();
            String[] deadlineParts = deadlineFeatureName.split(" = ");
            String rightSide = deadlineParts[deadlineParts.length - 1];
            if ("*".equals(rightSide)) {
                deadline = -1;
            } else {
                try {
                    deadline = Integer.parseInt(rightSide);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Could not determine deadline from feature '%s'".formatted(deadlineFeatureName), e);
                }
            }
        } else {
            throw new RuntimeException("Deadline feature missing");
        }
        return deadline;
    }

    /**
     * Parses the list of machines from the feature model
     *
     * @return The list of machines existing in the scheduling problem
     */
    public List<Machine> parseMachines() {
        // The machines might already have been parsed in a previous parsing step
        if (!this.machines.isEmpty())
            return this.machines;

        Optional<Feature> machineParent = this.featureModel.getRootFeature().getChildren()
                .stream()
                .flatMap(c -> c.getFeatures().stream())
                .filter(c -> c.getFeatureName().equalsIgnoreCase("M"))
                .findAny();
        if (machineParent.isEmpty()) {
            System.out.println("Machines could not be converted, check the model");
            return this.machines;
        }

        List<Group> machineGroups = machineParent.get().getChildren();
        List<Machine> parsedMachines = machineGroups
                .stream()
                .flatMap(c -> c.getFeatures().stream())
                .map(c -> new Machine(c.getFeatureName(), isFeatureOptional(c)))
                .toList();
        this.machines.addAll(parsedMachines);

        return this.machines;
    }

    public Collection<Task> parseTasks() {
        Optional<Feature> taskParent = this.featureModel.getRootFeature().getChildren()
                .stream()
                .flatMap(c -> c.getFeatures().stream())
                .filter(c -> c.getFeatureName().equalsIgnoreCase("P"))
                .findAny();
        if (taskParent.isEmpty()) {
            System.out.println("Tasks could not be converted, check the model");
            return new ArrayList<>();
        }

        // Parsing tasks requires the machines to be present
        if (this.machines.isEmpty()) {
            this.parseMachines();
        }

        List<Group> taskGroups = taskParent.get().getChildren();
        Map<String, Task> tasks = taskGroups
                .stream()
                .flatMap(c -> c.getFeatures().stream())
                .map(feature -> {
                    Task task = new Task();
                    task.setName(feature.getFeatureName());
                    task.setOptional(isFeatureOptional(feature));
                    setTaskDurations(feature, task);

                    return task;
                }).collect(Collectors.toMap(Task::getName, Function.identity()));

        List<Constraint> crossTreeConstraints = this.featureModel.getOwnConstraints();
        List<ImplicationConstraint> implicationConstraints = crossTreeConstraints
                .stream()
                .filter(c -> c instanceof ImplicationConstraint)
                .map(c -> (ImplicationConstraint) c)
                .toList();

        addMachines(new ArrayList<>(tasks.values()), implicationConstraints);
        addDurationConstraints(tasks, implicationConstraints);
        addExcludeConstraints(tasks, crossTreeConstraints);

        return tasks.values();
    }

    private void addMachines(List<Task> tasks, List<ImplicationConstraint> implicationConstraints) {
        List<String> machineNames = this.machines.stream().map(Machine::getName).toList();
        Map<String, String> machineAssignments = implicationConstraints.stream()
                .filter(c -> c.getLeft() instanceof LiteralConstraint && c.getRight() instanceof LiteralConstraint)
                .filter(c -> machineNames.contains(getLiteralString(c.getRight())))
                .collect(Collectors.toMap(
                        c -> getLiteralString(c.getLeft()),
                        c -> getLiteralString(c.getRight())
                ));

        for (Task task : tasks) {
            setTaskMachine(task, machineAssignments);
        }
    }

    private void addDurationConstraints(Map<String, Task> tasks, List<ImplicationConstraint> implicationConstraints) {
        List<ImplicationConstraint> durationConstraints = implicationConstraints.stream()
                .filter(c -> c.getLeft() instanceof LiteralConstraint && c.getRight() instanceof LiteralConstraint)
                .filter(c -> {
                    String leftString = getLiteralString(c.getLeft());
                    return leftString.startsWith("d") && leftString.contains(" = ");
                }).toList();

        for (ImplicationConstraint durationConstraint : durationConstraints) {
            String durationString = getLiteralString(durationConstraint.getLeft());
            int equalSignIndex = durationString.indexOf(" = ");
            String taskName = durationString.substring(1, equalSignIndex);
            int duration;
            try {
                duration = Integer.parseInt(durationString.substring(equalSignIndex + 3));
            } catch (NumberFormatException _) {
                System.err.printf("Could not parse the duration for constraint '%s'%n", durationConstraint);
                continue;
            }

            Task dependencyTask = tasks.get(getLiteralString(durationConstraint.getRight()));
            Task dependentTask = tasks.get(taskName);
            dependentTask.addTaskToDurationCon(duration, dependencyTask);
        }
    }

    private void addExcludeConstraints(Map<String, Task> tasks, List<Constraint> constraints) {
        List<Constraint> excludeConstraints = constraints.stream()
                .filter(c -> c.toString().contains("&") && c.toString().contains("|"))
                .toList();

        for (Constraint excludeConstraint : excludeConstraints) {
            Set<String> exclusionGroup = getVariables(excludeConstraint);
            for (String taskName : exclusionGroup) {
                List<String> excludedTasks = new ArrayList<>(exclusionGroup);
                excludedTasks.remove(taskName);
                tasks.get(taskName).setExcludeTasks(excludedTasks);
            }
        }
    }

    /**
     * Set the duration and unbound durations of a task
     *
     * @param feature The feature containing the duration
     * @param task    The Task-Object for which the duration should be set
     */
    private void setTaskDurations(Feature feature, Task task) {
        for (Group group : feature.getChildren()) {
            for (Feature childFeature : group.getFeatures()) {
                String childName = childFeature.getFeatureName();
                if (childName.startsWith("d")) {
                    if (childName.contains(" >= ")) {
                        int lowerBound = Integer.parseInt(childName.split(" >= ")[1]);
                        task.setUnboundDurations(lowerBound);
                    } else if (childName.contains(" = ")) {
                        int duration = Integer.parseInt(childName.split(" = ")[1]);
                        task.addDuration(duration);
                    }
                } else {
                    System.err.printf("Ignoring feature '%s' for task '%s'", childName, task.getName());
                }
            }
        }
    }

    /**
     * Set the machine of a task.
     *
     * @param task               The task to which to assign the machine.
     * @param machineAssignments The mapping of tasks to machines.
     */
    private void setTaskMachine(Task task, Map<String, String> machineAssignments) {
        // Find name of corresponding machine
        String machineName = machineAssignments.get(task.getName());
        if (machineName == null) {
            System.out.printf("Could not find a machine constraint for task %s.%n", task.getName());
            return;
        }

        // TODO: Could be optimized by adding a `Map<String, Machine> nameToMachine`
        // Find machine corresponding to machineName
        for (Machine m : this.machines) {
            if (machineName.equals(m.getName())) {
                task.setMachine(m);
                break;
            }
        }
    }

    /**
     * Get a set of the names of all variables used in the given constraint.
     *
     * @param constraint The constraint from which to extract the variables.
     * @return a set of all variables.
     */
    private Set<String> getVariables(Constraint constraint) {
        if (constraint instanceof LiteralConstraint) {
            return Set.of(getLiteralString(constraint));
        }

        Set<String> variables = new HashSet<>();
        for (Constraint part : constraint.getConstraintSubParts()) {
            variables.addAll(getVariables(part));
        }
        return variables;
    }

    private static String getLiteralString(Constraint c) {
        return ((LiteralConstraint) c).getLiteral();
    }

    private static boolean isFeatureOptional(Feature f) {
        return f.getParentGroup().GROUPTYPE == Group.GroupType.OPTIONAL;
    }
}
