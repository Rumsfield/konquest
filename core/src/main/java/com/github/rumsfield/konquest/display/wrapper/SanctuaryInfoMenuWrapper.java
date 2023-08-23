package com.github.rumsfield.konquest.display.wrapper;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.display.icon.InfoIcon;
import com.github.rumsfield.konquest.display.icon.MenuIcon;
import com.github.rumsfield.konquest.display.icon.TemplateIcon;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.*;

public class SanctuaryInfoMenuWrapper extends MenuWrapper {

    private final KonSanctuary infoSanctuary;

    public SanctuaryInfoMenuWrapper(Konquest konquest, KonSanctuary infoSanctuary) {
        super(konquest);
        this.infoSanctuary = infoSanctuary;
    }

    @Override
    public void constructMenu() {

        String pageLabel;
        List<String> loreList;

        String neutralColor = Konquest.neutralColor2;
        String titleColor = DisplayManager.titleFormat;
        String loreColor = DisplayManager.loreFormat;
        String valueColor = DisplayManager.valueFormat;

        /*
         * Display info about this Sanctuary:
         *  All properties
         *  Total land size, world name, number of templates
         *  A paged list of templates
         */
        ArrayList<KonMonumentTemplate> allTemplates = new ArrayList<>(infoSanctuary.getTemplates());
        HashMap<KonMonumentTemplate,Integer> templateUsedKingdomsMap = new HashMap<KonMonumentTemplate,Integer>();
        for(KonMonumentTemplate template : infoSanctuary.getTemplates()) {
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

        // Page 0
        pageLabel = titleColor+MessagePath.LABEL_SANCTUARY.getMessage()+" "+infoSanctuary.getName();
        getMenu().addPage(0, 1, pageLabel);

        /* Flags Info Icon (3) */
        loreList = new ArrayList<>();
        for(KonPropertyFlag flag : KonPropertyFlag.values()) {
            if(infoSanctuary.hasPropertyValue(flag)) {
                String flagDisplaySymbol = DisplayManager.boolean2Symbol(infoSanctuary.getPropertyValue(flag));
                loreList.add(loreColor+flag.getName()+": "+flagDisplaySymbol);
            }
        }
        InfoIcon propertyInfo = new InfoIcon(neutralColor+MessagePath.LABEL_FLAGS.getMessage(), loreList, Material.REDSTONE_TORCH, 3, false);
        getMenu().getPage(0).addIcon(propertyInfo);

        /* General Info Icon (5) */
        loreList.add(loreColor + MessagePath.LABEL_MONUMENT_TEMPLATE.getMessage() + ": " + valueColor + infoSanctuary.getTemplates().size());
        loreList.add(loreColor + MessagePath.LABEL_LAND.getMessage() + ": " + valueColor + infoSanctuary.getChunkList().size());
        loreList.add(loreColor + MessagePath.LABEL_WORLD.getMessage() + ": " + valueColor + infoSanctuary.getWorld().getName());
        InfoIcon generalInfo = new InfoIcon(neutralColor+MessagePath.LABEL_INFORMATION.getMessage(), loreList, Material.ENDER_EYE, 5, false);
        getMenu().getPage(0).addIcon(generalInfo);

        // Page 1+
        int pageNum = 1;
        int pageTotal = getTotalPages(allTemplates.size());
        ListIterator<KonMonumentTemplate> templateIter = allTemplates.listIterator();
        for(int i = 0; i < pageTotal; i++) {
            int numPageRows = getNumPageRows(allTemplates.size(),i);
            pageLabel = titleColor+ MessagePath.LABEL_MONUMENT_TEMPLATE.getMessage()+" "+(i+1)+"/"+pageTotal;
            getMenu().addPage(pageNum, numPageRows, pageLabel);
            int slotIndex = 0;
            while(slotIndex < MAX_ICONS_PER_PAGE && templateIter.hasNext()) {
                /* Template Icon (n) */
                KonMonumentTemplate currentTemplate = templateIter.next();
                double totalCost = getKonquest().getKingdomManager().getCostTemplate()+currentTemplate.getCost();
                loreList = new ArrayList<>();
                loreList.add(loreColor+MessagePath.LABEL_KINGDOMS.getMessage()+": "+valueColor+templateUsedKingdomsMap.get(currentTemplate));
                loreList.add(loreColor+MessagePath.LABEL_COST.getMessage()+": "+valueColor+totalCost);
                TemplateIcon templateIcon = new TemplateIcon(currentTemplate,""+ ChatColor.GOLD,loreList,slotIndex,false);
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
        // No clickable icons
    }
}
