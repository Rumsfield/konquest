package com.github.rumsfield.konquest.display;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.display.icon.InfoIcon;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public abstract class StateMenu {

    // Interfaces intended for enums of subclasses
    interface State {}
    interface Access {}

    protected final Konquest konquest;
    protected State currentState;
    protected Access menuAccess;
    protected final HashMap<State,DisplayMenu> views;
    protected final ArrayList<DisplayMenu> pages;
    protected int currentPage;

    protected final int MAX_ICONS_PER_PAGE 		= 45;

    public StateMenu(Konquest konquest, State initialState, Access initialAccess) {
        this.views = new HashMap<>();
        this.pages = new ArrayList<>();
        this.currentPage = 0;
        this.currentState = initialState;
        this.menuAccess = initialAccess;
        this.konquest = konquest;
    }

    // To be implemented by subclass.
    // This method populates pages with navigation icon buttons based on state context.
    abstract void refreshNavigationButtons(State context);

    protected int getTotalPages(int numElements) {
        int pageTotal = (int)Math.ceil(((double)numElements)/MAX_ICONS_PER_PAGE);
        pageTotal = Math.max(pageTotal,1);
        return pageTotal;
    }

    protected int getNumPageRows(int numElements, int pageIndex) {
        int numPageRows = (int)Math.ceil(((double)(numElements - pageIndex*MAX_ICONS_PER_PAGE))/9);
        numPageRows = Math.max(numPageRows,1);
        numPageRows = Math.min(numPageRows,5);
        return numPageRows;
    }

    protected DisplayMenu goPageBack() {
        DisplayMenu result;
        int newIndex = currentPage-1;
        if(newIndex >= 0) {
            currentPage = newIndex;
        }
        result = pages.get(currentPage);
        views.put(currentState, result);
        return result;
    }

    protected DisplayMenu goPageNext() {
        DisplayMenu result;
        int newIndex = currentPage+1;
        if(newIndex < pages.size()) {
            currentPage = newIndex;
        }
        result = pages.get(currentPage);
        views.put(currentState, result);
        return result;
    }

    protected InfoIcon navIconClose(int index) {
        return new InfoIcon(ChatColor.GOLD+ MessagePath.LABEL_CLOSE.getMessage(), Collections.emptyList(), Material.STRUCTURE_VOID,index,true);
    }

    protected InfoIcon navIconBack(int index) {
        return new InfoIcon(ChatColor.GOLD+MessagePath.LABEL_BACK.getMessage(),Collections.emptyList(),Material.ENDER_PEARL,index,true);
    }

    protected InfoIcon navIconNext(int index) {
        return new InfoIcon(ChatColor.GOLD+MessagePath.LABEL_NEXT.getMessage(),Collections.emptyList(),Material.ENDER_PEARL,index,true);
    }

    protected InfoIcon navIconEmpty(int index) {
        return new InfoIcon(" ",Collections.emptyList(),Material.GRAY_STAINED_GLASS_PANE,index,false);
    }

    protected InfoIcon navIconReturn(int index) {
        return new InfoIcon(ChatColor.GOLD+MessagePath.MENU_PLOTS_BUTTON_RETURN.getMessage(),Collections.emptyList(),Material.FIREWORK_ROCKET,index,true);
    }

    protected void playStatusSound(Player bukkitPlayer, boolean status) {
        if(status) {
            Konquest.playSuccessSound(bukkitPlayer);
        } else {
            Konquest.playFailSound(bukkitPlayer);
        }
    }
}
