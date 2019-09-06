import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;

import libsvm.svm_parameter;
import mscanlib.common.MScanException;
import mscanlib.math.stats.regression.LS;
import mscanlib.ms.exp.Sample;
import mscanlib.ms.exp.SampleTools;
import mscanlib.ms.fdr.FDRTools;
import mscanlib.ms.mass.MassTools;
import mscanlib.ms.msms.MsMsAssignment;
import mscanlib.ms.msms.MsMsProteinHit;
import mscanlib.ms.msms.MsMsQuery;
import mscanlib.ms.msms.dbengines.mascot.io.MascotDatFileReader;
import mscanlib.ms.msms.io.MsMsScanConfig;
import mscanlib.ms.rt.RTPredictor;
import mscanlib.ui.interfaces.MScanWorkerListener;
import mscanlib.ui.threads.MScanWorker;

public class FdrSvmTest implements MScanWorkerListener
{
	private Sample mSample;
	private String mFilename;
	private FdrSvmConfig mConfig = null;

	/**
	 * Konstruktor
	 * @param filename
	 */
	public FdrSvmTest(String filename)
	{
		this.mFilename=filename;
		
		/*
		 * Odczyt pliku
		 */
		if ((this.mSample=this.readSample(filename))!=null)
		{
			/*
			 * 	Utworzenie konfiguracji
			 */
			this.mConfig=new FdrSvmConfig();
			this.mConfig.setmComputeSVM(true);				//liczenie q-wartosci na podstawie score SVM
			this.mConfig.setmQValueThreshold(0.2);			//prog q-wartosci dla zbioru treningowego
            this.mConfig.setmQValueOptimization(0.05);    	//prog q-wartosci do optymalizacji parametrow
			
			this.mConfig.mPlotsMin = 0.0;					//zakresy q-wartosci dla wykresow
			this.mConfig.mPlotsMax = 0.2;

			this.mConfig.setmSaveDataset(false);			//zapis zbiorów danych do plików
			this.mConfig.setmSaveTrainDataset(false);

            this.mConfig.setmBoostIter(3);                	//liczba iteracji wyznaczenia zbioru przykładów pozytywnych na podstawie nowego score
			this.mConfig.setmOptimize(true);              	//optymalizacja parametrow modelu SVM
            this.mConfig.setmOptimizeIter(3);             	//liczba iteracji optymalizacji (usrednienie wynikow)
            this.mConfig.setmCVFolds(3);                  	//liczba zbiorow walidacyjnych przy optymalizacji (jeśli 1 -> mOptimizeIter=1)
            this.mConfig.setmKernel(svm_parameter.LINEAR);  //typ jadra SVM

//			this.mConfig.setmC(new double[]{0.1, 1, 10});
//			this.mConfig.setmCneg_pos(new double[]{1, 3, 10});

			this.mConfig.getmScoreConfig().getFragmentationConfig().setInstrument(this.mSample.getHeader().getInstrument());
			this.mConfig.getmScoreConfig().setFragmentMMD(this.mSample.getHeader().getFragmentMMD());
			this.mConfig.getmScoreConfig().setFragmentMMDUnit(this.mSample.getHeader().getFragmentMMDUnit());

			/*
			 * Uruchomienie watku obliczeniowego
			 */
			FdrSvmWorker worker=new FdrSvmWorker(null,new Sample[] {this.mSample},this.mConfig);
			worker.addWorkerListener(this);
			worker.start();
		}
	}
	
