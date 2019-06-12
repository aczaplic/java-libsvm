package dataset;

import java.io.FileNotFoundException;
import java.util.ArrayList;

public class DatasetTools {

    /* data transformations */
    public static final int NormStd = 0;
    public static final int NormMinMax = 1;

    public static final String[] transformTable = new String[]{"NormStd", "NormMinMax"};

    /**
     * Load data from file line after line (separated by tabulator) to Dataset
     *
     * @param filename
     *              path to file
     * @param classIndex
     *              index of column containing class labels
     * @param colToSkip
     *              number of columns to skip
     *
     * @return data set
     */
    public static Dataset loadData(String filename, int classIndex, int colToSkip) throws FileNotFoundException {
        ArrayList<ArrayList<String>> dataList = FileHandler.readFile(filename);
        BasicDataset dataset = new BasicDataset();
        Object classValue = null;
        double[] values;
        int tmpCol;

        for (ArrayList<String> row : dataList) {
            if (classIndex == -1) values = new double[row.size() - colToSkip];
            else values = new double[row.size() - colToSkip - 1];
            tmpCol = colToSkip;

            for (int j = tmpCol; j < row.size(); j++) {
                if (j == classIndex) {
                    if (Double.parseDouble(row.get(j)) <= 0)
                        classValue = -1.0;
                    else
                        classValue = Double.parseDouble(row.get(j));
                    tmpCol++;
                } else values[j - tmpCol] = Double.parseDouble(row.get(j));
            }
            dataset.add(new BasicInstance(values, classValue));
        }
        return dataset;
    }

    /**
     * Load data from file line after line (separated by tabulator)
     * to Dataset without defined class labels.
     *
     * @param filename
     *              path to file
     *
     * @return data set
     */
    public static Dataset loadData(String filename) throws FileNotFoundException {
        return loadData(filename, -1, 0);
    }

    /**
     * Load data from file line after line (separated by tabulator)
     * to Dataset. All columns are used and saved as features or class label.
     *
     * @param filename
     *              path to file
     * @param classIndex
     *              index of column containing class labels
     *
     * @return data set
     */
    public static Dataset loadData(String filename, int classIndex) throws FileNotFoundException {
        return loadData(filename, classIndex, 0);
    }

    /**
     * Transform data set to interval [0,1] for every feature.
     *
     * @param data
     *              data set to be transformed
     *
     */
    public static double[][] normalizeMinMax(Dataset data) {
        int recordCount = data.size();
        int featureCount = data.numFeatures();
        double[] feature;
        double[][] min_max = new double[featureCount][2];

        for (int i = 0; i < featureCount; i++) {
            double min=Double.POSITIVE_INFINITY;
            double max=Double.NEGATIVE_INFINITY;

            feature = data.getFeature(i);

            for (int j = 0; j < recordCount; j++) {
                if (feature[j] > max)
                    max = feature[j];
                if (feature[j] < min)
                    min = feature[j];
            }
            min_max[i][0] = min;
            min_max[i][1] = max;

            for (int j = 0; j <recordCount; j++)
                data.getInstance(j).setFeatureValue(i, (feature[j]-min)/(max-min));
        }
        return min_max;
    }

    public static void normalizeMinMax(Instance instance, double[][] min_max) {
        for (int i = 0; i < instance.numFeatures(); i++) {
            double newFeature = (instance.getFeatureValue(i)-min_max[i][0])/(min_max[i][1]-min_max[i][0]);
            instance.setFeatureValue(i, newFeature);
        }
    }

    public static void dataTransformation(Instance instance, Integer transformation, double[][] values) {
        switch (transformation){
            case NormStd:
                //normalizeStd(instance, values); //TODO
            case NormMinMax:
                normalizeMinMax(instance, values);
        }
    }
}
