package com.dungeon;

import org.bukkit.plugin.java.JavaPlugin;

public class DungeonPlugin extends JavaPlugin {
    private DungeonManager manager;

    @Override
    public void onEnable() {
        manager = new DungeonManager(this);
        getCommand("dungeon").setExecutor((sender, cmd, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player p)) {
                sender.sendMessage("Chi player moi dung duoc!");
                return true;
            }
            if (args.length == 0) {
                p.sendMessage("§e/dungeon create | join | leave");
                return true;
            }
            switch (args[0].toLowerCase()) {
                case "create" -> manager.createDungeon(p);
                case "join" -> manager.joinDungeon(p);
                case "leave" -> manager.leaveDungeon(p);
                default -> p.sendMessage("§cLenh khong hop le!");
            }
            return true;
        });
        getServer().getPluginManager().registerEvents(manager, this);
        getLogger().info("DungeonPlugin 1.21.10 da bat!");
    }
}
