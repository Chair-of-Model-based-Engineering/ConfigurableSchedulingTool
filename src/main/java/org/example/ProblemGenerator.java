package org.example;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class ProblemGenerator {

    private String modellpfad = "src/main/modelle/FeatureModell";
    private String problempfad = "src/main/modelle/Probleminsanz";

    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document modellDoc = builder.parse(new File(modellpfad));
    Document problemDoc = builder.parse(new File(problempfad));

    NodeList pUndMList;
    NodeList selectedPandD;
    List<Task> tasks;
    List<Integer> machines;
    NodeList selectedD;
    NodeList constraintList;
    Node taskMenge;
    Node machineMenge;

    List<List<Task>> jobs;
    int anzahlMachines;

    public ProblemGenerator() throws ParserConfigurationException, IOException, SAXException {
        pUndMList = modellDoc.getElementsByTagName("and");
        selectedPandD = problemDoc.getElementsByTagName("feature");
        constraintList = modellDoc.getElementsByTagName("imp");


        //Task und Machine Menge sind die Nodes mit P und M, also die Features (naja logisch)
        //taskMenge = FindTaskMenge();
        machineMenge = FindMachineMenge();
        for(int i = 0; i < machineMenge.getChildNodes().getLength(); i++) {
            if(machineMenge.getChildNodes().item(i).getNodeType() == Node.ELEMENT_NODE) {
                anzahlMachines++;
            }
        }

        List[] tempArray = CreateTasks();
        tasks = tempArray[1];
        machines = tempArray[0];
        MachineAllocation();
        jobs = SortTasks();

    }


    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException {
        ProblemGenerator pg = new ProblemGenerator();

        /*System.out.println("Tasks:");
        for (int i = 0; i < pg.tasks.size(); i++) {
            System.out.println(pg.tasks.get(i).name + "   " + pg.tasks.get(i).duration + "   " + pg.tasks.get(i).machine);
        }*/

        for(int i = 0; i < pg.jobs.size(); i++) {
            System.out.println("Job " + i + ":  ");
            for(int j = 0; j < pg.jobs.get(i).size(); j++) {
                System.out.print("Task " + j + ":  ");
                System.out.println(pg.jobs.get(i).get(j).name + " " + pg.jobs.get(i).get(j).duration + " " + pg.jobs.get(i).get(j).machine);
            }
        }

        /*System.out.println("  ");
        for(int i = 0; i < pg.jobs.size(); i++) {
            System.out.println("Job " + i);
            for(int j = 0; j < pg.jobs.get(i).size(); j++) {
                System.out.print(pg.jobs.get(i).get(j).name + " ");
            }
            System.out.println(" ");
        }

        System.out.println("Anzahl Maschinen: " + pg.anzahlMachines);*/
    }


    public Node FindTaskMenge() {
        for (int i = 0; i < pUndMList.getLength(); i++) {
            Node currentNode = pUndMList.item(i);
            NamedNodeMap attributes = currentNode.getAttributes();
            int lastAttributeIndex = attributes.getLength() - 1;
            Node lastAttribute = attributes.item(1);

            if (Objects.equals(lastAttribute.getNodeValue(), "P")) {
                return currentNode;
            }
        }
        return null;
    }

    public Node FindMachineMenge() {
        for (int i = 0; i < pUndMList.getLength(); i++) {
            Node currentNode = pUndMList.item(i);
            NamedNodeMap attributes = currentNode.getAttributes();
            int lastAttributeIndex = attributes.getLength() - 1;
            Node lastAttribute = attributes.item(lastAttributeIndex);

            if (lastAttribute.getNodeValue().toString().equals("M")) {
                return currentNode;
            }
        }
        return null;
    }

    public List[] CreateTasks() {
        List<Task> tempPList = new ArrayList<>();
        List<Integer> tempMList = new ArrayList<>();

        Node currentNode;

        for (int i = 0; i < selectedPandD.getLength(); i++) {
            currentNode = selectedPandD.item(i);
            //Erst wenn eine Node/ Feature 2 Attribute hat, wurde es ausgewählt
            //manual/automatic = selected/unselected
            //name
            if (currentNode.getAttributes().getLength() == 2) {
                NamedNodeMap attributes = currentNode.getAttributes();
                if (attributes.item(0).getNodeValue().equals("selected")) {
                    //Checken ob es eine Task oder eine Duration ist
                    String name = attributes.item(1).getNodeValue();
                    if (name.startsWith("p")) {






                        //tempPList.add(new Task(-1, -1, name));







                    }

                    //Falls es eine Duration ist
                    if (name.startsWith("d")) {
                        //Damit das d entfernt wird
                        String substring = name.substring(1);
                        String[] durationSplits = substring.split(" ");

                        //Zugehörige Task finden
                        Task task = tempPList.stream()
                                .filter(t -> durationSplits[0].equals(t.name))
                                .findAny()
                                .orElse(null);

                        if (task != null) {
                            //Task die Duration zuweisen





                            //task.duration = Integer.parseInt(durationSplits[2]);





                        }
                    }

                    //Falls es eine Maschine ist
                    if (name.startsWith("m")) {
                        //Weil es wie m1, m2, etc. aussehen wird
                        String substring = name.substring(1);
                        tempMList.add(Integer.parseInt(substring));
                    }
                }
            }
        }

        List[] returnArray = {tempMList, tempPList};
        return returnArray;
    }

    public void MachineAllocation() {
        Node currentNode;

        for (int i = 0; i < constraintList.getLength(); i++) {
            //Die <imp>s durchgehen
            currentNode = constraintList.item(i);
            //Children des aktuellen <imp>
            NodeList childNodes = currentNode.getChildNodes();
            List<Node> childElements = new ArrayList<>();

            //getChildNodes gibt irgendwie Nodes und Elements zurück? keine Ahnung, aber Elements sind die <var>s
            //deswegen kommen die nochmal in eine eigene List
            for (int k = 0; k < childNodes.getLength(); k++) {
                if (childNodes.item(k).getNodeType() == Node.ELEMENT_NODE) {
                    childElements.add(childNodes.item(k));
                }
            }


            //Wenn die zweite Childnode/Element mit p startet, ist es eine Reihenfolge
            //Wenn es mit m startet, dann eine Zurodnung p -> m
            if (childElements.get(1).getTextContent().startsWith("m")) {
                Task task = tasks.stream()
                        .filter(t -> childElements.get(0).getTextContent().equals(t.name))
                        .findAny()
                        .orElse(null);
                if (task != null) {
                    //machine ist der Wert der zweiten Childnode, aber das m vorne weggenommen (also 1, 2, ...)
                    task.machine = Integer.parseInt(childElements.get(1).getTextContent().substring(1));
                }
            }
        }
    }

    public List<List<Task>> SortTasks() {
        Node currentNode;

        //Liste mit Orderpaaren {[p3,p2], [p2,p1], [p5,p4], ...}
        List<String[]> orderList = new ArrayList<>();

        //Tasks, die noch keinem Job zugeordnet wurden. Wenn sie zugeordnet werden, werden sie entfernt
        //Wenn am Ende noch welche drin sind, sind das unabhängige Tasks in einem eigenen Job
        Set<Task> unusedTasks = new HashSet<>(tasks);
        //Ist am Anfang leer, wenn
        List<Task> usedTasks = new ArrayList<>();

        for(int i = 0; i < constraintList.getLength(); i++) {
            currentNode = constraintList.item(i);
            List<Node> tempElements = new ArrayList<>();

            //Jede Iteration ist die Liste nur 2 groß und hat zwei <var>...</var>
            NodeList childNodes = currentNode.getChildNodes();

            //Liste mit den <var>s für die Reihenfolge der Tasks erstellen
            for(int j = 0; j < childNodes.getLength(); j++) {
                if(childNodes.item(j).getNodeType() == Node.ELEMENT_NODE) {
                    tempElements.add(childNodes.item(j));
                }
            }

            //Inhalt von zweiten Var
            if(tempElements.get(1).getTextContent().startsWith("p")) {
                String task1 = tempElements.get(0).getTextContent();
                String task2 = tempElements.get(1).getTextContent();
                orderList.add(new String[]{task1, task2});
            }



        }

        //Enthält am Ende alle Jobs (Job = Liste mit Tasks)
        List<List<Task>> jobs = new ArrayList<>();

        //orderList ist fertig aufgegüllt
        //Beispiel: orderList = {[p3,p2], [p2,p1], [p5,p4]} und P = {p1,p2,p3,p4,p5,p6}
        //Dann J1=p1,p2,p3  J2=p4,p5   J3=p6
        //Die tasks, die in der orderList bei keinem Orderpaar an erster Stelle stehen, sind Starttasks
        //Zuerst alle Tasks die bei den Orderpaaren an erster Stelle stehen in notStarter einfügen
        Set<Task> notStarter = new HashSet<>();
        for(int i = 0; i < orderList.size(); i++) {
            //Passende Task zu dem Namensstring finden
            int finalI = i;
            Task task = unusedTasks.stream()
                    .filter(t -> orderList.get(finalI)[0].equals(t.name))
                    .findAny()
                    .orElse(null);
            //Task zu den nicht-Startertasks hinzufügen (da sie an erster Stelle stehen)
            if(task != null) {
                notStarter.add(task);
            }
        }

        //Startertasks sind die Differenz von unusedTasks \ notStarter
        unusedTasks.removeAll(notStarter);
        //unusedTasks enthält jetzt nurnoch Tasks die auch Startertasks sind
        //Für jede unusedTask einen Job erstellen
        System.out.println(("Anzahl Startertasks: " + unusedTasks.size()));
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
            for(int j = 0; j < orderList.size(); j++) {
                if(orderList.get(j)[1].equals(currentTask.name)) {
                    //Task mit dem Namen <p2> von der Relation [p2,p1] finden
                    int finalJ = j;
                    Task task = notStarter.stream()
                            .filter(t -> orderList.get(finalJ)[0].equals(t.name))
                            .findAny()
                            .orElse(null);
                    if(task != null) {
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
                for(int j = 0; j < orderList.size(); j++) {
                    if(orderList.get(j)[1].equals(tempPreviousTask)) {
                        taskFound = true;
                        int finalJ = j;
                        Task task = notStarter.stream()
                                .filter(t -> orderList.get(finalJ)[0].equals(t.name))
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

        return jobs;
    }

}