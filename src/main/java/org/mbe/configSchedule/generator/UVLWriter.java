package org.mbe.configSchedule.generator;

import org.mbe.configSchedule.util.Machine;
import org.mbe.configSchedule.util.PathPreferences;
import org.mbe.configSchedule.util.SchedulingProblem;
import org.mbe.configSchedule.util.Task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UVLWriter {
    public void writeToFile(SchedulingProblem sp) throws IOException {
        String problemUVL = parseToUVL(sp);

        PathPreferences prefs = new PathPreferences();
        Path path = Path.of(prefs.getProblemSavePath());

        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }

        Path filepath = path.resolve(sp.getName() + ".uvl");
        Files.writeString(filepath, problemUVL);
    }


    /**
     * Parses the given scheduling problem to the UVL-format
     *
     * @param sp SchedulingProblem-Object for which the UVL-file is to be created
     * @return String in the format of a UVL-file
     */
    protected String parseToUVL(SchedulingProblem sp) {
        StringBuilder uvlString = new StringBuilder();
        uvlString.append("features").append(System.lineSeparator());
        uvlString.append("\t").append(sp.getName()).append(" {abstract true}").append(System.lineSeparator());
        uvlString.append("\t\tmandatory").append(System.lineSeparator());
        uvlString.append("\t\t\t\"dl = ").append(sp.getDeadline()).append("\"").append(System.lineSeparator());

        List<String>[] cons = parseTasks(sp, uvlString);
        parseMachines(sp.getMachines(), uvlString);
        parseConstraints(cons, uvlString);

        return uvlString.toString();
    }

    /**
     * Parses the tasks of the jobs with their characterisitics, e.g. duration. Appends the features to the String started in parseToUVL
     *
     * @param sp        Contains the generated scheduling problem
     * @param uvlString The UVL-string to be appended
     * @return Returns constraints for task order, excluding tasks, durations and machine
     */
    protected List<String>[] parseTasks(SchedulingProblem sp, StringBuilder uvlString) {
        uvlString.append("\t\t\tP {abstract true}").append(System.lineSeparator());

        List<Task> mandatoryTasks = new ArrayList<>();
        List<Task> optionalTasks = new ArrayList<>();

        List<String> taskOrderCons = new ArrayList<>();
        List<String> excludeCons = new ArrayList<>();
        List<String> durationCons = new ArrayList<>();
        List<String> machineCons = new ArrayList<>();

        for (Task task : sp.getTasks()) {
            if (task.isOptional()) {
                optionalTasks.add(task);
            } else {
                mandatoryTasks.add(task);
            }

            for (Map.Entry<Integer, List<Task>> taskDurationCon : task.getDurationCons().entrySet()) {
                List<Task> requiredTasks = taskDurationCon.getValue();
                for (Task requiredTask : requiredTasks) {
                    durationCons.add("\t\"d%s = %d\" => %s%n".formatted(task.getName(), taskDurationCon.getKey(), requiredTask.getName()));
                }
            }

            for (Task dependency : sp.getPrecedenceOrder().getOrDefault(task, new ArrayList<>())) {
                taskOrderCons.add("\t%s => %s%n".formatted(task.getName(), dependency.getName()));
            }
        }

        if (!mandatoryTasks.isEmpty())
            uvlString.append("\t\t\t\tmandatory").append(System.lineSeparator());
        for (Task task : mandatoryTasks) {
            parseTask(task, uvlString, machineCons);
        }

        List<String> excludeTasksAlreadyHandled = new ArrayList<>();
        if (!optionalTasks.isEmpty())
            uvlString.append("\t\t\t\toptional").append(System.lineSeparator());
        for (Task task : optionalTasks) {
            parseTask(task, uvlString, machineCons);
            if (task.getExcludeTasks().isEmpty() || excludeTasksAlreadyHandled.contains(task.getName())) {
                continue;
            }
            excludeTasksAlreadyHandled.addAll(task.getExcludeTasks());

            List<String> alternativeGroup = new ArrayList<>(task.getExcludeTasks());
            alternativeGroup.add(task.getName());

            StringBuilder excludeConString = new StringBuilder();
            excludeConString.append("\t");
            for (int i = 0; i < alternativeGroup.size(); i++) {
                excludeConString.append("(");
                for (int j = 0; j < alternativeGroup.size(); j++) {
                    if (i == j) {
                        excludeConString.append(alternativeGroup.get(j));
                    } else {
                        excludeConString.append("!").append(alternativeGroup.get(j));
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
            excludeConString.append(System.lineSeparator());

            excludeCons.add(excludeConString.toString());
        }


        @SuppressWarnings("unchecked")
        List<String>[] cons = new List[4];
        cons[0] = taskOrderCons;
        cons[1] = excludeCons;
        cons[2] = durationCons;
        cons[3] = machineCons;

        return cons;
    }

    /**
     * Appends the task as a feature with durations and its machine constraints.
     *
     * @param task        The task to parse.
     * @param uvlString   The UVL-String to be appended.
     * @param machineCons The machine constraints list to be appended.
     */
    protected void parseTask(Task task, StringBuilder uvlString, List<String> machineCons) {
        uvlString.append("\t\t\t\t\t").append(task.getName()).append(System.lineSeparator());

        List<String> durationFeatureStrings = new ArrayList<>();

        String durationStringTemplate = "\t\t\t\t\t\t\t\"d" + task.getName() + " = %d\"";
        for (int duration : task.getDurations()) {
            durationFeatureStrings.add(durationStringTemplate.formatted(duration));
        }
        String unboundDurationStringTemplate = durationStringTemplate.replace("=", ">=");
        task.getUnboundDurations().ifPresent(
                integer -> durationFeatureStrings.add(unboundDurationStringTemplate.formatted(integer))
        );

        uvlString.append("\t\t\t\t\t\t");
        uvlString.append(durationFeatureStrings.size() == 1 ? "mandatory" : "alternative");
        uvlString.append(System.lineSeparator());
        uvlString.append(String.join(System.lineSeparator(), durationFeatureStrings));
        uvlString.append(System.lineSeparator());

        machineCons.add("\t%s => %s%n".formatted(task.getName(), task.getMachine().getName()));
    }

    /**
     * Appends the features for the machines to the UVL-string
     *
     * @param machines  List of machines
     * @param uvlString The UVL-String to be appended
     */
    protected void parseMachines(List<Machine> machines, StringBuilder uvlString) {
        uvlString.append("\t\t\tM {abstract true}").append(System.lineSeparator());
        uvlString.append("\t\t\t\tmandatory").append(System.lineSeparator());

        for (Machine machine : machines) {
            uvlString.append("\t\t\t\t\t").append(machine.getName()).append(System.lineSeparator());
        }
    }

    /**
     * Appends the constraints to the UVL-String
     *
     * @param cons      List of constraints
     * @param uvlString The UVL-String to be appended
     */
    protected void parseConstraints(List<String>[] cons, StringBuilder uvlString) {
        uvlString.append("constraints").append(System.lineSeparator());
        for (List<String> conType : cons) {
            for (String con : conType) {
                uvlString.append(con);
            }
        }
    }
}
