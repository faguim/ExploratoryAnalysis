package step1;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.xml.bind.UnmarshalException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.digester3.Digester;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.xml.sax.SAXException;

public class XMLtoLuceneDocument {
	Digester dig;
	private static Document doc;
	
	public XMLtoLuceneDocument() throws ParserConfigurationException, SAXException {
		SAXParserFactory spf = SAXParserFactory.newInstance();
		
		spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		spf.setValidating(false);
		
		SAXParser p = spf.newSAXParser();
		
		dig = new Digester(p);

		dig.addObjectCreate("article", XMLtoLuceneDocument.class);
		dig.addObjectCreate("article", Article.class);
		
		dig.addSetProperties("article", "article-type", "articleType" );
		
	    dig.addBeanPropertySetter("article/front/article-meta/title-group/article-title", "articleTitle");
		dig.addBeanPropertySetter( "article/front/article-meta/abstract/p", "abstractArticle");
		
		dig.addCallMethod("article/front/article-meta/kwd-group/kwd", "addKeywords", 1);
		dig.addCallParam("article/front/article-meta/kwd-group/kwd", 0);

		dig.addSetNext("article", "populateDocument");
	}
	
	public void populateDocument(Article article) {
		doc = new Document();
		doc.add(new Field("title", article.getArticleTitle(), Field.Store.YES, Field.Index.ANALYZED));
		doc.add(new Field("type", article.getArticleType(), Field.Store.YES, Field.Index.ANALYZED));
		doc.add(new Field("abstract", article.getAbstractArticle(), Field.Store.YES, Field.Index.ANALYZED));
		doc.add(new Field("keywords", article.getKeywords().toString(), Field.Store.YES, Field.Index.ANALYZED));
	}
	
	public static Document parse(File f) throws ParserConfigurationException, SAXException, UnmarshalException, IOException {
		XMLtoLuceneDocument handler = new XMLtoLuceneDocument();

		handler.dig.parse(f);
		doc.add(new Field("content", new FileReader(f)));
		doc.add(new Field("filename", f.getName(), Field.Store.YES, Field.Index.NOT_ANALYZED));
		doc.add(new Field("fullpath", f.getCanonicalPath(), Field.Store.YES, Field.Index.NOT_ANALYZED));
		
		return doc;
	}
	
	public static void main(String[] args) {
		Document doc = null;
		try {
			doc = parse(new File("/home/fagner/TREC/papers/pmc-00/20/2374409.nxml"));
			System.out.println(doc);
		} catch (UnmarshalException | IOException | ParserConfigurationException | SAXException e) {
			e.printStackTrace();
		}
	}
	
	//Construtor para indexar seções do artigo
//	public XMLtoLuceneDocument() throws ParserConfigurationException, SAXException, IOException {
//		SAXParserFactory spf = SAXParserFactory.newInstance();
//		
//		spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
//		spf.setValidating(false);
//		
//		SAXParser p = spf.newSAXParser();
//		
//		dig = new Digester(p);
//		
//		dig.addObjectCreate("article", Article.class);
//	    dig.addSetProperties( "article", "article-type", "articleType" );
//		
//		dig.addObjectCreate("article/body/sec", Section.class);
//
//	    dig.addSetProperties( "article/body/sec", "sec-type", "secType" );
//		
////		dig.addBeanPropertySetter( "article/body/sec/title", "title");
////		dig.addBeanPropertySetter( "article/body/sec/p", "text");
//		dig.addBeanPropertySetter( "article/body/sec/*/p", "text");
//		dig.addBeanPropertySetter( "article/front/article-meta/abstact/p", "abstractArticle");
//
////	    dig.addCallMethod("!article/body/sec/?", "setText", 1);
////	    dig.addCallParam("!article/body/sec/?", 0);
//
//		dig.addSetNext( "article/body/sec", "addSection" );
//		
//		dig.addBeanPropertySetter("article/front/article-meta/title-group/article-title", "articleTitle");
//		
////		dig.addCallMethod("article/front/article-meta/article-id", "addId", 1);
////		dig.addCallParam("article/front/article-meta/article-id", 0);
//		
//
//		Article article = dig.parse(new File("/home/fagner/TREC/papers/pmc-00/00/2630847.nxml"));
//		System.out.println(article);
//	}
}
