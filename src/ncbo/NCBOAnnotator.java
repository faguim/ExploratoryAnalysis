package ncbo;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class NCBOAnnotator {
	private static final String REST_URL = "http://data.bioontology.org";
	private static final String API_KEY = "1cff5532-d88d-4a43-97a2-729e43dd2a4b";
	private static String OUTPUT_UTTERANCE_ADD = "microsoft/utterances/add/input/";

	private static final int MAX_LENGTH = 1376;
	
	public static void main(String[] args) throws JSONException, IOException {
		boolean success = (new File(OUTPUT_UTTERANCE_ADD)).mkdirs();
		if (success) {
			System.out.println("Directory folder " + OUTPUT_UTTERANCE_ADD + "was created");
		}
		
		
		int nFiles = 1;
		for (int i = 0; i < nFiles; i++) {
			try(BufferedReader br = new BufferedReader(new FileReader("papers/" + i +".txt"))) {
				StringBuilder sb = new StringBuilder();
			    String line = br.readLine();

			    while (line != null) {
			        sb.append(line);
			        sb.append(System.lineSeparator());
			        line = br.readLine();
			    }
			    String everything = sb.toString();
			    System.out.println(everything);
			    
			    if (everything.length() > MAX_LENGTH)
			    	everything = everything.substring(0, MAX_LENGTH);
			    
			    String urlParameters;
			    String textToAnnotate = URLEncoder.encode(everything, "ISO-8859-1");
			    urlParameters = "text=" + textToAnnotate 
			    		+ "&ontologies=MESH"
			    		+ "&exclude_numbers=true"
			    		+ "&exclude_synonyms=true"
			    		+ "&longest_only=true";
			    
			    String response = post(REST_URL + "/annotator", urlParameters);
			    
			    JSONArray responseJson = new JSONArray(response);
			    System.out.println("Resposta do servidor: " + responseJson);
			    
			    createJSONFile(i,everything,responseJson);
			    
//			    for (int j = 0; j < responseJson.length(); j++) {
//			    	JSONObject jsonObject = responseJson.getJSONObject(j);
//			    	System.out.println("Mapeamento " + j + ": " + jsonObject);
//			    	System.out.println("\n");
//			    }
//			    System.out.println("------------------- Next -------------------\n\n\n");

			}
		}
		

		// printAnnotations(annotations);
		// JSONArray collection = new JSONArray();
		// String response = post(REST_URL + "/annotator");
		// JSONObject responseJson = new JSONObject(response);
		// System.out.println(responseJson);
		// collection = responseJson.getJSONArray("collection");
		//
		// for (int i = 0; i < collection.length(); i++) {
		// System.out.println(collection.getJSONObject(i).get("prefLabel"));
		// System.out.println(collection.getJSONObject(i).get("@id"));
		// }

	}

	private static String post(String urlToGet, String urlParameters) {
		URL url;
		HttpURLConnection conn;
		String line;
		String result = "";
		try {
			url = new URL(urlToGet);
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setInstanceFollowRedirects(false);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Authorization", "apikey token=" + API_KEY);
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("charset", "utf-8");
			conn.setUseCaches(false);

			DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
			wr.writeBytes(urlParameters);
			wr.flush();
			wr.close();
			conn.disconnect();

			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			while ((line = rd.readLine()) != null) {
				result += line;
			}
			rd.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	private static String get(String urlToGet) {
		URL url;
		HttpURLConnection conn;
		BufferedReader rd;
		String line;
		String result = "";
		try {
			url = new URL(urlToGet);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Authorization", "apikey token=" + API_KEY);
			conn.setRequestProperty("Accept", "application/json");
			rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			while ((line = rd.readLine()) != null) {
				result += line;
			}
			rd.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public static void createJSONFile(int idPaper, String text, JSONArray mappingsJson) throws JSONException, IOException {
    	JSONArray utterances = new JSONArray();
		String intentName = "Description";
		
		JSONObject utterance = new JSONObject();
		utterance.put("text", text);
		utterance.put("intentName", intentName);

		JSONArray entityLabels = new JSONArray();
    	utterance.put("entityLabels", entityLabels);

		for (int i = 0; i < mappingsJson.length(); i++) {
	    	JSONObject mappedEntity = mappingsJson.getJSONObject(i);
	    	JSONObject annotatedClass = mappedEntity.getJSONObject("annotatedClass");
	    	String id = annotatedClass.getString("@id");

	    	JSONArray annotations = mappedEntity.getJSONArray("annotations");
	    	
	    	for (int j = 0; j < annotations.length(); j++) {
	    		String startCharIndex = annotations.getJSONObject(j).getString("from");
	    		String endCharIndex = annotations.getJSONObject(j).getString("to");
	    		String entityName = "MeSH Term";
	    		
	    		JSONObject entityLabel = new JSONObject();
	    		entityLabel.put("startCharIndex", Integer.valueOf(startCharIndex)-1);
	    		entityLabel.put("endCharIndex", Integer.valueOf(endCharIndex)-1);

	    		entityLabel.put("entityName", entityName);
				entityLabels.put(entityLabel);
			}
		}
		utterances.put(utterance);
		
		try (FileWriter file = new FileWriter(OUTPUT_UTTERANCE_ADD + idPaper + ".json")) {
			file.write(prettiffy(utterances.toString()));
		}
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