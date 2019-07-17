import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;


public class Node{


    ArrayList<Node> parents = new ArrayList<>();
    ArrayList<Node> children = new ArrayList<>();

    ArrayList<Node> faultyParents = new ArrayList<>();
    ArrayList<Node> faultyChildren = new ArrayList<>();

    ArrayList<Double> values = new ArrayList<>();


    CPV ConditionalProbTable = new CPV();

    BayesianNetwork tDBN = new BayesianNetwork();

    int timeSlice;
    int rootCause = 0; // 0 = not root cause. 1 = root cause.
    Double anomalyIndex = 0.000000000000000000000000000000000;
    String name = "";
    Double currentValue = 0.000000000;
    Double CF = 0.000000000000000000000000000000000;


//    @Override
//    public Node clone() {
//        Node newNode = new Node();
//        newNode.parents = (ArrayList<Node>) this.parents.clone();
//        newNode.children = (ArrayList<Node>) this.children.clone();
//        newNode.faultyChildren = (ArrayList<Node>) this.faultyChildren.clone();
//        newNode.faultyParents = (ArrayList<Node>) this.faultyParents.clone();
//        newNode.values = (ArrayList<Double>) this.values.clone();
//
//        // Pointers to unclonable objects.
//        newNode.ConditionalProbTable = this.ConditionalProbTable;
//        newNode.tDBN = this.tDBN;
//        newNode.timeSlice = this.timeSlice;
//        newNode.rootCause = this.rootCause;
//        newNode.anomalyIndex = this.anomalyIndex;
//        newNode.name = this.name;
//
//        return newNode;
//    }



    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///                                        Get functions                                             ///
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    public String getName() {
        return name;
    }

    public ArrayList<Node> getParents() {
        return parents;
    }

    public ArrayList<Node> getFaultyParents() {
        return faultyParents;
    }

    public  ArrayList<Double> getValues() {
        return this.values;
    }


    public ArrayList<Node> getChildren() {
        return this.children;
    }

    public ArrayList<Node> getFaultyChildren() {
        return this.faultyChildren;
    }

    //TODO: this printed kinda weird. Check this lol.
    public CPV getCPT() {
        return this.ConditionalProbTable;
    }

    public int getTimeSlice() {
        return this.timeSlice;
    }

    public int isRootCause() {
        return this.rootCause;
    }

    public Double getAnomalyIndex() {
        return this.anomalyIndex;
    }

    public Double getCurrentValue() {
        return this.currentValue;
    }

    public Double getCF() {
        return this.CF;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///                                        Set functions                                             ///
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    public void setTDBN(BayesianNetwork tDBN) {this.tDBN = tDBN; }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(Double val) {
        this.currentValue = val;
    }

    public void setCF(Double val) {
        this.CF = val;
    }

    public void addParent(Node parent) {
        this.parents.add(parent);
    }

    public void addChild(Node child){
        (this.children).add(child);
    }

    public void addFaultyParent(Node parent) {
        this.faultyParents.add(parent);
    }

    public void addFaultyChild(Node child){
        (this.faultyChildren).add(child);
    }


    public void setAnomalyIndex(Double index) {
        this.anomalyIndex = index;
    }


    public void addValues(String[] valuesList) {

        for (String val:valuesList) {
            this.values.add(Double.valueOf(val));
        }
    }

    public void setTimeSlice(int timeSlice) {
        this.timeSlice = timeSlice;
    }

    public void populateCPT(ArrayList<String> graphStructureString) {

        // Parse each line in CPT
        for (String valueStr: graphStructureString) {

            // Get prior values
            String priorValuesStr = valueStr.split(":")[0];
            priorValuesStr = priorValuesStr.substring(1, priorValuesStr.length()-1);
            String[] priorValuesArr = priorValuesStr.split(":")[0].split(", ");
            HashMap<String, Double> priorEntry = new HashMap<>();

            // Parse inner hashmap wiht prior values
            for (String str: priorValuesArr) {
                String substring = str;



                // Get information about all parents. Eg login_rt[0]=3.40045E7, login_rt[1]=3208000.0
                String[] parentInfoArr = substring.split(", ");
                String parentName = "";
                Double parentVal = 0.0;
                int parentTimeSlice = 0;


                for (String parentInfo: parentInfoArr) {
                    // Get parent name
                    parentName = parentInfo.split("\\[")[0];

                    // Get time slice of parent
                    parentTimeSlice = Integer.valueOf(parentInfo.split("\\[")[1].split("]")[0]);

                    // Get prior value of parent
                    // Handle scientific notation numbers
                    String numStr = parentInfo.split("=")[1];
                    if (numStr.contains("E")){

                        try{
                            Double sf = Double.parseDouble(numStr);
                            NumberFormat nf = new DecimalFormat("#######################################################################.##################################################################");
                            numStr = nf.format(sf);
                        }
                        catch( Exception e) {
                            e.printStackTrace();
                        }

                    }


                    parentVal = Double.valueOf(numStr);

                    String parentStr = parentName + "[" + String.valueOf(this.timeSlice + parentTimeSlice - 1) +"]";
                    // Add parent name & vals to priorEntry dictionary.
                    priorEntry.put(parentStr, parentVal);
                }


            }

            // Parse probability values
            String[] probValuesList = valueStr.split(": ")[1].split(" ");

            ArrayList<Double> probValuesArr = new ArrayList<>();
            for (String entry: probValuesList) {
                probValuesArr.add(Double.valueOf(entry));

            }

            // Add entry (key: inner hashmap and val: arraylist) to ConditionalProbTable matrix.
            ConditionalProbTable.AddPriorProbEntry(priorEntry, probValuesArr);

        }

    }

    public String nodeToString() {
        return this.getName() + "[" + this.getTimeSlice() + "]";
    }
}