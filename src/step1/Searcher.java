package step1;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.FieldCacheTermsFilter;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.json.JSONException;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import crawler.PubMed;
import input.Input;
import ncbo.NCBOAnnotator;

public class Searcher {
	private static final Version LUCENE_VERSION = Version.LUCENE_30;

	private static Directory dir;
	private static IndexSearcher is;

	private static String INDEX_DIR = "/home/fagner/Doutorado/step1/indexes";

	private static String STEP1_DIR = "/home/fagner/Doutorado/step1 (tests)/";
	private static String RESULTS_DIR = STEP1_DIR + "results/";

	private static String FILTER_RESULT = STEP1_DIR + "filteredPMIDS/";

	private static String TEXT_CONTENT = RESULTS_DIR + "text/";
	private static String MESH_CONTENT = RESULTS_DIR + "mesh/";

	private static String DIAGNOSIS_DIR = "diagnosis/";
	private static String TREATMENT_DIR = "treatment/";
	private static String PROGNOSIS_DIR = "prognosis/";
	private static String REVIEW_DIR = "review/";
	private static String ETIOLOGY_DIR = "etiology/";

	private static String[] header;

	public static void initiliaze() throws IOException {
		(new File(FILTER_RESULT)).mkdirs();
		
		(new File(TEXT_CONTENT + TREATMENT_DIR)).mkdirs();
		(new File(TEXT_CONTENT + DIAGNOSIS_DIR)).mkdirs();
		(new File(TEXT_CONTENT + PROGNOSIS_DIR)).mkdirs();
		(new File(TEXT_CONTENT + REVIEW_DIR)).mkdirs();
		(new File(TEXT_CONTENT + ETIOLOGY_DIR)).mkdirs();

		(new File(MESH_CONTENT + TREATMENT_DIR)).mkdirs();
		(new File(MESH_CONTENT + DIAGNOSIS_DIR)).mkdirs();
		(new File(MESH_CONTENT + PROGNOSIS_DIR)).mkdirs();
		(new File(MESH_CONTENT + REVIEW_DIR)).mkdirs();
		(new File(MESH_CONTENT + ETIOLOGY_DIR)).mkdirs();
		
		dir = FSDirectory.open(new File(INDEX_DIR));
		is = new IndexSearcher(dir);

		CSVWriter writer = new CSVWriter(new FileWriter(STEP1_DIR + "result.csv", false));

		header = new String[6];
		header[0] = "topic";
		header[1] = "treatment";
		header[2] = "diagnosis";
		header[3] = "prognosis";
		header[4] = "etiology";
		header[5] = "review";

		writer.writeNext(header);
		writer.close();
	}

	private static void filterPapers() throws ParserConfigurationException, SAXException, IOException, InterruptedException {
		String[] filterQueries = new String[5];

		filterQueries[0] = "randomized controlled trial[Publication Type] OR\n" + 
				"randomized[Title/Abstract] OR\n" + 
				"placebo[Title/Abstract]";
		filterQueries[1] = "sensitiv*[Title/Abstract] OR \n" + 
				"sensitivity and specificity[MeSH Terms] OR\n" + 
				"(predictive[Title/Abstract] AND value*[Title/Abstract]) OR \n" + 
				"predictive value of tests[MeSH Term] OR \n" + 
				"accuracy*[Title/Abstract]";
		filterQueries[2] = "prognosis[MeSH:noexp] OR \n" + 
				"diagnosed[Title/Abstract] OR \n" + 
				"cohort*[Title/Abstract] OR \n" + 
				"cohort effect[MeSH Term] OR \n" + 
				"cohort studies[MeSH:noexp] OR \n" + 
				"predictor*[Title/Abstract] OR \n" + 
				"death[Title/Abstract] OR \n" + 
				"\"models, statistical\"[MeSH Term]";
		filterQueries[3] = "risk[Title/Abstract] OR \n" + 
				"risk[MeSH:noexp] OR \n" + 
				"mortality[Title/Abstract] OR \n" + 
				"mortality[MeSH:noexp] OR \n" + 
				"cohort[Title/Abstract]";
		filterQueries[4] = "meta analysis[Publication Type] OR \n" + 
				"meta analysis[Title/Abstract] OR \n" + 
				"meta analysis[MeSH Terms] OR \n" + 
				"review[Publication Type] OR \n" + 
				"search*[Title/Abstract]";

		for (int i = 0; i < filterQueries.length; i++) {
			System.out.println(header[i+1]);

			String initialDate = "1950"; 
			String finalDate = "2014/01/21"; 

			PubMed.search(filterQueries[i], initialDate, finalDate, FILTER_RESULT + "search-metadata-result.csv", header[i+1]);
			List<String> pmids = PubMed.getPMIDs(FILTER_RESULT + "search-metadata-result.csv", header[i+1]);
			
			CSVWriter writer = new CSVWriter(new FileWriter(FILTER_RESULT + header[i+1] + ".csv", true));

			for (String pmid : pmids) {
				String[] pmidString = new String[1];

				pmidString[0] = pmid;
				writer.writeNext(pmidString);
			}


			writer.close();

			System.out.println("Filter Result to " + header[i+1] + " saved");
		}
	}

