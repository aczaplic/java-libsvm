package dataset;

import java.io.FileNotFoundException;
import java.util.ArrayList;

public class DatasetTools {

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
    public static void normalizeMinMax(Dataset data) {
        int recordCount = data.size();
        int featureCount = data.numFeatures();
        double[] feature;

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

            for (int j = 0; j <recordCount; j++)
                data.getInstance(j).setFeatureValue(i, (feature[j]-min)/(max-min));
        }
    }
}
