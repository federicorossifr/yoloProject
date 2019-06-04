package it.unipi.ing.mim.img.elasticsearch;

import java.io.File;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusLogger;


public class ElasticImgSearching implements AutoCloseable {

	private RestHighLevelClient client;
	private Pivots pivots;
	private int topKSearch;
	private Boolean useAccuracyClassScore; 
	private HashMap<String,ImgDescriptor> imageDescriptorMap = new HashMap<String, ImgDescriptor>();
	private enum TAGS_SEARCH_MODE  {FLICKR,YOLO};
	public static void main(String[] args) throws Exception {
		StatusLogger.getLogger().setLevel(Level.OFF);		
		try (ElasticImgSearching imgSearch = new ElasticImgSearching(Parameters.PIVOTS_FILE, Parameters.TOP_K_QUERY,false)) {
			//Image Query File
			File imgQuery = new File(Parameters.SRC_FOLDER, "im10001.jpg");
			
			DNNExtractor extractor = new DNNExtractor();
			
			float[] imgFeatures = extractor.extract(imgQuery, Parameters.DEEP_LAYER);
			
			ImgDescriptor query = new ImgDescriptor(imgFeatures, imgQuery.getName());
					
			long time = -System.currentTimeMillis();
			List<ImgDescriptor> res = imgSearch.searchByClass("(dog AND NOT person) OR (dog AND NOT person)",100);
			time += System.currentTimeMillis();
			System.out.println("Search time: " + time + " ms");
			Output.toHTML(res, Parameters.BASE_URI, Parameters.RESULTS_HTML_ELASTIC);
		}
	}
	
	/**
	 * Constructor for elastic image searching
	 * @param pivotsFile
	 * @param useAccuracyForClassScore
	 * @param topKSearch
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public ElasticImgSearching(File pivotsFile, int topKSearch, Boolean useAccuracyForClassScore) throws ClassNotFoundException, IOException {
		pivots = new Pivots(pivotsFile);
		this.topKSearch = topKSearch;
		this.useAccuracyClassScore = useAccuracyForClassScore;
		RestClientBuilder builder = RestClient.builder(new HttpHost("localhost", 9200, "http"));
		client = new RestHighLevelClient(builder);	
		List<ImgDescriptor> l = FeaturesStorage.load(Parameters.STORAGE_FILE);		
		imageDescriptorMap = new HashMap<String, ImgDescriptor>();
	    for(ImgDescriptor ll:l)
	    	imageDescriptorMap.put(ll.toString(), ll);
	    
	}
	
	
	public void close() throws IOException {
		client.close();
	}
	
	public void setAccuracyForClassScore(Boolean a) {
		this.useAccuracyClassScore = a;
	}
	
	public Boolean getAccuracyForClassScore() {
		return this.useAccuracyClassScore;
	}
	/**
	 * Image search by class name and afterwards by tag
	 * @param queryF
	 * @param k
	 * @return
	 * @throws ParseException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public List<ImgDescriptor> search(String queryF,int k) throws ParseException, IOException, ClassNotFoundException{
		List<ImgDescriptor> resClass =  searchByClass(queryF, k);
		List<ImgDescriptor> resTag =  searchByTag(queryF, k);
		List<ImgDescriptor> res = reorder(joinImgDescriptors(resTag, resClass));
		k = k>res.size()?res.size():k;
		return res.subList(0, k);
	}
	/**
	 * Search only by human tag
	 * @param queryF
	 * @param k
	 * @return
	 * @throws ParseException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public List<ImgDescriptor> searchByTag(String query,int k) throws ParseException, IOException, ClassNotFoundException{
		SearchResponse searchResponse = getSearchResponse(query, k, Fields.FLICKR_TAGS,Parameters.TAGS_INDEX_NAME);
		List<ImgDescriptor> resTag =  performTextualSearch(searchResponse,TAGS_SEARCH_MODE.FLICKR,query);
		resTag = reorder(resTag);
		k = k>resTag.size()?resTag.size():k;
		return normalizeMax(resTag.subList(0, k));	
	}
	 
	public List<ImgDescriptor> searchByClass(String query,int k) throws ParseException, IOException, ClassNotFoundException{
		SearchResponse searchResponse = getSearchResponse(query, k, Fields.YOLO_TAGS,Parameters.TAGS_INDEX_NAME);
		List<ImgDescriptor> resClass =  performTextualSearch(searchResponse,TAGS_SEARCH_MODE.YOLO,query);
		resClass = reorder(resClass);
		k = k>resClass.size()?resClass.size():k;
		return normalizeMax(resClass.subList(0, k));
	}	 
	
	private ArrayList<String> getQueryTerms(String query) {
		// Replace parentheses and boolean operators with one space 
		String queryTerms = query.replace("(", " ").replace(")", " ")
				.replace(" AND NOT ", " ")
				.replace(" OR NOT ", " ")
				.replace(" AND ", " ")
				.replace(" OR ", " ");
				
		
		// Replace multiple spaces with one space
		queryTerms = queryTerms.replaceAll(" +", " ").trim();

		System.out.println("Query: " + query);
		System.out.println("Polished query: " + queryTerms);
		
		// Split string by space to find query terms
		ArrayList<String> result = new ArrayList<String>(); 
		for(String s: queryTerms.split(" "))
		{
			System.out.println(s);
			result.add(s);
		}
		return result;
	}
	
	/**
	 * Method to perform a textual search (either on YOLO_TAGS or FLICKR_TAGS)
	 * 
	 * @param searchResponse
	 * @param mode
	 * @param query
	 * @return
	 * @throws IOException
	 */
	private List<ImgDescriptor> performTextualSearch(SearchResponse searchResponse, TAGS_SEARCH_MODE mode,String query) throws IOException {
		//Build an index for the query terms to be used later
		Set<String> queryMap = new HashSet<>();
		
		for(String s: getQueryTerms(query)) queryMap.add(s);
		
		List<ImgDescriptor> res = new ArrayList<ImgDescriptor>();
		SearchHit[] hits = searchResponse.getHits().getHits();
		
		for(SearchHit h:hits) {
			Map<String,Object> hitContent = h.getSourceAsMap();
			String imageId = (String)hitContent.get(Fields.IMG_ID);
			if(mode == TAGS_SEARCH_MODE.FLICKR) {
				ImgDescriptor imgDesc = new ImgDescriptor(null, imageId,Parameters.NO_BOUNDING_BOX);
				imgDesc.setDist(h.getScore());
				res.add(imgDesc);
			} else {
				DetailedImage di = new DetailedImage(imageId);
				ArrayList<String> classNames = di.getClassNames();
				for(int i = 0; i < classNames.size();++i) {
					if(queryMap.contains(classNames.get(i))) {
						ImgDescriptor imgDesc = new ImgDescriptor(null,imageId,i);
						
						float score = h.getScore();
						if(this.useAccuracyClassScore)
							score *= di.getScoreByIndex(i);
						imgDesc.setDist(score);

						res.add(imgDesc);
					}
				}
			}
			
		}
		return res;
	}

