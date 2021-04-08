package konquest.utility;

import konquest.Konquest;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;

public class Timer {

	private BukkitScheduler scheduler;
    private Runnable task;
    private int taskID;
    private int time;
    private int looptime;
    private Timeable timekeeper;
    private boolean isRunning;
    
    public Timer(Timeable timekeeper) {
    	this.timekeeper = timekeeper;
    	this.time = -1;
    	this.looptime = -1;
    	this.taskID = 0;
    	this.scheduler = Bukkit.getScheduler();
    	this.isRunning = false;
    }
    
    public int getTime() {
        return time;
    }
    
    public int getLoopTime() {
    	return looptime;
    }

    public void setTime(int time) {
        this.time = time;
        this.looptime = time;
    }

    public int getSeconds() {
        return time % 60;
    }

    public int getMinutes() {
        return time / 60;
    }

    public Runnable getTask() {
        return task;
    }

    public int getTaskID() {
        return taskID;
    }
    
    public boolean isRunning() {
    	return isRunning;
    }
    
    public void startTimer() {
        if (taskID != 0 && time > -1) {
            Bukkit.getLogger().severe("Warning: Attempted to start Timer when one has already been started. Cancelling...");
            return;
        }

        task = new Runnable() {
            public void run() {
                time--;
                if (time <= -1) {
                    timekeeper.onEndTimer(taskID);
                    stopTimer();
                }
            }
        };
        taskID = scheduler.scheduleSyncRepeatingTask(Konquest.getInstance().getPlugin(), task, 0, 20);
        isRunning = true;
    }
    
    public void startLoopTimer() {
        if (taskID != 0 && time > -1) {
            Bukkit.getLogger().severe("Warning: Attempted to start Timer when one has already been started. Cancelling...");
            return;
        }

        task = new Runnable() {
            public void run() {
                time--;
                if (time <= -1) {
                    timekeeper.onEndTimer(taskID);
                    time = looptime;
                }
                //ChatUtil.printDebug("Running timer loop task, time is "+time);
            }
        };
        taskID = scheduler.scheduleSyncRepeatingTask(Konquest.getInstance().getPlugin(), task, 0, 20);
        isRunning = true;
    }
    
    public void startLoopTimer(int ticks) {
        if (taskID != 0 && time > -1) {
            Bukkit.getLogger().severe("Warning: Attempted to start Timer when one has already been started. Cancelling...");
            return;
        }

        task = new Runnable() {
            public void run() {
                time--;
                if (time <= -1) {
                    timekeeper.onEndTimer(taskID);
                    time = looptime;
                }
                //ChatUtil.printDebug("Running timer loop task, time is "+time);
            }
        };
        taskID = scheduler.scheduleSyncRepeatingTask(Konquest.getInstance().getPlugin(), task, 0, ticks);
        isRunning = true;
    }


    public void stopTimer() {
        time = -1;
        looptime = -1;
        scheduler.cancelTask(taskID);
        taskID = 0;
        isRunning = false;
    }
    
}
