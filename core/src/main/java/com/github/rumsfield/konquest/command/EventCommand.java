package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.KonGlobalEvent;
import com.github.rumsfield.konquest.model.KonGlobalEventEffect;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.HelperUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

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
        if (!konquest.getGlobalEventManager().isEnabled()) {
            ChatUtil.sendError(sender,MessagePath.GENERIC_ERROR_DISABLED.getMessage());
            return;
        }
        final int MAX_LINES_PER_PAGE = 6;
        final String messagePrefix = "> ";
        final String lineTemplate = ChatColor.GOLD+messagePrefix+ChatColor.YELLOW+"%s"+ChatColor.WHITE+" = "+ChatColor.AQUA+"%s";
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
                    for (KonGlobalEventEffect effect : KonGlobalEventEffect.values()) {
                        String effectTitle = effect.getTitle();
                        String effectDescription = effect.getDescription();
                        String validDisplayText;
                        if (konquest.getGlobalEventManager().isEffectValid(effect)) {
                            if (sender instanceof ConsoleCommandSender) {
                                validDisplayText = ChatColor.DARK_GREEN+"+"+ChatColor.LIGHT_PURPLE;
                            } else {
                                validDisplayText = DisplayManager.boolean2Symbol(true)+ChatColor.LIGHT_PURPLE;
                            }
                        } else {
                            if (sender instanceof ConsoleCommandSender) {
                                validDisplayText = ChatColor.DARK_RED+"-"+ChatColor.AQUA;
                            } else {
                                validDisplayText = DisplayManager.boolean2Symbol(false)+ChatColor.AQUA;
                            }
                        }
                        effectLines.add(validDisplayText+" "+effectTitle+ChatColor.WHITE+" - "+effectDescription);
                    }
                    ChatUtil.sendNotice(sender, MessagePath.COMMAND_EVENT_NOTICE_EFFECTS.getMessage());
                    for (String line : effectLines) {
                        ChatUtil.sendMessage(sender, messagePrefix+line, ChatColor.GOLD);
                    }
                    break;
                case "list":
                    // Show paged list of all events
                    // Get all events, sorted by next active from now
                    ArrayList<KonGlobalEvent> events = konquest.getGlobalEventManager().getSortedEvents();
                    // Check for any events
                    if (events.isEmpty()) {
                        ChatUtil.sendNotice(sender, MessagePath.COMMAND_EVENT_NOTICE_NO_EVENTS.getMessage());
                        return;
                    }
                    // Make lines
                    List<String> lines = new ArrayList<>();
                    int linePos = 1;
                    for (KonGlobalEvent event : events) {
                        ChatColor eventColor = ChatColor.AQUA;
                        String dateInfo;
                        if (event.isActive()) {
                            if (event.isEnabled()) {
                                eventColor = ChatColor.LIGHT_PURPLE;
                            } else {
                                eventColor = ChatColor.GRAY;
                            }
                            dateInfo = MessagePath.COMMAND_EVENT_NOTICE_ENDS.getMessage(event.getNextEndDateFormat());
                        } else {
                            dateInfo = MessagePath.COMMAND_EVENT_NOTICE_STARTS.getMessage(event.getNextStartDateFormat());
                        }
                        String message = ChatColor.YELLOW+""+linePos+") "+eventColor+event.getName()+" "+dateInfo;
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
                    ChatUtil.sendNotice(sender, MessagePath.COMMAND_EVENT_NOTICE_EVENTS.getMessage(pageDisplay));
                    if (page == 1 && maxPages > 1) {
                        ChatUtil.sendNotice(sender, MessagePath.COMMAND_EVENT_NOTICE_PAGE.getMessage());
                    }
                    int startIdx = (page-1) * MAX_LINES_PER_PAGE;
                    int endIdx = startIdx + MAX_LINES_PER_PAGE;
                    for (int i = startIdx; i < endIdx && i < numLines; i++) {
                        ChatUtil.sendMessage(sender, messagePrefix+lines.get(i), ChatColor.GOLD);
                    }
                    break;
                case "info":
                    if (args.size() == 1) {
                        // Show info about current active events and effects
                        ArrayList<KonGlobalEvent> activeEvents = konquest.getGlobalEventManager().getEvents(true);
                        KonGlobalEvent nextEvent = konquest.getGlobalEventManager().getNextEvent();
                        // Header
                        ChatUtil.sendNotice(sender, MessagePath.COMMAND_EVENT_NOTICE_INFO.getMessage());
                        // Active event
                        if (activeEvents.isEmpty()) {
                            ChatUtil.sendMessage(sender,ChatColor.GOLD+messagePrefix+ChatColor.YELLOW+MessagePath.COMMAND_EVENT_NOTICE_INFO_NO_ACTIVE.getMessage());
                        } else {
                            ChatUtil.sendMessage(sender, ChatColor.GOLD+messagePrefix+ChatColor.YELLOW+MessagePath.COMMAND_EVENT_NOTICE_INFO_EFFECTS.getMessage());
                            // Make list of enabled effects
                            ArrayList<String> enabledEffectNames = new ArrayList<>();
                            for (KonGlobalEventEffect effect : konquest.getGlobalEventManager().getValidEffects()) {
                                enabledEffectNames.add(effect.getTitle());
                            }
                            String effectListFormat = MessagePath.COMMAND_EVENT_PROPERTY_NONE.getMessage();
                            if (!enabledEffectNames.isEmpty()) {
                                effectListFormat = HelperUtil.formatCommaSeparatedList(enabledEffectNames);
                            }
                            // Show effect and event list info
                            ChatUtil.sendMessage(sender,"  "+effectListFormat, ChatColor.LIGHT_PURPLE);
                            ChatUtil.sendMessage(sender, ChatColor.GOLD+messagePrefix+ChatColor.YELLOW+MessagePath.COMMAND_EVENT_NOTICE_INFO_EVENTS.getMessage());
                            int eventPos = 1;
                            for (KonGlobalEvent globalEvent : activeEvents) {
                                if (globalEvent.isEnabled()) {
                                    ChatUtil.sendMessage(sender,"  "+eventPos+") "+ChatColor.LIGHT_PURPLE+globalEvent.getName()+" "+MessagePath.COMMAND_EVENT_NOTICE_ENDS.getMessage(globalEvent.getNextEndDateFormat()));
                                    eventPos++;
                                }
                            }
                        }
                        // Next event
                        if (nextEvent == null) {
                            ChatUtil.sendMessage(sender, ChatColor.GOLD+messagePrefix+ChatColor.YELLOW+MessagePath.COMMAND_EVENT_NOTICE_INFO_NO_UPCOMING.getMessage());
                        } else {
                            ChatUtil.sendMessage(sender, ChatColor.GOLD+messagePrefix+ChatColor.YELLOW+MessagePath.COMMAND_EVENT_NOTICE_INFO_NEXT.getMessage());
                            ChatUtil.sendMessage(sender,"  "+ChatColor.AQUA+nextEvent.getName()+" "+MessagePath.COMMAND_EVENT_NOTICE_STARTS.getMessage(nextEvent.getNextStartDateFormat()));
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
                        String effectListFormat = MessagePath.COMMAND_EVENT_PROPERTY_NONE.getMessage();
                        if (!eventEffectNames.isEmpty()) {
                            effectListFormat = HelperUtil.formatCommaSeparatedList(eventEffectNames);
                        }
                        // Display info
                        ChatUtil.sendNotice(sender, MessagePath.COMMAND_EVENT_NOTICE_DETAILS.getMessage());
                        String [] eventInfo = {
                                String.format(lineTemplate,MessagePath.COMMAND_EVENT_PROPERTY_NAME.getMessage(),globalEvent.getName()),
                                String.format(lineTemplate,MessagePath.COMMAND_EVENT_PROPERTY_ENABLED.getMessage(),globalEvent.isEnabled()),
                                String.format(lineTemplate,MessagePath.COMMAND_EVENT_PROPERTY_ACTIVE.getMessage(),globalEvent.isActive()),
                                String.format(lineTemplate,MessagePath.COMMAND_EVENT_PROPERTY_DURATION.getMessage(),String.format("%.2f",globalEvent.getDurationHours())),
                                String.format(lineTemplate,MessagePath.COMMAND_EVENT_PROPERTY_REPETITION.getMessage(),String.format("%.2f",globalEvent.getRepetitionDays())),
                                String.format(lineTemplate,MessagePath.COMMAND_EVENT_PROPERTY_START_INITIAL.getMessage(),globalEvent.getStartDateFormat()),
                                String.format(lineTemplate,MessagePath.COMMAND_EVENT_PROPERTY_START_NEXT.getMessage(),globalEvent.getNextStartDateFormat()),
                                String.format(lineTemplate,MessagePath.COMMAND_EVENT_PROPERTY_END_NEXT.getMessage(),globalEvent.getNextEndDateFormat()),
                                String.format(lineTemplate,MessagePath.COMMAND_EVENT_PROPERTY_EFFECTS.getMessage(),effectListFormat)
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
