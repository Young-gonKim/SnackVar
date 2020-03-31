package com.opaleye.snackvar;

public class EquivExpression implements Comparable<EquivExpression> {
	private int gIndex1, gIndex2;
	private String HGVS;
	
	public EquivExpression(int gIndex1, int gIndex2, String hGVS) {
		super();
		this.gIndex1 = gIndex1;
		this.gIndex2 = gIndex2;
		HGVS = hGVS;
	}

	//equiv expression 끼리의 비교이므로 gIndex1, gIndex2사이 간격 같음. gIndex1끼리만 비교하면 됨.
	public int compareTo(EquivExpression e) {
		int g1 = this.gIndex1;
		int g2 = e.getgIndex1();
		return g2 - g1;
	}

	public int getgIndex1() {
		return gIndex1;
	}

	public void setgIndex1(int gIndex1) {
		this.gIndex1 = gIndex1;
	}

	public int getgIndex2() {
		return gIndex2;
	}

	public void setgIndex2(int gIndex2) {
		this.gIndex2 = gIndex2;
	}

	public String getHGVS() {
		return HGVS;
	}

	public void setHGVS(String hGVS) {
		HGVS = hGVS;
	}
}
