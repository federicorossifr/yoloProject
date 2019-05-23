package it.unipi.ing.mim.img.gui;

import javafx.scene.control.Button;
import java.io.File;
import java.io.IOException;

import it.unipi.ing.mim.deep.DNNExtractor;
import it.unipi.ing.mim.deep.ImgDescriptor;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

import it.unipi.ing.mim.deep.Parameters;
import it.unipi.ing.mim.img.elasticsearch.Pivots;

public class YoloGUI extends Application {
	
	private TextField humanTagsUrl = new TextField();
	private Button openButton = new Button("Search By Image");
	private ImageView img = new ImageView();
	private Button startSearch = new Button("Start Search");

	private float[] imgFeatures;
	private Pivots pivots;
	private String textFeatures = "";
	
	@Override
	public void start(Stage stage) {
	
		Label pathLabel = new Label("Insert Human Tag:");
		HBox hbox = new HBox(20,new Label(""),pathLabel, humanTagsUrl );
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Resource File");
		fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Image Files", "*.jpg"));
		
		openButton.setOnAction((ActionEvent ev) ->{
			
			File file = fileChooser.showOpenDialog(stage);
	        if (file != null) {
	        	Image i = new Image(file.toURI().toString());
	        	img.setImage(i);
	        	
	        	DNNExtractor extractor = new DNNExtractor();
				
				imgFeatures = extractor.extract(file, it.unipi.ing.mim.deep.Parameters.DEEP_LAYER);
				
				try {
					
					pivots = new Pivots(file);
					ImgDescriptor imDes = new ImgDescriptor(imgFeatures, file.getName());
					textFeatures = pivots.features2Text(imDes, it.unipi.ing.mim.deep.Parameters.TOP_K_QUERY);
					
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        }
	    });
		
		startSearch.setOnAction((ActionEvent ev)-> {
			
			
		});
		
		img.setFitWidth(200);
		img.setFitHeight(200);
		img.setPreserveRatio(true);
		HBox imgBox = new HBox(img);
		imgBox.setStyle("-fx-border-color: red; -fx-border-width: 1; -fx-border-style: dotted;");
		
		HBox hbox1 = new HBox(20,openButton, imgBox);
		VBox vbox = new VBox(40, hbox, hbox1, startSearch);
		Group root = new Group(vbox);
		Scene scene = new Scene(root, 800,800);
		
		stage.setScene(scene);
		stage.setTitle("Yolo GUI");
		stage.show();
		
	}

	
}
