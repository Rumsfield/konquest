package konquest;

import konquest.listener.BlockListener;
import konquest.listener.EntityListener;
import konquest.listener.HangingListener;
import konquest.listener.InventoryListener;
import konquest.listener.KonquestListener;
import konquest.listener.PlayerListener;
import konquest.listener.WorldListener;
import konquest.utility.ChatUtil;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
//import org.maxgamer.quickshop.api.QuickShopAPI;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

public class KonquestPlugin extends JavaPlugin {
	
	private Konquest konquest;
	private PluginManager pluginManager;
	private static Economy econ = null;
	private boolean enableSuccess = false;
	private static ProtocolManager plib = null;

	@Override
	public void onEnable() {
		pluginManager = getServer().getPluginManager();
		konquest = new Konquest(this);
        // Check for Vault & Economy
 		if (!setupEconomy()) {
 			getLogger().severe(String.format("%s disabled due to bad or missing economy plugin.", getDescription().getName()));
 			pluginManager.disablePlugin(this);
            return;
        }
 		plib = ProtocolLibrary.getProtocolManager();
        getCommand("konquest").setExecutor(konquest.getCommandHandler());
        getCommand("k").setExecutor(konquest.getCommandHandler());
        registerListeners();
        konquest.initialize();
        enableSuccess = true;
        ChatUtil.printConsoleAlert("Plugin enabled. Written by Rumsfield.");
	}
	
	@Override
	public void onDisable() {
		ChatUtil.printDebug("Executing onDisable...");
		if(enableSuccess) {
			konquest.getKingdomManager().saveKingdoms();
			konquest.getKingdomManager().saveCamps();
			konquest.getRuinManager().saveRuins();
			konquest.getRuinManager().removeAllGolems();
			konquest.getConfigManager().saveConfigs();
			konquest.getDatabaseThread().flushDatabase();
			konquest.getDatabaseThread().getDatabase().getDatabaseConnection().disconnect();
			ChatUtil.printDebug("Finished onDisable");
		}
	}
	
	public Konquest getKonquestInstance() {
		return konquest;
	}
	
	public static ProtocolManager getProtocolManager() {
		return plib;
	}
	
	private void registerListeners() {
		pluginManager.registerEvents(new PlayerListener(this), this);
		pluginManager.registerEvents(new KonquestListener(this), this);
		pluginManager.registerEvents(new EntityListener(this), this);
		pluginManager.registerEvents(new BlockListener(this), this);
		pluginManager.registerEvents(new InventoryListener(this), this);
		pluginManager.registerEvents(new HangingListener(this), this);
		pluginManager.registerEvents(new WorldListener(this), this);
		//pluginManager.registerEvents(new QuickShopListener(this), this);
	}
	
	private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
        	ChatUtil.printConsoleError("No Vault dependency found! Include the Vault plugin.");
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
        	ChatUtil.printConsoleError("No economy service found! Include an economy plugin, like EssentialsX.");
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }
	
	public static Economy getEconomy() {
        return econ;
    }
	
	/*
	 * Economy wrapper functions. Attempt to use Vault API methods, and then try depreciated methods if those fail.
	 */
	
	@SuppressWarnings("deprecation")
	public static double getBalance(OfflinePlayer offlineBukkitPlayer) {
		double result = 0;
		try {
			result = econ.getBalance(offlineBukkitPlayer);
		} catch(Exception e) {
			try {
				result = econ.getBalance(offlineBukkitPlayer.getName());
			} catch(Exception x) {
				result = econ.getBalance(formatStringForAConomyPlugin(offlineBukkitPlayer));
			}
		}
		return result;
	}
	
	@SuppressWarnings("deprecation")
	public static EconomyResponse withdrawPlayer(OfflinePlayer offlineBukkitPlayer, double amount) {
		EconomyResponse result;
		try {
			result = econ.withdrawPlayer(offlineBukkitPlayer, amount);
		} catch(Exception e) {
			try {
				result = econ.withdrawPlayer(offlineBukkitPlayer.getName(), amount);
			} catch(Exception x) {
				result = econ.withdrawPlayer(formatStringForAConomyPlugin(offlineBukkitPlayer), amount);
			}
		}
		return result;
	}
	
	@SuppressWarnings("deprecation")
	public static EconomyResponse depositPlayer(OfflinePlayer offlineBukkitPlayer, double amount) {
		EconomyResponse result;
		try {
			result = econ.depositPlayer(offlineBukkitPlayer, amount);
		} catch(Exception e) {
			try {
				result = econ.depositPlayer(offlineBukkitPlayer.getName(), amount);
			} catch(Exception x) {
				result = econ.depositPlayer(formatStringForAConomyPlugin(offlineBukkitPlayer), amount);
			}
		}
		return result;
	}
	
	/**
	 * This method is stupid, and is specific to unique way that AConomy implements its Vault API methods.
	 * @param offlineBukkitPlayer
	 * @return String of player's UUID when server is online-mode=true, else player's lowercase name.
	 */
	private static String formatStringForAConomyPlugin(OfflinePlayer offlineBukkitPlayer) {
		String playerString = "";
		if(Bukkit.getServer().getOnlineMode()) {
			playerString = offlineBukkitPlayer.getUniqueId().toString();
		} else {
			playerString = offlineBukkitPlayer.getName().toLowerCase();
		}
		return playerString;
	}
}
