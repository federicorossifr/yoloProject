package it.unipi.ing.mim.img.elasticsearch;

import it.unipi.ing.mim.deep.ImgDescriptor;
import it.unipi.ing.mim.deep.Parameters;
import it.unipi.ing.mim.deep.seq.SeqImageSearch;
import it.unipi.ing.mim.deep.tools.FeaturesStorage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;


public class Pivots {
	
	private SeqImageSearch seqPivots = new SeqImageSearch();
	
	//TODO
	public Pivots(File pivotsFile) throws ClassNotFoundException, IOException {
		seqPivots.open(pivotsFile);
	}

	public static void main(String[] args) throws ClassNotFoundException, IOException {
		List<ImgDescriptor> ids = FeaturesStorage.load(Parameters.STORAGE_FILE);
		//List<ImgDescriptor> pivs = Pivots.makeRandomPivots(ids, Parameters.NUM_PIVOTS);
		long time = -System.currentTimeMillis();
		List<ImgDescriptor> pivs = Pivots.make3MPivots(ids, Parameters.NUM_PIVOTS);
		time += System.currentTimeMillis();
		System.out.println(time + " ms");
		System.out.println("PIVOTS SELECTED");
		FeaturesStorage.store(pivs, Parameters.PIVOTS_FILE);		
		System.out.println("PIVOTS STORED");
	}
	
	//TODO
	public static List<ImgDescriptor> makeRandomPivots(List<ImgDescriptor> ids, int nPivs) {
		ArrayList<ImgDescriptor> pivots = new ArrayList<>();

		//LOOP
		//Create nPivs random pivots and add them in the pivots List
		Collections.shuffle(ids);
		for(int i = 0; i < nPivs; ++i)
			pivots.add(ids.get(i));		
		return pivots;
	}
	
	public static List<ImgDescriptor> make3MPivots(List<ImgDescriptor> ids, int nPivs){
		int MMM = 3*nPivs;
		int nSelectedPivs = 0;
		ArrayList<ImgDescriptor> candidateSet = new ArrayList<>();
		ArrayList<ImgDescriptor> pivots = new ArrayList<>();
		
		// Insert at most 3*m random object in the candidate set
		Collections.shuffle(ids, new Random(12));
		int ins=0;
		ListIterator<ImgDescriptor> it = ids.listIterator();
		while( ins<MMM && it.hasNext() ) {
			ImgDescriptor d = it.next();
			if( d.getBoundingBoxIndex() != -1 ) {
				candidateSet.add(it.next());
				ins++;
			}
		}
		
		// This vector will cointain min distances between each point
		// in the candidate set and selected pivots
		double[] minVector = new double[candidateSet.size()];
		for(int i=0; i<minVector.length; i++)
			minVector[i] = Double.POSITIVE_INFINITY;

		// Choose a random point as first pivot
		ImgDescriptor currentPivot = candidateSet.get(0);
		pivots.add(currentPivot);
		//candidateSet.remove(0);

		while(nSelectedPivs<nPivs && nSelectedPivs < candidateSet.size()) {
			// 1) update min distance between each point on the candidate set and pivots
			// note that min can only decrease when a new point is selected as pivots 
			for(int j=0; j<candidateSet.size(); j++) {
				if(minVector[j] != Double.NaN) {
					double newdist = currentPivot.distance(candidateSet.get(j));
					if(newdist < minVector[j])
						minVector[j] = newdist;
				}
			}
			
			// 2) compute max over mins
			// the point that has higher min distance is the furthest from pivots set
			double maxVal = Double.NEGATIVE_INFINITY;
			int maxValIndex = -1;
			for(int j=0; j<candidateSet.size(); j++)
				if(minVector[j] != Double.NaN && minVector[j] > maxVal) {
					maxVal = minVector[j];
					maxValIndex = j;
				}
			
			System.out.println(nSelectedPivs + ": pivots selected id=" + maxValIndex + " dist=" + minVector[maxValIndex]);

			minVector[maxValIndex] = Double.NaN;
			currentPivot = candidateSet.get(maxValIndex); 
			pivots.add(currentPivot);
			nSelectedPivs++;
		}
	

		pivots.remove(0);
		return pivots;
	}
	
	//TODO
	public String features2Text(ImgDescriptor imgF, int topK) {
		StringBuilder sb = new StringBuilder();

		//perform a sequential search to get the topK most similar pivots
		List<ImgDescriptor> topk_elems = seqPivots.search(imgF, topK);

		//LOOP
		for(int i = 0; i < topk_elems.size(); i++)
		{
			ImgDescriptor img = topk_elems.get(i);

			//compose the text string using pivot ids
			for(int j = i; j < topK; j++) {
				sb.append(img.toString());
				sb.append(' ');
			}
		}

		return sb.toString();
	}
}