package it.unipi.ing.mim.deep;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import static org.bytedeco.opencv.global.opencv_highgui.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;

/**
 * Class that holds an image detailed representation.
 * It contains all the bounding boxes with relative classes
 * 
 * @author federico
 *
 */
public class DetailedImage {
	private String imageID;
	private ArrayList<String> classNames = new ArrayList<>();
	private ArrayList<int[]> boundingBoxes = new ArrayList<>();
	private String[] humanTags;
	private Mat content;
	private ArrayList<Double>  scores=new ArrayList<>();

	/*

	 */
	
	/**
	 * Constrcutor for the DetailedImage from an image file and a metatata file
	 * @param imageFile file representing the image jpg/png
	 * @param yoloMetaData file containing metadata with following structure
	 * 		<class-name>;<score,left,right,top,bottom>
	 * 		<class-name>;<score,left,right,top,bottom>
	 * 		<tag1>,<tag2>,<tag3> 
	 * @throws IOException
	 */
	public DetailedImage(File imageFile,File yoloMetaData) throws IOException {
		imageID = imageFile.getName();
		FileReader metaReader = new FileReader(yoloMetaData);
		BufferedReader metaBuferedReader = new BufferedReader(metaReader);
		String metaLine = "";
		//Read yolo data
		while((metaLine = metaBuferedReader.readLine())!= null) {
			String[] splittedYoloData = metaLine.split(";");
			//If the split does not return two element-array we have done with yoloData
			if(splittedYoloData.length < 2) break;
			//Otherwise last element is a list of int coordinates for bounding boxes
			String[] coords = splittedYoloData[splittedYoloData.length-1].split(",");
			double score  = Double.parseDouble(coords[0]);
			coords = Arrays.copyOfRange(coords,1,coords.length);
			int[] intCoords = Arrays.stream(coords).mapToInt(Integer::parseInt).toArray();			
			//From first element to the n-1 element there are class-names, for the bounding box.
			classNames.add(splittedYoloData[0]);
			boundingBoxes.add(intCoords);
			scores.add(score);
			//Second element is a list of int-coordinates

		}
		
		//Extract flickr-tags, metaLine is a list of tags separated by comma
		humanTags = metaLine.split(",");
		
		//Read image content
		content = imread(imageFile.getPath());
		
		metaBuferedReader.close();
		metaReader.close();
	}
	
	/**
	 * Shorthand constructor with only the image ID as parameter
	 * @param imageID
	 * @throws IOException
	 */
	public DetailedImage(String imageID) throws IOException {
		this(new File(Parameters.SRC_FOLDER+File.separator+imageID),
			 new File(Parameters.META_SRC_FOLDER+File.separator+getFileNameWithoutExtension(imageID)+".txt"));
		
	}
	
	/**
	 * Constructor to build a DetailedImage object from its details as returned from search engine
	 * @param imageID image id to load the png/jpg
	 * @param bboxes list of string bounding boxes
	 * @param classes list of bounded classes
	 * @param tags list of human tags
	 */
	public DetailedImage(String imageID,ArrayList<String> bboxes,String[] classes,String[] tags) {
		this.imageID=imageID;
		for(String b:bboxes) {
			String[] coords = b.split(",");
			int[] intCoords = Arrays.stream(coords).mapToInt(Integer::parseInt).toArray();		
			boundingBoxes.add(intCoords);
		}
		classNames = (ArrayList<String>)Arrays.asList(classes);
		String imagePath = Parameters.SRC_FOLDER+File.separator+imageID;
		content = imread(imagePath);
		humanTags = tags;
	}
	
	/**
	 * Constructor to build an empty DetailedImage, to be populated afterwards
	 * @param imageID image id to load from png/jpg
	 * @param tags set of human tags
	 */
	public DetailedImage(String imageID,String[] tags) {
		this.imageID=imageID;
		String imagePath = Parameters.SRC_FOLDER+File.separator+imageID;
		content = imread(imagePath);
		humanTags = tags;
		
	}
	
	/**
	 * Add a bounding box with relative classname to the DetailedImage
	 * @param bbox bbox string representation
	 * @param className class name
	 */
	public void addBoundingBox(String bbox,String className) {
		classNames.add(className);
		String[] coords = bbox.split(",");
		int[] intCoords = Arrays.stream(coords).mapToInt(Integer::parseInt).toArray();		
		boundingBoxes.add(intCoords);		
	}
	

	/**
	 * Extract region of interests from the detailed image by cropping all available bounding boxes
	 * @return
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
	
	public ArrayList<String> getClassNames() {
		return classNames;
	}
	
	public String getClassByIndex(int i) {
		return classNames.get(i);
	}
	
	public int[] getBoundingBoxByIndex(int i) {
		return boundingBoxes.get(i);
	}
	public Double getScoreByIndex(int i) {
		return scores.get(i);
	}
	/**
	 * Serialize tags into a single string to be fed into indexer
	 * @return joint string of all human tags
	 */
	public String serializeHumanTags() {
		return String.join(",", humanTags);
	}
	
	/**
	 * Serialize the list of bounding boxes into a list of String to be singularly fed into indexer
	 * @return list of joint string of bounding boxes
	 */
	public ArrayList<String> serializeBoundingBoxes() {
		ArrayList<String> bboxes = new ArrayList<String>();
		for(int[] b: boundingBoxes) {
			String[] arr = ((IntStream)Arrays.stream(b)).mapToObj(String::valueOf).toArray(String[]::new);
			bboxes.add(String.join(",",arr));
		}
		return bboxes;
	}
	
	public Mat getContent() {
		return content;
	}
	
	public static void main(String[] args) throws IOException {
		String yoloData = "data/img/extracted/im21350.txt";
		String imageData = "data/img/mirflickr/im21350.jpg";
		DetailedImage di = new DetailedImage(new File(imageData), new File(yoloData));
		ArrayList<Mat> rois = di.getRegionsOfInterest();
		ArrayList<String> bboxes = di.serializeBoundingBoxes();
		imshow("Original",di.getContent());
		waitKey();
		for(Mat r:rois) {
			imshow("Region",r);
			waitKey();
		}
	}
	
	public static String getFileNameWithoutExtension(File file) {
		String filename = file.getName();
		return getFileNameWithoutExtension(filename);
	}
	
	public static String getFileNameWithoutExtension(String fileStr) {
		return fileStr.substring(0, fileStr.length()-4);
	}
	
	public String serializeDistinctClasses() {
		Set<String> distinctClasses = new HashSet<>();
		String result = "";
		for(String c:classNames)
			if(distinctClasses.add(c))
				result+=c+" ";
		return result.trim();
	}

	public String serializeClasses() {
		return String.join(" ", classNames);
	}
	
	public String getImageId() {
		return imageID;
	}
		
}
