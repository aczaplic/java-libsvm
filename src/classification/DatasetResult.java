package classification;

import java.util.ArrayList;

public class DatasetResult extends ArrayList<InstanceResult> {

    public Object[] getLabels() {
        Object[] labels = new Object[this.size()];
        for (int i=0; i<this.size(); i++)
            labels[i] = this.get(i).getLabel();
        return labels;
    }

    public double[][] getValues() {
        int numClasses = this.get(0).getValues().length;
        double[][] values = new double[numClasses][this.size()];
        double[] inst;
        for (int i=0; i<this.size(); i++) {
            inst = this.get(i).getValues();
            for (int n=0; n<numClasses; n++) {
                values[n][i] = inst[n];
            }
        }
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
}
