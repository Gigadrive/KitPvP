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

import java.util.ArrayList;

/**
 * Created by zeryt on 26.02.2017.
 */
public class Devil implements Kit {
    public Devil(){
        instance = this;
        KitStorage.STORAGE.add(this);

    }

    private static Devil instance;
    public static Devil getInstance(){
        return instance;
    }

    public int getID(){
        return 3;
    }

    public String getName(){
        return "Devil";
    }

    public ChatColor getColor(){
        return ChatColor.DARK_RED;
    }

    public int getPrice(){
        return 345;
    }

    public String getDescription(){
        return "Can set players within a 8 block radius on fire.";
    }

    public Material getDisplayIcon(){
        return Material.FLINT_AND_STEEL;
    }

    public short getDisplayIconDurability(){
        return 0;
    }

    public boolean hasDefensiveAbility() {
        return false;
    }

    public int getAbilityCooldown() {
        return 15;
    }

    public boolean doAbility(Player p){
        ChestUser u = ChestUser.getUser(p);
        boolean b = false;

        for(Entity e : p.getNearbyEntities(8,8,8)){
            if(e instanceof Player){
                b = true;

                Player p2 = (Player)e;
                KitPlayer k2 = KitPlayer.get(p2);

                if(k2.kit != null){
                    p2.setFireTicks(4*20);
                }
            }
        }

        if(b){
            Location center = p.getEyeLocation();
            double radius = 8.0;
            int amount = 15;
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
                ParticleEffect.SMOKE_NORMAL.display(0f,0f,0f,0f,amount,loc,30);
                ParticleEffect.SMOKE_NORMAL.display(0f,0f,0f,0f,amount,loc.add(0,1,0),30);
                ParticleEffect.SMOKE_NORMAL.display(0f,0f,0f,0f,amount,loc.add(0,2,0),30);
                ParticleEffect.SMOKE_NORMAL.display(0f,0f,0f,0f,amount,loc.add(0,3,0),30);
            }
        } else {
            p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("No target found."));
        }

        return b;
    }

    public void undoAbility(Player p){}
}
