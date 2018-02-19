package eu.thechest.kitpvp.inv;

import eu.thechest.chestapi.items.ItemUtil;
import eu.thechest.chestapi.maps.MapLocationData;
import eu.thechest.chestapi.server.ServerSettingsManager;
import eu.thechest.chestapi.user.ChestUser;
import eu.thechest.chestapi.util.StringUtils;
import eu.thechest.kitpvp.KitPvP;
import eu.thechest.kitpvp.kit.Kit;
import eu.thechest.kitpvp.kit.KitStorage;
import eu.thechest.kitpvp.user.KitPlayer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by zeryt on 25.02.2017.
 */
public class KitSelectionMenu implements Listener {
    public static void openFor(Player p){
        KitPlayer k = KitPlayer.get(p);
        ChestUser u = k.getUser();
        Inventory inv = Bukkit.createInventory(null,54,"Kit Selection");

        for(Kit kit : KitStorage.STORAGE){
            ItemStack kitDisplay = new ItemStack(kit.getDisplayIcon());
            kitDisplay.setDurability((short)kit.getDisplayIconDurability());

            ItemMeta kitDisplayMeta = kitDisplay.getItemMeta();
            ArrayList<String> lore = new ArrayList<String>();

            kitDisplayMeta.setDisplayName(kit.getColor() + u.getTranslatedMessage(kit.getName()));
            for(String s : StringUtils.getWordWrapLore(u.getTranslatedMessage(kit.getDescription()))){
                lore.add(ChatColor.DARK_GRAY + s);
            }

            lore.add(" ");

            if(k.available_kits.size() == 0){
                lore.add(ChatColor.GOLD + ">> " + ChatColor.YELLOW + u.getTranslatedMessage("Click to select this kit as your starter kit."));
            } else {
                if(k.available_kits.contains(kit)){
                    lore.add(ChatColor.DARK_GREEN + ">> " + ChatColor.GREEN + u.getTranslatedMessage("Click to play with this kit."));
                } else {
                    lore.add(ChatColor.DARK_RED + ">> " + ChatColor.RED + u.getTranslatedMessage("Click to buy this kit for %p.").replace("%p",ChatColor.GOLD.toString() + kit.getPrice() + " " + u.getTranslatedMessage("Coins") + ChatColor.RED));
                }
            }

            kitDisplayMeta.setLore(lore);
            kitDisplay.setItemMeta(kitDisplayMeta);
            inv.addItem(ItemUtil.hideFlags(kitDisplay));
        }

        inv.setItem(53, ItemUtil.namedItem(Material.BARRIER, ChatColor.DARK_RED + u.getTranslatedMessage("Close"), null));

        p.openInventory(inv);
        p.playSound(p.getEyeLocation(), Sound.CHEST_OPEN, 1f, 1f);
    }

    private void playKit(Player p, Kit kit){
        KitPlayer k = KitPlayer.get(p);
        ChestUser u = k.getUser();

        ArrayList<MapLocationData> spawnpoints = KitPvP.spawnpoints;
        if(spawnpoints.size() > 0){
            Collections.shuffle(spawnpoints);
            Location loc = spawnpoints.get(0).toBukkitLocation(KitPvP.mapToTeleportTo);

            k.setKit(kit);
            k.maySwitchKit = false;
            k.schedulers.add(new BukkitRunnable(){
                public void run(){
                    k.maySwitchKit = true;
                }
            }.runTaskLater(KitPvP.getInstance(), 60*20));

            p.teleport(loc);

            KitPvP.currentMap.sendMapCredits(p);
            KitPvP.currentMap.sendRateMapInfo(p);
        } else {
            k.setKit(kit);
            k.maySwitchKit = false;
            k.schedulers.add(new BukkitRunnable(){
                public void run(){
                    k.maySwitchKit = true;
                }
            }.runTaskLater(KitPvP.getInstance(), 60*20));

            p.teleport(Bukkit.getWorld(KitPvP.mapToTeleportTo).getSpawnLocation());

            KitPvP.currentMap.sendMapCredits(p);
            KitPvP.currentMap.sendRateMapInfo(p);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e){
        if(e.getWhoClicked() instanceof Player){
            Player p = (Player)e.getWhoClicked();
            KitPlayer k = KitPlayer.get(p);
            ChestUser u = k.getUser();
            Inventory inv = e.getInventory();
            int slot = e.getRawSlot();

            if(inv.getName().equals("Kit Selection")){
                e.setCancelled(true);

                if(slot == 53){
                    p.closeInventory();
                } else {
                    Kit kit = KitStorage.fromID(slot+1);

                    if(kit != null){
                        if(k.available_kits.size() == 0){
                            // SELECT AS STARTER KIT

                            p.closeInventory();
                            k.addKit(kit);
                            p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + u.getTranslatedMessage("You've successfully selected %k as your starter kit!").replace("%k",kit.getColor() + kit.getName() + ChatColor.GREEN));

                            playKit(p,kit);
                        } else {
                            if(k.available_kits.contains(kit)){
                                // PLAY

                                p.closeInventory();

                                playKit(p,kit);
                            } else {
                                // BUY

                                if(u.getCoins() >= kit.getPrice()){
                                    KitBuyConfirmation.openFor(p,kit);
                                } else {
                                    p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("You don't have enough coins."));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e){
        if(e.getPlayer() instanceof Player){
            Player p = (Player)e.getPlayer();
            KitPlayer k = KitPlayer.get(p);
            ChestUser u = k.getUser();
            Inventory inv = e.getInventory();

            if(inv.getName().equals("Kit Selection")){
                p.playSound(p.getEyeLocation(), Sound.CHEST_CLOSE, 1f, 1f);
            }
        }
    }
}
