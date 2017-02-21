package wordnet;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.io.*;

import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.data.Synset;
import net.didion.jwnl.dictionary.Dictionary;
import wordnet.SynProcess.ValueComparator;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;

class PatternGenByDep {
	public static MaxentTagger tagger;
	public static HashMap<String, String> emotionWords = null;
  /**
   * The main method demonstrates the easiest way to load a parser.
   * Simply call loadModel and specify the path, which can either be a
   * file or any resource in the classpath.  For example, this
   * demonstrates loading from the models jar file, which you need to
   * include in the classpath for ParserDemo to work.
 * @throws IOException 
   */
	public static void buildEmotionHash(String path) {
		emotionWords = new HashMap<String, String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(path));
			String line;
			while( ( line = br.readLine()) != null) {
				if(!emotionWords.containsKey(line) )
					emotionWords.put(line, line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
  public static void main(String[] args) throws IOException, JWNLException {
//	  redoCorpus("corpus.txt");
//	  tagger = new MaxentTagger("tagger/english-bidirectional-distsim.tagger");
    LexicalizedParser lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
    WordNetHelper.initialize("file_properties.xml");
    buildEmotionHash("extendedList.txt");
//    getEmoPatterns2("parsed" + File.separator, "emoParsedPattern" + File.separator);
//    statsPattern2("emoParsedPattern" + File.separator, "training_0508_3.0_isp.txt",false);
//    statsPattern2("emoParsedPattern" + File.separator, "training_0508_3.0_esp.txt",true);
//    statsPattern2_N_E("emoParsedPattern" + File.separator, "training_0508_3.0_esp.txt",true, "symPerplexity" + File.separator);
    
//  getEmoPatternsRC("parsed" + File.separator, "emoParsedPatternRCI_noGn" + File.separator, true, true );
//  																					  //isp //usePPL
//  getEmoPatternsRC("parsed" + File.separator, "emoParsedPatternRCE_noGn" + File.separator, false, true );
//  statsPatternRC("emoParsedPatternRCI" + File.separator, "training_0618_isp.txt");
//  statsPatternRC("emoParsedPatternRCE" + File.separator, "training_0618_esp.txt");
//  getEmoPatternsRC_N_E("parsed" + File.separator, "emoParsedPatternRCI" + File.separator, "emoParsedPatternRCEmoI" + File.separator, "symPerplexity" + File.separator, "symPerplexityRCI" + File.separator, true );
//  getEmoPatternsRC_N_E("parsed" + File.separator, "emoParsedPatternRCE" + File.separator, "emoParsedPatternRCEmoE" + File.separator, "symPerplexity" + File.separator, "symPerplexityRCE" + File.separator, false );
  statsPatternRC_N_E("emoParsedPatternRCI_noGn" + File.separator, "emoParsedPatternRCEmoI" + File.separator, "symPerplexityRCI" + File.separator, "training_NE0707_isp.txt",true,true);
  statsPatternRC_N_E("emoParsedPatternRCE_noGn" + File.separator, "emoParsedPatternRCEmoI" + File.separator, "symPerplexityRCE" + File.separator, "training_NE0707_esp.txt",true,true);   
    
  }
	public static void getEmoPatterns2(String inputDir, String outputDir) throws IOException, JWNLException{
//		  new InputStreamReader(new FileInputStream(file), "UTF-8");
		  final File folder = new File(inputDir );
		  ArrayList<String> allInputFileList = PatternStats.listFilesForFolder(folder);
		  ArrayList<String> inputTxtFileList = SynReplace.getInputTxtFileList(allInputFileList);
		  TreeMap<String, String> classTable = buildClassTable("emo3785to2.txt");
		  TreeMap<String, String> classTable2 = buildClassTable("emo3785to6.txt");
//		  TreeMap<String, perpNode> perpTable = buildPerpTable("classPerplexity" + File.separator);
		  Double threshold = 3.0;
//		  SWN3 sentiDic = new SWN3();
		  
		  for(int j=0; j<inputTxtFileList.size(); j++) {	//note: for all emotion word
			  String currentFile = inputTxtFileList.get(j);
			  BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputDir + currentFile), "UTF-8"));
			  BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputDir + currentFile),"UTF-8"));
			  String inline;
			  int count = 0;
			  while( (inline = br.readLine()) != null )
			  {	
				  String currentWord = currentFile.replace(".txt", "");
				  currentWord = currentWord.replaceAll("\\s+", "");
				  String emotionWord = inline.split(">>")[0];
//				  if( !currentWord.equals(emotionWord) ) continue;
				  String line = linePreprocess(inline.split(">>")[1]);
//				  if( line.contains("n't")) System.out.println("Altert!");
//				  String cleanedLine = line.replaceAll("-\\d+", "");
//				  System.out.println(line);
				  if( line.contains("ROOT-0, |")) continue;
				  ArrayList<String> nodeList = new ArrayList<String>(Arrays.asList(line.split("\\|")));
//				  System.out.println(nodeList);
				  String rootValue = findTargetValue(nodeList, "ROOT-0");
				  if( rootValue == null) continue;
				  ArrayList<ArrayList<String>> rootRelated = findRootRelated(rootValue, nodeList);
				  // conjunction issue
				  if( checkRootRelated(rootRelated) == false) continue;
//				  TreeMap<String, perpNode> perpTable = buildPerpTable("symPerplexity" + File.separator, currentWord);
				  TreeMap<String, Double> perpTable = buildPerpTable("symPerplexity" + File.separator, currentWord);
				  for(int i=0; i<rootRelated.size(); i++) {
					  if( rootRelated.get(i) != null) {
				//		  System.out.println(rootRelated);
						  myPattern pat = new myPattern(rootRelated.get(i));
				//		  pat.printPattern();
//						  String patternStr = pat.getPatternString();	//no perplexity
//						  String sixCat = classTable2.get(emotionWord);
						  String patternStr = pat.getPatternString(perpTable, currentWord, threshold, false, true, true);
						  if( patternStr != null /* && !patternStr.contains(emotionWord)  */ ) {
//							  String category = classTable.get(emotionWord);
							  //penguin0423
							  if( patternStr.contains("=<")) {
									String genStr = patternStr.replaceAll("\\w+=", "");
									String oriStr = patternStr.replaceAll("=<\\w+>", "");
									
									//for isp mode.
									bw.write(emotionWord + "," + genStr + "\n");
									bw.write(emotionWord + "," + oriStr + "\n");
									
									//for esp mode.
//									if( !patternStr.contains(emotionWord)){
//										bw.write(emotionWord + "," + genStr + "\n");
//										bw.write(emotionWord + "," + oriStr + "\n");
//								  	}
							  }		
							  else {
								  	//for isp mode
									bw.write(emotionWord + "," + patternStr + "\n");
									
									//for esp mode.
//									if( !patternStr.contains(emotionWord))
//										bw.write(emotionWord + "," + patternStr + "\n");						  
							  }
							  //penguin0423
							
						  }
					  }
				  }
				  count++;
				  if( (count % 1000) == 0)
					  System.out.println("..."); 
			  }
			  br.close();
			  bw.close();
		  }  
	  }
	public static void getEmoPatternsRC(String inputDir, String outputDir, Boolean ISP, Boolean usePPL) throws IOException, JWNLException{
//		  new InputStreamReader(new FileInputStream(file), "UTF-8");sadfsadfsafasf
		  final File folder = new File(inputDir );
		  ArrayList<String> allInputFileList = PatternStats.listFilesForFolder(folder);
		  ArrayList<String> inputTxtFileList = SynReplace.getInputTxtFileList(allInputFileList);
		  TreeMap<String, String> classTable = buildClassTable("emo3785to2.txt");
		  TreeMap<String, String> classTable2 = buildClassTable("emo3785to6.txt");
//		  TreeMap<String, perpNode> perpTable = buildPerpTable("classPerplexity" + File.separator);
		  TreeMap<String, Double> perpTable = new TreeMap<String, Double>();
		  Double threshold = 3.0;
//		  SWN3 sentiDic = new SWN3();
		  
		  for(int j=0; j<inputTxtFileList.size(); j++) {	//note: for all emotion word
			  String currentFile = inputTxtFileList.get(j);
			  BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputDir + currentFile), "UTF-8"));
			  BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputDir + currentFile),"UTF-8"));
			  String inline;
			  int count = 0;
			  while( (inline = br.readLine()) != null )
			  {	
				  String currentWord = currentFile.replace(".txt", "");
				  currentWord = currentWord.replaceAll("\\s+", "");
				  String emotionWord = inline.split(">>")[0];
//				  if( !currentWord.equals(emotionWord) ) continue;
				  String line = linePreprocess(inline.split(">>")[1]);
//				  if( line.contains("n't")) System.out.println("Altert!");
//				  String cleanedLine = line.replaceAll("-\\d+", "");
//				  System.out.println(line);
				  if( line.contains("ROOT-0, |")) continue;
				  ArrayList<String> nodeList = new ArrayList<String>(Arrays.asList(line.split("\\|")));
//				  System.out.println(nodeList);
				  String rootValue = findTargetValue(nodeList, "ROOT-0");
				  if( rootValue == null) continue;
				  ArrayList<ArrayList<String>> rootRelated = findRootRelated(rootValue, nodeList);
				  // conjunction issue
				  if( checkRootRelated(rootRelated) == false) continue;
				  if( usePPL) {
//					  TreeMap<String, perpNode> perpTable = buildPerpTable("symPerplexity" + File.separator, currentWord);
					  perpTable = buildPerpTable("symPerplexity" + File.separator, currentWord);					  
				  }
				  for(int i=0; i<rootRelated.size(); i++) {
					  if( rootRelated.get(i) != null) {
				//		  System.out.println(rootRelated);
						  myPattern pat = new myPattern(rootRelated.get(i));
				//		  pat.printPattern();
//						  String patternStr = pat.getPatternString();	//no perplexity
//						  String sixCat = classTable2.get(emotionWord);
						  String patternStr = pat.getPatternString(perpTable, currentWord, threshold, false, usePPL, true);
//                                                                                                    runtime usePPL
						  if( patternStr != null /* && !patternStr.contains(emotionWord)  */ ) {
//							  String category = classTable.get(emotionWord);
							  //penguin0423
							  if( patternStr.contains("=<")) {
									String genStr = patternStr.replaceAll("\\w+=", "");
									String oriStr = patternStr.replaceAll("=<\\w+>", "");
									
									if(ISP) {
										bw.write(emotionWord + "," + genStr + "\n");
										bw.write(emotionWord + "," + oriStr + "\n");
									}
									else {
										if( !patternStr.contains(emotionWord)){
											bw.write(emotionWord + "," + genStr + "\n");
											bw.write(emotionWord + "," + oriStr + "\n");
									  	}
									}	
							  }		
							  else {
								  if(ISP) {
									  bw.write(emotionWord + "," + patternStr + "\n");
									  
								  } 
								  else {
									  if( !patternStr.contains(emotionWord))
											bw.write(emotionWord + "," + patternStr + "\n");
								  }					  
							  }
							  //penguin0423
							
						  }
					  }
				  }
				  count++;
				  if( (count % 1000) == 0)
					  System.out.println("..."); 
			  }
			  br.close();
			  bw.close();
		  }  
	  }
	public static void getEmoPatternsRC_N_E(String inputDir, String outputDir, String emoDir, String pplInDir, String pplOutDir, Boolean ISP) throws IOException, JWNLException{
//		  new InputStreamReader(new FileInputStream(file), "UTF-8");sadfsadfsafasf
		  final File folder = new File(inputDir );
		  ArrayList<String> allInputFileList = PatternStats.listFilesForFolder(folder);
		  ArrayList<String> inputTxtFileList = SynReplace.getInputTxtFileList(allInputFileList);
		  TreeMap<String, String> classTable = buildClassTable("emo3785to2.txt");
		  TreeMap<String, String> classTable2 = buildClassTable("emo3785to6.txt");
//		  TreeMap<String, perpNode> perpTable = buildPerpTable("classPerplexity" + File.separator);
		  TreeMap<String, Double> perpTable = new TreeMap<String, Double>();
		  Double threshold = 3.0;
		  BufferedWriter bwErr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("ErrPatterns.txt"),"UTF-8"));
