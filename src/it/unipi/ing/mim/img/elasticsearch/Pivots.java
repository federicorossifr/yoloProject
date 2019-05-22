package it.unipi.ing.mim.img.elasticsearch;

import it.unipi.ing.mim.deep.ImgDescriptor;
import it.unipi.ing.mim.deep.Parameters;
import it.unipi.ing.mim.deep.seq.SeqImageSearch;
import it.unipi.ing.mim.deep.seq.SeqImageStorage;
import it.unipi.ing.mim.deep.tools.FeaturesStorage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Pivots {
	
	private SeqImageSearch seqPivots = new SeqImageSearch();
	
	//TODO
	public Pivots(File pivotsFile) throws ClassNotFoundException, IOException {
		seqPivots.open(pivotsFile);
	}

	public static void main(String[] args) throws ClassNotFoundException, IOException {
		List<ImgDescriptor> ids = FeaturesStorage.load(Parameters.STORAGE_FILE);
		List<ImgDescriptor> pivs = Pivots.makeRandomPivots(ids, Parameters.NUM_PIVOTS);
		FeaturesStorage.store(pivs, Parameters.PIVOTS_FILE);
	}
	
	//TODO
	public static List	<ImgDescriptor> makeRandomPivots(List<ImgDescriptor> ids, int nPivs) {
		ArrayList<ImgDescriptor> pivots = new ArrayList<>();

		//LOOP
		//Create nPivs random pivots and add them in the pivots List
		Collections.shuffle(ids);
		for(int i = 0; i < nPivs; ++i)
			pivots.add(ids.get(i));		
		return pivots;
	}
	
	//TODO
	public String features2Text(ImgDescriptor imgF, int topK) {
		StringBuilder sb = new StringBuilder();
		
		//perform a sequential search to get the topK most similar pivots
		List<ImgDescriptor> l = seqPivots.search(imgF, topK);

		//LOOP
			//compose the text string using pivot ids
		for(ImgDescriptor ll:l) {
			sb.append(ll.getId());
		}		
		return sb.toString();
	}
	
}