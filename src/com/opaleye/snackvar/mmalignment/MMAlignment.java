/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.opaleye.snackvar.mmalignment;

import java.io.File;

import com.opaleye.snackvar.GanseqTrace;
import com.opaleye.snackvar.RootController;
import com.opaleye.snackvar.reference.Reference;

public class MMAlignment {

	private double gapOpenPenalty;
	private static final double h = 0.5;
	private static final double minusLimit = -100;
	
	public MMAlignment(double gapOpenPenalty) {
		this.gapOpenPenalty = gapOpenPenalty;
		matrix = (new SubstitutionMatrix()).getMatrix();
	}
	
	private short indexOfBase(char c) {
		switch(c) {
		case 'A' : return 0;
		case 'T' : return 1;
		case 'G' : return 2; 
		case 'C' : return 3;
		case 'S' : return 4;
		case 'W' : return 5;
		case 'R' : return 6;
		case 'Y' : return 7;
		case 'K' : return 8;
		case 'M' : return 9;
		case 'B' : return 10;
		case 'V' : return 11;
		case 'H' : return 12;
		case 'D' : return 13;
		default : return 14;
		}
	}

	private short[] getIntArray(char[] charArray) {
		short[] ret = new short[charArray.length];
		for(int i=0;i<charArray.length;i++) 
			ret[i] = indexOfBase(charArray[i]);
		return ret;
	}


	private double[][] matrix = null;

	private double[] CC, DD, RR, SS;
	private int maxI = 0, maxJ = 0;
	private double maxC = 0;



	private double gap(int n) {
		if(n==0) return 0;
		else return gapOpenPenalty+(double)n*h;
	}

	private String getReverse(String s) {
		StringBuffer sb = new StringBuffer();
		int length = s.length();
		for(int i=0;i<length;i++) {
			sb.append(s.charAt(length-i-1));
		}
		return sb.toString();
	}

	public void firstScan (char[] s1, char[] s2) {
		short[] intArray1 = getIntArray (s1);
		short[] intArray2 = getIntArray (s2);

		maxI = 0; maxJ = 0; maxC = 0;

		int M = s1.length;
		int N = s2.length;

		CC = new double[N+1];
		DD = new double[N+1];

		double e=0, c=0, s=0;

		DD[0] = minusLimit;
		CC[0] = 0;


		for(int j=1;j<=N;j++) {
			CC[j] = 0;
			DD[j] = minusLimit;
		}

		for(int i=1;i<=M;i++) {
			//s : 대각선 왼쪽 위에꺼 (이때까지 CC는 윗줄)
			s = CC[0];

			//c : 한칸 왼쪽
			c = 0;

			//e: 현재 I값
			e = minusLimit;


			for(int j=1;j<=N;j++) {
				if(e>=c-gapOpenPenalty) e = e-h;
				else e = c-gapOpenPenalty-h;

				if(DD[j] >= CC[j]-gapOpenPenalty) DD[j] = DD[j]-h;
				else DD[j] = CC[j]-gapOpenPenalty-h;

				if(DD[j] >= e) c = DD[j];
				else c = e;

				double newC;
				//newC = s+weight(s1[i-1], s2[j-1], false);
				newC = s+matrix[intArray1[i-1]][intArray2[j-1]];

				if(c <= newC) c= newC;
				if(c<0) c = 0; 
				if(c>=maxC) {
					maxI = i;
					maxJ = j;
					maxC = c;
				}
				s = CC[j];
				CC[j] = c;
			}
		}
	}

	public void computeCCDD(char[] s1, char[] s2, double tbte ) {
		int M = s1.length;
		int N = s2.length;
		
		short[] intArray1 = getIntArray (s1);
		short[] intArray2 = getIntArray (s2);
				
		double e=0, c=0, s=0, t=0;

		CC[0] = 0;
		t = gapOpenPenalty;

		for(int j=1;j<=N;j++) {
			t = t+h;
			CC[j] = t;
			DD[j] = t+gapOpenPenalty;
		}

		t=tbte;

		for(int i=1;i<=M;i++) {
			s = CC[0];
			t = t + h;
			c =  t;
			CC[0] = c;
			e = t+gapOpenPenalty;
			for(int j=1;j<=N;j++) {
				if(e<c+gapOpenPenalty) e = e+h;
				else e = c+gapOpenPenalty+h;
				
				if(DD[j] < CC[j]+gapOpenPenalty) DD[j] = DD[j]+h;
				else DD[j] = CC[j]+gapOpenPenalty+h;
				
				if(DD[j] < e) c = DD[j];
				else c = e;

				double newC;
				newC = s-matrix[intArray1[i-1]][intArray2[j-1]];
				if(newC < c) 
					c = newC;

				s = CC[j];
				CC[j] = c;
			}
		}
		DD[0] = CC[0];
	}


