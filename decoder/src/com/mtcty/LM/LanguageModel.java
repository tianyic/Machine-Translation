package com.mtcty.LM;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
				//System.out.println(line);
				//count++;
			}
			//System.out.println(count);

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	public String begin(){
		// initial state is always <s>
		return "<s>";
	}
	
	
	
	
}
