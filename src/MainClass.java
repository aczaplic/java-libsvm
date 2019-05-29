import classification.*;
import dataset.Dataset;
import dataset.DatasetTools;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainClass {

    public static void main(String[] args) {
        Dataset trainData;
        int numFolds = 5;
        try {
            trainData = DatasetTools.loadData("./data/7bialek_0.2_1_score.txt", 1, 1);
            DatasetTools.normalizeMinMax(trainData); // TODO transformations on data as part of model

            List<Classifier> models = new ArrayList<>();
            for (int i=0; i<numFolds; i++)
                models.add(new SVMClassifier());

            CrossValidation cv = new CrossValidation(models);
            DatasetResult results = cv.crossValidation(trainData, numFolds, new Random(System.currentTimeMillis()), true);
            double err = ClassificationTools.evaluateError(trainData, results.getLabels());
            System.out.println("Error: " + err);

            for (int i=0; i<numFolds; i++) {
                results = models.get(i).classify(trainData);
                err = ClassificationTools.evaluateError(trainData, results.getLabels());
                System.out.println(i + ") error: " + err);
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
