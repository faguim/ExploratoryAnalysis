package crawler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import util.HttpClient;

public class PubMed {
	private static Client client;
	private static String baseurl = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/";
	private static String api_key = "7f7ad22b76bc7d1a40952b2213cf5f050708";

	private static final String XML_DIRECTORY = "papers/xml/";
	private static final String JSON_DIRECTORY = "papers/json/";

	public static void initialize() {
		if (client == null)
			client = Client.create();

		(new File(XML_DIRECTORY)).mkdirs();
		(new File(JSON_DIRECTORY)).mkdirs();
	}

	public static void fetchByTerm(String term) throws JSONException, ParserConfigurationException, SAXException,
			IOException, InterruptedException, TransformerException {
		initialize();

		String db = "pubmed";
		String usehistory = "y";
		String retmode = "json";
		String rettype = "uilist";
		String url = baseurl + "esearch.fcgi?";

		String parameters = "db=" + db + "&" + "term=" + term + "&" + "usehistory=" + usehistory + "&" + "retmode="
				+ retmode + "&" + "rettype=" + rettype + "&" + "api_key=" + api_key;

		String query = url + parameters;

		WebResource webResource = client.resource(query);

		ClientResponse response = webResource.get(ClientResponse.class);

		String responseBody = response.getEntity(String.class);

		JSONObject responseJson = new JSONObject(responseBody);
		JSONObject esearchresult = responseJson.getJSONObject("esearchresult");

		int retstart = 0;
		int retmax = 10000; // The maximum number of papers to be downloaded per call is 10k (recomendation
							// of eFetch docs)
		int count = esearchresult.getInt("count");
		retmode = "xml";
		rettype = "abstract";

		String webenv = esearchresult.getString("webenv");
		String key_query = esearchresult.getString("querykey");
		url = baseurl + "efetch.fcgi?";

		System.out.println("Downloading articles... ");

		while (retstart < count) {
			System.out.println("Indexes " + retstart + " to " + (retstart+retmax));
			parameters = "db=" + db + "&" + "query_key=" + key_query + "&" + "WebEnv=" + webenv + "&" + "rettype="
					+ rettype + "&" + "retmode=" + retmode + "&" + "retstart=" + retstart + "&" + "retmax=" + retmax
					+ "&" + "api_key=" + api_key;

			query = url + parameters;

			webResource = client.resource(query);
			response = webResource.get(ClientResponse.class);

			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(response.getEntityInputStream());

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(
					new File(XML_DIRECTORY + retstart + "-" + (retstart + retmax) + ".xml"));
			transformer.transform(source, result);

			NodeList articles = doc.getElementsByTagName("Article");

			JsonObject paper = new JsonObject();

			for (int i = 0; i < articles.getLength(); i++) {

				Node nArticle = articles.item(i);
				if (nArticle.getNodeType() == Node.ELEMENT_NODE) {
					Element article = (Element) nArticle;
					NodeList abstractTexts = article.getElementsByTagName("AbstractText");
					paper.addProperty("ArticleTitle",
							article.getElementsByTagName("ArticleTitle").item(0).getTextContent());
					if (abstractTexts.getLength() > 1) {
						JsonArray abstractElements = new JsonArray();
						try {
							for (int j = 0; j < abstractTexts.getLength(); j++) {
								JsonObject abstractNode = new JsonObject();
								NamedNodeMap attrs = abstractTexts.item(j).getAttributes();
								String label = attrs.getNamedItem("Label").getTextContent();

								abstractNode.addProperty("label", label);
								abstractNode.addProperty("content", abstractTexts.item(j).getTextContent());
								abstractElements.add(abstractNode);

							}
							paper.add("Abstract", abstractElements);

							try (BufferedWriter bw = new BufferedWriter(new FileWriter(JSON_DIRECTORY + i + ".json"))) {
								bw.write(prettiffy(paper.toString()));
							} catch (IOException e) {
								e.printStackTrace();
								System.exit(1);
							}
						} catch (Exception e) {
							System.out.print("File with error: " + i + " - " + article.getElementsByTagName("ArticleTitle").item(0).getTextContent() + " ");
							e.printStackTrace();
						}
					}

				}
			}

			retstart = retstart + retmax;
			Thread.sleep(1000);
		}
	}

