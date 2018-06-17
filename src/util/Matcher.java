package util;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.FileManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Matcher {
	public static String ontology_dir = "ontologies/";
	public static Model model;
	
	public static void getResource() {
		StringBuffer query = new StringBuffer();
		
		query.append("PREFIX owl:       <http://www.w3.org/2002/07/owl#> \n");
		query.append("PREFIX rdf:       <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n");
		query.append("PREFIX rdfs:      <http://www.w3.org/2000/01/rdf-schema#> \n");
		query.append("PREFIX oboinowl:  <http://www.geneontology.org/formats/oboInOwl#> \n");
		query.append("PREFIX xsd:       <http://www.w3.org/2001/XMLSchema#> \n");
		
		query.append("SELECT DISTINCT ?entity2 WHERE { ?entity rdfs:prefLabel \"Sensitivity and Specificity\" .");
		query.append(" ?entity2 rdfs:subClassOf* ?entity }");

		Query sparql = QueryFactory.create(query.toString());
		model = getModel("MESH");
		QueryExecution qExec = QueryExecutionFactory.create(sparql, model);
		ResultSet rs = qExec.execSelect();
		
		if (null != rs) {
			System.out.println("aqui");
			while (rs.hasNext()) {
				QuerySolution result = rs.nextSolution();
				System.out.println(result);
//				System.out.println(result.getLiteral("entity").getValue().toString());
			}

		}
		
	}
	
	private static Model getModel(String ontology) {
		model = ModelFactory.createDefaultModel();

		InputStream in = FileManager.get().open(ontology_dir + ontology + ".ttl");
		System.out.println(in);
		model.read(in, null, "TTL");

		return model;
	}
	
	public static List<String> explode(String label) throws UnsupportedEncodingException {
		StringBuffer query = new StringBuffer();
		
		query.append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n");
		query.append("PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n");
		query.append("PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n");
		query.append("PREFIX owl: <http://www.w3.org/2002/07/owl#> \n");
		query.append("PREFIX meshv: <http://id.nlm.nih.gov/mesh/vocab#> \n");
		query.append("PREFIX mesh: <http://id.nlm.nih.gov/mesh/> \n");

		query.append("SELECT DISTINCT ?label \n"); 
		query.append("FROM <http://id.nlm.nih.gov/mesh>"); 

		query.append("WHERE {\n");
		query.append("?entity rdfs:label \"" + label + "\"@en . \n");
		query.append("?entity meshv:treeNumber ?treeNum .\n");
		query.append("?childTreeNum meshv:parentTreeNumber+ ?treeNum .\n");
		query.append("?descriptor meshv:treeNumber ?childTreeNum .\n");
		query.append("?descriptor rdfs:label ?label .\n");
		query.append("} \n");
		
		String url = "https://id.nlm.nih.gov/mesh/sparql";

		int resultAmount = 0;
		int offset = 0;
		List<String> descendants = new ArrayList<>();

		do {
			String result = HttpClient.get(url + "?" + "query=" + URLEncoder.encode(query.toString(), "UTF-8") + "&format=JSON&offset=" + offset + "0&inference=true", "");

			JsonObject resultJson = new JsonParser().parse(result).getAsJsonObject();
			JsonArray bindings = resultJson.getAsJsonObject("results").getAsJsonArray("bindings");
			
			resultAmount = bindings.size();
			
			for (int i = 0; i < bindings.size(); i++) {
				String meshTerm = bindings.get(i).getAsJsonObject().getAsJsonObject("label").get("value").getAsString();
				descendants.add(meshTerm);
			}
			
			offset = offset + resultAmount;
		}  while (resultAmount/1000 == 1);
		
		return descendants;
	}
	
	public static void main(String[] args) throws UnsupportedEncodingException {
		System.out.println(explode("Sensitivity and Specificity"));
	}
}