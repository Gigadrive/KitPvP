package eu.thechest.kitpvp.kit;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Created by zeryt on 26.02.2017.
 */
public class Bomberman implements Kit {
    public Bomberman(){
        instance = this;
        KitStorage.STORAGE.add(this);
    }

    private static Bomberman instance;
    public static Bomberman getInstance(){
        return instance;
    }

    public int getID(){
        return 6;
    }

    public String getName(){
        return "Bomberman";
    }

    public ChatColor getColor(){
        return ChatColor.RED;
    }

    public int getPrice(){
        return 400;
    }

    public String getDescription(){
        return "Can throw grenades at other players.";
    }

    public Material getDisplayIcon(){
        return Material.TNT;
    }

    public short getDisplayIconDurability(){
        return 0;
    }

    public boolean hasDefensiveAbility() {
        return true;
    }

    public int getAbilityCooldown() {
        return 10;
    }

    public boolean doAbility(Player p){return false;}

    public void undoAbility(Player p){}
}
