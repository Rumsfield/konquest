package com.github.rumsfield.konquest.manager;


import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.hook.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


public class IntegrationManager {

	private final ArrayList<PluginHook> hooks;
	private final LuckPermsHook luckpermsHook;
	private final QuickShopHook quickshopHook;
	private final DiscordSrvHook discordsrvHook;
	private final DynmapHook dynmapHook;
	private final ProtocolLibHook protocollibHook;
	private final PlaceholderAPIHook placeholderapiHook;
	
	public IntegrationManager(Konquest konquest) {
		this.hooks = new ArrayList<>();
		// Define new hooks
		luckpermsHook = new LuckPermsHook(konquest);
		quickshopHook = new QuickShopHook(konquest);
		discordsrvHook = new DiscordSrvHook(konquest);
		dynmapHook = new DynmapHook(konquest);
		protocollibHook = new ProtocolLibHook();
		placeholderapiHook = new PlaceholderAPIHook();
		// Add hooks to set
		hooks.add(protocollibHook);
		hooks.add(placeholderapiHook);
		hooks.add(luckpermsHook);
		hooks.add(dynmapHook);
		hooks.add(discordsrvHook);
		hooks.add(quickshopHook);

	}
	
	public void initialize() {
		// Reload any disabled hooks, display status
		ChatUtil.printConsoleAlert("Integrated Plugin Status...");
		ArrayList<Integer> statusList = new ArrayList<>();
		for (PluginHook hook : hooks) {
			int status = 0; // Active
			if (!hook.isEnabled()) {
				status = hook.reload(); // Update status from reload attempt
			}
			statusList.add(status);
		}
		// Display status at end
		for(int i = 0; i < hooks.size(); i++) {
			PluginHook hook = hooks.get(i);
			int code = statusList.get(i);
			String hookStatus = String.format(ChatColor.GOLD+"> "+ChatColor.RESET+"%-16s -> %s",hook.getPluginName(),getStatus(code));
			Bukkit.getServer().getConsoleSender().sendMessage(hookStatus);
		}
	}
	
	public void disable() {
		// Shutdown all hooks
		for (PluginHook hook : hooks) {
			hook.shutdown();
		}
		ChatUtil.printDebug("Integration Manager is disabled");
	}

	private String getStatus(int code) {
		String result = "";
		switch(code) {
			case 0:
				result = ChatUtil.parseHex("#60C030")+"Active"; // Green
				break;
			case 1:
				result = ChatUtil.parseHex("#5080B0")+"Missing JAR"; // Light Blue
				break;
			case 2:
				result = ChatUtil.parseHex("#B040C0")+"Disabled"; // Light Purple
				break;
			case 3:
				result = ChatUtil.parseHex("#808080")+"Inactive"; // Dark Gray
				break;
			default:
				result = ChatUtil.parseHex("#FF2020")+"Failed"; // Red
				break;
		}
		return result;
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

	public DynmapHook getDynmap() {
		return dynmapHook;
	}

	public ProtocolLibHook getProtocolLib() {
		return protocollibHook;
	}

	public PlaceholderAPIHook getPlaceholderAPI() {
		return placeholderapiHook;
	}
	
}
