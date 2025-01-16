package org.mbe.configSchedule.parser;

import org.mbe.configSchedule.util.Machine;
import org.mbe.configSchedule.util.PathPreferences;
import org.mbe.configSchedule.util.SchedulingProblem;
import org.mbe.configSchedule.util.Task;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class FMReader {

    /**
     * Reads a {@link SchedulingProblem} from an xml-file
     * @param modelPath Path to the xml-file
     * @return          The {@link SchedulingProblem}
     */
    public static SchedulingProblem readFM(String modelPath) throws ParserConfigurationException,
            IOException, SAXException, XPathExpressionException {
        String modellpfad = "";

        if(!modelPath.contains("/")) {
            PathPreferences prefs = new PathPreferences();
            Path path = Path.of(prefs.getProblemSavePath());
            Path filePath = path.resolve(modelPath);
            modellpfad = filePath.toString();
        } else {
            modellpfad = modelPath;
        }

        List<List<Task>> jobs = new ArrayList<>();
        List<Machine> machines = new ArrayList<>();
        int deadline = -1;

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document modellDoc = builder.parse(new File(modellpfad));

        // XPath-Expressions
        XPath xPath = XPathFactory.newInstance().newXPath();
        String expressionP = "//and[@name=\"P\"]";
        String expressionM = "//and[@name=\"M\"]";
        String expressionDeadline = "//feature[starts-with(@name, 'dl')]/@name";
        String expressionCon = "//imp";
        String expressionDesc = "//description";
        String expressionExclCount = "//rule/disj";

        // Evaluate the expressions and retrieve Nodes
        Node m = (Node) xPath.compile(expressionM).evaluate(modellDoc, XPathConstants.NODE);
        Node p = (Node) xPath.compile(expressionP).evaluate(modellDoc, XPathConstants.NODE);
        NodeList constraints = (NodeList) xPath.compile(expressionCon).evaluate(modellDoc, XPathConstants.NODESET);
        String deadlineNode = (String) xPath.compile(expressionDeadline).evaluate(modellDoc, XPathConstants.STRING);
        NodeList exclConstraints = (NodeList) xPath.compile(expressionExclCount).evaluate(modellDoc, XPathConstants.NODESET);

        List<Task> allTasks = new ArrayList<>();
        // Name of Task/ Machine as key and its corresponding object as value
        Map<String, Task> taskNameMap = new HashMap<>();
        Map<String, Machine> machineNameMap = new HashMap<>();


        deadline = readDeadline(deadlineNode);
        readMachines(m, machineNameMap, machines);
        readTasks(p, taskNameMap, allTasks);
        List<String[]>[] tempConstraints = readConstraints(constraints, allTasks, taskNameMap, jobs);

        List<String[]> machineConstraints = tempConstraints[0];
        List<String[]> durationConstraints = tempConstraints[1];

        readMachineConstraints(machineConstraints, machines, allTasks);
        readExcludeConstraints(xPath, modellDoc, taskNameMap);
        readDurationConstraints(durationConstraints, allTasks);

        SchedulingProblem sp = new SchedulingProblem(jobs, machines, deadline);
        return sp;
    }

    /**
     * Reads deadline
     * @param deadlineNode  Text-content of deadline feature
     * @return {@link Integer} value of deadline
     */
    public static int readDeadline(String deadlineNode) {
        String deadlineString = deadlineNode;
        try {
            String[] parts = deadlineString.split("=");
            return Integer.parseInt(parts[1].strip());
        } catch (NumberFormatException e) {
            System.out.println("Deadline konnte nicht konvertiet werden, überprüfe Description von root");
            return -1;
        }
    }

    /**
     * Creates Machines
     * @param m              Node for the "parent feature" of all machines
     * @param machineNameMap {@link Map} of {@link String} keys as machine names with a {@link Machine} as its value
     * @param machines       A list of all machines
     */
    public static void readMachines(Node m, Map<String, Machine> machineNameMap, List<Machine> machines) {
        // Machines are child nodes of abstract feature "M"
        NodeList machineNodes = m.getChildNodes();

        int id = 0;

        for (int i = 0; i < machineNodes.getLength(); i++) {
            if (machineNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                // If a node has 2 attributes, then it has the attribute: "mandatory = false".
                // If it is not mandatory, it does not have this attributes. Using this we can determine if
                // a machine is mandatory or optional
                if (machineNodes.item(i).getAttributes().getLength() == 2) {
                    String name = machineNodes.item(i).getAttributes().getNamedItem("name").getNodeValue();
                    Machine machine = new Machine(name, false);
                    id++;
                    machineNameMap.put(machineNodes.item(i).getAttributes().item(1).getNodeValue(), machine);
                    machines.add(machine);
                } else {
                    String name = machineNodes.item(i).getAttributes().getNamedItem("name").getNodeValue();
                    Machine machine = new Machine(name, true);
                    id++;
                    machineNameMap.put(machineNodes.item(i).getAttributes().item(0).getNodeValue(), machine);
                    machines.add(machine);
                }
            }
        }
    }

    /**
     * Creates tasks
     * @param p           Node for the "parent feature" of a tasks
     * @param taskNameMap {@link Map} of {@link String} keys as task names with a {@link Task} as its value
     * @param allTasks    A list of all tasks
     */
    public static void readTasks(Node p, Map<String, Task> taskNameMap, List<Task> allTasks) {
        //Tasks are child nodes of parent feature "P"
        NodeList taskNodes = p.getChildNodes();

        for (int i = 0; i < taskNodes.getLength(); i++) {
            Node currentNode = taskNodes.item(i);
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                Task task = new Task();
                // If a node has 2 attributes, then it has the attribute: "mandatory = false".
                // If it is not mandatory, it does not have this attributes. Using this we can determine if
                // a task is mandatory or optional
                if (currentNode.getAttributes().getLength() == 2) {
                    task.setName(currentNode.getAttributes().item(1).getNodeValue());
                    task.setOptional(false);
                } else {
                    task.setName(currentNode.getAttributes().item(0).getNodeValue());
                    task.setOptional(true);
                }

                // Setting the duration of the task
                int[] durationsArr = new int[2];
                NodeList durations = currentNode.getChildNodes();
                int durationsArrIndex = 0;
                boolean durationsAlreadySet = true;
                List<Integer> durationsList = new ArrayList<>();
                for (int j = 0; j < durations.getLength(); j++) {
                    Node currentDuration = durations.item(j);
                    if (currentDuration.getNodeType() == Node.ELEMENT_NODE) {
                        // If it has two attributes, then because it has the attribute "mandatory = true"
                        // This means that it's the only (static) duration for this task
                        if (currentDuration.getAttributes().getLength() == 2) {
                            String durationString = currentDuration.getAttributes().item(1).getNodeValue();
                            String[] subStrings = durationString.split(" ");
                            durationsArr[0] = Integer.parseInt(subStrings[2]);
                            durationsArr[1] = Integer.parseInt(subStrings[2]);
                            // If it only has one attribute, then it is an alternative group
                            // Enter all the possible durations into a list and sort them to retrieve the
                            // min and max value for the task
                            // Only done if durationsAlreadySet = false, which is set here. Normally this boolean is true
                        } else if (currentDuration.getAttributes().getLength() == 1) {
                            durationsAlreadySet = false;
                            String durationString = currentDuration.getAttributes().item(0).getNodeValue();
                            String[] subStrings = durationString.split(" ");
                            durationsList.add(Integer.parseInt(subStrings[2]));
                        }
                    }
                }
                if (!durationsAlreadySet) {
                    Collections.sort(durationsList);
                    durationsArr[0] = durationsList.get(0);
                    durationsArr[1] = durationsList.get(durationsList.size() - 1);
                }

                Arrays.sort(durationsArr);
                task.setDuration(durationsArr);
                taskNameMap.put(task.getName(), task);
                allTasks.add(task);
            }
        }
    }

    /**
     * Reads constraints and order the tasks into jobs
     * @param constraints   A {@link NodeList} of constraints
     * @param allTasks      A {@link List} of {@link Task} containing every task
     * @param taskNameMap   A {@link Map} of {@link String} keys as task names with a {@link Task} as its value
     * @param jobs          The list of jobs
     * @return              An Array in which [0] are {@link List} of {@link String} as machine constraints and [1] are {@link List} of {@link String} as duratio constraints
     */
    public static List<String[]>[] readConstraints(NodeList constraints, List<Task> allTasks, Map<String, Task> taskNameMap, List<List<Task>> jobs) {
        List<String[]> orderConstraints = new ArrayList<>();
        List<String[]> machineConstraints = new ArrayList<>();
        List<String[]> durationConstraints = new ArrayList<>();
        for (int i = 0; i < constraints.getLength(); i++) {
            Node currentConstraint = constraints.item(i);
            NodeList constraintPair = currentConstraint.getChildNodes();

            String[] constraintPairArr = new String[2];

            // ConstraintPair füllen (man muss das mit einem Index machen,
            // da es mehr Nodes gibt als nur die Textnodes
            int index = 0;
            for (int j = 0; j < constraintPair.getLength(); j++) {
                if (constraintPair.item(j).getNodeType() == Node.ELEMENT_NODE) {
                    constraintPairArr[index] = constraintPair.item(j).getTextContent();
                    index++;
                }
            }

            // Zuordnen ob es eine Reihenfolge-, Duration- oder eine Machine-Constraint ist
            if (constraintPairArr[1].startsWith("m")) {
                machineConstraints.add(constraintPairArr);
            } else if (constraintPairArr[0].startsWith("d")) {
                durationConstraints.add(constraintPairArr);
            } else {
                orderConstraints.add(constraintPairArr);
            }
        }

        Set<Task> unusedTasks = new HashSet<>(allTasks);
        // Beispiel: orderConstraints = {[p3,p2], [p2,p1], [p5,p4]} und P = {p1,p2,p3,p4,p5,p6}
        // Dann J1=p1,p2,p3  J2=p4,p5   J3=p6
        // Die tasks, die in der orderList bei keinem Orderpaar an erster Stelle stehen, sind Starttasks
        // Zuerst alle Tasks die bei den Orderpaaren an erster Stelle stehen in notStarter einfügen
        Set<Task> notStarter = new HashSet<>();
        for (int i = 0; i < orderConstraints.size(); i++) {
            int finalI = i;
            Task task = allTasks.stream()
                    .filter(t -> orderConstraints.get(finalI)[0].equals(t.getName()))
                    .findAny()
                    .orElse(null);

            if (task != null) {
                notStarter.add(task);
            }
        }

        //Startertasks sind die Differenz von unusedTasks \ notStarter
        unusedTasks.removeAll(notStarter);

        // Für jede StarterTask (hier in unusedTasks) einen Job erstellen
        // OrderConstraints durchgehen und durch sie den Job versvollständigen
        // Wenn eine Task durch eine Orderconstraints hinzgefügt wurde, Constraint löschen
        // und wieder von vorne durch die Liste gehen
        for (Task task : unusedTasks) {
            List<Task> job = new ArrayList<>();
            job.add(task);
            String currentTaskName = task.getName();
            for (int i = 0; i < orderConstraints.size(); i++) {
                String[] currentConstraint = orderConstraints.get(i);
                if (orderConstraints.get(i)[1].equals(currentTaskName)) {
                    Task followTask = taskNameMap.get(orderConstraints.get(i)[0]);
                    job.add(followTask);
                    currentTaskName = orderConstraints.get(i)[0];
                    orderConstraints.remove(orderConstraints.get(i));
                    i = -1;

                }
            }
            jobs.add(job);
        }

        return new List[]{machineConstraints, durationConstraints};
    }

    /**
     * Assign the tasks to machines
     * @param machineConstraints {@link List} of machine constraints with [0] = p, [1] = m
     * @param machines           {@link List} of all machines
     * @param allTasks           {@link List} of all tasks
     */
    public static void readMachineConstraints(List<String[]> machineConstraints, List<Machine> machines, List<Task> allTasks) {
        for (String[] con : machineConstraints) {
            // Find the machine and task which have the same name as the machine/task in the consraints and assign them
            Machine machine = machines.stream()
                    .filter(mach -> con[1].equals(mach.getName()))
                    .findAny()
                    .orElse(null);

            Task task = allTasks.stream()
                    .filter(t -> con[0].equals(t.getName()))
                    .findAny()
                    .orElse(null);
            if ((machine != null) && (task != null)) {
                task.setMachine(machine);
            }
        }
    }

    /**
     * Reads the exclude constraints, from which the alternative task groups are formed
     * @param xPath       {@link XPath}-object to find the constraints
     * @param modellDoc   {@link Document} that contains the xml-file
     * @param taskNameMap A  {@link Map} of {@link String} keys as task names with a {@link Task} as its value
     */
    public static void readExcludeConstraints(XPath xPath, Document modellDoc, Map<String, Task> taskNameMap) throws XPathExpressionException {
        // The expression retrieves the list of tasks in an alternative task group
        // Because these are realised through XOR-expressions - (p1 * !p2) + (!p1 * p2) - there are as many lists
        // with the same tasks as there are clauses in the expression.
        // With i we can retrieve the task-list between the i'th <conj></conj>
        // We do this till we get an empty list
        // After every iteration we have to check, if it's the same list as last time, so that we can skip this list

        // Retrieve first task list
        int i = 1;
        String expression = "(//rule/disj/conj)[" + i + "]//text()";
        Object evaluation = xPath.compile(expression).evaluate(modellDoc, XPathConstants.NODESET);
        List<String> taskNames = new ArrayList<>();
        if (evaluation instanceof NodeList) {
            NodeList list = (NodeList) evaluation;
            for (int j = 0; j < list.getLength(); j++) {
                String text = list.item(j).getNodeValue().trim();
                if (!text.isEmpty()) {
                    taskNames.add(text);
                }
            }
        }
        int listSize = taskNames.size();
        List<String> prevList = new ArrayList<>();

        // Retrieve new list as long as list is not empty at the end of the while-loop
        while (listSize > 0) {
            i++;
            boolean sameList = false;
            // prevlist != null is only true, if it's the first iteration and we have the list that we
            // retrieved outsied of the while-loop
            // If the list has a different size than its previous list, then it's a different list anyway
            // If the size is the same, check if the list also contains the same tasks, then break
            // If not, then it is the same list if break was not called
            if ((prevList != null) && (taskNames.size() == prevList.size())) {
                sameList = true;
                for (int j = 0; j < taskNames.size(); j++) {
                    if (!prevList.contains(taskNames.get(j))) {
                        sameList = false;
                        break;
                    }

                }
            }

            // If it is not the same list, the tasks can be assigned to the other tasks excludeTasks-list
            if (!sameList) {
                String[] names = new String[taskNames.size()];
                names = taskNames.toArray(names);

                for (int j = 0; j < names.length; j++) {
                    Task task = taskNameMap.get(names[j]);

                    for (int k = 0; k < names.length; k++) {
                        if (k != j) {
                            task.getExcludeTasks().add(names[k]);
                        }
                    }
                }
            }

            if (prevList != null) {
                prevList.clear();
            }
            for (String n : taskNames) {
                prevList.add(n);
            }

            // Retrieve new list
            String expression2 = "(//rule/disj/conj)[" + i + "]//text()";
            Object evaluation2 = xPath.compile(expression2).evaluate(modellDoc, XPathConstants.NODESET);
            taskNames.clear();
            if (evaluation2 instanceof NodeList) {
                NodeList list = (NodeList) evaluation2;
                for (int j = 0; j < list.getLength(); j++) {
                    String text = list.item(j).getNodeValue().trim();
                    if (!text.isEmpty()) {
                        taskNames.add(text);
                    }
                }
            }
            listSize = taskNames.size();
        }
    }

    /**
     * Assigns the duration constraints
     * @param durationConstraints A {@link List} of {@link Arrays} of {@link String} with [0] = dp, [1] = p
     * @param allTasks            A list of all tasks
     */
    public static void readDurationConstraints(List<String[]> durationConstraints, List<Task> allTasks) {
        for (String con[] : durationConstraints) {
            String duration = con[0];
            // Split string to read task and duration separately
            // "dp1 = 1" -> "dp1", "=", "1"
            String[] subStrings = duration.split(" ");
            String taskDurationString = subStrings[0].substring(1);
            int durationValue = -1;
            try {
                durationValue = Integer.valueOf(subStrings[2]);
            } catch (Exception e) {
                System.out.println("Cannot read a duration value from " + duration);
            }

            // The task which has to be executed for the above selected duration to be eligible
            String requiredTaskString = con[1];

            // Search for both tasks
            Task task = allTasks.stream()
                    .filter(t -> taskDurationString.equals(t.getName()))
                    .findAny()
                    .orElse(null);

            Task requiredTask = allTasks.stream()
                    .filter(t -> requiredTaskString.equals(t.getName()))
                    .findAny()
                    .orElse(null);

            if (task != null && requiredTask != null && durationValue != -1) {
                if (task.getDurationCons().get(durationValue) != null) {
                    task.addTaskToDurationCon(durationValue, requiredTask);
                } else {
                    List<Task> taskList = new ArrayList<>();
                    taskList.add(requiredTask);
                    task.addDurationCon(durationValue, taskList);
                }
            }
        }
    }
}
