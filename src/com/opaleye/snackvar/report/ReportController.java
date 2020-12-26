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

package com.opaleye.snackvar.report;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;

import com.opaleye.snackvar.RootController;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ReportController implements Initializable  {
	@FXML private ScrollPane scrollPane;
	@FXML private VBox vBox, outerVBox;

	private RootController rootController = null;
	private Stage primaryStage;

	private final int imageWidth = 1080;

	private ArrayList<VBox> pages;


	@Override
	public void initialize(URL location, ResourceBundle resources) {
		//scrollPane.setContent(vbox);
	}

	public void setRootController(RootController rootController) {
		this.rootController = rootController;
	}

	public void setPrimaryStage(Stage primaryStage) {
		this.primaryStage = primaryStage;
	}

	public void handlePrint() {

		try {
			//Printer printer = Printer.getDefaultPrinter();
			PrinterJob printJob = PrinterJob.createPrinterJob();
			PageLayout pageLayout = printJob.getJobSettings().getPageLayout();
			//PageLayout pageLayout = printer.createPageLayout(Paper.A4, PageOrientation.PORTRAIT, 50,50,60,50 );   
			printJob.getJobSettings().setPageLayout(pageLayout);
			//System.out.println(String.format("left : %f,  right : %f", pageLayout.getLeftMargin(), pageLayout.getRightMargin()));
			//PageLayout pageLayout = printer.createPageLayout(Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT);

			if(printJob != null){
				printJob.showPrintDialog(primaryStage); 


				for(int i=0;i<pages.size();i++) {
					WritableImage wi = (pages.get(i)).snapshot(new SnapshotParameters(), null);
					ByteArrayOutputStream  byteOutput = new ByteArrayOutputStream();

					ImageIO.write( SwingFXUtils.fromFXImage(wi, null ), "png", byteOutput );
					ByteArrayInputStream  byteInput = new ByteArrayInputStream(byteOutput.toByteArray());

					Image image = new Image (byteInput);
					ImageView iv = new ImageView(image);
					iv.setPreserveRatio(true);
					iv.setFitWidth(pageLayout.getPrintableWidth());
					printJob.printPage(iv);

				}
				printJob.endJob();
			}

		}
		catch(Exception ex) {
			ex.printStackTrace();
		}



	}

	public void setVariantReportList(ArrayList<VariantReport> variantReportList) {
		pages = new ArrayList<VBox>();
		
		//first page
		//VBox currentPage = new VBox(vBox);
		VBox currentPage = vBox;
		pages.add(currentPage);

		try {
			
			//한페이지에 variant 2개씩, hetero indel은 1개만  (Logic confirmed)
			int pageVariantCnt = 0;
			for(int i=0;i<variantReportList.size();i++) {
				VariantReport variantReport = variantReportList.get(i);
				
				if(variantReport.getType()==1) 
					pageVariantCnt +=2;
				else 
					pageVariantCnt++;

				//System.out.println("InPage count : " + pageVariantCnt);
				if(pageVariantCnt > 2) {	//new Page
					currentPage = new VBox();
					outerVBox.getChildren().add(currentPage);
					pages.add(currentPage);

					if(variantReport.getType()==1) 
						pageVariantCnt = 2;
					else 
						pageVariantCnt = 1;
				}

				//variant description text box로 만들기
				currentPage.getChildren().add(new Label("Variant "+ (i+1)));
				//currentPage.getChildren().add(new Label("Variant "+ (i+1)));

				HBox descHbox = new HBox();
				descHbox.getChildren().add(new Label("Description : "));
				TextField descTextField = new TextField(variantReport.getVariantDescription());
				descTextField.setPrefWidth(400);
				descHbox.getChildren().add(descTextField);
				currentPage.getChildren().add(descHbox);
				//currentPage.getChildren().add(descHbox);

				//alignment pane, fwd, rev pane, hetero indel view (if applicable)
				int length = variantReport.getTitleList().size();
				ArrayList<String> titleList = variantReport.getTitleList();
				ArrayList<WritableImage> imageList = variantReport.getImageList();
				for(int j=0;j<length;j++) {
					Label label = new Label (titleList.get(j));
					currentPage.getChildren().add(label);
					//currentPage.getChildren().add(label);

					WritableImage wi = imageList.get(j);
					ByteArrayOutputStream  byteOutput = new ByteArrayOutputStream();
					ImageIO.write( SwingFXUtils.fromFXImage(wi, null ), "png", byteOutput );
					ByteArrayInputStream  byteInput = new ByteArrayInputStream(byteOutput.toByteArray());
					Image image = new Image (byteInput);
					ImageView iv = new ImageView(image);
					iv.setPreserveRatio(true);
					iv.setFitWidth(imageWidth);

					currentPage.getChildren().add(iv);
					currentPage.getChildren().add(new Label("   "));
				}

				currentPage.getChildren().add(new Label("   "));
				currentPage.getChildren().add(new Label("   "));
			}


		}
		catch(Exception ex) {
			ex.printStackTrace();
		}


	}





}
