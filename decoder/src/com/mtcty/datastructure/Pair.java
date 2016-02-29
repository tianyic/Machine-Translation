package com.mtcty.datastructure;

public class Pair {

	private String word;
	private Double logprob;
	
	public Pair(){};
	
	public Pair(String word, Double logprob){
		this.word = word;
		this.logprob = logprob;
	};
	
	public String getWord() {
		return word;
	}
	public void setWord(String word) {
		this.word = word;
	}
	public Double getLogprob() {
		return logprob;
	}
	public void setLogprob(Double logprob) {
		this.logprob = logprob;
	}
	
	
}
