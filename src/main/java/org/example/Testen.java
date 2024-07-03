package org.example;

import com.google.ortools.Loader;
import com.google.ortools.sat.*;

public class Testen {

    public static void main(String[] args) throws Exception {
        Loader.loadNativeLibraries();

        CpModel model = new CpModel();

        IntVar x = model.newIntVar(0, 3, "x");
        IntVar y = model.newIntVar(0, 3, "y");

        model.addDifferent(x,y);
        model.addLessOrEqual(LinearExpr.weightedSum(new IntVar[] {x,y}, new long[] {2,7}), 30);

        CpSolver solver = new CpSolver();
        CpSolverStatus status = solver.solve(model);

        if(status == CpSolverStatus.OPTIMAL) {
            System.out.print("Optimale Lösung");
        } else if(status == CpSolverStatus.FEASIBLE) {
            System.out.println("Mögliche Lösung");
        } else {
            System.out.println("Keine Lösung");
        }
    }
}
