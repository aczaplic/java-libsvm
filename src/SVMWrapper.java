import libsvm.*;

import java.util.Arrays;

public class SVMWrapper {
    private svm_problem prob;
    private svm_parameter param;
    private svm_model model;

    public void defineSVMProblem(double[][] train)
    {
        prob = new svm_problem();
        int recordCount = train.length;
        int featureCount = train[0].length-1;
        prob.y = new double[recordCount];
        prob.l = recordCount;
        prob.x = new svm_node[recordCount][featureCount];

        for (int i = 0; i < recordCount; i++){
            double[] features = train[i];
            prob.x[i] = new svm_node[featureCount];
            for (int j = 1; j < features.length; j++){
                svm_node node = new svm_node();
                node.index = j-1;
                node.value = features[j];
                prob.x[i][j-1] = node;
            }
            prob.y[i] = train[i][0];
        }
    }

    public svm_model svmTrain()
    {
        param = new svm_parameter();
        setSVMParameters(svm_parameter.C_SVC, svm_parameter.RBF, 100, 10);

        model = svm.svm_train(prob, param);
        return model;
    }

    private void setSVMParameters(int type, int kernel, double gamma, double cost)
    {
        //param.probability = 1;
        param.svm_type = type;
        param.kernel_type = kernel;
        param.gamma = gamma;
        param.C = cost;
    }

    private void setSVMParameters(int type, int kernel, double gamma, double cost, double eps, double cache_size)
    {
        //param.probability = 1;
        param.svm_type = type;
        param.kernel_type = kernel;
        param.gamma = gamma;
        //param.nu = 0.5;
        param.C = cost;
        param.cache_size = cache_size; //default 100MB
        param.eps = eps; // default 0.001
    }

    public double evaluatePred(double[] features, svm_model model)
    {
        svm_node[] nodes = new svm_node[features.length-1];
        for (int i = 1; i < features.length; i++)
        {
            svm_node node = new svm_node();
            node.index = i;
            node.value = features[i];

            nodes[i-1] = node;
        }

        int totalClasses = 2;
        int[] labels = new int[totalClasses];
        svm.svm_get_labels(model,labels);

        double[] prob_estimates = new double[totalClasses];
        double v = svm.svm_predict_probability(model, nodes, prob_estimates);

        //for (int i = 0; i < totalClasses; i++)
        //    System.out.print("(" + labels[i] + ":" + prob_estimates[i] + ")");
        //System.out.println("(Actual:" + features[0] + " Prediction:" + v + ")");

        return v;
    }

    public double[] svmPredict(double[][] test, svm_model model)
    {

        double[] yPred = new double[test.length];

        for(int k = 0; k < test.length; k++){
            double[] record = test[k];
            yPred[k] = evaluatePred(record, model);
        }

        return yPred;
    }

    public double evaluateMSE(double[][] data, double[] yPred)
    {
        double err = 0.0;
        for(int k = 0; k < data.length; k++)
            err += (data[k][0] - yPred[k])*(data[k][0] - yPred[k]);
        return err/data.length;
    }

    public double evaluateError(double[][] data, double[] yPred)
    {
        double err = 0;
        for(int k = 0; k < data.length; k++)
            if(data[k][0] != yPred[k]) err++;
        return err/data.length;
    }
}
