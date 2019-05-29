package it.unipi.ing.mim.img.elasticsearch;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
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
	
	//TODO
	public void createIndex() throws IOException {
		//Create the Elasticsearch index
		IndicesClient idx = client.indices();
		CreateIndexRequest request = new CreateIndexRequest(Parameters.INDEX_NAME);
		Builder s = Settings.builder()
				.put("index.number_of_shards",1)
				.put("number_of_replicas",0)
				.put("analysis.analyzer.first.type","whitespace");
		request.settings(s);
		idx.create(request, RequestOptions.DEFAULT);
	}

	public void deleteIndex() throws IOException {
		IndicesClient idx = client.indices();
		DeleteIndexRequest request = new DeleteIndexRequest(Parameters.INDEX_NAME);
		idx.delete(request, RequestOptions.DEFAULT);
	}

	//TODO
	public void index() throws IOException {
		//LOOP
			//index all dataset features into Elasticsearch
		int i = 0;
		for(ImgDescriptor imgDesc: imgDescDataset) {
			System.out.println("Indexing " + i);

			File imgFile = new File(imgDesc.getId());
			DetailedImage dimg = new DetailedImage(imgFile,
					new File(Parameters.META_SRC_FOLDER.getPath() + "/" + DetailedImage.getFileNameWithoutExtension(imgFile) + ".txt"));
			IndexRequest req = composeRequest(imgDesc, dimg);
			client.index(req, RequestOptions.DEFAULT);

			i++;
			//if(i==1000)
			//	break;
		}
	}
	
	//TODO
	private IndexRequest composeRequest(ImgDescriptor imgDesc, DetailedImage detImg) {
		IndexRequest request = new IndexRequest(Parameters.INDEX_NAME, "doc");
		Map<String,String> jMap = new HashMap<String, String>();
		
		// ImgDescriptor -> (imgid, boundingboxindex, features (of the single bounding box))
		// DetailedImage -> (imgid, List<bounding boxes>, human tags)

		// get bb_index from ImgDescriptor and use it to index the bounding box associated
		// to the feature of ImgDescriptor
		int bb_index = imgDesc.getBoundingBoxIndex();

		// Index
		if(bb_index == -1) {
			jMap.put(Fields.BOUNDING_BOX, "-1");
			jMap.put(Fields.CLASS_NAME, "");
		}
		else {
			jMap.put(Fields.BOUNDING_BOX, bb_index+"");
			jMap.put(Fields.CLASS_NAME,detImg.getClassByIndex(bb_index));
		}

		// Human tags are stored in DetailedImage
		jMap.put(Fields.FLICKR_TAGS, detImg.serializeHumanTags().replace(","," "));

		// Remaining fields are stored in ImgDescriptor
		jMap.put(Fields.IMG_ID, imgDesc.getId());		
		jMap.put(Fields.BOUNDING_BOX_FEAT, pivots.features2Text(imgDesc, topKIdx));
		request.source(jMap);

		return request;
	}
}
