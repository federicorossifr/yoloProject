package it.unipi.ing.mim.deep;

import java.io.File;

import org.bytedeco.javacpp.indexer.FloatRawIndexer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_dnn.*;

import static org.bytedeco.opencv.global.opencv_dnn.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;

public class DNNExtractor {

	private Scalar meanValues;
	private Net net;
	private Size imgSize;
	
	
	public DNNExtractor() {		
		//Create the importer of Caffe framework network
		net = readNetFromDarknet(new File(Parameters.DEEP_PROTO).getPath(), new File(Parameters.DEEP_MODEL).getPath());
        
        imgSize = new Size(Parameters.IMG_WIDTH, Parameters.IMG_HEIGHT);

        if (Parameters.MEAN_VALUES != null) {
			meanValues = new Scalar(Parameters.MEAN_VALUES[0], Parameters.MEAN_VALUES[1], Parameters.MEAN_VALUES[2], Parameters.MEAN_VALUES[3]);
        }
	}

	public float[] extract(File image, String layer) {
		Mat img = imread(image.getPath());
		return extract(img, layer);
	}

	public float[] extract(Mat img, String layer) {
		resize(img, img, imgSize);
		
		// Convert Mat to dnn::Blob image batch
		Mat inputBlob = blobFromImage(img);

		// set the network input
		net.setInput(inputBlob, "data", 1.0, meanValues);
		
		// compute output
		Mat prob = net.forward();

		float[] features = new float[(int) prob.total()];
		
		// gather output of "fc7" layer
		((FloatRawIndexer) prob.createIndexer()).get(0, features);
		for(float f: features) System.out.println(f);
		return features;
	}
}
