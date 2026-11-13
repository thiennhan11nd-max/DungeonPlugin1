package com.dungeon;

import org.bukkit.*;
import org.bukkit.block.Chest;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public class DungeonManager implements Listener {
    private final JavaPlugin plugin;
    private Location dungeonSpawn;
    private Location lobby;
    private final Set<UUID> inDungeon = new HashSet<>();

    public DungeonManager(JavaPlugin plugin) { this.plugin = plugin; }

    public void createDungeon(Player p) {
        World world = p.getWorld();
        lobby = p.getLocation();
        Location base = new Location(world, p.getLocation().getX() + 100, 60, p.getLocation().getZ());
        dungeonSpawn = base.clone().add(2, 1, 2);
        for (int room = 0; room < 5; room++) {
            buildRoom(world, base.getBlockX() + room * 12, 60, base.getBlockZ(), room == 4);
        }
        p.sendMessage("§aDa tao Dungeon 1.21.10 5 phong! /dungeon join de vao.");
    }

    private void buildRoom(World world, int x, int y, int z, boolean isBossRoom) {
        for (int dx = 0; dx < 10; dx++) {
            for (int dz = 0; dz < 10; dz++) {
                Material floor = (dx + dz) % 2 == 0 ? Material.TUFF_BRICKS : Material.COPPER_BLOCK;
                world.getBlockAt(x + dx, y, z + dz).setType(floor);
                world.getBlockAt(x + dx, y + 4, z + dz).setType(Material.TUFF_BRICKS);
                if (dx == 0 || dx == 9 || dz == 0 || dz == 9) {
                    for (int h = 1; h <= 3; h++) world.getBlockAt(x + dx, y + h, z + dz).setType(Material.TUFF_BRICKS);
                } else {
                    for (int h = 1; h <= 3; h++) world.getBlockAt(x + dx, y + h, z + dz).setType(Material.AIR);
                }
            }
        }
        for (int h = 1; h <= 2; h++) {
            world.getBlockAt(x + 9, y + h, z + 5).setType(Material.AIR);
            world.getBlockAt(x, y + h, z + 5).setType(Material.AIR);
        }
        world.getBlockAt(x + 5, y + 3, z + 5).setType(Material.COPPER_BULB);
        Location spawn = new Location(world, x + 5, y + 1, z + 5);
        if (isBossRoom) {
            Breeze boss = (Breeze) world.spawnEntity(spawn, EntityType.BREEZE);
            boss.setCustomName("§c§lBOSS DUNGEON 1.21");
            boss.setCustomNameVisible(true);
            boss.setMaxHealth(150);
            boss.setHealth(150);
            world.getBlockAt(x + 5, y + 1, z + 8).setType(Material.CHEST);
            Chest chest = (Chest) world.getBlockAt(x + 5, y + 1, z + 8).getState();
            chest.getInventory().addItem(new ItemStack(Material.MACE));
            chest.getInventory().addItem(new ItemStack(Material.DIAMOND_SWORD));
            chest.getInventory().addItem(new ItemStack(Material.WIND_CHARGE, 16));
            chest.getInventory().addItem(new ItemStack(Material.DIAMOND, 5));
            chest.update();
        } else {
            EntityType[] types = {EntityType.BREEZE, EntityType.BOGGED, EntityType.ZOMBIE, EntityType.SKELETON};
            Random r = new Random();
            for (int i = 0; i < 4; i++) world.spawnEntity(spawn, types[r.nextInt(types.length)]);
        }
    }

    public void joinDungeon(Player p) {
        if (dungeonSpawn == null) { p.sendMessage("§cChua co dungeon! /dungeon create truoc."); return; }
        inDungeon.add(p.getUniqueId());
        p.teleport(dungeonSpawn);
        p.sendMessage("§aDa vao Dungeon! Diet Boss de lay Mace.");
    }
    public void leaveDungeon(Player p) {
        inDungeon.remove(p.getUniqueId());
        if (lobby != null) p.teleport(lobby);
        p.sendMessage("§eDa roi khoi Dungeon.");
    }
    @EventHandler
    public void onMobDeath(EntityDeathEvent e) {
        if (e.getEntity().getCustomName() != null && e.getEntity().getCustomName().contains("BOSS")) {
            Bukkit.broadcastMessage("§6§lBOSS DUNGEON DA BI HA! Mo ruong lay Mace!");
        }
    }
    @EventHandler
    public void onQuit(PlayerQuitEvent e) { inDungeon.remove(e.getPlayer().getUniqueId()); }
}
