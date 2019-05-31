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
	public static final String HTTP_BASE_URI = "http://localhost:9001/mirflickr/";

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
	mappings.addMap("GET", "/search_byimg", new AbstractResponse() {
            @Override
            public Response getResponse(Request req) {
            	String htmlres = "";

            	try {
            		String filename = req.getAttribute("file");
            		if(filename.equals("null"))
            			throw new Exception("file field required");

        			//Image Query File
        			File imgQuery = new File(Parameters.SRC_FOLDER, filename);

        			DNNExtractor extractor = new DNNExtractor();
        			float[] imgFeatures = extractor.extract(imgQuery, Parameters.DEEP_LAYER);
        			ImgDescriptor query = new ImgDescriptor(imgFeatures, imgQuery.getName());
        			List<ImgDescriptor> res = imgSearch.search(query,100);

        			htmlres = Output.generateHTML(res, HTTP_BASE_URI);
            	} catch(Exception e) {
            		StringWriter errors = new StringWriter();
            		e.printStackTrace(new PrintWriter(errors));
            		htmlres = "Exception: " + errors.toString();
            	}

                return new Response(htmlres);
            }
        });

        mappings.addMap("GET", "/search_bytext", new AbstractResponse() {
            @Override
            public Response getResponse(Request req) {
            	String htmlres = "";

            	try {
            		String querytext = req.getAttribute("text");
            		if(querytext.equals("null"))
            			throw new Exception("text field required");

        			List<ImgDescriptor> res = imgSearch.search(querytext,100);

        			htmlres = Output.generateHTML(res, HTTP_BASE_URI);
            	} catch(Exception e) {
            		StringWriter errors = new StringWriter();
            		e.printStackTrace(new PrintWriter(errors));
            		htmlres = "Exception: " + errors.toString();
            	}

                return new Response(htmlres);
            }
        });

        mappings.addMap("GET", "/search_bytag", new AbstractResponse() {
            @Override
            public Response getResponse(Request req) {
                String htmlres = "";

                try {
                        String querytext = req.getAttribute("text");
                        if(querytext.equals("null"))
                                throw new Exception("text field required");

                                List<ImgDescriptor> res = imgSearch.searchByTag(querytext,100);

                                htmlres = Output.generateHTML(res, HTTP_BASE_URI);
                } catch(Exception e) {
                        StringWriter errors = new StringWriter();
                        e.printStackTrace(new PrintWriter(errors));
                        htmlres = "Exception: " + errors.toString();
                }

                return new Response(htmlres);
            }
        });

        mappings.addMap("GET", "/search_byclass", new AbstractResponse() {
            @Override
            public Response getResponse(Request req) {
                String htmlres = "";

                try {
                        String querytext = req.getAttribute("text");
                        if(querytext.equals("null"))
                                throw new Exception("text field required");

                                List<ImgDescriptor> res = imgSearch.searchByClass(querytext,100);

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
            server = new HttpServer(9000, mappings);
            Request req = server.accept();
            server.sendResponse(req);
            server.shut();
        }
    }
}
