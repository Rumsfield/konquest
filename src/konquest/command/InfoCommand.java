package konquest.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.model.KonKingdom;
import konquest.model.KonOfflinePlayer;
import konquest.model.KonPlayer;
import konquest.model.KonTown;
import konquest.model.KonUpgrade;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class InfoCommand extends CommandBase {

	public InfoCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	public void execute() {
		// k info <kingdomName>|<townName>|<playerName>
		//ChatUtil.printDebug("Entering info command execution...");
    	if (getArgs().length != 1 && getArgs().length != 2) {
    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        } else {
        	Player bukkitPlayer = (Player) getSender();
        	int displayState = 0; // 0 = kingdom, 1 = town, 2 = player
        	// Init info as own player's
        	if(!getKonquest().getPlayerManager().isPlayer(bukkitPlayer)) {
    			ChatUtil.printDebug("Failed to find non-existent player");
    			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
        	KonPlayer sender = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        	KonOfflinePlayer player = sender;
        	KonKingdom kingdom = player.getKingdom();
        	KonTown town = null;
        	String color = ""+ChatColor.GREEN;
        	if(getArgs().length == 1) {
        		// Display your own Kingdom info
        		displayState = 0;
        	} else {
        		String name = getArgs()[1];
        		// preliminary town search
        		boolean foundTown = false;
        		for(KonKingdom k : getKonquest().getKingdomManager().getKingdoms()) {
    				if(k.hasTown(name)) {
    					foundTown = true;
    					town = k.getTown(name);
    				}
    			}
        		/*
        		if(sender.isAdminBypassActive()) {
        			for(KonKingdom k : getKonquest().getKingdomManager().getKingdoms()) {
        				if(k.hasTown(name)) {
        					foundTown = true;
        					town = k.getTown(name);
        				}
        			}
        		} else if(sender.getKingdom().hasTown(name)){
        			foundTown = true;
        			town = sender.getKingdom().getTown(name);
        		}*/
        		//Check for Kingdom
        		if(getKonquest().getKingdomManager().isKingdom(name)) {
        			//ChatUtil.printDebug("Parsed kingdom");
        			displayState = 0;
        			kingdom = getKonquest().getKingdomManager().getKingdom(name);
        			if(!player.getKingdom().equals(kingdom)) {
        				color = ""+ChatColor.RED;
        			}
        		} else if(name.equalsIgnoreCase("barbarians")) {
        			//ChatUtil.printDebug("Parsed Barbarian kingdom");
        			displayState = 0;
        			kingdom = getKonquest().getKingdomManager().getBarbarians();
        			if(!player.getKingdom().equals(kingdom)) {
        				color = ""+ChatColor.RED;
        			}
        		} else if(foundTown) {
        			//ChatUtil.printDebug("Parsed Town in sender's kingdom");
        			//Check for Town
        			displayState = 1;
        			if(!player.getKingdom().equals(town.getKingdom())) {
        				color = ""+ChatColor.RED;
        			}
        		} else {
        			// Check for Player
        			//ChatUtil.printDebug("Parsing player");
        			KonOfflinePlayer otherPlayer = getKonquest().getPlayerManager().getAllPlayerFromName(name);
        			//ChatUtil.printDebug("Fetched KonOfflinePlayer");
        			if(otherPlayer != null) {
        				//ChatUtil.printDebug("Player is not null");
        				displayState = 2;
        				if(!player.getKingdom().equals(otherPlayer.getKingdom())) {
        					color = ""+ChatColor.RED;
        				}
        				player = otherPlayer;
        			} else {
            			//ChatUtil.sendError((Player) getSender(), "Failed to find unknown name, check spelling: "+name);
            			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(name));
            	        return;
        			}
        		}
        	}
        	String labelTotalPlayers = MessagePath.LABEL_TOTAL_PLAYERS.getMessage();
        	String labelOnlinePlayers = MessagePath.LABEL_ONLINE_PLAYERS.getMessage();
        	String labelPlayer = MessagePath.LABEL_PLAYER.getMessage();
        	String labelKingdom = MessagePath.LABEL_KINGDOM.getMessage();
        	String labelBarbarian = MessagePath.LABEL_BARBARIAN.getMessage();
        	String labelTowns = MessagePath.LABEL_TOWNS.getMessage();
        	String labelLand = MessagePath.LABEL_LAND.getMessage();
        	String labelFavor = MessagePath.LABEL_FAVOR.getMessage();
        	String labelOpen = MessagePath.LABEL_OPEN.getMessage();
        	String labelHealth = MessagePath.LABEL_HEALTH.getMessage();
        	String labelUpgrades = MessagePath.LABEL_UPGRADES.getMessage();
        	String labelResidents = MessagePath.LABEL_RESIDENTS.getMessage();
        	String labelInvites = MessagePath.LABEL_INVITES.getMessage();
        	String labelRequests = MessagePath.LABEL_REQUESTS.getMessage();
        	String labelResidencies = MessagePath.LABEL_RESIDENCIES.getMessage();
        	
        	switch(displayState) {
        	case 0: // Display kingdom info
        		//ChatUtil.printDebug("Displaying Kingdom");
        		String kingdomName = kingdom.getName();
	        	int numKingdomTowns = kingdom.getTowns().size();
	        	int numKingdomPlayers = getKonquest().getPlayerManager().getPlayersInKingdom(kingdom.getName()).size();
	        	ArrayList<KonOfflinePlayer> allPlayersInKingdom = getKonquest().getPlayerManager().getAllPlayersInKingdom(kingdom.getName());
	        	int numAllKingdomPlayers = allPlayersInKingdom.size();
	        	int numKingdomLand = 0;
	        	for(KonTown kingdomTown : kingdom.getTowns()) {
	        		numKingdomLand += kingdomTown.getChunkList().size();
	        	}
	        	int numKingdomFavor = 0;
	        	for(KonOfflinePlayer kingdomPlayer : allPlayersInKingdom) {
	        		numKingdomFavor += (int) KonquestPlugin.getEconomy().getBalance(kingdomPlayer.getOfflineBukkitPlayer());
	        	}
	        	if(kingdom.equals(getKonquest().getKingdomManager().getBarbarians())) {
		        	String[] message = {
		        			ChatColor.DARK_RED + "#==~ "+kingdomName+" ~==#",
		        			ChatColor.DARK_RED + "# "+labelTotalPlayers+": "+ChatColor.AQUA+numKingdomPlayers};
		        	for(String line : message) {
		        		ChatUtil.sendMessage(bukkitPlayer, line);
		        	}
	        	} else {
	        		String[] message = {
		        			ChatColor.GOLD + "#==~ "+MessagePath.COMMAND_INFO_NOTICE_KINGDOM_HEADER.getMessage(""+color+kingdomName)+ChatColor.GOLD+" ~==#",
		        			ChatColor.GOLD + "|  "+labelOnlinePlayers+": "+ChatColor.AQUA+numKingdomPlayers,
		        			ChatColor.GOLD + "|  "+labelTotalPlayers+": "+ChatColor.AQUA+numAllKingdomPlayers,
		        			ChatColor.GOLD + "|  "+labelTowns+": "+ChatColor.AQUA+numKingdomTowns,
		        			ChatColor.GOLD + "|  "+labelLand+": "+ChatColor.AQUA+numKingdomLand,
		        			ChatColor.GOLD + "# "+labelFavor+": "+ChatColor.AQUA+numKingdomFavor};
		        	for(String line : message) {
		        		ChatUtil.sendMessage(bukkitPlayer, line);
		        	}
	        	}
        		break;
        	case 1: // Display town info
        		if(town != null) {
        			String townName = town.getName();
        			String isOpen = String.valueOf(town.isOpen());
        			int townSize = town.getChunkList().size();
        			int maxCriticalhits = getKonquest().getConfigManager().getConfig("core").getInt("core.monuments.destroy_amount");
        			int townHealth = maxCriticalhits - town.getMonument().getCriticalHits();
        			String residents = "";
        			String lord = "";
        			ArrayList<String> knightList = new ArrayList<String>();
        			ArrayList<String> residentList = new ArrayList<String>();
        			for(OfflinePlayer resident : town.getPlayerResidents()) {
        				if(town.isPlayerLord(resident)) {
        					lord = ChatColor.DARK_PURPLE+"L:"+color+resident.getName();
        				} else if(town.isPlayerElite(resident)) {
        					knightList.add(ChatColor.LIGHT_PURPLE+"K:"+color+resident.getName());
        				} else {
        					residentList.add(color+resident.getName());
        				}
        			}
        			ArrayList<String> allNames = new ArrayList<String>();
        			allNames.add(lord);
        			allNames.addAll(knightList);
        			allNames.addAll(residentList);
        			for(String name : allNames) {
        				residents = residents+name+ChatColor.GRAY+", ";
        			}
        			if(residents.length() > 2) {
        				residents = residents.substring(0,residents.length()-2);
        			}
        			String upgrades = "";
        			for(KonUpgrade upgrade : KonUpgrade.values()) {
        				int currentLevel = town.getRawUpgradeLevel(upgrade);
        				if(currentLevel > 0) {
        					String formattedUpgrade = ChatColor.LIGHT_PURPLE+upgrade.getDescription()+" "+currentLevel;
        					if(town.isUpgradeDisabled(upgrade)) {
        						int reducedLevel = town.getUpgradeLevel(upgrade);
        						if(reducedLevel > 0) {
        							formattedUpgrade = ChatColor.LIGHT_PURPLE+upgrade.getDescription()+" "+ChatColor.DARK_GRAY+reducedLevel;
        						} else {
        							formattedUpgrade = ChatColor.GRAY+""+ChatColor.STRIKETHROUGH+upgrade.getDescription()+" "+reducedLevel;
        						}
        						
        						
        					}
        					upgrades = upgrades+formattedUpgrade+ChatColor.GRAY+", ";
        				}
        			}
        			if(upgrades.length() > 2) {
        				upgrades = upgrades.substring(0,upgrades.length()-2);
        			}
        			if(town.isPlayerElite(bukkitPlayer)) {
        				String invitedPlayers = "";
        				for(OfflinePlayer offlineBukkitPlayer : town.getJoinInvites()) {
        					invitedPlayers = invitedPlayers+ChatColor.WHITE+offlineBukkitPlayer.getName()+ChatColor.GRAY+", ";
        				}
        				if(invitedPlayers.length() > 2) {
        					invitedPlayers = invitedPlayers.substring(0,invitedPlayers.length()-2);
            			}
            			String requestingPlayers = "";
            			for(OfflinePlayer offlineBukkitPlayer : town.getJoinRequests()) {
            				requestingPlayers = requestingPlayers+ChatColor.WHITE+offlineBukkitPlayer.getName()+ChatColor.GRAY+", ";
        				}
        				if(requestingPlayers.length() > 2) {
        					requestingPlayers = requestingPlayers.substring(0,requestingPlayers.length()-2);
            			}
        				String[] message = {
        						ChatColor.GOLD + "#==~ "+MessagePath.COMMAND_INFO_NOTICE_TOWN_HEADER.getMessage(""+color+townName)+ChatColor.GOLD+" ~==#",
    		        			ChatColor.GOLD + "|  "+labelOpen+": "+ChatColor.AQUA+isOpen,
    		        			ChatColor.GOLD + "|  "+labelLand+": "+ChatColor.AQUA+townSize,
    		        			ChatColor.GOLD + "|  "+labelHealth+": "+ChatColor.AQUA+townHealth+"/"+maxCriticalhits,
    		        			ChatColor.GOLD + "|  "+labelUpgrades+": "+upgrades,
    		        			ChatColor.GOLD + "# "+labelResidents+": "+ChatColor.AQUA+residents,
    		        			ChatColor.GOLD + "# "+labelInvites+": "+invitedPlayers,
    		        			ChatColor.GOLD + "# "+labelRequests+": "+requestingPlayers
    		        			};
    		        	for(String line : message) {
    		        		ChatUtil.sendMessage(bukkitPlayer, line);
    		        	}
        			} else {
        				String[] message = {
        						ChatColor.GOLD + "#==~ "+MessagePath.COMMAND_INFO_NOTICE_TOWN_HEADER.getMessage(""+color+townName)+ChatColor.GOLD+" ~==#",
    		        			ChatColor.GOLD + "|  "+labelOpen+": "+ChatColor.AQUA+isOpen,
    		        			ChatColor.GOLD + "|  "+labelLand+": "+ChatColor.AQUA+townSize,
    		        			ChatColor.GOLD + "|  "+labelHealth+": "+ChatColor.AQUA+townHealth+"/"+maxCriticalhits,
    		        			ChatColor.GOLD + "|  "+labelUpgrades+": "+upgrades,
    		        			ChatColor.GOLD + "# "+labelResidents+": "+ChatColor.AQUA+residents
    		        			};
    		        	for(String line : message) {
    		        		ChatUtil.sendMessage(bukkitPlayer, line);
    		        	}
        			}
        		} else {
        			ChatUtil.printDebug("Failed to display null town info of town "+getArgs()[1]);
        			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
        		}
        		break;
        	case 2: // Display player info
        		getKonquest().getDisplayManager().displayPlayerInfoMenu(sender, player);
        		/*
        		//ChatUtil.printDebug("Displaying Player");
        		String playerName = player.getOfflineBukkitPlayer().getName();
        		String playerKingdom = player.getKingdom().getName();
        		int numPlayerFavor = (int) KonquestPlugin.getEconomy().getBalance(player.getOfflineBukkitPlayer());
        		if(player.isBarbarian()) {
        			String[] message = {
    	        			ChatColor.DARK_RED + "#==~ "+labelPlayer+" "+playerName+ChatColor.DARK_RED+" ~==#",
    	        			ChatColor.DARK_RED + "# "+labelBarbarian};
        			for(String line : message) {
    	        		ChatUtil.sendMessage(bukkitPlayer, line);
    	        	}
        		} else {
        			String townList = "";
        			for(KonTown playerTown : player.getKingdom().getTowns()) {
        				if(playerTown.isPlayerLord(player.getOfflineBukkitPlayer())) {
        					townList = townList + ChatColor.DARK_PURPLE+"L:"+color+playerTown.getName()+ChatColor.GRAY+", ";
        				} else if(playerTown.isPlayerElite(player.getOfflineBukkitPlayer())) {
        					townList = townList + ChatColor.LIGHT_PURPLE+"K:"+color+playerTown.getName()+ChatColor.GRAY+", ";
        				} else if(playerTown.isPlayerResident(player.getOfflineBukkitPlayer())){
        					townList = townList + color+playerTown.getName()+ChatColor.GRAY+", ";
        				}
        			}
        			if(townList.length() > 2) {
        				townList = townList.substring(0,townList.length()-2);
        			}
        			String[] message = {
    	        			ChatColor.GOLD + "#==~ "+labelPlayer+" "+color+playerName+ChatColor.GOLD+" ~==#",
    	        			ChatColor.GOLD + "|  "+labelKingdom+": "+ChatColor.AQUA+playerKingdom,
    	        			ChatColor.GOLD + "|  "+labelFavor+": "+ChatColor.AQUA+numPlayerFavor,
    	        			ChatColor.GOLD + "# "+labelResidencies+": "+townList};
        			for(String line : message) {
    	        		ChatUtil.sendMessage(bukkitPlayer, line);
    	        	}
        		}
        		*/
        		break;
        	default:
        		ChatUtil.printDebug("Failed to display info, unknown state");
        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
        		break;
        	}
        }
	}
	
	@Override
	public List<String> tabComplete() {
		// k info <kingdomName>|<townName>|<playerName>
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		if(getArgs().length == 2) {
			List<String> kingdomList = getKonquest().getKingdomManager().getKingdomNames();
			List<String> townList = new ArrayList<String>();
			for(KonKingdom kingdom : getKonquest().getKingdomManager().getKingdoms()) {
				townList.addAll(kingdom.getTownNames());
			}
			List<String> playerList = new ArrayList<>();
			//for(KonPlayer player : getKonquest().getPlayerManager().getPlayersOnline()) {
			for(OfflinePlayer bukkitOfflinePlayer : getKonquest().getPlayerManager().getAllOfflinePlayers()) {
				playerList.add(bukkitOfflinePlayer.getName());
			}
			
			tabList.addAll(kingdomList);
			tabList.addAll(townList);
			tabList.addAll(playerList);
			
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[1], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
}
