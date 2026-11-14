package com.dungeon;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GearCommand implements CommandExecutor {
    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        if (args.length == 0) {
            p.sendMessage("§6§l=== GEAR DUNGEON ===");
            p.sendMessage("§e/gear 1 - Full set Gio");
            p.sendMessage("§e/gear 2 - Full set Lua");
            p.sendMessage("§e/gear 3 - Full set Bien");
            p.sendMessage("§e/gear 4 - Full set Bong Toi");
            p.sendMessage("§cChi OP moi dung duoc!");
            return true;
        }
        if (!p.isOp()) { p.sendMessage("§cChi OP moi lay duoc Gear!"); return true; }
        try { int id = Integer.parseInt(args[0]); if (id < 1 || id > 4) { p.sendMessage("§cChi co 1-4!"); return true; } DungeonGear.giveAll(p, id); }
        catch (NumberFormatException e) { p.sendMessage("§cDung: /gear 1"); }
        return true;
    }
}
