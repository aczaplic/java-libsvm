package classification;

public class InstanceResult implements ClassificationResult {

    private Object label;
    private double[] values;
    private double[] probability;

    private InstanceResult(Object label, double[] values, double[] probability) {
        this.label = label;
        this.values = values;
        this.probability = probability;
    }

    /**
     * Creates new instance of result class for one instance
     * from data sets, saving results of classification.
     *
     * @param label
     *            value of class label for instance
     * @param val_prob
     *            array of values or probability for each class
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
            return new InstanceResult(label, null, val_prob);
    }

    @Override
    public Object getLabel() {
        return label;
    }

    @Override
    public double[] getValues() {
        return values;
    }

    @Override
    public double[] getProbability() {
        return probability;
    }
}
