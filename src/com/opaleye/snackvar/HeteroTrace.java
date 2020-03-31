package com.opaleye.snackvar;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import com.opaleye.snackvar.variants.Indel;
import com.opaleye.snackvar.variants.Variant;

public class HeteroTrace extends com.opaleye.snackvar.GanseqTrace {
	private final int maxIndelSize = 1000;
	private final double indelCutoff = 0.6;
	private String result = "not run yet";

	//1: insertion, -1 : deletion
	private int insOrDel = -1;
	private int indelSize = 0;
	char[] refSeq = null;
	char[] subtractedSeq = null;	//double peak 있는부분
	char[] subtractedSeq2 = null;  //double peak 없는부분


	private int doublePeakStartIndex = 1;	//1부터 시작하는 좌표
	private int alignedDoublePeakStartIndex = 1;	//1부터 시작하는 좌표, double peak 시작하는 점의 alignedPoints 상에서의 좌표
	private int alignedIndelStartIndex = 0;
	private int alignedIndelEndIndex = 0;


	public HeteroTrace(GanseqTrace trace, RootController rootController) {
		this.rootController = rootController;
		this.direction = trace.direction;
		this.traceA = trace.traceA;
		this.traceT = trace.traceT;
		this.traceG = trace.traceG;
		this.traceC = trace.traceC;
		this.traceLength = trace.traceLength;
		this.traceHeight = trace.traceHeight;
		this.sequence = trace.sequence;
		this.sequenceLength = trace.sequenceLength; 
		this.qCalls = trace.qCalls;
		this.baseCalls = trace.baseCalls;

		this.transformedA = trace.transformedA;
		this.transformedT = trace.transformedT;
		this.transformedG = trace.transformedG;
		this.transformedC = trace.transformedC;
		this.alignedRegionStart = trace.alignedRegionStart;
		this.alignedRegionEnd = trace.alignedRegionEnd;
	}

	/**
	 * returns char1 - char2
	 * char1 : original seq 역할 (potentially ambiguous)
	 * char2 : reference 역할.
	 * 
	 * char1 이 single peak이면 char2에 상관없이 char1과 같은 base return
	 * char1 이 double peak이면, char2에 해당하는게 있으면 빼고 남은거 return
	 *                           char2에 해당하는게 없으면 N return
	 */
	private char subtract(char char1, char char2) {
		char ret = 'N';

		//double peak 없는 경우.
		if(char1=='A' || char1 == 'T' || char1 == 'G' || char1 =='C') {
			ret = char1;
		}

		//double peak 있는경우. 조합에 따라. 
		//double peak 둘다 reference랑 다른경우 : 한 자리에 SNV서로 다른거 두개? rare. 무시하고그냥 N으로.
		else if(char1 == 'R') {
			if(char2 == 'A') ret = 'G';
			else if(char2 == 'G') ret = 'A';
		}
		else if(char1 == 'Y') {
			if(char2 == 'C') ret = 'T';
			else if(char2 == 'T') ret = 'C';
		}
		else if(char1 == 'K') {
			if(char2 == 'T') ret = 'G';
			else if(char2 == 'G') ret = 'T';
		}
		else if (char1 == 'M') {
			if(char2 == 'A') ret = 'C';
			else if(char2 == 'C') ret = 'A';
		}
		else if (char1 == 'S') {
			if(char2 == 'C') ret = 'G';
			else if(char2 == 'G') ret = 'C';
		}
		else if ( char1 == 'W') {
			if(char2 == 'A') ret = 'T';
			else if(char2 == 'T') ret = 'A';
		}

		return ret;

	}


