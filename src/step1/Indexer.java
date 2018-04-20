package step1;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;

public class Indexer {
	
	public static void main(String[] args) throws CorruptIndexException, LockObtainFailedException, IOException {
		String indexDir = "/home/fagner/TREC/indexes/pmc-00";
		String dataDir = "/home/fagner/TREC/pmc-00/00";
		
		long start = System.currentTimeMillis();
		
		Indexer indexer = new Indexer(indexDir);
		int numIndexed;
		
		try {
			numIndexed = indexer.index(dataDir, new TextFilesFilter());
		} finally {
			indexer.close();
		}
		
		long end = System.currentTimeMillis();
		
		System.out.println("Indexing " + numIndexed + " files took " + (end - start) + " milliseconds");
		
	}
	
	public int index(String dataDir, FileFilter filter) throws CorruptIndexException, IOException {
		File[] files = new File(dataDir).listFiles();
		
		for (File f : files) {
			if(!f.isDirectory() && !f.isHidden() && f.exists() && f.canRead() && (filter == null || filter.accept(f))) {
				indexFile(f);
			}
		}
		
		return writer.numDocs();
	}
	
	private IndexWriter writer;
	
	public Indexer(String indexDir) throws CorruptIndexException, LockObtainFailedException, IOException {
		Directory dir = FSDirectory.open(new File(indexDir));
		writer = new IndexWriter(dir, new StandardAnalyzer(Version.LUCENE_30), true, IndexWriter.MaxFieldLength.UNLIMITED);
		
	}
	
	public void close() throws CorruptIndexException, IOException {
		writer.close();
	}
	
	public void indexFile(File f) throws CorruptIndexException, IOException {
		System.out.println("Indexing " + f.getCanonicalPath());
		Document doc = getDocument(f);
		writer.addDocument(doc);
	}
	
	public Document getDocument(File f) throws IOException {
		Document doc = new Document();
		doc.add(new Field("contents", new FileReader(f)));
		doc.add(new Field("filename", f.getName(), Field.Store.YES, Field.Index.NOT_ANALYZED));
		doc.add(new Field("fullpath", f.getCanonicalPath(), Field.Store.YES, Field.Index.NOT_ANALYZED));
		return doc;
	}
	
	private static class TextFilesFilter implements FileFilter {
		public boolean accept(File path) {
			return path.getName().toLowerCase().endsWith(".nxml");
		}
	}
}
