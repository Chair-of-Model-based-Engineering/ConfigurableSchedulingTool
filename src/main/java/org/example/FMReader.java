package org.example;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class FMReader {

    List<List<Task>> jobs = new ArrayList<>();
    List<Machine> machines = new ArrayList<>();

    public FMReader() {
    }

    public static void main(String[] args) throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
        FMReader reader = new FMReader();
        reader.ReadFM();

    }
    public void ReadFM() throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        String modellpfad = "src/main/modelle/FeatureModell";

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document modellDoc = builder.parse(new File(modellpfad));

        XPath xPath = XPathFactory.newInstance().newXPath();
        String expressionP = "//and[@name=\"P\"]";
        String expressionM = "//and[@name=\"M\"]";
        String expressionCon = "//imp";

        Node m = (Node) xPath.compile(expressionM).evaluate(modellDoc, XPathConstants.NODE);
        Node p = (Node) xPath.compile(expressionP).evaluate(modellDoc, XPathConstants.NODE);
        NodeList constraints = (NodeList) xPath.compile(expressionCon).evaluate(modellDoc, XPathConstants.NODESET);

        List<Task> allTasks = new ArrayList<>();
        Map<String, Task> taskNameMap = new HashMap<>();
        Map<String, Machine> machineNameMap = new HashMap<>();

        // ======================================================================
        // Maschinen
        // ======================================================================
        NodeList machineNodes = m.getChildNodes();

        int id = 0;

        for(int i = 0; i < machineNodes.getLength(); i++) {
            System.out.println("Childnode " + i);
            if(machineNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                if(machineNodes.item(i).getAttributes().getLength() == 2) {
                    Machine machine = new Machine(id, false);
                    id++;
                    machineNameMap.put(machineNodes.item(i).getAttributes().item(1).getNodeValue(), machine);
                    machines.add(machine);
                } else {
                    Machine machine = new Machine(id, true);
                    id++;
                    machineNameMap.put(machineNodes.item(i).getAttributes().item(0).getNodeValue(), machine);
                    machines.add(machine);
                }
            }
        }
        System.out.println(machineNameMap);

        // ======================================================================
        // Tasks
        // ======================================================================

        NodeList taskNodes = p.getChildNodes();

        for(int i = 0; i < taskNodes.getLength(); i++) {
            Node currentNode = taskNodes.item(i);
            if(currentNode.getNodeType() == Node.ELEMENT_NODE) {
                System.out.println("Task");
                Task task = new Task();
                if(currentNode.getAttributes().getLength() == 2) {
                    task.name = currentNode.getAttributes().item(1).getNodeValue();
                    task.optional = false;
                } else {
                    task.name = currentNode.getAttributes().item(0).getNodeValue();
                    task.optional = true;
                }

                // Duration für Task
                int[] durationsArr = new int[2];
                NodeList durations = currentNode.getChildNodes();
                int durationsArrIndex = 0;
                for(int j = 0; j < durations.getLength(); j++) {
                    Node currentDuration = durations.item(j);
                    if(currentDuration.getNodeType() == Node.ELEMENT_NODE) {
                        if(currentDuration.getAttributes().getLength() == 2) {
                            String durationString = currentDuration.getAttributes().item(1).getNodeValue();
                            String[] subStrings = durationString.split(" ");
                            durationsArr[0] = Integer.parseInt(subStrings[2]);
                            durationsArr[1] = Integer.parseInt(subStrings[2]);
                        } else if(currentDuration.getAttributes().getLength() == 1) {
                            String durationString = currentDuration.getAttributes().item(0).getNodeValue();
                            String[] subStrings = durationString.split(" ");
                            durationsArr[durationsArrIndex] = Integer.parseInt(subStrings[2]);
                            durationsArrIndex++;
                        }
                    }
                }

                Arrays.sort(durationsArr);
                task.duration = durationsArr;
                taskNameMap.put(task.name, task);
                allTasks.add(task);
            }
        }

        for(Task task : allTasks) {
            System.out.println(task.name + "   " + task.optional + "   " + task.duration[0] + " " + task.duration[1]);
        }

        // =============================================================
        // Req. Constraints
        // ==============================================================

        for(int i = 0; i < constraints.getLength(); i++) {
            Node currentConstraint = constraints.item(i);
            NodeList constraintPair = currentConstraint.getChildNodes();

            String[] constraintPairArr = new String[2];

            int index = 0;
            for(int j = 0; j < constraintPair.getLength(); j++) {
                if(constraintPair.item(j).getNodeType() == Node.TEXT_NODE) {
                    constraintPairArr[index] = constraintPair.item(j).getTextContent();
                }
            }

            System.out.print("\n" + "Constraint " + i + ":  ");
            for(String s : constraintPairArr) {
                System.out.println(s + " ");
            }
        }
    }
}
