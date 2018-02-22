package watson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.NaturalLanguageUnderstanding;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.AnalysisResults;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.AnalyzeOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.EntitiesOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.Features;
import com.ibm.watson.developer_cloud.service.exception.BadRequestException;
import com.ibm.watson.developer_cloud.service.exception.InternalServerErrorException;

public class WatsonNER {
	private static String PAPERS_JSON = "papers/json/";
	private static String OUTPUT_IBM_ENTITIES = "ibm/output/";

	private static Features features;
	private static NaturalLanguageUnderstanding service;
	
	public static void initiliaze() {
		(new File(OUTPUT_IBM_ENTITIES)).mkdirs();
		
		service = new NaturalLanguageUnderstanding(
				NaturalLanguageUnderstanding.VERSION_DATE_2017_02_27,
				"7f295c5b-9cc3-428e-a337-89ffe8bc5655",
				"vAZ37EdXwNE4"
				);
		
		EntitiesOptions entitiesOptions = new EntitiesOptions.Builder()
				.emotion(false)
				.sentiment(false)
				.limit(250)
				.build();

		features = new Features.Builder()
				.entities(entitiesOptions)
				.build();
		}
	
	public static JsonArray getEntities(String text) {
		System.out.println(text);
		JsonArray entities = new JsonArray();
		try {
			
			AnalyzeOptions parameters = new AnalyzeOptions.Builder()
					.text(text)
					.features(features)
					.build();
			
			AnalysisResults response = service
					.analyze(parameters)
					.execute();
			
			entities = new JsonParser().parse(response.getEntities().toString()).getAsJsonArray();
		} catch (BadRequestException e) {
			e.printStackTrace();
		} catch (InternalServerErrorException e) {
			e.printStackTrace();
		}
		
		return entities;
	}
	
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		initiliaze();
		
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
					String text = ((JsonObject) jsonElement).get("content").getAsString();
					jsonElement.getAsJsonObject().add("entities", getEntities(text));
				}
				createJSONFile(listOfFiles[i].getName(), paperJson);
			}
		}
	}
	
	public static void createJSONFile(String nameOfFile, JsonElement jsonElement) throws IOException {
		try (FileWriter file = new FileWriter(OUTPUT_IBM_ENTITIES + nameOfFile)) {
			file.write(prettiffy(jsonElement.toString()));
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
