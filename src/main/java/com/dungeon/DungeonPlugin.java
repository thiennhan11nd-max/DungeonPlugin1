package com.dungeon;

import org.bukkit.plugin.java.JavaPlugin;

public class DungeonPlugin extends JavaPlugin {
    private DungeonManager manager;

    @Override
    public void onEnable() {
        manager = new DungeonManager(this);
        getCommand("dungeon").setExecutor(new DungeonCommand(manager));
        getServer().getPluginManager().registerEvents(manager, this);
        getLogger().info("DungeonPlugin 1.21.10 da bat!");
    }
}
