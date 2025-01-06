package org.mbe.configSchedule.util;

import java.util.prefs.Preferences;

public class PathPreferences {

    Preferences prefs = Preferences.userRoot().node("prefs/paths");

    public void setSolutionSavePath(String path) {
        prefs.put("solutionSave", path);
    }

    public String getSolutionSavePath() {
        return prefs.get("solutionSave", "");
    }

    public void setProblemSavePath(String path) {
        prefs.put("problemSave", path);
    }

    public String getProblemSavePath() {
        return prefs.get("problemSave", "");
    }
}
