package com.mtcty.decoder;

import java.lang.reflect.Field;
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
import java.util.TreeMap;

import com.mtcty.LM.LanguageModel;
import com.mtcty.dao.DataStream;
import com.mtcty.datastructure.Pair;
import com.mtcty.utils.ListUtils;

public class GreedyDecoder {
	
	private List<List<String>> Frenches;
	/**
	 * TM save key: french phrase, value: List (translated english phrase, logprob)
	 * sorted in descent order
	 */
	private Map<List<String>, List<Pair>> TM; 
	private LanguageModel LM;
	private double alpha;
	private int disThreshold;
	
	
	public List<List<Hypothesis>> train(){
		// Warning, no default alpha is offered in the paper.
		this.alpha = this.alpha == 0.0 ? 1.1 : this.alpha;
		
		List<List<Hypothesis>> translated_sen = new ArrayList<>();
		
		

		for( List<String> f_sen : this.Frenches ){
			
			
			// Use Greedy Search to Get the Best Hypothesis
			List<Hypothesis> hlist = this.greedySearch(f_sen);
			
			// sort by English phrase in order to output
			List<Hypothesis> sorted_h = this.sortHList(hlist, "eStartLoc");
			System.out.print("Translation    : ");
			for( Hypothesis h : sorted_h ){
				if(h.getPhrase() !=null){
					System.out.print(h.getPhrase().getWord()+" ");
				}
			}
			System.out.println();

			translated_sen.add(sorted_h);
		}
		
		
		return translated_sen;
	}
	
	public List<Hypothesis> greedySearch(List<String> f_sen){
		/**
		 *  Figure 1: Core of the greedy search algorithm
		 */
		System.out.println("----------------------------------------");
		System.out.print("French sentence: ");
		for( String fphrase : f_sen ){
			System.out.print(fphrase+" ");
		}
		System.out.println();
		List<Hypothesis> current = this.seed(f_sen);
		
		// loop
		while(true){

			double s_current = this.score(current);
			double s = s_current;			
			List<Hypothesis> best = new ArrayList<>(current);

			List<List<Hypothesis>> neighbor = this.neighborhood(current);
			
			// if there is no neighbor, continue
			if( neighbor.isEmpty() ){

				return current;
			}else{
				for( List<Hypothesis> h : neighbor ){
					double c = this.score(h);
					if( c >= s ){
						s = c;
						best = Hypothesis.cplist(h);
					}
				}
			}
			
			// Termination Criterion
			if( s == s_current ){
				return current;
			}else{
				current = Hypothesis.cplist(best);
			}	
			
		}
	}
	
