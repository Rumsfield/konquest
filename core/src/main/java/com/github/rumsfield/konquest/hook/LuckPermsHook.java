package com.github.rumsfield.konquest.hook;

import com.github.rumsfield.konquest.utility.CorePath;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.utility.ChatUtil;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LuckPermsHook implements PluginHook {

	private final Konquest konquest;
	private boolean isEnabled;
	private LuckPerms lpAPI;
	
	public LuckPermsHook(Konquest konquest) {
		this.konquest = konquest;
		this.isEnabled = false;
		this.lpAPI = null;
	}

	@Override
	public String getPluginName() {
		return "LuckPerms";
	}

	@Override
	public int reload() {
		// Attempt to integrate Luckperms
		Plugin luckPerms = Bukkit.getPluginManager().getPlugin("LuckPerms");
		if(luckPerms == null){
			return 1;
		}
		if(!luckPerms.isEnabled()){
			return 2;
		}
		if(!konquest.getCore().getBoolean(CorePath.INTEGRATION_LUCKPERMS.getPath(),false)) {
			return 3;
		}
		RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
		if(provider == null){
			ChatUtil.printConsoleError("Failed to integrate LuckPerms, plugin not found or disabled");
			return -1;
		}
		lpAPI = provider.getProvider();
		isEnabled = true;
		return 0;
	}

	@Override
	public boolean isEnabled() {
		return isEnabled;
	}

	@Nullable
	public LuckPerms getLuckPermsAPI() {
		return lpAPI;
	}

	@NotNull
	public String getPrefix(Player player) {
		String prefix = "";
		if(!isEnabled)return prefix;

		User user = lpAPI.getUserManager().getUser(player.getUniqueId());
		QueryOptions queryOptions = lpAPI.getContextManager().getQueryOptions(player);
		CachedMetaData metaData = user.getCachedData().getMetaData(queryOptions);
		if (metaData.getPrefix() != null) {
			prefix = metaData.getPrefix().equalsIgnoreCase("null") ? "" : metaData.getPrefix();
		}

		return prefix;
	}

	@NotNull
	public String getSuffix(Player player) {
		String suffix = "";
		if(!isEnabled)return suffix;
		User user = lpAPI.getUserManager().getUser(player.getUniqueId());
		QueryOptions queryOptions = lpAPI.getContextManager().getQueryOptions(player);
		CachedMetaData metaData = user.getCachedData().getMetaData(queryOptions);
		if (metaData.getSuffix() != null) {
			suffix = metaData.getSuffix().equalsIgnoreCase("null") ? "" : metaData.getSuffix();
		}
		return suffix;
	}

}
