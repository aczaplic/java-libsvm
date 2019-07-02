package classification;

public class InstanceResult implements ClassificationResult {

    private Object label;
    private double value;
    private double[] probability;

    public InstanceResult(Object label, double[] value, double[] probability) {
        this.label = label;
        this.value = value[0];
        this.probability = probability;
    }

    /**
     * Creates new instance of result class for one instance
     * from data sets, saving results of classification.
     *
     * @param label
     *            value of class label for instance
     * @param val_prob
     *            array of value or probability for each class
     *            according to probability
     * @param probability
     *            flag of probability parameter in SVM model
     *
     * @return new instance of Instanceresult class
     */
    public static InstanceResult createInstanceResult(Object label, double[] val_prob, int probability) {
        if (probability == 0)
            return new InstanceResult(label, val_prob, null);
        else
            return new InstanceResult(label, new double[]{Double.NaN}, val_prob);
    }

    @Override
    public Object getLabel() {
        return label;
    }

    @Override
    public double getValue() {
        return value;
    }

    @Override
    public double[] getProbability() {
        return probability;
    }

    public double getScore(int classIndex) {
        if (probability == null)
            return value;
        else
            return probability[classIndex];
    }
}
