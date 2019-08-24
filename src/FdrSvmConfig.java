import libsvm.svm_parameter;
import mscanlib.ms.fdr.FDRConfig;
import mscanlib.ms.fdr.FDRTools;
import mscanlib.ms.msms.dbengines.DbEngineScoring;
import mscanlib.ms.msms.dbengines.DbEngineScoringConfig;

public class FdrSvmConfig extends FDRConfig
{
	private boolean mComputeSVM = true;
	private double mQValueThreshold = 0.2;
	private double mQValueOptimization = 0.05;
	private boolean mSaveDataset = false;
	private boolean mSaveTrainDataset = false;
	private int mBoostIter = 5;
	private boolean mOptimize = true;
	private int mOptimizeIter = 5;
    private int mCVFolds = 3;
	private int mKernel = svm_parameter.RBF;
	private double[] mC = {0.1, 1, 10, 100};
	private double[] mCneg_pos = {1, 3, 10};
	private double[] mGamma = {0.01, 0.03, 1, 3, 10};
	private int mProbabilityCount = 0;
    private DbEngineScoringConfig mScoreConfig;

    public FdrSvmConfig()
    {
        this.mDatabase = FDRTools.DB_JOYNED;
        this.mScoreMethod = DbEngineScoring.SCORE_MMT;
        this.mBothDbMethod = FDRTools.BOTH_EXCLUDE;

        this.mPlotsMin = 0;
        this.mPlotsMax = 0.2;

        this.mScoreConfig = new DbEngineScoringConfig();
    }

    public boolean ismComputeSVM() {
        return mComputeSVM;
    }

    public void setmComputeSVM(boolean mComputeSVM) {
        this.mComputeSVM = mComputeSVM;
    }

    public double getmQValueThreshold() {
        return mQValueThreshold;
    }

    public void setmQValueThreshold(double mQValueThreshold) {
        this.mQValueThreshold = mQValueThreshold;
    }

    public double getmQValueOptimization() {
        return mQValueOptimization;
    }

    public void setmQValueOptimization(double mQValueOptimization) {
        this.mQValueOptimization = mQValueOptimization;
    }

    public boolean ismSaveDataset() {
        return mSaveDataset;
    }

    public void setmSaveDataset(boolean mSaveDataset) {
        this.mSaveDataset = mSaveDataset;
    }

    public boolean ismSaveTrainDataset() {
        return mSaveTrainDataset;
    }

    public void setmSaveTrainDataset(boolean mSaveTrainDataset) {
        this.mSaveTrainDataset = mSaveTrainDataset;
    }

    public int getmBoostIter() {
        return mBoostIter;
    }

    public void setmBoostIter(int mBoostIter) {
        this.mBoostIter = mBoostIter;
    }

    public boolean ismOptimize() {
        return mOptimize;
    }

    public void setmOptimize(boolean mOptimize) {
        this.mOptimize = mOptimize;
    }

    public int getmOptimizeIter() {
        return mOptimizeIter;
    }

    public void setmOptimizeIter(int mOptimizeIter) {
        this.mOptimizeIter = mOptimizeIter;
    }

    public int getmCVFolds() {
        return mCVFolds;
    }

    public void setmCVFolds(int mCVFolds) {
        this.mCVFolds = mCVFolds;
    }

    public int getmKernel() {
        return mKernel;
    }

    public void setmKernel(int mKernel) {
        this.mKernel = mKernel;
    }

    public double[] getmC() {
        return mC;
    }

    public void setmC(double[] mC) {
        this.mC = mC;
    }

    public double[] getmCneg_pos() {
        return mCneg_pos;
    }

    public void setmCneg_pos(double[] mCneg_pos) {
        this.mCneg_pos = mCneg_pos;
    }

    public double[] getmGamma() {
        return mGamma;
    }

    public void setmGamma(double[] mGamma) {
        this.mGamma = mGamma;
    }

    public int getmProbabilityCount() {
        return mProbabilityCount;
    }

    public void setmProbabilityCount(int mProbabilityCount) {
        this.mProbabilityCount = mProbabilityCount;
    }

    public DbEngineScoringConfig getmScoreConfig() {
        return mScoreConfig;
    }

    public void setmScoreConfig(DbEngineScoringConfig mScoreConfig) {
        this.mScoreConfig = mScoreConfig;
    }
}
