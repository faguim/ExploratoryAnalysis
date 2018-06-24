package step2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.FieldCacheTermsFilter;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.xml.sax.SAXException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import crawler.PubMed;
import input.Input;
import ncbo.NCBOAnnotator;
import step1.Topic;

public class SemanticSearcher {
	private static String INDEX_DIR = "/home/fagner/Doutorado/step1/indexes";

	private static String STEP2_DIR = "/home/fagner/Doutorado/step2/";
//	private static String STEP1_DIR = "/home/fagner/Doutorado/step1/";
	private static String STEP1_DIR = "/home/fagner/Doutorado/step1 (tests)/";


	private static String RESULTS_DIR = STEP2_DIR + "results/";
	
	private static String DIAGNOSIS_DIR = RESULTS_DIR + "diagnosis/";
	private static String TREATMENT_DIR = RESULTS_DIR + "treatment/";
	private static String PROGNOSIS_DIR = RESULTS_DIR + "prognosis/";
	private static String REVIEW_DIR = RESULTS_DIR + "review/";
	private static String ETIOLOGY_DIR = RESULTS_DIR + "etiology/";
	
	private static String FILTER_RESULT = STEP1_DIR + "filteredPMIDS/";

	private static final Version LUCENE_VERSION = Version.LUCENE_30;

	private static Directory dir;
	private static IndexSearcher is;
	
	private static String[] header;
	
	private static void offlineExperiment(List<Topic> topics) throws ParserConfigurationException, SAXException, IOException, InterruptedException, ParseException, TransformerException {
		String[] results = new String[6];

		CSVWriter writer = new CSVWriter(new FileWriter(STEP2_DIR + "result-onlineexperiment.csv", true));

		for (Topic topic : topics) {
			System.out.println(topic.getDescription());

			JsonArray jsonArray = NCBOAnnotator.annotate(topic.getDescription());
			System.out.println(jsonArray);
			
			String meshTerms = "";
			
			for (JsonElement jsonElement : jsonArray) {
				String meshTerm = jsonElement.getAsJsonObject().get("prefLabel").getAsString();
				meshTerms += meshTerm + ", ";
			}
			System.out.println(meshTerms);
			
			results[0] = topic.getNumber().toString();
			results[1] = searchByCategory(meshTerms, topic.getNumber(), "treatement.csv", TREATMENT_DIR).toString();
			results[2] = searchByCategory(meshTerms, topic.getNumber(), "diagnosis.csv", DIAGNOSIS_DIR).toString();
			results[3] = searchByCategory(meshTerms, topic.getNumber(), "prognosis.csv", PROGNOSIS_DIR).toString();
			results[4] = searchByCategory(meshTerms, topic.getNumber(), "etiology.csv", ETIOLOGY_DIR).toString();
			results[5] = searchByCategory(meshTerms, topic.getNumber(), "review.csv", REVIEW_DIR).toString();

			writer.writeNext(results);
		}
		writer.close();

		System.out.println("Result saved");
	}
	
