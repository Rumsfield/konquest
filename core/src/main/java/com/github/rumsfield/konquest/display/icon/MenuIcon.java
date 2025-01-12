package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.display.StateMenu;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.utility.HelperUtil;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public abstract class MenuIcon {

	private int index; // The slot index in the inventory of this icon
	private StateMenu.State state; // The state that this icon will update the menu to

	private final List<String> alerts;
	private final List<String> properties;
	private final List<String> values;
	private final List<String> descriptions;
	private final List<String> hints;

	public MenuIcon(int index) {
		this.index = index;
		this.state = null;
		this.alerts = new ArrayList<>();
		this.properties = new ArrayList<>();
		this.values = new ArrayList<>();
		this.descriptions = new ArrayList<>();
		this.hints = new ArrayList<>();
	}

	/*
	 * Common Methods
	 */

	public int getIndex() {
		return index;
	}

	public void setIndex(int val) {
		this.index = val;
	}

	public @Nullable StateMenu.State getState() {
		return state;
	}

	public void setState(StateMenu.State state) {
		this.state = state;
	}

	public void addAlert(String alert) {
		alerts.addAll(HelperUtil.stringPaginate(alert, DisplayManager.alertFormat));
	}

	public void addProperty(String property) {
		properties.addAll(HelperUtil.stringPaginate(property, DisplayManager.propertyFormat));
	}

	public void addNameValue(String name, String value) {
		values.add(DisplayManager.loreFormat+name+": "+DisplayManager.valueFormat+value);
	}

	public void addNameValue(String name, int value) {
		values.add(DisplayManager.loreFormat+name+": "+DisplayManager.valueFormat+value);
	}

	public void addDescription(String description) {
		descriptions.addAll(HelperUtil.stringPaginate(description, DisplayManager.loreFormat));
	}

	public void addDescription(String description, ChatColor color) {
		descriptions.addAll(HelperUtil.stringPaginate(description, color));
	}

	public void addError(String description) {
		descriptions.addAll(HelperUtil.stringPaginate(description, DisplayManager.errorFormat));
	}

	public void addHint(String hint) {
		hints.addAll(HelperUtil.stringPaginate(hint, DisplayManager.hintFormat));
	}

	public List<String> getLore() {
		List<String> lore = new ArrayList<>();
		lore.addAll(alerts);
		lore.addAll(properties);
		lore.addAll(values);
		lore.addAll(descriptions);
		lore.addAll(hints);
		return lore;
	}

	/*
	 * Abstract Methods
	 */

	public abstract String getName();

	public abstract ItemStack getItem();

	public abstract boolean isClickable();
	
}