	public void computeRRSS(char[] s1, char[] s2, double tbte ) {
		int M = s1.length;
		int N = s2.length;
		
		short[] intArray1 = getIntArray (s1);
		short[] intArray2 = getIntArray (s2);
				
		double e=0, c=0, s=0, t=0;

		RR[0] = 0;
		t = gapOpenPenalty;

		for(int j=1;j<=N;j++) {
			t = t+h;
			RR[j] = t;
			SS[j] = t+gapOpenPenalty;
		}

		t=tbte;

		for(int i=1;i<=M;i++) {
			s = RR[0];
			t = t + h;
			c =  t;
			RR[0] = c;
			e = t+gapOpenPenalty;
			for(int j=1;j<=N;j++) {
				if(e<c+gapOpenPenalty) e = e+h;
				else e = c+gapOpenPenalty+h;
				
				if(SS[j] < RR[j]+gapOpenPenalty) SS[j] = SS[j]+h;
				else SS[j] = RR[j]+gapOpenPenalty+h;
				
				if(SS[j] < e) c = SS[j];
				else c = e;

				double newC;
				newC = s-matrix[intArray1[i-1]][intArray2[j-1]];
				if(newC < c) 
					c = newC;

				s = RR[j];
				RR[j] = c;
			}
		}
		SS[0] = RR[0];
	}

	private AlignedPair diff(String A, String B, double tb, double te) {
		String alignedString1 = "";
		String alignedString2 = "";
		AlignedPair ret = null;
		int M = A.length();
		int N = B.length();
		if(N==0) {
			if(M>0) {
				alignedString1 = A;
				for(int i=0;i<A.length();i++) {
					alignedString2 += "-";
				}
				ret = new AlignedPair(alignedString1, alignedString2);
				return ret;
			}
			else return new AlignedPair("", "");
		}
		else if(M==0) {
			alignedString2 = B;
			for(int i=0;i<B.length();i++) {
				alignedString1 += "-";
			}
			ret = new AlignedPair(alignedString1, alignedString2);
			return ret;
		}
		else if(M==1) {
			double cost1 = Double.min(tb, te) + h + gap(N);
			double cost2 = Double.MAX_VALUE;
			int cost2Index = 0;

			for(int j=1;j<=N;j++) {
				double tempCost = gap(j-1) - matrix[indexOfBase(A.charAt(0))][indexOfBase(B.charAt(j-1))] + gap(N-j);
				if(tempCost < cost2) {
					cost2 = tempCost;
					cost2Index = j;
				}
			}
			if(cost1 < cost2) {
				if(tb == 0) {	//deletion A -> insertion B
					alignedString1 = A;
					for(int i=0;i<B.length();i++) {
						alignedString1 += "-";
					}
					alignedString2 = "-"+B;
				}
				else if(te == 0) { //insertion B -> deletion A
					for(int i=0;i<B.length();i++) {
						alignedString1 += "-";
					}
					alignedString1 += A;

					alignedString2 = B + "-";
				}
				else {		//아무렇게나 해도 되지만 deletion A -> insertion B
					alignedString1 = A;
					for(int i=0;i<B.length();i++) {
						alignedString1 += "-";
					}
					alignedString2 = "-"+B;
				}
			}
			else {
				alignedString2 = B;
				for(int i=1;i<cost2Index;i++) {
					alignedString1 += "-";
				}
				alignedString1 += A;
				for(int i=cost2Index+1;i<=B.length();i++) {
					alignedString1 += "-";
				}
			}
			ret = new AlignedPair(alignedString1, alignedString2);
			return ret;
		}

		else {
			int I = M/2;
			String AI = A.substring(0,I);
			String AT = A.substring(I, A.length());
			String ATrev = getReverse(AT);
			String Brev = getReverse(B);

			computeCCDD(AI.toCharArray(), B.toCharArray(), tb);
			computeRRSS(ATrev.toCharArray(), Brev.toCharArray(), te);

			int type = 0;
			int minType = 0;
			int minJ = 0;
			double minValue = Double.MAX_VALUE;

			for(int j=0;j<=N;j++) {
				double value = 0;
				double CR = CC[j] + RR[N-j];
				double DS = DD[j] + SS[N-j] - gapOpenPenalty;
				if(DS < CR) {
					value = DS;
					type = 2;
				}
				else {
					value = CR;
					type = 1;
				}
				if(value < minValue) {
					minValue = value;
					minJ = j;
					minType = type;
				}
			}

			String BJ = B.substring(0,minJ);
			String BT = B.substring(minJ, B.length());


			if(minType == 1) {
				return diff(AI, BJ, tb, gapOpenPenalty).addRight(diff(AT, BT, gapOpenPenalty, te));
			}
			else {
				AlignedPair middle = new AlignedPair(A.substring(I-1, I+1), "-" + "-");
				return diff(AI.substring(0,AI.length()-1), BJ, tb, 0).addRight(middle).addRight(diff(AT.substring(1,AT.length()), BT, 0, te));
			}
		}
	}

