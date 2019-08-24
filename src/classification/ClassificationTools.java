package classification;

import dataset.Dataset;

public class ClassificationTools {

    /**
     * Evaluates mean square error of classifying data set by some machine learning model.
     *
     * @param data
     *            classified data set
     * @param yPred
     *            predicted values of class labels
     *
     */
    public static double evaluateMSE(Dataset data, double[] yPred)
    {
        double err = 0.0;
        double y;
        for(int k = 0; k < data.size(); k++) {
            y = Double.parseDouble(data.getInstance(k).getClassValue().toString());
            err += (y - yPred[k]) * (y - yPred[k]);
        }
        return err/data.size();
    }

    /**
     * Evaluates classification error of data set.
     *
     * @param data
     *            classified data set
     * @param yPred
     *            predicted values of class labels
     *
     */
    public static double evaluateError(Dataset data, Object[] yPred)
    {
        double err = 0;
        Object y;
        for(int k = 0; k < data.size(); k++) {
            y = Double.parseDouble(data.getInstance(k).getClassValue().toString());
            if (!yPred[k].equals(y)) err++;
        }
        return err/data.size();
    }
}
