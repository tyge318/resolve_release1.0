package wordnet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;
import Jama.Matrix;
import Jama.EigenvalueDecomposition;


public class PatternStats {
//	public static MaxentTagger tagger;
	public static TreeMap<String, synObject> table;
	public static TreeMap<String, synObject> wtable;
	public static TreeMap<String, Double> zeroPatterns;
//	public static ArrayList<synResult> resultsBackup = new ArrayList<synResult>();
	public static void main(String[] args) throws IOException {
//		generalization("emoPatternlst_L1_ICF_0217.txt", "emoPatternlst_L1_ICF_0224.txt");
//		/*
//		initialize_new("emoPatternlst.txt");
		initialize_new("mergedEmoPatternlist0219.txt");
//		applyWeighting("emoPatternlst.txt", "emoPatternlst_weighted.txt");
		try{ 
			 int serverPort = 3456; //Integer.parseInt(args[0]); 
			 @SuppressWarnings("resource")
			ServerSocket lookUpServer = new ServerSocket(serverPort); 
			 System.out.println("LookUp Process running...");
			 while (true){ 
				 Socket clientSocket = lookUpServer.accept( ); 
//				 lookUpThread thread = new lookUpThread(clientSocket); 
				 Thread newT = new Thread(new PatternLookUPThread(clientSocket));
				 newT.start( ); 
			 } 		 
		} 
		catch(Exception e){e.printStackTrace( );}
//		*/
	}
	public static void buildZeroPatterns(String filePath) throws IOException {
		zeroPatterns = new TreeMap<String, Double>();
		BufferedReader br;
		br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"));
		String line;
		while( (line = br.readLine()) != null) {
			if(line.length()<1) continue;
			String[] tokens = line.split("\t");
			Double value = Double.valueOf(tokens[1]);
			if( !zeroPatterns.containsKey(tokens[0]))
				zeroPatterns.put(tokens[0], value);
		}
		br.close();
	}
	public static HashMap<String, Double> localTest(ArrayList<String> inputClausePatterns, ArrayList<String> synonyms) throws IOException {
		initialize_new("emoPatternlst.txt");
		ArrayList<synResult> results = new ArrayList<synResult>();
		for(int i=0; i<synonyms.size(); i++) {
			if(table.get(synonyms.get(i)) == null) continue;	//go through every synonyms and skip those not supported.
			synObject currentSyn = table.get(synonyms.get(i));	//synObject holds static data.
//			TreeMap<String, ArrayList<patternEntry>> currentPatternCounts = currentSyn.getAllMatchedClausePatterns(inputClausePatterns, false);
			ArrayList<matchedPatternEntry> currentPatternCounts = currentSyn.getAllMatchedClausePatterns(inputClausePatterns, false);
			//get the "user-pattern to synonym-matched patterns" table. 
			synResult currentResult = new synResult(currentSyn.getEmoWord(), currentSyn.getPercentage(), currentSyn.getPatternTable(), currentPatternCounts);	//synResult holds run-time/dynamic data.
			//setup/initialize run-time object with known information from static data.
			results.add(currentResult);
		}
		
		ArrayList<mergedList> patternMerged = getMergedPatternList(results, inputClausePatterns);
		//pattern alignment issue: each user pattern might match to various number of existing patterns under different synonyms. This is to get a maximum possible matching table.
		results = adjustResults(results, patternMerged, false);
		//adjusting for pattern alignment.
		patternMerged = null;
		System.gc();
//		for(int i=0; i<results.size(); i++) {
//			System.out.println( results.get(i).synonymWord + ": " +  results.get(i).getTable());
//		}
		getProbability(results);
		HashMap<String, Double> scores = getSynScores(results);
		return scores;
	}
	public static HashMap<String, Double> localTestSettingSpecifyDebug(ArrayList<String> inputClausePatterns, ArrayList<String> synonyms, String settingPath, boolean doRegex) throws IOException {
		initialize_new(settingPath);
//		buildZeroPatterns("unseen.txt");
		ArrayList<synResult> results = new ArrayList<synResult>();
		
		long startTime = System.currentTimeMillis();
		
		for(int i=0; i<synonyms.size(); i++) {
			if(table.get(synonyms.get(i)) == null) continue;	//go through every synonyms and skip those not supported.
			synObject currentSyn = table.get(synonyms.get(i));	//synObject holds static data.
//			TreeMap<String, ArrayList<patternEntry>> currentPatternCounts = currentSyn.getAllMatchedClausePatterns(inputClausePatterns, doRegex);
			ArrayList<matchedPatternEntry> currentPatternCounts = currentSyn.getAllMatchedClausePatterns(inputClausePatterns, doRegex);
			//get the "user-pattern to synonym-matched patterns" table. 
			synResult currentResult = new synResult(currentSyn.getEmoWord(), currentSyn.getPercentage(), currentSyn.getPatternTable() ,currentPatternCounts);	//synResult holds run-time/dynamic data.
			
			//setup/initialize run-time object with known information from static data.
			results.add(currentResult);
		}
		
		ArrayList<mergedList> patternMerged = getMergedPatternList(results, inputClausePatterns);
		//pattern alignment issue: each user pattern might match to various number of existing patterns under different synonyms. This is to get a maximum possible matching table.
		results = adjustResults(results, patternMerged, doRegex);
		//adjusting for pattern alignment.
//		patternMerged = null;
//		System.gc();

		getProbability(results);
		HashMap<String, Double> scores = getSynScores(results);
		
		ArrayList<Map.Entry<String, Double>> synonymScoresSort = new ArrayList<Map.Entry<String, Double>>(scores.size());
    	synonymScoresSort.addAll(scores.entrySet());
    	Collections.sort(synonymScoresSort, new ValueComparator());
		
//    	/*
    	HashMap<String, String> matchableList = getMatchablePatterns(results);
    	System.out.println("Input Patterns:");
    	for(int i=0; i<inputClausePatterns.size(); i++) {
    		if( matchableList.containsKey(inputClausePatterns.get(i)))
    			System.out.println(inputClausePatterns.get(i) + "(v)");
    		else
    			System.out.println(inputClausePatterns.get(i));    		
    	}
    		
    	System.out.println(synonymScoresSort);
    	
    	
    	BufferedReader tmp = new BufferedReader(new InputStreamReader(System.in));
    	System.out.println("1. Relevance Values\n2. Logged Probabilities");
    	int choice = Integer.valueOf(tmp.readLine()).intValue();
    	if( choice == 1 )
    		adjustResultsDebug(results);
    	else if( choice == 2)
    		probResultsDebug(results);
    	else;
//    		*/
		//compare the found patterns among synonym candidates.
    	
		return scores;
	}
	public static HashMap<String, Double> localTestSettingSpecify(ArrayList<String> inputClausePatterns, ArrayList<String> synonyms, String settingPath, boolean doRegex) throws IOException {
		initialize_new(settingPath);
//		buildZeroPatterns("unseen.txt");
		ArrayList<synResult> results = new ArrayList<synResult>();
		for(int i=0; i<synonyms.size(); i++) {
			if(table.get(synonyms.get(i)) == null) continue;	//go through every synonyms and skip those not supported.
			synObject currentSyn = table.get(synonyms.get(i));	//synObject holds static data.
//			TreeMap<String, ArrayList<patternEntry>> currentPatternCounts = currentSyn.getAllMatchedClausePatterns(inputClausePatterns, doRegex);
			ArrayList<matchedPatternEntry> currentPatternCounts = currentSyn.getAllMatchedClausePatterns(inputClausePatterns, doRegex);
			//get the "user-pattern to synonym-matched patterns" table. 
			synResult currentResult = new synResult(currentSyn.getEmoWord(), currentSyn.getPercentage(), currentSyn.getPatternTable() ,currentPatternCounts);	//synResult holds run-time/dynamic data.
			
			//setup/initialize run-time object with known information from static data.
			results.add(currentResult);
		}
		
		ArrayList<mergedList> patternMerged = getMergedPatternList(results, inputClausePatterns);
		//pattern alignment issue: each user pattern might match to various number of existing patterns under different synonyms. This is to get a maximum possible matching table.
		results = adjustResults(results, patternMerged, doRegex);
		//adjusting for pattern alignment.
		patternMerged = null;
		System.gc();
//		for(int i=0; i<results.size(); i++) {
//			System.out.println( results.get(i).synonymWord + ": " +  results.get(i).getTable());
//		}
		getProbability(results);
		HashMap<String, Double> scores = getSynScores(results);
		
//		adjustResultsDebug(results);
		return scores;
	}
	public static HashMap<String, Double> localTestSettingSpecify_reForm(ArrayList<String> inputClausePatterns, ArrayList<String> synonyms, String settingPath, String PeFile, boolean doRegex) throws IOException {
		initialize_new(settingPath);
		HashMap<String, Double>  pe = initialize_Pe(PeFile);
//		buildZeroPatterns("unseen.txt");
		ArrayList<synResult> results = new ArrayList<synResult>();
		for(int i=0; i<synonyms.size(); i++) {
			if(table.get(synonyms.get(i)) == null) continue;	//go through every synonyms and skip those not supported.
			synObject currentSyn = table.get(synonyms.get(i));	//synObject holds static data.
//			TreeMap<String, ArrayList<patternEntry>> currentPatternCounts = currentSyn.getAllMatchedClausePatterns(inputClausePatterns, doRegex);
			ArrayList<matchedPatternEntry> currentPatternCounts = currentSyn.getAllMatchedClausePatterns(inputClausePatterns, doRegex);
			//get the "user-pattern to synonym-matched patterns" table. 
			synResult currentResult = new synResult(currentSyn.getEmoWord(), currentSyn.getPercentage(), currentSyn.getPatternTable() ,currentPatternCounts);	//synResult holds run-time/dynamic data.
			
			//setup/initialize run-time object with known information from static data.
			results.add(currentResult);
		}
		
		ArrayList<mergedList> patternMerged = getMergedPatternList(results, inputClausePatterns);
		//pattern alignment issue: each user pattern might match to various number of existing patterns under different synonyms. This is to get a maximum possible matching table.
		results = adjustResults(results, patternMerged, doRegex);
		//adjusting for pattern alignment.
		patternMerged = null;
		System.gc();
//		for(int i=0; i<results.size(); i++) {
//			System.out.println( results.get(i).synonymWord + ": " +  results.get(i).getTable());
//		}
		getProbability(results);
		HashMap<String, Double> scores = getSynPeScores(results,pe,inputClausePatterns.size());
		
//		adjustResultsDebug(results);
		return scores;
	}
	public static HashMap<String, Double> localTestSettingSpecify_weight(ArrayList<String> inputClausePatterns, ArrayList<String> oriStr, ArrayList<String> synonyms, String settingPath, String weightFile, boolean doRegex) throws IOException {
		initialize_new(settingPath);
		initialize_weight(weightFile);
//		buildZeroPatterns("unseen.txt");
		ArrayList<synResult> results = new ArrayList<synResult>();
		for(int i=0; i<synonyms.size(); i++) {
			if(table.get(synonyms.get(i)) == null) continue;	//go through every synonyms and skip those not supported.
			synObject currentSyn = table.get(synonyms.get(i));	//synObject holds static data.
//			TreeMap<String, ArrayList<patternEntry>> currentPatternCounts = currentSyn.getAllMatchedClausePatterns(inputClausePatterns, doRegex);
			ArrayList<matchedPatternEntry> currentPatternCounts = currentSyn.getAllMatchedClausePatterns(inputClausePatterns, doRegex);
			//get the "user-pattern to synonym-matched patterns" table. 
			synResult currentResult = new synResult(currentSyn.getEmoWord(), currentSyn.getPercentage(), currentSyn.getPatternTable() ,currentPatternCounts);	//synResult holds run-time/dynamic data.
			
			//setup/initialize run-time object with known information from static data.
			results.add(currentResult);
		}
		
		ArrayList<mergedList> patternMerged = getMergedPatternList(results, inputClausePatterns);
		//pattern alignment issue: each user pattern might match to various number of existing patterns under different synonyms. This is to get a maximum possible matching table.
		results = adjustResults(results, patternMerged, doRegex);
		//adjusting for pattern alignment.
		patternMerged = null;
		System.gc();
//		for(int i=0; i<results.size(); i++) {
//			System.out.println( results.get(i).synonymWord + ": " +  results.get(i).getTable());
//		}
		getProbability(results);
		
//		HashMap<String, Double> scores = getSynScores(results);
		HashMap<String, Double> scores = getWeightSynScores(results, oriStr);
		
//		adjustResultsDebug(results);
		return scores;
	}
	public static HashMap<String, Double> localTestSettingSpecify_opp(String currentFile, ArrayList<String> inputClausePatterns, ArrayList<String> synonyms, String settingPath, boolean doRegex) throws IOException {
		initialize_new(settingPath);
		BufferedWriter bw = new BufferedWriter(new FileWriter("all240_pattern" + File.separator + currentFile));
//		buildZeroPatterns("unseen.txt");
		ArrayList<synResult> results = new ArrayList<synResult>();
		for(int i=0; i<synonyms.size(); i++) {
			if(table.get(synonyms.get(i)) == null) continue;	//go through every synonyms and skip those not supported.
			synObject currentSyn = table.get(synonyms.get(i));	//synObject holds static data.
//			TreeMap<String, ArrayList<patternEntry>> currentPatternCounts = currentSyn.getAllMatchedClausePatterns(inputClausePatterns, doRegex);
			ArrayList<matchedPatternEntry> currentPatternCounts = currentSyn.getAllMatchedClausePatterns(inputClausePatterns, doRegex);
			//get the "user-pattern to synonym-matched patterns" table. 
			synResult currentResult = new synResult(currentSyn.getEmoWord(), currentSyn.getPercentage(), currentSyn.getPatternTable() ,currentPatternCounts);	//synResult holds run-time/dynamic data.
			
			//setup/initialize run-time object with known information from static data.
			results.add(currentResult);
		}
		
		ArrayList<mergedList> patternMerged = getMergedPatternList(results, inputClausePatterns);
		//pattern alignment issue: each user pattern might match to various number of existing patterns under different synonyms. This is to get a maximum possible matching table.
		results = adjustResults(results, patternMerged, doRegex);
		//adjusting for pattern alignment.
		patternMerged = null;
		System.gc();
//		for(int i=0; i<results.size(); i++) {
//			System.out.println( results.get(i).synonymWord + ": " +  results.get(i).getTable());
//		}
		getProbability(results);
		HashMap<String, Double> scores = getSynScores(results);
		
		for(int i=0; i<results.size(); i++) {
			bw.write(results.get(i).getSynonymWord()+ "\t" + "0\n");
			ArrayList<matchedPatternProbability> tempTable = results.get(i).getPTable();
			for(int j=0; j<tempTable.size(); j++) {
				bw.write(tempTable.get(j).pattern + "\t" + tempTable.get(j).getValue().get(0).getValue() + "\n");
			}
		}
		bw.close();
//		System.out.format("%-15s ", (results.get(i).getSynonymWord() + ":")); 
//		System.out.print(results.get(i).getTable());
//		System.out.print(results.get(i).getPTable());
//		System.out.format(" Score = %5.5f   \n", results.get(i).getSynonymScore());
		
//		adjustResultsDebug(results);
		return scores;
	}
	public static HashMap<String, Double> localTestSettingSpecify_PEN(ArrayList<String> inputClausePatterns, ArrayList<String> synonyms, String settingPath, boolean doRegex) throws IOException {
		initialize_new(settingPath);
//		buildZeroPatterns("unseen.txt");
		ArrayList<synResult> results = new ArrayList<synResult>();
		for(int i=0; i<synonyms.size(); i++) {
			if(table.get(synonyms.get(i)) == null) continue;	//go through every synonyms and skip those not supported.
			synObject currentSyn = table.get(synonyms.get(i));	//synObject holds static data.
//			TreeMap<String, ArrayList<patternEntry>> currentPatternCounts = currentSyn.getAllMatchedClausePatterns(inputClausePatterns, doRegex);
			ArrayList<matchedPatternEntry> currentPatternCounts = currentSyn.getAllMatchedClausePatterns_PEN(inputClausePatterns, doRegex);
			//get the "user-pattern to synonym-matched patterns" table. 
			synResult currentResult = new synResult(currentSyn.getEmoWord(), currentSyn.getPercentage(), currentSyn.getPatternTable() ,currentPatternCounts);	//synResult holds run-time/dynamic data.
			
			//setup/initialize run-time object with known information from static data.
			results.add(currentResult);
		}
		
		ArrayList<mergedList> patternMerged = getMergedPatternList(results, inputClausePatterns);
		//pattern alignment issue: each user pattern might match to various number of existing patterns under different synonyms. This is to get a maximum possible matching table.
		results = adjustResults(results, patternMerged, doRegex);
		//adjusting for pattern alignment.
		patternMerged = null;
		System.gc();
//		for(int i=0; i<results.size(); i++) {
//			System.out.println( results.get(i).synonymWord + ": " +  results.get(i).getTable());
//		}
		getProbability(results);
		HashMap<String, Double> scores = getSynScores(results);
		
//		adjustResultsDebug(results);
		return scores;
	}
	public static HashMap<String, Double> localTestSettingSpecify_N_E(ArrayList<String> inputClausePatterns, ArrayList<String> synonyms, String settingPath, boolean doRegex,boolean useE, boolean useN) throws IOException {
		initialize_new(settingPath);
//		buildZeroPatterns("unseen.txt");
		ArrayList<synResult> results = new ArrayList<synResult>();
		for(int i=0; i<synonyms.size(); i++) {
			if(table.get(synonyms.get(i)) == null) continue;	//go through every synonyms and skip those not supported.
			synObject currentSyn = table.get(synonyms.get(i));	//synObject holds static data.
//			TreeMap<String, ArrayList<patternEntry>> currentPatternCounts = currentSyn.getAllMatchedClausePatterns(inputClausePatterns, doRegex);
			ArrayList<matchedPatternEntry> currentPatternCounts = currentSyn.getAllMatchedClausePatterns_N_E(inputClausePatterns, doRegex,useE,useN);
			//get the "user-pattern to synonym-matched patterns" table. 
			synResult currentResult = new synResult(currentSyn.getEmoWord(), currentSyn.getPercentage(), currentSyn.getPatternTable() ,currentPatternCounts);	//synResult holds run-time/dynamic data.
			
			//setup/initialize run-time object with known information from static data.
			results.add(currentResult);
		}
		
		ArrayList<mergedList> patternMerged = getMergedPatternList(results, inputClausePatterns);
		//pattern alignment issue: each user pattern might match to various number of existing patterns under different synonyms. This is to get a maximum possible matching table.
		results = adjustResults(results, patternMerged, doRegex);
		//adjusting for pattern alignment.
		patternMerged = null;
		System.gc();
//		for(int i=0; i<results.size(); i++) {
//			System.out.println( results.get(i).synonymWord + ": " +  results.get(i).getTable());
//		}
		getProbability(results);
		HashMap<String, Double> scores = getSynScores(results);
		
//		adjustResultsDebug(results);
		return scores;
	}
	public static HashMap<String, Double> localTestSettingSpecify_N_Eonly(ArrayList<String> inputClausePatterns, ArrayList<String> synonyms, String settingPath, boolean doRegex,boolean useE, boolean useN) throws IOException {
		initialize_new(settingPath);
//		buildZeroPatterns("unseen.txt");
		ArrayList<synResult> results = new ArrayList<synResult>();
		for(int i=0; i<synonyms.size(); i++) {
			if(table.get(synonyms.get(i)) == null) continue;	//go through every synonyms and skip those not supported.
			synObject currentSyn = table.get(synonyms.get(i));	//synObject holds static data.
//			TreeMap<String, ArrayList<patternEntry>> currentPatternCounts = currentSyn.getAllMatchedClausePatterns(inputClausePatterns, doRegex);
			ArrayList<matchedPatternEntry> currentPatternCounts = currentSyn.getAllMatchedClausePatterns_N_Eonly(inputClausePatterns, doRegex,useE,useN);
			//get the "user-pattern to synonym-matched patterns" table. 
			synResult currentResult = new synResult(currentSyn.getEmoWord(), currentSyn.getPercentage(), currentSyn.getPatternTable() ,currentPatternCounts);	//synResult holds run-time/dynamic data.
			
			//setup/initialize run-time object with known information from static data.
			results.add(currentResult);
		}
		
		ArrayList<mergedList> patternMerged = getMergedPatternList(results, inputClausePatterns);
		//pattern alignment issue: each user pattern might match to various number of existing patterns under different synonyms. This is to get a maximum possible matching table.
		results = adjustResults(results, patternMerged, doRegex);
		//adjusting for pattern alignment.
		patternMerged = null;
		System.gc();
//		for(int i=0; i<results.size(); i++) {
//			System.out.println( results.get(i).synonymWord + ": " +  results.get(i).getTable());
//		}
		getProbability(results);
		HashMap<String, Double> scores = getSynScores(results);
		
//		adjustResultsDebug(results);
		return scores;
	}
	public static HashMap<String, Double> localTestSettingSpecify_N_Eonly_opp(String currentFile, ArrayList<String> inputClausePatterns, ArrayList<String> synonyms, String settingPath, boolean doRegex,boolean useE, boolean useN) throws IOException {
		initialize_new(settingPath);
//		BufferedWriter bw = new BufferedWriter(new FileWriter("all240_pattern" + File.separator + currentFile));
//		buildZeroPatterns("unseen.txt");
		ArrayList<synResult> results = new ArrayList<synResult>();
		for(int i=0; i<synonyms.size(); i++) {
			if(table.get(synonyms.get(i)) == null) continue;	//go through every synonyms and skip those not supported.
			synObject currentSyn = table.get(synonyms.get(i));	//synObject holds static data.
//			TreeMap<String, ArrayList<patternEntry>> currentPatternCounts = currentSyn.getAllMatchedClausePatterns(inputClausePatterns, doRegex);
			ArrayList<matchedPatternEntry> currentPatternCounts = currentSyn.getAllMatchedClausePatterns_N_Eonly(inputClausePatterns, doRegex,useE,useN);
			//get the "user-pattern to synonym-matched patterns" table. 
			synResult currentResult = new synResult(currentSyn.getEmoWord(), currentSyn.getPercentage(), currentSyn.getPatternTable() ,currentPatternCounts);	//synResult holds run-time/dynamic data.
			
			//setup/initialize run-time object with known information from static data.
			results.add(currentResult);
		}
		
		ArrayList<mergedList> patternMerged = getMergedPatternList(results, inputClausePatterns);
		//pattern alignment issue: each user pattern might match to various number of existing patterns under different synonyms. This is to get a maximum possible matching table.
		results = adjustResults(results, patternMerged, doRegex);
		//adjusting for pattern alignment.
		patternMerged = null;
		System.gc();
//		for(int i=0; i<results.size(); i++) {
//			System.out.println( results.get(i).synonymWord + ": " +  results.get(i).getTable());
//		}
		getProbability(results);
		HashMap<String, Double> scores = getSynScores(results);
//		for(int i=0; i<results.size(); i++) {
//			bw.write(results.get(i).getSynonymWord()+ "\t" + "0\n");
//			ArrayList<matchedPatternProbability> tempTable = results.get(i).getPTable();
//			bw.write(tempTable.get(0).pattern + "\t" + tempTable.get(0).getValue() + "\n");
//		}
//		bw.close();
//		adjustResultsDebug(results);
		return scores;
	}
	public static HashMap<String, Double> localTestSettingSpecifyWord(ArrayList<String> inputClausePatterns, ArrayList<String> synonyms, String settingPath, boolean doRegex) throws IOException {
		initialize_new(settingPath);
//		buildZeroPatterns("unseen.txt");
		ArrayList<synResult> results = new ArrayList<synResult>();
		for(int i=0; i<synonyms.size(); i++) {
			if(table.get(synonyms.get(i)) == null) continue;	//go through every synonyms and skip those not supported.
			synObject currentSyn = table.get(synonyms.get(i));	//synObject holds static data.
//			TreeMap<String, ArrayList<patternEntry>> currentPatternCounts = currentSyn.getAllMatchedClausePatterns(inputClausePatterns, doRegex);
//			ArrayList<matchedPatternEntry> currentPatternCounts = currentSyn.getAllMatchedClausePatterns(inputClausePatterns, doRegex);
			ArrayList<matchedPatternEntry> currentPatternCounts = currentSyn.getAllMatchedClauseWords(inputClausePatterns, doRegex);
			//get the "user-pattern to synonym-matched patterns" table. 
			synResult currentResult = new synResult(currentSyn.getEmoWord(), currentSyn.getPercentage(), currentSyn.getPatternTable() ,currentPatternCounts);	//synResult holds run-time/dynamic data.
			
			//setup/initialize run-time object with known information from static data.
			results.add(currentResult);
		}
		
		ArrayList<mergedList> patternMerged = getMergedPatternList(results, inputClausePatterns);
		//pattern alignment issue: each user pattern might match to various number of existing patterns under different synonyms. This is to get a maximum possible matching table.
		results = adjustResults(results, patternMerged, doRegex);
		//adjusting for pattern alignment.
		patternMerged = null;
		System.gc();
//		for(int i=0; i<results.size(); i++) {
//			System.out.println( results.get(i).synonymWord + ": " +  results.get(i).getTable());
//		}
		getProbability(results);
		HashMap<String, Double> scores = getSynScores(results);
		return scores;
	}
	public static HashMap<String, Double> localTestSettingSpecifyDice(ArrayList<String> inputClausePatterns, ArrayList<String> synonyms, String settingPath, boolean doRegex, TreeMap<String,Double> patternStat, TreeMap<String,Double> emoStat) throws IOException {
		initialize_new(settingPath);
//		buildZeroPatterns("unseen.txt");
		ArrayList<synResult> results = new ArrayList<synResult>();
		for(int i=0; i<synonyms.size(); i++) {
			if(table.get(synonyms.get(i)) == null) continue;	//go through every synonyms and skip those not supported.
			synObject currentSyn = table.get(synonyms.get(i));	//synObject holds static data.
//			TreeMap<String, ArrayList<patternEntry>> currentPatternCounts = currentSyn.getAllMatchedClausePatterns(inputClausePatterns, doRegex);
			ArrayList<matchedPatternEntry> currentPatternCounts = currentSyn.getAllMatchedClausePatterns(inputClausePatterns, doRegex);
			//get the "user-pattern to synonym-matched patterns" table. 
			synResult currentResult = new synResult(currentSyn.getEmoWord(), currentSyn.getPercentage(), currentSyn.getPatternTable() ,currentPatternCounts);	//synResult holds run-time/dynamic data.
			
			//setup/initialize run-time object with known information from static data.
			results.add(currentResult);
		}
		
		ArrayList<mergedList> patternMerged = getMergedPatternList(results, inputClausePatterns);
		//pattern alignment issue: each user pattern might match to various number of existing patterns under different synonyms. This is to get a maximum possible matching table.
		results = adjustResults(results, patternMerged, doRegex);
		//adjusting for pattern alignment.
		patternMerged = null;
		System.gc();
//		for(int i=0; i<results.size(); i++) {
//			System.out.println( results.get(i).synonymWord + ": " +  results.get(i).getTable());
//		}
//		getProbability(results);
		getProbabilityDice(results, patternStat, emoStat, synonyms);
		HashMap<String, Double> scores = getSynScores(results);
		return scores;
	}
	public static HashMap<String, Double> localTestSettingSpecifyPMI(ArrayList<String> inputClausePatterns, ArrayList<String> synonyms, String settingPath, boolean doRegex, TreeMap<String,Double> patternStat, TreeMap<String,Double> emoStat) throws IOException {
		initialize_new(settingPath);
//		buildZeroPatterns("unseen.txt");
		ArrayList<synResult> results = new ArrayList<synResult>();
		for(int i=0; i<synonyms.size(); i++) {
			if(table.get(synonyms.get(i)) == null) continue;	//go through every synonyms and skip those not supported.
			synObject currentSyn = table.get(synonyms.get(i));	//synObject holds static data.
//			TreeMap<String, ArrayList<patternEntry>> currentPatternCounts = currentSyn.getAllMatchedClausePatterns(inputClausePatterns, doRegex);
			ArrayList<matchedPatternEntry> currentPatternCounts = currentSyn.getAllMatchedClausePatterns(inputClausePatterns, doRegex);
			//get the "user-pattern to synonym-matched patterns" table. 
			synResult currentResult = new synResult(currentSyn.getEmoWord(), currentSyn.getPercentage(), currentSyn.getPatternTable() ,currentPatternCounts);	//synResult holds run-time/dynamic data.
			
			//setup/initialize run-time object with known information from static data.
			results.add(currentResult);
		}
		
		ArrayList<mergedList> patternMerged = getMergedPatternList(results, inputClausePatterns);
		//pattern alignment issue: each user pattern might match to various number of existing patterns under different synonyms. This is to get a maximum possible matching table.
		results = adjustResults(results, patternMerged, doRegex);
		//adjusting for pattern alignment.
		patternMerged = null;
		System.gc();
//		for(int i=0; i<results.size(); i++) {
//			System.out.println( results.get(i).synonymWord + ": " +  results.get(i).getTable());
//		}
//		getProbability(results);
		getProbabilityPMI(results, patternStat, emoStat, synonyms);
		HashMap<String, Double> scores = getSynScores(results);
		return scores;
	}
	
