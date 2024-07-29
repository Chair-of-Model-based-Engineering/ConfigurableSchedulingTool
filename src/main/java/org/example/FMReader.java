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
    int deadline;

    public FMReader() {
    }

    public static void main(String[] args) throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
        FMReader reader = new FMReader();
        String modelPath = "src/main/modelle/FM2.xml";
        reader.ReadFM(modelPath);

        /*
        System.out.println("\n" + "Erstellte Jobs:");
        for(int i = 0; i < reader.jobs.size(); i++) {
            System.out.println("\n Job " + i + ": ");
            for(Task task : reader.jobs.get(i)) {
                System.out.print(task.name + "  [" + task.duration[0] + "," + task.duration[1] + "]  " + task.machine + "  " + task.optional + "  |  ");
                for(String s : task.excludeTasks) {
                    System.out.print(" " + s);
                }
            }
        }
        for(int i = 0; i < reader.machines.size(); i++) {
            System.out.print("\n Machine " + i + ": " + reader.machines.get(i).name + "  " + reader.machines.get(i).id + "  " + reader.machines.get(i).optional);
        }

         */


    }
    public void ReadFM(String modelPath) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        String modellpfad = modelPath;

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document modellDoc = builder.parse(new File(modellpfad));

        XPath xPath = XPathFactory.newInstance().newXPath();
        String expressionP = "//and[@name=\"P\"]";
        String expressionM = "//and[@name=\"M\"]";
        String expressionCon = "//imp";
        String expressionDesc = "//description";
        String expressionNot = "//conj";

        Node m = (Node) xPath.compile(expressionM).evaluate(modellDoc, XPathConstants.NODE);
        Node p = (Node) xPath.compile(expressionP).evaluate(modellDoc, XPathConstants.NODE);
        NodeList constraints = (NodeList) xPath.compile(expressionCon).evaluate(modellDoc, XPathConstants.NODESET);
        Node deadlineNode = (Node) xPath.compile(expressionDesc).evaluate(modellDoc, XPathConstants.NODE);
        NodeList exclConstraints = (NodeList) xPath.compile(expressionNot).evaluate(modellDoc, XPathConstants.NODESET);

        List<Task> allTasks = new ArrayList<>();
        Map<String, Task> taskNameMap = new HashMap<>();
        Map<String, Machine> machineNameMap = new HashMap<>();

        // =====================================================================
        // Deadline auslesen
        // =====================================================================
        String deadlineString;
        deadlineString = deadlineNode.getTextContent();
        try {
            deadline = Integer.parseInt(deadlineString);
        } catch (NumberFormatException e) {
            System.out.println("Deadline konnte nicht konvertiet werden, überprüfe Description von root");
        }

        // ======================================================================
        // Maschinen
        // ======================================================================
        NodeList machineNodes = m.getChildNodes();

        int id = 0;

        for(int i = 0; i < machineNodes.getLength(); i++) {
            if(machineNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                if(machineNodes.item(i).getAttributes().getLength() == 2) {
                    Machine machine = new Machine(id, false);
                    machine.name = machineNodes.item(i).getAttributes().getNamedItem("name").getNodeValue();
                    id++;
                    machineNameMap.put(machineNodes.item(i).getAttributes().item(1).getNodeValue(), machine);
                    machines.add(machine);
                } else {
                    Machine machine = new Machine(id, true);
                    machine.name = machineNodes.item(i).getAttributes().getNamedItem("name").getNodeValue();
                    id++;
                    machineNameMap.put(machineNodes.item(i).getAttributes().item(0).getNodeValue(), machine);
                    machines.add(machine);
                }
            }
        }

        // ======================================================================
        // Tasks
        // ======================================================================

        NodeList taskNodes = p.getChildNodes();

        for(int i = 0; i < taskNodes.getLength(); i++) {
            Node currentNode = taskNodes.item(i);
            if(currentNode.getNodeType() == Node.ELEMENT_NODE) {
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
                boolean durationsAlreadySet = true;
                List<Integer> durationsList = new ArrayList<>();
                for(int j = 0; j < durations.getLength(); j++) {
                    Node currentDuration = durations.item(j);
                    if(currentDuration.getNodeType() == Node.ELEMENT_NODE) {
                        if(currentDuration.getAttributes().getLength() == 2) {
                            String durationString = currentDuration.getAttributes().item(1).getNodeValue();
                            String[] subStrings = durationString.split(" ");
                            durationsArr[0] = Integer.parseInt(subStrings[2]);
                            durationsArr[1] = Integer.parseInt(subStrings[2]);
                        } else if(currentDuration.getAttributes().getLength() == 1) {
                            durationsAlreadySet = false;
                            String durationString = currentDuration.getAttributes().item(0).getNodeValue();
                            String[] subStrings = durationString.split(" ");
                            durationsList.add(Integer.parseInt(subStrings[2]));
                            //durationsArr[durationsArrIndex] = Integer.parseInt(subStrings[2]);
                            //durationsArrIndex++;
                        }
                    }
                }
                if(!durationsAlreadySet) {
                    Collections.sort(durationsList);
                    durationsArr[0] = durationsList.get(0);
                    durationsArr[1] = durationsList.get(durationsList.size() - 1);
                }

                Arrays.sort(durationsArr);
                task.duration = durationsArr;
                taskNameMap.put(task.name, task);
                allTasks.add(task);
            }
        }

        // =============================================================
        // Req. Constraints
        // ==============================================================

        List<String[]> orderConstraints = new ArrayList<>();
        List<String[]> machineConstraints = new ArrayList<>();
        for(int i = 0; i < constraints.getLength(); i++) {
            Node currentConstraint = constraints.item(i);
            NodeList constraintPair = currentConstraint.getChildNodes();

            String[] constraintPairArr = new String[2];

            int index = 0;
            for(int j = 0; j < constraintPair.getLength(); j++) {
                if(constraintPair.item(j).getNodeType() == Node.ELEMENT_NODE) {
                    constraintPairArr[index] = constraintPair.item(j).getTextContent();
                    index++;
                }
            }

            // Zuordnen ob es eine Reihenfolge-Constraint oder eine Machine-Constraint ist
            if(constraintPairArr[1].startsWith("m")) {
                machineConstraints.add(constraintPairArr);
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
        for(int i = 0; i < orderConstraints.size(); i++) {
            int finalI = i;
            Task task = allTasks.stream()
                    .filter(t -> orderConstraints.get(finalI)[0].equals(t.name))
                    .findAny()
                    .orElse(null);

            if(task != null) {
                notStarter.add(task);
            }
        }

        //Startertasks sind die Differenz von unusedTasks \ notStarter
        unusedTasks.removeAll(notStarter);

        //unusedTasks enthält jetzt nurnoch Tasks die auch Startertasks sind
        //Für jede unusedTask einen Job erstellen
        for(int i = 0; i < unusedTasks.size(); i++) {
            Task currentTask = unusedTasks.stream().toList().get(i);
            List<Task> job = new ArrayList<>();
            job.add(currentTask);
            //unusedTasks.remove(currentTask);

            //Enthält den namen der aktuellen/ bis jetzt letzten Task im Job, damit die task aus
            //notStarter gelöscht werden kann
            String tempPreviousTask = "no next task";

            //für jeden Job einmal die orderList durchgehen und die order finden, bei der die Startertask an zweiter
            //Stelle steht
            for (int j = 0; j < orderConstraints.size(); j++) {
                if (orderConstraints.get(j)[1].equals(currentTask.name)) {
                    //Task mit dem Namen <p2> von der Relation [p2,p1] finden
                    int finalJ = j;
                    Task task = notStarter.stream()
                            .filter(t -> orderConstraints.get(finalJ)[0].equals(t.name))
                            .findAny()
                            .orElse(null);
                    if (task != null) {
                        job.add(task);
                        tempPreviousTask = task.name;
                        notStarter.remove(task);
                    }
                }
            }

            //Job enthält jetzt 2 Tasks, von hier die Jobkette weiter vervollständigen
            //Vorigen und jetzigen Schritt hätte man bestimmt in einem machen können, aber naja
            while(!tempPreviousTask.equals("no next task")) {
                boolean taskFound = false;
                for(int j = 0; j < orderConstraints.size(); j++) {
                    if(orderConstraints.get(j)[1].equals(tempPreviousTask)) {
                        taskFound = true;
                        int finalJ = j;
                        Task task = notStarter.stream()
                                .filter(t -> orderConstraints.get(finalJ)[0].equals(t.name))
                                .findAny()
                                .orElse(null);
                        if(task != null) {
                            job.add(task);
                            tempPreviousTask = task.name;
                            notStarter.remove(task);
                        }
                    } else { taskFound = false; }
                }
                if(!taskFound) {
                    tempPreviousTask = "no next task";
                }
            }

            jobs.add(job);
        }
        // ==============================
        // Machine Constraints
        // ==============================

        /*System.out.println("In ReadFM die allTasks: ");
        for(Task task : allTasks) {
            System.out.println(task.name + "   " + task.machine  + "   [" + task.duration[0] + "," + task.duration[1] + "]  " + task.optional);
        }
        System.out.println("Und die Machines: ");
        for(Machine m1 : machines) {
            System.out.println(m1.name + "  " + m1.active + "  " + m1.optional + "  " + m1.id);
        }
        System.out.println("Und jetzt noch die Constraints: ");
        for(String[] pair :  machineConstraints) {
            System.out.println(pair[0] + "  " + pair[1]);
        }*/

        for(String[] con : machineConstraints) {
            // Maschine Finden die den selben Namen hat wie die Maschine in der Constraint
            Machine machine = machines.stream()
                    .filter(mach -> con[1].equals(mach.name))
                    .findAny()
                    .orElse(null);

            Task task = allTasks.stream()
                    .filter(t -> con[0].equals(t.name))
                    .findAny()
                    .orElse(null);
            if((machine != null) && (task != null)) {
                task.machine = machine.id;
            }
        }

        // ==============================
        // Exclude Constraints
        // ==============================
        for(int i = 0; i < exclConstraints.getLength(); i++) {
            Node currentConstraint = exclConstraints.item(i);

            NodeList constraintPair = currentConstraint.getChildNodes();

            String[] constraintPairArr = new String[2];

            int index = 0;
            for(int j = 0; j < constraintPair.getLength(); j++) {
                if(constraintPair.item(j).getNodeType() == Node.ELEMENT_NODE) {
                    constraintPairArr[index] = constraintPair.item(j).getTextContent();
                    index++;
                }
            }

            taskNameMap.get(constraintPairArr[0]).excludeTasks.add(constraintPairArr[1]);
            taskNameMap.get(constraintPairArr[1]).excludeTasks.add(constraintPairArr[0]);
        }
    }
}
