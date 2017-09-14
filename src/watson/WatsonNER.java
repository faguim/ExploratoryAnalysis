package watson;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.ibm.watson.developer_cloud.natural_language_understanding.v1.NaturalLanguageUnderstanding;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.AnalysisResults;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.AnalyzeOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.EntitiesOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.Features;

public class WatsonNER {
	@SuppressWarnings("deprecation")
	public static void main(String[] args) {
		NaturalLanguageUnderstanding service = new NaturalLanguageUnderstanding(
				NaturalLanguageUnderstanding.VERSION_DATE_2017_02_27,
				"aca3b8dc-8808-4323-9267-08e2d0c87004",
				"ZKNM1k5fia4A"
				);

		try {
			String text = FileUtils.readFileToString(new File("samples/congenital-heart-disease-emergencies.txt"));

			EntitiesOptions entitiesOptions = new EntitiesOptions.Builder()
					.emotion(false)
					.sentiment(false)
					.limit(250)
					.build();

			Features features = new Features.Builder()
					.entities(entitiesOptions)
					.build();

			AnalyzeOptions parameters = new AnalyzeOptions.Builder()
					.text(text)
					.features(features)
					.build();

			AnalysisResults response = service
					.analyze(parameters)
					.execute();
			System.out.println(response.getEntities());
		} catch (IOException e) {
			e.printStackTrace();
		}


	}
}
