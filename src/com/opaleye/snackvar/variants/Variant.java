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

import org.biojava.bio.symbol.IllegalAlphabetException;

import com.opaleye.snackvar.EquivExpression;
import com.opaleye.snackvar.RootController;

import javafx.beans.property.SimpleStringProperty;

/**
 * Title : Variant
 * A super of SNV and Indel
 * @author Young-gon Kim
 *2018.10
 */
public class Variant implements Comparable<Variant>{
	protected int direction;
	protected String cIndex;
	protected String HGVS;
	protected String AAChange="";
	protected int alignmentIndex, fwdTraceIndex, revTraceIndex;		
	protected char fwdTraceChar, revTraceChar;
	protected int hitCount= 1;
	protected int gIndex;
	protected boolean onTarget = false;
	protected String zygosity = "";
	protected SimpleStringProperty variantProperty;
	protected SimpleStringProperty zygosityProperty;
	protected SimpleStringProperty frequencyProperty;
	protected SimpleStringProperty fromProperty;
	protected SimpleStringProperty equivalentExpressionsProperty;
	protected String combinedExpression = "";
	protected RootController rootController;
	
	/**
	 * Determines the order to appear in the variant list
	 */
	public int compareTo(Variant s) {
		int gIndex1 = this.gIndex;
		int gIndex2 = s.getgIndex();
		String hgvs1 = this.HGVS;
		String hgvs2 = s.getHGVS();
		String zygosity1 = this.zygosity;
		String zygosity2 = s.getZygosity();

		//hetero indel을 맨 앞에, 그다음에 homo indel
		if(this instanceof Indel) {
			if(s instanceof Indel) {
				if(!zygosity1.equals(zygosity2))
					return zygosity1.compareTo(zygosity2);
				else if(gIndex1 != gIndex2)
					return gIndex1 - gIndex2;
				else if(!hgvs1.equals(hgvs2)) {
					return hgvs1.compareTo(hgvs2);
				} 
					
			}
			else
				return -1;
		}
		else if(this instanceof SNV) {
			if(s instanceof Indel) {
				return 1;
			}
		}
		
		
		
		if(this.onTarget == s.isOnTarget()) {
			if(gIndex1 != gIndex2)
				return gIndex1 - gIndex2;
			else if(!hgvs1.equals(hgvs2)) {
				return hgvs1.compareTo(hgvs2);
			}
			else return zygosity1.compareTo(zygosity2);
		}
		else if(this.onTarget) return -1;
		else return 1;
	}

	/**
	 * Returns corresponding amino acid from DNA triple
	 * @param inputAASeq
	 * @throws IllegalAlphabet Exception when DNA triple does not correspond to a amino acid
	 * @return
	 */
	public static String getAAfromTriple(String triple) throws IllegalAlphabetException {
		String ret = "";
		triple = triple.toUpperCase();
		if(triple.equals("TTT") || triple.equals("TTC")) ret = "Phe";
		else if (triple.equals("TTA") || triple.equals("TTG")) ret = "Leu";
		else if ((triple.substring(0, 2)).equals("CT")) ret = "Leu";
		else if (triple.equals("ATT") || triple.equals("ATC") || triple.equals("ATA")) ret = "Ile";
		else if (triple.equals("ATG")) ret = "Met";
		else if ((triple.substring(0, 2)).equals("GT")) ret = "Val";
		else if ((triple.substring(0, 2)).equals("TC")) ret = "Ser";
		else if ((triple.substring(0, 2)).equals("CC")) ret = "Pro";
		else if ((triple.substring(0, 2)).equals("AC")) ret = "Thr";
		else if ((triple.substring(0, 2)).equals("GC")) ret = "Ala";
		else if (triple.equals("TAT") || triple.equals("TAC")) ret = "Tyr";
		else if (triple.equals("TAA") || triple.equals("TAG") || triple.equals("TGA")) ret = "*";
		else if (triple.equals("CAT") || triple.equals("CAC")) ret = "His";
		else if (triple.equals("CAA") || triple.equals("CAG")) ret = "Gln";
		else if (triple.equals("AAT") || triple.equals("AAC")) ret = "Asn";
		else if (triple.equals("AAA") || triple.equals("AAG")) ret = "Lys";
		else if (triple.equals("GAT") || triple.equals("GAC")) ret = "Asp";
		else if (triple.equals("GAA") || triple.equals("GAG")) ret = "Glu";
		else if (triple.equals("TGT") || triple.equals("TGC")) ret = "Cys";
		else if (triple.equals("TGG")) ret = "Trp";
		else if ((triple.substring(0, 2)).equals("CG")) ret = "Arg";
		else if (triple.equals("AGT") || triple.equals("AGC")) ret = "Ser";
		else if (triple.equals("AGA") || triple.equals("AGG")) ret = "Arg";
		else if ((triple.substring(0, 2)).equals("GG")) ret = "Gly";
		else throw new IllegalAlphabetException();
		return ret;
	}

