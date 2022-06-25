package konquest.manager;


import java.util.HashSet;

import konquest.Konquest;
import konquest.hook.DiscordSrvHook;
import konquest.hook.LuckPermsHook;
import konquest.hook.PluginHook;
import konquest.hook.QuickShopHook;
import konquest.utility.ChatUtil;


public class IntegrationManager {

	private Konquest konquest;
	private HashSet<PluginHook> hooks;
	private LuckPermsHook luckpermsHook;
	private QuickShopHook quickshopHook;
	private DiscordSrvHook discordsrvHook;
	
	public IntegrationManager(Konquest konquest) {
		this.konquest = konquest;
		this.hooks = new HashSet<PluginHook>();
	}
	
	
	public void initialize() {
		// Shutdown any existing hooks
		for (PluginHook hook : hooks) {
			hook.shutdown();
		}
		
		// Define new hooks
		luckpermsHook = new LuckPermsHook(konquest);
		quickshopHook = new QuickShopHook(konquest);
		discordsrvHook = new DiscordSrvHook(konquest);
		
		hooks.clear();
		hooks.add(luckpermsHook);
		hooks.add(quickshopHook);
		hooks.add(discordsrvHook);
		
		// Reload all hooks
		for (PluginHook hook : hooks) {
			hook.reload();
		}
		
		ChatUtil.printDebug("Integration Manager is ready");

	}
	
	public void disable() {
		// Shutdown all hooks
		for (PluginHook hook : hooks) {
			hook.shutdown();
		}
		ChatUtil.printDebug("Integration Manager is disabled");
	}
	
	public LuckPermsHook getLuckPerms() {
		return luckpermsHook;
	}
	
	public QuickShopHook getQuickShop() {
		return quickshopHook;
	}
	
	public DiscordSrvHook getDiscordSrv() {
		return discordsrvHook;
	}
	
}
