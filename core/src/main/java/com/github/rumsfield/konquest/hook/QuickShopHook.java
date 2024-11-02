package com.github.rumsfield.konquest.hook;

import com.ghostchu.quickshop.api.QuickShopProvider;
import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.listener.QuickShopListener;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import org.bukkit.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.Nullable;
import com.ghostchu.quickshop.api.QuickShopAPI;

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
		return "QuickShop-Hikari";
	}

	@Override
	public int reload() {
		isEnabled = false;
		// Check for older QuickShop
		if (Bukkit.getPluginManager().isPluginEnabled("QuickShop")) {
			ChatUtil.printConsoleError("Unsupported QuickShop plugin found, install QuickShop-Hikari plugin instead");
		}
		// Attempt to integrate QuickShop
		Plugin quickShop = Bukkit.getPluginManager().getPlugin("QuickShop-Hikari");
		if(quickShop == null) {
			return 1;
		}
		if(!quickShop.isEnabled()) {
			return 2;
		}
		if(!konquest.getCore().getBoolean(CorePath.INTEGRATION_QUICKSHOP.getPath(),false)) {
			return 3;
		}
		RegisteredServiceProvider<QuickShopProvider> provider = Bukkit.getServicesManager().getRegistration(QuickShopProvider.class);
		if(provider == null){
			ChatUtil.printConsoleError("Failed to integrate QuickShop-Hikari, plugin not found or disabled");
			return -1;
		}
		quickShopAPI = provider.getProvider().getApiInstance();
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
