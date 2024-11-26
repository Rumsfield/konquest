package com.github.rumsfield.konquest.listener;

import com.github.rumsfield.konquest.api.event.KonquestEvent;
import com.github.rumsfield.konquest.utility.ChatUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class KonquestListener implements Listener {

    public KonquestListener() {}

    // Self listener
    @EventHandler()
    public void onKonquest(KonquestEvent event) {
        String eventName = event.getEventName();
        ChatUtil.printDebug("Caught Konquest event: "+eventName);
    }

}
