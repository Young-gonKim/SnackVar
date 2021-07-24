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

package com.opaleye.snackvar;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.FileHandler;

import com.opaleye.snackvar.mmalignment.AlignedPair;
import com.opaleye.snackvar.mmalignment.MMAlignment;
import com.opaleye.snackvar.reference.Reference;
import com.opaleye.snackvar.reference.TVController;
import com.opaleye.snackvar.reference.TranscriptVariant;
import com.opaleye.snackvar.report.ReportController;
import com.opaleye.snackvar.report.VariantReport;
import com.opaleye.snackvar.settings.SettingsController;
import com.opaleye.snackvar.tools.AutoCompleteTextField;
import com.opaleye.snackvar.tools.SymbolTools;
import com.opaleye.snackvar.tools.TooltipDelay;
import com.opaleye.snackvar.variants.Indel;
import com.opaleye.snackvar.variants.Variant;
import com.opaleye.snackvar.variants.VariantCallerFilter;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Title : RootController
 * FXML Controller class for MainStage.fxml
 * Main class of the Ganseq application
 * @author Young-gon Kim
 *2018.5
 */
public class RootController implements Initializable {
	public static final String version = "2.4.2";
	public static final int fontSize = 13;
	public static final int defaultTrimWithoutConfirm = 35;
	public static final double defaultSecondPeakCutoff = 0.30;
	
	// 아래 두개는 case specific parameters --> case 새로 open 될때마다 reset. (handleOpenRef, handleOpenSavedRef, confirmFwdTrace, confirmRevTrace)
	//2021. 03. 07 : 원래는 handle run 할때마다 reset 했는데, 그럴경우 같은 sample로 option만 바꾸면서 re-run 할 경우 자꾸 reset 되어서. 위와같이 수정함.
	public static final int defaultGOP = 30;
	public static final int defaultDelinsCutoff = 5;


	public static final double paneWidth = 1238; 
	public static final int filterQualityCutoff = 25;

	private String lastVisitedDir="D:\\GoogleDrive\\SnackVar\\#실험데이타\\2차";
	

	/**
	 * Settings parameters
	 */
	public double secondPeakCutoff = defaultSecondPeakCutoff;
	public int gapOpenPenalty = defaultGOP;
	public int trimWithoutConfirm = defaultTrimWithoutConfirm;
	public int delinsCutoff = defaultDelinsCutoff;
	


	@FXML private ScrollPane  fwdPane, revPane, alignmentPane, newAlignmentPane;
	@FXML private Label refFileLabel, fwdTraceFileLabel, revTraceFileLabel;
	@FXML private Button fwdRemoveBtn, revRemoveBtn, removeVariant;
	@FXML private Button fwdHeteroBtn, revHeteroBtn;
	@FXML private Button fwdEditTrimBtn, revEditTrimBtn;
	@FXML private Button fwdZoomInButton, fwdZoomOutButton, revZoomInButton, revZoomOutButton;
	@FXML private TextField tf_firstNumber;
	@FXML private Label offsetLabel;
	@FXML private Label cutoffLabel;

	
	@FXML private Button btn_settings;
	//@FXML private ImageView fwdRuler, revRuler;
	@FXML private TableView<Variant> variantTable;

	@FXML private TextField goPositionText;
	
	ChangeListener<Number> cl = null;

	//variables for jpro file opening
	//@FXML private Label openFwdLabel;
	@FXML private HBox openFileHBox, leftHBox;
	private FileHandler fwdFileHandler = null, revFileHandler = null, refFileHandler = null;
	private Button openRefButton = null;
	private AutoCompleteTextField atf = null;

	public int runMode = 0;
	public int firstNumber = 1; 
	public Formatter formatter = null; 

	private String refFileName = null, fwdFileName = null, revFileName = null;


	
	private Stage primaryStage;
	public void setPrimaryStage(Stage primaryStage) {
		this.primaryStage = primaryStage;
	}


	//public static boolean AIFiltering;


	public boolean alignmentPerformed = false;
	public Vector<AlignedPoint> alignedPoints = null;

	public int startRange = 0, endRange = 0;		//range : fwd, rev 양쪽다 align 된 range
	private File fwdTraceFile, revTraceFile;
	public Reference reference;

	public GanseqTrace trimmedFwdTrace, trimmedRevTrace;
	private GanseqTrace originalFwdTrace, originalRevTrace;
	private HeteroTrace fwdHeteroTrace, revHeteroTrace;
	public boolean refLoaded = false, fwdLoaded = false, revLoaded = false;

	private GridPane gridPane = null;
	private Label[][] labels = null;
	public int fwdTraceStart = 0, fwdTraceEnd = 0;
	public int revTraceStart = 0, revTraceEnd = 0;

	TableColumn tcVariant = null;
	TableColumn tcZygosity = null;
	TableColumn tcFrequency = null;
	TableColumn tcFrom = null;
	TableColumn tcEquivalentExpressions = null;



	//WONDOW mode only
	public void makeButtons() {
		openRefButton = new Button();
		openRefButton.setOnAction((ActionEvent) -> {handleOpenRef();});
		openRefButton.setPrefHeight(23);
		openRefButton.setPrefWidth(170);
		openRefButton.setText("Open Reference File");
		//openFileHBox.getChildren().add(openRefButton);

		Button openFwdButton = new Button();
		openFwdButton.setOnAction((ActionEvent) -> {handleOpenFwdTrace();});
		openFwdButton.setPrefHeight(23);
		openFwdButton.setPrefWidth(170);
		openFwdButton.setText("Open Fwd Trace File");
		openFileHBox.getChildren().add(openFwdButton);

		Button openRevButton = new Button();
		openRevButton.setOnAction((ActionEvent) -> {handleOpenRevTrace();});
		openRevButton.setPrefHeight(23);
		openRevButton.setPrefWidth(170);
		openRevButton.setText("Open Rev Trace File");
		openFileHBox.getChildren().add(openRevButton);

	}