	/**
	 * Metoda wywolywana po zakonczeniu obliczen
	 */
	@Override
	public void workerFinished(MScanWorker worker)
	{
		/*
		 * 	Wykresy q-wartosci
		 */
		FDRTools.plotNumber(((FdrSvmWorker)worker).getQValues(),((FdrSvmWorker)worker).getConfig());
		if (this.mConfig.ismComputeSVM())
			FDRTools.plotNumber(((FdrSvmWorker)worker).getQValuesSVM(),((FdrSvmWorker)worker).getConfig());
		
		/*
		 * Zapisanie wynikow
		 */
		PrintStream consoleStream=System.out;
		try
		{
			PrintStream fileStream = new PrintStream(new File(this.mFilename.substring(0 , this.mFilename.lastIndexOf(".")) + "_fdr.txt"));
			System.setOut(fileStream); 
		}
		catch (Exception e) {}
		
		//licznik przypisan o q-wartosci <= od progu
		int qPos=0;
		double[] thresholds = {0.01, 0.05, 0.1, 0.2};
		int[] nrPositive = new int[thresholds.length];
		
		//dla kazdego zapytania 
		for (MsMsQuery query: this.mSample.getQueries())
		{
			if (query!=null)
			{
				//dla kazdego przypisania z danego zapytania
				for (MsMsAssignment assignment: query.getAssignmentsList())
				{
					StringBuilder str=new StringBuilder();
					
					str.append(query.getNr());	//nr zapytania
					str.append("\t");
					if (assignment.getDecoy()==FDRTools.IS_DECOY)		//baza target/decoy
						str.append("D");
					else if (assignment.getDecoy()==FDRTools.IS_TARGET)
						str.append("T");
					else if (assignment.getDecoy()==FDRTools.IS_BOTH)
						str.append("T/D");
					else
						str.append("-");
					str.append("\t");
					str.append(assignment.getSequence());	//sekwencja
					str.append("\t");
					for (MsMsProteinHit p:assignment.getProteinHits())	//lista bialek
					{
						str.append(p.getId());
						str.append(";");
					}
					str.append("\t");
					str.append(query.getCharge());	//stopien naladowania
					str.append("\t");
					str.append(query.getMz());		//m/z
					str.append("\t");
					str.append(query.getMass());	//masa
					str.append("\t");
					if (!assignment.isNA())			
						str.append(MassTools.getDelta(query.getMass(),assignment.getCalcMass()));	//blad masy
					else
						str.append("-");
					str.append("\t");
					str.append(query.getRtInSec());	//czas retencji
					str.append("\t");
					str.append(query.getPrecursorIntensity());	//wysokosc piku prekursora
					str.append("\t");
					if (!assignment.isNA())
					{
						str.append(assignment.getScore());        //score
						str.append("\t");
						str.append(assignment.getScoreDelta());    //score delta
						str.append("\t");
						str.append(query.getMIT());                //MIT
						str.append("\t");
						str.append(query.getMHT());                //MHT
						str.append("\t");
						str.append(assignment.getEValue());        //e-wartosc
						str.append("\t");
						str.append(assignment.getQValue());        //q-wartosc
					}
					else
						str.append("-\t-\t-\t-\t-\t-");

					System.out.println(str);
				}
			}
		}
		System.setOut(consoleStream);
		

		System.out.println("Done");
	}

	@Override
	public void workerError(MScanWorker worker, MScanException mse)
	{
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Metoda odczytujaca wyniki przeszukania bazodanowego
	 * 
	 * @param filename nazwa pliku wejsiowego
	 * 
	 * @return wyniki przeszukania bazodanowego
	 */
	public Sample readSample(String filename)
    {
        Sample sample = null;
        MsMsScanConfig scanConfig = new MsMsScanConfig();
        scanConfig.mReadSpectra = true;

        try
        {
            System.out.print("Reading file: " + filename + "... ");
            MascotDatFileReader fileReader = new MascotDatFileReader(filename, scanConfig);
            fileReader.readFile();

            sample = new Sample("Test sample");
            sample.setQueries(fileReader.getQueries(), true);
            sample.setHeader(fileReader.getHeader());

            System.out.println(sample.getQueries().length + " queries.");

            SampleTools.updateRtRange(sample);            //zakres czasu retencji

            try                                            //czas retencji
            {
                RTPredictor predictor = new RTPredictor(RTPredictor.METHOD_KROKHIN3);

                ArrayList<Double> rtList = new ArrayList<>();
                ArrayList<Double> rtPredList = new ArrayList<>();

                for (MsMsQuery query : sample.getQueries())
                {
                    if (query != null && query.isSignificant())
                    {
                        MsMsAssignment assignment = query.getAssignment();
                        if (!assignment.isNA())
                        {
                            rtList.add(query.getRtInSec());
                            rtPredList.add(predictor.getRt(assignment.getSequence()));
                        }
                    }
                }

                double rt[] = new double[rtList.size()];
                double rtPred[] = new double[rtList.size()];
                for (int i = 0; i < rtList.size(); i++)
                {
                    rt[i] = rtList.get(i);
                    rtPred[i] = rtPredList.get(i);
                }

                LS ls = new LS();
                double coeffs[] = ls.regress(rtPred, rt, true);

                for (MsMsQuery query : sample.getQueries())
                {
                    if (query != null)
                    {
                        for (MsMsAssignment assignment : query.getAssignmentsList())
                            if (!assignment.isNA())
                                assignment.setCalcRtInSec(coeffs[0] * predictor.getRt(assignment.getSequence()) + coeffs[1]);
                    }
                }
            }
            catch (Exception e) {}
        }
		catch (MScanException mse)
		{
			System.out.println(mse.toString());
		}
		return(sample);
	}


	/**
	 * Main
	 */
	public static void main(String[] args)
	{
		if (args!=null && args.length>0)
			new FdrSvmTest(args[0]);
        else
            new FdrSvmTest(".//data//HAX_3.dat");					//z enzymem (semitypsyna)
            //new FdrSvmTest(".//data//CA_A.dat");					//z enzymem (semitypsyna)
            //new FdrSvmTest(".//data//peptytdy_CA_A.dat");			//bez eznymu
    }
}
