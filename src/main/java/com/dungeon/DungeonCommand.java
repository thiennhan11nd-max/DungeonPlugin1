package com.dungeon;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DungeonCommand implements CommandExecutor {
    private final DungeonManager manager;
    public DungeonCommand(DungeonManager manager) { this.manager = manager; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Chi nguoi choi moi dung duoc!"); return true; }
        if (args.length == 0) {
            p.sendMessage("§6§l=== DUNGEON ===");
            p.sendMessage("§e/dungeon 1-4 - Vao Dungeon");
            p.sendMessage("§e/dungeon top - Xem TOP");
            p.sendMessage("§e/dungeon info - Xem Boss");
            p.sendMessage("§e/dungeon leave - Thoat");
            p.sendMessage("§c/dungeon delete 1-4 - Xoa Dungeon");
            p.sendMessage("§c/dungeon delete all - Xoa het");
            return true;
        }
        String a = args[0].toLowerCase();
        if (a.equals("top")) { manager.showTop(p); return true; }
        if (a.equals("info")) { manager.showInfo(p); return true; }
        if (a.equals("leave")) { manager.leaveDungeon(p); return true; }
        if (a.equals("delete")) {
            if (!p.isOp()) { p.sendMessage("§cChi OP moi duoc xoa Dungeon!"); return true; }
            if (args.length < 2) { p.sendMessage("§cDung: /dungeon delete 1 / 2 / 3 / 4 / all"); return true; }
            if (args[1].equalsIgnoreCase("all")) { manager.deleteDungeon(p, -1); return true; }
            try {
                int id = Integer.parseInt(args[1]);
                manager.deleteDungeon(p, id);
            } catch (NumberFormatException e) { p.sendMessage("§cDung: /dungeon delete 1"); }
            return true;
        }
        try {
            int id = Integer.parseInt(a);
            manager.joinDungeon(p, id);
        } catch (NumberFormatException e) {
            p.sendMessage("§cDung: /dungeon 1");
        }
        return true;
    }
}
