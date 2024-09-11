package org.mbe.configschedule.parser;

import org.mbe.configschedule.util.Machine;
import org.mbe.configschedule.util.SchedulingProblem;
import org.mbe.configschedule.util.Task;
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

    public FMReader() {
    }

    public static void main(String[] args) throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {

        /*FMReader reader = new FMReader();
        String modelPath = "src/main/modelle/J2_T10_M2_O2_A4.xml";
        reader.ReadFM(modelPath);

        System.out.println("\n" + "Erstellte Jobs:");
        for (int i = 0; i < reader.jobs.size(); i++) {
            System.out.println("\n Job " + i + ": ");
            for (Task task : reader.jobs.get(i)) {
                System.out.print(task.name + "  [" + task.duration[0] + "," + task.duration[1] + "]  " + task.machine + "  " + task.optional + "  |  ");
                for (String s : task.excludeTasks) {
                    System.out.print(" " + s);
                }
            }
        }
        for (int i = 0; i < reader.machines.size(); i++) {
            System.out.print("\n Machine " + i + ": " + reader.machines.get(i).name + "  " + reader.machines.get(i).id + "  " + reader.machines.get(i).optional);
        }

         */


    }

    public SchedulingProblem ReadFM(String modelPath) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        String modellpfad = modelPath;

        List<List<Task>> jobs = new ArrayList<>();
        List<Machine> machines = new ArrayList<>();
        int deadline = -1;

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document modellDoc = builder.parse(new File(modellpfad));

        // XPath-Expressions
        XPath xPath = XPathFactory.newInstance().newXPath();
        String expressionP = "//and[@name=\"P\"]";
        String expressionM = "//and[@name=\"M\"]";
        String expressionCon = "//imp";
        String expressionDesc = "//description";
        String expressionExclCount = "//rule/disj";

        // Expressions evaluieren und Nodes zurückgeben lassen
        Node m = (Node) xPath.compile(expressionM).evaluate(modellDoc, XPathConstants.NODE);
        Node p = (Node) xPath.compile(expressionP).evaluate(modellDoc, XPathConstants.NODE);
        NodeList constraints = (NodeList) xPath.compile(expressionCon).evaluate(modellDoc, XPathConstants.NODESET);
        Node deadlineNode = (Node) xPath.compile(expressionDesc).evaluate(modellDoc, XPathConstants.NODE);
        NodeList exclConstraints = (NodeList) xPath.compile(expressionExclCount).evaluate(modellDoc, XPathConstants.NODESET);

        List<Task> allTasks = new ArrayList<>();
        // Der Name der Task/ Der Maschine als key und das Objekt als value
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

        // Maschinen sind die Child Nodes des abstract Features M
        NodeList machineNodes = m.getChildNodes();

        int id = 0;

        for (int i = 0; i < machineNodes.getLength(); i++) {
            if (machineNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                // Wenn die Node 2 Attribute hat, hat es ein mandatory = false Attribut. Ist es nicht mandatory,
                // hat es dieses Attribute einfach nicht. Darüber können wir bestimmen ob die Maschine optional
                // oder nicht optional ist
                if (machineNodes.item(i).getAttributes().getLength() == 2) {
                    String name = machineNodes.item(i).getAttributes().getNamedItem("name").getNodeValue();
                    Machine machine = new Machine(name, id, false);
                    id++;
                    machineNameMap.put(machineNodes.item(i).getAttributes().item(1).getNodeValue(), machine);
                    machines.add(machine);
                } else {
                    String name = machineNodes.item(i).getAttributes().getNamedItem("name").getNodeValue();
                    Machine machine = new Machine(name, id, true);
                    id++;
                    machineNameMap.put(machineNodes.item(i).getAttributes().item(0).getNodeValue(), machine);
                    machines.add(machine);
                }
            }
        }

        // ======================================================================
        // Tasks
        // ======================================================================

        //Tasks befinden sich in Childnodes des abstract Features P
        NodeList taskNodes = p.getChildNodes();

        for (int i = 0; i < taskNodes.getLength(); i++) {
            Node currentNode = taskNodes.item(i);
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                Task task = new Task();
                // Selbe wie bei Maschinen
                // Wenn die Node 2 Attribute hat, hat es ein mandatory = false Attribut. Ist es nicht mandatory,
                // hat es dieses Attribute einfach nicht. Darüber können wir bestimmen ob die Task optional
                // oder nicht optional ist
                if (currentNode.getAttributes().getLength() == 2) {
                    task.setName(currentNode.getAttributes().item(1).getNodeValue());
                    task.setOptional(false);
                } else {
                    task.setName(currentNode.getAttributes().item(0).getNodeValue());
                    task.setOptional(true);
                }

                // Duration für Task
                int[] durationsArr = new int[2];
                NodeList durations = currentNode.getChildNodes();
                int durationsArrIndex = 0;
                boolean durationsAlreadySet = true;
                List<Integer> durationsList = new ArrayList<>();
                for (int j = 0; j < durations.getLength(); j++) {
                    Node currentDuration = durations.item(j);
                    if (currentDuration.getNodeType() == Node.ELEMENT_NODE) {
                        // Wenn es zwei Attribute hat, dann weil es ein mandatory = true Attribut hat,
                        // damit ist es die einzige, feste Duration für die Task
                        if (currentDuration.getAttributes().getLength() == 2) {
                            String durationString = currentDuration.getAttributes().item(1).getNodeValue();
                            String[] subStrings = durationString.split(" ");
                            durationsArr[0] = Integer.parseInt(subStrings[2]);
                            durationsArr[1] = Integer.parseInt(subStrings[2]);
                        // Wenn es nur ein Attribut hat, ist es eine alternative Group, deswegen alle
                        // möglichen Durations in eine Liste packen, danach sortieren und die min und max Werte
                        // in das Durations-Array der Task packen
                        // Wird nur gemacht, wenn durationsAlreadySet = false, was hier gesetzt wird. Normalerweise
                        // ist er true
                        } else if (currentDuration.getAttributes().getLength() == 1) {
                            durationsAlreadySet = false;
                            String durationString = currentDuration.getAttributes().item(0).getNodeValue();
                            String[] subStrings = durationString.split(" ");
                            durationsList.add(Integer.parseInt(subStrings[2]));
                            //durationsArr[durationsArrIndex] = Integer.parseInt(subStrings[2]);
                            //durationsArrIndex++;
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

        // =============================================================
        // Req. Constraints
        // ==============================================================

        List<String[]> orderConstraints = new ArrayList<>();
        List<String[]> machineConstraints = new ArrayList<>();
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

            // Zuordnen ob es eine Reihenfolge-Constraint oder eine Machine-Constraint ist
            if (constraintPairArr[1].startsWith("m")) {
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

        // ==============================
        // Machine Constraints
        // ==============================

        for (String[] con : machineConstraints) {
            // Maschine und Tasken Finden die den selben Namen hat wie die Maschine/ Task in der Constraint
            // und zuordnen
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

        // ==============================
        // Exclude Constraints
        // ==============================

        // Man hätte es wahrscheinlich auch rekursiv machen können, aber so funtkioniert es auch
        // Uns wurde auch mal beigebracht dass rekursive Funktionen sehr schlecht performen, deswegen
        // habe ich es mal ein bisschen umständlicher gemacht, aber ob meine Version WIRKLICH performanter ist,
        // wurde nicht getestet

        // Durch die Expression wird sozusagen einmal die Liste der Tasks in einer alternative Task Group
        // geholt, da sie aber durch XOR-Ausdrücke (p1 * !p2) + (!p1 * p2) realisiert sind, gibt es so viele Listen
        // mit den selben Tasks, wie es Klasueln in dem Ausdruck gibt. Mit i kann man sich die Task-Liste holen,
        // die zwischen dem i'ten <conj></conj> steht
        // Das wird so lange gemacht, bis man eine leere Liste wiederbekommt
        // Nach jeder Iteration muss man aber einmal überprüfen, ob es die selbe Liste wie letztes mal ist, damit man
        // sie skippen kann

        //Zuerst muss man sich eine Task-Liste holen
        int i = 1;
        String expression = "(//rule/disj/conj)[" + i + "]//text()";
        Object evaluation = xPath.compile(expression).evaluate(modellDoc, XPathConstants.NODESET);
        List<String> taskNames = new ArrayList<>();
        if (evaluation instanceof NodeList) {
            NodeList list = (NodeList) evaluation;
            for (int j = 0; j < list.getLength(); j++) {
                String text = list.item(j).getNodeValue().trim();
                if (!text.isEmpty()) {
                    System.out.println(text);
                    taskNames.add(text);
                }
            }
        }
        int listSize = taskNames.size();
        List<String> prevList = new ArrayList<>();

        // Solange die Liste die man sich am Ende der while Schleife holt nicht leer ist
        while (listSize > 0) {
            i++;
            boolean sameList = false;
            // prevlist != null tritt ein, wenn es die erste Iteration ist und wir nur die Liste haben,
            // die wir außerhalb der while Schleife geholt haben
            // Wenn Liste unterschiedliche Größe zu vorher hat, dann ist sie eh nicht die selbe
            // Bei selber Größe solange checken ob die selben Tasks enthalten sind, dann breaken,
            // Ansonsten ist es zum Schluss die selbe Liste wenn break nicht aufgerufen wird
            if ((prevList != null) && (taskNames.size() == prevList.size())) {
                sameList = true;
                for (int j = 0; j < taskNames.size(); j++) {
                    if (!prevList.contains(taskNames.get(j))) {
                        sameList = false;
                        break;
                    }

                }
            }

            // Wenn es nicht die selbe Liste ist, können den Tasks die anderen Task in der alternative Gruppe
            // zu der exludeTasks-Liste hinzugefügt werden
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

            // Man muss leider über die Liste iterieren und alle Elemente prevListe hinzufügen,
            // da es ansonsten immer nur eine referenz auf die alte Liste ist
            if (prevList != null) {
                prevList.clear();
            }
            for(String n : taskNames) {
                prevList.add(n);
            }

            // Neue Liste holen
            String expression2 = "(//rule/disj/conj)[" + i + "]//text()";
            Object evaluation2 = xPath.compile(expression2).evaluate(modellDoc, XPathConstants.NODESET);
            taskNames.clear();
            if (evaluation2 instanceof NodeList) {
                NodeList list = (NodeList) evaluation2;
                for (int j = 0; j < list.getLength(); j++) {
                    String text = list.item(j).getNodeValue().trim();
                    if (!text.isEmpty()) {
                        System.out.println(text);
                        taskNames.add(text);
                    }
                }
            }
            listSize = taskNames.size();
        }

        SchedulingProblem sp = new SchedulingProblem(jobs, machines, deadline);
        return sp;
    }
}
