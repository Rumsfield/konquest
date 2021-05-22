package konquest.model;

import konquest.utility.MessagePath;

public enum KonPrefixCategory {

	CLERGY			(MessagePath.PREFIX_CATEGORY_CLERGY.getMessage()),
	NOBILITY		(MessagePath.PREFIX_CATEGORY_NOBILITY.getMessage()),
	TRADESMAN		(MessagePath.PREFIX_CATEGORY_TRADESMAN.getMessage()),
	MILITARY		(MessagePath.PREFIX_CATEGORY_MILITARY.getMessage()),
	FARMING			(MessagePath.PREFIX_CATEGORY_FARMING.getMessage()),
	COOKING			(MessagePath.PREFIX_CATEGORY_COOKING.getMessage()),
	FISHING			(MessagePath.PREFIX_CATEGORY_FISHING.getMessage()),
	JOKING			(MessagePath.PREFIX_CATEGORY_JOKING.getMessage()),
	ROYALTY			(MessagePath.PREFIX_CATEGORY_ROYALTY.getMessage());
	
	private String title;
	KonPrefixCategory(String title) {
		this.title = title;
	}
	
	public String getTitle() {
		return title;
	}
	
}