	public static HashMap<String, Double> localTestSettingSpecifyTFIDF(ArrayList<String> inputClausePatterns, ArrayList<String> synonyms, String settingPath, boolean doRegex, TreeMap<String,Double> patternStat, TreeMap<String,Double> emoStat) throws IOException {
		initialize_new(settingPath);
//		buildZeroPatterns("unseen.txt");
		ArrayList<synResult> results = new ArrayList<synResult>();
		for(int i=0; i<synonyms.size(); i++) {
			if(table.get(synonyms.get(i)) == null) continue;	//go through every synonyms and skip those not supported.
			synObject currentSyn = table.get(synonyms.get(i));	//synObject holds static data.
//			TreeMap<String, ArrayList<patternEntry>> currentPatternCounts = currentSyn.getAllMatchedClausePatterns(inputClausePatterns, doRegex);
			ArrayList<matchedPatternEntry> currentPatternCounts = currentSyn.getAllMatchedClausePatterns(inputClausePatterns, doRegex);
			//get the "user-pattern to synonym-matched patterns" table. 
			synResult currentResult = new synResult(currentSyn.getEmoWord(), currentSyn.getPercentage(), currentSyn.getPatternTable() ,currentPatternCounts);	//synResult holds run-time/dynamic data.
			
			//setup/initialize run-time object with known information from static data.
			results.add(currentResult);
		}
		
		ArrayList<mergedList> patternMerged = getMergedPatternList(results, inputClausePatterns);
		//pattern alignment issue: each user pattern might match to various number of existing patterns under different synonyms. This is to get a maximum possible matching table.
		results = adjustResults(results, patternMerged, doRegex);
		//adjusting for pattern alignment.
		patternMerged = null;
		System.gc();
//		for(int i=0; i<results.size(); i++) {
//			System.out.println( results.get(i).synonymWord + ": " +  results.get(i).getTable());
//		}
//		getProbability(results);
		getProbabilityTFIDF(results, patternStat, emoStat, synonyms);
		HashMap<String, Double> scores = getSynScores(results);
		return scores;
	}
	
//	public static ArrayList<synResult> getOrderedResults(ArrayList<Map.Entry<String, Double>> results) {
//		ArrayList<synResult> orderedResult = new ArrayList<synResult>();
//		
//		
//		return orderedResult;
//	}
	public static HashMap<String, Double> clientServerModeProcess(ArrayList<String> inputClausePatterns, ArrayList<String> synonyms) throws IOException, ClassNotFoundException {
		
		InetAddress serverHost = InetAddress.getByName("localhost"); 
		int serverPort = 3456;
		Socket clientSocket = new Socket(serverHost, serverPort);
		ObjectInputStream OIs = new ObjectInputStream(clientSocket.getInputStream());
//		ObjectOutputStream OOs = new ObjectOutputStream(clientSocket.getOutputStream());
//		BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		PrintStream Ps = new PrintStream(clientSocket.getOutputStream());
		
		ArrayList<synResult> results = new ArrayList<synResult>();
		for(int i=0; i<synonyms.size(); i++) {
			Ps.println(synonyms.get(i));
			synObject data = (synObject)OIs.readObject();
			if(data == null) continue;
			synObject currentSyn = data;
//			TreeMap<String, ArrayList<patternEntry>> currentPatternCounts = currentSyn.getAllMatchedClausePatterns(inputClausePatterns, false);
			ArrayList<matchedPatternEntry> currentPatternCounts = currentSyn.getAllMatchedClausePatterns(inputClausePatterns, false);
			synResult currentResult = new synResult(currentSyn.getEmoWord(), currentSyn.getPercentage(), currentSyn.getPatternTable(), currentPatternCounts);
			results.add(currentResult);
		}
		
		clientSocket.close();
		
		ArrayList<mergedList> patternMerged = getMergedPatternList(results, inputClausePatterns);
		results = adjustResults(results, patternMerged, false);
		if( results.size() < 1)
			return null;
		patternMerged = null;
		System.gc();
		getProbability(results);
		HashMap<String, Double> scores = getSynScores(results);
		return scores;
	}
	public static ArrayList<ArrayList<Double>> getSum(ArrayList<synResult> results) {
		if( results.size() < 1) return null;
		ArrayList<ArrayList<Double>> sumResults = results.get(0).getValueSet();
		for(int i=1; i<results.size(); i++) {
			ArrayList<ArrayList<Double>> currentValueSet = results.get(i).getValueSet();
//			System.out.println(currentValueSet);
			for(int j=0; j<currentValueSet.size(); j++) {
				ArrayList<Double> currentPatternValueSet = currentValueSet.get(j);
				for(int k=0; k<currentPatternValueSet.size(); k++) {
					sumResults.get(j).set(k, (sumResults.get(j).get(k) + currentPatternValueSet.get(k) ) );
				}
			}
		}
		return sumResults;		
	}
	public static ArrayList<ArrayList<Double>> getIDF(ArrayList<synResult> results) {
		if( results.size() < 1) return null;
//		ArrayList<ArrayList<Double>> sumResults = results.get(0).getValueSet();
		ArrayList<ArrayList<Double>> IDFResults = results.get(0).getValueSet();
		for(int i=0; i<results.size(); i++) {
			ArrayList<ArrayList<Double>> currentValueSet = results.get(i).getValueSet();
//			System.out.println(currentValueSet);
			for(int j=0; j<currentValueSet.size(); j++) {
				ArrayList<Double> currentPatternValueSet = currentValueSet.get(j);
				double totalDoc=(double)currentValueSet.size();
				double hitDoc=0.0;
				for(int k=0; k<currentPatternValueSet.size(); k++) {
					if(currentPatternValueSet.get(k)>0) {
						hitDoc++;
					}
//					sumResults.get(j).set(k, (sumResults.get(j).get(k) + currentPatternValueSet.get(k) ) );
				}
				for(int k=0; k<currentPatternValueSet.size(); k++) {
					IDFResults.get(j).set(k, totalDoc/hitDoc);
				}
//				IDFResults.get(j).set(k, totalDoc/hitDoc);
			}
		}
		return IDFResults;		
	}
	public static void getProbability(ArrayList<synResult> results) {
//		for(int i=0; i<results.size(); i++) {
//			results.get(i).weightingCount();
//		}s
		
		ArrayList<ArrayList<Double>> sum = getSum(results);
//		System.out.println(sum);
		for(int i=0; i<results.size(); i++) {
			results.get(i).setProbability(sum);
//			System.out.println(results.get(i).getPTable());
		}
		for(int i=0; i<results.size(); i++) {
//			results.get(i).penalizedProbability(true);
//			results.get(i).penalizedProbability(false);
			results.get(i).penalizedReFormProbability(false);
			// option for penalty sum up to one
//			System.out.println(results.get(i).getPTable());
		}
//		System.out.println();
		for(int i=0; i<results.size(); i++) {
			results.get(i).patternReduce();
//			System.out.println(results.get(i).getPTable());
		}
//		System.out.println();
		for(int i=0; i<results.size(); i++) {
			results.get(i).patternTakeLog();
//			System.out.format("%-15s ", (results.get(i).getSynonymWord() + ":")); 
//			System.out.print(results.get(i).getPTable() + "\n");
////			System.out.format(" Score = %5.5f   \n", results.get(i).getSynonymScore());
//			System.out.flush();
////			System.out.println(results.get(i).getPTable());
		}
//		System.out.println();
	}
	
