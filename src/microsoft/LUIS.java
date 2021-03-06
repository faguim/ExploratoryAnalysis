package microsoft;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class LUIS {
	// Update the host if your LUIS subscription is not in the West US region
	final static String LUIS_BASE = "https://westus.api.cognitive.microsoft.com";
	final static String LUIS_APP = "/luis/api/v2.0/apps/{app_id}";
	final static String LUIS_VERSION = "/versions/{app_version}";

	// Enter information about your LUIS application and key below
	static final String APP_ID = "69b86786-213a-45ea-88ad-d74d7c5d5abd";
	static final String APP_VERSION = "0.2";
	static final String LUIS_PROGRAMMATIC_ID = "0f03533f6b4e4e5b9d6dc33ef75659a4";

	// File names for utterance and result files
	static final String INPUT_UTTERANCE_ADD = "microsoft/utterances/add/input/";
	static final String OUTPUT_UTTERANCES_ADD = "microsoft/utterances/add/output/";
	static final String OUTPUT_UTTERANCES_GET = "microsoft/utterances/get/output.json";

	static final String UTF8 = "UTF-8";

	// endpoint method names
	final String TRAIN = "/train";
	final static String EXAMPLES = "/examples";
	final static String EXAMPLE = "/example";
	final static String APP_INFO = "/";
	final static String INTENTS = "/intents";
	final static String ENTITIES = "/entities";

	static String path;
	static HttpClient httpclient;

	public static void initialize() {
		new File("microsoft/utterances/add/input").mkdirs();
		new File("microsoft/utterances/add/output").mkdirs();
		new File("microsoft/utterances/get").mkdirs();

		path = LUIS_BASE + LUIS_APP.replace("{app_id}", APP_ID) + LUIS_VERSION.replace("{app_version}", APP_VERSION);

	}

	public static void getUtterances() throws URISyntaxException, ClientProtocolException, IOException {
		HttpResponse response = get(EXAMPLES);
		HttpEntity httpEntityResponse = response.getEntity();

		JsonArray utterances = new JsonParser().parse(EntityUtils.toString(httpEntityResponse)).getAsJsonArray();

		int predicted = 0;

		for (JsonElement utterance : utterances) {
			JsonArray entityLabels = utterance.getAsJsonObject().get("entityLabels").getAsJsonArray();
			JsonArray entityPredictions = utterance.getAsJsonObject().get("entityPredictions").getAsJsonArray();
			int id = utterance.getAsJsonObject().get("id").getAsInt();

			if (entityPredictions.size() != entityLabels.size()) {
				System.out.println(utterance);
				System.out.println("Input: "+ entityLabels);
				System.out.println("Predicted: "+entityPredictions);
			}
			predicted += (entityPredictions.size() - entityLabels.size());

		}

				System.out.println(predicted);
		//		utterances.
		//		
		//		if (httpEntityResponse != null) {
		//			String entityResponse = EntityUtils.toString(httpEntityResponse);
		//
		//			File file = new File(OUTPUT_UTTERANCES_GET);
		//			if (!file.exists())
		//				file.createNewFile();
		//
		//			try (FileOutputStream stream = new FileOutputStream(file)) {
		//				entityResponse = prettiffy(entityResponse);
		//
		//				stream.write(entityResponse.getBytes(UTF8));
		//				stream.flush();
		//			}
		//		}
	}

	public static void addUtterance()
			throws URISyntaxException, FileNotFoundException, IOException, InterruptedException {

		String[] files = new File(INPUT_UTTERANCE_ADD).list();
		for (String inputFile : files) {
			System.out.println(inputFile);
			try (FileInputStream stream = new FileInputStream(INPUT_UTTERANCE_ADD + inputFile)) {
				String data = new Scanner(stream, UTF8).useDelimiter("\\A").next();
				JsonArray utterances = new JsonParser().parse(data).getAsJsonArray();

				boolean allIntentsAdded = true;
				boolean allEntitiesAdded = true;

				for (JsonElement utteranceElement : utterances) {
					JsonObject utterance = utteranceElement.getAsJsonObject();
					JsonObject intent = new JsonObject();

					intent.addProperty("name", utterance.get("intentName").getAsString());

					if (!addIntent(intent))
						allIntentsAdded = false;

					JsonArray entityLabels = utterance.getAsJsonArray("entityLabels");
					for (JsonElement entityElement : entityLabels) {
						JsonObject entityJson = entityElement.getAsJsonObject();

						JsonObject entity = new JsonObject();
						entity.addProperty("name", entityJson.get("entityName").getAsString());
						if (!addEntity(entity))
							allEntitiesAdded = false;
					}
				}

				//				if (allEntitiesAdded && allIntentsAdded) {
				HttpResponse response = post(EXAMPLES, data);

				HttpEntity httpEntityResponse = response.getEntity();

				if (httpEntityResponse != null) {

					String entityResponse = EntityUtils.toString(httpEntityResponse);

					File outputFile = new File(OUTPUT_UTTERANCES_ADD + inputFile);
					if (!outputFile.exists())
						outputFile.createNewFile();

					try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
						entityResponse = prettiffy(entityResponse);

						outputStream.write(entityResponse.getBytes(UTF8));
						outputStream.flush();
					}
				}
				//				}

			}
		}

	}

	public static boolean addEntity(JsonObject entity) throws ClientProtocolException, URISyntaxException, IOException {
		HttpResponse response = post(ENTITIES, entity.toString());

		if (response.getStatusLine().getStatusCode() == 201)
			return true;
		else
			return false;
	}

	public static boolean addIntent(JsonObject intent) throws FileNotFoundException, IOException, URISyntaxException {
		HttpResponse response = post(INTENTS, intent.toString());

		if (response.getStatusLine().getStatusCode() == 201)
			return true;
		else
			return false;
	}

	public static HttpResponse post(String endpoint, String data)
			throws URISyntaxException, ClientProtocolException, IOException {
		httpclient = HttpClients.createDefault();

		URIBuilder builder = new URIBuilder(path + endpoint);
		URI uri = builder.build();
		HttpPost request = new HttpPost(uri);
		request.setHeader("Ocp-Apim-Subscription-Key", LUIS_PROGRAMMATIC_ID);
		request.setHeader("Content-Type", MediaType.APPLICATION_JSON);

		StringEntity reqEntity = new StringEntity(data);

		request.setEntity(reqEntity);
		HttpResponse response = httpclient.execute(request);
		return response;
	}

	public static HttpResponse get(String endpoint)
			throws URISyntaxException, ClientProtocolException, IOException {
		httpclient = HttpClients.createDefault();

		URIBuilder builder = new URIBuilder(path + endpoint);
		URI uri = builder.build();
		HttpGet request = new HttpGet(uri);
		request.setHeader("Ocp-Apim-Subscription-Key", LUIS_PROGRAMMATIC_ID);
		request.setHeader("Content-Type", MediaType.APPLICATION_JSON);

		HttpResponse response = httpclient.execute(request);
		return response;
	}

	public static void main(String[] args)
			throws IOException, org.json.simple.parser.ParseException, URISyntaxException, InterruptedException {
		initialize();
		getUtterances();
		//		addUtterance();
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