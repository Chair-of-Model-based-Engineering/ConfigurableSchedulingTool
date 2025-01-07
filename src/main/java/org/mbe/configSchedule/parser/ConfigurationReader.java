package org.mbe.configSchedule.parser;

import org.mbe.configSchedule.util.Machine;
import org.mbe.configSchedule.util.SchedulingProblem;
import org.mbe.configSchedule.util.Task;
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

    public ConfigurationReader() {
    }

    ;

    public static void main(String[] args) throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
        String configPath = "src/main/modelle/J2_T10_M2_O2_A4Configs/01487.xml";
        String modelPath = "src/main/modelle/J2_T10_M2_O2_A4.xml";
        ConfigurationReader reader = new ConfigurationReader();
        reader.ReadConfig(configPath, modelPath);


        System.out.println("Jobs: ");
        for (List<Task> job : reader.jobs) {
            System.out.print("Job: ");
            for (Task task : job) {
                System.out.print(task.getName() + ", M: " + task.getMachine() + ", D: [" + task.getDuration()[0] + "," + task.getDuration()[1] + "], " + "O: " + task.isOptional() + "E: " + task.getExcludeTasks().size() + " | ");
            }
            System.out.print("\n");
        }

        System.out.println("Machines: ");
        for (Machine machine : reader.machines) {
            System.out.print(machine.getName() + " " + machine.isOptional() + " | ");
        }

    }

    public SchedulingProblem ReadConfig(String configPath, String modelPath) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        jobs.clear();
        machines.clear();
        deadline = -1;

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document modelDoc = builder.parse(new File(modelPath));
        Document configDoc = builder.parse(new File(configPath));

        XPath xPath = XPathFactory.newInstance().newXPath();

        // Expressions
        String exprRoot = "//description";
        String exprOrder = "//imp";
        String exprTask = "//feature[starts-with(@name, 'p')]";
        String exprMachine = "//feature[starts-with(@name, 'm')]";
        String exprDuration = "//feature[starts-with(@name, 'd')]";

        // Expressions evaluieren und Nodes/ Nodelisten bekommen
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

        SchedulingProblem sp = new SchedulingProblem(jobs, machines, deadline);
        return sp;
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

            // Selected = angewählt, naja offensichtlich
            if (taskNode.getNodeType() == Node.ELEMENT_NODE && taskNode.getAttributes().item(0).getNodeValue().equals("selected")) {
                Task task = new Task();
                task.setName(taskNode.getAttributes().getNamedItem("name").getNodeValue());
                // In einer instanz ist keine task optional
                task.setOptional(false);
                task.setExcludeTasks(new ArrayList<>());

                nameToTask.put(task.getName(), task);
            }
        }
    }

    private void ReadDurations(NodeList durationNodes) {
        for (int i = 0; i < durationNodes.getLength(); i++) {
            Node durationNode = durationNodes.item(i);

            // Duration Features haben ja den Tasknamen im Namen, von daher kann man sie darüber zuordnen
            // Die Duration kommt in beide Arrayplätze, sodass die Range von x bis x geht (x = gleiche Zahl)
            if (durationNode.getNodeType() == Node.ELEMENT_NODE && durationNode.getAttributes().item(0).getNodeValue().equals("selected")) {
                String durationString = durationNode.getAttributes().getNamedItem("name").getNodeValue();
                String[] subStrings = durationString.split(" ");
                int[] durationsArr = new int[2];
                durationsArr[0] = Integer.parseInt(subStrings[2]);
                durationsArr[1] = Integer.parseInt(subStrings[2]);

                String nameString = subStrings[0].substring(1);
                Task task = nameToTask.get(nameString);
                task.setDuration(durationsArr);
            }
        }
    }

    public void CreateMachines(NodeList machineNodes) {
        int amountMachines = 0;
        for (int i = 0; i < machineNodes.getLength(); i++) {
            Node currentNode = machineNodes.item(i);

            if (currentNode.getNodeType() == Node.ELEMENT_NODE && currentNode.getAttributes().item(0).getNodeValue().equals("selected")) {
                String name = currentNode.getAttributes().getNamedItem("name").getNodeValue();
                Machine machine = new Machine(name, false);
                machine.setOptional(false);
                amountMachines++;

                nameToMachine.put(machine.getName(), machine);
                machines.add(machine);
            }
        }
    }

    public void CreateJobs(NodeList orderNodes) {
        List<String[]> orderPairs = new ArrayList<>();
        for (int i = 0; i < orderNodes.getLength(); i++) {
            Node impNode = orderNodes.item(i);
            NodeList impNodeChilds = impNode.getChildNodes();

            String[] constrPair = new String[2];
            int index = 0;

            for (int j = 0; j < impNodeChilds.getLength(); j++) {
                Node currentNode = impNodeChilds.item(j);

                if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                    constrPair[index] = currentNode.getTextContent();
                    index++;
                }
            }
            // Falls der erste Constraint-String mit m Startet, ist es eine zuweisung von Task zu Machine
            // Wenn er mit p beginnt, dann eine Reihenfolgen require-Constraint
            if (constrPair[1].startsWith("m")) {
                if (nameToTask.containsKey(constrPair[0]) && nameToMachine.containsKey(constrPair[1])) {
                    Task task = nameToTask.get(constrPair[0]);
                    task.setMachine(nameToMachine.get(constrPair[1]));
                }
            } else {
                if (nameToTask.containsKey(constrPair[0]) && nameToTask.containsKey(constrPair[1])) {
                    orderPairs.add(constrPair);
                }
            }

        }

        // Nicht-Startertasks finden
        // Wenn eine Task auf der linken Seite einer Relation (p1, p2) steht, dann kann sie kein Starter sein,
        // weil das bedeutet, dass sie nach p2 startet
        Set<String> starterTasks = new HashSet<>(nameToTask.keySet());
        Set<String> notStarter = new HashSet<>();
        for (String[] pair : orderPairs) {
            notStarter.add(pair[0]);
        }

        // Enthält jetzt nur noch Starter-Tasks
        // Enthält auch immer noch optionale Tasks, da diese ja ein eigener Job sind
        starterTasks.removeAll(notStarter);

        // Für jede Startertask einen Job erstellen
        for (String taskname : starterTasks) {
            String currentTaskname = taskname;
            List<Task> job = new ArrayList<>();

            Task task = nameToTask.get(taskname);
            job.add(task);

            // Die Orderpairs durchgehen und jedes mal, wenn eine Order, die die letzte Task des
            // aktuellen Jobs enthält, die andere Task anhängen, die Order löschen und wieder von vorne
            // die Orderpairs durchgehen
            for (int i = 0; i < orderPairs.size(); i++) {
                String[] currentPair = orderPairs.get(i);
                if ((currentPair[1].equals(currentTaskname))) {
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
