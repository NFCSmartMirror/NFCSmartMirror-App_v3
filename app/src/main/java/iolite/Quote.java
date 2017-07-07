package iolite;

public class Quote {

	private final String quote;
	private final String author;

	public Quote(final String quote, final String author) {
		this.quote = quote;
		this.author = author;
	}

	public String getAuthor() {
		return this.author;
	}

	public String getQuote() {
		return this.quote;
	}

}
