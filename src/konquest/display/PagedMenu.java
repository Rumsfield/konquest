package konquest.display;

import java.util.ArrayList;

import konquest.utility.ChatUtil;

public class PagedMenu {

	private ArrayList<DisplayMenu> pages;
	
	public PagedMenu() {
		pages = new ArrayList<DisplayMenu>();
	}
	
	/*
	 * Pages must have a bottom row dedicated to navigation buttons
	 */
	
	public void addPage(int index, int rows, String label) {
		if(index > pages.size()) {
			ChatUtil.printDebug("Failed to add page beyond list index");
			return;
		}
		if(rows > 5) {
			ChatUtil.printDebug("Failed to add page with too many rows");
			return;
		}
		pages.add(index, new DisplayMenu(rows+1, label));
	}
	
	public DisplayMenu getPage(int index) {
		if(index < 0 || index > pages.size()) {
			ChatUtil.printDebug("Failed to get page beyond list index");
			return null;
		}
		return pages.get(index);
	}
	
}