	private List<Hypothesis> seed(List<String> f_sen){
		
		Map<Integer, Map<List<String>, Hypothesis >> stacks = new HashMap<>();
		Hypothesis intial_hp = new Hypothesis(0.0, LM.begin(), null, null, null, 0,0,0,0);
		Map<List<String>, Hypothesis > map = new HashMap<>(); // for save variable
		map.put( LM.begin(), intial_hp );
		for(int i = 0; i <= f_sen.size(); i++){
			if( i == 0 ){
				stacks.put(0, map);
			}else{
				stacks.put(i, new HashMap<List<String>, Hypothesis >());
			}
		}
		

		for( int i = 0; i <= f_sen.size(); i++ ){

			Map<List<String>, Hypothesis > stack = stacks.get(i);
			if( stack.isEmpty()){
				continue;
			}
			stack = sortStack(stack); 
			
			// for prune 
			int count = 0;
			
			for( Hypothesis h : stack.values() ){
				// prune
				if( count > 1){
					break;
				}
				count++;
				
				for( int j = i + 1; j <= f_sen.size(); j++ ){
					List<String> sublistF = f_sen.subList(i, j);
					if( this.TM.containsKey(sublistF) ){
						for( Pair phrase : this.TM.get(sublistF) ){
							Double logprob = h.getLogprob() + phrase.getLogprob();
							int phrase_count = h.getSrcPhrasecount() + 1;
							int eEndPos = h.geteEndLoc();
							List<String> lm_state = h.getLm_state(); // List?
							
							// Consider Language model- n-gram model
							// need to understand how to calculate
							for( String word : phrase.getWord().split(" ") ){
								eEndPos++;
								Map rtmap = this.LM.score(lm_state, word); 
								lm_state = (List<String>) rtmap.get("ngram");
								double word_logprob = (double) rtmap.get("score");
								logprob += word_logprob;
							}

							// Consider distortion logprob
							logprob -= Math.log(this.distortionProb(h.geteEndLoc(), eEndPos));

							
							// Consider the end of french sentence;
							if( j == f_sen.size() ){
								logprob += this.LM.end(lm_state);
							}
							
							// Set new hypothesis
							Hypothesis new_hypothesis = new Hypothesis(logprob, lm_state, h, phrase, sublistF, i, j
									, h.geteEndLoc(), eEndPos);

				
							// j is the length of lm_state
							// if stacks[j] doesn't contain new hypothesis, or previous logprob < new logprob
							// put new hypothesis into stacks[j]							
							if( stacks.get(j).isEmpty() ){
								stacks.get(j).put(lm_state, new_hypothesis);
							}else if( (!stacks.get(j).containsKey(lm_state)) || (stacks.get(j).get(lm_state).getLogprob() < logprob) ){
								stacks.get(j).put(lm_state, new_hypothesis);
							}
							
						}
					}
				}
			}

		}// for stack
		
		// for the last stack, select the one with largest logprob
		Map<List<String>, Hypothesis > laststack = stacks.get( stacks.size()-1 );

		double maxlogprob = -Double.MAX_VALUE;
		Hypothesis winner = new Hypothesis();
		
		for( Hypothesis h : laststack.values() ){
			if( h.getLogprob() >= maxlogprob ){

				maxlogprob = h.getLogprob();
				winner = h;
				
			}
		}

		// Use linkedlist to save 

		List<Hypothesis> translationList = new ArrayList<Hypothesis>();
		extract_english(translationList, winner);
		
		return translationList;
		
	}// function
	

	
	private List<List<Hypothesis>> neighborhood( List<Hypothesis> current){
		/*
		 * There are six operations that can transform current translation
		 * 
		 * 1. Move, 2. Swap, 3. Replace, 4. Bi-replace, 5.Split, 6. Merge
		 * 
		 */
		


		List<List<Hypothesis>> ngh_move = this.move(current);
		List<List<Hypothesis>> ngh_swap = this.swap(current);
		List<List<Hypothesis>> ngh_replace = this.replace(current);
		List<List<Hypothesis>> ngh_bireplace = this.bireplace(current);
		List<List<Hypothesis>> ngh_split = this.split(current);
		List<List<Hypothesis>> ngh_merge = this.merge(current);
		
		List<List<Hypothesis>> neighbor = new ArrayList<>();
		
		neighbor.addAll(ngh_move);
		neighbor.addAll(ngh_swap);
		neighbor.addAll(ngh_replace);
		neighbor.addAll(ngh_bireplace);
		neighbor.addAll(ngh_split);
		neighbor.addAll(ngh_merge);
		

		return neighbor;
 		
	}
	
