import org.jgrapht.graph.DefaultEdge;

import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.jgrapht.*;
import org.jgrapht.graph.*;
import org.jgrapht.traverse.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;



public class Main {

    public static void main(String[] args) throws IOException, ParseException {



        ////////////////////////////////////////////////////////////////////////////////////////////////////////
        ///                                         Read File                                                ///
        ////////////////////////////////////////////////////////////////////////////////////////////////////////
//        String filename = "/Users/jessica.a.wu/Documents/Personal/2019/ESIPS/AllImplementation/ColesDataProcessing/CompleteBuckets/Trimsim2=300-p1-ind4-r1-ns-sp-ll.txt";
        String filename = "/Users/jessica.a.wu/Documents/Personal/2019/ESIPS/AllImplementation/ColesDataProcessing/CompleteBuckets/trim3flipped276-p1-ind4-r1-ns-sp-ll.txt";
        File file = new File(filename);
        Scanner sc = new Scanner(file);
        String line = "";


        ////////////////////////////////////////////////////////////////////////////////////////////////////////
        ///                              Extract Graph Structure                                             ///
        ////////////////////////////////////////////////////////////////////////////////////////////////////////
        ArrayList<String> GraphStructureString = new ArrayList<>();

        BayesianNetwork tDBN = new BayesianNetwork();
        tDBN.addTimeSlice();
        tDBN.addTimeSlice();

        int timeCount = 1;

        // Read text file
        while (sc.hasNextLine()) {

            // Temporarily stores previous line
            String tmp = line;
            line = sc.nextLine();
            if (line.contentEquals("") && !tmp.isEmpty()) {
            }

            // Condition to show that the inspected lines describe structure - line contains an arrow, and new line is empty
            if (tmp.contains("->") && line.contentEquals("")) {
                extractTimeSlice(tDBN, GraphStructureString);
                GraphStructureString.clear();
                continue;
            }

            if (line.contains("--------")) {
                timeCount++;
            }

            // Skip lines between time slice & CPT information
            if ((line.contentEquals("-----------------")) || (line.contentEquals("") && tmp.contentEquals("")) || (line.contentEquals("") && tmp.contains("-----------------"))) {
                GraphStructureString.clear();
                continue;
            }


            // Extract CPT information after all CPT lines are read
            if (line.contentEquals("") && !tmp.isEmpty()) {

                ExtractCPT(GraphStructureString, tDBN, timeCount);
                GraphStructureString.clear();

                continue;
            }

            GraphStructureString.add(line);
        }

//        System.out.println(tDBN.toString() + "\n");



        ////////////////////////////////////////////////////////////////////////////////////////////////////////
        ///                                    Set anomaly index of nodes                                    ///
        ////////////////////////////////////////////////////////////////////////////////////////////////////////

        String header = "es_rt__1,db_rt__1,login_rt__1,solr_rt__1,wcs_rt__1,odr_qt__1,es_rt__2,db_rt__2,login_rt__2,solr_rt__2,wcs_rt__2,odr_qt__2,es_rt__3,db_rt__3,login_rt__3,solr_rt__3,wcs_rt__3,odr_qt__3,es_rt__4,db_rt__4,login_rt__4,solr_rt__4,wcs_rt__4,odr_qt__4,es_rt__5,db_rt__5,login_rt__5,solr_rt__5,wcs_rt__5";

        // Get symptom node
        String StartNodeName = "db_rt__1";
        int timePadding = 1;

        // Get time that event at symptom node took place.
        int startSeconds = stringToMinutes("00:00:00");
        int endSeconds = stringToMinutes("00:24:04");


        // S: es_rt__2, RC: odr_qt__2
//        String currentVals = "60000.00,250.00,1200000.00,200000.00,200000.00,0.12,18180000.00,300,242400000.00,60600000.00,60600000.00,302.11,18180000.00,300,6544800000.00,1605900000.00,60600000.00,302.11,27270000.00,300,363600000.00,818100000.00,60600000.00,302.11,60000.00,260,1200000.00,100000.00,200000.00,0.70\n";
//        String currentVals = "75000.00,260.00,3200000.00,200000.00,200000.00,0.10,60000.00,260.00,1200000.00,200000.00,100000.00,0.10,75000.00,250.00,3600000.00,200000.00,200000.00,0.37,6465000.00,230.00,85200000.00,10100000.00,200000.00,0.37,53610000.00,330.00,64800000.00,45100000.00,61900000.00,0.37,75000.00,250.00,3200000.00,200000.00,200000.00,0.02";
        String currentVals = "19920000,260,91600000,5300000,32600000,301.4,19920000,260,91600000,5300000,32600000,301.4,19920000,260,91600000,5300000,32600000,301.4,19920000,260,91600000,5300000,32600000,301.4,19920000,260,91600000,5300000,32600000,301.4,19920000,260,91600000,5300000,32600000,301.4";
        // Populate tDBN with values
        String[] headerList = header.split(",");
        String[] currentValsList = currentVals.split(",");

        HashMap<String, Node> allNodes = new HashMap<>();

        int count = 0;

        for (String substr: headerList) {
            String name = substr.split("__")[0];
            int timeSlice = Integer.valueOf(substr.split("__")[1]);

            Node n = tDBN.getNode(timeSlice, name);

            n.setValue(Double.parseDouble(currentValsList[count]));


            allNodes.put(substr, n);

            count ++;
        }

        // Extract AnomalyInfo in that same time period.
        HashMap<String, ArrayList> AnomalyData = ExtractAnomalyData("/Users/jessica.a.wu/Documents/Personal/2019/ESIPS/AllImplementation/AnomalyDetectionLuminol/Output.txt", startSeconds, endSeconds);

        System.out.println("AnomalyData size:" + AnomalyData.size());

        // Set the anomalous nodes with their anomaly levels
        for (Map.Entry<String, ArrayList> entry: AnomalyData.entrySet()) {
//            System.out.println(entry.getKey() + ": " + entry.getValue());

            // Calculate time slice of anomalies from the time.
            int timeSlice = (minuteToTS((int) entry.getValue().get(0)) + timePadding )%6;



            String nodeName = entry.getKey() + "__"+ (timeSlice);
            Double anomalyLevel = (Double) entry.getValue().get(1);

            System.out.println(nodeName + " : " + anomalyLevel);


            Node anomalyNode = allNodes.get(nodeName);
            anomalyNode.setAnomalyIndex(anomalyLevel);

        }




        System.out.println("About to carry out fault propagation... \n");
        tDBN.WangFaultProp(allNodes.get(StartNodeName), 4.0);

//        for (ArrayList<Node> SinglePath: path) {
//            for (Node n: SinglePath) {
//                System.out.println(n.getName() + "[" + n.getTimeSlice() + "] val = " + n.getCurrentValue() + ", as = " + n.getAnomalyIndex());
//            }
//        }
    }

