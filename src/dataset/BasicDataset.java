package dataset;

import java.util.*;

public class BasicDataset extends ArrayList<Instance> implements Dataset {

    private static final long serialVersionUID = -879896157683879525L;

    private TreeSet<Object> classes = new TreeSet<>();

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
     * Checks if instance has the same number of features
     * as all instances in data set before adding.
     *
     * @param inst
     *            instance to check
     */
    private boolean check(Instance inst) {
        return this.size() == 0 || inst.numFeatures() == this.get(0).numFeatures();
    }

    /**
     *
     * Checks if all instances have the same number of features,
     * so can be added to one data set.
     *
     * @param c
     *            collection with instances
     */
    private boolean check(Collection<? extends Instance> c) {
        Set<Integer> numFeatures = new TreeSet<>();
        for (Instance i : c)
            numFeatures.add(i.numFeatures());
        return numFeatures.size() == 1;
    }

    @Override
    public synchronized boolean add(Instance inst) {
        if (check(inst)) {
            if (inst.classValue() != null)
                classes.add(inst.classValue());
            return super.add(inst);
        }
        return false;
    }

    @Override
    public synchronized boolean addAll(Collection<? extends Instance> coll) {
        if (check(coll)) {
            for (Instance inst : coll)
                if (inst.classValue() != null)
                    classes.add(inst.classValue());
            return super.addAll(coll);
        }
        return false;
    }

    @Override
    public SortedSet<Object> classes() {
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
            values[i] = super.get(i).featureValue(pos);
        }
        return values;
    }

    @Override
    public int numFeatures() {
        if (this.size() == 0)
            return 0;
        return get(0).numFeatures();
    }

    @Override
    public int classIndex(Instance inst) {
        if (inst.classValue() != null)
            return this.classes().headSet(inst.classValue()).size();
        else
            return -1;
    }

    @Override
    public Object classValue(int index) {
        return this.classes().toArray()[index];
    }

    @Override
    public Dataset copy() {
        BasicDataset dataset = new BasicDataset();
        for (Instance i : this)
            dataset.add(i.copy());
        return dataset;
    }

}
