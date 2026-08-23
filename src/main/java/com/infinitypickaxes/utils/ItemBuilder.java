package com.infinitypickaxes.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material != null ? material : Material.STONE);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(ItemStack itemStack) {
        this.item = itemStack != null ? itemStack.clone() : new ItemStack(Material.STONE);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder name(String miniMessageName) {
        if (meta != null && miniMessageName != null) {
            meta.displayName(TextUtil.parse(miniMessageName));
        }
        return this;
    }

    public ItemBuilder name(Component component) {
        if (meta != null && component != null) {
            meta.displayName(component);
        }
        return this;
    }

    public ItemBuilder lore(List<String> miniMessageLore) {
        if (meta != null && miniMessageLore != null) {
            meta.lore(TextUtil.parseList(miniMessageLore));
        }
        return this;
    }

    public ItemBuilder loreComponents(List<Component> lore) {
        if (meta != null && lore != null) {
            meta.lore(lore);
        }
        return this;
    }

    public ItemBuilder addLoreLine(String line) {
        if (meta != null && line != null) {
            List<Component> current = meta.lore();
            if (current == null) current = new ArrayList<>();
            current.add(TextUtil.parse(line));
            meta.lore(current);
        }
        return this;
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, amount));
        return this;
    }

    public ItemBuilder unbreakable(boolean unbreakable) {
        if (meta != null) {
            meta.setUnbreakable(unbreakable);
        }
        return this;
    }

    public ItemBuilder flags(ItemFlag... flags) {
        if (meta != null && flags != null) {
            meta.addItemFlags(flags);
        }
        return this;
    }

    public ItemBuilder hideAllFlags() {
        if (meta != null) {
            meta.addItemFlags(ItemFlag.values());
        }
        return this;
    }

    public ItemBuilder customModelData(Integer data) {
        if (meta != null && data != null) {
            meta.setCustomModelData(data);
        }
        return this;
    }

    public ItemBuilder enchant(Enchantment enchantment, int level) {
        if (meta != null && enchantment != null) {
            meta.addEnchant(enchantment, level, true);
        }
        return this;
    }

    public <T, Z> ItemBuilder pdc(NamespacedKey key, PersistentDataType<T, Z> type, Z value) {
        if (meta != null && key != null && type != null && value != null) {
            meta.getPersistentDataContainer().set(key, type, value);
        }
        return this;
    }

    public ItemStack build() {
        if (meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }
}
