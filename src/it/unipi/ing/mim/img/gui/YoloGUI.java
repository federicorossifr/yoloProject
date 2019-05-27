package it.unipi.ing.mim.img.gui;

import javafx.scene.control.Button;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.queryparser.classic.ParseException;

import it.unipi.ing.mim.deep.DNNExtractor;
import it.unipi.ing.mim.deep.ImageUtils;
import it.unipi.ing.mim.deep.ImgDescriptor;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

import it.unipi.ing.mim.deep.Parameters;
import it.unipi.ing.mim.img.elasticsearch.ElasticImgSearching;
import it.unipi.ing.mim.img.elasticsearch.Pivots;

public class YoloGUI extends Application {
	
	private TextField humanTags = new TextField();
	private Button openButton = new Button("Open an image");
	private ImageView img = new ImageView();
	private Button startSearch = new Button("Start Search");
	private GridPane imageResults = new GridPane();
	private ScrollPane scroll = new ScrollPane();
		
	private float[] imgFeatures;
	private File openedImage;
	
	private ElasticImgSearching eSearch; 
	
	@Override
	public void start(Stage stage) {
	
		Label pathLabel = new Label("Insert Human Tag:");
		HBox hbox = new HBox(20,new Label(""),pathLabel, humanTags );
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Resource File");
		fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Image Files", "*.jpg"));
		
		try {
			
			eSearch = new ElasticImgSearching(it.unipi.ing.mim.deep.Parameters.PIVOTS_FILE, it.unipi.ing.mim.deep.Parameters.TOP_K_QUERY);
		
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		openButton.setOnAction((ActionEvent ev) ->{
			
			openedImage = fileChooser.showOpenDialog(stage);
	        if (openedImage != null) {
	
	        	Image i = new Image(openedImage.toURI().toString());
	        	img.setImage(i);
	        	
	        }
	    });
		
		startSearch.setOnAction((ActionEvent ev)-> {
			
			List<ImgDescriptor> searched = null;
			ArrayList<Image> imageTemp = new ArrayList<Image>();
			imageResults.getChildren().clear();
			
			if(!humanTags.getText().equals("")) {
				searched = tagSearch(humanTags.getText());
				
			}else if(img.getImage() != null) {
				searched = imageSearch(openedImage);
			}
			
			for(ImgDescriptor i : searched) {
				try {
					imageTemp.add(ImageUtils.getDrawable(i));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			 int cols=6, colCnt = 0, rowCnt = 0;
			 for (int i=0; i<imageTemp.size(); i++) {
			    	ImageView imgIn = new ImageView(imageTemp.get(i));
			    	imgIn.setFitHeight(200);
			    	imgIn.setFitWidth(200);
			    	imgIn.setPreserveRatio(true);
			        imageResults.add(imgIn, colCnt, rowCnt);
			        colCnt++;
			        if (colCnt>cols) {
			            rowCnt++;
			            colCnt=0;
			        }
			}

			
		});
		
		img.setFitWidth(200);
		img.setFitHeight(200);
		img.setPreserveRatio(true);
		HBox imgBox = new HBox(img);
		imgBox.setStyle("-fx-border-color: red; -fx-border-width: 1; -fx-border-style: dotted;");

		HBox hbox1 = new HBox(20,openButton, imgBox);
		imageResults.setPadding(new Insets(10,10,10,10));
		imageResults.setHgap(10);
		imageResults.setVgap(10);
		scroll.setContent(imageResults);
		scroll.setPrefWidth(1500);
		scroll.setPrefHeight(400);
		VBox vbox = new VBox(40, hbox, hbox1, startSearch, scroll);
		Group root = new Group(vbox);
		Scene scene = new Scene(root, 1500,800);
		
		stage.setScene(scene);
		stage.setTitle("Yolo GUI");
		stage.show();
		
	}
	
	private List<ImgDescriptor> tagSearch(String tag) {
		
		try {
			return eSearch.search(tag, it.unipi.ing.mim.deep.Parameters.K);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

	}
	
	private List<ImgDescriptor> imageSearch(File image) {
		
		DNNExtractor extractor = new DNNExtractor();


		imgFeatures = extractor.extract(image, it.unipi.ing.mim.deep.Parameters.DEEP_LAYER);

		ImgDescriptor imDes = new ImgDescriptor(imgFeatures, openedImage.getName());
		
		try {	
			return eSearch.search(imDes, it.unipi.ing.mim.deep.Parameters.K );
			
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}


		
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
