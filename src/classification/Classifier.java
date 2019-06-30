package classification;

import dataset.Dataset;
import dataset.Instance;

import java.io.IOException;
import java.io.Serializable;

public interface Classifier extends Serializable {

    interface ClassifierParameters extends Serializable {
    }

    /**
     * Create a classifier from the given data set.
     *
     * @param data
     *            the data set to be used to create the classifier
     */
    void buildClassifier(Dataset data);

    /**
     * Classify the instance according to this classifier.
     *
     * @param instance
     *            the instance to be classified
     * @return the instance of InstanceResult class which contains
     *         class label and other evaluated values.
     */
    InstanceResult classify(Instance instance);

    /**
     * Classifies data set using SVM model and returns
     * the results of classification.
     *
     * @param test
     *          data set to classify
     *
     * @return results of classification of the whole
     *          data set
     *
     */
    DatasetResult classify(Dataset test);

    /**
     * Save model of classifier to file.
     *
     * @param filename
     *            the path to file
     */
    public void saveModel(String filename) throws IOException;

    /**
     * Load model of classifier from file.
     *
     * @param filename
     *            the path to file
     * @return object of the created classifier model.
     */
    Object loadModel(String filename) throws IOException;
}
