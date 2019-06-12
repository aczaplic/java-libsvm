package dataset;

public abstract class AbstractInstance implements Instance {

    private static final long serialVersionUID = -1562796124343989825L;

    static int nextID = 0;

    private final int ID;
    private Object userData=null;
    private Object classValue;

    public int getID() {
        return ID;
    }

    public Object getUserData()
    {
        return(this.userData);
    }

    public void setUserData(Object data)
    {
        this.userData=data;
    }

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
    public Object getClassValue() {
        return classValue;
    }

    @Override
    public void setClassValue(Object value) {
        this.classValue = value;
    }
}
