package it.unipi.ing.mim.img.gui;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import it.unipi.ing.mim.deep.DetailedImage;
import it.unipi.ing.mim.deep.ImageUtils;
import it.unipi.ing.mim.deep.ImgDescriptor;
import it.unipi.ing.mim.deep.Parameters;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList; 

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
	
	public void refreshItems(List<ImgDescriptor> items, Boolean useAccuracy) throws IOException {
		if( useAccuracy == true )
			gallery.refreshItems(items);
		else {
			HashMap<String, ArrayList<ImgDescriptor>> hmap = new HashMap<>();
			for(ImgDescriptor d : items) {
				if(d.getBoundingBoxIndex() == Parameters.NO_BOUNDING_BOX)
					System.out.println(d.getId() + " has no bounding box");
				if( hmap.get(d.getId()) == null ){
					hmap.put(d.getId(), new ArrayList<ImgDescriptor>());
				}
				ArrayList<ImgDescriptor> al = hmap.get(d.getId());
				al.add(d);
			}
			gallery.refreshItems(hmap);
		}
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
		
		public void displayImageDetails(String id, double score, Image imTemp) throws IOException {
			Stage s = new Stage();
			ImageView tmp = new ImageView();
			tmp.setImage(imTemp);
			tmp.setClip(null);
			tmp.setEffect(new DropShadow(20,Color.BLACK));
			DetailedImage di = new DetailedImage(id);
			VBox bboxDetails = new VBox(5);
			Label bboxTags = new Label("YOLO BOUNDING BOXES:");
			bboxTags.setFont(Font.font("Arial", FontWeight.BOLD, 16));
			bboxDetails.getChildren().add(bboxTags);
			
			for(int j = 0; j<di.getBoundingBoxes().size(); ++j) {
				
				String details = String.valueOf(j+1) + ") CLASS: " + di.getClassByIndex(j) + "\t SCORE: " + di.getScoreByIndex(j) + "\n";
				Text lab = new Text(details);
				lab.setId(String.valueOf(j));
				lab.setOnMouseMoved(ev->{
					Text l = (Text) ev.getTarget(); 
					int idT = Integer.parseInt(l.getId());
					try {
						tmp.setImage(ImageUtils.getDrawable(new ImgDescriptor(null,id,idT)));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}); 
				bboxDetails.getChildren().add(lab);
				
			}
			
			Label tagsL = new Label("FLICKR TAGS:");
			tagsL.setFont(Font.font("Arial", FontWeight.BOLD, 16));
			Label t = new Label(di.serializeHumanTags());
			t.setMaxWidth(imTemp.getWidth()-20);
			t.setWrapText(true);
			bboxDetails.getChildren().add(tagsL);
			bboxDetails.getChildren().add(t);

			s.setTitle("Image");
			ScrollPane sp = new ScrollPane(bboxDetails);
			sp.setPrefHeight(153);
			Label det = new Label("DETAILS - Score " + score);
			det.setFont(Font.font("Arial", FontWeight.BOLD, 30));
			VBox vb = new VBox(20,tmp, det,sp);
			vb.setAlignment(Pos.CENTER);
			s.setScene(new Scene(new Group(vb),imTemp.getWidth(),imTemp.getHeight()+230));
			s.setTitle(id);
			s.show();
		}
		
		public void clearView() {
			getChildren().clear();
		}
		
		private void setImgPreview(ImageView imgIn, double score) {
			imgIn.setOnMouseClicked(ev -> {
	    		ImageView imm = (ImageView) ev.getTarget();
	    		Image imgg = imm.getImage();
	    		try {
					displayImageDetails(imm.getId(), score, imgg);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    	});
			imgIn.setFitHeight(imageSize);
	    	imgIn.setFitWidth(imageSize);
	    	imgIn.setPreserveRatio(true);
	    	imgIn.setClip(null);
	    	imgIn.setEffect(new DropShadow(20, Color.BLACK));
	    	imgIn.setOnMouseMoved(ev ->{
	    		ImageView onIm = (ImageView)ev.getTarget();
	    		onIm.setEffect(new DropShadow(40, Color.BLACK));
	    		});
	    	imgIn.setOnMouseExited(ev ->{
	    		ImageView onIm = (ImageView)ev.getTarget();
	    		onIm.setEffect(new DropShadow(20, Color.BLACK));
	    		});
		}
		
		private ArrayList<ArrayList<ImgDescriptor>> sortByValue(Map<String, ArrayList<ImgDescriptor>> unsortMap) {

	        // 1. Convert Map to List of Map
	        List<Entry<String, ArrayList<ImgDescriptor>>> list =
	                new LinkedList<Map.Entry<String, ArrayList<ImgDescriptor>>>(unsortMap.entrySet());

	        // 2. Sort list with Collections.sort(), provide a custom Comparator
	        //    Try switch the o1 o2 position for a different order
	        Collections.sort(list, new Comparator<Map.Entry<String, ArrayList<ImgDescriptor>>>() {
	            public int compare(Map.Entry<String, ArrayList<ImgDescriptor>> o1,
	                               Map.Entry<String, ArrayList<ImgDescriptor>> o2) {
	                return Double.compare(o2.getValue().get(0).getDist(), o1.getValue().get(0).getDist());
	            }
	        });
	        
	        ArrayList<ArrayList<ImgDescriptor>> retlist = new ArrayList<>();
	        for(Entry<String, ArrayList<ImgDescriptor>> entry : list)
	        	retlist.add(entry.getValue());

	        return retlist;
	    }
		
		public void refreshItems(HashMap<String, ArrayList<ImgDescriptor>> hmap)  throws IOException {
			ArrayList<ArrayList<ImgDescriptor>> list = sortByValue(hmap);
			
			clearView();
			int colCnt = 0, rowCnt = 0;
			
			for (ArrayList<ImgDescriptor> sublist : list) {
				ImageView imgIn = new ImageView(ImageUtils.getDrawable(sublist));
				imgIn.setId(sublist.get(0).getId());
				setImgPreview(imgIn, sublist.get(0).getDist());
				
		        add(imgIn, colCnt++, rowCnt);
		        
		        if (colCnt>numCols) {
		            rowCnt++;
		            colCnt=0;
		        }
			}
		}
		
		public void refreshItems(List<ImgDescriptor> items) throws IOException{
			
			clearView();
			
			int colCnt = 0, rowCnt = 0;
			for (int i=0; i<items.size(); i++) {
		    	ImageView imgIn = new ImageView(ImageUtils.getDrawable(items.get(i)));
		    	imgIn.setId(items.get(i).getId());
		    	setImgPreview(imgIn, items.get(i).getDist());
		    	
		        add(imgIn, colCnt++, rowCnt);

		        if (colCnt>numCols) {
		            rowCnt++;
		            colCnt=0;
		        }
			}
		}
	}
	
}
