package org.mbe.configSchedule.util;

public class ConfigurationSolverReturn {

    private boolean hasSolution;
    private SolverReturn solverReturn;
    private long readTime;
    private long timeSolve;
    private long neededTime;
    private int iteration;
    private int searchedConfigs;

    /**
     * Create a new object of type ConfiguraitonSoverReturen.
     *
     * @param hasSolution     true if the solver found a solution.
     * @param solverReturn    solution determined by the solver.
     * @param neededTimeRead  time spend reading data.
     * @param neededTimeSolve time spend solving the problem.
     * @param neededTime      overall time (read + solve).
     * @param iteration       index of configuration for which the solution is returned.
     * @param searchedConfigs number of configurations analyzed overall.
     */
    public ConfigurationSolverReturn(boolean hasSolution, SolverReturn solverReturn, long neededTimeRead, long neededTimeSolve, long neededTime, int iteration, int searchedConfigs) {
        this.hasSolution = hasSolution;
        this.solverReturn = solverReturn;
        this.readTime = neededTimeRead;
        this.timeSolve = neededTimeSolve;
        this.neededTime = neededTime;
        this.iteration = iteration;
        this.searchedConfigs = searchedConfigs;
    }

    /**
     * Returns whether a solution was found.
     *
     * @return true if a soultion was found.
     */
    public boolean isHasSolution() {
        return hasSolution;
    }

    /**
     * Sets value of hasSolution.
     *
     * @param hasSolution new boolean value.
     */
    public void setHasSolution(boolean hasSolution) {
        this.hasSolution = hasSolution;
    }

    /**
     * Return output from Solver.
     *
     * @return Object of type SolverReturn.
     */
    public SolverReturn getSolverReturn() {
        return solverReturn;
    }

    /**
     * Sets value of solverReturn.
     *
     * @param solverReturn new object of type SolverReturn.
     */
    public void setSolverReturn(SolverReturn solverReturn) {
        this.solverReturn = solverReturn;
    }

    /**
     * Get time spend to read data.
     *
     * @return time in ms.
     */
    public long getReadTime() {
        return readTime;
    }

    /**
     * Sets time spend to read data.
     *
     * @param readTime new time in ms.
     */
    public void setReadTime(long readTime) {
        this.readTime = readTime;
    }

    /**
     * Get time spend to solve Problem.
     *
     * @return time in ms.
     */
    public long getTimeSolve() {
        return timeSolve;
    }

    /**
     * Sets tome spend to solve Problem.
     *
     * @param timeSolve time in ms.
     */
    public void setTimeSolve(long timeSolve) {
        this.timeSolve = timeSolve;
    }

    /**
     * Get time spend overall.
     *
     * @return time in ms.
     */
    public long getNeededTime() {
        return neededTime;
    }

    /**
     * Set time spend overall.
     *
     * @param neededTime time in ms.
     */
    public void setNeededTime(long neededTime) {
        this.neededTime = neededTime;
    }

    /**
     * Get index of iteration in which a solution was found.
     *
     * @return Index of configuration.
     */
    public int getIteration() {
        return iteration;
    }

    /**
     * Set index of iteration in which a solution was found.
     *
     * @param iteration Index of configuration.
     */
    public void setIteration(int iteration) {
        this.iteration = iteration;
    }

    /**
     * Get number of overall analysed configuraitons.
     *
     * @return number of configurations.
     */
    public int getSearchedConfigs() {
        return searchedConfigs;
    }

    /**
     * Set number of configurations analysed.
     *
     * @param searchedConfigs number of configurations.
     */
    public void setSearchedConfigs(int searchedConfigs) {
        this.searchedConfigs = searchedConfigs;
    }
}
