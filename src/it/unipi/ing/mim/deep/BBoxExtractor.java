package it.unipi.ing.mim.deep;

import static org.bytedeco.opencv.global.opencv_dnn.blobFromImage;
import static org.bytedeco.opencv.global.opencv_dnn.readNetFromDarknet;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

import static org.bytedeco.opencv.global.opencv_dnn.NMSBoxes;
import static org.bytedeco.opencv.global.opencv_highgui.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import static org.bytedeco.opencv.global.opencv_core.*;
import org.bytedeco.javacpp.indexer.FloatRawIndexer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_core.StringVector;
import org.bytedeco.opencv.opencv_dnn.*;

import javafx.util.Pair;

public class BBoxExtractor {
	private Scalar meanValues;
	private Net net;
	private Size imgSize;
	private ArrayList<String> cocoClasses = new ArrayList<String>();
	private final static String DARK_CFG = "data/caffe/yolov3.cfg";
	private final static String DARK_WGH = "data/caffe/yolov3.weights";
	private final static String COCO_CLASSES = "data/caffe/coco.names";
	private final static int DARK_DIM = 608;
	
	public BBoxExtractor() throws IOException {		
		//Create the importer of Caffe framework network
		net = readNetFromDarknet(DARK_CFG,DARK_WGH);
		imgSize = new Size(DARK_DIM,DARK_DIM);
        if (Parameters.MEAN_VALUES != null) {
			meanValues = new Scalar(Parameters.MEAN_VALUES[0], Parameters.MEAN_VALUES[1], Parameters.MEAN_VALUES[2], Parameters.MEAN_VALUES[3]);
        }		
        FileReader cocoReader = new FileReader(new File(COCO_CLASSES));
        BufferedReader bufferedCocoReader = new BufferedReader(cocoReader);
        String cocoLine = "";
        while((cocoLine=bufferedCocoReader.readLine())!=null)
        	cocoClasses.add(cocoLine);
        bufferedCocoReader.close();
   }

	public ArrayList<Pair<String,Rect>>  extract(File image) {
		Mat img = imread(image.getPath());
		ArrayList<Pair<String,Rect>> boxes =  extract(img);
		for(Pair<String,Rect> p:boxes) {
			rectangle(img, p.getValue(), new Scalar(0.0,0.0,255.0,0.4));			
		}
		imshow("Image",img);
		waitKey();	
		return boxes;
	}

	public ArrayList<Pair<String,Rect>> extract(Mat img) {
		Mat inputBlob = blobFromImage(img,1/255.f,imgSize,new Scalar(0),true,false,CV_32F);
		long start = System.currentTimeMillis();
		net.setInput(inputBlob);
		StringVector names = net.getUnconnectedOutLayersNames();
		MatVector regionProposals = new MatVector();
		
		net.forward(regionProposals,names);
		RectVector boxesDetected = new RectVector();
		ArrayList<Float> confidences = new ArrayList<>();
		ArrayList<Integer> classIdx = new ArrayList<Integer>();
		float confidenceThreshold = 0.1f,nmsThreshold=0.5f;
		for(int i = 0; i < regionProposals.size(); ++i) {
			
			Mat proposal = regionProposals.get(i);
			for(int j = 0; j < proposal.rows(); ++j) {
				Mat row = proposal.row(j);
				/*
				 * Yolo features is composed as follow:
				 * [0-3] BBOX centerX,Y width height
				 * [4] Objecteness confidence
				 * [5-84] Class activations
				 */

				float[] yoloFeatures = new float[(int)row.cols()];
				((FloatRawIndexer) row.createIndexer()).get(0,yoloFeatures);				
				float objConfidence = yoloFeatures[4];
				int currentMaxIndex = -1;
				float currentMax = -1.0f;
				for(int k=0; k<row.cols();++k) {					
					//If in class activation pick the maximum activated class for the proposal
					if(k>=5) {
						//System.out.print(yoloFeatures[k]+",");
						if(yoloFeatures[k] >= currentMax) {
							currentMax = yoloFeatures[k];
							currentMaxIndex=k;
						}
					}					
				}
				int centerX = (int)(yoloFeatures[0]*img.cols());
				int centerY = (int)(yoloFeatures[1]*img.rows());
				int height = (int)(yoloFeatures[3]*img.rows());
				int width = (int)(yoloFeatures[2]*img.cols());
				int left = Math.abs(centerX-width/2);
				int right = Math.abs(centerX+width/2);
				int top = Math.abs(centerY -height/2);
				int bottom = Math.abs(centerY+height/2);
				Rect bbox = new Rect(left,top,width,height);
				boxesDetected.push_back(bbox);
				confidences.add(objConfidence);
				classIdx.add(currentMaxIndex-5);					
				
			}
		}

		float[] confs = new float[confidences.size()];
		int[] outIndices = new int[confidences.size()];
		for(int i = 0;i < outIndices.length;++i) outIndices[i] = -1;
		for(int i = 0; i < confidences.size();++i)confs[i]=confidences.get(i); 
		System.out.println("Found: "+confidences.size()+ " "+boxesDetected.size());
		NMSBoxes(boxesDetected, confs, confidenceThreshold, nmsThreshold,outIndices);
		ArrayList<Pair<String,Rect>> result = new ArrayList<Pair<String,Rect>>();
		
		for(int i = 0; i < outIndices.length;++i) {
			if(outIndices[i] > 0) {
				String className = convertToClassName(classIdx.get(outIndices[i]));
				Rect bbox = boxesDetected.get(outIndices[i]);
				System.out.println("["+bbox.x()+" "+bbox.y()+" "+bbox.width()+" "+bbox.height()+" ] "+className);
				System.out.println(bbox.x()+" "+(bbox.x()+bbox.width())+" "+bbox.y()+" "+(bbox.y()+bbox.height()));
				Pair<String,Rect> p = new Pair<String,Rect>(className,bbox);
				result.add(p);
			}
		}
		long end = System.currentTimeMillis();
		System.out.println("Done in: "+(end-start)/1000);				
		return result;
	}
	
	//TODO
	public String convertToClassName(int idx) {
		return cocoClasses.get(idx	);
	}
	
	public static void main(String[] args) throws IOException {
		BBoxExtractor b = new BBoxExtractor();
		b.extract(new File("data/img/mirflickr/im10011.jpg"));
		
		
	}
}
