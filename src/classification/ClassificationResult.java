package classification;

public interface ClassificationResult {

    /**
     * Returns predicted label of instance.
     *
     * @return predicted label
     */
    public Object getLabel();

    /**
     * Returns output value for one instance calculated by some classifier.
     *
     * @return predicted score
     */
    public double getValue();

    /**
     * Returns probability of all getClasses for one instance calculated by some classifier.
     *
     * @return array of probability of all getClasses
     */
    public double[] getProbability();
}
