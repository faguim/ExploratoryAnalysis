package crawler;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;

public class PubMed {
	private static ClientConfig clientConfig;
	private static Client client;
	private static String baseurl = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/";

	public static void initialize() {
		client = Client.create();
	}

	public static JSONArray eSearch() throws JSONException, org.codehaus.jettison.json.JSONException {
		String db = "pubmed";
		String term = "asthma[mesh]+AND+leukotrienes[mesh]+AND+2009[pdat]";
		String usehistory = "y";
		String retstart = "0";
		String retmax = "1";
		String retmode = "json";
		String webenv = "NCID_1_20056208_130.14.18.34_9001_1516985479_53996333_0MetA0_S_MegaStore_F_1";
		
		String url = baseurl + "esearch.fcgi?";
		
		String parameters = "db=" + db + "&" 
						  + "term=" + term + "&" 
						  + "retmax=" + retmax + "&"
						  + "usehistory="+usehistory + "&"
						  + "webenv=" + webenv + "&" 
						  + "retmode=" + retmode;

		String query = url + parameters;

		WebResource webResource = client.resource(query);

		ClientResponse response = webResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
				.get(ClientResponse.class);
		 System.out.println(response);

		String responseBody = response.getEntity(String.class);
		 System.out.println(responseBody);
		JSONObject responseJson = new JSONObject(responseBody);

		JSONObject esearchresult = responseJson.getJSONObject("esearchresult");
		
		String key_query = esearchresult.getString("querykey");
		JSONArray idlist = esearchresult.getJSONArray("idlist");
		
		System.out.println("Sumary ------------------------------- ");
		
		url = baseurl + "efetch.fcgi?";
		 
		parameters  = "db="+ db + "&"
				+ "query_key="+ key_query + "&"
				+ "WebEnv=" + webenv + "&"
				+ "rettype=abstract&" 
				+ "retmode=json";
		
		query = url + parameters;

		webResource = client.resource(query);

		response = webResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
				.get(ClientResponse.class);
		 System.out.println(response);
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
		String parameters = "db=" + db + "&" 
							+ "rettype=" + rettype + "&" 
							+ "retmode=" + retmode + "&" 
							+ "id=" + id;

		String query = baseurl + parameters;

		WebResource webResource = client.resource(query);

		ClientResponse response = webResource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
				.get(ClientResponse.class);
		System.out.println(response);
		String responseBody = response.getEntity(String.class);
		System.out.println(responseBody);

	}

	public static void main(String[] args) throws JSONException, org.codehaus.jettison.json.JSONException {
		initialize();
//		eFetch();
				eSearch();
	}
}
