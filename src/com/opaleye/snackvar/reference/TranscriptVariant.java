package com.opaleye.snackvar.reference;

import java.util.Vector;

/**
 * Title : TranscriptVariant
 * @author Young-gon Kim
 * 2018.10.
 */
public class TranscriptVariant {
	private Vector<Integer> cDnaStart, cDnaEnd;
	private int size = 0;
	private String description = null;
	
	/**
	 * Constructor
	 * @param cDnaStart
	 * @param cDnaEnd
	 * @param size
	 * @param description
	 */
	public TranscriptVariant(Vector<Integer> cDnaStart, Vector<Integer> cDnaEnd, int size, String description) {
		super();
		this.cDnaStart = cDnaStart;
		this.cDnaEnd = cDnaEnd;
		this.size = size;
		this.description = description;
		//System.out.println("size : " + size);
		//System.out.println("description : " + description);
	}
	/**
	 * getter functions
	 */
	public Vector<Integer> getcDnaStart() {
		return cDnaStart;
	}
	public Vector<Integer> getcDnaEnd() {
		return cDnaEnd;
	}
	public int getSize() {
		return size;
	}
	public String getDescription() {
		return description;
	}
}
