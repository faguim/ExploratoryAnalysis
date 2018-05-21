package step1;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import au.com.bytecode.opencsv.CSVWriter;

public class Searcher {
	private static final Version LUCENE_VERSION = Version.LUCENE_30;

	private static Directory dir;
	private static IndexSearcher is;

	private static String INDEX_DIR = "/home/fagner/Doutorado/step1/indexes";
	private static String TOPICS_FILE = "/home/fagner/Doutorado/topics2014.xml";
	
	private static String STEP1_DIR = "/home/fagner/Doutorado/step1/";
	private static String DIAGNOSIS_DIR = STEP1_DIR + "results/diagnosis/";
	private static String TREATMENT_DIR = STEP1_DIR + "results/treatment/";
	private static String PROGNOSIS_DIR = STEP1_DIR + "results/prognosis/";
	private static String REVIEW_DIR = STEP1_DIR + "results/review/";
	private static String ETIOLOGY_DIR = STEP1_DIR + "/results/etiology/";

	public static void initiliaze() throws IOException {
		(new File(TREATMENT_DIR)).mkdirs();
		(new File(DIAGNOSIS_DIR)).mkdirs();
		(new File(PROGNOSIS_DIR)).mkdirs();
		(new File(REVIEW_DIR)).mkdirs();
		(new File(ETIOLOGY_DIR)).mkdirs();

		dir = FSDirectory.open(new File(INDEX_DIR));
		is = new IndexSearcher(dir);

		CSVWriter writer = new CSVWriter(new FileWriter(STEP1_DIR + "result.csv", false));

		String[] header = new String[6];
		header[0] = "topic";
		header[1] = "treatement";
		header[2] = "diagnosis";
		header[3] = "prognosis";
		header[4] = "etiology";
		header[5] = "review";

		writer.writeNext(header);
        writer.close();
	}

	public static void main(String[] args) throws ParseException, SAXException, ParserConfigurationException, TransformerException, IOException {
		initiliaze();

		List<Topic> topics = getTopics(TOPICS_FILE);

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
		
		is.close();
	}

	private static Integer searchDiagnosis(String q, int topicNumber) throws ParseException, IOException, ParserConfigurationException, TransformerException {
		System.out.println("Diagnosis query: " + q);

		QueryParser parser = new  MultiFieldQueryParser(LUCENE_VERSION, new String[] {"title", "abstract"}, new StandardAnalyzer(LUCENE_VERSION));
		Query t1 = parser.parse("sensitiv* OR accuracy*");

		parser = new  MultiFieldQueryParser(LUCENE_VERSION, new String[] {"keywords"}, new StandardAnalyzer(LUCENE_VERSION));
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

	private static Integer searchTreatment(String q, int topicNumber) throws ParseException, IOException, ParserConfigurationException, TransformerException {
		System.out.println("Treatment query: " + q);

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
		Query t3 = parser.parse(q);

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

	public static void simplestSearch(String q) throws CorruptIndexException, IOException {
		Term t = new Term("abstract", q);
		Query query = new TermQuery(t);
		TopDocs topDocs = is.search(query, 10);
		System.out.println(topDocs.totalHits);
		is.close();
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