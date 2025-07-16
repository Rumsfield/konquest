package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.KonGlobalEvent;
import com.github.rumsfield.konquest.model.KonGlobalEventEffect;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.HelperUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class EventCommand extends CommandBase {

    public EventCommand() {
        // Define name and sender support
        super("event",false, false);
        // Define arguments
        // None
        setOptionalArgs(true);
        // [menu]
        addArgument(
                newArg("menu",true,false)
        );
        // effects
        addArgument(
                newArg("effects",true,false)
        );
        // list [<page>]
        addArgument(
                newArg("list",false,true)
                        .sub( newArg("page",false,false) )
        );
        // info [<name>]
        addArgument(
                newArg("info",true,true)
                        .sub( newArg("name",false,false) )
        );
    }

    @Override
    public void execute(Konquest konquest, CommandSender sender, List<String> args) {
        final int MAX_LINES_PER_PAGE = 6;
        final String lineTemplate = ChatColor.GOLD+"> "+ChatColor.RESET+"%-20s -> "+ChatColor.AQUA+"%s";
        if(args.isEmpty()) {
            // Display menu (player only)
            KonPlayer player = konquest.getPlayerManager().getPlayer(sender);
            if (player == null) {
                sendInvalidSenderMessage(sender);
                return;
            }
            ChatUtil.sendMessage(sender,"TODO");
            //konquest.getDisplayManager().displayScoreMenu(player);
        } else {
            // Has arguments
            switch (args.get(0).toLowerCase()) {
                case "menu":
                    // Display menu
                    KonPlayer player = konquest.getPlayerManager().getPlayer(sender);
                    if (player == null) {
                        sendInvalidSenderMessage(sender);
                        return;
                    }
                    // TODO implement menu
                    ChatUtil.sendMessage(sender,"TODO");
                    //konquest.getDisplayManager().displayScoreMenu(player);
                    break;
                case "effects":
                    // Display all effect details
                    List<String> effectLines = new ArrayList<>();
                    int pos = 1;
                    for (KonGlobalEventEffect effect : KonGlobalEventEffect.values()) {
                        String effectTitle = effect.getTitle();
                        String effectDescription = effect.getDescription();
                        String isValid = String.valueOf(konquest.getGlobalEventManager().isEffectValid(effect));
                        String message = pos+") "+ChatColor.AQUA+isValid+" "+ChatColor.LIGHT_PURPLE+effectTitle+ChatColor.WHITE+" - "+effectDescription;
                        effectLines.add(message);
                        pos++;
                    }
                    ChatUtil.sendNotice(sender, "Global Event Effects");
                    for (String line : effectLines) {
                        ChatUtil.sendMessage(sender, "  "+line);
                    }
                    break;
                case "list":
                    // Show paged list of all events
                    // TODO message paths
                    // Get all events, sorted by next active from now
                    ArrayList<KonGlobalEvent> events = konquest.getGlobalEventManager().getSortedEvents();
                    // Check for any events
                    if (events.isEmpty()) {
                        //ChatUtil.sendNotice(sender, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
                        ChatUtil.sendNotice(sender, "No global events");
                        return;
                    }
                    // Make lines
                    List<String> lines = new ArrayList<>();
                    int linePos = 1;
                    for (KonGlobalEvent event : events) {
                        String message = linePos+") "+ChatColor.LIGHT_PURPLE+event.getName()+ChatColor.WHITE+" - Active: "+event.isActive();
                        lines.add(message);
                        linePos++;
                    }
                    // Max number of pages given help lines
                    int numLines = lines.size();
                    int maxPages = (int)Math.ceil(((double)numLines)/MAX_LINES_PER_PAGE);
                    // Get page index to display
                    int page = 1;
                    if (args.size() == 2) {
                        try {
                            page = Integer.parseInt(args.get(1));
                        }
                        catch (NumberFormatException ignored) {
                            ChatUtil.sendError(sender, MessagePath.COMMAND_HELP_ERROR_PAGE.getMessage());
                            sendInvalidArgMessage(sender);
                            return;
                        }
                        page = Math.max(page,1);
                        page = Math.min(page,maxPages);
                    }
                    // Display lines
                    String pageDisplay = page + "/" + maxPages;
                    ChatUtil.sendNotice(sender, "Global Events, page "+pageDisplay);
                    if (page == 1 && maxPages > 1) {
                        ChatUtil.sendNotice(sender, MessagePath.COMMAND_HELP_NOTICE_PAGE.getMessage());
                    }
                    ChatUtil.sendNotice(sender, MessagePath.COMMAND_HELP_NOTICE_DETAIL.getMessage());
                    int startIdx = (page-1) * MAX_LINES_PER_PAGE;
                    int endIdx = startIdx + MAX_LINES_PER_PAGE;
                    for (int i = startIdx; i < endIdx && i < numLines; i++) {
                        ChatUtil.sendMessage(sender, "  "+lines.get(i));
                    }
                    break;
                case "info":
                    if (args.size() == 1) {
                        // Show info about current active events and effects
                        ArrayList<KonGlobalEvent> activeEvents = konquest.getGlobalEventManager().getEvents(true);
                        KonGlobalEvent nextEvent = konquest.getGlobalEventManager().getNextEvent();
                        // Header
                        ChatUtil.sendNotice(sender, "Global Event Info");
                        // Active event
                        if (activeEvents.isEmpty()) {
                            ChatUtil.sendMessage(sender,"No active global events");
                        } else {
                            ChatUtil.sendMessage(sender, "Active Global Events", ChatColor.GOLD);
                            // Make list of enabled effects
                            ArrayList<String> enabledEffectNames = new ArrayList<>();
                            for (KonGlobalEventEffect effect : konquest.getGlobalEventManager().getValidEffects()) {
                                enabledEffectNames.add(effect.getTitle());
                            }
                            String effectListFormat = "None";
                            if (!enabledEffectNames.isEmpty()) {
                                effectListFormat = HelperUtil.formatCommaSeparatedList(enabledEffectNames);
                            }
                            // Show effect and event list info
                            ChatUtil.sendMessage(sender,String.format(lineTemplate,"Effects",effectListFormat));
                            int eventPos = 1;
                            for (KonGlobalEvent globalEvent : activeEvents) {
                                ChatUtil.sendMessage(sender,eventPos+") "+globalEvent.getName());
                                ChatUtil.sendMessage(sender,String.format(lineTemplate,"Enabled",globalEvent.isEnabled()));
                                ChatUtil.sendMessage(sender,String.format(lineTemplate,"End Date",globalEvent.getNextEndDateFormat()));
                                eventPos++;
                            }
                        }
                        // Next event
                        if (nextEvent == null) {
                            ChatUtil.sendMessage(sender, "No upcoming global events");
                        } else {
                            ChatUtil.sendMessage(sender, "Next Global Event", ChatColor.GOLD);
                            String [] nextEventInfo = {
                                    String.format(lineTemplate,"Name",nextEvent.getName()),
                                    String.format(lineTemplate,"Start Date",nextEvent.getNextStartDateFormat())
                            };
                            for (String line : nextEventInfo) {
                                ChatUtil.sendMessage(sender,line);
                            }
                        }
                    } else if (args.size() == 2) {
                        // Show detailed info about an event
                        String eventName = args.get(1);
                        KonGlobalEvent globalEvent = konquest.getGlobalEventManager().getEvent(eventName);
                        if (globalEvent == null) {
                            ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(eventName));
                            return;
                        }
                        // Get info
                        ArrayList<String> eventEffectNames = new ArrayList<>();
                        for (KonGlobalEventEffect effect : KonGlobalEventEffect.values()) {
                            if (globalEvent.hasEffect(effect)) {
                                eventEffectNames.add(effect.getTitle());
                            }
                        }
                        String effectListFormat = "None";
                        if (!eventEffectNames.isEmpty()) {
                            effectListFormat = HelperUtil.formatCommaSeparatedList(eventEffectNames);
                        }
                        // Display info
                        ChatUtil.sendNotice(sender, "Global Event Details");
                        String [] eventInfo = {
                                String.format(lineTemplate,"Name",globalEvent.getName()),
                                String.format(lineTemplate,"Enabled",globalEvent.isEnabled()),
                                String.format(lineTemplate,"Active",globalEvent.isActive()),
                                String.format(lineTemplate,"Duration Hours",globalEvent.getDurationHours()),
                                String.format(lineTemplate,"Repetition Days",globalEvent.getRepetitionDays()),
                                String.format(lineTemplate,"Start Date",globalEvent.getNextStartDateFormat()),
                                String.format(lineTemplate,"End Date",globalEvent.getNextEndDateFormat()),
                                String.format(lineTemplate,"Effects",effectListFormat)
                        };
                        for (String line : eventInfo) {
                            ChatUtil.sendMessage(sender,line);
                        }
                    } else {
                        sendInvalidArgMessage(sender);
                    }
                    break;
                default:
                    sendInvalidArgMessage(sender);
            }
        }
    }

    @Override
    public List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args) {
        List<String> tabList = new ArrayList<>();
        if(args.size() == 1) {
            tabList.add("menu");
            tabList.add("effects");
            tabList.add("list");
            tabList.add("info");
        } else if(args.size() == 2) {
            switch (args.get(0).toLowerCase()) {
                case "list":
                    tabList.add("#");
                    break;
                case "info":
                    tabList.addAll(konquest.getGlobalEventManager().getEventNames());
                    break;
            }
        }
        return matchLastArgToList(tabList,args);
    }
}
