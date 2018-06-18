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

import org.apache.commons.digester3.Digester;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FieldCacheTermsFilter;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.json.JSONException;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import crawler.PubMed;

public class Searcher {
	private static final Version LUCENE_VERSION = Version.LUCENE_30;

	private static Directory dir;
	private static IndexSearcher is;

	private static String INDEX_DIR = "/home/fagner/Doutorado/step1/indexes2";
	private static String TOPICS_FILE = "/home/fagner/Doutorado/topics2014.xml";

	private static String STEP1_DIR = "/home/fagner/Doutorado/step1/";
	private static String RESULTS_DIR = STEP1_DIR + "results2/";

	private static String FILTER_RESULT = RESULTS_DIR + "filter/";

	private static String DIAGNOSIS_DIR = RESULTS_DIR + "diagnosis/";
	private static String TREATMENT_DIR = RESULTS_DIR + "treatment/";
	private static String PROGNOSIS_DIR = RESULTS_DIR + "prognosis/";
	private static String REVIEW_DIR = RESULTS_DIR + "review/";
	private static String ETIOLOGY_DIR = RESULTS_DIR + "etiology/";

	private static String[] header;

	public static void initiliaze() throws IOException {
		(new File(RESULTS_DIR)).mkdirs();
		(new File(FILTER_RESULT)).mkdirs();
		(new File(TREATMENT_DIR)).mkdirs();
		(new File(DIAGNOSIS_DIR)).mkdirs();
		(new File(PROGNOSIS_DIR)).mkdirs();
		(new File(REVIEW_DIR)).mkdirs();
		(new File(ETIOLOGY_DIR)).mkdirs();

		dir = FSDirectory.open(new File(INDEX_DIR));
		is = new IndexSearcher(dir);

		CSVWriter writer = new CSVWriter(new FileWriter(STEP1_DIR + "result.csv", false));

		header = new String[6];
		header[0] = "topic";
		header[1] = "treatement";
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

			List<String> pmids = PubMed.search(filterQueries[i], initialDate, finalDate);
			System.out.println("pmids: " + pmids.size());

			CSVWriter writer = new CSVWriter(new FileWriter(FILTER_RESULT + header[i+1] + ".csv", true));

			for (String pmid : pmids) {
				String[] pmidString = new String[1];

				pmidString[0] = pmid;
				writer.writeNext(pmidString);
			}


			writer.close();

			System.out.println("Filter Result from " + header[i+1] + " saved");
		}
	}

	public static void main(String[] args) throws ParseException, SAXException, ParserConfigurationException, TransformerException, IOException, JSONException, InterruptedException {
		initiliaze();

		List<Topic> topics = getTopics(TOPICS_FILE);

		onlineExperiment(topics);
		//		offlineExperiment(topics);

		//		search("mesh muco cutaneous lymph no de syndrome");
		//		System.out.println(search("pmid", "19906740"));
		//		simplestSearch("Mesh");
		//		
		is.close();
	}

	private static void offlineExperiment(List<Topic> topics) throws IOException, SAXException, ParseException, ParserConfigurationException, TransformerException {
		//		search("filename", "29906817.nxml");

		for (Topic topic : topics) {
			String[] results = new String[6];

			results[0] = topic.getNumber().toString();
			results[1] = searchTreatment(topic.getDescription(), topic.getNumber()).toString();
			results[2] = searchDiagnosis(topic.getDescription(), topic.getNumber()).toString();
			results[3] = searchPrognosis(topic.getDescription(), topic.getNumber()).toString();
			results[4] = searchEtiology(topic.getDescription(), topic.getNumber()).toString();
			results[5] = searchReview(topic.getDescription(), topic.getNumber()).toString();

			System.out.println("Resultados: "+results);

			CSVWriter writer = new CSVWriter(new FileWriter(STEP1_DIR + "result.csv", true));

			writer.writeNext(results);

			writer.close();

			System.out.println("Result saved");
		}

	}

	private static void onlineExperiment(List<Topic> topics) throws ParserConfigurationException, SAXException, IOException, InterruptedException, ParseException, TransformerException {
		File[] files = new File(FILTER_RESULT).listFiles();
		if (files.length == 0)
			filterPapers();

		for (Topic topic : topics) {
			String[] results = new String[6];
			
			results[0] = topic.getNumber().toString();
			results[1] = searchByCategory(topic.getDescription(), topic.getNumber(), "treatement.csv", TREATMENT_DIR).toString();
			results[2] = searchByCategory(topic.getDescription(), topic.getNumber(), "diagnosis.csv", DIAGNOSIS_DIR).toString();
			results[3] = searchByCategory(topic.getDescription(), topic.getNumber(), "prognosis.csv", PROGNOSIS_DIR).toString();
			results[4] = searchByCategory(topic.getDescription(), topic.getNumber(), "etiology.csv", ETIOLOGY_DIR).toString();
			results[5] = searchByCategory(topic.getDescription(), topic.getNumber(), "review.csv", REVIEW_DIR).toString();
			
			CSVWriter writer = new CSVWriter(new FileWriter(STEP1_DIR + "result.csv", true));

			writer.writeNext(results);
			writer.close();

			System.out.println("Result saved");
		}
	}

	private static Integer searchDiagnosis(String q, int topicNumber) throws ParseException, IOException, ParserConfigurationException, TransformerException {
		String search = "";

		System.out.println("Diagnosis query: " + q);

		QueryParser parser = new  MultiFieldQueryParser(LUCENE_VERSION, new String[] {"title", "abstract"}, new StandardAnalyzer(LUCENE_VERSION));
		Query t1 = parser.parse("sensitiv* OR accuracy*");

		parser = new  MultiFieldQueryParser(LUCENE_VERSION, new String[] {"meshTerms"}, new StandardAnalyzer(LUCENE_VERSION));
		Query t2 = parser.parse("\"sensitivity and specificity\" OR \"predictive value of tests\"");

		parser = new  MultiFieldQueryParser(LUCENE_VERSION, new String[] {"title", "abstract"}, new StandardAnalyzer(LUCENE_VERSION));
		Query t3 = parser.parse("predictive AND value*");

		BooleanQuery bq = new BooleanQuery();
		bq.add(t1, BooleanClause.Occur.SHOULD);
		bq.add(t2, BooleanClause.Occur.SHOULD);
		bq.add(t3, BooleanClause.Occur.SHOULD);

		System.out.println("Filter: " + bq.toString());

		Filter filter = new QueryWrapperFilter(bq);

		parser = new  QueryParser(LUCENE_VERSION, "content", new StandardAnalyzer(LUCENE_VERSION));
		Query t4 = parser.parse(q);

		TopDocs hits = is.search(t4, filter, 1);

		System.out.println("Quantidade: " + hits.totalHits);

		hits = is.search(t4, filter, hits.totalHits);

		createXMLResult(hits, DIAGNOSIS_DIR, topicNumber);

		return hits.totalHits;
	}

	private static Integer searchTreatment(String topicDescription, int topicNumber) throws ParseException, IOException, ParserConfigurationException, TransformerException {
		System.out.println("Treatment query: " + topicDescription);

		//		JsonArray meshTerms = NCBOAnnotator.annotate(topicDescription);
		//		for (JsonElement meshTerm : meshTerms) {
		//			System.out.println(meshTerm);
		//		}

		QueryParser parser = new  MultiFieldQueryParser(LUCENE_VERSION, new String[] {"title", "abstract"}, new StandardAnalyzer(LUCENE_VERSION));
		Query t1 = parser.parse("randomized OR placebo");

		parser = new  QueryParser(LUCENE_VERSION, "type", new StandardAnalyzer(LUCENE_VERSION));
		Query t2 = parser.parse("\"randomized controlled trial\"");

		BooleanQuery bq = new BooleanQuery();
		bq.add(t1, BooleanClause.Occur.SHOULD);
		bq.add(t2, BooleanClause.Occur.SHOULD);

		System.out.println("Filter: " + bq.toString());

		Filter filter = new QueryWrapperFilter(bq);

		parser = new  QueryParser(LUCENE_VERSION, "content", new StandardAnalyzer(LUCENE_VERSION));
		Query t3 = parser.parse(topicDescription);

		TopDocs hits = is.search(t3, filter, 1);

		System.out.println("Quantidade: " + hits.totalHits);

		hits = is.search(t3, filter, hits.totalHits);

		createXMLResult(hits, TREATMENT_DIR, topicNumber);
		return hits.totalHits;
	}

	private static Integer searchPrognosis(String q, int topicNumber) throws ParseException, IOException, ParserConfigurationException, TransformerException {
		System.out.println("Prognosis query: " + q);

		QueryParser parser = new  QueryParser(LUCENE_VERSION, "keywords", new StandardAnalyzer(LUCENE_VERSION));
		Query t1 = parser.parse("prognosis OR \"cohort effect\" OR \"cohort studies\" OR \"models, statistical\"");

		parser = new  MultiFieldQueryParser(LUCENE_VERSION, new String[] {"title", "abstract"}, new StandardAnalyzer(LUCENE_VERSION));
		Query t2 = parser.parse("diagnosed OR cohort* OR predictor* OR death*");

		BooleanQuery bq = new BooleanQuery();
		bq.add(t1, BooleanClause.Occur.SHOULD);
		bq.add(t2, BooleanClause.Occur.SHOULD);

		System.out.println("Filter: " + bq.toString());

		Filter filter = new QueryWrapperFilter(bq);

		parser = new  QueryParser(LUCENE_VERSION, "content", new StandardAnalyzer(LUCENE_VERSION));
		Query t3 = parser.parse(q);

		TopDocs hits = is.search(t3, filter, 1);

		System.out.println("Quantidade: " + hits.totalHits);

		hits = is.search(t3, filter, hits.totalHits);

		createXMLResult(hits, PROGNOSIS_DIR, topicNumber);
		return hits.totalHits;
	}

	public static Integer searchReview(String q, int topicNumber) throws IOException, ParseException, ParserConfigurationException, TransformerException {
		System.out.println("Review query: " + q);

		BooleanQuery bq = new BooleanQuery();

		QueryParser parser = new  MultiFieldQueryParser(Version.LUCENE_30, new String[] {"type", "title", "abstract", "keywords"}, new StandardAnalyzer(Version.LUCENE_30));
		Query t1 = parser.parse("\"meta analysis\"");

		parser = new  MultiFieldQueryParser(Version.LUCENE_30, new String[] {"title", "abstract"}, new StandardAnalyzer(Version.LUCENE_30));
		Query t2 = parser.parse("review OR search*");

		bq.add(t1, BooleanClause.Occur.SHOULD);
		bq.add(t2, BooleanClause.Occur.SHOULD);

		System.out.println(bq.toString());

		Filter filter = new QueryWrapperFilter(bq);

		parser = new  QueryParser(Version.LUCENE_30, "content", new StandardAnalyzer(Version.LUCENE_30));
		Query t3 = parser.parse(q);

		TopDocs hits = is.search(t3, filter, 1);

		System.out.println("Quantidade: " + hits.totalHits);

		hits = is.search(t3, filter, hits.totalHits);

		createXMLResult(hits, REVIEW_DIR, topicNumber);

		return hits.totalHits;
	}

	private static Integer searchEtiology(String q, int topicNumber) throws ParseException, IOException, ParserConfigurationException, TransformerException {
		System.out.println("Etiology query: " + q);

		BooleanQuery bq = new BooleanQuery();

		QueryParser parser = new  MultiFieldQueryParser(Version.LUCENE_30, new String[] {"title", "abstract", "keywords"}, new StandardAnalyzer(Version.LUCENE_30));
		Query t1 = parser.parse("risk OR mortality");

		parser = new  MultiFieldQueryParser(Version.LUCENE_30, new String[] {"title", "abstract"}, new StandardAnalyzer(Version.LUCENE_30));
		Query t2 = parser.parse("cohort");


		bq.add(t1, BooleanClause.Occur.SHOULD);
		bq.add(t2, BooleanClause.Occur.SHOULD);

		System.out.println(bq.toString());

		Filter filter = new QueryWrapperFilter(bq);

		parser = new  QueryParser(Version.LUCENE_30, "content", new StandardAnalyzer(Version.LUCENE_30));
		Query t3 = parser.parse(q);

		TopDocs hits = is.search(t3, filter, 1);

		System.out.println("Quantidade: " + hits.totalHits);

		hits = is.search(t3, filter, hits.totalHits);

		createXMLResult(hits, ETIOLOGY_DIR, topicNumber);

		return hits.totalHits;
	}

	private static Integer searchByCategory(String topicDescription, int topicNumber, String fileFilterResults, String dir) throws ParserConfigurationException, SAXException, IOException, InterruptedException, ParseException, TransformerException {
		String[] pmids;

		try (
				Reader reader = Files.newBufferedReader(Paths.get(FILTER_RESULT + fileFilterResults));
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

		for (ScoreDoc scoreDoc : hits.scoreDocs) {
			Document doc = is.doc(scoreDoc.doc);
		}

		createXMLResult(hits, dir, topicNumber);
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

	public static List<Topic> getTopics(String filepath) throws IOException, SAXException {
		Digester dig = new Digester();
		dig.setValidating(false);

		dig.addObjectCreate("topics", ArrayList.class);
		dig.addObjectCreate("topics/topic", Topic.class);

		dig.addSetProperties("topics/topic", "number", "number");
		dig.addSetProperties("topics/topic", "type", "type");

		dig.addBeanPropertySetter("topics/topic/description", "description");
		dig.addBeanPropertySetter("topics/topic/summary", "summary");

		dig.addSetNext("topics/topic", "add");

		List<Topic> topics = dig.parse(new File(filepath));

		return topics;
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

			Element abstractElement = xmlDoc.createElement("abstract");
			abstractElement.appendChild(xmlDoc.createTextNode(luceneDoc.get("abstract")));
			article.appendChild(abstractElement);

			String kwString = luceneDoc.get("keywords");
			if (!kwString.isEmpty()) {
				Element keywords = xmlDoc.createElement("keywords");
				keywords.appendChild(xmlDoc.createTextNode(kwString));
				article.appendChild(keywords);
			}

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