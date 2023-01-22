package com.github.rumsfield.konquest.hook;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.listener.QuickShopListener;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.Version;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
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
	}
	
	@Override
	public void reload() {
		// Attempt to integrate QuickShop
		quickShopAPI = (QuickShopAPI) Bukkit.getPluginManager().getPlugin("QuickShop");
		if(quickShopAPI == null){
			ChatUtil.printConsoleAlert("Could not integrate QuickShop, missing or disabled.");
			return;
		}
		final JavaPlugin plugin = (JavaPlugin) quickShopAPI;
		if (!plugin.isEnabled()){
			ChatUtil.printConsoleAlert("Could not integrate QuickShop, disabled.");
			return;
		}
		if(!konquest.getCore().getBoolean(CorePath.INTEGRATION_QUICKSHOP.getPath(),false)) {
			ChatUtil.printConsoleAlert("Disabled QuickShop integration from core config settings.");
			return;
		}

		// Verify version requirement
		String ver = plugin.getDescription().getVersion();
		String reqMin = "4.0.9.4";
		String reqMax = "4.0.9.10";
		boolean isAboveMinVersion = false;
		boolean isBelowMaxVersion = false;
		try {
			Version installedVersion = new Version(ver);
			Version minimumVersion = new Version(reqMin);
			Version maximumVersion = new Version(reqMax);
			isAboveMinVersion = (installedVersion.compareTo(minimumVersion) >= 0);
			isBelowMaxVersion = (installedVersion.compareTo(maximumVersion) <= 0);
		} catch(IllegalArgumentException e) {
			e.printStackTrace();
		}

		if(!isAboveMinVersion){
			ChatUtil.printConsoleError("Failed to integrate QuickShop, plugin version "+ver+" is too old. You must update it to at least version "+reqMin);
			return;
		}
		if(!isBelowMaxVersion){
			ChatUtil.printConsoleError("Failed to integrate QuickShop, plugin version "+ver+" is too new. You must revert it to at most version "+reqMax);
			return;
		}

		isEnabled = true;
		konquest.getPlugin().getServer().getPluginManager().registerEvents(new QuickShopListener(konquest.getPlugin()), konquest.getPlugin());
		ChatUtil.printConsoleAlert("Successfully integrated QuickShop version " + ver);

	}
	
	@Override
	public void shutdown() {
		// Do Nothing
	}

	@Override
	public boolean isEnabled() {
		return isEnabled;
	}
	
	public void deleteShopsInPoints(Collection<Point> points, World world) {
		if(isEnabled && !points.isEmpty()) {
			for(Point point : points) {
				Chunk chunk = Konquest.toChunk(point,world);
				Map<Location,Shop> shopList = quickShopAPI.getShopManager().getShops(chunk);
				if(shopList != null) {
					for(Map.Entry<Location, Shop> entry : shopList.entrySet()) {
						Location shopLoc = entry.getKey();
						final OfflinePlayer owner = Bukkit.getOfflinePlayer(entry.getValue().getOwner());
						world.playEffect(shopLoc, Effect.IRON_TRAPDOOR_TOGGLE, null);
						ChatUtil.printDebug("Deleting shop owned by "+owner.getName());
						entry.getValue().delete();
					}
				}
			}
		}
	}

}
