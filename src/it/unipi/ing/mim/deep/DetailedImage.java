package it.unipi.ing.mim.deep;

import java.awt.Image;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import static org.bytedeco.opencv.global.opencv_highgui.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;


public class DetailedImage {
	private ArrayList<String> classNames = new ArrayList<>();
	private ArrayList<int[]> boundingBoxes = new ArrayList<>();
	private String[] humanTags;
	private Mat content;
	/*
	 * Meta file structure
	 * <class-name>;<left,right,top,bottom>
	 * <class-name>;<left,right,top,bottom>
	 * <tag1>,<tag2>,<tag3>
	 */
	
	public DetailedImage(File imageFile,File yoloMetaData) throws IOException {
		FileReader metaReader = new FileReader(yoloMetaData);
		BufferedReader metaBuferedReader = new BufferedReader(metaReader);
		String metaLine = "";
		//Read yolo data
		while((metaLine = metaBuferedReader.readLine())!= null) {
			String[] splittedYoloData = metaLine.split(";");
			//If the split does not return two element-array we have done with yoloData
			if(splittedYoloData.length < 2) break;
			//Otherwise, first element is class-name, second is a bounding box.
			classNames.add(splittedYoloData[0]);
			//Second element is a list of int-coordinates
			String[] coords = splittedYoloData[1].split(",");
			int[] intCoords = Arrays.stream(coords).mapToInt(Integer::parseInt).toArray();
			boundingBoxes.add(intCoords);
		}
		
		//Extract flickr-tags, metaLine is a list of tags separated by comma
		humanTags = metaLine.split(",");
		
		//Read image content
		content = imread(imageFile.getPath());
		
		metaBuferedReader.close();
		metaReader.close();
	}
	
	/*
	 * Method to extract regions of interest pixels using the bounding boxes extracted
	 * from YOLO
	 */
	public ArrayList<Mat> getRegionsOfInterest() {
		ArrayList<Mat> rois = new ArrayList<>();
		for(int[] bbox: boundingBoxes) {
			// YOLO extracts bounding boxes as top-left corner and bottom-right corner
			int left=bbox[0],right=bbox[1],top=bbox[2],bottom=bbox[3];
			// OpenCv ROIs want top-left corner and width,height of the rectangle
			int width = Math.abs(right-left);
			int height = Math.abs(bottom-top);
			Rect r = new Rect(left,top,width,height);
			Mat crop = new Mat(content,r);
			rois.add(crop);
		}
		return rois;
	}
	
	public ArrayList<int[]> getBoundingBoxes() {
		return boundingBoxes;
	}
	
	public String[] humanTags() {
		return humanTags;
	}
	
	/*public static void main(String[] args) throws IOException {
		String yoloData = "data/img/extracted/im10001.txt";
		String imageData = "data/img/mirflickr/im10001.jpg";
		DetailedImage di = new DetailedImage(new File(imageData), new File(yoloData));
		ArrayList<Mat> rois = di.getRegionsOfInterest();
		for(Mat r:rois) {
			imshow("Region",r);
			waitKey();
		}
	}*/
	
}
