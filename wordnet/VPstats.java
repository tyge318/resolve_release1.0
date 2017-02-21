package wordnet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.instrument.Instrumentation;
import java.net.*;
import java.text.BreakIterator;
import java.util.*;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import net.didion.jwnl.*;
import net.didion.jwnl.data.*;
import net.didion.jwnl.data.relationship.*;
import net.didion.jwnl.dictionary.Dictionary;

public class VPstats {
//	public static MaxentTagger tagger;
	public static HashMap<String, emoObject> table;
	public static void main(String[] args) throws IOException {
		initialize();
//		System.out.println("Done initializing.");
		try{ 
			 int serverPort = 3456; //Integer.parseInt(args[0]); 
			 ServerSocket lookUpServer = new ServerSocket(serverPort); 
			 System.out.println("LookUp Process running...");
			 while (true){ 
				 Socket clientSocket = lookUpServer.accept( ); 
//				 lookUpThread thread = new lookUpThread(clientSocket); 
				 Thread newT = new Thread(new VPLookUPThread(clientSocket));
				 newT.start( ); 
			 } 		 
		} 
		catch(Exception e){e.printStackTrace( );}
//		emoObject obj = table.get("sad");
//		obj.printHash();
//		System.out.println(obj.findBestMatch("get lay out", "get") + " Frequence = " + obj.getFrequence(obj.findBestMatch("get lay out", "get")) );
	}
	public static void initialize() throws IOException {
//		HashMap<String, Integer> stats = new HashMap<String, Integer>();
		table = new HashMap<String, emoObject>();
//		tagger = new MaxentTagger("tagger/english-bidirectional-distsim.tagger");
		BufferedReader br = new BufferedReader(new FileReader("VPstats2.txt"));
//		BufferedWriter bw = new BufferedWriter(new FileWriter("patterns.txt")); 
		String line;
		while ((line = br.readLine()) != null) {
			String[] tokens = line.split(",");
			if( tokens[1].length() > 1) {
				if( !table.containsKey(tokens[0])) {
					emoObject obj = new emoObject(tokens[0]);
					obj.addExpression(tokens[1]);
					table.put(tokens[0], obj);
				}
				else {
					emoObject obj = table.get(tokens[0]);
					obj.addExpression(tokens[1]);
					table.put(tokens[0], obj);
				}
			}	
		}
		br.close();
	}
}
class emoObject {
//	public final int DELTA = 0.75;
	public String emoWord;
	public HashMap<String, Integer> expressions;
	public emoObject(String word) {
		emoWord = word;
		expressions = new HashMap<String, Integer>();
	}
	public void addExpression(String VP) {
		VP = VP.replaceAll("_[A-Z$]+\\s", " ");
		String[] tokens = VP.split("=");
		if( expressions.containsKey(tokens[0])) System.out.println("Duplicated!");
		else
			expressions.put(tokens[0], Integer.valueOf(tokens[1]));
	}
	public void printHash() {
		Iterator it = expressions.entrySet().iterator();
		while(it.hasNext())
			System.out.println(it.next().toString());
	}
	public double findVerbMatchValue(String input, String verb) {
		double result = 0.0;
		ArrayList<String> set = new ArrayList<String>(expressions.keySet());
		ArrayList<String> verbMatched = new ArrayList<String>();
		for(int i=0; i<set.size(); i++) {
			if( set.get(i).contains(verb))
				verbMatched.add(set.get(i));
		}
		for(int i=0; i<verbMatched.size(); i++)
			System.out.println(verbMatched.get(i));
		System.out.println();
		return result;
	}
	public double findBestMatchValue(String input, String verb) {
		if( expressions.containsKey(input)) {//perfactly matched
			return (double)expressions.get(input);
		}
		else {
			int temp, currentMax = 0;
			String[] inputTokens = input.split(" ");
//			int inputNumWord = inputTokens.length;
			String currentStr = "";
			double result = 0.0;
			ArrayList<String> set = new ArrayList<String>(expressions.keySet());
			for(int i=0; i<set.size(); i++) {
				if( set.get(i).contains(verb)) { //make sure target shares at least the verb
					temp = getSharedWordNum(input, verb, set.get(i));
					if( temp > currentMax ) {
						currentMax = temp;
						currentStr = set.get(i);
						result = ((double)temp / (double)inputTokens.length ) * (double)expressions.get(set.get(i));
					}
					else if( temp == currentMax) {
						int previousValue = longestSubstr(input, currentStr);
						int newValue = longestSubstr(input, set.get(i));
						if( previousValue >= newValue);
						else {
							currentStr = set.get(i);
							result = ((double)temp / (double)inputTokens.length ) * (double)expressions.get(set.get(i));				
						}
					}
				}
			}
//			System.out.println("Min = " + currentMin);
			return result;
		}		
	}
	public int longestSubstr(String first, String second) {
	    if (first == null || second == null || first.length() == 0 || second.length() == 0) {
	        return 0;
	    }

	    int maxLen = 0;
	    int fl = first.length();
	    int sl = second.length();
	    int[][] table = new int[fl][sl];

	    for (int i = 0; i < fl; i++) {
	        for (int j = 0; j < sl; j++) {
	            if (first.charAt(i) == second.charAt(j)) {
	                if (i == 0 || j == 0) {
	                    table[i][j] = 1;
	                }
	                else {
	                    table[i][j] = table[i - 1][j - 1] + 1;
	                }
	                if (table[i][j] > maxLen) {
	                    maxLen = table[i][j];
	                }
	            }
	        }
	    }
	    return maxLen;
	}
	public int getSharedWordNum(String input, String verb, String target) {
		int result = 0;
//		if( target.contains(verb)) { //make sure at least share the same verb
			if( input.length() <= target.length()) {
				String[] tokens = input.split(" ");
				for(int i=0; i<tokens.length; i++) {
					if( target.contains(tokens[i]))
						result++;
				}
			}
			else {
				String[] tokens = target.split(" ");
				for(int i=0; i<tokens.length; i++) {
					if( input.contains(tokens[i]))
						result++;
				}
			}
//		}
		return result;
	}
	public int getFrequence(String key) {
		if( expressions.containsKey(key))
			return expressions.get(key);
		else
			return 0;
	}
}
class VPLookUPThread implements Runnable {
	Socket clientSocket;
	VPLookUPThread(Socket cs) {
		clientSocket = cs;
	}
	public void run() {
		try {
//			clientSocket.setSoTimeout(3*60*1000);
			InputStream Is = clientSocket.getInputStream();
			OutputStream Os = clientSocket.getOutputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(Is));
			PrintStream Ps = new PrintStream(Os);
			String key;
			while( !clientSocket.isClosed() && ((key = br.readLine())!= null)) {
//				System.out.println("key = " + key);
				String[] tokens = key.split(",");
				String emoWord = tokens[0], VP = tokens[1], verb = tokens[2];
				emoObject obj = VPstats.table.get(emoWord);
				double value;
				if( obj != null) {
					value = obj.findBestMatchValue(VP, verb);
					obj.findVerbMatchValue(VP, verb);
				}
				else
					value = 0;
//				System.out.println("key = " + key +": " + value);
				Ps.println(value);
			}
			System.out.println("Thread ID:" + Thread.currentThread().getId() + " - Client closed socket.");
		}catch(Exception e){
			e.printStackTrace( );
		} 
	}
}