	private static void createFilters() {
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
		}
	}
	
	private static void onlineExperiment(List<Topic> topics) throws ParserConfigurationException, SAXException, IOException, InterruptedException, ParseException, TransformerException {
		String[] results = new String[6];

//		CSVWriter writer = new CSVWriter(new FileWriter(STEP2_DIR + "result-onlineexperiment.csv", true));

		for (Topic topic : topics) {
			JsonArray jsonArray = NCBOAnnotator.annotate(topic.getDescription());
			
			String meshTerms = "";
			
			for (JsonElement jsonElement : jsonArray) {
				String meshTerm = jsonElement.getAsJsonObject().get("prefLabel").getAsString();
				meshTerms += meshTerm + ", ";
			}
			
			results[0] = topic.getNumber().toString();
			System.out.println(topic.getDescription());
			System.out.println(meshTerms);
			results[1] = onlineSearchByCategory(topic.getDescription(), topic.getNumber(), "treatment", TREATMENT_DIR, FILTER_RESULT).toString();
//			results[2] = onlineSearchByCategory(meshTerms, topic.getNumber(), "diagnosis", DIAGNOSIS_DIR, FILTER_RESULT).toString();
//			results[3] = onlineSearchByCategory(meshTerms, topic.getNumber(), "prognosis", PROGNOSIS_DIR, FILTER_RESULT).toString();
//			results[4] = onlineSearchByCategory(meshTerms, topic.getNumber(), "etiology", ETIOLOGY_DIR, FILTER_RESULT).toString();
//			results[5] = onlineSearchByCategory(meshTerms, topic.getNumber(), "review", REVIEW_DIR, FILTER_RESULT).toString();

//			results[2] = searchByCategory(topic.getDescription(), topic.getNumber(), "diagnosis.csv", DIAGNOSIS_DIR).toString();
//			results[3] = searchByCategory(topic.getDescription(), topic.getNumber(), "prognosis.csv", PROGNOSIS_DIR).toString();
//			results[4] = searchByCategory(topic.getDescription(), topic.getNumber(), "etiology.csv", ETIOLOGY_DIR).toString();
//			results[5] = searchByCategory(topic.getDescription(), topic.getNumber(), "review.csv", REVIEW_DIR).toString();

//			writer.writeNext(results);
			break;
		}
//		writer.close();

		System.out.println("Result saved");
	}
	
	private static Integer searchByCategory(String meshTerms, int topicNumber, String fileFilterResults, String dir) throws ParserConfigurationException, SAXException, IOException, InterruptedException, ParseException, TransformerException {
		String[] pmids = getFilteredPMIDs(fileFilterResults);

		Filter filter = new FieldCacheTermsFilter("pmid", pmids);

		QueryParser parser = new  QueryParser(LUCENE_VERSION, "content", new StandardAnalyzer(LUCENE_VERSION));
		Query t3 = parser.parse(meshTerms);

		TopDocs hits = is.search(t3, filter, 1);

		hits = is.search(t3, filter, hits.totalHits);

		createCSV(hits, dir, topicNumber);
		return hits.totalHits;
	}
	
	private static Integer onlineSearchByCategory(String meshTerms, int topicNumber, String filter, String dir, String filter_dir) throws ParserConfigurationException, SAXException, IOException, InterruptedException, ParseException, TransformerException {
		String webEnv = "";
		String queryKey = "";
		long count = 0;
		try {
			Reader reader = Files.newBufferedReader(Paths.get(filter_dir + "search-metadata-result.csv"));
			CSVReader csvReader = new CSVReader(reader);
			String [] nextLine;

			while ((nextLine = csvReader.readNext()) != null) {
				if(nextLine[0].equals(filter)) {
					webEnv = nextLine[1];
					queryKey = nextLine[2];
					count = Long.valueOf(nextLine[3]);
					break;
				}
			}
			csvReader.close();
		} catch (NoSuchFileException e) {
			System.err.println("Don't exist filters yet");
		}
		List<String> pmidList = PubMed.searchWithFilter(meshTerms, webEnv, queryKey, count);
		System.out.println("Recuperados:" + pmidList.size());
		return 0;
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
	
	
	private static String[] getFilteredPMIDs(String fileFilterResults) throws IOException {
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
		return pmids;
	}
	
	public static void initiliaze() throws IOException {
		(new File(RESULTS_DIR)).mkdirs();
		(new File(TREATMENT_DIR)).mkdirs();
		(new File(DIAGNOSIS_DIR)).mkdirs();
		(new File(PROGNOSIS_DIR)).mkdirs();
		(new File(REVIEW_DIR)).mkdirs();
		(new File(ETIOLOGY_DIR)).mkdirs();

		dir = FSDirectory.open(new File(INDEX_DIR));
		is = new IndexSearcher(dir);

		CSVWriter writer = new CSVWriter(new FileWriter(STEP2_DIR + "result.csv", false));

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
	
	public static void createCSV(TopDocs hits, String dir, int topicNumber) throws CorruptIndexException, IOException {
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
	
	public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException, InterruptedException, ParseException, TransformerException {
		initiliaze();

		List<Topic> topics = Input.getTopics();

//		offlineExperiment(topics);
		onlineExperiment(topics);
	}
	
	
}