	/**
	 * Returns heteroIndel variant if detected
	 * if not detected, returns nulll
	 */
	public Variant detectHeteroIndel() {
		Variant variant = null;

		/*
		 * Parameters for second peak detection 
		 */
		char[] originalSeq = sequence.toCharArray();

		TreeMap<Integer, Integer> fwdMap = rootController.formatter.fwdCoordinateMap;
		TreeMap<Integer, Integer> revMap = rootController.formatter.revCoordinateMap;

		////////////////////////////
		//second peak 계산
		////////////////////////////
		boolean[] secondPeakExist = new boolean[sequenceLength] ;

		for(int i=0;i<sequenceLength;i++) {
			char baseChar = sequence.charAt(i);
			if(baseChar=='A' || baseChar=='T' || baseChar=='G' || baseChar=='C' || baseChar == Formatter.gapChar) {
				secondPeakExist[i] = false;
			}
			else 
				secondPeakExist[i] = true;
		}


		////////////////////////////
		//Het indel 시작점 찾기
		////////////////////////////
		int maxLRIndex = 0, maxRLIndex = 0;
		double maxLR = 0, maxRL = 0;
		double maxLt =0, maxRt = 0;

		//MAXLR, MAXRL 뽑기에서 제외시킬 양쪽칸수 (TRIMMING 한 후)
		int skip = 10;
		if(direction == 1) {
			//i : i부터 right(double peak 있는영역)에 포함. 0부터 시작하는 index
			//selectedRatio : 0부터 시작하는 index 기준으로 저장되어 있음.
			for(int i=1;i<sequenceLength;i++) {
				//left : 시작점 ~ 현재점-1
				double left = score(secondPeakExist,0,i-1, -1);
				//right : (현재점) ~ 끝점
				double right = score(secondPeakExist,i,sequenceLength-1, 1);
				double RL = right/left;
				if(RL > maxRL && i>=skip && i<sequenceLength-skip) {
					maxRL = RL;
					maxRLIndex = i+1; //1부터 시작하는 좌표니까 +1
					maxRt = right;
					maxLt = left;
				}
			}
		}


		else if(direction == -1) {
			//i : i까지 Left(double peak영역)에 포함, 0부터 시작하는 좌표.
			for(int i =0;i<sequenceLength;i++) {
				double left = score(secondPeakExist,0,i, -1);
				double right = score(secondPeakExist,i+1,sequenceLength-1, 1);
				double LR = left/right;
				if(LR > maxLR && i>=skip && i<sequenceLength-skip) {
					maxLR = LR;
					maxLRIndex = i+1;	//1부터 시작하는 좌표니까 +1
					maxRt = right;
					maxLt = left;
				}
				String tempString = String.format("%d, Lt:%.2f, Rt:%.2f, L/R:%.2f\n", i, left, right, LR);
				result += tempString;
				//System.out.println(tempString);
			}
		}
		String maxLRString = String.format("Index : %d/%d, MaxLt : %.2f, MaxRt : %.2f, MaxLR : %.2f\n", maxLRIndex, sequenceLength, maxLt, maxRt, maxLR);
		String maxRLString = String.format("Index : %d/%d, MaxLt : %.2f, MaxRt : %.2f, MaxRL : %.2f\n", maxRLIndex, sequenceLength, maxLt, maxRt, maxRL);

		if(direction==1) {
			if(maxRL<2.0 || maxRt<0.5) return null;

			//result = maxRLString + result;
			System.out.println(maxRLString);
			doublePeakStartIndex = maxRLIndex;
		}
		else {
			if(maxLR<2.0 || maxLt<0.5) return null;
			//result = maxLRString +result;
			System.out.println(maxLRString);
			doublePeakStartIndex = maxLRIndex;
		}

		//여기까지 왔다면 het indel 의심되는 상황
		
		
		
		
		////////////////////////////
		//Reference Seq, Subtracted Seq 생성
		////////////////////////////
		try {

			if(direction == 1) {
				subtractedSeq = new char[sequenceLength-maxRLIndex+1];
				subtractedSeq2 = new char[maxRLIndex-1];
			}
			else if (direction == -1){
				subtractedSeq = new char[maxLRIndex];
				subtractedSeq2 = new char[sequenceLength-maxLRIndex];
			}
			int subtractedSeqCounter = 0;
			int subtractedSeq2Counter = 0;
			refSeq = new char[sequenceLength];

			//i		: 0부터 시작하는 index. 아래에 mappedNo, FWDMAP, REVMAP은 모두 1부터 시작하는 index
			for (int i = 0; i < sequenceLength; i++)
			{
				int mappedNo = 0;
				Integer i_mappedNo = null;
				if(direction == FORWARD) {
					i_mappedNo = fwdMap.get(new Integer(i+1));
				}
				else {
					i_mappedNo = revMap.get(new Integer(i+1));
				}
				AlignedPoint ap = null;
				if(i_mappedNo != null) {	//alignment 안된영역 : i_mappedNo=null,  ap=null, mappedNo=0, reChar ='N', subtractedChar='N'
					mappedNo = i_mappedNo.intValue();
					ap = rootController.alignedPoints.get(mappedNo-1);
				}

				//RefSeq
				char realRefChar = 'N';		//실제 Reference파일 상의 sequence, c.f) refChar : trace상의 두 가닥 중 ref와 일치하는것.  
				if(ap != null) 
					realRefChar = ap.getRefChar();	//alignment 안된영역 : ap=null, mappedNo=0, reChar ='N', subtractedChar='N'

				//refSeq 구하기

				/*  이렇게 하면 안됨. RefSeq은 진짜 reference가 아니라 두 가닥 중에서 reference와 같은거 가 되어야 함.
				refSeq[i] = refChar;
				 */

				char subChar;

				if(originalSeq[i]=='A' || originalSeq[i] == 'T' || originalSeq[i] == 'G' || originalSeq[i] =='C') {	//single peak인 경우 refChar, subChar 모두 그 base
					refSeq[i] = originalSeq[i];
					subChar = originalSeq[i];
				}
				else {
					subChar = subtract(originalSeq[i], realRefChar);
					if(subChar != 'N') {	//origianlSeq이 2개짜리 ambiguous symbol 이면서 2개중 하나가 realRefChar일 경우
						refSeq[i] = realRefChar;
					}
					else {	//나머지의 모든 경우 refChar, subChar 모두 'N'
						refSeq[i] = 'N';
					}
				}
				

				//Hetero indel detection 대상 구간에서만 subtracted Seq 구하기
				if((direction ==1 && (i+1) >= maxRLIndex) || (direction==-1) && ((i+1) <= maxLRIndex)) {
					subtractedSeq[subtractedSeqCounter++] = subChar;
				}
				else {
					subtractedSeq2[subtractedSeq2Counter++] = subChar;
				}
			}

			/*
		Insertion인지 Deletion인지까지만 계산, 점수 계산해서 cutoff 
		정확한 calling은 또 그 다음에.
		Sliding comparison : Reference Seq (not first seq) VS subtracted Seq 
			 */
			result = "";
			if(direction == 1) {
				int max = -1;
				int maxGap = 0;
				insOrDel = -1;

				//deletion 1~maxIndelSize
				for(int gap=1; gap<=maxIndelSize; gap++) {
					int score = 0;
					for(int i=0; i<subtractedSeq.length-gap;i++) {
						//ambiguous symbol이나 'N'으로 일치하는 것은 count하지 않음. ATGC로 일치하는 것만 count
						if(subtractedSeq[i]=='A' || subtractedSeq[i] == 'T' || subtractedSeq[i] == 'G' || subtractedSeq[i] =='C')
							if(refSeq[maxRLIndex-1+i+gap] == subtractedSeq[i]) 
								score++;
					}
					result += String.format("Deletion Gap : %d, Score : %d\n", gap, score);
					if(score>max) {
						max = score;
						maxGap = gap;
					}
				}

				//insertion 1~maxIndelSize
				for(int gap=1; gap<=maxIndelSize; gap++) {
					int score = 0;
					for(int i=0; i<subtractedSeq.length-gap;i++) {

						//ambiguous symbol이나 'N'으로 일치하는 것은 count하지 않음. ATGC로 일치하는 것만 count
						if(subtractedSeq[i+gap]=='A' || subtractedSeq[i+gap] == 'T' || subtractedSeq[i+gap] == 'G' || subtractedSeq[i+gap] =='C')
							if(refSeq[maxRLIndex-1+i] == subtractedSeq[i+gap]) 
								score++;
					}
					result += String.format("Insertion Gap : %d, Score : %d\n", gap, score);
					if(score>max) {
						insOrDel = 1;
						max = score;
						maxGap = gap;
					}
				}

				//Applying Cutoff

				//분모 계산. ambiguous symbol 은 빼고 갯수 count
				int nonAmbiguousSymbolCount = 0;
				for(int i=0;i<subtractedSeq.length;i++) {
					if(subtractedSeq[i]=='A' || subtractedSeq[i] == 'T' || subtractedSeq[i] == 'G' || subtractedSeq[i] =='C')
						nonAmbiguousSymbolCount++;
				}
				double f_score = 0;

				if(nonAmbiguousSymbolCount != maxGap) //분모가 0아니면
					f_score = (double)max/(nonAmbiguousSymbolCount-maxGap);

				System.out.println(String.format("match ratio : %.2f", f_score ));
				if(f_score < indelCutoff) return null;

				result = "Gap : " + maxGap + '\n' + result;
				if(insOrDel == 1) result = "Insertion" + result;
				else result = "Deletion " + result;

				indelSize = maxGap;
			}

			else if(direction == -1) {
				int max = -1;
				int maxGap = 0;

				//deletion 1~maxIndelSize
				for(int gap=1; gap<=maxIndelSize; gap++) {
					int score = 0;
					for(int i=0; i<subtractedSeq.length-gap;i++) {
						//ambiguous symbol이나 'N'으로 일치하는 것은 count하지 않음. ATGC로 일치하는 것만 count
						if(subtractedSeq[gap+i]=='A' || subtractedSeq[gap+i] == 'T' || subtractedSeq[gap+i] == 'G' || subtractedSeq[gap+i] =='C')
							if(refSeq[i] == subtractedSeq[gap+i]) 
								score++;
					}
					result += String.format("Deletion Gap : %d, Score : %d\n", gap, score);
					if(score>max) {
						max = score;
						maxGap = gap;
					}
				}

				//insertion 1~maxIndelSize
				for(int gap=1; gap<=maxIndelSize; gap++) {
					int score = 0;
					for(int i=0; i<subtractedSeq.length-gap;i++) {

						//ambiguous symbol이나 'N'으로 일치하는 것은 count하지 않음. ATGC로 일치하는 것만 count
						if(subtractedSeq[i]=='A' || subtractedSeq[i] == 'T' || subtractedSeq[i] == 'G' || subtractedSeq[i] =='C')
							if(refSeq[i+gap] == subtractedSeq[i]) 
								score++;
					}
					result += String.format("Insertion Gap : %d, Score : %d\n", gap, score);
					if(score>max) {
						insOrDel = 1;
						max = score;
						maxGap = gap;
					}
				}
				//Applying Cutoff
				//분모 계산. ambiguous symbol 은 빼고 갯수 count
				int nonAmbiguousSymbolCount = 0;
				for(int i=0;i<subtractedSeq.length;i++) {
					if(subtractedSeq[i]=='A' || subtractedSeq[i] == 'T' || subtractedSeq[i] == 'G' || subtractedSeq[i] =='C')
						nonAmbiguousSymbolCount++;
				}
				double f_score = 0;
				if(nonAmbiguousSymbolCount != maxGap) //분모가 0아니면
					f_score = (double)max/(nonAmbiguousSymbolCount-maxGap);

				System.out.println(String.format("match ratio : %.2f", f_score ));
				if(f_score < indelCutoff) return null;

				result = "Gap : " + maxGap + '\n' + result;
				if(insOrDel == 1) result = "Insertion " + result;
				else result = "Deletion" + result;
				indelSize = maxGap;
			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
			return null;
		}

		
		//정확한 Indel calling
		
		TreeMap<Integer, Integer> coordiMap = null;
		String s_refSeq = new String(refSeq);
		String s_subtractedSeq = new String(subtractedSeq);
		String indelSeq = "";
		int type = -1;
		if(direction==1) {
			coordiMap = rootController.formatter.fwdCoordinateMap;
		}
		else {
			coordiMap = rootController.formatter.revCoordinateMap;
		}
		try {
			int refSeqIndex=0; //0부터 시작
			int subSeqIndex=0; //0부터 시작
			int indelStartIndex=0, indelEndIndex=0;	//1부터 시작

			if(insOrDel == -1) {
				if(direction==1) {
					//아래와 같이 정해지는이유 : 그림 참조. 모두 1부터 시작하는 index임.
					indelStartIndex = doublePeakStartIndex; 
					indelEndIndex = indelStartIndex + indelSize - 1; 

					if(indelEndIndex > s_refSeq.length()) return null;

					//delins detection (기본 : del)
					for(refSeqIndex=indelStartIndex-1; refSeqIndex+indelSize < refSeq.length && subSeqIndex < subtractedSeq.length && indelEndIndex <= s_refSeq.length(); ) {
						if(refSeq[refSeqIndex+indelSize] == subtractedSeq[subSeqIndex]) break;
						indelSeq += subtractedSeq[subSeqIndex];
						refSeqIndex++;	
						subSeqIndex++;  
						indelEndIndex++;
					}
					if(refSeqIndex==indelStartIndex-1) {
						type = Indel.deletion;
						indelSeq = s_refSeq.substring(doublePeakStartIndex-1, doublePeakStartIndex-1+indelSize);
					}
					else type = Indel.delins;

				}
				else if(direction == -1) {
					//아래와 같이 정해지는이유 : 그림 참조. 모두 1부터 시작하는 index임.
					indelStartIndex = doublePeakStartIndex-indelSize+1; 
					indelEndIndex = doublePeakStartIndex; 
					if(indelStartIndex<0) return null;

					//delins detection (기본 : del)
					subSeqIndex = s_subtractedSeq.length()-1;
					for(refSeqIndex=doublePeakStartIndex-1-indelSize; refSeqIndex>=0 && subSeqIndex>=0 && indelStartIndex>=1; ) {
						if(refSeq[refSeqIndex] == subtractedSeq[subSeqIndex]) break;
						indelSeq = subtractedSeq[subSeqIndex] + indelSeq;
						refSeqIndex--;	
						subSeqIndex--;  
						indelStartIndex--;
					}
					if(refSeqIndex==doublePeakStartIndex-1-indelSize) {
						type = Indel.deletion;
						indelSeq = s_refSeq.substring(doublePeakStartIndex-indelSize, doublePeakStartIndex);
					}
					else type = Indel.delins;
				}
			}

			else if(insOrDel == 1) {
				boolean duplication = true;

				if (direction == 1) { 
					//duplication 가정 시 start, end index
					//아래와 같이 정해지는이유 : 그림 참조.
					indelStartIndex = doublePeakStartIndex-indelSize;
					indelEndIndex = doublePeakStartIndex - 1;
					if(indelStartIndex<1) return null;

					for(subSeqIndex=0;subSeqIndex<indelSize;subSeqIndex++) {
						if(subtractedSeq[subSeqIndex] != refSeq[indelStartIndex-1+subSeqIndex]) {
							duplication = false;
							break;
						}
					}
					indelSeq = s_subtractedSeq.substring(0, indelSize);
					
					if(duplication == true) {
						type = Indel.duplication;
					}
					else {
						//duplication 아님. insertion 또는 delins
						//insertion이라고 가정하면 아래와 같이 index 변경
						indelStartIndex = doublePeakStartIndex-1;
						indelEndIndex = doublePeakStartIndex;

						//delins detection
						subSeqIndex = indelSize;

						for(refSeqIndex=doublePeakStartIndex-1; 
								refSeqIndex<refSeq.length && subSeqIndex<subtractedSeq.length && indelEndIndex<=refSeq.length;) {
							if(refSeq[refSeqIndex] == subtractedSeq[subSeqIndex]) break;
							indelSeq += subtractedSeq[subSeqIndex];
							refSeqIndex++;	
							subSeqIndex++;  
							indelEndIndex++;
						}
						if(refSeqIndex==doublePeakStartIndex-1) type = Indel.insertion;
						else {
							type = Indel.delins;
							//아래와 같이 하는 이유 : insertion일때와 delins일때 좌표 붙이는 기준이 다름.
							//insertion 일때 : insertion 되는 서열 양쪽좌표, delins일때 : deletion 일때처럼 deletion 되는애들 맨처음과 맨끝
							indelStartIndex++;
							indelEndIndex--;
						}
					}
				}

				else if(direction == -1) {
					//duplication 가정 시 start, end index
					//아래와 같이 정해지는이유 : 그림 참조.
					indelStartIndex = doublePeakStartIndex+1; 
					indelEndIndex = indelStartIndex + indelSize - 1; 

					if(indelEndIndex > s_refSeq.length()) return null;

					for(int i=0;i<indelSize;i++) {
						if(subtractedSeq[subtractedSeq.length-indelSize+i] != refSeq[indelStartIndex-1+i]) {
							duplication = false;
							break;
						}
					}
					indelSeq = s_subtractedSeq.substring(s_subtractedSeq.length()-indelSize, s_subtractedSeq.length());
					
					if(duplication == true) {
						type = Indel.duplication;
					}
					else {
						//duplication 아님. insertion 또는 delins
						//insertion이라고 가정하면 아래와 같이 index 변경
						indelStartIndex = doublePeakStartIndex;
						indelEndIndex = indelStartIndex + 1;
					
						//delins detection
						subSeqIndex = s_subtractedSeq.length()-1-indelSize;
						for(refSeqIndex=doublePeakStartIndex-1; refSeqIndex>=0 && subSeqIndex>=0 && indelStartIndex >=1; ) {

							if(refSeq[refSeqIndex] == subtractedSeq[subSeqIndex]) break;
							indelSeq = subtractedSeq[subSeqIndex] + indelSeq;
							refSeqIndex--;	
							subSeqIndex--;  
							indelStartIndex--;
						}
						if(refSeqIndex==doublePeakStartIndex-1) type = Indel.insertion;
						else {
							type = Indel.delins;
							//아래와 같이 하는 이유 : insertion일때와 delins일때 좌표 붙이는 기준이 다름.
							//insertion 일때 : insertion 되는 서열 양쪽좌표, delins일때 : deletion 일때처럼 deletion 되는애들 맨처음과 맨끝
							indelStartIndex++;
							indelEndIndex--;
						}
					}
				}
			}


			//Align 안된영역에서 indelStartindex, indelEndindex, doublePeakSTartIndex 나오면 return null;
			int maxIndex = Integer.max(Integer.max(indelStartIndex, indelEndIndex), doublePeakStartIndex);
			int minIndex = Integer.min(Integer.min(indelStartIndex, indelEndIndex), doublePeakStartIndex);
			
			if(maxIndex > alignedRegionEnd || minIndex < alignedRegionStart) return null;
			
			alignedIndelStartIndex = coordiMap.get(indelStartIndex);
			alignedIndelEndIndex = coordiMap.get(indelEndIndex);
			alignedDoublePeakStartIndex = coordiMap.get(doublePeakStartIndex);


			if (direction ==1)
				//variant = new Indel(direction, Indel.hetero, type, coordi1, coordi2, s_seq, alignedDoublePeakStartIndex, ap3.getFwdTraceIndex(), ap3.getRevTraceIndex(), ap3.getFwdChar(), Formatter.gapChar, ap1.getGIndex(), ap2.getGIndex(), ap1.isCoding(), ap2.isCoding(), true);
				variant = new Indel(rootController, "hetero", direction, type, alignedIndelStartIndex, alignedIndelEndIndex, alignedDoublePeakStartIndex, indelSeq, true);
			else if (direction == -1)
				//variant = new Indel(direction, Indel.hetero, type, coordi1, coordi2, s_seq, alignedDoublePeakStartIndex, ap3.getFwdTraceIndex(), ap3.getRevTraceIndex(), Formatter.gapChar, ap3.getRevChar(), ap1.getGIndex(), ap2.getGIndex(), ap1.isCoding(), ap2.isCoding(), true);
				variant = new Indel(rootController, "hetero", direction, type, alignedIndelStartIndex, alignedIndelEndIndex, alignedDoublePeakStartIndex, indelSeq, true);

			//variant.setHitCount(2);
		}
		catch(Exception ex) {
			ex.printStackTrace();
			return null;
		}

		return variant;
	}

	/**
	 * @param selectedPosition
	 * @return
	 */


	public BufferedImage getHeteroImage(Formatter formatter, TreeSet<Integer> highlightRefSeq, TreeSet<Integer> highlightSubSeq) {
		BufferedImage image = new BufferedImage(traceLength*traceWidth, traceHeight+50, BufferedImage.TYPE_BYTE_INDEXED);
		Graphics2D g = image.createGraphics();
		g.setBackground(Color.WHITE);
		g.clearRect(0, 0, traceLength*traceWidth, traceHeight+50);

		TreeMap<Integer, Integer> fwdMap = formatter.fwdCoordinateMap;
		TreeMap<Integer, Integer> revMap = formatter.revCoordinateMap;

		for(int i=0;i<traceLength-1;i++) {
			g.setColor(greenColor);
			g.drawLine (i*traceWidth,transformedA[i], (i+1) * traceWidth, transformedA[i+1]);

			g.setColor(Color.RED);
			g.drawLine (i*traceWidth,transformedT[i], (i+1) * traceWidth, transformedT[i+1]);

			g.setColor(Color.BLACK);
			g.drawLine (i*traceWidth,transformedG[i], (i+1) * traceWidth, transformedG[i+1]);

			g.setColor(Color.BLUE);
			g.drawLine (i*traceWidth,transformedC[i], (i+1) * traceWidth, transformedC[i+1]);
		}

		int subtractedSeqCounter = 0, subtractedSeqCounter2 = 0;
		for(int i=0;i<sequenceLength;i++) {
			int mappedNo = 0;
			Integer i_mappedNo = null;
			if(direction == FORWARD) {
				i_mappedNo = fwdMap.get(new Integer(i+1));
			}
			else {
				i_mappedNo = revMap.get(new Integer(i+1));
			}
			AlignedPoint ap = null;
			if(i_mappedNo != null) {	//alignment 안된영역 : i_mappedNo=null,  ap=null, mappedNo=0, reChar ='N', subtractedChar='N'
				mappedNo = i_mappedNo.intValue();
				ap = rootController.alignedPoints.get(mappedNo-1);
			}
			char refChar = refSeq[i];
			

			int xPos = baseCalls[i]*traceWidth;
			switch(refChar) {
			case 'A' : 
				g.setColor(greenColor);
				break;
			case 'T' : 
				g.setColor(Color.RED);
				break;
			case 'G' : 
				g.setColor(Color.BLACK);
				break;
			case 'C' : 
				g.setColor(Color.BLUE);
				break;
			default : 
				g.setColor(Color.PINK);
			}

			g.drawString(Character.toString(refChar), Integer.max(0, xPos-3), traceHeight+13);
			g.setColor(Color.BLACK);

			//두번째 base 쓰기. Hetero indel detection 대상 구간에서는 subtractedSeq
			if((direction ==1 && (i+1) >= doublePeakStartIndex) || (direction==-1) && ((i+1) <= doublePeakStartIndex)) {
				//Subtracted Seq (-51)
				char subtractedChar = subtractedSeq[subtractedSeqCounter++];
				switch(subtractedChar) {
				case 'A' : 
					g.setColor(greenColor);
					break;
				case 'T' : 
					g.setColor(Color.RED);
					break;
				case 'G' : 
					g.setColor(Color.BLACK);
					break;
				case 'C' : 
					g.setColor(Color.BLUE);
					break;
				default : 
					g.setColor(Color.PINK);
				}
				g.drawString(Character.toString(subtractedChar), Integer.max(0, xPos-3), traceHeight+23);
				g.setColor(Color.black);
			}
			//두번째 base 쓰기. Hetero indel detection 대상 구간 밖에서는 subtractedSeq2
			else {
				char subtractedChar = subtractedSeq2[subtractedSeqCounter2++];
				switch(subtractedChar) {
				case 'A' : 
					g.setColor(greenColor);
					break;
				case 'T' : 
					g.setColor(Color.RED);
					break;
				case 'G' : 
					g.setColor(Color.BLACK);
					break;
				case 'C' : 
					g.setColor(Color.BLUE);
					break;
				default : 
					g.setColor(Color.PINK);
				}
				g.drawString(Character.toString(subtractedChar), Integer.max(0, xPos-3), traceHeight+23);
				g.setColor(Color.black);
			}

			//좌표 표기.
			if(mappedNo%10 ==1) {
				g.drawLine(xPos, traceHeight+23, xPos, traceHeight+28);
				//alignment 후 좌표 표기.
				if(mappedNo!=0) 
					g.drawString(Integer.toString(mappedNo), Integer.max(0, xPos-3), traceHeight+38);

				//cDNA 좌표 표기.
				if(mappedNo!=0) 
					g.drawString(ap.getStringCIndex(), Integer.max(0, xPos-3), traceHeight+48);
			}
			g.setColor(Color.black);
		}


		//선택된 부분 색칠: RefSeq
		if(!highlightRefSeq.isEmpty()) {
			int refSeqStart = highlightRefSeq.first();
			int refSeqEnd = highlightRefSeq.last();

			int xPos1 = baseCalls[refSeqStart-1]*traceWidth;
			int xPos2 = baseCalls[refSeqEnd-1]*traceWidth;

			g.setColor(new Color(0, 191, 255));
			g.setComposite(AlphaComposite.SrcOver.derive(0.4f));
			g.fillRect(Integer.max(0, xPos1-6*traceWidth), traceHeight+3, (xPos2-xPos1) + 12*GanseqTrace.traceWidth, 10);
			g.setColor(Color.BLACK);
		}

		//선택된 부분 색칠: SubSeq
		if(!highlightSubSeq.isEmpty()) {
			int subSeqStart = highlightSubSeq.first();
			int subSeqEnd = highlightSubSeq.last();

			int xPos1 = baseCalls[subSeqStart-1]*traceWidth;
			int xPos2 = baseCalls[subSeqEnd-1]*traceWidth;

			g.setColor(new Color(0, 191, 255));
			g.fillRect(Integer.max(0, xPos1-6*traceWidth), traceHeight+13, (xPos2-xPos1) + 12*GanseqTrace.traceWidth, 10);
			g.setColor(Color.BLACK);
			g.setComposite(AlphaComposite.SrcOver);
		}

		return image;
	}

	/** returns average value of an input array
	 * 
	 * @param array : target array
	 * @param startIndex : start index
	 * @param endIndex : end index
	 */
	protected double score (boolean[] secondPeakExist, int startIndex, int endIndex, int direction) {
		double ret = -1;
		double offset = 0.2;
		if(startIndex > endIndex) return ret;
		int sum = 0;
		int denominator = 0;

		int weightCounter = 10;
		if(direction == 1) {
			for(int i=startIndex;i<=endIndex;i++) {
				denominator += weightCounter;
				if(secondPeakExist[i]) {
					sum += weightCounter;
				}
				if(weightCounter>1) weightCounter--;
			}
		}
		else if(direction == -1) {
			for(int i=endIndex;i>=startIndex;i--) {
				denominator += weightCounter;
				if(secondPeakExist[i]) {
					sum += weightCounter;
				}
				if(weightCounter>1) weightCounter--;
			}
		}
		ret = (double)sum / denominator;
		ret += offset;
		return ret;
	}


	/**
	 * Getters and setters for member variables 
	 */

	public int getInsOrDel() {
		return insOrDel;
	}
	public void setInsOrDel(int insOrDel) {
		this.insOrDel = insOrDel;
	}
	public int getIndelSize() {
		return indelSize;
	}
	public void setIndelSize(int indelSize) {
		this.indelSize = indelSize;
	}
	public char[] getSubtractedSeq() {
		return subtractedSeq;
	}
	public char[] getSubtractedSeq2() {
		return subtractedSeq2;
	}

	public void setSubtractedSeq(char[] subtractedSeq) {
		this.subtractedSeq = subtractedSeq;
	}
	public int getDoublePeakStartIndex() {
		return doublePeakStartIndex;
	}
	public char[] getRefSeq() {
		return refSeq;
	}
	public void setRefSeq(char[] refSeq) {
		this.refSeq = refSeq;
	}
	public int getAlignedDoublePeakStartIndex() {
		return alignedDoublePeakStartIndex;
	}
	public int getAlignedIndelStartIndex() {
		return alignedIndelStartIndex;
	}
	public int getAlignedIndelEndIndex() {
		return alignedIndelEndIndex;
	}



}
