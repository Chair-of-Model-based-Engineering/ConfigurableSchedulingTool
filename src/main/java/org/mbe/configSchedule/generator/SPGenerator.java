package org.mbe.configSchedule.generator;

import org.mbe.configSchedule.util.Machine;
import org.mbe.configSchedule.util.SchedulingProblem;
import org.mbe.configSchedule.util.Task;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.lang.Math.floor;

public class SPGenerator {

    public void generateProblem(int jobCount, int taskCount,
                                             int durationOutlierCount, int machineCount, int optionalCount,
                                             int altCount, int altGroupCount, int deadline, int durationConstraints, int maxDurationRequires, String name) throws IOException {
        List<List<Task>> jobs = new ArrayList<>();
        List<Machine> machines = new ArrayList<>();

        // Werden genutzt um dir Duration-Constraints zu erstellen
        // Bei einer mandatory Task mit einer Variablen Duration, können von einer Duration eine oder mehr
        // req. Constraints zu optionalen Tasks abgehen
        // Damit von der selben Duration aber nicht mehrere Constraints zu verschiedenen Tasks in einer alternativ-,
        // Gruppe gehen, werden alternativ-Gruppen als Task-Array gespeichert und nur eine Task in diesem Array kann
        // einer Constraint angehören
        List<Task[]> optionalTasks = new ArrayList<>();
        List<Task> mandatoryTasksWithVarDuration = new ArrayList<>();

        Random random = new Random();

        if(jobCount > taskCount) {
            System.out.println("Bitte mehr Tasks als Jobs eingeben");
        }

        // =================================
        // Maschinen erstellen
        // =================================
        int machineID = 1;
        for(int i = 0; i < machineCount; i++) {
            Machine machine = new Machine("m" + machineID, false);
            machineID++;
            machines.add(machine);
        }


        // ==================================
        // Standard Jobs erstellen
        // ==================================
        double tasksPerJobD = taskCount / jobCount;
        int tasksPerJob = (int) floor(tasksPerJobD);
        int restTasks = taskCount % tasksPerJob;
        int tasksInJob;

        boolean firstJob = true;
        int taskID = 1;

        for(int i = 0; i < jobCount; i++) {
            List<Task> job = new ArrayList<>();

            // Falls es der erste Job ist, sollen die überschüssigen Tasks in ihm sein
            if(firstJob) {
                tasksInJob = tasksPerJob + restTasks;
                firstJob = false;
            } else {
                tasksInJob = tasksPerJob;
            }

            for(int j = 0; j < tasksInJob; j++) {

                Task task = new Task();
                task.setName("p" + taskID);
                taskID++;
                task.setMachine(machines.get(random.nextInt(machineCount)));
                task.setDuration(new int[2]);

                // Wenn mehr durationOutlier übrig sind (oder so viele) wie noch zu erstellende Tasks gibt,
                // soll die Task auf jeden Fall ein Outlier sein
                if (((taskCount - taskID) == durationOutlierCount) && (durationOutlierCount > 0)){
                    // Soll es eine variable Dauer sein?
                    int variableDurationChance  = random.nextInt(2);
                    // 0 = Variable Dauer, 1 = feste Dauer
                    if(variableDurationChance == 0) {
                        // Zwischen 6 und 15
                        int dur1 = random.nextInt(10) + 6;
                        int dur2 = random.nextInt(10) + 6;
                        int[] duration = {dur1, dur2};
                        task.setDuration(duration);

                        // Task zu den mandatory Tasks hinzufügen für Duration Constraints
                        mandatoryTasksWithVarDuration.add(task);
                    } else {
                        int dur = random.nextInt(10) + 6;
                        int[] duration = {dur, dur};
                        task.setDuration(duration);
                    }
                    durationOutlierCount--;

                } else {
                    // Trotzdem noch die Chance ein Outlier zu werden
                    int outLierChance = random.nextInt(2);
                    // 0 = Outlier, 1 = Kein Outlier
                    if(outLierChance == 0 && durationOutlierCount > 0) {
                        // Soll es eine variable Dauer sein?
                        int variableDurationChance  = random.nextInt(2);
                        // 0 = Variable Dauer, 1 = feste Dauer
                        if(variableDurationChance == 0) {
                            // Zwischen 6 und 15
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

                        // Kein Outlier
                    } else {
                        // Soll es eine variable Dauer sein?
                        int variableDurationChance  = random.nextInt(2);
                        // 0 = Variable Dauer, 1 = feste Dauer
                        if(variableDurationChance == 0) {
                            // Zwischen 1 und 5
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

        // ===============================
        // Optionale Task-Jobs erstellen
        // ===============================
        for(int i = 0; i < optionalCount; i++) {
            List<Task> job = new ArrayList<>();
            Task task = new Task();

            task.setName("p" + taskID);
            taskID++;
            task.setMachine(machines.get(random.nextInt(machineCount)));
            task.setOptional(true);
            task.setDuration(new int[2]);

            int varDurationChance = random.nextInt(2);

            if(varDurationChance == 0) {
                // Zwischen 1 und 5
                int dur1 = random.nextInt(5) + 1;
                int dur2 = random.nextInt(5) + 1;
                int[] duration = {dur1, dur2};
                task.setDuration(duration);
            } else {
                // Nicht variabel
                int dur = random.nextInt(5) + 1;
                int[] duration = {dur, dur};
                task.setDuration(duration);
            }
            Task[] taskArr = {task};
            optionalTasks.add(taskArr);

            job.add(task);
            jobs.add(job);
        }

        // ===================================
        // Alternative Tasks erstellen
        // ===================================
        double tasksPerGroupD = altCount / altGroupCount;
        int tasksPerGroup = (int) floor(tasksPerGroupD);
        int restTasksAlt = altCount % tasksPerGroup;
        int tasksInGroup;

        boolean firstGroup = true;

        for(int i = 0; i < altGroupCount; i++) {

            // Wenn es die erste alt Gruppe ist, soll sie die überschüssigen Tasks enthalten
            if(firstGroup) {
                tasksInGroup = tasksPerGroup + restTasksAlt;
                firstGroup = false;
            } else {
                tasksInGroup = tasksPerGroup;
            }

            Task[] group = new Task[tasksInGroup];

            for(int j = 0; j < tasksInGroup; j++) {
                List<Task> job = new ArrayList<>();
                Task task = new Task();
                task.setName("p" + taskID);
                taskID++;
                task.setMachine(machines.get(random.nextInt(machineCount)));
                task.setDuration(new int[2]);

                // Soll es eine variable Dauer sein?
                int variableDurationChance  = random.nextInt(2);
                // 0 = Variable Dauer, 1 = feste Dauer
                if(variableDurationChance == 0) {
                    // Zwischen 1 und 5
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

                job.add(task);
                jobs.add(job);
            }

            optionalTasks.add(group);

            // Jeder Task in der Gruppe die anderen Tasks in excludeTasks Liste packen
            for(int j = 0; j < group.length; j++) {
                List<String> list = group[j].getExcludeTasks();

                for(int k = 0; k < group.length; k++) {
                    if(j != k) {
                        list.add(group[k].getName());
                    }
                }
            }
        }

        // ===================================
        // Duration Constraints
        // ===================================

        Random rand = new Random();
        List<Integer> usedMandIndices = new ArrayList<>();
        List<Integer> usedOptIndices  = new ArrayList<>();

        if(maxDurationRequires > optionalTasks.size()) {
            maxDurationRequires = optionalTasks.size();
        }

        for(int i = 0; i < durationConstraints; i++) {
            int mandTaskIndex = -1;
            while(usedMandIndices.contains(mandTaskIndex) || mandTaskIndex == -1) {
                mandTaskIndex = rand.nextInt(mandatoryTasksWithVarDuration.size());
            }
            usedMandIndices.add(mandTaskIndex);

            Task task = mandatoryTasksWithVarDuration.get(mandTaskIndex);

            int duration = random.nextInt(task.getDuration()[1] - task.getDuration()[0]) + task.getDuration()[0];
            List<Task> requiredTasks = new ArrayList<>();

            int constraintCount = random.nextInt(maxDurationRequires) + 1;

            for(int j = 0; j < constraintCount; j++) {
                int optTaskIndex = -1;
                while(usedOptIndices.contains(optTaskIndex) || optTaskIndex == -1) {
                    optTaskIndex  = rand.nextInt(optionalTasks.size());
                }
                usedOptIndices.add(optTaskIndex);

                Task[] taskGroup = optionalTasks.get(optTaskIndex);
                int taskInGroupIndex = rand.nextInt(taskGroup.length);

                Task optionalTask = taskGroup[taskInGroupIndex];

                requiredTasks.add(optionalTask);
            }

            task.addDurationCon(duration, requiredTasks);
        }



        SchedulingProblem sp = new SchedulingProblem(jobs, machines, deadline);

        //String fileOutputString = "src/main/probleme/" + name + ".txt";
        String fileOutputString = "/home/max/Schreibtisch/Generierte_Probleme/" + name + ".txt";
        FileOutputStream fOut = new FileOutputStream(fileOutputString);
        ObjectOutputStream oOut = new ObjectOutputStream(fOut);

        oOut.writeObject(sp);
        oOut.flush();
        oOut.close();

        //return new SchedulingProblem(jobs, machines, deadline);
    }
}
