package dataset;

import java.util.List;
import java.util.Random;
import java.util.SortedSet;

public interface Dataset extends List<Instance> {
    /**
     * Returns a set containing all different getClasses in this data set. If no getClasses are available, this will return
     * the empty set.
     *
     * @return sorted set of getClasses
     */
    public SortedSet<Object> getClasses();

    /**
     * Add an instance to this data set. The compatibility of the new item with the items in the data set should be
     * checked by the implementation. Incompatible items should not be added to the data set.
     *
     * @param i
     *            the instance to be added
     *
     * @return true if the instance was added, otherwise false
     *
     */
    public boolean add(Instance i);

    /**
     * Get the instance at the provided index.
     *
     * @param index
     *            the index of the instance you want to retrieve.
     * @return instance
     */
    public Instance getInstance(int index);

    /**
     * Get the array of values of the feature at the provided position.
     *
     * @param pos
     *            the position of the feature you want to retrieve
     *
     * @return an array of the features values
     *
     */
    public double[] getFeature(int pos);

    /**
     * The number of features in all instances in data set. When the data set contains no instances, this method should
     * return 0.
     *
     * @return number of features in all instances in data set
     *
     */
    public int numFeatures();

    /**
     * Returns the index of the class value of the provided instance. This method will return -1 if the class value
     * of this instance is not set.
     *
     * @param instance
     *            the data set to give the index for
     *
     * @return the index of the class value of the supplied instance
     *
     */
    public int getClassIndex(Instance instance);

    /**
     * Returns the class value of the supplied class index.
     *
     * @param index
     *            the index to give the class value for
     *
     * @return the class value of the instance with a certain index
     *
     */
    public Object getClassValue(int index);

    /**
     * Create a number of folds from the data set and return them. The supplied random generator is used to determine
     * which instances are assigned to each of the folds.
     *
     * @param numFolds
     *            the number of folds to create
     * @param rg
     *            the random generator
     * @param stratified
     *            whether to ensure that each class is equally represented
     *            in all the folds
     *
     * @return an array of data sets that contains <code>numFolds</code> data sets.
     *
     */
    public Dataset[] folds(int numFolds, Random rg, boolean stratified);

    /**
     * Create a deep copy of the data set. This method should also create deep copies of the instances in the data set.
     *
     * @return deep copy of this data set.
     *
     */
    public Dataset copy();
}
