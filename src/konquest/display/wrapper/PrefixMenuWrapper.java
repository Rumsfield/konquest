package konquest.display.wrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import konquest.Konquest;
import konquest.display.MenuIcon;
import konquest.display.PrefixCustomIcon;
import konquest.display.PrefixIcon;
import konquest.display.PrefixIcon.PrefixIconAction;
import konquest.manager.DisplayManager;
import konquest.model.KonCustomPrefix;
import konquest.model.KonPlayer;
import konquest.model.KonPrefixCategory;
import konquest.model.KonPrefixType;
import konquest.model.KonStatsType;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

public class PrefixMenuWrapper extends MenuWrapper {

	private KonPlayer observer;
	
	public PrefixMenuWrapper(Konquest konquest, KonPlayer observer) {
		super(konquest);
		this.observer = observer;
	}

	@Override
	public void constructMenu() {

		ChatColor titleColor = DisplayManager.titleColor;
		ChatColor loreColor = DisplayManager.loreColor;
		ChatColor valueColor = DisplayManager.valueColor;
		ChatColor hintColor = DisplayManager.hintColor;
		String pageLabel = "";
		String playerPrefix = "";
		if(observer.getPlayerPrefix().isEnabled()) {
			playerPrefix = ChatUtil.parseHex(observer.getPlayerPrefix().getMainPrefixName());
		}
		final int MAX_ICONS_PER_PAGE = 45;
		final int MAX_ROWS_PER_PAGE = 5;
		final int ICONS_PER_ROW = 9;
		
		// Top row of page 0 is "Off" and info icons
		// Start a new row for each category. Categories use as many rows as needed to fit all prefixes
		//ChatUtil.printDebug("Displaying new prefix menu...");
		// Determine number of pages and rows per category
		List<KonPrefixType> allPrefixes = new ArrayList<KonPrefixType>();
		Map<KonPrefixCategory,Double> categoryLevels = new HashMap<KonPrefixCategory,Double>();
		int totalRows = 1;
		for(KonPrefixCategory category : KonPrefixCategory.values()) {
			List<KonPrefixType> prefixList = new ArrayList<KonPrefixType>();
			double level = 0;
			for(KonStatsType statCheck : KonStatsType.values()) {
				if(statCheck.getCategory().equals(category)) {
					level = level + (observer.getPlayerStats().getStat(statCheck) * statCheck.weight());
				}
			}
			categoryLevels.put(category, level);
			int count = 0;
			for(KonPrefixType prefix : KonPrefixType.values()) {
				if(prefix.category().equals(category)) {
					count++;
					prefixList.add(prefix);
				}
			}
			prefixList = sortedPrefix(prefixList);
			allPrefixes.addAll(prefixList);
			// count is total number of icons per category
			// 9 icons per row
			int rows = (int)Math.ceil(((double)count)/ICONS_PER_ROW);
			//ChatUtil.printDebug("  Counted "+rows+" rows for category "+category.getTitle());
			totalRows += rows;
		}
		int pageTotal = (int)Math.ceil(((double)totalRows)/MAX_ROWS_PER_PAGE);
		//ChatUtil.printDebug("  Counted "+totalRows+" total rows");
		
		// Page 0+
		int pageNum = 0;
		PrefixIcon prefixIcon;
		ListIterator<KonPrefixType> prefixIter = allPrefixes.listIterator();
		for(int i = 0; i < pageTotal; i++) {
			int numPageRows = Math.min((totalRows - i*MAX_ROWS_PER_PAGE),MAX_ROWS_PER_PAGE);
			pageLabel = titleColor+playerPrefix+" "+titleColor+observer.getBukkitPlayer().getName()+" "+(i+1)+"/"+pageTotal;
			getMenu().addPage(pageNum, numPageRows, pageLabel);
			//ChatUtil.printDebug("  Created page "+i+" with "+numPageRows+" rows");
			int slotIndex = 0;
			// Off and Info Icons on first row of page 0
			if(pageNum == 0) {
				PrefixIcon offIcon = new PrefixIcon(KonPrefixType.getDefault(),Arrays.asList(hintColor+MessagePath.MENU_PREFIX_HINT_DISABLE.getMessage()),4,true,PrefixIconAction.DISABLE_PREFIX);
				getMenu().getPage(pageNum).addIcon(offIcon);
				slotIndex = 9;
			}
			// All other prefix icons
			while(slotIndex < (numPageRows*ICONS_PER_ROW) && prefixIter.hasNext()) {
				/* Prefix Icon (n) */
				KonPrefixType prefix = prefixIter.next();
				String categoryLevel = String.format("%.2f",categoryLevels.get(prefix.category()));
				String categoryFormat = ChatColor.WHITE+prefix.category().getTitle();
				String levelFormat = ChatColor.DARK_GREEN+categoryLevel+ChatColor.WHITE+"/"+ChatColor.AQUA+prefix.level();
				if(observer.getPlayerPrefix().hasPrefix(prefix)) {
					prefixIcon = new PrefixIcon(prefix,Arrays.asList(categoryFormat,levelFormat,hintColor+MessagePath.MENU_PREFIX_HINT_APPLY.getMessage()),slotIndex,true,PrefixIconAction.APPLY_PREFIX);
				} else {
					levelFormat = ChatColor.DARK_RED+categoryLevel+ChatColor.WHITE+"/"+ChatColor.AQUA+prefix.level();
					prefixIcon = new PrefixIcon(prefix,Arrays.asList(categoryFormat,levelFormat),slotIndex,false,PrefixIconAction.APPLY_PREFIX);
				}
				getMenu().getPage(pageNum).addIcon(prefixIcon);
				if(prefixIter.hasNext() && !allPrefixes.get(prefixIter.nextIndex()).category().equals(prefix.category())) {
					// New row
					slotIndex = slotIndex + (ICONS_PER_ROW - (slotIndex % ICONS_PER_ROW));
				} else {
					// Next slot
					slotIndex++;
				}
			}
			pageNum++;
		}
		// Page N+
		List<String> loreList;
		boolean isAllowed = false;
		List<KonCustomPrefix> allCustoms = getKonquest().getAccomplishmentManager().getCustomPrefixes();
		pageTotal = (int)Math.ceil(((double)allCustoms.size())/MAX_ICONS_PER_PAGE);
		if(pageTotal == 0) {
			pageTotal = 1;
		}
		if(!allCustoms.isEmpty()) {
			ListIterator<KonCustomPrefix> customIter = allCustoms.listIterator();
			for(int i = 0; i < pageTotal; i++) {
				int numPageRows = (int)Math.ceil(((double)(allCustoms.size() - i*MAX_ICONS_PER_PAGE))/9);
				if(numPageRows < 1) {
					numPageRows = 1;
				} else if(numPageRows > 5) {
					numPageRows = 5;
				}
				pageLabel = titleColor+MessagePath.MENU_PREFIX_CUSTOM_PAGES.getMessage()+" "+(i+1)+"/"+pageTotal;
				getMenu().addPage(pageNum, numPageRows, pageLabel);
				int slotIndex = 0;
				while(slotIndex < MAX_ICONS_PER_PAGE && customIter.hasNext()) {
					/* Custom Prefix Icon (n) */
					loreList = new ArrayList<String>();
					KonCustomPrefix currentCustom = customIter.next();
					if(!observer.getPlayerPrefix().isCustomAvailable(currentCustom.getLabel())) {
						loreList.add(loreColor+MessagePath.LABEL_COST.getMessage()+": "+valueColor+currentCustom.getCost());
					}
					if(observer.getBukkitPlayer().hasPermission("konquest.prefix."+currentCustom.getLabel())) {
						isAllowed = true;
						loreList.add(hintColor+MessagePath.MENU_PREFIX_HINT_APPLY.getMessage());
					} else {
						isAllowed = false;
						loreList.add(ChatColor.DARK_RED+MessagePath.MENU_PREFIX_NO_ALLOW.getMessage());
					}
			    	PrefixCustomIcon customIcon = new PrefixCustomIcon(currentCustom, loreList, slotIndex, isAllowed);
			    	getMenu().getPage(pageNum).addIcon(customIcon);
					slotIndex++;
				}
				pageNum++;
			}
		}
				
		getMenu().refreshNavigationButtons();
		getMenu().setPageIndex(0);
	}

