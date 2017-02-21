package wordnet;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;

import wordnet.SynProcess.ValueComparator;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;

class StParser {
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
//	public static void redoCorpus(String path) {
//		HashMap<String, String> sentences = new HashMap<String, String>();
//		try {
//			BufferedReader br = new BufferedReader(new FileReader(path));
//			String line;
//			while( ( line = br.readLine()) != null) {
//				if( !sentences.containsKey(line) ) {
//					sentences.put(line, line);
//				}
//			}
//			br.close();
//			BufferedWriter bw = new BufferedWriter(new FileWriter("corpus2.txt")); 
//			ArrayList<String> all = new ArrayList<String>(sentences.keySet());
//			sentences = null;
//			System.gc();
//			Iterator<String> it = all.iterator();
//			while(it.hasNext()) {
//				bw.write(it.next() + "\n");
//			}
//			bw.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
  public static void main(String[] args) throws IOException {
//	  redoCorpus("corpus.txt");
    LexicalizedParser lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
    WordNetHelper.initialize("file_properties.xml");
    buildEmotionHash("emoWords.txt");
    demoDP(lp, "testOriginal.txt");
  }

  /**
   * demoDP demonstrates turning a file into tokens and then parse
   * trees.  Note that the trees are printed by calling pennPrint on
   * the Tree object.  It is also possible to pass a PrintWriter to
   * pennPrint if you want to capture the output.
 * @throws IOException 
   */
  public static void sentencePreprocess(List<HasWord> sentence) {
	  for(int i=0; i<sentence.size(); i++) {
		  if( sentence.get(i).word().equals("'s") || sentence.get(i).word().equals("'re") )
			  sentence.get(i).setWord("be");
		  if( sentence.get(i).word().equals("'d") )
			  sentence.get(i).setWord("would");
	  }
  }
  public static void demoDP(LexicalizedParser lp, String filename) throws IOException {
    // This option shows loading and sentence-segmenting and tokenizing
    // a file using DocumentPreprocessor.
	  Writer wt = new FileWriter("test.txt", true);
		BufferedWriter bw = new BufferedWriter(wt); 
    // You could also create a tokenizer here (as below) and pass it
    // to DocumentPreprocessor
    int count = 0, thousandCount = 0;
//    boolean skipNext = false;
    for (List<HasWord> sentence : new DocumentPreprocessor(filename)) {
    	ArrayList<String> events = new ArrayList<String>();
    	ArrayList<String> emoWord = getEmotionWord(sentence);
//    	String strSentence = getSentenceString(sentence);
 //   	System.out.println("\nSentence = " + strSentence);
 //   	System.out.println("Emotion Words = " + emoWord.toString());
//    	bw.write(strSentence + "\n");
      Tree parse = lp.apply(sentence);
      String parseStr = parse.pennString();
//      wt.write(parseStr);
//      parse.pennPrint();
//      parse.percolateHeads(new SemanticHeadFinder());
//      System.out.println(parseStr);

//      extractVP(parse, events, false);   
//      System.out.println(events.toString());
//      parseStr = parseStr.replace("\n", "");
//   
      /*
      for(int i=0; i<emoWord.size(); i++) {
    	  for(int j=0; j<events.size(); j++) {
    		  if( !emoWord.get(i).equals(events.get(j)) ) {
//    			  System.out.println(emoWord.get(i)+","+events.get(j));
    			  bw.write( emoWord.get(i)+","+events.get(j) + "\n");
    		  }
    	  }
      }
//      parse.indentedListPrint();
//      parse.pennPrint();
//      System.out.println();
*/
      TreebankLanguagePack tlp = new PennTreebankLanguagePack();
      GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
      GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
//      Collection tdl = gs.typedDependenciesCCprocessed();
      Collection tdl = gs.typedDependencies();
//      System.out.println(tdl);
      String depStr = tdl.toString();
      depStr = depStr.replace("[", "");
      depStr = depStr.replace("]", "");
      bw.write(depStr + "\n");
      /*
      ArrayList<TypedDependency> rootRelated = getRootChildren(getRootNode(tdl), tdl);
      if( !rootRelated.isEmpty()) {
    	  String pattern = getPatternString(rootRelated);
      	for(int i=0; i<emoWord.size(); i++) {
    	  bw.write(emoWord.get(i) + "," + pattern + "\n");
      	}
      }*/
//      System.out.println(getPatternString(rootRelated));
//      System.out.println(rootRelated);
//      System.out.println();
      count++;
    	if( count % 1000 == 0) {
    		System.gc();
    		thousandCount++;
    		System.out.println("Done " + thousandCount*1000 + " sentences...");
    	}
    	bw.flush();
    }
    bw.close();
  }
  public static TypedDependency getRootNode(Collection tdl) {
	  TypedDependency result = null;
      Iterator<TypedDependency> it = tdl.iterator();
      while( it.hasNext()) {
    	  TypedDependency currentNode = it.next();
    	  if( currentNode.reln().toString().equals("root")) {
    		  result = currentNode;
    		  break;
    	  }
      }
      return result;
  }
  public static ArrayList<TypedDependency> getRootChildren(TypedDependency root, Collection tdl) {
	  ArrayList<TypedDependency> resultList = new ArrayList<TypedDependency>();
	  resultList.add(root);
	  Iterator<TypedDependency> it = tdl.iterator();
	  String rootValue = root.dep().toString();
      while( it.hasNext()) {
    	  TypedDependency currentNode = it.next();
    	  if( currentNode.gov().toString().equals(rootValue)) {
    		  resultList.add(currentNode);
    		  if( currentNode.reln().toString().contains("conj") ) {
    			  ArrayList<TypedDependency> subList = getRootChildren(currentNode, tdl);
    			  resultList.addAll(subList);
    		  }
    	  }
      }
      return resultList;
  }
  public static String getPatternString(ArrayList<TypedDependency> list) {
//	  System.out.println(list);
	  HashMap<String, Integer> pairs = new HashMap<String, Integer>();
	  Iterator<TypedDependency> it = list.iterator();
	  String result = "";
	  while(it.hasNext()) {
		  TypedDependency currentNode = it.next();
		  if( currentNode.reln().toString().equals("nsubj") || currentNode.reln().toString().equals("advcl")) continue;
//		  else {
			  String contentStr = currentNode.toString().replaceAll(".+\\(", "");
			  contentStr = contentStr.replaceAll("\\)", "");
			  String[] tokens = contentStr.split(", ");
			  for(int j=0; j<tokens.length; j++) {
//				  System.out.println(tokens[j]);
				  if(!tokens[j].equals("ROOT-0")) {
					  //System.out.println(tokens[j].charAt(tokens[j].length()-1) + " " + tokens[j].charAt(tokens[j].length()-2) + " " + tokens[j].charAt(tokens[j].length()-3));
					  if( tokens[j].charAt(tokens[j].length()-2) == '-') {
						  String key = tokens[j].substring(0, tokens[j].length()-2);
						  String valueStr = tokens[j].substring(tokens[j].length()-1);
						  key = key.toLowerCase();
						  pairs.put(textStemmer(key), Integer.valueOf(valueStr));
					  }
					  else if( tokens[j].charAt(tokens[j].length()-3) == '-') {
						  String key = tokens[j].substring(0, tokens[j].length()-3);
						  String valueStr = tokens[j].substring(tokens[j].length()-2);
						  key = key.toLowerCase();
						  pairs.put(textStemmer(key), Integer.valueOf(valueStr));
					  }
				  }
			  }
//			  pairs.put(textStemmer(contentStr.split(", ")[0].split("-")[0]), Integer.valueOf(contentStr.split(", ")[0].split("-")[1]));
	//		  pairs.put(textStemmer(contentStr.split(", ")[1].split("-")[0]), Integer.valueOf(contentStr.split(", ")[1].split("-")[1]));
//		  }
	  }
	  ArrayList<Map.Entry<String, Integer>> valueList = new ArrayList<Map.Entry<String, Integer>>(pairs.size());
	  valueList.addAll(pairs.entrySet());
	  ValueComparator vc = new ValueComparator();
	  Collections.sort(valueList, vc);
	  System.out.println(valueList);
	  Object[] valueListArr = valueList.toArray();
	  result += valueList.get(0).getKey();
	  if( valueList.size()>1) {
		  for(int i=1; i<valueListArr.length; i++) {
			  Map.Entry<String, Integer> previous = (Map.Entry<String, Integer>)valueListArr[i-1];
			  Map.Entry<String, Integer> current = (Map.Entry<String, Integer>)valueListArr[i];
			  if( (current.getValue() - previous.getValue()) > 1 ) {
				  result += "* ";
				  result += (current.getKey() );
			  }
			  else
				  result += (" " + current.getKey() );
		  }
		  result = result.replaceAll("\\s+", " ");
	  }
//	  System.out.println(result);
	  return result;
  }
  private static class ValueComparator implements Comparator<Map.Entry<String, Integer>> {
		public int compare(Map.Entry<String, Integer> mp1, Map.Entry<String, Integer> mp2) {
			if (mp1.getValue() >= mp2.getValue())
				return 1;
			else
				return -1;
		}
	}
  public static void extractVP(Tree parse, ArrayList<String> events, boolean attachVerb) {
	  if(emotionWords == null) {
		  buildEmotionHash("emoWords.txt");
	  }      
	  Iterator<Tree> it = parse.iterator();
      while(it.hasNext()) {
    	  Tree current = it.next().flatten();
    	  String temp = current.label().toString();
//    	  System.out.println("Value = " + current.value());
    	  if( temp.equals("VP")) {
    		  String fcLabel = current.firstChild().label().toString();
    		  if( fcLabel.equals("VB") || fcLabel.equals("VBD") || fcLabel.equals("VBG") || fcLabel.equals("VBN") || fcLabel.equals("VBP") || fcLabel.equals("VBZ")) {
    			  if( fcLabel.equals("VBD") && current.getChild(1).label().toString().equals("VBN") ) continue;
    			  //    		  System.out.println(current.dependencies().toString());
	    		  current.pennPrint();
//	    		  int subAt = getTargetNodeNum(current, "S");
//	    		  if( subAt == -1) subAt = getTargetNodeNum(current, "SBAR");
//	    		  if( subAt != -1){
//	    			  Tree nextChild = current.getChild(subAt);
//	    			  current.removeChild(subAt);
//	    			  System.out.println(current.yieldHasWord().toString());
//	//    			  bw.write(getSentenceString(tempCurrent.yieldHasWord()) + "\n" );
//	    		  }
	    		  List<TaggedWord> twl = current.taggedYield();
	    		  if( twl.size() >= 2 && !isBePlusParticiple(twl)) {
//		    		  System.out.println(twl.toString());
		    		  String VP = textStemmer( getVerbPhrase(twl) );
		    		  String verb = getVerb(twl);
		    		  if( !attachVerb) {
			    		  if( events.size() >=1 && events.get((events.size() - 1)).equals("not "+VP));
			    		  else {
			    			  if( events.size() >=1 && events.get(events.size()-1).equals("to "+VP)) {
			    				  events.remove(events.size()-1);
			    			  }
			    			  if( events.size() >= 2 && (events.get(events.size()-2).contains(events.get(events.size()-1)) && events.get(events.size()-2).contains(VP) ))
			    				  events.remove(events.size()-2);
			    			  if( !events.contains(VP)) {
			    					  events.add(VP);
			    			  }
			    			  System.out.println(VP);
			    		  }
		    		  }
		    		  else {
		    			  VP = VP+","+verb;
			    		  if( events.size() >=1 && events.get((events.size() - 1)).split(",")[0].equals("not "+VP.split(",")[0]));
			    		  else {
			    			  if( events.size() >=1 && events.get(events.size()-1).split(",")[0].equals("to "+VP.split(",")[0])) {
			    				  events.remove(events.size()-1);
			    			  }
			    			  if( events.size() >= 2 && (events.get(events.size()-2).contains(events.get(events.size()-1).split(",")[0]) && events.get(events.size()-2).contains(VP.split(",")[0]) ))
			    				  events.remove(events.size()-2);
			    			  if( !events.contains(VP)) {
			    					  events.add(VP);
			    			  }
		//	    			  System.out.println(VP);
			    		  }
		    		  }
	    		  }
	    	  }
    	  }
//    	  System.out.println(temp + "\n");
      }
  }
  public static ArrayList<String> getEmotionWord(List<HasWord> sentence) {
	  ArrayList<String> results = new ArrayList<String>();
	  Iterator<HasWord> it = sentence.iterator();
	  while(it.hasNext()) {
		  HasWord current = it.next();
		  if( isEmotionWord(current) && !results.contains(current.word()))
			  results.add(current.word());
	  }
	  return results;
  }
  public static boolean isEmotionWord(HasWord word) {
	  if( emotionWords.containsKey(word.word()))
		  return true;
	  else
		  return false;
  }
  public static boolean isBePlusParticiple(List<TaggedWord> tagged) {
	  if( isBeVerb(tagged.get(0)) && isParticiple(tagged.get(1)))
		  return true;
	  else
		  return false;
  }
  public static boolean isParticiple(TaggedWord word) {
	  if(word.tag().equals("VBG") || word.tag().equals("VBN"))
		  return true;
	  else
		  return false;
  }
  public static boolean isBeVerb(TaggedWord word) {
	  if( word.word().equals("be") || word.word().equals("am") || word.word().equals("is") || word.word().equals("are")
			  || word.word().equals("was") || word.word().equals("were") || word.word().equals("been"))
		  return true;
	  else
		  return false;
		  
  }
  public static boolean isNoun(TaggedWord word) {
	  if( word.tag().equals("NN") || word.tag().equals("NNS"))
		  return true;
	  else
		  return false;
  }
  public static String getVerb(List<TaggedWord> tagged) {
	  String result = null;
	  Iterator<TaggedWord> it = tagged.iterator();
	  while(it.hasNext()) {
		  TaggedWord current = it.next();
		  if( current.tag().equals("VB") || current.tag().equals("VBD") || current.tag().equals("VBG") || current.tag().equals("VBN")
				  || current.tag().equals("VBP") || current.tag().equals("VBZ")) {
			  if( result == null)
				  result = current.word();
			  else {
				  if( result.equals("be") || result.equals("have") || result.equals("need") ) {
					  result = current.word();
					  break;
				  }
			  }
		  }
	  }
	  return result;
  }
  public static String getVerbPhrase(List<TaggedWord> tagged) {
	  String result = "";
	  boolean keepGoing = true;
	  Iterator<TaggedWord> it = tagged.iterator();
	  while(it.hasNext()) {
		  TaggedWord current = it.next();
		  if( current.word().equals("n't"))
			  current.setWord("not");
		  if( current.word().equals("'s") || current.word().equals("'re"))
			  current.setWord("be");
		  if( keepGoing == true) {
			  if( !current.tag().equals("MD") && !current.tag().matches("\\W+")) {
				  result += (current.word() + " ");
			  }		  	
			  if( isNoun(current) ) {
				  keepGoing = false;
			  }
		  }
		  else{
			  if( current.word().equals("as")) {
				  result += (current.word() + " ");
				  keepGoing = true;
			  }
			  else
				  break;
		  }
//			  break;
	  }
	  return result;
  }
  public static String getSentenceString(List<HasWord> sentence) {
	  String result = "";
	  Iterator<HasWord> it = sentence.iterator();
	  while(it.hasNext()) {
		  HasWord current = it.next();
		  	result += ( current.word() + " ");
	  }
	  return result;
  }
  public static int getTargetNodeNum(Tree childTree, String targetLabel) {
	  int result = -1;
	  List<Tree> childList = childTree.getChildrenAsList();
	  for(int i=0; i<childList.size(); i++) {
		  if( childList.get(i).label().toString().equals(targetLabel)) {
			  result = i;
			  break;
		  }
	  }
	  return result;
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
  /**
   * demoAPI demonstrates other ways of calling the parser with
   * already tokenized text, or in some cases, raw text that needs to
   * be tokenized as a single sentence.  Output is handled with a
   * TreePrint object.  Note that the options used when creating the
   * TreePrint can determine what results to print out.  Once again,
   * one can capture the output by passing a PrintWriter to
   * TreePrint.printTree.
   */
  public static void demoAPI(LexicalizedParser lp) {
    // This option shows parsing a list of correctly tokenized words
    String[] sent = { "This", "is", "an", "easy", "sentence", "." };
    List<CoreLabel> rawWords = Sentence.toCoreLabelList(sent);
    Tree parse = lp.apply(rawWords);
    parse.pennPrint();
    System.out.println();

    // This option shows loading and using an explicit tokenizer
    String sent2 = "This is another sentence.";
    TokenizerFactory<CoreLabel> tokenizerFactory =
      PTBTokenizer.factory(new CoreLabelTokenFactory(), "");
    List<CoreLabel> rawWords2 =
      tokenizerFactory.getTokenizer(new StringReader(sent2)).tokenize();
    parse = lp.apply(rawWords2);

    TreebankLanguagePack tlp = new PennTreebankLanguagePack();
    GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
    GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
    List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
    System.out.println(tdl);
    System.out.println();

    TreePrint tp = new TreePrint("penn,typedDependenciesCollapsed");
    tp.printTree(parse);
  }

  private StParser() {} // static methods only

}
