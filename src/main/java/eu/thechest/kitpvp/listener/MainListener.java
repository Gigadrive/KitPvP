package eu.thechest.kitpvp.listener;

import eu.the5zig.mod.server.The5zigMod;
import eu.the5zig.mod.server.api.ModUser;
import eu.the5zig.mod.server.api.UserManager;
import eu.thechest.chestapi.ChestAPI;
import eu.thechest.chestapi.event.PlayerDataLoadedEvent;
import eu.thechest.chestapi.event.PlayerLandOnGroundEvent;
import eu.thechest.chestapi.event.PlayerLocaleChangeEvent;
import eu.thechest.chestapi.items.ItemUtil;
import eu.thechest.chestapi.maps.MapLocationData;
import eu.thechest.chestapi.server.ServerSettingsManager;
import eu.thechest.chestapi.user.ChestUser;
import eu.thechest.chestapi.user.Rank;
import eu.thechest.chestapi.util.BountifulAPI;
import eu.thechest.chestapi.util.ParticleEffect;
import eu.thechest.chestapi.util.StringUtils;
import eu.thechest.kitpvp.KitPvP;
import eu.thechest.kitpvp.cmd.MainExecutor;
import eu.thechest.kitpvp.kit.Bomberman;
import eu.thechest.kitpvp.kit.Pin;
import eu.thechest.kitpvp.user.KitPlayer;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by zeryt on 20.02.2017.
 */
