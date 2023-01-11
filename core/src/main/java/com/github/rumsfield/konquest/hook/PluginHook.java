package com.github.rumsfield.konquest.hook;

public interface PluginHook {

	default void reload() {}
	
	default void shutdown(){}
	
	boolean isEnabled();
	
}
