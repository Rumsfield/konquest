package konquest.model;

public enum KonPrefixCategory {

	CLERGY			("Clergy"),
	NOBILITY		("Nobility"),
	TRADESMAN		("Tradesman"),
	MILITARY		("Military"),
	FARMING			("Farming"),
	COOKING			("Cooking"),
	FISHING			("Fishing"),
	JOKING			("Joking"),
	ROYALTY			("Royalty");
	
	private String title;
	KonPrefixCategory(String title) {
		this.title = title;
	}
	
	public String getTitle() {
		return title;
	}
	
}
