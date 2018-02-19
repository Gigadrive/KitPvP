package eu.thechest.kitpvp;

import eu.thechest.chestapi.ChestAPI;
import eu.thechest.chestapi.maps.Map;
import eu.thechest.chestapi.maps.MapLocationData;
import eu.thechest.chestapi.maps.MapRatingManager;
import eu.thechest.chestapi.mysql.MySQLManager;
import eu.thechest.chestapi.server.GameState;
import eu.thechest.chestapi.server.GameType;
import eu.thechest.chestapi.server.ServerSettingsManager;
import eu.thechest.chestapi.server.ServerUtil;
import eu.thechest.chestapi.user.ChestUser;
import eu.thechest.chestapi.util.ParticleEffect;
import eu.thechest.kitpvp.cmd.MainExecutor;
import eu.thechest.kitpvp.inv.KitBuyConfirmation;
import eu.thechest.kitpvp.inv.KitSelectionMenu;
import eu.thechest.kitpvp.kit.KitStorage;
import eu.thechest.kitpvp.listener.MainListener;
import eu.thechest.kitpvp.user.KitPlayer;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.FileUtil;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Created by zeryt on 20.02.2017.
 */
public class KitPvP extends JavaPlugin {
    private static KitPvP instance;
    public Location lobbyLocation;

    public ArrayList<Material> DISALLOWED_BLOCKS = new ArrayList<Material>();
    public static boolean FALL_DAMAGE = true;

    public static String mapToTeleportTo = null;
    public static ArrayList<MapLocationData> spawnpoints = null;
    public static Map currentMap = null;
    public static String currentMapWorldName = null;

    public static int MAP_CHANGE_COUNTDOWN = 7*60;
    public static int MAP_RELOADS_UNTIL_RESTART = 15;

    public boolean maySwitch = true;

    public void onEnable(){
        saveDefaultConfig();

        instance = this;

        ServerSettingsManager.AUTO_OP = true;
        ServerSettingsManager.setMaxPlayers(24);
        ServerSettingsManager.VIP_JOIN = false;
        ServerSettingsManager.updateGameState(GameState.JOINABLE);
        ServerSettingsManager.ADJUST_CHAT_FORMAT = true;
        ServerSettingsManager.RUNNING_GAME = GameType.KITPVP;
        ServerSettingsManager.UPDATE_TAB_NAME_WITH_SCOREBOARD = true;
        ServerSettingsManager.ARROW_TRAILS = true;
        ServerSettingsManager.KILL_EFFECTS = true;
        ServerSettingsManager.SHOW_LEVEL_IN_EXP_BAR = true;

        DISALLOWED_BLOCKS.add(Material.BREWING_STAND);
        DISALLOWED_BLOCKS.add(Material.FURNACE);
        DISALLOWED_BLOCKS.add(Material.BURNING_FURNACE);
        DISALLOWED_BLOCKS.add(Material.WORKBENCH);
        DISALLOWED_BLOCKS.add(Material.TRAP_DOOR);
        DISALLOWED_BLOCKS.add(Material.CHEST);
        DISALLOWED_BLOCKS.add(Material.TRAPPED_CHEST);
        //DISALLOWED_BLOCKS.add(Material.FENCE_GATE);
        //DISALLOWED_BLOCKS.add(Material.SPRUCE_FENCE_GATE);
        //DISALLOWED_BLOCKS.add(Material.BIRCH_FENCE_GATE);
        //DISALLOWED_BLOCKS.add(Material.JUNGLE_FENCE_GATE);
        //DISALLOWED_BLOCKS.add(Material.DARK_OAK_FENCE_GATE);
        //DISALLOWED_BLOCKS.add(Material.ACACIA_FENCE_GATE);
        DISALLOWED_BLOCKS.add(Material.DIODE_BLOCK_OFF);
        DISALLOWED_BLOCKS.add(Material.DIODE_BLOCK_ON);
        DISALLOWED_BLOCKS.add(Material.REDSTONE_COMPARATOR_OFF);
        DISALLOWED_BLOCKS.add(Material.REDSTONE_COMPARATOR_ON);
        DISALLOWED_BLOCKS.add(Material.HOPPER);
        DISALLOWED_BLOCKS.add(Material.DROPPER);
        DISALLOWED_BLOCKS.add(Material.DISPENSER);
        DISALLOWED_BLOCKS.add(Material.BED_BLOCK);
        DISALLOWED_BLOCKS.add(Material.BEACON);
        DISALLOWED_BLOCKS.add(Material.ANVIL);
        DISALLOWED_BLOCKS.add(Material.ENCHANTMENT_TABLE);
        //DISALLOWED_BLOCKS.add(Material.STONE_BUTTON);
        //DISALLOWED_BLOCKS.add(Material.WOOD_BUTTON);
        DISALLOWED_BLOCKS.add(Material.JUKEBOX);
        DISALLOWED_BLOCKS.add(Material.NOTE_BLOCK);
        DISALLOWED_BLOCKS.add(Material.LEVER);

        for(World w : Bukkit.getWorlds()){
            prepareWorld(w);
        }

        registerListeners();
        registerCommands();

        lobbyLocation = new Location(Bukkit.getWorld(getConfig().getString("lobbyLocation.world")), getConfig().getDouble("lobbyLocation.x"), getConfig().getDouble("lobbyLocation.y"), getConfig().getDouble("lobbyLocation.z"), getConfig().getInt("lobbyLocation.yaw"), getConfig().getInt("lobbyLocation.pitch"));

        startMapChangeScheduler();

        KitStorage.init();
    }

