package eu.thechest.kitpvp.kit;

import eu.thechest.kitpvp.user.KitPlayer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Created by zeryt on 29.03.2017.
 */
public class Hulk implements Kit {
    public Hulk(){
        instance = this;
        KitStorage.STORAGE.add(this);
    }

    private static Hulk instance;
    public static Hulk getInstance(){
        return instance;
    }

    public int getID(){
        return 7;
    }

    public String getName(){
        return "Hulk";
    }

    public ChatColor getColor(){
        return ChatColor.DARK_GREEN;
    }

    public int getPrice(){
        return 650;
    }

    public String getDescription(){
        return "Launches into the air and damages players on landing.";
    }

    public Material getDisplayIcon(){
        return Material.DIRT;
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
        KitPlayer k = KitPlayer.get(p);
        k.hulkInAir = true;

        p.setVelocity(new Vector(0,2.5,0));

        return true;
    }

    public void undoAbility(Player p){}
}
