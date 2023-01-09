package com.github.rumsfield.konquest.display;

public interface ViewableMenu {

	public DisplayMenu getCurrentView();
	
	public DisplayMenu updateState(int slot, boolean clickType);
}
