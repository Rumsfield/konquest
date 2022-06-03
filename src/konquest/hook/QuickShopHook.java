package konquest.hook;

import java.awt.Point;
import java.util.Collection;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.maxgamer.quickshop.api.QuickShopAPI;
import org.maxgamer.quickshop.shop.Shop;

import konquest.Konquest;
import konquest.listener.QuickShopListener;
import konquest.utility.ChatUtil;
import konquest.utility.Version;

public class QuickShopHook implements PluginHook {

	private Konquest konquest;
	private boolean isEnabled;
	
	public QuickShopHook(Konquest konquest) {
		this.konquest = konquest;
		this.isEnabled = false;
	}
	
	@Override
	public void reload() {
		// Attempt to integrate QuickShop
		Plugin quickShop = Bukkit.getPluginManager().getPlugin("QuickShop");
        if (quickShop != null && quickShop.isEnabled()) {
        	if(konquest.getConfigManager().getConfig("core").getBoolean("core.integration.quickshop",false)) {
	        	// Verify version requirement
	        	String ver = quickShop.getDescription().getVersion();
	        	String reqMin = "4.0.9.4";
	        	String reqMax = "4.0.9.10";
	        	boolean isMinVersion = false;
	        	boolean isMaxVersion = false;
	        	try {
	        		Version installedVersion = new Version(ver);
	        		Version minimumVersion = new Version(reqMin);
	            	Version maximumVersion = new Version(reqMax);
	            	isMinVersion = (installedVersion.compareTo(minimumVersion) >= 0);
	            	isMaxVersion = (installedVersion.compareTo(maximumVersion) <= 0);
	        	} catch(IllegalArgumentException e) {
	        		e.printStackTrace();
	        	}
	        	if(isMinVersion && isMaxVersion) {
	        		isEnabled = true;
	            	konquest.getPlugin().getServer().getPluginManager().registerEvents(new QuickShopListener(konquest.getPlugin()), konquest.getPlugin());
	            	ChatUtil.printConsoleAlert("Successfully integrated QuickShop version "+ver);
	        	} else {
	        		if(isMinVersion) {
	        			ChatUtil.printConsoleError("Failed to integrate QuickShop, plugin version "+ver+" is too new. You must revert it to at most version "+reqMax);
	        		} else if(isMaxVersion) {
	        			ChatUtil.printConsoleError("Failed to integrate QuickShop, plugin version "+ver+" is too old. You must update it to at least version "+reqMin);
	        		} else {
	        			ChatUtil.printConsoleError("Failed to integrate QuickShop, plugin version "+ver+" is unknown. You must use a version between "+reqMin+" and "+reqMax);
	        		}
	        	}
        	} else {
        		ChatUtil.printConsoleAlert("Disabled QuickShop integration from core config settings.");
        	}
        } else {
        	ChatUtil.printConsoleAlert("Could not integrate QuickShop, missing or disabled.");
        }
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
				List<Shop> shopList = QuickShopAPI.getShopAPI().getShops(chunk);
				if(shopList != null) {
					for(Shop shop : shopList) {
						Location shopLoc = shop.getLocation();
						world.playEffect(shopLoc, Effect.IRON_TRAPDOOR_TOGGLE, null);
						ChatUtil.printDebug("Deleting shop owned by "+Bukkit.getOfflinePlayer(shop.getOwner()).getName());
						shop.delete();
					}
				}
			}
		}
	}

}