	private List<List<Hypothesis>> move( List<Hypothesis> current ){
		/*
		 * 	whenever two adjacent source phrases are translated by phrases that are distant,
		 *  we consider moving one of the translation closer to the other.
		 *  
		 *  Need to explain it in the writeup.
		 */
		List<List<Hypothesis>> moveList = new ArrayList<>();
		
		List<Hypothesis> unsorted_current = new ArrayList<>(current);
		List<Hypothesis> sorted_current = sortHList(unsorted_current,"srcStartloc");
		
		for( int i = 1; i < sorted_current.size() - 1; i++ ){
			
			// if the distance of two adjacent source phrase is larger than a threshold = 3 as default.
			if( Math.abs(sorted_current.get(i+1).geteStartLoc() - sorted_current.get(i).geteStartLoc()) >= this.disThreshold ){
				
				// There are two modes:
				
				// Mode 1: move one to the head of the other one
				
				List<Hypothesis> move_list1 = new ArrayList<>(sorted_current);
				
				// get current hypothesis
				Hypothesis cur_h1 = new Hypothesis();
				cur_h1.copy(sorted_current.get(i));
							
				// get adjacent hypothesis
				Hypothesis adj_h1 = new Hypothesis();
				adj_h1.copy(sorted_current.get(i+1));
				
				if( cur_h1.geteStartLoc() < adj_h1.geteStartLoc() ){
					// move cur_h1 to the head of adj_h1
					// delete i-th h from move_list1
					move_list1.remove(i);
					// calculate shifting distance
					int sh_distance1 = cur_h1.geteStartLoc() - cur_h1.geteEndLoc();
					// shift hypothesises
					this.shift(move_list1, sh_distance1, cur_h1.geteStartLoc(), adj_h1.geteStartLoc());
					// set cur_h1
					cur_h1.seteEndLoc(adj_h1.geteStartLoc());
					cur_h1.seteStartLoc( cur_h1.geteEndLoc() +  sh_distance1 );
					move_list1.add(cur_h1);
					
				}else{
					// move adj_h to the head of cur_h1
					// delete i+1-th h from move_list1
					move_list1.remove(i+1);
					// calculate shifting distance
					int sh_distance1 = adj_h1.geteStartLoc() - adj_h1.geteEndLoc();
					// shift hypothesises
					this.shift(move_list1, sh_distance1, adj_h1.geteStartLoc(), cur_h1.geteStartLoc());
					// set adj_h1
					adj_h1.seteEndLoc(cur_h1.geteStartLoc());
					adj_h1.seteStartLoc( adj_h1.geteEndLoc() +  sh_distance1 );
					move_list1.add(adj_h1);

				}

				// sort move_list1 
				List<Hypothesis> sorted_move_list1 = sortHList(move_list1,"srcStartloc");
				
				moveList.add(sorted_move_list1);
				
				// Mode 2: move one to the tail of the other one
				
				List<Hypothesis> move_list2 = new ArrayList<>();
				move_list2.addAll(sorted_current);
				
				// get current hypothesis
				Hypothesis cur_h2 = new Hypothesis();
				cur_h2.copy(sorted_current.get(i));
				
				// get adjacent hypothesis
				Hypothesis adj_h2 = new Hypothesis();
				adj_h2.copy(sorted_current.get(i+1));
				
				if ( cur_h2.geteStartLoc() < adj_h2.geteStartLoc() ){
					// move adj_h2 to the tail of cur_h2
					// delete i+1-th h from move_list2
					move_list2.remove(i+1);
					// calculate shifting distance
					int sh_distance2 = adj_h2.geteEndLoc() - adj_h2.geteStartLoc();
					// shift hypothesises
					this.shift(move_list2, sh_distance2, cur_h2.geteStartLoc(), adj_h2.geteStartLoc());
					// set adj_h2
					adj_h2.seteStartLoc(cur_h2.geteEndLoc());
					adj_h2.seteEndLoc(adj_h2.geteStartLoc() + sh_distance2 );
					move_list2.add(adj_h2);
				}else{
					// move cur_h2 to the tail of adj_h2
					// delete i-th h from move_list2
					move_list2.remove(i);
					
					// calculate shifting distance
					int sh_distance2 = cur_h2.geteEndLoc() - cur_h2.geteStartLoc();
					// shift hypothesis
					this.shift(move_list2, sh_distance2, adj_h2.geteStartLoc(), cur_h2.geteStartLoc());
					// set cur_h2
					cur_h2.seteStartLoc(adj_h2.geteEndLoc());
					cur_h2.seteEndLoc(cur_h2.geteStartLoc() + sh_distance2);
					move_list2.add(cur_h2);
				}
								
				moveList.add(move_list2);

			}
		}
		
		
		return moveList;
	}
	
