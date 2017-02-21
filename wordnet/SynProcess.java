package wordnet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.io.Writer;
import java.net.InetAddress;
import java.text.BreakIterator;
import java.text.DecimalFormat;
import java.util.*;
import java.net.*;

import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.Tree;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.PointerType;
import net.didion.jwnl.data.relationship.Relationship;

import org.codehaus.jackson.JsonProcessingException;
import org.ejml.simple.*;
import org.paukov.combinatorics.*;

class SynProcess {
	public static HashMap stopWords = null;
	private static HashMap<String, String> emotionWords;

	private static String target;
	private static String[] emotionWordLists;
	private static double DELTA = 0.75;

	public static void main(String[] args) {

	}
//
    public static String pairGenerate(String word1, String word2) {
    	String result = "";
    	String[] temp = new String[2];
    	if( emotionWords.containsKey(word1) && !emotionWords.containsKey(word2))
    		result = word1 + "," + word2;
    	else if( !emotionWords.containsKey(word1) && emotionWords.containsKey(word2))
    		result = word2 + "," + word1;
    	else {
    		temp[0] = word1; temp[1] = word2;
    		Arrays.sort(temp);
    		result = temp[0] + "," + temp[1];
    	}
    	return result;
    }
	public static int getStatsData(PrintStream ps, BufferedReader br, String query) throws NumberFormatException, IOException {
		ps.println(query);
		String resultStr = br.readLine();
		int result = (resultStr.isEmpty()) ? (-1) : Integer.parseInt(resultStr);
		return result;
	}
	public static double getStatsDataDouble(PrintStream ps, BufferedReader br, String query) throws NumberFormatException, IOException {
		ps.println(query);
		String resultStr = br.readLine();
		double result = (resultStr.isEmpty()) ? (-1) : Double.parseDouble(resultStr);
		return result;
	}
    public static ArrayList<Map.Entry<String, Double>> process(String text, String synStr, String targetWord) throws JWNLException, JsonProcessingException, IOException {
		target = targetWord;
		getEmotionWordList("extendedList.txt");
		buildEmotionHash();
		HashMap<String, Double> similarityValues = new HashMap<String, Double>();
		
		InetAddress serverHost = InetAddress.getByName("localhost"); 
		int serverPort = 3456;
		Socket clientSocket = new Socket(serverHost, serverPort);
		InputStream Is = clientSocket.getInputStream();
		OutputStream Os = clientSocket.getOutputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(Is));
		PrintStream Ps = new PrintStream(Os);
		
		String[] syns = synStr.split(" ");

		// Extracting each synonym's gloss, remove stop words, and stemming
		// ArrayList<String> docs = new ArrayList<String>();
		stopWordInit();
		
		// Add the stemmed bag of words of the input text (as the last vector)
		String BOWtext = stopWordRemoval(text);
//		System.out.println(BOWtext);
		String stemmedText = textStemmer(BOWtext);

		String[] textTokens = stemmedText.split(" ");
//		Double[] relatedness = new Double[syns.length];
//		System.out.println("Text:\n" + stemmedText);
		for(int i=0; i<syns.length; i++) {
			double total = 0.0;
			int tempSynFreq = getStatsData(Ps, br, syns[i]);
			double synFreq = ( tempSynFreq == -1) ? 0.0 : (double)tempSynFreq;
//			System.out.println("On synonym \"" + syns[i] + "\"(" + synFreq + "):");
			for(int j=0; j<textTokens.length; j++) {
				int tempTokenFreq = getStatsData(Ps, br, textTokens[j]);
				double tokenFreq = (tempTokenFreq == -1) ? 0.0 : (double)tempTokenFreq;
				String pair = pairGenerate(syns[i], textTokens[j]);
				int tempCoFreq = getStatsData(Ps, br, pair);
				double coFreq = (tempCoFreq == -1) ? 0.0 : (double)tempCoFreq;
				double cooccur, PMI, NPMI;
//				// plus-2 smooth 
//				synFreq += 2.0;
//				tokenFreq += 2.0;
//				coFreq += 2.0;
				if(tokenFreq == 0.0 || coFreq == 0.0 || synFreq == 0.0) {
					cooccur = 0.0;
					PMI = 0.0;
					NPMI = 0.0;
				} 
					
				else {
					cooccur = coFreq/(synFreq*tokenFreq);
//					System.out.println("<" + pair + ">: " + coFreq + ".");
				    PMI = Math.log( cooccur );
//				    NPMI = PMI/(-1*Math.log(coFreq));
//				    System.out.println("PMI = " + PMI + "\t");
//				    System.out.print("-log(co-occur) = " + (-1*Math.log(coFreq)) + "\t");
//				    System.out.println("NPMI = " + NPMI);
				}
				total += cooccur;
			}
			total = Math.log(total);
			similarityValues.put(syns[i], total);
		}
		clientSocket.close();
		
