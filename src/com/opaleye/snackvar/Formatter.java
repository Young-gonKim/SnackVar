package com.opaleye.snackvar;

import java.util.TreeMap;
import java.util.Vector;

import com.opaleye.snackvar.mmalignment.AlignedPair;
import com.opaleye.snackvar.reference.Reference;

/**
 * Title : Formatter
 * Contains functions that make a list of AlignedPoints based on the result of jAligner
 * Formatting functions are derived from jaligner.formats.Pair.format() 
 * @author Young-gon Kim
 * 2018.5.
 */

public class Formatter {
	
	public static final char gapChar = '-';
	public TreeMap<Integer, Integer> fwdCoordinateMap = new TreeMap<Integer, Integer>();
	public TreeMap<Integer, Integer> revCoordinateMap = new TreeMap<Integer, Integer>();;

	//public static BiMap<Integer, Integer> fwdCoordinateMap = HashBiMap.create();
	//public static BiMap<Integer, Integer> revCoordinateMap = HashBiMap.create();

	public int fwdStartOffset = 700;
	public int revStartOffset = 700;
	public int fwdNewLength = 0;
	public int revNewLength = 0;
	public int fwdTraceAlignStartPoint = 1;
	public int revTraceAlignStartPoint = 1;
	private int firstNumber = 1;
	
	

	public Formatter(int firstNumber) {
		this.firstNumber = firstNumber;
		//fwdCoordinateMap = HashBiMap.create();
		//revCoordinateMap = HashBiMap.create();

		fwdCoordinateMap = new TreeMap<Integer, Integer> ();
		revCoordinateMap = new TreeMap<Integer, Integer> ();


		//getImage2에서 그림 그려줄때 fwd, rev 길이 맞춰주기 위한 변수. 짧은놈에 offset 만큼 길이를 더해줘서 같은길이 되게
		//그래서 scroll 같이 될 수 있게.  단위는 Trace (base 아님)
		// 기본 : 화면 절반. 대략 700.
		fwdStartOffset = 700;
		revStartOffset = 700;
		fwdNewLength = 0;
		revNewLength = 0;
		fwdTraceAlignStartPoint = 1;
		revTraceAlignStartPoint = 1;

	}

	/**
	 * Returns a list (Vector) of AlignedPoints
	 * Used for the alignment of 2 sequences (reference vs fwd or rev)
	 * @param alignment : the result of the alignment of jAligner (ref vs fwd or rev)
	 * @param refFile : reference file
	 * @param trace : fwd or rev trace
	 * @param direction : 1:forward, -1:reverse
	 * 	@throws ArrayIndexOutOfBoundsExeption when the alignment fails
	 * 
	 * This functions is derived from jaligner.formats.Pair.format()
	 * 2018.5
	 */

