package com.opaleye.snackvar.variants;

import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.RNATools;
import org.biojava.bio.symbol.IllegalAlphabetException;
import org.biojava.bio.symbol.IllegalSymbolException;
import org.biojava.bio.symbol.SymbolList;

import com.opaleye.snackvar.RootController;

import javafx.beans.property.SimpleStringProperty;

/**
 * Title : SNV
 * A subclass of Variant
 * A class for SNV
 * @author Young-gon Kim
 *2018.10
 */
public class SNV extends Variant{
	private char refChar;

	/**
	 * 
	 * 	Constructor for a SNV
	 * @param refChar
	 * @param fwdTraceChar
	 * @param revTraceChar
	 * @param direction
	 * @param cIndex
	 * @param alignmentIndex
	 * @param fwdTraceIndex
	 * @param revTraceIndex
	 * @param coding
	 * @param gIndex
	 * @param hitCount
	 */
	
	public SNV(RootController rootController, char refChar, char fwdTraceChar, char revTraceChar, int direction, String cIndex, int alignmentIndex, int fwdTraceIndex, int revTraceIndex, boolean coding, int gIndex, boolean onTarget, String zygosity) {
		//tracePane adjust할때 fwdchar, revchar 모두 필요함. (gap이면 색칠안해야하니까)
		super();
		this.rootController = rootController;
		this.refChar = refChar;
		this.fwdTraceChar = fwdTraceChar;
		this.revTraceChar = revTraceChar;
		this.direction = direction;
		this.cIndex = cIndex;
		this.alignmentIndex = alignmentIndex;
		this.fwdTraceIndex = fwdTraceIndex;
		this.revTraceIndex = revTraceIndex;
		this.gIndex = gIndex;
		this.onTarget = onTarget;
		this.zygosity = zygosity;
		if(direction == 1) 
			HGVS = cIndex + refChar + ">" + fwdTraceChar;
		else
			HGVS = cIndex + refChar + ">" + revTraceChar;
		if(coding) makeAAChange();
		
		makeTableViewProperties();
		
	}

	/**
	 * A Getter for refChar 
	 */
	public char getRefChar() {
		return refChar;
	}

	/**
	 * Amino Acid change generation
	 */
	private void makeAAChange() {
		//gIndex : starts from 1
		String refString = rootController.reference.getRefString();
		String originalSeq = null;
		String variantSeq = null;
		char variantChar = ' ';
		int i_cIndex = 0;
		int frameIndex = 0;
		if(direction == 1) 
			variantChar =  fwdTraceChar;
		else
			variantChar =  revTraceChar;
		try {
			i_cIndex = Integer.parseInt(cIndex.replaceAll("[^0-9]",""));
			if(i_cIndex % 3 == 1) {	// the first base of codon is mutated
				originalSeq = refString.substring(gIndex-1, gIndex+2);
				variantSeq  = variantChar + originalSeq.substring(1,3);
			}
			else if (i_cIndex % 3 == 2) {// the 2nd base of codon is mutated
				originalSeq = refString.substring(gIndex-2, gIndex+1);
				variantSeq =originalSeq.substring(0,1) + variantChar + originalSeq.substring(2,3); 
			}
			else {// the 3rd base of codon is mutated
				originalSeq = refString.substring(gIndex-3, gIndex);
				variantSeq = new String(originalSeq);
				variantSeq  =  originalSeq.substring(0,2) + variantChar;
			}
			frameIndex = (i_cIndex-1)/3+1;
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return;
		}

		String originalAA = "";
		String variantAA = "";
		try {
			originalAA = Variant.getAAfromTriple(originalSeq);
			variantAA = Variant.getAAfromTriple(variantSeq);
			AAChange = "p.(" + originalAA + frameIndex + variantAA+ ")";
			//HGVS = HGVS + ", " + AAChange;
		}
		catch(IllegalAlphabetException iae) {
			//probably due to 'N' from indelSeq
			AAChange = "(untranslatable)";
			return;
			
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return;
		}
		
	}
}
