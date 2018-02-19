package eu.thechest.kitpvp.user;

import eu.thechest.chestapi.ChestAPI;
import eu.thechest.chestapi.items.ItemUtil;
import eu.thechest.chestapi.mysql.MySQLManager;
import eu.thechest.chestapi.user.ChestUser;
import eu.thechest.chestapi.util.StringUtils;
import eu.thechest.kitpvp.KitPvP;
import eu.thechest.kitpvp.kit.Bomberman;
import eu.thechest.kitpvp.kit.Kit;
import eu.thechest.kitpvp.kit.KitStorage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by zeryt on 20.02.2017.
 */
public class KitPlayer {
    private static HashMap<Player,KitPlayer> STORAGE = new HashMap<Player,KitPlayer>();

    public static HashMap<Player,KitPlayer> getStorage(){
        return STORAGE;
    }

    public static KitPlayer get(Player p){
        if(STORAGE.containsKey(p)){
            return STORAGE.get(p);
        } else {
            KitPlayer k = new KitPlayer(p);

            STORAGE.put(p, k);
            return k;
        }
    }

    public static void unregister(KitPlayer k){
        if(STORAGE.containsValue(k)){
            for(BukkitTask t : k.schedulers){
                t.cancel();
            }

            k.schedulers.clear();

            k.saveData();

            STORAGE.remove(k.getPlayer());
        }
    }

    private Player p;

    public int startKills;
    public int startDeaths;
    public int startPoints;
    public int highestStreak;

    public Kit kit;

    public int kills;
    public int deaths;
    public int points;
    public int killstreak;

    public ArrayList<BukkitTask> schedulers;
    public ArrayList<Kit> available_kits;

    public boolean isAbilityInCooldown;
    public int abilityCooldown;
    public boolean maySwitchKit;
    public boolean isIngame;
    public Player lastDamager;
    public boolean hulkInAir = false;
    public boolean allowSaveData = true;
    public boolean autoRespawn = true;
    public boolean spawnProtection = false;

    public Timestamp firstJoin;

