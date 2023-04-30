package com.github.rumsfield.konquest.display;

public interface ViewableMenu {

	DisplayMenu getCurrentView();
	
	DisplayMenu updateState(int slot, boolean clickType);
}
