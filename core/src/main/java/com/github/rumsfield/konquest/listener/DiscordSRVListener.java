package com.github.rumsfield.konquest.listener;

import com.github.rumsfield.konquest.Konquest;

import com.github.rumsfield.konquest.model.KonOfflinePlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;
import github.scarsz.discordsrv.DiscordSRV;

import github.scarsz.discordsrv.api.ListenerPriority;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.*;

import github.scarsz.discordsrv.util.DiscordUtil;
import org.bukkit.event.Listener;


public class DiscordSRVListener {

	private final Konquest konquest;

    public DiscordSRVListener(Konquest konquest) {
        this.konquest = konquest;
    }
    
    @Subscribe
    public void discordReadyEvent(DiscordReadyEvent event) {

        // ... we can also do anything other than listen for events with JDA now,
        ChatUtil.printConsole("Chatting on Discord with " + DiscordUtil.getJda().getUsers().size() + " users!");
        // see https://ci.dv8tion.net/job/JDA/javadoc/ for JDA's javadoc
        // see https://github.com/DV8FromTheWorld/JDA/wiki for JDA's wiki
        konquest.getIntegrationManager().getDiscordSrv().setDiscordReady();
        konquest.getIntegrationManager().getDiscordSrv().refreshRoles();
    }

    @Subscribe(priority = ListenerPriority.MONITOR)
    public void onDiscordAccountLinked(AccountLinkedEvent event) {
        ChatUtil.printDebug("Discord member linked account to player "+event.getPlayer().getName());
        KonOfflinePlayer player = konquest.getPlayerManager().getOfflinePlayerFromID(event.getPlayer().getUniqueId());
        if (player == null) {
            ChatUtil.printDebug("Failed to find linked player from Konquest database.");
            return;
        }
        konquest.getIntegrationManager().getDiscordSrv().refreshPlayerRoles(player);
    }

    @Subscribe(priority = ListenerPriority.MONITOR)
    public void onDiscordMessagePostProcess(DiscordGuildMessagePostProcessEvent event) {
    	String name = event.getAuthor().getName();
    	String channel = event.getChannel().getName();
    	String linkChannel = DiscordSRV.getPlugin().getDestinationGameChannelNameForTextChannel(event.getChannel());
    	String message = event.getMessage().getContentDisplay();
    	ChatUtil.printDebug("Received Discord message: Channel "+channel+"; Link "+linkChannel+"; Author "+name+"; Message "+message);
    	
    	// Send to kingdom chat if valid
    	konquest.getIntegrationManager().getDiscordSrv().sendDiscordToGameChatKingdomChannel(event.getAuthor(), event.getMessage(), linkChannel);
    }

}
