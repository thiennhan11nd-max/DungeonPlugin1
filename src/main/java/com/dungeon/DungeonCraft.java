package com.dungeon;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Arrays;

public class DungeonCraft {
    public static ItemStack material(int dungeon) {
        Material mat = switch(dungeon){
            case 1 -> Material.WIND_CHARGE;
            case 2 -> Material.BLAZE_POWDER;
            case 3 -> Material.PRISMARINE_SHARD;
            case 4 -> Material.WITHER_ROSE;
            default -> Material.DIAMOND;
        };
        String name = switch(dungeon){
            case 1 -> "§b§lLINH HON GIO";
            case 2 -> "§c§lLOI LUA";
            case 3 -> "§9§lVAY BIEN";
            case 4 -> "§5§lTRAI TIM BONG TOI";
            default -> "§6NGUYEN LIEU";
        };
        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList("§7Nguyen lieu Boss " + dungeon, "§eGiet Boss " + dungeon + " de nhan", "§aDung de che Kiem / Giap Than"));
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        item.setItemMeta(meta);
        return item;
    }

    public static void registerRecipes(JavaPlugin plugin) {
        for (int d = 1; d <= 4; d++) {
            Material mat = material(d).getType();
            ItemStack sword = DungeonGear.makeSword(d);
            ShapedRecipe rSword = new ShapedRecipe(new NamespacedKey(plugin, "sword_" + d), sword);
            rSword.shape("XMX", "XMX", "XSX");
            rSword.setIngredient('M', mat);
            rSword.setIngredient('X', Material.NETHERITE_INGOT);
            rSword.setIngredient('S', Material.STICK);
            Bukkit.addRecipe(rSword);

            ItemStack helmet = DungeonGear.makeArmorPiece(d, 0);
            ShapedRecipe rHelmet = new ShapedRecipe(new NamespacedKey(plugin, "helmet_" + d), helmet);
            rHelmet.shape("MMM", "MXM", "   ");
            rHelmet.setIngredient('M', mat);
            rHelmet.setIngredient('X', Material.NETHERITE_HELMET);
            Bukkit.addRecipe(rHelmet);

            ItemStack chest = DungeonGear.makeArmorPiece(d, 1);
            ShapedRecipe rChest = new ShapedRecipe(new NamespacedKey(plugin, "chest_" + d), chest);
            rChest.shape("MXM", "MMM", "MMM");
            rChest.setIngredient('M', mat);
            rChest.setIngredient('X', Material.NETHERITE_CHESTPLATE);
            Bukkit.addRecipe(rChest);

            ItemStack legs = DungeonGear.makeArmorPiece(d, 2);
            ShapedRecipe rLegs = new ShapedRecipe(new NamespacedKey(plugin, "legs_" + d), legs);
            rLegs.shape("MMM", "MXM", "M M");
            rLegs.setIngredient('M', mat);
            rLegs.setIngredient('X', Material.NETHERITE_LEGGINGS);
            Bukkit.addRecipe(rLegs);

            ItemStack boots = DungeonGear.makeArmorPiece(d, 3);
            ShapedRecipe rBoots = new ShapedRecipe(new NamespacedKey(plugin, "boots_" + d), boots);
            rBoots.shape("M M", "MXM", "   ");
            rBoots.setIngredient('M', mat);
            rBoots.setIngredient('X', Material.NETHERITE_BOOTS);
            Bukkit.addRecipe(rBoots);
        }
        Bukkit.getLogger().info("§aDa dang ky FULL SET Gear Dungeon!");
    }
}
