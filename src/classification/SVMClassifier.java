package classification;

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


public class SVMClassifier implements Classifier {

    private static final long serialVersionUID = 7293859346790783381L;

    private svm_problem problem;
    private svm_parameter parameters;
    private svm_model model;
    private LinkedHashMap<Integer, double[][]> transformations;

    /**
     * Constructor of SVMClassifier. Initializes class attributes.
     */
    public SVMClassifier()
    {
        parameters = new svm_parameter();
        transformations = new LinkedHashMap<>();

        parameters.svm_type = svm_parameter.C_SVC;
        parameters.kernel_type = svm_parameter.RBF;
        parameters.degree = 3;
        parameters.gamma = 0;	// 1/num_features
        parameters.coef0 = 0;
        parameters.nu = 0.5;
        parameters.cache_size = 100;
        parameters.C = 1;
        parameters.eps = 1e-3;
        parameters.p = 0.1;
        parameters.shrinking = 1;
        parameters.probability = 0;
        parameters.nr_weight = 0;
        parameters.weight_label = null;
        parameters.weight = null;
    }

    /**
     * Saves information about data transformation.
     *
     * @param transformation
     *          number defining the methid of data transformation
     * @param values
     *          table of values needed to do the same transformation on the other part of data
     *
     */
    public void addDataTransformation(Integer transformation, double[][] values)
    {
        transformations.put(transformation, values);
    }

    @Override
    public void buildClassifier(Dataset data) {
        defineSVMProblem(data);
        model = svm.svm_train(problem, parameters);
    }

    /**
     * Transforms dataset into instance of svm_problem (from libsvm library) class.
     *
     * @param train
     *          data set used to build classifier
     *
     */
    public void defineSVMProblem(Dataset train)
    {
        int recordCount = train.size();
        int featureCount = train.numFeatures();

        problem = new svm_problem();
        problem.y = new double[recordCount];
        problem.l = recordCount;
        problem.x = new svm_node[recordCount][featureCount];

        for (int i = 0; i < recordCount; i++)
        {
            double[] features = train.getInstance(i).allFeaturesValues();
            problem.x[i] = new svm_node[featureCount];

            for (int j = 0; j < features.length; j++)
            {
                svm_node node = new svm_node();
                node.index = j;
                node.value = features[j];
                problem.x[i][j] = node;
            }
            problem.y[i] = Double.parseDouble(train.getInstance(i).getClassValue().toString());
        }
    }

    @Override
    public DatasetResult classify(Dataset test)
    {

        DatasetResult results = new DatasetResult();
        Instance instance;

        for(int k = 0; k < test.size(); k++)
        {
            instance = test.getInstance(k);
            results.add(classify(instance));
        }

        return results;
    }

    @Override
    public InstanceResult classify(Instance instance)
    {
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
        }
        else
            {
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
    public void saveModel(String filename) throws IOException
    {
        if (!transformations.isEmpty())
        {
            String transformName = ".//out//" + filename + "_transformation.txt";
            DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(transformName)));
            for (Map.Entry<Integer, double[][]> entry : transformations.entrySet())
            {
                Integer key = entry.getKey();
                stream.writeBytes("transformation " + DatasetTools.transformTable[key] + "\n");
                double[][] values = entry.getValue();
                for (double[] value : values)
                {
                    for (double aValue : value)
                    {
                        stream.writeBytes(aValue + "\t");
                    }
                    stream.writeBytes("\n");
                }
                stream.writeBytes("\n\n");
            }
            stream.close();
        }