	public AlignedPair globalAlignment(String inputString1, String inputString2) {
		inputString1 = inputString1.toUpperCase();
		inputString2 = inputString2.toUpperCase();

		CC = new double[inputString2.length()+1];
		DD = new double[inputString2.length()+1];
		RR = new double[inputString2.length()+1];
		SS = new double[inputString2.length()+1];
		return diff(inputString1, inputString2, gapOpenPenalty, gapOpenPenalty);
	}

	public AlignedPair localAlignment(String s1, String s2) {
		int start1 = 0, start2 = 0;
		//long timeStamp = System.currentTimeMillis();

		s1 = s1.toUpperCase();
		s2 = s2.toUpperCase();

		firstScan(s1.toCharArray(),  s2.toCharArray());
		//System.out.println("time1 : " + (System.currentTimeMillis() - timeStamp));

		s1 = s1.substring(0,maxI);
		s2 = s2.substring(0,maxJ);

		String revS1 = getReverse(s1);
		String revS2 = getReverse(s2);
		//System.out.println("time2 : " + (System.currentTimeMillis() - timeStamp));

		firstScan(revS1.toCharArray(), revS2.toCharArray());
		//System.out.println("time3 : " + (System.currentTimeMillis() - timeStamp));
		start1 = revS1.length() - maxI;
		start2 = revS2.length() - maxJ;

		
		revS1 = revS1.substring(0,maxI);
		revS2 = revS2.substring(0,maxJ);
		
		
		System.out.println("start1 : " + start1 + ", start2 : " + start2);

		s1 = getReverse(revS1);
		s2 = getReverse(revS2);

		AlignedPair ret = globalAlignment(s1, s2);
		ret.setStart1(start1);
		ret.setStart2(start2);
		//System.out.println("time4 : " + (System.currentTimeMillis() - timeStamp));
		return ret;
	}

	public static void main(String[] args) {
		/*
		MMAlignment mma = new MMAlignment(30);
		File tempFile = null;
		File abiFile = null;

		Reference refFile = null;
		GanseqTrace tempTrace = null;
		String s1 = null, s2 = null;;
		try {
			//tempFile = new File("F:\\GoogleDrive\\ganseq\\Release\\test data\\kit.gb");
			//abiFile = new File("F:\\GoogleDrive\\ganseq\\Release\\test data\\C-KIT_17F.ab1");
			//tempFile = new File("F:\\GoogleDrive\\ganseq\\Release\\test data\\npm1.gb");
			//abiFile = new File("F:\\GoogleDrive\\ganseq\\Release\\test data\\NPM1_R_F04_16.ab1");

			//tempFile = new File("F:\\GoogleDrive\\ganseq\\data\\2009 SMC Lecture\\PRKN(PARK2).gb");
			//abiFile = new File("F:\\GoogleDrive\\ganseq\\data\\2009 SMC Lecture\\Case 13 PARK2\\PARK2_Sample_01F.ab1");

			tempFile = new File("F:\\GoogleDrive\\ganseq\\data\\안암병원 NGS validation\\BRCA1.gb");
			abiFile = new File("F:\\GoogleDrive\\ganseq\\data\\안암병원 NGS validation\\2018.11.22 0_FW_ Re_sanger confrim_181122\\NLA210676-BRCA1-E12F-F1_A12_01_FastSeq50_POP7_Z.ab1");

			refFile = new Reference(tempFile, Reference.GenBank);
			tempTrace = new GanseqTrace(abiFile, );
			s1 = refFile.getRefString();
			s2 = tempTrace.getSequence();
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		AlignedPair ap = mma.localAlignment(s1, s2);
		ap.printStrings();
*/

		MMAlignment mma = new MMAlignment(30);
		String a = "AATTTTAATTAAATGCATGCATGC";
		String b = "AATTGCA";
		AlignedPair ap = mma.localAlignment(a,b);
		ap.printStrings();
		System.out.println("length1 : " + ap.getAlignedString1().length());
		System.out.println("length2 : " + ap.getAlignedString2().length());
		
		
	}

	
	public void setGapOpenPenalty(double gapOpenPenalty) {
		this.gapOpenPenalty = gapOpenPenalty;
	}
	
	
}
