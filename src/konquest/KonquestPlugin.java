package konquest;

import konquest.listener.BlockListener;
import konquest.listener.EntityListener;
import konquest.listener.HangingListener;
import konquest.listener.InventoryListener;
import konquest.listener.KonquestListener;
import konquest.listener.PlayerListener;
import konquest.listener.WorldListener;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;
import konquest.utility.Metrics;
import konquest.utility.Updater;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

public class KonquestPlugin extends JavaPlugin {
	
	private Konquest konquest;
	private PluginManager pluginManager;
	private boolean isProtocolEnabled = false;
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
 		// Enable metrics
 		loadMetrics();
        // Enable protocol
 		setupProtocol();
 		// Register command executors & listeners
        getCommand("konquest").setExecutor(konquest.getCommandHandler());
        getCommand("k").setExecutor(konquest.getCommandHandler());
        registerListeners();
        // Initialize core
        konquest.initialize();
        // Register placeholders
        registerPlaceholders();
        // Check for updates
        checkForUpdates();
        // Done!
        enableSuccess = true;
        ChatUtil.printConsoleAlert("Plugin enabled. Written by Rumsfield.");
	}
	
	@Override
	public void onDisable() {
		ChatUtil.printDebug("Executing onDisable...");
		if(enableSuccess) {
			konquest.getKingdomManager().saveKingdoms();
			konquest.getCampManager().saveCamps();
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
	
	public boolean isProtocolEnabled() {
		return isProtocolEnabled;
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
	
	private void loadMetrics() {
		try {
	        new Metrics(this, 11980);
		} catch(Exception e) {
			ChatUtil.printConsoleError("Failed to load plugin metrics with bStats:");
			ChatUtil.printConsoleError(e.getMessage());
		}
	}
	
	private void setupProtocol() {
		plib = null;
		try {
			plib = ProtocolLibrary.getProtocolManager();
			isProtocolEnabled = true;
		} catch(Exception e) {
			ChatUtil.printConsoleError("Failed to load ProtocolLib, is it the latest version?");
			ChatUtil.printConsoleError(e.getMessage());
		}
	}
	
	private void checkForUpdates() {
		new Updater(this, 92220).getVersion(version -> {
            if (this.getDescription().getVersion().equalsIgnoreCase(version)) {
            	ChatUtil.printConsoleAlert("Konquest version is up to date.");
            } else {
            	String message = "Konquest version "+version+" is available to download! --> https://www.spigotmc.org/resources/konquest.92220/";
                ChatUtil.printConsoleError(message);
                konquest.opStatusMessages.add(message);
            }
        });
	}
	
	private void registerPlaceholders() {
		if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
			new KonquestPlaceholderExpansion(this).register();
			ChatUtil.printConsoleAlert("Successfully registered Placeholders.");
		}
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
			ChatUtil.printDebug("Failed to get balance using Player: "+e.getMessage());
			try {
				result = econ.getBalance(offlineBukkitPlayer.getName());
			} catch(Exception x) {
				ChatUtil.printDebug("Failed to get balance using Name: "+x.getMessage());
				result = econ.getBalance(formatStringForAConomyPlugin(offlineBukkitPlayer));
			}
		}
		return result;
	}
	
	@SuppressWarnings("deprecation")
	public static boolean withdrawPlayer(Player bukkitPlayer, double amount) {
		boolean result = false;
		// Check for discounts
		// Look for discount permissions, for biggest discount
		int discount = 0;
		for(PermissionAttachmentInfo p : bukkitPlayer.getEffectivePermissions()) {
			String perm = p.getPermission();
			if(perm.contains("konquest.discount")) {
				String valStr = perm.split("\\.",3)[2];
				//ChatUtil.printDebug("Withdraw discount found: "+valStr);
				int valNum = 0;
				try {
        			valNum = Integer.parseInt(valStr);
        		} catch(NumberFormatException e) {
        			ChatUtil.printDebug("Failed to parse discount value");
        		}
				if(valNum > discount) {
					discount = valNum;
				}
			}
		}
		// Apply discount
		double amountMod = amount;
		if(discount > 0 && discount <= 100) {
			//ChatUtil.printDebug("Applying discount of "+discount+"%");
			double amountOff = amount * ((double)discount / 100);
			amountMod = amount - amountOff;
			//String amountF = String.format("%.2f",amountOff);
			String amountF = econ.format(amountOff);
			ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_DISCOUNT_FAVOR.getMessage(discount,amountF));
		} else if(discount != 0) {
			ChatUtil.printDebug("Failed to apply invalid discount of "+discount+"%");
		}
		// Perform transaction
		EconomyResponse resp = null;
		try {
			resp = econ.withdrawPlayer(bukkitPlayer, amountMod);
		} catch(Exception e) {
			ChatUtil.printDebug("Failed to withdraw using Player: "+e.getMessage());
			try {
				resp = econ.withdrawPlayer(bukkitPlayer.getName(), amountMod);
			} catch(Exception x) {
				ChatUtil.printDebug("Failed to withdraw using Name: "+x.getMessage());
				resp = econ.withdrawPlayer(formatStringForAConomyPlugin(bukkitPlayer), amountMod);
			}
		}
		// Send message
		if(resp != null) {
			if(resp.transactionSuccess()) {
	        	//String balanceF = String.format("%.2f",r.balance);
	        	//String amountF = String.format("%.2f",r.amount);
				String balanceF = econ.format(resp.balance);
				String amountF = econ.format(resp.amount);
	        	ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_REDUCE_FAVOR.getMessage(amountF,balanceF));
	        	result = true;
	        } else {
	        	ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL_MESSAGE.getMessage(resp.errorMessage));
	        }
		} else {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
		}
		return result;
	}
	
	@SuppressWarnings("deprecation")
	public static EconomyResponse depositPlayer(OfflinePlayer offlineBukkitPlayer, double amount) {
		EconomyResponse result;
		try {
			result = econ.depositPlayer(offlineBukkitPlayer, amount);
		} catch(Exception e) {
			ChatUtil.printDebug("Failed to deposit using Player: "+e.getMessage());
			try {
				result = econ.depositPlayer(offlineBukkitPlayer.getName(), amount);
			} catch(Exception x) {
				ChatUtil.printDebug("Failed to deposit using Name: "+x.getMessage());
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
