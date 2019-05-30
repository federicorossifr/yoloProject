package it.unipi.ing.mim.http;

import it.unipi.ing.mim.deep.DNNExtractor;
import it.unipi.ing.mim.deep.ImgDescriptor;
import it.unipi.ing.mim.deep.Parameters;
import it.unipi.ing.mim.img.elasticsearch.ElasticImgSearching;
import it.unipi.ing.mim.deep.tools.Output;
import it.unipi.ing.mim.http.api.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

public class Runner {
	public static final String HTTP_BASE_URI = "localhost:9000/img/mirflickr/";
	
	private static ElasticImgSearching imgSearch;
	static {
		try {
			imgSearch = new ElasticImgSearching(Parameters.PIVOTS_FILE, Parameters.TOP_K_QUERY);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

    public static void main(String[] args) throws IOException {
        Mappings mappings = new Mappings();
        mappings.addMap("GET", "/search", new AbstractResponse() {
            @Override
            public Response getResponse(Request req) {
            	String htmlres = "";

            	try {
        			//Image Query File
        			File imgQuery = new File(Parameters.SRC_FOLDER, "im10001.jpg");

        			DNNExtractor extractor = new DNNExtractor();
        			float[] imgFeatures = extractor.extract(imgQuery, Parameters.DEEP_LAYER);
        			ImgDescriptor query = new ImgDescriptor(imgFeatures, imgQuery.getName());
        			List<ImgDescriptor> res = imgSearch.search("car",5);

        			htmlres = Output.generateHTML(res, HTTP_BASE_URI);
            	} catch(Exception e) {
            		StringWriter errors = new StringWriter();
            		e.printStackTrace(new PrintWriter(errors));
            		htmlres = "Exception: " + errors.toString();
            	}

                return new Response(htmlres);
            }
        });

        HttpServer server;
        while(true) {
            server = new HttpServer(8888, mappings);
            Request req = server.accept();
            server.sendResponse(req);
            server.shut();
        }
    }
}
