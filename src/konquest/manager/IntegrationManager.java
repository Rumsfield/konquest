package konquest.manager;


import java.awt.Point;
import java.util.Collection;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
//import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.maxgamer.quickshop.api.QuickShopAPI;
//import org.maxgamer.quickshop.integration.IntegratedPlugin;
import org.maxgamer.quickshop.shop.Shop;

import konquest.Konquest;
import konquest.listener.QuickShopListener;
//import konquest.model.KonCamp;
//import konquest.model.KonCapital;
//import konquest.model.KonPlayer;
//import konquest.model.KonTerritory;
//import konquest.model.KonTown;
import konquest.utility.ChatUtil;
import konquest.utility.Version;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;

public class IntegrationManager {

	private Konquest konquest;
	private boolean isQuickShopEnabled;
	private LuckPerms lpAPI;
	private boolean isLuckPermsEnabled;
	
	public IntegrationManager(Konquest konquest) {
		this.konquest = konquest;
		this.isQuickShopEnabled = false;
		this.lpAPI = null;
		this.isLuckPermsEnabled = false;
	}
	
	
	/**
	 * Integration summary:
	 * QuickShop - Use ShopPreCreateEvent to monitor shop creation, only allow shops in friendly towns and owner's camps
	 * 			 - Use QuickShopAPI to delete all shops in a town on conquer or removal
	 * 
	 */
	public void initialize() {
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
	        		isQuickShopEnabled = true;
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
		
		// Attempt to integrate Luckperms
		Plugin luckPerms = Bukkit.getPluginManager().getPlugin("LuckPerms");
		if (luckPerms != null && luckPerms.isEnabled()) {
			if(konquest.getConfigManager().getConfig("core").getBoolean("core.integration.luckperms",false)) {
				RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
				if (provider != null) {
					lpAPI = provider.getProvider();
					isLuckPermsEnabled = true;
					ChatUtil.printConsoleAlert("Successfully integrated LuckPerms");
				} else {
					ChatUtil.printConsoleError("Failed to integrate LuckPerms, plugin not found or disabled");
				}
			} else {
				ChatUtil.printConsoleAlert("Disabled LuckPerms integration from core config settings.");
			}
		} else {
			ChatUtil.printConsoleAlert("Could not integrate LuckPerms, missing or disabled.");
		}
		ChatUtil.printDebug("Integration Manager is ready");
	}
	
	public boolean isQuickShopEnabled() {
		return isQuickShopEnabled;
	}
	
	public boolean isLuckPermsEnabled() {
		return isLuckPermsEnabled;
	}
	
	// this can return null!
	public LuckPerms getLuckPermsAPI() {
		return lpAPI;
	}
	
	public String getLuckPermsPrefix(Player player) {
		String prefix = "";
		if(isLuckPermsEnabled) {
			User user = lpAPI.getUserManager().getUser(player.getUniqueId());
			QueryOptions queryOptions = lpAPI.getContextManager().getQueryOptions(player);
	        CachedMetaData metaData = user.getCachedData().getMetaData(queryOptions);
	        if (metaData.getPrefix() != null) {
	        	prefix = metaData.getPrefix().equalsIgnoreCase("null") ? "" : metaData.getPrefix();
	        }
		}
		return prefix;
	}
	
	public String getLuckPermsSuffix(Player player) {
		String suffix = "";
		if(isLuckPermsEnabled) {
			User user = lpAPI.getUserManager().getUser(player.getUniqueId());
			QueryOptions queryOptions = lpAPI.getContextManager().getQueryOptions(player);
	        CachedMetaData metaData = user.getCachedData().getMetaData(queryOptions);
	        if (metaData.getSuffix() != null) {
	        	suffix = metaData.getSuffix().equalsIgnoreCase("null") ? "" : metaData.getSuffix();
	        }
		}
		return suffix;
	}
	
	public void deleteShopsInPoints(Collection<Point> points, World world) {
		if(isQuickShopEnabled && !points.isEmpty()) {
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
