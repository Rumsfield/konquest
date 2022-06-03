package konquest.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import konquest.Konquest;
import konquest.utility.ChatUtil;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;

public class LuckPermsHook implements PluginHook {

	private Konquest konquest;
	private boolean isEnabled;
	private LuckPerms lpAPI;
	
	public LuckPermsHook(Konquest konquest) {
		this.konquest = konquest;
		this.isEnabled = false;
		this.lpAPI = null;
	}
	
	@Override
	public void reload() {
		// Attempt to integrate Luckperms
		Plugin luckPerms = Bukkit.getPluginManager().getPlugin("LuckPerms");
		if (luckPerms != null && luckPerms.isEnabled()) {
			if(konquest.getConfigManager().getConfig("core").getBoolean("core.integration.luckperms",false)) {
				RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
				if (provider != null) {
					lpAPI = provider.getProvider();
					isEnabled = true;
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
	}
	
	@Override
	public void shutdown() {
		// Do Nothing
	}

	@Override
	public boolean isEnabled() {
		return isEnabled;
	}
	
	// this can return null!
	public LuckPerms getLuckPermsAPI() {
		return lpAPI;
	}
	
	public String getPrefix(Player player) {
		String prefix = "";
		if(isEnabled) {
			User user = lpAPI.getUserManager().getUser(player.getUniqueId());
			QueryOptions queryOptions = lpAPI.getContextManager().getQueryOptions(player);
	        CachedMetaData metaData = user.getCachedData().getMetaData(queryOptions);
	        if (metaData.getPrefix() != null) {
	        	prefix = metaData.getPrefix().equalsIgnoreCase("null") ? "" : metaData.getPrefix();
	        }
		}
		return prefix;
	}
	
	public String getSuffix(Player player) {
		String suffix = "";
		if(isEnabled) {
			User user = lpAPI.getUserManager().getUser(player.getUniqueId());
			QueryOptions queryOptions = lpAPI.getContextManager().getQueryOptions(player);
	        CachedMetaData metaData = user.getCachedData().getMetaData(queryOptions);
	        if (metaData.getSuffix() != null) {
	        	suffix = metaData.getSuffix().equalsIgnoreCase("null") ? "" : metaData.getSuffix();
	        }
		}
		return suffix;
	}

}
