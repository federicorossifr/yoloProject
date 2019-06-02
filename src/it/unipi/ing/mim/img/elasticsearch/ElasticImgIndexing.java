package it.unipi.ing.mim.img.elasticsearch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;

import it.unipi.ing.mim.deep.DetailedImage;
import it.unipi.ing.mim.deep.ImgDescriptor;
import it.unipi.ing.mim.deep.Parameters;
import it.unipi.ing.mim.deep.tools.FeaturesStorage;

public class ElasticImgIndexing implements AutoCloseable {
	
	private Pivots pivots;
	
	private List<ImgDescriptor> imgDescDataset;
	private int topKIdx;
	private Set<String> present = new HashSet<>();
	private RestHighLevelClient client;
		
	public static void main(String[] args) throws ClassNotFoundException, IOException {
		try (ElasticImgIndexing esImgIdx = new ElasticImgIndexing(Parameters.PIVOTS_FILE, Parameters.STORAGE_FILE, Parameters.TOP_K_IDX)) {
			try {esImgIdx.deleteIndex();} catch(Exception e) { System.out.println("Cannot create index"); }
			esImgIdx.createIndex();
			esImgIdx.index();	
		}
	}
	
	//TODO
	public ElasticImgIndexing(File pivotsFile, File datasetFile, int topKIdx) throws IOException, ClassNotFoundException {
			pivots = new Pivots(pivotsFile);
			imgDescDataset = FeaturesStorage.load(datasetFile);
			this.topKIdx = topKIdx;
			RestClientBuilder builder = RestClient.builder(new HttpHost("localhost", 9200, "http"));
			client = new RestHighLevelClient(builder);			
	}
	
	//TODO
	public void close() throws IOException {
		client.close();
	}
	
	public void createIndex() throws IOException {
		createIndex(Parameters.FEATURE_INDEX_NAME);
		createIndex(Parameters.TAGS_INDEX_NAME);
	}
	
	//TODO
	public void createIndex(String indexName) throws IOException {
		//Create the Elasticsearch index
		IndicesClient idx = client.indices();
		CreateIndexRequest request = new CreateIndexRequest(indexName);
		Builder s = Settings.builder()
				.put("index.number_of_shards",1)
				.put("number_of_replicas",0)
				.put("analysis.analyzer.first.type","whitespace");
		request.settings(s);
		idx.create(request, RequestOptions.DEFAULT);
	}
	
	public void deleteIndex() throws IOException {
		deleteIndex(Parameters.FEATURE_INDEX_NAME);
		deleteIndex(Parameters.TAGS_INDEX_NAME);
	}

	public void deleteIndex(String indexName) throws IOException {
		IndicesClient idx = client.indices();
		DeleteIndexRequest request = new DeleteIndexRequest(indexName);
		idx.delete(request, RequestOptions.DEFAULT);
	}

	//TODO
	public void index() throws IOException {
		//LOOP
			//index all dataset features into Elasticsearch
		int i = 1;
		BulkRequest bulkRequest = new BulkRequest();
		for(ImgDescriptor imgDesc: imgDescDataset) {
			System.out.println("Indexing " + i);
			
			File imgFile = new File(imgDesc.getId());
			DetailedImage dimg = new DetailedImage(imgFile,
					new File(Parameters.META_SRC_FOLDER.getPath() + "/" + DetailedImage.getFileNameWithoutExtension(imgFile) + ".txt"));
			
			if(!present.contains(imgDesc.getId())) {
				//client.index(composeTagRequest(imgDesc,dimg), RequestOptions.DEFAULT);
				bulkRequest.add(composeTagRequest(imgDesc,dimg));
				present.add(imgDesc.getId());
			}
			//client.index(composeFeatureRequest(imgDesc,dimg), RequestOptions.DEFAULT);
			bulkRequest.add(composeFeatureRequest(imgDesc,dimg));
			if(i%Parameters.BULK_CHUNK == 0) {
				System.out.println("Bulking: "+Parameters.BULK_CHUNK);
				client.bulk(bulkRequest, RequestOptions.DEFAULT);
				bulkRequest = new BulkRequest();
			}
			i++;
		}
		if(i%Parameters.BULK_CHUNK != 0)
			client.bulk(bulkRequest, RequestOptions.DEFAULT);
	}
	private IndexRequest composeTagRequest(ImgDescriptor imgDesc,DetailedImage detImg) {
		IndexRequest request = new IndexRequest(Parameters.TAGS_INDEX_NAME, "doc");
		Map<String,String> jMap = new HashMap<String, String>();		
		jMap.put(Fields.FLICKR_TAGS, detImg.serializeHumanTags().replace(","," "));	
		jMap.put(Fields.YOLO_TAGS, detImg.serializeDistinctClasses());	
		jMap.put(Fields.IMG_ID, imgDesc.getId());				
		request.source(jMap);
		return request;
	}
	
	private IndexRequest composeFeatureRequest(ImgDescriptor imgDesc,DetailedImage detImg) {
		IndexRequest request = new IndexRequest(Parameters.FEATURE_INDEX_NAME, "doc");
		Map<String,String> jMap = new HashMap<String, String>();			
		jMap.put(Fields.IMG_ID, imgDesc.getId());		
		int bb_index = imgDesc.getBoundingBoxIndex();		
		jMap.put(Fields.BOUNDING_BOX_FEAT, pivots.features2Text(imgDesc, topKIdx));		

		
		//jMap.put(Fields.RAW_FEATURES,imgDesc.serializeFeatures());
		
		if(bb_index == -1) {
			jMap.put(Fields.BOUNDING_BOX, "-1");
			jMap.put(Fields.CLASS_NAME, "");
		}
		else {
			jMap.put(Fields.BOUNDING_BOX, bb_index+"");
			jMap.put(Fields.CLASS_NAME,detImg.getClassByIndex(bb_index));
		}		
		request.source(jMap);
		return request;
	}

}
