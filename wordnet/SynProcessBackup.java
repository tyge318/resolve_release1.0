package wordnet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.BreakIterator;
import java.util.*;

import org.ejml.simple.*;

class SynProcessBackup {
	public static HashMap stopWords = null;
//	public static HashMap words = null;
	public static ArrayList<String> docs = new ArrayList<String>();
	private static String target;

	public static void process(String text, String synStr, String targetWord, boolean arg) {
		target = targetWord;
		HashMap<String, Double> similarityValues = new HashMap<String, Double>();

		String[] syns = synStr.split(" ");

		// Extracting each synonym's gloss, remove stop words, and stemming
		// ArrayList<String> docs = new ArrayList<String>();
		stopWordInit();
		for (int i = 0; i < syns.length; i++) {
			String rawGloss = VocabularyDotCom.getGloss(syns[i]);
			if (rawGloss != null) {
				String BOWgloss = stopWordRemoval(rawGloss);
				String stemmedGloss = textStemmer(BOWgloss);
				// System.out.println(stemmedGloss);
				docs.add(stemmedGloss);
			} else
				docs.add(" ");
		}

		// Add the stemmed bag of words of the input text (as the last vector)
		String BOWtext = stopWordRemoval(text);
//		System.out.println(BOWtext);
		String stemmedText = textStemmer(BOWtext);
		docs.add(stemmedText);
//		System.out.println(docs.get(0));

		if (arg == false) // use input text as keyword resource
		{
			HashMap words = new HashMap<String, Integer>();
			// Extract key words
			keywordExtract(words, stemmedText, arg);
			keywordPrint(words);

			// generating the word-doc matrix
			SimpleMatrix wordDoc = wordDocMatrixGenerate(words, words.size(), docs.size());

			// System.out.println("Original matrix:");
			// wordDoc.print();

			SimpleSVD svd = wordDoc.svd();
			SimpleMatrix U = svd.getU();
			SimpleMatrix Sigma = svd.getW();
			SimpleMatrix V = svd.getV();

			// U.print();
			// Sigma.print();
			// V.print();

			SimpleMatrix Usub = U.extractMatrix(0, (words.size()), 0,
					(docs.size()));
			SimpleMatrix Sigmasub = Sigma.extractMatrix(0, (docs.size()), 0,
					(docs.size()));
			SimpleMatrix Vsub = V.extractMatrix(0, (docs.size()), 0,
					(docs.size()));

			// Usub.print();
			// Sigmasub.print();
			// Vsub.print();

			// System.out.println("Reconstructed matrix:");
			SimpleMatrix wordDocSimilar = Usub.mult(Sigmasub).mult(Vsub);
			// wordDocSimilar.print();

			System.out.println("Similarity values:");

			wordDocSimilar = wordDoc;

			SimpleMatrix textVector = wordDocSimilar.extractVector(false, docs.size() - 1);
			for (int i = 0; i < docs.size() - 1; i++) {
				double tempSim;
				SimpleMatrix tempVector = wordDocSimilar.extractVector(false, i);
				tempSim = tempVector.dot(textVector) / (tempVector.normF() * textVector.normF());
				System.out.print(tempSim + "\t");
				similarityValues.put(syns[i], tempSim);
			}

		} 
		else // use synonym gloss as keyword resource
		{
			for(int i=0; i<docs.size()-1; i++) {
				HashMap words = new HashMap<String, Integer>();
//				System.out.println("Keyword extraction on doc[" + i + "]..." + docs.get(i));
				keywordExtract(words, docs.get(i), arg);
				keywordPrint(words);
				
				Object[] tempKeyWords = words.keySet().toArray();
				String[] keyWords = new String[tempKeyWords.length];
				for(int j=0; j<tempKeyWords.length; j++)
					keyWords[j] = (String)tempKeyWords[j];
				
				Arrays.sort(keyWords);
				SimpleMatrix inputVector = singleVector(words, keyWords, docs.get(docs.size()-1), target);
				SimpleMatrix synVector = singleVector(words, keyWords, docs.get(i), syns[i]);
				double tempSim = inputVector.dot(synVector) / (inputVector.normF() * synVector.normF());
//				if( i == 0) {
//					System.out.println("InputVector Text:" + docs.get(docs.size()-1));
//					inputVector.print();
//					synVector.print();
//				}
				System.out.print(tempSim + "\n");
				similarityValues.put(syns[i], tempSim);
				
			}
		}
		
		ArrayList<Map.Entry<String, Double>> similarityValueList = new ArrayList<Map.Entry<String, Double>>(similarityValues.size());
		similarityValueList.addAll(similarityValues.entrySet());
		ValueComparator vc = new ValueComparator();
		Collections.sort(similarityValueList, vc);

		System.out.print("\nSuggested synonym (order from high to low): ");
		Iterator it = similarityValueList.iterator();
		while (it.hasNext()) {
			String[] entryString = it.next().toString().split("=");
			System.out.print(entryString[0] + " ");
		}
		System.out.print("\n");
		// System.out.println("The best-suggested synonym is \"" +
		// syns[maxIndex] + "\".");

	}
	public static SimpleMatrix singleVector(HashMap words, String[] keyWords, String inText, String entryWord) {
		SimpleMatrix vector = new SimpleMatrix(keyWords.length, 1);
		String[] docWords = inText.split(" ");
		HashMap<String, Integer>docVector = new HashMap<String, Integer>(words);
		for(int i=0; i<docWords.length; i++) {
			if( docVector.containsKey(docWords[i]))
				docVector.put(docWords[i], (docVector.get(docWords[i]) + 1));
		}
		if(docVector.containsKey(entryWord)) //if it is the word being explained, it tends to occur a lot. => reduce it to only 1.
			docVector.put(entryWord, 1);
		for(int i=0; i<keyWords.length; i++) //fill out the vector
			vector.set(i, 0, docVector.get(keyWords[i]));
		return vector;
	}
	private static class ValueComparator implements
			Comparator<Map.Entry<String, Double>> {
		public int compare(Map.Entry<String, Double> mp1, Map.Entry<String, Double> mp2) {
			if (mp1.getValue() >= mp2.getValue())
				return -1;
			else
				return 1;
		}
	}

