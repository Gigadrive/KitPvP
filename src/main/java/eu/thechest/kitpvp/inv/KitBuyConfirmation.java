package eu.thechest.kitpvp.inv;

import eu.thechest.chestapi.items.ItemUtil;
import eu.thechest.chestapi.server.ServerSettingsManager;
import eu.thechest.chestapi.user.ChestUser;
import eu.thechest.chestapi.util.StringUtils;
import eu.thechest.kitpvp.kit.Kit;
import eu.thechest.kitpvp.kit.KitStorage;
import eu.thechest.kitpvp.user.KitPlayer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/**
 * Created by zeryt on 26.02.2017.
 */
public class KitBuyConfirmation implements Listener {
    public static void openFor(Player p, Kit kit){
        KitPlayer k = KitPlayer.get(p);
        ChestUser u = k.getUser();
        Inventory inv = Bukkit.createInventory(null, 27, "Buy confirmation: #" + kit.getID());

        inv.setItem(11, ItemUtil.namedItem(Material.WOOL, ChatColor.GREEN + u.getTranslatedMessage("Buy this kit"), null, 5));
        inv.setItem(13, ItemUtil.namedItem(kit.getDisplayIcon(), kit.getColor() + u.getTranslatedMessage(kit.getName()), null, kit.getDisplayIconDurability()));
        inv.setItem(15, ItemUtil.namedItem(Material.WOOL, ChatColor.RED + u.getTranslatedMessage("Don't buy this kit"), null, 14));

        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e){
        if(e.getWhoClicked() instanceof Player){
            Player p = (Player)e.getWhoClicked();
            KitPlayer k = KitPlayer.get(p);
            ChestUser u = k.getUser();
            int slot = e.getRawSlot();
            Inventory inv = e.getInventory();

            if(inv.getName().startsWith("Buy confirmation: #")){
                e.setCancelled(true);

                if(StringUtils.isValidInteger(inv.getName().replace("Buy confirmation: #",""))){
                    int kitID = Integer.parseInt(inv.getName().replace("Buy confirmation: #",""));
                    Kit kit = KitStorage.fromID(kitID);

                    if(kit != null){
                         if(slot == 11){
                             if(u.getCoins() >= kit.getPrice()){
                                 p.playSound(p.getEyeLocation(), Sound.LEVEL_UP,1f,1f);
                                 k.addKit(kit);
                                 u.reduceCoins(kit.getPrice());
                                 p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GREEN + u.getTranslatedMessage("You've successfully bought the kit %k!").replace("%k",kit.getColor() + kit.getName() + ChatColor.GREEN));
                                 KitSelectionMenu.openFor(p);
                             } else {
                                 p.closeInventory();
                                 p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.RED + u.getTranslatedMessage("You don't have enough coins."));
                             }
                         } else if(slot == 15){
                             KitSelectionMenu.openFor(p);
                         }
                    }
                }
            }
        }
    }
}
