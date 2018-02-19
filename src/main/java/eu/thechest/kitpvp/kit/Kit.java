package eu.thechest.kitpvp.kit;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Created by zeryt on 20.02.2017.
 */
public interface Kit {

    public int getID();
    public String getName();
    public ChatColor getColor();
    public int getPrice();
    public String getDescription();
    public Material getDisplayIcon();
    public short getDisplayIconDurability();
    public boolean hasDefensiveAbility();
    public int getAbilityCooldown();

    public boolean doAbility(Player p);
    public void undoAbility(Player p);

}