	public static void main(String[] args) throws ParseException, SAXException, ParserConfigurationException, TransformerException, IOException, JSONException, InterruptedException {
		initiliaze();

		List<Topic> topics = Input.getTopics();

		experiment(topics);
		is.close();
	}

	private static void experiment(List<Topic> topics) throws ParserConfigurationException, SAXException, IOException, InterruptedException, ParseException, TransformerException {
		File[] files = new File(FILTER_RESULT).listFiles();
		if (files.length == 0) {
			filterPapers();
		} 

		String[] resultsFromDescription = new String[6];
		String[] resultsFromMeshTerms = new String[6];

		CSVWriter writer1 = new CSVWriter(new FileWriter(STEP1_DIR + "resultsFromDescription.csv", true));
		CSVWriter writer2 = new CSVWriter(new FileWriter(STEP1_DIR + "resultsFromMeshTerms.csv", true));

		for (Topic topic : topics) {
			System.out.println(topic);

			resultsFromDescription[0] = topic.getNumber().toString();
			resultsFromDescription[1] = searchByCategory(topic.getDescription(), topic.getNumber(), FILTER_RESULT + "treatment.csv", TEXT_CONTENT + TREATMENT_DIR).toString();
			resultsFromDescription[2] = searchByCategory(topic.getDescription(), topic.getNumber(), FILTER_RESULT + "diagnosis.csv", TEXT_CONTENT + DIAGNOSIS_DIR).toString();
			resultsFromDescription[3] = searchByCategory(topic.getDescription(), topic.getNumber(), FILTER_RESULT + "prognosis.csv", TEXT_CONTENT + PROGNOSIS_DIR).toString();
			resultsFromDescription[4] = searchByCategory(topic.getDescription(), topic.getNumber(), FILTER_RESULT + "etiology.csv", TEXT_CONTENT + ETIOLOGY_DIR).toString();
			resultsFromDescription[5] = searchByCategory(topic.getDescription(), topic.getNumber(), FILTER_RESULT + "review.csv", TEXT_CONTENT + REVIEW_DIR).toString();

			writer1.writeNext(resultsFromDescription);
			
			
			JsonArray jsonArray = NCBOAnnotator.annotate(topic.getDescription());
			
			String meshTerms = "";
			
			for (JsonElement jsonElement : jsonArray) {
				String meshTerm = jsonElement.getAsJsonObject().get("prefLabel").getAsString();
				meshTerms += meshTerm + ", ";
			}
			
			resultsFromMeshTerms[0] = topic.getNumber().toString();
			resultsFromMeshTerms[1] = searchByCategory(meshTerms, topic.getNumber(), FILTER_RESULT + "treatment.csv", MESH_CONTENT + TREATMENT_DIR).toString();
			resultsFromMeshTerms[2] = searchByCategory(meshTerms, topic.getNumber(), FILTER_RESULT + "diagnosis.csv", MESH_CONTENT + DIAGNOSIS_DIR).toString();
			resultsFromMeshTerms[3] = searchByCategory(meshTerms, topic.getNumber(), FILTER_RESULT + "prognosis.csv", MESH_CONTENT + PROGNOSIS_DIR).toString();
			resultsFromMeshTerms[4] = searchByCategory(meshTerms, topic.getNumber(), FILTER_RESULT + "etiology.csv", MESH_CONTENT + ETIOLOGY_DIR).toString();
			resultsFromMeshTerms[5] = searchByCategory(meshTerms, topic.getNumber(), FILTER_RESULT + "review.csv", MESH_CONTENT + REVIEW_DIR).toString();

			writer2.writeNext(resultsFromMeshTerms);
		}
		writer1.close();
		writer2.close();
		
		System.out.println("Results saved");
	}

	private static Integer searchByCategory(String topicDescription, int topicNumber, String filterResultFile, String dir) throws ParserConfigurationException, SAXException, IOException, InterruptedException, ParseException, TransformerException {
		String[] pmids;

		try (
				Reader reader = Files.newBufferedReader(Paths.get(filterResultFile));
				CSVReader csvReader = new CSVReader(reader);
				) 
		{
			List<String[]> pms = csvReader.readAll();
			pmids = new String[pms.size()];

			int i = 0;
			for (String[] pmid : pms) {
				pmids[i] = pmid[0];
				i++;
			}
		}

		Filter filter = new FieldCacheTermsFilter("pmid", pmids);

		QueryParser parser = new  QueryParser(LUCENE_VERSION, "content", new StandardAnalyzer(LUCENE_VERSION));
		Query t3 = parser.parse(topicDescription);

		TopDocs hits = is.search(t3, filter, 1);

		hits = is.search(t3, filter, hits.totalHits);

		createCSV(hits, dir, topicNumber);
		return hits.totalHits;
	}

