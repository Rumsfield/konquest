package com.github.rumsfield.konquest;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.hook.WorldGuardExec;
import com.github.rumsfield.konquest.listener.*;
import com.github.rumsfield.konquest.utility.*;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public class KonquestPlugin extends JavaPlugin {

	private static final String vaultEconomyWarning = "There is a problem with the economy plugin, %s! Attempting to work around...";
	private static final String vaultEconomyError = "Failed to access the economy plugin %s using Vault API. This is an error in the economy plugin, NOT Konquest.\n"+
			"Steps to troubleshoot:\n"+
			"  1. Check for console errors from the economy plugin, did it enable correctly?\n"+
			"  2. Make sure you only have one economy plugin. Having multiple may cause issues.\n"+
			"  3. Contact the economy plugin developer to make them aware of this issue.\n"+
			"  4. Use another economy plugin.\n"+
			"Economy Error Message: %s\n";

	private Konquest konquest;
	private PluginManager pluginManager;
	private static Economy econ = null;
	private boolean enableSuccess = false;

	boolean isSetupEconomy = false;
	boolean isSetupMetrics = false;
	boolean isSetupPlaceholders = false;
	boolean isCompatibiltyValidated = false;

	@Override
	public void onLoad() {
		// Attempt to register WorldGuard flags
		Plugin worldGuard = Bukkit.getPluginManager().getPlugin("WorldGuard");
		// WorldGuard must be present but not yet enabled to register flags.
		if(worldGuard == null){
			return;
		}
		boolean registerStatus = WorldGuardExec.load();
		if(registerStatus) {
			ChatUtil.printConsoleAlert("Successfully registered WorldGuard flags.");
		}
	}

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
	    // Check Version
		if (CompatibilityUtil.apiVersion == null) {
			getLogger().severe(String.format("%s disabled due to invalid Bukkit API version.", getDescription().getName()));
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
		// Validate API Compatibility
		isCompatibiltyValidated = CompatibilityUtil.runBIT();
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
		}
	}
	
	public Konquest getKonquestInstance() {
		return konquest;
	}

	private void registerListeners() {
		// Set command executors
		PluginCommand konquestCommand = getCommand("konquest");
		assert konquestCommand != null;
		konquestCommand.setExecutor(konquest.getCommandHandler());
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
                ChatUtil.printConsoleWarning(message);
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
		ChatUtil.printConsoleAlert("Using Economy: "+econ.getName());
        return true;
    }

	public void printVersion() {
		String pluginVersion = this.getDescription().getVersion();
		String serverVersion = Bukkit.getServer().getBukkitVersion();
		String lineTemplate = "%-30s -> %s";
		String [] versionInfo = {
				String.format(lineTemplate,"Konquest Version",ChatColor.AQUA+pluginVersion),
				String.format(lineTemplate,"Server Version",ChatColor.AQUA+serverVersion),
				String.format(lineTemplate,"Server Version Supported",ChatUtil.boolean2status(konquest.isVersionSupported()))
		};
		for (String row : versionInfo) {
			String line = ChatColor.GOLD+"> "+ChatColor.RESET + row;
			Bukkit.getServer().getConsoleSender().sendMessage(line);
		}
	}

	public static void printLogo() {
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
				String.format(lineTemplate,"Anonymous Metrics",ChatUtil.boolean2status(isSetupMetrics)),
				String.format(lineTemplate,"Economy Linked",ChatUtil.boolean2status(isSetupEconomy)),
				String.format(lineTemplate,"Placeholders Registered",ChatUtil.boolean2status(isSetupPlaceholders)),
				String.format(lineTemplate,"Team Colors Registered",ChatUtil.boolean2status(konquest.isVersionHandlerEnabled())),
				String.format(lineTemplate,"Minecraft Version Supported",ChatUtil.boolean2status(konquest.isVersionSupported())),
				String.format(lineTemplate,"API Compatibility Validated",ChatUtil.boolean2status(isCompatibiltyValidated))
		};
		ChatUtil.printConsoleAlert("Final Status...");
		for (String row : status) {
			String line = ChatColor.GOLD+"> "+ChatColor.RESET + row;
			Bukkit.getServer().getConsoleSender().sendMessage(line);
		}
	}
	
	/*
	 * Economy wrapper functions. Attempt to use Vault API methods, and then try depreciated methods if those fail.
	 */

	public static String getCurrencyFormat(double amount) {
		return econ.format(amount);
	}

	@SuppressWarnings("deprecation")
	public static double getBalance(OfflinePlayer offlineBukkitPlayer) {
		double result;
		try {
			result = econ.getBalance(offlineBukkitPlayer);
		} catch(Exception ignored) {
			ChatUtil.printConsoleWarning(String.format(vaultEconomyWarning,econ.getName()));
			try {
				result = econ.getBalance(offlineBukkitPlayer.getName());
			} catch(Exception x) {
				ChatUtil.printConsoleError(String.format(vaultEconomyError,econ.getName(),x.getMessage()));
				x.printStackTrace();
				result = 0;
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
		} catch(Exception ignored) {
			ChatUtil.printConsoleWarning(String.format(vaultEconomyWarning,econ.getName()));
			try {
				resp = econ.withdrawPlayer(offlineBukkitPlayer.getName(), amountMod);
			} catch(Exception x) {
				ChatUtil.printConsoleError(String.format(vaultEconomyError,econ.getName(),x.getMessage()));
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
		EconomyResponse resp = null;
		try {
			resp = econ.depositPlayer(offlineBukkitPlayer, amount);
		} catch(Exception ignored) {
			ChatUtil.printConsoleWarning(String.format(vaultEconomyWarning,econ.getName()));
			try {
				resp = econ.depositPlayer(offlineBukkitPlayer.getName(), amount);
			} catch(Exception x) {
				ChatUtil.printConsoleError(String.format(vaultEconomyError,econ.getName(),x.getMessage()));
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

}
