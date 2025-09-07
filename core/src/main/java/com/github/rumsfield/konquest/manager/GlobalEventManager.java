package com.github.rumsfield.konquest.manager;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.api.event.server.KonquestGlobalEventEndEvent;
import com.github.rumsfield.konquest.api.event.server.KonquestGlobalEventStartEvent;
import com.github.rumsfield.konquest.model.KonGlobalEvent;
import com.github.rumsfield.konquest.model.KonGlobalEventEffect;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.*;
import com.github.rumsfield.konquest.utility.Timer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.annotation.Nullable;
import java.util.*;

/*
 TODO
 - make event menu
 */

public class GlobalEventManager implements Timeable {

    private final Konquest konquest;
    private final ArrayList<KonGlobalEvent> events;
    private final HashSet<KonGlobalEventEffect> validEffects;
    private final Timer eventTimer;
    private boolean isEventDataNull;
    private final Comparator<KonGlobalEvent> eventComparator;

    private int eventIntervalSeconds;
    private boolean isEnabled;

    public static double favorDiscountMultiplier = 1.0;

    public GlobalEventManager(Konquest konquest) {
        this.konquest = konquest;
        this.events = new ArrayList<>();
        this.validEffects = new HashSet<>();
        this.eventTimer = new Timer(this);
        this.isEventDataNull = false;
        this.eventIntervalSeconds = 30;
        this.isEnabled = true;

        this.eventComparator = (eventOne, eventTwo) -> {
            // sort by start date
            int result = 0;
            long s1 = eventOne.getStartTime();
            long s2 = eventTwo.getStartTime();
            if(s1 < s2) {
                result = 1;
            } else if(s1 > s2) {
                result = -1;
            }
            return result;
        };
    }

    public void initialize() {
        // Load events from data file
        loadEvents();
        // Get core settings
        eventIntervalSeconds = Math.abs(konquest.getCore().getInt(CorePath.EVENT_INTERVAL.getPath()));
        if (eventIntervalSeconds == 0) {
            isEnabled = false;
            eventTimer.stopTimer();
        } else {
            isEnabled = true;
            // Update all events
            refreshAllEvents();
            // Start the event timer
            eventTimer.stopTimer();
            eventTimer.setTime(eventIntervalSeconds); // seconds
            eventTimer.startLoopTimer();
        }
        ChatUtil.printDebug("Global Event Manager is ready, enabled = "+isEnabled);
    }

