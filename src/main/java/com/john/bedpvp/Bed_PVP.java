package com.john.bedpvp;
import com.onarandombox.MultiverseCore.MultiverseCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.entity.*;
import com.onarandombox.MultiverseCore.api.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.codehaus.plexus.util.FileUtils;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public final class Bed_PVP extends JavaPlugin implements Listener {
    private boolean Start = false;
    private boolean Available = false;
    private MultiverseCore core = (MultiverseCore) Bukkit.getServer().getPluginManager().getPlugin("Multiverse-Core");
    Logger logger = getLogger();
    @Override
    public void onEnable() {
        // Plugin startup logic
        super.onEnable();
        logger.info("Loading Bed-PVP");
        logger.info("""
                  
                ***********************
                *Bed-PVP V0.1-Snapshot*
                *Loading......        *
                ***********************
                 
                """);
        ExecutorService executorService = Executors.newFixedThreadPool(1); // 参数为线程池大小

        logger.info("Registering events......");
        Bukkit.getPluginManager().registerEvents(this, this);

        logger.info("Checking dependency");
        if (getServer().getPluginManager().getPlugin("Multiverse-Core") == null) {
            logger.severe("Multiverse-Core not found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        MultiverseCore core = (MultiverseCore) Bukkit.getServer().getPluginManager().getPlugin("Multiverse-Core");
        logger.info("Creating world......");
        World world = getServer().getWorld("bed_world"); // 替换为您的世界名称
        MVWorldManager worldManager = core.getMVWorldManager();

            while (this.core==null){
                Logger.getLogger("Bed-PVP").info("Trying to check mv......");
                core.getMVWorldManager();
                if (core != null) {
                    break;
                }
            }
            remadeWorld();


            // 可选：如果玩家超出边界，可以设置不同的处理方式，比如传送回中心点
            getLogger().info("World border set and player movement restricted to a 19x19 block area around (0, 0).");

            logger.info("Done!");




        getServer().getScheduler().runTaskTimer(this, new SpawnSheepTask(), 0L, 20 * 60 * 3);
        // 进行其他启用插件的逻辑
    }



    @Override
    public void onDisable() {
        // Plugin shutdown logic
        Available = false;
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            player.kickPlayer("Server shutdown.");
        }
        logger.info("Disabling plugin......");
        logger.info("Done!");
    }





    public class SpawnSheepTask implements Runnable {

        @Override
        public void run() {
            if (!Start) return;

            // 直接获取主世界
            World mainWorld = Bukkit.getWorld("bed_world");

            if (mainWorld != null) {
                for (int i = 0; i < 5; i++) { // 生成5只动物
                    Location randomLocation = getRandomSafeLocation(mainWorld);
                    if (randomLocation != null) {
                        mainWorld.spawnEntity(randomLocation, EntityType.SHEEP);
                        mainWorld.spawnEntity(randomLocation, EntityType.COW);
                        mainWorld.spawnEntity(randomLocation, EntityType.HORSE);
                    }
                }
                broadcastMessage("Some animals were born in the main world.");
            } else {
                getLogger().warning("Main world not found!");
            }
        }

        private Location getRandomSafeLocation(World world) {
            // 获取世界的边界
            WorldBorder worldBorder = world.getWorldBorder();
            int borderSize = (int) worldBorder.getSize() / 2;

            Random random = new Random();
            while (true) {
                double x = random.nextDouble(-borderSize, borderSize);
                double z = random.nextDouble(-borderSize, borderSize);
                Location location = new Location(world, x, world.getHighestBlockYAt((int)x, (int)z), z);

                // 确保位置在世界边界内且上方有块固体方块供羊站立
                if (worldBorder.isInside(location) && location.getBlock().getType().isSolid()) {
                    return location;
                }
            }
        }

        private void broadcastMessage(String message) {
            Bukkit.broadcast(Component.text(message).color(TextColor.fromCSSHexString("#00FF00"))); // 绿色文字
        }
    }
    @EventHandler
    public void onPlayerTryEnterAnyPortal(PlayerPortalEvent event) {
        // 取消任何传送门事件，阻止玩家通过传送门进入任何维度
        event.setCancelled(true);
        // 发送消息给玩家
        event.getPlayer().sendMessage("No matter where you want to go, you can't enter any portal now!");
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
        if(!Available){
            player.kickPlayer(ChatColor.RED + "Game is not available yet,please wait for some time");
        }
        World targetWorld = Bukkit.getWorld("bed_world");
        if (targetWorld != null) {
            player.teleport(targetWorld.getSpawnLocation());
        } else {
            getLogger().warning("bed_world not found!!!");
        }
        // Clear the player's inventory when they join
        // If the game hasn't started, set the player to survival mode
        player.setGameMode(GameMode.SURVIVAL);

        // Clear inventory and add glowing effect as before
        player.getInventory().clear();
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 1, false, false));

        // Teleport the player to coordinates (0, ~, 0) at the highest solid block
        Location spawnLocation = new Location(player.getWorld(), 0.0, player.getWorld().getHighestBlockYAt(0, 0), 0.0);
        player.teleport(spawnLocation);
        player.setExp(0);
        player.setLevel(0);
        player.setTotalExperience(0);

    }
    private void sendNotStartedTitle(Player player) {
        player.sendTitle(
                ChatColor.RED + "Game Not Started",
                ChatColor.GRAY + "Please wait for the game to begin.",
                10, 70, 20 // 分别代表：显示时间、持续时间、消失时间，单位均为ticks（1秒=20ticks）
        );
    }
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // 获取复活的玩家
        Player player = event.getPlayer();

        // 获取玩家复活时所在的世界名称
        String respawnWorldName = event.getRespawnLocation().getWorld().getName();

        // 判断如果玩家复活的世界不是bed_world
        if (!respawnWorldName.equalsIgnoreCase("bed_world")) {
            // 获取bed_world世界
            World bedWorld = Bukkit.getWorld("bed_world");

            // 确保bed_world存在
            if (bedWorld != null) {
                // 设置玩家复活点到bed_world的 spawn point 或者你指定的位置
                event.setRespawnLocation(bedWorld.getSpawnLocation());

                // 可选：发送信息通知玩家
                player.sendMessage("You are back to battle ground!");
            } else {
                // 如果bed_world不存在，可以处理错误情况，比如记录日志或者发送消息给管理员
                logger.warning("Can't find world!!!");
            }
        } else {
            player.sendMessage("WOO!");
        }
    }
    private void remadeWorld(){

        if(core==null){
            getLogger().severe("Multiverse-Core plugin not found or not initialized properly. Please make sure it's installed and enabled.");
            return;
        }
        MVWorldManager worldManager = core.getMVWorldManager();
        World world = Bukkit.getServer().getWorld("bed_world");
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            player.kickPlayer("Bed pvp will re-create world!");
        }
        Available = false;

        if (world != null) {

            worldManager.unloadWorld("bed_world");
            //卸载世界
            // 打印确认信息
            logger.info("world unloaded!.");
        } else {
            logger.warning("The specified world does not exist.");
        }

        File bedWorldFolder = new File(Bukkit.getWorldContainer(), "bed_world");

        worldManager.deleteWorld("bed_world");
        // 创建新的bed_world
        logger.info("Spawning new world......");
        worldManager.addWorld(
                "bed_world", // The worldname
                World.Environment.NORMAL, // The overworld environment type.
                generateRandomInt(-922337203,922337203), // The world seed. Any seed is fine for me, so we just pass null.
                WorldType.NORMAL, // Nothing special. If you want something like a flat world, change this.
                true, // This means we want to structures like villages to generator, Change to false if you don't want this.
                null // Specifies a custom generator. We are not using any so we just pass null.
        );
        worldManager.loadWorld("bed_world");
        // 获取名为"bed_world"的世界，这里假设世界已经存在且加载
        World bedWorld = core.getMVWorldManager().getMVWorld("bed_world").getCBWorld();
        // 使用原生Bukkit/Spigot API进行设置
        Location spawnLocation = new Location(bedWorld, 0.0, bedWorld.getHighestBlockYAt(0, 0) + 1, 0.0);
        bedWorld.setSpawnLocation(spawnLocation);

        // 允许生物生成，使用原生API
        bedWorld.setGameRule(GameRule.DO_MOB_SPAWNING, true);

        // 设置无雨
        bedWorld.setStorm(false);

        // 设置无雷暴
        bedWorld.setThundering(false);

        // 设置难度为困难
        bedWorld.setDifficulty(Difficulty.HARD);

        // 设定时间为白天 (6000L 对应于中午)
        bedWorld.setTime(6000L);
        if (world != null) {
            world.setSpawnLocation(0, world.getHighestBlockYAt(0, 0), 0);
        } else {
            getLogger().severe("Failed to set spawn point: World not found.");
            return;
        }

        // 限制玩家活动范围
        int radius = 19; // 19x19区块的半径
        WorldBorder worldBorder = world.getWorldBorder();
        worldBorder.setCenter(0, 0); // 设置世界边界的中心点
        worldBorder.setSize(radius * 512, 0); // 设置世界边界的大小，1区块=16格，所以19区块=19*16=304格，乘以2得到直径
        Available = true;
        logger.info("Done!");
    }
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateRandomInt(long min, long max) {
        // nextInt()方法的参数是取值范围，因此max - min + 1是实际的取值范围大小
        // 加上min是为了将随机数映射到min到max的范围内
        return String.valueOf(RANDOM.nextLong(max - min + 1) + min);
    }
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!Start) return; // 如果未开始，则直接返回

        Projectile projectile = event.getEntity();
        if (projectile.getShooter() instanceof Player && projectile.getType() == EntityType.ARROW) {
            Block hitBlock = event.getHitBlock();

            // 判断世界类型并检查是否击中了床
            if (hitBlock != null) {
                if (hitBlock.getWorld().getEnvironment() == World.Environment.NORMAL && hitBlock.getType() == Material.WHITE_BED || hitBlock.getWorld().getEnvironment() != World.Environment.NORMAL && isBed(hitBlock)) {

                    BlockState bedState = hitBlock.getState();
                    bedState.setType(Material.AIR);
                    bedState.update(); // 移除床

                    // 在箭矢位置直接产生爆炸效果
                    hitBlock.getWorld().createExplosion(projectile.getLocation(), 4.0F, true, false);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                    // 取消摔落伤害
                    event.setCancelled(true);
            }
            if(!Start){
                event.setCancelled(true);
            }

        }
    }
    @EventHandler
    public void onPlayerDeath(EntityDeathEvent event) {
        // 确认死亡实体是玩家
        if (!Start){
            return;
        }
        if (!(event.getEntity() instanceof Player)) return;
        Player deadPlayer = (Player) event.getEntity();
        EntityDamageEvent damageEvent = deadPlayer.getLastDamageCause();
        if (damageEvent != null) {
            if (damageEvent.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION && damageEvent.getCause() != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
                event.getDrops().clear();
                return;

            }

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
            onlinePlayersCount=1;
            if (onlinePlayersCount == 1){
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
                        fwm.setPower(2);
                        fw.setFireworkMeta(fwm);
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    // 5秒后踢出最后的玩家并结束游戏
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                                player.kickPlayer("Bed-PVP game has over. See you next time!");
                            }
                            lastPlayer.kickPlayer(ChatColor.YELLOW + "Game over! You won!");
                            Start = false;
                        }
                    }.runTaskLater(this, 20 * 5); // 延迟5秒（100ticks=5秒）

                    logger.info("Game over!");
                    Available = false;
                    remadeWorld();
                }
            }

        }
        else if (onlinePlayersCount > 2) {
            // 切换玩家到旁观者模式
            deadPlayer.setGameMode(GameMode.SPECTATOR);
            deadPlayer.sendMessage(ChatColor.RED + "You were killed and switched to Spectator mode.");
        }

    }


    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        List<Block> nearbyBeds = new ArrayList<>();

        // 检查是否开始，未开始则提前返回
        if (!Start) return;

        // 遍历爆炸影响的方块，找出周围的床并记录
        for (Block block : event.blockList()) {
            if (isBed(block)) {
                nearbyBeds.add(block);
            }
        }

        // 清除不在爆炸列表中的床，防止重复作用
        for (Block bed : nearbyBeds) {
            if (!event.blockList().contains(bed)) {
                bed.setType(Material.AIR); // 将床移除，替换为空气
            }
        }

        // 对每个找到的床所在位置直接触发爆炸效果
        for (Block bed : nearbyBeds) {
            bed.getWorld().createExplosion(bed.getLocation(), 4.0F, true, false); // 参数调整以适应需求
        }
    }



    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        // 获取当前世界的边界信息
        WorldBorder worldBorder = player.getWorld().getWorldBorder();
        double centerX = worldBorder.getCenter().getX();
        double centerZ = worldBorder.getCenter().getZ();
        double diameter = worldBorder.getSize() / 2; // 注意这里的尺寸已经是直径了

        // 计算玩家新的坐标是否在边界外
        boolean isOutsideX = to.getX() < centerX - diameter || to.getX() > centerX + diameter;
        boolean isOutsideZ = to.getZ() < centerZ - diameter || to.getZ() > centerZ + diameter;

        if (isOutsideX || isOutsideZ) {
            // 玩家尝试移出边界，计算并设置新的安全位置
            double newX = Math.min(Math.max(to.getX(), centerX - diameter), centerX + diameter);
            double newZ = Math.min(Math.max(to.getZ(), centerZ - diameter), centerZ + diameter);

            // 保持Y轴坐标不变，以防将玩家拉到地下或拉得过高
            Location safeLocation = new Location(player.getWorld(), newX, to.getY(), newZ);
            player.teleport(safeLocation);

            //发送消息通知玩家
            player.sendMessage("You were pulled back to the border.");
        }
        if (!Start) {
            event.setCancelled(true);
            sendNotStartedTitle(player);
        }
    }


    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {

        Player player = event.getPlayer();
        if (!Start) {
            event.setCancelled(true);
            sendNotStartedTitle(player);
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getClickedBlock() != null &&event.getClickedBlock().getType() == Material.WHITE_BED && event.getClickedBlock().getWorld().getEnvironment() == World.Environment.NORMAL) {
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
                        bed-pvp-start:To start game
                        bed-pvp-stop:To stop game
                    
                    """);

        }
        if (label.equalsIgnoreCase("bed-pvp-start")) {
            if (Start) {
                sender.sendMessage(ChatColor.RED + "Game has already started!");
                return true;
            }
            logger.info("Game start!");
            if (sender.hasPermission("bedpvp.start") || sender.isOp()) {
                int onlinePlayersCount = Bukkit.getOnlinePlayers().size();
                if (onlinePlayersCount < 2) {
                    sender.sendMessage(ChatColor.RED + "Not enough players online. At least 2 players are required to start Bed-PVP.");
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
                            if (Bukkit.getOnlinePlayers().size() < 2) {
                                sender.sendMessage(ChatColor.RED + "Player count dropped below minimum during countdown. Aborting start.");
                                logger.info("Abort start!");
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
                            sender.sendMessage(ChatColor.YELLOW + "BedPvp will start in " + countdown + "...");
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
                if (!Start) {
                    sender.sendMessage("Bed-PVP is not started!");
                    return true;
                }
                logger.info("Game stop!");
                Start = false;
                sender.sendMessage("Bed-PVP has been stopped!");

                // Kick all online players
                for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                    player.kickPlayer("Bed-PVP game has stopped. See you next time!");
                }
                logger.info("Remade word......");
                remadeWorld();
            } else {
                sender.sendMessage("You don't have the permission to stop Bed-PVP.");
            }
        }
        if (label.equalsIgnoreCase("bed-pvp-remade")) {
            if (sender.hasPermission("bedpvp.remade") || sender.isOp()) {
                if (Start) {
                    sender.sendMessage("Bed-PVP is started!");
                    return true;
                }
                logger.info("Remade word......");
                Start = false;

                // Kick all online players
                for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                    player.kickPlayer("Bed-PVP need reload!");
                }
                remadeWorld();
            } else {
                sender.sendMessage("You don't have the permission to remade map.");
            }
        }
        return true;

    }

    public static boolean isBed(Block block) {
        Material blockType = block.getType();
        // 判断是否是BED类型或各种颜色的BED
        return blockType.name().endsWith("_BED");
    }
}

