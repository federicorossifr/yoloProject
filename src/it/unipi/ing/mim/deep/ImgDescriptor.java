package it.unipi.ing.mim.deep;

import java.io.Serializable;
import java.util.Comparator;


public class ImgDescriptor implements Serializable, Comparable<ImgDescriptor> , Comparator<ImgDescriptor>{

	private static final long serialVersionUID = 1L;
	
	private float[] normalizedVector; // bounding box image feature
	
	private String id; // unique id of the image (file name)
	
	private double dist; // used for sorting purposes
	
	private int boundingBoxIndex; // index for indexing DetailedImage Array

	public ImgDescriptor(float[] features, String id) {
		this(features, id, 0);
	}
	
	public ImgDescriptor(float[] features, String id, int boundingBoxIndex) {
		if (features != null) {
			float norm2 = evaluateNorm2(features);
			this.normalizedVector = getNormalizedVector(features, norm2);
		}

		if(boundingBoxIndex < 0 && boundingBoxIndex != -1)
			throw new IllegalArgumentException("Bounding box array size is invalid");
		this.setBoundingBoxIndex(boundingBoxIndex);

		this.id = id;
	}
	
	public float[] getFeatures() {
		return normalizedVector;
	}
	
    public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public int getBoundingBoxIndex() {
		return boundingBoxIndex;
	}

	public void setBoundingBoxIndex(int boundingBoxIndex) {
		this.boundingBoxIndex = boundingBoxIndex;
	}

	public double getDist() {
		return dist;
	}

	public void setDist(double dist) {
		this.dist = dist;
	}

	// compare with other friends using distances
	@Override
	public int compareTo(ImgDescriptor arg0) {
		if(Double.valueOf(dist).equals(arg0.dist)) return id.compareTo(arg0.id);
		return Double.valueOf(dist).compareTo(arg0.dist);
	}
	
	//evaluate Euclidian distance
	public double distance(ImgDescriptor desc) {
		float[] queryVector = desc.getFeatures();
		
		dist = 0;
		for (int i = 0; i < queryVector.length; i++) {
			dist += (normalizedVector[i] - queryVector[i]) * (normalizedVector[i] - queryVector[i]);
		}
		dist = Math.sqrt(dist);
		
		return dist;
	}
	
	//Normalize the vector values 
	private float[] getNormalizedVector(float[] vector, float norm) {
		if (norm != 0) {
			for (int i = 0; i < vector.length; i++) {
				vector[i] = vector[i]/norm;
			}
		}
		return vector;
	}
	
	//Norm 2
	private float evaluateNorm2(float[] vector) {
		float norm2 = 0;
		for (int i = 0; i < vector.length; i++) {
			norm2 += (vector[i]) * (vector[i]);
		}
		norm2 = (float) Math.sqrt(norm2);
		
		return norm2;
	}
	
	public String toString() {
		return  DetailedImage.getFileNameWithoutExtension(id) + "-" + boundingBoxIndex;
	}
	
	public static String toString(String id,int boundingBoxIndex) {
		return DetailedImage.getFileNameWithoutExtension(id) + "-" + boundingBoxIndex;
	}
    @Override
	public boolean equals(Object v) {
	     boolean retVal = false;
	     if (v instanceof ImgDescriptor){
	    	 ImgDescriptor ptr = (ImgDescriptor) v;
	         retVal = ptr.id.equals(this.id);
	     }
	     return retVal;
	 }

	@Override
	public int compare(ImgDescriptor o1, ImgDescriptor o2) {
		return Double.valueOf(o2.dist).compareTo(o1.dist);
	}

}
