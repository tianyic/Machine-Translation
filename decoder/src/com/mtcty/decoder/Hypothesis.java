package com.mtcty.decoder;

public class Hypothesis {
	
	private Double logprob;
	private String lm_state;
	private String predecessor;
	private String phrase;
	private int startloc;
	private int endloc;
	private int wordcount; 
	
	public Hypothesis(double logprob, String lm_state, String predecessor, String phrase){
		this.logprob = logprob;
		this.lm_state = lm_state;
		this.predecessor = predecessor;
		this.phrase = phrase;
	}

	public Double getLogprob() {
		return logprob;
	}

	public void setLogprob(Double logprob) {
		this.logprob = logprob;
	}

	public String getLm_state() {
		return lm_state;
	}

	public void setLm_state(String lm_state) {
		this.lm_state = lm_state;
	}

	public String getPredecessor() {
		return predecessor;
	}

	public void setPredecessor(String predecessor) {
		this.predecessor = predecessor;
	}

	public String getPhrase() {
		return phrase;
	}

	public void setPhrase(String phrase) {
		this.phrase = phrase;
	}

	public int getStartloc() {
		return startloc;
	}

	public void setStartloc(int startloc) {
		this.startloc = startloc;
	}

	public int getEndloc() {
		return endloc;
	}

	public void setEndloc(int endloc) {
		this.endloc = endloc;
	}

	public int getWordcount() {
		return wordcount;
	}

	public void setWordcount(int wordcount) {
		this.wordcount = wordcount;
	}

	
	
}
