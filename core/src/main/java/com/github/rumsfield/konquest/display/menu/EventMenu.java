package com.github.rumsfield.konquest.display.menu;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.display.DisplayView;
import com.github.rumsfield.konquest.display.StateMenu;
import com.github.rumsfield.konquest.display.icon.*;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.HelperUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Material;

import java.text.DateFormat;
import java.util.*;

public class EventMenu extends StateMenu {

    enum MenuState implements State {
        ROOT,
        CALENDAR,
        EVENT_LIST_ALL,
        EVENT_LIST_DAY,
        EFFECT_LIST
    }

    private final KonPlayer player;
    private Date listDate;
    private Calendar calendarView;

    public EventMenu(Konquest konquest, KonPlayer player) {
        super(konquest, MenuState.ROOT, null);
        this.player = player;
        this.listDate = null;
        this.calendarView = null;

        /* Initialize menu view */
        setCurrentView(MenuState.ROOT);

    }

    /**
     * Creates the root menu view for this menu.
     * This is a single page view.
     */
    private DisplayView createRootView() {

        // Check for enabled feature
        if (!getKonquest().getGlobalEventManager().isEnabled()) {
            ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_DISABLED.getMessage());
            return null;
        }

        DisplayView result;
        MenuIcon icon;

        /* Icon slot indexes */
        int rows = 1;
        // Row 0: 0  1  2  3  4  5  6  7  8
        int SLOT_ACTIVE_INFO = 0;
        int SLOT_NEXT_INFO = 2;
        int SLOT_CALENDAR = 4;
        int SLOT_LIST_EVENTS = 6;
        int SLOT_LIST_EFFECTS = 8;

        result = new DisplayView(rows, getTitle(MenuState.ROOT));
        int numEvents = getKonquest().getGlobalEventManager().getEventNames().size();
        int numEffects = KonGlobalEventEffect.values().length;
        ArrayList<KonGlobalEvent> activeEvents = getKonquest().getGlobalEventManager().getEvents(true);
        KonGlobalEvent nextEvent = getKonquest().getGlobalEventManager().getNextEvent();

        /* Active Info Icon */
        icon = new InfoIcon(MessagePath.COMMAND_EVENT_NOTICE_INFO.getMessage(), Material.OAK_SIGN, SLOT_ACTIVE_INFO, false);
        if (activeEvents.isEmpty()) {
            // No active events
            icon.addAlert(MessagePath.COMMAND_EVENT_NOTICE_INFO_NO_ACTIVE.getMessage());
        } else {
            // Active effects
            icon.addDescription(MessagePath.COMMAND_EVENT_NOTICE_INFO_EFFECTS.getMessage());
            ArrayList<String> enabledEffectNames = new ArrayList<>();
            for (KonGlobalEventEffect effect : getKonquest().getGlobalEventManager().getValidEffects()) {
                enabledEffectNames.add(effect.getTitle());
            }
            String effectListFormat = MessagePath.LABEL_NONE.getMessage();
            if (!enabledEffectNames.isEmpty()) {
                effectListFormat = HelperUtil.formatCommaSeparatedList(enabledEffectNames);
            }
            icon.addDescription(effectListFormat, DisplayManager.valueFormat);
            // Active events
            icon.addDescription(MessagePath.COMMAND_EVENT_NOTICE_INFO_EVENTS.getMessage());
            int eventPos = 1;
            for (KonGlobalEvent globalEvent : activeEvents) {
                if (globalEvent.isEnabled()) {
                    String eventDescription = eventPos+") "+globalEvent.getName()+" "+MessagePath.COMMAND_EVENT_NOTICE_ENDS.getMessage(globalEvent.getNextEndDateFormat());
                    icon.addDescriptionSingle(DisplayManager.valueFormat+eventDescription);
                    eventPos++;
                }
            }
        }
        result.addIcon(icon);

        /* Next Info Icon */
        icon = new InfoIcon(MessagePath.COMMAND_EVENT_NOTICE_INFO_NEXT.getMessage(), Material.SPRUCE_SIGN, SLOT_NEXT_INFO, false);
        if (nextEvent == null) {
            // No next event
            icon.addAlert(MessagePath.COMMAND_EVENT_NOTICE_INFO_NO_UPCOMING.getMessage());
        } else {
            // Next event
            String eventDescription = nextEvent.getName()+" "+MessagePath.COMMAND_EVENT_NOTICE_STARTS.getMessage(nextEvent.getNextStartDateFormat());
            icon.addDescriptionSingle(DisplayManager.valueFormat+eventDescription);
        }
        result.addIcon(icon);

