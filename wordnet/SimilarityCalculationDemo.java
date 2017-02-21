package wordnet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.HirstStOnge;
import edu.cmu.lti.ws4j.impl.JiangConrath;
import edu.cmu.lti.ws4j.impl.LeacockChodorow;
import edu.cmu.lti.ws4j.impl.Lesk;
import edu.cmu.lti.ws4j.impl.Lin;
import edu.cmu.lti.ws4j.impl.Path;
import edu.cmu.lti.ws4j.impl.Resnik;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;

public class SimilarityCalculationDemo {
        
        private static ILexicalDatabase db = new NictWordNet();
        private static RelatednessCalculator[] rcs = {
                        new HirstStOnge(db), new LeacockChodorow(db), new Lesk(db),  new WuPalmer(db), 
                        new Resnik(db), new JiangConrath(db), new Lin(db), new Path(db)
                        };
        
        @SuppressWarnings("unused")
		private static void run( String word1, String word2 ) {
                WS4JConfiguration.getInstance().setMFS(true);
                for ( RelatednessCalculator rc : rcs ) {
                        double s = rc.calcRelatednessOfWords(word1, word2);
                        System.out.println( rc.getClass().getName()+"\t"+s );
                }
        }
        public static double getLinSimilarity(String word1, String word2) {
        	double result = rcs[6].calcRelatednessOfWords(word1, word2);
        	return result;
        }
        public static void findSimilarPatterns(String patternFilePath, String oneTimePatternPath) throws IOException {
        	BufferedReader br;
    		br = new BufferedReader(new InputStreamReader(new FileInputStream(patternFilePath), "UTF-8"));
    		String line;
    		synObject allPatternList = new synObject("all"); 
    		while( (line = br.readLine()) != null) {
    			if( line.contains("===================") || line.matches("\\@[\\w-]+:") || line.contains("??"))  continue;
    			allPatternList.addPatternsCumm(line);
    		}
    		br.close();
    		
    		br = new BufferedReader(new InputStreamReader(new FileInputStream(oneTimePatternPath), "UTF-8"));
    		while( (line = br.readLine()) != null) {
    			ArrayList<patternEntry> currentMatched = allPatternList.getMatchedPatterns(line);
    			System.out.println("Existing patterns matched \"" + line + "\":");
    			for(int i=0; i<currentMatched.size(); i++) {
    				System.out.println(currentMatched.get(i).getKey());
    			}
    			System.out.println();
    		}
        }
        public static void main(String[] args) throws IOException {
//                long t0 = System.currentTimeMillis();
//                run( "act","moderate" );
//                long t1 = System.currentTimeMillis();
//                System.out.println( "Done in "+(t1-t0)+" msec." );
//        	System.out.println("Similarity between \"baseball\" and \"basketball\" = " + getLinSimilarity("baseball", "basketball"));
//        	System.out.println(getLinSimilarity("penguin", "penguin"));
        	String pn = "this a *";
        	String pwn = "this a pen";
        	twoDType minObj= patternLED(pwn,pn);
        	System.out.println("EDW: " + minObj.getX() + ", " + minObj.getY());
//        	findSimilarPatterns("emoPatternlst.txt", "test" + File.separator + "1timePatterns.txt");
//        	listSimilarPatterns("emo3785to6", "test" + File.separator + "1timePatterns.txt", "emoPatternlst.txt");
//        	listSimilarPatterns("emo3785to6", "test" + File.separator + "1timePatterns.txt", "test" + File.separator + "patternWemoStat_combine_new.txt");   	
//        	System.out.println("Edit distance between \"intention\" and \"execution\" = " + stringLED("intention", "execution"));
//        	String sentence1 = "Play with you";
//        	String sentence2 = "Play with him";
//        	String sentence3 = "Play with";
//        	System.out.println("Edit distance between \"" + sentence1 + "\" and \"" + sentence2 + "\" = " + sentenceLED(sentence1, sentence2));
//        	System.out.println("Edit distance between \"" + sentence1 + "\" and \"" + sentence3 + "\" = " + sentenceLED(sentence1, sentence3));
        }
        public static int getClassNum(String category) {
        	int result;
        	switch(category) {
        		case "anger":
        			result = 0;	break;
        		case "fear":
        			result = 1;	break;
        		case "disgust":
        			result = 2;	break;
        		case "joy":
        			result = 3;	break;
        		case "surprise":
        			result = 4;	break;
        		case "sadness":
        			result = 5;	break;
        		default:
        			result = -1;
        	}
        	return result;
        }
        public static void listSimilarPatterns(String emoClassFilePath, String oneTimePatternPath, String patternFilePath) throws IOException {
        	TreeMap<String, String> emoClassLookUp = new TreeMap<String, String>();
        	ArrayList<HashMap<String, String>> patternsInCategory = new ArrayList<HashMap<String, String>>();
        	TreeMap<String, String> oneTimePatterns = new TreeMap<String, String>();
        	BufferedReader br;
    		br = new BufferedReader(new InputStreamReader(new FileInputStream(emoClassFilePath), "UTF-8"));
    		String line;
    		while( (line = br.readLine())!= null) {
    			if(line.length() < 1) continue;
    			String synonym = line.split("\t")[0], category = line.split("\t")[1];
    			if( !emoClassLookUp.containsKey(synonym) )
    				emoClassLookUp.put(synonym, category);
    		}
    		br.close();
    		
    		br = new BufferedReader(new InputStreamReader(new FileInputStream(oneTimePatternPath), "UTF-8"));
    		while( (line = br.readLine())!= null) {
    			if( line.length()<1 ) continue;
    			if( !oneTimePatterns.containsKey(line))
    				oneTimePatterns.put(line, line);
    		}
    		br.close();
    		
    		for(int i=0; i<6; i++) {
    			HashMap<String, String> category = new HashMap<String, String>();
    			patternsInCategory.add(category);
    		}
    		
    		br = new BufferedReader(new InputStreamReader(new FileInputStream(patternFilePath), "UTF-8"));
    		int currentCategoryIndex = 0;
    		String currentEmoWord = "";
    		/*
    		while( (line = br.readLine())!= null) {
    			if( line.contains("===================")) {
//    				if( obj.getPatternTableSize() == 0.0) continue;
//    				allSum += obj.getPatternCountSum();
//    				table.put(currentEmoWord, obj);
    				continue;
    			}
    			if( line.matches("\\@[\\w-]+:")) {
    				currentEmoWord = line.replace("@", "");
    				currentEmoWord = currentEmoWord.replace(":", "");
    				String currentCategory = emoClassLookUp.get(currentEmoWord);
    				currentCategoryIndex = getClassNum(currentCategory);
    			}
    			else {
    				if( line.contains("??")) continue;	//invalid patterns
    				String validPattern = line.split("\t")[0];
    				HashMap<String, String> temp = patternsInCategory.get(currentCategoryIndex);
    				if( !temp.containsKey(validPattern))
    					temp.put(validPattern, currentEmoWord);
    				else {
    					String emoWordList = temp.get(validPattern);
    					emoWordList += ("::" + currentEmoWord);
    					temp.put(validPattern, emoWordList);
    				}
    				patternsInCategory.set(currentCategoryIndex, temp);
    			}
    		}*/
    		while( (line = br.readLine())!= null) {
    			if(line.length()<1) continue;
    			String[] tokens = line.split("\t");
    			if( tokens.length != 3 ) continue;
    			tokens[1] = tokens[1].replaceAll("\\s+", "");
//    			tokens[2] = tokens[2].replaceAll("\\s+", "");
//    			System.out.println(emoClassLookUp.get(tokens[1].replaceAll("\\s+", "")));
    			if( emoClassLookUp.get(tokens[1]) == null) continue;
    			if( oneTimePatterns.containsKey(tokens[2])) continue;
    			if( !tokens[1].equals(currentEmoWord)) {
    				currentEmoWord = tokens[1].replaceAll("\\s+", "");
    				currentCategoryIndex = getClassNum(emoClassLookUp.get(currentEmoWord));
    			}
    			if( line.contains("??")) continue;
    			String validPattern = tokens[2];
    			HashMap<String, String> temp = patternsInCategory.get(currentCategoryIndex);
				if( !temp.containsKey(validPattern))
					temp.put(validPattern, currentEmoWord);
				else {
					String emoWordList = temp.get(validPattern);
					emoWordList += ("::" + currentEmoWord);
					temp.put(validPattern, emoWordList);
				}
				patternsInCategory.set(currentCategoryIndex, temp);			
    		}
    		
    		br.close();
//    		ArrayList<String> oneTimePatternPair = new ArrayList<String>();
    		
    		br = new BufferedReader(new InputStreamReader(new FileInputStream(patternFilePath), "UTF-8"));
    		
    		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("mostSimilarPatternTest.txt"), "UTF-8"));
    		
    		while( (line = br.readLine())!= null) {
    			if( line.split("\t").length < 3) continue;
    			String[] lineTokens = line.split("\t");
    			String currentPattern = lineTokens[2];
    			if( oneTimePatterns.containsKey(currentPattern)) {
    				String category = emoClassLookUp.get(lineTokens[1].replaceAll("\\s+", ""));
    				if( category == null) continue;
    				int categoryIndex = getClassNum(category);
    				ArrayList<String> patternsWithinCategory = new ArrayList<String>(patternsInCategory.get(categoryIndex).keySet());
    				ArrayList<String> mostSimilar = getMostSimilarPatterns(currentPattern, patternsWithinCategory);
//    				System.out.println("For pattern \"" + currentPattern + "\":");
    				bw.write("For pattern \"" + currentPattern + "\":\n");
    				for(int i=0; i<mostSimilar.size(); i++) {
//    					System.out.println(mostSimilar.get(i));
    					bw.write(mostSimilar.get(i) + "; Related synonyms: " + patternsInCategory.get(categoryIndex).get(mostSimilar.get(i).split(" =")[0]) + "\n");
    				}
//    				System.out.println();
    				bw.write("\n");
    			}
    			else; //do nothing if it isn't one-time pattern
    		}
    		br.close();
    		bw.close();
        }
    	public static String getRootWord(String line) {
    		String[] tokens = line.split(" ");
    		String result = null;
    		for(int i=0; i<tokens.length; i++) {
    			if( tokens[i].contains("_ROOT"))
    				result = tokens[i].replace("_ROOT", ""); 
    		}
    		return result;
    	} 
        public static ArrayList<String> getMostSimilarPatterns(String inPattern, ArrayList<String> patternsList) {
        	ArrayList<String> results = new ArrayList<String>();
        	double currentMin = 999.0;
        	String rootWord = getRootWord(inPattern);
        	String pattern = inPattern.replace("_ROOT", "");
        	
        	for(int i=0; i<patternsList.size(); i++) {
        		String currentExaminingPattern = patternsList.get(i);
        		String currentExaminingPatternRoot = getRootWord(currentExaminingPattern);
        		currentExaminingPattern = currentExaminingPattern.replace("_ROOT", "");
        		if( !rootWord.equals(currentExaminingPatternRoot)) continue;
//        		String routing = "";
        		twoDType minObj = patternLED(pattern, currentExaminingPattern);
        		if( minObj.getWeightedLength() < currentMin) {
        			currentMin = minObj.getWeightedLength();
        			results.clear();
        			results.add(patternsList.get(i) + " = " + minObj.toString() + "; length = " + minObj.getWeightedLength() );
        		}
        		else if( minObj.getWeightedLength() == currentMin){
        			results.add(patternsList.get(i) + " = " + minObj.toString() + "; length = " + minObj.getWeightedLength() );
        		}
        		else {
        			; //do nothing
        		}
        	}
        	return results;
        }
        public static int stringLED(String str1, String str2) {
        	str1 = " " + str1;
        	str2 = " " + str2;
        	int[][] edTable = new int[str1.length()][str2.length()];
        	for(int i=0; i<str1.length(); i++)
        		edTable[i][0] = i;
        	for(int j=0; j<str2.length(); j++)
        		edTable[0][j] = j;
        	for(int i=1; i<str1.length(); i++) {
        		for(int j=1; j<str2.length(); j++) {
        			int option1 = edTable[i-1][j] + 1;
        			int option2 = edTable[i][j-1] + 1;
        			int option3 = edTable[i-1][j-1] + ((str1.charAt(i) == str2.charAt(j)) ? 0 : 2 );
        			edTable[i][j] = Math.min(option1, Math.min(option2, option3));
        		}
        	}
        	return edTable[str1.length()-1][str2.length()-1];
        }
        public static int sentenceLED(String sent1, String sent2) {
        	String[] sent1tokens = new String[1+sent1.split("\\s+").length];
        	String[] sent2tokens = new String[1+sent2.split("\\s+").length];
        	String[] empty = {" "};
        	System.arraycopy(empty, 0, sent1tokens, 0, 1);
        	System.arraycopy(sent1.split("\\s+"), 0, sent1tokens, 1, sent1.split("\\s+").length);
        	System.arraycopy(empty, 0, sent2tokens, 0, 1);
        	System.arraycopy(sent2.split("\\s+"), 0, sent2tokens, 1, sent2.split("\\s+").length);
        	int[][] edTable = new int[sent1tokens.length][sent2tokens.length];
        	for(int i=0; i<sent1tokens.length; i++)
        		edTable[i][0] = i;
        	for(int j=0; j<sent2tokens.length; j++)
        		edTable[0][j] = j;
        	for(int i=1; i<sent1tokens.length; i++) {
        		for(int j=1; j<sent2tokens.length; j++) {
        			int option1 = edTable[i-1][j] + 1;
        			int option2 = edTable[i][j-1] + 1;
        			int option3 = edTable[i-1][j-1] + ((sent1tokens[i].equals(sent2tokens[j])) ? 0 : 2 );
        			edTable[i][j] = Math.min(option1, Math.min(option2, option3));
        		}
        	}
        	return edTable[sent1tokens.length-1][sent2tokens.length-1];
        }
        public static twoDType patternLED(String pattern1, String pattern2) {
        	double wildCardIDCost = Math.E;
        	double wildCardRCost = Math.E - 1.0;
        	String[] pattern1tokens = new String[1+pattern1.split("\\s+").length];
        	String[] pattern2tokens = new String[1+pattern2.split("\\s+").length];
        	String[] empty = {" "};
        	System.arraycopy(empty, 0, pattern1tokens, 0, 1);
        	System.arraycopy(pattern1.split("\\s+"), 0, pattern1tokens, 1, pattern1.split("\\s+").length);
        	System.arraycopy(empty, 0, pattern2tokens, 0, 1);
        	System.arraycopy(pattern2.split("\\s+"), 0, pattern2tokens, 1, pattern2.split("\\s+").length);
        	
        	twoDType[][] edTable = new twoDType[pattern1tokens.length][pattern2tokens.length];
//        	String[][] routeTable = new String[pattern1tokens.length][pattern2tokens.length];
        	
//        	for(int i=0; i<pattern1tokens.length; i++)
//        		edTable[i][0] = new twoDType(i, 0);
//        	for(int j=0; j<pattern2tokens.length; j++)
//        		edTable[0][j] = new twoDType(j, 0);
        	      	
        	edTable[0][0] = new twoDType(0, 0, "");
//        	routeTable[0][0] = "";
        	for(int i=1; i<pattern1tokens.length; i++) {
        		if( pattern1tokens[i].equals("*")) {
        			edTable[i][0] = new twoDType(edTable[i-1][0].getX()+wildCardIDCost, 0, edTable[i-1][0].getRoute() + "D(" + pattern1tokens[i] + "); ");
        		}
        		else {
        			edTable[i][0] = new twoDType(edTable[i-1][0].getX()+1, 0, edTable[i-1][0].getRoute() + "D(" + pattern1tokens[i] + "); ");
        		}
        	}
        	for(int j=1; j<pattern2tokens.length; j++) {
        		if( pattern2tokens[j].equals("*")) {
        			edTable[0][j] = new twoDType(edTable[0][j-1].getX()+wildCardIDCost, 0, edTable[0][j-1].getRoute() + "I(" + pattern2tokens[j] + "); ");
        		}
        		else {
        			edTable[0][j] = new twoDType(edTable[0][j-1].getX()+1, 0, edTable[0][j-1].getRoute() + "I(" + pattern2tokens[j] + "); ");
        		}
        	}
        	
//        	String editRoute = "";
        	
        	for(int i=1; i<pattern1tokens.length; i++) {
        		for(int j=1; j<pattern2tokens.length; j++) {
        			twoDType option1, option2, option3;
        			
        			if( pattern1tokens[i].equals("*"))
        				option1 = new twoDType(edTable[i-1][j].getX()+wildCardIDCost, edTable[i-1][j].getY(), edTable[i-1][j].getRoute() + "D(" + pattern1tokens[i] + "); " );
        			else
        				option1 = new twoDType(edTable[i-1][j].getX()+1, edTable[i-1][j].getY(), edTable[i-1][j].getRoute() + "D(" + pattern1tokens[i] + "); ");
        			
        			if( pattern2tokens[j].equals("*"))
        				option2 = new twoDType(edTable[i][j-1].getX()+wildCardIDCost, edTable[i][j-1].getY(), edTable[i][j-1].getRoute() + "I(" + pattern2tokens[j] + "); ");
        			else
        				option2 = new twoDType(edTable[i][j-1].getX()+1, edTable[i][j-1].getY(), edTable[i][j-1].getRoute() + "I(" + pattern2tokens[j] + "); ");
        			
        			if( pattern1tokens[i].equals(pattern2tokens[j])) 
        				option3 = new twoDType(edTable[i-1][j-1].getX(), edTable[i-1][j-1].getY(), edTable[i-1][j-1].getRoute() + "S(" + pattern1tokens[i] + ", " + pattern2tokens[j] + "); ");
//        			else if (getLinSimilarity(pattern1tokens[i], pattern2tokens[j]) > 0.5) {
//        				option3 = new twoDType(edTable[i-1][j-1].getX(), edTable[i-1][j-1].getY(), edTable[i-1][j-1].getRoute() + "S(" + pattern1tokens[i] + ", " + pattern2tokens[j] + "); ");
//        			}
        			else {
        				if( pattern1tokens[i].equals("*"))
        					option3 = new twoDType(edTable[i-1][j-1].getX(), edTable[i-1][j-1].getY()+wildCardRCost, edTable[i-1][j-1].getRoute() + "S(" + pattern1tokens[i] + ", " + pattern2tokens[j] + "); ");
        				else if (pattern2tokens[j].equals("*"))
        					option3 = new twoDType(edTable[i-1][j-1].getX(), edTable[i-1][j-1].getY()+1, edTable[i-1][j-1].getRoute() + "S(" + pattern1tokens[i] + ", " + pattern2tokens[j] + "); ");
        				else {
    						option3 = new twoDType(edTable[i-1][j-1].getX()+2, edTable[i-1][j-1].getY(), edTable[i-1][j-1].getRoute() + "S(" + pattern1tokens[i] + ", " + pattern2tokens[j] + "); ");
        					/*	
        					if( pattern1tokens[i].matches("<\\w+>") || pattern2tokens[j].matches("<\\w+>"))
        						option3 = new twoDType(edTable[i-1][j-1].getX()+2, edTable[i-1][j-1].getY(), edTable[i-1][j-1].getRoute() + "S(" + pattern1tokens[i] + ", " + pattern2tokens[j] + "); ");
        					else {
        						double wordSemSim = getLinSimilarity(pattern1tokens[i], pattern2tokens[j]);
        						if( wordSemSim > 0.75)
        	        				option3 = new twoDType(edTable[i-1][j-1].getX(), edTable[i-1][j-1].getY(), edTable[i-1][j-1].getRoute() + "S(" + pattern1tokens[i] + ", " + pattern2tokens[j] + "); ");
        						else
            						option3 = new twoDType(edTable[i-1][j-1].getX()+2, edTable[i-1][j-1].getY(), edTable[i-1][j-1].getRoute() + "S(" + pattern1tokens[i] + ", " + pattern2tokens[j] + "); ");
        					}
        					*/
        				}
        			}
        			
        			
        			twoDType tempMin;
        			if( option1.getWeightedLength() <= option2.getWeightedLength()) {
        				tempMin = option1;
        			}
        			else {
        				tempMin = option2;
        			}
        				
        			if( option3.getWeightedLength() <= tempMin.getWeightedLength()) {
        				edTable[i][j] = option3;
        			}
        			else {
        				edTable[i][j] = tempMin;
        			}
        			edTable[i][j].setRoute(routeSimplify(edTable[i][j].getRoute()));
        		}
        	}
//        	routing = routeTable[pattern1tokens.length-1][pattern2tokens.length-1];
//        	System.out.println(routeTable[pattern1tokens.length-1][pattern2tokens.length-1]);
        	return edTable[pattern1tokens.length-1][pattern2tokens.length-1];

        }
        public static String routeSimplify(String routeString) {
        	String[] tokens = routeString.split("; ");
        	String result = "";
        	for(int i=0; i<tokens.length; i++) {
        		if( tokens[i].contains("S(")) {
        			String newToken = tokens[i].replace("S(", "");
        			newToken = newToken.replace(")", "");
        			String[] subTokens = newToken.split(", ");
        			if( subTokens[0].equals(subTokens[1]));
        			else
        				result += (tokens[i] + "; ");
        		}
        		else
        			result += (tokens[i] + "; ");
        	}
        	return result;
        }
}
class twoDType {
	private double x;
	private double y;
	private String route;
	public twoDType(double x, double y, String route) {
		this.x = x;
		this.y = y;
		this.route = route;
	}
	
	public void setX(double x) { this.x = x; }
	public void setY(double y) { this.y = y; }
	public void setXY(double x, double y) { this.x = x; this.y = y; }
	public void setRoute(String route) { this.route = route; }
	public double getX() { return x; }
	public double getY() { return y; }
	public String getRoute() { return route; }
	public String toString() {
		return ("(" + x + ", " + y + "): " + route );
	}
	public double getLength() {
		return (Math.sqrt((double)(x*x) + (double)(y*y)));
	}
	public double getWeightedLength() {
//		return (Math.sqrt((double)(x*x) + 1 + Math.log(y)*Math.log(y)));
		return (Math.sqrt((double)(x*x) + (double)(y*y)));
	}
}
