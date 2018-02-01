package crawler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class PubMed {
	private static Client client;
	private static String baseurl = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/";
	private static String api_key = "7f7ad22b76bc7d1a40952b2213cf5f050708";

	public static void initialize() {
		if (client==null)
			client = Client.create();
		
		String directoryName = "papers";
		boolean success = (new File(directoryName)).mkdirs();
		if (success) {
			System.out.println("Directory folder " + directoryName + "was created");
		}
	}

	public static void fetchByTerm(String term) throws JSONException, InterruptedException {
		initialize();
		
		String db = "pubmed";
		String usehistory = "y";
		int retstart = 0;
		int retmax = 1;
		String retmode = "json";
		String rettype = "uilist";
		String url = baseurl + "esearch.fcgi?";

		String parameters = "db=" + db + "&" + "term=" + term + "&"
		// + "retmax=" + retmax + "&"
				+ "usehistory=" + usehistory + "&" + "retmode=" + retmode + "&" + "rettype=" + rettype + "&"
				+ "api_key=" + api_key;

		String query = url + parameters;

		WebResource webResource = client.resource(query);

		ClientResponse response = webResource.get(ClientResponse.class);

		String responseBody = response.getEntity(String.class);
		System.out.println(responseBody);

		JSONObject responseJson = new JSONObject(responseBody);
		JSONObject esearchresult = responseJson.getJSONObject("esearchresult");

		System.out.println("------------------------------- Files ------------------------------- ");
		//
		retstart = 0;
		
		//The maximum number of papers to be downloaded per call is 10k (recomendation of eFetch docs)
		retmax = 10000;
		String webenv = esearchresult.getString("webenv");
		String key_query = esearchresult.getString("querykey");
		int count = esearchresult.getInt("count");

		url = baseurl + "efetch.fcgi?";

		int id = 0;
		while (retstart < count) {
			System.out.println(retstart);
			parameters = "db=" + db + "&" + "query_key=" + key_query + "&" + "WebEnv=" + webenv + "&"
					+ "rettype=abstract&" + "retmode=text" + "&" + "retstart=" + retstart + "&" + "retmax=" + retmax
					+ "&" + "api_key=" + api_key;

			query = url + parameters;

			webResource = client.resource(query);

			response = webResource.get(ClientResponse.class);

			responseBody = response.getEntity(String.class);

			retstart = retstart + retmax;
			Thread.sleep(1000);

			String[] articles = responseBody.split("\n\n\n\\d+. ");
			System.out.println(responseBody);
			
			for (String article : articles) {
				try (BufferedWriter bw = new BufferedWriter(new FileWriter("papers/" + id + ".txt"))) {
					String content = article;
					bw.write(content);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				
//				System.out.println(article);
//				System.out.println("------------");
				id++;
			}
		}

	}

	public static void main(String[] args) throws JSONException, InterruptedException {
		initialize();
		fetchByTerm("respiratory+failure");
	}

	public static JSONArray eSearch() throws JSONException {
		String db = "pubmed";
		String term = "respiratory+failure";
		String usehistory = "y";
		String retstart = "0";
		String retmax = "1";
		String retmode = "json";
		String api_key = "7f7ad22b76bc7d1a40952b2213cf5f050708";
		String url = baseurl + "esearch.fcgi?";

		String parameters = "db=" + db + "&" + "term=" + term + "&" + "api_key=" + api_key + "&"
		// + "retmax=" + retmax + "&"
				+ "usehistory=" + usehistory;
		// + "retmode=" + retmode;

		String query = url + parameters;

		WebResource webResource = client.resource(query);

		ClientResponse response = webResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
				.get(ClientResponse.class);
		System.out.println(response);

		String responseBody = response.getEntity(String.class);
		System.out.println(responseBody);
		JSONObject responseJson = new JSONObject(responseBody);

		JSONObject esearchresult = responseJson.getJSONObject("esearchresult");

		String webenv = esearchresult.getString("webenv");

		int key_query = Integer.parseInt(esearchresult.getString("querykey"));
		JSONArray idlist = esearchresult.getJSONArray("idlist");

		System.out.println("Sumary ------------------------------- ");

		url = baseurl + "efetch.fcgi?";

		parameters = "db=" + db + "&" + "query_key=" + key_query + "&" + "WebEnv=" + webenv + "&" + "rettype=abstract&"
				+ "retmode=text";

		query = url + parameters;

		webResource = client.resource(query);

		response = webResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
				.get(ClientResponse.class);

		// System.out.println(response);
		responseBody = response.getEntity(String.class);
		System.out.println(responseBody);

		return idlist;
	}

	public static void eFetch(JSONArray idlist) throws JSONException {
		String db = "pubmed";
		String id = "";
		String rettype = "abstract";
		String retmode = "text";

		for (int i = 0; i < 1; i++) {
			id += idlist.get(i) + ",";
		}
		id = id.substring(0, id.length() - 1);

		String baseurl = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?";
		String parameters = "db=" + db + "&" + "rettype=" + rettype + "&" + "retmode=" + retmode + "&" + "id=" + id;

		String query = baseurl + parameters;

		WebResource webResource = client.resource(query);

		ClientResponse response = webResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
				.get(ClientResponse.class);
		System.out.println(response);
		String responseBody = response.getEntity(String.class);
		System.out.println(responseBody);
	}
}
