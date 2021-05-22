package konquest.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import konquest.Konquest;
import konquest.model.KonDirective;
import konquest.model.KonOfflinePlayer;
import konquest.model.KonPlayer;
import konquest.model.KonPrefixType;
import konquest.model.KonStats;
import konquest.model.KonStatsType;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

public class KonquestDB extends Database{

	private boolean isReady;
	
	public KonquestDB(Konquest konquest) {
        super(konquest);
        this.isReady = false;
    }

    @Override
    public void initialize() {
        try {
            getDatabaseConnection().connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        spawnTables();
        getKonquest().getPlayerManager().initAllSavedPlayers();
        getKonquest().getKingdomManager().initCamps();
        isReady = true;
        ChatUtil.printStatus("SQLite database is ready");
        
        getKonquest().initOnlinePlayers();
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
            column.setDefaultValue(getKonquest().getKingdomManager().getBarbarians().getName());
            players.add(column);
            
            column = new Column("exileKingdom");
            column.setType("VARCHAR(255)");
            column.setDefaultValue(getKonquest().getKingdomManager().getBarbarians().getName());
            players.add(column);

            column = new Column("barbarian");
            column.setType("TINYINT(1)");
            column.setDefaultValue("1");
            players.add(column);
            
            column = new Column("prefix");
            column.setType("VARCHAR(255)");
            players.add(column);
            
            column = new Column("prefixOn");
            column.setType("TINYINT(1)");
            column.setDefaultValue("0");
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
    }
    
    public ArrayList<KonOfflinePlayer> getAllSavedPlayers() {
    	ArrayList<KonOfflinePlayer> players = new ArrayList<KonOfflinePlayer>();
    	ResultSet player = selectAll("players");
    	String uuid = "";
    	String kingdomName = "";
    	boolean isBarbarian = true;
    	try {
            while (player.next()) {
            	uuid = player.getString("uuid");
            	kingdomName = player.getString("kingdom");
            	isBarbarian = (player.getInt("barbarian") == 1);
            	//ChatUtil.printDebug("Database player row: "+uuid+", "+kingdomName+", "+isBarbarian);
            	if(kingdomName==null) { kingdomName = getKonquest().getKingdomManager().getBarbarians().getName(); }
            	players.add(new KonOfflinePlayer(Bukkit.getOfflinePlayer(UUID.fromString(uuid)), getKonquest().getKingdomManager().getKingdom(kingdomName), isBarbarian));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            ChatUtil.printDebug("A problem occured while getting all saved players from the database");
        }
    	return players;
    }

    /*public void fetchPlayerData(KonPlayer player) {
        Player bukkitPlayer = player.getBukkitPlayer();

        if (!exists("players", "uuid", bukkitPlayer.getUniqueId().toString())) {
            createPlayerData(player);
        }

        //ResultSet data = select("players", "uuid", bukkitPlayer.getUniqueId().toString());
        KonStats playerStats = player.getPlayerStats();
        ResultSet stats = select("stats", "uuid", bukkitPlayer.getUniqueId().toString());

        try {
            while (stats.next()) {
            	for(KonStatsType statEnum : KonStatsType.values()) {
            		playerStats.setStat(statEnum, stats.getInt(statEnum.toString()));
            	}
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        getKonquest().getAccomplishmentManager().initPlayerPrefixes(player);
    }*/
    
    public void fetchPlayerData(Player bukkitPlayer) {
    	KonPlayer player;
        if (!exists("players", "uuid", bukkitPlayer.getUniqueId().toString())) {
            createPlayerData(bukkitPlayer);
            player = getKonquest().getPlayerManager().createKonPlayer(bukkitPlayer);
        } else {
        	ResultSet playerInfo = select("players", "uuid", bukkitPlayer.getUniqueId().toString());
        	String kingdomName = "";
    		String exileKingdomName = "";
    		boolean isBarbarian = true;
    		String mainPrefix = "";
    		boolean enablePrefix = false;
    		try {
                while (playerInfo.next()) {
                	kingdomName = playerInfo.getString("kingdom");
                	exileKingdomName = playerInfo.getString("exileKingdom");
                	isBarbarian = playerInfo.getBoolean("barbarian");
                	mainPrefix = playerInfo.getString("prefix");
                	enablePrefix = playerInfo.getBoolean("prefixOn");
                }
                //ChatUtil.printDebug("SQL Imported player info: "+kingdomName+","+exileKingdomName+","+isBarbarian+","+mainPrefix+","+enablePrefix);
            } catch (SQLException e) {
                e.printStackTrace();
                ChatUtil.printDebug("Aborting player import "+bukkitPlayer.getName());
                return;
            }
    		if(kingdomName==null) { kingdomName = getKonquest().getKingdomManager().getBarbarians().getName(); }
    		if(exileKingdomName==null) { exileKingdomName = getKonquest().getKingdomManager().getBarbarians().getName(); }
        	// Create a player from existing info
    		player = getKonquest().getPlayerManager().importKonPlayer(bukkitPlayer, kingdomName, exileKingdomName, isBarbarian);
    		// Get stats and directives for the player
            ResultSet stats = select("stats", "uuid", bukkitPlayer.getUniqueId().toString());
            ResultSet directives = select("directives", "uuid", bukkitPlayer.getUniqueId().toString());
            String allDirectives = "";
            String allStats = "";
            try {
                while (stats.next()) {
                	for(KonStatsType statEnum : KonStatsType.values()) {
                		int statProgress = stats.getInt(statEnum.toString());
                		player.getPlayerStats().setStat(statEnum, statProgress);
                		allStats = allStats+statEnum.toString()+":"+statProgress+",";
                	}
                }
                while (directives.next()) {
                	for(KonDirective dirEnum : KonDirective.values()) {
                		int directiveProgress = directives.getInt(dirEnum.toString());
                		player.setDirectiveProgress(dirEnum, directiveProgress);
                		allDirectives = allDirectives+dirEnum.toString()+":"+directiveProgress+",";
                	}
                }
            } catch (SQLException e) {
                e.printStackTrace();
                ChatUtil.printDebug("Could not get stats and directives for "+bukkitPlayer.getName());
            }
            //ChatUtil.printDebug("Player "+bukkitPlayer.getName()+" stats = "+allStats);
            //ChatUtil.printDebug("Player "+bukkitPlayer.getName()+" directives = "+allDirectives);
    		// Add valid prefixes to the player based on stats
        	getKonquest().getAccomplishmentManager().initPlayerPrefixes(player);
            // Update player's main prefix
        	if(mainPrefix != null && mainPrefix != "" && getKonquest().getAccomplishmentManager().isEnabled()) {
        		boolean status = player.getPlayerPrefix().setPrefix(KonPrefixType.getPrefix(mainPrefix)); // Defaults to default prefix defined in KonPrefixType if mainPrefix is not a valid enum
        		if(!status) {
        			ChatUtil.printDebug("Failed to assign main prefix "+mainPrefix+" to player "+bukkitPlayer.getName());
        			// Schedule messages to display after 20-tick delay (1 second)
        	    	Bukkit.getScheduler().scheduleSyncDelayedTask(getKonquest().getPlugin(), new Runnable() {
        	            @Override
        	            public void run() {
        	            	//ChatUtil.sendError(bukkitPlayer, "Your prefix has been reverted to default.");
        	            	ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_PREFIX_ERROR_DEFAULT.getMessage());
        	            }
        	        }, 20);
        		}
        	} else {
        		enablePrefix = false;
        	}
        	player.getPlayerPrefix().setEnable(enablePrefix);
        }
        if(player == null) {
        	ChatUtil.printDebug("Bad fetch of null player "+bukkitPlayer.getName());
        	return;
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
        } catch (SQLException e) {
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
    
    
    /*public void createPlayerData(KonPlayer player) {
        Player bukkitPlayer = player.getBukkitPlayer();
        String uuid = bukkitPlayer.getUniqueId().toString();
        String displayName = bukkitPlayer.getDisplayName();
        insert("players", new String[] {"uuid", "name"}, new String[] {uuid, displayName});
        insert("stats", new String[] {"uuid"}, new String[] {uuid});
    }*/
    
    public void createPlayerData(Player bukkitPlayer) {
        String uuid = bukkitPlayer.getUniqueId().toString();
        insert("players", new String[] {"uuid"}, new String[] {uuid});
        insert("stats", new String[] {"uuid"}, new String[] {uuid});
        insert("directives", new String[] {"uuid"}, new String[] {uuid});
    }

    /*public void flushPlayerData(KonPlayer player) {
    	ChatUtil.printDebug("Flushing player database for "+player.getBukkitPlayer().getDisplayName());
        String playerUUIDString = player.getBukkitPlayer().getUniqueId().toString();
        KonStats playerStats = player.getPlayerStats();
        for(KonStatsType statEnum : KonStatsType.values()) {
        	set("stats", statEnum.toString(), Integer.toString(playerStats.getStat(statEnum)), "uuid", playerUUIDString);
    	}
    }*/
    
    public void flushPlayerData(Player bukkitPlayer) {
    	//ChatUtil.printDebug("Flushing player database for "+bukkitPlayer.getDisplayName());
        String playerUUIDString = bukkitPlayer.getUniqueId().toString();
        KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        String[] col;
        String[] val;
        int i;
        
        // Flush player data into players table
        col  = new String[] {"kingdom","exileKingdom","barbarian","prefix","prefixOn"};
        val  = new String[col.length];
        val[0] = "'"+player.getKingdom().getName()+"'";
        val[1] = "'"+player.getExileKingdom().getName()+"'";
        val[2] = player.isBarbarian() ? "1" : "0";
        val[3] = "'"+player.getPlayerPrefix().getMainPrefix().toString()+"'";
        val[4] = player.getPlayerPrefix().isEnabled() ? "1" : "0";
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
    }
    
}
