import classification.DatasetResult;
import classification.SVMClassifier;
import dataset.Dataset;
import dataset.DatasetTools;

import java.io.FileNotFoundException;

public class MainClass {

    public static void main(String[] args) {
        Dataset trainData;
        try {
            trainData = DatasetTools.loadData("./data/7bialek_0.2_1_score.txt", 1, 1);
            DatasetTools.normalizeMinMax(trainData);
            SVMClassifier svm = new SVMClassifier();
            svm.buildClassifier(trainData);
            DatasetResult results = svm.classify(trainData);
            //for(int k = 0; k < trainData.length; k++)
            //    System.out.println("(Actual:" + trainData[k][0] + " Prediction:" + yPred[k] + ")");
            double err = svm.evaluateError(trainData, results.getLabels());
            System.out.println(err);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
