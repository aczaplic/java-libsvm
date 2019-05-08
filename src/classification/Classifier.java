package classification;

import dataset.Dataset;
import dataset.Instance;

import java.io.IOException;
import java.io.Serializable;

public interface Classifier extends Serializable {
    /**
     * Create a classifier from the given data set.
     *
     * @param data
     *            the data set to be used to create the classifier
     */
    public void buildClassifier(Dataset data);

    /**
     * Classify the instance according to this classifier.
     *
     * @param instance
     *            the instance to be classified
     * @return the instance of InstanceResult class which contains
     *         class label and other evaluated values.
     */
    public InstanceResult classify(Instance instance);

    /**
     * Loads model of classifier from file.
     *
     * @param filename
     *            the path to file
     * @return object of the created classifier model.
     */
    public Object loadModel(String filename) throws IOException;
}
