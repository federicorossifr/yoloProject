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
import javafx.scene.input.MouseEvent;
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
	private TextField topK = new TextField();
	//private Button openButton = new Button("Open an image");
	private ImageView img = new ImageView();
	private Button startSearch = new Button("Start Search");
	private YoloGridView imageResults = new YoloGridView();
		
	private float[] imgFeatures;
	private File openedImage;
	
	private ElasticImgSearching eSearch; 
	
	@Override
	public void start(Stage stage) {
	
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Resource File");
		fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Image Files", "*.jpg"));
	
		Label tagLabel = new Label("Insert Human Tag:");
		Label topKLabel = new Label("Insert top K-NN: ");
		HBox hboxTag = new HBox(20,new Label(""),tagLabel, humanTags);
		HBox topKBox = new HBox(20,new Label(""), topKLabel, topK );

		VBox inputBox = new VBox(10,hboxTag, topKBox, startSearch);
		
		VBox imageBox = new VBox(img);
		HBox topPane = new HBox(300, inputBox, imageBox);
		VBox allPane = new VBox(20, topPane, imageResults);
		
		img.setFitWidth(200);
		img.setFitHeight(200);
		img.setPreserveRatio(true);
		imageBox.setStyle("-fx-border-color: red; -fx-border-width: 1; -fx-border-style: dotted;");
		Group root = new Group(allPane);
		Scene scene = new Scene(root, 1100,800);
		stage.setScene(scene);
		stage.setTitle("Yolo GUI");
		stage.show();
		
		
		try {
			
			eSearch = new ElasticImgSearching(it.unipi.ing.mim.deep.Parameters.PIVOTS_FILE, it.unipi.ing.mim.deep.Parameters.TOP_K_QUERY);
		
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	    imageBox.setOnMouseClicked( (MouseEvent event) ->{
	    	
	    	openedImage = fileChooser.showOpenDialog(stage);
	        if (openedImage != null) {
	
	        	Image i = new Image(openedImage.toURI().toString());
	        	img.setImage(i);
	        	
	        }
	    });
		
		startSearch.setOnAction((ActionEvent ev)-> {
			
			List<ImgDescriptor> searched = null;
			ArrayList<Image> imageTemp = new ArrayList<Image>();
			
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
			
			imageResults.refreshItems(imageTemp);
			
		});

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
