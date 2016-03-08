package com.mtcty.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mtcty.LM.LanguageModel;
import com.mtcty.dao.DataStream;
import com.mtcty.datastructure.Pair;
import com.mtcty.decoder.GreedyDecoder;
import com.mtcty.decoder.Hypothesis;
import com.mtcty.utils.MapUtils;

public class UI {

	public static void main(String[] args) {
		
		String tmfile = "data/tm";
		String lmfile = "data/lm";
		String frfile = "data/input";
		
		int k = 1;
		if( args.length == 1){
			k = Integer.parseInt(args[0]);
		}

		int disThreshold = 1;
		String outputfile = "output/output_k_"+k+".txt";
		GreedyDecoder gd = new GreedyDecoder();
		
		// input data
		gd.loadData(tmfile, lmfile, frfile, k, disThreshold);
		
		
		List<List<Hypothesis>> translated_eng = gd.train();
		
		DataStream.output(translated_eng, outputfile);

	}
	
}
