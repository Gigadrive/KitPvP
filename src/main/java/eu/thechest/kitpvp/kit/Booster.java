package eu.thechest.kitpvp.kit;

import eu.thechest.chestapi.server.ServerSettingsManager;
import eu.thechest.chestapi.user.ChestUser;
import eu.thechest.chestapi.util.ParticleEffect;
import eu.thechest.kitpvp.user.KitPlayer;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;

/**
 * Created by zeryt on 26.02.2017.
 */
public class Booster implements Kit {
    public Booster(){
        instance = this;
        KitStorage.STORAGE.add(this);

    }

    private static Booster instance;
    public static Booster getInstance(){
        return instance;
    }

    public int getID(){
        return 4;
    }

    public String getName(){
        return "Booster";
    }

    public ChatColor getColor(){
        return ChatColor.YELLOW;
    }

    public int getPrice(){
        return 215;
    }

    public String getDescription(){
        return "Can boost players away.";
    }

    public Material getDisplayIcon(){
        return Material.FEATHER;
    }

    public short getDisplayIconDurability(){
        return 0;
    }

    public boolean hasDefensiveAbility() {
        return false;
    }

    public int getAbilityCooldown() {
        return 10;
    }

    public boolean doAbility(Player p){
        ChestUser u = ChestUser.getUser(p);
        boolean b = false;

        for(Entity e : p.getNearbyEntities(5,5,5)){
            if(e instanceof Player){
                b = true;

                Player p2 = (Player)e;
                KitPlayer k2 = KitPlayer.get(p2);

                if(k2.kit != null){
                    p2.damage(2.0,p);
                    p2.setVelocity(p2.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().add(new Vector(0,1.5,0)));
                }
            }
        }

        if(b){
            Location center = p.getEyeLocation();
            double radius = 5.0;
            int amount = 25;
            World world = center.getWorld();
            double increment = (2 * Math.PI) / amount;
            ArrayList<Location> locations = new ArrayList<Location>();
            for(int i = 0;i < amount; i++) {
                double angle = i * increment;
                double x = center.getX() + (radius * Math.cos(angle));
                double z = center.getZ() + (radius * Math.sin(angle));
                locations.add(new Location(world, x, center.getY(), z));
            }

            for(Location loc : locations){
                ParticleEffect.FIREWORKS_SPARK.display(0f,0f,0f,0f,amount,loc,30);
                ParticleEffect.FIREWORKS_SPARK.display(0f,0f,0f,0f,amount,loc.add(0,1,0),30);
                ParticleEffect.FIREWORKS_SPARK.display(0f,0f,0f,0f,amount,loc.add(0,2,0),30);
                ParticleEffect.FIREWORKS_SPARK.display(0f,0f,0f,0f,amount,loc.add(0,3,0),30);
            }
        } else {
            p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("No target found."));
        }

        return b;
    }

    public void undoAbility(Player p){}
}