	public static void getProbabilityDice(ArrayList<synResult> results, TreeMap<String,Double> patternStat, TreeMap<String,Double> emoStat, ArrayList<String> synonyms) {
//		for(int i=0; i<results.size(); i++) {
//			results.get(i).weightingCount();
//		}s
		
//		ArrayList<ArrayList<Double>> sum = getSum(results);
//		System.out.println(sum);
		for(int i=0; i<results.size(); i++) {
			results.get(i).setProbabilityDice(patternStat, emoStat, synonyms.get(i));
//			System.out.println(results.get(i).getPTable());
		}
		for(int i=0; i<results.size(); i++) {
			results.get(i).penalizedProbability(true);
//			System.out.println(results.get(i).getPTable());
		}
//		System.out.println();
		for(int i=0; i<results.size(); i++) {
			results.get(i).patternReduce();
//			System.out.println(results.get(i).getPTable());
		}
//		System.out.println();
		for(int i=0; i<results.size(); i++) {
			results.get(i).patternTakeLog();
//			System.out.format("%-15s ", (results.get(i).getSynonymWord() + ":")); 
//			System.out.print(results.get(i).getPTable() + "\n");
////			System.out.format(" Score = %5.5f   \n", results.get(i).getSynonymScore());
//			System.out.flush();
////			System.out.println(results.get(i).getPTable());
		}
//		System.out.println();
	}
	public static void getProbabilityPMI(ArrayList<synResult> results, TreeMap<String,Double> patternStat, TreeMap<String,Double> emoStat, ArrayList<String> synonyms) {
//		for(int i=0; i<results.size(); i++) {
//			results.get(i).weightingCount();
//		}s
		
//		ArrayList<ArrayList<Double>> sum = getSum(results);
//		System.out.println(sum);
		for(int i=0; i<results.size(); i++) {
			results.get(i).setProbabilityPMI(patternStat, emoStat, synonyms.get(i));
//			System.out.println(results.get(i).getPTable());
		}
		for(int i=0; i<results.size(); i++) {
			results.get(i).penalizedProbability(true);
//			System.out.println(results.get(i).getPTable());
		}
//		System.out.println();
		for(int i=0; i<results.size(); i++) {
			results.get(i).patternReduce();
//			System.out.println(results.get(i).getPTable());
		}
//		System.out.println();
		for(int i=0; i<results.size(); i++) {
			results.get(i).patternTakeLog();
//			System.out.format("%-15s ", (results.get(i).getSynonymWord() + ":")); 
//			System.out.print(results.get(i).getPTable() + "\n");
////			System.out.format(" Score = %5.5f   \n", results.get(i).getSynonymScore());
//			System.out.flush();
////			System.out.println(results.get(i).getPTable());
		}
//		System.out.println();
	}
	public static void getProbabilityTFIDF(ArrayList<synResult> results, TreeMap<String,Double> patternStat, TreeMap<String,Double> emoStat, ArrayList<String> synonyms) {
//		for(int i=0; i<results.size(); i++) {
//			results.get(i).weightingCount();
//		}s
		
		ArrayList<ArrayList<Double>> sum = getSum(results);
		ArrayList<ArrayList<Double>> IDF = getIDF(results);
//		System.out.println(sum);
		for(int i=0; i<results.size(); i++) {
			results.get(i).setProbabilityTFIDF(patternStat, emoStat, synonyms.get(i), sum, IDF);
//			System.out.println(results.get(i).getPTable());
		}
		for(int i=0; i<results.size(); i++) {
			results.get(i).penalizedProbability(true);
//			System.out.println(results.get(i).getPTable());
		}
//		System.out.println();
		for(int i=0; i<results.size(); i++) {
			results.get(i).patternReduce();
//			System.out.println(results.get(i).getPTable());
		}
//		System.out.println();
		for(int i=0; i<results.size(); i++) {
			results.get(i).patternTakeLog();
//			System.out.format("%-15s ", (results.get(i).getSynonymWord() + ":")); 
//			System.out.print(results.get(i).getPTable() + "\n");
////			System.out.format(" Score = %5.5f   \n", results.get(i).getSynonymScore());
//			System.out.flush();
////			System.out.println(results.get(i).getPTable());
		}
//		System.out.println();
	}
	
