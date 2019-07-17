import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Object stores the prior probability values used in the CPT
 */
public class CPV {

    /*
    Matrix structure - nested Hashmap
    K: | Prior Condition: Hashmap with keys=node,value=data value | V: | ArrayList with all conditional probability values |
    K: {K:x V:1, K:y V:2, K:z V:3} V: [0.1, 0.2, 0.3]
    K: {K:x V:2, K:y V:3, K:z V:4} V: [0.2, 0.3, 0.4]
     */
    Map<HashMap<String, Double>, ArrayList<Double>> PriorProbMatrix = new  HashMap<HashMap<String, Double>, ArrayList<Double>>();


    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///                                        Set functions                                             ///
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    public void AddPriorProbEntry(HashMap<String, Double> PriorParentVals, ArrayList<Double> PriorProbValues) {
        this.PriorProbMatrix.put(PriorParentVals, PriorProbValues);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///                                        Get functions                                             ///
    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    public Map<HashMap<String, Double>, ArrayList<Double>> GetPriorProbMatrix() {
        return this.PriorProbMatrix;
    }

    public ArrayList<Double>  GetCPEntry(HashMap<String,Double> parentVals) {
        ArrayList<Double> CPEntry = PriorProbMatrix.get(parentVals);
        return CPEntry;
    }


    /*
    Get a single conditional probability value of a node. I.e P(valIndex | existingHMap)

    @param valIndex int Index of the node's actual value.
    @param existingHMap HashMap<String, Double> is a hashmap containing the prior values of the nodes parents. This is unique and acts as a key.
    @return Double conditional probability val.
     */
    public Double GetProbVal(HashMap<String, Double> existingHMap, int valIndex, Double currentValue) {

        // Get probability value array list by the appropriate key in the hashmap.
//        System.out.println("Existing Hmap: ");
//
//        for (Map.Entry<String, Double > entry : existingHMap.entrySet()) {
//            System.out.println(entry.getKey().toString() + ":" + entry.getKey() +", val= " + currentValue );
//        }

        ArrayList<Double> valueList = PriorProbMatrix.get(existingHMap);
        Double probVal = valueList.get(valIndex);

        return probVal;
    }


    public String cptToString() {
        String output = "";

        // Go through each entry
        for (Map.Entry<HashMap<String, Double>, ArrayList<Double>> entry : PriorProbMatrix.entrySet()) {
            output = output + entry.getKey() + " = " + entry.getValue() + "\n";
        }

        return output;


    }
}



