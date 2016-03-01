package com.mtcty.dao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.mtcty.datastructure.Pair;
import com.mtcty.utils.ListUtils;
import com.mtcty.utils.MapUtils;

public class DataStream {

	//  Read Translation Model Data
	public static Map readTM(String fileName, int k){
		
		File file_TM = new File(fileName);
		
		BufferedReader reader_TM = null;
		
		System.out.println("Reading translation model from "+fileName+"...");
		
		// Create Maps to save Transition Model
		Map<List<String>, List<Pair>> unsorted_TM = new HashMap<>();
		Map<List<String>, List<Pair>> sorted_TM  = new HashMap<>();
		
		try {
			
			reader_TM = new BufferedReader(new FileReader(file_TM));
			
			String line = null;
			
			// Warning: Java can not split string directly by pipe '|', need to Escape characters
			// metacharacters '\\|'
			while( ( line = reader_TM.readLine() ) != null ){
				
				String[] linesplits = line.replaceAll("\n", "").split(" \\|\\|\\| ");

				List<String> f_words = Arrays.asList(linesplits[0].split(" "));
				String e_translation = linesplits[1];
				Double logprob = Double.parseDouble(linesplits[2]);
				Pair pair = new Pair( e_translation, logprob);
				if( unsorted_TM.containsKey(f_words) ){
					unsorted_TM.get(f_words).add(pair);
				}else{
					List<Pair> pairs = new LinkedList<>();
					pairs.add(pair);
					unsorted_TM.put(f_words, pairs);
				}
				
			}
			
			// sort unsorted_TM
			for( Map.Entry<List<String>, List<Pair>> entry : unsorted_TM.entrySet() ){
				ListUtils.sortByComparator( entry.getValue() );
				// Get Largest K Translations
				if( entry.getValue().size() < k){
					sorted_TM.put( entry.getKey(), entry.getValue());
				}else{
					List<Pair> sublist = entry.getValue().subList(0, k);
					sorted_TM.put( entry.getKey(), sublist );
				}
				
			}			
			// free memory of unsorted_TM
			unsorted_TM.clear();
			
			return sorted_TM;
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return null;
	}
	
	public static List<List<String>> readFreach(String filename){
		File file_French = new File(filename);
		
		BufferedReader reader_French = null;
		
		System.out.println("Reading French from "+filename+"...");
		
		List<List<String>> Frenches = new ArrayList<>(); 
		
		try {
			reader_French = new BufferedReader(new FileReader(file_French));
			
			String line = null;
			
			while( (line = reader_French.readLine()) != null ){
				Frenches.add(Arrays.asList(line.split(" ")));
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			return Frenches;
		}
		
	}
}
