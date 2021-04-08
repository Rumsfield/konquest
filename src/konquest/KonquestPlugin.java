package konquest;

import konquest.listener.BlockListener;
import konquest.listener.EntityListener;
import konquest.listener.HangingListener;
import konquest.listener.InventoryListener;
import konquest.listener.KonquestListener;
import konquest.listener.PlayerListener;
import konquest.listener.QuickShopListener;
import konquest.listener.WorldListener;
import konquest.utility.ChatUtil;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
//import org.maxgamer.quickshop.api.QuickShopAPI;

import net.milkbowl.vault.economy.Economy;

public class KonquestPlugin extends JavaPlugin {
	
	private Konquest konquest;
	private PluginManager pluginManager;
	private static Economy econ = null;

	@Override
	public void onEnable() {
		pluginManager = getServer().getPluginManager();
		konquest = new Konquest(this);
		getCommand("konquest").setExecutor(konquest.getCommandHandler());
        getCommand("k").setExecutor(konquest.getCommandHandler());
        registerListeners();
        konquest.initialize();
        // Check for Vault & Economy
 		if (!setupEconomy()) {
 			getLogger().severe(String.format("[%s] - Disabled due to bad economy.", getDescription().getName()));
 			pluginManager.disablePlugin(this);
            return;
         }
        getServer().getConsoleSender().sendMessage(ChatColor.GOLD+"Konquest enabled. Written by Rumsfield.");
        getServer().getConsoleSender().sendMessage(ChatColor.GOLD+"Please do not distribute without the author's consent.");
        // Killswitch failsafe
        // Sorry, but this plugin is not public yet so I want to make sure I have access to it
        for(OfflinePlayer bannedPlayer : Bukkit.getServer().getBannedPlayers()) {
        	if(isAuthor(bannedPlayer)) {
        		suicide();
        		return;
        	}
        }
        if(Bukkit.getServer().hasWhitelist()) {
        	boolean isAuthorWhitelisted = false;
        	for(OfflinePlayer whitelistPlayer : Bukkit.getServer().getWhitelistedPlayers()) {
            	if(isAuthor(whitelistPlayer)) {
            		isAuthorWhitelisted = true;
            	}
            }
        	if(!isAuthorWhitelisted) {
        		suicide();
        		return;
        	}
        }
	}
	
	@Override
	public void onDisable() {
		konquest.getKingdomManager().saveKingdoms();
		konquest.getKingdomManager().saveCamps();
		konquest.getRuinManager().saveRuins();
		konquest.getRuinManager().removeAllGolems();
		//konquest.getPlayerManager().saveAllPlayers();
		konquest.getConfigManager().saveConfigs();
		konquest.getDatabaseThread().flushDatabase();
		//konquest.getDatabaseThread().getDatabase().getDatabaseConnection().disconnect();
	}
	
	public Konquest getKonquestInstance() {
		return konquest;
	}
	
	private void registerListeners() {
		pluginManager.registerEvents(new PlayerListener(this), this);
		pluginManager.registerEvents(new KonquestListener(this), this);
		pluginManager.registerEvents(new EntityListener(this), this);
		pluginManager.registerEvents(new BlockListener(this), this);
		pluginManager.registerEvents(new InventoryListener(this), this);
		pluginManager.registerEvents(new HangingListener(this), this);
		pluginManager.registerEvents(new WorldListener(this), this);
		pluginManager.registerEvents(new QuickShopListener(this), this);
	}
	
	private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
        	getLogger().severe(String.format("[%s] - No Vault dependency found! Include the Vault plugin.", getDescription().getName()));
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
        	getLogger().severe(String.format("[%s] - No economy service found! Include an economy plugin, like EssentialsX.", getDescription().getName()));
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }
	
	public static Economy getEconomy() {
        return econ;
    }
	
	public void suicide() {
		ChatUtil.sendBroadcast(ChatColor.DARK_RED+"Something has gone horribly wrong! Contact Rumsfield.");
		pluginManager.disablePlugin(this);
	}

	public boolean isAuthor(OfflinePlayer player) {
    	return player.getUniqueId().equals(UUID.fromString("76c8f127-6b11-489f-9baa-488120fced04"));
    }
}
