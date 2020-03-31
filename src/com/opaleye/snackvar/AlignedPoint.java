package com.opaleye.snackvar;


/**
 * Title : AlignedPoint
 * A container class for a column of alignment (reference symbol, fwd/rev trace symbol, indicies, etc.)
 * @author Young-gon Kim
 * 2018.5.
 */
public class AlignedPoint {
	
	private char refChar, fwdChar, revChar;
	private char discrepency;
	private boolean coding;
	//private boolean exon;
	private String stringCIndex;
	private int gIndex;
	private int fwdQuality, revQuality;
	private int fwdTraceIndex, revTraceIndex;

	/**
	 * A constructor 
	 * @param refChar : reference character
	 * @param fwdChar : forward trace character
	 * @param revChar : reverse trace character
	 * @param discrepency : if discrepency exits : '*', poor quality : '+'
	 * @param gIndex : genomic DNA index
	 * @param fwdTraceIndex : forward trace index
	 * @param revTraceIndex : reverse trace index
	 */
	public AlignedPoint(char refChar, char fwdChar, char revChar, char discrepency, int gIndex, int fwdTraceIndex, int revTraceIndex) {
		super();
		this.refChar = refChar;
		this.fwdChar = fwdChar;
		this.revChar = revChar;
		this.discrepency = discrepency;
		this.gIndex = gIndex;
		this.fwdTraceIndex = fwdTraceIndex;
		this.revTraceIndex = revTraceIndex;
	}

	/**
	 * Getters and setters for member variables
	 */
	public int getGIndex() {
		return gIndex;
	}
	public void setGIndex(int gIndex) {
		this.gIndex = gIndex;
	}
	
	/*
	public boolean isExon() {
		return exon;
	}
	public void setExon(boolean exon) {
		this.exon = exon;
	}
	*/
	public int getFwdQuality() {
		return fwdQuality;
	}
	public void setFwdQuality(int fwdQuality) {
		this.fwdQuality = fwdQuality;
	}
	public int getRevQuality() {
		return revQuality;
	}
	public void setRevQuality(int revQuality) {
		this.revQuality = revQuality;
	}
	public char getRefChar() {
		return refChar;
	}
	public void setRefChar(char refChar) {
		this.refChar = refChar;
	}
	public char getFwdChar() {
		return fwdChar;
	}
	public void setFwdChar(char fwdChar) {
		this.fwdChar = fwdChar;
	}
	public char getRevChar() {
		return revChar;
	}
	public void setRevChar(char revChar) {
		this.revChar = revChar;
	}
	public char getDiscrepency() {
		return discrepency;
	}
	public void setDiscrepency(char discrepency) {
		this.discrepency = discrepency;
	}
	public boolean isCoding() {
		return coding;
	}
	public void setCoding(boolean coding) {
		this.coding = coding;
	}
	public String getStringCIndex() {
		return stringCIndex;
	}
	public void setStringCIndex(String stringCIndex) {
		this.stringCIndex = stringCIndex;
	}
	public int getFwdTraceIndex() {
		return fwdTraceIndex;
	}
	public void setFwdTraceIndex(int fwdTraceIndex) {
		this.fwdTraceIndex = fwdTraceIndex;
	}
	public int getRevTraceIndex() {
		return revTraceIndex;
	}
	public void setRevTraceIndex(int revTraceIndex) {
		this.revTraceIndex = revTraceIndex;
	}
}