	private List<List<Hypothesis>> swap( List<Hypothesis> current ){
		/*
		 *  It happens rather frequently that two adjacent source segments do not
		 *  form a phrase that belongs to the transfer table.
		 *  allows to swap two adjacent source phrase
		 *  
		 */
		
		List<List<Hypothesis>> swapList = new ArrayList<>();
		
		List<Hypothesis> unsorted_current = new ArrayList<>(current);
		List<Hypothesis> sorted_current = sortHList(unsorted_current,"srcStartloc");
		
		// skip <s>
		for( int i = 1; i < sorted_current.size() - 1; i++ ){
			// get current hypothesis
			Hypothesis cur_h = new Hypothesis();
			cur_h.copy(sorted_current.get(i));
			
			// get adjacent hypothesis
			Hypothesis adj_h = new Hypothesis();
			adj_h.copy(sorted_current.get(i+1));
			
			List<Hypothesis> swp_list = new ArrayList<>(sorted_current);
			
			if( cur_h.geteStartLoc() < adj_h.geteStartLoc() ){
				
				// calculate shifting distance
				int sh_distance = adj_h.getPhrase().getWord().split(" ").length - cur_h.getPhrase().getWord().split(" ").length;
				
				this.shift(swp_list, sh_distance, cur_h.geteStartLoc(), adj_h.geteStartLoc());
				// set cur_h, adj_h
				cur_h.seteEndLoc(adj_h.geteEndLoc());
				cur_h.seteStartLoc(cur_h.geteEndLoc() - cur_h.getPhrase().getWord().split(" ").length);
				adj_h.seteStartLoc(cur_h.geteStartLoc());
				adj_h.seteEndLoc(adj_h.geteStartLoc() + adj_h.getPhrase().getWord().split(" ").length);

			}else{
				// calculate shifting distance
				int sh_distance = - adj_h.getPhrase().getWord().split(" ").length + cur_h.getPhrase().getWord().split(" ").length;
				
				this.shift(swp_list, sh_distance, adj_h.geteStartLoc(), cur_h.geteStartLoc());
				
				// set cur_h, adj_h
				adj_h.seteEndLoc(cur_h.geteEndLoc());
				adj_h.seteStartLoc(adj_h.geteEndLoc() - adj_h.getPhrase().getWord().split(" ").length);
				cur_h.seteStartLoc(adj_h.geteStartLoc());
				cur_h.seteEndLoc(cur_h.geteStartLoc() + cur_h.getPhrase().getWord().split(" ").length);	
				
			}
			
			swp_list.set(i+1, cur_h);
			swp_list.set(i, adj_h);

			swapList.add(swp_list);
			
		}
		
		return swapList;
	}
	

	private List<List<Hypothesis>> replace( List<Hypothesis> current ){
		/*
		 *  Allow to change the translation given for a specific source segment by another
		 *  one found in the translation table.
		 */
		
		List<List<Hypothesis>> replaceList = new ArrayList<>();
		List<Hypothesis> unsorted_current = new ArrayList<>(current);
		List<Hypothesis> sorted_current = sortHList(unsorted_current,"srcStartloc");

		// skip <s>
		for( int i = 1; i < sorted_current.size(); i++ ){
			
			// get current hypothesis
			Hypothesis cur_h = new Hypothesis();
			cur_h.copy(sorted_current.get(i));
			
			// Only consider the fphrase that founded in the TM
			if( this.TM.containsKey(cur_h.getFphrase()) ){
				for( Pair ephrase : this.TM.get(cur_h.getFphrase()) ){
					if( !ephrase.equal(cur_h.getPhrase()) ){
						List<Hypothesis> rp_list = new ArrayList<>();				
						rp_list = Hypothesis.cplist(sorted_current);
						
						Hypothesis rp = new Hypothesis();
						rp.copy(rp_list.get(i));
						
						// Calculate shifting distance
						int sh_distance = ephrase.getWord().split(" ").length - rp.getPhrase().getWord().split(" ").length;
						
						// Shift the rest hypothesis
						this.shift(rp_list, sh_distance, rp.geteStartLoc());
						
						// Set rp
						rp.setPhrase(ephrase);
						rp.seteEndLoc(rp.geteStartLoc() + sh_distance);
						
						rp_list.set(i, rp);
						
						replaceList.add(rp_list);
					}
				}
			}
		}
		
		return replaceList;
	}
	

