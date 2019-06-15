import mscanlib.ms.fdr.FDRConfig;
import mscanlib.ms.fdr.FDRTools;
import mscanlib.ms.msms.dbengines.DbEngineScoring;

public class FdrSvmConfig extends FDRConfig
{
	public boolean mComputeSVM = true;
	public double mQValueThreshold = 0.01;
	public boolean mSaveDataset = false;
	public boolean mSaveTrainDataset = false;
	
	public FdrSvmConfig()
	{
		this.mDatabase=FDRTools.DB_JOYNED;	
		this.mScoreMethod=DbEngineScoring.SCORE_MMT;
		this.mBothDbMethod=FDRTools.BOTH_EXCLUDE;
		
		this.mPlotsMin=0;
		this.mPlotsMax=0.2;
	}
}
