package com.dungeon;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Arrays;

public class DungeonGear {
    public static ItemStack makeSword(int dungeon) {
        String name = switch(dungeon){
            case 1 -> "§b§lKIEM THAN GIO";
            case 2 -> "§c§lKIEM THAN LUA";
            case 3 -> "§9§lKIEM THAN BIEN";
            case 4 -> "§5§lKIEM THAN BONG TOI";
            default -> "§6§lKIEM DUNGEON";
        };
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD, 1);
        ItemMeta meta = sword.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList("§7Kiem Dungeon " + dungeon, "§eDamage cuc manh", "§aChe tu Nguyen lieu Boss " + dungeon));
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS);
        int dmg = switch(dungeon){ case 1->5; case 2->6; case 3->7; case 4->10; default->5; };
        meta.addEnchant(Enchantment.SHARPNESS, dmg, true);
        meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
        meta.addEnchant(Enchantment.KNOCKBACK, 2, true);
        meta.addEnchant(Enchantment.LOOTING, 3, true);
        meta.addEnchant(Enchantment.UNBREAKING, 10, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);
        sword.setItemMeta(meta);
        return sword;
    }

    public static ItemStack makeArmorPiece(int dungeon, int slot) {
        String prefix = switch(dungeon){ case 1->"§b§l"; case 2->"§c§l"; case 3->"§9§l"; case 4->"§5§l"; default->"§6§l"; };
        String name = switch(dungeon){ case 1->"GIAP GIO"; case 2->"GIAP LUA"; case 3->"GIAP BIEN"; case 4->"GIAP BONG TOI"; default->"GIAP DUNGEON"; };
        Material[] mats = {Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS};
        String[] parts = {"Mu", "Ao", "Quan", "Giay"};
        ItemStack item = new ItemStack(mats[slot], 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(prefix + name + " - " + parts[slot]);
        meta.setLore(Arrays.asList("§7Giap Dungeon " + dungeon, "§eSieu cung - Giam 80% sat thuong"));
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.PROTECTION, 4, true);
        meta.addEnchant(Enchantment.FIRE_PROTECTION, 4, true);
        meta.addEnchant(Enchantment.BLAST_PROTECTION, 4, true);
        meta.addEnchant(Enchantment.PROJECTILE_PROTECTION, 4, true);
        meta.addEnchant(Enchantment.THORNS, 3, true);
        meta.addEnchant(Enchantment.UNBREAKING, 10, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);
        if (slot==3) meta.addEnchant(Enchantment.DEPTH_STRIDER, 3, true);
        if (slot==0) meta.addEnchant(Enchantment.RESPIRATION, 3, true);
        item.setItemMeta(meta);
        return item;
    }

    public static void giveArmor(Player p, int dungeon) {
        for (int i=0;i<4;i++) p.getInventory().addItem(makeArmorPiece(dungeon, i));
        p.sendMessage("§aDa nhan full Giap Dungeon " + dungeon + "!");
    }

    public static void giveAll(Player p, int dungeon) {
        p.getInventory().addItem(makeSword(dungeon));
        giveArmor(p, dungeon);
        p.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 16));
        p.getInventory().addItem(new ItemStack(Material.TOTEM_OF_UNDYING, 2));
        p.getInventory().addItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 2));
        p.sendMessage("§6§lDA NHAN FULL SET DUNGEON " + dungeon + "!");
    }
          }
