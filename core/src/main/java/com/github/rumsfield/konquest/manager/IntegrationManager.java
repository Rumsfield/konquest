package com.github.rumsfield.konquest.manager;


import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.hook.DiscordSrvHook;
import com.github.rumsfield.konquest.hook.LuckPermsHook;
import com.github.rumsfield.konquest.hook.PluginHook;
import com.github.rumsfield.konquest.hook.QuickShopHook;
import com.github.rumsfield.konquest.utility.ChatUtil;

import java.util.HashSet;


public class IntegrationManager {

	private final HashSet<PluginHook> hooks;
	private final LuckPermsHook luckpermsHook;
	private final QuickShopHook quickshopHook;
	private final DiscordSrvHook discordsrvHook;
	
	public IntegrationManager(Konquest konquest) {
		this.hooks = new HashSet<>();
		// Define new hooks
		luckpermsHook = new LuckPermsHook(konquest);
		quickshopHook = new QuickShopHook(konquest);
		discordsrvHook = new DiscordSrvHook(konquest);
		
		hooks.add(luckpermsHook);
		hooks.add(quickshopHook);
		hooks.add(discordsrvHook);
	}
	
	
	public void initialize() {
		// Reload any disabled hooks
		for (PluginHook hook : hooks) {
			if (!hook.isEnabled()) {
				hook.reload();
			}
		}
		ChatUtil.printDebug("Integration Manager is ready");
	}
	
	public void disable() {
		// Shutdown all hooks
		for (PluginHook hook : hooks) {
			hook.shutdown();
		}
		ChatUtil.printDebug("Integration Manager is disabled");
	}
	
	public LuckPermsHook getLuckPerms() {
		return luckpermsHook;
	}
	
	public QuickShopHook getQuickShop() {
		return quickshopHook;
	}
	
	public DiscordSrvHook getDiscordSrv() {
		return discordsrvHook;
	}
	
}
