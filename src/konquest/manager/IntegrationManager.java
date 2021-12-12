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
		if(konquest.getConfigManager().getConfig("core").getBoolean("core.integration.quickshop",false)) {
			Plugin quickShop = Bukkit.getPluginManager().getPlugin("QuickShop");
            if (quickShop != null && quickShop.isEnabled()) {
            	// Verify version requirement
            	String ver = quickShop.getDescription().getVersion();
            	String reqMin = "4.0.9.4";
            	String reqMax = "4.0.9.10";
            	Version installedVersion = new Version(ver);
            	Version minimumVersion = new Version(reqMin);
            	Version maximumVersion = new Version(reqMax);
            	boolean isMinVersion = (installedVersion.compareTo(minimumVersion) >= 0);
            	boolean isMaxVersion = (installedVersion.compareTo(maximumVersion) <= 0);
            	if(isMinVersion && isMaxVersion) {
            		isQuickShopEnabled = true;
                	konquest.getPlugin().getServer().getPluginManager().registerEvents(new QuickShopListener(konquest.getPlugin()), konquest.getPlugin());
                	ChatUtil.printConsoleAlert("Successfully integrated QuickShop version "+ver);
            	} else {
            		if(isMinVersion) {
            			ChatUtil.printConsoleError("Failed to integrate QuickShop, plugin version "+ver+" is too old. You must update it to at least version "+reqMin);
            		} else if(isMaxVersion) {
            			ChatUtil.printConsoleError("Failed to integrate QuickShop, plugin version "+ver+" is too new. You must update it to at most version "+reqMax);
            		} else {
            			ChatUtil.printConsoleError("Failed to integrate QuickShop, plugin version "+ver+" is unknown. You must use a version between "+reqMin+" and "+reqMax);
            		}
            	}
            } else {
            	ChatUtil.printConsoleError("Failed to integrate QuickShop, plugin not found or disabled");
            }
		} else {
			ChatUtil.printDebug("Skipping QuickShop integration from config settings");
		}
		
		// Attempt to integrate Luckperms
		if(konquest.getConfigManager().getConfig("core").getBoolean("core.integration.luckperms",false)) {
			Plugin luckPerms = Bukkit.getPluginManager().getPlugin("LuckPerms");
			if (luckPerms != null && luckPerms.isEnabled()) {
				RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
				if (provider != null) {
					lpAPI = provider.getProvider();
					isLuckPermsEnabled = true;
					ChatUtil.printConsoleAlert("Successfully integrated LuckPerms");
				} else {
					ChatUtil.printConsoleError("Failed to integrate LuckPerms, plugin not found or disabled");
				}
			} else {
				ChatUtil.printConsoleError("Failed to integrate LuckPerms, plugin not found or disabled");
			}
		} else {
			ChatUtil.printDebug("Skipping LuckPerms integration from config settings");
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
	
	/*
	public void integrateQuickShopPlugin() {
		QuickShopAPI.getIntegrationManager().register(new IntegratedPlugin() {
			@Override
			public boolean canCreateShopHere(Player bukkitPlayer, Location location) {
				// Bypass all checks for admins in bypass mode
				KonPlayer player = konquest.getPlayerManager().getPlayer(bukkitPlayer);
				if(!player.isAdminBypassActive()) {
					if(konquest.getKingdomManager().isChunkClaimed(location.getChunk())) {
						//ChatUtil.printDebug("...checking claimed chunk");
						KonTerritory territory = konquest.getKingdomManager().getChunkTerritory(location.getChunk());
						if(territory instanceof KonTown && !player.getKingdom().equals(territory.getKingdom())) {
							//ChatUtil.printDebug("...chunk is enemy town");
							ChatUtil.sendError(bukkitPlayer, "Cannot create a shop in enemy towns!");
							return false;
						} else if(territory instanceof KonCamp) {
							KonCamp camp = (KonCamp)territory;
							//ChatUtil.printDebug("...chunk is camp");
							if(!camp.isPlayerOwner(bukkitPlayer)) {
								//ChatUtil.printDebug("...player is not camp owner");
								ChatUtil.sendError(bukkitPlayer, "Can only create shops in your own camp!");
								return false;
							} else {
								//ChatUtil.printDebug("...player is camp owner, permit");
							}
						} else if(territory instanceof KonCapital) {
							//ChatUtil.printDebug("...chunk is capital");
							ChatUtil.sendError(bukkitPlayer, "Cannot create a shop here!");
							return false;
						} else {
							//ChatUtil.printDebug("...chunk is unknown, permit");
						}
					} else {
						//ChatUtil.printDebug("...chunk is not claimed, block");
						ChatUtil.sendError(bukkitPlayer, "Cannot create shops in the wild!");
						return false;
					}
				}
				return true;
			}

			@Override
			public boolean canTradeShopHere(Player bukkitPlayer, Location location) {
				// Bypass all checks for admins in bypass mode
				KonPlayer player = konquest.getPlayerManager().getPlayer(bukkitPlayer);
				if(!player.isAdminBypassActive()) {
					if(konquest.getKingdomManager().isChunkClaimed(location.getChunk())) {
						//ChatUtil.printDebug("...checking claimed chunk");
						KonTerritory territory = konquest.getKingdomManager().getChunkTerritory(location.getChunk());
						if(!player.getKingdom().equals(territory.getKingdom())) {
							ChatUtil.sendError(bukkitPlayer, "Cannot trade with enemy shops!");
							return false;
						}
					}
				}
				return true;
			}

			@Override
			public String getName() {
				return "Konquest";
			}

			@Override
			public void load() {
				// TODO Auto-generated method stub
			}

			@Override
			public void unload() {
				// TODO Auto-generated method stub
			}
		});
	}
	*/
	
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
