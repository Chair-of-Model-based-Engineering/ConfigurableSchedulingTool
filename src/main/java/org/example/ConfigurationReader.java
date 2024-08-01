package org.example;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
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
import java.util.*;

public class ConfigurationReader {

    List<List<Task>> jobs = new ArrayList<>();
    List<Machine> machines = new ArrayList<>();
    int deadline;

    Map<String, Task> nameToTask = new HashMap<>();
    Map<String, Machine> nameToMachine = new HashMap<>();

    ConfigurationReader() {};

    public static void main(String[] args) throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
        String configPath = "src/main/modelle/J2_T10_M2_O2_A4Configs/01487.xml";
        String modelPath = "src/main/modelle/J2_T10_M2_O2_A4.xml";
        ConfigurationReader reader = new ConfigurationReader();
        reader.ReadConfig(configPath, modelPath);


        System.out.println("Jobs: ");
        for(List<Task> job : reader.jobs) {
            System.out.print("Job: ");
            for(Task task : job) {
                System.out.print(task.name + ", M: " + task.machine + ", D: [" + task.duration[0] + "," + task.duration[1] + "], " + "O: " + task.optional + "E: " + task.excludeTasks.size() + " | ");
            }
            System.out.print("\n");
        }

        System.out.println("Machines: ");
        for(Machine machine : reader.machines) {
            System.out.print(machine.id + " " + machine.name + " " + machine.optional + " | ");
        }

    }

    public void ReadConfig(String configPath, String modelPath) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document modelDoc = builder.parse(new File(modelPath));
        Document configDoc = builder.parse(new File(configPath));

        XPath xPath = XPathFactory.newInstance().newXPath();

        String exprRoot = "//description";
        String exprOrder = "//imp";
        String exprTask = "//feature[starts-with(@name, 'p')]";
        String exprMachine = "//feature[starts-with(@name, 'm')]";
        String exprDuration = "//feature[starts-with(@name, 'd')]";

        Node rootDescription = (Node) xPath.compile(exprRoot).evaluate(modelDoc, XPathConstants.NODE);
        NodeList orderNodes = (NodeList) xPath.compile(exprOrder).evaluate(modelDoc, XPathConstants.NODESET);
        NodeList taskNodes = (NodeList) xPath.compile(exprTask).evaluate(configDoc, XPathConstants.NODESET);
        NodeList machineNodes = (NodeList) xPath.compile(exprMachine).evaluate(configDoc, XPathConstants.NODESET);
        NodeList durationNodes = (NodeList) xPath.compile(exprDuration).evaluate(configDoc, XPathConstants.NODESET);

        ReadDeadline(rootDescription);
        CreateTasks(taskNodes);
        ReadDurations(durationNodes);
        CreateMachines(machineNodes);
        CreateJobs(orderNodes);
    }

    private void ReadDeadline(Node root) {
        String deadlineString;
        deadlineString = root.getTextContent();
        try {
            deadline = Integer.parseInt(deadlineString);
        } catch (NumberFormatException e) {
            System.out.println("Deadline konnte nicht konvertiet werden, überprüfe Description von root");
        }
    }

    private void CreateTasks(NodeList taskNodes) {
        for (int i = 0; i < taskNodes.getLength(); i++) {
            Node taskNode = taskNodes.item(i);

            if(taskNode.getNodeType() == Node.ELEMENT_NODE && taskNode.getAttributes().item(0).getNodeValue().equals("selected")) {
                Task task = new Task();
                task.name = taskNode.getAttributes().getNamedItem("name").getNodeValue();
                task.optional = false;
                task.excludeTasks = new ArrayList<>();

                nameToTask.put(task.name, task);
            }
        }
    }

    private void ReadDurations(NodeList durationNodes) {
        for(int i = 0; i < durationNodes.getLength(); i++) {
            Node durationNode = durationNodes.item(i);

            if(durationNode.getNodeType() == Node.ELEMENT_NODE && durationNode.getAttributes().item(0).getNodeValue().equals("selected")) {
                String durationString = durationNode.getAttributes().getNamedItem("name").getNodeValue();
                String[] subStrings = durationString.split(" ");
                int[] durationsArr = new int[2];
                durationsArr[0] = Integer.parseInt(subStrings[2]);
                durationsArr[1] = Integer.parseInt(subStrings[2]);

                String nameString = subStrings[0].substring(1);
                Task task = nameToTask.get(nameString);
                task.duration = durationsArr;
            }
        }
    }

    public void CreateMachines(NodeList machineNodes) {
        int amountMachines = 0;
        for(int i = 0; i < machineNodes.getLength(); i++) {
            Node currentNode = machineNodes.item(i);

            if(currentNode.getNodeType() == Node.ELEMENT_NODE && currentNode.getAttributes().item(0).getNodeValue().equals("selected")) {
                Machine machine = new Machine(amountMachines, false);
                machine.name = currentNode.getAttributes().getNamedItem("name").getNodeValue();
                machine.id = amountMachines;
                machine.optional = false;
                amountMachines++;

                nameToMachine.put(machine.name, machine);
                machines.add(machine);
            }
        }
    }

    public void CreateJobs(NodeList orderNodes) {
        List<String[]> orderPairs = new ArrayList<>();
        for(int i = 0; i < orderNodes.getLength(); i++) {
            Node impNode = orderNodes.item(i);
            NodeList impNodeChilds = impNode.getChildNodes();

            String[] constrPair = new String[2];
            int index = 0;

            for(int j = 0; j < impNodeChilds.getLength(); j++) {
                Node currentNode = impNodeChilds.item(j);

                if(currentNode.getNodeType() == Node.ELEMENT_NODE) {
                    constrPair[index] = currentNode.getTextContent();
                    index++;
                }
            }
                if (constrPair[1].startsWith("m")) {
                    if(nameToTask.containsKey(constrPair[0]) && nameToMachine.containsKey(constrPair[1])) {
                        Task task = nameToTask.get(constrPair[0]);
                        task.machine = nameToMachine.get(constrPair[1]).id;
                    }
                } else {
                    if(nameToTask.containsKey(constrPair[0]) && nameToTask.containsKey(constrPair[1])) {
                        orderPairs.add(constrPair);
                    }
                }

        }

        // Nicht-Startertasks finden
        Set<String> starterTasks = new HashSet<>(nameToTask.keySet());
        Set<String> notStarter = new HashSet<>();
        for(String[] pair : orderPairs) {
            notStarter.add(pair[0]);
        }
        // Enthält jetzt nur noch Starter-Tasks
        starterTasks.removeAll(notStarter);

        for(String taskname : starterTasks) {
            String currentTaskname = taskname;
            List<Task> job = new ArrayList<>();

            Task task = nameToTask.get(taskname);
            job.add(task);

            for(int i = 0; i < orderPairs.size(); i++) {
                String[] currentPair = orderPairs.get(i);
                if((currentPair[1].equals(currentTaskname))) {
                    Task followTask = nameToTask.get(currentPair[0]);
                    job.add(followTask);
                    currentTaskname = currentPair[0];
                    orderPairs.remove(currentPair);
                    i = -1;
                }
            }

            jobs.add(job);
        }
    }
}
