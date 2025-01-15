package org.mbe.configSchedule.generator;

import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import de.vill.model.Group;
import org.mbe.configSchedule.util.Machine;
import org.mbe.configSchedule.util.PathPreferences;
import org.mbe.configSchedule.util.SchedulingProblem;
import org.mbe.configSchedule.util.Task;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

import static java.lang.Math.floor;

public class SPGenerator {

    /**
     * Generates a scheduling problem with the parameters
     *
     * @param jobCount             Number of jobs
     * @param taskCount            Number of mandatory tasks
     * @param durationOutlierCount Number of duration Outliers (tasks that take longer than usual)
     * @param machineCount         Number of machines
     * @param optionalCount        Number of optional tasks
     * @param altCount             Number of alternative tasks
     * @param altGroupCount        Number of alternative task groups
     * @param deadline             Deadline
     * @param durationConstraints  Number of duration constraints
     * @param maxDurationRequires  Maximum number of duration constraints originating from one task
     * @param name                 Name of the problem
     * @throws IOException
     */
    public void generateProblem(int jobCount, int taskCount,
                                int durationOutlierCount, int machineCount, int optionalCount,
                                int altCount, int altGroupCount, int deadline, int durationConstraints, int maxDurationRequires, String name) throws IOException {
        List<List<Task>> jobs = new ArrayList<>();
        List<Machine> machines = new ArrayList<>();

        // Used to create duration-constraints
        // From a mandatory task with a variable duration, can one or more require constraints originate from one duration
        // To make sure that there are no require constraints from the same duration leading to multiple tasks in the same
        // alternative task group, the tasks in an alternative task group (and optional tasks) are saved in an array,
        // so that a require constraint can lead to only one task in that group
        List<Task[]> optionalTasks = new ArrayList<>();
        List<Task> mandatoryTasksWithVarDuration = new ArrayList<>();

        Random random = new Random();

        if (jobCount > taskCount) {
            System.out.println("Please enter more tasks than jobs");
        }

        // Fill the necessary lists (jobs and machines)
        createMachines(machineCount, machines);
        createStandardJobs(taskCount, jobCount, machineCount, durationOutlierCount, machines, jobs, mandatoryTasksWithVarDuration);
        createOptionalTasks(optionalCount, machineCount, machines, jobs, optionalTasks);
        createAlternativeTasks(altCount, altGroupCount, machineCount, machines, jobs, optionalTasks);
        createDurationConstraints(optionalTasks, mandatoryTasksWithVarDuration, durationConstraints, maxDurationRequires);

        // Create a SchedulingProblem from the beforehand created lists
        SchedulingProblem sp = new SchedulingProblem(jobs, machines, deadline);
        String problemUVL = parseToUVL(sp, name);

/*
        PathPreferences prefs = new PathPreferences();
        String fileOutputString = prefs.getProblemSavePath() + name + ".txt";
        FileOutputStream fOut = new FileOutputStream(fileOutputString);
        ObjectOutputStream oOut = new ObjectOutputStream(fOut);

        oOut.writeObject(sp);
        oOut.flush();
        oOut.close();

 */


        PathPreferences prefs = new PathPreferences();
        PrintWriter out = new PrintWriter(new FileOutputStream(prefs.getProblemSavePath() + name + ".txt"));
        out.println(problemUVL);
        out.close();
    }

    /**
     * Creates the machines for the problem genreated by the Problem Generator
     *
     * @param machineCount The number of machines (to be created)
     * @param machines     List of machines
     */
    private static void createMachines(int machineCount, List<Machine> machines) {
        int machineID = 1;
        for (int i = 0; i < machineCount; i++) {
            Machine machine = new Machine("m" + machineID, false);
            machineID++;
            machines.add(machine);
        }
    }