	private List<List<Hypothesis>> bireplace( List<Hypothesis> current ){
		// Allow the translation of two adjacent source phrases to change simultaneously
		List<List<Hypothesis>> bireplaceList = new ArrayList<>();
		
		List<Hypothesis> unsorted_current = new ArrayList<>(current);
		List<Hypothesis> sorted_current = sortHList(unsorted_current,"srcStartloc");
		
		// skip <s>
		for( int i = 1; i < sorted_current.size() - 1; i++ ){
			// get current hypothesis
			Hypothesis cur_h = new Hypothesis();
			cur_h.copy(sorted_current.get(i));
			
			// get adjacent hypothesis
			Hypothesis adj_j = new Hypothesis();
			adj_j.copy(sorted_current.get(i+1));
			
			// Only consider two ephrases that are distinct from current ephrases
			if( this.TM.containsKey(cur_h.getFphrase()) && this.TM.containsKey(adj_j.getFphrase()) ){
				for( Pair ephrase1 : this.TM.get(cur_h.getFphrase()) ){
					for( Pair ephrase2 : this.TM.get(adj_j.getFphrase()) ){
						// change simultaneously
						if( (!ephrase1.equal(cur_h.getPhrase())) && (!ephrase2.equal(adj_j.getPhrase())) ){
							List<Hypothesis> br_list = new ArrayList<>(sorted_current);
							
							Hypothesis rp1 = new Hypothesis();
							rp1.copy(br_list.get(i));
							
							Hypothesis rp2 = new Hypothesis();
							rp2.copy(br_list.get(i+1));
							
							// calculate shifting distance1
							int sh_distance1 = ephrase1.getWord().split(" ").length - rp1.getPhrase().getWord().split(" ").length + 
									ephrase2.getWord().split(" ").length - rp2.getPhrase().getWord().split(" ").length;
							// calculate shifting distance2
							int sh_distance2 = ephrase1.getWord().split(" ").length - rp1.getPhrase().getWord().split(" ").length;
							// calculate shifting distance3
							int sh_distance3 = ephrase2.getWord().split(" ").length - rp2.getPhrase().getWord().split(" ").length;
							
							// shift the rest elements in the list after later one of rp1, and rp2 
							// shift the elements between rp1, and rp2
							if( rp1.geteStartLoc() < rp2.geteStartLoc() ){
								this.shift(br_list, sh_distance1, rp2.geteStartLoc());
								this.shift(br_list, sh_distance2, rp1.geteStartLoc(), rp2.geteStartLoc());
								// set rp1
								rp1.setPhrase(ephrase1);
								rp1.seteEndLoc( rp1.geteStartLoc()+ephrase1.getWord().split(" ").length );
								
								// set rp2
								rp2.setPhrase(ephrase2);
								rp2.seteStartLoc(rp2.geteStartLoc()+sh_distance2);
								rp2.seteEndLoc( rp2.geteStartLoc()+ephrase2.getWord().split(" ").length );

							}else{
								this.shift(br_list, sh_distance1, rp1.geteStartLoc());
								this.shift(br_list, sh_distance3, rp2.geteStartLoc(), rp1.geteStartLoc());
								// set rp2
								rp2.setPhrase(ephrase2);
								rp2.seteEndLoc( rp2.geteStartLoc()+ephrase2.getWord().split(" ").length );
								
								// set rp1
								rp1.setPhrase(ephrase1);
								rp1.seteStartLoc(rp1.geteStartLoc()+sh_distance3);
								rp1.seteEndLoc( rp1.geteStartLoc()+ephrase1.getWord().split(" ").length );

							}
							
						
							br_list.set(i, rp1);
							br_list.set(i+1, rp2);
							
							bireplaceList.add(br_list);
						}
					}
				}
				
			}
		}
		
		
		// bireplaceList can be null
		return bireplaceList;
	}
	
