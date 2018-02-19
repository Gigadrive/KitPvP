package eu.thechest.kitpvp.kit;

import eu.thechest.chestapi.server.ServerSettingsManager;
import eu.thechest.chestapi.user.ChestUser;
import eu.thechest.kitpvp.user.KitPlayer;
import org.bukkit.*;
import org.bukkit.entity.Player;

/**
 * Created by zeryt on 20.02.2017.
 */
public class Assassin implements Kit {
    public Assassin(){
        instance = this;
        KitStorage.STORAGE.add(this);
    }

    private static Assassin instance;
    public static Assassin getInstance(){
        return instance;
    }

    public int getID(){
        return 1;
    }

    public String getName() {
        return "Assassin";
    }

    public ChatColor getColor(){ return ChatColor.WHITE; }

    public int getPrice(){
        return 500;
    }

    public String getDescription(){
        return "Can teleport behind their enemy.";
    }

    public Material getDisplayIcon() {
        return Material.SHEARS;
    }

    public short getDisplayIconDurability() {
        return 0;
    }

    public boolean hasDefensiveAbility() {
        return false;
    }

    public int getAbilityCooldown() {
        return 20;
    }

    public boolean doAbility(Player p) {
        ChestUser u = ChestUser.getUser(p);
        Player p2 = null;

        for(Player all : Bukkit.getOnlinePlayers()){
            if(all.isDead()) continue;
            if(all != p){
                if(KitPlayer.get(all).kit != null){
                    if(all.getWorld() == p.getWorld()){
                        if(p2 == null){
                            p2 = all;
                        } else {
                            if(p2.getLocation().distance(p.getLocation()) > all.getLocation().distance(p.getLocation())){
                                p2 = all;
                            }
                        }
                    }
                }
            }
        }

        if(p2 == null || p2.getLocation().distance(p.getLocation()) > 20.0){
            p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("No target found."));
            return false;
        } else {
            Location oldLoc = p.getLocation();
            Location newLoc = p2.getLocation().add(p2.getLocation().getDirection().setY(0).normalize().multiply(-2));
            //Location newLoc = p2.getLocation().add(0.0, 1.0, 0.0);

            Location nLoc = newLoc.clone();

            double y = newLoc.getY();

            while((newLoc.getBlock() != null && newLoc.getBlock().getType() != null && newLoc.getBlock().getType() != Material.AIR)){
                y++;
                newLoc.setY(y);
            }

            if(nLoc.distance(newLoc) >= 5) newLoc = p2.getLocation();

            p.getLocation().getWorld().playEffect(oldLoc, Effect.ENDER_SIGNAL, 1);
            p.getLocation().getWorld().playSound(p.getLocation(),Sound.ENDERMAN_TELEPORT,1f,1f);
            p.teleport(newLoc);

            p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + u.getTranslatedMessage("You were teleported to %s.",ChatColor.YELLOW + p2.getDisplayName() + ChatColor.GREEN));
            return true;
        }
    }

    public void undoAbility(Player p) {}
}
