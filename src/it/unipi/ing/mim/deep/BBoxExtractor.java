package it.unipi.ing.mim.deep;
/*****************
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *  NOT WORKING FOR NOW
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 */
import static org.bytedeco.opencv.global.opencv_dnn.blobFromImage;
import static org.bytedeco.opencv.global.opencv_dnn.readNetFromDarknet;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_highgui.*;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;
import static org.bytedeco.opencv.global.opencv_core.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bytedeco.javacpp.indexer.FloatRawIndexer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_core.StringVector;
import org.bytedeco.opencv.opencv_dnn.Net;

public class BBoxExtractor {
	private Scalar meanValues;
	private Net net;
	private Size imgSize;
	private final static String DARK_CFG = "data/caffe/yolov3.cfg";
	private final static String DARK_WGH = "data/caffe/yolov3.weights";
	private final static int DARK_DIM = 608;
	
	public BBoxExtractor() {		
		//Create the importer of Caffe framework network
		net = readNetFromDarknet(DARK_CFG,DARK_WGH);
		imgSize = new Size(DARK_DIM,DARK_DIM);
        if (Parameters.MEAN_VALUES != null) {
			meanValues = new Scalar(Parameters.MEAN_VALUES[0], Parameters.MEAN_VALUES[1], Parameters.MEAN_VALUES[2], Parameters.MEAN_VALUES[3]);
        }		
   }

	public void extract(File image) {
		Mat img = imread(image.getPath());
		//imshow("Im",img);
		//waitKey();
		extract(img);
	}

	public void extract(Mat img) {
		
		// Convert Mat to dnn::Blob image batch

		Mat inputBlob = blobFromImage(img,1/255.f,imgSize,new Scalar(0),true,false,CV_32F);
			
		// set the network input
		long start = System.currentTimeMillis();
		net.setInput(inputBlob);
		Mat prob = net.forward();
		for(int i=0; i<prob.rows();++i) {
			for(int j=0;j < prob.cols();++j) {
				float f = prob.row(i).col(j).getFloatBuffer().get();
				
			}
			
		}
		long end = System.currentTimeMillis();
		System.out.println("Done in: "+(end-start)/1000);
		// compute output
	/*	MatVector probs = new MatVector();
		StringVector names = net.getUnconnectedOutLayersNames();

		net.forward(probs,names);
		for(int i = 0; i < probs.size(); ++i) {
			
			Mat prob = probs.get(i);
			System.out.println(names.get(i).getString());
			System.out.println(prob.cols()+" "+prob.rows());
			for(int j = 0; j < prob.rows(); ++j) {
				Mat row = prob.row(j);
				float[] rowF = new float[(int)row.cols()];
				for(int k=0; k<row.cols();++k) {
					Mat lll = row.col(k);
					rowF[k] = lll.getFloatBuffer().get();
				}
				
				//minMaxLoc(row.colRange(6, row.cols()-1), res);
				int centerX = (int)(rowF[0]*DARK_DIM);
				int centerY = (int)(rowF[1]*DARK_DIM);
				int height = (int)(rowF[2]*DARK_DIM);
				int width = (int)(rowF[3]*DARK_DIM);
				int left = Math.abs(centerX-width/2);
				int top = Math.abs(centerY -height/2);
					System.out.print("["+left+","+top+"]");
					for(int bb=4; bb<rowF.length;++bb) System.out.print(rowF[bb]+",");
					System.out.println();
			}
		}*/
		


		
	}
	

	
	
	public static void main(String[] args) {
		BBoxExtractor b = new BBoxExtractor();
		b.extract(new File("data/img/mirflickr/im10001.jpg"));
		
	}
}