		ArrayList<Map.Entry<String, Double>> similarityValueList = new ArrayList<Map.Entry<String, Double>>(similarityValues.size());
		similarityValueList.addAll(similarityValues.entrySet());
		ValueComparator vc = new ValueComparator();
		Collections.sort(similarityValueList, vc);

		return similarityValueList;
		
	}
//    
    public static ArrayList<Map.Entry<String, Double>> processPattern(String text, String synStr, String targetWord, LexicalizedParser lp, boolean usePPL) throws IOException, ClassNotFoundException, JWNLException {
    	ArrayList<String> patterns = PatternGenByDep.getPatternsFromText(text, targetWord, lp, usePPL, true);
    	ArrayList<String> synonyms = new ArrayList<String>(Arrays.asList(synStr.split(" ")));
//    	HashMap<String, Double> synonymScores = PatternStats.localTest(patterns, synonyms);
    	HashMap<String, Double> synonymScores = PatternStats.clientServerModeProcess(patterns, synonyms);
    	if( synonymScores == null) return null;
    	ArrayList<Map.Entry<String, Double>> synonymScoresSort = new ArrayList<Map.Entry<String, Double>>(synonymScores.size());
    	synonymScoresSort.addAll(synonymScores.entrySet());
    	Collections.sort(synonymScoresSort, new ValueComparator());
    	
    	return synonymScoresSort;
    }

    public static ArrayList<Map.Entry<String, Double>> processPatternLocalSpecify(String text, String synStr, String targetWord, LexicalizedParser lp, String settingPath, boolean doRegex, boolean usePPL) throws IOException, ClassNotFoundException, JWNLException {
    	ArrayList<String> patterns = PatternGenByDep.getPatternsFromText(text, targetWord, lp, usePPL, true);
    	ArrayList<String> synonyms = new ArrayList<String>(Arrays.asList(synStr.split(" ")));
//    	HashMap<String, Double> synonymScores = PatternStats.localTestSettingSpecify(patterns, synonyms, settingPath, doRegex);
    	HashMap<String, Double> synonymScores = PatternStats.localTestSettingSpecify(patterns, synonyms, settingPath, doRegex);
//    	HashMap<String, Double> synonymScores = PatternStats.clientServerModeProcess(patterns, synonyms);
    	if( synonymScores == null) return null;
    	ArrayList<Map.Entry<String, Double>> synonymScoresSort = new ArrayList<Map.Entry<String, Double>>(synonymScores.size());
    	synonymScoresSort.addAll(synonymScores.entrySet());
    	Collections.sort(synonymScoresSort, new ValueComparator());
    	
    	return synonymScoresSort;
    }
    public static ArrayList<Map.Entry<String, Double>> processPatternLocalSpecify_reForm(String text, String synStr, String targetWord, LexicalizedParser lp, String settingPath, String PeFile, boolean doRegex, boolean usePPL) throws IOException, ClassNotFoundException, JWNLException {
    	ArrayList<String> patterns = PatternGenByDep.getPatternsFromText(text, targetWord, lp, usePPL, true);
    	ArrayList<String> synonyms = new ArrayList<String>(Arrays.asList(synStr.split(" ")));
//    	HashMap<String, Double> synonymScores = PatternStats.localTestSettingSpecify(patterns, synonyms, settingPath, doRegex);
    	HashMap<String, Double> synonymScores = PatternStats.localTestSettingSpecify_reForm(patterns, synonyms, settingPath, PeFile, doRegex);
//    	HashMap<String, Double> synonymScores = PatternStats.clientServerModeProcess(patterns, synonyms);
    	if( synonymScores == null) return null;
    	ArrayList<Map.Entry<String, Double>> synonymScoresSort = new ArrayList<Map.Entry<String, Double>>(synonymScores.size());
    	synonymScoresSort.addAll(synonymScores.entrySet());
    	Collections.sort(synonymScoresSort, new ValueComparator());
    	
    	return synonymScoresSort;
    }
    public static ArrayList<Map.Entry<String, Double>> processPatternLocalSpecify_weight(String text, String synStr, String targetWord, LexicalizedParser lp, String settingPath, String weightFile, boolean doRegex, boolean usePPL) throws IOException, ClassNotFoundException, JWNLException {
    	ArrayList<String> oriStr = new ArrayList<String>();
    	ArrayList<String> patterns = PatternGenByDep.getPatternsFromText_weight(text, targetWord, lp, oriStr, usePPL, false);
    	ArrayList<String> synonyms = new ArrayList<String>(Arrays.asList(synStr.split(" ")));
//    	HashMap<String, Double> synonymScores = PatternStats.localTestSettingSpecify(patterns, synonyms, settingPath, doRegex);
    	HashMap<String, Double> synonymScores = PatternStats.localTestSettingSpecify_weight(patterns, oriStr, synonyms, settingPath, weightFile, doRegex);
//    	HashMap<String, Double> synonymScores = PatternStats.clientServerModeProcess(patterns, synonyms);
    	if( synonymScores == null) return null;
    	ArrayList<Map.Entry<String, Double>> synonymScoresSort = new ArrayList<Map.Entry<String, Double>>(synonymScores.size());
    	synonymScoresSort.addAll(synonymScores.entrySet());
    	Collections.sort(synonymScoresSort, new ValueComparator());
    	
    	return synonymScoresSort;
    }
    public static ArrayList<Map.Entry<String, Double>> processPatternLocalSpecify_N_E(String text, String synStr, String targetWord, LexicalizedParser lp, String settingPath, boolean doRegex, boolean usePPL,boolean useE, boolean useN) throws IOException, ClassNotFoundException, JWNLException {
    	ArrayList<String> patterns = PatternGenByDep.getPatternsFromText_N_E(text, targetWord, lp, usePPL,useE, useN);
    	ArrayList<String> synonyms = new ArrayList<String>(Arrays.asList(synStr.split(" ")));
    	HashMap<String, Double> synonymScores = PatternStats.localTestSettingSpecify_N_E(patterns, synonyms, settingPath, doRegex,useE,useN);
//    	HashMap<String, Double> synonymScores = PatternStats.clientServerModeProcess(patterns, synonyms);
    	if( synonymScores == null) return null;
    	ArrayList<Map.Entry<String, Double>> synonymScoresSort = new ArrayList<Map.Entry<String, Double>>(synonymScores.size());
    	synonymScoresSort.addAll(synonymScores.entrySet());
    	Collections.sort(synonymScoresSort, new ValueComparator());
    	
    	return synonymScoresSort;
    }
    public static ArrayList<Map.Entry<String, Double>> processPatternLocalSpecify_N_E_add(String text, String synStr, String targetWord, LexicalizedParser lp, String settingPath, boolean doRegex, boolean usePPL,boolean useE, boolean useN) throws IOException, ClassNotFoundException, JWNLException {
    	ArrayList<String> patterns = PatternGenByDep.getPatternsFromText(text, targetWord, lp, usePPL, true);
    	System.out.println("Patterns: " + patterns.size() + " : " + patterns);
    	ArrayList<String> emotions = PatternGenByDep.getEmotionsFromText(text, targetWord, lp, usePPL);
    	System.out.println("Emotions: " + emotions.size() + " : " + emotions);
    	ArrayList<String> nouns = PatternGenByDep.getNounsFromText(text, targetWord, lp, usePPL);
    	System.out.println("Nouns: " + nouns.size() + " : " + nouns);
    	
    	ArrayList<String> synonyms = new ArrayList<String>(Arrays.asList(synStr.split(" ")));
    	HashMap<String, Double> synonymScoresP = PatternStats.localTestSettingSpecify(patterns, synonyms, settingPath, doRegex);
    	HashMap<String, Double> synonymScoresE = PatternStats.localTestSettingSpecify_N_Eonly(emotions, synonyms, settingPath, doRegex,useE,useN);
    	HashMap<String, Double> synonymScoresN = PatternStats.localTestSettingSpecify_N_Eonly(nouns, synonyms, settingPath, doRegex,useE,useN);
    	//    	HashMap<String, Double> synonymScores = PatternStats.clientServerModeProcess(patterns, synonyms);
    	if( synonymScoresP == null || synonymScoresE == null || synonymScoresN == null) return null;
    	HashMap<String, Double> synonymScores = mergedScore(synonymScoresP,synonymScoresE,synonymScoresN, patterns.size(), emotions.size(), nouns.size()); 
    	ArrayList<Map.Entry<String, Double>> synonymScoresSort = new ArrayList<Map.Entry<String, Double>>(synonymScores.size());
    	synonymScoresSort.addAll(synonymScores.entrySet());
    	Collections.sort(synonymScoresSort, new ValueComparator());
    	
    	return synonymScoresSort;
    }
    public static ArrayList<Map.Entry<String, Double>> processPatternLocalSpecify_N_E_add_opp(String currentFile, String text, String synStr, String targetWord, LexicalizedParser lp, String settingPath, boolean doRegex, boolean usePPL,boolean useE, boolean useN) throws IOException, ClassNotFoundException, JWNLException {
    	ArrayList<String> patterns = PatternGenByDep.getPatternsFromText(text, targetWord, lp, usePPL, false);
//    	System.out.println("Patterns: " + patterns.size() + " : " + patterns);
//    	ArrayList<String> emotions = PatternGenByDep.getEmotionsFromText(text, targetWord, lp, usePPL);
//    	System.out.println("Emotions: " + emotions.size() + " : " + emotions);
//    	ArrayList<String> nouns = PatternGenByDep.getNounsFromText(text, targetWord, lp, usePPL);
//    	System.out.println("Nouns: " + nouns.size() + " : " + nouns);
    	
    	ArrayList<String> synonyms = new ArrayList<String>(Arrays.asList(synStr.split(" ")));
    	HashMap<String, Double> synonymScores = PatternStats.localTestSettingSpecify_opp(currentFile, patterns, synonyms, settingPath, doRegex);
//    	HashMap<String, Double> synonymScoresE = PatternStats.localTestSettingSpecify_N_Eonly_opp(currentFile, emotions, synonyms, settingPath, doRegex,useE,useN);
//    	HashMap<String, Double> synonymScoresN = PatternStats.localTestSettingSpecify_N_Eonly_opp(currentFile, nouns, synonyms, settingPath, doRegex,useE,useN);
    	//    	HashMap<String, Double> synonymScores = PatternStats.clientServerModeProcess(patterns, synonyms);
//    	if( synonymScoresP == null || synonymScoresE == null || synonymScoresN == null) return null;
//    	HashMap<String, Double> synonymScores = mergedScore(synonymScoresP,synonymScoresE,synonymScoresN, patterns.size(), emotions.size(), nouns.size()); 
//    	ArrayList<Map.Entry<String, Double>> synonymScoresSort = new ArrayList<Map.Entry<String, Double>>(synonymScores.size());
//    	synonymScoresSort.addAll(synonymScores.entrySet());
//    	Collections.sort(synonymScoresSort, new ValueComparator());
    	if( synonymScores == null) return null;
    	ArrayList<Map.Entry<String, Double>> synonymScoresSort = new ArrayList<Map.Entry<String, Double>>(synonymScores.size());
    	synonymScoresSort.addAll(synonymScores.entrySet());
    	Collections.sort(synonymScoresSort, new ValueComparator());
    	
    	return synonymScoresSort;
    }
