package com.mtcty.LM;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mtcty.datastructure.Ngrams;

public class LanguageModel {
	
	public Map<List<String>, Ngrams> table;
	
	public LanguageModel(){}
	
	public LanguageModel(String fileName){
		System.out.println("Reading language model from "+fileName+"...");
		this.table = new HashMap();
		
		File file_LM = new File(fileName);
		BufferedReader reader_LM = null;
		
		try {
			reader_LM = new BufferedReader(new FileReader(file_LM) );
			String line = null;
			//int count = 0;
			while( (line = reader_LM.readLine()) != null ){
				String[] linesplits = line.split("\t");
				if ( linesplits.length > 1 && linesplits[0] != "ngram"){
					Ngrams ngram;
					if( linesplits.length == 3){
						ngram = new Ngrams(Double.parseDouble(linesplits[0]), linesplits[1], Double.parseDouble(linesplits[2]));
					}else{
						ngram = new Ngrams(Double.parseDouble(linesplits[0]), linesplits[1], 0.0);
					}
					List<String> key = ngram.getWords();
					this.table.put(key, ngram);
				}

			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	public List<String> begin(){
		// initial state is always <s>
		return Arrays.asList("<s>");
	}
	
	public Map score(List<String> state, String word){
		

		double scoreval = 0.0;
		List<String> ngram = new ArrayList<>();
		ngram.addAll(state);
		ngram.add(word);

		Map returnmap = new HashMap();
		
		while( ngram.size() > 0){
			if( this.table.containsKey(ngram) ){
				
				scoreval += this.table.get(ngram).getLogProb();
				ngram = ngram.subList(ngram.size()-2>=0 ? ngram.size()-2:0, ngram.size());
				returnmap.put("ngram", ngram);
				returnmap.put("score", scoreval);
				return returnmap;
			}else{ // back off
				if( ngram.size() >1 ){
					//System.out.println("back off:"+this.table.get(ngram.subList(0, ngram.size()-1 )).getBackOff() );
					scoreval += this.table.get(ngram.subList(0, ngram.size()-1 )).getBackOff();
				}
				ngram = ngram.subList(1, ngram.size());
			}
	
		}
		returnmap.put("ngram", new ArrayList<String>());
		returnmap.put("score", this.table.get(Arrays.asList("<unk>")).getLogProb());
		return returnmap;
		

	}
	
	public double end(List<String> state){
		return (double) this.score(state, "</s>").get("score");
	}
	
	
	
}
