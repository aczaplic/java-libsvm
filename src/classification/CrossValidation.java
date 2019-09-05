package classification;

import dataset.BasicDataset;
import dataset.Dataset;
import dataset.Instance;

import java.util.*;

public class CrossValidation {

    private List<Classifier> classifiersList;

    /**
     * Constructor of CrossValidation object. Saves the provided list od classifiers which are meant to be fitted and
     * used in cross validation procedure.
     *
     * @param classifiersList
     *            the list of classifiers
     */
    public CrossValidation(List<Classifier> classifiersList) {
        this.classifiersList = classifiersList;
    }

    /**
     * Performs cross validation with the specified parameters.
     *
     * @param data
     *            the data set to use in the cross validation. This data set is being split in the defined
     *            number of folds
     * @param numFolds
     *            the number of folds to create
     * @param rg
     *            random generator to create the folds
     * @param stratified
     *            whether to ensure or not that each class is equally represented in all the folds
     *
     * @return the results of classification using the cross-validation method.
     */
    public DatasetResult crossValidation(Dataset data, int numFolds, Random rg, boolean stratified) {
        Dataset[] folds = data.folds(numFolds, rg, stratified);
        SortedMap<Integer, InstanceResult> results = new TreeMap<Integer, InstanceResult>();
        DatasetResult out = new DatasetResult();

        for (int i = 0; i < numFolds; i++) {
            Dataset validation = folds[i];
            Dataset training = new BasicDataset();
            for (int j = 0; j < numFolds; j++)
                if (j != i)
                    training.addAll(folds[j]);

            Classifier classifier = classifiersList.get(i);
            classifier.buildClassifier(training);

            for (Instance instance: validation) {
                InstanceResult result = classifier.classify(instance);
                results.put(instance.getID(), result);
            }
        }
        for (int i=0; i < data.size(); i++) {
            int ind = data.getInstance(i).getID();
            out.add(results.get(ind));
        }

        return out;

    }

    /**
     * Performs cross validation with the specified parameters without equal division of getClasses.
     *
     * @param data
     *            the data set to use in the cross validation. This data set is being split in the defined
     *            number of folds
     * @param numFolds
     *            the number of folds to create
     * @param rg
     *            random generator to create the folds
     *
     * @return the results of classification using the cross-validation method.
     */
    public DatasetResult crossValidation(Dataset data, int numFolds, Random rg) {
        return crossValidation(data, numFolds, rg, false);
    }

    /**
     * Performs cross validation with the specified parameters without equal division of getClasses and random generator
     * with seed set to current time.
     *
     * @param data
     *            the data set to use in the cross validation. This data set is being split in the defined
     *            number of folds
     * @param numFolds
     *            the number of folds to create
     *
     * @return the results of classification using the cross-validation method.
     */
    public DatasetResult crossValidation(Dataset data, int numFolds) {
        return crossValidation(data, numFolds, new Random(System.currentTimeMillis()));
    }

}
