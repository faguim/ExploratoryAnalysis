package step1;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Article {
	@XmlAttribute private String articleTitle;
	@XmlAttribute private String articleType;
	@XmlAttribute private String abstractArticle = "";
	@XmlAttribute private List<String> keywords = new ArrayList<>();
	@XmlAttribute private List<Section> sections = new ArrayList<>();
	
	public void addSection(Section section) {
		this.sections.add(section);
	}
	
	public String getArticleTitle() {
		return articleTitle;
	}

	public void setArticleTitle(String articleTitle) {
		this.articleTitle = articleTitle;
	}

	public String getArticleType() {
		return articleType;
	}

	public void setArticleType(String articleType) {
		this.articleType = articleType;
	}

	public String getAbstractArticle() {
		return abstractArticle;
	}

	public void setAbstractArticle(String abstractArticle) {
		this.abstractArticle += abstractArticle + " ";
	}
	
	

	@Override
	public String toString() {
		return "Article [articleTitle=" + articleTitle + ", articleType=" + articleType + ", abstractArticle="
				+ abstractArticle + "]";
	}

	public void addKeywords(String keyword) {
		this.getKeywords().add(keyword);
	}

	public List<String> getKeywords() {
		return keywords;
	}

	public void setKeywords(List<String> keywords) {
		this.keywords = keywords;
	}
}