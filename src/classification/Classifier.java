package classification;

import dataset.Dataset;
import dataset.Instance;

import java.io.IOException;
import java.io.Serializable;

public interface Classifier extends Serializable {

    /**
     * Creates (builds structure and fits) a classifier for the given data set.
     *
     * @param data
     *            the dataset for which to create the classifier (train set)
     */
    void buildClassifier(Dataset data);

    /**
     * Classifies the instance according to this classifier and returns the results of classification.
     *
     * @param instance
     *            the instance to be classified
     *
     * @return the instance of InstanceResult class which contains provided class label and other evaluated values.
     */
    InstanceResult classify(Instance instance);

    /**
     * Classifies data set according to this classifier and returns the results of classification.
     *
     * @param test
     *          data set to classify
     *
     * @return the instance of DatasetResult class which contains results of classification for the whole data set
     *
     */
    DatasetResult classify(Dataset test);

    /**
     * Save model of this classifier to file.
     *
     * @param filename
     *            the path to file
     */
    void saveModel(String filename) throws IOException;

    /**
     * Load model of this classifier from file.
     *
     * @param filename
     *            the path to file
     *
     * @return object of the imported classifier model.
     */
    Object loadModel(String filename) throws IOException;
}
