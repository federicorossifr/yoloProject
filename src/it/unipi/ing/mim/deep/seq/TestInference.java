package it.unipi.ing.mim.deep.seq;


import org.bytedeco.javacpp.indexer.FloatRawIndexer;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_dnn.*;
import static org.bytedeco.opencv.global.opencv_dnn.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;

import java.awt.FlowLayout;
import java.awt.Image;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import it.unipi.ing.mim.deep.Parameters;

public class TestInference {
	
	  private static Map<Integer, Float> sortByValue(Map<Integer, Float> unsortMap) {

	        // 1. Convert Map to List of Map
	        List<Map.Entry<Integer, Float>> list =
	                new LinkedList<Map.Entry<Integer, Float>>(unsortMap.entrySet());

	        // 2. Sort list with Collections.sort(), provide a custom Comparator
	        //    Try switch the o1 o2 position for a different order
	        Collections.sort(list, new Comparator<Map.Entry<Integer, Float>>() {
	            public int compare(Map.Entry<Integer, Float> o1,
	                               Map.Entry<Integer, Float> o2) {
	                return ((o2.getValue()).compareTo(o1.getValue()));
	            }
	        });

	        // 3. Loop the sorted list and put it into a new insertion order Map LinkedHashMap
	        Map<Integer, Float> sortedMap = new LinkedHashMap<Integer, Float>();
	        for (Map.Entry<Integer, Float> entry : list) {
	            sortedMap.put(entry.getKey(), entry.getValue());
	        }

	        /*
	        //classic iterator example
	        for (Iterator<Map.Entry<String, Integer>> it = list.iterator(); it.hasNext(); ) {
	            Map.Entry<String, Integer> entry = it.next();
	            sortedMap.put(entry.getKey(), entry.getValue());
	        }*/


	        return sortedMap;
	    }
	  public static void displayImage(Image img2,String cl) {

		    //BufferedImage img=ImageIO.read(new File("/HelloOpenCV/lena.png"));
		    ImageIcon icon=new ImageIcon(img2);
		    JFrame frame=new JFrame();
		    JLabel prClass = new JLabel();
		    frame.setLayout(new FlowLayout());        
		    frame.setSize(img2.getWidth(null)+150, img2.getHeight(null)+150);     
		    JLabel lbl=new JLabel();
		    lbl.setIcon(icon);
		    prClass.setText(cl);
		    frame.add(lbl);
		    frame.add(prClass);
		    frame.setVisible(true);
		    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		}  
	  
	 public static HashMap<Integer,String> parseLabels(File f) throws IOException {
		 HashMap<Integer, String> labels = new HashMap<Integer, String>();
		 FileReader fr = new FileReader(f);
		 BufferedReader br = new BufferedReader(fr);
		 String line = "";
		 while((line=br.readLine())!= null) {
			 String[] parts = line.split(":");
			 parts[0] = parts[0].replace("{", "").trim();
			 parts[1] = parts[1].replace("}", "").trim();
			 labels.put(Integer.parseInt(parts[0]),parts[1]);
		 } 
		 br.close();
		 return labels;
	 }
	  
	public static void main(String[] args) throws IOException {
		File image = new File(Parameters.SRC_FOLDER, "000000452891.jpg");
		HashMap<Integer, String> llll = parseLabels(new File("data/labels.txt"));
		Mat img = imread(image.getPath());
		Net net = readNetFromCaffe(new File(Parameters.DEEP_PROTO).getPath(), new File(Parameters.DEEP_MODEL).getPath());
		Size imgSize = new Size(Parameters.IMG_WIDTH, Parameters.IMG_HEIGHT);
		resize(img, img, imgSize);
		// Convert Mat to dnn::Blob image batch
		Mat inputBlob = blobFromImage(img);

		// set the network input
		Scalar meanValues = new Scalar(Parameters.MEAN_VALUES[0], Parameters.MEAN_VALUES[1], Parameters.MEAN_VALUES[2], Parameters.MEAN_VALUES[3]);
		net.setInput(inputBlob, "data", 1.0, meanValues);
		
		// compute output
		Mat prob = net.forward();
		// compute output
//		Mat masks = net.forward("detection_masks");
	//	System.out.println(masks.size());
		float[] features = new float[(int) prob.total()];
		((FloatRawIndexer) prob.createIndexer()).get(0, features);
        Map<Integer, Float> ff = new HashMap<Integer, Float>();

		for(int i = 0; i < features.length; ++i) {
//			System.out.println(features[i]);
			ff.put(i, features[i]);
		}
		Map<Integer,Float> sFF = sortByValue(ff);
		Iterator<Entry<Integer, Float>> it = sFF.entrySet().iterator();
		StringBuilder sb = new StringBuilder();
		int i = 0;
		while(it.hasNext() && (i++) < 5 ) {
			Entry<Integer,Float> e = it.next();
			System.out.println(e.getKey()+" - "+e.getValue()+" - "+llll.get(e.getKey()));
			sb.append(llll.get(e.getKey()));
		}
		System.out.println(sb.toString());
		displayImage(ImageIO.read(image),sb.toString());			
		
	}
}