	public static HashMap<String, Double> getSynScores(ArrayList<synResult> results) {
		HashMap<String, Double> scoreMap = new HashMap<String, Double>();
		for(int i=0; i<results.size(); i++) {
			scoreMap.put(results.get(i).getSynonymWord(), results.get(i).getSynonymScore());
//			System.out.format("%-15s ", (results.get(i).getSynonymWord() + ":")); 
//			System.out.print(results.get(i).getTable());
//			System.out.print(results.get(i).getPTable());
//			System.out.format(" Score = %5.5f   \n", results.get(i).getSynonymScore());
//			System.out.flush();
		}
		return scoreMap;
	}
	public static HashMap<String, Double> getSynPeScores(ArrayList<synResult> results, HashMap<String, Double> pe, int size) throws IOException {
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		HashMap<String, Double> scoreMap = new HashMap<String, Double>();
		double localsum=0.0,localivtsum=0.0;
		for(int i=0; i<results.size(); i++) {
			localsum += pe.get(results.get(i).getSynonymWord());
//			System.out.println("pe: " + pe.get(results.get(i).getSynonymWord()));
//			System.out.println("??: " + results.get(i).getSynonymWord() + ": " +pe.get(results.get(i).getSynonymWord()));
//			if(pe.get(results.get(i).getSynonymWord())==0.0)
//				scoreMap.put(results.get(i).getSynonymWord(), results.get(i).getSynonymScore()+(1-size)*(-99.0));
//			else
//				scoreMap.put(results.get(i).getSynonymWord(), results.get(i).getSynonymScore()+(1-size)*Math.log10(pe.get(results.get(i).getSynonymWord())));
//			System.out.format("%-15s ", (results.get(i).getSynonymWord() + ":")); 
//			System.out.print(results.get(i).getTable());
//			System.out.print(results.get(i).getPTable());
//			System.out.format(" Score = %5.5f   \n", results.get(i).getSynonymScore());
//			System.out.flush();
		}
		for(int i=0; i<results.size(); i++) {
			localivtsum += 1 / Math.log(pe.get(results.get(i).getSynonymWord()) / localsum);
		}
		
//		System.out.println("localsum: " + localsum + " localivtsum: " + localivtsum);
//		stdin.readLine();
		for(int i=0; i<results.size(); i++) {
			if(pe.get(results.get(i).getSynonymWord())==0.0)
				scoreMap.put(results.get(i).getSynonymWord(), results.get(i).getSynonymScore()+(1-size)*(-99.0));
			else
				scoreMap.put(results.get(i).getSynonymWord(), results.get(i).getSynonymScore()+(1-size)*Math.log(localivtsum / Math.log(pe.get(results.get(i).getSynonymWord()) / localsum)));
//				scoreMap.put(results.get(i).getSynonymWord(), results.get(i).getSynonymScore()+(1-size)*Math.log(pe.get(results.get(i).getSynonymWord()) / localsum));

		}
		return scoreMap;
	}
	public static HashMap<String, Double> getWeightSynScores(ArrayList<synResult> results, ArrayList<String> oriStr) {
		double combineFactor=0.5;
		double damp=0.2;
		int maxMakovitr=50;
		HashMap<String, Double> scoreMap = new HashMap<String, Double>();
		for(int i=0; i<results.size(); i++) {
			ArrayList<matchedPatternProbability> tempTable = results.get(i).getPTable();
//			ArrayList<String> textPattern = new  ArrayList<String>();
			HashMap<String, Double> patternScore = new HashMap<String, Double>();		
			HashMap<String, Double> patternWeight = new HashMap<String, Double>();
			for(int j=0; j<tempTable.size(); j++) {
//				textPattern.add(tempTable.get(j).pattern);
				patternScore.put(tempTable.get(j).pattern, tempTable.get(j).getValue().get(0).getValue());
			}
//			synObject currentSyn = wtable.get(results.get(i).getSynonymWord());
			if(wtable.get(results.get(i).getSynonymWord()) == null) 
				scoreMap.put(results.get(i).getSynonymWord(), results.get(i).getSynonymScore());
			else {
				ArrayList<Entry<String, Double>> wordList = new ArrayList<Entry<String, Double>>(wtable.get(results.get(i).getSynonymWord()).getPatternTable().entrySet());
				double [][] matrix = new double[oriStr.size()][oriStr.size()];
				double rootweight=0.0,tempMCTP,tempNCTP;
				
				int rootM=99,rootN=99,indexM,indexN;
				for(int m=0;m<oriStr.size();m++) {
	//				System.out.println("patternM: " + oriStr.get(m));
					ArrayList<String> tokensM = new ArrayList(Arrays.asList(oriStr.get(m).split(" ")));
					for(int n=m+1;n<oriStr.size();n++) {
	//					System.out.println("patternN: " + oriStr.get(n));
						ArrayList<String> tokensN = new ArrayList(Arrays.asList(oriStr.get(n).split(" ")));
						double [][] tokenmatrix = new double[tokensM.size()-1][tokensN.size()-1];
						double [] tokenMCTP = new double[tokensM.size()-1];
						double [] tokenNCTP = new double[tokensN.size()-1];
						double rootMCTP=0.0,rootNCTP=0.0;
						for(int p=0;p<tokensM.size();p++){
							for(int q=0;q<tokensN.size();q++){
								if(tokensM.get(p).contains("_ROOT") && tokensN.get(q).contains("_ROOT")){
									Iterator<Entry<String, Double>> it = wordList.iterator();
									while(it.hasNext()) {
										Entry<String, Double> currentWord = it.next();
										if(tokensM.get(p).replace("_ROOT", "").equals(currentWord.getKey())){
											rootMCTP=currentWord.getValue();
										}
										if(tokensN.get(q).replace("_ROOT", "").equals(currentWord.getKey())){
											rootNCTP=currentWord.getValue();
										}
									}
									double sim;
									if(tokensM.get(p).equals(tokensN.get(q)))
										sim=1.0;
									else
										sim=SimilarityCalculationDemo.getLinSimilarity(tokensM.get(p).replace("_ROOT", ""), tokensN.get(q).replace("_ROOT", ""));
								
									rootweight=sim*rootMCTP*rootNCTP;
									rootM=p;
									rootN=q;
								}
								else {
									if(tokensM.get(p).contains("_ROOT")){
										rootM=p;
										continue;
									}
									if(tokensN.get(q).contains("_ROOT")){
										rootN=q;
										continue;
									}
									if(p>rootM)
										indexM=p-1;
									else
										indexM=p;
									if(q>rootN)
										indexN=q-1;
									else
										indexN=q;
									tempMCTP=0.0;
									tempNCTP=0.0;
									Iterator<Entry<String, Double>> it = wordList.iterator();
									while(it.hasNext()) {
										Entry<String, Double> currentWord = it.next();
										if(tokensM.get(p).equals(currentWord.getKey())){
											tempMCTP=currentWord.getValue();
										}
										if(tokensN.get(q).equals(currentWord.getKey())){
											tempNCTP=currentWord.getValue();
										}
									}
									double sim;
									if(tokensM.get(p).equals(tokensN.get(q)))
										sim=1.0;
									else
										sim=SimilarityCalculationDemo.getLinSimilarity(tokensM.get(p),tokensN.get(q));
								
	//								System.out.println("tokenM/tokenN: " + tokensM.get(p) + " " + tokensN.get(q));
	//								System.out.println("p/M/q/N " + p + " " + indexM + " " + q + " " + indexN +" " + rootM +" " + rootN + " " +tokensM.size() + " " + tokensN.size());
//									tokenmatrix[indexM][indexN]=0.0;
									tokenmatrix[indexM][indexN]=sim*tempMCTP*tempNCTP;
									tokenMCTP[indexM]=tempMCTP;
									tokenNCTP[indexN]=tempNCTP;
								}
							}
						}// for all token in M
						double avgtokenmatrix = 0.0, sentenceM = combineFactor*Math.pow(rootMCTP, 2), sentenceN = combineFactor*Math.pow(rootNCTP, 2);
						for(int p=0;p<tokensM.size()-1;p++){
							for(int q=0;q<tokensN.size()-1;q++){
								avgtokenmatrix += tokenmatrix[p][q];
							}
						}
						if(tokensM.size()<2||tokensN.size()<2)
							avgtokenmatrix=0.0;
						else
							avgtokenmatrix /= (tokensM.size()-1)*(tokensN.size()-1);
	//					System.out.println("ROOTM/NCTP: " + tokensM.get(rootM) + ":" + rootMCTP + "/" +  tokensN.get(rootN) + ":" + rootNCTP);
	//					System.out.println("ROOTWeight: " + rootweight);
	//					System.out.println("avgtokenmatrix: " + avgtokenmatrix);
						for(int p=0;p<tokensM.size()-1;p++){
							for(int q=0;q<tokensM.size()-1;q++){
								if(p>=rootM)
									indexM=p+1;
								else
									indexM=p;
								if(q>=rootM)
									indexN=q+1;
								else
									indexN=q;
								double sim;
								if(tokensM.get(indexM).equals(tokensM.get(indexN)))
										sim=1.0;
								else
									sim=SimilarityCalculationDemo.getLinSimilarity(tokensM.get(indexM),tokensM.get(indexN));
								
								sentenceM += (1-combineFactor)*tokenMCTP[p]*tokenMCTP[q]*sim / ((tokensM.size()-1)*(tokensM.size()-1));
	//							System.out.println("sentenceM/p/q: " + sentenceM + "/" + p + "/" + q + "/" + indexM + "/" + indexN + "/" + SimilarityCalculationDemo.getLinSimilarity(tokensM.get(indexM),tokensM.get(indexN)));
							}
						}
						for(int p=0;p<tokensN.size()-1;p++){
							for(int q=0;q<tokensN.size()-1;q++){
								if(p>=rootN)
									indexM=p+1;
								else
									indexM=p;
								if(q>=rootN)
									indexN=q+1;
								else
									indexN=q;
								double sim;
								if(tokensN.get(indexM).equals(tokensN.get(indexN)))
										sim=1.0;
								else
									sim=SimilarityCalculationDemo.getLinSimilarity(tokensN.get(indexM),tokensN.get(indexN));
								
								sentenceN += (1-combineFactor)*tokenNCTP[p]*tokenNCTP[q]*sim / ((tokensN.size()-1)*(tokensN.size()-1));
							}
						}
	//					System.out.println("sentenceM/size: " + sentenceM + "/" + tokensM.size());
	//					System.out.println(Arrays.toString(tokenMCTP));
	//					System.out.println("sentenceN/size: " + sentenceN + "/" + tokensN.size());
	//					System.out.println(Arrays.toString(tokenNCTP));
						if(sentenceM==0.0 && sentenceN==0.0)
							matrix[m][n]=0.0;
						else
							matrix[m][n]=(combineFactor*rootweight+(1-combineFactor)*avgtokenmatrix )/(Math.pow(sentenceM, 0.5)+Math.pow(sentenceN, 0.5));
						if(Double.isNaN(matrix[m][n])) {
							System.out.println("patternM: " + oriStr.get(m));
							System.out.println("patternN: " + oriStr.get(n));
							System.out.println("matrix[m][n]: " + matrix[m][n]);
							System.out.println("rootweight: " + rootweight);
							System.out.println("avgtokenmatrix: " + avgtokenmatrix);
							System.out.println("sentenceM: " + sentenceM);
							System.out.println("sentenceN: " + sentenceN);
						}
						
					}
				}// for all pattern in i
	//			Matrix oriMatrix = new Matrix(matrix);
	//			oriMatrix.print(oriStr.size(), 5);
	
				for(int m=0;m<oriStr.size();m++) {
					matrix[m][m]=1.0;
					double localsum=0.0;
					for(int n=0;n<m-1;n++) {
						matrix[m][n]=matrix[n][m];
					}
					for(int n=0;n<oriStr.size();n++) {
						localsum += matrix[m][n];
					}
					for(int n=0;n<oriStr.size();n++) {
						matrix[m][n] = matrix[m][n] / localsum * (1-damp) + damp/oriStr.size();
					}
				}
	//			System.out.println(matrix[0][0]);
				Matrix markovMatrix = new Matrix(matrix);
	//			markovMatrix.print(oriStr.size(), 2);
				markovMatrix = markovMatrix.transpose();
		        Matrix eigen = new Matrix(oriStr.size(), 1, 1.0 / oriStr.size()); // initial guess for eigenvector
		        for (int m = 0; m < maxMakovitr; m++) {
		            eigen = markovMatrix.times(eigen);
		            eigen = eigen.times(1.0 / eigen.norm1());       // rescale
		        }
				for(int j=0; j<tempTable.size(); j++) {
					patternWeight.put(tempTable.get(j).pattern, eigen.get(j, 0));
				}
	//			eigen.print(1, 2);
				scoreMap.put(results.get(i).getSynonymWord(), results.get(i).getWeightSynonymScore(patternWeight));
	//			System.out.format("%-15s ", (results.get(i).getSynonymWord() + ":")); 
	//			System.out.print(results.get(i).getTable());
	//			System.out.print(results.get(i).getPTable());
	//			System.out.format(" Score = %5.5f   \n", results.get(i).getSynonymScore());
	//			System.out.flush();
			}
		}
		return scoreMap;
	}
	public static HashMap<String, String> getMatchablePatterns(ArrayList<synResult> results) {
		HashMap<String, String> matchable = new HashMap<String, String>();
		ArrayList<matchedPatternEntry> list = results.get(0).getTable();
		for(int i=0; i<list.size(); i++) {
			String matchablePattern = list.get(i).getKey();
			if(!matchable.containsKey(matchablePattern))
				matchable.put(matchablePattern, matchablePattern);
		}
		
		return matchable;
 	}
	public static void probResultsDebug(ArrayList<synResult> results) { 
		int matchedPatternEntryListSize = results.get(0).getPTable().size();
		for(int i=0; i<matchedPatternEntryListSize; i++) {
			for(int j=0; j<results.size(); j++) {
				synResult currentResult = results.get(j);
				ArrayList<matchedPatternProbability> resultsTable = currentResult.getPTable();
				System.out.format("%-15s ",(currentResult.getSynonymWord() + ":"));
				System.out.println(resultsTable.get(i));
//				System.out.println(currentResult.getSynonymWord() + ":\t" + resultsTable.get(i));
			}
			System.out.println();	
			System.out.flush();
		}
	}
	public static void adjustResultsDebug(ArrayList<synResult> results) { 
		int matchedPatternEntryListSize = results.get(0).getTable().size();
		for(int i=0; i<matchedPatternEntryListSize; i++) {
			for(int j=0; j<results.size(); j++) {
				synResult currentResult = results.get(j);
				ArrayList<matchedPatternEntry> resultsTable = currentResult.getTable();
				System.out.format("%-15s ",(currentResult.getSynonymWord() + ":"));
				System.out.println(resultsTable.get(i));
//				System.out.println(currentResult.getSynonymWord() + ":\t" + resultsTable.get(i));
			}
			System.out.println();	
			System.out.flush();
		}
	}
	public static ArrayList<synResult> adjustResults(ArrayList<synResult> results, ArrayList<mergedList> patternMerged, boolean doRegex) {
//		System.out.println(patternMerged);
		ArrayList<synResult> adjustedResults = new ArrayList<synResult>(results);
		for(int i=0; i<results.size(); i++) {
			synResult currentResult = results.get(i);
			currentResult.updateMyPatternCountResults(patternMerged, doRegex);
//			System.out.println(currentResult.getTable());
			currentResult.tableClean();
			adjustedResults.set(i, currentResult);
		}
		return adjustedResults;
	}
	public static ArrayList<mergedList> getMergedPatternList(ArrayList<synResult> results, ArrayList<String> inputClausePatterns) {
//		get all possible matched existing patterns (crossing synonym) to an user input pattern
		ArrayList<mergedList> merged = new ArrayList<mergedList>();
		for(int i=0; i<inputClausePatterns.size(); i++) {
			TreeMap<String, String> patternListHash = new TreeMap<String, String>();
			for(int j=0; j<results.size(); j++) {
				ArrayList<String> currentPatternList = results.get(j).getPatternListByIndex(i);
				Iterator<String> it = currentPatternList.iterator();
				while(it.hasNext()) {
					String currentPattern = it.next();
					patternListHash.put(currentPattern, currentPattern);
				}
			}
//			mergedPatternSet.put(inputClausePatterns.get(i), new ArrayList<String>(patternListHash.keySet()));
			merged.add(new mergedList(inputClausePatterns.get(i), new ArrayList<String>(patternListHash.keySet())) );
		}
		return merged;
	}
//	/*
	public static void generalization(String patternFilePath, String patternFileOutputPath) throws IOException {
		table = new TreeMap<String, synObject>();
		BufferedReader br;
		br = new BufferedReader(new InputStreamReader(new FileInputStream(patternFilePath), "UTF-8"));
		String line;
		synObject obj = null;
		String currentEmoWord = null;
		while( (line=br.readLine())!= null ) {		
			if( line.contains("===================")) {
				if( obj.getPatternTableSize() == 0.0) continue;
				table.put(currentEmoWord, obj);
				continue;
			}
			if( line.matches("\\@[\\w-]+:")) {
				currentEmoWord = line.replace("@", "");
				currentEmoWord = currentEmoWord.replace(":", "");
				obj = new synObject(currentEmoWord);
			}
			else {
				if( line.contains("??")) continue;	//invalid patterns
				obj.addPatterns(line);
			}
		}
		br.close();
//		System.out.println("allSum = " + allSum);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(patternFileOutputPath), "UTF-8"));
		Iterator<synObject> itr = table.values().iterator();
		while( itr.hasNext()) {
			synObject current = itr.next();
//			current.setPercentage(allSum);
//			current.weightApply();
			current.generalization();
			System.out.println("...");
			bw.write("@"+current.getEmoWord()+":\n");
			Iterator<Entry<String, Double>> it = current.patterns.entrySet().iterator();
			while( it.hasNext()) {
				Entry<String, Double> crnt = it.next();
				bw.write(crnt.getKey() + "\t" + crnt.getValue() + "\n");
			}
			bw.write("======================\n");
			
//			System.out.println("Synonym \""+ current.getEmoWord() + "\" percentage = " + current.getPercentage());
		}
		bw.close();
	}
//	*/
	public static boolean selfContainTest(String line, String emoWord) {
		String pattern = line.split("\t")[0];
		String[] tokens = pattern.split(" ");
		boolean result = false;
		for(int i=0; i<tokens.length; i++) {
			if( tokens[i].equals(emoWord)) {
				result = true;
				break;
			}
		}
		return result;
	}
	public static TreeMap<String, String> buildClassTable(String filePath) throws IOException {
		TreeMap<String, String> classTable = new TreeMap<String, String>();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"));
		String line;
		while( (line = br.readLine())!= null) {
			if( line.length() < 1) continue;
			String[] tokens = line.split("\t");
			if( !classTable.containsKey(tokens[0]))
				classTable.put(tokens[0], tokens[1]);
		}
		
		return classTable;
	}
	public static void initialize_new(String patternFilePath) throws IOException {
		table = new TreeMap<String, synObject>();
//		TreeMap<String, String> classTable = buildClassTable("emo3785to6.txt");
		
		BufferedReader br;
		br = new BufferedReader(new InputStreamReader(new FileInputStream(patternFilePath), "UTF-8"));
		String line;
//		double allSum = 0.0;
		synObject obj = null;
		String currentEmoWord = null;
		while( (line=br.readLine())!= null ) {		
			if( line.contains("===================")) {
				if( obj.getPatternTableSize() == 0.0) continue;
//				allSum += obj.getPatternCountSum();	//stats how many total pattern counts under this synonym
				table.put(currentEmoWord, obj);
				continue;
			}
			if( line.matches("\\@[\\w-]+:")) {
				currentEmoWord = line.replace("@", "");
				currentEmoWord = currentEmoWord.replace(":", "");
				obj = new synObject(currentEmoWord);
			}
			else {
				if( line.contains("??")) continue;	//invalid patterns
				obj.addPatterns(line);
				/*
				String category = classTable.get(currentEmoWord);
				if( category.equals("sadness") || category.equals("disgust"))	//include all patterns
					obj.addPatterns(line);
				else {
					if( !selfContainTest(line, currentEmoWord) ) //exclude self-containing patterns
						obj.addPatterns(line);
				} 
				*/
				
			}
		}
		br.close();
//		System.out.println("allSum = " + allSum);
		/*
		Iterator<synObject> itr = table.values().iterator();
		while( itr.hasNext()) {
			synObject current = itr.next();
			current.setPercentage(allSum);
//			current.weightApply();
//			System.out.println("Synonym \""+ current.getEmoWord() + "\" percentage = " + current.getPercentage());
		} 
		*/
	}
	public static void initialize_weight(String patternFilePath) throws IOException {
		wtable = new TreeMap<String, synObject>();
//		TreeMap<String, String> classTable = buildClassTable("emo3785to6.txt");
		
		BufferedReader br;
		br = new BufferedReader(new InputStreamReader(new FileInputStream(patternFilePath), "UTF-8"));
		String line;
//		double allSum = 0.0;
		synObject obj = null;
		String currentEmoWord = null;
		while( (line=br.readLine())!= null ) {		
			if( line.contains("===================")) {
				if( obj.getPatternTableSize() == 0.0) continue;
//				allSum += obj.getPatternCountSum();	//stats how many total pattern counts under this synonym
				wtable.put(currentEmoWord, obj);
				continue;
			}
			if( line.matches("\\@[\\w-]+:")) {
				currentEmoWord = line.replace("@", "");
				currentEmoWord = currentEmoWord.replace(":", "");
				obj = new synObject(currentEmoWord);
			}
			else {
				if( line.contains("??")) continue;	//invalid patterns
				obj.addPatterns(line);
				/*
				String category = classTable.get(currentEmoWord);
				if( category.equals("sadness") || category.equals("disgust"))	//include all patterns
					obj.addPatterns(line);
				else {
					if( !selfContainTest(line, currentEmoWord) ) //exclude self-containing patterns
						obj.addPatterns(line);
				} 
				*/
				
			}
		}
		br.close();
//		System.out.println("allSum = " + allSum);
		/*
		Iterator<synObject> itr = table.values().iterator();
		while( itr.hasNext()) {
			synObject current = itr.next();
			current.setPercentage(allSum);
//			current.weightApply();
//			System.out.println("Synonym \""+ current.getEmoWord() + "\" percentage = " + current.getPercentage());
		} 
		*/
	}
	public static HashMap<String, Double> initialize_Pe(String patternFilePath) throws IOException {
		HashMap<String, Double> pe = new HashMap<String, Double>();
//		wtable = new TreeMap<String, synObject>();
//		TreeMap<String, String> classTable = buildClassTable("emo3785to6.txt");
		
		BufferedReader br;
		br = new BufferedReader(new InputStreamReader(new FileInputStream(patternFilePath), "UTF-8"));
		String line;
//		double allSum = 0.0;
		synObject obj = null;
		String currentEmoWord = null;
		while( (line=br.readLine())!= null ) {		
			if( line.contains("===================")) {
//				if( obj.getPatternTableSize() == 0.0) continue;
//				allSum += obj.getPatternCountSum();	//stats how many total pattern counts under this synonym
//				wtable.put(currentEmoWord, obj);
				continue;
			}
			if( line.matches("\\@[\\w-]+:")) {
				currentEmoWord = line.replace("@", "");
				currentEmoWord = currentEmoWord.replace(":", "");
//				obj = new synObject(currentEmoWord);
			}
			else {
				if( line.contains("??")) continue;	//invalid patterns
				pe.put(currentEmoWord, Double.valueOf(line));
//				obj.addPatterns(line);
				/*
				String category = classTable.get(currentEmoWord);
				if( category.equals("sadness") || category.equals("disgust"))	//include all patterns
					obj.addPatterns(line);
				else {
					if( !selfContainTest(line, currentEmoWord) ) //exclude self-containing patterns
						obj.addPatterns(line);
				} 
				*/
				
			}
		}
		br.close();
//		System.out.println("allSum = " + allSum);
		/*
		Iterator<synObject> itr = table.values().iterator();
		while( itr.hasNext()) {
			synObject current = itr.next();
			current.setPercentage(allSum);
//			current.weightApply();
//			System.out.println("Synonym \""+ current.getEmoWord() + "\" percentage = " + current.getPercentage());
		} 
		*/
		return pe;
	}
	public static ArrayList<String> listFilesForFolder(final File folder) {
		ArrayList<String> results = new ArrayList<String>();
	    for (final File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	            results.addAll(listFilesForFolder(fileEntry));
	        } else {
	            results.add(fileEntry.getName());
	        }
	    }
	    return results;
	}
	public static class ValueComparator implements Comparator<Map.Entry<String, Double>> {
		public int compare(Map.Entry<String, Double> mp1, Map.Entry<String, Double> mp2) {
			if (mp1.getValue() >= mp2.getValue())
				return -1;
			else
				return 1;
		}
	}
	/**** Begin of code not used. *****/
	/*
	public static void initialize() throws IOException {
		//HashMap<String, Integer> stats = new HashMap<String, Integer>();
		//System.out.println("Server first run. Initializing...");
		table = new HashMap<String, synObject>();
		final File folder = new File("emoWords" + File.separator );
		ArrayList<String> emotionLists = listFilesForFolder(folder);
		//System.out.println(emotionLists);
		Iterator<String> it = emotionLists.iterator();
		BufferedReader br;
		long allSum = 0;
		while(it.hasNext()) {
			String currentFile = it.next();
			String currentSyn = currentFile.replace(".txt", "");
			currentSyn = currentSyn.replaceAll("\\s+", "");
			if( currentFile.equals("Allpatterns.txt") || currentFile.equals(".DS_Store") || currentFile.equals("desktop.ini")) continue;
			br = new BufferedReader(new FileReader("emoWords" + File.separator + currentFile));
			synObject obj = new synObject(currentSyn);
			String line;
			while ((line = br.readLine()) != null) {
				obj.addPatterns(line);			
			}
			br.close();
			allSum += obj.getPatternCountSum();
			if( obj.getPatternTableSize() == 0) continue;
			table.put(currentSyn, obj);
		}
		Iterator<synObject> itr = table.values().iterator();
		while( itr.hasNext()) {
			synObject current = itr.next();
			current.setPercentage(allSum);
		}
	}
	public static ArrayList<String> getEmotionList(String path) throws IOException {
		ArrayList<String> results = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"));
		String line;
		while( (line = br.readLine())!= null) {
			if( line.length() < 1 ) continue;
			results.add(line);
		}
		br.close();
		return results;
	}
	*/
	/**** End of code not used. ****/
}
class mergedList{
	public String pattern;
	public ArrayList<String> matchedPattern;
	
