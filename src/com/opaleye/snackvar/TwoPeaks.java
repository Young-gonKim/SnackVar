package com.opaleye.snackvar;

public class TwoPeaks {
	private boolean secondPeakExist;
	private char firstBase, secondBase;
	private int firstPeakHeight, secondPeakHeight;
	
	public TwoPeaks(char firstBase, char secondBase, int firstPeakHeight, int secondPeakHeight, boolean secondPeakExist) {
		super();
		this.firstBase = firstBase;
		this.secondBase = secondBase;
		this.firstPeakHeight = firstPeakHeight;
		this.secondPeakHeight = secondPeakHeight;
		this.secondPeakExist = secondPeakExist;
	}
	
	public boolean secondPeakExist() {
		return secondPeakExist;
	}
	public char getFirstBase() {
		return firstBase;
	}
	public char getSecondBase() {
		return secondBase;
	}

	
	

}