//		  SWN3 sentiDic = new SWN3();
		  
		  for(int j=0; j<inputTxtFileList.size(); j++) {	//note: for all emotion word
			  String currentFile = inputTxtFileList.get(j);
			  BufferedReader brP = new BufferedReader(new InputStreamReader(new FileInputStream(inputDir + currentFile), "UTF-8"));
			  BufferedReader brN = new BufferedReader(new InputStreamReader(new FileInputStream(pplInDir + currentFile.replace(".txt", "")), "UTF-8"));

			  BufferedWriter bwP = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputDir + currentFile),"UTF-8"));
			  BufferedWriter bwE = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(emoDir + currentFile),"UTF-8"));
			  BufferedWriter bwN = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pplOutDir + currentFile),"UTF-8"));
			  
			  ArrayList<String> nounList = new ArrayList<String>();
			  String inline;
			  while( (inline = brN.readLine()) != null ) {
				  String noun = inline.split("\t")[0];
				  Double ppl = Double.parseDouble(inline.split("\t")[1]);
				  if(ppl<threshold) {
					  nounList.add(noun);
				  }
			  }
			  brN.close();
			  
			  int count = 0;
			  while( (inline = brP.readLine()) != null )
			  {	
				  String currentWord = currentFile.replace(".txt", "");
				  currentWord = currentWord.replaceAll("\\s+", "");
				  String emotionWord = inline.split(">>")[0];
//				  if( !currentWord.equals(emotionWord) ) continue;
				  String line = linePreprocess(inline.split(">>")[1]);
//				  if( line.contains("n't")) System.out.println("Altert!");
//				  String cleanedLine = line.replaceAll("-\\d+", "");
//				  System.out.println(line);
				  if( line.contains("ROOT-0, |")) continue;
				  ArrayList<String> nodeList = new ArrayList<String>(Arrays.asList(line.split("\\|")));
				  
				  for(int i=0;i<nodeList.size();i++) {
					  try 
					  {
						  String reln=nodeList.get(i).split("\\(")[0];
//						  System.out.println(reln);
						  String nodeWord=nodeList.get(i).split(", ")[1];
						  String terms[]=nodeWord.split("-");
						  String endTerm=terms[terms.length-1];
						  nodeWord=nodeWord.replace("-" + endTerm, "");
						  if (reln.contains("obj")) {
							  if(nounList.contains(nodeWord)) {
								  bwN.write(emotionWord + "," + nodeWord + "\n");
							  }
						  }
						  if(emotionWords.containsKey(nodeWord)) {
							  bwE.write(emotionWord + "," + nodeWord + "\n");
						  }
					  }
					  catch (Exception e) {
						  bwErr.write(nodeList+" : "+currentWord + "\n");
					  }
				  }
//				  System.out.println("nodeList:" + nodeList);
				  String rootValue = findTargetValue(nodeList, "ROOT-0");
				  if( rootValue == null) continue;
				  ArrayList<ArrayList<String>> rootRelated = findRootRelated(rootValue, nodeList);
				  // conjunction issue
				  if( checkRootRelated(rootRelated) == false) continue;
//				  TreeMap<String, perpNode> perpTable = buildPerpTable("symPerplexity" + File.separator, currentWord);
//				  TreeMap<String, Double> perpTable = buildPerpTable("symPerplexity" + File.separator, currentWord);
				  for(int i=0; i<rootRelated.size(); i++) {
					  if( rootRelated.get(i) != null) {
				//		  System.out.println(rootRelated);
						  myPattern pat = new myPattern(rootRelated.get(i));
				//		  pat.printPattern();
//						  String patternStr = pat.getPatternString();	//no perplexity
//						  String sixCat = classTable2.get(emotionWord);
						  String patternStr = pat.getPatternString(perpTable, currentWord, threshold, false, false, true);
//                                                                                                  runtime usePPL
						  if( patternStr != null /* && !patternStr.contains(emotionWord)  */ ) {
//							  String category = classTable.get(emotionWord);
							  //penguin0423
							  if( patternStr.contains("=<")) {
									String genStr = patternStr.replaceAll("\\w+=", "");
									String oriStr = patternStr.replaceAll("=<\\w+>", "");
									
									if(ISP) {
										bwP.write(emotionWord + "," + genStr + "\n");
										bwP.write(emotionWord + "," + oriStr + "\n");
									}
									else {
										if( !patternStr.contains(emotionWord)){
											bwP.write(emotionWord + "," + genStr + "\n");
											bwP.write(emotionWord + "," + oriStr + "\n");
									  	}
									}	
							  }		
							  else {
								  if(ISP) {
									  bwP.write(emotionWord + "," + patternStr + "\n");
									  
								  } 
								  else {
									  if( !patternStr.contains(emotionWord))
											bwP.write(emotionWord + "," + patternStr + "\n");
								  }					  
							  }
							  //penguin0423
							
						  }
					  }
				  }
				  count++;
				  if( (count % 1000) == 0)
					  System.out.println("..."); 
			  }
			  bwE.close();
			  bwN.close();
			  brP.close();
			  bwP.close();
		  }  
	  }
  public static void statsPattern2(String inputDir, String outputFile, Boolean ESP) throws IOException {	  
	  TreeMap<String, String> dirtyList = new TreeMap<String, String>();
	  TreeMap<String, Integer> statTable = new TreeMap<String, Integer>();
	  ArrayList<String> allEmo = new ArrayList<String>();
	  BufferedReader br;
	  br = new BufferedReader(new InputStreamReader(new FileInputStream("test" + File.separator + "dirtyPatternList.txt"), "UTF-8"));
	  String line;
	  while( (line = br.readLine())!= null) {
		  if( line.length() < 1) continue;
		  if( !dirtyList.containsKey(line))
			  dirtyList.put(line, line);
	  }
	  br.close();
	  
	  br = new BufferedReader(new FileReader("allEmo.txt"));
	  while( (line = br.readLine()) != null) {
		  allEmo.add(line);
	  }
	  br.close();
	  
	  final File folder = new File(inputDir );
	  ArrayList<String> allInputFileList = PatternStats.listFilesForFolder(folder);
	  ArrayList<String> inputTxtFileList = SynReplace.getInputTxtFileList(allInputFileList);
	  for(int i=0; i<inputTxtFileList.size(); i++) {
		  String currentFile = inputTxtFileList.get(i);	  
		  String currentEmoWord = currentFile.replace(".txt", "");
		  br = new BufferedReader(new InputStreamReader(new FileInputStream(inputDir + currentFile), "UTF-8"));		  
		  String key;
		  int count = 0;
		  while( (key = br.readLine()) != null) {
			  if( key.equals("") || key.split(",").length<2) continue;
			  String patternPart = key.split(",")[1];
			  if( dirtyList.containsKey(patternPart) || !patternPart.contains("_ROOT")) continue;
			  if( rootNotWord(patternPart)) continue;
			  if( ESP) {
				  if( detectIS(patternPart,allEmo) ) continue;
			  }
			  //true if pattern contains one of the emotion word
//			  if( patternPart.contains(currentEmoWord)) continue;
			  
			  statTable = putIntoStatTableTop(key, statTable);
			  
			  key = key.replace(key.split(",")[0], currentEmoWord);
			  statTable = putIntoStatTableTop(key, statTable);
			  
//			  if( key.contains("=<")) { //generalized one.
//				  String genKey = key.replaceAll("\\w+=", "");
//				  String oriKey = key.replaceAll("=<\\w+>", "");
//				  statTable = putIntoStatTable(statTable, genKey);
//				  statTable = putIntoStatTable(statTable, oriKey);
//			  }
//			  else { //non-generalized one.
//				  statTable = putIntoStatTable(statTable, key);
//			  }
			  count++;
		  }
		  br.close();
	  }
	  BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile),"UTF-8"));
//		  System.out.println(count);
	  Iterator<Entry<String, Integer>> it = statTable.entrySet().iterator();
	  String emoWord = "";
	  while( it.hasNext()) {
		  Entry<String, Integer> current = it.next();
		  if( current.getValue() > 0) {
			  String[]  currentTokens = current.getKey().split(",");
			  String currentEmoWord = currentTokens[0], currentPattern = currentTokens[1];
			  if( !currentEmoWord.equals(emoWord) ) {
				  emoWord = currentEmoWord;
				  if(!emoWord.equals(""))
					  bw.write("===================\n");
				  bw.write("@"+emoWord+":\n");
			  }
			  bw.write(currentPattern + "\t" + current.getValue() + "\n");
		  }
	  }
	  bw.write("===================\n");
	  bw.close();	 
  } 
  public static void statsPatternRC(String inputDir, String outputFile) throws IOException {	  
	  TreeMap<String, String> dirtyList = new TreeMap<String, String>();
	  TreeMap<String, Integer> statTable = new TreeMap<String, Integer>();
	  ArrayList<String> allEmo = new ArrayList<String>();
	  BufferedReader br;
	  br = new BufferedReader(new InputStreamReader(new FileInputStream("test" + File.separator + "dirtyPatternList.txt"), "UTF-8"));
	  String line;
	  while( (line = br.readLine())!= null) {
		  if( line.length() < 1) continue;
		  if( !dirtyList.containsKey(line))
			  dirtyList.put(line, line);
	  }
	  br.close();
	  
	  br = new BufferedReader(new FileReader("allEmo.txt"));
	  while( (line = br.readLine()) != null) {
		  allEmo.add(line);
	  }
	  br.close();
	  
	  final File folder = new File(inputDir );
	  ArrayList<String> allInputFileList = PatternStats.listFilesForFolder(folder);
	  ArrayList<String> inputTxtFileList = SynReplace.getInputTxtFileList(allInputFileList);
	  for(int i=0; i<inputTxtFileList.size(); i++) {
		  String currentFile = inputTxtFileList.get(i);	  
		  String currentEmoWord = currentFile.replace(".txt", "");
		  br = new BufferedReader(new InputStreamReader(new FileInputStream(inputDir + currentFile), "UTF-8"));		  
		  String key;
		  int count = 0;
		  while( (key = br.readLine()) != null) {
			  if( key.equals("") || key.split(",").length<2) continue;
			  String patternPart = key.split(",")[1];
			  if( dirtyList.containsKey(patternPart) || !patternPart.contains("_ROOT")) continue;
			  if( rootNotWord(patternPart)) continue;
//			  if( ESP) {
//				  if( detectIS(patternPart,allEmo) ) continue;
//			  }
			  //true if pattern contains one of the emotion word
			  if( patternPart.contains(currentEmoWord)) continue;
			  
			  statTable = putIntoStatTableTop(key, statTable);
			  
			  key = key.replace(key.split(",")[0], currentEmoWord);
			  statTable = putIntoStatTableTop(key, statTable);
			  
//			  if( key.contains("=<")) { //generalized one.
//				  String genKey = key.replaceAll("\\w+=", "");
//				  String oriKey = key.replaceAll("=<\\w+>", "");
//				  statTable = putIntoStatTable(statTable, genKey);
//				  statTable = putIntoStatTable(statTable, oriKey);
//			  }
//			  else { //non-generalized one.
//				  statTable = putIntoStatTable(statTable, key);
//			  }
			  count++;
		  }
		  br.close();
	  }
	  BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile),"UTF-8"));
