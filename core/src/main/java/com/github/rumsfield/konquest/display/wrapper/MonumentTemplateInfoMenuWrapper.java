package com.github.rumsfield.konquest.display.wrapper;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.display.icon.InfoIcon;
import com.github.rumsfield.konquest.display.icon.MenuIcon;
import com.github.rumsfield.konquest.display.icon.TemplateIcon;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.KonKingdom;
import com.github.rumsfield.konquest.model.KonMonumentTemplate;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.model.KonSanctuary;
import com.github.rumsfield.konquest.utility.ChatUtil;
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
        String loreColor = DisplayManager.titleFormat;
        String valueColor = DisplayManager.titleFormat;

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
                if(template.isValid()) {
                    allTemplates.add(template);
                    templateSanctuaryMap.put(template,sanctuary.getName());
                    // Count kingdoms using this template
                    int count = 0;
                    for(KonKingdom kingdom : getKonquest().getKingdomManager().getKingdoms()) {
                        if(kingdom.getMonumentTemplate().equals(template)) {
                            count++;
                        }
                    }
                    templateUsedKingdomsMap.put(template,count);
                }
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
                loreList.add(loreColor+"Name"+": "+valueColor+currentTemplate.getName());
                loreList.add(loreColor+"Sanctuary"+": "+valueColor+templateSanctuaryMap.get(currentTemplate));
                loreList.add(loreColor+"Kingdoms"+": "+valueColor+templateUsedKingdomsMap.get(currentTemplate));
                loreList.add(loreColor+"Critical Hits"+": "+valueColor+currentTemplate.getNumCriticals());
                loreList.add(loreColor+"Loot Chests"+": "+valueColor+currentTemplate.getNumLootChests());
                loreList.add(loreColor+"Favor Cost"+": "+valueColor+totalCost);
                InfoIcon icon = new InfoIcon(ChatColor.GOLD+"Monument Template", loreList, Material.CRAFTING_TABLE, slotIndex, false);
                getMenu().getPage(pageNum).addIcon(icon);
                slotIndex++;
            }
            pageNum++;
        }

        getMenu().refreshNavigationButtons();
        getMenu().setPageIndex(0);
    }

    @Override
    public void onIconClick(KonPlayer clickPlayer, MenuIcon clickedIcon) {
        // This menu has no clickable icons.
    }
}
