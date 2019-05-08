package dataset;

public class BasicInstance extends AbstractInstance implements Instance{

    private static final long serialVersionUID = 2747823344714783381L;

    /* Holds values of features */
    private double[] features;

    /**
     * Creates a new instance with the provide values of the features. The
     * class label will be set to null.
     *
     * @param values
     *            the value of the instance
     */
    public BasicInstance(double[] values) {
        this(values, null);
    }

    /**
     * Creates a new instance with the provided values of features and the
     * provided class label.
     *
     * @param values
     *            the attribute values
     * @param classValue
     *            the class label
     */
    public BasicInstance(double[] values, Object classValue) {
        super(classValue);
        this.features = values.clone();
    }

    /* Hide argumentless constructor */
    private BasicInstance() {
    }

    @Override
    public int numFeatures() {
        return features.length;
    }

    @Override
    public double featureValue(int pos) {
        return features[pos];
    }

    @Override
    public double[] allFeaturesValues() {
        return features.clone();
    }

    @Override
    public void setFeatureValue(int pos, Double value) {
        features[pos] = value;
    }

    @Override
    public Instance copy() {
        BasicInstance instClone = new BasicInstance();
        instClone.setClassValue(this.classValue());
        instClone.features = this.features.clone();
        return instClone;
    }
}