    public KitPlayer(Player p){
        if(STORAGE.containsKey(p)) return;

        this.p = p;
        this.schedulers = new ArrayList<BukkitTask>();
        this.available_kits = new ArrayList<Kit>();
        this.killstreak = 0;

        isAbilityInCooldown = false;
        abilityCooldown = 0;
        isIngame = false;
        maySwitchKit = true;

        kills = 0;
        deaths = 0;
        points = 0;

        startPoints = 100;
        startKills = 0;
        startDeaths = 0;
        highestStreak = 0;

        firstJoin = new Timestamp(System.currentTimeMillis());

        kit = null;

        try {
            PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `kpvp_stats` WHERE `uuid` = ?");
            ps.setString(1,p.getUniqueId().toString());
            ResultSet rs = ps.executeQuery();

            if(rs.first()){
                startPoints = rs.getInt("points");
                startKills = rs.getInt("kills");
                startDeaths = rs.getInt("deaths");
                highestStreak = rs.getInt("highestStreak");
                firstJoin = rs.getTimestamp("firstJoin");
                autoRespawn = rs.getBoolean("autoRespawn");

                loadAvailableKits();
            } else {
                PreparedStatement insert = MySQLManager.getInstance().getConnection().prepareStatement("INSERT INTO `kpvp_stats` (`uuid`) VALUES (?)");
                insert.setString(1,p.getUniqueId().toString());
                insert.execute();
                insert.close();
            }

            MySQLManager.getInstance().closeResources(rs,ps);

            STORAGE.put(p,this);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    private void loadAvailableKits() {
        ChestAPI.async(() -> {
            try {
                PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("SELECT * FROM `kpvp_boughtKits` WHERE `uuid` = ?");
                ps.setString(1,p.getUniqueId().toString());
                ResultSet rs = ps.executeQuery();

                rs.beforeFirst();

                while(rs.next()){
                    Kit k = KitStorage.fromID(rs.getInt("kitID"));

                    if(k != null && !available_kits.contains(k)){
                        available_kits.add(k);
                    }
                }

                MySQLManager.getInstance().closeResources(rs,ps);
            } catch(Exception e){
                e.printStackTrace();
            }
        });
    }

    public void addKit(Kit k){
        ChestAPI.async(() -> {
            if(!available_kits.contains(k)){
                try {
                    PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("INSERT INTO `kpvp_boughtKits` (`uuid`,`kitID`) VALUES(?,?)");
                    ps.setString(1,p.getUniqueId().toString());
                    ps.setInt(2,k.getID());
                    ps.execute();
                    ps.close();

                    available_kits.add(k);
                } catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    public Player getPlayer(){
        return this.p;
    }

    public ChestUser getUser(){
        return ChestUser.getUser(p);
    }

    public BukkitTask getTask(int schedulerID){
        for(BukkitTask t : schedulers){
            if(t.getTaskId() == schedulerID){
                return t;
            }
        }

        return null;
    }

    public void startAbilityCooldown(){
        if(this.kit != null && isAbilityInCooldown == false && abilityCooldown == 0 && this.kit.hasDefensiveAbility() == false){
            isAbilityInCooldown = true;
            abilityCooldown = this.kit.getAbilityCooldown();

            BukkitTask br = new BukkitRunnable(){
                public void run(){
                    abilityCooldown--;

                    p.getInventory().setItem(8, ItemUtil.namedItem(Material.SULPHUR, ChatColor.DARK_GRAY + getUser().getTranslatedMessage("Cooldown") + ": " + ChatColor.WHITE + abilityCooldown, null, 0, abilityCooldown));

                    if(abilityCooldown == 0){
                        isAbilityInCooldown = false;
                        schedulers.remove(getTask(getTaskId()));
                        p.getInventory().setItem(8, ItemUtil.namedItem(Material.REDSTONE, ChatColor.DARK_RED.toString() + ChatColor.BOLD + ">> " + ChatColor.RED + getUser().getTranslatedMessage("ABILITY CHARGED") + ChatColor.DARK_RED.toString() + ChatColor.BOLD + " <<", null));
                        cancel();
                    }
                }
            }.runTaskTimer(KitPvP.getInstance(), 0, 1*20);

            schedulers.add(br);
        }
    }

    public void giveLobbyItems(){
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);

        p.getInventory().setItem(0,ItemUtil.namedItem(Material.ENDER_CHEST,ChatColor.GREEN + getUser().getTranslatedMessage("Select your Kit"),null));

        p.getInventory().setItem(8, ItemUtil.namedItem(Material.CHEST, org.bukkit.ChatColor.RED + getUser().getTranslatedMessage("Back to Lobby"), null));
    }

    public void updateScoreboard(){
        Objective ob = null;

        if(getUser().getScoreboard().getObjective(DisplaySlot.SIDEBAR) != null){
            getUser().getScoreboard().getObjective(DisplaySlot.SIDEBAR).unregister();
        }

        ob = getUser().getScoreboard().registerNewObjective("side", "dummy");
        ob.setDisplayName(ChatColor.YELLOW + "KitPvP");
        ob.setDisplaySlot(DisplaySlot.SIDEBAR);

        /*ob.getScore(ChatColor.GREEN + getUser().getTranslatedMessage("Points") + ":").setScore(startPoints + points);
        ob.getScore(ChatColor.GREEN + getUser().getTranslatedMessage("Kills") + ":").setScore(startKills + kills);
        ob.getScore(ChatColor.GREEN + getUser().getTranslatedMessage("Deaths") + ":").setScore(startDeaths + deaths);
        ob.getScore(ChatColor.GREEN + getUser().getTranslatedMessage("Killstreak") + ":").setScore(killstreak);*/

        ob.getScore(" ").setScore(15);
        ob.getScore(ChatColor.AQUA.toString() + ChatColor.BOLD.toString() + getUser().getTranslatedMessage("Points") + ":").setScore(14);
        ob.getScore(ChatColor.YELLOW + String.valueOf(startPoints + points)).setScore(13);
        ob.getScore("  ").setScore(12);
        ob.getScore(ChatColor.AQUA.toString() + ChatColor.BOLD.toString() + getUser().getTranslatedMessage("Kills") + ":").setScore(11);
        ob.getScore(ChatColor.YELLOW + String.valueOf(startKills + kills) + " ").setScore(10);
        ob.getScore("   ").setScore(9);
        ob.getScore(ChatColor.AQUA.toString() + ChatColor.BOLD.toString() + getUser().getTranslatedMessage("Deaths") + ":").setScore(8);
        ob.getScore(ChatColor.YELLOW + String.valueOf(startDeaths + deaths) + "  ").setScore(7);
        ob.getScore("    ").setScore(6);
        ob.getScore(ChatColor.AQUA.toString() + ChatColor.BOLD.toString() + getUser().getTranslatedMessage("Killstreak") + ":").setScore(5);
        ob.getScore(ChatColor.YELLOW + String.valueOf(killstreak) + "   ").setScore(4);
        ob.getScore("     ").setScore(3);
        ob.getScore(StringUtils.SCOREBOARD_LINE_SEPERATOR).setScore(2);
        ob.getScore(StringUtils.SCOREBOARD_FOOTER_IP).setScore(1);

        p.setScoreboard(getUser().getScoreboard());
    }

    public void achievementCheck(){
        int totalKills = startKills+kills;

        if(totalKills >= 50) getUser().achieve(42);
        if(totalKills >= 100) getUser().achieve(43);
        if(totalKills >= 200) getUser().achieve(44);
        if(totalKills >= 500) getUser().achieve(45);
        if(totalKills >= 1000) getUser().achieve(46);
    }

    public void setKit(Kit kit){
        setKit(kit,true,true);
    }

    public void setKit(Kit kit,boolean clearSched,boolean clearSpawnProtection){
        this.kit = kit;

        if(clearSched){
            for(BukkitTask t : schedulers){
                t.cancel();
            }

            schedulers.clear();
        }

        hulkInAir = false;
        if(clearSpawnProtection) spawnProtection = false;

        if(kit != null){
            restoreKit();
        }
    }

    public void restoreKit(){
        if(this.kit != null){
            p.getInventory().clear();
            p.getInventory().setArmorContents(null);

            ItemStack helmet = getHelmet();
            ItemStack chestplate = getChestplate();
            ItemStack leggings = getLeggings();
            ItemStack boots = getBoots();

            if(helmet != null) p.getInventory().setHelmet(helmet);
            if(chestplate != null) p.getInventory().setChestplate(chestplate);
            if(leggings != null) p.getInventory().setLeggings(leggings);
            if(boots != null) p.getInventory().setBoots(boots);

            ItemStack sword = new ItemStack(Material.IRON_SWORD);
            ItemMeta swordM = sword.getItemMeta();
            swordM.spigot().setUnbreakable(true);
            sword.setItemMeta(swordM);

            ItemStack bow = ItemUtil.setUnbreakable(new ItemStack(Material.BOW),true);
            ItemStack arrow = new ItemStack(Material.ARROW, 64);
            ItemStack rod = new ItemStack(Material.FISHING_ROD);
            rod.setDurability((short)48);

            p.getInventory().addItem(sword);
            p.getInventory().addItem(bow);
            p.getInventory().addItem(arrow);
            p.getInventory().addItem(arrow);

            if(kit.hasDefensiveAbility()){
                if(kit instanceof Bomberman){
                    p.getInventory().setItem(8, ItemUtil.namedItem(Material.EGG, ChatColor.RED + "Grenade", null));
                } else {
                    p.getInventory().setItem(8, ItemUtil.namedItem(Material.GLOWSTONE_DUST, ChatColor.GOLD.toString() + ChatColor.BOLD + ">> " + ChatColor.YELLOW + "[" + getUser().getTranslatedMessage("DEFENSIVE ABILITY") + "]" + ChatColor.GOLD.toString() + ChatColor.BOLD + " <<", null));
                }
            } else {
                p.getInventory().setItem(8, ItemUtil.namedItem(Material.REDSTONE, ChatColor.DARK_RED.toString() + ChatColor.BOLD + ">> " + ChatColor.RED + getUser().getTranslatedMessage("ABILITY CHARGED") + ChatColor.DARK_RED.toString() + ChatColor.BOLD + " <<", null));
            }
        }
    }

    public void saveData(){
        if(!allowSaveData) return;

        ChestAPI.async(() -> {
            try {
                PreparedStatement ps = MySQLManager.getInstance().getConnection().prepareStatement("UPDATE `kpvp_stats` SET `points`=`points`+?, `monthlyPoints`=`monthlyPoints`+?, `deaths`=`deaths`+?, `kills`=`kills`+?, `highestStreak`=?, `autoRespawn`=? WHERE `uuid`=?");
                ps.setInt(1,this.points);
                ps.setInt(2,this.points);
                ps.setInt(3,this.deaths);
                ps.setInt(4,this.kills);
                ps.setInt(5,highestStreak);
                ps.setBoolean(6,autoRespawn);
                ps.setString(7,p.getUniqueId().toString());
                ps.executeUpdate();
                ps.close();
            } catch(Exception e){
                e.printStackTrace();
            }
        });
    }

    public ItemStack getHelmet(){
        if(kit != null){
            ItemStack helmet = new ItemStack(Material.GOLD_HELMET); helmet.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
            return ItemUtil.setUnbreakable(helmet,true);
        }

        return null;
    }

    public ItemStack getChestplate(){
        if(kit != null){
            ItemStack chestplate = new ItemStack(Material.CHAINMAIL_CHESTPLATE); chestplate.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1);
            return ItemUtil.setUnbreakable(chestplate,true);
        }

        return null;
    }

    public ItemStack getLeggings(){
        if(kit != null){
            ItemStack leggings = new ItemStack(Material.GOLD_LEGGINGS); leggings.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
            return ItemUtil.setUnbreakable(leggings,true);
        }

        return null;
    }

    public ItemStack getBoots(){
        if(kit != null){
            ItemStack boots = new ItemStack(Material.GOLD_BOOTS); boots.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
            return ItemUtil.setUnbreakable(boots,true);
        }

        return null;
    }

    public void addPoints(int points){
        for(int i = 0; i < points; i++){
            //if((startPoints+this.points+i)<=0) break;

            this.points++;
        }
    }

    public void reducePoints(int points){
        for(int i = 0; i < points; i++){
            if((startPoints+this.points+(i/-1))<=0) break;

            this.points--;
        }
    }
}
