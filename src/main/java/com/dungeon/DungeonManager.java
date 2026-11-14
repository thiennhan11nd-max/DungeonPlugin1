package com.dungeon;

import org.bukkit.*;
import org.bukkit.block.Chest;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
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
        p.sendMessage("§eMe cung 5x5 - Tim duong toi phong Boss cuoi me cung!");
    }

    public void createDungeon(Player p, int id) {
        if (id < 1 || id > 4) { p.sendMessage("§cChi co Dungeon 1-4 thoi!"); return; }
        World world = p.getWorld();
        lobby = p.getLocation();
        Location base = new Location(world, p.getLocation().getX() + 100 + (id-1)*400, 60, p.getLocation().getZ());
        Location spawn = base.clone().add(2, 1, 2);
        spawns.put(id, spawn);
        buildMaze(world, base.getBlockX(), 60, base.getBlockZ(), id);
        buildPortalPads(world, spawn, id);
        p.sendMessage("§aDa tao Me Cung Dungeon " + id + "!");
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
        p.sendMessage("§aDa vao Me Cung Dungeon " + id + "!");
        p.sendMessage("§e§lME CUNG 5x5 - Di theo duong mon, tim phong Boss o cuoi!");
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

    // ===== ME CUNG: DFS - TAT CA PHONG DEU THONG NHAU =====
    private void buildMaze(World world, int startX, int y, int startZ, int id) {
        int N = 5;
        int CELL = 9;
        int SIZE = N * CELL + 1;
        Material floor = floorMat(id); Material wall = wallMat(id);
        for (int dx = 0; dx < SIZE; dx++) for (int dz = 0; dz < SIZE; dz++) {
            world.getBlockAt(startX+dx, y, startZ+dz).setType(floor);
            world.getBlockAt(startX+dx, y+4, startZ+dz).setType(wall);
            world.getBlockAt(startX+dx, y+1, startZ+dz).setType(wall);
            world.getBlockAt(startX+dx, y+2, startZ+dz).setType(wall);
            world.getBlockAt(startX+dx, y+3, startZ+dz).setType(wall);
        }
        boolean[][] visited = new boolean[N][N];
        boolean[][] wallH = new boolean[N+1][N];
        boolean[][] wallV = new boolean[N][N+1];
        for (int i=0;i<=N;i++) for(int j=0;j<N;j++) wallH[i][j]=true;
        for (int i=0;i<N;i++) for(int j=0;j<=N;j++) wallV[i][j]=true;
        Stack<int[]> st = new Stack<>();
        st.push(new int[]{0,0}); visited[0][0]=true;
        Random rnd = new Random(id * 12345L + System.currentTimeMillis()%1000);
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        while(!st.isEmpty()){
            int[] cur = st.peek();
            int r=cur[0], c=cur[1];
            List<int[]> nei = new ArrayList<>();
            for(int[] d: dirs){
                int nr=r+d[0], nc=c+d[1];
                if(nr>=0&&nr<N&&nc>=0&&nc<N&&!visited[nr][nc]) nei.add(new int[]{nr,nc});
            }
            if(nei.isEmpty()){ st.pop(); continue; }
            int[] next = nei.get(rnd.nextInt(nei.size()));
            int nr=next[0], nc=next[1];
            if(nr==r-1) wallH[r][c]=false;
            if(nr==r+1) wallH[r+1][c]=false;
            if(nc==c-1) wallV[r][c]=false;
            if(nc==c+1) wallV[r][c+1]=false;
            visited[nr][nc]=true;
            st.push(next);
        }
        for(int r=0;r<N;r++) for(int c=0;c<N;c++){
            int cellX = startX + c*CELL;
            int cellZ = startZ + r*CELL;
            for(int dx=1;dx<CELL;dx++) for(int dz=1;dz<CELL;dz++){
                for(int h=1;h<=3;h++) world.getBlockAt(cellX+dx, y+h, cellZ+dz).setType(Material.AIR);
            }
            if(!wallH[r][c]){
                for(int w=3;w<=5;w++) for(int h=1;h<=3;h++) world.getBlockAt(cellX+w, y+h, cellZ).setType(Material.AIR);
            }
            if(!wallH[r+1][c]){
                for(int w=3;w<=5;w++) for(int h=1;h<=3;h++) world.getBlockAt(cellX+w, y+h, cellZ+CELL).setType(Material.AIR);
            }
            if(!wallV[r][c]){
                for(int w=3;w<=5;w++) for(int h=1;h<=3;h++) world.getBlockAt(cellX, y+h, cellZ+w).setType(Material.AIR);
            }
            if(!wallV[r][c+1]){
                for(int w=3;w<=5;w++) for(int h=1;h<=3;h++) world.getBlockAt(cellX+CELL, y+h, cellZ+w).setType(Material.AIR);
            }
            if(r==0 && c==0){
                for(int w=3;w<=5;w++) for(int h=1;h<=3;h++) world.getBlockAt(cellX, y+h, cellZ+w).setType(Material.AIR);
            }
        }
        for(int r=0;r<N;r++) for(int c=0;c<N;c++){
            if((r==0&&c==0)||(r==N-1&&c==N-1)) continue;
            int cellX = startX + c*CELL;
            int cellZ = startZ + r*CELL;
            Location s = new Location(world, cellX+5, y+1, cellZ+5);
            mobSpawns.computeIfAbsent(id, k -> new ArrayList<>()).add(s);
            spawnMazeMob(world, s, id);
            if(rnd.nextBoolean()){
                Location s2 = new Location(world, cellX+2, y+1, cellZ+2);
                spawnMazeMob(world, s2, id);
            }
        }
        int bossCellX = startX + (N-1)*CELL;
        int bossCellZ = startZ + (N-1)*CELL;
        for(int dx=1;dx<CELL;dx++) for(int dz=1;dz<CELL;dz++) for(int h=1;h<=3;h++) world.getBlockAt(bossCellX+dx, y+h, bossCellZ+dz).setType(Material.AIR);
        Location center = new Location(world, bossCellX+5, y+1, bossCellZ+5);
        bossCenters.put(id, center);
        world.getBlockAt(bossCellX+5, y, bossCellZ+5).setType(Material.GOLD_BLOCK);
        Entity e = world.spawnEntity(center, bossType(id));
        if(e instanceof LivingEntity le){
            le.setPersistent(true); le.setRemoveWhenFarAway(false);
            le.setCustomName(bossName(id)); le.setCustomNameVisible(true);
            le.setMaxHealth(bossHp(id)); le.setHealth(bossHp(id));
            bosses.put(id, le);
        }
        world.getBlockAt(bossCellX+7, y+1, bossCellZ+7).setType(Material.CHEST);
        Chest chest = (Chest) world.getBlockAt(bossCellX+7, y+1, bossCellZ+7).getState();
        chest.getInventory().addItem(getLoot(id));
        chest.update();
    }

    private void spawnMazeMob(World world, Location s, int id){
        switch(id){
            case 1 -> {
                EntityType[] t = {EntityType.BOGGED, EntityType.BREEZE, EntityType.SKELETON};
                spawnMob(world, s, t[new Random().nextInt(t.length)], "§aLinh Dungeon 1", 30);
            }
            case 2 -> {
                EntityType[] t = {EntityType.BLAZE, EntityType.PIGLIN_BRUTE, EntityType.MAGMA_CUBE};
                spawnMob(world, s, t[new Random().nextInt(t.length)], "§cLinh Dungeon 2", 40);
            }
            case 3 -> {
                EntityType[] t = {EntityType.DROWNED, EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN};
                spawnMob(world, s, t[new Random().nextInt(t.length)], "§9Linh Dungeon 3", 50);
            }
            case 4 -> {
                EntityType[] t = {EntityType.WITHER_SKELETON, EntityType.ENDERMAN, EntityType.WARDEN};
                spawnMob(world, s, t[new Random().nextInt(t.length)], "§5Linh Dungeon 4", 60);
            }
        }
    }

    private ItemStack[] getLoot(int id){
        return switch(id){
            case 1 -> new ItemStack[]{ new ItemStack(Material.WIND_CHARGE,8), new ItemStack(Material.DIAMOND,3) };
            case 2 -> new ItemStack[]{ new ItemStack(Material.FIRE_CHARGE,8), new ItemStack(Material.BLAZE_ROD,4) };
            case 3 -> new ItemStack[]{ new ItemStack(Material.DIAMOND,4), new ItemStack(Material.NAUTILUS_SHELL,1) };
            case 4 -> new ItemStack[]{ new ItemStack(Material.NETHERITE_INGOT,1), new ItemStack(Material.GOLDEN_APPLE,2) };
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
                long alive = w.getNearbyEntities(spawns.get(id), 80, 10, 80).stream()
                    .filter(en -> en instanceof Mob && en.getCustomName() != null && en.getCustomName().contains("Linh"))
                    .count();
                if (alive < 5) {
                    List<Location> locs = mobSpawns.get(id);
                    if (locs != null && !locs.isEmpty()) {
                        Location s = locs.get(new Random().nextInt(locs.size()));
                        spawnMazeMob(w, s, id);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void tryDrop(World w, Location loc, Material mat, int amount, int chance) {
        if (new Random().nextInt(100) < chance) {
            w.dropItemNaturally(loc, new ItemStack(mat, amount));
        }
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent e){
        if(e.getEntity().getCustomName()==null) return;
        for(Map.Entry<Integer, LivingEntity> entry : new HashMap<>(bosses).entrySet()){
            if(e.getEntity().equals(entry.getValue())){
                int id = entry.getKey();
                Bukkit.broadcastMessage("§6§lBOSS " + id + " DA BI HA!");
                World w = e.getEntity().getWorld(); Location loc = e.getEntity().getLocation();
                w.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 2, 1);
                bosses.remove(id);
                bossRespawnAt.put(id, System.currentTimeMillis() + 60000);
                Bukkit.broadcastMessage("§eBoss " + id + " se hoi sinh sau 60s!");
                switch(id){
                    case 1 -> {
                        tryDrop(w, loc, Material.MACE, 1, 5);
                        tryDrop(w, loc, Material.WIND_CHARGE, 8, 30);
                        tryDrop(w, loc, Material.DIAMOND, 3, 30);
                        tryDrop(w, loc, Material.GOLDEN_APPLE, 1, 30);
                        tryDrop(w, loc, Material.TOTEM_OF_UNDYING, 1, 10);
                    }
                    case 2 -> {
                        tryDrop(w, loc, Material.NETHERITE_SWORD, 1, 8);
                        tryDrop(w, loc, Material.FIRE_CHARGE, 8, 30);
                        tryDrop(w, loc, Material.NETHERITE_INGOT, 1, 10);
                        tryDrop(w, loc, Material.GOLDEN_APPLE, 1, 30);
                        tryDrop(w, loc, Material.BLAZE_ROD, 4, 30);
                        tryDrop(w, loc, Material.TOTEM_OF_UNDYING, 1, 10);
                    }
                    case 3 -> {
                        tryDrop(w, loc, Material.TRIDENT, 1, 5);
                        tryDrop(w, loc, Material.HEART_OF_THE_SEA, 1, 10);
                        tryDrop(w, loc, Material.DIAMOND, 4, 30);
                        tryDrop(w, loc, Material.ENCHANTED_GOLDEN_APPLE, 1, 10);
                        tryDrop(w, loc, Material.NAUTILUS_SHELL, 1, 30);
                        tryDrop(w, loc, Material.TOTEM_OF_UNDYING, 1, 10);
                    }
                    case 4 -> {
                        tryDrop(w, loc, Material.NETHER_STAR, 1, 5);
                        tryDrop(w, loc, Material.NETHERITE_CHESTPLATE, 1, 8);
                        tryDrop(w, loc, Material.NETHERITE_SWORD, 1, 8);
                        tryDrop(w, loc, Material.TOTEM_OF_UNDYING, 1, 10);
                        tryDrop(w, loc, Material.ENCHANTED_GOLDEN_APPLE, 1, 10);
                    }
                }
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

    // ===== CHONG PHA ME CUNG - FULL COMBO =====
    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (inDungeon.contains(p.getUniqueId())) {
            e.setCancelled(true);
            p.sendMessage("§c§lCAM DAP BLOCK TRONG DUNGEON!");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (inDungeon.contains(p.getUniqueId())) {
            e.setCancelled(true);
            p.sendMessage("§c§lCAM DAT BLOCK TRONG DUNGEON!");
        }
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent e) {
        for (Location spawn : spawns.values()) {
            if (spawn != null && spawn.getWorld().equals(e.getLocation().getWorld())) {
                if (spawn.distance(e.getLocation()) < 100) {
                    e.setCancelled(true);
                    e.blockList().clear();
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onCreeperDamage(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Creeper) {
            for (Location spawn : spawns.values()) {
                if (spawn != null && spawn.getWorld().equals(e.getEntity().getWorld())) {
                    if (spawn.distance(e.getEntity().getLocation()) < 100) {
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onWitherBreakBlock(EntityChangeBlockEvent e) {
        if (e.getEntityType() == EntityType.WITHER || e.getEntityType() == EntityType.WITHER_SKULL) {
            e.setCancelled(true);
            return;
        }
        if (e.getEntityType() == EntityType.ENDERMAN) {
            for (Location spawn : spawns.values()) {
                if (spawn != null && spawn.getWorld().equals(e.getBlock().getWorld())) {
                    if (spawn.distance(e.getBlock().getLocation()) < 100) {
                        e.setCancelled(true);
                        return;
                    }
                }
            }
        }
        if (e.getEntity() instanceof Mob) {
            for (Location spawn : spawns.values()) {
                if (spawn != null && spawn.getWorld().equals(e.getBlock().getWorld())) {
                    if (spawn.distance(e.getBlock().getLocation()) < 100) {
                        e.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler public void onQuit(PlayerQuitEvent e){ inDungeon.remove(e.getPlayer().getUniqueId()); currentDungeon.remove(e.getPlayer().getUniqueId()); startTimes.remove(e.getPlayer().getUniqueId()); }
        }
