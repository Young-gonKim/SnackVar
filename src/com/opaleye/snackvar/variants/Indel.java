package com.opaleye.snackvar.variants;

import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import org.biojava.bio.symbol.IllegalAlphabetException;

import com.opaleye.snackvar.AlignedPoint;
import com.opaleye.snackvar.EquivExpression;
import com.opaleye.snackvar.Formatter;
import com.opaleye.snackvar.RootController;

import javafx.beans.property.SimpleStringProperty;

/**
 * Title : Indel
 * A subclass of Variant
 * A class for Indel Variant
 * @author Young-gon Kim
 *2018.10
 */
public class Indel extends Variant{
	public static final int deletion = -1;
	public static final int insertion = 1;
	public static final int duplication = 2;
	public static final int delins = 0;

	private int type;
	private int gIndex2;
	private boolean coding1, coding2;
	private String cIndex2;
	private String indelSeq;

	protected TreeSet<EquivExpression> equivExpressionList;



	private int getAlignedIndexFromGIndex(int gIndex) throws Exception {
		Vector<AlignedPoint> aps = rootController.alignedPoints;
		for(int i=0;i<aps.size();i++) {
			AlignedPoint ap = aps.get(i);
			if(ap.getGIndex()==gIndex) return (i+1);	//찾았으면 최소 1 return
		}
		return 0;
	}

	private String getMutatedSeq(int newGIndex1, int newGIndex2) throws Exception {
		String ret = "";
		Vector<AlignedPoint> aps = rootController.alignedPoints;
		//int newGIndex1 = gIndex + offset;
		//int newGIndex2 = gIndex2 + offset;

		AlignedPoint firstPoint = aps.get(0);
		AlignedPoint lastPoint = aps.get(aps.size()-1);
		if(newGIndex1<firstPoint.getGIndex() || newGIndex2>lastPoint.getGIndex()) 
			return null;


		StringBuffer dupBuffer = new StringBuffer();
		for(int i=0;i<aps.size();i++) {
			AlignedPoint ap = aps.get(i);
			if(ap.getRefChar()== Formatter.gapChar) 
				continue;
			if(type == deletion) {
				if(ap.getGIndex()>=newGIndex1 && ap.getGIndex()<=newGIndex2) {
					//do nothing
				}
				else  {
					ret += ap.getRefChar();
				}
			}
			else if(type == insertion) {
				ret += ap.getRefChar();
				if(ap.getGIndex() == newGIndex1) {
					ret +=indelSeq;
				}
			}
			else if(type == duplication) {
				if(ap.getGIndex()>=newGIndex1 && ap.getGIndex()<=newGIndex2) {
					dupBuffer.append(ap.getRefChar());
				}
				ret += ap.getRefChar();
				if(ap.getGIndex() == newGIndex2) 
					ret += dupBuffer.toString();
			}
			else if(type == delins) {
				if(ap.getGIndex()>=newGIndex1 && ap.getGIndex()<=newGIndex2) {
					//do nothing
				}
				else { 
					ret += ap.getRefChar();
				}

				if(ap.getGIndex() == newGIndex1) 
					ret += indelSeq;
			}
		}

		return ret;

	}


