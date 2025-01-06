package org.mbe.configSchedule.util;

import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import de.vill.model.Group;
import de.vill.model.constraint.*;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

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
            String deadlineString = featureModel.getRootFeature().getAttributes().get("featuredescription__").getValue().toString();
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

        List<Constraint> machineConstraints = ctc
                .stream()
                .filter(c -> c instanceof ImplicationConstraint)
                .filter(c -> c.getConstraintSubParts().get(1).toString().startsWith("m"))
                .toList();
        List<String[]> machineConstraintsStrings = machineConstraintToString(machineConstraints);

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

        jobs = parseMandatoryJobs(orderConstraints);
        List<List<Task>> optionalJobs = parseOptionalJobs(optionalTasks, machineConstraintsStrings);
        for(List<Task> job: optionalJobs) {
            jobs.add(job);
        }

        excludeConstraintsToString(excludeConstraints);




        /*
        for(List<Task> job : jobs) {
            System.out.print("New Job    ");
            for(Task task : job) {
                System.out.print(task.toString() + " || ");
            }
            System.out.println("\n");
        }
                 */

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

        return jobs;
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

    public List<List<Task>> parseMandatoryJobs(List<Constraint> orderConstraints) {
        Set<String[]> orderPairs = new HashSet<>();
        Set<String> notStarter = new HashSet<>();
        Set<String> starterTasks = new HashSet<>();
        for(Constraint c : orderConstraints) {
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

        for(String starterTask : starterTasks) {
            List<Task> job = new ArrayList<>();
            Task task = new Task();
            task.setName(starterTask);

            job.add(task);

            String currentTaskName = task.getName();

            for(int i = 0; i < orderConstraints.size(); i++) {
                Constraint orderPair = orderConstraints.get(i);
                LiteralConstraint lLiteral = (LiteralConstraint) orderPair.getConstraintSubParts().get(0);
                LiteralConstraint rLiteral = (LiteralConstraint) orderPair.getConstraintSubParts().get(1);
                String l = lLiteral.getLiteral();
                String r = rLiteral.getLiteral();
                if(r.equals(currentTaskName)) {
                    Task followTask = new Task();
                    followTask.setName(l);
                    job.add(followTask);
                    currentTaskName = followTask.getName();
                    orderConstraints.remove(orderPair);
                    i = -1;

                }
            }
            jobs.add(job);
        }
        return jobs;
    }

    public List<List<Task>> parseOptionalJobs(List<Feature> optionalTasks, List<String[]> machineConstraints) {
        List<List<Task>> jobs = new ArrayList<>();

        for(Feature feature : optionalTasks) {
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
            for(Group group : durations) {
                for(Feature durationFeature : group.getFeatures()) {
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

            if(machine.isPresent()) {
                task.setMachine(machine.get());
            } else {
                System.out.printf("Could not find machine for %s\n", name);
            }


            job.add(task);
            jobs.add(job);
        }

        return jobs;
    }

    public List<String[]> machineConstraintToString(List<Constraint> machineConstraints) {
        List<String[]> constraintStrings = new ArrayList<>();

        for(Constraint constraint : machineConstraints) {
            LiteralConstraint p = (LiteralConstraint) constraint.getConstraintSubParts().get(0);
            LiteralConstraint m = (LiteralConstraint) constraint.getConstraintSubParts().get(1);

            String pString = p.getLiteral();
            String mString = m.getLiteral();

            constraintStrings.add(new String[]{pString,mString});
        }

        return constraintStrings;
    }

    public List<String[]> excludeConstraintsToString(List<Constraint> excludeConstraints) {
        List<String[]> constraintStrings = new ArrayList<>();

        List<List<Constraint>> cs = new ArrayList<>();

        // pa1 & !pa2 & !pa3 | !pa1 & pa2 & pa3 | !pa1 & !pa2 & pa3
        for(Constraint constraint : excludeConstraints) {
            Constraint c = findAndClause(constraint);
        }

        return constraintStrings;
    }

    public Constraint findAndClause(Constraint constraint) {
        /*
        OrConstraint c = (OrConstraint) constraint;
        List<OrConstraint> left = c.getConstraintSubParts();
        while(left.get(0).getConstraintSubParts().size() > 1) {
            left = left.get(0).getConstraintSubParts();
        }

         */

        return null;
    }


}