	public static Map<String, List<String>> fetchById(String db, String rettype, String retmode, String idsParameter, String webEnv, String queryKey, int n) throws SAXException, IOException, ParserConfigurationException, InterruptedException {
		String url = baseurl + "efetch.fcgi?";

		int retstart = 0;
		int retmax = 50;
		
		Map<String, List<String>> meshTermsMap = new HashMap<>();

//		System.out.println(n);
		while (retstart < n) {
			System.out.println(retstart + " " + n);
			System.out.println("Downloading articles: " + retstart + " -> " + (retstart+retmax));
			String parameters = "db=" + db + 
					"&retstart=" + retstart +
					"&retmax=" + retmax +
					"&rettype=" + rettype + 
					"&retmode=" + retmode + 
					"&WebEnv=" + webEnv + 
					"&query_key=" + queryKey +
					"&api_key=" + api_key;

			String response = HttpClient.post(url, parameters, api_key);
			
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(new InputSource(new StringReader(response)));
			
			NodeList articles = doc.getElementsByTagName("PubmedArticle");

			for (int i = 0; i < articles.getLength(); i++) {
				Node articleNode = articles.item(i);
				List<String> meshTerms = new ArrayList<>();
				
				if (articleNode.getNodeType() == Node.ELEMENT_NODE) {
					Element article = (Element) articleNode;
					
					NodeList articleId = article.getElementsByTagName("ArticleId");
					String pmid = "";
					for (int j = 0; j < articleId.getLength(); j++) {
						Node idType = articleId.item(j).getAttributes().getNamedItem("IdType");
						String idTypeContent = idType.getTextContent();
						
						if(idTypeContent.equals("pubmed")) {
							pmid = articleId.item(j).getTextContent();
						}
					}
					
					NodeList meshHeadings = article.getElementsByTagName("MeshHeading");
					
					for (int j = 0; j < meshHeadings.getLength(); j++) {
						meshTerms.add(meshHeadings.item(j).getTextContent());
					}
					meshTermsMap.put(pmid, meshTerms);
				}
			}
			retstart += retmax;
			Thread.sleep(1000);
		}
		
		System.out.println(meshTermsMap.size());
		return meshTermsMap;
	}
	
	public static Map<String, String> ePost(String db, String ids) throws ParserConfigurationException, SAXException, IOException, InterruptedException {
		String url = baseurl + "epost.fcgi?";
		
		String parameters = "db=pubmed&" + "&api_key=" + api_key + "&id=" + ids;
		
		String response = HttpClient.post(url, parameters, api_key);
		
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		Document doc = docBuilder.parse(new InputSource(new StringReader(response)));
		
		NodeList webEnvXMLElement = doc.getElementsByTagName("WebEnv");
		NodeList queryKey = doc.getElementsByTagName("QueryKey");

		Map<String, String> historyServer = new HashMap<>();
		historyServer.put("WebEnv", webEnvXMLElement.item(0).getTextContent());
		historyServer.put("QueryKey", queryKey.item(0).getTextContent());

		Thread.sleep(1000);
		
		return historyServer;
	}
	
	public static void main(String[] args) throws JSONException, InterruptedException, ParserConfigurationException,
			SAXException, IOException, TransformerException {
		initialize();
//		fetchByTerm("respiratory+failure");
//		fetchById("pubmed", "xml", "full");
//		ePost("pubmed", "19172194");
	}

	public static Map<String, List<String>> getMeshTerms(List<String> idsParameter) throws SAXException, IOException, ParserConfigurationException, InterruptedException{
		String db = "pubmed";
		String retmode = "xml";
		String rettype = "full";
		
		int n = idsParameter.size();
		
		String ids = idsParameter.remove(0);
		for (String id : idsParameter) {
			ids += "," + id;
		}
		
		Map<String, String> historyServer = ePost(db, ids);
		
		return fetchById(db, rettype, retmode, ids, historyServer.get("WebEnv"), historyServer.get("QueryKey"), n);
		
//		return new HashMap<>();
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

	
	
	public static String prettiffy(String entityResponse) {
		JsonElement jsonResponse;
		try {
			jsonResponse = new JsonParser().parse(entityResponse);
		} catch (JsonSyntaxException ex) {
			jsonResponse = new JsonParser().parse("{ \"message\": \"Invalid JSON response\" }");
		}
		return new GsonBuilder().setPrettyPrinting().create().toJson(jsonResponse);
	}
}
