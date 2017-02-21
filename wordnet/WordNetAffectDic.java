package wordnet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.BreakIterator;
import java.util.*;

import net.didion.jwnl.*;
import net.didion.jwnl.data.*;
import net.didion.jwnl.data.relationship.*;
import net.didion.jwnl.dictionary.Dictionary;

public class WordNetAffectDic {
	public static HashMap Word2Offset = new HashMap();
	public static HashMap Offset2Word = new HashMap();
	public static void initialize() {
		String listFile = "../WordNetAffectEmotionLists/all.txt";
		try {
			BufferedReader br = new BufferedReader(new FileReader(listFile));
			String line;
			while ((line = br.readLine()) != null) {
//				System.out.println(line);
				//build the offset-to-synset dictionary
				Offset2Word.put(line.substring(0,10), line.substring(11));
				
				//build the word-to-offset dictionary
				String [] tokens = line.split(" "); //break with blank
				for(int i=1; i < tokens.length; i++) {
					if( Word2Offset.containsKey(tokens[i]) ) { //if the key already exists
						String prevValue = (String) Word2Offset.get(tokens[i]); //preserve previous value
						if( !(prevValue.contains(tokens[0])) ) //exclude the already-in cases
							Word2Offset.put(tokens[i], (prevValue+"_"+tokens[0]) );
					}
					else
						Word2Offset.put(tokens[i], tokens[0]);
				}
			}
			br.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static String[] outputEmotionWords() {
		ArrayList<String> collection = new ArrayList<String>();
		Iterator it = Word2Offset.keySet().iterator();
		while(it.hasNext()) {
			collection.add(it.next().toString());
		}
		String[] results = new String[collection.size()];
		results = collection.toArray(results);
		return results;
	}
	public static String findSynset(String key) {
		String synSetStr = "";
		String offset = (String) Word2Offset.get(key); //find the corresponding offset with the word
//		System.out.print(offset.length());
		String [] offTokens = offset.split("_"); //split with "_"
		for(int i=0; i < offTokens.length; i++)
			synSetStr = synSetMerge(synSetStr, offTokens[i]); //look up synsets and merge
//		System.out.println("\n"+offset);
//		System.out.println(synSetStr);
		return synSetStr;
		
	}
	
	public static String synSetMerge(String current, String offset) {
		String newSynSet = (String)Offset2Word.get(offset);
		String [] newSynSetTokens = newSynSet.split(" ");
		for(int i=0; i< newSynSetTokens.length; i++)
		{
			if( !(current.contains(newSynSetTokens[i])) ) //if not in current synset, adds to it
				current = current + newSynSetTokens[i] + " ";
		}
		return current;
	}
}