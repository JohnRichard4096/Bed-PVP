package com.john.bedpvp;
import org.bukkit.Bukkit;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
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
import java.util.logging.Logger;

public final class Bed_PVP extends JavaPlugin implements Listener {
    private boolean Start = false;
    private boolean Available = false;
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

        // 添加发光效果，时长为无限
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 1, false, false));
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
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
    public void onEntityExplode(EntityExplodeEvent event) {
        List<Block> nearbyBeds = new ArrayList<>();

        // 遍历爆炸范围内的方块，将周围的床添加到列表中
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
        if (label.equalsIgnoreCase("bed-pvp-start")){


        }
        return false;

    }

    public static boolean isBed(Block block) {
        Material blockType = block.getType();
        // 判断是否是BED类型或各种颜色的BED
        return blockType.name().endsWith("_BED");
    }
}

