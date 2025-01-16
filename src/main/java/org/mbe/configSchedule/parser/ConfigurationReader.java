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

    /**
     * Creates a scheduling problem from an xml-configuration-file (and from the model-file)
     *
     * @param configPath The file-path to the configuration-file
     * @param modelPath  The file-path to the model-file
     * @return A SchedulingProblem-object
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     * @throws XPathExpressionException
     */
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
        String expressionDeadline = "//feature[starts-with(@name, 'dl')]/@name";

        // Evaluate expressions to retrieve Nodes and NodeLists
        Node rootDescription = (Node) xPath.compile(exprRoot).evaluate(modelDoc, XPathConstants.NODE);
        NodeList orderNodes = (NodeList) xPath.compile(exprOrder).evaluate(modelDoc, XPathConstants.NODESET);
        NodeList taskNodes = (NodeList) xPath.compile(exprTask).evaluate(configDoc, XPathConstants.NODESET);
        NodeList machineNodes = (NodeList) xPath.compile(exprMachine).evaluate(configDoc, XPathConstants.NODESET);
        NodeList durationNodes = (NodeList) xPath.compile(exprDuration).evaluate(configDoc, XPathConstants.NODESET);
        String deadlineString = (String) xPath.compile(expressionDeadline).evaluate(modelDoc, XPathConstants.STRING);

        ReadDeadline(deadlineString);
        CreateTasks(taskNodes);
        ReadDurations(durationNodes);
        CreateMachines(machineNodes);
        CreateJobs(orderNodes);

        SchedulingProblem sp = new SchedulingProblem(jobs, machines, deadline);
        return sp;
    }

    /**
     * Sets the deadline from a String in the form of "dl = x" with x being an integer value
     *
     * @param deadlineString The String from which the deadline should be determined
     */
    private void ReadDeadline(String deadlineString) {
        try {
            String[] parts = deadlineString.split("=");
            deadline = Integer.parseInt(parts[1].strip());
        } catch (NumberFormatException e) {
            System.out.println("Deadline konnte nicht konvertiet werden, überprüfe Description von root");
        }
    }

    /**
     * Creates tasks from the NodeList, sets their name, optional and exclude-tasks
     *
     * @param taskNodes NodeList containing Nodes for the tasks
     */
    private void CreateTasks(NodeList taskNodes) {
        for (int i = 0; i < taskNodes.getLength(); i++) {
            Node taskNode = taskNodes.item(i);

            // If selected, then tasks exists in configuration
            if (taskNode.getNodeType() == Node.ELEMENT_NODE && taskNode.getAttributes().item(0).getNodeValue().equals("selected")) {
                Task task = new Task();
                task.setName(taskNode.getAttributes().getNamedItem("name").getNodeValue());
                // There are no optional tasks in an instance
                task.setOptional(false);
                task.setExcludeTasks(new ArrayList<>());

                nameToTask.put(task.getName(), task);
            }
        }
    }

    /**
     * Sets the duration of the tasks
     *
     * @param durationNodes NodeList containing Nodes with the durations
     */
    private void ReadDurations(NodeList durationNodes) {
        for (int i = 0; i < durationNodes.getLength(); i++) {
            Node durationNode = durationNodes.item(i);

            // Since duration-features contain the task name, they can be assigned over that string
            // Same duration in both array elements, so that the duration ranges from x to x (same value)
            if (durationNode.getNodeType() == Node.ELEMENT_NODE && durationNode.getAttributes().item(0).getNodeValue().equals("selected")) {
                String durationString = durationNode.getAttributes().getNamedItem("name").getNodeValue();
                String[] subStrings = durationString.split(" ");
                int[] durationsArr = new int[2];
                durationsArr[0] = Integer.parseInt(subStrings[2]);
                durationsArr[1] = Integer.parseInt(subStrings[2]);

                String nameString = subStrings[0].substring(1);
                if(!nameString.equals("l")) {
                    Task task = nameToTask.get(nameString);
                    task.setDuration(durationsArr);
                }
            }
        }
    }

    /**
     * Creates the machines
     *
     * @param machineNodes NodeList containing Nodes for the machines
     */
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

    /**
     * Creates jobs and orders the tasks in these
     *
     * @param orderNodes NodeList containing the constraints for the ordering of tasks
     */
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
            // If the first constraint string begins with "m", the constraint is an assignment from task to machine
            // If it begins with "p", then it is an task order constraint
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

        // Find the not-start-tasks
        // If a task is on the left side of a relation (p1, p2), then it can't be a starter
        // Here: p1 is not a starter, because it starts after p2
        Set<String> starterTasks = new HashSet<>(nameToTask.keySet());
        Set<String> notStarter = new HashSet<>();
        for (String[] pair : orderPairs) {
            notStarter.add(pair[0]);
        }

        // Now only contains starter-tasks
        // Also contains optional tasks, since these are their own job
        starterTasks.removeAll(notStarter);

        // Create a job for every starter-task
        for (String taskname : starterTasks) {
            String currentTaskname = taskname;
            List<Task> job = new ArrayList<>();

            Task task = nameToTask.get(taskname);
            job.add(task);

            // Every time an order pair contains the last task of the current job, append the other task
            // and delete the order pair and start the iteration from the beginning
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
