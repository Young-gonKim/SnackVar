package com.opaleye.snackvar;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.TreeSet;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Title : HeteroController
 * FXML Controller class for Hetero.fxml
 * @author Young-gon Kim
 * 2018.7.
 */
public class HeteroController implements Initializable {
	@FXML private ScrollPane tracePane;
	@FXML private ScrollPane resultPane;
	@FXML private Button zoomInButton;
	@FXML private Button zoomOutButton;


	private Label[][] labels = null;

	private HeteroTrace heteroTrace;

	private int direction;
	private int insOrDel;
	private int indelSize;
	private char[] refSeq;
	private char[] subtractedSeq;
	private char[] subtractedSeq2;
	private String s_refSeq;
	private String s_subtractedSeq;
	private String s_subtractedSeq2;
	private String HGVS = "";
	private int indelStartGIndex;
	private int indelEndGIndex;
	
	private RootController rootController;
	private ImageView imageView;
	

	//indel 영역에 해당되어 highlight 되는 위치들.
	TreeSet<Integer> highlightSet = new TreeSet<Integer>();

	//Chromatogram 상에서 highlight 될 position들
	TreeSet<Integer> highlightRefSeq = new TreeSet<Integer>();
	TreeSet<Integer> highlightSubSeq = new TreeSet<Integer>();


	TreeMap<Integer, Integer> coordiMap = null;
	private int length;
	private Stage primaryStage;
	public void setPrimaryStage(Stage primaryStage) {
		this.primaryStage = primaryStage;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
	}


	public void setRootController(RootController rootController) {
		this.rootController = rootController;
	}
	

	
	/**
	 * Shows image of heteroTrace and hetero indel alignment 
	 */
	public void showImage() {

		java.awt.image.BufferedImage awtImage = heteroTrace.getHeteroImage(rootController.formatter, highlightRefSeq, highlightSubSeq);
		javafx.scene.image.Image fxImage = SwingFXUtils.toFXImage(awtImage, null);
		imageView = new ImageView(fxImage);
		imageView.setMouseTransparent(true);
		tracePane.setContent(imageView);

	}

