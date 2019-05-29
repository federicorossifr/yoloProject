package it.unipi.ing.mim.img.elasticsearch;

import java.io.File;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpHost;
import org.apache.lucene.queryparser.classic.ParseException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import it.unipi.ing.mim.deep.DNNExtractor;
import it.unipi.ing.mim.deep.ImgDescriptor;
import it.unipi.ing.mim.deep.Parameters;
import it.unipi.ing.mim.deep.tools.FeaturesStorage;
import it.unipi.ing.mim.deep.tools.Output;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusLogger;


public class ElasticImgSearching implements AutoCloseable {

	private RestHighLevelClient client;
	private Pivots pivots;
	private Map<String,ImgDescriptor> imgDescMap;
	
	private Map<String,ImgDescriptor> resultMap = new HashMap<>();
	private Map<String,Boolean> presenceMap = new HashMap<>();
	private int topKSearch;
		
	public static void main(String[] args) throws Exception {
		StatusLogger.getLogger().setLevel(Level.OFF);		
		try (ElasticImgSearching imgSearch = new ElasticImgSearching(Parameters.PIVOTS_FILE, Parameters.TOP_K_QUERY)) {
			//Image Query File
			File imgQuery = new File(Parameters.SRC_FOLDER, "im10001.jpg");
			
			DNNExtractor extractor = new DNNExtractor();
			
			float[] imgFeatures = extractor.extract(imgQuery, Parameters.DEEP_LAYER);
			
			ImgDescriptor query = new ImgDescriptor(imgFeatures, imgQuery.getName());
					
			long time = -System.currentTimeMillis();
			List<ImgDescriptor> res = imgSearch.search("car",200);
			time += System.currentTimeMillis();
			System.out.println("Search time: " + time + " ms");
			Output.toHTML(res, Parameters.BASE_URI, Parameters.RESULTS_HTML_ELASTIC);
		}
	}
	
