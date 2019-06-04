package it.unipi.ing.mim.img.gui;

import javafx.scene.control.*;
import javafx.scene.*;
import javafx.scene.layout.*;
import javafx.scene.control.Alert.AlertType;
import java.io.*;
import java.util.*;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.lucene.queryparser.classic.ParseException;

import it.unipi.ing.mim.deep.DNNExtractor;
import it.unipi.ing.mim.deep.ImgDescriptor;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import it.unipi.ing.mim.img.elasticsearch.ElasticImgSearching;

public class YoloGUI extends Application {
	
	private TextField humanTags = new TextField();
	private TextField topK = new TextField();
	private ImageView img = new ImageView();
	private Button startSearch = new Button("Start Search");
	private YoloGridView imageResults = new YoloGridView();
	private ImageView loading = new ImageView();
	private RadioButton tagR, classR, bothR;
	private String loadingPath = "data/img/gui/loading.gif";
	
	private final int wImgPreview = 200;
	private final int hImgPreview = 200;
	
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
		StatusLogger.getLogger().setLevel(Level.OFF);			
		FileChooser fileChooser = new FileChooser();
		initializeFileChooser(fileChooser);
		
		ImageView yoloIcon = new ImageView(new Image(new File("data/img/gui/darknet.png").toURI().toString()));
		ImageView yoloText = new ImageView(new Image(new File("data/img/gui/yologo.png").toURI().toString()));
		initializeIcon(yoloIcon);
		initializeLogo(yoloText);
		
		HBox fooTitle = new HBox(20,yoloIcon, yoloText);
		HBox topTitle = new HBox(1, new HBox(),fooTitle);
		topTitle.setStyle("-fx-background-color: #283747;");
		fooTitle.setAlignment(Pos.CENTER);
		topTitle.setAlignment(Pos.CENTER);
		
		topK.setText(String.valueOf(it.unipi.ing.mim.deep.Parameters.K));
		
		Label tagLabel = new Label("Tag:");
		Label topKLabel = new Label("Top K-NN: ");
		HBox foobox = new HBox();
		foobox.setPrefWidth(27);
		HBox hboxTag = new HBox(20,new Label(""),tagLabel,foobox, humanTags);
		HBox topKBox = new HBox(20,new Label(""),topKLabel,topK );
		
		loading.setFitHeight(30);
		loading.setFitWidth(30);
		loading.setPreserveRatio(true);
		loading.setImage(null);
		HBox searchBox = new HBox(30, startSearch, loading);
		searchBox.setAlignment(Pos.CENTER);
		
		Label checkboxL = new Label("Search in: ");
		tagR = new RadioButton("Tags");
		classR = new RadioButton("Yolo Classes");
		bothR = new RadioButton("Both");
		bothR.setSelected(true);
		ToggleGroup tg = new ToggleGroup();
		tagR.setToggleGroup(tg);
		classR.setToggleGroup(tg);
		bothR.setToggleGroup(tg);
		HBox checkboxBox = new HBox(20, new Label(""),checkboxL, tagR, classR, bothR); 
		
		VBox inputBox = new VBox(20,checkboxBox,hboxTag, topKBox);
		inputBox.setAlignment(Pos.CENTER);
		
		initializeImgPreview(img);
		Text inftx = new Text("Click Here to Load an Image");
	    StackPane pane = new StackPane(img, inftx);
	    pane.setAlignment(Pos.CENTER);
	    
		VBox imageBox = new VBox(pane);
		imageBox.setAlignment(Pos.CENTER);

		HBox topPane = new HBox(150, inputBox, new VBox(160,new VBox(),searchBox), imageBox);
		VBox allPane = new VBox(20, topTitle,topPane, imageResults);
		
	
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
		
		humanTags.textProperty().addListener(
				(ObservableValue<? extends String> observable,String oldValue, String newValue)->{
	            	if(!newValue.equals(""))
	            		img.setImage(null);
	            });
	    imageBox.setOnMouseClicked( (MouseEvent event) ->{
	    	
	    	openedImage = fileChooser.showOpenDialog(stage);
	        if (openedImage != null) {
	
	        	Image i = new Image(openedImage.toURI().toString());
	        	img.setImage(i);
	        	humanTags.setText("");
	        	startSearch.requestFocus();
	        	
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

	private void initializeFileChooser(FileChooser fileChooser) {
		fileChooser.setTitle("Open Resource File");
		fileChooser.setInitialDirectory(new File("data/img/mirflickr"));
		fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Image Files", "*.jpg"));
	}
	
	private void initializeIcon(ImageView yoloIcon) {
		yoloIcon.setFitHeight(100);
		yoloIcon.setFitWidth(100);
		yoloIcon.setPreserveRatio(true);
	}
	
	private void initializeLogo(ImageView logo) {
		logo.setFitWidth(150);
		logo.setFitHeight(100);
		logo.setPreserveRatio(true);
	}
	
	private void initializeImgPreview(ImageView i) {
		i.setClip(null);
		i.setEffect(new DropShadow(20, Color.BLACK));
		i.setFitWidth(wImgPreview);
		i.setFitHeight(hImgPreview);
		//i.setPreserveRatio(true);
	}
	
	private List<ImgDescriptor> tagSearch(String tag, int k) {
		try {
			if(bothR.isSelected())
				return eSearch.search(tag, k);
			else if(tagR.isSelected())
				return eSearch.searchByTag(tag, k);
			else if(classR.isSelected())
				return eSearch.searchByClass(tag, k);
			else
				return null;
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
		
			if(!humanTags.getText().equals("")) {		
				int k = it.unipi.ing.mim.deep.Parameters.K;
				
				try {
					k = Integer.parseInt(topK.getText());
					if(k<=0)
						k = it.unipi.ing.mim.deep.Parameters.K;	
				}catch(NumberFormatException e) {
					System.out.println("K error");
					k = it.unipi.ing.mim.deep.Parameters.K;
				}finally {
					topK.setText(String.valueOf(k));
					searched = tagSearch(humanTags.getText(), k);
				}	
			}else if(img.getImage() != null) {
				int k = it.unipi.ing.mim.deep.Parameters.K;
				try {
					k = Integer.parseInt(topK.getText());
					if(k<=0)
						k = it.unipi.ing.mim.deep.Parameters.K;
				}catch(NumberFormatException e) {
					k = it.unipi.ing.mim.deep.Parameters.K;
				}finally {
					topK.setText(String.valueOf(k));
					searched = imageSearch(openedImage, k);
				}	
			}
			
			if(searched != null) {		
				final ArrayList<ImgDescriptor> imageTemp = new ArrayList<>(searched);
				
				Platform.runLater(()->{
					try {
						imageResults.refreshItems(imageTemp);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
					loading.setImage(null); 
					startSearch.setDisable(false);
				});
			}
			
		}

	}
}
