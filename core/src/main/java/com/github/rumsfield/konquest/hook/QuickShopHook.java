package com.github.rumsfield.konquest.hook;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.listener.QuickShopListener;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.Version;
import org.bukkit.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.dynmap.DynmapAPI;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.api.QuickShopAPI;
import org.maxgamer.quickshop.api.shop.Shop;

import java.awt.*;
import java.util.Collection;
import java.util.Map;

public class QuickShopHook implements PluginHook {

	private final Konquest konquest;
	private QuickShopAPI quickShopAPI;
	private boolean isEnabled;
	
	public QuickShopHook(Konquest konquest) {
		this.konquest = konquest;
		this.isEnabled = false;
		this.quickShopAPI = null;
	}

	@Override
	public String getPluginName() {
		return "QuickShop";
	}

	@Override
	public int reload() {
		// Attempt to integrate QuickShop
		Plugin quickShop = Bukkit.getPluginManager().getPlugin("QuickShop");
		if(quickShop == null) {
			return 1;
		}
		if(!quickShop.isEnabled()) {
			return 2;
		}
		if(!konquest.getCore().getBoolean(CorePath.INTEGRATION_QUICKSHOP.getPath(),false)) {
			return 3;
		}

		quickShopAPI = (QuickShopAPI) quickShop;
		isEnabled = true;
		konquest.getPlugin().getServer().getPluginManager().registerEvents(new QuickShopListener(konquest.getPlugin()), konquest.getPlugin());
		return 0;
	}
	
	@Override
	public void shutdown() {
		// Do Nothing
	}

	@Override
	public boolean isEnabled() {
		return isEnabled;
	}

	@Nullable
	public QuickShopAPI getAPI() {
		return quickShopAPI;
	}

}
