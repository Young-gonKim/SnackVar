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

package com.opaleye.snackvar.settings;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import com.opaleye.snackvar.RootController;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class SettingsController implements Initializable  {
	@FXML private TextField tf_secondPeakCutoff;
	@FXML private TextField tf_gapOpenPenalty;
	@FXML private TextField tf_trimWithoutConfirm;
	@FXML private TextField tf_trainStartFileNo;
	@FXML private TextField tf_testStartFileNo;
	@FXML private TextField tf_delinsCutoff;
	
	@FXML private HBox refHBox;
	@FXML private CheckBox heteroIndelCheckBox;

	public static final String noFiltering = "No filtering";
	public static final String ruleBasedFiltering = "Rule based";
	//public static final String AIBasedFiltering = "AI based";

	//for test data generation
	private static int trainCsvCounter = 0;
	private static int testCsvCounter = 0;
	private static File baseDir = new File("../../chromatogram");
	private static File baseTrainDir = new File(baseDir, "train");
	private static File featuresDirTrain = new File(baseTrainDir, "features");
	private static File labelsDirTrain = new File(baseTrainDir, "labels");
	private static File baseTestDir = new File(baseDir, "test");
	private static File featuresDirTest = new File(baseTestDir, "features");
	private static File labelsDirTest = new File(baseTestDir, "labels");

	private RootController rootController = null;
	private Stage primaryStage;



	@Override
	public void initialize(URL location, ResourceBundle resources) {
	}

	public void setRootController(RootController rootController) {
		this.rootController = rootController;
	}

	public void setPrimaryStage(Stage primaryStage) {
		this.primaryStage = primaryStage;
	}

	public void initValues(double secondPeakCutoff, int gapOpenPenalty, int filterQualityCutoff, int trimWithoutConfirm, int delinsCutoff) {
		refHBox.getChildren().add(rootController.getOpenRefButton());

		/*
		tf_trainStartFileNo.setText(String.format("%d",  trainCsvCounter));
		tf_testStartFileNo.setText(String.format("%d",  testCsvCounter));
		 */
		tf_secondPeakCutoff.setText(String.format("%.2f",  secondPeakCutoff));
		tf_gapOpenPenalty.setText(String.format("%d",  gapOpenPenalty));
		tf_trimWithoutConfirm.setText(String.format("%d",  trimWithoutConfirm));
		tf_delinsCutoff.setText(String.format("%d", delinsCutoff));
		
		if(gapOpenPenalty >=200) 
			heteroIndelCheckBox.setSelected(true);
		else
			heteroIndelCheckBox.setSelected(false);

		heteroIndelCheckBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
		    @Override
		    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
		    	if(newValue == true) {
		    		tf_gapOpenPenalty.setText("200");
		    	}
		    	else {
		    		tf_gapOpenPenalty.setText("30");
		    	}
		    }
		});


	}


	private boolean setValues() {
		double secondPeakCutoff;
		int gapOpenPenalty;
		int trimWithoutConfirm;
		int delinsCutoff;

		try {
			secondPeakCutoff = Double.parseDouble((tf_secondPeakCutoff.getText()).replace(',',  '.'));
			
			gapOpenPenalty = Integer.parseInt(tf_gapOpenPenalty.getText());
			trimWithoutConfirm = Integer.parseInt(tf_trimWithoutConfirm.getText());
			trimWithoutConfirm = Integer.parseInt(tf_trimWithoutConfirm.getText());
			delinsCutoff = Integer.parseInt(tf_delinsCutoff.getText());

			if(secondPeakCutoff>1 || secondPeakCutoff <0)
				throw new NumberFormatException("Cutoff value should be number between 0 and 1");
			if(gapOpenPenalty <= 0)
				throw new NumberFormatException("Gap open penalty should be positive integer");

			if(trimWithoutConfirm <= 0)
				throw new NumberFormatException("Trimming without confirm value should be positive integer");

			if(delinsCutoff>10 || delinsCutoff <1)
				throw new NumberFormatException("Cutoff value should be number between 1 and 10");

			
			rootController.setProperties(secondPeakCutoff, gapOpenPenalty, trimWithoutConfirm, delinsCutoff);
		}
		catch (Exception ex) {
			rootController.popUp(ex.getMessage());
			return false;
		}
		return true;
	}

	public void handleConfirm() {
		if(setValues()) {
			if(rootController.fwdLoaded)
				rootController.trimmedFwdTrace.applyAmbiguousSymbol();
			if(rootController.revLoaded)
				rootController.trimmedRevTrace.applyAmbiguousSymbol();
			if(rootController.alignmentPerformed) {
				rootController.handleRun();
			}
			primaryStage.close();
		}
	}

	public void handleDefault() {
		tf_secondPeakCutoff.setText(String.format("%.2f",  RootController.defaultSecondPeakCutoff));
		tf_gapOpenPenalty.setText(String.format("%d",  RootController.defaultGOP));
		tf_trimWithoutConfirm.setText(String.format("%d",  RootController.defaultTrimWithoutConfirm));
		tf_delinsCutoff.setText(String.format("%d",  RootController.defaultDelinsCutoff));
	}
}
