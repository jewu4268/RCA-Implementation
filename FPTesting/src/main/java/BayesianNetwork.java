import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.jgrapht.*;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.*;
import org.jgrapht.traverse.*;
import sun.security.x509.CertificatePolicyMap;

import java.util.*;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.logging.Logger;


public class BayesianNetwork {

//    private final static Logger LOGGER = Logger.getLogger(BayesianNetwork.class.getName());

    //Array with index corresponding to timeslice. Each element in array will contain a hashmap with the key being the node name and value being the Object
    ArrayList<HashMap<String,Node>> GraphArr = new ArrayList<>();



    ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///                                                 Get functions                                      ///
    //////////////////////////////////////////////////////////////////////////////////////////////////////////

    public ArrayList<HashMap<String,Node>> getGraphArr() {
        return this.GraphArr;
    }

    public HashMap<String,Node> getSlice(int index) {
        return GraphArr.get(index);
    }

    public Node getNode(int timeSlice, String name) {
        Map<String,Node> slice = GraphArr.get(timeSlice);
        return slice.get(name);
    }

    public int getTotalSlices() {
        return this.GraphArr.size();
    }



    ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///                                              Add functions                                         ///
    //////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void addTimeSlice() {
        HashMap<String,Node> emptyMap = new HashMap<>();
        this.GraphArr.add(emptyMap);
    }

    public Node addNodeInTimeSlice(String name, Node node, int TimeSlice) {
        (this.GraphArr.get(TimeSlice)).put(name, node);
        node.setTDBN(this);
        return node;
    }

    public void checkAddSlice(int index) {
        if (index >= this.getTotalSlices()) {
            this.addTimeSlice();
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///                                     Fault Propagation Functions                                    ///
    //////////////////////////////////////////////////////////////////////////////////////////////////////////


    /*
    Find all possible root causes based off their associated DBCI index, and their associated fault propagation path.

    @param Node faultyNode Symptom node. Fault propagation will start from here and work backwards along reverse arcs.
    @return ArrayList<ArrayList<Node>> An arraylist of fault propagation paths (arrays).
     */
    public  ArrayList<ArrayList<Node>> ProposeFaultProp(Node faultyNode) throws IOException {
        // Contains all heuristic val & associated nodes.
        MultiValuedMap<Double, Node> CFMap = new ArrayListValuedHashMap<>();
        CFMap.put(CalculateCF(faultyNode) + 0.00001, faultyNode);

        // Clone and add first node to the graph
        Node root = faultyNode;
        Graph<Node, DefaultEdge> faultGraph = new DefaultDirectedGraph<>(DefaultEdge.class);

        // Use a queue to store nodes to explore, and nodes that have already been explored during the BFS traversal.
        Queue<Node> toExplore = new LinkedList<>();
        Queue<Node> explored = new LinkedList<>();
        toExplore.add(root);

        /*
         * Until there are no more nodes to explore, explores anomalous nodes through BFS, sorts nodes in sorted
         * tree, prunes sorted nodes, stores in queue, saves nodes into a faulty graph
         * */

        while (toExplore.size() > 0 ) {

            // Find head
            Node head = toExplore.peek();

            // Add  parents of current node to parentDict
            Map<Node, Double> parentDict = new HashMap<> ();

            for (int parentInd = 0; parentInd < head.getParents().size(); parentInd++) {

                Node parent = head.getParents().get(parentInd);
                System.out.println("parent: " + parent.getName() + "[" + parent.getTimeSlice() + "], " + parent.anomalyIndex );

                // If parent isn't anomalous, skip.
                if (parent.getAnomalyIndex() == 0) {
                    continue;
                }
                // Otherwise, add anomalous parent to unsorted list
                else {
                    parentDict.put(parent, parent.getAnomalyIndex());
                }

            }

            // Add anomalous parents to faultgraph
            for (Map.Entry<Node, Double> entry: parentDict.entrySet()) {
                Node node = entry.getKey();
                head.addFaultyParent(node);
                node.addFaultyChild(head);

                // Add every node in faultgraph to CF map
                CFMap.put(CalculateCF(node), node);

                faultGraph.addVertex(node);
                if (!faultGraph.containsVertex(head)) {
                    faultGraph.addVertex(head);
                }
                faultGraph.addEdge(node,head);
                toExplore.add(node);
            }

            // Remove head of toExplore queue into explored queue
            explored.add(toExplore.remove());
        }



        // Print out the graph to be sure it's really complete
//        Iterator<Node> iter = new DepthFirstIterator<>(faultGraph);
//        while (iter.hasNext()) {
//            Node vertex = iter.next();
//            System.out.println(
//                    "Vertex " + vertex + "(" + vertex.getName() + "[" + vertex.getTimeSlice() + "]" + " is connected to: "
//                            + faultGraph.edgesOf(vertex).toString());
//        }
//        System.out.println();


        // Now that we have this shitty cyclic grpah, we want to find possible paths from root to all leaves.
        // First, find leaves in this fault graph. This is the root causes.
        Iterator<Node> iter2 = new DepthFirstIterator<>(faultGraph);
        List<Node> leaves = new ArrayList<>();

        while (iter2.hasNext()) {

            // Calculate and set CF for every faulty node
            Node vertex = iter2.next();

            CalculateCF(vertex);


            if (vertex.getFaultyParents().size() == 0) {
                leaves.add(vertex);
                System.out.println("We found a leaf: " + vertex.getName() + "[" + vertex.getTimeSlice() + "]");
            }
        }

        System.out.println("Number of leaves: " + leaves);


        // Next, carry out dijkstras for each root to leaf to get possible paths.
        ArrayList<ArrayList<Node>> allPaths = new ArrayList<>();

        for (Node leaf: leaves) {
            System.out.println("leaf.getCF(): " + CalculateCF(leaf));
            CFMap.put(CalculateCF(leaf), leaf);

            // Finds the paths between 2 different nodes
            List<GraphPath<Node,DefaultEdge>> allJgraphPath = new AllDirectedPaths(faultGraph).getAllPaths(leaf, root,true, 10);
//            System.out.println("Number of paths between: " + leaf.getName() + "[" + leaf.getTimeSlice() + "] and " + tmp.getName() + "[" + tmp.getTimeSlice() + "] = "  + allJgraphPath.size());

            // ConvertGraphPath object to arraylist
            for (int i = 0; i < allJgraphPath.size(); i ++) {
                GraphPath<Node,DefaultEdge> singleJpath = allJgraphPath.get(i);

                ArrayList<Node> singlePath = new ArrayList<>();
                for (Node n: singleJpath.getVertexList()) {
                    singlePath.add(n);
                }
                allPaths.add(singlePath);

            }
        }

        System.out.println("List of paths: ");
        System.out.println(jPathtoString(allPaths));


        // Finally displaying most likely root cause and output DBCI values to csv.
        //TODO: Handle duplicate CFValues
        String DBCIPath = "/Users/jessica.a.wu/Documents/Personal/2019/ESIPS/AllImplementation/ColesDataProcessing/ContributionFactorData/Proposed:DBCI_" + faultyNode.getName() + "[" + faultyNode.getTimeSlice() + "]";
        FileWriter fileWriter = new FileWriter(DBCIPath, true);
        BufferedWriter writer = new BufferedWriter(fileWriter);

        System.out.println("Most likely root cause:   " );

        System.out.println("size of cf map = " + CFMap.size());
        // Write DBCI values to DBCI.csv file - graph this.
        for (Map.Entry<Double,Node> entry: CFMap.entries()) {
            Node n = entry.getValue();
            System.out.println(entry.getKey() + ":"+entry.getValue().getName() + "[" + n.getTimeSlice() + "]");
            String DBCIInfo = n.getName() + "[" + n.getTimeSlice() + "]," + entry.getKey() + "\n";
            writer.append(DBCIInfo);
        }

        // Removing the faulty node from pool of root cause candidates.
        CFMap.remove(CalculateCF(faultyNode));


        System.out.println("Size of CFMap after: " + CFMap.size());

        // If there is not fault path, then return the problem node as the root cause.
        if (CFMap.size() == 0) {
            ArrayList<Node> singularNode = new ArrayList<>();
            singularNode.add(faultyNode);
            allPaths.add(singularNode);
            System.out.print("RCA infers that anomalous node is the RC");
            System.out.println(faultyNode.getName() + "[" + faultyNode.getTimeSlice() + "]" );
            return allPaths;
        }

        Double max = Collections.max(CFMap.keys());

        for (Map.Entry<Double,Node> entry: CFMap.entries()) {
            Node n = entry.getValue();
            if (entry.getKey().equals(max)) {
                System.out.println(n.getName() + "[" + n.getTimeSlice() + "]");
            }
        }
        writer.close();

        return allPaths;
    }


    public  ArrayList<ArrayList<Node>> MorifaultProp(Node faultyNode) throws IOException {

        MultiValuedMap<Double, Node> CFMap = new ArrayListValuedHashMap<>();


        // Clone and add first node to the graph
        Node root = faultyNode;
        Node tmp = faultyNode;
        Graph<Node, DefaultEdge> faultGraph = new DefaultDirectedGraph<>(DefaultEdge.class);

        // Use a queue to store nodes to explore, and nodes that have already been explored during the BFS traversal.
        Queue<Node> toExplore = new LinkedList<>();
        Queue<Node> explored = new LinkedList<>();
        toExplore.add(root);

        /**
         * Until there are no more nodes to explore, explores anomalous nodes through BFS, sorts nodes in sorted
         * tree, prunes sorted nodes, stores in queue, saves nodes into a faulty graph
         * **/

        CFMap.put(CalculateMoriHeuristic(faultyNode)+0.00001, faultyNode);


        while (toExplore.size() > 0 ) {

            // Find head
            Node head = toExplore.peek();




            // Add  parents of current node to parentDict
            Map<Node, Double> parentDict = new HashMap<> ();

            for (int parentInd = 0; parentInd < head.getParents().size(); parentInd++) {


                Node parent = head.getParents().get(parentInd);

                // Set DBCI value.
                if (parent.getTimeSlice() != 0) {
                    CFMap.put(CalculateMoriHeuristic(parent), parent);
                }

                // If parent passes CI threshold, add to unsorted list
                if (CalculateMoriHeuristic(parent) < 0.00001) {
                    continue;
                }
                else {
                    parentDict.put(parent, CalculateMoriHeuristic(parent));
                }

            }

            // Add anomalous parents to faultgraph
            for (Map.Entry<Node, Double> entry: parentDict.entrySet()) {
                Node node = entry.getKey();
                head.addFaultyParent(node);
                node.addFaultyChild(head);

                faultGraph.addVertex(node);
                if (!faultGraph.containsVertex(head)) {
                    faultGraph.addVertex(head);
                }
                faultGraph.addEdge(node,head);
                toExplore.add(node);
            }

            // Remove head of toExplore queue into explored queue
            explored.add(toExplore.remove());
        }


        // Now that we have this shitty cyclic grpah, we want to find possible paths from root to all leaves.
        // First, find leaves in this fault graph. This is the root causes.
        Iterator<Node> iter2 = new DepthFirstIterator<>(faultGraph);
        List<Node> leaves = new ArrayList<>();

        while (iter2.hasNext()) {

            Node vertex = iter2.next();
            if (vertex.getFaultyParents().size() == 0) {
                leaves.add(vertex);
                System.out.println("We found a leaf: " + vertex.getName() + "[" + vertex.getTimeSlice() + "]");
            }
        }

        System.out.println("Number of leaves: " + leaves);

        // Next, carry out dijkstras for each root to leaf to get possible paths.
        ArrayList<ArrayList<Node>> allPaths = new ArrayList<>();

        for (Node leaf: leaves) {

            // Finds the paths between 2 different nodes
            List<GraphPath<Node,DefaultEdge>> allJgraphPath = new AllDirectedPaths(faultGraph).getAllPaths(leaf, tmp,true, 10);

            // ConvertGraphPath object to arraylist
            for (int i = 0; i < allJgraphPath.size(); i ++) {
                GraphPath<Node,DefaultEdge> singleJpath = allJgraphPath.get(i);

                ArrayList<Node> singlePath = new ArrayList<>();
                for (Node n: singleJpath.getVertexList()) {
                    singlePath.add(n);
                }
                allPaths.add(singlePath);

            }
        }

        System.out.println("List of paths: ");
        System.out.println(jPathtoString(allPaths));


        // Finally displaying most likely root cause and output DBCI values to csv.
        //TODO: Handle duplicate CFValues
        String DBCIPath = "/Users/jessica.a.wu/Documents/Personal/2019/ESIPS/AllImplementation/ColesDataProcessing/ContributionFactorData/Mori:DBCI__" + faultyNode.getName() + "[" + faultyNode.getTimeSlice() + "]";
        FileWriter fileWriter = new FileWriter(DBCIPath, true);
        BufferedWriter writer = new BufferedWriter(fileWriter);

        System.out.println("Most likely root cause:   " );

        // Write DBCI values to DBCI.csv file - graph this.
        for (Map.Entry<Double,Node> entry: CFMap.entries()) {
            Node n = entry.getValue();
            String DBCIInfo = n.getName() + "[" + n.getTimeSlice() + "]," + entry.getKey() + "\n";
            writer.append(DBCIInfo);
            System.out.println(DBCIInfo);

        }

        // Removing the faulty node from pool of root cause candidates.
        CFMap.remove(CalculateMoriHeuristic(faultyNode));

        // If there is not fault path, then return the problem node as the root cause.
        if (CFMap.size() == 0) {
            ArrayList<Node> singularNode = new ArrayList<>();
            singularNode.add(faultyNode);
            allPaths.add(singularNode);
            System.out.print("RCA infers that anomalous node is the RC");
            System.out.println(faultyNode.getName() + "[" + faultyNode.getTimeSlice() + "]" );
            return allPaths;
        }


        Double max = Collections.max(CFMap.keys());

        for (Map.Entry<Double,Node> entry: CFMap.entries()) {
            Node n = entry.getValue();
            if (entry.getKey().equals(max)) {
                System.out.println(n.getName() + "[" + n.getTimeSlice() + "]");
            }
        }
        writer.close();

        return allPaths;
    }


    /*
     Implementation of Wang's fault propagation algorithm

     @param Node faultyNode Starting symptom node.
     @param Double pruneThreshold Threshold for anomaly index.
     @return Double heuristic value
      */
    public  ArrayList<ArrayList<Node>> WangFaultProp(Node faultyNode, Double pruneThreshold) throws IOException {
        // Contains all heuristic val & associated nodes.
        MultiValuedMap<Double, Node> CFMap = new ArrayListValuedHashMap<>();


        // Clone and add first node to the graph
        Node root = faultyNode;
        Graph<Node, DefaultEdge> faultGraph = new DefaultDirectedGraph<>(DefaultEdge.class);

        // Use a queue to store nodes to explore, and nodes that have already been explored during the BFS traversal.
        Queue<Node> toExplore = new LinkedList<>();
        Queue<Node> explored = new LinkedList<>();
        toExplore.add(root);


        CFMap.put(CalculateWangHeuristic(faultyNode), faultyNode);

        /*
         * Until there are no more nodes to explore, explores anomalous nodes through BFS, sorts nodes in sorted
         * tree, prunes sorted nodes, stores in queue, saves nodes into a faulty graph
         */
        Random ranDouble = new Random();


        while (toExplore.size() > 0 ) {

            // Find head
            Node head = toExplore.peek();

            // Add  parents of current node to parentDict
            Map<Node, Double> parentDict = new HashMap<> ();

            for (int parentInd = 0; parentInd < head.getParents().size(); parentInd++) {

                Node parent = head.getParents().get(parentInd);
                System.out.println("parent: " + parent.getName() + "[" + parent.getTimeSlice() + "], " + parent.getAnomalyIndex() );

                // Add parent to parent dict if parent is above anomaly severity threshold.
                if (parent.getAnomalyIndex() < pruneThreshold) {
                    continue;
                }
                else {
                    parentDict.put(parent, parent.getAnomalyIndex());
                }

            }

            // Add anomalous parents to faultgraph
            for (Map.Entry<Node, Double> entry: parentDict.entrySet()) {
                Node node = entry.getKey();
                head.addFaultyParent(node);
                node.addFaultyChild(head);

                // Add every node in faultgraph to CF map
                CFMap.put(CalculateWangHeuristic(node) + ranDouble.nextDouble(), node);

                faultGraph.addVertex(node);
                if (!faultGraph.containsVertex(head)) {
                    faultGraph.addVertex(head);
                }
                faultGraph.addEdge(node,head);
                toExplore.add(node);
            }

            // Remove head of toExplore queue into explored queue
            explored.add(toExplore.remove());
        }


        // Print out the graph to be sure it's really complete
        Iterator<Node> iter = new DepthFirstIterator<>(faultGraph);
        while (iter.hasNext()) {
            Node vertex = iter.next();
            System.out.println(
                    "Vertex " + vertex + "(" + vertex.getName() + "[" + vertex.getTimeSlice() + "]" + " is connected to: "
                            + faultGraph.edgesOf(vertex).toString());
        }
        System.out.println();



        // Find possible paths from root to all leaves.
        // First, find leaves in this fault graph.
        Iterator<Node> iter2 = new DepthFirstIterator<>(faultGraph);
        List<Node> leaves = new ArrayList<>();

        while (iter2.hasNext()) {

            // Calculate and set CF for every faulty node
            Node vertex = iter2.next();

            CalculateWangHeuristic(vertex);


            if (vertex.getFaultyParents().size() == 0) {
                leaves.add(vertex);
                System.out.println("We found a leaf: " + vertex.getName() + "[" + vertex.getTimeSlice() + "]");
            }
        }

        System.out.println("Number of leaves: " + leaves);


        // Next, carry out dijkstras for each root to leaf to get possible paths.
        ArrayList<ArrayList<Node>> allPaths = new ArrayList<>();

        for (Node leaf: leaves) {
            System.out.println("leaf.getCF(): " + CalculateWangHeuristic(leaf));
            CFMap.put(CalculateWangHeuristic(leaf), leaf);

            // Finds the paths between 2 different nodes
            List<GraphPath<Node,DefaultEdge>> allJgraphPath = new AllDirectedPaths(faultGraph).getAllPaths(leaf, root,true, 10);

            // ConvertGraphPath object to arraylist
            for (int i = 0; i < allJgraphPath.size(); i ++) {
                GraphPath<Node,DefaultEdge> singleJpath = allJgraphPath.get(i);

                ArrayList<Node> singlePath = new ArrayList<>();
                for (Node n: singleJpath.getVertexList()) {
                    singlePath.add(n);
                }
                allPaths.add(singlePath);

            }
        }

        System.out.println("List of paths: ");
        System.out.println(jPathtoString(allPaths));


        // Finally displaying most likely root cause and output DBCI values to csv.
        //TODO: Handle duplicate CFValues
        String DBCIPath = "/Users/jessica.a.wu/Documents/Personal/2019/ESIPS/AllImplementation/ColesDataProcessing/ContributionFactorData/Wang:" + faultyNode.getName() + "[" + faultyNode.getTimeSlice() + "]";
        FileWriter fileWriter = new FileWriter(DBCIPath, true);
        BufferedWriter writer = new BufferedWriter(fileWriter);

        System.out.println("Most likely root cause:   " );
        System.out.println("Size of cp map: " + CFMap.size());

        // Write DBCI values to DBCI.csv file - graph this.
        for (Map.Entry<Double,Node> entry: CFMap.entries()) {
            Node n = entry.getValue();
            System.out.println(entry.getKey() + ":"+entry.getValue().getName() + "[" + n.getTimeSlice() + "]");
            String DBCIInfo = n.getName() + "[" + n.getTimeSlice() + "]," + entry.getKey() + "\n";
            writer.append(DBCIInfo);
        }

        // Removing the faulty node from pool of root cause candidates.
        CFMap.remove(CalculateWangHeuristic(faultyNode));


        System.out.println("Size of CFMap after: " + CFMap.size());

        // If there is not fault path, then return the problem node as the root cause.
        if (CFMap.size() == 0) {
            ArrayList<Node> singularNode = new ArrayList<>();
            singularNode.add(faultyNode);
            allPaths.add(singularNode);
            System.out.println("RCA infers that anomalous node is the RC: ");
            System.out.println(faultyNode.getName() + "[" + faultyNode.getTimeSlice() + "]" );
            return allPaths;
        }

        Double max = Collections.max(CFMap.keys());

        for (Map.Entry<Double,Node> entry: CFMap.entries()) {
            Node n = entry.getValue();
            if (entry.getKey().equals(max)) {
                System.out.println(n.getName() + "[" + n.getTimeSlice() + "]");
            }
        }
        writer.close();

        return allPaths;
    }

    /*
    Points to correct calculation method for algorithm specified. Sets heuristic value for node.

    @param String algorithm Name of algorithm
    @param Node n heuristic will be calculated for this node.
    @return Double heuristic value
     */
    public Double CalculateHeuristic(String algorithm, Node n) {

        Double heuristic = -1.0;
        if (algorithm.equals("Proposed")) {
            heuristic =  CalculateCF(n);
        }
        else if (algorithm.equals("Mori")) {
            heuristic = CalculateMoriHeuristic(n);
        }
        else {
            heuristic = CalculateWangHeuristic(n);
        }

        n.setCF(heuristic);
        return heuristic;
    }



    /*
      Simplification of calculation of wang's heuristic. Instead of looking at the most probable fault propagation sequences, prioritise by the anomalousness.

      @param Node n heuristic will be calculated for this node.
      @return Double heuristic value
   */
    public Double CalculateWangHeuristic(Node n) {
        Double heuristic = -1.0;

        if (n.getTimeSlice() == 0) {
            return 0.0;
        }


        // Get conditional probability value
        System.out.println("Calculating Wang's heuristic for node " + n.getName() + "[" + n.getTimeSlice() + "]");


        heuristic = n.getAnomalyIndex();

        System.out.println("Wangs's heuristic for node : " + n.getName() + "[" + n.getTimeSlice() + "] :" + heuristic);


        return heuristic;

    }


    public Double CalculateMoriHeuristic(Node n) {
        if (n.getTimeSlice() == 0) {
            return 0.0;
        }

        // Get parents of node n
        HashMap<String, Double> parentVals = new HashMap<>();

        for (Node p: n.getParents()) {
            Node realP = this.getNode(p.getTimeSlice(), p.getName());
            String parentStr = realP.getName() + "[" + realP.getTimeSlice() + "]";
            parentVals.put(parentStr,realP.getCurrentValue());

        }

        //TODO: Eventually fix this coz it won't work for values it hasn't seen before.

        // Get conditional probability value
//        System.out.println("Calculating DBPI for node " + n.getName() + "[" + n.getTimeSlice() + "]");
        Double DBPI = CalculateDBPI(n, parentVals);

        System.out.println("Mori's DBCI for node : " + n.getName() + "[" + n.getTimeSlice() + "] :" + DBPI );

        return DBPI;

    }


    /*
       Calculate Contribution Factor (CF)

       @param Node n node that the CF will be calculated for.
       @return Double Contribution factor.
    */

    // TODO: Fix Problems:
    // As we are using a discrete probability distribution, this val index is problematic when we encounter continuous values.
    // Update DBPI to include average across time.
    public Double CalculateCF(Node n) {

        // Get parents of node n
        HashMap<String, Double> parentVals = new HashMap<>();
        for (Node p: n.getParents()) {
            Node realP = this.getNode(p.getTimeSlice(), p.getName());
            String parentStr = realP.getName() + "[" + realP.getTimeSlice() + "]";
            parentVals.put(parentStr,realP.getCurrentValue());

        }

        //TODO: Eventually fix this coz it won't work for values it hasn't seen before.

        // Get conditional probability value
        Double DBPI = CalculateDBPI(n, parentVals);
        Double CF = n.getAnomalyIndex() * DBPI;


        n.setCF(CF);

        System.out.println("CF for node : " + n.getName() + "[" + n.getTimeSlice() + "] :" + CF + " = " + n.getAnomalyIndex() + " X " + DBPI);

        return CF;
    }

    /*
    Calculate the DBPI of a node. A lower probablistic event = higher DBPI.

    @param Node n DBPI will be calculated for this node n.
    @param HashMap<String, Double> parentVals This is the events occurring at the parents. Will be used to lookup appropriate list of conditional probabilities.
    @return Double DBPI value.
     */
    public Double CalculateDBPI(Node n, HashMap<String, Double> parentVals) {

        // Sort values in ascending order & associated conditional probabilities.
        ArrayList<Double> CPEntry = n.getCPT().GetCPEntry(parentVals);
        ArrayList<Double> values = n.getValues();
        Map<Double, Double> PDFHash = new HashMap<>();

        for (int i = 0; i < values.size(); i++) {
            PDFHash.put(values.get(i), CPEntry.get(i));
        }

        Map<Double, Double> SortedPDF = PDFHash.entrySet()
                                                .stream()
                                                .sorted(Map.Entry.comparingByKey())
                                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));




        // Find mean val by finding associated value to max conditional probability.
        // Find max conditional probability.
        Map.Entry<Double, Double> maxEntry = null;

        for (Map.Entry<Double, Double> entry : SortedPDF.entrySet())
        {
            if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0)
            {
                maxEntry = entry;
            }
        }

        Double meanVal = maxEntry.getKey();

        // Find symmetrical upper and lower limits according relative to the mean. Assume that mean = value where conditional prob = max
        Double currentVal = n.getCurrentValue();
        Double upperLimit = 0.0;
        Double lowerLimit = 0.0;

        if (currentVal > meanVal) {
            upperLimit = currentVal;
            lowerLimit = 2 * meanVal - currentVal;
        }
        else {
            upperLimit = 2 * meanVal - currentVal;
            lowerLimit = currentVal;
        }


        System.out.println(lowerLimit + " < " + currentVal + " > " + upperLimit);

        // Sum all conditional probability values between these two points. (I,e taking the integral of est PDF)
        Double DBPI = 0.0;

        for (Map.Entry<Double, Double> entry: SortedPDF.entrySet()) {
            if ((entry.getKey() > lowerLimit) && (entry.getKey() < upperLimit)) {
                DBPI += entry.getValue();
//                System.out.println("Adding dBPI: " + DBPI);
            }
        }

        if (DBPI == 0.0) {
            DBPI = 0.00001;

            if (lowerLimit-upperLimit == 0) {
                DBPI = PDFHash.get(currentVal)/2.0;
//                System.out.println(n.nodeToString() + " has mean DBPI = " + DBPI);

            }

//            System.out.println(n.nodeToString() + " has DBPI = " + DBPI);

        }

        if (DBPI == 1.0) {
            DBPI = DBPI/2.0;

        }

        System.out.println(n.nodeToString() + "final dbpi: " + DBPI);


        return DBPI;
    }

    /*
       Prints all paths to strings

       @param ArrayList<ArrayList<Node>> allPaths All fault propagation paths.
       @return String Paths in form of string
    */

    public String jPathtoString(ArrayList<ArrayList<Node>> allPaths) {
        String pathStr = "";
        // ConvertGraphPath object to arraylist
        for (int i = 0; i < allPaths.size(); i ++) {

            ArrayList<Node> singlePath = allPaths.get(i);


            for (Node n: singlePath) {
                pathStr += n.getName() + "[" + n.getTimeSlice() + "], ";

            }

            pathStr += "\n";
        }

        return pathStr;
    }


    /*
        Prints the entire DBN as a string

        @return String graph in string format.
     */
    @Override
    public String toString() {

        String information = "";

        for (int timeSlice = 0; timeSlice < GraphArr.size(); timeSlice ++) {
            information = information + "Time slice : " + timeSlice + "\n";

            // Iterate through each hashmap per time slice
            for (Map.Entry<String, Node> entry: GraphArr.get(timeSlice).entrySet()) {
                information = information + "Node: " + entry.getKey() + " \n";

                // Iterate through each child of current node
                Node node = entry.getValue();
                ArrayList<Node> children = node.getChildren();
                for (int childrenInd = 0; childrenInd < children.size(); childrenInd ++) {
                    information = information + "Child[" +childrenInd + "], name: " + children.get(childrenInd).getName() + "\n";
                }
            }
        }

        return information;
    }


}
