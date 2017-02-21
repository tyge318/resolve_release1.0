package wordnet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.net.InetAddress;
import java.text.BreakIterator;
import java.text.DecimalFormat;
import java.util.*;
import java.net.*;

import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.PointerType;
import net.didion.jwnl.data.relationship.Relationship;

import org.codehaus.jackson.JsonProcessingException;
import org.ejml.simple.*;
import org.paukov.combinatorics.*;

class SynProcessBackup2 {
	public static HashMap stopWords = null;
	private static HashMap<String, String> emotionWords;
//	public static HashMap words = null;
	public static ArrayList<String> docs = new ArrayList<String>();
	private static String target;
	private static String[] emotionWordLists;
	private static double DELTA = 0.75;

	public static void main(String[] args) {
		HashMap<String, Integer> keywordsSingle = new HashMap<String, Integer>();
		HashMap<String, Integer> keywordsTwo = new HashMap<String, Integer>();
//		ArrayList<String> Sentences = new ArrayList<String>();
		WordNetHelper.initialize("file_properties.xml");
		getEmotionWordList();
		buildEmotionHash();
		stopWordInit();
//		String test = "world-weary, cultured bureaucrat whose years in Paris have made him a committed Francophile and less enthusiastic Communist";
//		String stemmedtest = textStemmer(stopWordRemoval(test));
//		printEmotionWordList();
		String corpusPath = "corpus.txt";

		try {
//			RandomAccessFile raf = new RandomAccessFile(new File(corpusPath), "r");
			BufferedReader br = new BufferedReader(new FileReader(corpusPath));
			String line;
			
			int count = 0;
			
			// first pass: filter unnecessary words, find all frequency.
			while ((line = br.readLine()) != null) {
				String stemmedline = stopWordRemoval(textStemmer(stopWordRemoval(line)));
				String[] lineTokens = stemmedline.split("\\s+");
				String[] lineTokensComb = wordComb(lineTokens);
				
				for(int i=0; i<lineTokens.length; i++) {
					if(!keywordsSingle.containsKey(lineTokens[i])) {
//						if(lineTokens[i].equals("guilt_feelings"))
//							System.out.println(lineTokens[i]);
						if( !lineTokens[i].equals("") && !lineTokens[i].equals("_") && !lineTokens[i].equals("__"))
							keywordsSingle.put(lineTokens[i], 1);
					}
					else {
						int tempValue = (Integer) keywordsSingle.get(lineTokens[i]);
						tempValue++;
						keywordsSingle.put(lineTokens[i], tempValue);
					}
				}
				
				if(lineTokensComb != null) {
					for(int i=0; i<lineTokensComb.length; i++) {
						if( !keywordsTwo.containsKey(lineTokensComb[i]))
							keywordsTwo.put(lineTokensComb[i], 1);
						else {
							int tempValue = (Integer) keywordsTwo.get(lineTokensComb[i]);
							tempValue++;
							keywordsTwo.put(lineTokensComb[i], tempValue);
						}
					}
				} 
				count++;
				if( (count % 10000) == 0)
					System.out.println("...");
			}
			
//			raf.seek(0); //move cursor back to stop
			br.close();
			
			WordNetHelper.termination();
			stopWords = null;
			emotionWords = null;
			emotionWordLists = null;
			System.gc();
			
			System.out.println("Begin writing...");
			Writer wt = new FileWriter("stats.txt");
			BufferedWriter bw = new BufferedWriter(wt); 
			
			Map<String, Integer> sortedKeySingle = new TreeMap<String, Integer>(keywordsSingle);
			keywordsSingle = null; 
			System.gc(); //release memory
			Iterator it = sortedKeySingle.entrySet().iterator();
			String entry = "";
			while(it.hasNext()) {
				entry = it.next().toString();
				entry += "\n";
				bw.write(entry);
			} 
			sortedKeySingle = null;
			System.gc();
			
			bw.write("======================\n");
			Map<String, Integer> sortedKeyTwo = new TreeMap<String, Integer>(keywordsTwo);
			keywordsTwo = null;
			System.gc();
			
			it = sortedKeyTwo.entrySet().iterator();
			while(it.hasNext()) {
				entry = it.next().toString();
				entry += "\n";
				bw.write(entry);
			}
			sortedKeyTwo = null;
			System.gc();
//			String[] keywordsArr = keywords.keySet().toArray();
//			Iterator strIt = Sentences.iterator();
			
			bw.close();
//			raf.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	public static String[] wordComb(String[] inWords) {
		if(inWords.length>=2)
		{
			ICombinatoricsVector<String> initialVector = Factory.createVector(inWords);
			Generator<String> gen = Factory.createSimpleCombinationGenerator(initialVector, 2);
		
			List<String> allComb = new ArrayList<String>();
//			System.out.println("#=" + gen.getNumberOfGeneratedObjects());
			Iterator it = gen.iterator();
			while(it.hasNext()) {
				String tempStr = getDelimitedSubstring(it.next().toString(), "[", "]");
//				tempStr = tempStr.replace(", ", "_");
				String[] tempStrArr = tempStr.split(", ");
				if( (!tempStrArr[0].equals(tempStrArr[1])) && (emotionWords.containsKey(tempStrArr[0]) || emotionWords.containsKey(tempStrArr[1])) ) 
				{ //if not duplicated and either one contains the emotional expression
					List<String> list = new ArrayList<String>();
					if( emotionWords.containsKey(tempStrArr[0]) && !emotionWords.containsKey(tempStrArr[1])) {
						list.add(tempStrArr[0]); list.add(tempStrArr[1]);
					}
					else if (!emotionWords.containsKey(tempStrArr[0]) && emotionWords.containsKey(tempStrArr[1])) {
						list.add(tempStrArr[1]); list.add(tempStrArr[0]);
					}
					else {
						list.add(tempStrArr[0]); list.add(tempStrArr[1]);
						Collections.sort(list, String.CASE_INSENSITIVE_ORDER);
					}
					allComb.add(list.get(0) + "," + list.get(1));
				}
				//else just skip this result.
			}
			String[] results = allComb.toArray(new String[allComb.size()]);
			
			return results;
		} else
			return null;
	}
    public static String getDelimitedSubstring(String text, String startDelimiter, String endDelimiter) {
            int start;
            int stop;
            String subStr = "";

            if ((text != null) && (startDelimiter != null) &&
                    (endDelimiter != null)) {
                start = text.indexOf(startDelimiter);

                if (start >= 0) {
                    stop = text.indexOf(endDelimiter, start + 1);

                    if (stop > start) {
                        subStr = text.substring(start + 1, stop);
                    }
                }
            }

            return subStr;
        }
	public static HashMap<String, Integer> buildStatsHash(String path) {
		HashMap<String, Integer> table = new HashMap<String, Integer>();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(path));
			String entry;
			while ( (entry = br.readLine()) != null) {
				if( !entry.equals("======================")) {
					String [] elements = entry.split("=");
					table.put(elements[0], Integer.valueOf(elements[1]));
				}
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return table;
	}
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
    /*
    public static double alsoFindCoSyn(HashMap<String, Integer> table, String emoWord, String coWord) {
    	String coWordSynStr = WordNetAffectDic.findSynset(coWord);
    	String[] coWordSyns = coWordSynStr.split(" ");
    	double result = 0.0;
    	for(int i=0; i<coWordSyns.length; i++) {
    		String pair = pairGenerate(emoWord, coWordSyns[i]);
    		if( table.get(pair) != null)
    			result += (double)table.get(pair);
    	}
    	return result;
    }
    public static double alsoFindSyn(HashMap<String, Integer> table, String coWord) {
    	String coWordSynStr = WordNetAffectDic.findSynset(coWord);
    	String[] coWordSyns = coWordSynStr.split(" ");
    	double result = 0.0;
    	for(int i=0; i<coWordSyns.length; i++) {
    		if( table.get(coWordSyns[i]) != null)
    			result += (double)table.get(coWordSyns[i]);
    	}
    	return result;
    } */
	public static int getStatsData(PrintStream ps, BufferedReader br, String query) throws NumberFormatException, IOException {
		ps.println(query);
		String resultStr = br.readLine();
		int result = (resultStr.isEmpty()) ? (-1) : Integer.parseInt(resultStr);
		return result;
	}
    public static void process(String text, String synStr, String targetWord, boolean arg) throws JWNLException, JsonProcessingException, IOException {
		target = targetWord;
		getEmotionWordList();
		buildEmotionHash();
//		HashMap<String, Integer> statsTable = buildStatsHash("stats.txt");
		HashMap<String, Double> similarityValues = new HashMap<String, Double>();
		/*
		HashMap words = new HashMap<String, Double>(); 
		*/
		
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
		for(int i=0; i<syns.length; i++) {
			double total = 0.0;
			int tempSynFreq = getStatsData(Ps, br, syns[i]);
			double synFreq = ( tempSynFreq == -1) ? 0.0 : (double)tempSynFreq;
			System.out.println("On synonym \"" + syns[i] + "\"(" + synFreq + "):");
			for(int j=0; j<textTokens.length; j++) {
				int tempTokenFreq = getStatsData(Ps, br, textTokens[j]);
				double tokenFreq = (tempTokenFreq == -1) ? 0.0 : (double)tempTokenFreq;
//				tokenFreq += DELTA * alsoFindSyn(statsTable, textTokens[j]);
				String pair = pairGenerate(syns[i], textTokens[j]);
				int tempCoFreq = getStatsData(Ps, br, pair);
				double coFreq = (tempCoFreq == -1) ? 0.0 : (double)tempCoFreq;
//				coFreq += DELTA * alsoFindCoSyn(statsTable, syns[i], textTokens[j]);
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
					System.out.println("<" + pair + ">: " + coFreq + ".");
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
		/*
		// Extract keywords
		keywordExtract(words, stemmedText, arg);
		keywordPrint(words);
//		int rows = words.size();
		for(int i=0; i<syns.length; i++) {
			if( !words.containsKey(syns[i]) )
				words.put(syns[i], (double)0);
		}
		
//		docs.add(stemmedText);
//		System.out.println(docs.get(0));
		

		if (arg == false) // use input text as keyword resource
		{

			// generating the word-doc matrix
			SimpleMatrix PMIs = wordDocMatrixGenerate(words, syns, (words.size()-syns.length) );

			// System.out.println("Original matrix:");
//			PMIs.print();

//			SimpleSVD svd = wordDoc.svd();


			System.out.println("Similarity values:");
			int rows = PMIs.getNumElements() / syns.length;
			for(int i=0; i<syns.length; i++) {
				double tempTotal = 0.0;
				for(int j=0; j<rows; j++)
					tempTotal += PMIs.get(j, i);
				tempTotal = Math.log(tempTotal);
				System.out.println("Synonym \"" + syns[i] + "\":" + tempTotal + "."); 
				similarityValues.put(syns[i], tempTotal);
			}

		} */
		
		ArrayList<Map.Entry<String, Double>> similarityValueList = new ArrayList<Map.Entry<String, Double>>(similarityValues.size());
		similarityValueList.addAll(similarityValues.entrySet());
		ValueComparator vc = new ValueComparator();
		Collections.sort(similarityValueList, vc);

		System.out.print("\nSuggested synonym (top 5): ");
		Iterator it = similarityValueList.iterator();
		int current = 0;
		while (it.hasNext()) {
			String[] entryString = it.next().toString().split("=");
//			System.out.print(entryString[0] + " ");
			System.out.print(entryString[0]+"("+entryString[1]+")"+" ");
			current++;
			if( current == 5)
				break;
		}
		System.out.print("\n");
		// System.out.println("The best-suggested synonym is \"" +
		// syns[maxIndex] + "\".");
		
	}
	private static class ValueComparator implements Comparator<Map.Entry<String, Double>> {
		public int compare(Map.Entry<String, Double> mp1, Map.Entry<String, Double> mp2) {
			if (mp1.getValue() >= mp2.getValue())
				return -1;
			else
				return 1;
		}
	}
	public static String[] sortHashMap(HashMap inputMap) {
		Object[] tmpKeyWords = inputMap.keySet().toArray();
		String[] keyWords = new String[tmpKeyWords.length];
		for (int i = 0; i < tmpKeyWords.length; i++) {
			keyWords[i] = (String) tmpKeyWords[i];
			// System.out.println(keyWords[i]);
		}
		Arrays.sort(keyWords); // sorting the keyWords
		return keyWords;
	}
 	public static SimpleMatrix wordDocMatrixGenerate(HashMap words, String [] syns, int rows) throws JWNLException, JsonProcessingException, IOException {
		SimpleMatrix wordDoc = new SimpleMatrix(rows, syns.length+1);
		SimpleMatrix synStats = new SimpleMatrix(syns.length, syns.length+1);
		SimpleMatrix PMIs = new SimpleMatrix(rows, syns.length);
		
		String []keyWords = sortHashMap(words);
		
		for (int i = 0; i < syns.length; i++) { // go through each syn
//			String[] docWords = docs.get(i).split(" ");
			String text = "";
			HashMap<String, Double> docVector = new HashMap<String, Double>(words); 	//to hold temp results

			System.out.println("Begin processing on synonym[" + i + "]...");
	/*		try {
				BufferedReader br = new BufferedReader(new FileReader("examples/" + syns[i] + ".txt"));
				String line;
				while ((line = br.readLine()) != null) {
					text += line;
				}
				br.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			} */
//			text = JsonProcess.jsonProcess(syns[i]);
			String BOWsentence = stopWordRemoval(text);
			String stemmedSentence = textStemmer(BOWsentence);
			String []tokens = stemmedSentence.split(" ");
			for(int j=0; j < tokens.length; j++) {
				if (docVector.containsKey(tokens[j])) // if this word exists
					docVector.put(tokens[j], (docVector.get(tokens[j]) + 1.0));
				else {							
					for(int k=0; k<keyWords.length; k++) {	
						if( WordNetHelper.isRelationship(tokens[j], keyWords[k], PointerType.SIMILAR_TO) ) //if this word is a synonym to a keyword
							docVector.put(keyWords[k], (docVector.get(keyWords[k]) + DELTA));
//						if( WordNetHelper.isRelationship(tokens[j], keyWords[k], PointerType.HYPERNYM) ) //if this word is a hypernym to a keyword
//							docVector.put(keyWords[k], (docVector.get(keyWords[k]) + DELTA*DELTA));
//						if( WordNetHelper.isRelationship(tokens[j], keyWords[k], PointerType.HYPONYM) ) //if this word is a hyponym to a keyword
//							docVector.put(keyWords[k], (docVector.get(keyWords[k]) + DELTA*DELTA));
					}
				}
						
			}
//			System.out.println("Count at " + (count++) + ".");
			for(int j=0; j<syns.length; j++) {
				synStats.set(j, i, docVector.get(syns[j]));
				docVector.remove(syns[j]);
			}
			String [] newKeyWords = sortHashMap(docVector);
			for (int j = 0; j < docVector.size(); j++) { // fill out the matrix
				wordDoc.set(j, i, docVector.get(newKeyWords[j]));
			}
			System.out.println("Done processing on synonym[" + i + "]...");
		}
		for(int i=0; i<rows; i++) {
			double tempTotal = 0.0;
			for(int j=0; j<syns.length; j++)
				tempTotal += wordDoc.get(i, j);
			wordDoc.set(i, syns.length, tempTotal);
		}
		for(int i=0; i<syns.length; i++) {
			double tempTotal = 0.0;
			for(int j=0; j<syns.length; j++)
				tempTotal += synStats.get(i, j);
			synStats.set(i, syns.length, tempTotal);
		}
//		wordDoc.print();
//		synStats.print();
		
		for(int i=0; i<rows; i++) {
			for(int j=0; j<syns.length; j++) {
				double tempPMI;
				if( wordDoc.get(i, syns.length)*synStats.get(j, syns.length) != 0)
					tempPMI = wordDoc.get(i, j)/(wordDoc.get(i, syns.length)*synStats.get(j, syns.length));
				else
					tempPMI = 0.0;
//				if(tempPMI != 0.0)
//					tempPMI = Math.log(tempPMI);
				PMIs.set(i, j, tempPMI);
			}
		}

		return PMIs;
	}
 	
	public static void keywordExtract(HashMap words, String stemmedText, boolean arg) {
//		SWN3 _sw = new SWN3();
		// Double targetValue = _sw.extract(target);
//		System.out.println("StemmedText = " + stemmedText + ".");
		String[] textTokens = stemmedText.split(" ");
		for (int i = 0; i < textTokens.length; i++) {
			// if(textTokens[i].equals("excite"))
			// System.out.println("excite weight = "+_sw.extract(textTokens[i]));
			if (!words.containsKey(textTokens[i]))
//					&& (!arg || (_sw.extract(target) * _sw.extract(textTokens[i]) > 0)))
				words.put(textTokens[i], (double)0);
		}
	}

	public static void keywordPrint(HashMap words) {
		System.out.print("Keywords: ");
		Iterator it = words.keySet().iterator();
		while (it.hasNext()) {
			System.out.print(it.next().toString() + " ");
		}
		System.out.print("\n");
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
	public static void getEmotionWordList() {
		WordNetAffectDic.initialize();
		emotionWordLists = WordNetAffectDic.outputEmotionWords();
//		for(int i=0; i<emotionWordLists.length; i++)
//			emotionWordLists[i] = emotionWordLists[i].replace("_", " ");
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
//		HashMap<String, String> emotionWords = buildEmotionHash();
		BreakIterator bi = BreakIterator.getSentenceInstance();
		bi.setText(text);
		int index = 0;

		// System.out.println("Here is the stemmed paragraph: \n");
		while (bi.next() != BreakIterator.DONE) {
			String sentence = text.substring(index, bi.current());
			String[] tokens = sentence.split("\\s");
			// String newSentence = "";

			for (int j = 0; j < tokens.length; j++) {
				String temp = null;
				if (j == 1 && pronounDetect(tokens[j]))
					tokens[j] = tokens[j].toLowerCase();
				if( !emotionWords.containsKey(tokens[j]) && !tokens[j].contains("-")) //if this is not an emotion expression and contains no dash, do stemming
					temp = WordNetHelper.Stem(tokens[j]);
				else
					temp = tokens[j];
//				if( temp != null && temp.equals("a"))
//					System.out.println("PAUSE");
				/*
				 * if(tokens[j].contains("*") ) { System.out.println(tokens[j+1]
				 * + "\n"); targetWord = tokens[j+1]; }
				 */
				// Rebuild new sentence
				if(temp != null)
					resultText += (temp + " ");
			}
			// resultText += " ";
			// System.out.println(newSentence);
			index = bi.current();
		}
//		resultText = resultText.replace("  ", " ");
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
