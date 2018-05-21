package step1;

public class Topic {
	private Integer number;
	private String type;
	private String description;
	private String summary;
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getSummary() {
		return summary;
	}
	public void setSummary(String summary) {
		this.summary = summary;
	}

	@Override
	public String toString() {
		return "Topic [number=" + getNumber() + ", type=" + type + ", description=" + description + ", summary=" + summary
				+ "]";
	}
	public Integer getNumber() {
		return number;
	}
	public void setNumber(Integer number) {
		this.number = number;
	}
}