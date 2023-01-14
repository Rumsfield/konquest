package com.github.rumsfield.konquest.database;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;

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
    	String dbType = konquest.getConfigManager().getConfig("core").getString("core.database.connection","sqlite");
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
