package com.github.rumsfield.konquest.utility;

import org.bukkit.*;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * A class with static helper methods
 */
public class HelperUtil {

    /*
     * Chunk / Point Methods
     */

    /**
     * Gets chunks around loc, (2r-1)^2 chunks squared
     * @param loc location of area
     * @param radius radius of area around loc
     * @return (2r-1)^2 chunks squared
     */
    public static ArrayList<Chunk> getAreaChunks(Location loc, int radius) {
        ArrayList<Chunk> areaChunks = new ArrayList<>();
        if (loc.getWorld() == null) return areaChunks;
        areaChunks.add(loc.getChunk());
        int curX = loc.getChunk().getX();
        int curZ = loc.getChunk().getZ();
        if(radius > 0) {
            int min = (radius-1)*-1;
            int max = (radius-1);
            for(int x=min;x<=max;x++) {
                for(int z=min;z<=max;z++) {
                    if(x != 0 || z != 0) {
                        areaChunks.add(loc.getWorld().getChunkAt(curX+x, curZ+z));
                    }
                }
            }
        }
        return areaChunks;
    }

    public static ArrayList<Point> getAreaPoints(Location loc, int radius) {
        ArrayList<Point> areaPoints = new ArrayList<>();
        Point center = toPoint(loc);
        areaPoints.add(center);
        if(radius > 0) {
            int min = (radius-1)*-1;
            int max = (radius-1);
            for(int x=min;x<=max;x++) {
                for(int z=min;z<=max;z++) {
                    if(x != 0 || z != 0) {
                        areaPoints.add(new Point(center.x + x, center.y + z));
                    }
                }
            }
        }
        return areaPoints;
    }

    /**
     * Gets chunks surrounding loc, (2r-1)^2-1 chunks squared
     * @param loc location of area
     * @param radius radius of area around loc
     * @return (2r-1)^2 chunks squared
     */
    public static ArrayList<Chunk> getSurroundingChunks(Location loc, int radius) {
        ArrayList<Chunk> areaChunks = new ArrayList<>();
        if (loc.getWorld() == null) return areaChunks;
        int curX = loc.getChunk().getX();
        int curZ = loc.getChunk().getZ();
        if(radius > 0) {
            int min = (radius-1)*-1;
            int max = (radius-1);
            for(int x=min;x<=max;x++) {
                for(int z=min;z<=max;z++) {
                    if(x != 0 || z != 0) {
                        areaChunks.add(loc.getWorld().getChunkAt(curX+x, curZ+z));
                    }
                }
            }
        }
        return areaChunks;
    }

    public static ArrayList<Point> getBorderPoints(Location loc, int radius) {
        ArrayList<Point> areaPoints = new ArrayList<>();
        Point center = toPoint(loc);
        if(radius > 0) {
            int min = (radius-1)*-1;
            int max = (radius-1);
            for(int x=min;x<=max;x++) {
                for(int z=min;z<=max;z++) {
                    if(x == min || z == min || x == max || z == max) {
                        areaPoints.add(new Point(center.x + x, center.y + z));
                    }
                }
            }
        }
        return areaPoints;
    }

    public static ArrayList<Point> getSidePoints(Location loc) {
        ArrayList<Point> sidePoints = new ArrayList<>();
        Point center = toPoint(loc);
        int[] coordLUTX = {0,1,0,-1};
        int[] coordLUTZ = {1,0,-1,0};
        for(int i = 0;i<4;i++) {
            sidePoints.add(new Point(center.x + coordLUTX[i], center.y + coordLUTZ[i]));
        }
        return sidePoints;
    }

    public static Point toPoint(Location loc) {
        return new Point((int)Math.floor((double)loc.getBlockX()/16),(int)Math.floor((double)loc.getBlockZ()/16));
    }

    public static Point toPoint(Chunk chunk) {
        return new Point(chunk.getX(),chunk.getZ());
    }

    public static int chunkDistance(Location loc1, Location loc2) {
        if (loc1.getWorld() == null) return -1;
        if(loc1.getWorld().getName().equals(loc2.getWorld().getName())) {
            int loc1X = (int)Math.floor((double)loc1.getBlockX()/16);
            int loc1Z = (int)Math.floor((double)loc1.getBlockZ()/16);
            int loc2X = (int)Math.floor((double)loc2.getBlockX()/16);
            int loc2Z = (int)Math.floor((double)loc2.getBlockZ()/16);
            return Math.max(Math.abs(loc1X - loc2X), Math.abs(loc1Z - loc2Z));
        } else {
            return -1;
        }
    }

    /*
     * String Formatting Methods
     */

