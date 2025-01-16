package org.mbe.configSchedule.util;

import java.util.prefs.Preferences;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathPreferences {

    String workingDir = System.getProperty("user.dir");
    Path relativeProblemPath = Paths.get(workingDir,"problems");
    Path relativeSchedulePath = Paths.get(workingDir, "schedules");

    Path absoluteProblemPath = relativeProblemPath.toAbsolutePath().resolve("");
    Path absoluteSchedulePath = relativeSchedulePath.toAbsolutePath().resolve("");

    /**
     * Holds Path Preferences
     */
    Preferences prefs = Preferences.userRoot().node("prefs/paths");

    /**
     * Set path preference for saving solutions.
     *
     * @param path new path preference.
     */
    public void setSolutionSavePath(String path) {
        prefs.put("solutionSave", path);
    }

    /**
     * Get path preference for saving solutions.
     *
     * @return path preference.
     */
    public String getSolutionSavePath() {
        return prefs.get("solutionSave", absoluteSchedulePath.toString());
    }

    /**
     * Set path preference for problems.
     *
     * @param path new path preference.
     */
    public void setProblemSavePath(String path) {
        prefs.put("problemSave", path);
    }

    /**
     * Get path preference for problems.
     *
     * @return path preference.
     */
    public String getProblemSavePath() {
        return prefs.get("problemSave", absoluteProblemPath.toString());
    }

    public void removeSolutionSavePath() {
        prefs.remove("solutionSave");
    }

    public void removeProblemSavePath() {
        prefs.remove("problemSave");
    }
}
