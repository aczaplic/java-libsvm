package dataset;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;

public class DatasetTools {

    /* data transformations */
    public static final int NORM_STD = 0;
    public static final int NORM_MIN_MAX = 1;

    public static final String[] transformTable = new String[]{"NORM_STD", "NORM_MIN_MAX"};

    /**
     * Loads data from file line after line (separated by tabulator) to Dataset
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
     * Loads data from file line after line (separated by tabulator) to Dataset without defined class labels.
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
     * Loads data from file line after line (separated by tabulator) to Dataset. All columns are used and saved as
     * features or class label.
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
     * Saves data to file. One line is for one instance in data set. If features names have been defined in Dataset
     * class object than the first line is for this names.
     *
     * @param filename
     *              path to output file
     * @param dataset
     *              data set to be saved
     */
    public static void saveData(String filename, BasicDataset dataset) throws FileNotFoundException {
        PrintStream consoleStream = System.out;
        PrintStream fileStream = new PrintStream(new File(filename));
        System.setOut(fileStream);
        if (dataset.getFeaturesNames().size() > 0)
        {
            StringBuilder str = new StringBuilder("sequence\tpos_neg");
            for (String feature: dataset.getFeaturesNames()) str.append("\t").append(feature);
            System.out.println(str);
        }
        for (int ind = 0; ind < dataset.size(); ind++)
        {
            BasicInstance instance = (BasicInstance) dataset.getInstance(ind);
            StringBuilder str = new StringBuilder();
            str.append(instance.getUserData());
            str.append("\t");
            str.append(instance.getClassValue());
            for (double value : instance.allFeaturesValues())
            {
                str.append("\t");
                str.append(value);
            }
            System.out.println(str);
        }
        System.setOut(consoleStream);
    }

    /**
     * Transforms data set to interval [0,1] for every feature.
     *
     * @param data
     *              data set to be transformed
     *
     * @return minimum and maximum values for all features
     *
     */
    public static double[][] normalizeMinMax(Dataset data) {
        int recordCount = data.size();
        int featureCount = data.numFeatures();
        double[] feature;
        double[][] min_max = new double[featureCount][2];

        for (int i = 0; i < featureCount; i++)
        {
            double min=Double.POSITIVE_INFINITY;
            double max=Double.NEGATIVE_INFINITY;

            feature = data.getFeature(i);

            for (int j = 0; j < recordCount; j++)
            {
                if (feature[j] > max)
                    max = feature[j];
                if (feature[j] < min)
                    min = feature[j];
            }
            min_max[i][0] = min;
            min_max[i][1] = max;

            if (min == max)
            {
                for (int j = 0; j <recordCount; j++)
                    data.getInstance(j).setFeatureValue(i, 0.0);
            }
            else
                {
                for (int j = 0; j <recordCount; j++)
                    data.getInstance(j).setFeatureValue(i, (feature[j]-min)/(max-min));
                }
        }
        return min_max;
    }

    /**
     * Transforms values of instance features to interval [0,1] using provided values of transformation.
     *
     * @param instance
     *              new instance to be transformed
     * @param min_max
     *              table with values (minimum and maximum of every feature) needed to do the transformation on
     *              the new instance of data
     *
     */
    public static void normalizeMinMax(Instance instance, double[][] min_max) {
        for (int i = 0; i < instance.numFeatures(); i++) {
            double newFeature = 0;
            if ((min_max[i][0]) != (min_max[i][1]))
                newFeature = (instance.getFeatureValue(i)-min_max[i][0])/(min_max[i][1]-min_max[i][0]);
            instance.setFeatureValue(i, newFeature);
        }
    }

    /**
     * Transforms values of instance features to using proper method and provided values of transformation.
     *
     * @param instance
     *              new instance to be transformed
     * @param transformation
     *              number defining the methid of data transformation
     * @param values
     *              table with values needed to do the transformation on the new instance of data
     *
     */
    public static void transformData(Instance instance, Integer transformation, double[][] values) {
        switch (transformation){
            case NORM_STD:
                //normalizeStd(instance, values); //TODO
            case NORM_MIN_MAX:
                normalizeMinMax(instance, values);
        }
    }
}
