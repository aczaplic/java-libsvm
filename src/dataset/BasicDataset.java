package dataset;

import java.util.*;

public class BasicDataset extends ArrayList<Instance> implements Dataset {

    private static final long serialVersionUID = -879896157683879525L;

    private TreeSet<Object> classes = new TreeSet<>();
    private ArrayList<String> featuresNames = new ArrayList<>();

    /**
     * Creates an empty data set.
     */
    public BasicDataset() {
        // nothing to do.
    }

    /**
     *
     * Creates a data set that contains the provided instances of data.
     *
     * @param coll
     *            collection with instances
     */
    public BasicDataset(Collection<Instance> coll) {
        this.addAll(coll);
    }

    /**
     *
     * Creates a data set that contains the provided instances of data and keeps the names of all features.
     *
     * @param featuresNames
     *            names of all features
     * @param coll
     *            collection with instances
     */
    public BasicDataset(Collection<Instance> coll, ArrayList<String> featuresNames) {
        this.featuresNames = featuresNames;
        this.addAll(coll);
    }

    /**
     *
     * Checks if instance has the same number of features as all instances in data set before adding.
     *
     * @param inst
     *            instance to check
     */
    private boolean check(Instance inst) {
        return this.size() == 0 || inst.numFeatures() == this.get(0).numFeatures();
    }

    /**
     *
     * Checks if all instances have the same number of features and if this number is equal to number of features
     * in data set before, so can be added to this data set.
     *
     * @param c
     *            collection with instances
     */
    private boolean check(Collection<? extends Instance> c) {
        Set<Integer> numFeatures = new TreeSet<>();
        if (this.size() > 0) numFeatures.add(this.numFeatures());
        for (Instance i : c)
            numFeatures.add(i.numFeatures());
        return numFeatures.size() == 1;
    }

    /**
     * Getter of the list with names of features in data set.
     *
     * @return a list of the features names
     *
     */
    public ArrayList<String> getFeaturesNames(){
        return this.featuresNames;
    }

    /**
     * Setter of the list with names of features in data set.
     *
     * @param featuresNames
     *            a list of the features names
     *
     */
    public void setFeaturesNames(ArrayList<String> featuresNames){
        if (featuresNames.size() == this.numFeatures())
            this.featuresNames = featuresNames;
        this.featuresNames = new ArrayList<>();
    }

    @Override
    public synchronized boolean add(Instance inst) {
        if (check(inst)) {
            if (inst.getClassValue() != null)
                classes.add(inst.getClassValue());
            return super.add(inst);
        }
        return false;
    }

    @Override
    public synchronized boolean addAll(Collection<? extends Instance> coll) {
        if (check(coll)) {
            for (Instance inst : coll)
                if (inst.getClassValue() != null)
                    classes.add(inst.getClassValue());
            return super.addAll(coll);
        }
        return false;
    }

    @Override
    public SortedSet<Object> getClasses() {
        return classes;
    }

    @Override
    public Instance getInstance(int index) {
        return super.get(index);
    }

    @Override
    public double[] getFeature(int pos) {
        double[] values = new double[this.size()];
        for(int i=0; i<this.size(); i++){
            values[i] = super.get(i).getFeatureValue(pos);
        }
        return values;
    }

    @Override
    public int numFeatures() {
        if (this.size() == 0)
            return 0;
        if (this.featuresNames.size() != 0)
            return this.featuresNames.size();
        return this.getInstance(0).numFeatures();
    }

    @Override
    public int getClassIndex(Instance inst) {
        if (inst.getClassValue() != null)
            return this.getClasses().headSet(inst.getClassValue()).size();
        else
            return -1;
    }

    @Override
    public Object getClassValue(int index) {
        return this.getClasses().toArray()[index];
    }

    @Override
    public Dataset[] folds(int numFolds, Random rg, boolean stratified) {
        Dataset[] out = new Dataset[numFolds];
        List<List<Integer>> indices = new ArrayList<>();
        if (stratified) for (Object ignored : classes) indices.add(new Vector<Integer>());
        else indices.add(new Vector<Integer>());

        if (stratified) {
            int ind;
            for (int i = 0; i < this.size(); i++) {
                ind = getClassIndex(this.getInstance(i));
                indices.get(ind).add(i);
            }
        }
        else
            for (int i = 0; i < this.size(); i++)
                indices.get(0).add(i);

        int size = (this.size() / numFolds) + 1;
        int[][] array = new int[numFolds][size];
        Iterator<List<Integer>> iterator = indices.iterator();
        List<Integer> class_indices = iterator.next();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < numFolds; j++) {
                if (class_indices.size() > 0) {
                    array[j][i] = class_indices.remove(rg.nextInt(class_indices.size()));
                    if (class_indices.size() == 0 && iterator.hasNext())
                        class_indices = iterator.next();
                }
                else
                    array[j][i] = -1;
            }
        }

        for (int i = 0; i < numFolds; i++) {
            int[] ind_i;
            if (array[i][size - 1] == -1) {
                ind_i = new int[size - 1];
                System.arraycopy(array[i], 0, ind_i, 0, size - 1);
            } else {
                ind_i = new int[size];
                System.arraycopy(array[i], 0, ind_i, 0, size);
            }
            out[i] = new Fold(this, ind_i);

        }
        return out;
    }

    @Override
    public Dataset copy() {
        BasicDataset dataset = new BasicDataset();
        for (Instance i : this)
            dataset.add(i.copy());
        return dataset;
    }

}
