import java.io.File;
import java.io.PrintStream;

import mscanlib.common.MScanException;
import mscanlib.ms.exp.Sample;
import mscanlib.ms.fdr.FDRTools;
import mscanlib.ms.mass.MassTools;
import mscanlib.ms.msms.MsMsAssignment;
import mscanlib.ms.msms.MsMsProteinHit;
import mscanlib.ms.msms.MsMsQuery;
import mscanlib.ms.msms.dbengines.mascot.io.MascotDatFileReader;
import mscanlib.ms.msms.io.MsMsScanConfig;
import mscanlib.ui.interfaces.MScanWorkerListener;
import mscanlib.ui.threads.MScanWorker;

public class FdrSvmTest implements MScanWorkerListener
{
	private Sample			mSample=null;
	private String 			mFilename=null;
	private FdrSvmConfig mConfig=null;
	
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
			this.mConfig.mComputeSVM = true;			//liczenie q-wartosci na podstawie score SVM
			this.mConfig.mQValueThreshold = 0.2;		//prog q-wartosci dla zbioru treningowego
			
			this.mConfig.mPlotsMin = 0.0;				//zakresy q-wartosci dla wykresow
			this.mConfig.mPlotsMax = 0.2;

			this.mConfig.mSaveDataset = false;			//zapis zbiorów danych do plików
			this.mConfig.mSaveTrainDataset = true;
			
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
		if (this.mConfig.mComputeSVM)
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
		
		//dla kazdego zapytania 
		for (MsMsQuery query: this.mSample.getQueries())
		{
			if (query!=null)
			{
				//dla kazdego przypisania z danego zapytania
				for (MsMsAssignment assignment:query.getAssignmentsList())
				{
					StringBuilder str=new StringBuilder();
					
					str.append(query.getNr());	//nr. zapytania
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
						str.append(assignment.getScore());		//score
						str.append("\t");
						str.append(assignment.getScoreDelta());	//score delta
						str.append("\t");						
						str.append(query.getMIT());				//MIT
						str.append("\t");			
						str.append(query.getMHT());				//MHT
						str.append("\t");
						str.append(assignment.getEValue());		//e-wartosc
						str.append("\t");
						str.append(assignment.getQValue());		//q-wartosc
						
						//zliczanie przypisan o q-wartosci <= od progu
						if (assignment.getQValue()<this.mConfig.mQValueThreshold)
							qPos++;
					}
					else
						str.append("-\t-\t-\t-\t-\t-");
					
					System.out.println(str);
				}
			}
		}
		System.setOut(consoleStream);
		
		System.out.println("Queries with q<" + this.mConfig.mQValueThreshold + ": " + qPos);
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
		Sample 				sample=null;
		MsMsScanConfig		scanConfig=null;
		
		scanConfig=new MsMsScanConfig();
		scanConfig.mReadSpectra=true;
		
		try
		{
			System.out.print("Reading file: " + filename + "... ");
			MascotDatFileReader	fileReader=new MascotDatFileReader(filename,scanConfig);
			fileReader.readFile();
				
			sample=new Sample("Test sample");			
			sample.setQueries(fileReader.getQueries(),true);
			sample.setHeader(fileReader.getHeader());
			
			System.out.println(sample.getQueries().length + " queries.");
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
			new FdrSvmTest(".//data//K_0_4_F060457.dat");
	}
}