    public void shutdown() {
        // Disable events
        konquest.getKingdomManager().enableGlobalEventWar(false);
        konquest.getKingdomManager().enableGlobalEventPeace(false);
        konquest.getShieldManager().enableGlobalEventFreeShield(false);
        konquest.getShieldManager().enableGlobalEventDisableShield(false);
        konquest.getShieldManager().enableGlobalEventDisableArmor(false);
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public void onEndTimer(int taskID) {
        if(taskID == 0) {
            ChatUtil.printDebug("Event Timer ended with null taskID!");
        } else if(taskID == eventTimer.getTaskID()) {
            refreshAllEvents();
        }
    }

    private void refreshAllEvents() {
        HashSet<KonGlobalEventEffect> eventEffects = new HashSet<>();
        // Refresh active events
        for (KonGlobalEvent event : events) {
            // Send notifications, events
            boolean wasActive = event.isActive();
            boolean nowActive = event.refreshActive();
            if (!wasActive && nowActive) {
                // Event started
                if (event.isEnabled()) {
                    notifyAllPlayers(MessagePath.COMMAND_EVENT_BROADCAST_START.getMessage(event.getName()),true);
                    // Fire event
                    ArrayList<String> eventEffectNames = new ArrayList<>();
                    ArrayList<String> eventEffectDescriptions = new ArrayList<>();
                    for (KonGlobalEventEffect effect : KonGlobalEventEffect.values()) {
                        if (event.hasEffect(effect)) {
                            eventEffectNames.add(effect.getTitle());
                            eventEffectDescriptions.add(effect.getDescription());
                        }
                    }
                    KonquestGlobalEventStartEvent invokeEvent = new KonquestGlobalEventStartEvent(konquest,event.getName(),event.getDurationHours(),eventEffectNames,eventEffectDescriptions);
                    Konquest.callKonquestEvent(invokeEvent);
                }
            } else if (wasActive && !nowActive) {
                // Event ended
                if (event.isEnabled()) {
                    notifyAllPlayers(MessagePath.COMMAND_EVENT_BROADCAST_END.getMessage(event.getName()),false);
                    // Fire event
                    ArrayList<String> eventEffectNames = new ArrayList<>();
                    ArrayList<String> eventEffectDescriptions = new ArrayList<>();
                    for (KonGlobalEventEffect effect : KonGlobalEventEffect.values()) {
                        if (event.hasEffect(effect)) {
                            eventEffectNames.add(effect.getTitle());
                            eventEffectDescriptions.add(effect.getDescription());
                        }
                    }
                    KonquestGlobalEventEndEvent invokeEvent = new KonquestGlobalEventEndEvent(konquest,event.getName(),event.getRepetitionDays(),eventEffectNames,eventEffectDescriptions);
                    Konquest.callKonquestEvent(invokeEvent);
                }
            }
            if (event.isActive() && event.isEnabled()) {
                eventEffects.addAll(event.getEffects());
            }
        }
        // Update valid effects
        validEffects.clear();
        // Filter out lower priority group effects
        for (KonGlobalEventEffect effect : eventEffects) {
            boolean isHighestPriorityEffect = true;
            for (KonGlobalEventEffect otherEffect : eventEffects) {
                if (effect.isHigherPriority(otherEffect)) {
                    isHighestPriorityEffect = false;
                    break;
                }
            }
            if (isHighestPriorityEffect) {
                validEffects.add(effect);
            }
        }
        // Apply effects
        konquest.getKingdomManager().enableGlobalEventWar(isEffectValid(KonGlobalEventEffect.ALL_WAR));
        konquest.getKingdomManager().enableGlobalEventPeace(isEffectValid(KonGlobalEventEffect.ALL_PEACE));
        konquest.getShieldManager().enableGlobalEventFreeShield(isEffectValid(KonGlobalEventEffect.FREE_SHIELDS));
        konquest.getShieldManager().enableGlobalEventDisableShield(isEffectValid(KonGlobalEventEffect.NO_SHIELDS));
        konquest.getShieldManager().enableGlobalEventDisableArmor(isEffectValid(KonGlobalEventEffect.NO_ARMOR));
        updateFavorDiscountMultiplier();
    }

    public void refreshDelayedEvents() {
        Bukkit.getScheduler().scheduleSyncDelayedTask(Konquest.getInstance().getPlugin(), this::refreshAllEvents, 20);
    }

    public boolean isEffectValid(KonGlobalEventEffect effect) {
        // Check whether an effect is currently active in any enabled event
        return validEffects.contains(effect);
    }

    public ArrayList<KonGlobalEventEffect> getValidEffects() {
        // Return all effects in active, enabled events with group priority filtered
        return new ArrayList<>(validEffects);
    }

    private void updateFavorDiscountMultiplier() {
        if (validEffects.contains(KonGlobalEventEffect.FAVOR_3)) {
            favorDiscountMultiplier = 0.20;
        } else if (validEffects.contains(KonGlobalEventEffect.FAVOR_2)) {
            favorDiscountMultiplier = 0.50;
        } else if (validEffects.contains(KonGlobalEventEffect.FAVOR_1)) {
            favorDiscountMultiplier = 0.80;
        } else {
            favorDiscountMultiplier = 1.00;
        }
    }

    public int getMonumentLootMultiplier() {
        if (validEffects.contains(KonGlobalEventEffect.MONUMENT_LOOT_3)) {
            return 10;
        } else if (validEffects.contains(KonGlobalEventEffect.MONUMENT_LOOT_2)) {
            return 5;
        } else if (validEffects.contains(KonGlobalEventEffect.MONUMENT_LOOT_1)) {
            return 2;
        } else {
            return 1;
        }
    }

    public int getRuinLootMultiplier() {
        if (validEffects.contains(KonGlobalEventEffect.RUIN_LOOT_3)) {
            return 10;
        } else if (validEffects.contains(KonGlobalEventEffect.RUIN_LOOT_2)) {
            return 5;
        } else if (validEffects.contains(KonGlobalEventEffect.RUIN_LOOT_1)) {
            return 2;
        } else {
            return 1;
        }
    }

    public int getExpMultiplier() {
        if (validEffects.contains(KonGlobalEventEffect.EXP_BOOST_3)) {
            return 10;
        } else if (validEffects.contains(KonGlobalEventEffect.EXP_BOOST_2)) {
            return 5;
        } else if (validEffects.contains(KonGlobalEventEffect.EXP_BOOST_1)) {
            return 2;
        } else {
            return 1;
        }
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

    public ArrayList<KonGlobalEvent> getEventsOnDay(Date day) {
        ArrayList<KonGlobalEvent> result = new ArrayList<>();
        for (KonGlobalEvent event : events) {
            if (event.isActiveOnDay(day)) {
                result.add(event);
            }
        }
        return result;
    }

    public @Nullable KonGlobalEvent getNextEvent() {
        // Get the event with the next closest start time to now
        KonGlobalEvent nextEvent = null;
        Date nextStart = null;
        for (KonGlobalEvent event : events) {
            Date eventNextStart = event.getNextStartDate();
            if (eventNextStart != null && (nextStart == null || eventNextStart.before(nextStart))) {
                nextStart = eventNextStart;
                nextEvent = event;
            }
        }
        return nextEvent;
    }

    public ArrayList<KonGlobalEvent> getSortedEvents() {
        // Get all events, sorted by start time
        ArrayList<KonGlobalEvent> result = new ArrayList<>(events);
        result.sort(eventComparator);
        return result;
    }

    public ArrayList<String> getEventNames() {
        // Get all event names
        ArrayList<String> result = new ArrayList<>();
        for (KonGlobalEvent event : events) {
            result.add(event.getName());
        }
        return result;
    }

    public @Nullable KonGlobalEvent getEvent(String name) {
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

    public boolean createEvent(String name, boolean isEnabled, long start, long duration, long repetition, List<KonGlobalEventEffect> effects) {
        // Make a new event
        if (konquest.validateNameConstraints(name) != 0) {
            return false;
        }
        KonGlobalEvent globalEvent = new KonGlobalEvent(name);
        globalEvent.setEnabled(isEnabled);
        globalEvent.setStart(start);
        globalEvent.setDuration(duration);
        globalEvent.setRepetition(repetition);
        for (KonGlobalEventEffect effect : effects) {
            globalEvent.addEffect(effect);
        }
        return addEvent(globalEvent);
    }

    public void modifyEventEnable(String name, boolean isEnabled) {
        KonGlobalEvent globalEvent = getEvent(name);
        if (globalEvent == null) return;
        globalEvent.setEnabled(isEnabled);
        refreshDelayedEvents();
    }

    public void modifyEventStart(String name, Date startDate) {
        KonGlobalEvent globalEvent = getEvent(name);
        if (globalEvent == null) return;
        globalEvent.setStart(startDate);
        refreshDelayedEvents();
    }

    public boolean modifyEventDuration(String name, long duration) {
        KonGlobalEvent globalEvent = getEvent(name);
        if (globalEvent == null) return false;
        if (globalEvent.getRepetitionTime() < duration) {
            return false;
        }
        globalEvent.setDuration(duration);
        refreshDelayedEvents();
        return true;
    }

    public boolean modifyEventRepetition(String name, long repetition) {
        KonGlobalEvent globalEvent = getEvent(name);
        if (globalEvent == null) return false;
        if (globalEvent.getDurationTime() > repetition) {
            return false;
        }
        globalEvent.setRepetition(repetition);
        refreshDelayedEvents();
        return true;
    }

    public void modifyEventEffects(String name, List<KonGlobalEventEffect> effects) {
        KonGlobalEvent globalEvent = getEvent(name);
        if (globalEvent == null) return;
        globalEvent.clearEffects();
        for (KonGlobalEventEffect effect : effects) {
            globalEvent.addEffect(effect);
        }
        refreshDelayedEvents();
    }

    public void enableEvent(String name, boolean isEnabled) {
        // Enable or disable an existing event
        KonGlobalEvent globalEvent = getEvent(name);
        if (globalEvent == null) return;
        boolean wasEnabled = globalEvent.isEnabled();
        globalEvent.setEnabled(isEnabled);
        if (globalEvent.isActive()) {
            // Broadcast changes to enable
            if (!wasEnabled && isEnabled) {
                // Event enabled
                ChatUtil.sendBroadcast(MessagePath.COMMAND_EVENT_BROADCAST_ENABLE.getMessage(globalEvent.getName()));
            } else if (wasEnabled && !isEnabled) {
                // Event disabled
                ChatUtil.sendBroadcast(MessagePath.COMMAND_EVENT_BROADCAST_DISABLE.getMessage(globalEvent.getName()));
            }
        }
        refreshDelayedEvents();
    }

    public void cancelEvent(String name) {
        // Stop an existing event
        KonGlobalEvent globalEvent = getEvent(name);
        if (globalEvent == null) return;
        if (globalEvent.isActive()) {
            notifyAllPlayers(MessagePath.COMMAND_EVENT_BROADCAST_END.getMessage(globalEvent.getName()),false);
            // Fire event
            ArrayList<String> eventEffectNames = new ArrayList<>();
            ArrayList<String> eventEffectDescriptions = new ArrayList<>();
            for (KonGlobalEventEffect effect : KonGlobalEventEffect.values()) {
                if (globalEvent.hasEffect(effect)) {
                    eventEffectNames.add(effect.getTitle());
                    eventEffectDescriptions.add(effect.getDescription());
                }
            }
            KonquestGlobalEventEndEvent invokeEvent = new KonquestGlobalEventEndEvent(konquest,globalEvent.getName(),0,eventEffectNames,eventEffectDescriptions);
            Konquest.callKonquestEvent(invokeEvent);
        }
        removeEvent(globalEvent);
        refreshDelayedEvents();
    }

    public void startEvent(String name) {
        // Start an existing event (enable it too)
        KonGlobalEvent globalEvent = getEvent(name);
        if (globalEvent == null) return;
        globalEvent.setEnabled(true);
        Date now = new Date();
        globalEvent.setStart(now.getTime());
        // Broadcast messages included in refresh
        refreshDelayedEvents();
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

    private void notifyAllPlayers(String message, boolean type) {
        for (KonPlayer onlinePlayer : konquest.getPlayerManager().getPlayersOnline()) {
            if (type) {
                Konquest.playNotificationGoodSound(onlinePlayer.getBukkitPlayer());
            } else {
                Konquest.playNotificationBadSound(onlinePlayer.getBukkitPlayer());
            }
        }
        ChatUtil.sendBroadcast(message);
    }

    private void loadEvents() {
        // Load events from file
        FileConfiguration eventsConfig = konquest.getConfigManager().getConfig("global-events");
        if (eventsConfig.get("global-events") == null) {
            ChatUtil.printConsoleError("Failed to load any global events from global-events.yml! Check file permissions.");
            isEventDataNull = true;
            return;
        }
        boolean isEnabled;
        long startTime, durationTime, repetitionTime;
        List<String> eventEffects;
        KonGlobalEvent globalEvent;
        // Load all Events
        ConfigurationSection eventsSection = eventsConfig.getConfigurationSection("global-events");
        for(String eventName : eventsConfig.getConfigurationSection("global-events").getKeys(false)) {
            ConfigurationSection eventSection = eventsSection.getConfigurationSection(eventName);
            // Gather data
            isEnabled = eventSection.getBoolean("enabled");
            startTime = eventSection.getLong("start");
            durationTime = eventSection.getLong("duration");
            repetitionTime = eventSection.getLong("repetition");
            eventEffects = eventSection.getStringList("effects");
            // Create event
            globalEvent = new KonGlobalEvent(eventName);
            globalEvent.setEnabled(isEnabled);
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
                eventSection.set("start", event.getStartTime());
                eventSection.set("duration", event.getDurationTime());
                eventSection.set("repetition", event.getRepetitionTime());
                // Effects
                ArrayList<String> eventEffectNames = new ArrayList<>();
                for (KonGlobalEventEffect effect : KonGlobalEventEffect.values()) {
                    if (event.hasEffect(effect)) {
                        eventEffectNames.add(effect.toString());
                    }
                }
                eventSection.set("effects", eventEffectNames);
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
