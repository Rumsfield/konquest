package konquest.listener;

import github.scarsz.discordsrv.dependencies.jda.api.events.guild.GuildUnavailableEvent;
import github.scarsz.discordsrv.dependencies.jda.api.hooks.ListenerAdapter;
import konquest.utility.ChatUtil;

// see https://ci.dv8tion.net/job/JDA/javadoc/ for JDA's javadoc
// see https://github.com/DV8FromTheWorld/JDA/wiki for JDA's wiki

public class JDAListener extends ListenerAdapter {

    public JDAListener() {
    }

    @Override // we can use any of JDA's events through ListenerAdapter, just by overriding the methods
    public void onGuildUnavailable(GuildUnavailableEvent event) {
        ChatUtil.printConsoleError("Oh no " + event.getGuild().getName() + " went unavailable :(");
    }
}
