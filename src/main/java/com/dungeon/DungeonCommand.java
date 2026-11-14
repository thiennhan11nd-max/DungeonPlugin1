package com.dungeon;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
public class DungeonCommand implements CommandExecutor {
    private final DungeonManager m;
    public DungeonCommand(DungeonManager m){ this.m=m; }
    @Override public boolean onCommand(CommandSender s, Command c, String l, String[] a){
        if(!(s instanceof Player p)){ s.sendMessage("Chi player moi dung duoc"); return true; }
        if(a.length==0){ p.sendMessage("§e/dungeon 1|2|3|4 - /dungeon top - /dungeon info - /dungeon leave"); return true; }
        if(a[0].equalsIgnoreCase("leave")){ m.leaveDungeon(p); return true; }
        if(a[0].equalsIgnoreCase("top")){ m.showTop(p); return true; }
        if(a[0].equalsIgnoreCase("info")){ m.showInfo(p); return true; }
        try{
            int id = Integer.parseInt(a[0]);
            if(id>=1 && id<=4){ m.joinDungeon(p, id); return true; }
        }catch(Exception e){}
        if(a[0].equalsIgnoreCase("create") && a.length>1){
            try{ int id=Integer.parseInt(a[1]); m.createDungeon(p,id); return true; }catch(Exception e){}
        }
        p.sendMessage("§cDung: /dungeon 1, 2, 3, 4, top, info, leave");
        return true;
    }
}
