package it.unipi.ing.mim.img.gui;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.queryparser.classic.ParseException;

import it.unipi.ing.mim.deep.DNNExtractor;
import it.unipi.ing.mim.deep.ImageUtils;
import it.unipi.ing.mim.deep.ImgDescriptor;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

import it.unipi.ing.mim.deep.Parameters;
import it.unipi.ing.mim.img.elasticsearch.ElasticImgSearching;
import it.unipi.ing.mim.img.elasticsearch.Pivots;

public class YoloGUI extends Application {
	
	private TextField humanTags = new TextField();
	private TextField topK = new TextField();
	private ImageView img = new ImageView();
	private Button startSearch = new Button("Start Search");
	private YoloGridView imageResults = new YoloGridView();
	private ImageView loading = new ImageView();
	
	private String loadingPath = "data/img/gui/loading.gif";
	
	private float[] imgFeatures;
	private File openedImage;
	
	private ElasticImgSearching eSearch; 
	
	private void showException(Exception ex) {
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("YOLO-GUI");
		alert.setHeaderText("Sorry, there was an error");
		alert.setContentText(ex.getMessage());

		// Create expandable Exception.
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		ex.printStackTrace(pw);
		String exceptionText = sw.toString();

		Label label = new Label("The exception stacktrace was:");

		TextArea textArea = new TextArea(exceptionText);
		textArea.setEditable(false);
		textArea.setWrapText(true);

		textArea.setMaxWidth(Double.MAX_VALUE);
		textArea.setMaxHeight(Double.MAX_VALUE);
		GridPane.setVgrow(textArea, Priority.ALWAYS);
		GridPane.setHgrow(textArea, Priority.ALWAYS);

		GridPane expContent = new GridPane();
		expContent.setMaxWidth(Double.MAX_VALUE);
		expContent.add(label, 0, 0);
		expContent.add(textArea, 0, 1);

		// Set expandable Exception into the dialog pane.
		alert.getDialogPane().setExpandableContent(expContent);
		alert.showAndWait();
		Platform.exit();
	}
	
	@Override
	public void start(Stage stage) {
	
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Resource File");
		fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Image Files", "*.jpg"));
		
		ImageView yoloIcon = new ImageView(new Image(new File("data/img/gui/darknet.png").toURI().toString()));
		ImageView yoloText = new ImageView(new Image(new File("data/img/gui/yologo.png").toURI().toString()));
		yoloIcon.setFitHeight(100);
		yoloIcon.setFitWidth(100);
		yoloIcon.setPreserveRatio(true);
		yoloText.setFitWidth(150);
		yoloText.setFitHeight(100);
		yoloText.setPreserveRatio(true);
		HBox fooTitle = new HBox(20,yoloIcon, yoloText);
		HBox topTitle = new HBox(1, new HBox(),fooTitle);
		topTitle.setStyle("-fx-background-color: #283747;");
		fooTitle.setAlignment(Pos.CENTER);
		topTitle.setAlignment(Pos.CENTER);
		
		topK.setText(String.valueOf(it.unipi.ing.mim.deep.Parameters.K));
		
		Label tagLabel = new Label("Human Tag:");
		Label topKLabel = new Label("Top K-NN: ");
		HBox hboxTag = new HBox(20,new Label(""),tagLabel, humanTags);
		HBox topKBox = new HBox(20,new Label(""), topKLabel, topK );
		
		loading.setFitHeight(30);
		loading.setFitWidth(30);
		loading.setPreserveRatio(true);
		loading.setImage(null);
		HBox searchBox = new HBox(30, startSearch, loading);
		searchBox.setAlignment(Pos.CENTER);
		
		VBox inputBox = new VBox(20,hboxTag, topKBox);
		inputBox.setAlignment(Pos.CENTER);
		
		Text inftx = new Text("Click Here to Load an Image");
	    StackPane pane = new StackPane(img, inftx);
	    pane.setAlignment(Pos.CENTER);
	    
		VBox imageBox = new VBox(pane);
		imageBox.setAlignment(Pos.CENTER);

	    
		HBox topPane = new HBox(150, inputBox, new VBox(160,new VBox(),searchBox), imageBox);
		VBox allPane = new VBox(20, topTitle,topPane, imageResults);
		
		img.setFitWidth(200);
		img.setFitHeight(200);
		//img.setPreserveRatio(true);
		imageBox.setStyle("-fx-border-color: red; -fx-border-width: 1; -fx-border-style: dotted;");
		Group root = new Group(allPane);
		Scene scene = new Scene(root, 1100,800);
		stage.setScene(scene);
		stage.setTitle("Yolo GUI");
		stage.getIcons().add(new Image(new File("data/img/gui/darknet.png").toURI().toString()));
		stage.show();
		
		
		try {
			
			eSearch = new ElasticImgSearching(it.unipi.ing.mim.deep.Parameters.PIVOTS_FILE, it.unipi.ing.mim.deep.Parameters.TOP_K_QUERY);
		
		} catch (ClassNotFoundException e) {
			showException(e);
			e.printStackTrace();
		} catch (IOException e) {
			showException(e);
			e.printStackTrace();
		}
		
	    imageBox.setOnMouseClicked( (MouseEvent event) ->{
	    	
	    	openedImage = fileChooser.showOpenDialog(stage);
	        if (openedImage != null) {
	
	        	Image i = new Image(openedImage.toURI().toString());
	        	img.setImage(i);
	        	
	        }else
	        	img.setImage(null);
	    });
		
	   startSearch.setStyle("-fx-background-color: linear-gradient(#008CFF, #66B2FF), radial-gradient(center 50% -40%, radius 200%, #008CFF 45%, #66B2FF 50%); -fx-background-radius: 6, 5; "
                + "-fx-background-insets: 0, 1; -fx-text-fill: white;-fx-font-weigth: bold");
	   
		startSearch.setOnAction((ActionEvent ev)-> {
			
			loading.setImage(new Image(new File(loadingPath).toURI().toString()));
			startSearch.setDisable(true);
			
			LoadGallery loadThread = new LoadGallery();
			Thread t = new Thread(loadThread);
			t.start();

		});

	}
	
