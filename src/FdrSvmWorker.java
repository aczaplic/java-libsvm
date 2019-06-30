import java.io.File;
import java.io.PrintStream;
import java.util.*;

import classification.*;
import dataset.BasicDataset;
import dataset.BasicInstance;
import dataset.Dataset;
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

import static java.util.Arrays.sort;
import static mscanlib.math.MathFun.sum;

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

        for (Sample mSample : this.mSamples)
            this.computeQValues(mSample);
		
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

                //licznik przypisan o q-wartosci <= od progu
                int qPos=0;
                double[] thresholds = {0.01, 0.05, 0.1, 0.2};
                int[] nrPositive = new int[thresholds.length];
                for (MsMsQuery query: queries)
                {
                    if (query!=null) {
                        //dla kazdego przypisania z danego zapytania
                        for (MsMsAssignment assignment : query.getAssignmentsList()) {
                            if (assignment.getDecoy()==FDRTools.IS_TARGET) {
                                if (assignment.getQValue() < this.mConfig.mQValueThreshold)
                                    qPos++;
                                for (int n = thresholds.length - 1; n >= 0; n--) {
                                    if (assignment.getQValue() < thresholds[n])
                                        nrPositive[n]++;
                                    else break;
                                }
                            }

                        }
                    }
                }

                System.out.println("\nBEFORE POST-PROCESSING\nQueries with q-values <");
                System.out.println(this.mConfig.mQValueThreshold + "(user defined threshold): " + qPos);
                for (int n=0; n<thresholds.length; n++)
                    System.out.println(thresholds[n] + ": " + nrPositive[n]);
			
				//liczenie nowych q-wartosci z uzyciem SVM
				if (this.mConfig.mComputeSVM)
				{
					this.selfBoostingSVM(sample, queries);
				}
			}
		}
	}

    /**
     * Metoda iteracyjnego poprawiania zbioru przykładów pozytywnych
     *
     * @param queries
     */
    private void selfBoostingSVM(Sample sample, MsMsQuery queries[]) {
        for (int i = 0; i<this.mConfig.mBoostIter; i++) {
            //liczenie score SVM
            double scores[] = this.computeSvmScores(queries, i);
            //System.out.println(Arrays.toString(scores));

            //liczenie q-wartosci na podstawie score SVM
            double qValuesSVM[][] = FDRTools.computeQValues(queries,scores,this.mConfig,this,0);
            //System.out.println(Arrays.toString(qValuesSVM[FDRTools.ROW_QVALUE]));

            //zapamietanie q-wartosci
            FDRTools.setQValues(sample,qValuesSVM);
            this.mQValuesSVM.add(qValuesSVM);
        }
        //this.mQValuesSVM.add(qValuesSVM); ????
    }

	
	/**
	 * Metoda liczaca score SVM  
	 * 
	 * @param queries
	 * @return
	 */
	private double[] computeSvmScores(MsMsQuery queries[], int iteration)
	{
		/*
		 *  Budowanie modelu
		 */
		
		//Tworzenie zbioru treningowego (wszystkie decoy i target o q-wartosciach <= od progu)
        Vector<MsMsQuery> tQueries = new Vector<>();
		BasicDataset trainDataset = this.createTrainDataset(queries, iteration, tQueries);
		double[][] min_max = DatasetTools.normalizeMinMax(trainDataset);

		MsMsQuery trainQueries[] = new MsMsQuery[tQueries.size()];
		for(int i = 0; i < tQueries.size(); ++i)
		    trainQueries[i] = tQueries.get(i);

		//Optymalizacja parametrow modelu
        double C, Cneg_pos, gamma;
        if (this.mConfig.mOptimize)
        {
            int maxAt = optimParameters(trainDataset, trainQueries, min_max);
            double[] gammaOptim;
            if (this.mConfig.mKernel == 1)
                gammaOptim = new double[]{1};
            else gammaOptim = this.mConfig.mGamma;
            C = this.mConfig.mC[(maxAt % (this.mConfig.mC.length * this.mConfig.mCneg_pos.length)) / this.mConfig.mCneg_pos.length];
            Cneg_pos = this.mConfig.mCneg_pos[maxAt % this.mConfig.mCneg_pos.length];
            gamma = gammaOptim[maxAt / (this.mConfig.mC.length * this.mConfig.mCneg_pos.length)];
        }
        else
        {
            C = 10;
            Cneg_pos = 3;
            gamma = 10;
        }

        //Trenowanie klasyfikatora
		SVMClassifier svm = new SVMClassifier();
        svm.addDataTransformation(DatasetTools.NormMinMax, min_max);
        svm.setSVMParameters(this.mConfig.mKernel, gamma, C, new int[]{1, -1}, new double[]{1, Cneg_pos});
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
					InstanceResult result = svm.classify(instance);
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
	private BasicDataset createTrainDataset(MsMsQuery queries[], int iteration, Vector<MsMsQuery> trainQueries)
	{
		BasicDataset dataset = new BasicDataset();

        PrintStream consoleStream = System.out;
        if (this.mConfig.mSaveTrainDataset) {
            try
            {
                String filename = ".//data//train_data_" + this.mConfig.mQValueThreshold + "_iter_" + (iteration+1) + ".txt";
                PrintStream fileStream = new PrintStream(new File(filename));
                System.setOut(fileStream);
            }
            catch (Exception e) {}
            System.out.println("sequence\tpos_neg\tmass\tmass_error\tcharge\tMS_intensity\tMascot_score\tMIT\tMHT\tMascot_delta_score\tlength\tmissed_cleavages");
        }

		//dla kazdego zapytania
		for (MsMsQuery query:queries)
		{
		    int assignmentsTrain[] = new int[query.getAssignmentsCount()];
		    int i = 0;
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
					    assignmentsTrain[i] = 1;
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
				i++;
			}
			if (sum(assignmentsTrain)>0)
            {
                MsMsQuery tmpQuery = new MsMsQuery(query);
                for (i=assignmentsTrain.length-1; i>=0; i--)
                    if (assignmentsTrain[i]==0)
                        tmpQuery.removeAssignment(i);
                trainQueries.add(tmpQuery);
            }
		}
        System.setOut(consoleStream);
		return(dataset);
	}

    /**
     * Metoda optymalizyjaca parametry C i gamma modelu SVM z uzyciem walidacji krzyzowej
     * wedlug kryterium liczby pozytywnych identyfikacji ponizej zadanego progu q-wartosci
     * @param trainDataset
     * @param trainQueries
     * @param min_max
     * @return
     */
	private int optimParameters(Dataset trainDataset, MsMsQuery[] trainQueries, double[][] min_max)
    {
        double[] gammaOptim;
        if (this.mConfig.mKernel == 1)
            gammaOptim = new double[]{1};
        else gammaOptim = this.mConfig.mGamma;

        int numPos[] = new int[gammaOptim.length*this.mConfig.mC.length*this.mConfig.mCneg_pos.length];

        if (this.mConfig.mCVFolds == 1) this.mConfig.mOptimizeIter = 1;
        for (int iter = 0; iter < this.mConfig.mOptimizeIter; iter++) {
            int n = 0;
            for (double gamma : gammaOptim) {
                for (double C : this.mConfig.mC) {
                    for (double Cneg_pos : this.mConfig.mCneg_pos) {
                        List<Classifier> models = new ArrayList<>();
                        for (int i = 0; i < this.mConfig.mCVFolds; i++) {
                            SVMClassifier svm = new SVMClassifier();
                            svm.addDataTransformation(DatasetTools.NormMinMax, min_max);
                            int[] weight_labels = {1, -1};
                            double[] weight = {1, Cneg_pos};
                            svm.setSVMParameters(this.mConfig.mKernel, gamma, C, weight_labels, weight);
                            models.add(svm);
                        }

                        CrossValidation cv = new CrossValidation(models);
                        DatasetResult results = cv.crossValidation(trainDataset, this.mConfig.mCVFolds, new Random(111*iter), true);
                        double scores[] = results.getValues();

                        //liczenie q-wartosci na podstawie score SVM
                        double qValuesSVM[][] = FDRTools.computeQValues(trainQueries, scores, this.mConfig, this, 0);
                        for (int i = 0; i < qValuesSVM[0].length; i++)
                            if (qValuesSVM[3][i] < this.mConfig.mQValueOptimization && qValuesSVM[3][i] == FDRTools.IS_TARGET)
                                numPos[n]++;

                        n++;
                    }
                }
            }
        }
        for (int i = 0; i < numPos.length; i++) {
            numPos[i] /= this.mConfig.mOptimizeIter;
        }

        //Wybor najlepszej kombinacji wartosci parametrow pog wzgledem liczby prawidlowych identyfikacji ponizej zadanego progu q-wartosci
        int maxAt = 0;
        for (int i = 0; i < numPos.length; i++) {
            maxAt = numPos[i] > numPos[maxAt] ? i : maxAt;
        }
        return  maxAt;
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