    /**
     * Creates the standard jobs for the problem generated by the Problem Generator
     *
     * @param taskCount                     The total number of tasks (to be created)
     * @param jobCount                      The number of jobs (to be created)
     * @param machineCount                  The number of machines
     * @param durationOutlierCount          The number of tasks with a duration that lies outside the "normal" range
     * @param machines                      The number of machines
     * @param jobs                          List of jobs
     * @param mandatoryTasksWithVarDuration List of mandatory tasks that have a variable duration
     */
    private static void createStandardJobs(int taskCount, int jobCount, int machineCount, int durationOutlierCount,
                                           List<Machine> machines, List<List<Task>> jobs, List<Task> mandatoryTasksWithVarDuration) {
        Random random = new Random();

        double tasksPerJobD = taskCount / jobCount;
        int tasksPerJob = (int) floor(tasksPerJobD);
        int restTasks = taskCount % tasksPerJob;
        int tasksInJob;

        boolean firstJob = true;
        int taskID = 1;

        for (int i = 0; i < jobCount; i++) {
            List<Task> job = new ArrayList<>();

            // If this is the first job, the excess-tasks should be in this job
            if (firstJob) {
                tasksInJob = tasksPerJob + restTasks;
                firstJob = false;
            } else {
                tasksInJob = tasksPerJob;
            }

            for (int j = 0; j < tasksInJob; j++) {

                Task task = new Task();
                task.setName("p" + taskID);
                taskID++;
                task.setMachine(machines.get(random.nextInt(machineCount)));
                task.setDuration(new int[2]);

                // If there are equal to or more durationOutlier than tasks to be created,
                // then the task has to be an outlier
                if (((taskCount - taskID) == durationOutlierCount) && (durationOutlierCount > 0)) {
                    // Should duration be variable?
                    int variableDurationChance = random.nextInt(2);
                    // 0 = Variable duration, 1 = static duration
                    if (variableDurationChance == 0) {
                        // between 6 and 15
                        int dur1 = random.nextInt(10) + 6;
                        int dur2 = random.nextInt(10) + 6;
                        int[] duration = {dur1, dur2};
                        task.setDuration(duration);

                        // Add tasks to mandatory tasks for the duration constraints
                        mandatoryTasksWithVarDuration.add(task);
                    } else {
                        int dur = random.nextInt(10) + 6;
                        int[] duration = {dur, dur};
                        task.setDuration(duration);
                    }
                    durationOutlierCount--;

                } else {
                    // Tasks duration still has the chance to be an outlier
                    int outLierChance = random.nextInt(2);
                    // 0 = Outlier, 1 = not an outlier
                    if (outLierChance == 0 && durationOutlierCount > 0) {
                        // Should it be a variable duration?
                        int variableDurationChance = random.nextInt(2);
                        // 0 = Variable duration, 1 = Static duration
                        if (variableDurationChance == 0) {
                            // Between 6 and 15
                            int dur1 = random.nextInt(10) + 6;
                            int dur2 = random.nextInt(10) + 6;
                            int[] duration = {dur1, dur2};
                            task.setDuration(duration);

                            mandatoryTasksWithVarDuration.add(task);
                        } else {
                            int dur = random.nextInt(10) + 6;
                            int[] duration = {dur, dur};
                            task.setDuration(duration);
                        }
                        durationOutlierCount--;

                        // No outlier
                    } else {
                        // Should it be a variable duration?
                        int variableDurationChance = random.nextInt(2);
                        // 0 = Variable duration, 1 = Static duration
                        if (variableDurationChance == 0) {
                            // Between 1 and 5
                            int dur1 = random.nextInt(5) + 1;
                            int dur2 = random.nextInt(5) + 1;
                            int[] duration = {dur1, dur2};
                            task.setDuration(duration);

                            mandatoryTasksWithVarDuration.add(task);
                        } else {
                            int dur = random.nextInt(5) + 1;
                            int[] duration = {dur, dur};
                            task.setDuration(duration);
                        }
                    }
                }

                task.setOptional(false);
                job.add(task);
            }
            jobs.add(job);
        }
    }

    /**
     * Creates the optional tasks for the problem generated by the Problem Generator
     *
     * @param optionalCount The number of optional tasks to be created
     * @param machineCount  the number of machines
     * @param machines      List of machines
     * @param jobs          List of jobs
     * @param optionalTasks List of optionalTasks
     */
    private static void createOptionalTasks(int optionalCount, int machineCount, List<Machine> machines, List<List<Task>> jobs,
                                            List<Task[]> optionalTasks) {

        Random random = new Random();

        int taskID = 1;

        // Create optional tasks
        for (int i = 0; i < optionalCount; i++) {
            List<Task> job = new ArrayList<>();
            Task task = new Task();

            task.setName("po" + taskID);
            taskID++;
            task.setMachine(machines.get(random.nextInt(machineCount)));
            task.setOptional(true);
            task.setDuration(new int[2]);

            // Randomly choose if task has a variable duration
            int varDurationChance = random.nextInt(2);

            // 0 = Variable, 1 = static
            if (varDurationChance == 0) {
                // Between 1 and 5
                int dur1 = random.nextInt(5) + 1;
                int dur2 = random.nextInt(5) + 1;
                int[] duration = {dur1, dur2};
                task.setDuration(duration);
            } else {
                // Not variable
                int dur = random.nextInt(5) + 1;
                int[] duration = {dur, dur};
                task.setDuration(duration);
            }

            // Add to optional tasks
            Task[] taskArr = {task};
            optionalTasks.add(taskArr);

            job.add(task);
            jobs.add(job);
        }
    }

