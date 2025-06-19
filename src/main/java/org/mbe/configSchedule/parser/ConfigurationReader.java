package org.mbe.configSchedule.parser;

import de.vill.model.FeatureModel;
import org.mbe.configSchedule.util.Machine;
import org.mbe.configSchedule.util.SchedulingProblem;
import org.mbe.configSchedule.util.Task;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class ConfigurationReader {
    List<List<Task>> jobs = new ArrayList<>();
    List<Machine> machines = new ArrayList<>();
    int deadline;

    /**
     * Creates a scheduling problem from an xml-configuration-file (and from the model-file)
     *
     * @param configPath The file-path to the configuration-file
     * @param modelPath  The file-path to the model-file
     * @return A SchedulingProblem-object
     * @throws IOException                  if the model or configuration file cannot be read.
     * @throws ParserConfigurationException if no fitting {@link DocumentBuilder} can be found.
     * @throws SAXException                 if the configuration XML cannot be parsed.
     */
    public SchedulingProblem ReadConfig(String configPath, String modelPath) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        this.jobs.clear();
        this.machines.clear();
        this.deadline = -1;

        FeatureModel featureModel = UVLReader.read(Path.of(modelPath));
        SchedulingProblem schedulingProblem = SchedulingProblem.fromFeatureModel(featureModel);

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document configDoc = builder.parse(new File(configPath));

        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList selectedFeatureNameEntries = (NodeList) xPath.compile("/configuration/feature[@manual='selected']/@name").evaluate(configDoc, XPathConstants.NODESET);

        List<String> selectedFeatureNames = new ArrayList<>();
        Map<String, Integer> selectedDurations = new HashMap<>();
        for (int i = 0; i < selectedFeatureNameEntries.getLength(); i++) {
            String value = selectedFeatureNameEntries.item(i).getNodeValue();
            if (value == null || value.equals("P") || value.equals("M") || value.equals(schedulingProblem.getName())) {
                //noinspection UnnecessaryContinue
                continue;
            } else if (value.startsWith("d") && value.contains(" = ")) {
                String[] parts = value.split(" = ");
                String taskName = parts[0];
                int duration = Integer.parseInt(parts[1]);
                selectedDurations.put(taskName, duration);
            } else {
                selectedFeatureNames.add(value);
            }
        }

        for (Task task : schedulingProblem.getTasks()) {
            if (selectedFeatureNames.contains(task.getName()))
                task.setOptional(false);
            if (selectedDurations.containsKey(task.getName()))
                task.setDurations(new int[] {selectedDurations.get(task.getName())});
        }

        return schedulingProblem;
    }
}
