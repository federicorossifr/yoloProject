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
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
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
	private int topKSearch;
		
	public static void main(String[] args) throws Exception {
		StatusLogger.getLogger().setLevel(Level.OFF);		
		try (ElasticImgSearching imgSearch = new ElasticImgSearching(Parameters.PIVOTS_FILE, Parameters.TOP_K_QUERY)) {
			//Image Query File
			File imgQuery = new File(Parameters.SRC_FOLDER, "im2.jpg");
			
			DNNExtractor extractor = new DNNExtractor();
			
			float[] imgFeatures = extractor.extract(imgQuery, Parameters.DEEP_LAYER);
			
			ImgDescriptor query = new ImgDescriptor(imgFeatures, imgQuery.getName());
					
			long time = -System.currentTimeMillis();
			List<ImgDescriptor> res = imgSearch.search("nikon", Parameters.K);
			time += System.currentTimeMillis();
			System.out.println("Search time: " + time + " ms");
			Output.toHTML(res, Parameters.BASE_URI, Parameters.RESULTS_HTML_REORDERED);
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
		for(ImgDescriptor ll:l) imgDescMap.put(ll.getId(), ll);
	}
	
	public void close() throws IOException {
		client.close();
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
		SearchResponse searchResponse = getSearchResponse(queryF, k,Fields.CLASS_NAME);
		List<ImgDescriptor> resClass =  performSearch(searchResponse,false);
		//perform search by tags and add them if they are not already present with bbox
		searchResponse = getSearchResponse(queryF, k, Fields.FLICKR_TAGS);
		List<ImgDescriptor> resTag =  performSearch(searchResponse,true);
		return reorder(queryF,joinImgDescriptors(resTag, resClass));
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
		for (ImgDescriptor im: resTag) {
			if(!resClass.contains(im))
				resClass.add(im);
		}
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
		SearchResponse searchResponse = getSearchResponse(f2t, k,Fields.BOUNDING_BOX_FEAT);
		return reorder(queryF,performSearch(searchResponse,false));
	}
	
	/**
	 * Call composeSearch to get SearchRequest object and perform elasticsearch search
	 * @param queryF
	 * @param k
	 * @param field
	 * @return
	 * @throws IOException
	 */
	private SearchResponse getSearchResponse(String queryF,int k,String field) throws IOException{
		SearchRequest sr = composeSearch(queryF, k,field);
		return client.search(sr);
	}
	
    /**
     * For each result retrieve the ImgDescriptor from imgDescMap and call setDist to set the score and add it to list of ImgDescritpor
     * @param searchResponse
     * @param tags
     * @return
     */
	private List<ImgDescriptor> performSearch(SearchResponse searchResponse, boolean tags) {
		List<ImgDescriptor> res = new ArrayList<ImgDescriptor>();
		SearchHit[] hits = searchResponse.getHits().getHits();
		for(int i = 0; i < hits.length; ++i) {
			String id = (String)hits[i].getSourceAsMap().get(Fields.IMG_ID);
			ImgDescriptor im = imgDescMap.get(id);
			im.setDist(hits[i].getScore());
			if(tags)
				im.setBoundingBoxIndex(Parameters.NO_BOUNDING_BOX);
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
	private SearchRequest composeSearch(String query, int k, String field) {
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