	protected void makeTableViewProperties() {
		String aaChange = AAChange;
		if(aaChange == null || aaChange =="")
			aaChange = "p.?";
		variantProperty = new SimpleStringProperty(HGVS + ", " + aaChange);
		zygosityProperty= new SimpleStringProperty(zygosity);
		frequencyProperty= new SimpleStringProperty(String.format("%d",  hitCount));
		String from;
		if(direction == 1) 
			from = "Fwd";
		else 
			from = "Rev";
		
		//System.out.println("from : " + from);
		fromProperty = new SimpleStringProperty(from);

		if(this instanceof SNV) {
			equivalentExpressionsProperty= new SimpleStringProperty(combinedExpression);
		}
		else if(this instanceof Indel) {

			Indel indel = (Indel)this;
			StringBuffer equivBuffer = new StringBuffer();
			EquivExpression[] equivExpressionArray = new EquivExpression[indel.equivExpressionList.size()];
			indel.equivExpressionList.toArray(equivExpressionArray);
			for(int i=0;i<equivExpressionArray.length;i++) {
				EquivExpression tempExpression = equivExpressionArray[i];
				String tempHGVS = tempExpression.getHGVS();
				if(indel.getHGVS().equals(tempHGVS)) continue;	//자기자신이랑 같은거는 list에서 제외
				
				equivBuffer.append(tempHGVS);
				if(i != indel.equivExpressionList.size()-1) 
					equivBuffer.append("; ");
			}
			String equivString= equivBuffer.toString();
			if(combinedExpression.length()>=1)
				equivString =  combinedExpression + "; " + equivString;
			System.out.println("equiv list : " + equivString);
			equivalentExpressionsProperty= new SimpleStringProperty(equivString);
		}
	}


	/**
	 * Getters and setters for member variables
	 */
	public int getDirection() {
		return direction;
	}
	public String getcIndex() {
		return cIndex;
	}
	public String getHGVS() {
		return HGVS;
	}
	public int getAlignmentIndex() {
		return alignmentIndex;
	}
	public int getFwdTraceIndex() {
		return fwdTraceIndex;
	}
	public int getRevTraceIndex() {
		return revTraceIndex;
	}
	public char getFwdTraceChar() {
		return fwdTraceChar;
	}
	public char getRevTraceChar() {
		return revTraceChar;
	}
	public void setFwdTraceChar(char fwdTraceChar) {
		this.fwdTraceChar = fwdTraceChar;
	}
	public void setFwdTraceIndex(int fwdTraceIndex) {
		this.fwdTraceIndex = fwdTraceIndex;
	}

	public String getAAChange() {
		return AAChange;
	}
	public void setHitCount(int hitCount) {
		this.hitCount = hitCount;
		frequencyProperty.set(String.format("%d",  hitCount));
		if(hitCount == 2) 
			fromProperty.set("Fwd, Rev");
	}

	public int getHitCount() {
		return hitCount;
	}

	public int getgIndex() {
		return gIndex;
	}

	public boolean isOnTarget() {
		return onTarget;
	}

	public String getVariantProperty() {
		return variantProperty.get();
	}

	public String getZygosityProperty() {
		return zygosityProperty.get();
	}

	public String getFrequencyProperty() {
		return frequencyProperty.get();
	}
	
	public String getEquivalentExpressionsProperty() {
		return equivalentExpressionsProperty.get();
	}
	public String getZygosity() {
		return zygosity;
	}

	public void setCombinedExpression(String combinedExpression) {
		this.combinedExpression = combinedExpression;
	}

	public String getFromProperty() {
		return fromProperty.get();
	}

	
	
}
