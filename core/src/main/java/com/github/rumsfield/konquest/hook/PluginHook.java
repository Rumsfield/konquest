package com.github.rumsfield.konquest.hook;

public interface PluginHook {

	public void reload();
	
	public void shutdown();
	
	public boolean isEnabled();
	
}
