package org.mbe.configSchedule.parser;


import com.google.ortools.sat.Literal;
import de.vill.main.UVLModelFactory;
import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import de.vill.model.Group;
import de.vill.model.constraint.Constraint;
import de.vill.model.constraint.ImplicationConstraint;
import de.vill.model.constraint.LiteralConstraint;
import de.vill.model.constraint.OrConstraint;
import org.mbe.configSchedule.util.Machine;
import org.mbe.configSchedule.util.Task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

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
     * Parses the deadline from the feature model
     *
     * @param featureModel The feature model from which the deadline should be retrieved
     * @return the deadline value
     */
    public static int parseDeadline(FeatureModel featureModel) {
        int deadline = -1;

        Optional<Feature> machineParent = featureModel.getRootFeature().getChildren()
                .stream()
                .flatMap(c -> c.getFeatures().stream())
                .filter(c -> c.getFeatureName().contains("dl"))
                .findAny();
        if (machineParent.isPresent()) {
            deadline = getDuration(machineParent.get().getFeatureName());
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
        List<Feature> mandatoryMachines = machineGroups
                .stream()
                .filter(c -> c.GROUPTYPE == Group.GroupType.MANDATORY)
                .flatMap(c -> c.getFeatures().stream())
                .toList();
        List<Feature> optionalMachines = machineGroups
                .stream()
                .filter(c -> c.GROUPTYPE == Group.GroupType.OPTIONAL)
                .flatMap(c -> c.getFeatures().stream())
                .toList();
        machines.addAll(mandatoryMachines
                .stream()
                .map(c -> new Machine(c.getFeatureName(), false))
                .toList());
        machines.addAll(optionalMachines
                .stream()
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

        List<Constraint> ctc = featureModel.getOwnConstraints();

        List<Constraint> machineConstraints = ctc
                .stream()
                .filter(c -> c instanceof ImplicationConstraint)
                .filter(c -> c.getConstraintSubParts().get(1).toString().startsWith("m"))
                .toList();
        List<String[]> machineConstraintsStrings = machineConstraintsToString(machineConstraints);

        List<Constraint> orderConstraints = ctc
                .stream()
                .filter(c -> c instanceof ImplicationConstraint)
                .filter(c -> ((ImplicationConstraint) c).getLeft().toString().startsWith("p")
                        && ((ImplicationConstraint) c).getRight().toString().startsWith("p"))
                .collect(Collectors.toCollection(ArrayList::new));

        List<Constraint> durationConstraints = ctc
                .stream()
                .filter(c -> c instanceof ImplicationConstraint)
                .filter(c -> c.getConstraintSubParts().get(0).toString().startsWith("\"d")
                        && c.getConstraintSubParts().get(0).toString().contains(" = "))
                .toList();

        List<Constraint> excludeConstraints = ctc
                .stream()
                .filter(c -> c.toString().contains(" & "))
                .toList();


        // First parse mandatory jobs, then add optional jobs
        jobs = parseMandatoryJobs(orderConstraints, mandatoryTasks);
        List<List<Task>> optionalJobs = parseOptionalJobs(optionalTasks, machineConstraintsStrings, machines);
        for (List<Task> job : optionalJobs) {
            jobs.add(job);
        }

        // Parse the constraints for excluding tasks and duration constraints
        List<String[]> excludeConstraintStrings = excludeConstraintsToString(excludeConstraints);
        Map<String, Map<Integer, List<String>>> TaskToDurationToRequiredTask = durationConstraintsToString(durationConstraints);

        // Assign machines to tasks
        // and complete the alternative groups
        for (List<Task> job : jobs) {
            for (Task task : job) {
                // Find name of corresponding machine
                String machineName = null;
                for (String[] constraint : machineConstraintsStrings) {
                    if (constraint[0].equals(task.getName())) {
                        machineName = constraint[1];
                        break;
                    }
                }

                if (machineName != null) {
                    // Find machine corresponding to machineName
                    Machine machine;
                    for (Machine m : machines) {
                        if (machineName.equals(m.getName())) {
                            task.setMachine(m);
                            break;
                        }
                    }
                } else {
                    System.out.printf("Could not find a machine constraint for task %s \n", task.getName());
                }


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
                if (TaskToDurationToRequiredTask.keySet().contains(task.getName())) {
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
     * Gets the Duration from the feature wihh a name in the format "dp = x"
     *
     * @param featureName Name of the feature
     * @return Integer value of the duration
     */
    private static int getDuration(String featureName) {
        try {
            String[] parts = featureName.split("=");
            return Integer.parseInt(parts[1].strip());
        } catch (Exception e) {
            System.out.println("Could not determine duration for feature \"" + featureName + "\", using 0 instead.");
            return 0;
        }
    }

    /**
     * Parses the mandatory jobs/ tasks. Also orders them in the correct order
     *
     * @param orderConstraints List containing the task-order-constraints
     * @param mandatoryTasks   List containing the features for the mandatory tasks
     * @return The mandatory tasks, divided into their jobs
     */
    private static List<List<Task>> parseMandatoryJobs(List<Constraint> orderConstraints, List<Feature> mandatoryTasks) {
        Set<String[]> orderPairs = new HashSet<>();
        Set<String> notStarter = new HashSet<>();
        Set<String> starterTasks = new HashSet<>();
        for (Constraint c : orderConstraints) {
            LiteralConstraint lLiteral = (LiteralConstraint) c.getConstraintSubParts().get(0);
            LiteralConstraint rLiteral = (LiteralConstraint) c.getConstraintSubParts().get(1);
            String l = lLiteral.getLiteral();
            String r = rLiteral.getLiteral();

            orderPairs.add(new String[]{l, r});
            starterTasks.add(l);
            starterTasks.add(r);
            notStarter.add(l);
        }

        // Remove notStarter, so starterTasks only contains tasks that start a job
        starterTasks.removeAll(notStarter);

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

            String currentTaskName = task.getName();

            for (int i = 0; i < orderConstraints.size(); i++) {
                Constraint orderPair = orderConstraints.get(i);
                LiteralConstraint lLiteral = (LiteralConstraint) orderPair.getConstraintSubParts().get(0);
                LiteralConstraint rLiteral = (LiteralConstraint) orderPair.getConstraintSubParts().get(1);
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

                    job.add(followTask);
                    allTasks.add(followTask);

                    currentTaskName = followTask.getName();
                    orderConstraints.remove(orderPair);
                    i = -1;

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
     * @param machineConstraints List of the constraints assigning the tasks to machines in the style of [p,m]
     * @param machines           List of machines
     * @return The optional tasks, each in their own job
     */
    private static List<List<Task>> parseOptionalJobs(List<Feature> optionalTasks, List<String[]> machineConstraints, List<Machine> machines) {
        List<List<Task>> jobs = new ArrayList<>();

        for (Feature feature : optionalTasks) {
            List<Task> job = new ArrayList<>();

            // Create Task and set name
            String name = feature.getFeatureName();
            Task task = new Task();
            task.setName(name);

            // Set optional of task to true
            task.setOptional(true);

            // Set duration of task
            List<Group> durations = feature.getChildren();
            List<Integer> durationIntegers = new ArrayList<>();
            for (Group group : durations) {
                for (Feature durationFeature : group.getFeatures()) {
                    String durationString = durationFeature.getFeatureName();
                    String[] durationSubstring = durationString.split(" = ");
                    durationIntegers.add(Integer.parseInt(durationSubstring[durationSubstring.length - 1]));
                }
            }
            Collections.sort(durationIntegers);
            task.setDuration(new int[]{durationIntegers.get(0), durationIntegers.get(durationIntegers.size() - 1)});

            // Set machine of task
            Optional<Machine> machine = machines.stream()
                    .filter(m -> machineConstraints.stream()
                            .filter(c -> c[0].equals(name))
                            .map(c -> c[1])
                            .findFirst()
                            .map(cm -> cm.equals(m.getName()))
                            .orElse(false))
                    .findFirst();

            if (machine.isPresent()) {
                task.setMachine(machine.get());
            } else {
                System.out.printf("Could not find machine for %s\n", name);
            }


            job.add(task);
            allTasks.add(task);
            jobs.add(job);
        }

        return jobs;
    }

    /**
     * Parses the machine constraints to a List of String-arrays in the style of [p,m]
     *
     * @param machineConstraints List of machine constraints
     * @return A List containing the machine-constraint-pairs as String-arrays
     */
    private static List<String[]> machineConstraintsToString(List<Constraint> machineConstraints) {
        List<String[]> constraintStrings = new ArrayList<>();

        for (Constraint constraint : machineConstraints) {
            LiteralConstraint p = (LiteralConstraint) constraint.getConstraintSubParts().get(0);
            LiteralConstraint m = (LiteralConstraint) constraint.getConstraintSubParts().get(1);

            String pString = p.getLiteral();
            String mString = m.getLiteral();

            constraintStrings.add(new String[]{pString, mString});
        }

        return constraintStrings;
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
            constraintStrings.add(findAndClause(constraint));
        }

        return constraintStrings;
    }

    /**
     * Helper function that strips a constraint of all its characters, except the task names
     *
     * @param constraint The constraint to be processed
     * @return A String array containing the task names featured in the constraint
     */
    private static String[] findAndClause(Constraint constraint) {

        String c = constraint.toString().split(" \\| ")[0];
        c = c.replaceAll("[()!&]", "");
        c = c.replaceAll("_\\S*", "");
        c = c.replaceAll("\\s+", " ");
        c = c.trim();

        String[] substrings = c.split(" ");
        String stop;

        return substrings;
    }

    /**
     * Set the duration of a task
     *
     * @param feature The feature containing the duration
     * @param task    The Task-Object for which the duration should be set
     */
    private static void setTaskDuration(Feature feature, Task task) {
        List<Group> durations = feature.getChildren();
        List<Integer> durationIntegers = new ArrayList<>();
        for (Group group : durations) {
            for (Feature durationFeature : group.getFeatures()) {
                String durationString = durationFeature.getFeatureName();
                String[] durationSubstring = durationString.split(" = ");
                durationIntegers.add(Integer.parseInt(durationSubstring[durationSubstring.length - 1]));
            }
        }
        Collections.sort(durationIntegers);
        task.setDuration(new int[]{durationIntegers.get(0), durationIntegers.get(durationIntegers.size() - 1)});
    }

    /**
     * Parses the duration constraints to a Map containing the task names, the duration of a task and its required tasks for that duration
     *
     * @param durationConstraints The List of duration constraints
     * @return A Map<String,Map<Integer, List<String>>> in the style of <taskname,<durationOfTask,requiredTasksForDuration>>>
     */
    private static Map<String, Map<Integer, List<String>>> durationConstraintsToString(List<Constraint> durationConstraints) {
        Map<String, Map<Integer, List<String>>> TaskToDurationToRequiredTask = new HashMap<>();

        for (Constraint constraint : durationConstraints) {
            LiteralConstraint d = (LiteralConstraint) constraint.getConstraintSubParts().get(0);
            LiteralConstraint p = (LiteralConstraint) constraint.getConstraintSubParts().get(1);

            String dString = d.getLiteral();
            String pString = p.getLiteral();

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
