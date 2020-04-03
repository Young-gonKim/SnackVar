package com.opaleye.snackvar;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.TreeMap;

import org.biojava.bio.program.abi.ABITrace;
import org.biojava.bio.seq.DNATools;

import com.opaleye.snackvar.tools.SymbolTools;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

public class GanseqTrace implements Cloneable{

	//현재 : basecall 전후로 10칸씩. 총 21칸을 사용. 

	protected final Color greenColor = new Color(0, 153, 51);

	private static final int LSTMLength = 10;

	public static final int FORWARD = 1;
	public static final int REVERSE = -1;
	public static final int originalTraceHeight = 110;
	public static final int headTrimPopUp = 35; 
	public static final int tailTrimPopUp = 35;

	protected int traceHeight = 0;

	public static final int traceWidth = 2;

	protected int direction = FORWARD;	//기본은 fwd로 시작, makeTrimmedTrace에서 rev일 경우 변경
	protected int[] traceA = null;
	protected int[] traceT = null;
	protected int[] traceG = null;
	protected int[] traceC = null;
	protected int traceLength = 0;
	protected String sequence = null;
	protected int sequenceLength = 0; 
	protected int[] qCalls = null;
	protected int[] baseCalls = null;

	protected int[] transformedA = null;
	protected int[] transformedT = null;
	protected int[] transformedG = null;
	protected int[] transformedC = null;
	protected int maxHeight = -1;
	protected double ratio = 1.0;

	protected int alignedRegionStart = 0;
	protected int alignedRegionEnd = 0; 

	//originally called by KB basecaller. 
	protected RootController rootController;



	public GanseqTrace() {};
	public GanseqTrace(File ABIFile, RootController rootController) throws Exception {
		this.rootController = rootController;

		//function calls from ABITrace
		ABITrace tempTrace = new ABITrace(ABIFile);
		traceA = tempTrace.getTrace(DNATools.a());
		traceT = tempTrace.getTrace(DNATools.t());
		traceG = tempTrace.getTrace(DNATools.g());
		traceC = tempTrace.getTrace(DNATools.c());
		traceLength = Integer.min(Integer.min(traceA.length, traceT.length), 
				Integer.min(traceG.length, traceC.length));
		sequence = tempTrace.getSequence().seqString().toUpperCase();
		sequenceLength = sequence.length();
		qCalls = tempTrace.getQcalls();
		baseCalls = tempTrace.getBasecalls();

		transformTrace();
	}

	private void transformTrace() {
		maxHeight = -1;
		double imageHeightRatio = 0;
		for(int i=0;i<traceLength;i++) {
			if(traceA[i] > maxHeight) maxHeight = traceA[i];
			if(traceT[i] > maxHeight) maxHeight = traceT[i];
			if(traceG[i] > maxHeight) maxHeight = traceG[i];
			if(traceC[i] > maxHeight) maxHeight = traceC[i];
		}

		transformedA = new int[traceLength];
		transformedT = new int[traceLength];
		transformedG = new int[traceLength];
		transformedC = new int[traceLength];

		traceHeight = (int)(originalTraceHeight * ratio);
		imageHeightRatio = (double)traceHeight / (double)maxHeight;

		//System.out.println("maxHeight : " + maxHeight);
		//System.out.println("Image Height Ratio : " + imageHeightRatio);

		for(int i=0;i<traceLength;i++) {
			//System.out.print("traceA : " + traceA[i]);
			transformedA[i] = new Double((maxHeight-traceA[i]) * imageHeightRatio).intValue();
			//System.out.println("transformed traceA : " + traceA[i]);
			transformedT[i] = new Double((maxHeight-traceT[i]) * imageHeightRatio).intValue();
			transformedG[i] = new Double((maxHeight-traceG[i]) * imageHeightRatio).intValue();
			transformedC[i] = new Double((maxHeight-traceC[i]) * imageHeightRatio).intValue();
		}
	}