/*
    public static ArrayList<Map.Entry<String, Double>> processPatternLocalSpecify_N_E_add_rm(String text, String synStr, String targetWord, LexicalizedParser lp, String settingPath, boolean doRegex, boolean usePPL,boolean useE, boolean useN) throws IOException, ClassNotFoundException, JWNLException {
    	ArrayList<String> patterns = PatternGenByDep.getPatternsFromText(text, targetWord, lp, usePPL);
    	System.out.println("Patterns: " + patterns.size() + " : " + patterns);   	
    	ArrayList<String> synonyms = new ArrayList<String>(Arrays.asList(synStr.split(" ")));
    	
    	HashMap<String, Double> synonymScoresP = PatternStats.localTestSettingSpecify_PEN(patterns, synonyms, settingPath, doRegex);   	//    	HashMap<String, Double> synonymScores = PatternStats.clientServerModeProcess(patterns, synonyms);
    	if( synonymScoresP == null) return null;
    	HashMap<String, Double> synonymScores = mergedScore(synonymScoresP,synonymScoresE,synonymScoresN, patterns.size(), emotions.size(), nouns.size()); 
    	ArrayList<Map.Entry<String, Double>> synonymScoresSort = new ArrayList<Map.Entry<String, Double>>(synonymScores.size());
    	synonymScoresSort.addAll(synonymScores.entrySet());
    	Collections.sort(synonymScoresSort, new ValueComparator());
    	
    	return synonymScoresSort;
    }
    */
    public static ArrayList<Map.Entry<String, Double>> processPatternLocalSpecifyWord(String text, String synStr, String targetWord, LexicalizedParser lp, String settingPath, boolean doRegex, boolean usePPL) throws IOException, ClassNotFoundException, JWNLException {
    	getEmotionWordList("extendedList.txt");
		buildEmotionHash();
//    	ArrayList<String> patterns = PatternGenByDep.getPatternsFromText(text, targetWord, lp, usePPL);
    	ArrayList<String> patterns = PatternGenByDep.getWordsFromText(text, targetWord, lp, usePPL);
//    	System.out.println(patterns);
    	ArrayList<String> synonyms = new ArrayList<String>(Arrays.asList(synStr.split(" ")));
//    	HashMap<String, Double> synonymScores = PatternStats.localTestSettingSpecify(patterns, synonyms, settingPath, doRegex);
    	HashMap<String, Double> synonymScores = PatternStats.localTestSettingSpecifyWord(patterns, synonyms, settingPath, doRegex);
//    	HashMap<String, Double> synonymScores = PatternStats.clientServerModeProcess(patterns, synonyms);
    	if( synonymScores == null) return null;
    	ArrayList<Map.Entry<String, Double>> synonymScoresSort = new ArrayList<Map.Entry<String, Double>>(synonymScores.size());
    	synonymScoresSort.addAll(synonymScores.entrySet());
    	Collections.sort(synonymScoresSort, new ValueComparator());
    	
    	return synonymScoresSort;
    }
    public static ArrayList<Map.Entry<String, Double>> processPatternLocalSpecifyDice(String text, String synStr, String targetWord, LexicalizedParser lp, String settingPath, boolean doRegex, boolean usePPL, TreeMap<String,Double> patternStat, TreeMap<String,Double> emoStat) throws IOException, ClassNotFoundException, JWNLException {
    	ArrayList<String> patterns = PatternGenByDep.getPatternsFromText(text, targetWord, lp, usePPL, true);
    	ArrayList<String> synonyms = new ArrayList<String>(Arrays.asList(synStr.split(" ")));
//    	HashMap<String, Double> synonymScores = PatternStats.localTestSettingSpecify(patterns, synonyms, settingPath, doRegex);
    	HashMap<String, Double> synonymScores = PatternStats.localTestSettingSpecifyDice(patterns, synonyms, settingPath, doRegex, patternStat, emoStat);
//    	HashMap<String, Double> synonymScores = PatternStats.clientServerModeProcess(patterns, synonyms);
    	if( synonymScores == null) return null;
    	ArrayList<Map.Entry<String, Double>> synonymScoresSort = new ArrayList<Map.Entry<String, Double>>(synonymScores.size());
    	synonymScoresSort.addAll(synonymScores.entrySet());
    	Collections.sort(synonymScoresSort, new ValueComparator());
    	
    	return synonymScoresSort;
    }
    public static ArrayList<Map.Entry<String, Double>> processPatternLocalSpecifyPMI(String text, String synStr, String targetWord, LexicalizedParser lp, String settingPath, boolean doRegex, boolean usePPL, TreeMap<String,Double> patternStat, TreeMap<String,Double> emoStat) throws IOException, ClassNotFoundException, JWNLException {
    	ArrayList<String> patterns = PatternGenByDep.getPatternsFromText(text, targetWord, lp, usePPL, true);
    	ArrayList<String> synonyms = new ArrayList<String>(Arrays.asList(synStr.split(" ")));
//    	HashMap<String, Double> synonymScores = PatternStats.localTestSettingSpecify(patterns, synonyms, settingPath, doRegex);
    	HashMap<String, Double> synonymScores = PatternStats.localTestSettingSpecifyPMI(patterns, synonyms, settingPath, doRegex, patternStat, emoStat);
//    	HashMap<String, Double> synonymScores = PatternStats.clientServerModeProcess(patterns, synonyms);
    	if( synonymScores == null) return null;
    	ArrayList<Map.Entry<String, Double>> synonymScoresSort = new ArrayList<Map.Entry<String, Double>>(synonymScores.size());
    	synonymScoresSort.addAll(synonymScores.entrySet());
    	Collections.sort(synonymScoresSort, new ValueComparator());
    	
    	return synonymScoresSort;
    }
    public static ArrayList<Map.Entry<String, Double>> processPatternLocalSpecifyTFIDF(String text, String synStr, String targetWord, LexicalizedParser lp, String settingPath, boolean doRegex, boolean usePPL, TreeMap<String,Double> patternStat, TreeMap<String,Double> emoStat) throws IOException, ClassNotFoundException, JWNLException {
    	ArrayList<String> patterns = PatternGenByDep.getPatternsFromText(text, targetWord, lp, usePPL, true);
    	ArrayList<String> synonyms = new ArrayList<String>(Arrays.asList(synStr.split(" ")));
//    	HashMap<String, Double> synonymScores = PatternStats.localTestSettingSpecify(patterns, synonyms, settingPath, doRegex);
    	HashMap<String, Double> synonymScores = PatternStats.localTestSettingSpecifyTFIDF(patterns, synonyms, settingPath, doRegex, patternStat, emoStat);
//    	HashMap<String, Double> synonymScores = PatternStats.clientServerModeProcess(patterns, synonyms);
    	if( synonymScores == null) return null;
    	ArrayList<Map.Entry<String, Double>> synonymScoresSort = new ArrayList<Map.Entry<String, Double>>(synonymScores.size());
    	synonymScoresSort.addAll(synonymScores.entrySet());
    	Collections.sort(synonymScoresSort, new ValueComparator());
    	
    	return synonymScoresSort;
    }
    public static HashMap<String, Double> mergedScore(HashMap<String, Double> pScore, HashMap<String, Double> eScore, HashMap<String, Double> nScore, int pSize, int eSize, int nSize){
    	HashMap<String, Double> scoreMap = new HashMap<String, Double>();
    	for(Map.Entry<String, Double> entry : pScore.entrySet()) {
    		String key = entry.getKey();
    		Double value = entry.getValue()/(double)pSize + eScore.get(key)/(double)eSize + nScore.get(key)/(double)nSize;
    		System.out.println("synonym: " + key);
    		System.out.println("ScoreP/E/N: " + entry.getValue() + ", "+ eScore.get(key) + ", " + nScore.get(key));
    		scoreMap.put(key, value);
    	}
    	return scoreMap;
    }
	public static class ValueComparator implements Comparator<Map.Entry<String, Double>> {
		public int compare(Map.Entry<String, Double> mp1, Map.Entry<String, Double> mp2) {
			if (mp1.getValue() >= mp2.getValue())
				return -1;
			else
				return 1;
		}
	}
 	
	public static void stopWordInit() {
		String stopWordListPath = "stopwords.txt";
		stopWords = new HashMap();
		try {
			BufferedReader br = new BufferedReader(new FileReader(
					stopWordListPath));
			String word;
			while ((word = br.readLine()) != null) {
				stopWords.put(word, word);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
//	public static void getEmotionWordList() {
//		WordNetAffectDic.initialize();
//		emotionWordLists = WordNetAffectDic.outputEmotionWords();
////		for(int i=0; i<emotionWordLists.length; i++)
////			emotionWordLists[i] = emotionWordLists[i].replace("_", " ");
//	}
	public static void getEmotionWordList(String path) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"));
//		WordNetAffectDic.initialize();
		ArrayList<String> emoWords = new ArrayList<String>();
		String line;
		while( (line = br.readLine()) != null) {
			if( line.length() < 1) continue;
			emoWords.add(line);
		}
		emotionWordLists = new String[emoWords.size()];
		emoWords.toArray(emotionWordLists);
	}
	public static void printEmotionWordList() {
		for(int i=0; i<emotionWordLists.length;i++)
//			if(emotionWordLists[i].contains("_"))
				System.out.println(emotionWordLists[i]);
	}
	public static String[] keepEmotionExpression(String[] inTokens) {	
		String reconstruct = "";
		for(int i=0; i<inTokens.length; i++)
			reconstruct += (inTokens[i].toLowerCase() + " ");
		for(int i=0; i<emotionWordLists.length; i++) {
			String temp = emotionWordLists[i].replace("_", " "); //so that we can do the substring match
			if(reconstruct.contains(temp) ) {
				reconstruct = reconstruct.replace(temp, emotionWordLists[i]);
			}
		}
		String[] result = reconstruct.split(" ");
		return result;
		
	}
	public static int idxOfNextSpace(String[] inString, int begin) {
		int end = -1;
		for(int i=begin; i<inString.length; i++) {
			if( inString[i].matches("[a-zA-Z]+") || inString[i].equals("-"));
			else
			{
				end = i;
				break;
			}
		}
		return end;
	}
	public static String createWord(String[] inString, int begin, int end) {
		String result = "";
		for(int i=begin; i<end; i++)
			result += inString[i];
		return result;
	}
	public static String[] filterPunctuation(String[] inTokens) {
		List<String> tempList = new ArrayList<String>();
		for(int i=0; i<inTokens.length; i++) {
			if( inTokens[i].matches("[a-zA-Z]+")) {
				String temp = "";
				int nextSpace = idxOfNextSpace(inTokens, i);
				if( (nextSpace-i) > 2) {
					temp = createWord(inTokens, i, nextSpace);
					tempList.add(temp);
					i = nextSpace-1;
				}
				else
					tempList.add(inTokens[i]);
					
			}
			/*
			if( (i+2 < inTokens.length) && inTokens[i+1].equals("-") && inTokens[i].matches("[a-zA-z]+") && inTokens[i+2].matches("[a-zA-z]+"))
			{
				String temp = inTokens[i] + inTokens[i+1] + inTokens[i+2];
				tempList.add(temp);
				i+=2;
			}
			else if( inTokens[i].matches("[a-zA-Z]+") )
				tempList.add(inTokens[i]);
			if( i >= inTokens.length) break; */
		}
		String[] results = tempList.toArray(new String[tempList.size()]);
		return results;
	}
	public static String stopWordRemoval(String inStr) {
		String outStr = "";
		String[] tokens = filterPunctuation(inStr.split("\\b"));
		String[] inTokens = keepEmotionExpression(tokens);
		// System.out.println("test:" + stopWords.containsKey("a"));
		for (int i = 0; i < inTokens.length; i++) {
			if (!(stopWords.containsKey(inTokens[i].toLowerCase())) ) 
			{
				// if this word is not a stop word and not a name
				outStr += (inTokens[i].toLowerCase() + " ");

			}
		}
		return outStr;
	}
	public static void buildEmotionHash()
	{
		emotionWords = new HashMap<String, String>();
		for(int i=0; i<emotionWordLists.length;i++) {
			if(!emotionWords.containsKey(emotionWordLists[i]))
				emotionWords.put(emotionWordLists[i], emotionWordLists[i]);
		}
	}
    public static String textStemmer(String text) {
    	String resultText = "";
        BreakIterator bi = BreakIterator.getSentenceInstance(Locale.US);
        bi.setText(text);
        int index = 0;
        
//        System.out.println("Here is the stemmed paragraph: \n");
        while( bi.next() != BreakIterator.DONE)
        {
        	String sentence = text.substring(index, bi.current());
        	String[] tokens = sentence.split("\\b");
            //String newSentence = "";
            
            for (int j=0; j < tokens.length; j++) {
            	if( j==1 && pronounDetect(tokens[j]) )
            		tokens[j] = tokens[j].toLowerCase();
            	tokens[j] = WordNetHelper.Stem(tokens[j]);
            	// Rebuild new sentence
                resultText += tokens[j];
            }
            //resultText += " ";
            //System.out.println(newSentence);
        	index = bi.current();
        }
        return resultText;
    }
	
	public static boolean pronounDetect(String word) {
		String pronouns[] = { "I", "you", "he", "she", "it", "they", "we",
				"my", "our", "your", "their", "her", "his", "its" };
		for (int i = 0; i < pronouns.length; i++) {
			if (word.equalsIgnoreCase(pronouns[i]))
				return true;
		}
		// System.out.println("\n" + word);
		return false;
	}
}