public class MainListener implements Listener {
    public static void death(Player p, Player killer){
        KitPlayer k = KitPlayer.get(p);
        ChestUser u = k.getUser();

        KitPlayer kk = KitPlayer.get(killer);
        ChestUser ku = kk.getUser();

        for(Player all : Bukkit.getOnlinePlayers()){
            KitPlayer aa = KitPlayer.get(all);
            ChestUser a = aa.getUser();

            if(a.hasPermission(Rank.VIP)){
                all.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GOLD + a.getTranslatedMessage("%p was killed by %k.").replace("%p",u.getRank().getColor() + p.getName() + ChatColor.GOLD).replace("%k",ChestUser.getUser(killer).getRank().getColor() + killer.getName() + ChatColor.GOLD));
            } else {
                all.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GOLD + a.getTranslatedMessage("%p was killed by %k.").replace("%p",p.getDisplayName() + ChatColor.GOLD).replace("%k",killer.getDisplayName() + ChatColor.GOLD));
            }
        }

        kk.killstreak++;
        if(kk.killstreak > kk.highestStreak) kk.highestStreak = kk.killstreak;
        kk.kills++;
        //kk.addPoints(5);
        ku.addCoins(3);
        ku.achieve(27);
        ku.giveExp(4);
        Bukkit.getScheduler().scheduleSyncDelayedTask(KitPvP.getInstance(), new Runnable() {
            @Override
            public void run() {
                kk.achievementCheck();
            }
        },2*20);
        killer.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 10*20, 1));
        if(killer.getFireTicks() > 0) ku.achieve(41);

        int points = KitPvP.calculateRating(kk.startPoints+kk.points,k.startPoints+k.points,1,20);

        kk.addPoints(points);
        k.reducePoints(points);

        kk.updateScoreboard();
        k.updateScoreboard();

        String streakName = null;

        int streak = kk.killstreak;
        if(streak == 2){
            streakName = ChatColor.GREEN + "DOUBLE";
        } else if(streak == 3){
            streakName = ChatColor.DARK_GREEN + "TRIPPLE";
        } else if(streak == 10){
            streakName = ChatColor.YELLOW + "LEGENDARY";
        }

        if(streak == 10) ku.achieve(3);

        if(streakName != null){
            for(Player all : Bukkit.getOnlinePlayers()){
                KitPlayer a = KitPlayer.get(all);
                ChestUser ua = a.getUser();

                if(ua.hasPermission(Rank.VIP)){
                    all.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GOLD + ua.getTranslatedMessage("%p has a %s killstreak!").replace("%s",streakName + ChatColor.GRAY + " (" + streak + ")" + ChatColor.GOLD).replace("%p",ChestUser.getUser(killer).getRank().getColor() + killer.getName() + ChatColor.GOLD));
                } else {
                    all.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GOLD + ua.getTranslatedMessage("%p has a %s killstreak!").replace("%s",streakName + ChatColor.GRAY + " (" + streak + ")" + ChatColor.GOLD).replace("%p",killer.getDisplayName() + ChatColor.GOLD));
                }
            }

            if(The5zigMod.getInstance().getUserManager().isModUser(killer)){
                ModUser m = The5zigMod.getInstance().getUserManager().getUser(killer);

                m.getStatsManager().sendLargeText(ChatColor.GOLD + kk.getUser().getTranslatedMessage("You've reached a %s killstreak!").replace("%s",streakName + ChatColor.GRAY + " (" + streak + ")" + ChatColor.GOLD));

                Bukkit.getScheduler().scheduleSyncDelayedTask(KitPvP.getInstance(), new Runnable(){
                    public void run(){
                        m.getStatsManager().resetLargeText();
                    }
                },2*20);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e){
        Player p = e.getPlayer();
        p.teleport(KitPvP.getInstance().lobbyLocation);

        e.setJoinMessage(null);

        p.setFireTicks(0);
        p.setMaxHealth(20);
        p.setHealth(p.getMaxHealth());
        p.setGameMode(GameMode.SURVIVAL);
        p.setFoodLevel(20);

        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
    }

    @EventHandler
    public void onLoaded(PlayerDataLoadedEvent e){
        Player p = e.getPlayer();

        ChestAPI.async(() -> {
            KitPlayer k = KitPlayer.get(p);
            ChestUser u = k.getUser();

            ChestAPI.sync(() -> k.updateScoreboard());
            k.achievementCheck();
            k.giveLobbyItems();

            new BukkitRunnable(){
                @Override
                public void run() {
                    StringUtils.sendJoinMessage(p);
                }
            }.runTaskLater(KitPvP.getInstance(),10);
        });
    }

    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent e){
        if(e.getEntity() instanceof Player && e.getDamager() instanceof Player){
            Player p = (Player)e.getEntity();
            Player damager = (Player)e.getDamager();

            if(KitPlayer.get(p).kit != null && KitPlayer.get(damager).kit != null){
                KitPlayer.get(p).lastDamager = damager;

                if(KitPlayer.get(p).kit instanceof Pin){
                    if(!(KitPlayer.get(damager).kit instanceof Pin)){
                        damager.damage(2.0,p);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e){
        Player p = e.getPlayer();
        KitPlayer k = KitPlayer.get(p);
        ChestUser u = k.getUser();

        e.setQuitMessage(null);

        StringUtils.sendQuitMessage(p);

        if(k.kit != null && k.lastDamager != null){
            death(p,k.lastDamager);
        }

        KitPlayer.unregister(k);
    }

    @EventHandler
    public void onLocaleChange(PlayerLocaleChangeEvent e){
        Player p = e.getPlayer();
        ChestUser u = ChestUser.getUser(p);
        KitPlayer k = KitPlayer.get(p);

        u.clearScoreboard();
        k.updateScoreboard();

        if(k.kit != null && !k.isAbilityInCooldown){
            if(!(k.kit instanceof Bomberman)){
                if(!k.kit.hasDefensiveAbility()){
                    p.getInventory().setItem(8, ItemUtil.namedItem(Material.REDSTONE, ChatColor.DARK_RED.toString() + ChatColor.BOLD + ">> " + ChatColor.RED + u.getTranslatedMessage("ABILITY CHARGED") + ChatColor.DARK_RED.toString() + ChatColor.BOLD + " <<", null));
                } else {
                    p.getInventory().setItem(8, ItemUtil.namedItem(Material.GLOWSTONE_DUST, ChatColor.GOLD.toString() + ChatColor.BOLD + ">> " + ChatColor.YELLOW + "[" + u.getTranslatedMessage("DEFENSIVE ABILITY") + "]" + ChatColor.GOLD.toString() + ChatColor.BOLD + " <<", null));
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e){
        Player p = e.getPlayer();
        ChestUser u = ChestUser.getUser(p);

        if(p.getGameMode() != GameMode.CREATIVE){
            e.setCancelled(true);
        } else if(!u.hasPermission(Rank.ADMIN)){
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e){
        Player p = e.getPlayer();
        ChestUser u = ChestUser.getUser(p);

        if(p.getGameMode() != GameMode.CREATIVE){
            e.setCancelled(true);
        } else if(!u.hasPermission(Rank.ADMIN)){
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e){
        Player p = e.getPlayer();
        ChestUser u = ChestUser.getUser(p);

        if(p.getGameMode() != GameMode.CREATIVE){
            e.setCancelled(true);
        } else if(!u.hasPermission(Rank.ADMIN)){
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e){
        if(e.getEntity() instanceof Player){
            Player p = (Player)e.getEntity();
            KitPlayer k = KitPlayer.get(p);
            ChestUser u = k.getUser();

            if(k.spawnProtection){
                e.setCancelled(true);
                return;
            }

            if(k.kit == null){
                e.setCancelled(true);
            } else {
                if(e.getCause() == EntityDamageEvent.DamageCause.VOID){
                    e.setDamage(20);
                } else if(e.getCause() == EntityDamageEvent.DamageCause.FALL){
                    if(KitPvP.FALL_DAMAGE == false || k.hulkInAir){
                        e.setCancelled(true);
                    }
                } else if(e.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION || e.getCause() == EntityDamageEvent.DamageCause.FALLING_BLOCK){
                    e.setCancelled(true);
                }
            }
        } else if(e.getEntity() instanceof EnderCrystal){
            e.setCancelled(true);
        }
    }

    public static Location respawn(Player p){
        return respawn(p,false);
    }

    public static Location respawn(Player p, boolean delay){
        ChestUser u = ChestUser.getUser(p);
        KitPlayer k = KitPlayer.get(p);

        Location loc = p.getLocation();

        ArrayList<MapLocationData> spawnpoints = KitPvP.spawnpoints;
        if(spawnpoints.size() > 0){
            Collections.shuffle(spawnpoints);
            loc = spawnpoints.get(0).toBukkitLocation(KitPvP.mapToTeleportTo);
        } else {
            loc = Bukkit.getWorld(KitPvP.mapToTeleportTo).getSpawnLocation();
        }

        for(BukkitTask t : k.schedulers){
            t.cancel();
        }

        k.schedulers.clear();

        if(delay){
            k.setKit(k.kit,false,false);
        } else {
            Bukkit.getScheduler().scheduleSyncDelayedTask(KitPvP.getInstance(),new Runnable(){
                @Override
                public void run() {
                    k.setKit(k.kit,false,false);
                }
            });
        }
        u.updateLevelBar();

        k.spawnProtection = true;
        k.schedulers.add(new BukkitRunnable(){
            @Override
            public void run() {
                k.spawnProtection = false;
                p.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GOLD + u.getTranslatedMessage("Your spawn protection has worn off."));
            }
        }.runTaskLater(KitPvP.getInstance(),5*20));

        return loc;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e){
        Player p = e.getEntity();
        KitPlayer k = KitPlayer.get(p);
        ChestUser u = k.getUser();
        e.setDroppedExp(0);

        k.lastDamager = null;
        for(Player all : Bukkit.getOnlinePlayers()){
            if(KitPlayer.get(all).lastDamager == p){
                KitPlayer.get(all).lastDamager = null;
            }
        }

        if(k.kit != null){
            k.killstreak = 0;
            k.deaths++;

            for(BukkitTask t : k.schedulers){
                t.cancel();
            }

            k.schedulers.clear();

            k.maySwitchKit = true;
            k.abilityCooldown = 0;
            k.isAbilityInCooldown = false;

            e.getDrops().clear();
            e.setDeathMessage(null);

            if(k.autoRespawn){
                u.bukkitReset();
                p.teleport(respawn(p));
                p.setVelocity(new Vector(0,0,0));

                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 2*20, 1, true, true));
                p.playSound(p.getEyeLocation(),Sound.IRONGOLEM_DEATH,1f,1f);

                BountifulAPI.sendTitle(p,0,2*20,0,ChatColor.RED.toString() + ChatColor.BOLD.toString() + u.getTranslatedMessage("YOU DIED!"),ChatColor.GRAY + u.getTranslatedMessage("Respawning.."));
            }

            if(p.getKiller() == null || (p.getKiller() != null && p.getKiller() == p)){
                k.reducePoints(7);

                for(Player all : Bukkit.getOnlinePlayers()){
                    KitPlayer aa = KitPlayer.get(all);
                    ChestUser a = aa.getUser();

                    if(a.hasPermission(Rank.VIP)){
                        all.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GOLD + a.getTranslatedMessage("%p died.").replace("%p",u.getRank().getColor() + p.getName() + ChatColor.GOLD));
                    } else {
                        all.sendMessage(ServerSettingsManager.RUNNING_GAME.getPrefix() + ChatColor.GOLD + a.getTranslatedMessage("%p died.").replace("%p",p.getDisplayName() + ChatColor.GOLD));
                    }
                }
            } else {
                death(p,p.getKiller());
            }
        }

        k.updateScoreboard();
    }

    @EventHandler
    public void onLand(PlayerLandOnGroundEvent e){
        Player p = e.getPlayer();
        KitPlayer k = KitPlayer.get(p);
        ChestUser u = k.getUser();

        if(k.hulkInAir){
            p.getWorld().playSound(p.getLocation(), Sound.EXPLODE, 1f, 1f);
            ParticleEffect.EXPLOSION_HUGE.display(0f,0f,0f,0.2f,6,p.getLocation().clone().add(1,0,0),30);
            ParticleEffect.EXPLOSION_HUGE.display(0f,0f,0f,0.2f,6,p.getLocation().clone().add(-1,0,0),30);
            ParticleEffect.EXPLOSION_HUGE.display(0f,0f,0f,0.2f,6,p.getLocation().clone().add(0,0,1),30);
            ParticleEffect.EXPLOSION_HUGE.display(0f,0f,0f,0.2f,6,p.getLocation().clone().add(0,0,-1),30);

            for(Entity entity : p.getNearbyEntities(6,6,6)){
                if(entity instanceof Player){
                    Player p2 = (Player)entity;

                    if(p != p2){
                        p2.damage((3.5*2)*2,p);
                    }
                }
            }

            k.hulkInAir = false;
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e){
        Player p = e.getPlayer();
        KitPlayer k = KitPlayer.get(p);
        ChestUser u = k.getUser();

        e.setRespawnLocation(respawn(p));
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent e){
        if(e.getEntity() instanceof Player){
            Player p = (Player)e.getEntity();
            e.setCancelled(true);
            p.setFoodLevel(20);
        }
    }

    @EventHandler
    public void onHit(ProjectileHitEvent e){
        Projectile pr = e.getEntity();

        if(pr.getType() == EntityType.EGG){
            pr.remove();

            if(pr.getShooter() != null && (pr.getShooter() instanceof Player)){
                Player p = (Player)pr.getShooter();

                pr.getWorld().playSound(pr.getLocation(), Sound.EXPLODE, 1f, 1f);
                ParticleEffect.EXPLOSION_LARGE.display(0f,0f,0f,0.2f,3,pr.getLocation(),30);

                for(Entity en : pr.getNearbyEntities(5,5,5)){
                    if(en instanceof Player){
                        Player p2 = (Player)en;

                        if(p != p2 && KitPlayer.get(p2).kit != null){
                            p2.damage(6.0*2,p);
                        }
                    }
                }
            }
        } else if(pr.getType() == EntityType.ARROW){
            pr.remove();
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e){
        Player p = e.getPlayer();
        KitPlayer k = KitPlayer.get(p);
        ChestUser u = k.getUser();

        if(e.getAction() == Action.RIGHT_CLICK_BLOCK){
            if(e.getClickedBlock() != null){
                if(KitPvP.getInstance().DISALLOWED_BLOCKS.contains(e.getClickedBlock().getType())){
                    e.setCancelled(true);
                    e.setUseInteractedBlock(Event.Result.DENY);
                    e.setUseItemInHand(Event.Result.DENY);
                }

                if(e.getClickedBlock().getType() == Material.SIGN || e.getClickedBlock().getType() == Material.SIGN_POST || e.getClickedBlock().getType() == Material.WALL_SIGN){
                    Sign s = (Sign)e.getClickedBlock().getState();

                    if(s.getLine(1).equals(ChatColor.DARK_BLUE + "[Choose kit]")){
                        p.performCommand("kit");
                    } else if(s.getLine(1).equals(ChatColor.DARK_BLUE + "[Statistics]")){
                        p.performCommand("stats");
                    } else if(s.getLine(1).equals(ChatColor.DARK_BLUE + "[Leaderboard]")){
                        p.performCommand("top");
                    } else if(s.getLine(1).equals(ChatColor.DARK_RED + "[Back to hub]")){
                        u.connectToLobby();
                    }
                }
            }
        }

        if(e.getAction() == Action.RIGHT_CLICK_BLOCK||e.getAction() == Action.RIGHT_CLICK_AIR){
            if(p.getItemInHand() != null && p.getItemInHand().getItemMeta() != null && p.getItemInHand().getItemMeta().getDisplayName() != null){
                String dis = p.getItemInHand().getItemMeta().getDisplayName();

                if(dis.equals(ChatColor.GREEN + u.getTranslatedMessage("Select your Kit"))){
                    e.setCancelled(true);
                    e.setUseInteractedBlock(Event.Result.DENY);
                    e.setUseItemInHand(Event.Result.DENY);

                    if(k.kit == null)
                        p.performCommand("kit");
                }

                if(dis.equals(ChatColor.RED + u.getTranslatedMessage("Back to Lobby"))){
                    e.setCancelled(true);
                    e.setUseInteractedBlock(Event.Result.DENY);
                    e.setUseItemInHand(Event.Result.DENY);

                    u.connectToLobby();
                }

                if(dis.equals(ChatColor.RED + "Grenade")){
                    e.setCancelled(true);
                    ItemStack i = p.getInventory().getItemInHand();

                    if(i.getAmount() == 1){
                        p.getInventory().setItemInHand(null);
                    } else {
                        i.setAmount(i.getAmount()-1);
                        p.getInventory().setItemInHand(i);
                    }

                    p.launchProjectile(Egg.class);

                    BukkitTask br = new BukkitRunnable(){
                        public void run(){
                            boolean hasGrenade = false;

                            for(ItemStack i : p.getInventory().getContents()){
                                if(i != null){
                                    if(i.getType() == Material.EGG){

                                        hasGrenade = true;
                                    }
                                }
                            }

                            if(!hasGrenade){
                                p.getInventory().setItem(8, ItemUtil.namedItem(Material.EGG, ChatColor.RED + "Grenade", null));
                            }
                        }
                    }.runTaskLater(KitPvP.getInstance(), k.kit.getAbilityCooldown() * 20);

                    k.schedulers.add(br);
                }

                if(dis.equals(ChatColor.DARK_RED.toString() + ChatColor.BOLD + ">> " + ChatColor.RED + u.getTranslatedMessage("ABILITY CHARGED") + ChatColor.DARK_RED.toString() + ChatColor.BOLD + " <<")){
                    if(k.kit.doAbility(p)){
                        k.startAbilityCooldown();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent e){
        e.setCancelled(true);
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent e){
        e.setCancelled(true);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e){
        if(e.getWhoClicked() instanceof Player){
            Player p = (Player)e.getWhoClicked();
            KitPlayer k = KitPlayer.get(p);
            Inventory inv = e.getInventory();
            int slot = e.getSlot();

            e.setCancelled(true);
        }
    }
}
