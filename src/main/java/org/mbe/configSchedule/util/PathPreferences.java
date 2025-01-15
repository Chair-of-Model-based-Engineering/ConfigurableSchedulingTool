package org.mbe.configSchedule.util;

import java.util.prefs.Preferences;

public class PathPreferences {

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
        return prefs.get("solutionSave", "");
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
        return prefs.get("problemSave", "");
    }
}
