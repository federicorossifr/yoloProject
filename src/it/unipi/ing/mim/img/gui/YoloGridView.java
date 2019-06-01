package it.unipi.ing.mim.img.gui;

import java.io.File;
import java.io.IOException;
import java.util.List;

import it.unipi.ing.mim.deep.DetailedImage;
import it.unipi.ing.mim.deep.ImageUtils;
import it.unipi.ing.mim.deep.ImgDescriptor;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class YoloGridView extends ScrollPane{
	
	private MyGrid gallery;
	
	public YoloGridView(int w, int h, int nCols, int iSize, int childD) {
		setPrefHeight(h);
		setPrefWidth(w);
		gallery = new MyGrid(nCols, iSize, childD);
		setContent(gallery);
	}

	public YoloGridView() {
		this(1100,460, 4, 200,10);
	}
	
	public void clearView() {
		gallery.clearView();
	}
	
	public void refreshItems(List<ImgDescriptor> items) throws IOException {
		gallery.refreshItems(items);
	}
	
	private class MyGrid extends GridPane{
		
		private int numCols;
		private int imageSize;
		
		public MyGrid(int nCols, int iSize, int childD) {
			numCols = nCols;
			imageSize = iSize;
			setHgap(childD);
			setVgap(childD);
			setPadding(new Insets(10,10,10,10));
		}
		
		public void displayImageDetails(String id, Image imTemp) throws IOException {
			Stage s = new Stage();
			ImageView tmp = new ImageView();
			tmp.setImage(imTemp);
			DetailedImage di = new DetailedImage(id);
			Label lab = new Label();
			String details = new String("");
			for(int j = 0; j<di.getBoundingBoxes().size(); ++j)
				details += "CLASS: " + di.getClassByIndex(j) + "\t SCORE: " + di.getScoreByIndex(j) + "\n";
			lab.setText(details);
			s.setTitle("Image");
			ScrollPane sp = new ScrollPane(lab);
			sp.setPrefHeight(100);
			Label det = new Label("DETAILS");
			det.setFont(Font.font("Arial", FontWeight.BOLD, 30));
			VBox vb = new VBox(20,tmp, det,sp);
			vb.setAlignment(Pos.CENTER);
			s.setScene(new Scene(new Group(vb),imTemp.getWidth(),imTemp.getHeight()+177));
			s.setTitle(id);
			s.show();
		}
		
		public void clearView() {
			getChildren().clear();
		}
		
		public void refreshItems(List<ImgDescriptor> items) throws IOException{
			
			clearView();
			
			int colCnt = 0, rowCnt = 0;
			 for (int i=0; i<items.size(); i++) {
			    	ImageView imgIn = new ImageView(ImageUtils.getDrawable(items.get(i)));
			    	imgIn.setId(items.get(i).getId());
			    	imgIn.setOnMouseClicked(ev -> {
			    		ImageView imm = (ImageView) ev.getTarget();
			    		Image imgg = imm.getImage();
			    		try {
							displayImageDetails(imm.getId(), imgg);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
			    	});
			    	imgIn.setFitHeight(imageSize);
			    	imgIn.setFitWidth(imageSize);
			    	imgIn.setPreserveRatio(true);
			        add(imgIn, colCnt, rowCnt);
			        colCnt++;
			        if (colCnt>numCols) {
			            rowCnt++;
			            colCnt=0;
			        }
			}

		}
	}
	
}
