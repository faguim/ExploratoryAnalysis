package ncbo;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.http.client.utils.URIBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class NCBOAnnotator {
	private static final String REST_URL = "http://data.bioontology.org";
	private static final String API_KEY = "1cff5532-d88d-4a43-97a2-729e43dd2a4b";
	private static String OUTPUT_UTTERANCE_ADD = "microsoft/utterances/add/input/";
	private static String PAPERS_JSON = "papers/json/";

	private static final int MAX_LENGTH = 1376;

	public static void initiliaze() {
		(new File(OUTPUT_UTTERANCE_ADD)).mkdirs();
	}

	public static JsonArray annotate(String inputText) {
		String urlParameters;

		urlParameters = "text=" + URLEncoder.encode(inputText) + "&ontologies=MESH" + "&exclude_numbers=true"
				+ "&exclude_synonyms=false" + "&longest_only=true";
		
		String response = post(REST_URL + "/annotator", urlParameters);
		
		JsonArray responseArray = (JsonArray) new JsonParser().parse(response);
		JsonArray meshTerms = new JsonArray();
		
		for (int i = 0; i < responseArray.size(); i++) {
			JsonObject mappedEntity = (JsonObject) responseArray.get(i);
			
			JsonObject annotatedClass = mappedEntity.getAsJsonObject("annotatedClass");
			String id = annotatedClass.get("@id").getAsString();
			
			JsonArray annotations = mappedEntity.getAsJsonArray("annotations");

			for (int j = 0; j < annotations.size(); j++) {
				String startCharIndex = ( (JsonObject )annotations.get(j)).get("from").getAsString();
				String endCharIndex = ( (JsonObject )annotations.get(j)).get("to").getAsString();
				String text = ((JsonObject) annotations.get(j)).get("text").getAsString();

				JsonObject entityLabel = new JsonObject();

				entityLabel.addProperty("uri", id);
				entityLabel.addProperty("startCharIndex", Integer.valueOf(startCharIndex) - 1);
				entityLabel.addProperty("endCharIndex", Integer.valueOf(endCharIndex) - 1);
				entityLabel.addProperty("prefLabel", text);
				
				meshTerms.add(entityLabel);
			}
		}

		return meshTerms;
	}

	public static void main(String[] args) throws JSONException, IOException {
		initiliaze();
		// int nFiles = 1;
		File folder = new File(PAPERS_JSON);
		File[] listOfFiles = folder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
			System.out.println("File " + listOfFiles[i].getName());

			try (FileInputStream stream = new FileInputStream(PAPERS_JSON + listOfFiles[i].getName())) {
				String everything = new Scanner(stream, "UTF-8").useDelimiter("\\A").next();
				JsonObject paperJson = new JsonParser().parse(everything).getAsJsonObject();

				JsonArray abstractElements = paperJson.getAsJsonArray("Abstract");

				List<Map<String, Object>> maps = new ArrayList<>();
				for (JsonElement jsonElement : abstractElements) {

					Map<String, Object> map = new HashMap<>();

					map.put("label", ((JsonObject) jsonElement).get("label").getAsString());
					map.put("content", ((JsonObject) jsonElement).get("content").getAsString());

					String textToAnnotate = URLEncoder.encode(map.get("content").toString(), "ISO-8859-1");

					if (!textToAnnotate.isEmpty()) {
						String urlParameters;

						urlParameters = "text=" + textToAnnotate + "&ontologies=MESH" + "&exclude_numbers=true"
								+ "&exclude_synonyms=true" + "&longest_only=true";
						String response = post(REST_URL + "/annotator", urlParameters);
						JSONArray responseJson = new JSONArray(response);
						map.put("response", responseJson);
						maps.add(map);
					}


				}
				createJSONFile(listOfFiles[i].getName(), maps);

				// if (everything.length() > MAX_LENGTH)
				// everything = everything.substring(0, MAX_LENGTH);
			}
		}

	}

//	public static void annotate(String textToAnnotte) {
//		System.out.println(textToAnnotte);
//	}
	
	public static void createJSONFile(String nameOfFile, List<Map<String, Object>> maps)
			throws IOException, JSONException {
		JSONArray utterances = new JSONArray();
		for (Map<String, Object> map : maps) {
			JSONObject utterance = new JSONObject();
			utterance.put("intentName", map.get("label").toString());
			utterance.put("text", map.get("content").toString());

			JSONArray entityLabels = new JSONArray();
			utterance.put("entityLabels", entityLabels);

			JSONArray responseJson = ((JSONArray) map.get("response"));

			for (int i = 0; i < responseJson.length(); i++) {
				try {
					JSONObject mappedEntity = responseJson.getJSONObject(i);
					JSONObject annotatedClass = mappedEntity.getJSONObject("annotatedClass");
					String id = annotatedClass.getString("@id");
					JSONArray ancestors = new JSONArray(
							get(annotatedClass.getJSONObject("links").get("ancestors").toString()));

					String entityName = "";
					if (ancestors.length() == 0) {
						JSONObject jsonObject = new JSONObject(get(id));
						entityName = jsonObject.getString("prefLabel");
					} else {
						entityName = ((JSONObject) ancestors.get(ancestors.length() - 1)).getString("prefLabel");
					}

					JSONArray annotations = mappedEntity.getJSONArray("annotations");

					for (int j = 0; j < annotations.length(); j++) {
						String startCharIndex = annotations.getJSONObject(j).getString("from");
						String endCharIndex = annotations.getJSONObject(j).getString("to");
						String text = annotations.getJSONObject(j).getString("text");

						JSONObject entityLabel = new JSONObject();
						entityLabel.put("startCharIndex", Integer.valueOf(startCharIndex) - 1);
						entityLabel.put("endCharIndex", Integer.valueOf(endCharIndex) - 1);

						entityLabel.put("entityName", entityName);
						entityLabels.put(entityLabel);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}

			}
			utterances.put(utterance);
		}

		try (FileWriter file = new FileWriter(OUTPUT_UTTERANCE_ADD + nameOfFile)) {
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

	private static String get(String urlToGet) {
		URL url;
		HttpURLConnection conn;
		BufferedReader rd;
		String line;
		String result = "";
		try {
			url = new URL(urlToGet);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
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
}