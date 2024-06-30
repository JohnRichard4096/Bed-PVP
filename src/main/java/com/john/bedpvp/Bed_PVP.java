package com.john.bedpvp;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public final class Bed_PVP extends JavaPlugin implements Listener {
    private boolean Start = false;
    Logger logger = getLogger();
    @Override
    public void onEnable() {
        // Plugin startup logic
        logger.info("Loading Bed-PVP");
        System.out.println("""
                 
                ***********************
                *Bed-PVP V0.1-Snapshot*
                *Loading......        *
                ***********************
                 
                """);
        if (!checkDependency("WorldEdit")) {
            getLogger().severe("Dependency WorldEdit not found, disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
        }
        logger.info("Registering events......");
        Bukkit.getPluginManager().registerEvents(this, this);


        logger.info("Remade world ......");

        logger.info("Done!");
        // 进行其他启用插件的逻辑
    }



    @Override
    public void onDisable() {
        // Plugin shutdown logic
        logger.info("Disabling plugin......");
        logger.info("Done!");
    }
    private boolean checkDependency(String dependencyName) {
        Plugin plugin = getServer().getPluginManager().getPlugin(dependencyName);
        if (plugin == null) {
            return false;
        }
        return true;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (Start) {
            // Set the player to spectator mode
            player.setGameMode(GameMode.SPECTATOR);

            // Send a red-colored message to the player indicating the game has started
            player.sendMessage(ChatColor.RED + "The game has already started! You are in Spectator mode.");
        }
        // 添加发光效果，时长为无限
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 1, false, false));
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!Start) return;//没有开始时则返回
        Projectile projectile = event.getEntity();
        if (projectile.getShooter() instanceof Player && projectile.getType() == EntityType.ARROW) {
            Block hitBlock = event.getHitBlock();

            if (hitBlock.getWorld().getEnvironment()==World.Environment.NORMAL){
                if(hitBlock.getType()==Material.WHITE_BED  &&hitBlock != null){
                    BlockState bedState = hitBlock.getState();
                    bedState.setType(Material.AIR);
                    bedState.update();

                    TNTPrimed tnt = (TNTPrimed) hitBlock.getWorld().spawnEntity(projectile.getLocation(), EntityType.PRIMED_TNT);
                    tnt.setFuseTicks(0);
                }

            }
            else if(hitBlock.getWorld().getEnvironment()!=World.Environment.NORMAL){
                if(hitBlock != null && isBed(hitBlock)){
                    BlockState bedState = hitBlock.getState();
                    bedState.setType(Material.AIR);
                    bedState.update();

                    TNTPrimed tnt = (TNTPrimed) hitBlock.getWorld().spawnEntity(projectile.getLocation(), EntityType.PRIMED_TNT);
                    tnt.setFuseTicks(0);
                }

            }
        }
    }
    @EventHandler
    public void onPlayerDeath(EntityDeathEvent event) {
        // 确认死亡实体是玩家
        if (!(event.getEntity() instanceof Player)) return;
        Player deadPlayer = (Player) event.getEntity();
        EntityDamageEvent damageEvent = deadPlayer.getLastDamageCause();
        if (damageEvent != null) {
            if (damageEvent.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION||damageEvent.getCause() != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            }
            return;
            // 如果不是被炸死，直接结束此方法
        }
         int onlinePlayersCount = 0;
        for (Player player : deadPlayer.getServer().getOnlinePlayers()) {
            if (player.getGameMode() != GameMode.SPECTATOR) { // 排除旁观者模式的玩家
                onlinePlayersCount++;
            }
        }

        // 当在线非旁观者玩家等于2时，死亡的玩家被踢出并发送消息
        if (onlinePlayersCount == 2) {
            deadPlayer.kickPlayer(ChatColor.RED + "You lost the game.");
            return;
        }

        // 当只剩下一名非旁观者玩家时，该玩家获胜
        if (onlinePlayersCount == 1) {
            Player lastPlayer = deadPlayer.getServer().getOnlinePlayers().stream()
                    .filter(p -> p.getGameMode() != GameMode.SPECTATOR)
                    .findFirst()
                    .orElse(null);
            if (lastPlayer != null) {
                // 发送获胜消息并生成烟花庆祝
                lastPlayer.sendTitle(ChatColor.YELLOW + "Congratulations!", ChatColor.GREEN + "You are the last survivor!", 10, 70, 20);

                Location playerLocation = lastPlayer.getLocation();
                for (int i = 0; i < 10; i++) {
                    Firework fw = (Firework) lastPlayer.getWorld().spawnEntity(playerLocation.add(new Random().nextInt(15) - 7, new Random().nextInt(5), new Random().nextInt(15) - 7), EntityType.FIREWORK);
                    FireworkMeta fwm = fw.getFireworkMeta();
                    fwm.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BURST).withColor(Color.YELLOW).flicker(true).trail(true).build());
                    fwm.setPower(1);
                    fw.setFireworkMeta(fwm);
                }

                // 5秒后踢出最后的玩家并结束游戏
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        lastPlayer.kickPlayer(ChatColor.YELLOW + "Game over! You won!");
                    }
                }.runTaskLater(this, 20 * 5); // 延迟5秒（100ticks=5秒）
            }
            Start = false;
            logger.info("Game over!");
        } else if (onlinePlayersCount >= 3) {
            // 切换玩家到旁观者模式
            deadPlayer.setGameMode(GameMode.SPECTATOR);
            deadPlayer.sendMessage(ChatColor.RED + "You were killed and switched to Spectator mode.");
        }
    }


    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        List<Block> nearbyBeds = new ArrayList<>();
        if (!Start) return;//没有开始时则返回
        // 遍历爆炸范围内的方块，将周围的床添加到列表
        for (Block block : event.blockList()) {
            if (isBed(block)) {
                nearbyBeds.add(block);
            }
        }

        // 移除周围的其他床
        for (Block bed : nearbyBeds) {
            if (!event.blockList().contains(bed)) {
                bed.setType(Material.AIR);
            }
        }

        // 生成多个TNT
        for (Block bed : nearbyBeds) {
            TNTPrimed tnt = (TNTPrimed) bed.getWorld().spawnEntity(bed.getLocation(), EntityType.PRIMED_TNT);
            tnt.setFuseTicks(0);
        }


    }



    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!Start) return;//没有开始时则返回
        Player player = event.getPlayer();
        if (event.getAction() == Action.LEFT_CLICK_BLOCK && player.getInventory().getItemInMainHand().getType() == Material.AIR) {
            if (event.getClickedBlock() != null && isBed(event.getClickedBlock()) && event.getClickedBlock().getWorld().getEnvironment() == World.Environment.NORMAL) {
                event.getClickedBlock().setType(Material.AIR); // 将床方块变为空气
                player.getWorld().createExplosion(event.getClickedBlock().getLocation(), 4F); // 在床位置创建爆炸
                event.setCancelled(true); // 取消事件，避免床被放置

            }
        }
    }
    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        if (event.getType() == ServerLoadEvent.LoadType.RELOAD) {
            getLogger().warning("Server was reloaded! Now will kick all player!!!");
            for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                player.kickPlayer("Server was reloaded,but BED-PVP have not reload well yet!");

            }
        }
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("bed-pvp")) {
            sender.sendMessage("""
                    
                    bed-pvp V0.1-Snapshot
                    By JohnRichard4096
                    Commands:
                        bed-pvp:To show command help
                    
                    """);

        }
        if (label.equalsIgnoreCase("bed-pvp-start")) {
            logger.info("Game start!");
            if (sender.hasPermission("bedpvp.start") || sender.isOp()) {
                int onlinePlayersCount = Bukkit.getOnlinePlayers().size();
                if (onlinePlayersCount < 3) {
                    sender.sendMessage(ChatColor.RED + "Not enough players online. At least 3 players are required to start Bed-PVP.");
                    return true; // 玩家数量不足，直接返回不执行后续逻辑
                }

                sender.sendMessage(ChatColor.GREEN + "Starting Bed-PVP in 5 seconds...");

                // 创建一个异步任务来发送标题、播放音效并在倒计时后改变布尔值
                new BukkitRunnable() {
                    int countdown = 5;

                    @Override
                    public void run() {
                        if (countdown <= 0) {
                            // 在正式开始前再次检查玩家数量，以防在倒计时期间有玩家离开
                            if (Bukkit.getOnlinePlayers().size() < 3) {
                                sender.sendMessage(ChatColor.RED + "Player count dropped below minimum during countdown. Aborting start.");
                                cancel();
                                return;
                            }

                            // 倒计时结束，改变布尔值并发送广播及播放末影龙音效
                            Start = true;
                            String titleMessage = ChatColor.translateAlternateColorCodes('&', "&aBed-PVP has been started!");
                            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                                onlinePlayer.sendTitle(titleMessage, "", 10, 70, 20);
                                onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1, 1); // 末影龙音效
                            }
                            cancel(); // 结束任务
                        } else {
                            // 发送倒计时到操作者并播放音符盒音效
                            sender.sendMessage(ChatColor.YELLOW + "BedPvp will start after " + countdown + "...");
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1, 1); // 音符盒的do音
                            }
                            countdown--;
                        }
                    }
                }.runTaskTimer(this, 0L, 20L); // 每20ticks（1秒）执行一次
            } else {
                sender.sendMessage(ChatColor.RED + "You don't have the permission to start Bed-PVP.");
            }
        }


        if (label.equalsIgnoreCase("bed-pvp-stop")) {
            if (sender.hasPermission("bedpvp.stop") || sender.isOp()) {
                logger.info("Game stop!");
                Start = false;
                sender.sendMessage("Bed-PVP has been stopped!");
            } else {
                sender.sendMessage("You don't have the permission to stop Bed-PVP.");
            }
        }
        return false;

    }

    public static boolean isBed(Block block) {
        Material blockType = block.getType();
        // 判断是否是BED类型或各种颜色的BED
        return blockType.name().endsWith("_BED");
    }
}

