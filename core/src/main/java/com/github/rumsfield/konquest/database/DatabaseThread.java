package com.github.rumsfield.konquest.database;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import org.bukkit.Bukkit;

public class DatabaseThread implements Runnable {
    private final Konquest konquest;

    private KonquestDB database;
    private final Thread thread;
    private int sleepSeconds;
    private boolean running = false;

    public DatabaseThread(Konquest konquest) {
        this.konquest = konquest;
        this.sleepSeconds = 3600;
        thread = new Thread(this);
    }

    public void run() {
        running = true;
        // Start timeout check for database ready
        long delayTicks = 10*20; // 10 seconds
        Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), () -> {
            // Actions to run after delay
            if (database.isReady()) {
                ChatUtil.printConsoleAlert("Konquest Database passed startup check.");
            } else {
                ChatUtil.printConsoleError("Something went wrong when starting the Konquest Database, and the plugin will not behave correctly! Please report this to the Konquest developer.");
            }
        }, delayTicks);

        createDatabase();
        database.initialize();

        //TODO - look at exchanging this for a Schedule every sleepSeconds, instead of using infinite loop
        Thread databaseFlusher = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(sleepSeconds* 1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                flushDatabase();
            }
        });
        databaseFlusher.start();
    }
    
    public boolean isRunning() {
    	return running;
    }

    public void createDatabase() {
    	String dbType = konquest.getCore().getString(CorePath.DATABASE_CONNECTION.getPath(),"sqlite");
    	DatabaseType type = DatabaseType.getType(dbType);
    	database = new KonquestDB(type,konquest);
    }

    public Thread getThread() {
        return thread;
    }

    public KonquestDB getDatabase() {
        return database;
    }
    
    public void setSleepSeconds(int val) {
    	sleepSeconds = val;
    }

    public void flushDatabase() {
    	ChatUtil.printDebug("Flushing entire database for all online players");
        for (KonPlayer player : konquest.getPlayerManager().getPlayersOnline()) {
            database.flushPlayerData(player.getBukkitPlayer());
        }
    }
}
