package org.mbe.configschedule.parser;


import de.vill.main.UVLModelFactory;
import de.vill.model.FeatureModel;
import org.mbe.configschedule.util.SchedulingProblem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UVLReader {
    public static FeatureModel read(String path) throws IOException {
        Path filePath = Paths.get(path);
        String content = new String(Files.readAllBytes(filePath));
        UVLModelFactory uvlModelFactory = new UVLModelFactory();
        return uvlModelFactory.parse(content);
    }
    public static void write(String path, FeatureModel featureModel) throws IOException {
        String uvlModel = featureModel.toString();
        Path filePath = Paths.get(featureModel.getNamespace() + ".uvl");
        Files.write(filePath, uvlModel.getBytes());
    }
}