	/**
	 * shows the alignment of deconvoluted trace that contain heterozygous indel   
	 */
	public void showResult() {
		
		int indelStartIndex = heteroTrace.getAlignedIndelStartIndex();
		int indelEndIndex = heteroTrace.getAlignedIndelEndIndex();
		AlignedPoint ap1 = rootController.alignedPoints.get(indelStartIndex - 1);
		AlignedPoint ap2 = rootController.alignedPoints.get(indelEndIndex - 1);
		indelStartGIndex = ap1.getGIndex();
		indelEndGIndex = ap2.getGIndex();
		
		direction = heteroTrace.getDirection();
		insOrDel = heteroTrace.getInsOrDel();
		indelSize = heteroTrace.getIndelSize();
		refSeq = heteroTrace.getRefSeq();
		subtractedSeq = heteroTrace.getSubtractedSeq();
		subtractedSeq2 = heteroTrace.getSubtractedSeq2();
		s_refSeq = new String(refSeq);
		s_subtractedSeq = new String(subtractedSeq);
		s_subtractedSeq2 = new String(subtractedSeq2);
		

		int doublePeakStartIndex = heteroTrace.getDoublePeakStartIndex(); //1부터시작

		if(direction==1) {
			coordiMap = rootController.formatter.fwdCoordinateMap;
		}
		else {
			coordiMap = rootController.formatter.revCoordinateMap;
		}


		if(direction == 1) {	//forward
			if(insOrDel == 1) {	//insertion
				String firstPart = s_refSeq.substring(0,doublePeakStartIndex-1);
				String secondPart = s_refSeq.substring(doublePeakStartIndex-1, s_refSeq.length());
				for(int i=0;i<indelSize;i++)
					firstPart = firstPart + "*";
				s_refSeq = firstPart + secondPart;
			}
			else if(insOrDel == -1) {	//deletion
				for(int i=0; i<indelSize;i++)
					s_subtractedSeq = "*" + s_subtractedSeq;
			}
			/*
			for(int i=0;i<indelStartIndex-1;i++)
				s_subtractedSeq = " " + s_subtractedSeq;
			 */
			s_subtractedSeq = s_subtractedSeq2 + s_subtractedSeq;
		}

		else if(direction == -1) {		//reverse
			if(insOrDel == -1) { //deletion	
				for(int i=0;i<indelSize;i++) {
					s_refSeq = " " + s_refSeq;
					s_subtractedSeq = s_subtractedSeq + "*";
				}

			}
			else if (insOrDel == 1){	//insertion
				String firstPart = s_refSeq.substring(0,doublePeakStartIndex);
				String secondPart = s_refSeq.substring(doublePeakStartIndex, s_refSeq.length());
				for(int i=0;i<indelSize;i++)
					firstPart = firstPart + "*";
				s_refSeq = firstPart + secondPart;

				for(int i=0; i<indelSize;i++)
					s_subtractedSeq = " " + s_subtractedSeq;

			}
			s_subtractedSeq += s_subtractedSeq2;
		}

		length = Integer.max(s_refSeq.length(), s_subtractedSeq.length());

		labels = new Label[2][length];
		GridPane gridPane = new GridPane();
		gridPane.setPrefSize(10*length, 40);

		Label firstSeqLabel = new Label("1st Sequence (= reference) : ");
		Label secondSeqLabel = new Label("2nd Sequence (subtracted) : ");

		firstSeqLabel.setPrefSize(250, 10);

		secondSeqLabel.setPrefSize(250, 10);


		gridPane.add(firstSeqLabel, 0, 0);
		gridPane.add(secondSeqLabel, 0, 1);

		String highlightColor = "#00bfff";
		//Refseq 표시
		int traceIndex = 0;
		for(int i=0;i<s_refSeq.length();i++) {
			String refSeqChar = s_refSeq.substring(i,i+1);
			labels[0][i] = new Label(refSeqChar);
			labels[0][i].getStyleClass().add("gridPane");
			labels[0][i].setPrefSize(10, 10);
			
			
			//*로 표시되는 부분도 indel 영역에 포함되므로 highlight
			if(refSeqChar.equals("*")) {
				labels[0][i].setBackground(new Background(new BackgroundFill(Color.web(highlightColor), CornerRadii.EMPTY, Insets.EMPTY)));
				highlightSet.add(i);
				//System.out.println(String.format("Highlight set added : %d",  i));
			}

			if(!(refSeqChar.equals(" ") || refSeqChar.equals("*"))) {
				traceIndex++;

				//subSeq과 일치하면 노란색
				if(i<s_subtractedSeq.length())
					if(new String("ATGC").contains(refSeqChar))	 	//ambiguous symbol이나 N일 경우 일치로 간주하지 않음
						if(refSeqChar.equals(s_subtractedSeq.substring(i,i+1))) 
							labels[0][i].setBackground(new Background(new BackgroundFill(Color.web("#FFFF5A"), CornerRadii.EMPTY, Insets.EMPTY)));

				//AlignedPoint 찾아오기 
				//Map : key, value 둘다 1부터 시작하는 index
				Integer i_index = coordiMap.get(new Integer(traceIndex));
				if(i_index!= null) {
					int index = i_index.intValue();
					AlignedPoint point = rootController.alignedPoints.get(index-1);

					//Tooltip 설정
					String tooltipText = (index) + "\nCoding DNA : " + point.getStringCIndex() + "\nBase # in gene : " + point.getGIndex() + "\n";
					Tooltip tooltip = new Tooltip(tooltipText);
					//tooltip.setOpacity(0.7);
					tooltip.setAutoHide(false);
					labels[0][i].setTooltip(tooltip);

					//Indel 영역이면 highlight
					if(point.getGIndex()>=indelStartGIndex && point.getGIndex()<=indelEndGIndex) {
						labels[0][i].setBackground(new Background(new BackgroundFill(Color.web(highlightColor), CornerRadii.EMPTY, Insets.EMPTY)));
						highlightSet.add(i);
						highlightRefSeq.add(traceIndex);
					}

				}
			}
			gridPane.add(labels[0][i],  i+1,  0);
			
		}

		//Subseq 표시
		traceIndex = 0;
		for(int i=0;i<s_subtractedSeq.length();i++) {
			String subSeqChar = s_subtractedSeq.substring(i,i+1);
			labels[1][i] = new Label(subSeqChar);
			labels[1][i].getStyleClass().add("gridPane");
			if(!(subSeqChar.equals(" ") || subSeqChar.equals("*"))) 
				traceIndex++;
				
			if(i<s_refSeq.length())
				if(new String("ATGC").contains(s_refSeq.substring(i,i+1)))  //ambiguous symbol이나 N일 경우 일치로 간주하지 않음
					if(s_refSeq.substring(i,i+1).equals(subSeqChar)) 
						labels[1][i].setBackground(new Background(new BackgroundFill(Color.web("#FFFF5A"), CornerRadii.EMPTY, Insets.EMPTY)));

			if(highlightSet.contains(i)) {
				labels[1][i].setBackground(new Background(new BackgroundFill(Color.web(highlightColor), CornerRadii.EMPTY, Insets.EMPTY)));
				if(!subSeqChar.equals("*")) highlightSubSeq.add(traceIndex);
			}
			
			gridPane.add(labels[1][i],  i+1,  1);
		}

		//Label resultLabel = new Label(HGVS);
		//gridPane.add(resultLabel, 0, 4, length,1);
		resultPane.setContent(gridPane);

		
		adjustTracePane(doublePeakStartIndex);
		adjustAlignmentPane();
		showImage();
	}