	public Vector<AlignedPoint> format2(AlignedPair ap, Reference refFile, GanseqTrace trace, int direction) throws ArrayIndexOutOfBoundsException {


		//(1) Reference 상에서의 좌표. 1부터 시작. +1 해줌. genomic DNA의 의미. 실제 array access에 사용되지 않음. 
		int refPos = ap.getStart1()+1;
		//(2) Trace 상에서의 좌표. Qcall, BaseCall 읽기위함. Qcall, BaseCall은 0부터 저장되어 있으므로 0부터시작. 
		int tracePos = ap.getStart2();
		char[] refSeq = ap.getAlignedString1().toCharArray();
		char[] traceSeq = ap.getAlignedString2().toCharArray();
		int alignmentLength = ap.getAlignedString1().length();

		//(3)Alignment 상에서의 좌표. (즉 char[] refSeq, traceSeq 의 index) 0부터 시작. 
		int alignmentPos = 0;
		Vector<AlignedPoint> alignedPoints = new Vector<AlignedPoint>();
		while(alignmentPos < alignmentLength) {
			char refChar = refSeq[alignmentPos];
			char traceChar = traceSeq[alignmentPos];
			AlignedPoint tempPoint = null;

			if(refChar != gapChar) {
				char discrepency = ' ';
				if(refChar!=traceChar)
					discrepency = '*';

				if(direction == 1) 
					tempPoint = new AlignedPoint (refChar, traceChar, gapChar, discrepency, refPos, tracePos+1, -1);
				else if(direction == -1)
					tempPoint = new AlignedPoint (refChar, gapChar, traceChar, discrepency, refPos, -1, tracePos+1);

				if(traceChar!=gapChar) {
					if(direction == 1) {
						tempPoint.setFwdQuality(trace.getQCalls()[tracePos]);
						fwdCoordinateMap.put(new Integer(tracePos+1), new Integer(alignedPoints.size()+1));
					}
					else if(direction == -1) {
						tempPoint.setRevQuality(trace.getQCalls()[tracePos]);
						revCoordinateMap.put(new Integer(tracePos+1), new Integer(alignedPoints.size()+1));
					}
					tracePos++;
				}
				refPos++;
				alignmentPos++;

			}

			else if(refChar == gapChar) {
				char discrepency = '*';
				if(direction == 1)
					tempPoint = new AlignedPoint (refChar, traceChar, gapChar, discrepency, refPos-1, tracePos+1, -1);
				else if (direction == -1)
					tempPoint = new AlignedPoint (refChar, gapChar, traceChar, discrepency, refPos-1, -1, tracePos+1);
				if(traceChar!=gapChar) {
					if(direction == 1) {
						tempPoint.setFwdQuality(trace.getQCalls()[tracePos]);
						fwdCoordinateMap.put(new Integer(tracePos+1), new Integer(alignedPoints.size()+1));
					}
					else if(direction == -1) {
						tempPoint.setRevQuality(trace.getQCalls()[tracePos]);
						revCoordinateMap.put(new Integer(tracePos+1), new Integer(alignedPoints.size()+1));
					}
					tracePos++;
				}
				alignmentPos++; 

			}

			if(tempPoint.getDiscrepency() == ' ') {	
				if(direction == 1 && tempPoint.getFwdQuality()<20) {
					tempPoint.setDiscrepency('+');
				}
				if(direction == -1 && tempPoint.getRevQuality()<20) {
					tempPoint.setDiscrepency('+');
				}
			}
			alignedPoints.add(tempPoint);
		}

		if(direction ==1) 
			fwdNewLength = fwdStartOffset + trace.getTraceLength()*2 + (int)RootController.paneWidth/2;
		else if (direction == -1)
			revNewLength = revStartOffset + trace.getTraceLength()*2 + (int)RootController.paneWidth/2;

		//cDNA number 만들기
		int startGIndex = 0, endGIndex = 0;
		startGIndex = ap.getStart1()+1;
		endGIndex = refPos;

		alignedPoints = addCDnaNumber(alignedPoints, startGIndex, endGIndex, refFile);

		return alignedPoints;
	}

