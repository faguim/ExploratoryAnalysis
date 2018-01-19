package microsoft;

import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class InitialTest {
	public static void main(String[] args) {
		HttpClient httpclient = HttpClients.createDefault();

		try {

			// The ID of a public sample LUIS app that recognizes intents for turning on and
			// off lights
			String AppId = "59b79259-a995-425b-b2a9-e14f5d8862f5";
			

			// Add your subscription key
			String SubscriptionKey = "0f03533f6b4e4e5b9d6dc33ef75659a4";

			URIBuilder builder = new URIBuilder(
					"https://westus.api.cognitive.microsoft.com/luis/v2.0/apps/" + AppId + "?");

			builder.setParameter("q", "turn on the left light");
			builder.setParameter("timezoneOffset", "0");
			builder.setParameter("verbose", "false");
			builder.setParameter("spellCheck", "false");
			builder.setParameter("staging", "false");

			URI uri = builder.build();
			HttpGet request = new HttpGet(uri);
			request.setHeader("Ocp-Apim-Subscription-Key", SubscriptionKey);

			HttpResponse response = httpclient.execute(request);
			HttpEntity entity = response.getEntity();

			if (entity != null) {
				System.out.println(EntityUtils.toString(entity));
			}
		}

		catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
}
