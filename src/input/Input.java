package input;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.digester3.Digester;
import org.xml.sax.SAXException;

import step1.Topic;

public class Input {
	private static String TOPICS_FILE = "/home/fagner/Doutorado/topics2014.xml";
	
	public static List<Topic> getTopics() throws IOException, SAXException {
		Digester dig = new Digester();
		dig.setValidating(false);

		dig.addObjectCreate("topics", ArrayList.class);
		dig.addObjectCreate("topics/topic", Topic.class);

		dig.addSetProperties("topics/topic", "number", "number");
		dig.addSetProperties("topics/topic", "type", "type");

		dig.addBeanPropertySetter("topics/topic/description", "description");
		dig.addBeanPropertySetter("topics/topic/summary", "summary");

		dig.addSetNext("topics/topic", "add");

		List<Topic> topics = dig.parse(new File(TOPICS_FILE));

		return topics;
	}
}
