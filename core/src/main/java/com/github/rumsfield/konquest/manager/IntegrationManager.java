package com.github.rumsfield.konquest.manager;


import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.hook.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.ArrayList;


public class IntegrationManager {

	private final ArrayList<PluginHook> hooks;
	private final LuckPermsHook luckpermsHook;
	private final ChestShopHook chestshopHook;
	private final QuickShopHook quickshopHook;
	private final DiscordSrvHook discordsrvHook;
	private final DynmapHook dynmapHook;
	private final BlueMapHook bluemapHook;
	private final SquaremapHook squaremapHook;
	private final WorldGuardHook worldguardHook;
	private final ProtocolLibHook protocollibHook;
	private final PlaceholderAPIHook placeholderapiHook;
	private final EssentialsXHook essentialsXHook;
	
	public IntegrationManager(Konquest konquest) {
		this.hooks = new ArrayList<>();
		// Define new hooks
		luckpermsHook = new LuckPermsHook(konquest);
		chestshopHook = new ChestShopHook(konquest);
		quickshopHook = new QuickShopHook(konquest);
		discordsrvHook = new DiscordSrvHook(konquest);
		dynmapHook = new DynmapHook(konquest);
		bluemapHook = new BlueMapHook(konquest);
		squaremapHook = new SquaremapHook(konquest);
		worldguardHook = new WorldGuardHook(konquest);
		protocollibHook = new ProtocolLibHook();
		placeholderapiHook = new PlaceholderAPIHook();
		essentialsXHook = new EssentialsXHook(konquest);
		// Add hooks to set
		hooks.add(protocollibHook);
		hooks.add(placeholderapiHook);
		hooks.add(essentialsXHook);
		hooks.add(luckpermsHook);
		hooks.add(dynmapHook);
		hooks.add(bluemapHook);
		hooks.add(squaremapHook);
		hooks.add(worldguardHook);
		hooks.add(discordsrvHook);
		hooks.add(quickshopHook);
		hooks.add(chestshopHook);

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
			String hookStatus = String.format(ChatColor.GOLD+"> "+ChatColor.RESET+"%-30s -> %s",hook.getPluginName(),getStatus(code));
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
				result = ChatColor.DARK_GREEN+"Active";
				break;
			case 1:
				result = ChatColor.DARK_AQUA+"Not Loaded";
				break;
			case 2:
				result = ChatColor.LIGHT_PURPLE+"Disabled";
				break;
			case 3:
				result = ChatColor.GRAY+"Inactive";
				break;
			case 4:
				result = ChatColor.RED+"Unsupported Plugin";
				break;
			default:
				result = ChatColor.DARK_RED+"Failed";
				break;
		}
		return result;
	}
	
	public LuckPermsHook getLuckPerms() {
		return luckpermsHook;
	}

	public ChestShopHook getChestShop() {
		return chestshopHook;
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

	public BlueMapHook getBlueMap() {
		return bluemapHook;
	}

	public SquaremapHook getSquaremap() {
		return squaremapHook;
	}

	public WorldGuardHook getWorldGuard() {
		return worldguardHook;
	}

	public ProtocolLibHook getProtocolLib() {
		return protocollibHook;
	}

	public PlaceholderAPIHook getPlaceholderAPI() {
		return placeholderapiHook;
	}

	public EssentialsXHook getEssentialsXAPI() {
		return essentialsXHook;
	}

}
