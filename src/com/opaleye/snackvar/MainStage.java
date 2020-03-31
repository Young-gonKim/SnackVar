package com.opaleye.snackvar;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Title : MainStage
 * MainStage of the application
 * @author Young-gon Kim
 * 2018.5
 */
public class MainStage extends Application {
	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setX(0);
		primaryStage.setY(0);
		primaryStage.setTitle("SnackVAR Ver. " + RootController.version);
		FXMLLoader loader = new FXMLLoader(getClass().getResource("MainStage.fxml"));
		
		Parent root = loader.load();
		RootController controller = loader.getController();
		controller.setPrimaryStage(primaryStage);
		controller.makeButtons();
		Scene scene = new Scene(root);
		primaryStage.setScene(scene);
		primaryStage.show();
	}
	public static void main (String[] args) {
		launch(args);
	}
}
