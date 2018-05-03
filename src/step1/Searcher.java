package step1;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class Searcher {
	private static String INDEX_DIR = "/home/fagner/TREC/indexes3";
	private static Directory dir;
	private static IndexSearcher is;
	
	public static void main(String[] args) throws IOException, ParseException {
		String q = "abdominal pain";
		dir = FSDirectory.open(new File(INDEX_DIR));
		is = new IndexSearcher(dir);
//		search(q);
//		searchReview(q);
		simplestSearch(q);
	}

	public static void searchReview(String q) throws IOException, ParseException {
		BooleanQuery bq = new BooleanQuery();

		QueryParser parser = new  QueryParser(Version.LUCENE_30, "type", new SimpleAnalyzer());
		Query t1 = parser.parse("meta analysis");
		
		parser = new  QueryParser(Version.LUCENE_30, "title", new SimpleAnalyzer());
		Query t2 = parser.parse("meta analysis");
		
		parser = new  QueryParser(Version.LUCENE_30, "abstract", new SimpleAnalyzer());
		Query t3 = parser.parse("meta analysis");
		
		parser = new  QueryParser(Version.LUCENE_30, "keywords", new SimpleAnalyzer());
		Query t4 = parser.parse("meta analysis");
		
		parser = new  QueryParser(Version.LUCENE_30, "type", new SimpleAnalyzer());
		Query t5 = parser.parse("review");
		
		parser = new  QueryParser(Version.LUCENE_30, "title", new SimpleAnalyzer());
		Query t6 = parser.parse(q);
		
		parser = new  QueryParser(Version.LUCENE_30, "abstract", new SimpleAnalyzer());
		Query t7 = parser.parse(q);

		bq.add(t1, BooleanClause.Occur.SHOULD);
		bq.add(t2, BooleanClause.Occur.SHOULD);
		bq.add(t3, BooleanClause.Occur.SHOULD);
		bq.add(t4, BooleanClause.Occur.SHOULD);
		bq.add(t5, BooleanClause.Occur.SHOULD);
		bq.add(t6, BooleanClause.Occur.SHOULD);
		bq.add(t7, BooleanClause.Occur.SHOULD);

		TopDocs hits = is.search(bq, 10);
		System.out.println("Quantidade: " + hits.totalHits);
		for (ScoreDoc scoreDoc : hits.scoreDocs) {
			Document doc = is.doc(scoreDoc.doc);
			System.out.println("Titulo: "+doc.get("title"));
			System.out.println("Resumo: "+doc.get("abstract"));
			System.out.println("Keywords: "+doc.get("keywords") + "\n");

		}
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
		is.close();
	}
	
	public static void simplestSearch(String q) throws CorruptIndexException, IOException {
		Term t = new Term("abstract", q);
		Query query = new TermQuery(t);
		TopDocs topDocs = is.search(query, 10);
		System.out.println(topDocs.totalHits);
		is.close();
	}
}
