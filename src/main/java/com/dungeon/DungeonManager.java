package com.dungeon;

import org.bukkit.*;
import org.bukkit.block.Chest;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import java.util.*;

public class DungeonManager implements Listener {
    private final JavaPlugin plugin;
    private Location lobby;
    private final Map<Integer, Location> spawns = new HashMap<>();
    private final Map<Integer, LivingEntity> bosses = new HashMap<>();
    private final Map<Integer, Location> bossCenters = new HashMap<>();
    private final Set<UUID> inDungeon = new HashSet<>();
    private final Map<UUID, Long> startTimes = new HashMap<>();
    private final Map<UUID, Integer> currentDungeon = new HashMap<>();
    private final Map<String, Long> bestTimes = new HashMap<>();
    private final Map<String, Integer> bestDungeon = new HashMap<>();
    private final Map<Integer, Long> bossRespawnAt = new HashMap<>();
    private final Map<Integer, List<Location>> mobSpawns = new HashMap<>();

    public DungeonManager(JavaPlugin plugin) { this.plugin = plugin; }

    public void showTop(Player p) {
        p.sendMessage("§6§l=== BANG XEP HANG GIET BOSS NHANH NHAT ===");
        if (bestTimes.isEmpty()) { p.sendMessage("§eChua co ai giet Boss het!"); return; }
        List<Map.Entry<String, Long>> list = new ArrayList<>(bestTimes.entrySet());
        list.sort(Map.Entry.comparingByValue());
        int rank = 1;
        for (Map.Entry<String, Long> e : list) {
            if (rank > 10) break;
            p.sendMessage("§e#" + rank + " §f" + e.getKey() + " §a- Dungeon " + bestDungeon.get(e.getKey()) + " - §b" + e.getValue() + "s");
            rank++;
        }
    }

    public void showInfo(Player p) {
        p.sendMessage("§6§l=== THONG TIN DUNGEON ===");
        for (int id = 1; id <= 4; id++) {
            if (!spawns.containsKey(id)) { p.sendMessage("§7Dungeon " + id + ": Chua tao"); continue; }
            if (bosses.containsKey(id)) {
                LivingEntity b = bosses.get(id);
                p.sendMessage("§aDungeon " + id + ": Boss con song - HP: " + (int)b.getHealth() + "/" + (int)b.getMaxHealth());
            } else {
                long left = bossRespawnAt.containsKey(id) ? (bossRespawnAt.get(id) - System.currentTimeMillis())/1000 : 0;
                if (left < 0) left = 0;
                p.sendMessage("§cDungeon " + id + ": Boss da chet - Hoi sinh sau " + left + "s");
            }
        }
        p.sendMessage("§eQuai thuong tu spawn lai sau 30s neu chet het!");
    }

    public void createDungeon(Player p, int id) {
        if (id < 1 || id > 4) { p.sendMessage("§cChi co Dungeon 1-4 thoi!"); return; }
        World world = p.getWorld();
        lobby = p.getLocation();
        Location base = new Location(world, p.getLocation().getX() + 100 + (id-1)*300, 60, p.getLocation().getZ());
        Location spawn = base.clone().add(2, 1, 2);
        spawns.put(id, spawn);
        for (int room = 0; room < 3; room++) buildRoom(world, base.getBlockX() + room * 12, 60, base.getBlockZ(), id);
        int bossX = base.getBlockX() + 3 * 12 + 5;
        buildBossRoom(world, bossX, 60, base.getBlockZ(), id);
        buildPortalPads(world, spawn, id);
        p.sendMessage("§aDa tao Dungeon " + id + "!");
        startBossSkills(id);
        startRespawnLoop(id);
    }

