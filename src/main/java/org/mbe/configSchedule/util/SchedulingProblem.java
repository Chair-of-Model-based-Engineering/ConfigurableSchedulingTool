package org.mbe.configSchedule.util;

import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import de.vill.model.Feature;
import de.vill.model.Group;
import de.vill.model.constraint.Constraint;
import de.vill.model.constraint.ImplicationConstraint;
import de.vill.model.constraint.LiteralConstraint;
import org.w3c.dom.Node;

import javax.swing.text.html.Option;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;


public class SchedulingProblem implements Serializable {
    private List<List<Task>> jobs;
    private List<Machine> machines;
    private int deadline;

    public SchedulingProblem(List<List<Task>> jobs, List<Machine> machines, int deadline) {
        this.jobs = jobs;
        this.machines = machines;
        this.deadline = deadline;
    }

    public SchedulingProblem(FeatureModel featureModel) {
        this.deadline = parseDeadline(featureModel);
        this.machines = parseMachines(featureModel);
        this.jobs = parseJobs(featureModel, machines);
    }

    private int parseDeadline(FeatureModel featureModel) {
        deadline = -1;
        try {
            String deadlineString = featureModel.getRootFeature().getAttributes().get("deadline").getValue().toString();
            deadline = Integer.parseInt(deadlineString);
        } catch (Exception e) {
            System.out.println("Deadline konnte nicht konvertiert werden, überprüfe Description von root");
            return deadline;
        }
        return deadline;
    }

    private List<Machine> parseMachines(FeatureModel featureModel) {
        List<Machine> machines = new ArrayList<>();
        Optional<Feature> machineParent = featureModel.getRootFeature().getChildren()
                .stream()
                .flatMap(c -> c.getFeatures().stream())
                .filter(c->c.getFeatureName().equalsIgnoreCase("M"))
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
                .map(c->new Machine(c.getFeatureName(), false))
                .toList());
        machines.addAll(optionalMachines
                .stream()
                .map(c->new Machine(c.getFeatureName(), true))
                .toList());
        return machines;
    }

    private List<List<Task>> parseJobs(FeatureModel featureModel, List<Machine> machines) {
        List<List<Task>> tasks = new ArrayList<>();
        Optional<Feature> taskParent = featureModel.getRootFeature().getChildren()
                .stream()
                .flatMap(c -> c.getFeatures().stream())
                .filter(c->c.getFeatureName().equalsIgnoreCase("P"))
                .findAny();
        if (taskParent.isEmpty()) {
            System.out.println("Tasks could not be converted, check the model");
            return tasks;
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
        /*


        List<Constraint> machineConstraints = ctc
                .stream()
                .filter(c->c instanceof ImplicationConstraint)
                .map(c->(ImplicationConstraint)c)
                .filter(c -> c.getConstraintSubParts()
                        .stream()
                        .filter(l -> l instanceof LiteralConstraint)
                        .map(m ->  ((LiteralConstraint) m).getFeature())
                        .filter(f -> machines.stream().anyMatch(m -> m.getName().equals(f.getFeatureName())))).toList()



        Map<String, Machine> machineMap = new HashMap<String, Machine>();
        //ctc.stream().forEach(c -> machineMap.put(c.getConstraintSubParts());
        //tasks = mandatoryTasks.stream().map(c->new Task())
         */

        return null;
    }
    private int getDuration(String featureName){
        try{
            String[] parts = featureName.split("=");
            return Integer.parseInt(parts[1].strip());
        }catch (Exception e){
            return 0;
        }
    }

    public List<List<Task>> getJobs() {
        return jobs;
    }

    public void setJobs(List<List<Task>> jobs) {
        this.jobs = jobs;
    }

    public List<Machine> getMachines() {
        return machines;
    }

    public void setMachines(List<Machine> machines) {
        this.machines = machines;
    }

    public int getDeadline() {
        return deadline;
    }

    public void setDeadline(int deadline) {
        this.deadline = deadline;
    }


}