	private List<List<Hypothesis>> split( List<Hypothesis> current ){
		// Given a source phrase, split in two parts
		List<List<Hypothesis>> splitList = new ArrayList<>();
		
		List<Hypothesis> unsorted_current = new ArrayList<>(current);
		List<Hypothesis> sorted_current = sortHList(unsorted_current,"srcStartloc");

		// skip head <s>
		for( int i = 1; i < sorted_current.size(); i++){
			
			// get current hypothesis
			Hypothesis cur_h = new Hypothesis();
			cur_h.copy(sorted_current.get(i));
			
			
			for( int j = 1; j < cur_h.getFphrase().size(); j++ ){
				List<String> fphrase1 = cur_h.getFphrase().subList(0, j);
				List<String> fphrase2 = cur_h.getFphrase().subList(j, cur_h.getFphrase().size() );
				
				// Only consider the two new source phrases receive a translation found in the 
				// transfer table.
				// Combine all possible e phrase.
				if( this.TM.containsKey(fphrase1) && this.TM.containsKey(fphrase2) ){
					//System.out.println(fphrase1+" "+fphrase2);
					for( Pair ephrase1 : this.TM.get(fphrase1) ){
						for( Pair ephrase2 : this.TM.get(fphrase2) ){

							List<Hypothesis> sp_list = new ArrayList<>();
							sp_list.addAll(sorted_current);
							
							Hypothesis sp_h = new Hypothesis();
							sp_h.copy(sp_list.get(i));
							
							
							// Calculating shifting distance
							int ephrase1_size = ephrase1.getWord().split(" ").length;
							int ephrase2_size = ephrase2.getWord().split(" ").length;
							
							// shifting distance = new length - old length
							int sh_distance = ( ephrase1_size + ephrase2_size ) - cur_h.getPhrase().getWord().split(" ").length;
							
							this.shift( sp_list, sh_distance, sp_h.geteStartLoc() );
							
							Hypothesis sp_h1 = new Hypothesis();
							sp_h1.copy(sp_h);						
							sp_h1.setFphrase(fphrase1);
							sp_h1.setPhrase(ephrase1);
							sp_h1.seteEndLoc( sp_h1.geteEndLoc() + ephrase1_size );
							sp_h1.setSrcEndloc(sp_h1.getSrcStartloc() + fphrase1.size() );
							
							Hypothesis sp_h2 = new Hypothesis();
							sp_h2.setFphrase(fphrase2);
							sp_h2.setPhrase(ephrase2);
							sp_h2.seteStartLoc( sp_h1.geteEndLoc() );
							sp_h2.seteEndLoc( sp_h2.geteStartLoc() + ephrase2_size );
							sp_h2.setSrcStartloc(sp_h1.getSrcEndloc());
							sp_h2.setSrcEndloc(sp_h2.geteStartLoc() + fphrase2.size());
							
							sp_list.set(i, sp_h1);
							sp_list.add(i+1, sp_h2);
							
							splitList.add(sp_list);
							
						}
					}
				}
			}
			
		}

		
		
		// splitlist can be null
		return splitList;
	}
	
	
	private List<List<Hypothesis>> merge( List<Hypothesis> current ) {
		/*
		 *  Opposed to the split operation
		 *  
		 *  Two adjacent source phrases are merged.
		 *  
		 *  Only consider the case that a new translation can be picked up from translation table.
		 *  
		 */
		
		
		List<List<Hypothesis>> mergeList = new ArrayList<>();
		
		List<Hypothesis> unsorted_current = new ArrayList<>();
		
		unsorted_current = Hypothesis.cplist(current);
		
		List<Hypothesis> sorted_current = sortHList(unsorted_current,"srcStartloc");
		
		
		// start index == 1 to skip <s> 
		for( int i = 1 ; i < sorted_current.size() - 1 ; i++){
			
			// get current hypothesis
			Hypothesis cur_h = new Hypothesis();
			cur_h.copy(sorted_current.get(i));;
			//cur_h = (Hypothesis) sorted_current.get(i).clone();
			
			// get adjacent hypothesis
			Hypothesis adj_h = new Hypothesis();
			adj_h.copy(sorted_current.get(i+1));
			//adj_h = (Hypothesis) sorted_current.get(i+1).clone();
			
			// merge French phrases together
			List<String> merged_fPhrase = new ArrayList<>();
			merged_fPhrase.addAll(cur_h.getFphrase());
			merged_fPhrase.addAll(adj_h.getFphrase());

			// length of English phrase 1
			int e1_size = cur_h.geteEndLoc() - cur_h.geteStartLoc();
			int e2_size = adj_h.geteEndLoc() - adj_h.geteStartLoc();
			
			// Only consider the case that a new translation can be picked up from translation table.
			if( this.TM.containsKey(merged_fPhrase) ){
				
				// if a new translation can be founded in TM
				// for each possible phrase, construct a new Hypothesis list
				for( Pair phrase : this.TM.get(merged_fPhrase) ){
					
					List<Hypothesis> merged_list = new ArrayList<>();
					merged_list = Hypothesis.cplist(sorted_current);
					
					Hypothesis merged_h = merged_list.get(i);
					merged_h.setPhrase(phrase);
					merged_h.setFphrase(merged_fPhrase);
					merged_h.setSrcEndloc(adj_h.getSrcEndloc());
					
					merged_list.remove(i+1);
					
					// shifting distance
					int sh_distance = phrase.getWord().split(" ").length - e1_size - e2_size;
					
					// if shifting distance ~= 0, then the rest following hypothesis should be changed
					this.shift( merged_list, sh_distance, merged_h.geteStartLoc() );
					
					merged_h.seteEndLoc( adj_h.geteEndLoc() + sh_distance );
					merged_list.set(i, merged_h);
					
					mergeList.add(merged_list);
					
				}
				
			}
			
			
		}

		
		// mergeList can be null
		return mergeList;
	}
	
