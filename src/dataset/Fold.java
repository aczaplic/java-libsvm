package dataset;

import java.util.*;

public class Fold implements Dataset {
    private int[] indices;
    private Dataset parent;

    /**
     *
     * Constructor of Fold class. Creates one fold of data set that contains the instances with the provided indices.
     *
     * @param indices
     *            provided indices of data which are ascribed to this fold
     * @param parent
     *            data set to be divided into folds
     *
     */
    public Fold(Dataset parent, int[] indices) {
        this.indices = indices;
        this.parent = parent;
    }

    @Override
    public SortedSet<Object> getClasses() {
        return parent.getClasses();
    }

    @Override
    public int size() {
        return indices.length;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean contains(Object o) {
        if (parent.contains(o))
            for(Instance i:this)
                if (i.equals(o)) return true;
        return false;
    }

    @Override
    public Iterator<Instance> iterator() {
        return new FoldIterator();
    }

    class FoldIterator implements ListIterator<Instance> {

        private int currentIndex = 0;

        public FoldIterator(int index) {
            this.currentIndex = index;
        }

        public FoldIterator() {
            this(0);
        }

        @Override
        public boolean hasNext() {
            return currentIndex < indices.length;
        }

        @Override
        public Instance next() {
            currentIndex++;
            return getInstance(currentIndex - 1);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Forbidden operation for a fold of a data set.");

        }

        @Override
        public void add(Instance arg0) {
            throw new UnsupportedOperationException("Forbidden operation for a fold of a data set.");

        }

        @Override
        public boolean hasPrevious() {
            return currentIndex > 0;
        }

        @Override
        public int nextIndex() {
            return currentIndex;
        }

        @Override
        public Instance previous() {
            currentIndex--;
            return getInstance(currentIndex);
        }

        @Override
        public int previousIndex() {
            return currentIndex;
        }

        @Override
        public void set(Instance arg0) {
            throw new UnsupportedOperationException("Forbidden operation for a fold of a data set.");

        }
    }

    @Override
    public Object[] toArray() {
        Object[] out = new Object[indices.length];
        for (int i = 0; i < size(); i++) {
            out[i] = getInstance(i);
        }
        return out;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        Vector<T> tmp = new Vector<T>();
        for (Instance i : this) {
            tmp.add((T) i);
        }
        return tmp.toArray(a);
    }

    @Override
    public boolean add(Instance i) {
        throw new UnsupportedOperationException("Forbidden operation for a fold of a data set.");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Forbidden operation for a fold of a data set.");
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends Instance> collection) {
        throw new UnsupportedOperationException("Forbidden operation for a fold of a data set.");
    }

    @Override
    public boolean addAll(int i, Collection<? extends Instance> collection) {
        throw new UnsupportedOperationException("Forbidden operation for a fold of a data set.");
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        throw new UnsupportedOperationException("Forbidden operation for a fold of a data set.");
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        throw new UnsupportedOperationException("Forbidden operation for a fold of a data set.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Forbidden operation for a fold of a data set.");
    }

    @Override
    public Instance get(int i) {
        return getInstance(i);
    }

    @Override
    public Instance set(int i, Instance instance) {
        throw new UnsupportedOperationException("Forbidden operation for a fold of a data set.");
    }

    @Override
    public void add(int i, Instance instance) {
        throw new UnsupportedOperationException("Forbidden operation for a fold of a data set.");
    }

    @Override
    public Instance remove(int i) {
        throw new UnsupportedOperationException("Forbidden operation for a fold of a data set.");
    }

    @Override
    public int indexOf(Object o) {
        if (parent.contains(o))
            for(int i = 0; i < indices.length; i++)
                if (getInstance(i).equals(o)) return i;
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        int last = -1;
        if (parent.contains(o))
            for(int i = 0; i < indices.length; i++)
                if (getInstance(i).equals(o)) last = i;
        return last;
    }

    @Override
    public ListIterator<Instance> listIterator() {
        return new FoldIterator();
    }

    @Override
    public ListIterator<Instance> listIterator(int i) {
        return new FoldIterator(i);
    }

    @Override
    public List<Instance> subList(int i, int i1) {
        throw new UnsupportedOperationException("Forbidden operation for a fold of a data set.");
    }

    @Override
    public Instance getInstance(int index) {
        return parent.getInstance(indices[index]);
    }

    @Override
    public double[] getFeature(int pos) {
        double[] values = new double[this.size()];
        for(int i=0; i<this.size(); i++){
            values[i] = getInstance(i).getFeatureValue(pos);
        }
        return values;
    }

    @Override
    public int numFeatures() {
        return parent.numFeatures();
    }

    @Override
    public int getClassIndex(Instance instance) {
        return parent.getClassIndex(instance);
    }

    @Override
    public Object getClassValue(int index) {
        return parent.getClassValue(index);
    }

    @Override
    public Dataset[] folds(int numFolds, Random rg, boolean stratified) {
        throw new UnsupportedOperationException("Forbidden operation for a fold of a data set.");
    }

    @Override
    public Dataset copy() {
        Dataset out=new BasicDataset();
        for(Instance i:this)
            out.add(i.copy());
        return out;
    }
}
