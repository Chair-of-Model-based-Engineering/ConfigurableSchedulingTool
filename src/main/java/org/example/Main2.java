package org.example;

import com.google.ortools.Loader;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.IntervalVar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main2 {
    public static void main(String[] args) {
        Loader.loadNativeLibraries();

        CpModel model = new CpModel();
        int maxduration = 10;

        MandatoryTask p1 = new MandatoryTask();
        p1.start = model.newIntVar(0, maxduration, "p1_start");
        p1.end = model.newIntVar(0, maxduration, "p1_end");
        p1.name = "p1";
        p1.machine = 1;
        p1.duration = model.newIntVar(2, 2, "p1_duration");
        p1.interval = model.newIntervalVar(p1.start, p1.duration, p1.end, "p1_interval");

        MandatoryTask p2 = new MandatoryTask();
        p2.start = model.newIntVar(0, maxduration, "p2_start");
        p2.end = model.newIntVar(0, maxduration, "p2_end");
        p2.name = "p2";
        p2.machine = 1;
        p2.duration = model.newIntVar(2, 4, "p2_duration");
        p2.interval = model.newIntervalVar(p2.start, p2.duration, p2.end, "p2_interval");

        MandatoryTask p3 = new MandatoryTask();
        p3.start = model.newIntVar(0, maxduration, "p3_start");
        p3.end = model.newIntVar(0, maxduration, "p3_end");
        p3.name = "p3";
        p3.machine = 1;
        p3.duration = model.newIntVar(2, 3, "p3_duration");
        p3.interval = model.newIntervalVar(p3.start, p3.duration, p3.end, "p3_interval");

        OptionalTask p4 = new OptionalTask();
        p4.start = model.newIntVar(0, maxduration, "p4_start");
        p4.end = model.newIntVar(0, maxduration, "p4_end");
        p4.name = "p4";
        p4.machine = 1;
        p4.duration = model.newIntVar(1, 1, "p4_duration");
        p4.interval = model.newIntervalVar(p4.start, p4.duration, p4.end, "p4_interval");
        p4.status = model.newBoolVar("p4_status");
        
        // Dass p1 vor p2 beendet wird, etc.
        model.addLessThan(p1.end, p2.start);
        model.addLessThan(p2.end, p3.start);
        model.addLessThan(p3.end, p4.start);

        // Job erstellen
        List<FeatureTask> job = new ArrayList<>();
        List<IntervalVar> jobIntervalle = new ArrayList<>();
        job.add(p1);
        job.add(p2);
        job.add(p3);
        job.add(p4);

        // In einem Job dürfen sich die Intervalle (Tasks) nicht überlappen
        jobIntervalle.add(p1.interval);
        jobIntervalle.add(p2.interval);
        jobIntervalle.add(p3.interval);
        jobIntervalle.add(p4.interval);
        model.addNoOverlap(jobIntervalle);

        // Array mit Maschinen
        int[] machines = {1};

        // Jede Maschine hat eine Liste mit Intervallen, die sich am Ende nicht überlappen dürfen
        // Intervalle = Ausführungen der Tasks
        Map<Integer, List<IntervalVar>> intervalsOnMachines = new HashMap<>();

        // Für jede Maschine wird eine Liste mit Intervallen erstellt (die die Tasks repräsentieren)
        // Die Intervalle (Tasks) dürfen sich nicht überlappen
        for(int machine : machines) {
            List<IntervalVar> intervals = new ArrayList<>();
            for (FeatureTask task : job) {
                intervals.add(model.newIntervalVar(task.start, task.duration, task.end, task.name + "intervalOnMachine"));
            }
            model.addNoOverlap(intervals);
            intervalsOnMachines.put(machine, intervals);
        }




    }

    public void ReadFM() {

    }
}
