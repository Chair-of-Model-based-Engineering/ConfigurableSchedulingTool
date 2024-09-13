package org.mbe.configSchedule.util;

public class ConfigurationSolverReturn {

    private boolean hasSolution;
    private SolverReturn solverReturn;
    private long readTime;
    private long timeSolve;
    private long neededTime;
    private int iteration;
    private int searchedConfigs;


    public ConfigurationSolverReturn(boolean hasSolution, SolverReturn solverReturn, long neededTimeRead, long neededTimeSolve, long neededTime, int iteration, int searchedConfigs) {
        this.hasSolution = hasSolution;
        this.solverReturn = solverReturn;
        this.readTime = neededTimeRead;
        this.timeSolve = neededTimeSolve;
        this.neededTime = neededTime;
        this.iteration = iteration;
        this.searchedConfigs = searchedConfigs;
    }

    public boolean isHasSolution() {
        return hasSolution;
    }

    public void setHasSolution(boolean hasSolution) {
        this.hasSolution = hasSolution;
    }

    public SolverReturn getSolverReturn() {
        return solverReturn;
    }

    public void setSolverReturn(SolverReturn solverReturn) {
        this.solverReturn = solverReturn;
    }

    public long getReadTime() {
        return readTime;
    }

    public void setReadTime(long readTime) {
        this.readTime = readTime;
    }

    public long getTimeSolve() {
        return timeSolve;
    }

    public void setTimeSolve(long timeSolve) {
        this.timeSolve = timeSolve;
    }

    public long getNeededTime() {
        return neededTime;
    }

    public void setNeededTime(long neededTime) {
        this.neededTime = neededTime;
    }

    public int getIteration() {
        return iteration;
    }

    public void setIteration(int iteration) {
        this.iteration = iteration;
    }

    public int getSearchedConfigs() {
        return searchedConfigs;
    }

    public void setSearchedConfigs(int searchedConfigs) {
        this.searchedConfigs = searchedConfigs;
    }
}