	public mergedList(String input, ArrayList<String> inList) {
		pattern = input;
		matchedPattern = inList;
	}
	public ArrayList<String >getMatchedPattern() {
		return matchedPattern;
	}
	public String toString () {
		return (pattern + " = " + matchedPattern.toString());
	}
}
class synResult {
	public String synonymWord;
	public double percentage;
//	public TreeMap<String, ArrayList<patternEntry>> patternCountResults;
	public ArrayList<matchedPatternEntry> patternCountResults;
	public ArrayList<matchedPatternProbability> patternProbabilityResults;
//	public ArrayList<matchedPattern> patternList;
	public TreeMap<String, Double> patterns;
	public synResult(String word, double odds, TreeMap<String, Double> patternTable, ArrayList<matchedPatternEntry> table) {
		synonymWord = word;
		percentage = odds;
		patterns = patternTable;
		patternCountResults = table;
	}
	public void weightingCount() {
		for(int i=0; i<patternCountResults.size(); i++) {
			matchedPatternEntry current = patternCountResults.get(i);
//			String currentKey = current.getKey();
			ArrayList<patternEntry> currentPatterns = current.getValue();
			for(int j=0; j<currentPatterns.size(); j++) {
				double weightedValue = currentPatterns.get(j).getValue()*(1/percentage);
				currentPatterns.get(j).setValue(weightedValue);
			}
			current.setValue(currentPatterns);
			patternCountResults.set(i, current);
		}
	}
	public String getSynonymWord() {	return synonymWord; }
	public ArrayList<matchedPatternEntry> getTable(){ return patternCountResults; }
	public ArrayList<matchedPatternProbability> getPTable() { return patternProbabilityResults; }
	public ArrayList<ArrayList<Double>> getValueSet() {
//		System.out.println("SynonymWord = " + synonymWord + " percentage = " + percentage );
		ArrayList<ArrayList<Double>> results = new ArrayList<ArrayList<Double>>();
		Iterator<matchedPatternEntry> it = patternCountResults.iterator();
		while( it.hasNext()) {
			matchedPatternEntry current = it.next();
			ArrayList<patternEntry> currentList = current.getValue();
			ArrayList<Double> currentValues = new ArrayList<Double>();
			for(int i=0; i<currentList.size(); i++)
				currentValues.add((double)currentList.get(i).getValue());
			results.add(currentValues);
		}
		return results;
	}
	public void setProbability(ArrayList<ArrayList<Double>> sum) {
		patternProbabilityResults = new ArrayList<matchedPatternProbability>();
		Iterator<matchedPatternEntry> it = patternCountResults.iterator();
		int index = 0;
		while(it.hasNext()) {
			matchedPatternEntry current = it.next();
			ArrayList<patternEntry> currentPatterns = current.getValue();
			ArrayList<patternProbability> currentProbability = new ArrayList<patternProbability>();
			for(int i=0; i<currentPatterns.size(); i++) {
				double probabilityValue = ((double)(currentPatterns.get(i).getValue() ) * ((double)1 / (double) sum.get(index).get(i)));
				currentProbability.add(new patternProbability(currentPatterns.get(i).getKey(), probabilityValue));
			}
			index++;
			patternProbabilityResults.add(new matchedPatternProbability(current.getKey(), currentProbability));
		}
	}
	
