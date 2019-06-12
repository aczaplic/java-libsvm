package classification;

import dataset.BasicInstance;
import dataset.Dataset;
import dataset.DatasetTools;
import dataset.Instance;
import libsvm.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static classification.InstanceResult.createInstanceResult;
import static libsvm.svm.svm_load_model;
import static libsvm.svm.svm_save_model;


public class SVMClassifier implements Classifier {

    private static final long serialVersionUID = 7293859346790783381L;

    private svm_problem prob;
    private svm_parameter param;
    private svm_model model;
    private LinkedHashMap<Integer, double[][]> transform;

    /**
     * Creates a new instance of SVMClassifier.
     */
    public SVMClassifier()
    {
        param = new svm_parameter();
        transform = new LinkedHashMap<>();

        param.svm_type = svm_parameter.C_SVC;
        param.kernel_type = svm_parameter.RBF;
        param.degree = 3;
        param.gamma = 0;	// 1/num_features
        param.coef0 = 0;
        param.nu = 0.5;
        param.cache_size = 100;
        param.C = 1;
        param.eps = 1e-3;
        param.p = 0.1;
        param.shrinking = 1;
        param.probability = 0;
        param.nr_weight = 0;
        param.weight_label = null;
        param.weight = null;
    }

    public void addDataTransformation(Integer transformation, double[][] values)
    {
        transform.put(transformation, values);
    }

    @Override
    public void buildClassifier(Dataset data) {
        defineSVMProblem(data);
        setSVMParameters(svm_parameter.C_SVC, svm_parameter.RBF, 10, 10);
        model = svm.svm_train(prob, param);
    }

    /**
     * Transforms dataset into instance of svm_problem class.
     *
     * @param train
     *          dataset used for training model
     *
     */
    public void defineSVMProblem(Dataset train)
    {
        int recordCount = train.size();
        int featureCount = train.numFeatures();

        prob = new svm_problem();
        prob.y = new double[recordCount];
        prob.l = recordCount;
        prob.x = new svm_node[recordCount][featureCount];

        for (int i = 0; i < recordCount; i++)
        {
            double[] features = train.getInstance(i).allFeaturesValues();
            prob.x[i] = new svm_node[featureCount];

            for (int j = 0; j < features.length; j++)
            {
                svm_node node = new svm_node();
                node.index = j;
                node.value = features[j];
                prob.x[i][j] = node;
            }
            prob.y[i] = Double.parseDouble(train.getInstance(i).getClassValue().toString());
        }
    }

    @Override
    public DatasetResult classify(Dataset test)
    {

        DatasetResult results = new DatasetResult();
        Instance instance;

        for(int k = 0; k < test.size(); k++){
            instance = test.getInstance(k);
            results.add(classify(instance));
        }

        return results;
    }

    @Override
    public InstanceResult classify(Instance instance) {
        svm_node[] test_x = new svm_node[instance.numFeatures()];
        for (int i = 0; i < instance.numFeatures(); i++)
        {
            svm_node node = new svm_node();
            node.index = i;
            node.value = instance.getFeatureValue(i);
            test_x[i] = node;
        }

        double label;
        double[] val_prob;
        if (model.param.probability == 1)
        {
            int totalClasses = 2;
            int[] labels = new int[totalClasses];
            svm.svm_get_labels(model, labels);
            val_prob = new double[totalClasses];
            label = svm.svm_predict_probability(model, test_x, val_prob);
            //for (int i = 0; i < totalClasses; i++)
            //    System.out.print("(" + labels[i] + ":" + val_prob[i] + ")");
        } else {
            val_prob = new double[1];
            //double v = svm.svm_predict(model, test_x);
            label = svm.svm_predict_values(model, test_x, val_prob);
            //System.out.print("(value :" + val_prob[0] + ")");
        }

        //if(!instance.getClassValue().equals(label))
        //    System.out.println("ID:" + instance.getID() + " (Actual:" + instance.getClassValue() + " Prediction:" + label + ")");
        return createInstanceResult(label, val_prob, model.param.probability);
    }

