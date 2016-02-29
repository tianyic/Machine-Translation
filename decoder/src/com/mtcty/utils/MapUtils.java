package com.mtcty.utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.mtcty.datastructure.Ngrams;
import com.mtcty.datastructure.Pair;

public class MapUtils {
	
	// Sort Map by the logprob in pair
	public static Map<String, Pair> sortByComparator1( Map<String, Pair> unsort_Map){
		
		// Convert Map to LinkedList
		List< Map.Entry<String, Pair> > list = new LinkedList<>(unsort_Map.entrySet());
		
		// Sort list with comparator
		Collections.sort(list, new Comparator< Map.Entry< String, Pair > >(){
			public int compare( Map.Entry<String, Pair> o1, Map.Entry<String, Pair> o2){
				return (o1.getValue().getLogprob()).compareTo(o2.getValue().getLogprob());
			}
		});
		
		// Convert sorted map back to a Map
		Map<String, Pair> sortedMap = new LinkedHashMap<>();
		Iterator<Map.Entry<String, Pair>> it = list.iterator();
		while( it.hasNext() ){
			Map.Entry<String, Pair> entry = it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		
		return sortedMap;
		
	}
	
	// Sort Map by the logprob in pair List
	public static Map<String, Pair> sortByComparator2( Map<String, Pair> unsort_Map){
		
		// Convert Map to LinkedList
		List< Map.Entry<String, Pair> > list = new LinkedList<>(unsort_Map.entrySet());
		
		// Sort list with comparator
		Collections.sort(list, new Comparator< Map.Entry< String, Pair > >(){
			public int compare( Map.Entry<String, Pair> o1, Map.Entry<String, Pair> o2){
				return (o1.getValue().getLogprob()).compareTo(o2.getValue().getLogprob());
			}
		});
		
		// Convert sorted map back to a Map
		Map<String, Pair> sortedMap = new LinkedHashMap<>();
		Iterator<Map.Entry<String, Pair>> it = list.iterator();
		while( it.hasNext() ){
			Map.Entry<String, Pair> entry = it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		
		return sortedMap;
		
	}
	
	public static void printTM1( Map<String, Pair> map){
		for( Map.Entry<String, Pair> entry: map.entrySet() ){
			System.out.println("French Phrase: "+entry.getKey()+" English Phrase: "+entry.getValue().getWord()+" LogProb: "+entry.getValue().getLogprob());
		}
	}
	
	public static void printTM2( Map<List<String>, List<Pair>> map){
		for( Map.Entry<List<String>, List<Pair>> entry: map.entrySet() ){
			System.out.print("French Phrase: ");
			for( String f_word : entry.getKey() ){
				System.out.print("\""+f_word+"\" ");
			}
			System.out.print(", English Phrases: ");
			for( Pair pair : entry.getValue() ){
				System.out.print(" ("+pair.getWord()+", "+pair.getLogprob()+") ");
			}
			System.out.println();
		}
	}
	
	public static void printLM( Map<List<String>, Ngrams> map){
		for( Map.Entry<List<String>, Ngrams> entry : map.entrySet() ){
			System.out.print("Ngrams: (");
			for( String e_word : entry.getKey() ){
				System.out.print("\""+e_word+"\" ");
			}
			System.out.print(" ) ");
			System.out.print(" ngrams_stats: ");
			System.out.print(" Logprob: "+ entry.getValue().getLogProb());
			System.out.print(" BackOff: "+ entry.getValue().getBackOff());
			System.out.println();
		}
	}
}