//		  System.out.println(count);
	  Iterator<Entry<String, Integer>> it = statTable.entrySet().iterator();
	  String emoWord = "";
	  while( it.hasNext()) {
		  Entry<String, Integer> current = it.next();
		  if( current.getValue() > 0) {
			  String[]  currentTokens = current.getKey().split(",");
			  String currentEmoWord = currentTokens[0], currentPattern = currentTokens[1];
			  if( !currentEmoWord.equals(emoWord) ) {
				  emoWord = currentEmoWord;
				  if(!emoWord.equals(""))
					  bw.write("===================\n");
				  bw.write("@"+emoWord+":\n");
			  }
			  bw.write(currentPattern + "\t" + current.getValue() + "\n");
		  }
	  }
	  bw.write("===================\n");
	  bw.close();	 
  } 
  public static void statsPatternRC_N_E(String inputPDir, String inputEDir, String inputNDir, String outputFile, Boolean useE, Boolean useN) throws IOException {	  
	  TreeMap<String, String> dirtyList = new TreeMap<String, String>();
	  TreeMap<String, Integer> statTable = new TreeMap<String, Integer>();
	  ArrayList<String> allEmo = new ArrayList<String>();
	  BufferedReader br,brP,brE,brN;
	  br = new BufferedReader(new InputStreamReader(new FileInputStream("test" + File.separator + "dirtyPatternList.txt"), "UTF-8"));
	  String line;
	  while( (line = br.readLine())!= null) {
		  if( line.length() < 1) continue;
		  if( !dirtyList.containsKey(line))
			  dirtyList.put(line, line);
	  }
	  br.close();
	  
	  br = new BufferedReader(new FileReader("allEmo.txt"));
	  while( (line = br.readLine()) != null) {
		  allEmo.add(line);
	  }
	  br.close();
	  
	  final File folder = new File(inputPDir );
	  ArrayList<String> allInputFileList = PatternStats.listFilesForFolder(folder);
	  ArrayList<String> inputTxtFileList = SynReplace.getInputTxtFileList(allInputFileList);
	  for(int i=0; i<inputTxtFileList.size(); i++) {
		  String currentFile = inputTxtFileList.get(i);	  
		  String currentEmoWord = currentFile.replace(".txt", "");
		  brP = new BufferedReader(new InputStreamReader(new FileInputStream(inputPDir + currentFile), "UTF-8"));		  
		  String key;
		  int count = 0;
		  while( (key = brP.readLine()) != null) {
			  if( key.equals("") || key.split(",").length<2) continue;
			  String patternPart = key.split(",")[1];
			  if( dirtyList.containsKey(patternPart) || !patternPart.contains("_ROOT")) continue;
			  if( rootNotWord(patternPart)) continue;
//			  if( ESP) {
//				  if( detectIS(patternPart,allEmo) ) continue;
//			  }
			  //true if pattern contains one of the emotion word
			  if( patternPart.contains(currentEmoWord)) continue;
			  
			  statTable = putIntoStatTableTop(key, statTable);
			  
			  key = key.replace(key.split(",")[0], currentEmoWord);
			  statTable = putIntoStatTableTop(key, statTable);
			  
//			  if( key.contains("=<")) { //generalized one.
//				  String genKey = key.replaceAll("\\w+=", "");
//				  String oriKey = key.replaceAll("=<\\w+>", "");
//				  statTable = putIntoStatTable(statTable, genKey);
//				  statTable = putIntoStatTable(statTable, oriKey);
//			  }
//			  else { //non-generalized one.
//				  statTable = putIntoStatTable(statTable, key);
//			  }
			  count++;
		  }
		  brP.close();
		  
		  if(useE) {
			  brE = new BufferedReader(new InputStreamReader(new FileInputStream(inputEDir + currentFile), "UTF-8"));
			  while( (key = brE.readLine()) != null) {
				  statTable = putIntoStatTableTop(key, statTable);
			  }
			  brE.close();
		  }
		  
		  if(useN) {
			  brN = new BufferedReader(new InputStreamReader(new FileInputStream(inputNDir + currentFile), "UTF-8"));
			  while( (key = brN.readLine()) != null) {
				  statTable = putIntoStatTableTop(key, statTable);
			  }
			  brN.close();
		  }
		  
	  }
	  BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile),"UTF-8"));
