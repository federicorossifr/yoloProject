package it.unipi.ing.mim.deep;

import java.io.Serializable;


public class ImgDescriptor implements Serializable, Comparable<ImgDescriptor> {

	private static final long serialVersionUID = 1L;
	
	private float[] normalizedVector; // image feature
	
	private String id; // unique id of the image (usually file name)
	
	private double dist; // used for sorting purposes
	
	private int[] boundingBox;
	
	private String human_tags;

	public ImgDescriptor(float[] features, String id) {
		this(features, id, null, "");
	}
	
	public ImgDescriptor(float[] features, String id, int[] boundingBox, String human_tags) {
		if (features != null) {
			float norm2 = evaluateNorm2(features);
			this.normalizedVector = getNormalizedVector(features, norm2);
		}

		if(boundingBox.length != 4 && boundingBox != null)
			throw new IllegalArgumentException("Bounding box array size is not 4");
		this.setBoundingBox(boundingBox);

		this.setHumanTags(human_tags);
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
	
	public int[] getBoundingBox() {
		return boundingBox;
	}

	public void setBoundingBox(int[] boundingBox) {
		this.boundingBox = boundingBox;
	}

	public String getHumanTags() {
		return human_tags;
	}

	public void setHumanTags(String human_tags) {
		this.human_tags = human_tags;
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
}
