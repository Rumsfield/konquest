package com.github.rumsfield.konquest.database;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class KonquestDB extends Database{

	private boolean isReady;
	private final Konquest konquest;
	
	public KonquestDB(DatabaseType type, Konquest konquest) {
        super(type);
        this.konquest = konquest;
        this.isReady = false;
    }

    @Override
    public void initialize() {
        try {
            getDatabaseConnection().connect();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        konquest.getAccomplishmentManager().loadCustomPrefixes();
        spawnTables();
        konquest.getPlayerManager().initAllSavedPlayers();
        konquest.getKingdomManager().loadLegacyKingdomMemberships();
        konquest.getCampManager().initCamps();
        konquest.getMapHandler().drawDynmapAllTerritories();
        isReady = true;
        ChatUtil.printConsole("SQL database is ready");
        
        konquest.initOnlinePlayers();
    }
    
    public boolean isReady() {
    	return isReady;
    }

    public void spawnTables() {
        Column column;

        // Table players - Stores Konquest fields about players
        Table players = new Table("players", this);
        {
            column = new Column("uuid");
            column.setType("CHAR(36)");
            column.setPrimary(true);
            players.add(column);
            
            column = new Column("kingdom");
            column.setType("VARCHAR(255)");
            column.setDefaultValue("'"+konquest.getKingdomManager().getBarbarians().getName()+"'");
            players.add(column);
            
            column = new Column("exileKingdom");
            column.setType("VARCHAR(255)");
            column.setDefaultValue("'"+konquest.getKingdomManager().getBarbarians().getName()+"'");
            players.add(column);

            column = new Column("barbarian");
            column.setType("TINYINT");
            column.setDefaultValue("1");
            players.add(column);
            
            column = new Column("prefix");
            column.setType("VARCHAR(255)");
            players.add(column);
            
            column = new Column("prefixOn");
            column.setType("TINYINT(1)");
            column.setDefaultValue("0");
            players.add(column);
            
            column = new Column("custom");
            column.setType("VARCHAR(255)");
            players.add(column);
        }
        players.execute();

        // Table stats - Stores Konquest statistics per player
        Table stats = new Table("stats", this);
        {
            column = new Column("uuid");
            column.setType("CHAR(36)");
            column.setPrimary(true);
            stats.add(column);

            for(KonStatsType stat : KonStatsType.values()) {
    			String name = stat.toString();
    			String value = String.valueOf(0);
    			column = new Column(name);
                column.setType("INTEGER");
                column.setDefaultValue(value);
                stats.add(column);
    		}
        }
        stats.execute();
        
        // Table directives - Stores Konquest directives per player
        Table directives = new Table("directives", this);
        {
            column = new Column("uuid");
            column.setType("CHAR(36)");
            column.setPrimary(true);
            directives.add(column);

            for(KonDirective dir : KonDirective.values()) {
    			String name = dir.toString();
    			column = new Column(name);
                column.setType("INTEGER");
                column.setDefaultValue("0");
                directives.add(column);
    		}
        }
        directives.execute();
        
        // Table customs - Stores Konquest custom prefix labels per player
        Table customs = new Table("customs", this);
        {
        	column = new Column("uuid");
            column.setType("CHAR(36)");
            column.setPrimary(true);
            customs.add(column);
            
            for(String label : konquest.getAccomplishmentManager().getCustomPrefixLabels()) {
            	column = new Column(label);
            	column.setType("TINYINT(1)");
                column.setDefaultValue("0");
                customs.add(column);
            }
        }
        customs.execute();
    }
    
    public ArrayList<KonOfflinePlayer> getAllSavedPlayers() {
    	ArrayList<KonOfflinePlayer> players = new ArrayList<KonOfflinePlayer>();
    	ResultSet player = selectAll("players");
    	String uuid;
    	String kingdomName;
    	boolean isBarbarian;
    	try {
            while (player.next()) {
            	uuid = player.getString("uuid");
            	kingdomName = player.getString("kingdom");
            	isBarbarian = (player.getInt("barbarian") == 1);
            	if(kingdomName==null) { kingdomName = konquest.getKingdomManager().getBarbarians().getName(); }
            	OfflinePlayer offlineBukkitPlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
            	// Check that a valid offlinePlayer was fetched by checking for not null name
            	if(offlineBukkitPlayer.getName() != null) {
            		players.add(new KonOfflinePlayer(offlineBukkitPlayer, konquest.getKingdomManager().getKingdom(kingdomName), isBarbarian));
            	}
            }
        } catch (Exception e) {
            e.printStackTrace();
            ChatUtil.printConsoleError("A problem occured while getting all saved players from the database");
        }
    	return players;
    }
    
    public void setOfflinePlayers(ArrayList<KonOfflinePlayer> offlinePlayers) {
    	for(KonOfflinePlayer offlinePlayer : offlinePlayers) {
    		setOfflinePlayer(offlinePlayer);
    	}
    }
    
    public void setOfflinePlayer(KonOfflinePlayer offlinePlayer) {
		if(konquest.getPlayerManager().isOfflinePlayer(offlinePlayer.getOfflineBukkitPlayer())) {
			String playerUUIDString = offlinePlayer.getOfflineBukkitPlayer().getUniqueId().toString();
	        String[] col;
	        String[] val;
	        // Flush player data into players table
	        col  = new String[] {"kingdom","barbarian"};
	        val  = new String[col.length];
	        val[0] = "'"+offlinePlayer.getKingdom().getName()+"'";
	        val[1] = offlinePlayer.isBarbarian() ? "1" : "0";
	        set("players", col, val, "uuid", playerUUIDString);
		} else {
			ChatUtil.printDebug("Failed to flush non-existent offlinePlayer to database");
		}
    }
    
    public void fetchPlayerData(Player bukkitPlayer) {
    	KonPlayer player;
        if (!exists("players", "uuid", bukkitPlayer.getUniqueId().toString())) {
            createPlayerData(bukkitPlayer);
            player = konquest.getPlayerManager().createKonPlayer(bukkitPlayer);
        } else {
        	assertPlayerData(bukkitPlayer);
        	//TODO: Insert player into tables if they're not already in them,
        	// i.e. the customs table is new and existing players must be inserted.
        	
        	ResultSet playerInfo = select("players", "uuid", bukkitPlayer.getUniqueId().toString());
        	String kingdomName = "";
    		String exileKingdomName = "";
    		boolean isBarbarian = true;
    		String mainPrefix = "";
    		boolean enablePrefix = false;
    		String customPrefix = "";
    		try {
                while (playerInfo.next()) {
                	kingdomName = playerInfo.getString("kingdom");
                	exileKingdomName = playerInfo.getString("exileKingdom");
                	isBarbarian = playerInfo.getBoolean("barbarian");
                	mainPrefix = playerInfo.getString("prefix");
                	enablePrefix = playerInfo.getBoolean("prefixOn");
                	customPrefix = playerInfo.getString("custom");
                }
                //ChatUtil.printDebug("SQL Imported player info: "+kingdomName+","+exileKingdomName+","+isBarbarian+","+mainPrefix+","+enablePrefix);
            } catch (Exception e) {
                e.printStackTrace();
                ChatUtil.printDebug("Aborting player import "+bukkitPlayer.getName());
                return;
            }
    		if(kingdomName==null) { kingdomName = konquest.getKingdomManager().getBarbarians().getName(); }
    		if(exileKingdomName==null) { exileKingdomName = konquest.getKingdomManager().getBarbarians().getName(); }
        	// Create a player from existing info
    		player = konquest.getPlayerManager().importKonPlayer(bukkitPlayer, kingdomName, exileKingdomName, isBarbarian);
    		// Get stats and directives for the player
            ResultSet stats = select("stats", "uuid", bukkitPlayer.getUniqueId().toString());
            ResultSet directives = select("directives", "uuid", bukkitPlayer.getUniqueId().toString());
            try {
                while (stats.next()) {
                	for(KonStatsType statEnum : KonStatsType.values()) {
                		int statProgress = stats.getInt(statEnum.toString());
                		player.getPlayerStats().setStat(statEnum, statProgress);
                	}
                }
                while (directives.next()) {
                	for(KonDirective dirEnum : KonDirective.values()) {
                		int directiveProgress = directives.getInt(dirEnum.toString());
                		player.setDirectiveProgress(dirEnum, directiveProgress);
                	}
                }
            } catch (Exception e) {
                e.printStackTrace();
                ChatUtil.printDebug("Could not get stats and directives for "+bukkitPlayer.getName());
            }
            // Update custom prefixes
            ResultSet customs = select("customs", "uuid", bukkitPlayer.getUniqueId().toString());
            try {
                while (customs.next()) {
                	for(String label : konquest.getAccomplishmentManager().getCustomPrefixLabels()) {
                		boolean isAvailable = customs.getBoolean(label);
                		if(isAvailable) {
                			player.getPlayerPrefix().addAvailableCustom(label);
                		}
                	}
                }
            } catch (Exception e) {
                e.printStackTrace();
                ChatUtil.printDebug("Could not get custom prefixes for "+bukkitPlayer.getName());
            }
    		// Add valid prefixes to the player based on stats
            konquest.getAccomplishmentManager().initPlayerPrefixes(player);
            boolean prefixStatus = false;
            if(customPrefix != null && !customPrefix.equals("") && konquest.getAccomplishmentManager().isEnabled()) {
            	// Update player's custom prefix first
            	prefixStatus = konquest.getAccomplishmentManager().setPlayerCustomPrefix(player, customPrefix);
            } else if(mainPrefix != null && !mainPrefix.equals("") && konquest.getAccomplishmentManager().isEnabled()) {
            	// Update player's main prefix
            	prefixStatus = player.getPlayerPrefix().setPrefix(KonPrefixType.getPrefix(mainPrefix)); // Defaults to default prefix defined in KonPrefixType if mainPrefix is not a valid enum
        	} else {
        		enablePrefix = false;
        	}
            if(!prefixStatus && konquest.getAccomplishmentManager().isEnabled()) {
            	ChatUtil.printDebug("Failed to assign main prefix or custom prefix to player "+bukkitPlayer.getName());
    			// Schedule messages to display after 20-tick delay (1 second)
    	    	Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), () -> {
                    //ChatUtil.sendError(bukkitPlayer, "Your prefix has been reverted to default.");
                    ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_PREFIX_ERROR_DEFAULT.getMessage());
                }, 20);
            }
        	player.getPlayerPrefix().setEnable(enablePrefix);
        }
    }

    public KonStats pullPlayerStats(OfflinePlayer offlineBukkitPlayer) {
    	KonStats playerStats = new KonStats();
    	ResultSet stats = select("stats", "uuid", offlineBukkitPlayer.getUniqueId().toString());
    	try {
            while (stats.next()) {
            	for(KonStatsType statEnum : KonStatsType.values()) {
            		int statProgress = stats.getInt(statEnum.toString());
            		playerStats.setStat(statEnum, statProgress);
            	}
            }
        } catch (Exception e) {
            e.printStackTrace();
            ChatUtil.printDebug("Could not pull stats for "+offlineBukkitPlayer.getName());
        }
    	return playerStats;
    }
    
    public void pushPlayerStats(OfflinePlayer offlineBukkitPlayer, KonStats stats) {
        String[] col = new String[KonStatsType.values().length];
        String[] val = new String[KonStatsType.values().length];
        int i = 0;
        for(KonStatsType iter : KonStatsType.values()) {
        	col[i] = iter.toString();
        	val[i] = Integer.toString(stats.getStat(iter));
        	i++;
    	}
        set("stats", col, val, "uuid", offlineBukkitPlayer.getUniqueId().toString());
    }
    
    public void createPlayerData(Player bukkitPlayer) {
        String uuid = bukkitPlayer.getUniqueId().toString();
        insert("players", new String[] {"uuid"}, new String[] {uuid});
        insert("stats", new String[] {"uuid"}, new String[] {uuid});
        insert("directives", new String[] {"uuid"}, new String[] {uuid});
        insert("customs", new String[] {"uuid"}, new String[] {uuid});
    }
    
    public void assertPlayerData(Player bukkitPlayer) {
        String uuid = bukkitPlayer.getUniqueId().toString();
        if (!exists("players", "uuid", uuid)) {
        	insert("players", new String[] {"uuid"}, new String[] {uuid});
        }
        if (!exists("stats", "uuid", uuid)) {
        	insert("stats", new String[] {"uuid"}, new String[] {uuid});
        }
        if (!exists("directives", "uuid", uuid)) {
        	insert("directives", new String[] {"uuid"}, new String[] {uuid});
        }
        if (!exists("customs", "uuid", uuid)) {
        	insert("customs", new String[] {"uuid"}, new String[] {uuid});
        }
    }
    
    public void flushPlayerData(Player bukkitPlayer) {
    	if(!konquest.getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
			ChatUtil.printDebug("Failed to flush non-existent player to database");
			return;
		}
        String playerUUIDString = bukkitPlayer.getUniqueId().toString();
        KonPlayer player = konquest.getPlayerManager().getPlayer(bukkitPlayer);
        String[] col;
        String[] val;
        int i;
        
        // Flush player data into players table
        col  = new String[] {"kingdom","exileKingdom","barbarian","prefix","prefixOn","custom"};
        val  = new String[col.length];
        val[0] = "'"+player.getKingdom().getName()+"'";
        val[1] = "'"+player.getExileKingdom().getName()+"'";
        val[2] = player.isBarbarian() ? "1" : "0";
        val[3] = "'"+player.getPlayerPrefix().getMainPrefix().toString()+"'";
        val[4] = player.getPlayerPrefix().isEnabled() ? "1" : "0";
        val[5] = "'"+player.getPlayerPrefix().getCustom()+"'";
        set("players", col, val, "uuid", playerUUIDString);
        
        // Flush player data into stats table
        col = new String[KonStatsType.values().length];
        val = new String[KonStatsType.values().length];
        i = 0;
        for(KonStatsType iter : KonStatsType.values()) {
        	col[i] = iter.toString();
        	val[i] = Integer.toString(player.getPlayerStats().getStat(iter));
        	i++;
    	}
        set("stats", col, val, "uuid", playerUUIDString);
        
        // Flush player data into directives table
        col = new String[KonDirective.values().length];
        val = new String[KonDirective.values().length];
        i = 0;
        for(KonDirective iter : KonDirective.values()) {
        	col[i] = iter.toString();
        	val[i] = Integer.toString(player.getDirectiveProgress(iter));
        	i++;
    	}
        set("directives", col, val, "uuid", playerUUIDString);
        
        // Flush player data into customs table
        Set<String> labels = konquest.getAccomplishmentManager().getCustomPrefixLabels();
        if(!labels.isEmpty()) {
	        col = new String[labels.size()];
	        val = new String[labels.size()];
	        i = 0;
	        for(String iter : labels) {
	        	col[i] = iter;
	        	val[i] = player.getPlayerPrefix().isCustomAvailable(iter) ? "1" : "0";
	        	i++;
	    	}
	        set("customs", col, val, "uuid", playerUUIDString);
        }
    }
    
}