	public BufferedImage getDefaultImage() {
		BufferedImage image = new BufferedImage(traceLength*traceWidth, traceHeight+30, BufferedImage.TYPE_BYTE_INDEXED);
		Graphics2D g = image.createGraphics();
		g.setBackground(Color.WHITE);
		g.clearRect(0, 0, traceLength*traceWidth, traceHeight+30);

		for(int i=0;i<traceLength-1;i++) {
			g.setColor(greenColor);
			g.drawLine (i*traceWidth,transformedA[i],(i+1) * traceWidth, transformedA[i+1]);

			g.setColor(Color.RED);
			g.drawLine (i*traceWidth,transformedT[i],(i+1) * traceWidth, transformedT[i+1]);

			g.setColor(Color.BLACK);
			g.drawLine (i*traceWidth,transformedG[i],(i+1) * traceWidth, transformedG[i+1]);

			g.setColor(Color.BLUE);
			g.drawLine (i*traceWidth,transformedC[i],(i+1) * traceWidth, transformedC[i+1]);
		}

		for(int i=0;i<sequenceLength;i++) {
			char baseChar[] = {sequence.charAt(i)};
			int xPos = baseCalls[i]*traceWidth;
			switch(baseChar[0]) {
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
				g.setColor(new Color(255,20,147));;
			}
			g.drawChars(baseChar,  0,  1, Integer.max(0, xPos-3), traceHeight+13);
			g.setColor(Color.BLACK);
			if((i+1)%10 ==1) {
				g.drawLine(traceWidth*baseCalls[i], traceHeight+13, traceWidth*baseCalls[i], traceHeight+18);
				g.drawString(Integer.toString((i+1)), traceWidth*baseCalls[i]-3, traceHeight+30);
			}

		}
		return image;
	}

	/**
	 * Returns a Shaded image
	 * @param startTrimPosition : Left side trim position
	 * @param endTrimPosition : Right side trim position
	 * @author Young-gon Kim
	 */
	public Image getTrimmingImage(int startTrimPosition, int endTrimPosition) {

		BufferedImage originalImage = getDefaultImage();
		Graphics2D g = originalImage.createGraphics();

		g.setColor(Color.BLUE);
		g.setComposite(AlphaComposite.SrcOver.derive(0.2f));
		g.fillRect(0, 0, (startTrimPosition+1), traceHeight);
		g.fillRect(Integer.min(endTrimPosition, traceWidth*traceLength-1), 0, (traceWidth*traceLength-endTrimPosition), traceHeight);

		Image ret = SwingFXUtils.toFXImage(originalImage, null);
		return ret;		
	}

	/**
	 * 
	 * @param startPosition
	 * @param endPosition
	 * @param option  : 0:no shading, 1:point (based on traceBaseNumbering) 2:area (based on newBaseNumbering) 
	 * @return
	 */
	public BufferedImage getShadedImage(Formatter formatter, int option, int startPosition, int endPosition) {

		int newTraceLength = 0;
		int startOffset = 0;

		if(direction == FORWARD) {
			newTraceLength = formatter.fwdNewLength;
			startOffset = formatter.fwdStartOffset;
		}
		else {
			newTraceLength = formatter.revNewLength;
			startOffset = formatter.revStartOffset;
		}

		BufferedImage image = new BufferedImage(newTraceLength, traceHeight+30, BufferedImage.TYPE_BYTE_INDEXED);
		Graphics2D g = image.createGraphics();
		g.setBackground(Color.WHITE);
		g.clearRect(0, 0, newTraceLength, traceHeight+30);


		TreeMap<Integer, Integer> fwdMap = formatter.fwdCoordinateMap;
		TreeMap<Integer, Integer> revMap = formatter.revCoordinateMap;

		for(int i=0;i<traceLength-1;i++) {
			g.setColor(greenColor);
			g.drawLine (startOffset + i*traceWidth,transformedA[i], startOffset +(i+1) * traceWidth, transformedA[i+1]);

			g.setColor(Color.RED);
			g.drawLine (startOffset + i*traceWidth,transformedT[i], startOffset +(i+1) * traceWidth, transformedT[i+1]);

			g.setColor(Color.BLACK);
			g.drawLine (startOffset + i*traceWidth,transformedG[i], startOffset +(i+1) * traceWidth, transformedG[i+1]);

			g.setColor(Color.BLUE);
			g.drawLine (startOffset + i*traceWidth,transformedC[i], startOffset +(i+1) * traceWidth, transformedC[i+1]);
		}

		for(int i=0;i<sequenceLength;i++) {
			if(i+1<alignedRegionStart || i+1>alignedRegionEnd) continue;

			char baseChar[] = {sequence.charAt(i)};
			int xPos = startOffset + baseCalls[i]*traceWidth;
			switch(baseChar[0]) {
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
				g.setColor(new Color(255,20,147));
			}
			g.drawChars(baseChar,  0,  1, Integer.max(0, xPos-3), traceHeight+13);
			g.setColor(Color.BLACK);

			int mappedNo = 0;
			Integer i_mappedNo = null;
			if(direction == FORWARD) {
				i_mappedNo = fwdMap.get(new Integer(i+1));
			}
			else {
				i_mappedNo = revMap.get(new Integer(i+1));
			}

			if(i_mappedNo !=null) {
				mappedNo = i_mappedNo.intValue();
				if(mappedNo%10 ==1) {
					g.drawLine(startOffset + traceWidth*baseCalls[i], traceHeight+13, startOffset + traceWidth*baseCalls[i], traceHeight+18);
					g.drawString(Integer.toString(mappedNo), startOffset + traceWidth*baseCalls[i]-3, traceHeight+30);
				}
			}
		}


		if(option == 1) {
			g.setColor(Color.BLUE);
			g.setComposite(AlphaComposite.SrcOver.derive(0.2f));
			g.fillRect(startOffset + (baseCalls[startPosition]-5) * traceWidth, 0, 10*traceWidth, traceHeight+30);
			return image;

		}
		else if (option == 2) {
			g.setColor(Color.BLUE);
			g.setComposite(AlphaComposite.SrcOver.derive(0.2f));
			g.fillRect(startOffset + (baseCalls[startPosition]-5) * traceWidth, 0, (baseCalls[endPosition]-baseCalls[startPosition]+10)*traceWidth, traceHeight+30);

			return image;
		}
		else 
			return image;
	}


