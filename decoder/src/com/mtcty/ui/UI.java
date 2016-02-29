package com.mtcty.ui;

import java.util.List;
import java.util.Map;

import com.mtcty.dao.DataStream;
import com.mtcty.datastructure.Pair;

public class UI {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String tmfile = "tm";
		String lmfile = "lm";
		int k = 1;
		Map<List<String>, List<Pair>> TM = DataStream.readTM(tmfile, k);
		
		DataStream.readLM(lmfile, k);
	}

}
