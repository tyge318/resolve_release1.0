package wordnet;

import java.util.*;

import org.htmlparser.*;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

public class VocabularyDotCom {
	public static String url = "http://www.vocabulary.com/dictionary/";
	public static void main(String[] args) {
		System.out.println(getSynonyms("delighted"));
/*		String myURL = "http://www.merriam-webster.com/dictionary/sad";
		String synonymsString, relatedWordString;
		try {
			Parser parser = new Parser(myURL);
			NodeFilter filter = new HasAttributeFilter("class", "synonyms-reference");
			NodeList nodes = parser.extractAllNodesThatMatch(filter);
			if( nodes.size() > 0) {
//				System.out.println("Got something!");
				NodeList synonymList = nodes.elementAt(0).getChildren().elementAt(1).getChildren().elementAt(0).getChildren();
				synonymList.remove(0);
				synonymsString = synonymList.asString();
				System.out.println("Synonyms: " + synonymsString);
			}else {
				System.out.println("Null");
			}
			parser = new Parser(myURL);
			filter = new HasAttributeFilter("class","accordion-body collapse");
			nodes = parser.extractAllNodesThatMatch(filter);
			if( nodes.size() > 0) {
//				System.out.println("Got something!");
				NodeList relatedWordList = nodes.elementAt(0).getChildren().elementAt(0).getChildren();
				relatedWordList.remove(0);
				relatedWordString = relatedWordList.asString();
				relatedWordString = relatedWordString.replaceAll("\\s\\([a-z\\s]+\\)", "");
				System.out.println("Related Words: " + relatedWordString);
//				System.out.println(nodes.elementAt(0).getChildren().elementAt(0).getChildren().toString());
			}else {
				System.out.println("Null");
			}
		}catch (ParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} */
	}
	public static String getGloss(String word) {
		String gloss = "";
		word = word.replace("_", "%20");
//		System.out.println(word);
		try {
			Parser parser = new Parser(url+word);
			parser.setEncoding("utf-8");
			NodeFilter filter = new HasAttributeFilter("class", "section blurb"); //find the gloss part
			NodeList nodes = parser.extractAllNodesThatMatch(filter);
			if( nodes.size() > 0) {
				//extract both short and long gloss
				gloss += nodes.elementAt(0).getChildren().elementAt(1).toPlainTextString();
				gloss += "\n";
				gloss += nodes.elementAt(0).getChildren().elementAt(3).toPlainTextString();
			}
			else {
//				System.out.println("No gloss found on this word!");
				gloss = null;
			}
		} catch (ParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return gloss;
	}
	public static String getSynonyms(String word) {
		String results = "";
		String myURL = "http://www.merriam-webster.com/dictionary/";
		String synonymsString = "";
		try {
			Parser parser = new Parser(myURL + word);
			NodeFilter filter = new HasAttributeFilter("class", "synonyms-reference");
			NodeList nodes = parser.extractAllNodesThatMatch(filter);
			if( nodes.size() > 0) {
//				System.out.println("Got something!");
				NodeList synonymList = nodes.elementAt(0).getChildren().elementAt(1).getChildren().elementAt(0).getChildren();
				synonymList.remove(0);
				synonymsString = synonymList.asString();
				String[] synonymsTokens = synonymsString.split(", ");
				for(int i=0; i<synonymsTokens.length; i++) {
					if( synonymsTokens[i].matches("\\w+") && !synonymsTokens[i].contains("_"))
						results += (synonymsTokens[i] + " ");
				}
//				synonymsString = synonymsString.replaceAll("\\s\\([a-z\\s]+\\)", "");
//				synonymsString = synonymsString.replace(" [British]", "");
//				synonymsString = synonymsString.replace(", ",  ",");
//				synonymsString = synonymsString.replace(" ", "_");
//				synonymsString = synonymsString.replace(",", " ");
//				System.out.println("Synonyms: " + results);
			}else {
//				System.out.println("Null");
			}
		} catch (ParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return results;
	}

}