	public static SimpleMatrix wordDocMatrixGenerate(HashMap words, int rows, int cols) {
		SimpleMatrix wordDoc = new SimpleMatrix(rows, cols);
		Object[] tmpKeyWords = words.keySet().toArray();
		String[] keyWords = new String[tmpKeyWords.length];
		for (int i = 0; i < tmpKeyWords.length; i++) {
			keyWords[i] = (String) tmpKeyWords[i];
			// System.out.println(keyWords[i]);
		}
		Arrays.sort(keyWords); // sorting the keyWords
		// for(int i=0; i<keyWords.length; i++)
		// System.out.println(keyWords[i]);
		for (int i = 0; i < cols; i++) { // go through each doc
			String[] docWords = docs.get(i).split(" ");
			HashMap<String, Integer> docVector = new HashMap<String, Integer>(words);
			for (int j = 0; j < docWords.length; j++) {
				if (docVector.containsKey(docWords[j])) // if this word exists
					docVector
							.put(docWords[j], (docVector.get(docWords[j]) + 1)); // increment
																					// count
			}
			for (int j = 0; j < rows; j++) { // fill out the matrix
				wordDoc.set(j, i, docVector.get(keyWords[j]));
			}
		}
		return wordDoc;
	}

	public static void keywordExtract(HashMap words, String stemmedText, boolean arg) {
		SWN3 _sw = new SWN3();
		// Double targetValue = _sw.extract(target);
//		System.out.println("StemmedText = " + stemmedText + ".");
		String[] textTokens = stemmedText.split(" ");
		for (int i = 0; i < textTokens.length; i++) {
			// if(textTokens[i].equals("excite"))
			// System.out.println("excite weight = "+_sw.extract(textTokens[i]));
			if (!words.containsKey(textTokens[i])
					&& (arg || (_sw.extract(target) * _sw.extract(textTokens[i]) > 0)))
				words.put(textTokens[i], 0);
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
		String stopWordListPath = "../stopwords.txt";
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

	public static String stopWordRemoval(String inStr) {
		String outStr = "";
		String[] inTokens = inStr.split("\\s+");
		for (int i = 0; i < inTokens.length; i++) {
			inTokens[i] = inTokens[i].replaceAll("[/(/)*.,;:?!\"]", "");
			// inTokens[i] = inTokens[i].replace("\"","");
		}
		// System.out.println("test:" + stopWords.containsKey("a"));
		for (int i = 0; i < inTokens.length; i++) {
			if (!(stopWords.containsKey(inTokens[i].toLowerCase()))
					&& !Character.isUpperCase(inTokens[i].charAt(0)))
				// if this word is not a stop word and not a name
				outStr += (inTokens[i] + " ");
		}
		return outStr;
	}

	public static String textStemmer(String text) {
		String resultText = "";
		BreakIterator bi = BreakIterator.getSentenceInstance();
		bi.setText(text);
		int index = 0;

		// System.out.println("Here is the stemmed paragraph: \n");
		while (bi.next() != BreakIterator.DONE) {
			String sentence = text.substring(index, bi.current());
			String[] tokens = sentence.split("\\b");
			// String newSentence = "";

			for (int j = 0; j < tokens.length; j++) {
				if (j == 1 && pronounDetect(tokens[j]))
					tokens[j] = tokens[j].toLowerCase();
				tokens[j] = WordNetHelper.Stem(tokens[j]);
				/*
				 * if(tokens[j].contains("*") ) { System.out.println(tokens[j+1]
				 * + "\n"); targetWord = tokens[j+1]; }
				 */
				// Rebuild new sentence
				resultText += tokens[j];
			}
			// resultText += " ";
			// System.out.println(newSentence);
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
