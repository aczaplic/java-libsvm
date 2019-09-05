import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

import classification.*;
import dataset.*;
import mscanlib.math.MathFun;
import mscanlib.ms.exp.*;
import mscanlib.ms.fdr.FDRConfig;
import mscanlib.ms.fdr.FDRTools;
import mscanlib.ms.mass.AminoAcidSequence;
import mscanlib.ms.mass.Enzyme;
import mscanlib.ms.mass.MassTools;
import mscanlib.ms.msms.*;
import mscanlib.ms.msms.dbengines.DbEngineScoring;
import mscanlib.ms.msms.dbengines.mscandb.MScanDbScoring;
import mscanlib.ms.msms.dbengines.xtandem.XTandemScoring;
import mscanlib.ms.rt.RTTools;
import mscanlib.ui.*;
import mscanlib.ui.threads.*;
import org.apache.commons.lang3.ArrayUtils;

import static mscanlib.math.MathFun.sum;

public final class FdrSvmWorker extends MScanWorker
{	
	/**
	 * Konfiguracja
	 */
	private FdrSvmConfig mConfig;
	
	/**
	 * Tablica probek
	 */
	private Sample mSamples[];
	
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
	private List<double[][]> mQValues = null;
	private List<double[][]> mQValuesSVM = null;
	
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
		MsMsQuery queries[];
		double qValues[][];
		
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
                    if (query != null)
                    {
                        //dla kazdego przypisania z danego zapytania
                        for (MsMsAssignment assignment : query.getAssignmentsList())
                        {
                            if (assignment.getDecoy() == FDRTools.IS_TARGET)
                            {
                                if (assignment.getQValue() < this.mConfig.getmQValueThreshold())
                                    qPos++;
                                for (int n = thresholds.length - 1; n >= 0; n--)
                                {
                                    if (assignment.getQValue() < thresholds[n])
                                        nrPositive[n]++;
                                    else break;
                                }
                            }

                        }
                    }
                }

                System.out.println("\nBEFORE POST-PROCESSING\nQueries with q-values <");
                System.out.println(this.mConfig.getmQValueThreshold() + "(user defined threshold): " + qPos);
                for (int n = 0; n < thresholds.length; n++)
                    System.out.println(thresholds[n] + ": " + nrPositive[n]);
			
				//liczenie nowych q-wartosci z uzyciem SVM
				if (this.mConfig.ismComputeSVM())
				{
					this.selfBoostingSVM(sample, queries);
				}
			}
		}
	}

    /**
     * Metoda iteracyjnego poprawiania zbioru przykładów pozytywnych
     *
     * @param sample
     * @param queries
     */
    private void selfBoostingSVM(Sample sample, MsMsQuery queries[])
    {
        double qValuesSVM[][] = new double[0][];
        for (int iter = 0; iter<this.mConfig.getmBoostIter(); iter++)
        {
            System.out.println("-----------------------------------------------------------------------");
            System.out.println("Boosting iteration : " + (iter+1) + "/" + this.mConfig.getmBoostIter());
            //podział wszystkich queries w próbce na fold'y
            int numFolds = this.mConfig.getmCVFolds();
            int[][] indOfQueries = new int[numFolds][];
            MsMsQuery[][] foldQueries = this.foldsQueries(queries, indOfQueries);
            double[][] Scores = new double[3][];
            int numQueries = 0;
            for (int n = 0; n < numFolds; n++)
                numQueries += foldQueries[n].length;

            for (int n = 0; n < this.mConfig.getmCVFolds(); n++) {
                //podział queries na służace do zbudowania modelu i klasyfikowane przy jego użyciu
                MsMsQuery[] classifyingQueries = foldQueries[n];
                MsMsQuery[] trainQueries = new MsMsQuery[numQueries - classifyingQueries.length];
                int start_ind = 0;
                for (int j = 0; j < numFolds; j++) {
                    if (j != n) {
                        int size_copied = foldQueries[j].length;
                        System.arraycopy(foldQueries[j], 0, trainQueries, start_ind, size_copied);
                        start_ind += size_copied;
                    }
                }

                //liczenie score dla każdego fold'u
                System.out.println("-----------------------------------------------------------------------");
                System.out.println("Model number: " + (n+1));
                double foldScores[] = this.computeSvmScores(sample, classifyingQueries, trainQueries, iter, n);
                Scores[n] = foldScores;
            }

            //przeskalowanie wartości do porównywalnego SVM score
            for (int n = 0; n < this.mConfig.getmCVFolds(); n++)
            {
                double[] SortedScores = new double[Scores[n].length];
                System.arraycopy(Scores[n], 0, SortedScores, 0, Scores[n].length);
                double foldQValues[][] = FDRTools.computeQValues(foldQueries[n],SortedScores,this.mConfig,this,0);

                double[] qValuesCopy = new double[Scores[n].length];
                System.arraycopy(foldQValues[3], 0, qValuesCopy, 0, Scores[n].length);
                for (int j = 0; j < qValuesCopy.length; j++)
                    qValuesCopy[j] = Math.abs(qValuesCopy[j] - 0.01);
                List<Double> scoresCopy = Arrays.asList(ArrayUtils.toObject(foldQValues[5]));
                ArrayList<Double> sortedScores_001 = new ArrayList<>(scoresCopy);
                sortedScores_001.sort(Comparator.comparing(s -> qValuesCopy[scoresCopy.indexOf(s)]));
                Arrays.sort(qValuesCopy);
                int ind = 1;
                double SVMscore0 = sortedScores_001.get(0);
                while (qValuesCopy[ind] == qValuesCopy[0]){
                    SVMscore0 += sortedScores_001.get(ind);
                    ind++;
                }
                SVMscore0 = SVMscore0/ind;

                List<Double> decoyScoresList = new ArrayList<>();
                for (int j = 0; j < Scores[n].length; j++)
                    if (foldQValues[4][j] == FDRTools.IS_DECOY)
                        decoyScoresList.add(foldQValues[5][j]);
                double[] decoyScores = new double[decoyScoresList.size()];
                for (int j = 0; j < decoyScores.length; j++)
                    decoyScores[j] = decoyScoresList.get(j);
                Arrays.sort(decoyScores);
                double SVMscore_1;
                if (decoyScores.length % 2 == 0)
                    SVMscore_1 = (decoyScores[decoyScores.length/2] + decoyScores[decoyScores.length/2 - 1])/2;
                else
                    SVMscore_1 = decoyScores[decoyScores.length/2];

                //skalowanie
                for (int j = 0; j < Scores[n].length; j++)
                    Scores[n][j] = (Scores[n][j]-SVMscore_1)/(SVMscore0-SVMscore_1)-1;
            }

            //połączenie 3 list SVM score w jedną odpowiadającą orginalnej tablicy queries
            int numPSM = 0;
            for (int n = 0; n < numFolds; n++)
                numPSM += Scores[n].length;
            double[] SVMScores = new double[numPSM];
            for (int n = 0; n < this.mConfig.getmCVFolds(); n++)
            {
                MsMsQuery[] classifyingQueries = foldQueries[n];
                int q = 0;
                int a = 0;
                for (MsMsQuery query: classifyingQueries)
                {
                    int queryNrAssignments = query.getAssignmentsCount();
                    int startInd = indOfQueries[n][q];
                    for (int ind = 0; ind < queryNrAssignments; ind++)
                    {
                        SVMScores[startInd+ind] = Scores[n][a];
                        a++;
                    }
                    q++;
                }
            }

            //liczenie q-wartosci na podstawie score SVM
            qValuesSVM = FDRTools.computeQValues(queries,SVMScores,this.mConfig,this,0);
            //System.out.println(Arrays.toString(qValuesSVM[FDRTools.ROW_QVALUE]));

            //przypisanie q-wartosci do próbki białkowej i zapamietanie ich w systemie
            FDRTools.setQValues(sample,qValuesSVM);
            //this.mQValuesSVM.add(qValuesSVM);
        }
        this.mQValuesSVM.add(qValuesSVM);
    }

    /**
     * Metoda podziału zapytań na fold'y do zwalczania przeuczenia
     *
     * @param queries
     */
    private MsMsQuery[][] foldsQueries(MsMsQuery queries[], int indOfQueries[][])
    {
        Vector<MsMsQuery> queriesVector = new Vector<>();
        ArrayList<Integer> nrOfAssignments = new ArrayList<>();
        nrOfAssignments.add(0);
        for (MsMsQuery query:queries)
        {
            queriesVector.add(new MsMsQuery(query));
            nrOfAssignments.add(nrOfAssignments.get(nrOfAssignments.size()-1)+query.getAssignmentsCount());
        }
        nrOfAssignments.remove(nrOfAssignments.size()-1);

        Random rg = new Random(111);
        int numFolds = this.mConfig.getmCVFolds();
        int maxFoldSize = (int) Math.ceil((double)queriesVector.size()/numFolds);
        MsMsQuery[][] queriesFolds = new MsMsQuery[numFolds][maxFoldSize];
        for (int j = 0; j < numFolds; j++)
            indOfQueries[j] = new int[maxFoldSize];
        for (int i = 0; i < maxFoldSize; i++) {
            for (int j = 0; j < numFolds; j++) {
                if (queriesVector.size() > 0) {
                    int randomInd = rg.nextInt(queriesVector.size());
                    queriesFolds[j][i] = queriesVector.remove(randomInd);
                    indOfQueries[j][i] = nrOfAssignments.remove(randomInd);
                }
                else {
                    MsMsQuery[] tmpQueries = new MsMsQuery[maxFoldSize - 1];
                    System.arraycopy(queriesFolds[j], 0, tmpQueries, 0, maxFoldSize - 1);
                    queriesFolds[j] = tmpQueries;
                    int[] tmpIndexes = new int[maxFoldSize - 1];
                    System.arraycopy(indOfQueries[j], 0, tmpIndexes, 0, maxFoldSize - 1);
                    indOfQueries[j] = tmpIndexes;
                }
            }
        }
        return queriesFolds;
    }
	
	/**
	 * Metoda liczaca score SVM  
	 * 
	 * @param buildingQueries
	 * @return
	 */
	private double[] computeSvmScores(Sample sample, MsMsQuery classifyingQueries[], MsMsQuery buildingQueries[], int iteration, int fold_number)
	{
		/*
		 *  Budowanie modelu
		 */
		
		//Tworzenie zbioru treningowego (wszystkie decoy i target o q-wartosciach <= od progu)
        Vector<MsMsQuery> tQueries = new Vector<>();
		BasicDataset trainDataset = this.createTrainDataset(sample, buildingQueries, tQueries);
        if (this.mConfig.ismSaveTrainDataset()) {
            String filename = ".//out//train_data_iter_" + (iteration+1) + "_fold_" + (fold_number+1) + ".txt";
            try {
                DatasetTools.saveData(filename, trainDataset);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

		double[][] min_max = DatasetTools.normalizeMinMax(trainDataset);

		MsMsQuery trainQueries[] = new MsMsQuery[tQueries.size()];
		for(int i = 0; i < tQueries.size(); ++i)
		    trainQueries[i] = tQueries.get(i);

		//Optymalizacja parametrow modelu
        double C, Cneg_pos, gamma;
        if (this.mConfig.getmCVFolds() < 2)
            this.mConfig.setmOptimize(false);
        if (this.mConfig.ismOptimize())
        {
            int maxAt = optimParameters(trainDataset, trainQueries, min_max, fold_number);
            double[] gammaOptim;
            if (this.mConfig.getmKernel() == 1)
                gammaOptim = new double[]{1};
            else gammaOptim = this.mConfig.getmGamma();
            C = this.mConfig.getmC()[(maxAt % (this.mConfig.getmC().length * this.mConfig.getmCneg_pos().length)) / this.mConfig.getmCneg_pos().length];
            Cneg_pos = this.mConfig.getmCneg_pos()[maxAt % this.mConfig.getmCneg_pos().length];
            gamma = gammaOptim[maxAt / (this.mConfig.getmC().length * this.mConfig.getmCneg_pos().length)];
        }
        else
        {
            C = 10;
            Cneg_pos = 3;
            gamma = 10;
        }

        //Trenowanie klasyfikatora
		SVMClassifier svm = new SVMClassifier();
        svm.addDataTransformation(DatasetTools.NORM_MIN_MAX, min_max);
        svm.setSVMParameters(this.mConfig.getmKernel(), gamma, C, new int[]{1, -1}, new double[]{1, Cneg_pos}, this.mConfig.getmProbabilityCount());
		svm.buildClassifier(trainDataset);

		//Sprawdzenie na zbiorze treningowym
        DatasetResult train_results = svm.classify(trainDataset);
        int sum_pos_train = 0;
        for (Object pred_label: train_results.getLabels())
            if (pred_label.equals(1.0))
                sum_pos_train ++;

        System.out.println("\n\n" + sum_pos_train + " / " + train_results.size());
        System.out.println("error: " + ClassificationTools.evaluateError(trainDataset, train_results.getLabels()));

//        try
//        {
//            svm.saveModel("test");
//            svm.loadModel("test");
//        }
//        catch (IOException e)
//        {
//            e.printStackTrace();
//        }

        /*
		 * Tworzenie tablicy wartosci score do liczenia q-wartosci
		 */

		//alokacja tablicy o dlugosci rownej liczbie przypisan
		int assignmentsCount = (this.mConfig.mOnlyFirst)?classifyingQueries.length:DbEngineScoring.getAssignmentsCount(classifyingQueries);
		double scores[] = new double[assignmentsCount];
//		Object labels[] = new Object[assignmentsCount];
		MathFun.set(scores,Double.NEGATIVE_INFINITY);
		Collection<Instance> instancesColl = new ArrayList<>();

		//klasyfikacja wszystkich przypisan (poza tymi, ktore nie maja sekwencji)
		int counter = 0;
        int sum_pos = 0;
        double err = 0;
		for (MsMsQuery query:classifyingQueries)
		{
			for (MsMsAssignment assignment:query.getAssignmentsList())
			{
				if (!assignment.isNA())	
				{
					int label=assignment.getDecoy();
                    ArrayList<String> featuresNames = new ArrayList<>();
					BasicInstance instance = new BasicInstance(this.getValues(sample,query,assignment,featuresNames),(label==FDRTools.IS_DECOY)?-1:1);
					instancesColl.add(instance);

					svm.transform(instance);
					InstanceResult result = svm.classify(instance);
                    scores[counter] = result.getScore(0); //prawdopodobieństwo klasy pozytywnej lub wartość
//					labels[counter] = result.getLabel();
                    if (result.getLabel().equals(1.0)) sum_pos ++;
                    if (!result.getLabel().equals(Double.parseDouble(instance.getClassValue().toString()))) err++;
				}
				counter++;
			}
		}

        BasicDataset classifyDataset = new BasicDataset(instancesColl, trainDataset.getFeaturesNames());
		DatasetResult results = svm.classify(classifyDataset);
        err = ClassificationTools.evaluateError(classifyDataset, results.getLabels());

		System.out.println(sum_pos + " / " + scores.length);
        System.out.println("error: " + err + "\n");

		if (this.mConfig.ismSaveDataset() && iteration == 0)
        {
            try {
                DatasetTools.saveData(".//out//classify_data_iter_" + (iteration+1) + "_fold_" + (fold_number+1) + ".txt", classifyDataset);
                svm.saveModel("classify_iter_" + (iteration+1) + "_fold_" + (fold_number+1));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
		return scores;
	}
	
	/**
	 * Metoda tworzaca zbior treningowy (zawiera wszystkie wszystkie z decoy oraz te z target, ktore maja q-wartosci <= od progu)
	 * @param queries
	 * @return
	 */
	private BasicDataset createTrainDataset(Sample sample, MsMsQuery queries[], Vector<MsMsQuery> trainQueries)
	{
		BasicDataset dataset = new BasicDataset();
        ArrayList<String> featuresNames = new ArrayList<>();

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
					if (label==FDRTools.IS_DECOY || (label==FDRTools.IS_TARGET && assignment.getQValue()<=this.mConfig.getmQValueThreshold()))
					{
					    assignmentsTrain[i] = 1;
					    featuresNames = new ArrayList<>();
						BasicInstance instance = new BasicInstance(assignment.getSequence(), this.getValues(sample,query,assignment,featuresNames),(label==FDRTools.IS_DECOY)?-1:1);
						dataset.add(instance);
					}
				}
				i++;
			}
			if (sum(assignmentsTrain) > 0)
            {
                MsMsQuery tmpQuery = new MsMsQuery(query);
                for (i = assignmentsTrain.length-1; i >= 0; i--)
                    if (assignmentsTrain[i] == 0)
                        tmpQuery.removeAssignment(i);
                trainQueries.add(tmpQuery);
            }
		}
        dataset.setFeaturesNames(featuresNames);
		return dataset;
	}

    /**
     * Metoda optymalizyjaca parametry C i gamma modelu SVM z uzyciem walidacji krzyzowej
     * wedlug kryterium liczby pozytywnych identyfikacji ponizej zadanego progu q-wartosci
     * @param trainDataset
     * @param trainQueries
     * @param min_max
     * @return
     */
	private int optimParameters(Dataset trainDataset, MsMsQuery[] trainQueries, double[][] min_max, int fold_number)
    {
        double[] gammaOptim;
        if (this.mConfig.getmKernel() == 0)
            gammaOptim = new double[]{1};
        else gammaOptim = this.mConfig.getmGamma();

        int numPos[] = new int[gammaOptim.length*this.mConfig.getmC().length*this.mConfig.getmCneg_pos().length];

        if (this.mConfig.getmCVFolds() == 1) this.mConfig.setmOptimizeIter(1);
        for (int iter = 0; iter < this.mConfig.getmOptimizeIter(); iter++) {
            int n = 0;
            for (double gamma : gammaOptim)
            {
                for (double C : this.mConfig.getmC())
                {
                    for (double Cneg_pos : this.mConfig.getmCneg_pos())
                    {
                        if (this.mConfig.getmKernel() == 2)
                            System.out.println("\nSVM parameters: gamma = " + gamma + ", C = " + C + ", Cneg_pos = " + Cneg_pos);
                        else
                            System.out.println("\nSVM parameters: C = " + C + ", Cneg_pos = " + Cneg_pos);
                        List<Classifier> models = new ArrayList<>();
                        for (int i = 0; i < this.mConfig.getmCVFolds(); i++)
                        {
                            SVMClassifier svm = new SVMClassifier();
                            svm.addDataTransformation(DatasetTools.NORM_MIN_MAX, min_max);
                            int[] weight_labels = {1, -1};
                            double[] weight = {1, Cneg_pos};
                            svm.setSVMParameters(this.mConfig.getmKernel(), gamma, C, weight_labels, weight, this.mConfig.getmProbabilityCount());
                            models.add(svm);
                        }

                        CrossValidation cv = new CrossValidation(models);
                        DatasetResult results = cv.crossValidation(trainDataset, this.mConfig.getmCVFolds(), new Random(111*iter), true);
                        double scores[] = results.getScores(0);

                        //liczenie q-wartosci na podstawie score SVM
                        double qValuesSVM[][] = FDRTools.computeQValues(trainQueries, scores, this.mConfig, this, 0);
                        for (int i = 0; i < qValuesSVM[0].length; i++)
                            if (qValuesSVM[3][i] < this.mConfig.getmQValueOptimization() && qValuesSVM[4][i] == FDRTools.IS_TARGET)
                                numPos[n]++;


//                        // TODO -------------- na rzecz testow
//                        {
//                            PrintStream consoleStream = System.out;
//                            String filename = ".//out//model_" + (fold_number + 1) + "//table_q_values_gamma_ " + gamma + "_C_" + C + "_Cneg_pos_" + Cneg_pos + "_iter_CV_" + iter + ".txt";
//                            PrintStream fileStream = null;
//                            try {
//                                fileStream = new PrintStream(new File(filename));
//                            } catch (FileNotFoundException e) {
//                                e.printStackTrace();
//                            }
//                            System.setOut(fileStream);
//                            String[] names = {"Sample", "Query", "Assignment", "q-value", "decoy", "score"};
//                            StringBuilder str;
//                            int nr = 0;
//                            for (double[] qValue_row : qValuesSVM) {
//                                str = new StringBuilder();
//                                str.append(names[nr]);
//                                for (double value : qValue_row) {
//                                    str.append(value);
//                                    str.append("\t");
//                                }
//                                System.out.println(str);
//                                nr++;
//                            }
//                            System.setOut(consoleStream);
//                        }

                        n++;
                    }
                }
            }
        }
        for (int i = 0; i < numPos.length; i++)
        {
            numPos[i] /= this.mConfig.getmOptimizeIter();
        }

//        // TODO -------------- na rzecz testow
//        {
//            PrintStream consoleStream = System.out;
//            String filename = ".//out//model_" + (fold_number + 1) + "//num_pos_q" + this.mConfig.getmQValueOptimization() + ".txt";
//            PrintStream fileStream = null;
//            try {
//                fileStream = new PrintStream(new File(filename));
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
//            System.setOut(fileStream);
//            System.out.println(Arrays.toString(numPos));
//            System.setOut(consoleStream);
//        }

        //Wybor najlepszej kombinacji wartosci parametrow pog wzgledem liczby prawidlowych identyfikacji ponizej zadanego progu q-wartosci
        int maxAt = 0;
        for (int i = 0; i < numPos.length; i++)
            maxAt = numPos[i] > numPos[maxAt] ? i : maxAt;
        return maxAt;
    }
	
	/**
	 * Metoda liczaca cechy na podstawie zapytania i przypisania
	 * @param query
	 * @param assignment
	 * @return
	 */
	private double[] getValues(Sample sample,MsMsQuery query, MsMsAssignment assignment, ArrayList<String> featuresNames)
	{
		ArrayList<Double> valuesList=new ArrayList<>();

		valuesList.add(query.getMass());												//Masa (mass)
		featuresNames.add("mass");
        valuesList.add(MassTools.getDelta(query.getMass(),assignment.getCalcMass()));	//Blad masy w Da (mass_error)
        featuresNames.add("mass_error[Da]");
        valuesList.add((double)query.getCharge());										//Stopien naladowanie (charge)
        featuresNames.add("charge");
        valuesList.add(query.getPrecursorIntensity());									//Wysokosc piku prekursora (MS_intensity)
        featuresNames.add("MS_intensity");
        valuesList.add(assignment.getScore());											//Score Mascota (Mascot_score)
        featuresNames.add("Mascot_score");
        valuesList.add(query.getMIT());													//MIT  (MIT)
        featuresNames.add("MIT");
        valuesList.add(query.getMHT());													//MHT  (MHT)
        featuresNames.add("MHT");
        valuesList.add(assignment.getScoreDelta());										//Score Delta Mascota (Mascot_delta_score)
        featuresNames.add("Mascot_delta_score");
        valuesList.add((double)assignment.getSequenceLength());							//Dlugosc sekwencji (length)
        featuresNames.add("length");

		//Blad masy w PPM (Parts Per Million) - dla MS jest to lepsza miara bledu
		valuesList.add(MassTools.getDeltaPPM(query.getMass(),assignment.getCalcMass()));
        featuresNames.add("mass_error[PPM]");

		//Cechy liczone sa tylko jesli uzywany byl enzym proteolityczny
		Enzyme enzyme = sample.getHeader().getEnzyme();
		if (enzyme != null && !enzyme.getName().equals("None"))
		{
			//liczba liczba niedotrawek (missed_cleavages)
			valuesList.add((double)assignment.getMissedCleavagesCount());
            featuresNames.add("missed_cleavages");

			//specyficznosc N-konca (n-term_specificity) i C-konca (c-term_specificity) - liczone tylko gdy enzym jest semispecyficzny
			if (enzyme.isSemispecific())
			{
				valuesList.add(assignment.isNTermSpecific(enzyme,false)?1.0:0.0);
                featuresNames.add("N_term_specificity");
				valuesList.add(assignment.isCTermSpecific(enzyme,false)?1.0:0.0);
                featuresNames.add("C_term_specificity");
			}
		}

		//wzgledny czas retencji, odniesiony do maksymalnego czasu w probce
		double rtScale=RTTools.min2sec(sample.getRtRange(1));
		valuesList.add(query.getRtInSec()/rtScale);
        featuresNames.add("RT");

		//wzgledne odstepstwo od teoretycznego czasu retencji, odniesione do maksymalnego czasu w probce (RT_error)
		valuesList.add((assignment.getCalcRtInSec()-query.getRtInSec())/rtScale);
        featuresNames.add("RT_error");

		//score MScanDB i X!Tandem
		AminoAcidSequence sequence = new AminoAcidSequence(assignment.getSequence(true),assignment.getPTMsString(),sample.getHeader().getVariablePTMsMap(),sample.getHeader().getFixedPTMs());
		valuesList.add(MScanDbScoring.processSpectrumAndComputeScore(query.getSpectrum(),sequence,query.getCharge(),this.mConfig.getmScoreConfig()));
        featuresNames.add("MScanDb_score");
		valuesList.add(XTandemScoring.processSpectrumAndComputeScore(query.getSpectrum(),sequence,query.getMz(),query.getCharge(),this.mConfig.getmScoreConfig()));
        featuresNames.add("XTandem_score");

		//liczba widm, do ktorych przypisano ta sama sekwencje (psm_count)
		MsMsPeptideHit peptide = sample.getPeptide(assignment.getSequenceKey());
		valuesList.add((double)peptide.getQueriesCount());
        featuresNames.add("psm_count");

		//liczba zidentyfikowanych peptydow bialka, z ktorego pochodzi dana sekwencja (wartosc maksymalna jesli jest wiecej takich bialek)
		int pepCount = 0;
		for (String id:peptide.getProteins().keySet())
		{
			int n = sample.getProtein(id).getPeptidesCount();
            if (n > pepCount)
				pepCount = n;
		}
		valuesList.add((double)pepCount);
        featuresNames.add("peptides_count");

        //e-value
        valuesList.add(assignment.getEValue());
        featuresNames.add("e_value");

		double values[] = new double[valuesList.size()];
		for (int i = 0; i < values.length; i++)
			values[i] = valuesList.get(i);

		return values;
	}
}
