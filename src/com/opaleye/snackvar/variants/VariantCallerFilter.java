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

package com.opaleye.snackvar.variants;

import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import com.opaleye.snackvar.AlignedPoint;
import com.opaleye.snackvar.EquivExpression;
import com.opaleye.snackvar.Formatter;
import com.opaleye.snackvar.GanseqTrace;
import com.opaleye.snackvar.RootController;
import com.opaleye.snackvar.TwoPeaks;
import com.opaleye.snackvar.settings.SettingsController;
import com.opaleye.snackvar.tools.SymbolTools;

public class VariantCallerFilter {
	private boolean fwdLoaded, revLoaded;
	private int startRange, endRange;
	private Vector<Variant> heteroIndelList;
	private GanseqTrace trimmedFwdTrace, trimmedRevTrace;
	private Vector<AlignedPoint> alignedPoints;
	private RootController rootController;
	
	public VariantCallerFilter(RootController rootController, Vector<Variant> heteroIndelList) {
		this.rootController = rootController;
		this.heteroIndelList = heteroIndelList;
		this.fwdLoaded = rootController.fwdLoaded;
		this.revLoaded = rootController.revLoaded;
		this.startRange = rootController.startRange;
		this.endRange = rootController.endRange;
		this.trimmedFwdTrace = rootController.trimmedFwdTrace;
		this.trimmedRevTrace = rootController.trimmedRevTrace; 
		this.alignedPoints = rootController.alignedPoints;
		
	}

