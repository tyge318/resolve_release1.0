package wordnet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader; 
import java.io.OutputStreamWriter; 
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

	public class SWN3 {
		private String pathToSWN = "SWN3"+File.separator+"SentiWordNet_3.0.0.txt";
		private HashMap<String, Double> _dict;
		
		public static void main(String[] args) throws IOException {
//			mergeData("emoPatternlst_L1_ICF_0219best_ispNE.txt", "emoPatternlst_L1_ICF_0219best_espNE.txt", "emoPatternlst_L1_ICF_0219best_mergedNE.txt");
//			mergeData("training_NE0619_isp.txt", "training_NE0619_esp.txt", "training_NE0619_merged.txt");
			mergeDataandPE("emoPatternlst_L1_CTPonly_0219best_isp.txt", "emoPatternlst_L1_CTPonly_0219best_esp.txt", "emoPatternlst_L1_CTPonly_0219best_merged.txt", "emoPatternlst_L1_CTPonly_0219best_merged_pe.txt");
			// isp, esp, merged
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
			br.close();
			return classTable;
		}
		public static HashMap<String, Double> buildPETable(String filePath) throws IOException {
			HashMap<String, Double> PETable = new HashMap<String, Double>();
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"));
			String line;
			while( (line = br.readLine())!= null) {
				if( line.length() < 1) continue;
				PETable.put(line, 0.0);
			}
			br.close();
			return PETable;
		}
		public static void mergeData(String file1, String file2, String output) throws IOException {
			TreeMap<String, String> classTable = buildClassTable("emo3785to2_0506.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file1), "UTF-8"));
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output), "UTF-8"));
			String line, buffer = "", currentEmoWord;
			boolean disAndSad = true, donePrint = false;
			while( (line = br.readLine())!= null) {
				if( line.length() < 1) continue;

				if( line.matches("\\@[\\w-]+:")) {
					currentEmoWord = line.replace("@", "");
					currentEmoWord = currentEmoWord.replace(":", "");
					String category = classTable.get(currentEmoWord);
//					if( category.equals("disgust") || category.equals("sad")) {
					if( category.equals("isp")) {
						disAndSad = true;
						donePrint = false;
						bw.write(line + "\n");
					}
					else {
						disAndSad = false;
					}
				}
				else if( !line.contains("===================") ){
					if( disAndSad  == true) {
						bw.write(line+"\n");
					}
				}
				else {
					if( donePrint == false) {
						bw.write(line+"\n");
						bw.flush();
						donePrint = true;
					}
				}
			}
			br.close();
			
			disAndSad = false;
			br = new BufferedReader(new InputStreamReader(new FileInputStream(file2), "UTF-8"));
			while( (line = br.readLine())!= null) {
				if( line.length() < 1) continue;
				if( line.matches("\\@[\\w-]+:")) {
					currentEmoWord = line.replace("@", "");
					currentEmoWord = currentEmoWord.replace(":", "");
					String category = classTable.get(currentEmoWord);
//					if( category.equals("disgust") || category.equals("sad")) {
					if( category.equals("isp")) {
						disAndSad = true;
					}
					else {
						disAndSad = false;
						donePrint = false;
						bw.write(line + "\n");
					}
				}
				else if(!line.contains("===================") ){
					if( disAndSad  == false ) {
						bw.write(line + "\n");
					}
				}
				else {
					if( donePrint == false) {
						bw.write(line + "\n");
						bw.flush();
						donePrint = true;
					}
				}
			}
			br.close();
			bw.close();
		}
		public static void mergeDataandPE(String file1, String file2, String output, String outputPE) throws IOException {
			TreeMap<String, String> classTable = buildClassTable("emo3785to2_0506.txt");
			HashMap<String, Double> peTable = buildPETable("allEmo.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file1), "UTF-8"));
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output), "UTF-8"));
			BufferedWriter PEbw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPE), "UTF-8"));
			String line, currentEmoWord = "";
			double localsum=0.0;
			boolean disAndSad = true, donePrint = false;
			while( (line = br.readLine())!= null) {
				if( line.length() < 1) continue;

				if( line.matches("\\@[\\w-]+:")) {
					currentEmoWord = line.replace("@", "");
					currentEmoWord = currentEmoWord.replace(":", "");
					String category = classTable.get(currentEmoWord);
//					if( category.equals("disgust") || category.equals("sad")) {
					if( category.equals("isp")) {
						disAndSad = true;
						donePrint = false;
						bw.write(line + "\n");
					}
					else {
						disAndSad = false;
					}
				}
				else if( !line.contains("===================") ){
					if( disAndSad  == true) {
						bw.write(line+"\n");
						double num = Double.parseDouble(line.split("\t")[1]) + peTable.get(currentEmoWord);
						peTable.put(currentEmoWord, num);
						localsum += Double.parseDouble(line.split("\t")[1]);
					}
				}
				else {
					if( donePrint == false) {
						bw.write(line+"\n");
						bw.flush();
						donePrint = true;
					}
				}
			}
			br.close();
			
			disAndSad = false;
			br = new BufferedReader(new InputStreamReader(new FileInputStream(file2), "UTF-8"));
			while( (line = br.readLine())!= null) {
				if( line.length() < 1) continue;
				if( line.matches("\\@[\\w-]+:")) {
					currentEmoWord = line.replace("@", "");
					currentEmoWord = currentEmoWord.replace(":", "");
					String category = classTable.get(currentEmoWord);
//					if( category.equals("disgust") || category.equals("sad")) {
					if( category.equals("isp")) {
						disAndSad = true;
					}
					else {
						disAndSad = false;
						donePrint = false;
						bw.write(line + "\n");
					}
				}
				else if(!line.contains("===================") ){
					if( disAndSad  == false ) {
						bw.write(line + "\n");
						double num = Double.parseDouble(line.split("\t")[1]) + peTable.get(currentEmoWord);
						peTable.put(currentEmoWord, num);
						localsum += Double.parseDouble(line.split("\t")[1]);
					}
				}
				else {
					if( donePrint == false) {
						bw.write(line + "\n");
						bw.flush();
						donePrint = true;
					}
				}
			}
			br.close();
			bw.close();
			
			
			BufferedReader bremo = new BufferedReader(new InputStreamReader(new FileInputStream("allEmo.txt"), "UTF-8"));
			while( (line = bremo.readLine())!= null) {
				double pe = peTable.get(line) / localsum;
				PEbw.write("@" + line + ":\n");
				PEbw.write(String.format("%10f", pe) + "\n");
				PEbw.write("===================\n");
			}
			PEbw.close();
			bremo.close();
		}
		public SWN3(){

			_dict = new HashMap<String, Double>();
			HashMap<String, Vector<Double>> _temp = new HashMap<String, Vector<Double>>();
			try{
				BufferedReader csv =  new BufferedReader(new FileReader(pathToSWN));
				String line = "";			
				while((line = csv.readLine()) != null)
				{
//					System.out.println("test:" + line);
					String[] data = line.split("\t");
//					System.out.println("test:" + data[0] + "_" + data[1] + "_" + data[2] + "_" + data[3]);
					Double score = Double.parseDouble(data[2])-Double.parseDouble(data[3]);
					String[] words = data[4].split(" ");
					for(String w:words)
					{
						String[] w_n = w.split("#");
						w_n[0] += "#"+data[0];
						int index = Integer.parseInt(w_n[1])-1;
						if(_temp.containsKey(w_n[0]))
						{
							Vector<Double> v = _temp.get(w_n[0]);
							if(index>v.size())
								for(int i = v.size();i<index; i++)
									v.add(0.0);
							v.add(index, score);
							_temp.put(w_n[0], v);
						}
						else
						{
							Vector<Double> v = new Vector<Double>();
							for(int i = 0;i<index; i++)
								v.add(0.0);
							v.add(index, score);
							_temp.put(w_n[0], v);
						}
					}
				}
				csv.close();
				Set<String> temp = _temp.keySet();
				for (Iterator<String> iterator = temp.iterator(); iterator.hasNext();) {
					String word = (String) iterator.next();
					Vector<Double> v = _temp.get(word);
					double score = 0.0;
					double sum = 0.0;
					for(int i = 0; i < v.size(); i++)
						score += ((double)1/(double)(i+1))*v.get(i);
					for(int i = 1; i<=v.size(); i++)
						sum += (double)1/(double)i;
					score /= sum;
					/*
					String sent = "";				
					if(score>=0.75)
						sent = "strong_positive";
					else
					if(score > 0.25 && score<=0.5)
						sent = "positive";
					else
					if(score > 0 && score>=0.25)
						sent = "weak_positive";
					else
					if(score < 0 && score>=-0.25)
						sent = "weak_negative";
					else
					if(score < -0.25 && score>=-0.5)
						sent = "negative";
					else
					if(score<=-0.75)
						sent = "strong_negative";
						*/
					_dict.put(word, score);	
				}
			}
			catch(Exception e){e.printStackTrace();}		
		}

		public Double extract(String word)
		{
		    Double total = new Double(0);
		    if(_dict.get(word+"#n") != null)
		         total = _dict.get(word+"#n") + total;
		    if(_dict.get(word+"#a") != null)
		        total = _dict.get(word+"#a") + total;
		    if(_dict.get(word+"#r") != null)
		        total = _dict.get(word+"#r") + total;
		    if(_dict.get(word+"#v") != null)
		        total = _dict.get(word+"#v") + total;
		    return total;
		}
	}