	/**
	 * Initializes required settings
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		File tempFile = new File(lastVisitedDir);
		if(!tempFile.exists())
			lastVisitedDir=".";
		fwdHeteroBtn.setVisible(false);
		revHeteroBtn.setVisible(false);
		fwdRemoveBtn.setVisible(false);
		revRemoveBtn.setVisible(false);
		fwdEditTrimBtn.setVisible(false);
		revEditTrimBtn.setVisible(false);

		Tooltip zoomInTooltip = new Tooltip("Zoom In");
		Tooltip zoomOutTooltip = new Tooltip("Zoom Out");
		Tooltip offsetTooltip = new Tooltip("For custom reference file");
		TooltipDelay.activateTooltipInstantly(zoomInTooltip);
		TooltipDelay.activateTooltipInstantly(zoomOutTooltip);
		TooltipDelay.activateTooltipInstantly(offsetTooltip);


		fwdZoomInButton.setTooltip(zoomInTooltip);
		fwdZoomOutButton.setTooltip(zoomOutTooltip);
		revZoomInButton.setTooltip(zoomInTooltip);
		revZoomOutButton.setTooltip(zoomOutTooltip);
		offsetLabel.setTooltip(offsetTooltip);
		cutoffLabel.setText(new Double(this.secondPeakCutoff).toString());


		atf = new AutoCompleteTextField(this);
		atf.setPrefWidth(150);
		File dir = new File("./reference");
		String[] refSeqList = dir.list();
		for(int i=0;i<refSeqList.length;i++) {
			refSeqList[i] = refSeqList[i].substring(0, refSeqList[i].length()-6);
		}

		atf.getEntries().addAll(Arrays.asList(refSeqList));
		leftHBox.getChildren().add(atf);

		/*
		Button openSavedRefButton = new Button();
		openSavedRefButton.setOnAction((ActionEvent) -> {handleOpenSavedRef();});
		openSavedRefButton.setPrefHeight(23);
		openSavedRefButton.setPrefWidth(100);
		openSavedRefButton.setText("Set as reference");
		leftHBox.getChildren().add(openSavedRefButton);
		 */
	}

	public void setProperties(double secondPeakCutoff, int gapOpenPenalty, int trimWithoutConfirm, int delinsCutoff) {
		this.secondPeakCutoff = secondPeakCutoff;
		cutoffLabel.setText(new Double(this.secondPeakCutoff).toString());
		this.gapOpenPenalty = gapOpenPenalty;
		this.trimWithoutConfirm = trimWithoutConfirm;
		this.delinsCutoff = delinsCutoff;
	}
	
	/**
	 * Jump to the input c. location
	 */
	public void handleGo() {
		if(!alignmentPerformed) {
			popUp("This function can be used only after the alinment is performed");
			return;
		}
		String goPosString = goPositionText.getText();
		if(goPosString == null || goPosString.length()==0) {
			popUp("Input the location to go (ex. 101 or c.101");
			return;
		}

		for(int i=0;i<alignedPoints.size();i++)  {
			AlignedPoint ap = alignedPoints.get(i);
			if(!(goPosString.length() >=2 && goPosString.substring(0,2).equals("c.")))
				goPosString = "c." + goPosString;
			if(ap.getStringCIndex().equals(goPosString)) {
				focus(i);
				return;
			}
		}
		popUp("There is no point wiht the designated coordinate in current alignment. Please check you have given right form of cDNA coordinate. ex. 101 or c.101");
	}
	

	
	public void handleSettings() {
		//System.out.println(fwdFileHandler.fileUploader.getUploadedFile().getAbsolutePath());
		try {
			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("settings.fxml"));
			Parent root1 = (Parent) fxmlLoader.load();
			Stage stage = new Stage();
			Image image = new Image(getClass().getResourceAsStream("snack_icon.png"));
			stage.getIcons().add(image);

			SettingsController controller = fxmlLoader.getController();
			controller.setPrimaryStage(stage);
			controller.setRootController(this);
			controller.initValues(secondPeakCutoff, gapOpenPenalty, filterQualityCutoff, trimWithoutConfirm, delinsCutoff);
			stage.setScene(new Scene(root1));
			stage.setTitle("Advanced");
			//stage.setAlwaysOnTop(true);
			stage.initOwner(primaryStage);

			stage.show();
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return;
		}
	}


	private void resetParameters() {

		alignmentPerformed = false;
		alignedPoints = null;
		gridPane = null;
		labels = null;
		alignmentPane.setContent(new Label(""));
		fwdTraceStart = 0; fwdTraceEnd = 0;
		revTraceStart = 0; revTraceEnd = 0;
		startRange = 0; 
		endRange = 0;		
		fwdHeteroTrace = null; 
		revHeteroTrace = null;
		fwdHeteroBtn.setVisible(false);
		revHeteroBtn.setVisible(false);
		variantTable.getItems().clear();
	
	}


	public void handleOpenSavedRef() {
		String fileName = atf.getText()+".fasta";
		File inputFile = new File("./reference/" + fileName);
		try {
			reference = new Reference(inputFile, Reference.FASTA);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			popUp(ex.getMessage());
			return;
		}
		
		gapOpenPenalty = defaultGOP;
		delinsCutoff = defaultDelinsCutoff;

		resetParameters();
		refLoaded = true;
		refFileLabel.setText(reference.getRefName());
	}



	/**
	 * Open and Read reference file
	 */

	public void handleOpenRef() {
		File tempFile2 = new File(lastVisitedDir);
		if(!tempFile2.exists())
			lastVisitedDir=".";

		Vector<String> refTypeList = new Vector();
		refTypeList.add("*.gb*");
		refTypeList.add("*.fasta");
		refTypeList.add("*.txt");

		FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("GenBank or FASTA or TXT", refTypeList),
				new ExtensionFilter("All Files", "*.*"));
		fileChooser.setInitialDirectory(new File(lastVisitedDir));

		File inputFile = fileChooser.showOpenDialog(primaryStage);
		if(inputFile == null) return;
		lastVisitedDir=inputFile.getParent();
		refFileName = inputFile.getName();



		try {
			//String selectedExtension = fileChooser.getSelectedExtensionFilter().getDescription();
			String selectedExtension = "";
			if(refFileName.substring(refFileName.length()-6, refFileName.length()).equals(".fasta") ||
					refFileName.substring(refFileName.length()-4, refFileName.length()).equals(".txt"))
				selectedExtension = "Fasta";
			else if(refFileName.substring(refFileName.length()-3, refFileName.length()).equals(".gb"))
				selectedExtension = "Genbank";

			if(selectedExtension.equals("Fasta")) {
				reference = new Reference(inputFile, Reference.FASTA);
				System.out.println(reference.getRefString());
			}
			else if (selectedExtension.equals("Genbank")) {
				reference = new Reference(inputFile, Reference.GenBank);

				if(reference.getTvList().size() == 0) {
					throw new Exception("No coding DNA information in Genbank file");
				}
				else if(reference.getTvList().size()==1) {		//one transcript variant from Genbank file
					setTranscriptVariant(0);
				}
				else {	//many transcript variant from Genbank file
					FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("TranscriptVariant.fxml"));
					Parent root1 = (Parent) fxmlLoader.load();
					Stage stage = new Stage();
					Image image = new Image(getClass().getResourceAsStream("snack_icon.png"));
					stage.getIcons().add(image);

					TVController controller = fxmlLoader.getController();
					controller.setPrimaryStage(stage);
					controller.setRootController(this);
					controller.init(reference.getTvList());
					stage.setScene(new Scene(root1));
					//stage.setAlwaysOnTop(true);
					stage.initOwner(primaryStage);
					stage.setTitle("Choose a Transcript Variant");
					stage.show();
				}
			}


		}
		catch (Exception ex) {
			ex.printStackTrace();
			popUp(ex.getMessage());
			return;
		}
		gapOpenPenalty = defaultGOP;
		delinsCutoff = defaultDelinsCutoff;

		resetParameters();
		refLoaded = true;
		refFileLabel.setText(refFileName);
	}


	public void setTranscriptVariant (int selectedId) {
		TranscriptVariant tv = reference.getTvList().get(selectedId);
		reference.setcDnaStart(tv.getcDnaStart());
		reference.setcDnaEnd(tv.getcDnaEnd());
		for(int i=0;i<tv.getcDnaStart().size();i++) {
			int start = tv.getcDnaStart().get(i).intValue();
			int end = tv.getcDnaEnd().get(i).intValue();
			System.out.println("(" + start + ", " + end + ")");
		}
	}

	/** 
	 * Removes reference file
	 */
	public void handleRemoveRef() {
		resetParameters();
		refFileLabel.setText("");
		reference = null;
		refLoaded = false;
	}

	/** 
	 * Open forward trace file and opens trim.fxml with that file
	 */

	public void handleFwdEditTrimming() {
		try {
			GanseqTrace newTrace = (GanseqTrace)originalFwdTrace.clone();
			popUpTrimTrace(newTrace, false);
		}
		catch(Exception ex) {
			popUp(ex.getMessage());
			ex.printStackTrace();
		}
	}

	public void handleRevEditTrimming() {
		try {
			GanseqTrace newTrace = (GanseqTrace)originalRevTrace.clone();
			popUpTrimTrace(newTrace, true);
		}
		catch(Exception ex) {
			popUp(ex.getMessage());
			ex.printStackTrace();
		}
	}


	public void popUpTrimTrace(GanseqTrace tempTrace, boolean complement) {
		try {
			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Trim.fxml"));
			Parent root1 = (Parent) fxmlLoader.load();
			Stage stage = new Stage();
			Image image = new Image(getClass().getResourceAsStream("snack_icon.png"));
			stage.getIcons().add(image);

			TrimController controller = fxmlLoader.getController();
			controller.setPrimaryStage(stage);
			controller.setTargetTrace(tempTrace, complement);
			controller.setRootController(this);
			controller.init();
			stage.setScene(new Scene(root1));
			//stage.setAlwaysOnTop(true);
			stage.initOwner(primaryStage);
			stage.initModality(Modality.WINDOW_MODAL);
			stage.setTitle("Trim sequences");
			stage.show();
			//stage.show();
		}
		catch (Exception ex) {
			ex.printStackTrace();
			popUp("Error in loading forward trace file\n" + ex.getMessage());
			return;
		}
	}

	public void handleOpenFwdTrace() {
		File tempFile2 = new File(lastVisitedDir);
		if(!tempFile2.exists())
			lastVisitedDir=".";

		FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("AB1 Files", "*.ab1"), 
				new ExtensionFilter("All Files", "*.*"));
		fileChooser.setInitialDirectory(new File(lastVisitedDir));

		File fwdTraceFile = fileChooser.showOpenDialog(primaryStage);
		if(fwdTraceFile == null) return;
		lastVisitedDir=fwdTraceFile.getParent();

		try {
			GanseqTrace tempTrace = new GanseqTrace(fwdTraceFile, this);
			if(tempTrace.getSequenceLength()<30) {
				popUp("Invalid trace file: too short sequence length(<30bp) or too poor quality of sequence");
				return;
			}
			fwdFileName = fwdTraceFile.getName();
			originalFwdTrace = (GanseqTrace)tempTrace.clone();

			//trimming 많이 안될땐 popup 안하고 아래 코드로 끝냄 
			int startTrimPosition = tempTrace.getFrontTrimPosition();
			int endTrimPosition = tempTrace.getTailTrimPosition();
			int[] bc = tempTrace.getBaseCalls();

			try {
				if(startTrimPosition < bc[trimWithoutConfirm]*GanseqTrace.traceWidth && endTrimPosition > bc[tempTrace.getSequenceLength() - trimWithoutConfirm]*GanseqTrace.traceWidth) {
					tempTrace.makeTrimmedTrace(startTrimPosition, endTrimPosition, false);
					confirmFwdTrace(tempTrace);
					return;
				}
			}
			catch(Exception ex) {}

			popUpTrimTrace(tempTrace, false);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			popUp("Error in loading a trace file\nPlease check the file format and check if the basecalling has been performed.");
			return;
		}
	}

	/**
	 * Loads the image of trimmed forward trace file
	 * @param trace : trimmed forward trace file
	 */
	public void confirmFwdTrace(GanseqTrace trimmedTrace) {
		trimmedFwdTrace = trimmedTrace;
		try {
			BufferedImage awtImage = trimmedFwdTrace.getDefaultImage();
			Image fxImage = SwingFXUtils.toFXImage(awtImage, null);
			ImageView imageView = new ImageView(fxImage);
			imageView.setMouseTransparent(true);
			fwdPane.setContent(imageView);
			fwdTraceFileLabel.setText(fwdFileName);
			fwdLoaded = true;
			fwdRemoveBtn.setVisible(true);
			fwdEditTrimBtn.setVisible(true);
			
		}
		catch(Exception ex) {
			popUp("Error in loading forward trace file\n" + ex.getMessage());
			ex.printStackTrace();
		}
		finally {

		}
		gapOpenPenalty = defaultGOP;
		delinsCutoff = defaultDelinsCutoff;

		resetParameters();
	}

	private void fwdToRev() throws Exception {
		trimmedRevTrace = (GanseqTrace)trimmedFwdTrace.clone();
		trimmedRevTrace.makeComplement();

		originalRevTrace = (GanseqTrace)originalFwdTrace.clone();
		//originalRevTrace.makeComplement();

		revFileName = new String(fwdFileName);
		revTraceFileLabel.setText(revFileName);
		revLoaded = true;
		revRemoveBtn.setVisible(true);
		revEditTrimBtn.setVisible(true);

		BufferedImage awtImage = trimmedRevTrace.getDefaultImage();
		Image fxImage = SwingFXUtils.toFXImage(awtImage, null);
		ImageView imageView = new ImageView(fxImage);
		imageView.setMouseTransparent(true);
		revPane.setContent(imageView);
		handleRemoveFwd();
	}

	private void revToFwd() throws Exception {
		trimmedFwdTrace = (GanseqTrace)trimmedRevTrace.clone();
		trimmedFwdTrace.makeComplement();

		originalFwdTrace = (GanseqTrace)originalRevTrace.clone();
		//originalFwdTrace.makeComplement();

		fwdFileName = new String(revFileName);
		fwdTraceFileLabel.setText(fwdFileName);
		fwdLoaded = true;
		fwdRemoveBtn.setVisible(true);
		fwdEditTrimBtn.setVisible(true);
		
		BufferedImage awtImage = trimmedFwdTrace.getDefaultImage();
		Image fxImage = SwingFXUtils.toFXImage(awtImage, null);
		ImageView imageView = new ImageView(fxImage);
		imageView.setMouseTransparent(true);
		fwdPane.setContent(imageView);
		handleRemoveRev();
	}

	private void swap() throws Exception {
		GanseqTrace tempTrace = (GanseqTrace)trimmedRevTrace.clone();
		trimmedRevTrace = (GanseqTrace)trimmedFwdTrace.clone();
		trimmedRevTrace.makeComplement();
		trimmedFwdTrace = tempTrace;
		trimmedFwdTrace.makeComplement();

		GanseqTrace tempTrace2 = (GanseqTrace)originalRevTrace.clone();
		originalRevTrace = (GanseqTrace)originalFwdTrace.clone();
		//originalRevTrace.makeComplement();
		originalFwdTrace = tempTrace2;
		//originalFwdTrace.makeComplement();



		String tempFileName = new String(revFileName);
		revFileName = new String(fwdFileName);
		fwdFileName = tempFileName;
		fwdTraceFileLabel.setText(fwdFileName);
		revTraceFileLabel.setText(revFileName);

		BufferedImage awtImage = trimmedFwdTrace.getDefaultImage();
		Image fxImage = SwingFXUtils.toFXImage(awtImage, null);
		ImageView imageView = new ImageView(fxImage);
		imageView.setMouseTransparent(true);
		fwdPane.setContent(imageView);

		BufferedImage awtImage2 = trimmedRevTrace.getDefaultImage();
		Image fxImage2 = SwingFXUtils.toFXImage(awtImage2, null);
		ImageView imageView2 = new ImageView(fxImage2);
		imageView2.setMouseTransparent(true);
		revPane.setContent(imageView2);
	}


	/**
	 * Remove forward trace file
	 */
	public void handleRemoveFwd() {
		resetParameters();
		fwdTraceFileLabel.setText("");
		fwdPane.setContent(new Label(""));
		fwdTraceFile = null;
		trimmedFwdTrace = null;
		fwdLoaded = false;
		fwdHeteroBtn.setVisible(false);
		fwdRemoveBtn.setVisible(false);
		fwdEditTrimBtn.setVisible(false);

		
	}

	/** 
	 * Open reverse trace file and opens trim.fxml with that file
	 */

	public void handleOpenRevTrace() {
		File tempFile2 = new File(lastVisitedDir);
		if(!tempFile2.exists())
			lastVisitedDir=".";

		FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("AB1 Files", "*.ab1"), 
				new ExtensionFilter("All Files", "*.*"));

		//fileChooser.setInitialDirectory(new File("f:/GoogleDrive/ganseq"));
		fileChooser.setInitialDirectory(new File(lastVisitedDir));
		File revTraceFile = fileChooser.showOpenDialog(primaryStage);
		if(revTraceFile == null) return;
		lastVisitedDir=revTraceFile.getParent();

		try {
			GanseqTrace tempTrace = new GanseqTrace(revTraceFile, this);
			if(tempTrace.getSequenceLength()<30) {
				popUp("Invalid trace file: too short sequence length(<30bp) or too poor quality of sequence");
				return;
			}
			revFileName = revTraceFile.getName();
			originalRevTrace = (GanseqTrace)tempTrace.clone();

			//trimming 많이 안될땐 popup 안하고 아래 코드로 끝냄
			int startTrimPosition = tempTrace.getFrontTrimPosition();
			int endTrimPosition = tempTrace.getTailTrimPosition();
			int[] bc = tempTrace.getBaseCalls();

			try {
				if(startTrimPosition < bc[trimWithoutConfirm]*GanseqTrace.traceWidth && endTrimPosition > bc[tempTrace.getSequenceLength() - trimWithoutConfirm]*GanseqTrace.traceWidth) {
					tempTrace.makeTrimmedTrace(startTrimPosition, endTrimPosition, true);
					confirmRevTrace(tempTrace);
					return;
				}
			}
			catch(Exception ex) {}

			popUpTrimTrace(tempTrace, true);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			popUp("Error in loading a trace file\nPlease check the file format and check if the basecalling has been performed.");
			return;
		}
	}




	/**
	 * Loads the image of trimmed reverse trace file
	 * @param trace : trimmed reverse trace file
	 */
	public void confirmRevTrace(GanseqTrace trimmedTrace) {
		trimmedRevTrace = trimmedTrace;

		try {
			BufferedImage awtImage = trimmedRevTrace.getDefaultImage();
			Image fxImage = SwingFXUtils.toFXImage(awtImage, null);
			ImageView imageView = new ImageView(fxImage);
			imageView.setMouseTransparent(true);
			revPane.setContent(imageView);

			//revRuler.setImage(trimmedRevTrace.getRulerImage());

			//최초에 읽어올때만 fileName setting, editTrimming후 confirm할때는 setting할필요 없음 (하면 꼬임)
			revTraceFileLabel.setText(revFileName);
			revLoaded = true;
			revRemoveBtn.setVisible(true);
			revEditTrimBtn.setVisible(true);


		}
		catch(Exception ex) {
			popUp("Error in loading reverse trace file\n" + ex.getMessage());
			ex.printStackTrace();
		}
		
		gapOpenPenalty = defaultGOP;
		delinsCutoff = defaultDelinsCutoff;

		resetParameters();
	}

	/**
	 * Remove reverse trace file
	 */
	public void handleRemoveRev() {
		resetParameters();
		revTraceFileLabel.setText("");
		revPane.setContent(new Label(""));

		revTraceFile = null;
		trimmedRevTrace = null;
		revLoaded = false;
		revHeteroBtn.setVisible(false);
		revRemoveBtn.setVisible(false);
		revEditTrimBtn.setVisible(false);
	}

	/**
	 * Activates Hetero Indel View for forward trace 
	 */
	public void handleFwdHetero() {

		try {
			if(trimmedFwdTrace == null) {
				popUp("forward trace file is not loaded!");
				return;
			}
			if(fwdHeteroTrace == null) {
				popUp("No Hetero Indel Detected");
				return;
			}
			if(alignmentPerformed == false) {
				popUp("Alignment is not performed yet!");
				return;
			}

			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Hetero.fxml"));
			Parent root1 = (Parent) fxmlLoader.load();
			Stage stage = new Stage();
			Image image = new Image(getClass().getResourceAsStream("snack_icon.png"));
			stage.getIcons().add(image);

			HeteroController controller = fxmlLoader.getController();
			controller.setPrimaryStage(stage);
			controller.setRootController(this);
			controller.setHeteroTrace(fwdHeteroTrace);
			stage.setScene(new Scene(root1)); 
			stage.setTitle("Hetero Indel View");
			//stage.setAlwaysOnTop(true);
			stage.initOwner(primaryStage);
			stage.show();

			controller.showResult();
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public WritableImage getFwdHeteroImage() {
		WritableImage ret = null; 
		try {
			if(trimmedFwdTrace == null) {
				return null;
			}
			if(fwdHeteroTrace == null) {
				return null;
			}
			if(alignmentPerformed == false) {
				return null;
			}

			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Hetero.fxml"));
			Parent root1 = (Parent) fxmlLoader.load();
			Stage stage = new Stage();
			//Image image = new Image(getClass().getResourceAsStream("snack_icon.png"));
			//stage.getIcons().add(image);

			HeteroController controller = fxmlLoader.getController();
			controller.setPrimaryStage(stage);
			controller.setRootController(this);
			controller.setHeteroTrace(fwdHeteroTrace);
			stage.setScene(new Scene(root1)); 
			//stage.setTitle("Hetero Indel View");
			//stage.setAlwaysOnTop(true);
			stage.initOwner(primaryStage);
			stage.show();
			controller.showResult();
			ret = controller.getRoot().snapshot(new SnapshotParameters(), null);
			stage.close();
			
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		return ret;
		
	}

	/**
	 * Activates Hetero Indel View for reverse trace 
	 */
	public void handleRevHetero() {
		try {
			if(trimmedRevTrace == null) {
				popUp("reverse trace file is not loaded!");
				return;
			}
			if(revHeteroTrace == null) {
				popUp("No Hetero Indel Detected");
				return;
			}
			if(alignmentPerformed == false) {
				popUp("Alignment is not performed yet!");
				return;
			}
			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Hetero.fxml"));
			Parent root1 = (Parent) fxmlLoader.load();
			Stage stage = new Stage();
			Image image = new Image(getClass().getResourceAsStream("snack_icon.png"));
			stage.getIcons().add(image);

			HeteroController controller = fxmlLoader.getController();
			controller.setPrimaryStage(stage);
			controller.setRootController(this);
			controller.setHeteroTrace(revHeteroTrace);
			stage.setScene(new Scene(root1));
			stage.setTitle("Hetero Indel View");
			//stage.setAlwaysOnTop(true);
			stage.initOwner(primaryStage);
			stage.show();
			controller.showResult();
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public WritableImage getRevHeteroImage() {
		WritableImage ret = null; 
		try {
			if(trimmedRevTrace == null) {
				return null;
			}
			if(revHeteroTrace == null) {
				return null;
			}
			if(alignmentPerformed == false) {
				return null;
			}

			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Hetero.fxml"));
			Parent root1 = (Parent) fxmlLoader.load();
			Stage stage = new Stage();
			//Image image = new Image(getClass().getResourceAsStream("snack_icon.png"));
			//stage.getIcons().add(image);

			HeteroController controller = fxmlLoader.getController();
			controller.setPrimaryStage(stage);
			controller.setRootController(this);
			controller.setHeteroTrace(revHeteroTrace);
			stage.setScene(new Scene(root1)); 
			//stage.setTitle("Hetero Indel View");
			//stage.setAlwaysOnTop(true);
			stage.initOwner(primaryStage);
			stage.show();
			controller.showResult();
			ret = controller.getRoot().snapshot(new SnapshotParameters(), null);
			stage.close();
			
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		return ret;
		
	}

	/**
	 * Shows the message with a popup
	 * @param message : message to be showen
	 */
	public void popUp (String message) {
		Stage popUpStage = new Stage(StageStyle.DECORATED);
		popUpStage.initOwner(primaryStage);
		popUpStage.setTitle("Notice");
		Parent parent;
		try {
			parent = FXMLLoader.load(getClass().getResource("popup.fxml"));
			Label messageLabel = (Label)parent.lookup("#messageLabel");


			messageLabel.setText(message);
			messageLabel.setWrapText(true);
			Button okButton = (Button) parent.lookup("#okButton");
			okButton.setOnAction(event->popUpStage.close());
			Scene scene = new Scene(parent);

			popUpStage.setScene(scene);
			popUpStage.setResizable(false);
			//dialog.show();

			popUpStage.show();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}

	}
	
	/**
	 * Shows the Terms of use popup
	 * @param message : message to be showen
	 */
	public void termsPopUp (String message) {
		Stage popUpStage = new Stage(StageStyle.DECORATED);
		popUpStage.initOwner(primaryStage);
		popUpStage.setTitle("Terms of Use");
		Parent parent;
		try {
			parent = FXMLLoader.load(getClass().getResource("Terms.fxml"));
			TextArea termsArea = (TextArea)parent.lookup("#termsArea");


			termsArea.setText(message);
			//termsArea.setWrapText(true);
			Button okButton = (Button) parent.lookup("#okButton");
			okButton.setOnAction(event->popUpStage.close());
			Scene scene = new Scene(parent);

			popUpStage.setScene(scene);
			popUpStage.setResizable(false);
			//dialog.show();

			popUpStage.show();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}

	}
	


	private boolean doAlignment() {
		//When only fwd trace is given as input
		MMAlignment mma = new MMAlignment(gapOpenPenalty);
		AlignedPair fwdAp = null, complementedFwdAp = null;
		AlignedPair revAp = null, complementedRevAp = null;
		boolean fwdReversed = false, revReversed = false;

		if(fwdLoaded == true) {
			try {
				fwdAp = mma.localAlignment(reference.getRefString(), trimmedFwdTrace.getSequence());

				int alignmentScore1 = 0;
				for(int i=0;i<fwdAp.getAlignedString1().length();i++) {
					if(fwdAp.getAlignedString1().charAt(i)==fwdAp.getAlignedString2().charAt(i)) 
						alignmentScore1++;
				}

				// complement 만들어보기. && score 계산
				complementedFwdAp = mma.localAlignment(reference.getRefString(), SymbolTools.getComplementString(trimmedFwdTrace.getSequence()));
				int alignmentScore2 = 0;
				for(int i=0;i<complementedFwdAp.getAlignedString1().length();i++) {
					if(complementedFwdAp.getAlignedString1().charAt(i)==complementedFwdAp.getAlignedString2().charAt(i)) 
						alignmentScore2++;
				}
				System.out.println(String.format("score1, score2 : %d, %d",  alignmentScore1, alignmentScore2));

				//score1이 더 높으면 원상복귀.
				if(alignmentScore1 < alignmentScore2) {
					fwdReversed = true;
				}
			}
			catch (Exception ex) {
				popUp(ex.getMessage());
				ex.printStackTrace();
				return false;
			}
		}

		if(revLoaded == true) {
			try {
				revAp = mma.localAlignment(reference.getRefString(), trimmedRevTrace.getSequence());

				int alignmentScore1 = 0;
				for(int i=0;i<revAp.getAlignedString1().length();i++) {
					if(revAp.getAlignedString1().charAt(i)==revAp.getAlignedString2().charAt(i)) 
						alignmentScore1++;
				}

				// complement 만들어보기. && score 계산
				complementedRevAp = mma.localAlignment(reference.getRefString(), SymbolTools.getComplementString(trimmedRevTrace.getSequence()));
				int alignmentScore2 = 0;
				for(int i=0;i<complementedRevAp.getAlignedString1().length();i++) {
					if(complementedRevAp.getAlignedString1().charAt(i)==complementedRevAp.getAlignedString2().charAt(i)) 
						alignmentScore2++;
				}
				System.out.println(String.format("score1, score2 : %d, %d",  alignmentScore1, alignmentScore2));

				//score1이 더 높으면 원상복귀.
				if(alignmentScore1 < alignmentScore2) {
					revReversed = true;
				}

			}

			catch (Exception ex) {
				popUp(ex.getMessage());
				ex.printStackTrace();
				return false;
			}
		}


		//반대로 들어왔을 경우 뒤집기. 에러나면 굳이 안뒤집기.
		try {
			if(revLoaded == false && fwdReversed) {
				popUp("Reversed assignment of fwd/rev trace.");
				fwdToRev();
				fwdAp = null;
				revAp = complementedFwdAp;
			}
			else if(fwdLoaded == false && revReversed) {
				popUp("Reversed assignment of fwd/rev trace.");
				revToFwd();
				revAp = null;
				fwdAp = complementedRevAp;
			}
			else if(fwdReversed == true && revReversed == true) {
				popUp("Reversed assignment of fwd/rev trace.");
				swap();
				fwdAp = complementedRevAp;
				revAp = complementedFwdAp;
			}
			else if(fwdReversed) {
				popUp("Reversed assignment of fwd trace is suspected");
			}
			else if(revReversed) {
				popUp("Reversed assignment of rev trace is suspected");
			}



		}
		catch(Exception ex) {
			popUp(ex.getMessage());
			ex.printStackTrace();
			return false;
		}

		if(fwdLoaded == true && revLoaded == false) {
			try {
				alignedPoints = formatter.format2(fwdAp, reference, trimmedFwdTrace, 1);
			}

			catch (Exception ex) {
				popUp(ex.getMessage());
				ex.printStackTrace();
				return false;
			}
		}

		//When only rev trace is given as input
		else if(fwdLoaded == false && revLoaded == true) {
			try {
				alignedPoints = formatter.format2(revAp, reference, trimmedRevTrace, -1);
			}
			catch (Exception ex) {
				popUp(ex.getMessage());
				ex.printStackTrace();
				return false;
			}
		}

		//When both of fwd trace and rev trace are given
		else  if(fwdLoaded == true && revLoaded == true) {

			try {
				alignedPoints = formatter.format3(fwdAp, revAp, reference, trimmedFwdTrace, trimmedRevTrace);
			}
			catch (NoContigException ex) {
				popUp(ex.getMessage());
				ex.printStackTrace();
				return false;
			}
			catch (Exception ex) {
				popUp(ex.getMessage());
				ex.printStackTrace();
				return false;
			}
		}
		return true;
	}

	public void handleSaveRef() {
		if(refLoaded == false) {
			return;
		}

		String source = reference.getRefString().toLowerCase();
		for(int i=0;i<reference.getcDnaStart().size();i++) {
			int from = reference.getcDnaStart().get(i)-1;
			int to = reference.getcDnaEnd().get(i)-1;
			source = source.substring(0, from) + (source.substring(from, to+1)).toUpperCase() + source.substring(to+1, source.length()) ;
		}

		File file = new File(lastVisitedDir + "/" + reference.getRefName() + ".fasta");
		FileWriter writer = null;
		BufferedWriter bWriter = null;

		try {
			writer = new FileWriter(file, true);
			bWriter = new BufferedWriter(writer);

			bWriter.write(source);
			bWriter.flush();

			System.out.println (file.getAbsolutePath() + " has been written.");

		} catch(IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if(bWriter != null) bWriter.close();
				if(writer != null) writer.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}


	/**
	 * Performs alignment, Detects variants, Shows results
	 */
	public void handleRun() {
		resetParameters();
		
		try {
			firstNumber = Integer.parseInt(tf_firstNumber.getText());
			if(firstNumber<1) throw new Exception();
		}
		catch(Exception ex) {
			popUp("first coding DNA nubmer should be a positive integer.");
			return;
		}

		formatter = new Formatter(firstNumber);

		if(refLoaded == false) {
			popUp("Reference File should be loaded before running.");
			return;
		}
		else if(fwdLoaded == false && revLoaded == false) {  
			popUp("At least one of forward trace file and reverse trace file \n should be loaded before running.");
			return;
		}

		if(!doAlignment()) return;

		setRange();
		printAlignedResult();
		alignmentPerformed = true;

		if(fwdLoaded) {
			// 새로운 좌표로 update (fwdPane, revPane)
			java.awt.image.BufferedImage awtImage = trimmedFwdTrace.getShadedImage(formatter, 0,0,0);
			javafx.scene.image.Image fxImage = SwingFXUtils.toFXImage(awtImage, null);
			ImageView imageView = new ImageView(fxImage);
			imageView.setMouseTransparent(true);
			fwdPane.setContent(imageView);
			// 시작점에 화면 align
		}
		if(revLoaded) {
			java.awt.image.BufferedImage awtImage2 = trimmedRevTrace.getShadedImage(formatter, 0,0,0);
			javafx.scene.image.Image fxImage2 = SwingFXUtils.toFXImage(awtImage2, null);
			ImageView imageView2 = new ImageView(fxImage2);
			imageView2.setMouseTransparent(true);
			revPane.setContent(imageView2);
		}

		adjustFwdRevPane(alignedPoints.get(0));

		Vector<Variant> heteroIndelList = detectHeteroIndel();
		System.out.println("heteroindel size : " + heteroIndelList.size());
		VariantCallerFilter vcf = new VariantCallerFilter(this, heteroIndelList);
		TreeSet<Variant> variantList = vcf.getVariantList();

		if(vcf.misAlignment(variantList)) {
			if(gapOpenPenalty == defaultGOP)
				gapOpenPenalty = 200;
			else if(gapOpenPenalty < 1000) {
				gapOpenPenalty += 200;
			}
			handleRun();
			return;
		}
		
		if(gapOpenPenalty > 30) 
			popUp("Hetero indel optimization mode is activated.\nHigher gap opening penalty than default value is being used.\nDeactivation is available in 'Advanced'");


		if(variantList.size()==0) popUp("No variant detected!");
		else {
			variantTable.setEditable(true);
			tcVariant = variantTable.getColumns().get(0);
			tcZygosity = variantTable.getColumns().get(1);
			tcFrequency = variantTable.getColumns().get(2);
			tcFrom = variantTable.getColumns().get(3);
			tcEquivalentExpressions = variantTable.getColumns().get(4);

			tcVariant.setCellValueFactory(new PropertyValueFactory("variantProperty"));
			tcZygosity.setCellValueFactory(new PropertyValueFactory("zygosityProperty"));
			tcFrequency.setCellValueFactory(new PropertyValueFactory("frequencyProperty"));
			tcFrom.setCellValueFactory(new PropertyValueFactory("fromProperty"));
			tcEquivalentExpressions.setCellValueFactory(new PropertyValueFactory("equivalentExpressionsProperty"));

			tcVariant.setCellFactory(TextFieldTableCell.<Variant>forTableColumn());
			tcEquivalentExpressions.setCellFactory(TextFieldTableCell.<Variant>forTableColumn());

			ObservableList<Variant> observableList= FXCollections.observableArrayList(variantList);
			observableList.sort(Variant::compareTo);
			variantTable.setItems(observableList);


			if(cl != null) 
				variantTable.getSelectionModel().selectedIndexProperty().removeListener(cl);

			cl = new ChangeListener<Number>() {
				@Override
				public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
					if(newValue.intValue()<0) return;
					Variant variant = variantTable.getItems().get(newValue.intValue());
					if(variant instanceof Indel && ((Indel) variant).getZygosity().equals("homo"))
						focus2((Indel)variant);
					else { 
						focus(variant.getAlignmentIndex()-1);
					}

				}
			};

			variantTable.getSelectionModel().selectedIndexProperty().addListener(cl);

			Scene scene = primaryStage.getScene();
			scene.setOnKeyPressed(event-> {
				if(event.getCode()==KeyCode.DELETE) handleRemoveVariant();
			});
		}
		
	}

	/**
	 * Sets the start and end range of the alignment
	 */
	private void setRange() {
		boolean fwdFound = false, revFound = false;
		for(int i=0;i<alignedPoints.size();i++) {
			AlignedPoint ap = alignedPoints.get(i);
			if(!fwdFound && ap.getFwdChar() != Formatter.gapChar) { 
				fwdTraceStart = i+1;
				trimmedFwdTrace.setAlignedRegionStart(ap.getFwdTraceIndex());
				fwdFound = true;
			}
			if(!revFound && ap.getRevChar() != Formatter.gapChar) { 
				revTraceStart = i+1;
				trimmedRevTrace.setAlignedRegionStart(ap.getRevTraceIndex());
				revFound = true;
			}
			if(revFound && fwdFound) break;
		}

		fwdFound = false; 
		revFound = false;
		for(int i=alignedPoints.size()-1;i>=0; i--) {
			AlignedPoint ap = alignedPoints.get(i);
			if(!fwdFound && ap.getFwdChar() != Formatter.gapChar) { 
				fwdTraceEnd = i+1;
				trimmedFwdTrace.setAlignedRegionEnd(ap.getFwdTraceIndex());
				fwdFound = true;
			}
			if(!revFound && ap.getRevChar() != Formatter.gapChar) { 
				revTraceEnd = i+1;
				trimmedRevTrace.setAlignedRegionEnd(ap.getRevTraceIndex());
				revFound = true;
			}
			if(revFound && fwdFound) break;
		}

		startRange = Integer.max(fwdTraceStart,revTraceStart);
		endRange = Integer.min(fwdTraceEnd, revTraceEnd);

	}


	private Vector<Variant> detectHeteroIndel() {
		Vector<Variant> heteroIndelList = new Vector<Variant>();
		Variant fwdIndel=null, revIndel = null;

		fwdHeteroTrace = null;
		revHeteroTrace = null;

		char tempFwdChar = 'N';
		int tempFwdIndex = 0;

		if(fwdLoaded) {
			fwdHeteroTrace = new HeteroTrace(trimmedFwdTrace, this);
			fwdIndel = fwdHeteroTrace.detectHeteroIndel();

			if(fwdIndel != null) {
				tempFwdChar = fwdIndel.getFwdTraceChar();
				tempFwdIndex = fwdIndel.getFwdTraceIndex();
				heteroIndelList.add(fwdIndel);
				fwdHeteroBtn.setVisible(true);
			}
			else fwdHeteroTrace = null;
		}


		if(revLoaded) {
			revHeteroTrace = new HeteroTrace(trimmedRevTrace, this);
			revIndel = revHeteroTrace.detectHeteroIndel();
			if(revIndel != null) {
				//fwd, rev 양쪽에서 같은 variant 찾을 경우, fwd, revpane 모두에서 볼 수 있게. --> 나중에 fwd꺼 지우기. range 설정한 다음 지워야 함. VariantCollerFilter.makeVariantList()
				if(fwdIndel != null) 
					if(fwdIndel.getHGVS().equals(revIndel.getHGVS())) {
						revIndel.setHitCount(2);
						revIndel.setFwdTraceChar(tempFwdChar);
						revIndel.setFwdTraceIndex(tempFwdIndex);
					}
				heteroIndelList.add(revIndel);
				revHeteroBtn.setVisible(true);
			}
			else revHeteroTrace = null;
		}
		return heteroIndelList;
	}

	/**
	 * Prints the result of alignment on the alignment pane
	 */
	private void printAlignedResult() {
		final int gridHeight = 15;
		labels = new Label[3][alignedPoints.size()];
		gridPane = new GridPane();

		Label refTitle = new Label("Reference : ");
		refTitle.getStyleClass().add("gridPane");
		//refTitle.setFont(new Font("Consolas", 14));
		refTitle.setMinSize(130,gridHeight);
		refTitle.setPrefSize(130, gridHeight);
		gridPane.add(refTitle, 0,  1);

		if(fwdLoaded) {
			Label fwdTitle = new Label("Forward   : ");
			fwdTitle.getStyleClass().add("gridPane");
			//fwdTitle.setFont(new Font("Consolas", 14));
			fwdTitle.setMinSize(130,gridHeight);
			fwdTitle.setPrefSize(130, gridHeight);
			gridPane.add(fwdTitle, 0,  2);
		}

		if(revLoaded) {
			Label revTitle = new Label("Reverse   : ");
			revTitle.getStyleClass().add("gridPane");
			//revTitle.setFont(new Font("Consolas", 14));
			revTitle.setMinSize(130,gridHeight);
			revTitle.setPrefSize(130, gridHeight);
			gridPane.add(revTitle, 0,  3);
		}

		for (int i=0;i<alignedPoints.size();i++) {
			AlignedPoint point = alignedPoints.get(i);

			//Tooltip 설정
			String tooltipText = (i+1) + "\nCoding DNA : " + point.getStringCIndex() + "\nBase # in reference : " + point.getGIndex() + "\n";

			Tooltip tooltip = new Tooltip(tooltipText);
			//tooltip.setOpacity(0.7);
			tooltip.setAutoHide(false);
			TooltipDelay.activateTooltipInstantly(tooltip);
			TooltipDelay.holdTooltip(tooltip);

			Label refLabel = new Label();
			Label fwdLabel = new Label();
			Label revLabel = new Label();
			Label discrepencyLabel = new Label();
			Label indexLabel = new Label();

			refLabel.getStyleClass().add("gridPane");
			fwdLabel.getStyleClass().add("gridPane");
			revLabel.getStyleClass().add("gridPane");
			discrepencyLabel.getStyleClass().add("gridPane");
			indexLabel.getStyleClass().add("gridPane");

			int fwdTraceIndex = point.getFwdTraceIndex();
			int revTraceIndex = point.getRevTraceIndex();

			refLabel.setTooltip(tooltip);
			discrepencyLabel.setTooltip(tooltip);
			indexLabel.setTooltip(tooltip);
			fwdLabel.setTooltip(tooltip);
			revLabel.setTooltip(tooltip);

			//Index  
			if(i%10==0 && alignedPoints.size()-i >= 5) {
				indexLabel.setText(String.valueOf(i+1));
				GridPane.setColumnSpan(indexLabel, 10);
				indexLabel.setPrefSize(100, 10);
				indexLabel.setOnMouseClicked(new ClickEventHandler(i, fwdTraceIndex, revTraceIndex, point.getFwdChar(), point.getRevChar()));
				gridPane.add(indexLabel, i+1, 0);
			}

			//Reference
			String sRefChar = Character.toString(point.getRefChar());
			if(!point.isCoding()) sRefChar = sRefChar.toLowerCase();
			refLabel.setText(sRefChar);
			refLabel.setPrefSize(10, 10);
			refLabel.setOnMouseClicked(new ClickEventHandler(i, fwdTraceIndex, revTraceIndex, point.getFwdChar(), point.getRevChar()));


			gridPane.add(refLabel,  i+1, 1);
			labels[0][i] = refLabel;

			//Forward
			if(fwdLoaded) {
				fwdLabel.setText(Character.toString(point.getFwdChar()));
				//fwdLabel.setTextFill(Color.web("#8BBCFF"));

				if(point.getFwdChar() == Formatter.gapChar || point.getFwdQuality()>=40)
					fwdLabel.setBackground(new Background(new BackgroundFill(Color.web("#FFFFFF"), CornerRadii.EMPTY, Insets.EMPTY)));
				else if(point.getFwdQuality()>=30)
					fwdLabel.setBackground(new Background(new BackgroundFill(Color.web("#FFFFC6"), CornerRadii.EMPTY, Insets.EMPTY)));
				else if(point.getFwdQuality()>=20)
					fwdLabel.setBackground(new Background(new BackgroundFill(Color.web("#FFFF5A"), CornerRadii.EMPTY, Insets.EMPTY)));
				else if(point.getFwdQuality()>=10)
					fwdLabel.setBackground(new Background(new BackgroundFill(Color.web("#FFBB00"), CornerRadii.EMPTY, Insets.EMPTY)));
				else 
					fwdLabel.setBackground(new Background(new BackgroundFill(Color.web("#FFBB00"), CornerRadii.EMPTY, Insets.EMPTY)));

				fwdLabel.setPrefSize(10, 10);
				//System.out.println("forward trace index : " + point.getFwdTraceIndex());
				fwdLabel.setOnMouseClicked(new ClickEventHandler(i, fwdTraceIndex, revTraceIndex, point.getFwdChar(), point.getRevChar()));
				gridPane.add(fwdLabel,  i+1, 2);
				labels[1][i] = fwdLabel;
			}

			//Reverse
			if(revLoaded) {
				revLabel.setText(Character.toString(point.getRevChar()));
				if(point.getRevChar() == Formatter.gapChar || point.getRevQuality()>=40)
					revLabel.setBackground(new Background(new BackgroundFill(Color.web("#FFFFFF"), CornerRadii.EMPTY, Insets.EMPTY)));
				else if(point.getRevQuality()>=30)
					revLabel.setBackground(new Background(new BackgroundFill(Color.web("#FFFFC6"), CornerRadii.EMPTY, Insets.EMPTY)));
				else if(point.getRevQuality()>=20)
					revLabel.setBackground(new Background(new BackgroundFill(Color.web("#FFFF5A"), CornerRadii.EMPTY, Insets.EMPTY)));
				else if(point.getRevQuality()>=10)
					revLabel.setBackground(new Background(new BackgroundFill(Color.web("#FFBB00"), CornerRadii.EMPTY, Insets.EMPTY)));
				else 
					revLabel.setBackground(new Background(new BackgroundFill(Color.web("#FFBB00"), CornerRadii.EMPTY, Insets.EMPTY)));

				revLabel.setPrefSize(10, 10);
				revLabel.setOnMouseClicked(new ClickEventHandler(i, fwdTraceIndex, revTraceIndex, point.getFwdChar(), point.getRevChar()));
				gridPane.add(revLabel,  i+1, 3);
				labels[2][i] = revLabel;
			}

			//Discrepency
			discrepencyLabel.setText(Character.toString(point.getDiscrepency()));
			discrepencyLabel.setPrefSize(10, 10);
			discrepencyLabel.setOnMouseClicked(new ClickEventHandler(i, fwdTraceIndex, revTraceIndex, point.getFwdChar(), point.getRevChar()));
			gridPane.add(discrepencyLabel,  i+1, 4);
		}

		alignmentPane.setContent(gridPane);
	}

	/**
	 * Focuses the designated point on the alignment pane
	 * @param index : the point to be focused
	 */
	private void adjustAlignmentPane(int index) {
		if(labels==null) return;
		if(labels[0]==null) return;

		double length = labels[0][labels[0].length-1].getLayoutX();
		if(length<=1280) return;
		double coordinate = labels[0][index].getLayoutX();
		double hValue = (coordinate - 640.0) / (length - 1280.0);
		alignmentPane.setHvalue(hValue);

	}

	private void adjustFwdRevPane(AlignedPoint ap) {
		double fwdCoordinate=0, revCoordinate=0;
		double hValue=0;

		//System.out.println(String.format("fwdImageLength : %d, revImageLength : %d", formatter.fwdNewLength, formatter.revNewLength));
		//System.out.println(String.format("fwdTraceIndex : %d, revTraceIndex : %d", ap.getFwdTraceIndex(), ap.getRevTraceIndex()));

		if(fwdLoaded) {
			fwdCoordinate = formatter.fwdStartOffset + trimmedFwdTrace.getBaseCalls()[ap.getFwdTraceIndex()-1]*GanseqTrace.traceWidth;
		}

		if(revLoaded) {
			revCoordinate = formatter.revStartOffset + trimmedRevTrace.getBaseCalls()[ap.getRevTraceIndex()-1]*GanseqTrace.traceWidth;
		}
		System.out.println(String.format("fwdCoordinate : %f, revCoordinate : %f", fwdCoordinate, revCoordinate));

		if(fwdLoaded && revLoaded) {

			// 양쪽끝 튀어나온부분 처리.
			if(ap.getFwdTraceIndex() == 1 || ap.getRevTraceIndex() == 1) {
				double min = Double.min(fwdCoordinate, revCoordinate);
				fwdCoordinate = min;
				revCoordinate = min;
			}

			if(ap.getFwdTraceIndex() > trimmedFwdTrace.getSequenceLength() || ap.getRevTraceIndex() > trimmedRevTrace.getSequenceLength()) {
				double max = Double.max(fwdCoordinate, revCoordinate);
				fwdCoordinate = max;
				revCoordinate = max;
			}
		}

		if(fwdLoaded) {
			hValue = (fwdCoordinate - paneWidth/2) / (formatter.fwdNewLength - paneWidth);
			if(formatter.fwdNewLength > paneWidth)
				fwdPane.setHvalue(hValue);
		}

		if(revLoaded) {
			hValue = (revCoordinate - paneWidth/2) / (formatter.revNewLength - paneWidth);
			if(formatter.revNewLength > paneWidth)
				revPane.setHvalue(hValue);
		}
	}


	/**
	 * Focuses on the designated points (Alignment pane, forward trace pane, reverse trace pane)
	 * @param selectedAlignmentPos : position to be focused on the alignment pane
	 */
	public void focus(int selectedAlignmentPos) {
		//selectedAlignmentPos : 이것만 0부터 시작하는 index
		//selectedFwdPos, selectedRevPos : 1부터 시작하는 index
		
		AlignedPoint ap = alignedPoints.get(selectedAlignmentPos);
		char fwdChar = Formatter.gapChar;
		char revChar = Formatter.gapChar;
		int selectedFwdPos = 0;
		int selectedRevPos = 0;
		
		if(fwdLoaded) {
			selectedFwdPos = ap.getFwdTraceIndex();
			fwdChar = ap.getFwdChar();
		}
		if(revLoaded) {
			selectedRevPos = ap.getRevTraceIndex();
			revChar = ap.getRevChar();
		}

		
		boolean fwdGap = (fwdChar == Formatter.gapChar); 
		boolean revGap = (revChar == Formatter.gapChar);

		for(int i=0; i<alignedPoints.size();i++) {
			Label boxedLabel = labels[0][i];
			if(boxedLabel == null) continue;
			if(i==selectedAlignmentPos) {
				boxedLabel.setBorder(new Border(new BorderStroke(Color.BLUE, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(2))));
				adjustAlignmentPane(i);
			}
			else {
				boxedLabel.setBorder(Border.EMPTY);
			}
		}
		if(fwdLoaded) {
			for(int i=0; i<alignedPoints.size();i++) {
				Label boxedLabel = labels[1][i];
				if(boxedLabel == null) continue;
				if(i==selectedAlignmentPos) {
					boxedLabel.setBorder(new Border(new BorderStroke(Color.BLUE, 
							BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(2))));
				}
				else {
					boxedLabel.setBorder(Border.EMPTY);
				}
			}

			int tempFwdPos = selectedFwdPos;

			BufferedImage awtImage = null;
			if(fwdGap == true) awtImage = trimmedFwdTrace.getShadedImage(formatter, 0,0,0);
			else awtImage = trimmedFwdTrace.getShadedImage(formatter, 1, tempFwdPos-1, tempFwdPos-1);

			javafx.scene.image.Image fxImage = SwingFXUtils.toFXImage(awtImage, null);
			ImageView imageView = new ImageView(fxImage);
			imageView.setMouseTransparent(true);
			fwdPane.setContent(imageView);

		}
		if(revLoaded) {
			for(int i=0; i<alignedPoints.size();i++) {
				Label boxedLabel = labels[2][i];
				if(boxedLabel == null) continue;
				if(i==selectedAlignmentPos) {
					boxedLabel.setBorder(new Border(new BorderStroke(Color.BLUE, 
							BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(2))));
				}
				else {
					boxedLabel.setBorder(Border.EMPTY);
				}
			}

			int tempRevPos = selectedRevPos;
			BufferedImage awtImage2 = null;
			if(revGap == true) awtImage2 = trimmedRevTrace.getShadedImage(formatter, 0,0,0);
			else awtImage2 = trimmedRevTrace.getShadedImage(formatter, 1, tempRevPos-1, tempRevPos-1);
			javafx.scene.image.Image fxImage2 = SwingFXUtils.toFXImage(awtImage2, null);
			ImageView imageView2 = new ImageView(fxImage2);
			imageView2.setMouseTransparent(true);
			revPane.setContent(imageView2);


		}
		adjustFwdRevPane(alignedPoints.get(selectedAlignmentPos));
	}


	/**
	 * Focus method for Homo deletion variants (highlight range)
	 * Homo insertion : When test data is available
	 * Will be finished Later
	 * @param indel 
	 */
	public void focus2(Indel indel) {
		//다 1부터 시작하는 좌표
		System.out.println("fwd loaded : " + fwdLoaded);
		System.out.println("rev loaded : " + revLoaded);
		int startAlignmentPos=0, endAlignmentPos=0;	
		int startFwdTracePos=0, endFwdTracePos=0;
		int startRevTracePos=0, endRevTracePos=0;

		AlignedPoint ap = null;
		if(indel.getType() == Indel.duplication) {
			startAlignmentPos = indel.getAlignmentIndex();
			ap = alignedPoints.get(startAlignmentPos-1);
		}

		else {
			if(indel.getAlignmentIndex() > 1) 	//이미 맨 왼쪽 아니라면 한칸 왼쪽의 점 선택
				startAlignmentPos = indel.getAlignmentIndex() - 1;
			else
				startAlignmentPos = indel.getAlignmentIndex();
			ap = alignedPoints.get(startAlignmentPos-1);
		}


		startFwdTracePos =  ap.getFwdTraceIndex();
		startRevTracePos = ap.getRevTraceIndex();

		int counter = 0;
		AlignedPoint ap2 = null;

		int endOffset = 0;
		if(indel.getType()==Indel.deletion || indel.getType() == Indel.duplication)
			endOffset = 1;


		System.out.println(String.format("g1 : %d, g2 : %d",  indel.getgIndex(), indel.getgIndex2()));

		while(indel.getAlignmentIndex()-1+counter < alignedPoints.size()) {
			ap2 = alignedPoints.get(indel.getAlignmentIndex()-1+counter);
			if(ap2.getGIndex() == indel.getgIndex2()+endOffset) {
				counter++;
				break;
			}
			counter++;
		}
		counter--;


		endAlignmentPos =  indel.getAlignmentIndex()+counter;
		endFwdTracePos = ap2.getFwdTraceIndex();
		endRevTracePos = ap2.getRevTraceIndex();

		if(indel.getType() == Indel.duplication) {
			endAlignmentPos--;
			endFwdTracePos--;
			endRevTracePos--;
		}


		adjustAlignmentPane(startAlignmentPos-1);
		for(int i=0; i<alignedPoints.size();i++) {
			Label boxedLabel = labels[0][i];
			if(boxedLabel == null) continue;
			if(i >= startAlignmentPos-1 && i<= endAlignmentPos-1) {
				boxedLabel.setBorder(new Border(new BorderStroke(Color.BLUE, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(2))));
			}
			else {
				boxedLabel.setBorder(Border.EMPTY);
			}
		}
		if(fwdLoaded) {
			for(int i=0; i<alignedPoints.size();i++) {
				Label boxedLabel = labels[1][i];
				if(boxedLabel == null) continue;
				if(i >= startAlignmentPos-1 && i<= endAlignmentPos-1 && (i+1) >= fwdTraceStart && (i+1) <= fwdTraceEnd) {
					boxedLabel.setBorder(new Border(new BorderStroke(Color.BLUE, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(2))));
				}
				else {
					boxedLabel.setBorder(Border.EMPTY);
				}
			}


			int colorStart = Integer.max(0, startFwdTracePos-1);
			int colorEnd = Integer.max(0, endFwdTracePos-1);

			java.awt.image.BufferedImage awtImage = trimmedFwdTrace.getShadedImage(formatter, 2, colorStart, colorEnd);
			javafx.scene.image.Image fxImage = SwingFXUtils.toFXImage(awtImage, null);
			ImageView imageView = new ImageView(fxImage);
			imageView.setMouseTransparent(true);
			fwdPane.setContent(imageView);
		}
		if(revLoaded) {
			for(int i=0; i<alignedPoints.size();i++) {
				Label boxedLabel = labels[2][i];
				if(boxedLabel == null) continue;
				if(i >= startAlignmentPos-1 && i<= endAlignmentPos-1 && (i+1) >= revTraceStart && (i+1) <= revTraceEnd) {
					boxedLabel.setBorder(new Border(new BorderStroke(Color.BLUE, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(2))));
				}
				else {
					boxedLabel.setBorder(Border.EMPTY);
				}
			}

			int colorStart = Integer.max(0, startRevTracePos-1);
			int colorEnd = Integer.max(0, endRevTracePos-1);

			java.awt.image.BufferedImage awtImage2 = trimmedRevTrace.getShadedImage(formatter, 2, colorStart, colorEnd);
			javafx.scene.image.Image fxImage2 = SwingFXUtils.toFXImage(awtImage2, null);
			ImageView imageView2 = new ImageView(fxImage2);
			imageView2.setMouseTransparent(true);
			revPane.setContent(imageView2);
		}
		adjustFwdRevPane(alignedPoints.get(startAlignmentPos));
	}

	/**
	 * Title : ClickEventHandler
	 * Click event handler for focusing
	 * @author Young-gon Kim
	 */
	class ClickEventHandler implements EventHandler<MouseEvent> {
		private int selectedAlignmentPos = 0, selectedFwdPos = 0, selectedRevPos = 0;
		char fwdChar, revChar;
		public ClickEventHandler(int selectedAlignmentPos, int selectedFwdPos, int selectedRevPos, char fwdChar, char revChar) {
			super();
			this.selectedAlignmentPos = selectedAlignmentPos;
			this.selectedFwdPos = selectedFwdPos;
			this.selectedRevPos = selectedRevPos;
			this.fwdChar = fwdChar;
			this.revChar = revChar;
		}

		@Override
		public void handle(MouseEvent t) {
			focus(selectedAlignmentPos);
		}
	}



	/**
	 * Handler for remove button
	 */
	public void handleRemoveVariant() {
		//if(!variantListViewFocused) return;
		int index = variantTable.getSelectionModel().getSelectedIndex();
		System.out.println(String.format("remove index : %d",  index));
		if(index == -1) return;
		int newSelectedIdx = (index == variantTable.getItems().size() - 1)
				? index - 1
						: index;
		//o_variantList.remove(index);
		//v_variantList.remove(index);
		variantTable.getItems().remove(index);

		variantTable.getSelectionModel().select(newSelectedIdx); // 지워지면 그 위에꺼 자동으로 가리키니까 그냥 그 자리에 있도록

		if(index == 0 && variantTable.getItems().size()>0) {	//맨위에꺼 지우면 그위에꺼 자동으로 못 가리킴. changeListener 호출 안함 --> 강제로  focus
			Variant variant = variantTable.getItems().get(0);
			if(variant instanceof Indel && ((Indel) variant).getZygosity().equals("homo"))
				focus2((Indel)variant);
			else 
				focus(variant.getAlignmentIndex()-1);
		}
	}


	/**
	 * Getters for member variables
	 */

	public void handleFwdZoomIn() {
		if(fwdLoaded) {
			trimmedFwdTrace.zoomIn();
			BufferedImage awtImage = trimmedFwdTrace.getDefaultImage();
			Image fxImage = SwingFXUtils.toFXImage(awtImage, null);
			ImageView imageView = new ImageView(fxImage);
			imageView.setMouseTransparent(true);
			fwdPane.setContent(imageView);
			fwdPane.layout();
			fwdPane.setVvalue(1.0);
		}
	}
	public void handleFwdZoomOut() {
		if(fwdLoaded) {
			trimmedFwdTrace.zoomOut();
			BufferedImage awtImage = trimmedFwdTrace.getDefaultImage();
			Image fxImage = SwingFXUtils.toFXImage(awtImage, null);
			ImageView imageView = new ImageView(fxImage);
			imageView.setMouseTransparent(true);
			fwdPane.setContent(imageView);
			fwdPane.layout();
			fwdPane.setVvalue(1.0);
		}
	}
	public void handleRevZoomIn() {
		if(revLoaded) {
			trimmedRevTrace.zoomIn();
			BufferedImage awtImage = trimmedRevTrace.getDefaultImage();
			Image fxImage = SwingFXUtils.toFXImage(awtImage, null);
			ImageView imageView = new ImageView(fxImage);
			imageView.setMouseTransparent(true);
			revPane.setContent(imageView);
			revPane.layout();
			revPane.setVvalue(1.0);
		}
	}
	public void handleRevZoomOut() {
		if(revLoaded) {
			trimmedRevTrace.zoomOut();
			BufferedImage awtImage = trimmedRevTrace.getDefaultImage();
			Image fxImage = SwingFXUtils.toFXImage(awtImage, null);
			ImageView imageView = new ImageView(fxImage);
			imageView.setMouseTransparent(true);
			revPane.setContent(imageView);
			revPane.layout();
			revPane.setVvalue(1.0);
		}
	}
	
	public void handleTermsOfUse() {
		String text = "";
		try (BufferedReader reader = new BufferedReader(new FileReader("Terms_of_use.txt"))) {
			String temp; 
			while((temp=reader.readLine())!=null)
				text+=temp+'\n';
		}
		catch (Exception ex) {
			ex.printStackTrace();
			popUp("Missing Terms_of_use.txt file");
		}
		termsPopUp(text);
	}
	
	public void handleGenerateReport() {
		if(variantTable.getItems().size() <= 0) {
			popUp("No variant to report!");
			return;
		}
		
		ArrayList<VariantReport> variantReportList = new ArrayList<VariantReport>();
		int originalIndex = variantTable.getSelectionModel().getSelectedIndex();
		
		for(int i=0;i<variantTable.getItems().size();i++) {
			Variant variant = variantTable.getItems().get(i);
			
			
			variantTable.getSelectionModel().select(i);
			
			String description = variant.getVariantProperty() + ", " + variant.getZygosityProperty();
			ArrayList<String> titleList = new ArrayList<String>();
			ArrayList<WritableImage> imageList = new ArrayList<WritableImage>();
			
			//titleList, imageList 만들기
			titleList.add("Alignment");
			imageList.add(alignmentPane.snapshot(new SnapshotParameters(), null));
			
			String tempTitle = "Forward Trace";
			if(fwdTraceFileLabel != null && fwdTraceFileLabel.getText() != null) 
				tempTitle += (" : " + fwdTraceFileLabel.getText());
			titleList.add(tempTitle);

			imageList.add(fwdPane.snapshot(new SnapshotParameters(), null));

			tempTitle = "Reverse Trace";
			if(revTraceFileLabel != null && revTraceFileLabel.getText() != null) 
				tempTitle += (" : " + revTraceFileLabel.getText());
			titleList.add(tempTitle);

			imageList.add(revPane.snapshot(new SnapshotParameters(), null));

			//hetero indel은 type 1, 나머지는 type 0
			int type = 0;
			if(variant instanceof Indel && ((Indel) variant).getZygosity().equals("hetero")) {
				type = 1;
				if(variant.getHitCount()==2) {
					tempTitle = "Hetero Indel View (Forward)";
					if(fwdTraceFileLabel != null && fwdTraceFileLabel.getText() != null) 
						tempTitle += (" : " + fwdTraceFileLabel.getText());
					titleList.add(tempTitle);
					
					imageList.add(getFwdHeteroImage());

					tempTitle = "Hetero Indel View (Reverse)";
					if(revTraceFileLabel != null && revTraceFileLabel.getText() != null) 
						tempTitle += (" : " + revTraceFileLabel.getText());
					titleList.add(tempTitle);
					
					imageList.add(getRevHeteroImage());
				}
				else if(variant.getDirection() == GanseqTrace.FORWARD) {
					tempTitle = "Hetero Indel View (Forward)";
					if(fwdTraceFileLabel != null && fwdTraceFileLabel.getText() != null) 
						tempTitle += (" : " + fwdTraceFileLabel.getText());
					titleList.add(tempTitle);
					
					imageList.add(getFwdHeteroImage());
				}
				else if (variant.getDirection() == GanseqTrace.REVERSE) {
					tempTitle = "Hetero Indel View (Reverse)";
					if(revTraceFileLabel != null && revTraceFileLabel.getText() != null) 
						tempTitle += (" : " + revTraceFileLabel.getText());
					titleList.add(tempTitle);
					
					imageList.add(getRevHeteroImage());
				}
			}
			

			VariantReport vr = new VariantReport(description, titleList, imageList, type);
			variantReportList.add(vr);
			
		}
		variantTable.getSelectionModel().select(originalIndex);
		
		try {
			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("report.fxml"));
			Parent root1 = (Parent) fxmlLoader.load();
			Stage stage = new Stage();
			Image image = new Image(getClass().getResourceAsStream("snack_icon.png"));
			stage.getIcons().add(image);
			ReportController controller = fxmlLoader.getController();
			controller.setPrimaryStage(stage);
			controller.setRootController(this);
			controller.setRefFileName(refFileLabel.getText());
			controller.setVariantReportList(variantReportList);
			stage.setScene(new Scene(root1));
			stage.setTitle("SnackVar Report");
			//stage.setAlwaysOnTop(true);
			stage.initOwner(primaryStage);

			stage.show();
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return;
		}
		
	}
	

	public void setRunMode(int runMode) {
		this.runMode = runMode;
	}


	public FileHandler getRefFileHandler() {
		return refFileHandler;
	}



	public Button getOpenRefButton() {
		return openRefButton;
	}

}
