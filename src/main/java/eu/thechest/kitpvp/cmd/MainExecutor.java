package eu.thechest.kitpvp.cmd;

import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;
import eu.thechest.chestapi.ChestAPI;
import eu.thechest.chestapi.mysql.MySQLManager;
import eu.thechest.chestapi.server.ServerSettingsManager;
import eu.thechest.chestapi.user.ChestUser;
import eu.thechest.chestapi.user.Rank;
import eu.thechest.chestapi.util.PlayerUtilities;
import eu.thechest.chestapi.util.StringUtils;
import eu.thechest.kitpvp.KitPvP;
import eu.thechest.kitpvp.inv.KitSelectionMenu;
import eu.thechest.kitpvp.kit.KitStorage;
import eu.thechest.kitpvp.listener.MainListener;
import eu.thechest.kitpvp.user.KitPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by zeryt on 24.02.2017.
 */
public class MainExecutor implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(cmd.getName().equalsIgnoreCase("autorespawn")){
            if(sender instanceof Player){
                Player p = (Player)sender;
                KitPlayer k = KitPlayer.get(p);
                ChestUser u = ChestUser.getUser(p);

                k.autoRespawn = !k.autoRespawn;
                if(k.autoRespawn){
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + u.getTranslatedMessage("Auto-respawn is now toggled on."));
                } else {
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + u.getTranslatedMessage("Auto-respawn is now toggled off."));
                }
            } else {
                sender.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "You have to be a player.");
            }
        }

        if(cmd.getName().equalsIgnoreCase("kit")){
            if(sender instanceof Player){
                Player p = (Player)sender;
                KitPlayer k = KitPlayer.get(p);
                ChestUser u = k.getUser();

                if(k.kit == null){
                    KitSelectionMenu.openFor(p);
                } else {
                    if(k.maySwitchKit){
                        for(BukkitTask t : k.schedulers){
                            t.cancel();
                        }

                        k.schedulers.clear();

                        k.abilityCooldown = 0;
                        k.isAbilityInCooldown = false;

                        k.kit = null;
                        p.teleport(KitPvP.getInstance().lobbyLocation);
                        p.setFireTicks(0);
                        p.setMaxHealth(20);
                        p.setHealth(p.getMaxHealth());
                        p.setGameMode(GameMode.SURVIVAL);
                        p.setFoodLevel(20);
                        p.getInventory().clear();
                        p.getInventory().setArmorContents(null);

                        k.giveLobbyItems();

                        for(Player all : Bukkit.getOnlinePlayers()){
                            if(KitPlayer.get(all).lastDamager == p){
                                KitPlayer.get(all).lastDamager = null;
                            }
                        }

                        if(k.lastDamager != null){
                            MainListener.death(p,k.lastDamager);
                        }
                    } else {
                        p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("Please wait a little bit before executing this command again."));
                    }
                }
            } else {
                sender.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "You have to be a player.");
            }
        }

        if(cmd.getName().equalsIgnoreCase("setlobby")){
            if(sender instanceof Player){
                Player p = (Player)sender;
                KitPlayer k = KitPlayer.get(p);

                if(p.isOp()){
                    KitPvP.getInstance().setLobbyLocation(p.getLocation());
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + "Lobby:");
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + "W: " + p.getLocation().getWorld().getName());
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + "X: " + p.getLocation().getX());
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + "Y: " + p.getLocation().getY());
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + "Z: " + p.getLocation().getZ());
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + "Yaw: " + p.getLocation().getYaw());
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + "Pitch: " + p.getLocation().getPitch());
                } else {
                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + k.getUser().getTranslatedMessage("You aren't permitted to execute this command."));
                }
            } else {
                sender.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + "You have to be a player.");
            }
        }

        return false;
    }
}