    /*
    Convert each timeslice stanza into object. Sets up "skeleton" for DBN.

    @param tDBN BayesianNetwork object which will be expanded
    @param graphStructureString String that will be converted to object
    @return void
    */
    private static void extractTimeSlice(BayesianNetwork tDBN, ArrayList<String> graphStructureString) {

        for (int linkInd = 0; linkInd < graphStructureString.size(); linkInd++) {
            // Split each line into parent and children strings
            String[] link = graphStructureString.get(linkInd).split(" -> ");
            String parentName = link[0].split("\\[")[0];
            int parentTimeSlice = Integer.valueOf(link[0].split("\\[")[1].split("]")[0]);

            String childName = link[1].split("\\[")[0];
            int childTimeSlice = Integer.valueOf(link[1].split("\\[")[1].split("]")[0]);

            // Check number of time slices in DBN. Increase as needed.
            tDBN.checkAddSlice(childTimeSlice);
            tDBN.checkAddSlice(parentTimeSlice);


            // Create parent and child node if they don't exist and add into graph
            HashMap<String, Node> parentSlice = tDBN.getSlice(parentTimeSlice);
            HashMap<String, Node> childSlice = tDBN.getSlice(childTimeSlice);
            Node parent;
            Node child;

            if (!parentSlice.containsKey(parentName)) {
                parent = new Node();
                parent.setName(parentName);
                parent.setTimeSlice(parentTimeSlice);
                tDBN.addNodeInTimeSlice(parentName, parent, parentTimeSlice);
            }

            if (!childSlice.containsKey(childName)) {
                child = new Node();
                child.setName(childName);
                child.setTimeSlice(childTimeSlice);
                tDBN.addNodeInTimeSlice(childName, child, childTimeSlice);
            }

            // Creating link between parent and child
            parent = tDBN.getNode(parentTimeSlice, parentName);
            child = tDBN.getNode(childTimeSlice, childName);
            parent.addChild(child);
            child.addParent(parent);
        }
    }



