package com.github.rumsfield.konquest.command.admin;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandBase;
import com.github.rumsfield.konquest.model.KonGlobalEvent;
import com.github.rumsfield.konquest.model.KonGlobalEventEffect;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.command.CommandSender;

import java.text.DateFormat;
import java.util.*;

public class EventAdminCommand extends CommandBase {

    public EventAdminCommand() {
        // Define name and sender support
        super("event",false, true);
        // Define arguments
        // menu
        addArgument(
                newArg("menu",true,false)
        );
        // create <name>
        addArgument(
                newArg("create",true,false)
                        .sub( newArg("name",false,false) )
        );
        // remove <name>
        addArgument(
                newArg("remove",true,false)
                        .sub( newArg("name",false,false) )
        );
        // start <name> [now]|[<year> <month> <day> [<hour>] [minute]]
        addArgument(
                newArg("start",true,false)
                        .sub( newArg("name",false,true)
                                .sub( newArg("now",true,false) )
                                .sub( newArg("year",false,false)
                                        .sub( newArg("month",false,false)
                                                .sub( newArg("day",false,true)
                                                        .sub( newArg("hour",false,true)
                                                            .sub( newArg("minute",false,false) ) ) ) ) ) )
        );
        // duration <name> days|hours|minutes <time>
        List<String> timeNames = Arrays.asList("days", "hours", "minutes");
        addArgument(
                newArg("duration",true,false)
                        .sub( newArg("name",false,false)
                                .sub( newArg(timeNames,true,false)
                                        .sub( newArg("time",false,false) ) ) )
        );
        // repetition <name> days|hours|minutes <time>
        addArgument(
                newArg("repetition",true,false)
                        .sub( newArg("name",false,false)
                                .sub( newArg(timeNames,true,false)
                                        .sub( newArg("time",false,false) ) ) )
        );
        // effect <name> add|remove <effect>
        List<String> argNames = Arrays.asList("add", "remove");
        addArgument(
                newArg("effect",true,false)
                        .sub( newArg("name",false,false)
                                .sub( newArg(argNames,true,false)
                                        .sub( newArg("effect",false,false) ) ) )
        );
        // enable <name> true|false
        List<String> valueNames = Arrays.asList("true", "false");
        addArgument(
                newArg("enable",true,false)
                        .sub( newArg("name",false,false)
                                .sub( newArg(valueNames,true,false) ) )
        );
    }