	/**
	 * Search only by bounding box class 
	 * @param queryF
	 * @param k
	 * @return
	 * @throws ParseException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private List<ImgDescriptor> normalizeMax(List<ImgDescriptor> sorted){
		if(sorted.size() <= 0)
			return sorted;

		Double max = sorted.get(0).getDist();
		if(max==0.0)
			return sorted;
		for (ImgDescriptor im: sorted)
			im.setDist(im.getDist()/max);
		return sorted;
	}
	

	
	
	/**
	 * Image search by class name and afterwards by tag, matches by tag are not added if already present
	 * @param queryF
	 * @param k
	 * @return
	 * @throws ParseException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private List<ImgDescriptor> joinImgDescriptors( List<ImgDescriptor> resTag, List<ImgDescriptor> resClass){
		HashSet<String> resClassSet = new HashSet<String>();
		for (ImgDescriptor im: resClass)
			resClassSet.add(im.getId());

		for (ImgDescriptor im: resTag)
			if(!resClassSet.contains(im.getId()))
				resClass.add(im);
		return resClass;
	}

	/**
	 * Image search by example
	 * @param queryF
	 * @param k
	 * @return
	 * @throws ParseException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public List<ImgDescriptor> search(ImgDescriptor queryF, int k) throws ParseException, IOException, ClassNotFoundException{	
		//convert queryF to text
		String f2t = pivots.features2Text(queryF, topKSearch);
		SearchResponse searchResponse = getSearchResponse(f2t, k, Fields.BOUNDING_BOX_FEAT, Parameters.FEATURE_INDEX_NAME);
		List<ImgDescriptor> res = reorder(queryF,performExampleSearch(searchResponse, false));
		k = k>res.size()?res.size():k;
		return res.subList(0, k);
	}
	
	/**
	 * Call composeSearch to get SearchRequest object and perform elasticsearch search
	 * @param queryF
	 * @param k
	 * @param field
	 * @return
	 * @throws IOException
	 */
	private SearchResponse getSearchResponse(String queryF,int k,String field,String indexName) throws IOException{
		SearchRequest sr = composeSearch(queryF, k,field,indexName);
		return client.search(sr,RequestOptions.DEFAULT);
	}
	
    /**
     * For each result retrieve the ImgDescriptor from imgDescMap and call setDist to set the score and add it to list of ImgDescritpor
     * @param searchResponse
     * @param tags
     * @return
     */
	private List<ImgDescriptor> performExampleSearch(SearchResponse searchResponse, boolean flickrTags) throws IOException{
		List<ImgDescriptor> res = new ArrayList<ImgDescriptor>();
		SearchHit[] hits = searchResponse.getHits().getHits();
		for(int i = 0; i < hits.length; ++i) {
			String id = (String)hits[i].getSourceAsMap().get(Fields.IMG_ID);
			String bbox_index = (String)hits[i].getSourceAsMap().get(Fields.BOUNDING_BOX);
			ImgDescriptor im = imageDescriptorMap.get(ImgDescriptor.toString(id,bbox_index));
			res.add(im);
		}
		return res;
	}
	
	/**
	 * Initialize SearchRequest and set query and k
	 * @param query
	 * @param k
	 * @param field
	 * @return
	 */
	private SearchRequest composeSearch(String query, int k, String field,String indexName) {
		SearchRequest searchRequest = null;
		//QueryBuilder qb = QueryBuilders.multiMatchQuery(query, field);
		QueryBuilder qb = QueryBuilders.queryStringQuery(query).defaultField(field);
		SearchSourceBuilder sb = new SearchSourceBuilder();
		sb.query(qb);
		sb.size(k);
		searchRequest = new SearchRequest(indexName);
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
	public List<ImgDescriptor> reorder(List<ImgDescriptor> res) throws IOException, ClassNotFoundException {
		Collections.sort(res,Collections.reverseOrder());
		return res;
	}
}
