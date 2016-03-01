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
import com.mtcty.utils.MapUtils;

public class UI {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String tmfile = "tm";
		String lmfile = "lm";
		String frfile = "input";
		int k = 1;
		
		GreedyDecoder gd = new GreedyDecoder();
		
		// input data
		gd.loadData(tmfile, lmfile, frfile, k);
		
		MapUtils.printTM2(gd.getTM());
		
		gd.train();
		

		int sum = 2;
		plus(1,sum);
		System.out.println(sum);
	}
	
	public static void plus(int i , int sum){
		sum = sum + i;
	}

}
