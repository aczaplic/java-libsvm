package classification;

import java.util.ArrayList;

public class DatasetResult extends ArrayList<InstanceResult> {

    public Object[] getLabels() {
        Object[] labels = new Object[this.size()];
        for (int i=0; i<this.size(); i++)
            labels[i] = this.get(i).getLabel();
        return labels;
    }

    public double[] getValues() {
        double[] values = new double[this.size()];
        for (int i=0; i<this.size(); i++)
            values[i] = this.get(i).getValue();
        return values;
    }

    public double[][] getProbability() {
        int numClasses = this.get(0).getProbability().length;
        double[][] probability = new double[numClasses][this.size()];
        double[] inst;
        for (int i=0; i<this.size(); i++) {
            inst = this.get(i).getProbability();
            for (int n=0; n<numClasses; n++) {
                probability[n][i] = inst[n];
            }
        }
        return probability;
    }

    public double[] getScores(int classIndex) {
        double[] scores = new double[this.size()];
        for (int i=0; i<this.size(); i++)
            scores[i] = this.get(i).getScore(classIndex);
        return scores;
    }
}
