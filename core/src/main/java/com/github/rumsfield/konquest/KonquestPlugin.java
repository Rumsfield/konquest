package com.github.rumsfield.konquest;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.listener.*;
import com.github.rumsfield.konquest.utility.*;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public class KonquestPlugin extends JavaPlugin {
	
	private Konquest konquest;
	private PluginManager pluginManager;
	private static Economy econ = null;
	private boolean enableSuccess = false;

	boolean isSetupEconomy = false;
	boolean isSetupMetrics = false;
	boolean isSetupPlaceholders = false;

	@Override
	public void onEnable() {
		pluginManager = getServer().getPluginManager();
		konquest = new Konquest(this);
		// Display logo in console
		printLogo();
        // Check for Vault & Economy
		isSetupEconomy = setupEconomy();
 		if (!isSetupEconomy) {
 			getLogger().severe(String.format("%s disabled due to bad or missing economy plugin.", getDescription().getName()));
 			pluginManager.disablePlugin(this);
            return;
        }
 		// Enable metrics
 		isSetupMetrics = loadMetrics();
 		// Register command executors & listeners
        registerListeners();
        // Initialize core
        konquest.initialize();
        // Register API
        registerApi(konquest);
        // Register placeholders
		isSetupPlaceholders = registerPlaceholders();
        // Check for updates
        checkForUpdates();
        // Done!
        enableSuccess = true;
		// Display status in console
		printEnableStatus();

		ChatUtil.printConsoleAlert("Successfully enabled");
	}
	
	@Override
	public void onDisable() {
		if(enableSuccess) {
			konquest.disable();
			konquest.getSanctuaryManager().saveSanctuaries();
			konquest.getKingdomManager().saveKingdoms();
			konquest.getCampManager().saveCamps();
			konquest.getRuinManager().saveRuins();
			konquest.getRuinManager().removeAllGolems();
			konquest.getKingdomManager().removeAllRabbits();
			konquest.getConfigManager().saveConfigs();
			konquest.getDatabaseThread().flushDatabase();
			konquest.getDatabaseThread().getDatabase().getDatabaseConnection().disconnect();
		}
	}
	
	public Konquest getKonquestInstance() {
		return konquest;
	}

	private void registerListeners() {
		// Set command executors
		getCommand("konquest").setExecutor(konquest.getCommandHandler());
		getCommand("k").setExecutor(konquest.getCommandHandler());
		// Register event listeners
		pluginManager.registerEvents(new PlayerListener(this), this);
		pluginManager.registerEvents(new EntityListener(this), this);
		pluginManager.registerEvents(new BlockListener(this), this);
		pluginManager.registerEvents(new InventoryListener(this), this);
		pluginManager.registerEvents(new HangingListener(this), this);
		pluginManager.registerEvents(new WorldListener(this), this);
	}
	
	private void registerApi(KonquestAPI api) {
		this.getServer().getServicesManager().register(KonquestAPI.class, api, this, ServicePriority.Normal);
	}
	
	private boolean loadMetrics() {
		try {
			return new Metrics(this, 11980).isEnabled();
		} catch(Exception e) {
			ChatUtil.printConsoleError("Failed to load plugin metrics with bStats:");
			ChatUtil.printConsoleError(e.getMessage());
		}
		return false;
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
	
	private boolean registerPlaceholders() {
		if (konquest.getIntegrationManager().getPlaceholderAPI().isEnabled()) {
			new KonquestPlaceholderExpansion(this).register();
			return true;
		}
		return false;
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
        return true;
    }

	private void printLogo() {
//		String color1 = ChatUtil.parseHex("#FFA000");
//		String color2 = ChatUtil.parseHex("#FFB020");
//		String color3 = ChatUtil.parseHex("#FFC040");
//		String color4 = ChatUtil.parseHex("#FFD060");
//		String color5 = ChatUtil.parseHex("#FFE080");
//		String color6 = ChatUtil.parseHex("#FFF0A0");
		String [] logo = {
				ChatColor.GOLD+" _  __                                 _   ",
				ChatColor.GOLD+"| |/ /___  _ __   __ _ _   _  ___  ___| |_ ",
				ChatColor.GOLD+"| ' // _ \\| '_ \\ / _` | | | |/ _ \\/ __| __|",
				ChatColor.GOLD+"| . \\ (_) | | | | (_| | |_| |  __/\\__ \\ |_ ",
				ChatColor.GOLD+"|_|\\_\\___/|_| |_|\\__, |\\__,_|\\___||___/\\__|",
				ChatColor.GOLD+"                    |_|                    ",
				ChatColor.GOLD+"========== Konquest by Rumsfield =========="
		};
		for (String row : logo) {
			String line = "    " + row;
			Bukkit.getServer().getConsoleSender().sendMessage(line);
		}
	}

	private void printEnableStatus() {
		String lineTemplate = "%-30s -> %s";
		String [] status = {
				String.format(lineTemplate,"Anonymous Metrics",boolean2status(isSetupMetrics)),
				String.format(lineTemplate,"Economy Linked",boolean2status(isSetupEconomy)),
				String.format(lineTemplate,"Placeholders Registered",boolean2status(isSetupPlaceholders)),
				String.format(lineTemplate,"Team Colors Registered",boolean2status(konquest.isVersionHandlerEnabled())),
				String.format(lineTemplate,"Minecraft Version Supported",boolean2status(konquest.isVersionSupported()))
		};
		ChatUtil.printConsoleAlert("Final Status...");
		for (String row : status) {
			String line = ChatColor.GOLD+"> "+ChatColor.RESET + row;
			Bukkit.getServer().getConsoleSender().sendMessage(line);
		}
	}

	private String boolean2status(boolean val) {
		String result = "";
		if(val) {
			//result = ChatUtil.parseHex("#60C030")+"Success"; // Green
			result = ChatColor.DARK_GREEN+"Success";
		} else {
			//result = ChatUtil.parseHex("#FF2020")+"Fail"; // Red
			result = ChatColor.DARK_RED+"Fail";
		}
		return result;
	}
	
	/*
	 * Economy wrapper functions. Attempt to use Vault API methods, and then try depreciated methods if those fail.
	 */
	@SuppressWarnings("deprecation")
	public static double getBalance(OfflinePlayer offlineBukkitPlayer) {
		double result;
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
	
	public static boolean withdrawPlayer(OfflinePlayer offlineBukkitPlayer, double amount) {
		return withdrawPlayer(offlineBukkitPlayer, amount, false);
	}
	
	@SuppressWarnings("deprecation")
	public static boolean withdrawPlayer(OfflinePlayer offlineBukkitPlayer, double amount, boolean ignoreDiscounts) {
		boolean result = false;
		boolean isOnlinePlayer = offlineBukkitPlayer instanceof Player;
		// Check for discounts
		// Look for discount permissions, for biggest discount
		int discount = 0;
		if(!ignoreDiscounts && isOnlinePlayer) {
			for(PermissionAttachmentInfo p : ((Player)offlineBukkitPlayer).getEffectivePermissions()) {
				String perm = p.getPermission();
				if(perm.contains("konquest.discount")) {
					String[] permArr = perm.split("\\.",3);
					if(permArr.length == 3) {
						String valStr = permArr[2];
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
					} else {
						ChatUtil.printDebug("Failed to parse malformed discount permission: "+perm);
					}
				}
			}
		}
		// Apply discount
		double amountMod = amount;
		if(discount > 0 && discount <= 100) {
			//ChatUtil.printDebug("Applying discount of "+discount+"%");
			double amountOff = amount * ((double)discount / 100);
			amountMod = amount - amountOff;
			if(amountOff > 0) {
				String amountF = econ.format(amountOff);
				if(offlineBukkitPlayer.isOnline()) {
					ChatUtil.sendNotice((Player)offlineBukkitPlayer, MessagePath.GENERIC_NOTICE_DISCOUNT_FAVOR.getMessage(discount,amountF), ChatColor.DARK_AQUA);
				}
			}
		} else if(discount != 0) {
			ChatUtil.printDebug("Failed to apply invalid discount of "+discount+"%");
		}
		// Perform transaction
		EconomyResponse resp = null;
		try {
			resp = econ.withdrawPlayer(offlineBukkitPlayer, amountMod);
		} catch(Exception e) {
			ChatUtil.printDebug("Failed to withdraw using Player: "+e.getMessage());
			try {
				resp = econ.withdrawPlayer(offlineBukkitPlayer.getName(), amountMod);
			} catch(Exception x) {
				ChatUtil.printDebug("Failed to withdraw using Name: "+x.getMessage());
				resp = econ.withdrawPlayer(formatStringForAConomyPlugin(offlineBukkitPlayer), amountMod);
			}
		}
		// Send message
		if(resp != null) {
			if(resp.transactionSuccess()) {
	        	if(resp.amount > 0) {
					String balanceF = econ.format(resp.balance);
					String amountF = econ.format(resp.amount);
					if(isOnlinePlayer) {
						ChatUtil.sendNotice((Player)offlineBukkitPlayer, MessagePath.GENERIC_NOTICE_REDUCE_FAVOR.getMessage(amountF,balanceF), ChatColor.DARK_AQUA);
					}
		        	result = true;
	        	}
	        } else {
	        	if(isOnlinePlayer) {
	        		ChatUtil.sendError((Player)offlineBukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL_MESSAGE.getMessage(resp.errorMessage));
	        	}
	        }
		} else {
			if(isOnlinePlayer) {
				ChatUtil.sendError((Player)offlineBukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
			}
		}
		return result;
	}
	
	@SuppressWarnings("deprecation")
	public static boolean depositPlayer(OfflinePlayer offlineBukkitPlayer, double amount) {
		boolean result = false;
		boolean isOnlinePlayer = offlineBukkitPlayer instanceof Player;
		// Perform transaction
		EconomyResponse resp;
		try {
			resp = econ.depositPlayer(offlineBukkitPlayer, amount);
		} catch(Exception e) {
			ChatUtil.printDebug("Failed to deposit using Player: "+e.getMessage());
			try {
				resp = econ.depositPlayer(offlineBukkitPlayer.getName(), amount);
			} catch(Exception x) {
				ChatUtil.printDebug("Failed to deposit using Name: "+x.getMessage());
				resp = econ.depositPlayer(formatStringForAConomyPlugin(offlineBukkitPlayer), amount);
			}
		}
		// Send message
		if(resp != null) {
			if(resp.transactionSuccess()) {
				if(resp.amount > 0) {
		        	String balanceF = econ.format(resp.balance);
					String amountF = econ.format(resp.amount);
					if(isOnlinePlayer) {
		        		ChatUtil.sendNotice((Player)offlineBukkitPlayer, MessagePath.GENERIC_NOTICE_REWARD_FAVOR.getMessage(amountF,balanceF), ChatColor.DARK_GREEN);
					}
		        	result = true;
				}
	        } else {
	        	if(isOnlinePlayer) {
	        		ChatUtil.sendError((Player)offlineBukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL_MESSAGE.getMessage(resp.errorMessage));
	        	}
	        }
		} else {
			if(isOnlinePlayer) {
        		ChatUtil.sendError((Player)offlineBukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
			}
		}
		return result;
	}
	
	/**
	 * This method is stupid, and is specific to unique way that AConomy implements its Vault API methods.
	 * @param offlineBukkitPlayer The player to format
	 * @return String of player's UUID when server is online-mode=true, else player's lowercase name.
	 */
	private static String formatStringForAConomyPlugin(OfflinePlayer offlineBukkitPlayer) {
		String playerString;
		if(Bukkit.getServer().getOnlineMode()) {
			playerString = offlineBukkitPlayer.getUniqueId().toString();
		} else {
			playerString = offlineBukkitPlayer.getName().toLowerCase();
		}
		return playerString;
	}
}
