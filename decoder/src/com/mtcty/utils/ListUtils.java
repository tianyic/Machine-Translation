package com.mtcty.utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.mtcty.datastructure.Pair;

public class ListUtils {
	
	public static void sortByComparator(List<Pair> unsorted_list){
		
		// Sort list with comparator
//		Collections.sort(unsorted_list, new Comparator< Pair >(){
//			public int compare( Map.Entry<String, Pair> o1, Map.Entry<String, Pair> o2){
//				return (o1.getValue().getLogprob()).compareTo(o2.getValue().getLogprob());
//			}
//		});
		
		Collections.sort(unsorted_list, new Comparator< Pair >() {
			public int compare( Pair pair1, Pair pair2 ){
				//return pair1.getLogprob().compareTo(pair2.getLogprob());
				return pair2.getLogprob().compareTo(pair1.getLogprob());
			}
		});
	}

}