	private List<ImgDescriptor> tagSearch(String tag, int k) {
		
		try {
			return eSearch.search(tag, k);
		} catch (ClassNotFoundException e) {
			showException(e);
			return null;
		} catch (ParseException e) {
			showException(e);
			return null;
		} catch (IOException e) {
			showException(e);
			return null;
		}

	}
	
	private List<ImgDescriptor> imageSearch(File image, int k) {
		
		DNNExtractor extractor = new DNNExtractor();


		imgFeatures = extractor.extract(image, it.unipi.ing.mim.deep.Parameters.DEEP_LAYER);

		ImgDescriptor imDes = new ImgDescriptor(imgFeatures, openedImage.getName());
		
		try {	
			return eSearch.search(imDes,k);
			
		} catch (ClassNotFoundException e) {
			showException(e);
			return null;
		} catch (ParseException e) {
			showException(e);
			return null;
		} catch (IOException e) {
			showException(e);
			return null;
		}


		
	}
	
	public static void main(String[] args) {
		launch(args);
	}
	
	private class LoadGallery implements Runnable{
		
		public void run() {
			
			List<ImgDescriptor> searched = null;
			ArrayList<Image> imageTemp = new ArrayList<Image>();
			
			if(!humanTags.getText().equals("")) {
				
				if(!topK.getText().equals("")) {
					
					int k = Integer.parseInt(topK.getText());
					if( k > 0)
						searched = tagSearch(humanTags.getText(), k);
					else {
						
						topK.setText(String.valueOf(it.unipi.ing.mim.deep.Parameters.K));
						searched = tagSearch(humanTags.getText(), it.unipi.ing.mim.deep.Parameters.K);
						
					}
					
				}else{
					
					topK.setText(String.valueOf(it.unipi.ing.mim.deep.Parameters.K));
					searched = tagSearch(humanTags.getText(), it.unipi.ing.mim.deep.Parameters.K);
					
				}
					
				
			}else if(img.getImage() != null) {
				
				if(!topK.getText().equals("")) {
					
					int k = Integer.parseInt(topK.getText());
					if(k>0)
						searched = imageSearch(openedImage, k);
					else {
						topK.setText(String.valueOf(it.unipi.ing.mim.deep.Parameters.K));
						searched = imageSearch(openedImage, it.unipi.ing.mim.deep.Parameters.K);
					}
					
				}else{
					
					topK.setText(String.valueOf(it.unipi.ing.mim.deep.Parameters.K));
					searched = tagSearch(humanTags.getText(), it.unipi.ing.mim.deep.Parameters.K);
					
				}
			}
			
			for(ImgDescriptor i : searched) {
				try {
					imageTemp.add(ImageUtils.getDrawable(i));
				} catch (IOException e) {
					showException(e);
				}
			}
			
			Platform.runLater(()->{
				imageResults.refreshItems(imageTemp); 
				loading.setImage(null); 
				startSearch.setDisable(false);
			});
		
		}

	}
}