	// shift  rest of list from point given shifting distance
	// For split, merge operations
	private void shift(List<Hypothesis> hlist, int shift_distance, int startLoc, int... endLoc){
		if( shift_distance == 0 ){
			return;
		}else if( endLoc.length == 0 ){
			for( Hypothesis h : hlist ){
				if( h.geteStartLoc() > startLoc ){
					h.seteStartLoc( h.geteStartLoc() + shift_distance );
					h.seteEndLoc( h.geteEndLoc() + shift_distance );
				}
			}
		}else{
			for( Hypothesis h : hlist ){
				if( h.geteStartLoc() > startLoc && h.geteStartLoc() < endLoc[0] ){
					h.seteStartLoc( h.geteStartLoc() + shift_distance );
					h.seteEndLoc( h.geteEndLoc() + shift_distance );
				}
			}
		}

	}
	
 	public void loadData(String tmfile, String lmfile, String frfile, int k, int disThreshold){
		
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
		this.disThreshold = disThreshold;
		
	}

 	private double score(List<Hypothesis> current){
 		
 		// Assume lambda is equivalent for equation(1)
 		double logprob = 0.0;
 		
 		List<String> lm_state = this.LM.begin();
 		
 		for( Hypothesis h : current ){
 			
 			if( h.getPhrase() != null ){

 				// p(f|e)
 				logprob += h.getPhrase().getLogprob();

 				// LM prob
 				for( String word : h.getPhrase().getWord().split(" ")){
 					Map map = this.LM.score(lm_state, word);
 					logprob += (double) map.get("score");
 					lm_state = (List<String>) map.get("ngram");
 				}

 				logprob -= Math.log(this.distortionProb(h.geteStartLoc(), h.geteEndLoc()));

 			}

 		}

 		logprob += this.LM.end(lm_state);
 		
 		return logprob;
 		
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
	
	
	// sort hypothesis list by some attribute
	private static List<Hypothesis> sortHList(List<Hypothesis> hplist, final String attribute){
		
		if( hplist == null ){
			return hplist;
		}
		
		List<Hypothesis> sortedlist = new ArrayList<>();
		sortedlist.addAll(hplist);
		
		Collections.sort(sortedlist, new Comparator< Hypothesis >(){
			public int compare( Hypothesis o1, Hypothesis o2){
				Object oj1 = o1;
				Object oj2 = o2;
				Class<?> c = oj1.getClass();
				Field f;
				try {
					f = c.getDeclaredField(attribute);
					f.setAccessible(true);
					Integer valo1 = f.getInt(o1);
					Integer valo2 = f.getInt(o2);
					return (valo1).compareTo(valo2);
				} catch (NoSuchFieldException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return 0;

			}
		});
		
		return sortedlist;
	}  
	
	// the first element of list is not <s>

	private static void extract_english(List<Hypothesis> list, Hypothesis h){
		if( h.getPredecessor() != null ){
			extract_english( list, h.getPredecessor() );
		}
		list.add(h);
		return;
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