//		  System.out.println(count);
	  Iterator<Entry<String, Integer>> it = statTable.entrySet().iterator();
	  String emoWord = "";
	  while( it.hasNext()) {
		  Entry<String, Integer> current = it.next();
		  if( current.getValue() > 0) {
			  String[]  currentTokens = current.getKey().split(",");
			  try {
			  String currentEmoWord = currentTokens[0], currentPattern = currentTokens[1];
			  
			  if( !currentEmoWord.equals(emoWord) ) {
				  emoWord = currentEmoWord;
				  if(!emoWord.equals(""))
					  bw.write("===================\n");
				  bw.write("@"+emoWord+":\n");
			  }
			  bw.write(currentPattern + "\t" + current.getValue() + "\n");
			  }
			  catch (Exception e) {
				  System.out.println(currentTokens);
			  }
		  }
	  }
	  bw.write("===================\n");
	  bw.close();	 
  } 
  public static boolean detectIS (String patternString, ArrayList<String> allEmo) throws IOException {
	  Iterator<String> it = allEmo.iterator();
	  String emo;
	  int i;
	  int matchIndex;
	  while( it.hasNext()) {
		  emo=it.next();
		  matchIndex=patternString.indexOf(emo, 0);
		  i=matchIndex+1;
		  while(i<patternString.length()-1 && matchIndex!=-1) {
			  if (matchIndex==0) {
				  //in the beginning
				  if (patternString.charAt(emo.length())==' ' || patternString.charAt(emo.length())=='_'){
					  // space means the whole word match, underline means this emo as root 
					  return true;
				  }
			  }
			  else if (matchIndex>0) {
				  //not in the beginning
				  if (patternString.charAt(matchIndex+emo.length())==' ' || patternString.charAt(matchIndex+emo.length())=='_'){
					  // the end is ok
					  if (patternString.charAt(matchIndex-1)==' '){
						  // the beginning is ok
						  return true;
					  }
				  }
			  }
			  matchIndex=patternString.indexOf(emo, i);
			  i=matchIndex+1;
		  }
	  }
	  return false;
  }
  
  public static void getSeedWords(String path, String output) throws IOException {
	  BufferedReader br = new BufferedReader(new FileReader(path));
	  BufferedWriter bw = new BufferedWriter(new FileWriter(output));
	  String line;
	  while( (line = br.readLine()) != null) {
		  String[] tokens = line.split(", ");
		  for(int i=0; i<tokens.length; i++) {
			  if( emotionWords.containsKey(tokens[i]))
				  bw.write(tokens[i] + "\n");
		  }
	  }
	  br.close();
	  bw.close();
  }
  public static boolean rootNotWord(String pattern) {
	  String[] tokens = pattern.split(" ");
	  String rootToken = null;
	  for(int i=0; i<tokens.length; i++) {
		  if( tokens[i].contains("_ROOT")) {
			  rootToken = tokens[i];
			  break;
		  }
	  }
	  if( rootToken.matches("[a-zA-Z_-]+_ROOT") )
		  return false;
	  else
		  return true;
  }
  public static TreeMap<String, Integer> putIntoStatTable(TreeMap<String, Integer> statTable, String key) {
	  if( !statTable.containsKey(key) ) {
		  statTable.put(key, 1);
	  }
	  else {
		  statTable.put(key, statTable.get(key)+1);
	  }
	  return statTable;
  }
  
  public static TreeMap<String, Integer> putIntoStatTableTop(String key, TreeMap<String, Integer> statTable) {
	  if( key.contains("=<")) { //generalized one.

//		  String genKey = key.replaceAll("\\w+=", "");
		  String[] tokens = key.split(" ");
		  String genKey="";
		  for(int i=0; i<tokens.length;i++) {
			  if(tokens[i].contains("=<")) {
				  tokens[i]=tokens[i].replace(tokens[i].split("=")[0] + "=", "");
			  }
		  }
		  for(int i=0; i<tokens.length; i++) {
				genKey += tokens[i] + " ";
		  }
		  String oriKey = key.replaceAll("=<\\w+>", "");
//		  System.out.println(key);
//		  System.out.println(genKey);
//		  System.out.println(oriKey);
		  statTable = putIntoStatTable(statTable, genKey);
		  statTable = putIntoStatTable(statTable, oriKey);
	  }
	  else { //non-generalized one.
		  statTable = putIntoStatTable(statTable, key);
	  }
	  return statTable;
  }
  public static String findTargetValue(ArrayList<String> nodeList, String targetValue) {
	  Iterator<String> it = nodeList.iterator();
	  String rootValue = null;
	  try {
		  while(it.hasNext()) {
			  String current = it.next();
			  if( current.contains(targetValue)) {
				  rootValue = current.split(", ")[1];
				  rootValue = rootValue.replaceAll("\\)", "");
				  break;
			  }
		  }
	  }
	  catch (Exception e) {
		  return null;
	  }
	  return rootValue;
  }
  public static ArrayList<ArrayList<String>> breakConjuncted(ArrayList<String> preResult, ArrayList<ArrayList<String>> preRelatedList, String rootValue) {
	  ArrayList<String> results = preResult;
	  ArrayList<ArrayList<String>> relateList = preRelatedList;
	  ArrayList<String> dobjList = getAllReln(results, "dobj");
	  ArrayList<String> pobjList = getAllReln(results, "prep_");
//	  ArrayList<String> iobjList = getAllReln(results, "iobj");
	  
	  relateList.add(results);
	  if( dobjList.size() > 1) {
		  int relateListSize = relateList.size();
		  for(int j=0; j<relateListSize; j++) {
			  results = relateList.get(j);
			  relateList.remove(results);
			  for(int i=0; i<dobjList.size(); i++)
				  results.remove(dobjList.get(i));
			  for(int i=0; i<dobjList.size(); i++) {
				  ArrayList<String> tempResult = new ArrayList(results);
				  tempResult.add(dobjList.get(i));
				  relateList.add(tempResult);
			  }
		  }
	  }
	  if( pobjList.size() > 1) {
		  int relateListSize = relateList.size();
		  for(int i=0; i<relateListSize; i++) {
			  results = relateList.get(i);
			  relateList.remove(results);
			  for(int j=0; j<pobjList.size(); j++)
				  results.remove(pobjList.get(j));
			  for(int j=0; j<pobjList.size(); j++) {
				  ArrayList<String> tempResult = new ArrayList(results);
				  tempResult.add(pobjList.get(j));
				  relateList.add(tempResult);
			  }
		  }
	  }
	  return relateList;
  }
  public static ArrayList<ArrayList<String>> findRootRelated(String rootValue, ArrayList<String> nodeList) {
	  if( rootValue != null) {
		  ArrayList<ArrayList<String>> relateList = new ArrayList<ArrayList<String>>();
		  ArrayList<String> results = new ArrayList();
		  Iterator<String> it = nodeList.iterator();
		  while(it.hasNext()) {
			  String current = it.next();
			  if(current.contains(rootValue)) {
				  if( current.contains("ROOT-0"))
					  results.add(0, current);
				  else
					  results.add(current);
			  }
		  }
		  
		  
		  ArrayList<String> rootCCList = findCCtoRoot(results, rootValue);
		  
		  //Here only consider those related to "root" (ignore those connected to root by conjunction.)
		  for(int i=0; i<rootCCList.size(); i++)
			  results.remove(rootCCList.get(i));
		  
		  for(int i=0; i<rootCCList.size(); i++) {
			  String ccToRootWord = rootCCList.get(i).split(", ")[1].replaceAll("\\)", "");
			  ArrayList<String> ccToRootRelated = getAllReln(nodeList, ccToRootWord);
			  
			  ccToRootRelated.remove(rootCCList.get(i)); //ignore itself (since adding this node will include "root") 
			  String adjusted = "root(ROOT-0, "+ccToRootWord+")";
			  ccToRootRelated.add(0, adjusted);
			  relateList.add(ccToRootRelated);
		  }
		  	  
		  relateList = breakConjuncted(results, relateList, rootValue);  
		  
//		  System.out.println(relateList);
		  return relateList;
	  }
	  else
		  return null;
  }
  public static ArrayList<String> findCCtoRoot(ArrayList<String> rootRelated, String rootValue) {
	  ArrayList<String> results = new ArrayList<String>();
	  Iterator<String> it = rootRelated.iterator();
	  while(it.hasNext()) {
		  String current = it.next();
		  if( /* (current.contains(rootValue) && current.contains("root")) || */ (current.contains(rootValue) && current.contains("conj")) ) {
//			  current = current.split(", ")[1].replaceAll("\\)", "");
			  results.add(current);
		  }
	  }
	  return results;
  }
  public static String findRelnDepFirst(ArrayList<String> rootRelated, String reln) {
	  Iterator<String> it = rootRelated.iterator();
	  String result = null;
	  while(it.hasNext()) {
		  String current = it.next();
		  if( current.contains(reln)) {
			  result = current.split(", ")[1].replaceAll("-\\d+\\)", "");
			  break;
		  }
	  }
	  return result;
  }
  public static ArrayList<String> getAllReln(ArrayList<String> rootRelated, String reln) {
	  ArrayList<String> results = new ArrayList<String>();
	  Iterator<String> it = rootRelated.iterator();
	  while( it.hasNext()) {
		  String current = it.next();
		  if( current.contains(reln))
			  results.add(current);
	  }
	  return results;
  }
  public static String linePreprocess(String line) {
	  line = line.replace("), ", ")|");
	  line = line.replace("'re", "be");
	  line = line.replace("'m", "be");
	  line = line.replace("'s", "be");
	  line = line.replace("’re", "be");
	  line = line.replace("’m", "be");
	  line = line.replace("’s", "be");
	  line = line.replace("'ve", "have");
	  line = line.replace("'d", "would");
	  line = line.replace("'ll", "will");
//	  line = line.replace("\\(m-", "\\(be-");
//	  line = line.replace(", m-", ", be-");
	  line = line.replace("n't", "not");
	  line = line.replace("n’t", "not");
	  line = line.replace("isn", "not");
	  line = line.replace("aren", "not");
	  line = line.replace("isn", "not");
	  line = line.replace("wasn", "not");
	  line = line.replace("weren", "not");
	  line = line.replace("hasn", "not");
	  line = line.replace("haven", "not");
	  line = line.replace("shouldn", "not");
	  line = line.replace("chouldn", "not");
	  line = line.replace("whouldn", "not");
	  line = line.replace("mightn", "not");
	  line = line.replace("won", "not");
	  return line;
  }
  public static ArrayList<String> getPatternsFromText(String text, String emoWord, LexicalizedParser lp, boolean usePPL, boolean gen) throws JWNLException, IOException {
	  if(emotionWords == null) {
		  buildEmotionHash("extendedList.txt");
	  }   
//	  LexicalizedParser lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
	  StringReader sr; // we need to re-read each line into its own reader because the tokenizer is over-complicated garbage
	  PTBTokenizer tkzr; // tokenizer object
	  ArrayList<String> results = new ArrayList<String>();
	  TreeMap<String, String> classTable2 = buildClassTable("emo3785to6.txt");
//	  TreeMap<String, perpNode> perpTable = buildPerpTable("classPerplexity" + File.separator);
//	  TreeMap<String, perpNode> perpTable = buildPerpTable("symPerplexity" + File.separator);
	  String sixCat = classTable2.get(emoWord);
	  Double threshold = 5.0;
	  
//	  SWN3 sentiDic = new SWN3();
	  BreakIterator bi = BreakIterator.getSentenceInstance();
	  bi.setText(text);
	  int index = 0;
	  while( bi.next() != BreakIterator.DONE)
	  {
	  	String str = text.substring(index, bi.current());
//	 	String stemmed = textStemmer(str);
	  	sr = new StringReader(str);
	  	tkzr = PTBTokenizer.newPTBTokenizer(sr);
    	List toks = tkzr.tokenize();
		Tree parse = lp.apply(toks);
		ArrayList<Tree> clauseList = getClauses(parse);

		ArrayList<String> extractedDeps = getExtractedDeps(clauseList);
		for(int i=0; i<extractedDeps.size(); i++) {
			String line = linePreprocess(extractedDeps.get(i));
//			System.out.println("line?:" + line);
			ArrayList<String> nodeList = new ArrayList<String>(Arrays.asList(line.split("\\|")));
			ArrayList<ArrayList<String>> rootRelated = findRootRelated(findTargetValue(nodeList, "ROOT-0"), nodeList);
			for(int j=0; j<rootRelated.size(); j++) {
				if( rootRelated.get(j) != null) {
					myPattern pat = new myPattern(rootRelated.get(j));
//					String patternStr = pat.getPatternString();	 //no perplexity
//					TreeMap<String, Double> perpTable = buildPerpTable("symPerplexity" + File.separator, emoWord);
					TreeMap<String, Double> perpTable = new TreeMap<String, Double>();
					// perpTable in runTime is useless, just keep it in here to simplify the program
					String patternStr = pat.getPatternString(perpTable, sixCat, threshold, true, usePPL, gen);
					if( patternStr != null){
						results.add(patternStr);
//						String
//						System.out.println("line?Pattern:" + patternStr);
					}
				}
			}
		} 
		index = bi.current();
      }
//	  System.out.println(results);
	  return results;
  }
  public static ArrayList<String> getPatternsFromText_weight(String text, String emoWord, LexicalizedParser lp, ArrayList<String> oriStr, boolean usePPL, boolean gen) throws JWNLException, IOException {
	  if(emotionWords == null) {
		  buildEmotionHash("extendedList.txt");
	  }   
//	  LexicalizedParser lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
	  StringReader sr; // we need to re-read each line into its own reader because the tokenizer is over-complicated garbage
	  PTBTokenizer tkzr; // tokenizer object
	  ArrayList<String> results = new ArrayList<String>();
	  TreeMap<String, String> classTable2 = buildClassTable("emo3785to6.txt");
//	  TreeMap<String, perpNode> perpTable = buildPerpTable("classPerplexity" + File.separator);
//	  TreeMap<String, perpNode> perpTable = buildPerpTable("symPerplexity" + File.separator);
	  String sixCat = classTable2.get(emoWord);
	  Double threshold = 5.0;
	  
//	  SWN3 sentiDic = new SWN3();
	  BreakIterator bi = BreakIterator.getSentenceInstance();
	  bi.setText(text);
	  int index = 0;
	  while( bi.next() != BreakIterator.DONE)
	  {
	  	String str = text.substring(index, bi.current());
//	 	String stemmed = textStemmer(str);
	  	sr = new StringReader(str);
	  	tkzr = PTBTokenizer.newPTBTokenizer(sr);
    	List toks = tkzr.tokenize();
		Tree parse = lp.apply(toks);
		ArrayList<Tree> clauseList = getClauses(parse);

		ArrayList<String> extractedDeps = getExtractedDeps(clauseList);
		for(int i=0; i<extractedDeps.size(); i++) {
			String line = linePreprocess(extractedDeps.get(i));
//			System.out.println("line?:" + line);
			ArrayList<String> nodeList = new ArrayList<String>(Arrays.asList(line.split("\\|")));
			ArrayList<ArrayList<String>> rootRelated = findRootRelated(findTargetValue(nodeList, "ROOT-0"), nodeList);
			for(int j=0; j<rootRelated.size(); j++) {
				if( rootRelated.get(j) != null) {
					myPattern pat = new myPattern(rootRelated.get(j));
//					String patternStr = pat.getPatternString();	 //no perplexity
//					TreeMap<String, Double> perpTable = buildPerpTable("symPerplexity" + File.separator, emoWord);
					TreeMap<String, Double> perpTable = new TreeMap<String, Double>();
					// perpTable in runTime is useless, just keep it in here to simplify the program
					String patternStr = pat.getPatternString(perpTable, sixCat, threshold, true, usePPL, gen);
					if( patternStr != null){
						results.add(patternStr);
						String oriSentence=oriString(line);
						oriStr.add(oriSentence);
					}
				}
			}
		} 
		index = bi.current();
      }
//	  System.out.println(results);
	  return results;
  }
  public static String oriString(String line) {
	  ArrayList<String> nodeList = new ArrayList<String>(Arrays.asList(line.split("\\|")));
	  
	  ArrayList<String> oriNode = new ArrayList<String>();
//	  System.out.println("Ori: " + nodeList.toString());
	  for(int i=0;i<nodeList.size();i++){
		  if(nodeList.get(i).contains("ROOT-0")) {
			  oriNode.add(PatternGenByDep.textStemmer(nodeList.get(i).split(" ")[1].split("-")[0]).toLowerCase().replace(" ", "") + "_ROOT");
		  }
		  else {
			  oriNode.add(PatternGenByDep.textStemmer(nodeList.get(i).split(" ")[1].split("-")[0].toLowerCase()));
		  }
	  }
//	  System.out.println("Ori: " + oriNode.toString());
//	  System.out.println(oriNode.toString().replace("[", "").replace("]", "").replace(",", "").replace("  ", " "));
	return oriNode.toString().replace("[", "").replace("]", "").replace(",", "").replace("  ", " ");
  }
