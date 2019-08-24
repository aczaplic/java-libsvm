package classification;

public class InstanceResult implements ClassificationResult {

    private Object label;
    private double value;
    private double[] probability;

    /**
     * Constructor of InstanceResult with provided values of class attributes.
     *
     * @param label
     *            value of class label for instance
     * @param value
     *            output value from machine learing model
     * @param probability
     *            probability of each class calculated by machine learning model
     */
    public InstanceResult(Object label, double[] value, double[] probability) {
        this.label = label;
        this.value = value[0];
        this.probability = probability;
    }

    /**
     * Creates new instance of result class for one instance in dataset, saving results of classification.
     *
     * @param label
     *            value of class label for instance
     * @param val_prob
     *            array of value or probability for each class
     *            according to probability
     * @param probability
     *            flag of probability parameter in SVM model
     *
     * @return new instance of InstanceResult class
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

    /**
     * Returns output value or probability of provided class for one instance calculated by some classifier
     * depending on configuration of model during building (which score have been calculated).
     *
     * @param classIndex
     *          index of class
     *
     * @return value or probability for one class
     */
    public double getScore(int classIndex) {
        if (probability == null)
            return value;
        else
            return probability[classIndex];
    }
}
