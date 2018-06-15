package step1;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.UnmarshalException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import crawler.PubMed;

public class Indexer {
	private static String INDEX_DIR = "/home/fagner/Doutorado/step1/indexes2";

	private static String PAPER_DIR = "/home/fagner/Doutorado/papers/";

	public static void main(String[] args) throws CorruptIndexException, LockObtainFailedException, IOException, ParserConfigurationException, SAXException, ParseException {
//		long start = System.currentTimeMillis();
//		Indexer indexer = new Indexer(INDEX_DIR);
//		int numIndexed = 0;
//
//		int nDir = new File(PAPER_DIR).listFiles().length;
//
//		try {
//			for (int i = 0; i < nDir; i++) {
//				String current_dir = PAPER_DIR + "pmc-0" + i +"/";
//				File[] files = new File(current_dir).listFiles();
//				System.out.println(current_dir);
//				for (int j = 0; j < files.length; j++) {
//					String current_subdir = current_dir + files[j].getName();
//					System.out.println("Nome:"+current_subdir);
//					numIndexed = indexer.index(current_subdir, new TextFilesFilter());
//				}
//			}
//		} finally {
//			indexer.close();
//		}
//
//
//		long end = System.currentTimeMillis();
//
//		System.out.println("Indexing " + numIndexed + " files took " + (end - start) + " milliseconds");

//		Bloco para teste de uma pasta específica
		Indexer indexer = new Indexer(INDEX_DIR);
		try {
			indexer.index(PAPER_DIR + "pmc-01/09/", new TextFilesFilter());
			indexer.updateIndex(PAPER_DIR + "pmc-01/09/", new TextFilesFilter());
//			indexer.viewIndexedDocs(PAPER_DIR + "pmc-01/09/", new TextFilesFilter());
		} finally {
			indexer.close();
		}
		
//		indexer.close();
	}

	public void viewIndexedDocs(String dataDir, FileFilter filter) throws IOException, ParseException {
		File[] files = new File(dataDir).listFiles();
		
		for (File f : files) {
			if(!f.isDirectory() && !f.isHidden() && f.exists() && f.canRead() && (filter == null || filter.accept(f))) {
				Document doc = Searcher.searchByFileName(f.getName());
				System.out.println(doc);
			}
		}
	}

	public int index(String dataDir, FileFilter filter) throws IOException, SAXException, ParserConfigurationException {
		File[] files = new File(dataDir).listFiles();

		for (File f : files) {
			if(!f.isDirectory() && !f.isHidden() && f.exists() && f.canRead() && (filter == null || filter.accept(f))) {
				indexFile(f);
			}
		}

		return writer.numDocs();
	}
	
	public void updateIndex(String dataDir, FileFilter filter) throws IOException, ParseException, ParserConfigurationException, SAXException {
		List<String> pmids = new ArrayList<String>();
		File[] files = new File(dataDir).listFiles();
		
		for (File f : files) {
			if(!f.isDirectory() && !f.isHidden() && f.exists() && f.canRead() && (filter == null || filter.accept(f))) {
				Document updateDoc = Searcher.searchByFileName(f.getName());
				if (updateDoc != null) {
					if (null != updateDoc.get("pmid"))
						pmids.add(updateDoc.get("pmid"));
				}
			}
		}
		Map<String, List<String>> meshTermsMap = PubMed.getMeshTerms(pmids);
		
		for (File f : files) {
			if(!f.isDirectory() && !f.isHidden() && f.exists() && f.canRead() && (filter == null || filter.accept(f))) {
				Document updateDoc = Searcher.searchByFileName(f.getName());
				if (updateDoc != null) {
					if (null != updateDoc.get("pmid")) {
						List<String> meshTerms = meshTermsMap.get(updateDoc.get("pmid"));
						if (null != meshTerms) {
							System.out.println("meshTerms: "+meshTerms);
							updateDoc.add(new Field("meshTerms", meshTerms.toString(), Field.Store.YES, Field.Index.ANALYZED));
							writer.updateDocument(new Term("filename", f.getName()), updateDoc);
						}
					}
				}
			}
		}
	}
	
	public void indexFile(File f) {
		try {
			System.out.println("Indexing " + f.getCanonicalPath());
			Document doc;
			doc = XMLtoLuceneDocument.parse(f);
			
			if (doc!=null)
				writer.addDocument(doc);
		} catch (UnmarshalException | IOException | ParserConfigurationException | SAXException e) {
			e.printStackTrace();
		}
	}

	private IndexWriter writer;

	public Indexer(String indexDir) throws CorruptIndexException, LockObtainFailedException, IOException {
		Directory dir = FSDirectory.open(new File(indexDir));
		writer = new IndexWriter(dir, new StandardAnalyzer(Version.LUCENE_30), true, IndexWriter.MaxFieldLength.UNLIMITED);

	}

	public void close() throws CorruptIndexException, IOException {
		writer.close();
	}

	private static class TextFilesFilter implements FileFilter {
		public boolean accept(File path) {
			return path.getName().toLowerCase().endsWith(".nxml");
		}
	}
}