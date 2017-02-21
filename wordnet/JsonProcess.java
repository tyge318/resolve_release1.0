package wordnet;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.*;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;

import net.didion.jwnl.JWNLException;


public class JsonProcess {
	public static String[] getEmotionWordList(String path) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(path));
		ArrayList<String> allEmoWords = new ArrayList<String>();
		String line;
		while( (line = br.readLine()) != null)  {
			if( line.length() < 1) continue;
			allEmoWords.add(line);
		}
		String[] results = new String[allEmoWords.size()];
		results = allEmoWords.toArray(results);
		return results;
	}
	public static String[] getExcludedList() throws IOException {
		final File folder = new File("emoWords" + File.separator );
		ArrayList<String> results = new ArrayList<String>();
		ArrayList<String> emotionLists = PatternStats.listFilesForFolder(folder);
		emotionLists.remove(".DS_Store");
		emotionLists.remove("Allpatterns.txt");
		for(int i=0; i<emotionLists.size(); i++) {
			String current = emotionLists.get(i);
			current = current.replace(".txt", "");
			current = current.replaceAll("\\s+", "");
			emotionLists.set(i, current);
		}
		HashMap<String, String> currentEmos = new HashMap<String, String>();
		String[] currentEmoList = getEmotionWordList("test" + File.separator + "extendedList.txt");
		for(int i=0; i<currentEmoList.length; i++) {
			if( currentEmoList[i].length()<1) continue;
			if( !currentEmos.containsKey(currentEmoList[i]))
				currentEmos.put(currentEmoList[i], currentEmoList[i]);
		}
		for(int i=0; i<emotionLists.size(); i++) {
			if( !currentEmos.containsKey(emotionLists.get(i))) {
				System.out.println(emotionLists.get(i));
				results.add(emotionLists.get(i));
			}
		}
		String[] arr = new String[results.size()];
		arr = results.toArray(arr);
		return arr;
	}
	public static void main(String[] args) throws JsonProcessingException, IOException {
//		WordNetAffectDic.initialize();
//		String[] emotionWordLists = WordNetAffectDic.outputEmotionWords();
//		String[] emotionWordLists = getExcludedList();
//		System.out.println(emotionWordLists.length);
		/*
		for(int i=0; i<emotionWordLists.length; i++) {
			emotionWordLists[i] = emotionWordLists[i].replace("_", "%20");
			emotionWordLists[i] = emotionWordLists[i].replace(" ", "%20");
//			if(emotionWordLists[i].equals("greedy")) System.out.println("at "+i);
		} */
		reorganizeCorpus("test"+File.separator+"corpus_append.txt", "test"+File.separator+"corpus_new.txt", "test"+File.separator+"stats.txt");
		
		/*
		int divideNum = 2;
		for(int i=0; i<divideNum; i++) {
			int chunkSize = emotionWordLists.length/divideNum;
			int remainder = emotionWordLists.length%divideNum;
			String[] myList;
			if( i!= divideNum-1 )
				myList = Arrays.copyOfRange(emotionWordLists, i*chunkSize, (i+1)*chunkSize);
			else
				myList = Arrays.copyOfRange(emotionWordLists, i*chunkSize, (i+1)*chunkSize+remainder);
			ExThread thread = new ExThread(myList, "test" + File.separator + "oldcorpus"+i+".txt");
			thread.start();
				
		} 
//		System.out.println(emotionWordLists[1002]);
//		System.out.println("Total emotion words = " + emotionWordLists.length);
//		writeEmotionList(emotionWordLists);
//		buildCorpus(emotionWordLists, "corpus.txt");
//		System.out.println("Just for test...");
//		System.out.print(jsonProcess("elated"));		*/
	}
	public static void reorganizeCorpus(String input, String path, String stats) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(input), "UTF-8"));
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "UTF-8"));
		
		HashMap<String, String> allSentences = new HashMap<String, String>();
		TreeMap<String, Integer> sentenceCountTable = new TreeMap<String, Integer>(); 
		
		String line;
		int sentenceCount = 0, synCount = 0;
		String currentEmoWord = "";
		while( (line = br.readLine())!= null ) {
			if( line.contains("=======================================")) {
//				if( currentEmoWord.length()<1) continue;
				sentenceCountTable.put(currentEmoWord, sentenceCount);
//				sentenceCount = 0;
				continue;
			}
			if( line.matches("\\*\\*\\w+:")) {
				currentEmoWord = line.replace("**", "");
				currentEmoWord = currentEmoWord.replace(":", "");
				synCount++;
			}
			else {
				if( line.length()<=1) continue;
				if( !allSentences.containsKey(line)) { //if this sentence not existed
					allSentences.put(line, line);
					sentenceCount++;
				}
			}
		}
		br.close();
		System.out.println("Sentence count = " + allSentences.size() + "," + sentenceCount + "; Synonym Count = " + synCount);
		
		/*
		br = new BufferedReader(new InputStreamReader(new FileInputStream("test"+File.separator+"oldcorpus.txt"), "UTF-8"));
		bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("test"+File.separator+"oldcorpus_new.txt"), "UTF-8"));
		while( (line = br.readLine())!= null ) {
			if( line.contains("=======================================")) {
//				if( currentEmoWord.length()<1) continue;
				sentenceCountTable.put(currentEmoWord, sentenceCount);
				sentenceCount = 0;
				continue;
			}
			if( line.matches("\\*\\*\\w+:")) {
				currentEmoWord = line.replace("**", "");
				currentEmoWord = currentEmoWord.replace(":", "");
			}
			else {
				if( line.length()<=1) continue;
				if( !allSentences.containsKey(line)) { //if this sentence not existed
					bw.write(line + "\n");
					sentenceCount++;
				}
			}
		}
		bw.close();
		br.close();
		*/
		
		Iterator<String> it = allSentences.keySet().iterator();
		while(it.hasNext()) {
			bw.write(it.next()+"\n");
		}
		bw.close();
		
		bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(stats), "UTF-8"));
		Iterator<Map.Entry<String, Integer>> itr = sentenceCountTable.entrySet().iterator();
		while( itr.hasNext()) {
			Map.Entry<String, Integer> current = itr.next();
			bw.write(current.getKey() + "\t" + current.getValue() + "\n");
		}
		bw.close(); 
	}
	public static void writeEmotionList(String[] words) throws IOException {
		Writer wt = new FileWriter("emoWords.txt");
		BufferedWriter bw = new BufferedWriter(wt);
		for(int i=0; i<words.length; i++) {
			words[i] = words[i].replace("%20", " ");
			bw.write(words[i]+"\n");
		}
		bw.close();
	}
	public static void buildCorpus(String[] words, String path, String message) throws IOException {
		Writer wt = new FileWriter(path);
		BufferedWriter bw = new BufferedWriter(wt); 
		String temp = null;
		for(int i=0; i<words.length; i++) {
			System.out.println(message + "\t" + "Working on emotion word[" + i + "]...");
			bw.write("**" + words[i] + ":\n");
			temp = jsonProcess(words[i]);
			bw.write(temp);
			bw.write("==========================================\n");
//			bw.flush();
		}
		bw.close();
	}
	public static String jsonProcess(String word) throws JsonProcessingException, IOException {
		String result="";
		int maxHits = 0;
		String url = "https://corpus.vocabulary.com/examples.json?query=";
		URL queryURL = new URL(url+word+"&maxResults=24&startOffset=0&filter=3");
		
		ObjectMapper mapper = new ObjectMapper();
		JsonFactory f = mapper.getJsonFactory();
		JsonParser jp = f.createJsonParser(queryURL);
		
		/* First find out how many hits (example sentences) there are in the corpus. */
		if( jp.nextToken() != JsonToken.START_OBJECT) {
			System.out.println("No object on the root.");
		}
		while(jp.nextToken() != JsonToken.END_OBJECT) {
			String tokenname = jp.getCurrentName();
			if( tokenname.equals("totalHits")) {
				//JsonNode node = jp.readValueAsTree();
				jp.nextToken();
				maxHits = jp.getValueAsInt();
				break;
			}
		} 
//		System.out.println("Total hits = " + maxHits);
//		if(maxHits >= 1000)
//			maxHits = 1000;
		queryURL = new URL(url+word+"&maxResults="+maxHits+"&startOffset=0&filter=3");
		
		String JsonContent = null;
		do {
			JsonContent = URLcontent(queryURL);
		
			jp = f.createJsonParser(JsonContent);
			if( jp.nextToken() != JsonToken.START_OBJECT) {
				System.out.println("No object on the root.");
			}
			else
				break;
		}while(true);
		JsonToken current;
		PrintStream out = new PrintStream(System.out, true, "UTF-8");
		while(jp.nextToken() != JsonToken.END_OBJECT) {
			String tokenname = jp.getCurrentName();
			current = jp.nextToken();
//			System.out.println(tokenname);
			if( tokenname.equals("sentences")) {
				if( current == JsonToken.START_ARRAY) {
					while(jp.nextToken()!= JsonToken.END_ARRAY) {
						JsonNode node = jp.readValueAsTree();
//						System.out.println("Sentence:" + node.get("sentence").asText());
						String thisSentence = node.get("sentence").asText();
						thisSentence = thisSentence.replace("\uFFFD", "");
						thisSentence = thisSentence.replaceAll("\\s+", " ");
						thisSentence = thisSentence.replaceAll("\\*+", "");
//						out.println("Sentence:" + thisSentence);
						result += (thisSentence + "\n");
					}
				}
			}
		}  
		return result;
	}
	public static String URLcontent(URL queryURL) {
		int count = 0;
		int maxTry = 3;
		String result = "";
		while( true ) {
	        try {
	            InputStream is = queryURL.openStream();
	            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
	             
	            String line;
	            while ( (line = br.readLine()) != null) {
	            	String UTF8Str = new String(line.getBytes(),"UTF-8");
	            	
	            	UTF8Str = UTF8Str.replaceAll("\\$d\\(\\d+\\)", "123");
//	            	UTF8Str = UTF8Str.replaceAll("[0-9]{13}", "@@@@@");
//	            	UTF8Str = UTF8Str.replaceAll("[0-9]{12}", "@@@@@");
//	            	UTF8Str = UTF8Str.replace("$d(@@@@@)", "123");
//	            	UTF8Str = UTF8Str.replace("$", "");
	            	
	            	result += UTF8Str;
	            	
	            }
	            br.close();
	            is.close();
	            break;
	        } catch (Exception e) {
	        	System.out.println("Exception occur. Retring...");
	        	if (++count == maxTry)
	        		e.printStackTrace();
	            
	        }  
		}
        return result;
    }
}
class ExThread extends Thread {
	public String[] words;
	public String path;
	public ExThread(String[] inputWords, String inputPath) {
		words = inputWords;
		path = inputPath;
	}
	public void run() {
		try{ 
			System.out.println("ThreadID: " + Thread.currentThread().getId() + " - begin running...");
			String message = "ThreadID:"+Thread.currentThread().getId();
//			System.out.println("My array size = " + words.length + ". My path name = " + path);
//			System.out.println(words[0] + " : " + words[words.length-1]);
			JsonProcess.buildCorpus(words, path, message);
		} 
		catch(Exception e){e.printStackTrace( );}
	}
}