	/**
	 * Returns a list (Vector) of AlignedPoints
	 * Used for the alignment of 3 sequences (reference vs fwd and rev)
	 * @param fwdAlignment : the result of the alignment of jAligner (fwd vs ref)
	 * @param revAlignment : the result of the alignment of jAligner (rev vs ref)
	 * @param refFile : reference file
	 * @param fwdTrace : fwd trace
	 * @param revTrace : rev trace
	 * @throws ArrayIndexOutOfBoundsExeption when the alignment fails
	 * @throws NoContigExeption when fwd trace and rev trace don't have overlap
	 * 
	 * This functions is derived from jaligner.formats.Pair.format()
	 * 2018.5
	 */
	public Vector<AlignedPoint> format3(AlignedPair fwdAp, AlignedPair revAp, Reference refFile, GanseqTrace fwdTrace, GanseqTrace revTrace) throws ArrayIndexOutOfBoundsException, NoContigException {

		int fwdAlignmentLength = fwdAp.getAlignedString1().length();
		int revAlignmentLength = revAp.getAlignedString1().length();

		//(1) Reference 상에서의 좌표. 1부터 시작. +1 해줌. genomic DNA의 의미. 실제 array access에 사용되지 않음. 
		int fwdRefPos = fwdAp.getStart1()+1;
		int revRefPos = revAp.getStart1()+1;

		//(2) Trace 상에서의 좌표. Qcall, BaseCall 읽기위함. Qcall, BaseCall은 0부터 저장되어 있으므로 0부터시작. 
		int fwdTracePos = fwdAp.getStart2();
		int revTracePos = revAp.getStart2(); 


		char[] fwdRefSeq = fwdAp.getAlignedString1().toCharArray();
		char[] fwdTraceSeq = fwdAp.getAlignedString2().toCharArray();
		char[] revRefSeq = revAp.getAlignedString1().toCharArray();
		char[] revTraceSeq = revAp.getAlignedString2().toCharArray();

		//(3)Alignment 상에서의 좌표. (즉 char[] fwdRefSeq, fwdTraceSeq, revRefSeq, revTraceSeq 의 index) 0부터 시작. 
		int fwdAlignmentPos = 0;
		int revAlignmentPos = 0;

		Vector<AlignedPoint> alignedPoints = new Vector<AlignedPoint>();

		//앞쪽에 fwd, rev 중 하나만 있는 영역
		//fwd가 앞에 튀어나온 경우
		if(fwdRefPos < revRefPos) {

			//fwd와 rev가 너무 멀리 떨어져 있어서 겹치는 부분 없을 경우 (그냥 두면 ArrayIndexOutofBoundsException 발생)
			//Reference 좌표끼리의 비교.  어디 어디에 붙었나 가지고 비교하는거니까.
			if(revRefPos-fwdRefPos > fwdRefSeq.length) 
				throw new NoContigException();

			while(fwdRefPos<revRefPos) {	// 만약에 loop 끝나고 나갔을 때 fwdRefChar가 gap이다 : 괜찮음. 조건문에 따라 적절히 처리됨. 
				char fwdRefChar = fwdRefSeq[fwdAlignmentPos];
				char fwdTraceChar = fwdTraceSeq[fwdAlignmentPos];
				char revTraceChar = gapChar;

				AlignedPoint tempPoint = null;
				if(fwdRefChar == gapChar)
					tempPoint = new AlignedPoint (fwdRefChar, fwdTraceChar, revTraceChar, ' ', fwdRefPos-1, fwdTracePos+1, revTracePos+1);
				else {
					tempPoint = new AlignedPoint (fwdRefChar, fwdTraceChar, revTraceChar, ' ', fwdRefPos++, fwdTracePos+1, revTracePos+1);
				}

				if(fwdTraceChar!=gapChar) {
					tempPoint.setFwdQuality(fwdTrace.getQCalls()[fwdTracePos]);
					//Trace 그림 보여줄때 좌표 통일하기 위해 mapping 만들어둠.
					//보여주는 좌표는 1부터 시작하므로 +1 (fwdTracePos는 위에서 ++ 했으므로 그냥)
					fwdCoordinateMap.put(new Integer(fwdTracePos+1), new Integer(alignedPoints.size()+1));
					fwdTracePos++;
				}
				alignedPoints.add(tempPoint);
				fwdAlignmentPos++;

			}

			revStartOffset += fwdTrace.getBaseCalls()[fwdTracePos-1] * 2;
		}

		//rev가 앞에 튀어나온 경우
		else if(revRefPos < fwdRefPos) {

			if(fwdRefPos-revRefPos > revRefSeq.length) 
				throw new NoContigException();

			while(revRefPos<fwdRefPos) {
				char revRefChar = revRefSeq[revAlignmentPos];
				char revTraceChar = revTraceSeq[revAlignmentPos];
				char fwdTraceChar = gapChar;

				AlignedPoint tempPoint = null;
				if(revRefChar == gapChar)
					tempPoint = new AlignedPoint (revRefChar, fwdTraceChar, revTraceChar, ' ', revRefPos-1, fwdTracePos+1, revTracePos+1);
				else {
					tempPoint = new AlignedPoint (revRefChar, fwdTraceChar, revTraceChar, ' ', revRefPos++, fwdTracePos+1, revTracePos+1);
				}

				if(revTraceChar!=gapChar) {
					tempPoint.setRevQuality(revTrace.getQCalls()[revTracePos]);
					//Trace 그림 보여줄때 좌표 통일하기 위해 mapping 만들어둠.
					//보여주는 좌표는 1부터 시작하므로 +1 (revTracePos는 위에서 ++ 했으므로 그냥)
					revCoordinateMap.put(new Integer(revTracePos+1), new Integer(alignedPoints.size()+1));
					revTracePos++;
				}
				alignedPoints.add(tempPoint);
				revAlignmentPos++;
			}
			fwdStartOffset += revTrace.getBaseCalls()[revTracePos-1] * 2;
		}
		fwdTraceAlignStartPoint = fwdTracePos+1;
		revTraceAlignStartPoint = revTracePos+1;

		//둘 중에 하나가 끝나면 loop 빠져나감.
		while(fwdAlignmentPos < fwdAlignmentLength && revAlignmentPos < revAlignmentLength) {

			char fwdRefChar = fwdRefSeq[fwdAlignmentPos];
			char revRefChar = revRefSeq[revAlignmentPos];
			char fwdTraceChar = fwdTraceSeq[fwdAlignmentPos];
			char revTraceChar = revTraceSeq[revAlignmentPos];
			AlignedPoint tempPoint = null;

			//System.out.println("fwdAli, revAli, fwdRef, revRef, fwdTra, revTra, fwdRC, revRc, fwdTc, revTc");
			//System.out.println(String.format("%d, %d, %d, %d, %d, %d, %c, %c, %c, %c", 
			//		fwdAlignmentPos, revAlignmentPos, fwdRefPos, revRefPos, fwdTracePos,
			//		revTracePos, fwdRefChar, revRefChar, fwdTraceChar, revTraceChar));

			//homozygous insertion 없음. 
			if(fwdRefChar == revRefChar && fwdRefChar != gapChar) {
				char discrepency = ' ';
				if(fwdRefChar!=fwdTraceChar || revRefChar != revTraceChar)
					discrepency = '*';
				tempPoint = new AlignedPoint (fwdRefChar, fwdTraceChar, revTraceChar, discrepency, fwdRefPos, fwdTracePos+1, revTracePos+1);
				if(fwdTraceChar!=gapChar) {
					tempPoint.setFwdQuality(fwdTrace.getQCalls()[fwdTracePos]);
					fwdCoordinateMap.put(new Integer(fwdTracePos+1), new Integer(alignedPoints.size()+1));
					fwdTracePos++;
				}
				if(revTraceChar!=gapChar) {
					tempPoint.setRevQuality(revTrace.getQCalls()[revTracePos]);
					revCoordinateMap.put(new Integer(revTracePos+1), new Integer(alignedPoints.size()+1));
					revTracePos++;
				}
				fwdRefPos++;
				revRefPos++;
				fwdAlignmentPos++;
				revAlignmentPos++;
			}

			//양쪽 alignment에서 둘다 homozygous insertion이 있는 경우.
			else if(fwdRefChar == gapChar && revRefChar == gapChar) {
				char discrepency = '*';
				tempPoint = new AlignedPoint (fwdRefChar, fwdTraceChar, revTraceChar, discrepency, fwdRefPos-1, fwdTracePos+1, revTracePos+1);
				if(fwdTraceChar!=gapChar) {
					tempPoint.setFwdQuality(fwdTrace.getQCalls()[fwdTracePos]);
					fwdCoordinateMap.put(new Integer(fwdTracePos+1), new Integer(alignedPoints.size()+1));
					fwdTracePos++;
				}
				if(revTraceChar!=gapChar) {
					tempPoint.setRevQuality(revTrace.getQCalls()[revTracePos]);
					revCoordinateMap.put(new Integer(revTracePos+1), new Integer(alignedPoints.size()+1));
					revTracePos++;
				}
				fwdAlignmentPos++; 
				revAlignmentPos++;
			}

			//fwd에만 homo insertion으로 인한 GAP 있는 경우. rev는 쉰다.
			else if(fwdRefChar==gapChar) {
				tempPoint = new AlignedPoint (gapChar, fwdTraceChar, gapChar, '*', fwdRefPos-1, fwdTracePos+1, revTracePos+1);

				tempPoint.setFwdQuality(fwdTrace.getQCalls()[fwdTracePos]);
				fwdCoordinateMap.put(new Integer(fwdTracePos+1), new Integer(alignedPoints.size()+1));

				fwdAlignmentPos++; 
				fwdTracePos++;
			}

			//rev에만 homo insertion으로 인한 GAP 있는경우. fwd는 쉰다.
			else if(revRefChar==gapChar) {
				tempPoint = new AlignedPoint (gapChar, gapChar, revTraceChar, '*', fwdRefPos-1, fwdTracePos+1, revTracePos+1);
				tempPoint.setRevQuality(revTrace.getQCalls()[revTracePos]);
				revCoordinateMap.put(new Integer(revTracePos+1), new Integer(alignedPoints.size()+1));
				revAlignmentPos++;
				revTracePos++;
			}

			if(tempPoint.getDiscrepency() == ' ') {	
				if(tempPoint.getFwdQuality()<30 && tempPoint.getRevQuality()<30) {
					tempPoint.setDiscrepency('+');
				}
			}
			alignedPoints.add(tempPoint);
		}

		//뒷쪽에 튀어나온 부분 처리.
		//fwd가 튀어나온 경우
		if(revAlignmentPos == revAlignmentLength) {
			if(fwdTracePos<=0) throw new NoContigException();

			while(fwdAlignmentPos < fwdAlignmentLength) {
				char refChar = fwdRefSeq[fwdAlignmentPos];
				char fwdTraceChar = fwdTraceSeq[fwdAlignmentPos];
				char revTraceChar = gapChar;

				AlignedPoint tempPoint = null;
				if(refChar == gapChar) 
					tempPoint = new AlignedPoint (refChar, fwdTraceChar, revTraceChar, ' ', fwdRefPos-1, fwdTracePos+1, revTracePos);
				else
					tempPoint = new AlignedPoint (refChar, fwdTraceChar, revTraceChar, ' ', fwdRefPos++, fwdTracePos+1, revTracePos);

				if(fwdTraceChar!=gapChar) {
					tempPoint.setFwdQuality(fwdTrace.getQCalls()[fwdTracePos]);
					fwdCoordinateMap.put(new Integer(fwdTracePos+1), new Integer(alignedPoints.size()+1));
					fwdTracePos++;
				}
				alignedPoints.add(tempPoint);
				fwdAlignmentPos++;

			}
		}

		//rev가 튀어나온 경우
		else if(fwdAlignmentPos == fwdAlignmentLength) {
			if(revTracePos<=0) throw new NoContigException(); 

			while(revAlignmentPos < revAlignmentLength) {
				char refChar = revRefSeq[revAlignmentPos];
				char fwdTraceChar = gapChar;
				char revTraceChar = revTraceSeq[revAlignmentPos];

				AlignedPoint tempPoint = null;
				if(refChar == gapChar) 
					tempPoint = new AlignedPoint (refChar, fwdTraceChar, revTraceChar, ' ', revRefPos-1, fwdTracePos, revTracePos+1);
				else
					tempPoint = new AlignedPoint (refChar, fwdTraceChar, revTraceChar, ' ', revRefPos++, fwdTracePos, revTracePos+1);

				if(revTraceChar!=gapChar) {
					tempPoint.setRevQuality(revTrace.getQCalls()[revTracePos]);
					revCoordinateMap.put(new Integer(revTracePos+1), new Integer(alignedPoints.size()+1));
					revTracePos++;
				}
				alignedPoints.add(tempPoint);
				revAlignmentPos++;

			}
		}

		fwdNewLength = fwdStartOffset + fwdTrace.getTraceLength()*2 + (int)RootController.paneWidth/2;
		revNewLength = revStartOffset + revTrace.getTraceLength()*2 + (int)RootController.paneWidth/2;
		
		fwdNewLength = Integer.max(fwdNewLength, revNewLength);
		revNewLength = Integer.max(fwdNewLength, revNewLength);
			
		
		System.out.println(String.format("fwdLength : %d, revLength : %d",  fwdNewLength, revNewLength));

		//cDNA number 만들기


		int startGIndex = 0, endGIndex = 0;
		if(fwdAp.getStart1() <= revAp.getStart1()) startGIndex = fwdAp.getStart1()+1;
		else startGIndex = revAp.getStart1()+1;

		if(fwdRefPos >= revRefPos) endGIndex = fwdRefPos;
		else endGIndex = revRefPos;

		alignedPoints = addCDnaNumber(alignedPoints, startGIndex, endGIndex, refFile);

		return alignedPoints;
	}

