import mscanlib.ms.fdr.FDRConfig;
import mscanlib.ms.fdr.FDRTools;
import mscanlib.ms.msms.dbengines.DbEngineScoring;

public class FdrSvmConfig extends FDRConfig
{
	public boolean mComputeSVM = true;
	public double mQValueThreshold = 0.2;
	public double mQValueOptimization = 0.05;
	public boolean mSaveDataset = false;
	public boolean mSaveTrainDataset = false;
	public int mBoostIter = 5;
	public boolean mOptimize = true;
	public int mOptimizeIter = 5;
    public int mCVFolds = 3;
	public int mKernel = 2;
	public double[] mC = {0.1, 1, 10, 100};
	public double[] mCneg_pos = {1, 3, 10};
	public double[] mGamma = {0.01, 0.05, 1, 5, 10};
	
	public FdrSvmConfig()
	{
		this.mDatabase=FDRTools.DB_JOYNED;	
		this.mScoreMethod=DbEngineScoring.SCORE_MMT;
		this.mBothDbMethod=FDRTools.BOTH_EXCLUDE;
		
		this.mPlotsMin=0;
		this.mPlotsMax=0.2;
	}
}