    @Override
    public void execute(Konquest konquest, CommandSender sender, List<String> args) {
        // Parse arguments
        if (args.isEmpty() || (args.size() == 1 && args.get(0).equalsIgnoreCase("menu"))) {
            // Menu
            // TODO implement admin event menu
            ChatUtil.sendMessage(sender,"TODO");

        } else if (args.size() >= 2) {
            // Other commands
            String cmdName = args.get(0);
            String eventName = args.get(1);

            // Validate event name
            if (!cmdName.equalsIgnoreCase("create")) {
                if (!konquest.getGlobalEventManager().isEvent(eventName)) {
                    ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(eventName));
                    return;
                }
            }

            // Execute sub-commands
            switch (cmdName) {
                case "create":
                    if (args.size() != 2) {
                        sendInvalidArgMessage(sender);
                        return;
                    }
                    // Validate name
                    if (konquest.validateName(eventName,sender) != 0) {
                        // Method includes status messages
                        return;
                    }
                    // Create a new event
                    boolean createEnabled = true; // start enabled
                    long createStart = 0; // no start
                    long createDuration = 60 * 60 * 1000; // 1 hour in ms
                    long createRepetition = 0; // no repetition
                    boolean status = konquest.getGlobalEventManager().createEvent(eventName,createEnabled,createStart,createDuration,createRepetition,Collections.emptyList());
                    if (status) {
                        // Successfully created event, prompt to edit options
                        ChatUtil.sendNotice(sender, "Successfully created event "+eventName+", use this command again to modify and enable the event.");
                    } else {
                        // Failed, likely due to bad name
                        ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_TAKEN_NAME.getMessage());
                        return;
                    }
                    break;

                case "remove":
                    // Remove an existing event
                    if (args.size() != 2) {
                        sendInvalidArgMessage(sender);
                        return;
                    }
                    konquest.getGlobalEventManager().cancelEvent(eventName);
                    ChatUtil.sendNotice(sender, "Successfully removed event "+eventName+".");
                    break;

                case "start":
                    // Set an event's start time
                    if (args.size() == 3 && args.get(2).equalsIgnoreCase("now")) {
                        // Start event now
                        konquest.getGlobalEventManager().startEvent(eventName);
                        ChatUtil.sendNotice(sender, "Successfully started event "+eventName+" now.");
                    } else if (args.size() == 5 || args.size() == 6 || args.size() == 7) {
                        // Start at given date
                        int startYear = 0;
                        int startMonth = 1;
                        int startDay = 1;
                        int startHour = 0;
                        int startMinute = 0;
                        try {
                            startYear = Integer.parseInt(args.get(2)); // 2000 - ?
                            startMonth = Integer.parseInt(args.get(3)); // 1 - 12
                            startDay = Integer.parseInt(args.get(4)); // 1 - 31
                            if (args.size() >= 6) {
                                startHour = Integer.parseInt(args.get(5)); // 0 - 23
                            }
                            if (args.size() == 7) {
                                startMinute = Integer.parseInt(args.get(6)); // 0 - 59
                            }

                        } catch(NumberFormatException e) {
                            ChatUtil.sendError(sender, "Incorrect number format: "+e.getMessage());
                            return;
                        }
                        if (startYear < 2000) {
                            ChatUtil.sendError(sender, "Year must be greater than 2000.");
                            return;
                        }
                        if (startMonth < 1 || startMonth > 12) {
                            ChatUtil.sendError(sender, "Month must be between 1 (January) and 12 (December).");
                            return;
                        }
                        if (startDay < 1 || startDay > 31) {
                            ChatUtil.sendError(sender, "Day must be between 1 and 31.");
                            return;
                        }
                        if (startHour < 0 || startHour > 23) {
                            ChatUtil.sendError(sender, "Hour must be between 0 and 23.");
                            return;
                        }
                        if (startMinute < 0 || startMinute > 59) {
                            ChatUtil.sendError(sender, "Minute must be between 0 and 59.");
                            return;
                        }
                        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT);
                        try {
                            df.getCalendar().set(Calendar.YEAR, startYear);
                            df.getCalendar().set(Calendar.MONTH, startMonth-1);
                            df.getCalendar().set(Calendar.DAY_OF_MONTH, startDay);
                            df.getCalendar().set(Calendar.HOUR_OF_DAY, startHour);
                            df.getCalendar().set(Calendar.MINUTE, startMinute);
                        } catch (ArrayIndexOutOfBoundsException exc) {
                            ChatUtil.sendError(sender, "Incorrect date format: "+exc.getMessage());
                            return;
                        }
                        Date startDate = df.getCalendar().getTime();
                        konquest.getGlobalEventManager().modifyEventStart(eventName,startDate);
                        ChatUtil.sendNotice(sender, "Successfully set event start on "+df.format(startDate));
                    } else {
                        sendInvalidArgMessage(sender);
                        return;
                    }
                    break;

                case "duration":
                case "repetition":
                    // Set an event's duration or repetition time
                    if (args.size() != 4) {
                        sendInvalidArgMessage(sender);
                        return;
                    }
                    String resolution = args.get(2);
                    double timeValue;
                    try {
                        timeValue = Double.parseDouble(args.get(3));
                    } catch(NumberFormatException e) {
                        ChatUtil.sendError(sender, "Incorrect number format: "+e.getMessage());
                        return;
                    }
                    long timeDuration;
                    if (resolution.equalsIgnoreCase("days")) {
                        timeDuration = (long)(timeValue * 24 * 60 * 60 * 1000);
                    } else if (resolution.equalsIgnoreCase("hours")) {
                        timeDuration = (long)(timeValue * 60 * 60 * 1000);
                    } else if (resolution.equalsIgnoreCase("minutes")) {
                        timeDuration = (long)(timeValue * 60 * 1000);
                    } else {
                        sendInvalidArgMessage(sender);
                        return;
                    }
                    if (cmdName.equalsIgnoreCase("duration")) {
                        konquest.getGlobalEventManager().modifyEventDuration(eventName,timeDuration);
                        ChatUtil.sendNotice(sender, "Successfully set event duration to "+timeValue+" "+resolution);
                    } else {
                        konquest.getGlobalEventManager().modifyEventRepetition(eventName,timeDuration);
                        ChatUtil.sendNotice(sender, "Successfully set event repetition to "+timeValue+" "+resolution);
                    }
                    break;

