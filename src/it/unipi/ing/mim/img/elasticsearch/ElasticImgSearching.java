package it.unipi.ing.mim.img.elasticsearch;

import java.io.File;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpHost;
import org.apache.lucene.queryparser.classic.ParseException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import it.unipi.ing.mim.deep.DNNExtractor;
import it.unipi.ing.mim.deep.DetailedImage;
import it.unipi.ing.mim.deep.ImgDescriptor;
import it.unipi.ing.mim.deep.Parameters;
import it.unipi.ing.mim.deep.tools.FeaturesStorage;
import it.unipi.ing.mim.deep.tools.Output;

public class ElasticImgSearching implements AutoCloseable {

	private RestHighLevelClient client;
	
	private Pivots pivots;
	private Map<String,ImgDescriptor> imgDescMap;
	private int topKSearch;
		
	public static void main(String[] args) throws Exception {
		
		try (ElasticImgSearching imgSearch = new ElasticImgSearching(Parameters.PIVOTS_FILE, Parameters.TOP_K_QUERY)) {
			//Image Query File
			File imgQuery = new File(Parameters.SRC_FOLDER, "im121.jpg");
			
			DNNExtractor extractor = new DNNExtractor();
			
			float[] imgFeatures = extractor.extract(imgQuery, Parameters.DEEP_LAYER);
			
			ImgDescriptor query = new ImgDescriptor(imgFeatures, imgQuery.getName());
					
			long time = -System.currentTimeMillis();
			List<ImgDescriptor> res = imgSearch.search("dog", Parameters.K);
			time += System.currentTimeMillis();
			System.out.println("Search time: " + time + " ms");
			
			Output.toHTML(res, Parameters.BASE_URI, Parameters.RESULTS_HTML_ELASTIC);
			
			//Uncomment for the optional step
			res = imgSearch.reorder(query, res);
			Output.toHTML(res, Parameters.BASE_URI, Parameters.RESULTS_HTML_REORDERED);
		}
	}
	
	//TODO
	public ElasticImgSearching(File pivotsFile, int topKSearch) throws ClassNotFoundException, IOException {
		pivots = new Pivots(pivotsFile);
		this.topKSearch = topKSearch;
		RestClientBuilder builder = RestClient.builder(new HttpHost("localhost", 9200, "http"));
		client = new RestHighLevelClient(builder);		
		List<ImgDescriptor> l = FeaturesStorage.load(Parameters.STORAGE_FILE);
		imgDescMap = new HashMap<String, ImgDescriptor>();
		for(ImgDescriptor ll:l) imgDescMap.put(ll.getId(), ll);
	}
	
	//TODO
	public void close() throws IOException {
		client.close();
	}
	
	//TODO
	public List<ImgDescriptor> search(String queryF,int k) throws ParseException, IOException, ClassNotFoundException{
		List<ImgDescriptor> res = new ArrayList<ImgDescriptor>();
		SearchRequest sr = composeSearch(queryF, k,Fields.CLASS_NAME);
		//perform elasticsearch search
		@SuppressWarnings("deprecation")
		SearchResponse searchResponse = client.search(sr);
		return performSearch(searchResponse);
	}
	public List<ImgDescriptor> search(ImgDescriptor queryF, int k) throws ParseException, IOException, ClassNotFoundException{	
		//convert queryF to text
		String f2t = pivots.features2Text(queryF, topKSearch);
		//call composeSearch to get SearchRequest object
		SearchRequest sr = composeSearch(f2t, k,Fields.BOUNDING_BOX_FEAT);
		//perform elasticsearch search
		@SuppressWarnings("deprecation")
		SearchResponse searchResponse = client.search(sr);
		return performSearch(searchResponse);
	}
	private List<ImgDescriptor> performSearch(SearchResponse searchResponse) {
		List<ImgDescriptor> res = new ArrayList<ImgDescriptor>();
		SearchHit[] hits = searchResponse.getHits().getHits();
		//LOOP to fill res
			//for each result retrieve the ImgDescriptor from imgDescMap and call setDist to set the score
		for(int i = 0; i < hits.length; ++i) {
			String id = (String)hits[i].getSourceAsMap().get(Fields.IMG_ID);
			//STEP 1: ImgDescriptor im = new ImgDescriptor(null,id);
			//STEP 2:
			ImgDescriptor im = imgDescMap.get(id);
			im.setDist(hits[i].getScore());
			res.add(im);
		}	
		return res;
	}
	
	//TODO
	private SearchRequest composeSearch(String query, int k, String field) {
		//Initialize SearchRequest and set query and k
		SearchRequest searchRequest = null;
		QueryBuilder qb = QueryBuilders.multiMatchQuery(query, field);
		SearchSourceBuilder sb = new SearchSourceBuilder();
		sb.query(qb);
		sb.size(k);
		searchRequest = new SearchRequest(Parameters.INDEX_NAME);
		searchRequest.types("doc");
		searchRequest.source(sb);
	    return searchRequest;
	}
	
	//TODO
	public List<ImgDescriptor> reorder(ImgDescriptor queryF, List<ImgDescriptor> res) throws IOException, ClassNotFoundException {
		//Optional Step!!!
		//LOOP
		//for each result evaluate the distance with the query, call  setDist to set the distance, then sort the results
		for(ImgDescriptor r:res) r.distance(queryF);
		Collections.sort(res);
		return res;
	}
}
