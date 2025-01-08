package org.mbe.configSchedule.solver;

import com.google.ortools.constraintsolver.Solver;
import com.google.ortools.sat.CpSolverStatus;
import org.mbe.configSchedule.parser.ConfigurationReader;
import org.mbe.configSchedule.util.ConfigurationSolverReturn;
import org.mbe.configSchedule.util.SchedulingProblem;
import org.mbe.configSchedule.util.SolverReturn;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

public class ConfigurationSolver {
    public static ConfigurationSolverReturn getFirst(String configDirectoryPath, String modelPath) throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
        File directory = new File(configDirectoryPath);
        File[] directoryFiles = directory.listFiles();

        ConfigurationReader cReader = new ConfigurationReader();

        // mode = 0 -> feasible Schedule
        if (directoryFiles != null) {

            int iteration = 1;
            long sumTimeRead = 0;
            long sumTimeSolve = 0;

            for (File file : directoryFiles) {
                String filePath = file.getPath();

                Instant readStart = Instant.now();
                SchedulingProblem sp = cReader.ReadConfig(filePath, modelPath);
                Instant readEnd = Instant.now();

                Instant solveStart = Instant.now();

                ProblemSolver problemSolver = new ProblemSolver(sp);
                SolverReturn sr = problemSolver.getFirstSolution();

                Instant solveEnd = Instant.now();

                long readTime = Duration.between(readStart, readEnd).toMillis();
                long solveTime = Duration.between(solveStart, solveEnd).toMillis();
                sumTimeRead += readTime;
                sumTimeSolve += solveTime;

                if (sr != null && (sr.getStatus() == CpSolverStatus.OPTIMAL || sr.getStatus() == CpSolverStatus.FEASIBLE)) {
                    ConfigurationSolverReturn csr = new ConfigurationSolverReturn(true, sr, sumTimeRead, sumTimeSolve, sumTimeRead + sumTimeSolve, iteration, iteration);
                    return csr;
                }
                iteration++;
            }
            ConfigurationSolverReturn csr = new ConfigurationSolverReturn(false, null, sumTimeRead, sumTimeSolve, sumTimeRead + sumTimeSolve, iteration, iteration);
            return csr;
        }
        return null;
    }

    public static ConfigurationSolverReturn getBest(String configDirectoryPath, String modelPath) throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
        File directory = new File(configDirectoryPath);
        File[] directoryFiles = directory.listFiles();

        ConfigurationReader cReader = new ConfigurationReader();
        if (directoryFiles != null) {

            int iteration = 1;
            Double bestResultTime = Double.POSITIVE_INFINITY;
            SolverReturn bestResult = new SolverReturn();
            long sumTimeRead = 0;
            long sumTimeSolve = 0;
            int bestIteration = -1;

            for (File file : directoryFiles) {
                String filePath = file.getPath();

                Instant readStart = Instant.now();
                SchedulingProblem sp = cReader.ReadConfig(filePath, modelPath);
                Instant readEnd = Instant.now();

                Instant solveStart = Instant.now();

                ProblemSolver problemSolver = new ProblemSolver(sp);
                SolverReturn sr = problemSolver.getBestSolution();

                Instant solveEnd = Instant.now();

                if (sr != null && (sr.getStatus() == CpSolverStatus.OPTIMAL && sr.getTime() < bestResultTime)) {
                    bestResultTime = sr.getTime();
                    bestResult = sr;
                    bestIteration = iteration;
                }
                iteration++;

                long readTime = Duration.between(readStart, readEnd).toMillis();
                long solveTime = Duration.between(solveStart, solveEnd).toMillis();
                sumTimeRead += readTime;
                sumTimeSolve += solveTime;
            }

            if (bestIteration != -1) {
                ConfigurationSolverReturn csr = new ConfigurationSolverReturn(true, bestResult, sumTimeRead, sumTimeSolve, sumTimeRead + sumTimeSolve, bestIteration, iteration);
                return csr;
            } else {
                ConfigurationSolverReturn csr = new ConfigurationSolverReturn(false, bestResult, sumTimeRead, sumTimeSolve, sumTimeRead + sumTimeSolve, bestIteration, iteration);
                return csr;
            }
        }
        return null;
    }
}
