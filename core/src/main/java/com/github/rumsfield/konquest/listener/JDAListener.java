package com.github.rumsfield.konquest.listener;

import github.scarsz.discordsrv.dependencies.jda.api.events.guild.GuildUnavailableEvent;
import github.scarsz.discordsrv.dependencies.jda.api.hooks.ListenerAdapter;
import com.github.rumsfield.konquest.utility.ChatUtil;
/**
 * JDA Listener
 * @deprecated DiscordSRV has its own listener now
 * @see <a href="https://ci.dv8tion.net/job/JDA/javadoc/">for JDA's javadoc</a>
 * @see <a href="https://github.com/DV8FromTheWorld/JDA/wiki">for JDA's wiki</a>
 * {@link DiscordSRVListener}
 */
@Deprecated()
public class JDAListener extends ListenerAdapter {

    @Override // we can use any of JDA's events through ListenerAdapter, just by overriding the methods
    public void onGuildUnavailable(GuildUnavailableEvent event) {
        ChatUtil.printConsoleError("Oh no " + event.getGuild().getName() + " went unavailable :(");
    }

}
