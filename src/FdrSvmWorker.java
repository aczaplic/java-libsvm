import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

import classification.ClassificationTools;
import classification.InstanceResult;
import classification.SVMClassifier;
import dataset.BasicDataset;
import dataset.BasicInstance;
import dataset.DatasetTools;
import mscanlib.math.MathFun;
import mscanlib.ms.exp.*;
import mscanlib.ms.fdr.FDRConfig;
import mscanlib.ms.fdr.FDRTools;
import mscanlib.ms.mass.MassTools;
import mscanlib.ms.msms.*;
import mscanlib.ms.msms.dbengines.DbEngineScoring;
import mscanlib.ui.*;
import mscanlib.ui.threads.*;

public final class FdrSvmWorker extends MScanWorker
{	
	/**
	 * Konfiguracja
	 */
	private FdrSvmConfig mConfig=null;
	
	/**
	 * Tablica probek
	 */
	private Sample mSamples[]=null;
	
	/**
	 * Listy tablic z wynikami liczenia q-wartosci (po jednaj na probke)
	 * 
	 * Kazda tablica ma 6 wierszy:
	 * 
	 * indeks Sample (wiersz {@link mscanlib.ms.fdr.FDRTools#ROW_SAMPLE}),
	 * indeks Query w ramach Sample (wiersz {@link mscanlib.ms.fdr.FDRTools#ROW_QUERY}),
	 * indeks Assignment w ramach Query (wiersz {@link mscanlib.ms.fdr.FDRTools#ROW_ASSIGNMENT}),
	 * q-wartosc (wiersz {@link mscanlib.ms.fdr.FDRTools#ROW_QVALUE}),
	 * znacznik decoy ({@link mscanlib.ms.fdr.FDRTools#ROW_DECOY}),
	 * score (wiersz {@link mscanlib.ms.fdr.FDRTools#ROW_SCORE}).
	 * 
	 * Liczbie kolumn rowna jest dlugosci tablic wejsciowych (kazda kolumna odpowida jednemu przypisaniu).
	 * Elementy w wierszach sa posortowane zgodnie z rosnacymi q-wartosciami (malejacym score).
	 */
	private List<double[][]>	mQValues=null;
	private List<double[][]> mQValuesSVM=null;
	
	/**
	 * Konstruktor
	 * @param frame
	 * @param samples
	 * @param config
	 */
	public FdrSvmWorker(MScanAppFrame frame, Sample samples[], FdrSvmConfig config)
	{
		super(frame);

		this.mSamples = samples;
		this.mConfig = config;
	}

	//
	//MScanWorker
	/**
	 * Metoda wywolywana przy uruchomieniu watku
	 */
	@Override
	public Object construct()
	{
		this.mQValues = new ArrayList<double[][]>(this.mSamples.length);
		this.mQValuesSVM = new ArrayList<double[][]>(this.mSamples.length);
		
		for (int i = 0; i<this.mSamples.length; i++)
			this.computeQValues(this.mSamples[i]);
		
		this.updateProgress("Done...",100);	
		return "Done";
	}
	/**
	 * Metoda wywolywana po zakonczeniu obliczen
	 */
	@Override
	public void finished()
	{	
		this.notifyFinished();
	}
	
	//
	//getters
	public FDRConfig getConfig()
	{
		return(this.mConfig);
	}
	
	public List<double[][]> getQValues()
	{
		return(this.mQValues);
	}
	
	public List<double[][]> getQValuesSVM()
	{
		return(this.mQValuesSVM);
	}
	
	public Sample[] getSamples()
	{
		return(this.mSamples);
	}
	
