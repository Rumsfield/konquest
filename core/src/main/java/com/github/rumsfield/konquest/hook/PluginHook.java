package com.github.rumsfield.konquest.hook;

public interface PluginHook {

	/**
	 * Reload the hook to attempt to register with the other plugin.
	 * @return Status code:
	 * 		0	Successfully registered plugin hook
	 * 		1	Hook plugin is missing or disabled
	 * 		2	Hook plugin is disabled
	 * 		3	Integration is turned off in Konquest config
	 * 		-1	A problem occurred while registering the hook
	 */
	default int reload() {return 0;}
	
	default void shutdown(){}
	
	boolean isEnabled();

	String getPluginName();
	
}
