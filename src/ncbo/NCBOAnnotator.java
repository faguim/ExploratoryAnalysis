package ncbo;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class NCBOAnnotator {
	private static final String REST_URL = "http://data.bioontology.org";
	private static final String API_KEY = "1cff5532-d88d-4a43-97a2-729e43dd2a4b";

	public static void main(String[] args) throws UnsupportedEncodingException, JSONException {
		String urlParameters;
		String textToAnnotate = URLEncoder.encode("history of arterial hypertension, with irregular treatment",
				"ISO-8859-1");
		urlParameters = "text=" + textToAnnotate + "&ontologies=MESH";
		String response = post(REST_URL + "/annotator", urlParameters);

		JSONArray responseJson = new JSONArray(response);

		for (int i = 0; i < responseJson.length(); i++) {
			JSONObject jsonObject = responseJson.getJSONObject(i);
			JSONObject annotatedClass = jsonObject.getJSONObject("annotatedClass");

			String id = annotatedClass.getString("@id");
			System.out.println("URI: " + id);

			JSONArray annotations = jsonObject.getJSONArray("annotations");
			System.out.println(annotations);
			// for (int j = 0; j < annotations.length(); j++) {
			// System.out.println(annotations.getJSONObject(j).getString("text"));
			// }
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
}