    private void buildPortalPads(World world, Location spawn, int id) {
        Location base = spawn.clone().add(0, -1, 4);
        world.getBlockAt(base).setType(Material.LIME_WOOL);
        world.getBlockAt(base.clone().add(1, 0, 0)).setType(Material.YELLOW_WOOL);
        world.getBlockAt(base.clone().add(2, 0, 0)).setType(Material.RED_WOOL);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.getTo() == null) return;
        if (e.getFrom().getBlock().equals(e.getTo().getBlock())) return;
        Player p = e.getPlayer();
        if (!inDungeon.contains(p.getUniqueId())) return;
        Material under = e.getTo().clone().add(0, -1, 0).getBlock().getType();
        Integer cur = currentDungeon.get(p.getUniqueId());
        if (cur == null) return;
        if (under == Material.LIME_WOOL) {
            int next = cur % 4 + 1;
            p.sendMessage("§a§lCONG DICH CHUYEN! -> Dungeon " + next);
            joinDungeon(p, next);
        } else if (under == Material.YELLOW_WOOL) {
            int prev = cur == 1 ? 4 : cur - 1;
            p.sendMessage("§e§lCONG DICH CHUYEN! -> Dungeon " + prev);
            joinDungeon(p, prev);
        } else if (under == Material.RED_WOOL) {
            p.sendMessage("§c§lThoat Dungeon!");
            leaveDungeon(p);
        }
    }

    public void joinDungeon(Player p, int id) {
        if (!spawns.containsKey(id)) { createDungeon(p, id); }
        inDungeon.add(p.getUniqueId());
        currentDungeon.put(p.getUniqueId(), id);
        startTimes.put(p.getUniqueId(), System.currentTimeMillis());
        p.teleport(spawns.get(id));
        p.sendMessage("§aDa vao Dungeon " + id + "!");
        p.sendMessage("§e§lCONG: §aXanh la = Dungeon sau §e| Vang = Dungeon truoc §c| Do = Thoat");
        p.sendMessage("§6Giet Boss cang nhanh cang len TOP! /dungeon top");
    }
    public void leaveDungeon(Player p) {
        inDungeon.remove(p.getUniqueId());
        currentDungeon.remove(p.getUniqueId());
        startTimes.remove(p.getUniqueId());
        if (lobby != null) p.teleport(lobby);
        p.sendMessage("§eDa roi khoi Dungeon.");
    }

    private String bossName(int id) {
        return switch(id) {
            case 1 -> "§b§lBOSS 1 - VUA GIO";
            case 2 -> "§c§lBOSS 2 - VUA LUA";
            case 3 -> "§9§lBOSS 3 - VUA BIEN";
            case 4 -> "§5§lBOSS 4 - VUA BONG TOI";
            default -> "BOSS";
        };
    }
    private double bossHp(int id) { return switch(id){ case 1->300; case 2->400; case 3->500; case 4->600; default->300; }; }
    private EntityType bossType(int id) { return switch(id){ case 1->EntityType.BREEZE; case 2->EntityType.BLAZE; case 3->EntityType.ELDER_GUARDIAN; case 4->EntityType.WITHER; default->EntityType.BREEZE; }; }

    private void spawnMob(World world, Location loc, EntityType type, String name, double hp) {
        Entity e = world.spawnEntity(loc, type);
        if (e instanceof Mob mob) {
            mob.setPersistent(true); mob.setRemoveWhenFarAway(false);
            if (name != null) { mob.setCustomName(name); mob.setCustomNameVisible(true); }
            if (hp > 0) { mob.setMaxHealth(hp); mob.setHealth(hp); }
        }
    }
    private Material floorMat(int id) { return switch(id){ case 1->Material.TUFF_BRICKS; case 2->Material.NETHERRACK; case 3->Material.PRISMARINE_BRICKS; case 4->Material.DEEPSLATE_TILES; default->Material.STONE_BRICKS; }; }
    private Material wallMat(int id) { return switch(id){ case 1->Material.TUFF_BRICKS; case 2->Material.NETHER_BRICKS; case 3->Material.DARK_PRISMARINE; case 4->Material.OBSIDIAN; default->Material.STONE_BRICKS; }; }

    private void buildRoom(World world, int x, int y, int z, int id) {
        Material floor = floorMat(id); Material wall = wallMat(id);
        for (int dx = 0; dx < 10; dx++) for (int dz = 0; dz < 10; dz++) {
            world.getBlockAt(x+dx, y, z+dz).setType(floor);
            world.getBlockAt(x+dx, y+4, z+dz).setType(wall);
            if (dx==0||dx==9||dz==0||dz==9) for(int h=1;h<=3;h++) world.getBlockAt(x+dx,y+h,z+dz).setType(wall);
            else for(int h=1;h<=3;h++) world.getBlockAt(x+dx,y+h,z+dz).setType(Material.AIR);
        }
        for(int h=1;h<=2;h++){ world.getBlockAt(x+9,y+h,z+5).setType(Material.AIR); world.getBlockAt(x,y+h,z+5).setType(Material.AIR); }
        Location s = new Location(world, x+5, y+1, z+5);
        mobSpawns.computeIfAbsent(id, k -> new ArrayList<>()).add(s);
        switch(id){
            case 1 -> { spawnMob(world,s,EntityType.BOGGED,"§aLinh Dungeon 1",30); spawnMob(world,s.clone().add(2,0,2),EntityType.BREEZE,"§aLinh Dungeon 1",30); spawnMob(world,s.clone().add(-2,0,-2),EntityType.SKELETON,"§aLinh Dungeon 1",30); }
            case 2 -> { spawnMob(world,s,EntityType.BLAZE,"§cLinh Dungeon 2",40); spawnMob(world,s.clone().add(2,0,2),EntityType.PIGLIN_BRUTE,"§cLinh Dungeon 2",40); spawnMob(world,s.clone().add(-2,0,-2),EntityType.MAGMA_CUBE,"§cLinh Dungeon 2",40); }
            case 3 -> { spawnMob(world,s,EntityType.DROWNED,"§9Linh Dungeon 3",50); spawnMob(world,s.clone().add(2,0,2),EntityType.GUARDIAN,"§9Linh Dungeon 3",50); spawnMob(world,s.clone().add(-2,0,-2),EntityType.ELDER_GUARDIAN,"§9Linh Dungeon 3",50); }
            case 4 -> { spawnMob(world,s,EntityType.WITHER_SKELETON,"§5Linh Dungeon 4",60); spawnMob(world,s.clone().add(2,0,2),EntityType.ENDERMAN,"§5Linh Dungeon 4",60); spawnMob(world,s.clone().add(-2,0,-2),EntityType.WARDEN,"§5Linh Dungeon 4",60); }
        }
    }

    private void buildBossRoom(World world, int x, int y, int z, int id) {
        int S=30, H=8; Material floor=floorMat(id); Material wall=wallMat(id);
        for(int dx=0;dx<S;dx++) for(int dz=0;dz<S;dz++){
            boolean edge = dx<2||dx>=S-2||dz<2||dz>=S-2;
            world.getBlockAt(x+dx,y,z+dz).setType(edge?Material.MAGMA_BLOCK:floor);
            world.getBlockAt(x+dx,y+H,z+dz).setType(wall);
            if(dx==0||dx==S-1||dz==0||dz==S-1) for(int h=1;h<H;h++) world.getBlockAt(x+dx,y+h,z+dz).setType(wall);
            else for(int h=1;h<H;h++) world.getBlockAt(x+dx,y+h,z+dz).setType(Material.AIR);
        }
        for(int h=1;h<=3;h++) for(int w=-1;w<=1;w++) world.getBlockAt(x,y+h,z+S/2+w).setType(Material.AIR);
        Location center = new Location(world, x+S/2, y+1, z+S/2);
        bossCenters.put(id, center);
        Entity e = world.spawnEntity(center, bossType(id));
        if(e instanceof LivingEntity le){
            le.setPersistent(true); le.setRemoveWhenFarAway(false);
            le.setCustomName(bossName(id)); le.setCustomNameVisible(true);
            le.setMaxHealth(bossHp(id)); le.setHealth(bossHp(id));
            bosses.put(id, le);
        }
        world.getBlockAt(x+S/2, y+1, z+S-4).setType(Material.CHEST);
        Chest chest = (Chest) world.getBlockAt(x+S/2, y+1, z+S-4).getState();
        chest.getInventory().addItem(getLoot(id));
        chest.update();
    }

    private ItemStack[] getLoot(int id){
        return switch(id){
            case 1 -> new ItemStack[]{ new ItemStack(Material.MACE), new ItemStack(Material.WIND_CHARGE,32), new ItemStack(Material.DIAMOND,10), new ItemStack(Material.GOLDEN_APPLE,5) };
            case 2 -> new ItemStack[]{ new ItemStack(Material.NETHERITE_SWORD), new ItemStack(Material.FIRE_CHARGE,32), new ItemStack(Material.NETHERITE_INGOT,3), new ItemStack(Material.GOLDEN_APPLE,8), new ItemStack(Material.BLAZE_ROD,16) };
            case 3 -> new ItemStack[]{ new ItemStack(Material.TRIDENT), new ItemStack(Material.HEART_OF_THE_SEA), new ItemStack(Material.DIAMOND,15), new ItemStack(Material.ENCHANTED_GOLDEN_APPLE,2), new ItemStack(Material.NAUTILUS_SHELL,5) };
            case 4 -> new ItemStack[]{ new ItemStack(Material.NETHER_STAR), new ItemStack(Material.NETHERITE_CHESTPLATE), new ItemStack(Material.NETHERITE_SWORD), new ItemStack(Material.TOTEM_OF_UNDYING,2), new ItemStack(Material.ENCHANTED_GOLDEN_APPLE,3) };
            default -> new ItemStack[]{ new ItemStack(Material.DIAMOND,5) };
        };
    }

    private void startBossSkills(int id) {
        new BukkitRunnable(){
            int tick=0;
            @Override public void run(){
                LivingEntity boss = bosses.get(id);
                if(boss==null || boss.isDead()){ cancel(); return; }
                tick++;
                World world = boss.getWorld(); Location bl = boss.getLocation();
                List<Player> targets = new ArrayList<>();
                for(Player p: world.getPlayers()) if(p.getLocation().distance(bl)<35) targets.add(p);
                if(targets.isEmpty()) return;
                boolean enraged = boss.getHealth() < bossHp(id)/2;
                int interval = enraged ? 60 : 100;
                if(tick % interval != 0) return;
                Player target = targets.get(new Random().nextInt(targets.size()));
                Random r = new Random();
                switch(id){
                    case 1 -> {
                        int s = r.nextInt(3);
                        if(s==0){ Bukkit.broadcastMessage("§b§lBOSS 1: BAO GIO!"); for(int i=0;i<8;i++){ double a=i*Math.PI/4; Vector dir=new Vector(Math.cos(a),0.3,Math.sin(a)); WindCharge wc=(WindCharge)world.spawnEntity(bl.clone().add(0,1,0),EntityType.WIND_CHARGE); wc.setVelocity(dir.multiply(1.5)); } }
                        else if(s==1){ Bukkit.broadcastMessage("§e§lBOSS 1: TRIEU HOI!"); spawnMob(world,bl.clone().add(3,0,0),EntityType.BOGGED,"§eLinh Boss 1",40); spawnMob(world,bl.clone().add(-3,0,0),EntityType.BREEZE,"§eLinh Boss 1",40); }
                        else { Bukkit.broadcastMessage("§d§lBOSS 1: SOC GIO!"); for(Player p:targets){ Vector push=p.getLocation().toVector().subtract(bl.toVector()).normalize().multiply(2); push.setY(1.2); p.setVelocity(push); p.damage(3); } }
                    }
                    case 2 -> {
                        int s = r.nextInt(3);
                        if(s==0){ Bukkit.broadcastMessage("§c§lBOSS 2: MUA CAU LUA!"); for(int i=0;i<5;i++){ Fireball fb=(Fireball)world.spawnEntity(bl.clone().add(0,2,0),EntityType.FIREBALL); Vector dir=target.getLocation().toVector().subtract(bl.toVector()).normalize(); fb.setVelocity(dir.multiply(1.2)); } }
                        else if(s==1){ Bukkit.broadcastMessage("§6§lBOSS 2: GOI LINH LUA!"); spawnMob(world,bl.clone().add(3,0,0),EntityType.BLAZE,"§cLinh Lua",40); spawnMob(world,bl.clone().add(-3,0,0),EntityType.MAGMA_CUBE,"§cLinh Lua",40); }
                        else { Bukkit.broadcastMessage("§4§lBOSS 2: VONG LUA!"); for(Player p:targets){ p.setFireTicks(100); p.damage(4); } world.spawnParticle(Particle.FLAME, bl, 60, 3,1,3, 0.2); }
                    }
                    case 3 -> {
                        int s = r.nextInt(3);
                        if(s==0){ Bukkit.broadcastMessage("§9§lBOSS 3: TRIEU HOI SET!"); world.strikeLightning(target.getLocation()); target.damage(5); }
                        else if(s==1){ Bukkit.broadcastMessage("§b§lBOSS 3: GOI QUAI BIEN!"); spawnMob(world,bl.clone().add(3,0,0),EntityType.DROWNED,"§9Linh Bien",50); spawnMob(world,bl.clone().add(-3,0,0),EntityType.GUARDIAN,"§9Linh Bien",50); }
                        else { Bukkit.broadcastMessage("§3§lBOSS 3: SONG THAN!"); for(Player p:targets){ p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 100, 2)); p.damage(4); } world.spawnParticle(Particle.SPLASH, bl, 80, 3,1,3, 0.3); }
                    }
                    case 4 -> {
                        int s = r.nextInt(4);
                        if(s==0){ Bukkit.broadcastMessage("§5§lBOSS 4: DAU WITHER!"); for(int i=0;i<3;i++){ WitherSkull sk=(WitherSkull)world.spawnEntity(bl.clone().add(0,2,0),EntityType.WITHER_SKULL); Vector dir=target.getLocation().toVector().subtract(bl.toVector()).normalize(); sk.setVelocity(dir.multiply(1.5)); } }
                        else if(s==1){ Bukkit.broadcastMessage("§d§lBOSS 4: HUT MAU!"); for(Player p:targets){ p.damage(5); boss.setHealth(Math.min(boss.getMaxHealth(), boss.getHealth()+10)); } }
                        else if(s==2){ Bukkit.broadcastMessage("§8§lBOSS 4: GOI LINH BONG TOI!"); spawnMob(world,bl.clone().add(3,0,0),EntityType.WITHER_SKELETON,"§5Linh Toi",60); spawnMob(world,bl.clone().add(-3,0,0),EntityType.ENDERMAN,"§5Linh Toi",60); }
                        else {
                            Bukkit.broadcastMessage("§5§lBOSS 4: TANG HINH! Tim ta di!");
                            boss.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY, 100, 1));
                            Location behind = target.getLocation().add(target.getLocation().getDirection().multiply(-3));
                            behind.setY(target.getLocation().getY());
                            boss.teleport(behind);
                            world.spawnParticle(Particle.SMOKE, bl, 40, 1,1,1, 0.3);
                            world.playSound(bl, Sound.ENTITY_ENDERMAN_TELEPORT, 2, 1);
                            new BukkitRunnable(){ @Override public void run(){ if(!boss.isDead()){ boss.removePotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY); Bukkit.broadcastMessage("§5§lBOSS 4: TA O DAY NAY!"); world.spawnParticle(Particle.SOUL, boss.getLocation(), 30, 1,1,1, 0.3); } } }.runTaskLater(plugin, 100L);
                        }
                    }
                }
                if(enraged) Bukkit.broadcastMessage("§4§lBOSS DANG NOI GIAN!");
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void startRespawnLoop(int id) {
        new BukkitRunnable() {
            @Override public void run() {
                if (!spawns.containsKey(id)) { cancel(); return; }
                if (!bosses.containsKey(id) && bossRespawnAt.containsKey(id)) {
                    long left = (bossRespawnAt.get(id) - System.currentTimeMillis()) / 1000;
                    if (left <= 0) {
                        Location center = bossCenters.get(id);
                        if (center != null) {
                            Entity e = center.getWorld().spawnEntity(center, bossType(id));
                            if (e instanceof LivingEntity le) {
                                le.setPersistent(true); le.setRemoveWhenFarAway(false);
                                le.setCustomName(bossName(id)); le.setCustomNameVisible(true);
                                le.setMaxHealth(bossHp(id)); le.setHealth(bossHp(id));
                                bosses.put(id, le);
                                Bukkit.broadcastMessage("§c§lBOSS " + id + " DA HOI SINH! Mau vao danh!");
                                startBossSkills(id);
                            }
                            bossRespawnAt.remove(id);
                        }
                    }
                }
                World w = spawns.get(id).getWorld();
                long alive = w.getNearbyEntities(spawns.get(id), 60, 10, 60).stream()
                    .filter(en -> en instanceof Mob && en.getCustomName() != null && en.getCustomName().contains("Linh"))
                    .count();
                if (alive < 3) {
                    List<Location> locs = mobSpawns.get(id);
                    if (locs != null && !locs.isEmpty()) {
                        Location s = locs.get(new Random().nextInt(locs.size()));
                        switch(id){
                            case 1 -> spawnMob(w, s, EntityType.BOGGED, "§aLinh Dungeon 1", 30);
                            case 2 -> spawnMob(w, s, EntityType.BLAZE, "§cLinh Dungeon 2", 40);
                            case 3 -> spawnMob(w, s, EntityType.DROWNED, "§9Linh Dungeon 3", 50);
                            case 4 -> spawnMob(w, s, EntityType.WITHER_SKELETON, "§5Linh Dungeon 4", 60);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent e){
        if(e.getEntity().getCustomName()==null) return;
        for(Map.Entry<Integer, LivingEntity> entry : new HashMap<>(bosses).entrySet()){
            if(e.getEntity().equals(entry.getValue())){
                int id = entry.getKey();
                Bukkit.broadcastMessage("§6§lBOSS " + id + " DA BI HA!");
                World w = e.getEntity().getWorld(); Location loc = e.getEntity().getLocation();
                for(ItemStack item : getLoot(id)) w.dropItemNaturally(loc, item);
                w.dropItemNaturally(loc, new ItemStack(Material.TOTEM_OF_UNDYING,1));
                w.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 2, 1);
                bosses.remove(id);
                bossRespawnAt.put(id, System.currentTimeMillis() + 60000);
                Bukkit.broadcastMessage("§eBoss " + id + " se hoi sinh sau 60s!");
                for(Player p : w.getPlayers()){
                    if(inDungeon.contains(p.getUniqueId()) && currentDungeon.get(p.getUniqueId())!=null && currentDungeon.get(p.getUniqueId())==id && startTimes.containsKey(p.getUniqueId())){
                        long secs = (System.currentTimeMillis() - startTimes.get(p.getUniqueId()))/1000;
                        String name = p.getName();
                        if(!bestTimes.containsKey(name) || secs < bestTimes.get(name)){
                            bestTimes.put(name, secs);
                            bestDungeon.put(name, id);
                            Bukkit.broadcastMessage("§6§lKY LUC MOI! " + name + " giet Boss " + id + " trong " + secs + "s!");
                        } else {
                            p.sendMessage("§eBan giet Boss " + id + " trong " + secs + "s. Ky luc cua ban: " + bestTimes.get(name) + "s");
                        }
                    }
                }
            }
        }
    }
    @EventHandler public void onQuit(PlayerQuitEvent e){ inDungeon.remove(e.getPlayer().getUniqueId()); currentDungeon.remove(e.getPlayer().getUniqueId()); startTimes.remove(e.getPlayer().getUniqueId()); }
    }
