package eu.thechest.kitpvp.kit;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Created by zeryt on 26.02.2017.
 */
public class Pin implements Kit {
    public Pin(){
        instance = this;
        KitStorage.STORAGE.add(this);
    }

    private static Pin instance;
    public static Pin getInstance(){
        return instance;
    }

    public int getID(){
        return 5;
    }

    public String getName(){
        return "Pin";
    }

    public ChatColor getColor(){
        return ChatColor.GRAY;
    }

    public int getPrice(){
        return 280;
    }

    public String getDescription(){
        return "Damages the attacker. (Does not work with players that have the same kit)";
    }

    public Material getDisplayIcon(){
        return Material.IRON_FENCE;
    }

    public short getDisplayIconDurability(){
        return 0;
    }

    public boolean hasDefensiveAbility() {
        return true;
    }

    public int getAbilityCooldown() {
        return 0;
    }

    public boolean doAbility(Player p){return false;}

    public void undoAbility(Player p){}
}
