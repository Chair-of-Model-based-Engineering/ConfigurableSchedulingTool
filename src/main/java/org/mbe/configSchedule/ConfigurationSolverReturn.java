package org.example;

public class ConfigurationSolverReturn {

    boolean hasSolution;
    SolverReturn solverReturn;
    long readTime;
    long timeSolve;
    long neededTime;
    int iteration;
    int searchedConfigs;


    public ConfigurationSolverReturn(boolean hasSolution, SolverReturn solverReturn, long neededTimeRead, long neededTimeSolve, long neededTime, int iteration, int searchedConfigs) {
        this.hasSolution = hasSolution;
        this.solverReturn = solverReturn;
        this.readTime = neededTimeRead;
        this.timeSolve = neededTimeSolve;
        this.neededTime = neededTime;
        this.iteration = iteration;
        this.searchedConfigs = searchedConfigs;
    }
}