	//
	//private
	/**
	 * Metoda liczaca q-wartosci 
	 * @param sample
	 */
	private void computeQValues(Sample sample)
	{
		MsMsQuery queries[] = null;
		double qValues[][] = null, qValuesSVM[][] = null;
		
		//pobranie tablicy zapytan do systemu bazodanowego (obiekty klasy MsMsQuery, zawieraja informacje o widmie oraz przypisanych do niego sekwencjach)
		if ((queries=sample.getQueries(true))!=null)
		{	
			//liczenie q-wartosci na podstawie score Mascota
			qValues = FDRTools.computeQValues(queries,this.mConfig,this,0);
			if (qValues!=null)
			{
				//zapamiatanie q-wartosci
				FDRTools.setQValues(sample,qValues);				
				this.mQValues.add(qValues);
			
				//liczenie nowych q-wartosci z uzyciem SVM
				if (this.mConfig.mComputeSVM)
				{
					//liczenie score SVM
					double scores[] = this.computeSvmScores(queries);
	//				//System.out.println(Arrays.toString(scores));
					
					//liczenie q-wartosci na podstawie score SVM 
					qValuesSVM = FDRTools.computeQValues(queries,scores,this.mConfig,this,0);
					//System.out.println(Arrays.toString(qValuesSVM[FDRTools.ROW_QVALUE]));
					
					//zapamietanie q-wartosci
					FDRTools.setQValues(sample,qValuesSVM);
					this.mQValuesSVM.add(qValuesSVM);
				}
			}
		}
	}

	
	/**
	 * Metoda liczaca score SVM  
	 * 
	 * @param queries
	 * @return
	 */
	private double[] computeSvmScores(MsMsQuery queries[])
	{
		/*
		 *  Budowanie modelu
		 */
		
		//Tworzenie zbioru treningowego (wszystkie decoy i target o q-wartosciach <= od progu)
		BasicDataset trainDataset = this.createTrainDataset(queries);
		double[][] min_max = DatasetTools.normalizeMinMax(trainDataset);

		//Trenowanie klasyfikatora
		SVMClassifier svm = new SVMClassifier();
        svm.addDataTransformation(DatasetTools.NormMinMax, min_max);
		svm.buildClassifier(trainDataset);

//        try {
//            svm.saveModel("test");
//            svm.loadModel("test");
//        } catch (IOException e) {
//            System.err.println("\nNo such file with model - check the name\n");
//            e.printStackTrace();
//        }

        /*
		 * Tworzenie tablicy wartosci score do liczenia q-wartosci
		 */

		//alokacja tablicy o dlugosci rownej liczbie przypisan
		int assignmentsCount = (this.mConfig.mOnlyFirst)?queries.length:DbEngineScoring.getAssignmentsCount(queries);
		double scores[] = new double[assignmentsCount];
		Object labels[] = new Object[assignmentsCount];
		MathFun.set(scores,Double.NEGATIVE_INFINITY);

		PrintStream consoleStream = System.out;
        if (this.mConfig.mSaveDataset) {
            try
            {
                PrintStream fileStream = new PrintStream(new File(".//data//full_data.txt"));
                System.setOut(fileStream);
            }
            catch (Exception e) {}
            System.out.println("sequence\tpos_neg\tmass\tmass_error\tcharge\tMS_intensity\tMascot_score\tMIT\tMHT\tMascot_delta_score\tlength\tmissed_cleavages");
        }

		//klasyfikacja wszystkich przypisan (poza tymi, ktore nie maja sekwencji)
		int counter = 0;
		for (MsMsQuery query:queries)
		{
			for (MsMsAssignment assignment:query.getAssignmentsList())
			{
				if (!assignment.isNA())	
				{
					BasicInstance instance=new BasicInstance(this.getValues(query,assignment),0);

                    if (this.mConfig.mSaveDataset) {
                        StringBuilder str = new StringBuilder();
                        str.append(assignment.getSequence());
                        str.append("\t");
                        str.append(assignment.getDecoy());
                        for (double value : instance.allFeaturesValues()) {
                            str.append("\t");
                            str.append(value);
                        }
                        System.out.println(str);
                    }

					svm.transform(instance);
					InstanceResult result=svm.classify(instance);
					//if (svm.getSVMParameters();
					scores[counter] = result.getValue();
					labels[counter] = result.getLabel();
				}
				counter++;
			}
		}
        System.setOut(consoleStream);
		return(scores);
	}
	
