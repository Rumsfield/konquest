package com.github.rumsfield.konquest.manager;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.KonGlobalEvent;
import com.github.rumsfield.konquest.model.KonGlobalEventEffect;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.Timeable;
import com.github.rumsfield.konquest.utility.Timer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GlobalEventManager implements Timeable {

    private final Konquest konquest;
    private final ArrayList<KonGlobalEvent> events;
    private final Timer eventTimer;
    private final int eventTimerInterval = 1200; // one minute in ticks, probably should be core.yml setting
    private boolean isEventDataNull;

    public GlobalEventManager(Konquest konquest) {
        this.konquest = konquest;
        this.events = new ArrayList<>();
        this.eventTimer = new Timer(this);
        this.isEventDataNull = false;
    }

    public void initialize() {
        loadEvents();
        refreshAllEvents();
        // Start the event timer
        eventTimer.stopTimer();
        eventTimer.setTime(eventTimerInterval);
        eventTimer.startLoopTimer();
        ChatUtil.printDebug("Global Event Manager is ready");
    }

    @Override
    public void onEndTimer(int taskID) {
        if(taskID == 0) {
            ChatUtil.printDebug("Event Timer ended with null taskID!");
        } else if(taskID == eventTimer.getTaskID()) {
            refreshAllEvents();
            // Apply effects to other managers
            // TODO implement effects
        }
    }

    private void refreshAllEvents() {
        // Refresh active events
        for (KonGlobalEvent event : events) {
            boolean wasActive = event.isActive();
            boolean nowActive = event.refreshActive();
            if (event.isBroadcast()) {
                // Broadcast start and end
                // TODO use message paths
                if (!wasActive && nowActive) {
                    // Event started
                    ChatUtil.sendBroadcast("Global Event "+event.getName()+" has started! Use /k events for details.");
                } else if (wasActive && !nowActive) {
                    // Event ended
                    ChatUtil.sendBroadcast("Global Event "+event.getName()+" has ended! Use /k events for details.");
                    // Remove events without repetition
                    if (!event.isRepeating()) {
                        removeEvent(event);
                    }
                }
            }
        }
    }

    public boolean isEffectActive(KonGlobalEventEffect effect) {
        // Check whether an effect is currently active in any enabled event
        for (KonGlobalEvent event : events) {
            if (event.isActive() && event.isEnabled() && event.hasEffect(effect)) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<KonGlobalEvent> getEvents(boolean isActive) {
        // Get all or only active events
        ArrayList<KonGlobalEvent> result = new ArrayList<>();
        if (isActive) {
            for (KonGlobalEvent event : events) {
                if (event.isActive()) {
                    result.add(event);
                }
            }
        } else {
            result.addAll(events);
        }
        return result;
    }

    public KonGlobalEvent getEvent(String name) {
        for (KonGlobalEvent event : events) {
            if (event.getName().equals(name)) {
                return event;
            }
        }
        // Could not find event by name
        return null;
    }

    public boolean isEvent(String name) {
        for (KonGlobalEvent event : events) {
            if (event.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public boolean createEvent(String name, boolean isBroadcast, boolean isEnabled, long start, long duration, long repetition, List<KonGlobalEventEffect> effects) {
        // Make a new event
        if (konquest.validateNameConstraints(name) != 0) {
            return false;
        }
        KonGlobalEvent globalEvent = new KonGlobalEvent(name);
        globalEvent.setBroadcast(isBroadcast);
        globalEvent.setEnabled(isEnabled);
        globalEvent.setStart(start);
        globalEvent.setDuration(duration);
        globalEvent.setRepetition(repetition);
        for (KonGlobalEventEffect effect : effects) {
            globalEvent.addEffect(effect);
        }
        return addEvent(globalEvent);
    }

    public void modifyEventBroadcast(String name, boolean isBroadcast) {
        KonGlobalEvent globalEvent = getEvent(name);
        if (globalEvent == null) return;
        globalEvent.setEnabled(isBroadcast);
    }

    public void modifyEventEnable(String name, boolean isEnabled) {
        KonGlobalEvent globalEvent = getEvent(name);
        if (globalEvent == null) return;
        globalEvent.setEnabled(isEnabled);
    }

    public void modifyEventDates(String name, long start, long duration, long repetition) {
        KonGlobalEvent globalEvent = getEvent(name);
        if (globalEvent == null) return;
        globalEvent.setStart(start);
        globalEvent.setDuration(duration);
        globalEvent.setRepetition(repetition);
    }

    public void modifyEventEffects(String name, List<KonGlobalEventEffect> effects) {
        KonGlobalEvent globalEvent = getEvent(name);
        if (globalEvent == null) return;
        globalEvent.clearEffects();
        for (KonGlobalEventEffect effect : effects) {
            globalEvent.addEffect(effect);
        }
    }

    public void cancelEvent(String name) {
        // Stop an existing event
        KonGlobalEvent globalEvent = getEvent(name);
        if (globalEvent == null) return;
        removeEvent(globalEvent);
    }

    public void startEvent(String name) {
        // Start an existing event
        KonGlobalEvent globalEvent = getEvent(name);
        if (globalEvent == null) return;
        Date now = new Date();
        globalEvent.setStart(now.getTime());
        refreshAllEvents();
    }

    private void removeEvent(KonGlobalEvent event) {
        // Remove event from list
        String eventName = event.getName();
        events.remove(event);
        ChatUtil.printConsole("Removed Global Event "+eventName);
    }

    private boolean addEvent(KonGlobalEvent event) {
        // Add event to list, with checks
        // Check for existing match
        if (events.contains(event)) {
            return false;
        }
        // Check for existing name
        if (isEvent(event.getName())) {
            return false;
        }
        // Add to list
        String eventName = event.getName();
        events.add(event);
        ChatUtil.printConsole("Added Global Event "+eventName);
        return true;
    }

    private void loadEvents() {
        // Load events from file
        FileConfiguration eventsConfig = konquest.getConfigManager().getConfig("global-events");
        if (eventsConfig.get("global-events") == null) {
            ChatUtil.printConsoleError("Failed to load any global events from global-events.yml! Check file permissions.");
            isEventDataNull = true;
            return;
        }
        boolean isEnabled, isBroadcast;
        long startTime, durationTime, repetitionTime;
        List<String> eventEffects;
        KonGlobalEvent globalEvent;
        // Load all Events
        ConfigurationSection eventsSection = eventsConfig.getConfigurationSection("global-events");
        for(String eventName : eventsConfig.getConfigurationSection("global-events").getKeys(false)) {
            ConfigurationSection eventSection = eventsSection.getConfigurationSection(eventName);
            // Gather data
            isEnabled = eventSection.getBoolean("enabled");
            isBroadcast = eventSection.getBoolean("broadcast");
            startTime = eventSection.getLong("start");
            durationTime = eventSection.getLong("duration");
            repetitionTime = eventSection.getLong("repetition");
            eventEffects = eventSection.getStringList("effects");
            // Create event
            globalEvent = new KonGlobalEvent(eventName);
            globalEvent.setEnabled(isEnabled);
            globalEvent.setBroadcast(isBroadcast);
            globalEvent.setStart(startTime);
            globalEvent.setDuration(durationTime);
            globalEvent.setRepetition(repetitionTime);
            for (String effectName : eventEffects) {
                globalEvent.addEffect(KonGlobalEventEffect.getEffect(effectName));
            }
            // Add event
            if (addEvent(globalEvent)) {
                ChatUtil.printDebug("Loaded global event "+eventName);
            } else {
                String message = "Could not load global event "+eventName+", global-events.yml may be corrupted and needs to be deleted.";
                ChatUtil.printConsoleError(message);
                konquest.opStatusMessages.add(message);
            }
        }
    }

    public void saveEvents() {
        // Save events to file
        if(isEventDataNull && events.isEmpty()) {
            // There was probably an issue loading events, do not save.
            ChatUtil.printConsoleError("Aborted saving global event data because a problem was encountered while loading data from global-events.yml");
            return;
        }
        // Create new config entries
        FileConfiguration newSaveConfig = new YamlConfiguration();
        ConfigurationSection root = newSaveConfig.createSection("global-events");
        try {
            for (KonGlobalEvent event : events) {
                ConfigurationSection eventSection = root.createSection(event.getName());
                eventSection.set("enabled", event.isEnabled());
                eventSection.set("broadcast", event.isBroadcast());
                eventSection.set("start", event.getStartTime());
                eventSection.set("duration", event.getDurationTime());
                eventSection.set("repetition", event.getRepetitionTime());
                // Effects
                ArrayList<KonGlobalEventEffect> eventEffects = new ArrayList<>();
                for (KonGlobalEventEffect effect : KonGlobalEventEffect.values()) {
                    if (event.hasEffect(effect)) {
                        eventEffects.add(effect);
                    }
                }
                eventSection.set("effects", eventEffects);
            }
            // Save to file
            FileConfiguration eventsConfig = konquest.getConfigManager().getConfig("global-events");
            eventsConfig.set("global-events", newSaveConfig.get("global-events")); // apply new save data
            if(!events.isEmpty()) {
                ChatUtil.printConsole("Saved Global Events");
            }
        } catch (Exception | Error internalError) {
            ChatUtil.printConsoleError("Failed to save global events, report this as a bug to the plugin author!");
            internalError.printStackTrace();
        }
    }

}
