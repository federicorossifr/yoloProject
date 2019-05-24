package it.unipi.ing.mim.deep;

import java.io.File;

public class Parameters {
	
	//DEEP parameters
	public static final String DEEP_PROTO = "data/caffe/hybridCNN_deploy_upgraded.prototxt";
	public static final String DEEP_MODEL = "data/caffe/hybridCNN_iter_700000_upgraded.caffemodel";
	public static final double[] MEAN_VALUES = {104, 117, 123, 0};
	
	public static final String DEEP_LAYER = "fc6";
	public static final int IMG_WIDTH = 227;
	public static final int IMG_HEIGHT = 227;
	
	
	//Image Source Folder
	public static final File SRC_FOLDER = new File("data/img/mirflickr");
	
	//Image Meta-Data Folder
	public static final File META_SRC_FOLDER = new File("data/img/meta/");
	
	//Features Storage File
	public static final File STORAGE_FILE = new File("data/deep.seq.dat");
	
	//k-Nearest Neighbors
	public static final int K = 30;
	
	//Pivots File
	public static final File  PIVOTS_FILE = new File("out/deep.pivots.dat");
	
	//Number Of Pivots
	public static final int NUM_PIVOTS = 100;

	//Top K pivots For Indexing
	public static final int TOP_K_IDX = 10;
	
	//Top K pivots For Searching
	public static final int TOP_K_QUERY = 10;
	
	//Lucene Index
	public static final String INDEX_NAME = "yolohybrid";
	
	//HTML Output Parameters
	public static final  String BASE_URI = "file:///" + Parameters.SRC_FOLDER.getAbsolutePath() + "/";
	public static final File RESULTS_HTML = new File("out/deep.seq.html");
	public static final File RESULTS_HTML_ELASTIC = new File("out/deep.elastic.html");
	public static final File RESULTS_HTML_REORDERED = new File("out/deep.reordered.html");

}