                case "effect":
                    // Add or remove an effect from an event
                    if (args.size() != 4) {
                        sendInvalidArgMessage(sender);
                        return;
                    }
                    String effectMode = args.get(2);
                    String effectName = args.get(3);
                    // Check effect name
                    KonGlobalEventEffect eventEffect = KonGlobalEventEffect.getEffect(effectName);
                    if (eventEffect == null) {
                        ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(effectName));
                        return;
                    }
                    KonGlobalEvent event = konquest.getGlobalEventManager().getEvent(eventName);
                    assert event != null;
                    boolean effectChangeStatus;
                    if (effectMode.equalsIgnoreCase("add")) {
                        effectChangeStatus = event.addEffect(eventEffect);
                        if (effectChangeStatus) {
                            // Successfully added
                            konquest.getGlobalEventManager().refreshDelayedEvents();
                            ChatUtil.sendNotice(sender, "Successfully added effect "+eventEffect.getTitle()+" to event "+eventName);
                        } else {
                            // Failed to add, other effects conflict
                            KonGlobalEventEffect conflictEffect = event.getConflictEffect(eventEffect);
                            String conflictEffectTitle = conflictEffect == null ? "unknown" : conflictEffect.getTitle();
                            ChatUtil.sendError(sender, "Failed, the event has a conflicting effect - "+conflictEffectTitle);
                        }
                    } else if (effectMode.equalsIgnoreCase("remove")) {
                        effectChangeStatus = event.removeEffect(eventEffect);
                        if (effectChangeStatus) {
                            // Successfully removed
                            konquest.getGlobalEventManager().refreshDelayedEvents();
                            ChatUtil.sendNotice(sender, "Successfully removed effect "+eventEffect.getTitle()+" from event "+eventName);
                        } else {
                            // Failed to remove, event does not contain this effect
                            ChatUtil.sendError(sender, "Failed, the event does not contain this effect.");
                        }
                    } else {
                        sendInvalidArgMessage(sender);
                        return;
                    }
                    break;

                case "enable":
                    // Enable or disable an event
                    if (args.size() != 3) {
                        sendInvalidArgMessage(sender);
                        return;
                    }
                    String enableValue = args.get(2);
                    if (enableValue.equalsIgnoreCase("true")) {
                        konquest.getGlobalEventManager().enableEvent(eventName,true);
                        ChatUtil.sendNotice(sender, "Enabled global event "+eventName);
                    } else if (enableValue.equalsIgnoreCase("false")) {
                        konquest.getGlobalEventManager().enableEvent(eventName,false);
                        ChatUtil.sendNotice(sender, "Disable global event "+eventName);
                    } else {
                        sendInvalidArgMessage(sender);
                        return;
                    }
                    break;

                default:
                    sendInvalidArgMessage(sender);
            }
        } else {
            sendInvalidArgMessage(sender);
        }
    }

    @Override
    public List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args) {
        List<String> tabList = new ArrayList<>();
        if (args.size() == 1) {
            // Suggest sub-commands
            tabList.add("menu");
            tabList.add("create");
            tabList.add("remove");
            tabList.add("start");
            tabList.add("duration");
            tabList.add("repetition");
            tabList.add("effect");
            tabList.add("enable");
        } else if (args.size() == 2) {
            // Suggest names
            switch(args.get(0).toLowerCase()) {
                case "create":
                    tabList.add("***");
                    break;
                case "remove":
                case "start":
                case "duration":
                case "repetition":
                case "effect":
                case "enable":
                    tabList.addAll(konquest.getGlobalEventManager().getEventNames());
                    break;
                default:
                    break;
            }
        } else if (args.size() == 3) {
            switch(args.get(0).toLowerCase()) {
                case "start":
                    tabList.add("now");
                    tabList.add("YEAR");
                    break;
                case "duration":
                case "repetition":
                    tabList.add("days");
                    tabList.add("hours");
                    tabList.add("minutes");
                    break;
                case "effect":
                    tabList.add("add");
                    tabList.add("remove");
                    break;
                case "enable":
                    tabList.add("true");
                    tabList.add("false");
                    break;
                default:
                    break;
            }
        } else if (args.size() == 4) {
            switch(args.get(0).toLowerCase()) {
                case "start":
                    if (!args.get(2).equalsIgnoreCase("now")) {
                        tabList.add("MONTH");
                    }
                    break;
                case "duration":
                case "repetition":
                    tabList.add("#");
                    break;
                case "effect":
                    for (KonGlobalEventEffect effect : KonGlobalEventEffect.values()) {
                        tabList.add(effect.toString());
                    }
                    break;
                default:
                    break;
            }
        } else if (args.size() >= 5 && args.get(0).equalsIgnoreCase("start") && !args.get(2).equalsIgnoreCase("now")) {
            if (args.size() == 5) {
                tabList.add("DAY");
            } else if (args.size() == 6) {
                tabList.add("HOUR");
            } else if (args.size() == 7) {
                tabList.add("MINUTE");
            }
        }
        return matchLastArgToList(tabList,args);
    }
}
