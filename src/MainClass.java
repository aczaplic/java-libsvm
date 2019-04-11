import libsvm.svm_model;

import java.io.FileNotFoundException;

public class MainClass {

    public static void main(String[] args) {
        double[][] trainData = new double[0][];
        try {
            trainData = DataPrepare.readData("./data/7bialek_0.2_1_score.txt");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        trainData = DataPrepare.normalizeMinMax(trainData, DataPrepare.getMinMax(trainData));

        SVMWrapper wrapper = new SVMWrapper();
        wrapper.defineSVMProblem(trainData);
        svm_model svmModel = wrapper.svmTrain();
        double[] yPred = wrapper.svmPredict(trainData, svmModel);
        double err = wrapper.evaluateError(trainData, yPred);
        System.out.println(err);
    }
}
