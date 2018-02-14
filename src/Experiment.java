import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.json.JSONException;
import org.xml.sax.SAXException;

import crawler.PubMed;
import ncbo.NCBOAnnotator;

public class Experiment {
	
	public static void main(String[] args) throws TransformerException, ParserConfigurationException, SAXException, IOException {
		try {
			//Get papers from PubMed.
			PubMed.fetchByTerm("respiratory+failure");
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
