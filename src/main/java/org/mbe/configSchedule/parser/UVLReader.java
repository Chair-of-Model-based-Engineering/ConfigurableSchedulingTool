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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class UVLReader {

    static List<Task> allTasks = new ArrayList<>();

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
     * @param featureModel The feature model from which the name should be retrieved.
     * @return the feature model's name
     */
    public static String parseName(FeatureModel featureModel) {
        return featureModel.getRootFeature().getFeatureName();
    }

    /**
     * Parses the deadline from the feature model.
     *
     * <p>If the deadline cannot be parsed, because the value is malformed or the feature is missing,
     * the method returns {@code -1} representing a deadline of infinity.
     *
     * @param featureModel The feature model from which the deadline should be retrieved
     * @return the deadline value or -1
     */
    public static int parseDeadline(FeatureModel featureModel) {
        int deadline;

        Optional<Feature> deadlineFeature = featureModel.getRootFeature().getChildren()
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
     * @param featureModel The feature model representing the scheduling problem
     * @return The list of machines existing in the scheduling problem
     */
    public static List<Machine> parseMachines(FeatureModel featureModel) {
        List<Machine> machines = new ArrayList<>();
        Optional<Feature> machineParent = featureModel.getRootFeature().getChildren()
                .stream()
                .flatMap(c -> c.getFeatures().stream())
                .filter(c -> c.getFeatureName().equalsIgnoreCase("M"))
                .findAny();
        if (machineParent.isEmpty()) {
            System.out.println("Machines could not be converted, check the model");
            return machines;
        }
        List<Group> machineGroups = machineParent.get().getChildren();
        Stream<Feature> mandatoryMachines = machineGroups
                .stream()
                .filter(c -> c.GROUPTYPE == Group.GroupType.MANDATORY)
                .flatMap(c -> c.getFeatures().stream());
        machines.addAll(mandatoryMachines
                .map(c -> new Machine(c.getFeatureName(), false))
                .toList());

        Stream<Feature> optionalMachines = machineGroups
                .stream()
                .filter(c -> c.GROUPTYPE == Group.GroupType.OPTIONAL)
                .flatMap(c -> c.getFeatures().stream());
        machines.addAll(optionalMachines
                .map(c -> new Machine(c.getFeatureName(), true))
                .toList());

        return machines;
    }

    /**
     * Parses the jobs and tasks from the feature model
     *
     * @param featureModel The feature model repreenting the scheduling problem
     * @param machines     The list of machines existing in the scheduling problem
     * @return The tasks, divided into jobs, existing in the scheduling problem
     */
    public static List<List<Task>> parseJobs(FeatureModel featureModel, List<Machine> machines) {
        List<List<Task>> jobs = new ArrayList<>();
        Optional<Feature> taskParent = featureModel.getRootFeature().getChildren()
                .stream()
                .flatMap(c -> c.getFeatures().stream())
                .filter(c -> c.getFeatureName().equalsIgnoreCase("P"))
                .findAny();
        if (taskParent.isEmpty()) {
            System.out.println("Tasks could not be converted, check the model");
            return jobs;
        }

        List<Group> taskGroups = taskParent.get().getChildren();
        List<Feature> mandatoryTasks = taskGroups
                .stream()
                .filter(c -> c.GROUPTYPE == Group.GroupType.MANDATORY)
                .flatMap(c -> c.getFeatures().stream())
                .toList();
        List<Feature> optionalTasks = taskGroups
                .stream()
                .filter(c -> c.GROUPTYPE == Group.GroupType.OPTIONAL)
                .flatMap(c -> c.getFeatures().stream())
                .toList();

        List<Constraint> crossTreeConstraints = featureModel.getOwnConstraints();

        List<ImplicationConstraint> implicationConstraints = crossTreeConstraints
                .stream()
                .filter(c -> c instanceof ImplicationConstraint)
                .map(c -> (ImplicationConstraint) c)
                .toList();

        List<String> machineNames = machines.stream().map(Machine::getName).toList();
        List<ImplicationConstraint> machineConstraints = implicationConstraints.stream()
                .filter(c -> c.getRight() instanceof LiteralConstraint && c.getLeft() instanceof LiteralConstraint)
                .filter(c -> machineNames.contains(((LiteralConstraint) c.getRight()).getLiteral()))
                .toList();
        Map<String, String> machineAssignments = machineConstraintsToString(machineConstraints);

        Set<String> taskNames = Stream.concat(mandatoryTasks.stream(), optionalTasks.stream())
                .map(Feature::getFeatureName)
                .collect(Collectors.toUnmodifiableSet());
        List<ImplicationConstraint> orderConstraints = implicationConstraints.stream()
                .filter(c -> {
                    String leftTaskName = ((LiteralConstraint) c.getLeft()).getLiteral();
                    String rightTaskName = ((LiteralConstraint) c.getRight()).getLiteral();
                    return taskNames.containsAll(List.of(leftTaskName, rightTaskName));
                })
                // This collector is needed because `Stream.toList()` returns an immutable list.
                .collect(Collectors.toCollection(ArrayList::new));

        List<ImplicationConstraint> durationConstraints = implicationConstraints.stream()
                .filter(c -> c.getLeft().toString().startsWith("\"d")
                        && c.getLeft().toString().contains(" = "))
                .toList();

        List<Constraint> excludeConstraints = crossTreeConstraints
                .stream()
                .filter(c -> c.toString().contains(" & "))
                .toList();

        // First parse mandatory jobs, then add optional jobs
        jobs = parseMandatoryJobs(orderConstraints, mandatoryTasks, machines, machineAssignments);
        List<List<Task>> optionalJobs = parseOptionalJobs(optionalTasks, machineAssignments, machines);
        jobs.addAll(optionalJobs);

        // Parse the constraints for excluding tasks and duration constraints
        List<String[]> excludeConstraintStrings = excludeConstraintsToString(excludeConstraints);
        Map<String, Map<Integer, List<String>>> TaskToDurationToRequiredTask = durationConstraintsToString(durationConstraints);

        // complete the alternative groups
        for (List<Task> job : jobs) {
            for (Task task : job) {
                // Add the excluded tasks of the alternative group to the excludeTask-List
                // Only optional tasks can exclude other optional tasks
                if (task.isOptional()) {
                    // Iterate over every exclude constraint
                    for (String[] constraint : excludeConstraintStrings) {
                        // Boolean for if the task is in the exclude group
                        // Only if the boolean is true the exclude group gets added to the task,
                        // otherwise its discarded
                        boolean containsTask = false;
                        List<String> excludedTasks = new ArrayList<>();
                        for (String p : constraint) {
                            if (p.equals(task.getName())) {
                                containsTask = true;
                            } else {
                                excludedTasks.add(p);
                            }
                        }

                        if (containsTask) {
                            task.getExcludeTasks().addAll(excludedTasks);
                        }
                    }
                }

                // Add duration constraints
                if (TaskToDurationToRequiredTask.containsKey(task.getName())) {
                    Map<Integer, List<String>> entry = TaskToDurationToRequiredTask.get(task.getName());
                    for (Integer duration : entry.keySet()) {
                        List<Task> requiredTasks = allTasks
                                .stream()
                                .filter(p -> entry.get(duration).contains(p.getName()))
                                .collect(Collectors.toCollection(ArrayList::new));
                        task.getDurationCons().put(duration, requiredTasks);
                    }
                }
            }
        }

        return jobs;
    }

    /**
     * Parses the mandatory jobs/ tasks. Also orders them in the correct order
     *
     * @param orderConstraints   List containing the task-order-constraints
     * @param mandatoryTasks     List containing the features for the mandatory tasks
     * @param machines           List containing all machines.
     * @param machineAssignments Map of assignments of machines to tasks.
     * @return The mandatory tasks, divided into their jobs
     */
    private static List<List<Task>> parseMandatoryJobs(
            List<ImplicationConstraint> orderConstraints,
            List<Feature> mandatoryTasks,
            List<Machine> machines,
            Map<String, String> machineAssignments
    ) {
        Set<String> dependentTasks = orderConstraints.stream()
                .map(c -> ((LiteralConstraint) c.getLeft()).getLiteral())
                .collect(Collectors.toUnmodifiableSet());
        Set<String> starterTasks = mandatoryTasks.stream()
                .map(Feature::getFeatureName)
                .filter(t -> !dependentTasks.contains(t))
                .collect(Collectors.toSet());

        List<List<Task>> jobs = new ArrayList<>();

        for (String starterTask : starterTasks) {
            List<Task> job = new ArrayList<>();
            Task task = new Task();
            task.setName(starterTask);

            job.add(task);
            allTasks.add(task);

            for (Feature feature : mandatoryTasks) {
                if (task.getName().equals(feature.getFeatureName())) {
                    setTaskDuration(feature, task);
                    break;
                }
            }
            setTaskMachine(task, machines, machineAssignments);

            String currentTaskName = task.getName();
            int i = 0;
            while (i < orderConstraints.size()) {
                ImplicationConstraint orderPair = orderConstraints.get(i);
                LiteralConstraint lLiteral = (LiteralConstraint) orderPair.getLeft();
                LiteralConstraint rLiteral = (LiteralConstraint) orderPair.getRight();
                String l = lLiteral.getLiteral();
                String r = rLiteral.getLiteral();
                if (r.equals(currentTaskName)) {
                    Task followTask = new Task();
                    followTask.setName(l);

                    for (Feature feature : mandatoryTasks) {
                        if (followTask.getName().equals(feature.getFeatureName())) {
                            setTaskDuration(feature, followTask);
                            break;
                        }
                    }
                    setTaskMachine(followTask, machines, machineAssignments);

                    job.add(followTask);
                    allTasks.add(followTask);

                    currentTaskName = followTask.getName();
                    orderConstraints.remove(orderPair);
                    i = 0;
                } else {
                    i++;
                }
            }
            jobs.add(job);
        }

        return jobs;
    }

    /**
     * Parses the optional jobs/ tasks
     *
     * @param optionalTasks      List containing the features for the optional tasks
     * @param machineAssignments Assignment of machines to tasks.
     * @param machines           List of machines
     * @return The optional tasks, each in their own job
     */
    private static List<List<Task>> parseOptionalJobs(List<Feature> optionalTasks, Map<String, String> machineAssignments, List<Machine> machines) {
        List<List<Task>> jobs = new ArrayList<>();

        for (Feature feature : optionalTasks) {
            List<Task> job = new ArrayList<>();

            // Create Task and set name
            String name = feature.getFeatureName();
            Task task = new Task();
            task.setName(name);
            task.setOptional(true);
            setTaskDuration(feature, task);
            setTaskMachine(task, machines, machineAssignments);

            // Set machine of task
            Optional<String> machineName = Optional.ofNullable(machineAssignments.get(name));
            Optional<Machine> machine = machines.stream()
                    .filter(m -> machineName.map(cm -> cm.equals(m.getName())).orElse(false))
                    .findFirst();

            if (machine.isPresent()) {
                task.setMachine(machine.get());
            } else {
                System.out.printf("Could not find machine for %s%n", name);
            }

            job.add(task);
            allTasks.add(task);
            jobs.add(job);
        }

        return jobs;
    }

    /**
     * Parses the machine constraints to a {@link Map} of tasks to machines (p->m).
     *
     * @param machineConstraints List of machine constraints
     * @return A Map containing the machine-constraint-pairs
     */
    private static Map<String, String> machineConstraintsToString(List<ImplicationConstraint> machineConstraints) {
        Map<String, String> taskMachineMap = new HashMap<>();

        for (ImplicationConstraint constraint : machineConstraints) {
            LiteralConstraint p = (LiteralConstraint) constraint.getLeft();
            LiteralConstraint m = (LiteralConstraint) constraint.getRight();

            String pString = p.getLiteral();
            String mString = m.getLiteral();

            taskMachineMap.put(pString, mString);
        }

        return taskMachineMap;
    }

    /**
     * Parses the exclude constraints to a List of String-arrays, so that each array contains the names
     * of the tasks of the group, e.g. [p1,p2,p3]
     *
     * @param excludeConstraints List of exclude constraints
     * @return A List containing the exclude constraints as String-arrays
     */
    private static List<String[]> excludeConstraintsToString(List<Constraint> excludeConstraints) {
        List<String[]> constraintStrings = new ArrayList<>();

        // pa1 & !pa2 & !pa3 | !pa1 & pa2 & pa3 | !pa1 & !pa2 & pa3
        for (Constraint constraint : excludeConstraints) {
            constraintStrings.add(getVariables(constraint).toArray(new String[0]));
        }

        return constraintStrings;
    }

    /**
     * Get a set of the names of all variables used in the given constraint.
     *
     * @param constraint The constraint from which to extract the variables.
     * @return a set of all variables.
     */
    private static Set<String> getVariables(Constraint constraint) {
        if (constraint instanceof LiteralConstraint) {
            return Set.of(((LiteralConstraint) constraint).getLiteral());
        }

        Set<String> variables = new HashSet<>();
        for (Constraint part : constraint.getConstraintSubParts()) {
            variables.addAll(getVariables(part));
        }
        return variables;
    }

    /**
     * Set the duration and unbound durations of a task
     *
     * @param feature The feature containing the duration
     * @param task    The Task-Object for which the duration should be set
     */
    private static void setTaskDuration(Feature feature, Task task) {
        for (Group group : feature.getChildren()) {
            for (Feature childFeature : group.getFeatures()) {
                String childName = childFeature.getFeatureName();
                if (childName.startsWith("d")) {
                    if (childName.contains(">=")) {
                        int lowerBound = Integer.parseInt(childName.split(" >= ")[1]);
                        task.setUnboundDurations(lowerBound);
                    } else if (childName.contains("=")) {
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
     * @param machines           The list of all machines.
     * @param machineAssignments The mapping of tasks to machines.
     */
    private static void setTaskMachine(Task task, List<Machine> machines, Map<String, String> machineAssignments) {
        // Find name of corresponding machine
        String machineName = machineAssignments.get(task.getName());
        if (machineName == null) {
            System.out.printf("Could not find a machine constraint for task %s.%n", task.getName());
            return;
        }

        // TODO: Could be optimized by adding a `Map<String, Machine> nameToMachine`
        // Find machine corresponding to machineName
        for (Machine m : machines) {
            if (machineName.equals(m.getName())) {
                task.setMachine(m);
                break;
            }
        }
    }

    /**
     * Parses the duration constraints to a Map containing the task names, the duration of a task and its required tasks for that duration
     *
     * @param durationConstraints The List of duration constraints
     * @return A Map<String,Map<Integer, List<String>>> in the style of <taskname,<durationOfTask,requiredTasksForDuration>>>
     */
    private static Map<String, Map<Integer, List<String>>> durationConstraintsToString(List<ImplicationConstraint> durationConstraints) {
        Map<String, Map<Integer, List<String>>> TaskToDurationToRequiredTask = new HashMap<>();

        for (ImplicationConstraint constraint : durationConstraints) {
            LiteralConstraint d = (LiteralConstraint) constraint.getLeft();
            LiteralConstraint p = (LiteralConstraint) constraint.getRight();

            String dString = d.getLiteral();
            String pString = p.getLiteral();

            // TODO: Remove dependence on the name of the task in the duration.
            //       Determine corresponding task from feature model.
            dString = dString.replace("\"\"", "");
            String[] dSubstrings = dString.split(" ");
            String taskName = dSubstrings[0].substring(1);

            try {
                int duration = Integer.parseInt(dSubstrings[2]);
                String requiredTaskName = pString;

                if (!TaskToDurationToRequiredTask.containsKey(taskName)) {
                    TaskToDurationToRequiredTask.put(taskName, new HashMap<>());
                }

                if (!TaskToDurationToRequiredTask.get(taskName).containsKey(duration)) {
                    TaskToDurationToRequiredTask.get(taskName).put(duration, new ArrayList<>());
                }

                TaskToDurationToRequiredTask.get(taskName).get(duration).add(requiredTaskName);

            } catch (NumberFormatException e) {
                System.out.println("Could probably not parse duration: " + dString);
            }
        }

        return TaskToDurationToRequiredTask;
    }
}