	//ruler Image
	public Image getRulerImage() {
		BufferedImage image = new BufferedImage(28, traceHeight, BufferedImage.TYPE_BYTE_INDEXED);
		Graphics2D g = image.createGraphics();
		g.setBackground(Color.WHITE);
		g.clearRect(0, 0, 28, traceHeight);
		g.setColor(Color.BLACK);

		double imageHeightRatio = (double)traceHeight / (double)maxHeight;
		for(int i=1000;i<=maxHeight;i+=1000) {
			int yPos = new Double((maxHeight-i) * imageHeightRatio).intValue();
			g.drawString(Integer.toString(i), 0, yPos);
		}
		return SwingFXUtils.toFXImage(image, null);
	}

	/**
	 * Returns a trimmed trace
	 * @param startTrimPosition : Left side trim position
	 * @param endTrimPosition : Right side trim position
	 * @author Young-gon Kim
	 */
	public void makeTrimmedTrace(int startTrimPosition, int endTrimPosition, boolean complement) throws Exception {
		int[] oldA = traceA;
		int[] oldT = traceT;
		int[] oldG = traceG;
		int[] oldC = traceC;

		String oldSequence = sequence;
		int oldSequenceLength = sequenceLength;
		int[] oldBaseCalls = baseCalls;
		int[] oldQCalls = qCalls;

		StringBuffer buffer = new StringBuffer();
		//System.out.println(String.format("startTrimPosition, endTrimposition : %d, %d", startTrimPosition, endTrimPosition));
		if(startTrimPosition != -1)
			startTrimPosition /= traceWidth;
		endTrimPosition /= traceWidth;
		//System.out.println(String.format("startTrimPosition, endTrimposition : %d, %d", startTrimPosition, endTrimPosition));
		traceLength = endTrimPosition - startTrimPosition - 1; 	//양 끝점이 포함되지 않으므로 -1

		traceA = new int[traceLength];
		traceT = new int[traceLength];
		traceG = new int[traceLength];
		traceC = new int[traceLength];

		for(int i=startTrimPosition+1; i<endTrimPosition; i++) {
			traceA[i-(startTrimPosition+1)] = oldA[i];
			traceT[i-(startTrimPosition+1)] = oldT[i];
			traceG[i-(startTrimPosition+1)] = oldG[i];
			traceC[i-(startTrimPosition+1)] = oldC[i];
		}
		for(int i=0;i<oldSequenceLength;i++) {
			if(oldBaseCalls[i]>startTrimPosition && oldBaseCalls[i] < endTrimPosition) {
				buffer.append(oldSequence.charAt(i));
			}
		}

		sequence = (buffer.toString()).toUpperCase();
		sequenceLength = sequence.length();
		qCalls = new int[sequenceLength];
		baseCalls = new int[sequenceLength];

		int count = 0;
		for(int i=0;i<oldSequenceLength;i++) {
			if(oldBaseCalls[i]>startTrimPosition && oldBaseCalls[i] < endTrimPosition) {
				qCalls[count] = oldQCalls[i];
				baseCalls[count] = oldBaseCalls[i]-(startTrimPosition+1);
				count++;
			}
		}

		transformTrace();

		try {
			applyAmbiguousSymbol();
			if(complement) 
				makeComplement();
		}
		catch(Exception ex) {
			ex.printStackTrace();
			throw new Exception(ex.getMessage());
		}
	}

