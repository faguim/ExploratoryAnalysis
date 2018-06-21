package crawler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
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

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
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

	public static List<String> fetchById(String db, String idsParameter, String webEnv, String queryKey, int n) throws SAXException, IOException, ParserConfigurationException, InterruptedException {
		String url = baseurl + "efetch.fcgi?";

		String retmode = "xml";
		String rettype = "full";
		
		int retstart = 0;
		int retmax = 50;

		List<String> papers = new ArrayList<>();
		
		while (retstart < n) {
//		while (retstart < 50) {
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
			papers.add(response);
			
			retstart += retmax;
			Thread.sleep(1000);
		}

		return papers;
	}

	public static String fetchById(String db, String pmid) {
		String url = baseurl + "efetch.fcgi?";
		String retmode = "xml";
		String rettype = "full";

		System.out.println("Downloading article: " + pmid);
		String parameters = "id=" + pmid + 
				"&db=" + db + 
				"&rettype=" + rettype + 
				"&retmode=" + retmode + 
				"&api_key=" + api_key;

		String response = HttpClient.post(url, parameters, api_key);
		System.out.println(response);
		return response;
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

	public static List<String> getMeshTerms(String pmid) throws ParserConfigurationException, SAXException, IOException {
		List<String> meshTerms = new ArrayList<>();

		String db = "pubmed";

		String paperString = fetchById(db, pmid);

		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		Document doc = docBuilder.parse(new InputSource(new StringReader(paperString)));

		NodeList articles = doc.getElementsByTagName("PubmedArticle");

		for (int i = 0; i < articles.getLength(); i++) {
			Node articleNode = articles.item(i);

			if (articleNode.getNodeType() == Node.ELEMENT_NODE) {
				Element article = (Element) articleNode;

				NodeList meshHeadings = article.getElementsByTagName("MeshHeading");

				for (int j = 0; j < meshHeadings.getLength(); j++) {
					Node meshHeadingNode = meshHeadings.item(j);

					if(meshHeadingNode.getNodeType() == Node.ELEMENT_NODE) {
						Element meshHeading = (Element) meshHeadingNode;
						NodeList descriptors = meshHeading.getElementsByTagName("DescriptorName");
						NodeList qualifiers = meshHeading.getElementsByTagName("QualifierName");

						String descriptor = "";
						
						for (int k = 0; k < descriptors.getLength(); k++) {
							String major = descriptors.item(k).getAttributes().getNamedItem("MajorTopicYN").getTextContent();
							descriptor = descriptors.item(k).getTextContent();
							
							if (major.equals("Y"))
								descriptor += "*";
						}
						
						if (qualifiers.getLength() == 0)
							meshTerms.add(descriptor);
						else {
							for (int k = 0; k < qualifiers.getLength(); k++) {
								String major = qualifiers.item(k).getAttributes().getNamedItem("MajorTopicYN").getTextContent();
								String qualifier = qualifiers.item(k).getTextContent();
								if (major.equals("Y"))
									qualifier += "*";
								meshTerms.add(descriptor + "/" + qualifier);
							}
						}
					}
				}
			}
		}
		System.out.println(meshTerms);
		return meshTerms;
	}

	public static Map<String, List<String>> getMeshTerms(List<String> idsParameter) throws SAXException, IOException, ParserConfigurationException, InterruptedException{
		String db = "pubmed";

		int n = idsParameter.size();

		String ids = idsParameter.remove(0);
		for (String id : idsParameter) {
			ids += "," + id;
		}

		Map<String, String> historyServer = ePost(db, ids);
		Map<String, List<String>> meshTermsMap = new HashMap<>();

		
		List<String> papers = fetchById(db, ids, historyServer.get("WebEnv"), historyServer.get("QueryKey"), n);
		
		System.out.println("Download Finished");
		
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		for (String paper : papers) {
			Document doc = docBuilder.parse(new InputSource(new StringReader(paper)));

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
						Node meshHeadingNode = meshHeadings.item(j);

						if(meshHeadingNode.getNodeType() == Node.ELEMENT_NODE) {
							Element meshHeading = (Element) meshHeadingNode;
							NodeList descriptors = meshHeading.getElementsByTagName("DescriptorName");
							NodeList qualifiers = meshHeading.getElementsByTagName("QualifierName");

							String descriptor = "";
							
							for (int k = 0; k < descriptors.getLength(); k++) {
								String major = descriptors.item(k).getAttributes().getNamedItem("MajorTopicYN").getTextContent();
								descriptor = descriptors.item(k).getTextContent();
								
								if (major.equals("Y"))
									descriptor += "*";
							}
							
							if (qualifiers.getLength() == 0)
								meshTerms.add(descriptor);
							else {
								for (int k = 0; k < qualifiers.getLength(); k++) {
									String major = qualifiers.item(k).getAttributes().getNamedItem("MajorTopicYN").getTextContent();
									String qualifier = qualifiers.item(k).getTextContent();
									if (major.equals("Y"))
										qualifier += "*";
									meshTerms.add(descriptor + "/" + qualifier);
								}
							}
						}
					}
					
					meshTermsMap.put(pmid, meshTerms);
				}
			}
		}

		return meshTermsMap;
	}

	public static List<String> getPMIDs(String querySearch, String mindate, String maxdate) throws ParserConfigurationException, SAXException, IOException, InterruptedException {
		String url = baseurl + "esearch.fcgi?";

		String db = "pubmed";
		String retmode = "xml";
		String rettype = "uilist";
		
		long retstart = 0;
		long retmax = 20000;
		String usehistory = "y";

		querySearch = URLEncoder.encode(querySearch);
		
		String parameters = "term=" + querySearch + 
				"&db=" + db + 
				"&retstart=" + retstart +
				"&retmax=" + retmax +
				"&rettype=" + rettype + 
				"&retmode=" + retmode + 
				"&usehistory=" + usehistory +
				"&api_key=" + api_key;
		
		if (!mindate.isEmpty() && !maxdate.isEmpty()) {
			parameters += "&datetype=edat" +
					   "&mindate=" + mindate +
					   "&maxdate=" + maxdate;
		}
		
		String query = url + parameters;

		String result = HttpClient.get(query);

		List<String> pmidList = new ArrayList<>();
		
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		Document doc = docBuilder.parse(new InputSource(new StringReader(result)));

		NodeList idList = doc.getElementsByTagName("IdList");
		for (int i = 0; i < idList.getLength(); i++) {
			Node idNode = idList.item(i);
			
			if (idNode.getNodeType() == Node.ELEMENT_NODE) {
				Element idNodeElement = (Element) idNode;

				NodeList ids = idNodeElement.getElementsByTagName("Id");
				for (int j = 0; j < ids.getLength(); j++) {
					pmidList.add(ids.item(j).getTextContent());
				}
			}
		}
		
		NodeList webEnvXMLElement = doc.getElementsByTagName("WebEnv");
		NodeList queryKeyElement = doc.getElementsByTagName("QueryKey");
		NodeList countElement = doc.getElementsByTagName("Count");

		String webEnv = webEnvXMLElement.item(0).getTextContent();
		String queryKey = queryKeyElement.item(0).getTextContent();
		long count = Long.valueOf(countElement.item(0).getTextContent());

		System.out.println("Count: " + count);
		
		retstart = retstart + retmax;
		
		querySearch = URLEncoder.encode("#" + queryKey);
		
		while(retstart < count) {
			parameters = "term=" + querySearch + 
					"&db=" + db + 
					"&retstart=" + retstart +
					"&retmax=" + retmax +
					"&rettype=" + rettype + 
					"&retmode=" + retmode + 
					"&usehistory=" + usehistory +
					"&WebEnv=" + webEnv + 
					"&query_key=" + queryKey +
					"&api_key=" + api_key;

			query = url + parameters;

			result = HttpClient.get(query);
			doc = docBuilder.parse(new InputSource(new StringReader(result)));
	
			idList = doc.getElementsByTagName("IdList");
			
			for (int i = 0; i < idList.getLength(); i++) {
				
				Node idNode = idList.item(i);
				
				if (idNode.getNodeType() == Node.ELEMENT_NODE) {
					Element idNodeElement = (Element) idNode;

					NodeList ids = idNodeElement.getElementsByTagName("Id");
					for (int j = 0; j < ids.getLength(); j++) {
						pmidList.add(ids.item(j).getTextContent());
					}
				}
			}
			
			retstart = retstart + retmax;
			Thread.sleep(1000);
		}
		return pmidList;
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

	public static void main(String[] args) throws JSONException, InterruptedException, ParserConfigurationException,
	SAXException, IOException, TransformerException {
		initialize();
		
		String searchTerm = "sensitiv*[Title/Abstract] OR \n" + 
				"sensitivity and specificity[MeSH Terms] OR\n" + 
				"(predictive[Title/Abstract] AND value*[Title/Abstract]) OR \n" + 
				"predictive value of tests[MeSH Term] OR \n" + 
				"accuracy*[Title/Abstract]";

		getPMIDs(searchTerm, "1950", "2014/01/21");
		//fetchByTerm("respiratory+failure");
//		fetchById("pubmed", "19906740");
//		getMeshTerms("19906740");
		//ePost("pubmed", "19172194");
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

	
	public static void search(String querySearch, String mindate, String maxdate, String dir_to_save, String filter) throws ParserConfigurationException, SAXException, IOException {
//		try (
//				Reader reader = Files.newBufferedReader(Paths.get(dir_to_save + "search-metadata-result.csv"));
//				CSVReader csvReader = new CSVReader(reader);
//				String [] nextLine;
//				while ((nextLine = csvReader.readNext()) != null) {
//				    // nextLine[] is an array of values from the line
//				    System.out.println(nextLine[0] + nextLine[1] + "etc...");
//				}
//				)
//		{
//			
//		}
		String url = baseurl + "esearch.fcgi?";

		String db = "pubmed";
		String retmode = "xml";
		String rettype = "uilist";
		
		String usehistory = "y";

		querySearch = URLEncoder.encode(querySearch);
		
		String parameters = "term=" + querySearch + 
				"&db=" + db + 
				"&rettype=" + rettype + 
				"&retmode=" + retmode + 
				"&usehistory=" + usehistory +
				"&api_key=" + api_key;
		
		if (!mindate.isEmpty() && !maxdate.isEmpty()) {
			parameters += "&datetype=edat" +
					   "&mindate=" + mindate +
					   "&maxdate=" + maxdate;
		}
		
		String query = url + parameters;

		String result = HttpClient.get(query);

		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		Document doc = docBuilder.parse(new InputSource(new StringReader(result)));

		NodeList webEnvXMLElement = doc.getElementsByTagName("WebEnv");
		NodeList queryKeyElement = doc.getElementsByTagName("QueryKey");
		NodeList countElement = doc.getElementsByTagName("Count");

		String webEnv = webEnvXMLElement.item(0).getTextContent();
		String queryKey = queryKeyElement.item(0).getTextContent();
		String count = countElement.item(0).getTextContent();

		String[] header = new String[4];
		
		File f = new File(dir_to_save + "search-metadata-result.csv");
		if(!f.exists()) { 
			header[0] = "Filter";
			header[1] = "WebEnv";
			header[2] = "QueryKey";
			header[3] = "Count";
			CSVWriter writer = new CSVWriter(new FileWriter(dir_to_save + "search-metadata-result.csv", true));
			writer.writeNext(header);	
			writer.close();
		} 

		CSVWriter writer = new CSVWriter(new FileWriter(dir_to_save + "search-metadata-result.csv", true));
		
		header[0] = filter;
		header[1] = webEnv;
		header[2] = queryKey;
		header[3] = count;
		writer.writeNext(header);
		

		
		writer.close();
		
	}
	
	public static void searchWithFilter(String query, int i, String webEnv, String query_id) {

		
		//		String url = baseurl + "esearch.fcgi?";
//
//		String db = "pubmed";
//		String retmode = "xml";
//		String rettype = "uilist";
//		
//		long retstart = 0;
//		long retmax = 20000;
//		String usehistory = "y";
//
//		querySearch = URLEncoder.encode(querySearch);
//		
//		String parameters = "term=" + querySearch + 
//				"&db=" + db + 
//				"&retstart=" + retstart +
//				"&retmax=" + retmax +
//				"&rettype=" + rettype + 
//				"&retmode=" + retmode + 
//				"&usehistory=" + usehistory +
//				"&api_key=" + api_key;
//		
	}
}
