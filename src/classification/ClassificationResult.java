package classification;

public interface ClassificationResult {

    /**
     * Returns predicted label of instance.
     *
     * @return predicted label
     */
    public Object getLabel();

    /**
     * Returns evaluated score for one instance
     * using created classifier.
     *
     * @return predicted score
     */
    public double getValue();

    /**
     * Returns evaluated probability of classes for
     * instance using created classifier.
     *
     * @return array of probability of classes
     */
    public double[] getProbability();
}
