package it.unipi.ing.mim.deep.seq;

import it.unipi.ing.mim.deep.DNNExtractor;
import it.unipi.ing.mim.deep.DetailedImage;
import it.unipi.ing.mim.deep.ImgDescriptor;
import it.unipi.ing.mim.deep.Parameters;
import it.unipi.ing.mim.deep.tools.FeaturesStorage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bytedeco.opencv.opencv_core.Mat;

public class SeqImageStorage {

	public static void main(String[] args) throws Exception {
				
		SeqImageStorage indexing = new SeqImageStorage();
				
		List<ImgDescriptor> descriptors = indexing.extractFeatures(Parameters.SRC_FOLDER, Parameters.META_SRC_FOLDER);
		
		FeaturesStorage.store(descriptors, Parameters.STORAGE_FILE);
	}
	
	private List<ImgDescriptor> extractFeatures(File imgFolder, File metaFolder){
		List<ImgDescriptor>  descs = new ArrayList<ImgDescriptor>();

		File[] img_files = imgFolder.listFiles();
		//File[] meta_files = metaFolder.listFiles();

		DNNExtractor extractor = new DNNExtractor();

		// for each image
		for (int i = 0; i < img_files.length; i++) {
			System.out.println(i + " - extracting " + img_files[i].getName());
			
			try {
				DetailedImage dimg = new DetailedImage(img_files[i],
						new File(metaFolder.getPath() + "/" + DetailedImage.getFileNameWithoutExtension(img_files[i]) + ".txt"));
				//ArrayList<int[]> bb_list = dimg.getBoundingBoxes();
				ArrayList<Mat> bb_mat = dimg.getRegionsOfInterest();

				long time = -System.currentTimeMillis();

				// entry for the main image
				System.out.println("Bounding box base");
				float[] features_img = extractor.extract(dimg.getContent(), Parameters.DEEP_LAYER);
				descs.add(new ImgDescriptor(features_img, img_files[i].getName(), -1));

				// otherwise for each image bounding box
				for(int bb_index=0; bb_index<bb_mat.size(); bb_index++) {
					System.out.println("Bounding box " + bb_index + "/" + bb_mat.size());
					float[] features = extractor.extract(bb_mat.get(bb_index), Parameters.DEEP_LAYER);
					descs.add(new ImgDescriptor(features, img_files[i].getName(), bb_index));
				}

				time += System.currentTimeMillis();
				System.out.println(time);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return descs;	
	}
}
