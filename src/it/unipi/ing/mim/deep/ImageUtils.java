package it.unipi.ing.mim.deep;

import static org.bytedeco.opencv.global.opencv_imgproc.rectangle;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;

import org.bytedeco.javacpp.indexer.ByteRawIndexer;
import org.bytedeco.javacpp.indexer.FloatRawIndexer;
import org.bytedeco.javacpp.indexer.UByteRawIndexer;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

public class ImageUtils {
	public static Image getDrawable(ImgDescriptor i) throws IOException {
		DetailedImage di = new DetailedImage(i.getId());
		Mat imageContent = di.getContent();
		int bboxIdx = i.getBoundingBoxIndex();
		if(bboxIdx >= 0) {
			int[] bboxCoords = di.getBoundingBoxByIndex(bboxIdx);
			Rect roi = getRectFromCorners(bboxCoords);
			rectangle(imageContent, roi, new Scalar(0.0,0.0,255.0,1));
		} 
		return matToImage(imageContent);
	}
	
	public static Image matToImage(Mat m) {
		int width = m.cols();
		int height = m.rows();
		int depth = m.channels();
		BufferedImage img = new BufferedImage(width,height,BufferedImage.TYPE_3BYTE_BGR);
		byte[] targetPixels = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
		m.data().get(targetPixels);
		return SwingFXUtils.toFXImage(img, null);
	}
	
	public static Rect getRectFromCorners(int[] bbox) {
		int left=bbox[0],right=bbox[1],top=bbox[2],bottom=bbox[3];
		// OpenCv ROIs want top-left corner and width,height of the rectangle
		int width = Math.abs(right-left);
		int height = Math.abs(bottom-top);
		Rect r = new Rect(left,top,width,height);
		return r;
	}
	

	
	public static void main(String[] args) throws IOException {
			Image m = getDrawable(new ImgDescriptor(null,"im10001.jpg",0));
	}
}