	public void setProbabilityDice(TreeMap<String,Double> patternStat, TreeMap<String,Double> emoStat, String emo) {
		patternProbabilityResults = new ArrayList<matchedPatternProbability>();
		Iterator<matchedPatternEntry> it = patternCountResults.iterator();
		int index = 0;
		while(it.hasNext()) {
			matchedPatternEntry current = it.next();
			ArrayList<patternEntry> currentPatterns = current.getValue();
			ArrayList<patternProbability> currentProbability = new ArrayList<patternProbability>();
			for(int i=0; i<currentPatterns.size(); i++) {
				
//				double probabilityValue = ((double)(currentPatterns.get(i).getValue() ) * ((double)1 / (double) sum.get(index).get(i)));
				double weighted = Math.log((double)(currentPatterns.get(i).getValue())) / Math.log((double)2);
//				System.out.println(currentPatterns.get(i).getKey());
//				System.out.println(patternStat.get(currentPatterns.get(i).getKey()));
//				System.out.println(emo);
				double emonumber=0.0;
				if (emoStat.containsKey(emo))
					emonumber=emoStat.get(emo);
//				System.out.println(emonumber);
//				System.out.println(currentPatterns.get(i).getValue());
				double dice = (double)2 * (double)(currentPatterns.get(i).getValue()) / (patternStat.get(currentPatterns.get(i).getKey()) + emonumber);
				double probabilityValue =  weighted * dice;
				
				currentProbability.add(new patternProbability(currentPatterns.get(i).getKey(), probabilityValue));
			}
			index++;
			patternProbabilityResults.add(new matchedPatternProbability(current.getKey(), currentProbability));
		}
	}
	public void setProbabilityPMI(TreeMap<String,Double> patternStat, TreeMap<String,Double> emoStat, String emo) {
		patternProbabilityResults = new ArrayList<matchedPatternProbability>();
		Iterator<matchedPatternEntry> it = patternCountResults.iterator();
		int index = 0;
		while(it.hasNext()) {
			matchedPatternEntry current = it.next();
			ArrayList<patternEntry> currentPatterns = current.getValue();
			ArrayList<patternProbability> currentProbability = new ArrayList<patternProbability>();
			for(int i=0; i<currentPatterns.size(); i++) {
				
//				double probabilityValue = ((double)(currentPatterns.get(i).getValue() ) * ((double)1 / (double) sum.get(index).get(i)));
//				System.out.println(currentPatterns.get(i).getKey());
//				System.out.println(patternStat.get(currentPatterns.get(i).getKey()));
//				System.out.println(emo);
				double emonumber=0.0;
				if (emoStat.containsKey(emo))
					emonumber=emoStat.get(emo);
//				System.out.println(emonumber);
//				System.out.println(currentPatterns.get(i).getValue());
				double probabilityValue = (double)(currentPatterns.get(i).getValue()) / (patternStat.get(currentPatterns.get(i).getKey()) + emonumber);
				currentProbability.add(new patternProbability(currentPatterns.get(i).getKey(), probabilityValue));
			}
			index++;
			patternProbabilityResults.add(new matchedPatternProbability(current.getKey(), currentProbability));
		}
	}
	public void setProbabilityTFIDF(TreeMap<String,Double> patternStat, TreeMap<String,Double> emoStat, String emo, ArrayList<ArrayList<Double>> sum, ArrayList<ArrayList<Double>> IDF) {
		patternProbabilityResults = new ArrayList<matchedPatternProbability>();
		Iterator<matchedPatternEntry> it = patternCountResults.iterator();
		int index = 0;
		while(it.hasNext()) {
			matchedPatternEntry current = it.next();
			ArrayList<patternEntry> currentPatterns = current.getValue();
			ArrayList<patternProbability> currentProbability = new ArrayList<patternProbability>();
			for(int i=0; i<currentPatterns.size(); i++) {
				
//				double probabilityValue = ((double)(currentPatterns.get(i).getValue() ) * ((double)1 / (double) sum.get(index).get(i)));
//				System.out.println(currentPatterns.get(i).getKey());
//				System.out.println(patternStat.get(currentPatterns.get(i).getKey()));
//				System.out.println(emo);
				double emonumber=0.0;
				if (emoStat.containsKey(emo))
					emonumber=emoStat.get(emo);
//				System.out.println(emonumber);
//				System.out.println(currentPatterns.get(i).getValue());
				double TF = ((double)(currentPatterns.get(i).getValue() ) * ((double)1 / (double) sum.get(index).get(i)));
				double logIDF = Math.log((double) IDF.get(index).get(i));
				double probabilityValue = TF * logIDF;
				currentProbability.add(new patternProbability(currentPatterns.get(i).getKey(), probabilityValue));
			}
			index++;
			patternProbabilityResults.add(new matchedPatternProbability(current.getKey(), currentProbability));
		}
	}
	public void penalizedReFormProbability(boolean sumUpOne) { 	//this must be called before patternReduce()
		for(int i=0; i<patternCountResults.size(); i++) {
			matchedPatternProbability currentProbability = patternProbabilityResults.get(i);
			matchedPatternEntry currentPattern = patternCountResults.get(i);
			String inputPattern = currentPattern.getKey();
			ArrayList<patternEntry> matchedList = currentPattern.getValue();
			ArrayList<patternProbability> matchedProbabilities = currentProbability.getValue();
			if( sumUpOne) {
				double penaltySum = 0.0;
				for(int j=0; j<matchedList.size(); j++) {
					String currentExamingPattern = matchedList.get(j).getKey();
					twoDType minObj = SimilarityCalculationDemo.patternLED(inputPattern, currentExamingPattern);
					double penalty = penaltyFunction(minObj.getWeightedLength());
					penaltySum += penalty;
				}
				for(int j=0; j<matchedProbabilities.size(); j++) {
					String currentExamingPattern = matchedList.get(j).getKey();
					twoDType minObj = SimilarityCalculationDemo.patternLED(inputPattern, currentExamingPattern);
					patternProbability currentCalculatingProbability = matchedProbabilities.get(j);
					double original = currentCalculatingProbability.getValue();
					double penalty = penaltyFunction(minObj.getWeightedLength());
					penalty /= penaltySum;
					double adjustedValue = original*penalty;
					currentCalculatingProbability.setValue(adjustedValue);
					matchedProbabilities.set(j, currentCalculatingProbability);
				}
				
			}
			else {
				for(int j=0; j<matchedProbabilities.size(); j++) {
					String currentExamingPattern = matchedList.get(j).getKey();
					twoDType minObj = SimilarityCalculationDemo.patternLED(inputPattern, currentExamingPattern);
					twoDType ivtminObj = SimilarityCalculationDemo.patternLED(currentExamingPattern, inputPattern);
					patternProbability currentCalculatingProbability = matchedProbabilities.get(j);
					double original = currentCalculatingProbability.getValue();
//					double penalty = penaltyFunction(minObj.getWeightedLength()) / penaltyFunction(ivtminObj.getWeightedLength());
					double penalty = penaltyFunction(minObj.getWeightedLength());
					currentCalculatingProbability.setValue(original*penalty);
					matchedProbabilities.set(j, currentCalculatingProbability);
				}
			}
		}
	}
	public void penalizedProbability(boolean sumUpOne) { 	//this must be called before patternReduce()
		for(int i=0; i<patternCountResults.size(); i++) {
			matchedPatternProbability currentProbability = patternProbabilityResults.get(i);
			matchedPatternEntry currentPattern = patternCountResults.get(i);
			String inputPattern = currentPattern.getKey();
			ArrayList<patternEntry> matchedList = currentPattern.getValue();
			ArrayList<patternProbability> matchedProbabilities = currentProbability.getValue();
			if( sumUpOne) {
				double penaltySum = 0.0;
				for(int j=0; j<matchedList.size(); j++) {
					String currentExamingPattern = matchedList.get(j).getKey();
					twoDType minObj = SimilarityCalculationDemo.patternLED(inputPattern, currentExamingPattern);
					double penalty = penaltyFunction(minObj.getWeightedLength());
					penaltySum += penalty;
				}
				for(int j=0; j<matchedProbabilities.size(); j++) {
					String currentExamingPattern = matchedList.get(j).getKey();
					twoDType minObj = SimilarityCalculationDemo.patternLED(inputPattern, currentExamingPattern);
					patternProbability currentCalculatingProbability = matchedProbabilities.get(j);
					double original = currentCalculatingProbability.getValue();
					double penalty = penaltyFunction(minObj.getWeightedLength());
					penalty /= penaltySum;
					double adjustedValue = original*penalty;
					currentCalculatingProbability.setValue(adjustedValue);
					matchedProbabilities.set(j, currentCalculatingProbability);
				}
				
			}
			else {
				for(int j=0; j<matchedProbabilities.size(); j++) {
					String currentExamingPattern = matchedList.get(j).getKey();
					twoDType minObj = SimilarityCalculationDemo.patternLED(inputPattern, currentExamingPattern);
					patternProbability currentCalculatingProbability = matchedProbabilities.get(j);
					double original = currentCalculatingProbability.getValue();
					double penalty = penaltyFunction(minObj.getWeightedLength());
					currentCalculatingProbability.setValue(original*penalty);
					matchedProbabilities.set(j, currentCalculatingProbability);
				}
			}
		}
	}
	public void patternReduce() {
		Iterator<matchedPatternProbability> it = patternProbabilityResults.iterator();
		while(it.hasNext()) {
			matchedPatternProbability current = it.next();
			ArrayList<patternProbability> currentProbability = current.getValue();
			if( currentProbability.size()>1) {
				Collections.sort(currentProbability, new Comparator<patternProbability>() {
			        @Override
			        public int compare(final patternProbability object1, final patternProbability object2) {
			            return Double.valueOf(object1.getValue()).compareTo(Double.valueOf(object2.getValue()) );
			        }
			       } 
				);
				patternProbability maxLeft = currentProbability.get(currentProbability.size()-1);
				//penguin0430 note: find non-matched pattern
				double maxP = currentProbability.get(0).getValue();
				double minP = currentProbability.get(currentProbability.size()-1).getValue();
				//penguin0430
				currentProbability.clear();
				
				//penguin0430
				if (maxP==minP){
					//current.setValue
					patternProbability emptyPattern = new patternProbability("no matched pattern",0.0);
					currentProbability.add(emptyPattern);
				}
				else{
					currentProbability.add(maxLeft);
				}		
				current.setValue(currentProbability);
			}
		}
	}
	public void patternTakeLog() {
		Iterator<matchedPatternProbability> it = patternProbabilityResults.iterator();
		while(it.hasNext()) {
			matchedPatternProbability current = it.next();
			ArrayList<patternProbability> currentProbability = current.getValue();
			for(int i=0; i<currentProbability.size(); i++) {
				double rawValue = currentProbability.get(i).getValue();
//				rawValue += 1.0;
				if( rawValue == 0.0 ) {
//					rawValue = ((double)1/(double)3785);
					currentProbability.get(i).setValue(-99.0);
				}
				else
					currentProbability.get(i).setValue(Math.log(rawValue));
			}
			
			current.setValue(currentProbability);
		}
	}
	public double getSynonymScore() {
		double score = 0.0;
		Iterator<matchedPatternProbability> it = patternProbabilityResults.iterator();
		while(it.hasNext()) {
			matchedPatternProbability current = it.next();
			ArrayList<patternProbability> currentProbability = current.getValue();
			for(int i=0; i<currentProbability.size(); i++) {
				score += currentProbability.get(i).getValue();
			}
		}
		return score;
	}
	public double getWeightSynonymScore(HashMap<String, Double> weight) {
		double score = 0.0;
		Iterator<matchedPatternProbability> it = patternProbabilityResults.iterator();
		while(it.hasNext()) {
			matchedPatternProbability current = it.next();
			ArrayList<patternProbability> currentProbability = current.getValue();
			for(int i=0; i<currentProbability.size(); i++) {
				score += currentProbability.get(i).getValue() * weight.get(current.getKey());
//				System.out.println("weight: " + current.getKey() + " : " + weight.get(current.getKey()));
			}
		}
		return score;
	}
	public ArrayList<String> getPatternListByName(String inputPatternName) {
		ArrayList<String> results = new ArrayList<String>();
		int inputPatternIndex = patternCountResults.indexOf(new matchedPatternEntry(inputPatternName, null));
		ArrayList<patternEntry> thisResult = patternCountResults.get(inputPatternIndex).getValue();
		for(int i=0; i<thisResult.size(); i++)
			results.add(thisResult.get(i).getKey());
		return results;
	}
	public ArrayList<String> getPatternListByIndex(int inputPatternIndex) {
		ArrayList<String> results = new ArrayList<String>();
		ArrayList<patternEntry> thisResult = patternCountResults.get(inputPatternIndex).getValue();
		for(int i=0; i<thisResult.size(); i++)
			results.add(thisResult.get(i).getKey());
		return results;
	}
	public void tableClean() {
		Iterator<matchedPatternEntry> it = patternCountResults.iterator();
//		ArrayList<String> toBeDeleted = new ArrayList<String>();
		ArrayList<Integer> deleteList = new ArrayList<Integer>();
		int index = 0;
		while(it.hasNext()) {
			matchedPatternEntry currentEntry = it.next();
			if( currentEntry.getValue().isEmpty()) {
//				toBeDeleted.add(currentEntry.getKey());
				deleteList.add(index);
			}
			index++;
		}
		for(int i=deleteList.size()-1; i>=0; i--)
			patternCountResults.remove((int)deleteList.get(i));
	}
	public double penaltyFunction(double length) {
		return (Math.exp(-length));
	}
	public ArrayList<String> getPatternKeySet() {
		ArrayList<String> results = new ArrayList<String>();
	
		for(int i=0; i<patternCountResults.size(); i++) {
			results.add(patternCountResults.get(i).getKey());
		}
		return results;
	}
	public void updateMyPatternCountResults(ArrayList<mergedList> patternMerged, boolean doRegex) {
		for(int i=0; i<patternCountResults.size(); i++) {
			matchedPatternEntry current = patternCountResults.get(i);
			ArrayList<patternEntry> currentEntry = current.getValue();
//			patternCountResults.remove(i);
			
			ArrayList<String> patternMergedList = patternMerged.get(i).getMatchedPattern();
			for(int j=0; j<patternMergedList.size(); j++) {
				String currentFPattern = patternMergedList.get(j);
				//go through each matching patterns.
//				Double fillValue = PatternStats.zeroPatterns.get(currentFPattern);
//				if( fillValue == null)
//					System.out.println("Alert!");
				patternEntry fillEntry = new patternEntry(currentFPattern, 0);
//				patternEntry fillEntry = new patternEntry(currentFPattern, fillValue);
				if( currentEntry.contains(fillEntry) );
					//do nothing if this pattern is matched under this synonym;
				else {
					if( !doRegex && patterns.containsKey(currentFPattern)) {
//						String inputPattern = current.getKey();
//						twoDType minObj = SimilarityCalculationDemo.patternLED(inputPattern, currentFPattern);
//						double penalizedScore = patterns.get(currentFPattern)*penaltyFunction(minObj.getWeightedLength());
						double penalizedScore = patterns.get(currentFPattern);
						currentEntry.add(new patternEntry(currentFPattern, penalizedScore));
					}
					else {
						//if this pattern is not matched, new this pattern but set its normalized count as 0
						currentEntry.add(fillEntry);
					}
				}
			}
			if( currentEntry.size()>1) {	//sorting the updated matched-list alphabetically
				Collections.sort(currentEntry, new Comparator<patternEntry>() {
			        @Override
			        public int compare(final patternEntry object1, final patternEntry object2) {
			            return object1.getKey().compareTo(object2.getKey());
			        }
			       } 
				);
			}
			current.setValue(currentEntry);
			current.addOne();
			patternCountResults.set(i, current);
			//update new table entry
		}
	}
	
	/**** Begin of code not used. ****/
	/*
	public ArrayList<Double> getPatternListValueByName(String inputPatternName) {
	ArrayList<Double> results = new ArrayList<Double>();
	ArrayList<patternEntry> thisResult = patternCountResults.get(inputPatternName);
	for(int i=0; i<thisResult.size(); i++)
		results.add(thisResult.get(i).getValue());
	return results;
	}
	*/
	/*** End of code not used. ***/
}
class clausePatternResult {
	public String clausePattern;
	public ArrayList<String> matchedPatternMerged;
	public clausePatternResult(String clause, ArrayList<String> matched) {
		clausePattern = clause;
		matchedPatternMerged = matched;
	}
	public void printMyPatterns() {
		System.out.println(clausePattern + ":" + matchedPatternMerged);
	}
}
class matchedPattern{
	public String pattern;
	public double relevance;
	public double probability;
	public matchedPattern(String inPattern, double inRelv, double inProb) {
		pattern = inPattern;
		relevance = inRelv;
		probability = inProb;
	}
	public String toString () {
		return String.format("%s = %10.5f (%10.5f)", pattern, relevance, probability);
//		return (pattern + "=" + probability);
	}
}
class patternProbability{
	public String pattern;
	public double probability;
	public patternProbability(String input, double odds) {
		pattern = input;
		probability = odds;
	}
	public String getKey() { return pattern; }
	public double getValue() { return probability; }
	public void setValue(double value) {
		probability = value;
	}
	public String toString () {
		return String.format("%s = %10.5f", pattern, probability);
//		return (pattern + "=" + probability);
	}
	@Override
	public boolean equals(Object object) {
		boolean sameSame = false;

        if (object != null && object instanceof patternProbability)
        {
            sameSame = this.pattern.equals(((patternProbability) object).pattern);
        }

        return sameSame;
	}
}
class matchedPatternProbability{
	public String pattern;
	public ArrayList<patternProbability> matchedList;
	public matchedPatternProbability(String input, ArrayList<patternProbability> inputList) {
		pattern = input;
		matchedList = inputList;
	}
	public void setValue(ArrayList<patternProbability> inputList) {
		matchedList = inputList;
	}
	public String getKey() { return pattern; }
	public ArrayList<patternProbability> getValue() { return matchedList; }
	public String toString () {
//		return String.format("%s = %10.5f", pattern, occurrence);
		return (pattern + " = " + matchedList);
	}
//	@Override
//	public boolean equals(Object object) {
//		boolean sameSame = false;
//
//        if (object != null && object instanceof patternEntry)
//        {
//            sameSame = this.pattern.equals(((patternEntry) object).pattern);
//        }
//
//        return sameSame;
//	}
}
class patternEntry{
	public String pattern;
	public double occurrence;
	public patternEntry(String input, double count) {
		pattern = input;
		occurrence = count;
	}
	public void setValue(double value) {
		occurrence = value;
	}
	public String getKey() { return pattern; }
	public double getValue() { return occurrence; }
	public void addOne() { occurrence++; }
	public String toString () {
		return String.format("%s = %10.5f", pattern, occurrence);
	}
	@Override
	public boolean equals(Object object) {
		boolean sameSame = false;

        if (object != null && object instanceof patternEntry)
        {
            sameSame = this.pattern.equals(((patternEntry) object).pattern);
        }

        return sameSame;
	}
}
class matchedPatternEntry{
	public String pattern;
	public ArrayList<patternEntry> matchedList;
	public matchedPatternEntry(String input, ArrayList<patternEntry> inputList) {
		pattern = input;
		matchedList = inputList;
	}
	public void setValue(ArrayList<patternEntry> inputList) {
		matchedList = inputList;
	}
	public String getKey() { return pattern; }
	public ArrayList<patternEntry> getValue() { return matchedList; }
	public String toString () {
//		return String.format("%s = %10.5f", pattern, occurrence);
		return (pattern + " = " + matchedList);
	}
	public void addOne() {
		for(int i=0; i<matchedList.size(); i++)
			matchedList.get(i).addOne();
	}
	@Override
	public boolean equals(Object object) {
		boolean sameSame = false;

        if (object != null && object instanceof patternEntry)
        {
            sameSame = this.pattern.equals(((patternEntry) object).pattern);
        }

        return sameSame;
	}
}
class synObject implements Serializable {
/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//	public final int DELTA = 0.75;
	public String emoWord;
	public TreeMap<String, Double> patterns;
	public double patternCountSum;
	public double percentage;
	public synObject(String word) {
		emoWord = word;
		patterns = new TreeMap<String, Double>();
	}
	public int getPatternTableSize() { return patterns.size(); }
	public double getPatternCountSum() {
		double result = 0;
		Iterator<Entry<String, Double>> it = patterns.entrySet().iterator();
		while( it.hasNext()) {
			Entry<String, Double> current = it.next();
			result += current.getValue();
		}
		patternCountSum = result;
		return result;
	}
	public TreeMap<String, Double> getPatternTable() { return patterns; }
	public void weightApply() {
		Iterator<Entry<String, Double>> it = patterns.entrySet().iterator();
		while( it.hasNext()) {
			Entry<String, Double> current = it.next();
			String currentKey = current.getKey();
			Double currentValue = current.getValue() * (1/percentage);
			patterns.put(currentKey, currentValue);
		}
	}
	public void setPercentage(double allSum) {
		percentage = (double)patternCountSum/(double)allSum;
	}
	public double getPercentage() { return percentage; }
	public String getEmoWord() { return emoWord; }
	public void addPatterns(String line) {
//		System.out.println(line);
		String pattern = line.split("\t")[0];
		double occur = Double.valueOf(line.split("\t")[1]);
		patterns.put(pattern, occur);
	}
	public void addPatternsCumm(String line) {
		String pattern = line.split("\t")[0];
		double occur = Double.valueOf(line.split("\t")[1]);
		if( !patterns.containsKey(pattern))
			patterns.put(pattern, occur);
		else {
			double previousOccur = patterns.get(pattern);
			patterns.put(pattern, occur + previousOccur );
		}
	}

