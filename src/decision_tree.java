import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.max;

// DO! Conversion from linear to categorical, and/or SKIPPING them
public class decision_tree {
    static int min_size = 75;

    public static void main(String[] args) {
        String train_input = "train.csv";
        String test_input = "test.csv";
        int max_depth = 5;
        String train_out = "train_out.csv";
        String test_out = "test_out.csv";
        String metrics_out = "metrics_out.csv";
        String print_out = "print_out.csv";

        String[] headers;


        int numHeaders = 0;
        ArrayList<String[]> train_input_array = new ArrayList<String[]>();
        try (BufferedReader bReader = new BufferedReader(new FileReader(train_input))) {
            String line;
            while ((line = bReader.readLine()) != null) {
                String[] values = line.split(",");
                train_input_array.add(values);
                numHeaders = values.length;

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        ArrayList<String[]> test_input_array = new ArrayList<String[]>();
        try (BufferedReader bReader = new BufferedReader(new FileReader(test_input))) {
            String line;
            while ((line = bReader.readLine()) != null) {
                String[] values = line.split(",");
                test_input_array.add(values);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (max_depth > numHeaders - 1) {
            max_depth = numHeaders - 1;
        }
        if (max_depth < 0) {
            max_depth = 0;
        }
        headers = new String[numHeaders];

        for (int i = 0; i < train_input_array.get(0).length; i++) {
            headers[i] = train_input_array.get(0)[i];
        }
        String[][] train_input_columns = new String[train_input_array.get(0).length][train_input_array.size() - 1];
        for (int i = 1; i < train_input_array.size(); i++) {
            for (int j = 0; j < train_input_array.get(0).length; j++) {
                train_input_columns[j][i - 1] = train_input_array.get(i)[j];
            }

        }
        String[][] test_input_columns = new String[test_input_array.get(0).length][test_input_array.size() - 1];
        for (int i = 1; i < test_input_array.size(); i++) {
            for (int j = 0; j < test_input_array.get(0).length; j++) {
                test_input_columns[j][i - 1] = test_input_array.get(i)[j];
            }

        }

        Node testingnode = new Node();
        testingnode.nodeDataColumns = train_input_columns;
        testingnode = splitNode(testingnode, max_depth, headers);
        String treeOutput = printTree(testingnode, 0);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(print_out))) {
            writer.write(treeOutput);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // DO! Predictions! (and Error)
        double[] train_predictions = new double[train_input_columns[0].length];
        for (int i = 0; i < train_predictions.length; i++) {
            String[] thing = train_input_array.get(i + 1);
            train_predictions[i] = getNodeResult(thing, testingnode);
        }
        String train_prediction_s = "";
        for (int i = 0; i < train_predictions.length; i++) {
            train_prediction_s = train_prediction_s + train_predictions[i] + "\n";
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(train_out))) {
            writer.write(train_prediction_s);
        } catch (IOException e) {
            e.printStackTrace();
        }
////
        double[] test_predictions = new double[test_input_columns[0].length];
        for (int i = 0; i < test_predictions.length; i++) {
            String[] thing = test_input_array.get(i + 1);
            test_predictions[i] = getNodeResult(thing, testingnode);
        }
        String test_prediction_s = "";
        for (int i = 0; i < test_predictions.length; i++) {
            test_prediction_s = test_prediction_s + test_predictions[i] + "\n";
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(test_out))) {
            writer.write(test_prediction_s);
        } catch (IOException e) {
            e.printStackTrace();
        }

        double finalErrorResult = getFinalErrorResult(train_predictions, train_input_columns[train_input_columns.length - 1]);
        double perCapitaError = Math.pow(finalErrorResult/train_input_columns[0].length,.5);
        String errorRatio = "error(train): " + finalErrorResult +"  That means error per house is: " + perCapitaError;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(metrics_out))) {
            writer.write(errorRatio);
        } catch (IOException e) {
            e.printStackTrace();
        }

//        System.out.println(testingnode.childNodes.length);

    }

    public static double getFinalErrorResult(double[] prediction, String[] truth) {
        //RESIDUALS/SSE DO!
        double sum = 0;
        for (int i = 0; i < prediction.length; i++) {
            //System.out.println(truth[i]+":"+i);
            double doubleTruth = Double.parseDouble(truth[i]);
            sum += Math.pow((prediction[i] - doubleTruth), 2);
        }
        return sum;
    }

    // DO! iterate over array for div, using categorizationFromLastNode to help figure out what and div to figure out where to go.
    public static double getNodeResult(String[] arr, Node node) {
        ////return average/mean instead here
        if (node.childNodes == null) {
            return node.mean;
        } else {
            int columnIndex = node.childNodes[0].indexFromLastNode;
            for(int i=0; i<node.childNodes.length; i++){
                if(arr[columnIndex].equals(node.childNodes[i].categorizationFromLastNode)){
                    return getNodeResult(arr, node.childNodes[i]);
                }
            }
        }
        return -99999;
    }

    // DO! printing, loop through array of child nodes and recurse onto them.
    public static String printTree(Node node, int indents) {
        String str = "";
        for (int i = 0; i < indents; i++) {
            str = str + "| ";
        }
        if (indents > 0) {
            str = str + node.splitHeader + " = " + node.splitNum + ": ";
        }
        str = str + "[" + node.nodeDataColumns[0].length + "]";

        if (node.childNodes != null) {
            for (int j = 0; j < node.childNodes.length; j++) {
                if (node.childNodes[j] != null) {
                    str = str + "\n" + printTree(node.childNodes[j], indents + 1);
                }
            }
        }else{
            str = str + "!!!!!";
        }
        return str;
    }


    //make 2 maps. one is label x name:arraylist of prices (multiple times to test each column) and label x name: arraylist of indexes of row (where the whole row is) (also only do this once at the end once we find optimal
    //part one: finding where to split
    //part 2: splitting it
    //key is categorization, column index (for separation, similar to header) and personal children array index are different
    public static Node splitNode(Node n, int depthLeft, String[] headers) {
        if ((n.nodeDataColumns[0].length < min_size) || (depthLeft == 0)) {
            //System.out.println("hey");
            n.childNodes = null;
            n.mean = getMean(n.nodeDataColumns[n.nodeDataColumns.length - 1]);
            return n;
        }

        int minSSEindex = -1;
        double minSSE = -1;
        for (int columnIndexAttempt = 1; columnIndexAttempt < n.nodeDataColumns.length - 1; columnIndexAttempt++) {
            HashMap<String, ArrayList<Double>> currentColumnMap = new HashMap<>();
            for (int row = 0; row < n.nodeDataColumns[columnIndexAttempt].length; row++) {
                if (!currentColumnMap.containsKey(n.nodeDataColumns[columnIndexAttempt][row])) {
                    currentColumnMap.put(n.nodeDataColumns[columnIndexAttempt][row], new ArrayList<Double>());
                }
                ArrayList<Double> tempArrList = currentColumnMap.get(n.nodeDataColumns[columnIndexAttempt][row]);
                tempArrList.add(Double.valueOf(n.nodeDataColumns[n.nodeDataColumns.length - 1][row]));
            }
            double tempSSE;
            tempSSE = getSSE(currentColumnMap);

            if ((minSSE == -1) || (tempSSE < minSSE)) {
                if(currentColumnMap.size()<7){
                    minSSE = tempSSE;
                    minSSEindex = columnIndexAttempt;
                }
            }

        }
        HashMap<String, ArrayList<Integer>> indexMap = new HashMap<>();
        for (int row = 0; row < n.nodeDataColumns[minSSEindex].length; row++) {
            if (!indexMap.containsKey(n.nodeDataColumns[minSSEindex][row])) {
                indexMap.put(n.nodeDataColumns[minSSEindex][row], new ArrayList<Integer>());
            }
            ArrayList<Integer> tempArrList = indexMap.get(n.nodeDataColumns[minSSEindex][row]);
            tempArrList.add(row);
        }

        Node[] childrenNodes = new Node[indexMap.size()];

        int childNodeIndex = 0;
        for (Map.Entry<String, ArrayList<Integer>> entry : indexMap.entrySet()) {
            childrenNodes[childNodeIndex] = new Node();
            childrenNodes[childNodeIndex].splitNum = childNodeIndex;
            childrenNodes[childNodeIndex].splitHeader = headers[minSSEindex];
            childrenNodes[childNodeIndex].indexFromLastNode = minSSEindex;
            childrenNodes[childNodeIndex].categorizationFromLastNode = entry.getKey();

            ArrayList<Integer> indexArr = entry.getValue();
            childrenNodes[childNodeIndex].nodeDataColumns = new String[n.nodeDataColumns.length][indexArr.size()];
            //row index means index of the row, so 3 is 4th row from the top
            //same with column
            for (int childRowIndex = 0; childRowIndex < indexArr.size(); childRowIndex++) {
                int parentRowIndex = indexArr.get(childRowIndex);
                for (int columnIndex = 0; columnIndex < n.nodeDataColumns.length; columnIndex++) {
                    childrenNodes[childNodeIndex].nodeDataColumns[columnIndex][childRowIndex] = n.nodeDataColumns[columnIndex][parentRowIndex];
                }

            }
            childNodeIndex++;
        }

        n.childNodes = childrenNodes;

        for (int i = 0; i < n.childNodes.length; i++) {
            n.childNodes[i] = splitNode(n.childNodes[i], depthLeft - 1, headers);
            //splitNode(n.childNodes[i], depthLeft-1, headers);
            //maybe first part isn't necessary? is return value used or necessary? THINK!
        }
        return n;
    }

    // DO! SSE needed instead
    public static double getSSE(HashMap<String, ArrayList<Double>> map) {
        double totalSSE = 0;
        for (ArrayList<Double> value : map.values()) {
            double tempMean = getMean(value);
            double tempSSE = getErrorSSE(value, tempMean);
            totalSSE += tempSSE;
        }
        return totalSSE;
    }

    public static double getMean(String[] stringArr) {
        double sum = 0;
        for (int i = 0; i < stringArr.length; i++) {
            sum += Double.parseDouble(stringArr[i]);
        }
        double average = sum / stringArr.length;
        return average;
    }

    public static double getMean(ArrayList<Double> doubleArr) {
        double sum = 0;
        for (int i = 0; i < doubleArr.size(); i++) {
            sum += doubleArr.get(i);
        }
        double average = sum / doubleArr.size();
        return average;
    }

    public static double getErrorSSE(ArrayList<Double> doubleArr, double average) {
        double sum = 0;
        for (int i = 0; i < doubleArr.size(); i++) {
            sum += Math.pow((doubleArr.get(i) - average), 2);
        }
        return sum;
    }


}

class Node {
    String categorizationFromLastNode;//always done
    int indexFromLastNode;//always done
    String[][] nodeDataColumns;//always done
    Node[] childNodes;//null if no more
    double mean;//always done, or only for leaf nodes
    String splitHeader;
    int splitNum;
//    int div;

    public Node() {
        //any of this necessary or can Node() constructor be empty? THINK!
        categorizationFromLastNode = null;
        indexFromLastNode = -1;
        nodeDataColumns = null;
        childNodes = null;
        mean = -1;
        splitHeader = null;
        splitNum = -1;
    }
}