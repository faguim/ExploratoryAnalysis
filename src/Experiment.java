import org.json.JSONException;

import crawler.PubMed;

public class Experiment {
	
	public static void main(String[] args) {
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