public static ArrayList<String> getPatternsFromText_N_E(String text, String emoWord, LexicalizedParser lp, boolean usePPL,boolean useE, boolean useN) throws JWNLException, IOException {
	  if(emotionWords == null) {
		  buildEmotionHash("extendedList.txt");
	  }   
//	  LexicalizedParser lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
	  StringReader sr; // we need to re-read each line into its own reader because the tokenizer is over-complicated garbage
	  PTBTokenizer tkzr; // tokenizer object
	  ArrayList<String> results = new ArrayList<String>();
	  TreeMap<String, String> classTable2 = buildClassTable("emo3785to6.txt");
//	  TreeMap<String, perpNode> perpTable = buildPerpTable("classPerplexity" + File.separator);
//	  TreeMap<String, perpNode> perpTable = buildPerpTable("symPerplexity" + File.separator);
	  String sixCat = classTable2.get(emoWord);
	  Double threshold = 5.0;
	  
//	  SWN3 sentiDic = new SWN3();
	  BreakIterator bi = BreakIterator.getSentenceInstance();
	  bi.setText(text);
	  int index = 0;
	  while( bi.next() != BreakIterator.DONE)
	  {
	  	String str = text.substring(index, bi.current());
//	 	String stemmed = textStemmer(str);
	  	sr = new StringReader(str);
	  	tkzr = PTBTokenizer.newPTBTokenizer(sr);
    	List toks = tkzr.tokenize();
		Tree parse = lp.apply(toks);
		ArrayList<Tree> clauseList = getClauses(parse);

		ArrayList<String> extractedDeps = getExtractedDeps(clauseList);
		for(int i=0; i<extractedDeps.size(); i++) {
			String line = linePreprocess(extractedDeps.get(i));
			ArrayList<String> nodeList = new ArrayList<String>(Arrays.asList(line.split("\\|")));
			
			
			for(int j=0;j<nodeList.size();j++) {
				  try 
				  {
					  String reln=nodeList.get(j).split("\\(")[0];
//					  System.out.println(reln);
					  String nodeWord=nodeList.get(j).split(", ")[1];
					  String terms[]=nodeWord.split("-");
					  String endTerm=terms[terms.length-1];
					  nodeWord=nodeWord.replace("-" + endTerm, "");
					  if (reln.contains("obj")) {
						  results.add(nodeWord);
					  }
					  if(emotionWords.containsKey(nodeWord)) {
						  results.add(nodeWord);
					  }
				  }
				  catch (Exception e) {
					  // do nothing
				  }
			  }
			
			
			
			ArrayList<ArrayList<String>> rootRelated = findRootRelated(findTargetValue(nodeList, "ROOT-0"), nodeList);
			for(int j=0; j<rootRelated.size(); j++) {
				if( rootRelated.get(j) != null) {
					myPattern pat = new myPattern(rootRelated.get(j));
//					String patternStr = pat.getPatternString();	 //no perplexity
//					TreeMap<String, Double> perpTable = buildPerpTable("symPerplexity" + File.separator, emoWord);
					TreeMap<String, Double> perpTable = new TreeMap<String, Double>();
					// perpTable in runTime is useless, just keep it in here to simplify the program
					String patternStr = pat.getPatternString(perpTable, sixCat, threshold, true, usePPL, true);
					if( patternStr != null)
						results.add(patternStr);
				}
			}
		} 
		index = bi.current();
      }
//	  System.out.println(results);
	  return results;
  }
  public static ArrayList<String> getEmotionsFromText(String text, String emoWord, LexicalizedParser lp, boolean usePPL) throws JWNLException, IOException {
	  if(emotionWords == null) {
		  buildEmotionHash("extendedList.txt");
	  }   
//	  LexicalizedParser lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
	  StringReader sr; // we need to re-read each line into its own reader because the tokenizer is over-complicated garbage
	  PTBTokenizer tkzr; // tokenizer object
	  ArrayList<String> results = new ArrayList<String>();
	  TreeMap<String, String> classTable2 = buildClassTable("emo3785to6.txt");
//	  TreeMap<String, perpNode> perpTable = buildPerpTable("classPerplexity" + File.separator);
//	  TreeMap<String, perpNode> perpTable = buildPerpTable("symPerplexity" + File.separator);
	  String sixCat = classTable2.get(emoWord);
	  Double threshold = 5.0;
	  
//	  SWN3 sentiDic = new SWN3();
	  BreakIterator bi = BreakIterator.getSentenceInstance();
	  bi.setText(text);
	  int index = 0;
	  while( bi.next() != BreakIterator.DONE)
	  {
	  	String str = text.substring(index, bi.current());
//	 	String stemmed = textStemmer(str);
	  	sr = new StringReader(str);
	  	tkzr = PTBTokenizer.newPTBTokenizer(sr);
    	List toks = tkzr.tokenize();
		Tree parse = lp.apply(toks);
		ArrayList<Tree> clauseList = getClauses(parse);

		ArrayList<String> extractedDeps = getExtractedDeps(clauseList);
		for(int i=0; i<extractedDeps.size(); i++) {
			String line = linePreprocess(extractedDeps.get(i));
			ArrayList<String> nodeList = new ArrayList<String>(Arrays.asList(line.split("\\|")));
			
			
			for(int j=0;j<nodeList.size();j++) {
				  try 
				  {
					  String reln=nodeList.get(j).split("\\(")[0];
//					  System.out.println(reln);
					  String nodeWord=nodeList.get(j).split(", ")[1];
					  String terms[]=nodeWord.split("-");
					  String endTerm=terms[terms.length-1];
					  nodeWord=nodeWord.replace("-" + endTerm, "");
//					  if (reln.contains("obj")) {
//						  results.add(nodeWord);
//					  }
					  if(emotionWords.containsKey(nodeWord)) {
						  results.add(nodeWord);
					  }
				  }
				  catch (Exception e) {
					  // do nothing
				  }
			  }
			
			
			
//			ArrayList<ArrayList<String>> rootRelated = findRootRelated(findTargetValue(nodeList, "ROOT-0"), nodeList);
//			for(int j=0; j<rootRelated.size(); j++) {
//				if( rootRelated.get(j) != null) {
//					myPattern pat = new myPattern(rootRelated.get(j));
////					String patternStr = pat.getPatternString();	 //no perplexity
////					TreeMap<String, Double> perpTable = buildPerpTable("symPerplexity" + File.separator, emoWord);
//					TreeMap<String, Double> perpTable = new TreeMap<String, Double>();
//					// perpTable in runTime is useless, just keep it in here to simplify the program
//					String patternStr = pat.getPatternString(perpTable, sixCat, threshold, true, usePPL);
//					if( patternStr != null)
//						results.add(patternStr);
//				}
//			}
		} 
		index = bi.current();
      }
//	  System.out.println(results);
	  return results;
  }
  public static ArrayList<String> getNounsFromText(String text, String emoWord, LexicalizedParser lp, boolean usePPL) throws JWNLException, IOException {
	  if(emotionWords == null) {
		  buildEmotionHash("extendedList.txt");
	  }   
//	  LexicalizedParser lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
	  StringReader sr; // we need to re-read each line into its own reader because the tokenizer is over-complicated garbage
	  PTBTokenizer tkzr; // tokenizer object
	  ArrayList<String> results = new ArrayList<String>();
	  TreeMap<String, String> classTable2 = buildClassTable("emo3785to6.txt");
//	  TreeMap<String, perpNode> perpTable = buildPerpTable("classPerplexity" + File.separator);
//	  TreeMap<String, perpNode> perpTable = buildPerpTable("symPerplexity" + File.separator);
	  String sixCat = classTable2.get(emoWord);
	  Double threshold = 5.0;
	  
//	  SWN3 sentiDic = new SWN3();
	  BreakIterator bi = BreakIterator.getSentenceInstance();
	  bi.setText(text);
	  int index = 0;
	  while( bi.next() != BreakIterator.DONE)
	  {
	  	String str = text.substring(index, bi.current());
//	 	String stemmed = textStemmer(str);
	  	sr = new StringReader(str);
	  	tkzr = PTBTokenizer.newPTBTokenizer(sr);
    	List toks = tkzr.tokenize();
		Tree parse = lp.apply(toks);
		ArrayList<Tree> clauseList = getClauses(parse);

		ArrayList<String> extractedDeps = getExtractedDeps(clauseList);
		for(int i=0; i<extractedDeps.size(); i++) {
			String line = linePreprocess(extractedDeps.get(i));
			ArrayList<String> nodeList = new ArrayList<String>(Arrays.asList(line.split("\\|")));
			
			
			for(int j=0;j<nodeList.size();j++) {
				  try 
				  {
					  String reln=nodeList.get(j).split("\\(")[0];
//					  System.out.println(reln);
					  String nodeWord=nodeList.get(j).split(", ")[1];
					  String terms[]=nodeWord.split("-");
					  String endTerm=terms[terms.length-1];
					  nodeWord=nodeWord.replace("-" + endTerm, "");
					  if (reln.contains("obj")) {
						  results.add(nodeWord);
					  }
//					  if(emotionWords.containsKey(nodeWord)) {
//						  results.add(nodeWord);
//					  }
				  }
				  catch (Exception e) {
					  // do nothing
				  }
			  }
			
			
			
//			ArrayList<ArrayList<String>> rootRelated = findRootRelated(findTargetValue(nodeList, "ROOT-0"), nodeList);
//			for(int j=0; j<rootRelated.size(); j++) {
//				if( rootRelated.get(j) != null) {
//					myPattern pat = new myPattern(rootRelated.get(j));
////					String patternStr = pat.getPatternString();	 //no perplexity
////					TreeMap<String, Double> perpTable = buildPerpTable("symPerplexity" + File.separator, emoWord);
//					TreeMap<String, Double> perpTable = new TreeMap<String, Double>();
//					// perpTable in runTime is useless, just keep it in here to simplify the program
//					String patternStr = pat.getPatternString(perpTable, sixCat, threshold, true, usePPL);
//					if( patternStr != null)
//						results.add(patternStr);
//				}
//			}
		} 
		index = bi.current();
      }
//	  System.out.println(results);
	  return results;
  }
  /*
  public static ArrayList<String> getEmotionsFromPatternDif(ArrayList<synResult> patternResults, String emoWord, LexicalizedParser lp, boolean usePPL) throws JWNLException, IOException {
	  if(emotionWords == null) {
		  buildEmotionHash("extendedList.txt");
	  }   
//	  LexicalizedParser lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
	  StringReader sr; // we need to re-read each line into its own reader because the tokenizer is over-complicated garbage
//	  PTBTokenizer tkzr; // tokenizer object
	  ArrayList<String> results = new ArrayList<String>();
	  TreeMap<String, String> classTable2 = buildClassTable("emo3785to6.txt");
//	  TreeMap<String, perpNode> perpTable = buildPerpTable("classPerplexity" + File.separator);
//	  TreeMap<String, perpNode> perpTable = buildPerpTable("symPerplexity" + File.separator);
	  String sixCat = classTable2.get(emoWord);
	  Double threshold = 5.0;

	  
//	  SWN3 sentiDic = new SWN3();
	  BreakIterator bi = BreakIterator.getSentenceInstance();
	  bi.setText(text);
	  int index = 0;
	  while( bi.next() != BreakIterator.DONE)
	  {
	  	String str = text.substring(index, bi.current());
//	 	String stemmed = textStemmer(str);
	  	sr = new StringReader(str);
	  	tkzr = PTBTokenizer.newPTBTokenizer(sr);
    	List toks = tkzr.tokenize();
		Tree parse = lp.apply(toks);
		ArrayList<Tree> clauseList = getClauses(parse);

		ArrayList<String> extractedDeps = getExtractedDeps(clauseList);
		for(int i=0; i<extractedDeps.size(); i++) {
			String line = linePreprocess(extractedDeps.get(i));
			ArrayList<String> nodeList = new ArrayList<String>(Arrays.asList(line.split("\\|")));
			
			
			for(int j=0;j<nodeList.size();j++) {
				  try 
				  {
					  String reln=nodeList.get(j).split("\\(")[0];
//					  System.out.println(reln);
					  String nodeWord=nodeList.get(j).split(", ")[1];
					  String terms[]=nodeWord.split("-");
					  String endTerm=terms[terms.length-1];
					  nodeWord=nodeWord.replace("-" + endTerm, "");
//					  if (reln.contains("obj")) {
//						  results.add(nodeWord);
//					  }
					  if(emotionWords.containsKey(nodeWord)) {
						  results.add(nodeWord);
					  }
				  }
				  catch (Exception e) {
					  // do nothing
				  }
			  }
			
			
			
//			ArrayList<ArrayList<String>> rootRelated = findRootRelated(findTargetValue(nodeList, "ROOT-0"), nodeList);
//			for(int j=0; j<rootRelated.size(); j++) {
//				if( rootRelated.get(j) != null) {
//					myPattern pat = new myPattern(rootRelated.get(j));
////					String patternStr = pat.getPatternString();	 //no perplexity
////					TreeMap<String, Double> perpTable = buildPerpTable("symPerplexity" + File.separator, emoWord);
//					TreeMap<String, Double> perpTable = new TreeMap<String, Double>();
//					// perpTable in runTime is useless, just keep it in here to simplify the program
//					String patternStr = pat.getPatternString(perpTable, sixCat, threshold, true, usePPL);
//					if( patternStr != null)
//						results.add(patternStr);
//				}
//			}
		} 
		index = bi.current();
      }
//	  System.out.println(results);
	  return results;
  }
  public static ArrayList<String> getNounsFromPatternDif(ArrayList<synResult> results, String emoWord, LexicalizedParser lp, boolean usePPL) throws JWNLException, IOException {
	  if(emotionWords == null) {
		  buildEmotionHash("extendedList.txt");
	  }   
//	  LexicalizedParser lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
	  StringReader sr; // we need to re-read each line into its own reader because the tokenizer is over-complicated garbage
	  PTBTokenizer tkzr; // tokenizer object
	  ArrayList<String> results = new ArrayList<String>();
	  TreeMap<String, String> classTable2 = buildClassTable("emo3785to6.txt");
//	  TreeMap<String, perpNode> perpTable = buildPerpTable("classPerplexity" + File.separator);
//	  TreeMap<String, perpNode> perpTable = buildPerpTable("symPerplexity" + File.separator);
	  String sixCat = classTable2.get(emoWord);
	  Double threshold = 5.0;
	  
//	  SWN3 sentiDic = new SWN3();
	  BreakIterator bi = BreakIterator.getSentenceInstance();
	  bi.setText(text);
	  int index = 0;
	  while( bi.next() != BreakIterator.DONE)
	  {
	  	String str = text.substring(index, bi.current());
//	 	String stemmed = textStemmer(str);
	  	sr = new StringReader(str);
	  	tkzr = PTBTokenizer.newPTBTokenizer(sr);
    	List toks = tkzr.tokenize();
		Tree parse = lp.apply(toks);
		ArrayList<Tree> clauseList = getClauses(parse);

		ArrayList<String> extractedDeps = getExtractedDeps(clauseList);
		for(int i=0; i<extractedDeps.size(); i++) {
			String line = linePreprocess(extractedDeps.get(i));
			ArrayList<String> nodeList = new ArrayList<String>(Arrays.asList(line.split("\\|")));
			
			
			for(int j=0;j<nodeList.size();j++) {
				  try 
				  {
					  String reln=nodeList.get(j).split("\\(")[0];
//					  System.out.println(reln);
					  String nodeWord=nodeList.get(j).split(", ")[1];
					  String terms[]=nodeWord.split("-");
					  String endTerm=terms[terms.length-1];
					  nodeWord=nodeWord.replace("-" + endTerm, "");
					  if (reln.contains("obj")) {
						  results.add(nodeWord);
					  }
//					  if(emotionWords.containsKey(nodeWord)) {
//						  results.add(nodeWord);
//					  }
				  }
				  catch (Exception e) {
					  // do nothing
				  }
			  }
			
			
			
//			ArrayList<ArrayList<String>> rootRelated = findRootRelated(findTargetValue(nodeList, "ROOT-0"), nodeList);
//			for(int j=0; j<rootRelated.size(); j++) {
//				if( rootRelated.get(j) != null) {
//					myPattern pat = new myPattern(rootRelated.get(j));
////					String patternStr = pat.getPatternString();	 //no perplexity
////					TreeMap<String, Double> perpTable = buildPerpTable("symPerplexity" + File.separator, emoWord);
//					TreeMap<String, Double> perpTable = new TreeMap<String, Double>();
//					// perpTable in runTime is useless, just keep it in here to simplify the program
//					String patternStr = pat.getPatternString(perpTable, sixCat, threshold, true, usePPL);
//					if( patternStr != null)
//						results.add(patternStr);
//				}
//			}
		} 
		index = bi.current();
      }
//	  System.out.println(results);
	  return results;
  }
  */
  public static ArrayList<String> getWordsFromText(String text, String emoWord, LexicalizedParser lp, boolean usePPL) throws JWNLException, IOException {
	  if(emotionWords == null) {
		  buildEmotionHash("extendedList.txt");
	  }   
//	  LexicalizedParser lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
//	  StringReader sr; // we need to re-read each line into its own reader because the tokenizer is over-complicated garbage
//	  PTBTokenizer tkzr; // tokenizer object
	  ArrayList<String> results = new ArrayList<String>();
//	  TreeMap<String, String> classTable2 = buildClassTable("emo3785to6.txt");
//	  TreeMap<String, perpNode> perpTable = buildPerpTable("classPerplexity" + File.separator);
//	  TreeMap<String, perpNode> perpTable = buildPerpTable("symPerplexity" + File.separator);
//	  String sixCat = classTable2.get(emoWord);
//	  Double threshold = 5.0;
	  SynProcess.stopWordInit();
		
		// Add the stemmed bag of words of the input text (as the last vector)
	  String BOWtext = SynProcess.stopWordRemoval(text);
//		System.out.println(BOWtext);
	  String stemmedText = textStemmer(BOWtext);
	  String[] textTokens = stemmedText.split(" ");
	  for (int i=0;i<textTokens.length;i++) {
		  results.add(textTokens[i]);
	  }
	  return results;
  }
  public static boolean checkParentheses(String s) {
	    int nesting = 0;
	    for (int i = 0; i < s.length(); ++i)
	    {
	        char c = s.charAt(i);
	        switch (c) {
	            case '(':
	                nesting++;
	                break;
	            case ')':
	                nesting--;
	                if (nesting < 0) {
	                    return false;
	                }
	                break;
	        }
	    }
	    return nesting == 0;
	}
  public static boolean checkRootRelated(ArrayList<ArrayList<String>> rootRelated) {
	  boolean result = true;
	  for(int i=0; i<rootRelated.size(); i++) {
		  ArrayList<String> current = rootRelated.get(i);
		  String currentArrayString = current.toString();
		  currentArrayString = currentArrayString.replace("[", "");
		  currentArrayString = currentArrayString.replace("]", "");
		  result &= checkParentheses(currentArrayString);
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
		
  public static ArrayList<String> getExtractedDeps(ArrayList<Tree> clauseList) {
	  ArrayList<String> extractedDeps = new ArrayList<String>();
	  TreebankLanguagePack tlp = new PennTreebankLanguagePack();
      GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
	  for(int i=clauseList.size()-1; i>=0; i--) {
		  GrammaticalStructure gs = gsf.newGrammaticalStructure(clauseList.get(i));
    	  Collection tdl = gs.typedDependenciesCCprocessed();
    	  TypedDependency currentRoot = getDepRootFromList(tdl);
    	  String depStr = tdl.toString();
    	  depStr = linePreprocess(depStr);
          depStr = depStr.replace("[", "");
          depStr = depStr.replace("]", "");
    	  if( currentRoot == null || doesContain(extractedDeps, depStr)) continue;
    	  extractedDeps.add(depStr);
	  }
	  return extractedDeps;
  }
  public static boolean doesContain(ArrayList<String> extractedDeps, String depStr) {
	  String target = depStr.replaceAll("-\\d+", "");
	  for(int i=0; i<extractedDeps.size(); i++) {
		  String current = extractedDeps.get(i).replaceAll("-\\d+", "");
		  if( target.startsWith(current))
			  return true;
	  }
	  return false;
  }
  public static TypedDependency getDepRootFromList(Collection tdl) {
	  Iterator<TypedDependency> it = tdl.iterator();
	  while(it.hasNext()) {
		  TypedDependency current = it.next();
		  if( current.reln().toString().equals("root"))
			  return current;
	  }
	  return null;
  }
  public static ArrayList<Tree> getClauses(Tree parse) {
	  ArrayList<Tree> results = new ArrayList<Tree>();
	  Iterator<Tree> it = parse.iterator();
      while(it.hasNext()) {
    	  Tree current = it.next();
    	  String temp = current.label().toString();
    	  if( temp.equals("S") || temp.equals("SQ")) {
    		  if( current.isLeaf()) {
    			  continue;
    		  }
    		  results.add(current);
    	  }
      }
	  return results;
  }
  public static String textStemmer(String text) {
		String resultText = "";
		String [] tokens = text.split("\\s");
		
		for(int j=0; j < tokens.length; j++) {
			String temp;
			if( !emotionWords.containsKey(tokens[j]) && !tokens[j].contains("-")) //if this is not an emotion expression and contains no dash, do stemming
				temp = WordNetHelper.Stem(tokens[j]);
			else
				temp = tokens[j];
			if(temp != null)
				resultText += (temp + " ");
		}
		return resultText;
	}
  public static String textStemmerForced(String word) {
	  String result = "";
	  if( !word.contains("-"))
		  result = WordNetHelper.Stem(word);
	  else
		  result = word;
	  if( result == null)
		  result = "";
	  return result;
	}
  /*
  public static TreeMap<String, perpNode> buildPerpTable(String dirPath, String currentWord) throws IOException {
	  TreeMap<String, perpNode> perpTable = new TreeMap<String, perpNode>();
	  final File folder = new File(dirPath);
	  ArrayList<String> allInputFileList = PatternStats.listFilesForFolder(folder);
	  BufferedReader br;
	  for(int i=0; i<allInputFileList.size(); i++) {
		  String currentFile = allInputFileList.get(i);
		  br = new BufferedReader(new FileReader(dirPath + currentFile));
		  String line;
		  while( (line = br.readLine())!= null) {
			  if(line.length()<1) continue;
			  String[] tokens = line.split("\t");
			  if( tokens.length < 2 ) continue;
			  String word = tokens[0];
			  double perpValue = Double.parseDouble(tokens[1]);
			  int count = Integer.parseInt(tokens[2]);
			  
			  perpNode wp;
			  if( !perpTable.containsKey(word)) { //new node
				  wp = new perpNode(word);
				  if( count > 0)
					  wp.setPerp(currentFile, perpValue, currentWord);	//currentFile: current emotion word
				  else
					  wp.setPerp(currentFile, 0.0, currentWord);
			  }
			  else { //existing node
				  wp = perpTable.get(word);
				  if( count > 0)
					  wp.setPerp(currentFile, perpValue, currentWord);
				  else
					  wp.setPerp(currentFile, 0.0, currentWord);
			  }
			  perpTable.put(word, wp);
 		  }
		  br.close();
	  }
	  return perpTable;
  }
  */
  public static TreeMap<String, Double> buildPerpTable(String dirPath, String currentWord) throws IOException {
	  TreeMap<String, Double> perpTable = new TreeMap<String, Double>();
//	  final File folder = new File(dirPath);
//	  ArrayList<String> allInputFileList = PatternStats.listFilesForFolder(folder);
	  BufferedReader br;
	  br = new BufferedReader(new FileReader(dirPath + currentWord));
	  String line;
	  while( (line = br.readLine())!= null) {
		  if(line.length()<1) continue;
		  String[] tokens = line.split("\t");
		  if( tokens.length < 2 ) continue;
		  String word = tokens[0];
		  double perpValue = Double.parseDouble(tokens[1]);
//		  int count = Integer.parseInt(tokens[2]);
		  perpTable.put(word, perpValue);
	  }
	  br.close();
	  return perpTable;
  }
  private PatternGenByDep() {} // static methods only

}
/*
class perpNode {
	private String word;
//	private double[] perpArr;
	private double perpArr;
	public perpNode(String inWord) {
		word = inWord;
//		perpArr = new double[3785];
		perpArr = 0.0;
	}
	void setPerp(String category, double value, String currentWord) throws IOException {
		BufferedReader br;
		String line;
		br = new BufferedReader(new InputStreamReader(new FileInputStream("allEmo.txt"), "UTF-8"));
		TreeMap<String, Integer> wordIdxMap = new TreeMap<String, Integer>();
		int idxCount = 0;
		while( (line= br.readLine())!= null) {
			if( line.length() < 1) continue;
			wordIdxMap.put(line, idxCount);
			idxCount++;
		}
		br.close();
		
		int currentIdx = wordIdxMap.get(category);
		perpArr[currentIdx] = value;
		switch(category) {
		case "anger":
			perpArr[0] = value;
			break;
		case "disgust":
			perpArr[1] = value;
			break;
		case "fear":
			perpArr[2] = value;
			break;
		case "joy":
			perpArr[3] = value;
			break;
		case "sadness":
			perpArr[4] = value;
			break;
		case "surprise":
			perpArr[5] = value;
			break;
		default:
				break;
		}
		
	}
	double getPerp(String category) throws IOException {
		int currentIdx = -1;
		if(category == null) return 0.0;
		
		BufferedReader br;
		String line;
		br = new BufferedReader(new InputStreamReader(new FileInputStream("allEmo.txt"), "UTF-8"));
		TreeMap<String, Integer> wordIdxMap = new TreeMap<String, Integer>();
		int idxCount = 0;
		while( (line= br.readLine())!= null) {
			if( line.length() < 1) continue;
			wordIdxMap.put(line, idxCount);
			idxCount++;
		}
		br.close();
		
		currentIdx = wordIdxMap.get(category);
		if( currentIdx== -1 )
			return 0.0;
		else
			return perpArr[currentIdx];		
		switch(category) {
		case "anger":
			index = 0;
			break;
		case "disgust":
			index = 1;
			break;
		case "fear":
			index = 2;
			break;
		case "joy":
			index = 3;
			break;
		case "sadness":
			index = 4;
			break;
		case "surprise":
			index = 5;
			break;
		default:
				break;
		}
		if( index == -1 )
			return 0.0;
		else
			return perpArr[index];	
				
	}
	
}
*/
class myPattern {
	class myNode{
		private String word;
		private String relnToGov;
		private int order;
		public myNode(String inWord, String reln, int inOrder) {
			word = inWord;
			relnToGov = reln;
			order = inOrder;
		}
		public int order() { return order; }
		public String word() { return word; }
		public String reln() { return relnToGov; }
		public String toString() {
			return (relnToGov + ":" + word + ":" + order);
		}
		@Override
		public boolean equals(Object that) {
			if( that instanceof myNode) {
				myNode tmp = (myNode) that;
				return this.word.equals(tmp.word) && this.order == tmp.order;
			}
			return false;
		}
	}
	class depNode {
		private String reln;
		private String gov;
		private String dep;
		private int govOrder;
		private int depOrder;
		private myNode govNode;
		private myNode depNode;
		public depNode(String nodeStr) {
			reln = nodeStr.split("\\(")[0];
			nodeStr = nodeStr.replaceAll("\\w+\\(", "");
			nodeStr = nodeStr.replaceAll("\\)", "");			
//			nodeStr = nodeStr.replaceAll("-\\d+", "");
			String[] tokens = nodeStr.split(", ");
			for(int i=0; i<tokens.length; i++) {
				int dashBreaker = tokens[i].lastIndexOf('-');
				if( i==0 ) {
					gov = tokens[i].substring(0, dashBreaker);
					gov = gov.replaceAll("\\s+", "");
					govOrder = Integer.valueOf(tokens[i].substring(dashBreaker+1).replace("\'", ""));
				}
				else {
					dep = tokens[i].substring(0, dashBreaker);
					dep = dep.replaceAll("\\s+", "");
					depOrder = Integer.valueOf(tokens[i].substring(dashBreaker+1).replace("\'", ""));
				}
			}
		}
		public String reln() { return reln;}
		public String gov() {return gov;}
		public String dep() {return dep; }
		public int govOrder() {return govOrder;}
		public int depOrder() {return depOrder;}
		public myNode getGovNode() {
			govNode = new myNode(gov(), "gov", govOrder());
			return govNode;
		}
		public myNode getDepNode() {
			depNode = new myNode(dep(), reln(), depOrder());
			return depNode;
		}
		
	}
//	public ArrayList<ArrayList<myNode>> patterns;
	public ArrayList<myNode> pattern;
	public ArrayList<depNode> depNodeList;
	public String wildCardCostStr = null;
	private String dobjConj = null;
//	private SWN3 sentiDic = null;
	public myPattern(ArrayList<String> patternRawNodes) {
//		patterns = new ArrayList<ArrayList<myNode>>();
		pattern = new ArrayList<myNode>();
//		sentiDic =  sDic;
		genDepNodeList(patternRawNodes);
		genPattern();
		sortPattern();

	}
	public String getWildCardCost() {
		return (wildCardCostStr == null) ? "[0]" : wildCardCostStr;
	}
	public void genPattern() {
		for(int i=0; i<depNodeList.size(); i++) {
			if( !pattern.contains(depNodeList.get(i).getGovNode()) && !depNodeList.get(i).getGovNode().word().equals("ROOT"))
				pattern.add(depNodeList.get(i).getGovNode());
			if( !pattern.contains(depNodeList.get(i).getDepNode()))
				pattern.add(depNodeList.get(i).getDepNode());
//			if( pattern.contains(depNodeList.get(i).getDepNode()) && pattern.contains(depNodeList.get(i).getGovNode()) && depNodeList.get(i).reln().contains("conj"))
//				dobjConj = depNodeList.get(i).reln().split("_")[1];
		}
	}
	public void genDepNodeList(ArrayList<String> patternRawNodes) {
		depNodeList = new ArrayList<depNode>();
		for(int i=0; i<patternRawNodes.size();i++) {
			depNode tempDep = new depNode(patternRawNodes.get(i));
			if( !tempDep.reln().contains("subj") && !tempDep.reln().contains("partmod") && !tempDep.reln().contains("comp") 
					&& !tempDep.reln().contains("parataxis") && !tempDep.reln().contains("advcl") && !tempDep.reln().contains("aux")
					&& !tempDep.reln().contains("poss") && !tempDep.reln().contains("det") && !tempDep.reln().contains("cc")
					&& !tempDep.reln().contains("dep") && !tempDep.reln().contains("advmod") )
			{
				depNodeList.add(tempDep);
			}
		}
	}
	public String getPatternString(TreeMap<String, Double> perpTable, String category, double threshold, boolean runTime, boolean usePPL, boolean gen) throws JWNLException, IOException { 
	ArrayList<String> patternStrList = insertWildCard(perpTable, category, threshold, runTime, usePPL, gen);
	String result = "";
	Iterator<String> it = patternStrList.iterator();
	while(it.hasNext()) {
		String current = it.next();
		if( result.length() == 0 && current.equals("*")) continue;
		result += (current + " ");
	}
	result = result.replaceAll("\\s+", " ");
	if( result.matches("\\W+")) result = null;
//	result = result.replaceAll("\\W+", " ");
//	if( result!= null) {
//		if( wildCardCostStr != null )
//			result += ("\t$" + wildCardCostStr);
//	}
	return result;
	}
	
	ArrayList<String> generalization(ArrayList<String> patternStrList, myNode now, String stemmedObj, boolean gen) throws JWNLException {
		//Do generalization without keeping the original form
		if (!gen) {
			return patternStrList;
		}
		else {
			IndexWord objWord = Dictionary.getInstance().getIndexWord(POS.NOUN, stemmedObj);
			if( objWord != null) {
				String lexFileName = objWord.getSense(1).getLexFileName();
				lexFileName = lexFileName.replace("noun.", "");
				
				if( !lexFileName.equals("Tops"))
					patternStrList.add("<" + lexFileName + ">");
				else
					patternStrList.add("<" + now.word() + ">");
			}
			else {
				if( !now.reln().equals("root") )
					patternStrList.add(PatternGenByDep.textStemmer(now.word().toLowerCase()));
				else
					patternStrList.add(PatternGenByDep.textStemmer(now.word().toLowerCase())+"_root");
			} 
			return patternStrList;
		}
	}
	
	ArrayList<String> noGeneralization(ArrayList<String> patternStrList, myNode now, String stemmedObj, boolean gen) throws JWNLException{
		//Do generalization but keep the original form.
		if (!gen) {
			return patternStrList;
		}
		else {
			IndexWord objWord = Dictionary.getInstance().getIndexWord(POS.NOUN, stemmedObj);
			if( objWord != null) {
				String lexFileName = objWord.getSense(1).getLexFileName();
				lexFileName = lexFileName.replace("noun.", "");
				
				if( !lexFileName.equals("Tops"))
					patternStrList.add(now.word() + "=<" + lexFileName + ">");
				else
					patternStrList.add(now.word() + "=<" + now.word() + ">");
			}
			else {
				if( !now.reln().equals("root") )
					patternStrList.add(PatternGenByDep.textStemmer(now.word().toLowerCase()));
				else
					patternStrList.add(PatternGenByDep.textStemmer(now.word().toLowerCase())+"_root");
			} 
			return patternStrList;
		}
	}
	
	public ArrayList<String> insertWildCard(TreeMap<String, Double> perpTable, String category, double threshold, boolean runTime, boolean usePPL, boolean gen) throws JWNLException, IOException {
		ArrayList<Integer> wildCardCost = new ArrayList<Integer>();
		ArrayList<String> patternStrList = new ArrayList<String>();
//		System.out.println("pattern:" + pattern.toString());
		if( pattern.size() == 0);
		else {
			if( pattern.get(0).word().toLowerCase().equals("as")) {
				if( !pattern.get(0).reln().equals("root") )
					patternStrList.add(pattern.get(0).word().toLowerCase());
				else
					patternStrList.add(pattern.get(0).word().toLowerCase()+"_root");
			}
			else {
				if( !pattern.get(0).reln().equals("root") )
					patternStrList.add(PatternGenByDep.textStemmer(pattern.get(0).word().toLowerCase()));
				else
					patternStrList.add(PatternGenByDep.textStemmer(pattern.get(0).word().toLowerCase())+"_root");
	//			patternStrList.add(PatternGenByDep.textStemmer(pattern.get(0).word().toLowerCase()));
			}
			if( pattern.size() > 1) {
//				System.out.println("pattern:" + pattern.toString());
	//			int dobjCount = 0;
				for(int i=1; i<pattern.size(); i++) {
					myNode previous = pattern.get(i-1);
					myNode now = pattern.get(i);
	//				if( !(now.reln().contains("xcomp") && xcomp) ) continue;
					if( now.order() - previous.order() > 1) { // the case that requires wild card insertion.				
						if( now.reln().contains("conj_")) {
							String conj = now.reln().split("_")[1];
							patternStrList.add(conj);
							if( (now.order() - previous.order()) > 2) {
								patternStrList.add("*");
								wildCardCost.add((now.order() - previous.order()));
							}
							
						}
						else if( now.reln().contains("prep_") ) {
							String[] prep = now.reln().split("_");
							for(int j=1; j<prep.length; j++)
								patternStrList.add(prep[j]);
							if( (now.order() - previous.order()) > prep.length) {
								patternStrList.add("*");
								wildCardCost.add((now.order() - previous.order()));
							}
						}
						else if( now.reln().contains("agent")) {
							patternStrList.add("by");
							if( (now.order() - previous.order()) > 2) {
								patternStrList.add("*");
								wildCardCost.add((now.order() - previous.order()));
							}
						}
						else {
							patternStrList.add("*");
							wildCardCost.add((now.order() - previous.order()));
						}
						if( now.word().equals("as")) {
							if( !now.reln().equals("root") )
								patternStrList.add(PatternGenByDep.textStemmer(now.word().toLowerCase()));
							else
								patternStrList.add(PatternGenByDep.textStemmer(now.word().toLowerCase())+"_root");
						}
						else if( now.reln().contains("obj") || now.reln().contains("prep_") || now.reln().contains("agent")) {
							String stemmedObj = PatternGenByDep.textStemmerForced(now.word());
							if(usePPL) {
								double perpValue;
								if (perpTable.containsKey(stemmedObj))
									perpValue = perpTable.get(stemmedObj);
								else
									perpValue = 0.0;
							/*
							double perpValue;
							perpNode tmpNode = perpTable.get(stemmedObj);
							if( tmpNode == null)
								perpValue = 0;
							else {
								perpValue = tmpNode.getPerp(category);
							}
							*/
							/*
							if( runTime == true) { 
								if( perpValue > threshold || perpValue < 1)
									patternStrList = generalization(patternStrList, now, stemmedObj); //do generalization.
								else {
									patternStrList = noGeneralization(patternStrList, now, stemmedObj);
								}
							}
							else 
								patternStrList = generalization(patternStrList, now, stemmedObj);
							*/
							// penguin0423
								if( runTime == true) { 
									patternStrList = noGeneralization(patternStrList, now, stemmedObj, gen);
								}
								else {
									if( perpValue > threshold || perpValue < 1)
										patternStrList = generalization(patternStrList, now, stemmedObj, gen); //do generalization.
									else {
										patternStrList = noGeneralization(patternStrList, now, stemmedObj, gen);
									}
								}
							}//if not usePPL
							else {
								patternStrList = generalization(patternStrList, now, stemmedObj, gen);

							}
							//penguin0423
						}
						else {
							if( !now.reln().equals("root") )
								patternStrList.add(PatternGenByDep.textStemmer(now.word().toLowerCase()));
							else
								patternStrList.add(PatternGenByDep.textStemmer(now.word().toLowerCase())+"_root");
						}
					}
					else {	//the case that doesn't require wild card insertion.
						if( isObjPronoun(now) )	continue;
						if( now.word().equals("as")){
							if( !now.reln().equals("root") )
								patternStrList.add(PatternGenByDep.textStemmer(now.word().toLowerCase()));
							else
								patternStrList.add(PatternGenByDep.textStemmer(now.word().toLowerCase())+"_root");
						}
						else if( now.reln().contains("obj") || now.reln().contains("prep_") || now.reln().contains("agent")) {
							String stemmedObj = PatternGenByDep.textStemmerForced(now.word());
							double perpValue;
							if (perpTable.containsKey(stemmedObj))
								perpValue = perpTable.get(stemmedObj);
							else
								perpValue = 0.0;
							/*
							double perpValue;
							perpNode tmpNode = perpTable.get(stemmedObj);
							if( tmpNode == null)
								perpValue = 0;
							else {
								perpValue = tmpNode.getPerp(category);
							}
							*/
							/*
							if( runTime == true) { 
								if( perpValue > threshold || perpValue < 1)
									patternStrList = generalization(patternStrList, now, stemmedObj); //do generalization.
								else {
									patternStrList = noGeneralization(patternStrList, now, stemmedObj);
								}
							}
							else 
								patternStrList = generalization(patternStrList, now, stemmedObj);
							*/
							// penguin0423
							if( runTime == true) { 
								patternStrList = noGeneralization(patternStrList, now, stemmedObj, gen);
							}
							else {
								if( perpValue > threshold || perpValue < 1)
									patternStrList = generalization(patternStrList, now, stemmedObj, gen); //do generalization.
								else {
									patternStrList = noGeneralization(patternStrList, now, stemmedObj, gen);
								}
							}
							//penguin0423
						}
						else {
							if( !now.reln().equals("root") )
								patternStrList.add(PatternGenByDep.textStemmer(now.word().toLowerCase()));
							else
								patternStrList.add(PatternGenByDep.textStemmer(now.word().toLowerCase())+"_root");
	//						patternStrList.add(PatternGenByDep.textStemmer(now.word().toLowerCase()));
						}
					}
				}
			}
		}
		if(wildCardCost.size()>0) {
			for(int i=0; i<wildCardCost.size(); i++)
				wildCardCost.set(i, wildCardCost.get(i)-1);
			wildCardCostStr = wildCardCost.toString();
		}
		patternStrListPostProcess(patternStrList);
		return patternStrList;
	}
	
	
	

	/******************* Those that don't consider perplexity **************************/
//	public String getPatternString() throws JWNLException { 
//		ArrayList<String> patternStrList = insertWildCard();
//		String result = "";
//		Iterator<String> it = patternStrList.iterator();
//		while(it.hasNext()) {
//			String current = it.next();
//			if( result.length() == 0 && current.equals("*")) continue;
//			result += (current + " ");
//		}
//		result = result.replaceAll("\\s+", " ");
//		if( result.matches("\\W+")) result = null;
////		result = result.replaceAll("\\W+", " ");
////		if( result!= null) {
////			if( wildCardCostStr != null )
////				result += ("\t$" + wildCardCostStr);
////		}
//		return result;
//	}
//	public ArrayList<String> insertWildCard() throws JWNLException {
//		ArrayList<Integer> wildCardCost = new ArrayList<Integer>();
//		ArrayList<String> patternStrList = new ArrayList<String>();
//		if( pattern.size() == 0);
//		else {
//			if( pattern.get(0).word().toLowerCase().equals("as")) {
//				if( !pattern.get(0).reln().equals("root") )
//					patternStrList.add(pattern.get(0).word().toLowerCase());
//				else
//					patternStrList.add(pattern.get(0).word().toLowerCase()+"_root");
//			}
//			else {
//				if( !pattern.get(0).reln().equals("root") )
//					patternStrList.add(PatternGenByDep.textStemmer(pattern.get(0).word().toLowerCase()));
//				else
//					patternStrList.add(PatternGenByDep.textStemmer(pattern.get(0).word().toLowerCase())+"_root");
////				patternStrList.add(PatternGenByDep.textStemmer(pattern.get(0).word().toLowerCase()));
//			}
//			if( pattern.size() > 1) {
////				int dobjCount = 0;
//				for(int i=1; i<pattern.size(); i++) {
//					myNode previous = pattern.get(i-1);
//					myNode now = pattern.get(i);
////					if( !(now.reln().contains("xcomp") && xcomp) ) continue;
//					if( now.order() - previous.order() > 1) { // the case that requires wild card insertion.				
//						if( now.reln().contains("conj_")) {
//							String conj = now.reln().split("_")[1];
//							patternStrList.add(conj);
//							if( (now.order() - previous.order()) > 2) {
//								patternStrList.add("*");
//								wildCardCost.add((now.order() - previous.order()));
//							}
//							
//						}
//						else if( now.reln().contains("prep_") ) {
//							String[] prep = now.reln().split("_");
//							for(int j=1; j<prep.length; j++)
//								patternStrList.add(prep[j]);
//							if( (now.order() - previous.order()) > prep.length) {
//								patternStrList.add("*");
//								wildCardCost.add((now.order() - previous.order()));
//							}
//						}
//						else if( now.reln().contains("agent")) {
//							patternStrList.add("by");
//							if( (now.order() - previous.order()) > 2) {
//								patternStrList.add("*");
//								wildCardCost.add((now.order() - previous.order()));
//							}
//						}
//						else {
//							patternStrList.add("*");
//							wildCardCost.add((now.order() - previous.order()));
//						}
//						if( now.word().equals("as")) {
//							if( !now.reln().equals("root") )
//								patternStrList.add(now.word().toLowerCase());
//							else
//								patternStrList.add(now.word().toLowerCase()+"_root");
//						}
////							patternStrList.add(now.word());
//						else if( now.reln().contains("obj") || now.reln().contains("prep_") || now.reln().contains("agent")) {
//							String stemmedObj = PatternGenByDep.textStemmerForced(now.word());
//							
////							/*
//							if( now.reln().equals("root") )
//								patternStrList.add(stemmedObj+"_root");
//							else
//								patternStrList.add(stemmedObj);
////							*/
//							/*
//							IndexWord objWord = Dictionary.getInstance().getIndexWord(POS.NOUN, stemmedObj);
//							if( objWord != null) {
////	 							Synset[] objWordSenses = objWord.getSenses();
//								String lexFileName = objWord.getSense(1).getLexFileName();
//								lexFileName = lexFileName.replace("noun.", "");
//								
//								if( !lexFileName.equals("Tops"))
//									patternStrList.add("<" + lexFileName + ">");
//								else
//									patternStrList.add("<" + objWord + ">");
//							}
//							else {
//								if( !now.reln().equals("root") )
//									patternStrList.add(PatternGenByDep.textStemmer(now.word().toLowerCase()));
//								else
//									patternStrList.add(PatternGenByDep.textStemmer(now.word().toLowerCase())+"_root");
//							} 
//							*/
//						}
//						else {
//							if( !now.reln().equals("root") )
//								patternStrList.add(PatternGenByDep.textStemmer(now.word().toLowerCase()));
//							else
//								patternStrList.add(PatternGenByDep.textStemmer(now.word().toLowerCase())+"_root");
//						}
////							patternStrList.add(PatternGenByDep.textStemmer(now.word().toLowerCase()));
//					}
//					else {	//the case that doesn't require wild card insertion.
//						if( isObjPronoun(now) )	continue;
//						if( now.word().equals("as")){
//							if( !now.reln().equals("root") )
//								patternStrList.add(now.word().toLowerCase());
//							else
//								patternStrList.add(now.word().toLowerCase()+"_root");
//						}
//						else if( now.reln().contains("obj") || now.reln().contains("prep_") || now.reln().contains("agent")) {
//							String stemmedObj = PatternGenByDep.textStemmerForced(now.word());
//							
////							/*
//							if( now.reln().equals("root") )
//								patternStrList.add(stemmedObj+"_root");
//							else
//								patternStrList.add(stemmedObj);
////							*/
//							/*
//							IndexWord objWord = Dictionary.getInstance().getIndexWord(POS.NOUN, stemmedObj);
//							if( objWord != null) {
//								String lexFileName = objWord.getSense(1).getLexFileName();
//								lexFileName = lexFileName.replace("noun.", "");
//								
//								if( !lexFileName.equals("Tops"))
//									patternStrList.add("<" + lexFileName + ">");
//								else
//									patternStrList.add("<" + objWord + ">");
//							}
//							else {
//								patternStrList.add(now.word());
//							} 
//							*/
//						}
//						else {
//							if( !now.reln().equals("root") )
//								patternStrList.add(PatternGenByDep.textStemmer(now.word().toLowerCase()));
//							else
//								patternStrList.add(PatternGenByDep.textStemmer(now.word().toLowerCase())+"_root");
////							patternStrList.add(PatternGenByDep.textStemmer(now.word().toLowerCase()));
//						}
//					}
//				}
//			}
//		}
//		if(wildCardCost.size()>0) {
//			for(int i=0; i<wildCardCost.size(); i++)
//				wildCardCost.set(i, wildCardCost.get(i)-1);
//			wildCardCostStr = wildCardCost.toString();
//		}
//		patternStrListPostProcess(patternStrList);
//		return patternStrList;
//	}
	/******************* End of Those that don't consider perplexity **************************/
	
	
	
	public boolean isObjPronoun(myNode node) {
		if( node.reln().contains("obj")) {
			if( node.word().equals("me") || node.word().equals("you") || node.word().equals("him")|| node.word().equals("her")
					|| node.word().equals("it") || node.word().equals("us") || node.word().equals("them") || node.word().equals("myself")
					|| node.word().equals("yourself") || node.word().equals("himself") || node.word().equals("herself") || node.word().equals("itself")
					|| node.word().equals("ourselves") || node.word().equals("yourselves") || node.word().equals("themselves"))
				return true;
		}
		return false;
	}
	public void patternStrListPostProcess(ArrayList<String> patternStrList) {
		for(int i=0; i<patternStrList.size(); i++) {
			if( patternStrList.get(i).contains(" _root"))
				patternStrList.set(i, patternStrList.get(i).replace(" _root", "_ROOT"));
			else if( patternStrList.get(i).contains("_root"))
				patternStrList.set(i, patternStrList.get(i).replace("_root", "_ROOT"));
		}
	}
	public void printPattern(){
		Iterator<myNode> it = pattern.iterator();
		while(it.hasNext()) {
			myNode current = it.next();
			System.out.print(current.word() + "-" + current.order() + "/");
		}
		System.out.print("\n");
	}
	public void sortPattern() {
		Collections.sort(pattern, new myComparator());
	}
	public class myComparator implements Comparator<myNode> {
		public int compare(myNode mp1, myNode mp2) {
			if (mp1.order() >= mp2.order())
				return 1;
			else
				return -1;
		}
	}
}
