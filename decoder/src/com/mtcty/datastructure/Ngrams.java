package com.mtcty.datastructure;

import java.util.Arrays;
import java.util.List;

public class Ngrams {
	
	private double logProb;
	private double backOff;
	private List<String> words;
	
	public Ngrams(){}
	
	public Ngrams(double logProb, String string, double backOff){
		
		this.logProb = logProb;
		this.backOff = backOff;
		this.words   = Arrays.asList(string.split(" "));
	}
	
	public double getLogProb() {
		return logProb;
	}
	public void setLogProb(double logProb) {
		this.logProb = logProb;
	}

	public List<String> getWords() {
		return words;
	}

	public void setWords(List<String> words) {
		this.words = words;
	}

	public double getBackOff() {
		return backOff;
	}

	public void setBackOff(double backOff) {
		this.backOff = backOff;
	}


	
	
	
}
