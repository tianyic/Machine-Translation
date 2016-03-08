package com.mtcty.decoder;

import java.util.ArrayList;
import java.util.List;

import com.mtcty.datastructure.Pair;

public class Hypothesis {
	
	private Double logprob;
	private List<String> lm_state;
	private Hypothesis predecessor;
	private Pair phrase;
	
	private List<String> fphrase;
	
	private int srcStartloc;
	private int srcEndloc;
	private int srcWordcount;
	private int srcPhrasecount;
	private int eStartLoc;
	private int eEndLoc;
	
	public Hypothesis(){}
	
	public Hypothesis(double logprob, List<String> lm_state, Hypothesis predecessor, Pair phrase, 
			List<String> fphrase, int srcStartloc, int srcEndloc,int eStartLoc, int eEndLoc){
		
		this.logprob = logprob;
		this.lm_state = lm_state;
		this.predecessor = predecessor;
		this.phrase = phrase;
		this.fphrase = fphrase;
		this.srcStartloc = srcStartloc;
		this.srcEndloc = srcEndloc;
		this.eStartLoc = eStartLoc;
		this.eEndLoc = eEndLoc;
	
	}
	
	public void show(){
		System.out.println("Hypothesis:");
		System.out.println("  logprob:"+this.logprob);
		System.out.println("  lm_state:"+this.lm_state);
		System.out.println("  predecessor:"+this.predecessor);
		if( this.phrase == null ){
			System.out.println("  phrase:"+this.phrase);
		}else{
			System.out.println("  phrase:"+this.phrase.getWord());
		}

		System.out.println("  fphrase:"+this.fphrase);
		System.out.println("  srcStartloc:"+this.srcStartloc);
		System.out.println("  srcEndloc:"+this.srcEndloc);
		System.out.println("  eStartLoc:"+this.eStartLoc);
		System.out.println("  eEndLoc:"+this.eEndLoc);
	}

	public void copy(Hypothesis h){
		

		this.logprob = ( h.logprob == null ? null : h.logprob );
		this.lm_state = ( h.lm_state == null ? null : h.lm_state );
		this.predecessor = ( h.predecessor == null ? null : h.predecessor );
		this.phrase = ( h.phrase == null ? null : h.phrase );
		this.fphrase = ( h.fphrase == null ? null : h.fphrase );
		this.srcStartloc = h.srcStartloc;
		this.srcEndloc = h.srcEndloc;
		this.eStartLoc = h.eStartLoc;
		this.eEndLoc = h.eEndLoc;
	}
	
	public static List<Hypothesis> cplist( List<Hypothesis> list){
		List<Hypothesis> copy_list = new ArrayList<>();
		for( Hypothesis h : list ){
			Hypothesis h_tmp = new Hypothesis();
			h_tmp.copy(h);
			copy_list.add(h_tmp);
		}
		return copy_list;
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
	public Double getLogprob() {
		return logprob;
	}

	public void setLogprob(Double logprob) {
		this.logprob = logprob;
	}



	public List<String> getLm_state() {
		return lm_state;
	}

	public void setLm_state(List<String> lm_state) {
		this.lm_state = lm_state;
	}

	public Hypothesis getPredecessor() {
		return predecessor;
	}

	public void setPredecessor(Hypothesis predecessor) {
		this.predecessor = predecessor;
	}

	public Pair getPhrase() {
		return phrase;
	}

	public void setPhrase(Pair phrase) {
		this.phrase = phrase;
	}

	public List<String> getFphrase() {
		return fphrase;
	}

	public void setFphrase(List<String> fphrase) {
		this.fphrase = fphrase;
	}



	public int getSrcPhrasecount() {
		return srcPhrasecount;
	}

	public void setSrcPhrasecount(int srcPhrasecount) {
		this.srcPhrasecount = srcPhrasecount;
	}

	public int getSrcStartloc() {
		return srcStartloc;
	}

	public void setSrcStartloc(int srcStartloc) {
		this.srcStartloc = srcStartloc;
	}

	public int getSrcEndloc() {
		return srcEndloc;
	}

	public void setSrcEndloc(int srcEndloc) {
		this.srcEndloc = srcEndloc;
	}

	public int getSrcWordcount() {
		return srcWordcount;
	}

	public void setSrcWordcount(int srcWordcount) {
		this.srcWordcount = srcWordcount;
	}

	public int geteStartLoc() {
		return eStartLoc;
	}

	public void seteStartLoc(int eStartLoc) {
		this.eStartLoc = eStartLoc;
	}

	public int geteEndLoc() {
		return eEndLoc;
	}

	public void seteEndLoc(int eEndLoc) {
		this.eEndLoc = eEndLoc;
	}



	
	
}