	public String getRootWord(String line) {
		String[] tokens = line.split(" ");
		String result = null;
		for(int i=0; i<tokens.length; i++) {
			if( tokens[i].contains("_ROOT"))
				result = tokens[i].replace("_ROOT", ""); 
		}
		return result;
	} 
	public void generalization() {
		ArrayList<Entry<String, Double>> patternList = new ArrayList<Entry<String, Double>>(patterns.entrySet());
		for(int i=0; i<patternList.size(); i++) {
			String currentPattern = patternList.get(i).getKey();
			Double currentPatternValue = 0.0;
			ArrayList<patternEntry> currentMatched = getMatchedPatterns(currentPattern);
			currentPatternValue = sumPatternValues(currentMatched);
			patterns.put(currentPattern, currentPatternValue);
		}
	}
	public Double sumPatternValues(ArrayList<patternEntry> currentMatched) {
		Double sum = 0.0;
		for(int i=0; i<currentMatched.size(); i++)
			sum += currentMatched.get(i).getValue();
		return sum;
	}
	public ArrayList<patternEntry> getMatchedPatterns(String inPattern) {	
		ArrayList<patternEntry> matchedPatternList = new ArrayList<patternEntry>();
		ArrayList<String> matchedInfo = new ArrayList<String>();
		ArrayList<Entry<String, Double>> patternList = new ArrayList<Entry<String, Double>>(patterns.entrySet());
		if( inPattern.contains("=<")) {
			String genKey = inPattern.replaceAll("\\w+=", "");
			String oriKey = inPattern.replaceAll("=<\\w+>", "");
			String[] allPatterns = {genKey, oriKey};
			double currentMin = 999.0;
			for(int i=0; i<allPatterns.length; i++) {
				Iterator<Entry<String, Double>> it = patternList.iterator();
				String rootWord = getRootWord(allPatterns[i]);
				String pattern = allPatterns[i].replace("_ROOT", "");
				while(it.hasNext()) {
					Entry<String, Double> currentPattern = it.next();
					String currentExaminingPattern = currentPattern.getKey();
					String currentExaminingPatternRoot = SimilarityCalculationDemo.getRootWord(currentExaminingPattern);
					currentExaminingPattern = currentExaminingPattern.replace("_ROOT", "");
					if( rootWord == null || !rootWord.equals(currentExaminingPatternRoot)) continue;
					twoDType minObj = SimilarityCalculationDemo.patternLED(pattern, currentExaminingPattern);
		    		if( minObj.getWeightedLength() < currentMin) {
		    			currentMin = minObj.getWeightedLength();
		    			matchedPatternList.clear();
		    			matchedPatternList.add(new patternEntry(currentPattern.getKey(), currentPattern.getValue()) );
		    			matchedInfo.clear();
		    			matchedInfo.add(currentPattern.getKey() + " = " + minObj.toString());
		    		}
		    		else if( minObj.getWeightedLength() == currentMin){
		    			patternEntry toBeAdded = new patternEntry(currentPattern.getKey(), currentPattern.getValue());
		    			if(!matchedPatternList.contains(toBeAdded)) {
		    				matchedPatternList.add(toBeAdded );
		    				matchedInfo.add(currentPattern.getKey() + " = " + minObj.toString());
		    			}
		    		}
		    		else {
		    			; //do nothing
		    		}
				}
			}
		}
		else {
			Iterator<Entry<String, Double>> it = patternList.iterator();
			double currentMin = 999.0;
	    	String rootWord = getRootWord(inPattern);
	    	String pattern = inPattern.replace("_ROOT", "");
			while(it.hasNext()) {
				Entry<String, Double> currentPattern = it.next();
				String currentExaminingPattern = currentPattern.getKey();
				String currentExaminingPatternRoot = SimilarityCalculationDemo.getRootWord(currentExaminingPattern);
				currentExaminingPattern = currentExaminingPattern.replace("_ROOT", "");
				if( rootWord == null || !rootWord.equals(currentExaminingPatternRoot)) continue;
				twoDType minObj = SimilarityCalculationDemo.patternLED(pattern, currentExaminingPattern);
	    		if( minObj.getWeightedLength() < currentMin) {
	    			currentMin = minObj.getWeightedLength();
	    			matchedPatternList.clear();
	    			matchedPatternList.add(new patternEntry(currentPattern.getKey(), currentPattern.getValue()) );
	    			matchedInfo.clear();
	    			matchedInfo.add(currentPattern.getKey() + " = " + minObj.toString());
	    		}
	    		else if( minObj.getWeightedLength() == currentMin){
	    			matchedPatternList.add(new patternEntry(currentPattern.getKey(), currentPattern.getValue()) );
	    			matchedInfo.add(currentPattern.getKey() + " = " + minObj.toString());
	    		}
	    		else {
	    			; //do nothing
	    		}
			}
		}
		return matchedPatternList;
	}
	public ArrayList<patternEntry> getMatchedPatterns_PEN(String inPattern) {	
		ArrayList<patternEntry> matchedPatternList = new ArrayList<patternEntry>();
		ArrayList<String> matchedInfo = new ArrayList<String>();
		ArrayList<Entry<String, Double>> patternList = new ArrayList<Entry<String, Double>>(patterns.entrySet());
		if( inPattern.contains("=<")) {
			String[] tokens = inPattern.split(" ");
			String genKey="";
			for(int i=0; i<tokens.length;i++) {
			  if(tokens[i].contains("=<")) {
				  tokens[i]=tokens[i].replace(tokens[i].split("=")[0] + "=", "");
			  }
			}
			for(int i=0; i<tokens.length; i++) {
				genKey += tokens[i] + " ";
			}
			String oriKey = inPattern.replaceAll("=<\\w+>", "");
			String[] allPatterns = {genKey, oriKey};
			double currentMin = 999.0;
			for(int i=0; i<allPatterns.length; i++) {
				Iterator<Entry<String, Double>> it = patternList.iterator();
				String rootWord = getRootWord(allPatterns[i]);
				String pattern = allPatterns[i].replace("_ROOT", "");
				while(it.hasNext()) {
					Entry<String, Double> currentPattern = it.next();
					String currentExaminingPattern = currentPattern.getKey();
					String currentExaminingPatternRoot = SimilarityCalculationDemo.getRootWord(currentExaminingPattern);
					currentExaminingPattern = currentExaminingPattern.replace("_ROOT", "");
					if( rootWord == null || !rootWord.equals(currentExaminingPatternRoot)) continue;
					twoDType minObj = SimilarityCalculationDemo.patternLED(pattern, currentExaminingPattern);
		    		if( minObj.getWeightedLength() < currentMin) {
		    			currentMin = minObj.getWeightedLength();
		    			matchedPatternList.clear();
		    			matchedPatternList.add(new patternEntry(currentPattern.getKey(), currentPattern.getValue()) );
		    			matchedInfo.clear();
		    			matchedInfo.add(currentPattern.getKey() + " = " + minObj.toString());
		    		}
		    		else if( minObj.getWeightedLength() == currentMin){
		    			patternEntry toBeAdded = new patternEntry(currentPattern.getKey(), currentPattern.getValue());
		    			if(!matchedPatternList.contains(toBeAdded)) {
		    				matchedPatternList.add(toBeAdded );
		    				matchedInfo.add(currentPattern.getKey() + " = " + minObj.toString());
		    			}
		    		}
		    		else {
		    			; //do nothing
		    		}
				}
			}
		}
		else {
			Iterator<Entry<String, Double>> it = patternList.iterator();
			double currentMin = 999.0;
	    	String rootWord = getRootWord(inPattern);
	    	String pattern = inPattern.replace("_ROOT", "");
			while(it.hasNext()) {
				Entry<String, Double> currentPattern = it.next();
				String currentExaminingPattern = currentPattern.getKey();
				String currentExaminingPatternRoot = SimilarityCalculationDemo.getRootWord(currentExaminingPattern);
				currentExaminingPattern = currentExaminingPattern.replace("_ROOT", "");
				if( rootWord == null || !rootWord.equals(currentExaminingPatternRoot)) continue;
				twoDType minObj = SimilarityCalculationDemo.patternLED(pattern, currentExaminingPattern);
	    		if( minObj.getWeightedLength() < currentMin) {
	    			currentMin = minObj.getWeightedLength();
	    			matchedPatternList.clear();
	    			matchedPatternList.add(new patternEntry(currentPattern.getKey(), currentPattern.getValue()) );
	    			matchedInfo.clear();
	    			matchedInfo.add(currentPattern.getKey() + " = " + minObj.toString());
	    		}
	    		else if( minObj.getWeightedLength() == currentMin){
	    			matchedPatternList.add(new patternEntry(currentPattern.getKey(), currentPattern.getValue()) );
	    			matchedInfo.add(currentPattern.getKey() + " = " + minObj.toString());
	    		}
	    		else {
	    			; //do nothing
	    		}
			}
		}
		return matchedPatternList;
	}
	public ArrayList<patternEntry> getMatchedPatterns_N_E(String inPattern,boolean useE, boolean useN) {	
		ArrayList<patternEntry> matchedPatternList = new ArrayList<patternEntry>();
		ArrayList<String> matchedInfo = new ArrayList<String>();
		ArrayList<Entry<String, Double>> patternList = new ArrayList<Entry<String, Double>>(patterns.entrySet());
		if( inPattern.contains("=<")) {
			String genKey = inPattern.replaceAll("\\w+=", "");
			String oriKey = inPattern.replaceAll("=<\\w+>", "");
			String[] allPatterns = {genKey, oriKey};
			double currentMin = 999.0;
			for(int i=0; i<allPatterns.length; i++) {
				Iterator<Entry<String, Double>> it = patternList.iterator();
				String rootWord = getRootWord(allPatterns[i]);
				String pattern = allPatterns[i].replace("_ROOT", "");
				while(it.hasNext()) {
					Entry<String, Double> currentPattern = it.next();
					String currentExaminingPattern = currentPattern.getKey();
					String currentExaminingPatternRoot = SimilarityCalculationDemo.getRootWord(currentExaminingPattern);
					currentExaminingPattern = currentExaminingPattern.replace("_ROOT", "");
					if(useE||useN) {
						String tokens[]=allPatterns[i].split(" ");
//						System.out.println("get N or E !: "+ allPatterns[i]);
						if( (tokens.length==1) && (rootWord==null)) {
							//pure N or E pattern
							//do nothing 
							System.out.println("get N or E: "+ allPatterns[i]);
						}
					}
					else {
						if( rootWord == null || !rootWord.equals(currentExaminingPatternRoot)) continue;
					}
					twoDType minObj = SimilarityCalculationDemo.patternLED(pattern, currentExaminingPattern);
		    		if( minObj.getWeightedLength() < currentMin) {
		    			currentMin = minObj.getWeightedLength();
		    			matchedPatternList.clear();
		    			matchedPatternList.add(new patternEntry(currentPattern.getKey(), currentPattern.getValue()) );
		    			matchedInfo.clear();
		    			matchedInfo.add(currentPattern.getKey() + " = " + minObj.toString());
		    		}
		    		else if( minObj.getWeightedLength() == currentMin){
		    			patternEntry toBeAdded = new patternEntry(currentPattern.getKey(), currentPattern.getValue());
		    			if(!matchedPatternList.contains(toBeAdded)) {
		    				matchedPatternList.add(toBeAdded );
		    				matchedInfo.add(currentPattern.getKey() + " = " + minObj.toString());
		    			}
		    		}
		    		else {
		    			; //do nothing
		    		}
				}
			}
		}
		else {
			Iterator<Entry<String, Double>> it = patternList.iterator();
			double currentMin = 999.0;
	    	String rootWord = getRootWord(inPattern);
	    	String pattern = inPattern.replace("_ROOT", "");
			while(it.hasNext()) {
				Entry<String, Double> currentPattern = it.next();
				String currentExaminingPattern = currentPattern.getKey();
				String currentExaminingPatternRoot = SimilarityCalculationDemo.getRootWord(currentExaminingPattern);
				currentExaminingPattern = currentExaminingPattern.replace("_ROOT", "");
				if(useE||useN) {
					String tokens[]=inPattern.split(" ");
//					System.out.println("get N or E !: "+ allPatterns[i]);
					if( (tokens.length==1) && (rootWord==null)) {
						//pure N or E pattern
						//do nothing 
//						System.out.println("get N or E: "+ inPattern);
					}
				}
				else {
					if( rootWord == null || !rootWord.equals(currentExaminingPatternRoot)) continue;
				}
				twoDType minObj = SimilarityCalculationDemo.patternLED(pattern, currentExaminingPattern);
	    		if( minObj.getWeightedLength() < currentMin) {
	    			currentMin = minObj.getWeightedLength();
	    			matchedPatternList.clear();
	    			matchedPatternList.add(new patternEntry(currentPattern.getKey(), currentPattern.getValue()) );
	    			matchedInfo.clear();
	    			matchedInfo.add(currentPattern.getKey() + " = " + minObj.toString());
	    		}
	    		else if( minObj.getWeightedLength() == currentMin){
	    			matchedPatternList.add(new patternEntry(currentPattern.getKey(), currentPattern.getValue()) );
	    			matchedInfo.add(currentPattern.getKey() + " = " + minObj.toString());
	    		}
	    		else {
	    			; //do nothing
	    		}
			}
		}
		
		return matchedPatternList;
	}
	public ArrayList<patternEntry> getMatchedPatterns_N_Eonly(String inPattern,boolean useE, boolean useN) {	
		ArrayList<patternEntry> matchedPatternList = new ArrayList<patternEntry>();
		ArrayList<String> matchedInfo = new ArrayList<String>();
		ArrayList<Entry<String, Double>> patternList = new ArrayList<Entry<String, Double>>(patterns.entrySet());
//		if( inPattern.contains("=<")) {
//			String genKey = inPattern.replaceAll("\\w+=", "");
//			String oriKey = inPattern.replaceAll("=<\\w+>", "");
//			String[] allPatterns = {genKey, oriKey};
//			double currentMin = 999.0;
//			for(int i=0; i<allPatterns.length; i++) {
//				Iterator<Entry<String, Double>> it = patternList.iterator();
//				String rootWord = getRootWord(allPatterns[i]);
//				String pattern = allPatterns[i].replace("_ROOT", "");
//				while(it.hasNext()) {
//					Entry<String, Double> currentPattern = it.next();
//					String currentExaminingPattern = currentPattern.getKey();
//					String currentExaminingPatternRoot = SimilarityCalculationDemo.getRootWord(currentExaminingPattern);
//					currentExaminingPattern = currentExaminingPattern.replace("_ROOT", "");
//					if(useE||useN) {
//						String tokens[]=allPatterns[i].split(" ");
////						System.out.println("get N or E !: "+ allPatterns[i]);
//						if( (tokens.length==1) && (rootWord==null)) {
//							//pure N or E pattern
//							//do nothing 
//							System.out.println("get N or E: "+ allPatterns[i]);
//						}
//					}
//					else {
//						if( rootWord == null || !rootWord.equals(currentExaminingPatternRoot)) continue;
//					}
//					twoDType minObj = SimilarityCalculationDemo.patternLED(pattern, currentExaminingPattern);
//		    		if( minObj.getWeightedLength() < currentMin) {
//		    			currentMin = minObj.getWeightedLength();
//		    			matchedPatternList.clear();
//		    			matchedPatternList.add(new patternEntry(currentPattern.getKey(), currentPattern.getValue()) );
//		    			matchedInfo.clear();
//		    			matchedInfo.add(currentPattern.getKey() + " = " + minObj.toString());
//		    		}
//		    		else if( minObj.getWeightedLength() == currentMin){
//		    			patternEntry toBeAdded = new patternEntry(currentPattern.getKey(), currentPattern.getValue());
//		    			if(!matchedPatternList.contains(toBeAdded)) {
//		    				matchedPatternList.add(toBeAdded );
//		    				matchedInfo.add(currentPattern.getKey() + " = " + minObj.toString());
//		    			}
//		    		}
//		    		else {
//		    			; //do nothing
//		    		}
//				}
//			}
//		}
//		else {
			Iterator<Entry<String, Double>> it = patternList.iterator();
			double currentMin = 999.0;
	    	String rootWord = getRootWord(inPattern);
	    	String pattern = inPattern.replace("_ROOT", "");
			while(it.hasNext()) {
				Entry<String, Double> currentPattern = it.next();
				String currentExaminingPattern = currentPattern.getKey();
				String currentExaminingPatternRoot = SimilarityCalculationDemo.getRootWord(currentExaminingPattern);
				currentExaminingPattern = currentExaminingPattern.replace("_ROOT", "");
				if(useE||useN) {
					String tokens[]=inPattern.split(" ");
//					System.out.println("get N or E !: "+ allPatterns[i]);
					if( (tokens.length==1) && (rootWord==null)) {
						//pure N or E pattern
						//do nothing 
//						System.out.println("get N or E: "+ inPattern);
					}
				}
				else {
					if( rootWord == null || !rootWord.equals(currentExaminingPatternRoot)) continue;
				}
				if(pattern.equals(currentExaminingPattern)) {
					matchedPatternList.clear();
	    			matchedPatternList.add(new patternEntry(currentPattern.getKey(), currentPattern.getValue()) );
	    			matchedInfo.clear();
	    			matchedInfo.add(currentPattern.getKey() + " = " + currentExaminingPattern);
				}
//				
//				
//				twoDType minObj = SimilarityCalculationDemo.patternLED(pattern, currentExaminingPattern);
//	    		if( minObj.getWeightedLength() < currentMin) {
//	    			currentMin = minObj.getWeightedLength();
//	    			matchedPatternList.clear();
//	    			matchedPatternList.add(new patternEntry(currentPattern.getKey(), currentPattern.getValue()) );
//	    			matchedInfo.clear();
//	    			matchedInfo.add(currentPattern.getKey() + " = " + minObj.toString());
//	    		}
//	    		else if( minObj.getWeightedLength() == currentMin){
//	    			matchedPatternList.add(new patternEntry(currentPattern.getKey(), currentPattern.getValue()) );
//	    			matchedInfo.add(currentPattern.getKey() + " = " + minObj.toString());
//	    		}
//	    		else {
//	    			; //do nothing
//	    		}
			}
//		}
		
		return matchedPatternList;
	}
	public ArrayList<patternEntry> getMatchedWords(String inPattern) {	
		ArrayList<patternEntry> matchedPatternList = new ArrayList<patternEntry>();
		ArrayList<String> matchedInfo = new ArrayList<String>();
		ArrayList<Entry<String, Double>> patternList = new ArrayList<Entry<String, Double>>(patterns.entrySet());
		Iterator<Entry<String, Double>> it = patternList.iterator();
//		double currentMin = 999.0;
//    	String rootWord = getRootWord(inPattern);
//    	String pattern = inPattern.replace("_ROOT", "");
		while(it.hasNext()) {
			Entry<String, Double> currentPattern = it.next();
			String currentExaminingPattern = currentPattern.getKey();
//			String currentExaminingPatternRoot = SimilarityCalculationDemo.getRootWord(currentExaminingPattern);
//			currentExaminingPattern = currentExaminingPattern.replace("_ROOT", "");
//			if( rootWord == null || !rootWord.equals(currentExaminingPatternRoot)) continue;
//			twoDType minObj = SimilarityCalculationDemo.patternLED(pattern, currentExaminingPattern);
//			System.out.println(currentExaminingPattern);
			if(inPattern.equals(currentExaminingPattern)) {
				matchedPatternList.add(new patternEntry(currentPattern.getKey(), currentPattern.getValue()) );
				matchedInfo.add(currentPattern.getKey() + " = " + currentPattern.getKey().toString());
//				System.out.println(currentPattern.getKey() + " = " + currentPattern.getKey().toString());
			}
//			
//			twoDType minObj = SimilarityCalculationDemo.wordLED(inPattern, currentExaminingPattern);
//    		if( minObj.getWeightedLength() < currentMin) {
//    			currentMin = minObj.getWeightedLength();
//    			matchedPatternList.clear();
//    			matchedPatternList.add(new patternEntry(currentPattern.getKey(), currentPattern.getValue()) );
//    			matchedInfo.clear();
//    			matchedInfo.add(currentPattern.getKey() + " = " + minObj.toString());
//    		}
//    		else if( minObj.getWeightedLength() == currentMin){
//    			matchedPatternList.add(new patternEntry(currentPattern.getKey(), currentPattern.getValue()) );
//    			matchedInfo.add(currentPattern.getKey() + " = " + minObj.toString());
//    		}
//    		else {
//    			; //do nothing
//    		}
		}
		return matchedPatternList;
	}
	public double penaltyFunction(double length) {
		return (Math.exp(-length));
	}
	public boolean samePolarity(String pat1, String pat2) {
		return (hasNeg(pat1) == hasNeg(pat2));
	}
	public boolean hasNeg(String pattern) {
		String[] tokens = pattern.split(" ");
		for(int i=0; i<tokens.length; i++) {
			if( tokens[i].equals("not") || tokens[i].equals("never"))
				return true;
		}
		return false;
	}
	public ArrayList<matchedPatternEntry> getAllMatchedClausePatterns(ArrayList<String> inputClausePatterns, boolean doRegex) {
		//Using user's input patterns to look for similar patterns under this synonym
		//It's in a hash-table format where each user pattern maps to a list of most similar (synonym) patterns.
//		TreeMap<String, ArrayList<patternEntry>> results = new TreeMap<String, ArrayList<patternEntry>>();
		ArrayList<matchedPatternEntry> matchedResults = new ArrayList<matchedPatternEntry>();
		for(int i=0; i<inputClausePatterns.size(); i++) {	//go through each user pattern, find a list of most similar patterns (in object "patternEntry")
			if(doRegex) {
//				results.put(inputClausePatterns.get(i), getMatchedPatterns_regex(inputClausePatterns.get(i), doRegex));
				matchedResults.add(new matchedPatternEntry(inputClausePatterns.get(i), getMatchedPatterns_regex(inputClausePatterns.get(i))));
			}
			else {
//				results.put(inputClausePatterns.get(i), getMatchedPatterns(inputClausePatterns.get(i)) );
				matchedResults.add(new matchedPatternEntry(inputClausePatterns.get(i), getMatchedPatterns(inputClausePatterns.get(i))) );
			}
		}
		return matchedResults;
	}
	public ArrayList<matchedPatternEntry> getAllMatchedClausePatterns_PEN(ArrayList<String> inputClausePatterns, boolean doRegex) {
		//Using user's input patterns to look for similar patterns under this synonym
		//It's in a hash-table format where each user pattern maps to a list of most similar (synonym) patterns.
//		TreeMap<String, ArrayList<patternEntry>> results = new TreeMap<String, ArrayList<patternEntry>>();
		ArrayList<matchedPatternEntry> matchedResults = new ArrayList<matchedPatternEntry>();
		for(int i=0; i<inputClausePatterns.size(); i++) {	//go through each user pattern, find a list of most similar patterns (in object "patternEntry")
			if(doRegex) {
//				results.put(inputClausePatterns.get(i), getMatchedPatterns_regex(inputClausePatterns.get(i), doRegex));
				matchedResults.add(new matchedPatternEntry(inputClausePatterns.get(i), getMatchedPatterns_regex(inputClausePatterns.get(i))));
			}
			else {
//				results.put(inputClausePatterns.get(i), getMatchedPatterns(inputClausePatterns.get(i)) );
//				matchedResults.add(new matchedPatternEntry(inputClausePatterns.get(i), getMatchedPatterns(inputClausePatterns.get(i))) );
				matchedResults.add(new matchedPatternEntry(inputClausePatterns.get(i), getMatchedPatterns_PEN(inputClausePatterns.get(i))) );
			}
		}
		return matchedResults;
	}
	public ArrayList<matchedPatternEntry> getAllMatchedClausePatterns_N_E(ArrayList<String> inputClausePatterns, boolean doRegex,boolean useE, boolean useN) {
		//Using user's input patterns to look for similar patterns under this synonym
		//It's in a hash-table format where each user pattern maps to a list of most similar (synonym) patterns.
//		TreeMap<String, ArrayList<patternEntry>> results = new TreeMap<String, ArrayList<patternEntry>>();
		ArrayList<matchedPatternEntry> matchedResults = new ArrayList<matchedPatternEntry>();
		for(int i=0; i<inputClausePatterns.size(); i++) {	//go through each user pattern, find a list of most similar patterns (in object "patternEntry")
			if(doRegex) {
//				results.put(inputClausePatterns.get(i), getMatchedPatterns_regex(inputClausePatterns.get(i), doRegex));
				matchedResults.add(new matchedPatternEntry(inputClausePatterns.get(i), getMatchedPatterns_regex(inputClausePatterns.get(i))));
			}
			else {
//				results.put(inputClausePatterns.get(i), getMatchedPatterns(inputClausePatterns.get(i)) );
				matchedResults.add(new matchedPatternEntry(inputClausePatterns.get(i), getMatchedPatterns_N_E(inputClausePatterns.get(i),useE,useN)) );
			}
		}
		return matchedResults;
	}
	public ArrayList<matchedPatternEntry> getAllMatchedClausePatterns_N_Eonly(ArrayList<String> inputClausePatterns, boolean doRegex,boolean useE, boolean useN) {
		//Using user's input patterns to look for similar patterns under this synonym
		//It's in a hash-table format where each user pattern maps to a list of most similar (synonym) patterns.
//		TreeMap<String, ArrayList<patternEntry>> results = new TreeMap<String, ArrayList<patternEntry>>();
		ArrayList<matchedPatternEntry> matchedResults = new ArrayList<matchedPatternEntry>();
		for(int i=0; i<inputClausePatterns.size(); i++) {	//go through each user pattern, find a list of most similar patterns (in object "patternEntry")
			if(doRegex) {
//				results.put(inputClausePatterns.get(i), getMatchedPatterns_regex(inputClausePatterns.get(i), doRegex));
				matchedResults.add(new matchedPatternEntry(inputClausePatterns.get(i), getMatchedPatterns_regex(inputClausePatterns.get(i))));
			}
			else {
//				results.put(inputClausePatterns.get(i), getMatchedPatterns(inputClausePatterns.get(i)) );
				matchedResults.add(new matchedPatternEntry(inputClausePatterns.get(i), getMatchedPatterns_N_Eonly(inputClausePatterns.get(i),useE,useN)) );
			}
		}
		return matchedResults;
	}
	public ArrayList<matchedPatternEntry> getAllMatchedClauseWords(ArrayList<String> inputClausePatterns, boolean doRegex) {
		//Using user's input patterns to look for similar patterns under this synonym
		//It's in a hash-table format where each user pattern maps to a list of most similar (synonym) patterns.
//		TreeMap<String, ArrayList<patternEntry>> results = new TreeMap<String, ArrayList<patternEntry>>();
		ArrayList<matchedPatternEntry> matchedResults = new ArrayList<matchedPatternEntry>();
		for(int i=0; i<inputClausePatterns.size(); i++) {	//go through each user pattern, find a list of most similar patterns (in object "patternEntry")
//			if(doRegex) {
////				results.put(inputClausePatterns.get(i), getMatchedPatterns_regex(inputClausePatterns.get(i), doRegex));
//				matchedResults.add(new matchedPatternEntry(inputClausePatterns.get(i), getMatchedPatterns_regex(inputClausePatterns.get(i))));
//			}
//			else {
//				results.put(inputClausePatterns.get(i), getMatchedPatterns(inputClausePatterns.get(i)) );
				matchedResults.add(new matchedPatternEntry(inputClausePatterns.get(i), getMatchedWords(inputClausePatterns.get(i))) );
//			}
		}
		return matchedResults;
	}
	