    @Override
    public void saveModel(String filename) throws IOException {
        if (!transform.isEmpty()) {
            String transformName = ".//out//" + filename + "_transformation.txt";
            DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(transformName)));
            for (Map.Entry<Integer, double[][]> entry : transform.entrySet()) {
                Integer key = entry.getKey();
                stream.writeBytes("transformation " + DatasetTools.transformTable[key] + "\n");
                double[][] values = entry.getValue();
                for (double[] value : values) {
                    for (double aValue : value) {
                        stream.writeBytes(aValue + "\t");
                    }
                    stream.writeBytes("\n");
                }
                stream.writeBytes("\n\n");
            }
            stream.close();
        }

        String modelName = ".//out//" + filename + "_model.txt";
        svm_save_model(modelName, this.model);
    }

    @Override
    public svm_model loadModel(String filename) throws IOException {
        System.out.println("\nLoading model parameters from file...");
        String modelName = ".//out//" + filename + "_model.txt";
        this.model = svm_load_model(modelName);
        this.param = model.param;

        try {
            System.out.println("Loading information about data transformation from file...\n");
            String transformName = ".//out//" + filename + "_transformation.txt";
            Integer key = -1;
            double[][] values;
            BufferedReader reader  = new BufferedReader(new FileReader(transformName));
            String var3;
            while ((var3 = reader.readLine()).startsWith("transformation")) {
                String[] line = var3.trim().split(" ");
                for(int i = 0; i < DatasetTools.transformTable.length; i++) {
                    if (line[1].equals(DatasetTools.transformTable[i])) {
                        key = i;
                        break;
                    }
                }
                ArrayList<ArrayList<String>> result = new ArrayList<>();
                while (!(var3 = reader.readLine()).isEmpty()) {
                    line = var3.trim().split("\\t");
                    ArrayList<String> record = new ArrayList<>();
                    Collections.addAll(record, line);
                    result.add(record);
                }
                values = new double[result.size()][result.get(0).size()];
                for (int i = 0; i < result.size(); i++) {
                    ArrayList<String> row = result.get(i);
                    for (int j = 0; j < row.size(); j++)
                        values[i][j] = Double.parseDouble(row.get(j));
                }
                transform.put(key, values);
            }
        } catch (IOException e) {
            System.err.println("No data transformation file for this model\n");
        }

        return model;
    }

    /**
     * Sets provided values of parameters that will be used for training.
     *
     * @param type
     *            type of SVM
     * @param kernel
     *            type of kernel function
     * @param gamma
     *            value of the parameter gamma in kernel function
     * @param cost
     *            value of the parameter C of C-SVC, epsilon-SVR, and nu-SVR
     * @param eps
     *            tolerance of termination criterion
     * @param cache_size
     *            cache memory size in MB
     *
     */
    public void setSVMParameters(int type, int kernel, double gamma, double cost, double eps, double cache_size)
    {
        param.svm_type = type;
        param.kernel_type = kernel;
        param.gamma = gamma;
        param.C = cost;
        param.cache_size = cache_size; //default 100MB
        param.eps = eps; // default 0.001

        System.out.println("SVM params:");
        printFields(param);
    }

    /**
     * Sets provided values of parameters that will be used for training.
     *
     * @param type
     *            type of SVM
     * @param kernel
     *            type of kernel function
     * @param gamma
     *            value of the parameter gamma in kernel function
     * @param cost
     *            value of the parameter C of C-SVC, epsilon-SVR, and nu-SVR
     *
     */
    public void setSVMParameters(int type, int kernel, double gamma, double cost)
    {
        setSVMParameters(type, kernel, gamma, cost, 1e-3, 100);
        //param.probability = 1;
    }

    /**
     * Prints all fields of object
     *
     * @param o
     *          object to print
     *
     */
    public static void printFields(Object o)
    {
        if (o!=null)
        {
            try
            {
                Field fields[] = o.getClass().getDeclaredFields();
                for (Field field: fields)
                    if ((field.getModifiers() & Modifier.STATIC) == 0)
                        System.out.println(field.getName() + " (" + field.getType() + "): " + field.get(o));
            }
            catch (Exception e) {}
            System.out.println();
        }
    }

    public LinkedHashMap<Integer, double[][]> getTransform() {
        return transform;
    }

    public void transform(Instance instance) {
        if (!transform.isEmpty()) {
            for (Map.Entry<Integer, double[][]> entry : transform.entrySet()) {
                Integer key = entry.getKey();
                double[][] values = entry.getValue();
                DatasetTools.dataTransformation(instance, key, values);
            }
        }
    }
}