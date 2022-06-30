package konquest.listener;

import konquest.Konquest;
//import konquest.model.KonPlayer;
import konquest.utility.ChatUtil;
import github.scarsz.discordsrv.DiscordSRV;
//import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.ListenerPriority;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.*;
//import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
//import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.util.DiscordUtil;
//import org.bukkit.Bukkit;

public class DiscordSRVListener {

	private Konquest konquest;

    public DiscordSRVListener(Konquest konquest) {
        this.konquest = konquest;
    }
    
    @Subscribe
    public void discordReadyEvent(DiscordReadyEvent event) {
        // Example of using JDA's events
        // We need to wait until DiscordSRV has initialized JDA, thus we're doing this inside DiscordReadyEvent
        DiscordUtil.getJda().addEventListener(new JDAListener());

        // ... we can also do anything other than listen for events with JDA now,
        ChatUtil.printConsole("Chatting on Discord with " + DiscordUtil.getJda().getUsers().size() + " users!");
        // see https://ci.dv8tion.net/job/JDA/javadoc/ for JDA's javadoc
        // see https://github.com/DV8FromTheWorld/JDA/wiki for JDA's wiki
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
    
    /*
    @Subscribe(priority = ListenerPriority.MONITOR)
    public void discordMessageReceived(DiscordGuildMessageReceivedEvent event) {
        // Example of logging a message sent in Discord

    	ChatUtil.printDebug("Received a chat message on Discord: " + event.getMessage());
    }
	*/
    
    @Subscribe(priority = ListenerPriority.MONITOR)
    public void aMessageWasSentInADiscordGuildByTheBot(DiscordGuildMessageSentEvent event) {
        // Example of logging a message sent in Minecraft (being sent to Discord)
    	
    	ChatUtil.printDebug("A message was sent to Discord: " + event.getMessage());
    }

    @Subscribe
    public void accountsLinked(AccountLinkedEvent event) {
        // Example of broadcasting a message when a new account link has been made

    	ChatUtil.printDebug(event.getPlayer().getName() + " just linked their MC account to their Discord user " + event.getUser() + "!");
    }

    /*
    @Subscribe
    public void accountUnlinked(AccountUnlinkedEvent event) {
        // Example of DM:ing user on unlink
        User user = DiscordUtil.getJda().getUserById(event.getDiscordId());

        // will be null if the bot isn't in a Discord server with the user (eg. they left the main Discord server)
        if (user != null) {

            // opens/retrieves the private channel for the user & sends a message to it (if retrieving the private channel was successful)
            user.openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Your account has been unlinked").queue());
        }

        // Example of sending a message to a channel called "unlinks" (defined in the config.yml using the Channels option) when a user unlinks
        TextChannel textChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName("unlinks");

        // null if the channel isn't specified in the config.yml
        if (textChannel != null) {
            textChannel.sendMessage(event.getPlayer().getName() + " (" + event.getPlayer().getUniqueId() + ") has unlinked their associated Discord account: "
                    + (event.getDiscordUser() != null ? event.getDiscordUser().getName() : "<not available>") + " (" + event.getDiscordId() + ")").queue();
        } else {
        	ChatUtil.printConsoleAlert("Channel called \"unlinks\" could not be found in the DiscordSRV configuration");
        }
    }
	*/
    
    /*
    @Subscribe
    public void discordMessageProcessed(DiscordGuildMessagePostProcessEvent event) {
        // Example of modifying a Discord -> Minecraft message
        event.setProcessedMessage(event.getProcessedMessage().replace("cat", "dog")); // dogs are superior to cats, obviously
    }
    */
    
    /*
    @SuppressWarnings("deprecation")
	@Subscribe
    public void onGameMessagePreProcess(GameChatMessagePreProcessEvent event) {
    	// Konquest has cleared the content of the message in Bukkit's AsyncPlayerChatEvent event.
    	// Add the message back for DiscordSRV.
    	KonPlayer player = konquest.getPlayerManager().getPlayer(event.getPlayer());
    	if(player != null) {
    		ChatUtil.printDebug("Modifying game message pre-process event");
    		event.setMessage(player.getLastChatMessage());
    		if(!player.isGlobalChat()) {
    			event.setChannel(player.getKingdom().getName());
    		}
    	}
    }
    */
}
