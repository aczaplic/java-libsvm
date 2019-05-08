package classification;

public interface ClassificationResult {

    /**
     * Returns predicted label of instance.
     *
     * @return predicted label
     */
    public Object getLabel();

    /**
     * Returns evaluated values for one instance
     * using created classifier.
     *
     * @return array of predicted values
     */
    public double[] getValues();

    /**
     * Returns evaluated probability of classes for
     * instance using created classifier.
     *
     * @return array of probability of classes
     */
    public double[] getProbability();
}