	//constructor for Indel
	public Indel(RootController rootController, String zygosity, int direction, int type, int indelStartIndex, int indelEndIndex, int focusedIndex,  String indelSeq, boolean onTarget) {
		super();
		this.rootController = rootController;
		equivExpressionList = new TreeSet<EquivExpression>();
		Vector<AlignedPoint> aps = rootController.alignedPoints;
		this.direction = direction;
		this.zygosity = zygosity;
		this.onTarget = onTarget;
		this.type = type;
		this.indelSeq = indelSeq;
		if(indelSeq == null) indelSeq = "";
		this.onTarget = true;
		AlignedPoint ap3 = aps.get(focusedIndex-1);
		this.alignmentIndex = focusedIndex;
		this.fwdTraceIndex = ap3.getFwdTraceIndex();
		this.revTraceIndex = ap3.getRevTraceIndex();
		if(direction == 1) {
			this.fwdTraceChar = ap3.getFwdChar();
			this.revTraceChar = Formatter.gapChar;
		}
		else if(direction == -1) {
			this.fwdTraceChar = Formatter.gapChar;
			this.revTraceChar = ap3.getRevChar();
		}


		//초기 ap1, ap2 및 파생변수
		AlignedPoint ap1 = aps.get(indelStartIndex-1);
		AlignedPoint ap2 = aps.get(indelEndIndex-1);
		this.cIndex = ap1.getStringCIndex();
		this.cIndex2 = ap2.getStringCIndex();
		this.coding1 = ap1.isCoding();
		this.coding2 = ap2.isCoding();
		this.gIndex = ap1.getGIndex();
		this.gIndex2 = ap2.getGIndex();

		int originalGIndex = gIndex;
		int originalGIndex2 = gIndex2;

		EquivExpression equivExpression = makeHGVS(indelStartIndex, indelEndIndex);
		equivExpressionList.add(equivExpression);	
		//HGVS = equivExpression.getHGVS();

		//Left Align된적용시 주석해제
		//int leftAlignedStartIndex = 0;
		//int leftAlignedEndIndex = 0;


		//EquivList 만들기. 
		try {
			String originalSeq = getMutatedSeq(gIndex, gIndex2);
			int offset = -1;
			//left 탐색
			for(;;offset--) {
				String mutatedSeq = getMutatedSeq(gIndex+offset, gIndex2+offset);
				if(mutatedSeq == null) {
					break;
				}
				else if(mutatedSeq.equals(originalSeq)) {
					int tempStartIndex = getAlignedIndexFromGIndex(gIndex+offset);
					int tempEndIndex = getAlignedIndexFromGIndex(gIndex2+offset);
					equivExpression = makeHGVS(tempStartIndex, tempEndIndex);
					equivExpressionList.add(equivExpression);
				}
			}


			//left Align 하지말기. Alignment, Chromatogram 과 안맞음. 그냥 equivalent expression 다 보여주기. 
			//그럼 그중에 맨 왼쪽꺼가 left alignment 된거니까. 

			/* left Align 사용시 아래 코드
			leftAlignedStartIndex = indelStartIndex;
			leftAlignedEndIndex = indelEndIndex;


			leftAlignedStartIndex = getAlignedIndexFromGIndex(gIndex+maxLeftOffset);
			leftAlignedEndIndex = getAlignedIndexFromGIndex(gIndex2+maxLeftOffset);
			if(leftAlignedStartIndex >0  && leftAlignedEndIndex >0) {	
				ap1 = aps.get(leftAlignedStartIndex-1);
				ap2 = aps.get(leftAlignedEndIndex-1);
				this.cIndex = ap1.getStringCIndex();
				this.cIndex2 = ap2.getStringCIndex();
				this.coding1 = ap1.isCoding();
				this.coding2 = ap2.isCoding();
				this.gIndex = ap1.getGIndex();
				this.gIndex2 = ap2.getGIndex();
			}
			 */


			//Right로도 탐색 (equivList 만들기 위해)
			offset = 1;
			for(;;offset++) {
				String mutatedSeq = getMutatedSeq(originalGIndex+offset, originalGIndex2+offset);
				if(mutatedSeq == null) {
					break;
				}
				else if(mutatedSeq.equals(originalSeq)) {
					int tempStartIndex = getAlignedIndexFromGIndex(originalGIndex+offset);
					int tempEndIndex = getAlignedIndexFromGIndex(originalGIndex2+offset);
					equivExpression = makeHGVS(tempStartIndex, tempEndIndex);
					equivExpressionList.add(equivExpression);
				}
			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}

		//Left Align 적용시 주석 해제
		//equivExpression = makeHGVS(leftAlignedStartIndex, leftAlignedEndIndex);
		//HGVS = equivExpression.getHGVS();

		

		if(coding1 && coding2 && rootController.formatter.getFirstNumber() == 1) makeAAChange();

		//right alignment
		EquivExpression rtMostExpression = null;
		int rtMostIndex = -1;
		Iterator<EquivExpression> iter = equivExpressionList.iterator();
		while(iter.hasNext()) {
			EquivExpression tempEquiv = (EquivExpression)iter.next();
			if(rtMostIndex < tempEquiv.getgIndex2()) {
				rtMostIndex = tempEquiv.getgIndex2();
				rtMostExpression = tempEquiv;
			}
		}
		
		HGVS = rtMostExpression.getHGVS();

		makeTableViewProperties();
	}

	/**
	 * Generates HGVS nomenclature
	 */

	//type, cIndex, cIndex2, indelSeq
	private EquivExpression makeHGVS(int alignedIndex1, int alignedIndex2) {
		String tempHGVS="";
		Vector<AlignedPoint> aps = rootController.alignedPoints;
		AlignedPoint ap1 = aps.get(alignedIndex1-1);
		AlignedPoint ap2 = aps.get(alignedIndex2-1);
		String localCIndex1 = ap1.getStringCIndex();
		String localCIndex2 = ap2.getStringCIndex();
		int gIndex1 = ap1.getGIndex();
		int gIndex2 = ap2.getGIndex();


		if(type == deletion) {
			//make indelSeq
			StringBuffer sb = new StringBuffer();
			for(int i=alignedIndex1;i<=alignedIndex2;i++) {
				AlignedPoint ap3 = aps.get(i-1);
				char refChar = ap3.getRefChar();
				sb.append(refChar);
			}
			String tempIndelSeq = sb.toString();
			
			if(localCIndex1.equals(localCIndex2)) 
				//tempHGVS =  localCIndex1 + "del" + tempIndelSeq;
				tempHGVS =  localCIndex1 + "del";
			else
			{
				if(localCIndex2 != null && localCIndex2.length()>2) 
					localCIndex2 = localCIndex2.substring(2, localCIndex2.length());
				//tempHGVS = localCIndex1 + "_" + localCIndex2 + "del" + tempIndelSeq;
				tempHGVS = localCIndex1 + "_" + localCIndex2 + "del";

			}
		}

		else if(type == insertion) {
			tempHGVS = localCIndex1 + "_" + localCIndex2 + "ins" + indelSeq;
		}

		else if(type ==  duplication) {
			if(localCIndex1.equals(localCIndex2))
				//tempHGVS = localCIndex1 + "dup" + indelSeq;
				tempHGVS = localCIndex1 + "dup";
			else {
				if(localCIndex2 != null &&  localCIndex2.length()>2) 
					localCIndex2 = localCIndex2.substring(2, localCIndex2.length());
				//tempHGVS = localCIndex1+ "_" + localCIndex2 + "dup" + indelSeq;
				tempHGVS = localCIndex1+ "_" + localCIndex2 + "dup";

			}
		}
		else if(type == delins) {
			if(localCIndex1.equals(localCIndex2))
				tempHGVS = localCIndex1 + "delins"+ indelSeq;
			else {
				if(localCIndex2 != null &&  localCIndex2.length()>2) 
					localCIndex2 = localCIndex2.substring(2, localCIndex2.length());
				tempHGVS = localCIndex1+ "_" + localCIndex2 + "delins" + indelSeq;
			}
		}
		return new EquivExpression(gIndex1, gIndex2, tempHGVS);
	}

	/**
	 * Generates AA change
	 */
	private void makeAAChange() {
		//System.out.println("HGVS : " + HGVS);
		int length = 0;
		int i_cIndex1=0, i_cIndex2=0;
		String refString = rootController.reference.getRefString();
		String originalSeq = "";
		String shiftedSeq = "";
		String ptnCoordi = "";

		Vector<Integer> cDnaStart = rootController.reference.getcDnaStart();
		Vector<Integer> cDnaEnd = rootController.reference.getcDnaEnd();
		for(int i=0;i<cDnaStart.size();i++) {
			int intCDnaStart = (cDnaStart.get(i)).intValue();
			int intCDnaEnd = (cDnaEnd.get(i)).intValue();
			originalSeq += refString.substring(intCDnaStart-1, intCDnaEnd);
		}

		try {
			i_cIndex1 = Integer.parseInt(cIndex.replaceAll("[^0-9]",""));
			i_cIndex2 = Integer.parseInt(cIndex2.replaceAll("[^0-9]",""));

			if(type == deletion) {
				length = gIndex2-gIndex+1;
				shiftedSeq = originalSeq.substring(0, i_cIndex1-1) + originalSeq.substring(i_cIndex2);
			}
			else if(type == insertion) {
				length = indelSeq.length();
				shiftedSeq = originalSeq.substring(0, i_cIndex1) + indelSeq + originalSeq.substring(i_cIndex2-1);
			}

			else if(type ==  duplication) {
				length = gIndex2-gIndex+1;
				shiftedSeq = originalSeq.substring(0, i_cIndex1-1) + originalSeq.substring(i_cIndex1-1,i_cIndex2) + originalSeq.substring(i_cIndex1-1);
			}

			else if(type == delins) {
				length = indelSeq.length() - (gIndex2-gIndex+1);
				shiftedSeq = originalSeq.substring(0, i_cIndex1-1) + indelSeq + originalSeq.substring(i_cIndex2);
			}

			if(length % 3 != 0) {		//frameshift
				int fsStartIndex = 0;
				int fsCount = 0;
				int i = 0;

				for(i=0;i<shiftedSeq.length();i+=3) {
					if(i+3>originalSeq.length()) break;
					String originalAA = Variant.getAAfromTriple(originalSeq.substring(i,i+3));
					String shiftedAA = Variant.getAAfromTriple(shiftedSeq.substring(i,i+3));

					//첫번째 aminoacid change가 termination이면 이후에 fs 등 붙일필요 없음. 
					if(shiftedAA.equals("*")) {
						fsStartIndex = (i/3)+1;
						AAChange = "p.(" + originalAA + fsStartIndex + shiftedAA + ")";
						return;
					}
					if(!originalAA.equals(shiftedAA)) {
						fsStartIndex = (i/3)+1;
						ptnCoordi = "p.(" + originalAA + fsStartIndex + shiftedAA + "fs";
						break;
					}
				}

				boolean terminalFound = false;
				for(;i<shiftedSeq.length();i+=3) {
					if(i+3>originalSeq.length()) break;
					fsCount++;
					String shiftedAA = Variant.getAAfromTriple(shiftedSeq.substring(i,i+3));
					System.out.println(String.format("%d : %s, %s",  fsCount, shiftedSeq.substring(i,i+3), shiftedAA));
					if(shiftedAA.equals("*")) {
						terminalFound = true;
						break;
					}
				}
				if(terminalFound)
					AAChange =ptnCoordi + "*" + fsCount + ")";
				else
					AAChange = ptnCoordi+")";
			}
			else {		//No frameshift
				Vector<String> originalAAList = new Vector<String>();
				Vector<String> shiftedAAList = new Vector<String>();

				for(int i=0;i<originalSeq.length();i+=3) {
					if((i+3)>originalSeq.length()) break;
					originalAAList.add(Variant.getAAfromTriple(originalSeq.substring(i,i+3)));
				}

				for(int i=0;i<shiftedSeq.length();i+=3) {
					if((i+3)>shiftedSeq.length()) break;
					shiftedAAList.add(Variant.getAAfromTriple(shiftedSeq.substring(i,i+3)));
				}
				//1부터 시작하는 좌표
				int leftPos = 1;	//마지막으로 같은 지점.
				int originalRightPos = originalAAList.size();
				int shiftedRightPos = shiftedAAList.size();

				//System.out.println("Original AA");
				for(int i=0;i<originalAAList.size();i++) {
					//System.out.print(String.format("%d : %s, ",  (i+1), originalAAList[i]));
				}

				//System.out.println("Shifted AA");
				for(int i=0;i<shiftedAAList.size();i++) {
					//System.out.print(String.format("%d : %s, ",  (i+1), shiftedAAList[i]));
				}

				if(originalAAList.size() == shiftedAAList.size()) {
					boolean same = true;
					for(int i=0;i<originalAAList.size();i++) {
						if(!originalAAList.get(i).equals(shiftedAAList.get(i))) same = false;
					}
					if(same) {
						AAChange = "no amino acid change";
						return;
					}
				}

				for(leftPos=1;leftPos<=originalAAList.size() && leftPos<=shiftedAAList.size();leftPos++) {
					if(!originalAAList.get(leftPos-1).equals(shiftedAAList.get(leftPos-1))) break;
				}
				leftPos--;	//leftPos는 1부터 시작하는 좌표 && 마지막으로 일치하는 지점

				while(originalRightPos>leftPos && shiftedRightPos>leftPos) {
					if(!originalAAList.get(originalRightPos-1).equals(shiftedAAList.get(shiftedRightPos-1))) break;
					originalRightPos--;
					shiftedRightPos--;
				}
				//System.out.println("original Right Pos : " + originalRightPos);
				//System.out.println("shifted Right pos : " + shiftedRightPos);

				//originalRightPos, shiftedRightPos : 처음으로 달라진 좌표 or left랑 만났을때 좌표
				if(leftPos == shiftedRightPos) {	//deletion
					ptnCoordi = "p.(";
					ptnCoordi += originalAAList.get(leftPos+1-1) + (leftPos+1);
					if(leftPos+1 != originalRightPos) ptnCoordi += "_" + originalAAList.get(originalRightPos-1) + originalRightPos;
					ptnCoordi += "del)";
				}
				else if(leftPos == originalRightPos ) { //insertion
					ptnCoordi = "p.(";

					//dup인지 판단.
					boolean dup = true;
					int indelSize = shiftedRightPos - leftPos;
					for(int i=leftPos+1;i<=shiftedRightPos;i++) {
						if(!(shiftedAAList.get(i-1)).equals(shiftedAAList.get(i-1-indelSize))) {
							dup = false;
							break;
						}
					}
					if(dup) {
						ptnCoordi += originalAAList.get(leftPos-1-(indelSize-1)) + (leftPos-(indelSize-1));
						if(indelSize > 1) {
							ptnCoordi += "_";
							ptnCoordi += originalAAList.get(leftPos-1) + (leftPos);
						}
						ptnCoordi += "dup)";
					}
					else {


						ptnCoordi += originalAAList.get(leftPos-1) + (leftPos);
						if(leftPos>=originalAAList.size()) ptnCoordi += "";
						else ptnCoordi += "_" +originalAAList.get(leftPos) + (leftPos+1);
						ptnCoordi += "ins";
						for(int i=leftPos+1;i<=shiftedRightPos;i++) {
							ptnCoordi+=shiftedAAList.get(i-1);
						}
						ptnCoordi+=")";
					}
				}
				else { //delins
					ptnCoordi = "p.(";
					ptnCoordi += originalAAList.get(leftPos+1-1) + (leftPos+1);
					if(leftPos+1 != originalRightPos) ptnCoordi += "_" + originalAAList.get(originalRightPos-1) + originalRightPos;
					ptnCoordi += "delins";
					for(int i=leftPos+1;i<=shiftedRightPos;i++) {
						ptnCoordi+=shiftedAAList.get(i-1);
					}
					ptnCoordi+=")";
				}
				AAChange = ptnCoordi;
			}

		}
		catch(IllegalAlphabetException iae) {
			//probably due to 'N' from indelSeq
			AAChange = "(untranslatable)";
			return;

		}
		catch(Exception ex) {
			//ex.printStackTrace();	안되면 만들지마, AA change 아무데나 치면 다 나오니
			return;
		}
		//HGVS += ", " + AAChange;
		//System.out.println(HGVS);
	}

	/** Getters and setters for member variables*/

	public int getType() {
		return type;
	}
	public String getCIndex2() {
		return cIndex2;
	}
	
	public String getIndelSeq() {
		return indelSeq;
	}

	public int getgIndex2() {
		return gIndex2;
	}

	public TreeSet<EquivExpression> getEquivExpressionList() {
		return equivExpressionList;
	}
	public void setEquivExpressionList(TreeSet<EquivExpression> equivExpressionList) {
		this.equivExpressionList = equivExpressionList;
	}


}
