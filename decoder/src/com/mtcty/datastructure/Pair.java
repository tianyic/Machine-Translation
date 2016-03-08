package com.mtcty.datastructure;

public class Pair {

	private String word;
	private Double logprob;
	
	public Pair(){};
	
	public Pair(String word, Double logprob){
		this.word = word;
		this.logprob = logprob;
	};
	
	public boolean equal(Pair cmppair){
		if( this.word.equals(cmppair.getWord()) && this.logprob.equals(cmppair.getLogprob()) ){
			return true;
		}
		return false;
	}
	
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
	
    public Object clone() {
        try {
            return super.clone();
        }
        catch (CloneNotSupportedException e) {
            // This should never happen
            throw new InternalError(e.toString());
        }
    }
}
