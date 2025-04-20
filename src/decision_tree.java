import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static java.lang.Math.max;

// DO! Conversion from linear to categorical, and/or SKIPPING them
public class decision_tree {
    //HYPERPARAMETER: HOW MANY ELEMENTS DOES A NODE NEED TO HAVE IN ORDER TO SPLIT
    static int min_size = 25;

    public static void main(String[] args) {
        String train_input = "train_categorized.csv";
        String test_input = "test.csv";
        //HYPERPARAMETER: HOW MANY LAYERS IS THE TREE ALLOWED TO GO
        int max_depth = 10;
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
        //for random forest: bootstrapping, average result of several trees for answer, and in each splitting of a node only a subset of features are considered

        //HYPERPARAMETER: number of trees for random forest
        int numTrees = 50;
        Node[] forestArray = new Node[numTrees];
        double[] forestArrayErrors = new double[numTrees];

        for (int treeIndex = 0; treeIndex<numTrees; treeIndex++) {
            Node testingnode = new Node();
            forestArray[treeIndex] = testingnode;
            String[][] train_input_columns_bootStrapped = new String[train_input_columns.length][train_input_columns[0].length];
            for(int ticRowIndex =0; ticRowIndex< train_input_columns[0].length; ticRowIndex++){
                int otherRowIndex=(int)(Math.random()*train_input_columns[0].length);
                for(int ticColIndex = 0; ticColIndex<train_input_columns.length; ticColIndex++){
                    train_input_columns_bootStrapped[ticColIndex][ticRowIndex] = train_input_columns[ticColIndex][otherRowIndex];
                }
            }


            testingnode.nodeDataColumns = train_input_columns_bootStrapped;
            testingnode = splitNode(testingnode, max_depth, headers);
            String treeOutput = printTree(testingnode, 0);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(print_out+""+treeIndex))) {
                writer.write(treeOutput);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ///////////////////////////////////////////////////////////////////////////////
            double[] train_predictions = new double[train_input_columns_bootStrapped[0].length];
            for (int i = 0; i < train_predictions.length; i++) {
                String[] currentRowData = new String[train_input_columns_bootStrapped.length];
                for (int j = 0; j < train_input_columns_bootStrapped.length; j++) {
                    currentRowData[j] = train_input_columns_bootStrapped[j][i];
                }
                train_predictions[i] = getNodeResult(currentRowData, testingnode);
            }
            ///////////////////////////////////////////////////////////////////////////
            String train_prediction_s = "";
            for (int i = 0; i < train_predictions.length; i++) {
                train_prediction_s = train_prediction_s + train_predictions[i] + "\n";
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(train_out+""+treeIndex))) {
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
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(test_out+""+treeIndex))) {
                writer.write(test_prediction_s);
            } catch (IOException e) {
                e.printStackTrace();
            }

            double finalErrorResult = getFinalErrorResult(train_predictions, train_input_columns_bootStrapped[train_input_columns_bootStrapped.length - 1]);
            double perCapitaError = Math.pow(finalErrorResult / train_input_columns_bootStrapped[0].length, .5);
            String errorRatio = "error(train): " + finalErrorResult + "  That means error per house is: " + perCapitaError;
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(metrics_out+""+treeIndex))) {
                writer.write(errorRatio);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        double[] forest_train_predictions = new double[train_input_columns[0].length];
        for (int i = 0; i < forest_train_predictions.length; i++) {
            String[] thing = train_input_array.get(i + 1);
            forest_train_predictions[i] = getForestResult(thing, forestArray);
        }
        double forestErrorResult = getFinalErrorResult(forest_train_predictions, train_input_columns[train_input_columns.length - 1]);
        double forest_perCapitaError = Math.pow(forestErrorResult / train_input_columns[0].length, .5);
        String forest_errorRatio = "error(train): " + forestErrorResult + "  That means error per house is: " + forest_perCapitaError;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("Forest.csv"))) {
            writer.write(forest_errorRatio);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static double getForestResult(String[] arr, Node[] nodes) {
        double sum = 0;
        for (int i = 0; i < nodes.length; i++) {
            sum += getNodeResult(arr, nodes[i]);
        }
        sum /= nodes.length;
        return sum;
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
            for (int i = 0; i < node.childNodes.length; i++) {
                if (arr[columnIndex].equals(node.childNodes[i].categorizationFromLastNode)) {
                    return getNodeResult(arr, node.childNodes[i]);
                }
            }
        }
        return node.mean;
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
        } else {
            str = str + "!!!!!";
        }
        return str;
    }


    //make 2 maps. one is label x name:arraylist of prices (multiple times to test each column) and label x name: arraylist of indexes of row (where the whole row is) (also only do this once at the end once we find optimal
    //part one: finding where to split
    //part 2: splitting it
    //key is categorization, column index (for separation, similar to header) and personal children array index are different
    public static Node splitNode(Node n, int depthLeft, String[] headers) {
        //HYPERPARAMETER: HOW SMALL CAN A NODE'S SIZE BE UNTIL IT CAN NO LONGER BE TRUSTED. IF THE DATASET IS TOO SMALL, AVERAGE 1/3 MEAN AND 2/3 PARENT MEAN
        if (n.nodeDataColumns[0].length > 5) {
            n.mean = getMean(n.nodeDataColumns[n.nodeDataColumns.length - 1]);
        } else {
            double tempMean = getMean(n.nodeDataColumns[n.nodeDataColumns.length - 1]);
            n.mean = (n.parentNode.mean + n.parentNode.mean + tempMean) / 3;
        }

        if ((n.nodeDataColumns[0].length < min_size) || (depthLeft == 0)) {
            //System.out.println("TESTING STRING");
            n.childNodes = null;
            return n;
        }

        //HYPERPARAMETER: instead of going through all the features each split, only looks at a random subset, size sqrt(total features)
        int[] randomColumnArray = new int[(int) Math.round(Math.pow(n.nodeDataColumns.length - 2, .5))];
        HashSet<Integer> tempRandomColumns = new HashSet<>();
        for (int i = 0; i < randomColumnArray.length; i++) {
            int tempInt = 1 + (int) (Math.random() * (n.nodeDataColumns.length - 2));
            if (tempRandomColumns.contains(tempInt)) {
                i--;
            } else {
                tempRandomColumns.add(tempInt);
                randomColumnArray[i] = tempInt;
            }

        }
        int minSSEindex = -1;
        double minSSE = -1;
        for (int randomColumnIndex = 0; randomColumnIndex < randomColumnArray.length; randomColumnIndex++) {
            int columnIndexAttempt = randomColumnArray[randomColumnIndex];
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
                //POSSIBLE HYPERPARAMETER: ONLY SPLITS IF THERE AREN'T THAT MANY POSSIBLE VALUES FOR A FEATURE, UNUSED FOR NOW
                //if(currentColumnMap.size()<6){
                minSSE = tempSSE;
                minSSEindex = columnIndexAttempt;
                //}
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
            childrenNodes[childNodeIndex].parentNode = n;
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
    Node parentNode;
    String categorizationFromLastNode;//always done
    int indexFromLastNode;//always done
    String[][] nodeDataColumns;//always done. also, n.nodeDataColumns.length is numColumns
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
        parentNode = null;
    }
}