    public static String formatPointsToString(Collection<Point> points) {
        StringBuilder result = new StringBuilder();
        for(Point point : points) {
            int x = (int)point.getX();
            int y = (int)point.getY();
            result.append(x).append(",").append(y).append(".");
        }
        return result.toString();
    }

    public static ArrayList<Point> formatStringToPoints(String coords) {
        ArrayList<Point> points = new ArrayList<>();
        String[] coord_list = coords.split("\\.");
        for(String coord : coord_list) {
            if(!coord.equals("")) {
                String[] coord_pair = coord.split(",");
                int x = Integer.parseInt(coord_pair[0]);
                int z = Integer.parseInt(coord_pair[1]);
                points.add(new Point(x,z));
            }
        }
        return points;
    }

    public static String formatLocationsToString(Collection<Location> locs) {
        StringBuilder result = new StringBuilder();
        for(Location loc : locs) {
            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();
            result.append(x).append(",").append(y).append(",").append(z).append(".");
        }
        return result.toString();
    }

    public static ArrayList<Location> formatStringToLocations(String coords, World world) {
        ArrayList<Location> locations = new ArrayList<>();
        String[] coord_list = coords.split("\\.");
        for(String coord : coord_list) {
            if(!coord.equals("")) {
                String[] coord_pair = coord.split(",");
                int x = Integer.parseInt(coord_pair[0]);
                int y = Integer.parseInt(coord_pair[1]);
                int z = Integer.parseInt(coord_pair[2]);
                // Add location in primary world by default
                locations.add(new Location(world,x,y,z));
            }
        }
        return locations;
    }

    public static UUID idFromString(String id) {
        UUID result = null;
        try {
            result = UUID.fromString(id);
        } catch(IllegalArgumentException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static List<String> stringPaginate(String sentence) {
        ArrayList<String> result = new ArrayList<>();
        String[] words = sentence.split(" ");
        StringBuilder line = new StringBuilder();
        // create lines no more than 30 characters (including spaces) long
        for(int i=0;i<words.length;i++) {
            String test = line + words[i];
            if(i == words.length-1) {
                if(test.length() > 30) {
                    result.add(line.toString().trim());
                    result.add(words[i].trim());
                } else {
                    result.add(test.trim());
                }
            } else {
                if(test.length() > 30) {
                    result.add(line.toString().trim());
                    line = new StringBuilder(words[i] + " ");
                } else {
                    line.append(words[i]).append(" ");
                }
            }
        }
        return result;
    }

    public static List<String> stringPaginate(String sentence, String format) {
        ArrayList<String> result = new ArrayList<>();
        List<String> lines = stringPaginate(sentence);
        for(String line : lines) {
            result.add(format+line);
        }
        return result;
    }

    public static List<String> stringPaginate(String sentence, ChatColor format) {
        String formatStr = ""+format;
        return stringPaginate(sentence,formatStr);
    }

    public static String getTimeFormat(int valSeconds, ChatColor color) {
        return getTimeFormat(valSeconds, ""+color);
    }

    public static String getTimeFormat(int valSeconds, String color) {
        int days = valSeconds / 86400;
        int hours = valSeconds % 86400 / 3600;
        int minutes = valSeconds % 3600 / 60;
        int seconds = valSeconds % 60;

        String nColor;
        String numColor;
        String result;
        String format;
        if(color != null && !color.equals("")) {
            nColor = ""+ChatColor.GRAY;
            numColor = color;
            if(valSeconds <= 30) {
                numColor = ""+ChatColor.DARK_RED;
            }
        } else {
            nColor = "";
            numColor = "";
        }

        if(days != 0) {
            format = numColor+"%03d"+nColor+"D:"+numColor+"%02d"+nColor+"H:"+numColor+"%02d"+nColor+"M:"+numColor+"%02d"+nColor+"S";
            result = String.format(format, days, hours, minutes, seconds);
        } else if(hours != 0) {
            format = numColor+"%02d"+nColor+"H:"+numColor+"%02d"+nColor+"M:"+numColor+"%02d"+nColor+"S";
            result = String.format(format, hours, minutes, seconds);
        } else if(minutes != 0) {
            format = numColor+"%02d"+nColor+"M:"+numColor+"%02d"+nColor+"S";
            result = String.format(format, minutes, seconds);
        } else {
            format = numColor+"%02d"+nColor+"S";
            result = String.format(format, seconds);
        }

        return result;
    }

    /*
     * Miscellaneous Methods
     */

    public static String getLastSeenFormat(OfflinePlayer offlineBukkitPlayer) {
        Date date = new Date(); // Now
        if(!offlineBukkitPlayer.isOnline()) {
            date = new Date(offlineBukkitPlayer.getLastPlayed()); // Last joined
        }
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy HH:mm");
        return formatter.format(date);
    }

}
