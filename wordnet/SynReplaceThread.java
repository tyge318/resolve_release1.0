// WordNet Synonym Replacing Examplepackage wordnet;import java.io.BufferedReader;import java.io.FileReader;import java.io.IOException;import java.io.InputStream;import java.io.InputStreamReader;import java.io.OutputStream;import java.io.PrintStream;import java.net.*;import java.text.BreakIterator;import java.util.*;import org.codehaus.jackson.JsonProcessingException;import edu.stanford.nlp.parser.lexparser.LexicalizedParser;import edu.stanford.nlp.tagger.maxent.MaxentTagger;import net.didion.jwnl.*;import net.didion.jwnl.data.*;import net.didion.jwnl.data.relationship.*;import net.didion.jwnl.dictionary.Dictionary;public class SynReplaceThread implements Runnable{		Socket clientSocket;		LexicalizedParser lp;		MaxentTagger tagger;		HashMap<String, String> emoTable;		SynReplaceThread(Socket cs, LexicalizedParser lparser, MaxentTagger tggr, HashMap<String, String> table) {			clientSocket = cs;			lp = lparser;			tagger = tggr;			emoTable = table;		}		public void run() {			try {//				PatternStats.initialize();//				clientSocket.setSoTimeout(30*60*1000);				clientSocket.setSoTimeout(30*1000);				InputStream Is = clientSocket.getInputStream();				OutputStream Os = clientSocket.getOutputStream();				BufferedReader br = new BufferedReader(new InputStreamReader(Is));				PrintStream Ps = new PrintStream(Os);//				while( !clientSocket.isClosed()) {//					Ps.println("This is a test message.");//					while( !br.ready() );//					Thread.sleep(40*60*1000);					char[] buff = new char[1024*1024];					String data = "";					int length;//					data += br.readLine();					length = br.read(buff);					data += new String(buff, 0, length);					System.out.println("Received(ID = " + Thread.currentThread().getId() + "):\n" + data);//					Thread.sleep(10000);					//String result = (statsTable.get(key) == null) ? "" : (statsTable.get(key));										//extract the starred emotion word			        String targetWord = extractTargetWord(data);			        System.out.println("Target Emotion Word is: " + targetWord + "\n");			        String targetWordPOS = findTargetWordPOS(data, targetWord, tagger);			        data = data.replace("*", "");			        			        //look up the word on wordnet affect, find synsets//			        WordNetAffectDic.initialize();//			        String tempSynSetStr = getSynsetStr(targetWord, targetWordPOS);//			        tempSynSetStr = mergeSynStr( tempSynSetStr, VocabularyDotCom.getSynonyms(targetWord));//			        String synSetStr = (tempSynSetStr.contains(targetWord+" ")) ? tempSynSetStr: (targetWord + " " + tempSynSetStr);//			        			        String tempSynSetStr = getSynsetStr(targetWord, targetWordPOS);			        tempSynSetStr = mergeSynStr( tempSynSetStr, VocabularyDotCom.getSynonyms(targetWord));			        String synSetStr = mergeSynStr( targetWord.toLowerCase(), tempSynSetStr);			        			        System.out.println("Synonym cadidates:");			        System.out.println(synSetStr + "\n");								        if( canProcess(synSetStr)) {				      //Process each synonyms	//			        ArrayList<Map.Entry<String, Double>> results = SynProcess.processVP(data, synSetStr, targetWord);				        ArrayList<Map.Entry<String, Double>> results = SynProcess.processPattern(data, synSetStr, targetWord, lp, false);				        				        String response = "";				        System.out.print("\nSuggested synonym (top 5): ");						Iterator it = results.iterator();						int current = 0;						while (it.hasNext()) {							String[] entryString = it.next().toString().split("=");	//						System.out.print(entryString[0] + " ");							System.out.print(entryString[0]+"("+entryString[1]+")"+" ");							response += (entryString[0] + "\n");							current++;							if( current == 5)								break;						}				        						Ps.println(response);						Ps.flush();			        }			        else {			        	System.out.println("Not emotion word. Return itself.");			        	Ps.println(targetWord);			        	Ps.flush();			        }//					Thread.currentThread().//					clientSocket.close();//				}//				System.out.println("Client closed socket.");//				System.out.println("Thread (ID:" + Thread.currentThread().getId() + ") is about to terminate.");			}catch(Exception e){//				System.out.println("Thread (ID:" + Thread.currentThread().getId() + ") terminated.");				e.printStackTrace( );			} 		}		public boolean canProcess(String synSetStr) {			String[] syns = synSetStr.split(" ");			if( emoTable.isEmpty()) {				System.out.println("Emotion Table not initialized!");				return false;			}			else {				boolean result = false;				for(int i=0; i<syns.length; i++) {					result |= (emoTable.containsKey(syns[i]));				}				return result;			}		}		public static String mergeSynStr(String synStr1, String synStr2) {			String result = "";			String[] token = synStr2.split(" ");			for(int i=0; i<token.length; i++) {				if( token[i].contains("_")) continue;					result += token[i] + " ";			}			String[] tokens = synStr1.split(" ");			for(int i=0; i<tokens.length; i++) {				if(result.contains(tokens[i]));				else					result = result + " " + tokens[i];			}			result = result.replaceAll("\\s+", " ");			return result;		}		/*    public static void main(String[] args) throws JWNLException, JsonProcessingException, IOException {        // Initialize the database        // You must configure the properties file to point to your dictionary files        WordNetHelper.initialize("file_properties.xml");                     //read user's input from file.        System.out.println("Input file path: " + args[0] + "\n");        String test = "";		try {			BufferedReader br = new BufferedReader(new FileReader(args[0]));			String line;			while ((line = br.readLine()) != null) {				test += (line + "\n");			}			br.close();		}		catch (IOException e) {			e.printStackTrace();		}        System.out.println("Content of the input file:\n" +test );                //extract the starred emotion word        String targetWord = extractTargetWord(test);        System.out.println("Target Emotion Word is: " + targetWord + "\n");        String targetWordPOS = findTargetWordPOS(test, targetWord);                //look up the word on wordnet affect, find synsets//        WordNetAffectDic.initialize();        String tempSynSetStr = getSynsetStr(targetWord, targetWordPOS);        String synSetStr = (tempSynSetStr.contains(targetWord)) ? tempSynSetStr: (targetWord + " " + tempSynSetStr);        System.out.println("Synonym cadidates:");        System.out.println(synSetStr + "\n");                //Process each synonyms        ArrayList<Map.Entry<String, Double>> results = SynProcess.process(test, synSetStr, targetWord);		        System.out.print("\nSuggested synonym (top 5): ");		Iterator it = results.iterator();		int current = 0;		while (it.hasNext()) {			String[] entryString = it.next().toString().split("=");//			System.out.print(entryString[0] + " ");			System.out.print(entryString[0]+"("+entryString[1]+")"+" ");			current++;			if( current == 5)				break;		}		System.out.print("\n");            } */    public static IndexWord getIndexWord(String targetWord, String targetWordPOS) throws JWNLException {    	IndexWord w;    	if(targetWordPOS==null)    		return null;    	if( targetWordPOS.equals("ADJECTIVE"))    		w = WordNetHelper.getWord(POS.ADJECTIVE, targetWord);    	else if( targetWordPOS.equals("ADVERB"))    		w = WordNetHelper.getWord(POS.ADVERB, targetWord);    	else if( targetWordPOS.equals("NOUN"))    		w = WordNetHelper.getWord(POS.NOUN, targetWord);    	else if ( targetWordPOS.equals("VERB"))    		w = WordNetHelper.getWord(POS.VERB, targetWord);    	else    		w = null;    	return w;    }    public static String getSynsetStr(String targetWord, String targetWordPOS) throws JWNLException {    	IndexWord w = getIndexWord(targetWord, targetWordPOS);    	String result = "";    	if( w != null) {    		ArrayList<Synset> a = WordNetHelper.getRelated(w,PointerType.SIMILAR_TO);//    		a.addAll(WordNetHelper.getRelated(w,PointerType.HYPONYM));    		if (a != null && !a.isEmpty()) {    			Iterator<Synset> it = a.iterator();    			while( it.hasNext()) {    				Word[] words = it.next().getWords();    				for(int i=0; i<words.length; i++) {    					result += (words[i].getLemma().replaceAll("\\(\\w+\\)", "") + " ");    				}    			}//    			System.out.println("result= " + result);    		}    	}    	return result;    }    public static String findTargetWordPOS(String text, String targetWord, MaxentTagger tagger) {//    	MaxentTagger tagger = new MaxentTagger("tagger/english-bidirectional-distsim.tagger");    	    	String targetSentence = getTargetSentence(text);//      System.out.println("Target Sentence: " + targetSentence);    	String tagged = tagger.tagString(targetSentence);//    	System.out.println("Taged Target Sentence:\n" + tagged);    	String POS = "";    	    	String[] tokens = tagged.split(" ");    	for(int i=0; i<tokens.length; i++) {    		if( tokens[i].contains(targetWord)) {    			POS = findPOS(tokens[i]);    			break;    		}    	}    	return POS;    }    public static String findPOS(String inStr) {    	String[] tokens = inStr.split("_");    	return posLookUP(tokens[1]);    }    public static String posLookUP(String POSabbr) {    	String result = "";    	if( POSabbr.equals("JJ") || POSabbr.equals("JJR") || POSabbr.equals("JJS"))    		result = "ADJECTIVE";    	else if( POSabbr.equals("NN") || POSabbr.equals("NNP") || POSabbr.equals("NNPS") || POSabbr.equals("NNS"))    		result = "NOUN";    	else if( POSabbr.equals("RB") || POSabbr.equals("RBR") || POSabbr.equals("RBS"))    		result = "ADVERB";    	else if( POSabbr.equals("VB") || POSabbr.equals("VBD") || POSabbr.equals("VBG") || POSabbr.equals("VBN") || POSabbr.equals("VBP") || POSabbr.equals("VBZ"))    		result = "VERB";    	else    		result = null;    	return result;    }    public static float getSimilarity(String text1, String text2) {    	int count = 0;    	float point = 0;    	HashMap text1hash = new HashMap();    	HashMap text2hash = new HashMap();    	String[] text2tokens = text2.split("\\b");    	for(int i=0; i<text2tokens.length; i++) {    		if( !pronounDetect(text2tokens[i]) && !articleDetect(text2tokens[i]) && !conjunctionDetect(text2tokens[i]))    			text2hash.put(text2tokens[i], text2tokens[i]);    	}    	String[] text1tokens = text1.split("\\b");    	for(int i=0; i<text1tokens.length; i++) {    		if( !pronounDetect(text1tokens[i]) && !articleDetect(text1tokens[i]) && !conjunctionDetect(text1tokens[i]))    			text1hash.put(text1tokens[i], text1tokens[i]);    	}    	    	for(Iterator iter = text1hash.keySet().iterator(); iter.hasNext(); ) {    		String str = (String)iter.next();    		if( text2hash.containsKey(str))    			count++;    	}    	//    	System.out.println(count + " " + text1hash.size());    	point = (float)count / text1hash.size();    	return point;    }    public static String extractTargetWord(String text) {    	String emotionWord = "";    	String[] tokens = text.split("\\b");    	for(int i=0; i < tokens.length; i++)    	{    		if(tokens[i].contains("*"))    			emotionWord = tokens[i+1];    	}    	return emotionWord;    }    public static String getTargetSentence(String text) {    	String resultSentence = "";    	BreakIterator bi = BreakIterator.getSentenceInstance();    	bi.setText(text);    	int index = 0;    	while( bi.next() != BreakIterator.DONE)    	{    		String sentence = text.substring(index, bi.current());    		if( sentence.contains("*"))    			resultSentence = sentence;    		index = bi.current();    	}    	resultSentence = resultSentence.replace("*", "");    	return resultSentence;    }    public static String textStemmer(String text) {    	String resultText = "";        BreakIterator bi = BreakIterator.getSentenceInstance(Locale.US);        bi.setText(text);        int index = 0;        //        System.out.println("Here is the stemmed paragraph: \n");        while( bi.next() != BreakIterator.DONE)        {        	String sentence = text.substring(index, bi.current());        	String[] tokens = sentence.split("\\b");            //String newSentence = "";                        for (int j=0; j < tokens.length; j++) {            	if( j==1 && pronounDetect(tokens[j]) )            		tokens[j] = tokens[j].toLowerCase();            	tokens[j] = WordNetHelper.Stem(tokens[j]);            	// Rebuild new sentence                resultText += tokens[j];            }            //resultText += " ";            //System.out.println(newSentence);        	index = bi.current();        }        return resultText;    }    public static boolean pronounDetect(String word) {    	String pronouns[] = {"I", "you", "he", "she", "it", "they", "we", "my", "our", "your", "their", "her", "his", "its"};    	for(int i=0; i<pronouns.length; i++)    	{    		if( word.equalsIgnoreCase(pronouns[i]))    			return true;    	}    	//System.out.println("\n" + word);    	return false;    }    public static boolean articleDetect(String word) {    	String articles[] = {"a", "an", "the"};    	for(int i=0; i<articles.length; i++)    		if(word.equalsIgnoreCase(articles[i]))    			return true;    	return false;    }    public static boolean conjunctionDetect(String word) {    	String conjs[] = {"and", "or", "but", "nor"};    	for(int i=0; i<conjs.length; i++)    		if(word.equalsIgnoreCase(conjs[i]))    			return true;    	return false;    }    public static String getSynonym(IndexWord w) throws JWNLException {        // Use the helper class to get an ArrayList of similar Synsets for an IndexWord        ArrayList a = WordNetHelper.getRelated(w,PointerType.SIMILAR_TO);        // As long as we have a non-empty ArrayList        if (a != null && !a.isEmpty()) {            System.out.println("Found a synonym for " + w.getLemma() + ".");            // Pick a random Synset            int rand = (int) (Math.random() * a.size());            Synset s = (Synset) a.get(rand);            // Pick a random Word from that Synset            Word[] words = s.getWords();            rand = (int) (Math.random() * words.length);            return words[rand].getLemma();        }        return null;    }}