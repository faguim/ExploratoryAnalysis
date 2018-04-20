package step1;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.digester.Digester;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.xml.sax.SAXException;


public class DigesterXMLDocument {
	Digester dig;
	private static Document doc;
	
	public DigesterXMLDocument() throws ParserConfigurationException, SAXException {
		SAXParserFactory spf = SAXParserFactory.newInstance();
		
		spf.setValidating(false);
		spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		
		SAXParser p = spf.newSAXParser();
		
		
		dig = new Digester(p);
		dig.setValidating(false);
		
		dig.addObjectCreate("article", DigesterXMLDocument.class);
		dig.addObjectCreate("article", Article.class);
		
		dig.addCallMethod("article/front/article-meta/title-group/article-title", "setArticleTitle", 0);
		
		dig.addSetNext("article", "populateDocument");
	}
	
	public synchronized Document getDocument(InputStream is) {
		try {
			dig.parse(is);
		} catch(IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
		return doc;
	}
	
	public void populateDocument(Article article) {
		doc = new Document();
		doc.add(new Field("article-title", article.getArticleTitle(), Field.Store.YES, Field.Index.NOT_ANALYZED));
	}
	
	public static void main(String[] args) throws FileNotFoundException, ParserConfigurationException, SAXException {
		DigesterXMLDocument handler = new DigesterXMLDocument();

		Document doc = handler.getDocument(new FileInputStream(new File("/home/fagner/TREC/pmc-00/00/13900.nxml")));
		System.out.println(doc);
	}
}