    /*
    Extract CPT information and Populates DBN with this CPT information.

    @param ArrayList<String> graphStructureString All strings containing CPT parameter information.
    @param BayesianNetwork tDBN DBN that will be populated.
    @param int timeslice Specific timeslice that will be populated in the DBN.
    @return void
    */
    private static void ExtractCPT(ArrayList<String> graphStructureString, BayesianNetwork tDBN, int timeslice) {
        String cptHeader = graphStructureString.get(0);
//        System.out.println("graph structure string: " + graphStructureString);


        // Get name and find node
        String name = cptHeader.split(":")[0];

        Node node = tDBN.getNode(timeslice, name);

        // Add list of values to node
        String values = ((cptHeader.split(":")[1]).split("\\[")[1]).split("]")[0];
        String[] valuesList = values.split(", ");
        node.addValues(valuesList);

        // Pop off CPT header string
        graphStructureString.remove(0);

        try {
            node.populateCPT(graphStructureString);
        } catch (Exception e) {
            e.printStackTrace();

        }
    }


    /*
    Extract anomaly information from AnomalyDetectionLuminol unit.

    @param String filepath Full file path to the text file output of ADLuminol.py.
    @param int lowerTimeLimit earliest time slice we are looking at.
    @param int upperTimeLimit latest time slice we are looking at.
    @return Hashmap<String, Double> Hashmap with key: anomalous node name, value: timestamp, anomaly severity - for the nodes within the time slice of interest.
     */
    private static HashMap<String, ArrayList> ExtractAnomalyData(String filepath, int lowerTimeLimit, int upperTimeLimit) throws FileNotFoundException, ParseException {
        File file = new File(filepath);

        Scanner sc = new Scanner(file);


        // Key: name of node, Value: time slice, anomaly score
        HashMap<String, ArrayList> anomalyInfo = new HashMap<>();

        String name = "";
        int minuteTS;

        while (sc.hasNextLine()) {
            String line = sc.nextLine();


            // Extract name of anomalous node
            if (line.contains("Name:")) {
                name = line.split(":")[1];
            }

            // Extract time slice
            if (line.contains("TS:")) {
                String timestr = line.split("\\(")[1].split("\\)")[0];
                timestr = timestr.replaceAll("'", "");
                timestr = timestr.split(", ")[0].split(" ")[1];
                minuteTS = stringToMinutes(timestr);

                // If the anomaly is in the time slice we are interested in, extract info
                if ((minuteTS >= lowerTimeLimit) && (minuteTS <= upperTimeLimit)) {
                    Double AS = Double.parseDouble(line.split("\\|")[1]);
                    ArrayList EventInfo = new ArrayList();
                    EventInfo.add(minuteTS);
                    EventInfo.add(AS);
                    anomalyInfo.put(name,  EventInfo);
                }
            }




        }

        return anomalyInfo;
    }

    private static int stringToMinutes(String line) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        Date reference = dateFormat.parse("00:00:00");
        Date date = dateFormat.parse(line);
        long seconds = (date.getTime() - reference.getTime()) / 1000L;
        long minutes = seconds/60;
        return (int) minutes;

    }

    private static int minuteToTS(int minutes) {
        int hour = minutes/60;

        return (int) Math.floor(hour/4);
    }

}
