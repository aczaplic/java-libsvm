package dataset;

public abstract class AbstractInstance implements Instance {

    private static final long serialVersionUID = -1562796124343989825L;

    static int nextID = 0;

    private final int ID;

    public int getID() {
        return ID;
    }

    private Object classValue;

    /**
     * Creates a new instance with the class label set to null.
     *
     */
    protected AbstractInstance() {
        this(null);
    }

    /**
     * Creates a new instance with the provide value of class label.
     *
     * @param classValue
     *            the value of class label for instance
     */
    protected AbstractInstance(Object classValue) {
        ID = nextID;
        nextID++;
        this.setClassValue(classValue);
    }

    @Override
    public Object classValue() {
        return classValue;
    }

    @Override
    public void setClassValue(Object value) {
        this.classValue = value;
    }
}
