package dataset;

import java.io.Serializable;

public interface Instance extends Serializable {
    /**
     * Returns the class value for this instance.
     *
     * @return class value of this instance, or null if the class is not set
     */
    public Object getClassValue();

    /**
     * Sets the class value for this instance.
     *
     * @param value
     *           class value of this instance
     *
     */
    public void setClassValue(Object value);

    /**
     * Returns the number of features this instance has.
     *
     * @return number of features
     */
    public int numFeatures();

    /**
     * Returns the feature value at the position pos for this instance.
     *
     * @return feature value at the position pos for this instance
     */
    public double getFeatureValue(int pos);

    /**
     * Returns values of all features for this instance.
     *
     * @return array of values of all features for this instance
     */
    public double[] allFeaturesValues();

    /**
     * Sets the value of feature in position pos for this instance.
     *
     * @param pos
     *          position of feature which is changed
     * @param value
     *          new value of the changed feature
     *
     */
    public void setFeatureValue(int pos, Double value);

    /**
     * Returns a unique identifier for this instance.
     *
     * @return unique identifier
     */
    public int getID();

    /**
     * Creates a deep copy of this instance
     *
     * @return a deep copy of this instance
     */
    public Instance copy();
}