	/**
	 * returns true if the gIndex is on exon
	 * returns false if the gIndex is on intron
	 * @param exonStart : a list of start positions of exons in the gene
	 * @param exonEnd : a list of end positions of exons in the gene
	 * @param gIndex : target Index (genomic DNA index)
	 */
	/*
	private static boolean isExon(Vector<Integer> exonStart, Vector<Integer> exonEnd, int gIndex) {
		if(exonStart == null || exonEnd == null) 
			return false;

		for(int i=0;i<exonStart.size();i++) {
			int start = (exonStart.get(i)).intValue();
			int end = (exonEnd.get(i)).intValue();
			if(gIndex >= start && gIndex <= end) return true;
		}
		return false;
	}
	 */

	/**
	 * makes a cDNA indexr for each points in alignedPoints
	 * @param alignedPoints : target array of AlignedPoints
	 * @param startGIndex : start position on genomic DNA 
	 * @param endGIndex : end position on genomic DNA
	 * @param refFile : reference file
	 */
	private Vector<AlignedPoint> addCDnaNumber (Vector<AlignedPoint> alignedPoints, int startGIndex, int endGIndex, Reference refFile) {
		TreeMap<Integer, String> cdnaMap = new TreeMap<Integer, String>();

		int intCDnaStart = 0, intCDnaEnd = 0;
		int cdsIndex = 0; // 몇번째 cds region

		Vector<Integer> cDnaStart = refFile.getcDnaStart();
		Vector<Integer> cDnaEnd = refFile.getcDnaEnd();

		if(cDnaStart != null && cDnaEnd != null) {
			cdsIndex = 0;
			int cDNA = firstNumber-1;
			for(int i=0;i<cDnaStart.size();i++) {
				intCDnaStart = (cDnaStart.get(i)).intValue();
				intCDnaEnd = (cDnaEnd.get(i)).intValue();

				if(startGIndex > intCDnaEnd) {
					cDNA += (intCDnaEnd - intCDnaStart +1);
				}
				else if (startGIndex <= intCDnaEnd && startGIndex >= intCDnaStart) {
					cDNA += (startGIndex-intCDnaStart);
					cdsIndex = i;
					break;
				}
				else {
					cdsIndex = i;
					break;
				}
			}


			for(int i=startGIndex;i<=endGIndex;i++) {
				String tempCIndex = "c.";

				//coding 일때
				if(i >= intCDnaStart && i <= intCDnaEnd) {
					cDNA++;
					tempCIndex += cDNA;
					cdnaMap.put(new Integer(i), tempCIndex);

					if(i==intCDnaEnd) {
						if(cdsIndex < (cDnaStart.size()-1)) {
							cdsIndex++;
							intCDnaStart = (cDnaStart.get(cdsIndex)).intValue();
							intCDnaEnd = (cDnaEnd.get(cdsIndex)).intValue();
						}
					}
				}
				//non-Coding 일때
				else {
					if(cdsIndex==0 && i < intCDnaStart) { //5' of first CDS
						int offSet = intCDnaStart - i;
						if(firstNumber > 1) {
							tempCIndex += firstNumber + "-" + offSet;
						}
						else {
							tempCIndex += "-" + offSet;
						}
					}
					else if (cdsIndex == cDnaStart.size()-1 && i > intCDnaEnd) { //3' of last CDS
						int offSet = i-intCDnaEnd;
						tempCIndex += "*" + offSet;
					}

					else {	//intron 
						int leftOffset = i-(cDnaEnd.get(cdsIndex-1)).intValue();
						int rightOffset = (cDnaStart.get(cdsIndex)).intValue() - i;

						if(leftOffset <= rightOffset) 
							tempCIndex += cDNA + "+" + leftOffset;

						else 
							tempCIndex += (cDNA+1) + "-" + rightOffset;
					}
					cdnaMap.put(new Integer(i), tempCIndex);
				}
			}

		}


		Vector<AlignedPoint> tempAlignedPoints = new Vector<AlignedPoint>();
		for(int i=0;i<alignedPoints.size();i++) {
			AlignedPoint tempPoint = alignedPoints.get(i);
			int tempGIndex = tempPoint.getGIndex();
			String stringTempCIndex = cdnaMap.get(new Integer(tempGIndex));
			boolean coding;
			if(stringTempCIndex.contains("+") || stringTempCIndex.contains("-"))
				coding = false;
			else 
				coding = true;
			tempPoint.setStringCIndex(stringTempCIndex);
			tempPoint.setCoding(coding);

			//tempPoint.setExon(isExon(refFile.getExonStart(), refFile.getExonEnd(), tempGIndex));

			tempAlignedPoints.add(tempPoint);

		}
		return tempAlignedPoints;
	}

	public int getFirstNumber() {
		return firstNumber;
	}

	public void setFirstNumber(int firstNumber) {
		this.firstNumber = firstNumber;
	}

}