        String modelName = ".//out//" + filename + "_model.txt";
        svm.svm_save_model(modelName, this.model);
    }

    @Override
    public svm_model loadModel(String filename) throws IOException
    {
        System.out.println("\nLoading model parameters from file...");
        String modelName = ".//out//" + filename + "_model.txt";
        this.model = svm.svm_load_model(modelName);
        this.parameters = model.param;

        try
        {
            System.out.println("Loading information about data transformation from file...\n");
            String transformName = ".//out//" + filename + "_transformation.txt";
            Integer key = -1;
            double[][] values;
            BufferedReader reader  = new BufferedReader(new FileReader(transformName));
            String var3;
            while ((var3 = reader.readLine()).startsWith("transformation"))
            {
                String[] line = var3.trim().split(" ");
                for(int i = 0; i < DatasetTools.transformTable.length; i++)
                {
                    if (line[1].equals(DatasetTools.transformTable[i]))
                    {
                        key = i;
                        break;
                    }
                }
                ArrayList<ArrayList<String>> result = new ArrayList<>();
                while (!(var3 = reader.readLine()).isEmpty())
                {
                    line = var3.trim().split("\\t");
                    ArrayList<String> record = new ArrayList<>();
                    Collections.addAll(record, line);
                    result.add(record);
                }
                values = new double[result.size()][result.get(0).size()];
                for (int i = 0; i < result.size(); i++)
                {
                    ArrayList<String> row = result.get(i);
                    for (int j = 0; j < row.size(); j++)
                        values[i][j] = Double.parseDouble(row.get(j));
                }
                transformations.put(key, values);
            }
        }
        catch (IOException e)
        {
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
     *            value of the parameters gamma in kernel function
     * @param cost
     *            value of the parameters C of C-SVC, epsilon-SVR, and nu-SVR
     * @param weight_labels
     *            labels of getClasses meaning the order of weights
     * @param weights
     *            values of individual weights of cost for each class
     * @param eps
     *            tolerance of termination criterion
     * @param cache_size
     *            cache memory size in MB
     * @param prob
     *            whether to calculate probability of all getClasses or just output score from model
     *
     */
    public void setSVMParameters(int type, int kernel, double gamma, double cost, int[] weight_labels, double[] weights,
                                 double eps, double cache_size, int prob)
    {
        parameters.svm_type = type;
        parameters.kernel_type = kernel;
        parameters.gamma = gamma;
        parameters.C = cost;
        if (weights != null)
        {
            parameters.nr_weight = weights.length;
            parameters.weight_label = weight_labels;
            parameters.weight = weights;
        }
        parameters.cache_size = cache_size; //default 100MB
        parameters.eps = eps; // default 0.001
        parameters.probability = prob;

        if (model != null)
            model.param = parameters;

        //System.out.println("\nSVM params:");
        //printFields(parameters);
    }

    /**
     * Sets provided values of parameters that will be used for training.
     *
     * @param kernel
     *            type of kernel function
     * @param gamma
     *            value of the parameters gamma in kernel function
     * @param cost
     *            value of the parameters C of C-SVC, epsilon-SVR, and nu-SVR
     *
     */
    public void setSVMParameters(int kernel, double gamma, double cost)
    {
        setSVMParameters(svm_parameter.C_SVC, kernel, gamma, cost, null, null, 1e-3, 100, 0);
    }

    /**
     * Sets provided values of parameters that will be used for training.
     *
     * @param kernel
     *            type of kernel function
     * @param gamma
     *            value of the parameters gamma in kernel function
     * @param cost
     *            value of the parameters C of C-SVC, epsilon-SVR, and nu-SVR
     * @param weight_labels
     *            labels of getClasses meaning the order of weights
     * @param weights
     *            values of individual weights of cost for each class
     * @param prob
     *            whether to calculate probability of all getClasses or just output score from model
     *
     */
    public void setSVMParameters(int kernel, double gamma, double cost, int[] weight_labels, double[] weights, int prob)
    {
        setSVMParameters(svm_parameter.C_SVC, kernel, gamma, cost, weight_labels, weights, 1e-3, 100, prob);
    }

    /**
     * Prints all fields of object.
     *
     * @param o
     *          object to print
     *
     */
    private static void printFields(Object o)
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

    /**
     * Getter of information about all data transformations.
     *
     * @return map with transformations methods and values needed to restore/repeat transformation
     *
     **/
    public LinkedHashMap<Integer, double[][]> getTransformations() {
        return transformations;
    }

    /**
     * Transforms new instance of data set using the same methods which where earlier used on the whole data set.
     *
     * @param instance
     *          new instance to be transformed
     *
     **/
    public void transform(Instance instance)
    {
        if (!transformations.isEmpty())
        {
            for (Map.Entry<Integer, double[][]> entry : transformations.entrySet())
            {
                Integer key = entry.getKey();
                double[][] values = entry.getValue();
                DatasetTools.transformData(instance, key, values);
            }
        }
    }

    /**
     * Getter of models parameters.
     *
     * @return parameters of SVM model
     *
     **/
    public svm_parameter getSVMParameters()
    {
        return this.model.param;
    }

    /**
     * Getter of SVM model.
     *
     * @return model
     *
     **/
    public svm_model getSVMModel()
    {
        return this.model;
    }
}