	/**
	 * Metoda tworzaca zbior treningowy (zawiera wszystkie wszystkie z decoy oraz te z target, ktore maja q-wartosci <= od progu)
	 * @param queries
	 * @return
	 */
	private BasicDataset createTrainDataset(MsMsQuery queries[])
	{
		BasicDataset dataset=new BasicDataset();

        PrintStream consoleStream = System.out;
        if (this.mConfig.mSaveTrainDataset) {
            try
            {
                String filename = ".//data//train_data_" + this.mConfig.mQValueThreshold + ".txt";
                PrintStream fileStream = new PrintStream(new File(filename));
                System.setOut(fileStream);
            }
            catch (Exception e) {}
            System.out.println("sequence\tpos_neg\tmass\tmass_error\tcharge\tMS_intensity\tMascot_score\tMIT\tMHT\tMascot_delta_score\tlength\tmissed_cleavages");
        }

		//dla kazdego zapytania
		for (MsMsQuery query:queries)
		{
			//dla kazdego przypisania do widma z danego zapytania
			for (MsMsAssignment assignment:query.getAssignmentsList())
			{
				//sprawdzenie czy do zapytania zostala przypisana sekwencja
				if (!assignment.isNA())
				{
					//pobranie informacji z jakiej bazy pochodzi sekwencja
					int label=assignment.getDecoy();
	
					//sprawdzenie warunkow przynaleznosci do zbioru treningowego (z bazy decoy, albo z target z q <= od progu)
					if (label==FDRTools.IS_DECOY || (label==FDRTools.IS_TARGET && assignment.getQValue()<=this.mConfig.mQValueThreshold))
					{
						BasicInstance instance=new BasicInstance(assignment.getSequence(), this.getValues(query,assignment),(label==FDRTools.IS_DECOY)?-1:1);
						dataset.add(instance);

                        if (this.mConfig.mSaveTrainDataset) {
                            StringBuilder str = new StringBuilder();
                            str.append(assignment.getSequence());
                            str.append("\t");
                            str.append(assignment.getDecoy());
                            for (double value : instance.allFeaturesValues()) {
                                str.append("\t");
                                str.append(value);
                            }
                            System.out.println(str);
                        }
					}
				}
			}
		}
        System.setOut(consoleStream);
		return(dataset);
	}
	
	/**
	 * Metoda liczaca cechy na podstawie zapytania i przypisania
	 * @param query
	 * @param assignment
	 * @return
	 */
	private double[] getValues(MsMsQuery query, MsMsAssignment assignment)
	{
		ArrayList<Double> valuesList=new ArrayList<>();
				
		valuesList.add(query.getMass());												//Masa (mass)
		valuesList.add(MassTools.getDelta(query.getMass(),assignment.getCalcMass()));	//Blad masy w Da (mass_error)
		valuesList.add((double)query.getCharge());										//Stopien naladowanie (charge)
		valuesList.add(query.getPrecursorIntensity());									//Wysokosc piku prekursowa (MS_intensity)
		valuesList.add(assignment.getScore());											//Score Mascota (Mascot_score)	
		valuesList.add(query.getMIT());													//MIT  (MIT)
		valuesList.add(query.getMHT());													//MHT  (MHT)
		valuesList.add(assignment.getScoreDelta());										//Score Delta Mascota (Mascot_delta_score)		
		valuesList.add((double)assignment.getSequenceLength());							//Dlugosc sekwencji (length)
		valuesList.add((double)assignment.getMissedCleavagesCount());					//Lisczba niedotrawek (missed_cleavages)
		
		double values[]=new double[valuesList.size()];
		for (int i=0;i<values.length;i++)
			values[i]=valuesList.get(i);
		
		return(values);
	}	
}
