package it.unipi.ing.mim.deep;

import static org.bytedeco.opencv.global.opencv_imgproc.rectangle;
import static org.bytedeco.opencv.global.opencv_imgproc.putText;
import static org.bytedeco.opencv.global.opencv_highgui.*;
import static org.bytedeco.opencv.global.opencv_imgproc.CV_FILLED;
import static org.bytedeco.opencv.global.opencv_core.addWeighted;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Scalar;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
public class ImageUtils {
	private final static double BBOX_ALPHA = 0.5;
	private static Scalar[] colorSet = {
			new Scalar(0,0,255,0.75),			
			new Scalar(255,0,0,0.75),
			new Scalar(60,120,20,0.75),
			new Scalar(0,131,255,0.75),
			new Scalar(255,0,255,0.75),
			new Scalar(2,2,112,0.75)
	};
	
	private static Scalar getRandomColor(int idx) {
		return colorSet[idx%colorSet.length];
	}
	/**
	 * Method to get a JavaFX image from an ImageDescriptor, with the relative bouding
	 * box, (if any) applied.
	 * @param i
	 * @return
	 * @throws IOException
	 */
	public static Image getDrawable(ImgDescriptor i) throws IOException {
		DetailedImage di = new DetailedImage(i.getId());
		Mat imageContent = di.getContent();
		int bboxIdx = i.getBoundingBoxIndex();
		if(bboxIdx >= 0) {
			int[] bboxCoords = di.getBoundingBoxByIndex(bboxIdx);
			applyRectAndLabel(imageContent, bboxCoords, di.getClassByIndex(bboxIdx),getRandomColor(bboxIdx));	
		} 

		return matToImage(imageContent);
	}
	
	private static void applyRectAndLabel(Mat imageContent,int[] bbox,String label,Scalar bgColor) {
		Rect roi = getRectFromCorners(bbox);
		int left=bbox[0],right=bbox[1],top=bbox[2],bottom=bbox[3];
		//Outer bounding box
		Mat overlay = new Mat();
		imageContent.copyTo(overlay);
		rectangle(overlay, roi, bgColor,2,8,0);
		
	
		
		//Label background box
		Rect bg = new Rect(new Point(left,top),new Point(right,top+15));
		rectangle(overlay,bg,bgColor,CV_FILLED,0,0);		
		addWeighted(overlay,  BBOX_ALPHA, imageContent, 1- BBOX_ALPHA, 0, imageContent);

		//Label
		putText(imageContent,label,new Point(left,top+10),0,0.4f,new Scalar(255.0,255.0,255.0,1));	

	}
	
	public static Image getDrawable(List<ImgDescriptor> li) throws IOException {
		Mat imageContent = null;
		for(ImgDescriptor i:li) {
			DetailedImage di = new DetailedImage(i.getId());
			if(imageContent == null)
				imageContent = di.getContent();
			int bboxIdx = i.getBoundingBoxIndex();
			if(bboxIdx >= 0) {
				int[] bboxCoords = di.getBoundingBoxByIndex(bboxIdx);
				applyRectAndLabel(imageContent, bboxCoords, di.getClassByIndex(bboxIdx),getRandomColor(bboxIdx));					
			} 

		}
	
		return matToImage(imageContent);		
	}
	
	/**
	 * Method to convert an OpenCV image matrix to a JavaFX Image via BufferedImage
	 * @param m
	 * @return
	 */
	private static Image matToImage(Mat m) {
		int width = m.cols();
		int height = m.rows();
		int depth = m.channels();
		BufferedImage img = new BufferedImage(width,height,BufferedImage.TYPE_3BYTE_BGR);
		byte[] targetPixels = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
		m.data().get(targetPixels);
		return SwingFXUtils.toFXImage(img, null);
	}
	
	
	/**
	 * Method to build an OpenCV rectangle from the four corners of a bounding box
	 * @param bbox
	 * @return
	 */
	private static Rect getRectFromCorners(int[] bbox) {
		int left=bbox[0],right=bbox[1],top=bbox[2],bottom=bbox[3];
		// OpenCv ROIs want top-left corner and width,height of the rectangle
		int width = Math.abs(right-left);
		int height = Math.abs(bottom-top);
		Rect r = new Rect(left,top,width,height);
		return r;
	}
	

	public static void main(String[] args) throws IOException {
		ArrayList<ImgDescriptor> test = new ArrayList<ImgDescriptor>();
		test.add(new ImgDescriptor(null,"im9715.jpg",0));
		test.add(new ImgDescriptor(null,"im9715.jpg",1));
		test.add(new ImgDescriptor(null,"im9715.jpg",3));
		test.add(new ImgDescriptor(null,"im9715.jpg",4));
		test.add(new ImgDescriptor(null,"im9715.jpg",5));
		
		Image m = getDrawable(test);
	}
}