    /**
     * Creates the alternative task groups for the problem generated by the Problem Generator
     *
     * @param altCount      The total number of tasks belonging to alternative task groups (to be created)
     * @param altGroupCount The number of alternative task groups (to be created)
     * @param machineCount  The number of machines
     * @param machines      List of machines
     * @param jobs          List of jobs
     * @param optionalTasks List of optional tasks
     */
    private static void createAlternativeTasks(int altCount, int altGroupCount, int machineCount, List<Machine> machines,
                                               List<List<Task>> jobs, List<Task[]> optionalTasks) {
        // Check if there even are alternative tasks to be created
        if (altCount > 0 && altGroupCount > 0) {
            Random random = new Random();

            if (altGroupCount > altCount) {
                altGroupCount = altCount / 2;
                System.out.printf("Number of alternative groups must be greater than number of alternative tasks. \n" +
                        "Set number of groups to %d (every group consists of 2 tasks). \n", altGroupCount);
            }
            // Calculate how many tasks beling in one group
            // If there are e.g. 5 tasks and 2 groups -> g1 = (1,2,3), g2 = (4,5)
            double tasksPerGroupD = altCount / altGroupCount;
            int tasksPerGroup = (int) floor(tasksPerGroupD);
            int restTasksAlt = altCount % tasksPerGroup;
            int tasksInGroup;

            boolean firstGroup = true;

            int taskID = 1;
            for (int i = 0; i < altGroupCount; i++) {
                // If it's the first group that is created, then it should include the surplus of tasks
                if (firstGroup) {
                    tasksInGroup = tasksPerGroup + restTasksAlt;
                    firstGroup = false;
                } else {
                    tasksInGroup = tasksPerGroup;
                }

                // Task-array that, at the end, contains all the tasks belonging to one alternative task group
                Task[] group = new Task[tasksInGroup];

                // Create tasks for the group
                for (int j = 0; j < tasksInGroup; j++) {
                    List<Task> job = new ArrayList<>();
                    Task task = new Task();
                    task.setName("pa" + taskID);
                    taskID++;
                    task.setMachine(machines.get(random.nextInt(machineCount)));
                    task.setDuration(new int[2]);

                    // Randomly choose if the task has a variable duration
                    int variableDurationChance = random.nextInt(2);
                    // 0 = Variable, 1 = static
                    if (variableDurationChance == 0) {
                        // Between 1 and 5
                        int dur1 = random.nextInt(5) + 1;
                        int dur2 = random.nextInt(5) + 1;
                        int[] duration = {dur1, dur2};
                        task.setDuration(duration);
                    } else {
                        int dur = random.nextInt(5) + 1;
                        int[] duration = {dur, dur};
                        task.setDuration(duration);
                    }

                    task.setOptional(true);
                    task.setExcludeTasks(new ArrayList<>());
                    group[j] = task;

                    // An alternative task is its own job
                    job.add(task);
                    jobs.add(job);
                }

                // Add the alternative task group to the optionalTasks
                optionalTasks.add(group);

                // Every task in the group has a list with the other tasks of the group
                // These are the tasks that the task exludes
                // If there is the group (1,2,3) -> ExcludeTasks(1) = {2,3}
                for (int j = 0; j < group.length; j++) {
                    List<String> list = group[j].getExcludeTasks();

                    for (int k = 0; k < group.length; k++) {
                        if (j != k) {
                            list.add(group[k].getName());
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates the duration constraints for the problem generated by the Problem Generator
     *
     * @param optionalTasks                 List with arrays of optional tasks. Optional Tasks are arrays with one task, alternative task groups are arrays with multiple tasks
     * @param mandatoryTasksWithVarDuration List with mandatory tasks that have a variable duration
     * @param durationConstraints           int that determines the number of durations that are part of a duration constraint
     * @param maxDurationRequires           int that determines the number of constraints originating from one duration
     */
    private static void createDurationConstraints(List<Task[]> optionalTasks, List<Task> mandatoryTasksWithVarDuration,
                                                  int durationConstraints, int maxDurationRequires) {
        Random random = new Random();

        // Check if there are enough mandatory Tasks for the number of durationConstraints
        if (durationConstraints > mandatoryTasksWithVarDuration.size()) {
            System.out.printf("Not enough mandatory tasks with variable durations for durationConstraints %d, set to %d.%n",
                    durationConstraints, mandatoryTasksWithVarDuration.size());
            durationConstraints = mandatoryTasksWithVarDuration.size();
        }

        // Check if there are enough optional Tasks for the max. amount
        // of Duration-Constraints originating from one task
        if (maxDurationRequires > optionalTasks.size()) {
            System.out.printf("Not enough optional/ alternative tasks for maxDurationRequires %d, set to %d.%n",
                    maxDurationRequires, optionalTasks.size());
            maxDurationRequires = optionalTasks.size();

        }

        if (!optionalTasks.isEmpty()) {
            List<Integer> mandIndeces = IntStream.range(0, mandatoryTasksWithVarDuration.size() + 1).boxed().toList();
            // Iterate over mandatory tasks with variable duration
            // If there are 3 durationConstraints, then the first 3 tasks will have duration constraints
            for (int i = 0; i < durationConstraints; i++) {
                Task task = mandatoryTasksWithVarDuration.get(i);

                // Choose which duration value is part of the constraint
                int minDuration = task.getDuration()[0];
                int maxDuration = task.getDuration()[1];
                int durationForConstraint = random.nextInt((maxDuration + 1) - minDuration) + minDuration;

                List<Task> requiredTasks = new ArrayList<>();

                // List with an Index for every optional task/ alternative task group
                // If there are 3 optional tasks, then the list is {0,1,2}
                List<Integer> optionalIndices = new ArrayList<>();
                for (int j = 0; j < optionalTasks.size(); j++) {
                    optionalIndices.add(j);
                }

                for (int j = 0; j < maxDurationRequires; j++) {
                    // Randomly choose one of the indeces in optionalIndices - Via that index we can
                    // choose an optional task group from optionalTasks
                    // Remove Index from optionalIndices, so that that index is not chosen twice
                    int taskGroupIndex = optionalIndices.get(random.nextInt(optionalIndices.size()));
                    optionalIndices.remove(optionalIndices.indexOf(taskGroupIndex));
                    Task[] taskGroup = optionalTasks.get(taskGroupIndex);

                    // Randomly chooses one of the tasks in the group to be
                    // the required task for the constraint
                    int taskIndex = random.nextInt(taskGroup.length);
                    Task requiredTask = taskGroup[taskIndex];
                    requiredTasks.add(requiredTask);
                }

                task.addDurationCon(durationForConstraint, requiredTasks);
            }
        }
    }


    /**
     * Parses the given scheduling problem to the UVL-format
     *
     * @param sp   SchedulingProblem-Object for which the UVL-file is to be created
     * @param name Name of the problemm
     * @return String in the format of a UVL-file
     */
    private static String parseToUVL(SchedulingProblem sp, String name) {
        StringBuilder uvlString = new StringBuilder();
        uvlString.append("features\n");
        uvlString.append("\t" + name + " {featuredescription__ \'" + sp.getDeadline() + "\', abstract true}\n");
        uvlString.append("\t\tmandatory\n");
        uvlString.append("\t\t\t\"dl = " + sp.getDeadline() + "\"\n");

        List<String>[] cons = parseTasks(sp.getJobs(), uvlString);
        parseMachines(sp.getMachines(), uvlString);
        parseConstraints(cons, uvlString);

        return uvlString.toString();
    }

    /**
     * Parses the tasks of the jobs with their characterisitics, e.g. duration. Appends the features to the String started in parseToUVL
     *
     * @param jobs      Contains every job and task of the problem
     * @param uvlString The UVL-string to be appended
     * @return Returns constraints for task order, excluding tasks, durations and machine
     */
    private static List<String>[] parseTasks(List<List<Task>> jobs, StringBuilder uvlString) {
        uvlString.append("\t\t\tP {abstract true}\n");

        List<List<Task>> mandatoryJobs = new ArrayList<>();
        List<List<Task>> optionalJobs = new ArrayList<>();

        List<String> taskOrderCons = new ArrayList<>();
        List<String> excludeCons = new ArrayList<>();
        List<String> durationCons = new ArrayList<>();
        List<String> machineCons = new ArrayList<>();

        for (List<Task> job : jobs) {
            if (job.get(0).isOptional()) {
                optionalJobs.add(job);
            } else {
                mandatoryJobs.add(job);
            }
        }

        uvlString.append("\t\t\t\tmandatory\n");
        for (List<Task> job : mandatoryJobs) {
            for (int i = 0; i < job.size(); i++) {
                Task task = job.get(i);
                uvlString.append("\t\t\t\t\t" + task.getName() + "\n");
                if (task.getDuration()[0] == task.getDuration()[1]) {
                    uvlString.append("\t\t\t\t\t\tmandatory\n");
                    uvlString.append("\t\t\t\t\t\t\t\"d" + task.getName() + " = " + task.getDuration()[0] + "\"\n");
                } else {
                    uvlString.append("\t\t\t\t\t\talternative\n");
                    for (int k = task.getDuration()[0]; k <= task.getDuration()[1]; k++) {
                        uvlString.append("\t\t\t\t\t\t\t\"d" + task.getName() + " = " + k + "\"\n");
                    }
                }


                machineCons.add("\t" + task.getName() + " => " + task.getMachine().getName() + "\n");

                // Add constraint for the task order in the job
                if (i < job.size() - 1) {
                    taskOrderCons.add("\t" + job.get(i + 1).getName() + " => " + task.getName() + "\n");
                }

                // Task-duration-constraints
                if (!task.getDurationCons().isEmpty()) {
                    for (Map.Entry<Integer, List<Task>> taskDurationCon : task.getDurationCons().entrySet()) {
                        List<Task> requiredTasks = taskDurationCon.getValue();
                        for (Task requiredTask : requiredTasks) {
                            durationCons.add("\t\"d" + task.getName() + " = " + taskDurationCon.getKey() + "\" => " + requiredTask.getName() + "\n");
                        }
                    }
                }

            }
        }

        List<String> excludeTasksAlreadyHandled = new ArrayList<>();
        uvlString.append("\t\t\t\toptional\n");
        for (List<Task> job : optionalJobs) {
            for (Task task : job) {
                uvlString.append("\t\t\t\t\t" + task.getName() + "\n");
                if (task.getDuration()[0] == task.getDuration()[1]) {
                    uvlString.append("\t\t\t\t\t\tmandatory\n");
                    uvlString.append("\t\t\t\t\t\t\t\"d" + task.getName() + " = " + task.getDuration()[0] + "\"\n");
                } else {
                    uvlString.append("\t\t\t\t\t\talternative\n");
                    for (int k = task.getDuration()[0]; k <= task.getDuration()[1]; k++) {
                        uvlString.append("\t\t\t\t\t\t\t\"d" + task.getName() + " = " + k + "\"\n");
                    }
                }

                machineCons.add("\t" + task.getName() + " => " + task.getMachine().getName() + "\n");

                if (!task.getExcludeTasks().isEmpty() && !excludeTasksAlreadyHandled.contains(task.getName())) {
                    for (String handledTask : task.getExcludeTasks()) {
                        excludeTasksAlreadyHandled.add(handledTask);
                    }

                    List<String> alternativeGroup = new ArrayList<>();
                    for (String excludeTask : task.getExcludeTasks()) {
                        alternativeGroup.add(excludeTask);
                    }
                    alternativeGroup.add(task.getName());

                    StringBuilder excludeConString = new StringBuilder();
                    excludeConString.append("\t");
                    for (int i = 0; i < alternativeGroup.size(); i++) {
                        excludeConString.append("(");
                        for (int j = 0; j < alternativeGroup.size(); j++) {
                            if (i == j) {
                                excludeConString.append(alternativeGroup.get(j));
                            } else {
                                excludeConString.append("!" + alternativeGroup.get(j));
                            }

                            if (j < alternativeGroup.size() - 1) {
                                excludeConString.append(" & ");
                            }
                        }
                        excludeConString.append(")");
                        if (i < alternativeGroup.size() - 1) {
                            excludeConString.append(" | ");
                        }
                    }
                    excludeConString.append("\n");

                    excludeCons.add(excludeConString.toString());
                }
            }
        }

        List<String>[] cons = new List[4];
        cons[0] = taskOrderCons;
        cons[1] = excludeCons;
        cons[2] = durationCons;
        cons[3] = machineCons;

        return cons;
    }

    /**
     * Appends the features for the machines to the UVL-string
     *
     * @param machines  List of machines
     * @param uvlString The UVL-String to be appended
     */
    private static void parseMachines(List<Machine> machines, StringBuilder uvlString) {
        uvlString.append("\t\t\tM {abstract true}\n");
        uvlString.append("\t\t\t\tmandatory\n");

        for (Machine machine : machines) {
            uvlString.append("\t\t\t\t\t" + machine.getName() + "\n");
        }
    }

    /**
     * Appends the constraints to the UVL-String
     *
     * @param cons      List of constraints
     * @param uvlString The UVL-String to be appended
     */
    private static void parseConstraints(List<String>[] cons, StringBuilder uvlString) {
        uvlString.append("constraints\n");
        for (List<String> conType : cons) {
            for (String con : conType) {
                uvlString.append(con);
            }
        }
    }
}