	/****** Begin of code not used. *******/
//	/*
	public void printHash() {
		Iterator it = patterns.entrySet().iterator();
		while(it.hasNext())
			System.out.println(it.next().toString());
	}
	public boolean isMatched(String line, String pattern) {
//		String lineRootWord = getRootWord(line);
//		String patternRootWord = getRootWord(pattern);
//		line = line.replace("_ROOT", "");
		String localPatternCopy = pattern.replace("<", "");
		localPatternCopy = localPatternCopy.replace(">", "");
		String wildCard = "([\\w\\d\\*]+\\s{1})+";
		String regExpression = "";
		regExpression += "([\\w\\d\\*]+\\s{1})*";
		regExpression += (localPatternCopy.replace("* ", wildCard));
		regExpression += ".*";
//		System.out.println("RegExpression = " + regExpression);
//		System.out.println(line.matches(regExpression));
		return line.matches(regExpression);
	}
	public ArrayList<patternEntry> getMatchedPatterns_regex(String input) {
		ArrayList<patternEntry> matchedPatternList = new ArrayList<patternEntry>();
		ArrayList<Entry<String, Double>> patternList = new ArrayList<Entry<String, Double>>(patterns.entrySet());
		Iterator<Entry<String, Double>> it = patternList.iterator();
		while(it.hasNext()) {
			Entry<String, Double> currentPattern = it.next();
			if( !samePolarity(input, currentPattern.getKey())) continue;
			if( isMatched(input, currentPattern.getKey())) {
				matchedPatternList.add(new patternEntry(currentPattern.getKey(), currentPattern.getValue()));
			}
			else {
				
			}
		}
		return matchedPatternList;
	}
	public double getMyThresholdLength(String inPattern, double percentage) {
		String[] tokens = inPattern.split(" ");
		int rootCount = 0, normalCount = 0, wildCardCount = 0;
		for(int i=0; i<tokens.length; i++) {
			if( tokens[i].contains("_ROOT"))
				rootCount++;
			else if( tokens[i].equals("*"))
				wildCardCount++;
			else
				normalCount++;
		}
//		System.out.println("length = " + tokens.length + "; rootCount = " + rootCount + "; normalCount = " + normalCount + "; wildCardCount = " + wildCardCount);
		double reducedNormal = (double)normalCount*percentage;
		double wildCardCost = wildCardCount*Math.E;
		double threshold = Math.sqrt(reducedNormal*reducedNormal + wildCardCost*wildCardCost);
		return threshold;
	}
//	*/
	/*** End of code not used. ***/
}
class PatternLookUPThread implements Runnable {
	Socket clientSocket;
	PatternLookUPThread(Socket cs) {
		clientSocket = cs;
	}
	public void run() {
		try {
			System.out.println(Thread.currentThread().getId() + " begins...");
//			clientSocket.setSoTimeout(3*60*1000);
//			ObjectInputStream OIs = new ObjectInputStream(clientSocket.getInputStream());
			ObjectOutputStream OOs = new ObjectOutputStream(clientSocket.getOutputStream());
			BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
//			PrintStream Ps = new PrintStream(clientSocket.getOutputStream());
			String key;
			while( !clientSocket.isClosed() && ((key = br.readLine())!= null)) {
				synObject obj = PatternStats.table.get(key);
				if( obj != null) {
					OOs.writeObject(obj);
					OOs.flush();
				}
				else
					OOs.writeObject(null);
//				System.out.println("key = " + key +": " + value);
//				Ps.println(value);
			}
			System.out.println("Thread ID:" + Thread.currentThread().getId() + " - Client closed socket.");
		}catch(Exception e){
			e.printStackTrace( );
		} 
	}
}