    public void onDisable(){
        for(Player p : Bukkit.getOnlinePlayers()) {
            if(KitPlayer.getStorage().containsKey(p)){
                KitPlayer k = KitPlayer.get(p);

                k.saveData();
                KitPlayer.unregister(k);
            }
        }

        if(currentMap != null){
            currentMap.removeMap(currentMapWorldName);
        }
    }

    public void setLobbyLocation(Location loc){
        this.lobbyLocation = loc;

        getConfig().set("lobbyLocation.world", loc.getWorld().getName());
        getConfig().set("lobbyLocation.x", loc.getX());
        getConfig().set("lobbyLocation.y", loc.getY());
        getConfig().set("lobbyLocation.z", loc.getZ());
        getConfig().set("lobbyLocation.yaw", loc.getYaw());
        getConfig().set("lobbyLocation.pitch", loc.getPitch());
        saveConfig();
    }

    public void prepareWorld(World w){
        w.setStorm(false);
        w.setPVP(true);
        w.setAmbientSpawnLimit(0);
        w.setAnimalSpawnLimit(0);
        w.setMonsterSpawnLimit(0);
        w.setDifficulty(Difficulty.EASY);
        w.setTime(0L);

        w.setGameRuleValue("commandBlockOutput", "false");
        w.setGameRuleValue("doDaylightCycle", "true");
        w.setGameRuleValue("doFireTick", "false");
        w.setGameRuleValue("doTileDrops", "false");
        w.setGameRuleValue("doMobSpawning", "false");
        w.setGameRuleValue("mobGriefing", "false");
        w.setGameRuleValue("naturalRegeneration", "true");

        for(Entity e : w.getEntities()){
            if(e.getType() != EntityType.ITEM_FRAME && e.getType() != EntityType.PAINTING && e.getType() != EntityType.ARMOR_STAND){
                e.remove();
            }
        }
    }

    // outcome = [ 1 = win, 0,5 = tie, 0 = lose ]
    // k = 20 [ https://de.wikipedia.org/wiki/Elo-Zahl ]
    public static int calculateRating(int p1Rating, int p2Rating, int outcome, double k){
        int diff = p1Rating - p2Rating;
        double expected = (double) (1.0 / (Math.pow(10.0, -diff / 400.0) + 1));
        //return (int) Math.round(p1Rating + k*(outcome - expected));
        int result = (int) Math.round(k*(outcome - expected));
        if(result == 0) result = 2;
        return result;
    }

    private void registerListeners(){
        Bukkit.getPluginManager().registerEvents(new MainListener(), this);
        Bukkit.getPluginManager().registerEvents(new KitSelectionMenu(), this);
        Bukkit.getPluginManager().registerEvents(new KitBuyConfirmation(), this);
    }

    private void registerCommands(){
        MainExecutor exec = new MainExecutor();
        getCommand("setlobby").setExecutor(exec);
        getCommand("kit").setExecutor(exec);
        getCommand("autorespawn").setExecutor(exec);
    }

    public static KitPvP getInstance(){
        return instance;
    }