	/**
	 * Makes a complemented Trace
	 * @author Young-gon Kim
	 */

	public void makeComplement() throws IllegalArgumentException {
		int[] newQcalls = new int[sequenceLength];
		int[] newBaseCalls = new int[sequenceLength];
		int[] newA = new int[traceLength];
		int[] newT = new int[traceLength];
		int[] newG = new int[traceLength];
		int[] newC = new int[traceLength];


		try {
			sequence = SymbolTools.getComplementString(sequence);
		}
		catch(Exception ex) {
			ex.printStackTrace();
			throw new IllegalArgumentException("Failed to make a complement file"); 
		}


		for(int i=0; i<sequenceLength; i++) {
			newQcalls[i] = qCalls[sequenceLength-1-i];
			newBaseCalls[i] = (traceLength-1) - baseCalls[sequenceLength-1-i];
		}

		for(int i=0; i<traceLength; i++) {
			newA[i] = traceT[traceLength-1-i];
			newT[i] = traceA[traceLength-1-i];
			newG[i] = traceC[traceLength-1-i];
			newC[i] = traceG[traceLength-1-i];
		}
		qCalls = newQcalls;
		baseCalls = newBaseCalls;
		traceA = newA;
		traceT = newT;
		traceG = newG;
		traceC = newC;
		if(direction == FORWARD) 
			direction = REVERSE;
		else
			direction = FORWARD;

		transformTrace();
	}

	/**
	 * Filtering Old version.      DO NOT REMOVE!!!	
	 * @param basePosition
	 * @param cutOff
	 * @return
	 */
	public TwoPeaks getTwoPeaks(int basePosition, double cutOff) {

		/*
		if(RootController.filteringOption.equals(SettingsController.AIBasedFiltering)) {
			return getTwoPeaks_LSTM(basePosition, cutOff);
		}

		else

		if(RootController.filteringOption.equals(SettingsController.noFiltering)) {
			return getTwoPeaks_noFiltering(basePosition, cutOff);
		}
		else if (RootController.filteringOption.equals(SettingsController.ruleBasedFiltering)) {
			return getTwoPeaks_ruleBasedFiltering(basePosition, cutOff);
		}
		 */ 

		return getTwoPeaks_ruleBasedFiltering(basePosition, cutOff);
	}

