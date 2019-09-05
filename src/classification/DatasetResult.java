package classification;

import java.util.ArrayList;

public class DatasetResult extends ArrayList<InstanceResult> {

    /**
     * Returns predicted labels for all instances in test set.
     *
     * @return array of predicted labels
     */
    public Object[] getLabels() {
        Object[] labels = new Object[this.size()];
        for (int i=0; i<this.size(); i++)
            labels[i] = this.get(i).getLabel();
        return labels;
    }

    /**
     * Returns output values from model for all instances in test set.
     *
     * @return array of values
     */
    public double[] getValues() {
        double[] values = new double[this.size()];
        for (int i=0; i<this.size(); i++)
            values[i] = this.get(i).getValue();
        return values;
    }

    /**
     * Returns evaluated probability of all getClasses for all instances in test set.
     *
     * @return table of probability values (row - class, column - instance)
     */
    public double[][] getProbabilities() {
        int numClasses = this.get(0).getProbability().length;
        double[][] probabilities = new double[numClasses][this.size()];
        double[] inst;
        for (int i=0; i<this.size(); i++) {
            inst = this.get(i).getProbability();
            for (int n=0; n<numClasses; n++) {
                probabilities[n][i] = inst[n];
            }
        }
        return probabilities;
    }

    /**
     * Returns output values or probability of provided class for all instances in test set calculated by some
     * classifier depending on configuration of model during building (which score have been calculated).
     *
     * @param classIndex
     *          index of class
     *
     * @return array of values or probabilities for one class
     */
    public double[] getScores(int classIndex) {
        double[] scores = new double[this.size()];
        for (int i=0; i<this.size(); i++)
            scores[i] = this.get(i).getScore(classIndex);
        return scores;
    }
}
