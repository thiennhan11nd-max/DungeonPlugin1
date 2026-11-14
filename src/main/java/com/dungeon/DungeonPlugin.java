package com.dungeon;

import org.bukkit.plugin.java.JavaPlugin;

public class DungeonPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        DungeonManager manager = new DungeonManager(this);
        getServer().getPluginManager().registerEvents(manager, this);
        getCommand("dungeon").setExecutor(new DungeonCommand(manager));
        getCommand("gear").setExecutor(new GearCommand());
        DungeonCraft.registerRecipes(this);
        getLogger().info("DungeonPlugin da bat! Co Dungeon + Gear + Craft!");
    }
}
