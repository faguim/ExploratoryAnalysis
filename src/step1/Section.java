package step1;

public class Section {
	private String secType;
	private String text = "";

	public String getText() {
		return text;
	}

	public void setText(String text) {
		System.out.println("text");
		System.out.println(text);
		this.text += " " + text;
	}

	public String getSecType() {
		return secType;
	}

	public void setSecType(String secType) {
		this.secType = secType;
	}

	@Override
	public String toString() {
		return "Section [secType=" + secType + ", text=" + text + "]";
	}
}