	/**
	 * Constructor for elastic image searching
	 * @param pivotsFile
	 * @param topKSearch
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public ElasticImgSearching(File pivotsFile, int topKSearch) throws ClassNotFoundException, IOException {
		pivots = new Pivots(pivotsFile);
		this.topKSearch = topKSearch;
		RestClientBuilder builder = RestClient.builder(new HttpHost("localhost", 9200, "http"));
		client = new RestHighLevelClient(builder);		
		List<ImgDescriptor> l = FeaturesStorage.load(Parameters.STORAGE_FILE);
		imgDescMap = new HashMap<String, ImgDescriptor>();
		for(ImgDescriptor ll:l) {
			imgDescMap.put(ll.toString(), ll);
		}
	}
	
	public void close() throws IOException {
		client.close();
	}
	
	/**
	 * Image search by full text tag or class
	 * @param queryF
	 * @param k
	 * @return
	 * @throws ParseException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public List<ImgDescriptor> search(String queryF,int k) throws ParseException, IOException, ClassNotFoundException{
		resultMap.clear();
		presenceMap.clear();
		SearchResponse searchResponse = getSearchResponse(queryF, k);
		SearchHit[] searchHits = searchResponse.getHits().getHits();
		for(SearchHit sh: searchHits) {
			
			Map<String,Object> hitContent = sh.getSourceAsMap();
			
			// Extract information needed to retrieve the ImageDescriptor
			String id = (String)hitContent.get(Fields.IMG_ID);
			int bbox_index = Integer.parseInt((String)hitContent.get(Fields.BOUNDING_BOX));
			String className = (String)hitContent.get(Fields.CLASS_NAME);
			ImgDescriptor res = imgDescMap.get(ImgDescriptor.toString(id, bbox_index));
			
			//Set the "distance" to the score, will order in reverse order
			res.setDist(sh.getScore());
			
			System.out.println(res.getId()+" "+res.getBoundingBoxIndex());
			
			//Partial key is used to access the presenceMap
			String partialKey = res.getId();
			//Key is used to acccess the resultMap
			String key = res.toString();
			// If image is present in one of its form inside the presenceMap
			if(presenceMap.containsKey(partialKey)) { 
				//If bounding box exists and matches with the class query
				if(res.getBoundingBoxIndex() >= 0 && queryF.contains(className)) {
					//Insert it into resultMap
					resultMap.put(key,res);
					//Check if an unbounded image exists in the result
					//The unbounded image will have -1 as bounding box index
					String unboundedKey = ImgDescriptor.toString(partialKey, -1);
					if(resultMap.containsKey(unboundedKey)) {
						//If present, remove it
						resultMap.remove(unboundedKey);
					}
				// If no bounding box available we skip it
				} 
			// If no image with partialKey ID has been inseted so far, insert it 
			} else { 
				presenceMap.put(partialKey,true);
				resultMap.put(key,res);
			}
		}
		//The entry set in resultMap is the actual result
		List<ImgDescriptor> response = new ArrayList<ImgDescriptor>();		
		response.clear();
		for(ImgDescriptor r: resultMap.values()) {
			response.add(r);
		}
		return reorder(queryF, response);
	}
	
	/**
	 * Image search by example, by extracting fc6 features
	 * @param queryF
	 * @param k
	 * @return
	 * @throws ParseException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public List<ImgDescriptor> search(ImgDescriptor queryF, int k) throws ParseException, IOException, ClassNotFoundException{	
		//convert queryF to surrogate text representation
		String f2t = pivots.features2Text(queryF, topKSearch);
		SearchResponse searchResponse = getSearchResponse(f2t, k,Fields.BOUNDING_BOX_FEAT);
		SearchHit[] searchHits = searchResponse.getHits().getHits();
		List<ImgDescriptor> response = new ArrayList<ImgDescriptor>();
		for(SearchHit sh:searchHits) {
			Map<String,Object> hitContent = sh.getSourceAsMap();			
			String id = (String)hitContent.get(Fields.IMG_ID);
			int bbox_index = Integer.parseInt((String)hitContent.get(Fields.BOUNDING_BOX));
			ImgDescriptor res = imgDescMap.get(ImgDescriptor.toString(id, bbox_index));
			response.add(res);
		}
		return reorder(queryF,response);
	}
	
	/**
	 * Call composeSearch to get SearchRequest object and perform elasticsearch search
	 * @param queryF
	 * @param k
	 * @param field
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("deprecation")
	private SearchResponse getSearchResponse(String queryF,int k,String field) throws IOException{
		SearchRequest sr = composeSearch(queryF, k,field);
		return client.search(sr);
	}
	
	/**
	 * Call composeSearch to get SearchRequest object and perform elasticsearch search
	 * @param queryF
	 * @param k
	 * @param field
	 * @return
	 * @throws IOException
	 */
	@SuppressWarnings("deprecation")
	private SearchResponse getSearchResponse(String queryF,int k) throws IOException{
		SearchRequest sr = composeSearch(queryF, k);
		return client.search(sr);
	}	
	

	
	/**
	 * Initialize SearchRequest and set query and k
	 * @param query
	 * @param k
	 * @param field
	 * @return
	 */
	private SearchRequest composeSearch(String query, int k) {
		SearchRequest searchRequest = null;
		QueryBuilder qb = QueryBuilders.multiMatchQuery(query, Fields.CLASS_NAME,Fields.FLICKR_TAGS);
		SearchSourceBuilder sb = new SearchSourceBuilder();
		sb.query(qb);
		sb.size(k);
		searchRequest = new SearchRequest(Parameters.INDEX_NAME);
		searchRequest.types("doc");
		searchRequest.source(sb);
	    return searchRequest;
	}
	
	/**
	 * Initialize SearchRequest and set query and k
	 * @param query
	 * @param k
	 * @param field
	 * @return
	 */
	private SearchRequest composeSearch(String query, int k,String field) {
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
	
	/**
	 * For each result evaluate the distance with the query, call  setDist to set the distance, then sort the results
	 * @param queryF
	 * @param res
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public List<ImgDescriptor> reorder(ImgDescriptor queryF, List<ImgDescriptor> res) throws IOException, ClassNotFoundException {
		for(ImgDescriptor r:res) r.distance(queryF);
		Collections.sort(res);
		return res;
	}
	
	/**
	 * Sort the results based on their score, higher is better hence the sort is inverted w.r.t. search by example
	 * @param queryF
	 * @param res
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public List<ImgDescriptor> reorder(String queryF, List<ImgDescriptor> res) throws IOException, ClassNotFoundException {
		Collections.sort(res,Collections.reverseOrder());
		return res;
	}
}