	@Override
	public boolean onIconClick(KonPlayer clickPlayer, MenuIcon clickedIcon) {
		boolean result = false;
		Player bukkitPlayer = clickPlayer.getBukkitPlayer();
		if(clickedIcon instanceof PrefixIcon) {
			// Prefix Icons alter the player's prefix
			PrefixIcon icon = (PrefixIcon)clickedIcon;
			boolean status = false;
			switch(icon.getAction()) {
				case DISABLE_PREFIX:
					status = getKonquest().getAccomplishmentManager().disablePlayerPrefix(clickPlayer);
					break;
				case APPLY_PREFIX:
					status = getKonquest().getAccomplishmentManager().applyPlayerPrefix(clickPlayer,icon.getPrefix());
					break;
				default:
					break;
			}
			if(status) {
				Konquest.playSuccessSound(bukkitPlayer);
			} else {
				Konquest.playFailSound(bukkitPlayer);
			}
			result = true;
		} else if(clickedIcon instanceof PrefixCustomIcon) {
			// Prefix Custom Icons alter the player's prefix
			PrefixCustomIcon icon = (PrefixCustomIcon)clickedIcon;
			boolean status = getKonquest().getAccomplishmentManager().applyPlayerCustomPrefix(clickPlayer,icon.getPrefix());
			if(status) {
				Konquest.playSuccessSound(bukkitPlayer);
			} else {
				Konquest.playFailSound(bukkitPlayer);
			}
			result = true;
		}
		return result;
	}

	// Sort prefix by level low-to-high
   	private List<KonPrefixType> sortedPrefix(List<KonPrefixType> inputList) {
   		// Sort each prefix list by level
   		Comparator<KonPrefixType> prefixComparator = new Comparator<KonPrefixType>() {
   			@Override
   			public int compare(final KonPrefixType k1, KonPrefixType k2) {
   				int result = 0;
   				if(k1.level() < k2.level()) {
   					result = -1;
   				} else if(k1.level() > k2.level()) {
   					result = 1;
   				}
   				return result;
   			}
   		};
   		Collections.sort(inputList, prefixComparator);
   		
   		return inputList;
   	}
   	
}
