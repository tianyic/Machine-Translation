package com.mtcty.decoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mtcty.LM.LanguageModel;
import com.mtcty.dao.DataStream;
import com.mtcty.datastructure.Pair;

public class GreedyDecoder {
	
	private List<List<String>> Frenches;
	private Map<List<String>, List<Pair>> TM;
	private LanguageModel LM;
	private double alpha;
	
	public void train(){
		for( List<String> f_sen : this.Frenches ){
			// Use Greedy Search to Get the Best Hypothesis
			Hypothesis h = this.greedySearch(f_sen);

			
		}
	}
	
	public Hypothesis greedySearch(List<String> f_sen){
		/**
		 *  
		 */
		this.seed(f_sen);
		return null;
	}
	
	public void seed(List<String> f_sen){
		
		Map<Integer, Map<List<String>, Hypothesis >> stacks = new HashMap<>();
		Hypothesis intial_hp = new Hypothesis(0.0, LM.begin(), null, null);
		Map<List<String>, Hypothesis > map = new HashMap<>();
		map.put(Arrays.asList(LM.begin()), intial_hp );
		for(int i = 0; i < f_sen.size(); i++){
			if( i == 0 ){
				stacks.put(0, map);
			}else{
				stacks.put(i, null);
			}
		}
		for( Map.Entry<Integer, Map<List<String>, Hypothesis >> entry : stacks.entrySet() ){
			Integer i = entry.getKey();
			Map<List<String>, Hypothesis > stack = entry.getValue();
			stack = sortStack(stack);
			
			for( Hypothesis h : stack.values() ){
				for( int j = i + 1; j < f_sen.size() + 1; j++ ){
					List<String> sublistF = f_sen.subList(i, j);
					if( this.TM.containsKey(sublistF) ){
						for( Pair phrase : this.TM.get(sublistF) ){
							Double logprob = h.getLogprob() + phrase.getLogprob();
							String lm_state = h.getLm_state();
							for( String word : phrase.getWord().split(" ") ){
								
							}
						}
					}
				}
			}
			

		}
		//stacks.put(0, map);
		
		
		
		
	}
	

 	public void loadData(String tmfile, String lmfile, String frfile, int k ){
		
		Map<List<String>, List<Pair>> TM = DataStream.readTM(tmfile, k);
		LanguageModel LM = new LanguageModel(lmfile);
		List<List<String>> Frenches = DataStream.readFreach(frfile);
		
		// input French word list:
		List<String> mergedlist = new ArrayList<>();
		for( List<String> list : Frenches ){
			mergedlist.addAll(list);
		}
		Set<String> fwordset = new HashSet(mergedlist);
		
		for( String f_word : fwordset ){
			List<String> key = Arrays.asList(f_word);
			if( ! TM.containsKey(key) ){
				Pair pair = new Pair( f_word, 0.0 );
				TM.put(key, Arrays.asList(pair));
			}
		}
		
		// set properties
		this.TM = TM;
		this.LM = LM;
		this.Frenches = Frenches;
		
	}

	private double distortionProb(int i, int j){
		/**
		 * BY Statistical Phrase-Based Translation
		 * Or Lecture Note
		 * Philipp Koehn
		 */
		return Math.pow(this.alpha, (double) Math.abs( i - j - 1) );
	}
	
	// Sort Map by the logprob in pair List
	public static Map< List<String>, Hypothesis > sortStack( Map< List<String>, Hypothesis > unsortStack){
		
		// Convert Map to LinkedList
		if ( unsortStack == null){
			return unsortStack;
		}
		List< Map.Entry< List<String>, Hypothesis > > list = new LinkedList<>(unsortStack.entrySet());
		
		// Sort list with comparator
		Collections.sort(list, new Comparator< Map.Entry< List<String>, Hypothesis > >(){
			public int compare( Map.Entry< List<String>, Hypothesis > o1, Map.Entry< List<String>, Hypothesis > o2){
				return (o2.getValue().getLogprob()).compareTo(o1.getValue().getLogprob());
			}
		});
		
		// Convert sorted map back to a Map
		Map< List<String>, Hypothesis > sortedStack = new LinkedHashMap<>();
		Iterator<Map.Entry< List<String>, Hypothesis > > it = list.iterator();
		while( it.hasNext() ){
			Map.Entry< List<String>, Hypothesis >  entry = it.next();
			sortedStack.put(entry.getKey(), entry.getValue());
		}
		
		return sortedStack;
		
	}
	
	public List<List<String>> getFrenches() {
		return Frenches;
	}

	public void setFrenches(List<List<String>> frenches) {
		Frenches = frenches;
	}

	public Map<List<String>, List<Pair>> getTM() {
		return TM;
	}

	public void setTM(Map<List<String>, List<Pair>> tM) {
		TM = tM;
	}

	public LanguageModel getLM() {
		return LM;
	}

	public void setLM(LanguageModel lM) {
		LM = lM;
	}
	


	
}