	/**
	 * Focuses the designated point on the alignment pane
	 * @param index : the point to be focused
	 */
	private void adjustAlignmentPane() {
		int index = highlightSet.first();
		if(labels==null) return;
		if(labels[0]==null) return;

		//double length = labels[0][s_refSeq.length()-1].getLayoutX();
		double length = 250+s_refSeq.length()*10;
		if(length<=1280) return;
		double coordinate = 250 +index*10;
		double hValue = (coordinate - 640.0) / (length - 1280.0);
		resultPane.setHvalue(hValue);

	}
	/**
	 * Focuses the designated index in the trace pane
	 * @param traceIndex : designated index
	 */
	
	private void adjustTracePane(int traceIndex) {
		int newLength = 0;
		int startOffset = 0;

		int[] baseCalls = heteroTrace.getBaseCalls();

		if(heteroTrace.getDirection() == 1) {
			newLength = rootController.formatter.fwdNewLength;
			startOffset = rootController.formatter.fwdStartOffset;
		}
		else {
			newLength = rootController.formatter.revNewLength;
			startOffset = rootController.formatter.revStartOffset;
		}

		newLength = heteroTrace.getTraceLength()*2;
		startOffset = 0;

		if(newLength <= 1280) return;
		if(traceIndex > baseCalls.length)
			traceIndex = baseCalls.length;

		double coordinate = startOffset + baseCalls[traceIndex-1]*2;
		double hValue = (coordinate - 640.0) / (newLength - 1280);
		tracePane.setHvalue(hValue);

	}

		/** setter for heteroTrace
	 * @param heteroTrace
	 */
	public void setHeteroTrace(HeteroTrace heteroTrace) {
		this.heteroTrace = heteroTrace;
	}

	public void handleZoomIn() {
		heteroTrace.zoomIn();
		java.awt.image.BufferedImage awtImage = heteroTrace.getHeteroImage(rootController.formatter, highlightRefSeq, highlightSubSeq);
		javafx.scene.image.Image fxImage = SwingFXUtils.toFXImage(awtImage, null);
		imageView.setImage(fxImage);
		tracePane.setContent(imageView);
		tracePane.layout();
		tracePane.setVvalue(1.0);
	}

	public void handleZoomOut() {
		heteroTrace.zoomOut();
		java.awt.image.BufferedImage awtImage = heteroTrace.getHeteroImage(rootController.formatter, highlightRefSeq, highlightSubSeq);
		javafx.scene.image.Image fxImage = SwingFXUtils.toFXImage(awtImage, null);
		imageView.setImage(fxImage);
		tracePane.setContent(imageView);
		tracePane.layout();
		tracePane.setVvalue(1.0);
	}


}
