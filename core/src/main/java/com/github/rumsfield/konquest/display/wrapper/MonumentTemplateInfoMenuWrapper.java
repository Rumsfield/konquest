package com.github.rumsfield.konquest.display.wrapper;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandType;
import com.github.rumsfield.konquest.display.icon.CommandIcon;
import com.github.rumsfield.konquest.display.icon.InfoIcon;
import com.github.rumsfield.konquest.display.icon.MenuIcon;
import com.github.rumsfield.konquest.display.icon.TemplateIcon;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.KonKingdom;
import com.github.rumsfield.konquest.model.KonMonumentTemplate;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.model.KonSanctuary;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.Labeler;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

public class MonumentTemplateInfoMenuWrapper extends MenuWrapper {


    public MonumentTemplateInfoMenuWrapper(Konquest konquest) {
        super(konquest);
    }

    @Override
    public void constructMenu() {

        String pageLabel;
        List<String> loreList;
        final int MAX_ICONS_PER_PAGE = 45;
        int pageTotal;

        String titleColor = DisplayManager.titleFormat;
        String loreColor = DisplayManager.loreFormat;
        String valueColor = DisplayManager.valueFormat;

        /*
         * Display an icon for each valid monument template. Show:
         * - Name
         * - Associated sanctuary
         * - Number of kingdoms using it
         * - Cost
         * - Critical blocks
         * - Loot chests
         */
        // Pre-fetch template info maps
        HashMap<KonMonumentTemplate,String> templateSanctuaryMap = new HashMap<KonMonumentTemplate,String>();
        HashMap<KonMonumentTemplate,Integer> templateUsedKingdomsMap = new HashMap<KonMonumentTemplate,Integer>();
        ArrayList<KonMonumentTemplate> allTemplates = new ArrayList<>();
        for(KonSanctuary sanctuary : getKonquest().getSanctuaryManager().getSanctuaries()) {
            for(KonMonumentTemplate template : sanctuary.getTemplates()) {
                allTemplates.add(template);
                templateSanctuaryMap.put(template,sanctuary.getName());
                // Count kingdoms using this template
                int count = 0;
                for(KonKingdom kingdom : getKonquest().getKingdomManager().getKingdoms()) {
                    KonMonumentTemplate kingdomTemplate = kingdom.getMonumentTemplate();
                    if(kingdomTemplate != null && kingdomTemplate.equals(template)) {
                        count++;
                    }
                }
                templateUsedKingdomsMap.put(template,count);
            }
        }

        // Page 0+
        int pageNum = 0;
        pageTotal = (int)Math.ceil(((double)allTemplates.size())/MAX_ICONS_PER_PAGE);
        if(pageTotal == 0) {
            pageTotal = 1;
        }
        ListIterator<KonMonumentTemplate> templateIter = allTemplates.listIterator();
        for(int i = 0; i < pageTotal; i++) {
            int numPageRows = (int)Math.ceil(((double)(allTemplates.size() - i*MAX_ICONS_PER_PAGE))/9);
            if(numPageRows < 1) {
                numPageRows = 1;
            } else if(numPageRows > 5) {
                numPageRows = 5;
            }
            pageLabel = titleColor+ MessagePath.LABEL_MONUMENT.getMessage()+" "+(i+1)+"/"+pageTotal;
            getMenu().addPage(pageNum, numPageRows, pageLabel);
            int slotIndex = 0;
            while(slotIndex < MAX_ICONS_PER_PAGE && templateIter.hasNext()) {
                /* Template Icon (n) */
                KonMonumentTemplate currentTemplate = templateIter.next();
                double totalCost = getKonquest().getKingdomManager().getCostTemplate()+currentTemplate.getCost();
                loreList = new ArrayList<>();
                loreList.add(loreColor+MessagePath.LABEL_SANCTUARY.getMessage()+": "+valueColor+templateSanctuaryMap.get(currentTemplate));
                loreList.add(loreColor+MessagePath.LABEL_KINGDOMS.getMessage()+": "+valueColor+templateUsedKingdomsMap.get(currentTemplate));
                loreList.add(loreColor+MessagePath.LABEL_COST.getMessage()+": "+valueColor+totalCost);
                TemplateIcon templateIcon = new TemplateIcon(currentTemplate,""+ChatColor.GOLD,loreList,slotIndex,true);
                getMenu().getPage(pageNum).addIcon(templateIcon);
                slotIndex++;
            }
            pageNum++;
        }

        getMenu().refreshNavigationButtons();
        getMenu().setPageIndex(0);
    }

    @Override
    public void onIconClick(KonPlayer clickPlayer, MenuIcon clickedIcon) {
        Player bukkitPlayer = clickPlayer.getBukkitPlayer();
        if(clickedIcon instanceof TemplateIcon) {
            // Template Icons close the GUI and print a command in chat
            TemplateIcon icon = (TemplateIcon)clickedIcon;
            KonMonumentTemplate template = icon.getTemplate();
            //TODO derive this from CommandType?
            String createCmdNotice = ChatColor.GOLD+"/k kingdom create "+template.getName()+" (name)";
            ChatUtil.sendNotice(bukkitPlayer, createCmdNotice);
        }
    }
}