	public static void search(String q) throws IOException, ParseException {
		QueryParser parser = new QueryParser(Version.LUCENE_30, "content", new StandardAnalyzer(Version.LUCENE_30));

		Query query = parser.parse(q);
		long start = System.currentTimeMillis();
		TopDocs hits = is.search(query, 10);
		long end = System.currentTimeMillis();

		System.err.println("Found " + hits.totalHits + " document(s) (in " + (end - start) + " milliseconds) that matched query '" + q + "':");

		for (ScoreDoc scoreDoc : hits.scoreDocs) {
			Document doc = is.doc(scoreDoc.doc);
			System.out.println("Caminho: "+doc.get(("fullpath")));
			System.out.println("Título: "+doc.get("title"));
			System.out.println("Tipo: "+doc.get("type"));
			System.out.println("Resumo: "+doc.get("abstract"));
		}
	}

	public static Document searchByFileName(String fileName) throws IOException, ParseException {
		initiliaze();
		Term t = new Term("filename", fileName);
		Query query = new TermQuery(t);
		TopDocs hits = is.search(query, 1);

		for (ScoreDoc scoreDoc : hits.scoreDocs) {
			Document doc = is.doc(scoreDoc.doc);
			return doc;
		}
		return null;
	}

	public static Document search(String field, String content) throws IOException, ParseException {
		initiliaze();
		Term t = new Term(field, content);
		Query query = new TermQuery(t);
		TopDocs hits = is.search(query, 1);

		Document doc = null;
		for (ScoreDoc scoreDoc : hits.scoreDocs) {
			doc = is.doc(scoreDoc.doc);
		}
		return doc;
	}

	public static void simplestSearch(String q) throws CorruptIndexException, IOException {
		Term t = new Term("content", q);
		Query query = new TermQuery(t);
		TopDocs topDocs = is.search(query, 10);
		System.out.println(topDocs.totalHits);
		is.close();
	}

	public static List<Document> getAllDocs() throws IOException {
		initiliaze();
		Query query = new MatchAllDocsQuery();

		TopDocs hits = is.search(query, 1);
		System.out.println(hits.totalHits);
		hits = is.search(query, hits.totalHits);

		List<Document> docs = new ArrayList<>();

		for (ScoreDoc scoreDoc : hits.scoreDocs) {
			docs.add(is.doc(scoreDoc.doc));
		}
		return docs;
	}

	public static void createCSV(TopDocs hits, String dir, int topicNumber) throws CorruptIndexException, IOException {
		System.out.println("Salvar: " + dir + topicNumber + ".csv");
		CSVWriter writer = new CSVWriter(new FileWriter(dir + topicNumber + ".csv", true));

		for (ScoreDoc scoreDoc : hits.scoreDocs) {
			Document doc = is.doc(scoreDoc.doc);

			String[] pmidString = new String[3];

			pmidString[0] = doc.get("fullpath");
			pmidString[1] = doc.get("title");
			pmidString[2] = String.valueOf(scoreDoc.score);

			writer.writeNext(pmidString);
		}
		writer.close();
	}

	public static void createXMLResult(TopDocs hits, String dir, int topicNumber) throws ParserConfigurationException, CorruptIndexException, IOException, TransformerException {

		List<String> filepaths = new ArrayList<>();

		DocumentBuilderFactory icFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder icBuilder = icFactory.newDocumentBuilder();

		org.w3c.dom.Document xmlDoc = icBuilder.newDocument();
		Element rootElement = xmlDoc.createElement("articles");
		xmlDoc.appendChild(rootElement);

		for (ScoreDoc scoreDoc : hits.scoreDocs) {
			Document luceneDoc = is.doc(scoreDoc.doc);

			filepaths.add(luceneDoc.get("fullpath"));

			Element article = xmlDoc.createElement("article");
			rootElement.appendChild(article);

			Element fullPath = xmlDoc.createElement("fullpath");
			fullPath.appendChild(xmlDoc.createTextNode(luceneDoc.get("fullpath")));
			article.appendChild(fullPath);

			Element title = xmlDoc.createElement("title");
			title.appendChild(xmlDoc.createTextNode(luceneDoc.get("title")));
			article.appendChild(title);

			Element type = xmlDoc.createElement("type");
			type.appendChild(xmlDoc.createTextNode(luceneDoc.get("type")));
			article.appendChild(type);

			//			Element abstractElement = xmlDoc.createElement("abstract");
			//			abstractElement.appendChild(xmlDoc.createTextNode(luceneDoc.get("abstract")));
			//			article.appendChild(abstractElement);

			//			String kwString = luceneDoc.get("keywords");
			//			if (!kwString.isEmpty()) {
			//				Element keywords = xmlDoc.createElement("keywords");
			//				keywords.appendChild(xmlDoc.createTextNode(kwString));
			//				article.appendChild(keywords);
			//			}

			//	Explanation explanation = is.explain(t8, scoreDoc.doc);
			//	System.out.println(explanation);
		}

		// write the content into xml file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(xmlDoc);
		StreamResult result = new StreamResult(new File(dir + topicNumber + ".xml"));
		transformer.transform(source, result);
	}


}