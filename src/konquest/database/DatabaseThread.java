package konquest.database;

import konquest.Konquest;
import konquest.model.KonPlayer;
import konquest.utility.ChatUtil;

public class DatabaseThread implements Runnable {
    private final Konquest konquest;

    private KonquestDB database;
    private Thread thread;

    private boolean running = false;

    public DatabaseThread(Konquest konquest) {
        this.konquest = konquest;
        thread = new Thread(this);
    }

    public void run() {
        running = true;

        createDatabase();
        database.initialize();

        Thread databaseFlusher = new Thread(new Runnable() {
            //int sleep = konquest.getConfigManager().getCoreConfig().getDatabaseFlushInterval();
        	int sleep = 3600; // default 60 minutes
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(sleep*1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    flushDatabase();
                }
            }
        });
        databaseFlusher.start();
    }
    
    public boolean isRunning() {
    	return running;
    }

    public void createDatabase() {
        //CoreConfig coreConfig = novsWar.getConfigManager().getCoreConfig();
        //String prefix = coreConfig.getDatabasePrefix();
        //database = new NovswarDB(konquest, type, prefix);
    	database = new KonquestDB(konquest);
    }

    public Thread getThread() {
        return thread;
    }

    public KonquestDB getDatabase() {
        return database;
    }

    public void flushDatabase() {
    	ChatUtil.printDebug("Flushing entire database for all online players");
        for (KonPlayer player : konquest.getPlayerManager().getPlayersOnline()) {
            database.flushPlayerData(player.getBukkitPlayer());
        }
    }
}
