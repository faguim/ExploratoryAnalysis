package util;

import java.io.InputStream;
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
	
	public static void main(String[] args) {
		getResource();
	}
}