    private void startMapChangeScheduler(){
        switchMap();
        ServerUtil.updateMapName(currentMap.getName());

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){
            public void run(){
                if(MAP_CHANGE_COUNTDOWN > 0){
                    if(MAP_CHANGE_COUNTDOWN == 60 || MAP_CHANGE_COUNTDOWN == 30 || MAP_CHANGE_COUNTDOWN == 20 || MAP_CHANGE_COUNTDOWN == 10 || MAP_CHANGE_COUNTDOWN == 5 || MAP_CHANGE_COUNTDOWN == 4 || MAP_CHANGE_COUNTDOWN == 3 || MAP_CHANGE_COUNTDOWN == 2 || MAP_CHANGE_COUNTDOWN == 1){
                        for(Player p : Bukkit.getOnlinePlayers()){
                            KitPlayer k = KitPlayer.get(p);
                            ChestUser u = ChestUser.getUser(p);

                            if(MAP_RELOADS_UNTIL_RESTART == 0){
                                p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + u.getTranslatedMessage("Server Restart in %s!").replace("%s",ChatColor.YELLOW.toString() + MAP_CHANGE_COUNTDOWN + "s" + ChatColor.GREEN));
                            } else {
                                if(k.kit != null){
                                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + u.getTranslatedMessage("New Map in %s!").replace("%s",ChatColor.YELLOW.toString() + MAP_CHANGE_COUNTDOWN + "s" + ChatColor.GREEN));
                                }

                                if(MAP_CHANGE_COUNTDOWN == 30){
                                    switchMap();
                                }
                            }
                        }
                    }

                    MAP_CHANGE_COUNTDOWN--;
                } else {
                    if(MAP_RELOADS_UNTIL_RESTART == 0){
                        restart();
                    } else {
                        MAP_CHANGE_COUNTDOWN = 7*60;
                        MAP_RELOADS_UNTIL_RESTART--;
                        ServerUtil.updateMapName(currentMap.getName());
                        mapToTeleportTo = currentMapWorldName;
                        spawnpoints = currentMap.getSpawnpoints();
                        teleportPlayersToMap();
                    }
                }
            }
        }, 1L, 20L);
    }

    private void switchMap(){
        if(!maySwitch) return;

        maySwitch = false;
        new BukkitRunnable(){
            @Override
            public void run() {
                maySwitch = true;
            }
        }.runTaskLater(this,40*20);

        try {
            int mapID = -1;
            PreparedStatement ps = null;
            boolean isFirstMap = false;
            Map oldWorld = null;
            String oldWorldName = null;

            if(currentMap != null && currentMapWorldName != null){
                oldWorld = currentMap;
                oldWorldName = currentMapWorldName;

                ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `maps` WHERE `mapType` = ? AND `id` != ? AND `active` = ? ORDER BY RAND()");
                ps.setString(1,"KITPVP");
                ps.setInt(2,currentMap.getID());
                ps.setBoolean(3,true);
            } else {
                isFirstMap = true;

                ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `maps` WHERE `mapType` = ? AND `active` = ? ORDER BY RAND()");
                ps.setString(1,"KITPVP");
                ps.setBoolean(2,true);
            }

            ResultSet rs = ps.executeQuery();

            if(rs.first()){
                mapID = rs.getInt("id");
            }

            MySQLManager.getInstance().closeResources(rs,ps);

            if(mapID != -1){
                if(currentMap != null){
                    if(mapID == currentMap.getID()) return;
                }

                Map map = Map.getMap(mapID);
                currentMapWorldName = map.loadMapToServer();

                Bukkit.getScheduler().scheduleSyncDelayedTask(KitPvP.getInstance(), new Runnable(){
                    public void run(){
                        if(Bukkit.getWorld(currentMapWorldName) != null)
                            KitPvP.getInstance().prepareWorld(Bukkit.getWorld(currentMapWorldName));
                    }
                }, 6*20);

                if(!isFirstMap){
                    Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable(){
                        public void run(){
                            currentMap = map;
                            MapRatingManager.MAP_TO_RATE = currentMap;
                        }
                    }, 25*20);

                    if(oldWorld != null && oldWorldName != null){

                        final Map o = oldWorld;
                        final String oW = oldWorldName;

                        Bukkit.getScheduler().scheduleSyncDelayedTask(KitPvP.getInstance(), new Runnable(){
                            public void run(){
                                o.removeMap(oW);
                            }
                        }, 60*20);
                    }
                } else {
                    currentMap = map;
                    MapRatingManager.MAP_TO_RATE = currentMap;
                    mapToTeleportTo = currentMapWorldName;
                    spawnpoints = currentMap.getSpawnpoints();
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void teleportPlayersToMap(){
        ArrayList<MapLocationData> spawnpoints = KitPvP.currentMap.getSpawnpoints();

        for(Player all : Bukkit.getOnlinePlayers()){
            if(KitPlayer.get(all).kit != null){
                if(spawnpoints.size() > 0){
                    Collections.shuffle(spawnpoints);
                    all.teleport(spawnpoints.get(0).toBukkitLocation(mapToTeleportTo));
                } else {
                    all.teleport(Bukkit.getWorld(mapToTeleportTo).getSpawnLocation());
                }

                currentMap.sendMapCredits(all);
                currentMap.sendRateMapInfo(all);

                if(ChestUser.getUser(all).hasGamePerk(4)) all.setHealth(all.getMaxHealth());
            }
        }
    }

    private void restart(){
        for(Player all : Bukkit.getOnlinePlayers()){
            ChestUser.getUser(all).connectToLobby();
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(this,new Runnable(){
            public void run(){
                //Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"restart");
                ChestAPI.stopServer();
            }
        }, 4*20);
    }
}
