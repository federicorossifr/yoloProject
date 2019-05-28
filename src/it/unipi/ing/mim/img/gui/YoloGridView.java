package it.unipi.ing.mim.img.gui;

import java.util.List;

import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

public class YoloGridView extends ScrollPane{
	
	private MyGrid gallery;
	
	public YoloGridView(int w, int h, int nCols, int iSize, int childD) {
		setPrefHeight(h);
		setPrefWidth(w);
		gallery = new MyGrid(nCols, iSize, childD);
		setContent(gallery);
	}

	public YoloGridView() {
		this(1100,600, 4, 200,10);
	}
	
	public void clearView() {
		gallery.clearView();
	}
	
	public void refreshItems(List<Image> items) {
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
		
		
		public void clearView() {
			getChildren().clear();
		}
		
		public void refreshItems(List<Image> items){
			
			clearView();
			
			int colCnt = 0, rowCnt = 0;
			 for (int i=0; i<items.size(); i++) {
			    	ImageView imgIn = new ImageView(items.get(i));
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