	/*
	public TwoPeaks getTwoPeaks_noFiltering(int basePosition, double cutOff) {	

		int baseHeights[] = new int [4];
		int position = baseCalls[basePosition];
		boolean secondPeakExist = false;

		baseHeights[0] = traceA[position];
		baseHeights[1] = traceT[position];
		baseHeights[2] = traceG[position];
		baseHeights[3] = traceC[position];

		int maxValue = -1, secondMaxValue = -1;
		int maxIndex = 0, secondMaxIndex = 0;

		for(int j=0;j<4;j++) {
			if(baseHeights[j] > maxValue) {
				maxValue = baseHeights[j];
				maxIndex = j;
			}
		}

		for(int j=0;j<4;j++) {
			if(j == maxIndex) continue;
			if(baseHeights[j] > secondMaxValue) {
				secondMaxValue = baseHeights[j];
				secondMaxIndex = j;
			}
		}


		if(maxValue != 0 && (secondMaxValue / (double)maxValue >= cutOff)) {
			secondPeakExist = true;
		}
		return new TwoPeaks(SymbolTools.numberToBase(maxIndex), SymbolTools.numberToBase(secondMaxIndex), maxValue, secondMaxValue, secondPeakExist);
	}
	 */
	public TwoPeaks getTwoPeaks_ruleBasedFiltering(int basePosition, double cutOff) {	

		int baseHeights[] = new int [4];
		int position = baseCalls[basePosition];
		boolean secondPeakExist = false;

		baseHeights[0] = traceA[position];
		baseHeights[1] = traceT[position];
		baseHeights[2] = traceG[position];
		baseHeights[3] = traceC[position];

		int maxValue = -1, secondMaxValue = -1;
		int maxIndex = 0, secondMaxIndex = 0;

		for(int j=0;j<4;j++) {
			if(baseHeights[j] > maxValue) {
				maxValue = baseHeights[j];
				maxIndex = j;
			}
		}

		for(int j=0;j<4;j++) {
			if(j == maxIndex) continue;
			if(baseHeights[j] > secondMaxValue) {
				secondMaxValue = baseHeights[j];
				secondMaxIndex = j;
			}
		}


		if(maxValue != 0 && (secondMaxValue / (double)maxValue >= cutOff)) {
			secondPeakExist = true;


			//False secondPeak 판단로직 (조건에 맞으면 filtering --> secondPeakExist = false)
			//원칙 : 확실한 것만 filter out. 애매한건 filtering 안함.
			//중간에 에러나면 그냥 filtering 안함.
			try {

				int[] targetTrace = null;
				int direction = 0;	
				// Right upward:1, Left upward:-1, Bidirectional upward : 2, Otherwise : 0 (ex 수평 or 양쪽 다 내려감) 


				switch(secondMaxIndex) {
				case 0: targetTrace = traceA; break;
				case 1: targetTrace = traceT; break;
				case 2: targetTrace = traceG; break;
				case 3: targetTrace = traceC; break;
				}

				if(position == 0) direction = 1;
				else if(position == traceLength-1) direction = -1;
				else {
					if(targetTrace[position-1] < secondMaxValue && secondMaxValue < targetTrace[position+1]) {
						direction = 1;
					}
					else if(targetTrace[position-1] > secondMaxValue && secondMaxValue > targetTrace[position+1]) {
						direction = -1;
					}
					else if(targetTrace[position-1] > secondMaxValue && secondMaxValue < targetTrace[position+1]) {
						direction = 2;
					}
				}
				//System.out.println(String.format("%d(%c) : %d", basePosition+1, numberToBase(secondMaxIndex), direction));

				int smallIncrement = 10;
				int bigIncrement = 20;

				boolean leftPeakFound = false, rightPeakFound = false;
				//오른쪽으로일곱칸 정도는 갈 수 있어야. 
				//base가 하나 더 있다면 당연히 7칸 갈수 있음 (대략 9~12칸 간격이니까)
				//맨 끝 base의 경우 너무 짧게 increase하고 filtering 되어버리는 경우 막기 위함.
				//이정도 하면 된다. 맨끝 base가 variant인 경우 어차피 잘 없음.
				if((direction == 1 || direction ==2) && position+7 < traceLength) {	
					int rightEnd = 0;
					if(basePosition == sequenceLength-1) 
						rightEnd = traceLength-1;
					else 
						rightEnd = baseCalls[basePosition+1];

					int i = position+1;
					for(;i<=rightEnd;i++) {
						int increment = targetTrace[i] - targetTrace[i-1];
						if(i==(position+2) || i==(rightEnd-1)) { 
							if(increment<smallIncrement) break;
						}
						else if(i>position+2 && i< rightEnd-1) {
							if(increment<bigIncrement) break;
						}
					}
					//중간에 break 안하고 loop 다 돌았으면 계속 정해진 값 이상으로 증가한것임
					if(i>rightEnd) rightPeakFound = true;;
				}
				if ((direction == -1 || direction ==2) && position-7 >= 0) {
					int leftEnd = 0;
					if(basePosition == 0)
						leftEnd = 0;
					else
						leftEnd = baseCalls[basePosition-1];

					int i = position-1;
					for(;i>=leftEnd;i--) {
						int increment = targetTrace[i] - targetTrace[i+1];
						if(i==(leftEnd+1) || i==(position-2)) {
							if(increment<smallIncrement) break;
						}
						else if(i>(leftEnd+1) && i<(position-2)) {
							if(increment<bigIncrement) break;
						}
					}
					if(i<leftEnd) leftPeakFound = true;

				}
				if(direction == 1 && rightPeakFound) 
					secondPeakExist = false;
				else if(direction == -1 && leftPeakFound)
					secondPeakExist = false;
				else if(direction == 2 && rightPeakFound && leftPeakFound)
					secondPeakExist = false;



			}
			catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		return new TwoPeaks(SymbolTools.numberToBase(maxIndex), SymbolTools.numberToBase(secondMaxIndex), maxValue, secondMaxValue, secondPeakExist);
	}


	/**
	 * Replaces symbols with corresponding ambiguous symbols where second peak exist
	 * @author Young-gon Kim
	 */
	public void applyAmbiguousSymbol() {
		char firstChar = 'N', secondChar = 'N', ambiguousChar = 'N';
		StringBuffer buffer = new StringBuffer();
		for(int i=0;i<sequenceLength;i++) {
			TwoPeaks twoPeaks = getTwoPeaks(i, rootController.secondPeakCutoff);
			if(twoPeaks.secondPeakExist()) {
				firstChar = twoPeaks.getFirstBase();
				secondChar = twoPeaks.getSecondBase();
				ambiguousChar = SymbolTools.makeAmbiguousSymbol(firstChar, secondChar); 
				buffer.append(ambiguousChar);
			}
			else {	//second peak exist : false일때, twoPeaks에서 1개만 나왔을때 (최소 1개는 나옴...)
				buffer.append(twoPeaks.getFirstBase());	
			}
		}
		sequence = buffer.toString();
	}

	public int getFrontTrimPosition() {
		int scoreTrimPosition = -1;
		int ret = -1;
		final int windowSize = 5;
		int qualitySearchLength = 100;
		final int scoreCutOff = 25;
		boolean qualityPointFound = false;

		qualitySearchLength = Integer.min(qualitySearchLength,  sequenceLength-windowSize);

		//error 생기면 그냥 trimming 안함.
		try {
			//1. Q-score sliding window
			//i : 0부터 시작하는 좌표.
			int basePosition = -1;

			for(int i=0;i<qualitySearchLength;i++) {
				int sum = 0;
				double avgScore = 0;

				for(int j=0;j<windowSize;j++) {
					sum += qCalls[i+j];
				}
				avgScore = sum/(double)windowSize;

				if(avgScore >= scoreCutOff) {
					qualityPointFound = true;
					for(basePosition=i+windowSize-1; basePosition>=i; basePosition--) {
						if(qCalls[basePosition]<scoreCutOff-10)
							break;
					}
					if(basePosition == -1) 
						scoreTrimPosition = -1;
					else {
						//searchLength->i->basePosition 추적해보면 basePosition의 최대값은 sequenceLength-2임. 따라서 아래코드 error X
						scoreTrimPosition = (baseCalls[basePosition] + baseCalls[basePosition+1])/2;
						scoreTrimPosition *= traceWidth;
					}
					break;
				}
			}

			if(!qualityPointFound) {
				//일단 searchLength 까지만이라도 자르기??
				//나중에 Troubleshooting
				basePosition = (qualitySearchLength-1);
				scoreTrimPosition = (baseCalls[basePosition] + baseCalls[basePosition+1])/2;
				scoreTrimPosition *= traceWidth;
			}

			System.out.println("5' trim, qSCore based : " + (basePosition+1) + " trace : " + scoreTrimPosition);



			ret = scoreTrimPosition;

			//5' terminal에 basecall 안된 trace 늘어져 있으면 자르기.
			if((ret == -1) && (baseCalls[0]>20)) {
				ret = (baseCalls[0]-3) * traceWidth;
			}

			return ret;
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		return ret;
	}

	// @return trimming 안할거면 traceLength*traceWidth
	public int getTailTrimPosition() {
		int scoreTrimPosition = traceLength*traceWidth;
		int ret = traceLength*traceWidth;
		final int windowSize = 20;
		int qScoreSearchLength = 2000;	// 일단 무한대
		final int scoreCutOff = 25;
		boolean qualityPointFound = false;


		//error 생기면 그냥 trimming 안함.
		try {
			// Q-score sliding window

			qScoreSearchLength = Integer.min(sequenceLength-windowSize, qScoreSearchLength);

			int basePosition = sequenceLength;
			for(int i=sequenceLength-1;i>=sequenceLength-qScoreSearchLength;i--) {
				int sum = 0;
				double avgScore = 0;

				for(int j=0;j<windowSize;j++) {
					sum += qCalls[i-j];
				}
				avgScore = sum/(double)windowSize;

				if(avgScore >= scoreCutOff) {
					qualityPointFound = true;
					for(basePosition=i-windowSize+1; basePosition<=i; basePosition++) {
						if(qCalls[basePosition]<scoreCutOff-10)
							break;
					}

					if(basePosition == sequenceLength) 
						scoreTrimPosition = traceLength*traceWidth;
					else {
						scoreTrimPosition = (baseCalls[basePosition] + baseCalls[basePosition-1])/2;
						scoreTrimPosition *= traceWidth;
					}
					break;
				}
			}

			// 끝까지 quality 안좋으면
			if(!qualityPointFound) {
				basePosition = sequenceLength-qScoreSearchLength;
				scoreTrimPosition = (baseCalls[basePosition] + baseCalls[basePosition-1])/2;
				scoreTrimPosition *= traceWidth;

				//나중에 troubleshooting
			}

			System.out.println("3' trim, qSCore based : " + (sequenceLength - (basePosition+1)) + " trace : " + (traceLength*traceWidth-scoreTrimPosition));


			//ret = Integer.min(scoreTrimPosition, peakHeightPosition);
			ret = scoreTrimPosition;

			//3' terminal에 basecall 안된 trace 늘어져 있으면 자르기.
			if((ret == traceLength*traceWidth) && (baseCalls[sequenceLength-1]+20<traceLength)) {
				ret = (baseCalls[sequenceLength-1]+3) * traceWidth;
			}
			return ret;
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		return ret;
	}





	public int getDirection() {
		return direction;
	}
	public int[] getTraceA() {
		return traceA;
	}
	public int[] getTraceT() {
		return traceT;
	}
	public int[] getTraceG() {
		return traceG;
	}
	public int[] getTraceC() {
		return traceC;
	}
	public int getTraceLength() {
		return traceLength;
	}
	public String getSequence() {
		return sequence;
	}
	public int getSequenceLength() {
		return sequenceLength;
	}
	public int[] getQCalls() {
		return qCalls;
	}
	public int[] getBaseCalls() {
		return baseCalls;
	}
	public int getAlignedRegionStart() {
		return alignedRegionStart;
	}
	public void setAlignedRegionStart(int alignedRegionStart) {
		this.alignedRegionStart = alignedRegionStart;
	}
	public int getAlignedRegionEnd() {
		return alignedRegionEnd;
	}
	public void setAlignedRegionEnd(int alignedRegionEnd) {
		this.alignedRegionEnd = alignedRegionEnd;
	}

	public int[] getqCalls() {
		return qCalls;
	}
	public void setqCalls(int[] qCalls) {
		this.qCalls = qCalls;
	}
	public void setSequence(String sequence) {
		this.sequence = sequence;
	}
	public void setBaseCalls(int[] baseCalls) {
		this.baseCalls = baseCalls;
	}
	public void setSequenceLength(int sequenceLength) {
		this.sequenceLength = sequenceLength;
	}
	public void zoomIn() {
		System.out.println("ratio: " + ratio);
		ratio += 0.5;
		transformTrace();
	}

	public void zoomOut() {
		System.out.println("ratio: " + ratio);
		if(ratio>0.6) 
			ratio -= 0.5;
		transformTrace();
	}
	
	public void zoomDefault() {
		ratio = 1.0;
		transformTrace();
	}
	
	@Override
	protected Object clone() throws CloneNotSupportedException {

		return super.clone();
	}
}