        /* Calendar Icon */
        icon = new InfoIcon(MessagePath.MENU_EVENT_CALENDAR.getMessage(), Material.DAYLIGHT_DETECTOR, SLOT_CALENDAR, true);
        icon.addDescription(MessagePath.MENU_EVENT_DESCRIPTION_CALENDAR.getMessage());
        icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
        icon.setState(MenuState.CALENDAR);
        result.addIcon(icon);

        /* Event List Icon */
        icon = new InfoIcon(MessagePath.MENU_EVENT_EVENTS.getMessage(), Material.ORANGE_BANNER, SLOT_LIST_EVENTS, true);
        icon.addDescription(MessagePath.MENU_EVENT_DESCRIPTION_EVENTS.getMessage());
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(),numEvents);
        icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
        icon.setState(MenuState.EVENT_LIST_ALL);
        result.addIcon(icon);

        /* Effect List Icon */
        icon = new InfoIcon(MessagePath.MENU_EVENT_EFFECTS.getMessage(), Material.PURPLE_BANNER, SLOT_LIST_EFFECTS, true);
        icon.addDescription(MessagePath.MENU_EVENT_DESCRIPTION_EFFECTS.getMessage());
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(),numEffects);
        icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
        icon.setState(MenuState.EFFECT_LIST);
        result.addIcon(icon);

        /* Navigation */
        addNavEmpty(result);
        addNavClose(result);
        addNavHome(result);

        return result;
    }

    /**
     * Creates the event list view.
     * This can be a multiple paged view.
     * Contexts: EVENT_LIST_ALL, EVENT_LIST_DAY
     */
    private List<DisplayView> createEventView(MenuState context) {
        // List events by day or all of them
        ArrayList<MenuIcon> icons = new ArrayList<>();
        List<KonGlobalEvent> events = new ArrayList<>();
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.LONG);

        // Determine list of towns given context
        String viewTitle = getTitle(context);
        switch (context) {
            case EVENT_LIST_ALL:
                // List of all events
                events.addAll(getKonquest().getGlobalEventManager().getEvents(false));
                break;
            case EVENT_LIST_DAY:
                // List of events on the given day
                if (listDate != null) {
                    events.addAll(getKonquest().getGlobalEventManager().getEventsOnDay(listDate));
                    viewTitle = viewTitle+" "+dateFormat.format(listDate);
                }
                break;
            default:
                return Collections.emptyList();
        }

        // Sort by initial start date
        events.sort(eventStartComparator);

        /* Event Icons */
        MenuIcon icon;
        for (KonGlobalEvent currentEvent : events) {
            // Make icon
            Material iconMat = currentEvent.isActive() ? Material.BEACON : Material.GLASS;
            icon = new InfoIcon(currentEvent.getName(),iconMat,0,false);
            icon.addProperty(MessagePath.MENU_EVENT_GLOBAL_EVENT.getMessage());
            if (currentEvent.isActive()) {
                icon.addAlert(MessagePath.LABEL_ACTIVE.getMessage());
            }
            if (!currentEvent.isEnabled()) {
                icon.addAlert(MessagePath.LABEL_DISABLED.getMessage());
            }
            // Property Details
            icon.addNameValue(MessagePath.COMMAND_EVENT_PROPERTY_DURATION.getMessage(), String.format("%.2f",currentEvent.getDurationHours()));
            icon.addNameValue(MessagePath.COMMAND_EVENT_PROPERTY_REPETITION.getMessage(), String.format("%.2f",currentEvent.getRepetitionDays()));
            icon.addNameValue(MessagePath.COMMAND_EVENT_PROPERTY_START_INITIAL.getMessage(), currentEvent.getStartDateFormat());
            icon.addNameValue(MessagePath.COMMAND_EVENT_PROPERTY_START_NEXT.getMessage(), currentEvent.getNextStartDateFormat());
            icon.addNameValue(MessagePath.COMMAND_EVENT_PROPERTY_END_NEXT.getMessage(), currentEvent.getNextEndDateFormat());
            // Effect Descriptions
            icon.addDescription(MessagePath.MENU_EVENT_EFFECTS.getMessage(),DisplayManager.nameFormat);
            int effectPos = 1;
            for (KonGlobalEventEffect currentEffect : currentEvent.getEffects()) {
                icon.addDescription(effectPos+") "+currentEffect.getTitle()+" > "+currentEffect.getDescription(), DisplayManager.valueFormat);
                effectPos++;
            }
            icons.add(icon);
        }

        /* Make Pages (includes navigation) */
        return new ArrayList<>(makePages(icons, viewTitle));
    }

    /**
     * Creates the effect list view.
     * This can be a multiple paged view.
     */
    private List<DisplayView> createEffectView() {
        ArrayList<MenuIcon> icons = new ArrayList<>();

        /* Effect Icons */
        MenuIcon icon;
        for (KonGlobalEventEffect currentEffect : KonGlobalEventEffect.values()) {
            // Make icon
            boolean isValid = getKonquest().getGlobalEventManager().isEffectValid(currentEffect);
            Material iconMat = isValid ? Material.EXPERIENCE_BOTTLE : Material.GLASS_BOTTLE;
            icon = new InfoIcon(currentEffect.getTitle(),iconMat,0,false);
            icon.addProperty(MessagePath.MENU_EVENT_GLOBAL_EFFECT.getMessage());
            if (isValid) {
                icon.addAlert(MessagePath.LABEL_ACTIVE.getMessage());
            }
            // Effect Description
            icon.addDescription(currentEffect.getDescription());
            icons.add(icon);
        }

        /* Make Pages (includes navigation) */
        return new ArrayList<>(makePages(icons, getTitle(MenuState.EFFECT_LIST)));
    }

    /**
     * Creates the calendar view.
     * This can be a multiple paged view.
     */
    private List<DisplayView> createCalendarView() {
        ArrayList<DisplayView> result = new ArrayList<>();
        if (calendarView == null) return result;

        // Today
        Calendar nowCal = Calendar.getInstance();

        // View month
        String monthName = calendarView.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
        String yearFormat = ""+calendarView.get(Calendar.YEAR);
        int numDaysInMonth = calendarView.getActualMaximum(Calendar.DAY_OF_MONTH);

        DisplayView page = new DisplayView(5, getTitle(MenuState.CALENDAR) + " - " + monthName + " " + yearFormat);

        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.LONG);
        Calendar dateCal = dateFormat.getCalendar();
        dateCal.set(Calendar.YEAR,calendarView.get(Calendar.YEAR));
        dateCal.set(Calendar.MONTH,calendarView.get(Calendar.MONTH));
        dateCal.set(Calendar.HOUR,0);
        dateCal.set(Calendar.MINUTE,0);
        dateCal.set(Calendar.SECOND,0);
        dateCal.set(Calendar.MILLISECOND,0);

        // Iterate over all days in month
        DateIcon icon;
        int iconIndex = 1;
        for (int n = 1; n <= numDaysInMonth; n++) {
            // Update date
            dateCal.set(Calendar.DAY_OF_MONTH,n);
            Date day = dateCal.getTime();
            String dayFormat = dateFormat.format(day);
            String dayName = dateCal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
            boolean isToday = dateCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                    dateCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR);
            // Get events on this day
            ArrayList<KonGlobalEvent> dayEvents = getKonquest().getGlobalEventManager().getEventsOnDay(day);
            boolean isClickable = !dayEvents.isEmpty();
            Material iconMat = isClickable ? Material.GREEN_TERRACOTTA : Material.GRAY_TERRACOTTA;
            // Make icon
            icon = new DateIcon(dayName,day,iconMat,iconIndex,isToday,isClickable);
            if (isToday) {
                icon.addProperty(MessagePath.MENU_EVENT_TODAY.getMessage());
            }
            icon.addNameValue(MessagePath.LABEL_DATE.getMessage(),dayFormat);
            if (isClickable) {
                icon.addDescription(MessagePath.MENU_EVENT_EVENTS.getMessage(),DisplayManager.nameFormat);
                int eventPos = 1;
                for (KonGlobalEvent dayEvent : dayEvents) {
                    icon.addDescription(eventPos+") "+dayEvent.getName(),DisplayManager.valueFormat);
                    eventPos++;
                }
                icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
            } else {
                // No active events
                icon.addAlert(MessagePath.COMMAND_EVENT_NOTICE_INFO_NO_ACTIVE.getMessage());
            }
            icon.setState(MenuState.EVENT_LIST_DAY);
            // Advance index
            if (Math.floorMod(iconIndex+2, 9) == 0) {
                iconIndex += 3;
            } else {
                iconIndex += 1;
            }
            // Add icon to page
            page.addIcon(icon);
        }

        /* Navigation */
        addNavEmpty(page);
        addNavClose(page);
        addNavReturn(page);
        addNavBack(page);
        addNavNext(page);

        result.add(page);

        return result;
    }

    /**
     * Create a list of views for a given menu state.
     * This creates new views, specific to each menu.
     *
     * @param context The menu state for the corresponding view
     * @return The list of menu views to be displayed to the player
     */
    @Override
    public ArrayList<DisplayView> createView(State context) {
        ArrayList<DisplayView> result = new ArrayList<>();
        MenuState currentState = (MenuState)context;
        switch (currentState) {
            case ROOT:
                result.add(createRootView());
                break;
            case EVENT_LIST_ALL:
            case EVENT_LIST_DAY:
                result.addAll(createEventView(currentState));
                break;
            case EFFECT_LIST:
                result.addAll(createEffectView());
                break;
            case CALENDAR:
                result.addAll(createCalendarView());
                break;
            default:
                break;
        }
        return result;
    }

    /**
     * Change the menu's state based on the clicked inventory slot and type of click (right or left mouse).
     * Assume a clickable icon was clicked and visible to the player.
     * Returning a null value will close the menu.
     *
     * @param slot      The inventory slot of the current view that was clicked
     * @param clickType The type of click, true for left-click, false for right click
     * @return The new view state of the menu, or null to close the menu
     */
    @Override
    public DisplayView updateState(int slot, boolean clickType) {
        DisplayView result = null;
        MenuState currentState = (MenuState)getCurrentState();
        if (currentState == null) return null;
        if (isCurrentNavSlot(slot)) {
            // Clicked in navigation bar
            if (isNavClose(slot)) {
                // Close the menu by returning a null view
                return null;
            } else if (isNavHome(slot)) {
                // Go to main menu
                getKonquest().getDisplayManager().displayMainMenu(player);
            }  else if (isNavReturn(slot)) {
                // Return to previous
                if (currentState == MenuState.EVENT_LIST_DAY) {
                    listDate = null;
                    result = setCurrentView(MenuState.CALENDAR);
                } else {
                    // Return to root
                    result = refreshNewView(MenuState.ROOT);
                }
            } else if (isNavBack(slot)) {
                if (currentState == MenuState.CALENDAR) {
                    // Previous month
                    previousMonth();
                    result = refreshNewView(MenuState.CALENDAR);
                } else {
                    // Page back
                    result = goPageBack();
                }
            } else if (isNavNext(slot)) {
                if (currentState == MenuState.CALENDAR) {
                    // Next month
                    nextMonth();
                    result = refreshNewView(MenuState.CALENDAR);
                } else {
                    // Page next
                    result = goPageNext();
                }
            }
        } else if (isCurrentMenuSlot(slot)) {
            // Clicked in menu
            DisplayView view = getCurrentView();
            if (view == null) return null;
            MenuIcon clickedIcon = view.getIcon(slot);
            MenuState nextState = (MenuState)clickedIcon.getState(); // could be null in some states
            // State logic
            switch (currentState) {
                case ROOT:
                    // Root view, use stored icon state
                    if (nextState == null) return null;
                    switch (nextState) {
                        case CALENDAR:
                            // Initialize calendar
                            calendarView = Calendar.getInstance();
                            result = refreshNewView(nextState);
                            break;
                        case EFFECT_LIST:
                        case EVENT_LIST_ALL:
                            // Go to next state as defined by icon
                            result = refreshNewView(nextState);
                            break;
                    }
                    break;
                case CALENDAR:
                    // Display events on clicked day
                    if(clickedIcon instanceof DateIcon) {
                        DateIcon icon = (DateIcon)clickedIcon;
                        listDate = icon.getDate();
                        if (listDate == null) {
                            ChatUtil.printDebug("Failed to get null calendar date for event list");
                        }
                        result = refreshNewView(MenuState.EVENT_LIST_DAY);
                    }
                    break;
                default:
                    break;
            }
        }
        return result;
    }

    private String getTitle(MenuState context) {
        String result = "error";
        switch (context) {
            case ROOT:
                result = MessagePath.MENU_EVENT_TITLE.getMessage();
                break;
            case CALENDAR:
                result = MessagePath.MENU_EVENT_CALENDAR.getMessage();
                break;
            case EVENT_LIST_ALL:
            case EVENT_LIST_DAY:
                result = MessagePath.MENU_EVENT_EVENTS.getMessage();
                break;
            case EFFECT_LIST:
                result = MessagePath.MENU_EVENT_EFFECTS.getMessage();
                break;
            default:
                break;
        }
        return result;
    }

    private void nextMonth() {
        if (calendarView == null) return;
        calendarView.add(Calendar.MONTH,1);
    }
    private void previousMonth() {
        if (calendarView == null) return;
        calendarView.add(Calendar.MONTH,-1);
    }
}