	public TreeSet<Variant> getVariantList() {
		TreeSet<Variant> ret = makeVariantList();

		ret = comparisonFilter(ret);
		ret = compressedPeakFilter(ret);

		try {
			ret = makeHomoDelins(ret);
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		
		
		return ret;
	}

	private TreeSet<Variant> compressedPeakFilter(TreeSet<Variant> variantList) {
		TreeSet<Variant> ret = new TreeSet<Variant>();
		Vector<String> filteredHGVSList = new Vector<String>();
		if(fwdLoaded) {
			for(int i=0;i<trimmedFwdTrace.getSequenceLength();i++) {
				try {	//에러나면 continue (filtering 안하면 됨)
					Vector<String> IUPACList = SymbolTools.IUPACtoSymbolList(trimmedFwdTrace.getSequence().charAt(i));
					if(IUPACList.size() != 2) continue;
					char refBase, nextBase;
					char base1 = IUPACList.get(0).charAt(0);
					char base2 = IUPACList.get(1).charAt(0);

					int index = rootController.formatter.fwdCoordinateMap.get(new Integer(i+1));
					AlignedPoint point = alignedPoints.get(index-1);
					if(base1 == point.getRefChar()) {
						refBase = base1;
						nextBase = base2;
					}
					else if(base2 == point.getRefChar()) {
						refBase = base2;
						nextBase = base1;
					}
					else
						continue;

					int[] secondTrace = null;
					switch(nextBase) {
					case 'A': secondTrace = trimmedFwdTrace.getTraceA();break;
					case 'T': secondTrace = trimmedFwdTrace.getTraceT();break;
					case 'G': secondTrace = trimmedFwdTrace.getTraceG();break;
					case 'C': secondTrace = trimmedFwdTrace.getTraceC();break;
					}

					int initialPos = trimmedFwdTrace.getBaseCalls()[i];
					int direction;

					if(secondTrace[initialPos-1] < secondTrace[initialPos] && secondTrace[initialPos] < secondTrace[initialPos+1])
						direction = 1;
					else if(secondTrace[initialPos-1] > secondTrace[initialPos] && secondTrace[initialPos] > secondTrace[initialPos+1])
						direction = -1;
					else
						continue;	//peak 위치 같으면 굳이 두개의 peak으로 구분할 필요가... 없을테니. 확실한 것만 filtering

					//System.out.println((String.format("position : %d, direction : %d",  i, direction)));
					//System.out.println((String.format("commpressed base : %c, refBase : %c, nextBase : %c", trimmedFwdTrace.getSequence().charAt(i), refBase, nextBase)));


					for(int index2=index+direction;;index2+=direction) {
						AlignedPoint point2 = alignedPoints.get(index2-1);

						char refBase2 = point2.getRefChar();
						//System.out.println(String.format("index2 : %d, refBase2 : %c", index2, refBase2));
						if(refBase2 != nextBase) break;
						if(refBase2 == nextBase && point2.getFwdChar()==Formatter.gapChar) {
							System.out.println(String.format("Compressed peak found at %d, %c->%c+%c\n", i,  trimmedFwdTrace.getSequence().charAt(i), refBase, nextBase));

							String removedSNV = point.getStringCIndex() + refBase + ">" + nextBase ;
							String removedDeletion = point2.getStringCIndex() + "del" + nextBase;

							filteredHGVSList.add(removedSNV);
							filteredHGVSList.add(removedDeletion);

							System.out.println("filtered from compressed peak : " + removedSNV);
							System.out.println("filtered from compressed peak : " + removedDeletion);
						}
					}
				}
				catch(ArrayIndexOutOfBoundsException ae) { // 맨끝에서 ambiguous symbol 있으면 ArrayIndexOutOfBoundsException 나게 되어있엄. 그냥 pass
					continue;
				}
				catch(Exception e) {
					//e.printStackTrace();
					continue;
				}
			}
		}

		if(revLoaded) {
			for(int i=0;i<trimmedRevTrace.getSequenceLength();i++) {
				try {	//에러나면 continue (filtering 안하면 됨)
					Vector<String> IUPACList = SymbolTools.IUPACtoSymbolList(trimmedRevTrace.getSequence().charAt(i));
					if(IUPACList.size() != 2) continue;
					char refBase, nextBase;
					char base1 = IUPACList.get(0).charAt(0);
					char base2 = IUPACList.get(1).charAt(0);

					int index = rootController.formatter.revCoordinateMap.get(new Integer(i+1));
					AlignedPoint point = alignedPoints.get(index-1);
					if(base1 == point.getRefChar()) {
						refBase = base1;
						nextBase = base2;
					}
					else if(base2 == point.getRefChar()) {
						refBase = base2;
						nextBase = base1;
					}
					else
						continue;

					int[] secondTrace = null;
					switch(nextBase) {
					case 'A': secondTrace = trimmedRevTrace.getTraceA();break;
					case 'T': secondTrace = trimmedRevTrace.getTraceT();break;
					case 'G': secondTrace = trimmedRevTrace.getTraceG();break;
					case 'C': secondTrace = trimmedRevTrace.getTraceC();break;
					}

					int initialPos = trimmedRevTrace.getBaseCalls()[i];
					int direction;

					if(secondTrace[initialPos-1] < secondTrace[initialPos] && secondTrace[initialPos] < secondTrace[initialPos+1])
						direction = 1;
					else if(secondTrace[initialPos-1] > secondTrace[initialPos] && secondTrace[initialPos] > secondTrace[initialPos+1])
						direction = -1;
					else
						continue;	//peak 위치 같으면 굳이 두개의 peak으로 구분할 필요가... 없을테니. 확실한 것만 filtering

					//System.out.println((String.format("position : %d, direction : %d",  i, direction)));
					//System.out.println((String.format("commpressed base : %c, refBase : %c, nextBase : %c", trimmedRevTrace.getSequence().charAt(i), refBase, nextBase)));


					for(int index2=index+direction;;index2+=direction) {
						AlignedPoint point2 = alignedPoints.get(index2-1);

						char refBase2 = point2.getRefChar();
						System.out.println(String.format("index2 : %d, refBase2 : %c", index2, refBase2));
						if(refBase2 != nextBase) break;
						if(refBase2 == nextBase && point2.getRevChar()==Formatter.gapChar) {
							//System.out.println(String.format("Compressed peak found at %d, %c->%c+%c\n", i,  trimmedRevTrace.getSequence().charAt(i), refBase, nextBase));

							String removedSNV = point.getStringCIndex() + refBase + ">" + nextBase ;
							String removedDeletion = point2.getStringCIndex() + "del" + nextBase;

							filteredHGVSList.add(removedSNV);
							filteredHGVSList.add(removedDeletion);

							System.out.println("filtered from compressed peak : " + removedSNV);
							System.out.println("filtered from compressed peak : " + removedDeletion);
						}
					}
				}
				catch(Exception e) {
					//e.printStackTrace();
					continue;
				}
			}
		}

		Iterator<Variant> iter = variantList.iterator();
		while(iter.hasNext()) {
			Variant v = iter.next();
			if(!filteredHGVSList.contains(v.getHGVS()))
				ret.add(v);
		}

		return ret;
	}

	//hetero indel 이 있는경우 이와 겹치는 영역에 homoindel이 동시에 calling 된 경우 misalignment로 인한 결과일 가능성 높음
	// 이럴 경우 gap opening penalty 증가시키도록 추천
	public boolean misAlignment(TreeSet<Variant> variantSet) {
		Iterator<Variant> iter = variantSet.iterator();
		while(iter.hasNext()) {
			Variant v1 = (Variant)iter.next();
			if(v1 instanceof Indel && v1.zygosity.equals("hetero")) {
				Indel hetIndel = (Indel)v1;
				TreeSet<EquivExpression> equivSet = hetIndel.getEquivExpressionList();
				int hetMin = Integer.MAX_VALUE;
				int hetMax = Integer.MIN_VALUE;
				//equiv expression이 span 하는 영역을 찾음
				Iterator<EquivExpression> iter2 = equivSet.iterator();
				while(iter2.hasNext()) {
					EquivExpression tempEquiv = (EquivExpression)iter2.next();
					if(hetMin > tempEquiv.getgIndex1())
						hetMin = tempEquiv.getgIndex1();
					if(hetMax < tempEquiv.getgIndex2())
						hetMax = tempEquiv.getgIndex2();
				}
				System.out.println(String.format("(hetMin, hetMax) : (%d, %d)", hetMin, hetMax));
				
				//영역 겹치는 homoindel 있는지 찾기.
				Iterator<Variant> iter3 = variantSet.iterator();
				while(iter3.hasNext()) {
					Variant v2 = (Variant)iter3.next();
					if(v2 instanceof Indel && v2.zygosity.equals("homo")) {
						Indel homoIndel = (Indel)v2;
						TreeSet<EquivExpression> equivSet2 = homoIndel.getEquivExpressionList();
						int homoMin = Integer.MAX_VALUE;
						int homoMax = Integer.MIN_VALUE;
						//equiv expression이 span 하는 영역을 찾음
						Iterator<EquivExpression> iter4 = equivSet2.iterator();
						while(iter4.hasNext()) {
							EquivExpression tempEquiv2 = (EquivExpression)iter4.next();
							if(homoMin > tempEquiv2.getgIndex1())
								homoMin = tempEquiv2.getgIndex1();
							if(homoMax < tempEquiv2.getgIndex2())
								homoMax = tempEquiv2.getgIndex2();
						}
						
						//range 안겹쳐도 근처에 있어도 detection되게함
						homoMin -= 50; hetMin -= 50;
						homoMax += 50; hetMax += 50;
						
						
						System.out.println(String.format("(homoMin, homoMax) : (%d, %d)", homoMin, homoMax));
						
						//겹치는 부분이 있으면 true return
						if(!(hetMax<homoMin || homoMax < hetMin))
							return true;
					}
				}
			}
		}
		return false;
	}


	/**
	 * Makes a list of variants (SNVs, Indels)
	 * @return  variant list
	 */
	public TreeSet<Variant> makeVariantList() {
		alignedPoints = rootController.alignedPoints;
		TreeSet<Variant> variantList = new TreeSet<Variant>();
		Variant variant = null;

		int fwdHeteroIndelStartPoint = alignedPoints.size();	//0부터 시작하는 index. 여기부터 hetero indel 시작하니까 이거 -1 까지만 SNV, homoIndel search
		int revHeteroIndelStartPoint = -1; 							   //0부터 시작하는 index. 여기부터 hetero indel 시작하니까 이거 +1 까지만 SNV, homoIndel search

		//HeteroIndel StartPosition 만들기
		for(Variant heteroIndel:heteroIndelList) {
			heteroIndel = (Indel)heteroIndel;
			if(heteroIndel.direction == 1) {
				fwdHeteroIndelStartPoint = heteroIndel.getAlignmentIndex()-1;	//1부터 시작하는 index -> 0부터 시작하는 index 이므로 -1
				//System.out.println("fwdHeteroIndelStartPoint : " + fwdHeteroIndelStartPoint);
			}
			else if(heteroIndel.direction == -1) {
				revHeteroIndelStartPoint = heteroIndel.getAlignmentIndex()-1;	//1부터 시작하는 index -> 0부터 시작하는 index 이므로 -1
				//System.out.println("revHeteroIndelStartPoint : " + revHeteroIndelStartPoint);
			}
		}

		//fwd와 rev 같은 hetero Indel variant이면 rev만 남기기 (rev에 합쳐놨음 <-- RootController.detectHeteroIndel())
		if(heteroIndelList.size() == 2) {
			Indel fwdIndel = (Indel)heteroIndelList.get(0);
			Indel revIndel = (Indel)heteroIndelList.get(1);
			if(fwdIndel.getHGVS().equals(revIndel.getHGVS())) {
				heteroIndelList = new Vector<Variant>();
				heteroIndelList.add(revIndel);
			}

		}
		variantList.addAll(heteroIndelList);

		//FWD SNV 
		for(int i=0;i<fwdHeteroIndelStartPoint;i++) {
			AlignedPoint ap = alignedPoints.get(i);

			if(fwdLoaded && ap.getFwdChar() != Formatter.gapChar && ap.getRefChar() != Formatter.gapChar) {	//Indel은 따로.
				//ATGC중 하나인 경우 (ambiguous 아닌 경우, 즉 single peak인 경우)
				if(ap.getFwdChar()=='A' || ap.getFwdChar()=='T' || ap.getFwdChar()=='G' || ap.getFwdChar()=='C') {
					if(ap.getFwdChar() != ap.getRefChar()) {	
						variant = new SNV(rootController, ap.getRefChar(), ap.getFwdChar(), ap.getRevChar(), 1, ap.getStringCIndex(), (i+1), ap.getFwdTraceIndex(), ap.getRevTraceIndex(), ap.isCoding(), ap.getGIndex(), onTarget(i+1), "homo");
						variantList.add(variant);
					}
				}

				else {		//ambiguous symbol
					Vector<String> IUPACList = SymbolTools.IUPACtoSymbolList(ap.getFwdChar());
					for(String baseString:IUPACList) {
						char baseChar = baseString.charAt(0);
						if(ap.getRefChar() != baseChar) {
							variant = new SNV(rootController, ap.getRefChar(), baseChar, ap.getRevChar(), 1,  ap.getStringCIndex(), (i+1), ap.getFwdTraceIndex(), ap.getRevTraceIndex(), ap.isCoding(), ap.getGIndex(), onTarget(i+1), "hetero");
							variantList.add(variant);
						}
					}
				}
			}
		}

		//REV SNV 
		for(int i=revHeteroIndelStartPoint+1;i<alignedPoints.size();i++) {
			AlignedPoint ap = alignedPoints.get(i);

			if(revLoaded && ap.getRevChar() != Formatter.gapChar && ap.getRefChar() != Formatter.gapChar) {	// Indel은 따로.

				//ATGC
				if(ap.getRevChar()=='A' || ap.getRevChar()=='T' || ap.getRevChar()=='G' || ap.getRevChar()=='C') {
					if(ap.getRevChar() != ap.getRefChar()) {	
						variant = new SNV(rootController, ap.getRefChar(), ap.getFwdChar(), ap.getRevChar(), -1, ap.getStringCIndex(), (i+1), ap.getFwdTraceIndex(), ap.getRevTraceIndex(), ap.isCoding(), ap.getGIndex(), onTarget(i+1), "homo");
						if(variantList.contains(variant)) {
							variantList.remove(variant);
							variant.setHitCount(2); 
						}
						variantList.add(variant);
					}
				}

				else  {		//ambiguous symbol
					Vector<String> IUPACList = SymbolTools.IUPACtoSymbolList(ap.getRevChar());
					for(String baseString:IUPACList) {
						char baseChar = baseString.charAt(0);
						if(ap.getRefChar() != baseChar) {
							variant = new SNV(rootController, ap.getRefChar(), ap.getFwdChar(), baseChar, -1, ap.getStringCIndex(), (i+1), ap.getFwdTraceIndex(), ap.getRevTraceIndex(), ap.isCoding(), ap.getGIndex(), onTarget(i+1), "hetero");
							if(variantList.contains(variant)) {	
								variantList.remove(variant);
								variant.setHitCount(2); 
							}
							variantList.add(variant);
						}
					}
				}
			}
		}

		//Homo Insertion Call Logic (Fwd)
		if(fwdLoaded) 
			for(int i=0;i<fwdHeteroIndelStartPoint;i++) {
				AlignedPoint ap = alignedPoints.get(i);

				if(ap.getRefChar() == Formatter.gapChar && ap.getFwdChar() != Formatter.gapChar) {
					StringBuffer buffer = new StringBuffer();
					int j=i;
					for(;j<fwdHeteroIndelStartPoint;j++) {
						AlignedPoint ap2 = alignedPoints.get(j);
						if(ap2.getRefChar()==Formatter.gapChar  &&  ap2.getFwdChar() != Formatter.gapChar)
							buffer.append(ap2.getFwdChar());
						else {
							break;
						}
					}
					String insertedSeq = buffer.toString();

					int index2 =0;
					if(j<alignedPoints.size())
						index2 = j;
					else 
						index2 = j-1;

					//duplication 여부판단
					boolean duplication = false;
					int dupStartIndex = i-insertedSeq.length();
					if(dupStartIndex >= 0) {
						int k =0;
						for(;k<insertedSeq.length();k++) {
							AlignedPoint ap3 = alignedPoints.get(dupStartIndex + k);
							if(ap3.getFwdChar()!=insertedSeq.charAt(k))
								break;
						}
						if(k==insertedSeq.length()) duplication = true;
					}
					if(duplication) {
						variant = new Indel(rootController, "homo", 1, Indel.duplication, dupStartIndex+1, dupStartIndex +insertedSeq.length(), dupStartIndex+1, insertedSeq, onTarget(dupStartIndex+1));
					}
					else
						variant = new Indel(rootController, "homo", 1, Indel.insertion, i+1, j+1,i+1, insertedSeq, onTarget(i+1));
					variantList.add(variant);

					i=j;
				}
			}

		//Homo Insertion Call Logic (Rev)
		if(revLoaded) 
			for(int i=revHeteroIndelStartPoint+1;i<alignedPoints.size();i++) {
				AlignedPoint ap = alignedPoints.get(i);

				if(ap.getRefChar() == Formatter.gapChar  && ap.getRevChar() != Formatter.gapChar) {
					StringBuffer buffer = new StringBuffer();
					int j=i;
					for(;j<alignedPoints.size();j++) {
						AlignedPoint ap2 = alignedPoints.get(j);
						if(ap2.getRefChar()==Formatter.gapChar  && ap2.getRevChar() != Formatter.gapChar)
							buffer.append(ap2.getRevChar());
						else {
							break;
						}
					}
					String insertedSeq = buffer.toString();

					int index2 =0;
					if(j<alignedPoints.size())
						index2 = j;
					else 
						index2 = j-1;

					//duplication 여부판단
					boolean duplication = false;
					int dupStartIndex = i-insertedSeq.length();
					if(dupStartIndex >= 0) {
						int k =0;
						for(;k<insertedSeq.length();k++) {
							AlignedPoint ap3 = alignedPoints.get(dupStartIndex + k);
							if(ap3.getRevChar()!=insertedSeq.charAt(k))
								break;
						}
						if(k==insertedSeq.length()) duplication = true;
					}
					if(duplication) {
						variant = new Indel(rootController, "homo", -1, Indel.duplication, dupStartIndex+1, dupStartIndex +insertedSeq.length(), dupStartIndex+1, insertedSeq, onTarget(dupStartIndex+1));
					}
					else
						variant = new Indel(rootController, "homo", -1, Indel.insertion, i+1, j+1, i+1, insertedSeq, onTarget(i+1));

					if(variantList.contains(variant)) {	//Fwd, Rev 양쪽에서 detection 된 SNV는 양쪽 pane에서 다 highlight
						variantList.remove(variant);
						variant.setHitCount(2);
						variant.setFwdTraceChar(ap.getFwdChar());
					}
					variantList.add(variant);
					//System.out.println(indel.getHGVS());
					i=j;
				}
			}

		//Homo deletion Call (FWD)
		if(fwdLoaded) 
			for(int i=0;i<fwdHeteroIndelStartPoint;i++) {
				AlignedPoint ap = alignedPoints.get(i);

				if(ap.getFwdChar() == Formatter.gapChar && ap.getRefChar() != Formatter.gapChar) {
					int j=i;
					for(;j<fwdHeteroIndelStartPoint;j++) {
						AlignedPoint ap2 = alignedPoints.get(j);
						if(!(ap2.getFwdChar()==Formatter.gapChar  &&  ap.getRefChar() != Formatter.gapChar))
							break;
					}

					if(i==0 || j==alignedPoints.size()) {	//맨앞, 맨뒤에 fwd rev 중 한가닥씩만 튀어나온 부분은 deletion call에서 제외
						i=j;
						continue;
					}

					variant = new Indel(rootController, "homo", 1, Indel.deletion, i+1, j,i+1, "", onTarget(i+1));
					variantList.add(variant);
					//System.out.println(indel.getHGVS());
					i=j;
				}
			}

		//Homo deletion Call (REV)
		if(revLoaded) 
			for(int i=revHeteroIndelStartPoint+1;i<alignedPoints.size();i++) {
				AlignedPoint ap = alignedPoints.get(i);

				if(ap.getRevChar() == Formatter.gapChar && ap.getRefChar() != Formatter.gapChar) {
					int j=i;
					for(;j<alignedPoints.size();j++) {
						AlignedPoint ap2 = alignedPoints.get(j);
						if(!(ap2.getRevChar()==Formatter.gapChar  &&  ap.getRefChar() != Formatter.gapChar))
							break;
					}
					if(i==0 || j==alignedPoints.size()) {	//맨앞, 맨뒤에 fwd rev 중 한가닥씩만 튀어나온 부분은 deletion call에서 제외
						i=j;
						continue;
					}

					variant = new Indel(rootController, "homo", -1, Indel.deletion, i+1, j,i+1, "", onTarget(i+1));
					if(variantList.contains(variant)) {	//Fwd, Rev 양쪽에서 detection 된 SNV는 양쪽 pane에서 다 highlight
						variantList.remove(variant);
						variant.setHitCount(2);
						variant.setFwdTraceChar(ap.getFwdChar());
					}
					variantList.add(variant);
					//System.out.println(indel.getHGVS());
					i=j;
				}
			}
		return variantList;
	}

	/**
	 * Filters variant List
	 * @param variantList
	 * @return returns filtered variant list
	 */
	private TreeSet<Variant> comparisonFilter(TreeSet<Variant> variantList) {
		if(!(fwdLoaded&&revLoaded)) return variantList;

		TreeSet<Variant> tempList = new TreeSet<Variant>(variantList);

		Iterator<Variant> i = variantList.iterator();
		while(i.hasNext()) {
			Variant v = i.next();
			//System.out.println(v.getHGVS());
			if(v.getHitCount() !=1 || v.getAlignmentIndex() < startRange || v.getAlignmentIndex() > endRange)	//fwd, rev 모두 mapping 된 영역이면서 hitCount 1인 variant에 대해서만 검토 
				continue;

			//hetero indel 이면 filtering X
			if(v instanceof Indel && v.getZygosity().equals("hetero")) {
				continue;
			}

			if(v instanceof SNV) {
				AlignedPoint ap = alignedPoints.get(v.getAlignmentIndex()-1);
				int oppositeSideQuality = 0;
				char oppositeSideChar =  'N';
				if(v.getDirection()==1) {
					oppositeSideQuality = ap.getRevQuality();
					oppositeSideChar  = ap.getRevChar();
				}
				else if(v.getDirection()==-1) {
					oppositeSideQuality = ap.getFwdQuality();
					oppositeSideChar = ap.getFwdChar();
				}
				if(oppositeSideQuality >= RootController.filterQualityCutoff && oppositeSideChar == ap.getRefChar()) {	//반대쪽 trace가 reference랑 같으면서 Quality 높을때 filtering
					//System.out.println(v.getHGVS() + " has been filtered");
					tempList.remove(v);
				}
			}
			else if (v instanceof Indel) {	
				Indel indelV = (Indel)v;
				if(indelV.getType()==Indel.deletion) {//size 1인 del에 대해서 filtering
					//if(!indelV.getcIndex().equals(indelV.getCIndex2()))  
					//	continue;

					boolean remove = true;
					int counter = 0;
					while(indelV.getAlignmentIndex()-1+counter < alignedPoints.size()) {
						AlignedPoint ap = alignedPoints.get(indelV.getAlignmentIndex()-1+counter);
						int oppositeSideQuality = 0;
						char oppositeSideChar = 'N';
						if(indelV.getDirection()==1) {
							oppositeSideQuality = ap.getRevQuality();
							oppositeSideChar = ap.getRevChar();
						}
						else if(indelV.getDirection()==-1) {
							oppositeSideQuality = ap.getFwdQuality();
							oppositeSideChar = ap.getFwdChar();
						}
						if(!(oppositeSideQuality >= RootController.filterQualityCutoff && oppositeSideChar == ap.getRefChar())) {
							remove = false;
							break;
						}
						if(ap.getGIndex() == indelV.getgIndex2()) break;
						counter++;
					}
					if(remove) {
						tempList.remove(v);
						System.out.println(v.getHGVS() + " has been filtered");
					}
				}
				else if(indelV.getType()==Indel.insertion) {//size 1인 ins에 대해서 filtering : 아직 테스트 해보지 못함 테스트 데이터 없음. Insertion 이면 반대쪽 다 공백인데 가능함??
					if(indelV.getIndelSeq().length() != 1)   
						continue;

					//현재 포인트 앞뒤로 quality가 모두 RootController.filterQualityCutoff인지를 확인
					int oppositeSideQuality1 = 0;
					int oppositeSideQuality2 = 0;
					char oppositeSideChar1 = 'N', oppositeSideChar2 = 'N';
					AlignedPoint ap1 = null, ap2 = null;
					try {
						ap1 = alignedPoints.get(indelV.getAlignmentIndex()-2);
						ap2 = alignedPoints.get(indelV.getAlignmentIndex());
						//혹시라도 위 두문장에서 error나면 그냥 quality1,2 = 0 --> filtering 안하고 남겨둠.

						if(indelV.getDirection()==1) {
							oppositeSideQuality1 = ap1.getRevQuality();
							oppositeSideQuality2 = ap2.getRevQuality();
							oppositeSideChar1 = ap1.getRevChar();
							oppositeSideChar2 = ap2.getRevChar();
						}
						else if(indelV.getDirection()==-1) {
							oppositeSideQuality1 = ap1.getFwdQuality();
							oppositeSideQuality2 = ap2.getFwdQuality();
							oppositeSideChar1 = ap1.getFwdChar();
							oppositeSideChar2 = ap2.getFwdChar();
						}
					}
					catch (Exception ex) {
						ex.printStackTrace();
					}

					if(oppositeSideQuality1 >= RootController.filterQualityCutoff && oppositeSideQuality2 >= RootController.filterQualityCutoff && oppositeSideChar1==ap1.getRefChar() && oppositeSideChar2 == ap2.getRefChar()) {
						tempList.remove(v);
						System.out.println(v.getHGVS() + " has been filtered");
					}
				}
			}
		}
		return tempList;
	}


	private TreeSet<Variant> makeHomoDelins (TreeSet<Variant> variantList) throws Exception {
		TreeSet<Variant> ret = new TreeSet<Variant>(variantList);
		TreeSet<Variant> tempVariantList = new TreeSet<Variant>();


		Variant[] variantArray = new Variant[variantList.size()];
		variantList.toArray(variantArray);

		boolean building = false;
		boolean v1v2Connected = false;
		boolean skipThisTime = false;

		for(int i=1;i<variantArray.length;i++) {
			//직전 loop에서 made 되었으면 이번 loop에서 v1은 이미 사용된거임. 따라서 pass
			if(skipThisTime) {
				skipThisTime = false;
				continue;
			}
			Variant v1 = variantArray[i-1];
			Variant v2 = variantArray[i];
			int v1_Rt_gIndex = 0;
			int v2_Lt_gIndex1 = 0;
			boolean v1MultipleExpression = false;
			boolean v2MultipleExpression = false;

			if(v1.zygosity.equals("hetero") || v2.zygosity.equals("hetero")) {	//둘중 하나 hetero이면 skip
				v1v2Connected = false;
			}
			else {	// 둘다 homo 일때
				if(v1 instanceof SNV) {
					v1_Rt_gIndex = v1.getgIndex();
				}
				else if (v1 instanceof Indel) {
					Indel indel1 = (Indel) v1;
					TreeSet<EquivExpression> eeList = indel1.getEquivExpressionList();
					if(eeList.size()>1) v1MultipleExpression = true;

					EquivExpression[] eeArray = new EquivExpression[eeList.size()];
					eeList.toArray(eeArray);
					EquivExpression ee = eeArray[eeArray.length-1];		//가장 오른쪽꺼

					v1_Rt_gIndex = ee.getgIndex2();

					if(indel1.getType()==Indel.insertion) {
						v1_Rt_gIndex--;
					}
				}

				if(v2 instanceof SNV) {
					v2_Lt_gIndex1 = v2.getgIndex();
				}
				else if (v2 instanceof Indel) {
					Indel indel2 = (Indel) v2;
					TreeSet<EquivExpression> eeList = indel2.getEquivExpressionList();
					if(eeList.size()>1) v2MultipleExpression = true;

					EquivExpression[] eeArray = new EquivExpression[eeList.size()];
					eeList.toArray(eeArray);
					EquivExpression ee = eeArray[0];		//가장 왼쪽꺼

					v2_Lt_gIndex1 = ee.getgIndex1();

					if(indel2.getType()==Indel.insertion) {
						v2_Lt_gIndex1++;
					}
				}

				//겹치거나 연결되면. (homo의 경우 겹칠일은 없을것 같긴 하지만..) hetero는 한자리에서 SNV 2개 있을수 있으므로 겹칠 수 있음.
				//단 multiple Expression = false 이면
				//계속 이어가기.
				if(v2_Lt_gIndex1 <= v1_Rt_gIndex + 1) {
					v1v2Connected = true;
				}
				else {
					v1v2Connected = false;
				}
			}

			if(building) {
				if(v1v2Connected) {
					tempVariantList.add(v2);
					if(v2MultipleExpression) {
						ret = updateVariantList(ret, tempVariantList);
						tempVariantList = new TreeSet<Variant>();
						building = false;
						skipThisTime = true;	//v2 소진되었으므로 다음에 v1으로 역할하면 안됨.
					}
				}
				else if(!v1v2Connected) {
					ret = updateVariantList(ret, tempVariantList);
					tempVariantList = new TreeSet<Variant>();
					building = false;
					//skipThisTime = true;	이번에 v2는 안쓴거이므로 skip 하면 안됨. 다음에 v1으로 역할해야됨.
				}
			}
			else if(!building) {
				if(v1v2Connected) {
					tempVariantList.add(v1);
					tempVariantList.add(v2);
					if(v2MultipleExpression) {
						ret = updateVariantList(ret, tempVariantList);
						tempVariantList = new TreeSet<Variant>();
						building = false;
						skipThisTime = true;	//v2 소진되었으므로 다음에 v1으로 역할하면 안됨.
					}
					else {
						building = true;
					}
				}
				else if(!v1v2Connected) {
					//할거없음
				}
			}
		}

		//hetero SNV 연속있는거 delins만들기
		for(int i=1;i<variantArray.length;i++) {
			//직전 loop에서 made 되었으면 이번 loop에서 v1은 이미 사용된거임. 따라서 pass
			if(skipThisTime) {
				skipThisTime = false;
				continue;
			}
			Variant v1 = variantArray[i-1];
			Variant v2 = variantArray[i];
			int v1_Rt_gIndex = 0;
			int v2_Lt_gIndex1 = 0;

			//둘중 하나 homo이거나 indel이면 skip
			if(v1.zygosity.equals("homo") || v2.zygosity.equals("homo") || v1 instanceof Indel || v2 instanceof Indel) {	
				v1v2Connected = false;
			}
			else {	//둘다 hetero SNV 일때
				v1_Rt_gIndex = v1.getgIndex();
				v2_Lt_gIndex1 = v2.getgIndex();

				if(v2_Lt_gIndex1 == v1_Rt_gIndex + 1) {
					v1v2Connected = true;
				}
				else {
					v1v2Connected = false;
				}
			}

			if(building) {
				if(v1v2Connected) {
					tempVariantList.add(v2);
				}
				else if(!v1v2Connected) {
					ret = updateVariantList(ret, tempVariantList);
					tempVariantList = new TreeSet<Variant>();
					building = false;
				}
			}
			else if(!building) {
				if(v1v2Connected) {
					tempVariantList.add(v1);
					tempVariantList.add(v2);
					building = true;
				}
				else if(!v1v2Connected) {
					//할거없음
				}
			}
		}
		return ret;
	}

	//targetVariant List에 있는 모든 variant들의 합쳐서 delins로 만들고 이를 targetVariantList안의 variant들의 combinedExpression에 할당 
	private TreeSet<Variant> updateVariantList(TreeSet<Variant> originalVariantList, TreeSet<Variant> targetVariantList) throws Exception{
		TreeSet<Variant> ret = new TreeSet<Variant>(originalVariantList);

		//delinsString : refString으로부터 시작하여 targetVariantList에 있는 variant들을 다 적용시켜서 만듬
		//이후 delinsString과 refString을 비교하여 delins calling
		String refString = "";
		String delinsString = "";

		//refString, delinsString의 각각의 base들의 gIndex를 저장하기 위한 구조
		Vector<Integer> refIndexList = new Vector<Integer>();
		Vector<Integer> delinsIndexList = new Vector<Integer>();

		for(int i=0;i<alignedPoints.size();i++) {
			AlignedPoint ap = alignedPoints.get(i);
			char refChar = ap.getRefChar();
			if(refChar != Formatter.gapChar) {
				refString += refChar;
				refIndexList.add(ap.getGIndex());

				//delins list도 일단 ref와 동일하게 초기화 해 둠.
				delinsString += refChar;
				delinsIndexList.add(ap.getGIndex());
			}
		}

		Variant[] targetVariantArray = new Variant[targetVariantList.size()];
		targetVariantList.toArray(targetVariantArray);
		System.out.print("targetVariantList : ");

		//delinsCharList와 delinsIndexList에다가 targetVariantList에 있는 variant들을 하나씩 모두 반영.
		for(int i=0;i<targetVariantList.size();i++) {

			Variant v = targetVariantArray[i];
			System.out.print(String.format("%d : %s, ", (i+1), v.getHGVS()));

			//초기값 -1 : not found
			int delinsListIndex1 = -1, delinsListIndex2 = -1;

			if(v instanceof SNV) {
				delinsListIndex2 = 0;
			}
			//위에서 만들어놓은 list 상에서 variant 좌표 어딘지 찾아놓기.
			for(int j=0;j<delinsIndexList.size();j++) {
				if(delinsIndexList.get(j) == v.getgIndex()) {
					delinsListIndex1 = j;
				}
				if(v instanceof Indel) {
					Indel indel = (Indel)v;
					if(delinsIndexList.get(j) == indel.getgIndex2()) {
						delinsListIndex2 = j;
					}
				}
			}

			if(delinsListIndex1 == -1 || delinsListIndex2 == -1) return ret;

			//System.out.println(String.format("delinsListIndex1, delinsListIndex2 : %d, %d",  delinsListIndex1, delinsListIndex2));

			if(v instanceof SNV) {
				SNV snv = (SNV)v;
				char replacementChar;
				if(snv.direction == 1) 
					replacementChar = snv.getFwdTraceChar();
				else 
					replacementChar = snv.getRevTraceChar();

				delinsString = delinsString.substring(0,delinsListIndex1) + replacementChar + delinsString.substring(delinsListIndex1+1, delinsString.length()); 
			}
			else if(v instanceof Indel) {
				Indel indel = (Indel)v;
				if(indel.getType()==Indel.deletion) {
					delinsString = delinsString.substring(0,delinsListIndex1) + delinsString.substring(delinsListIndex2+1, delinsString.length());
					for(int j=delinsListIndex1;j<=delinsListIndex2;j++) {
						//System.out.println(String.format("%d is deleted",  delinsIndexList.get(j)));
						//Vector도 linkedList니까 하나 지우면 뒤에꺼 땡겨옴. 그래서 아래와 같이 delinsListIndex1을 계속 지우면 됨.
						delinsIndexList.remove(delinsListIndex1);
					}
				}
				else if(indel.getType()==Indel.insertion) {
					delinsString = delinsString.substring(0, delinsListIndex1+1)+indel.getIndelSeq() + delinsString.substring(delinsListIndex2, delinsString.length());
					for(int j=0;j<indel.getIndelSeq().length();j++) {
						delinsIndexList.add(delinsListIndex1+1,  0);
					}
				}
				else if(indel.getType()==Indel.duplication) {
					delinsString = delinsString.substring(0, delinsListIndex2+1) + indel.getIndelSeq() + delinsString.substring(delinsListIndex2+1, delinsString.length());
					System.out.println("indelseq : " + indel.getIndelSeq());
					for(int j=0;j<indel.getIndelSeq().length();j++) {
						delinsIndexList.add(delinsListIndex2+1,  0);
					}
				}
			}
		}
		System.out.println();

		System.out.println("refstring : " + refString);
		System.out.println("dinstring : " + delinsString);

		int leftPos = 0;
		int originalRightPos = refString.length()-1;
		int shiftedRightPos = delinsString.length()-1;


		for(;leftPos<refString.length() && leftPos<delinsString.length();leftPos++) {
			if(refString.charAt(leftPos) != delinsString.charAt(leftPos)) break;
		}
		leftPos--;	//마지막으로 일치하는 지점

		while(originalRightPos>leftPos && shiftedRightPos>leftPos) {
			if(refString.charAt(originalRightPos) != delinsString.charAt(shiftedRightPos)) break;
			originalRightPos--;
			shiftedRightPos--;
		}
		//System.out.println("original Right Pos : " + originalRightPos);
		//System.out.println("shifted Right pos : " + shiftedRightPos);

		//originalRightPos, shiftedRightPos : 처음으로 달라진 좌표 or left랑 만났을때 좌표
		String combinedHgvs = "";
		String insertedSeq = "";

		if(leftPos == shiftedRightPos) {	//deletion
			//combine 된게deletion이다.. 이런경우는 없을듯.
			//combine 된건delins일수밖에 없음...  
		}
		else if(leftPos == originalRightPos ) { //insertion
			//combine 된게deletion이다.. 이런경우는 없을듯.
			//combine 된건delins일수밖에 없음...  
		}

		else { //delins
			String cIndex1 = getCIndexFromGIndex(refIndexList.get(leftPos+1));
			String cIndex2 = getCIndexFromGIndex(refIndexList.get(originalRightPos));
			if(cIndex1.equals(cIndex2)) 
				combinedHgvs = cIndex1;
			else
				combinedHgvs = cIndex1 + "_" +cIndex2;
			combinedHgvs += "delins";
			for(int i=leftPos+1;i<=shiftedRightPos;i++) {
				insertedSeq += delinsString.charAt(i);
			}
			combinedHgvs += insertedSeq;

			//AAchange 얻기 위해 indel 생성.
			Indel indel = new Indel(rootController, "homo", 1, Indel.delins, getAlignedIndexFromGIndex(refIndexList.get(leftPos+1)), 
					getAlignedIndexFromGIndex(refIndexList.get(originalRightPos)), getAlignedIndexFromGIndex(refIndexList.get(leftPos+1)), insertedSeq, true);
			combinedHgvs += ", " + indel.getAAChange();
		}

		System.out.println("combinedHGvs : " + combinedHgvs);

		for(int i=0;i<targetVariantArray.length;i++) {
			Variant v = targetVariantArray[i];
			ret.remove(v);
			v.setCombinedExpression(combinedHgvs);
			v.makeTableViewProperties();
			ret.add(v);
		}

		return ret;
	}

	private String getCIndexFromGIndex(int gIndex) {
		String ret= "";
		for(int i=0;i<alignedPoints.size();i++) {
			AlignedPoint ap = alignedPoints.get(i);
			if(ap.getGIndex() == gIndex) {
				ret = ap.getStringCIndex();
				break;
			}
		}
		return ret;
	}

	private int getAlignedIndexFromGIndex(int gIndex) throws Exception {
		Vector<AlignedPoint> aps = rootController.alignedPoints;
		for(int i=0;i<aps.size();i++) {
			AlignedPoint ap = aps.get(i);
			if(ap.getGIndex()==gIndex) return (i+1);	//찾았으면 최소 1 return
		}
		return 0;
	}


	private boolean onTarget(int index) {
		if(!fwdLoaded || !revLoaded) return true;
		if(index>=startRange && index<=endRange) return true;
		else return false;
	}




}
