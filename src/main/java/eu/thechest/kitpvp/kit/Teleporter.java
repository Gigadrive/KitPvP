package eu.thechest.kitpvp.kit;

import com.sun.xml.internal.bind.v2.schemagen.xmlschema.Particle;
import eu.thechest.chestapi.server.ServerSettingsManager;
import eu.thechest.chestapi.user.ChestUser;
import eu.thechest.chestapi.util.ParticleEffect;
import eu.thechest.kitpvp.user.KitPlayer;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

/**
 * Created by zeryt on 25.02.2017.
 */
public class Teleporter implements Kit {

    public Teleporter(){
        instance = this;
        KitStorage.STORAGE.add(this);
    }

    private static Teleporter instance;
    public static Teleporter getInstance(){
        return instance;
    }

    public int getID(){
        return 2;
    }

    public String getName() {
        return "Teleporter";
    }

    public ChatColor getColor(){ return ChatColor.DARK_PURPLE; }

    public int getPrice(){
        return 750;
    }

    public String getDescription(){
        return "Can teleport to where they're looking at.";
    }

    public Material getDisplayIcon() {
        return Material.ENDER_PEARL;
    }

    public short getDisplayIconDurability() {
        return 0;
    }

    public boolean hasDefensiveAbility() {
        return false;
    }

    public int getAbilityCooldown() {
        return 10;
    }

    public boolean doAbility(Player p) {
        KitPlayer k = KitPlayer.get(p);
        ChestUser u = k.getUser();
        Location loc = p.getEyeLocation();
        try {
            BlockIterator bi = new BlockIterator(loc, 0.0, 15);

            Location lastLoc = null;
            while(bi.hasNext()){
                Block b = bi.next();

                if(b.getType() == Material.AIR){
                    lastLoc = b.getLocation();
                } else {
                    break;
                }
            }

            if(lastLoc == null) lastLoc = loc;

            int c = (int)Math.ceil(loc.distance(lastLoc) / 2F) - 1;
            if(c > 0){
                Vector v = lastLoc.toVector().subtract(loc.toVector()).normalize().multiply(2F);
                Location l = loc.clone();
                for (int i = 0; i < c; i++) {
                    l.add(v);
                    //l.getWorld().playEffect(l, Effect.FLAME, 1);
                    ParticleEffect.FLAME.display(0f,0f,0f,0f,1,l,30);
                }
            }

            double y = lastLoc.getY();

            while((lastLoc.getBlock() != null && lastLoc.getBlock().getType() != null && lastLoc.getBlock().getType() != Material.AIR)){
                y++;
                lastLoc.setY(y);
            }

            lastLoc.setY(y);
            lastLoc.setYaw(loc.getYaw());
            lastLoc.setPitch(loc.getPitch());

            p.teleport(lastLoc);
            return true;
        } catch(IllegalStateException e){
            // Weak fix for issue #7

            p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("Teleport failed."));
            return false;
        }
    }

    public void undoAbility(Player p) {}

}
