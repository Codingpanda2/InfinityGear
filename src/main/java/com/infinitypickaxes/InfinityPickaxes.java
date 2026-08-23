package com.infinitypickaxes;

import com.infinitypickaxes.commands.InfinityPickaxeCommand;
import com.infinitypickaxes.config.ConfigManager;
import com.infinitypickaxes.config.MessageManager;
import com.infinitypickaxes.core.enchant.EnchantManager;
import com.infinitypickaxes.core.level.LevelManager;
import com.infinitypickaxes.core.perk.PerkManager;
import com.infinitypickaxes.core.pickaxe.PickaxeManager;
import com.infinitypickaxes.gui.GuiManager;
import com.infinitypickaxes.listeners.*;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class InfinityPickaxes extends JavaPlugin {

    private static InfinityPickaxes instance;

    private ConfigManager configManager;
    private MessageManager messageManager;
    private LevelManager levelManager;
    private EnchantManager enchantManager;
    private PerkManager perkManager;
    private PickaxeManager pickaxeManager;
    private GuiManager guiManager;
    private PickaxeHeldListener heldListener;

    @Override
    public void onEnable() {
        instance = this;

        long start = System.currentTimeMillis();
        getLogger().info("Iniciando InfinityPickaxes...");

        // 1. Initialize Configuration & Messages
        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);

        // 2. Initialize Core Subsystems
        this.levelManager = new LevelManager(this);
        this.enchantManager = new EnchantManager(this);
        this.perkManager = new PerkManager(this);
        this.pickaxeManager = new PickaxeManager(this);
        this.guiManager = new GuiManager(this);

        // 3. Register Event Listeners
        PluginManager pm = getServer().getPluginManager();
        BlockPlaceListener placeListener = new BlockPlaceListener(this);
        pm.registerEvents(placeListener, this);
        pm.registerEvents(new BlockBreakListener(this, placeListener), this);
        pm.registerEvents(new PickaxeInteractListener(this), this);
        pm.registerEvents(this.guiManager, this);

        this.heldListener = new PickaxeHeldListener(this);
        pm.registerEvents(this.heldListener, this);

        // 4. Register PlaceholderAPI Hook if present
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new com.infinitypickaxes.hooks.PlaceholderAPIHook(this).register();
            getLogger().info("PlaceholderAPI detectado. Placeholders de InfinityPickaxes registrados para zMenu/DeluxeMenus.");
        }

        // 5. Register Commands
        PluginCommand cmd = getCommand("infinitypickaxes");
        if (cmd != null) {
            InfinityPickaxeCommand executor = new InfinityPickaxeCommand(this);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        long elapsed = System.currentTimeMillis() - start;
        getLogger().info("InfinityPickaxes v" + getDescription().getVersion() + " habilitado correctamente en " + elapsed + "ms.");
    }

    @Override
    public void onDisable() {
        if (heldListener != null) {
            heldListener.stopTickTask();
        }
        getLogger().info("InfinityPickaxes deshabilitado.");
        instance = null;
    }

    public static InfinityPickaxes getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public EnchantManager getEnchantManager() {
        return enchantManager;
    }

    public PerkManager getPerkManager() {
        return perkManager;
    }

    public PickaxeManager getPickaxeManager() {
        return pickaxeManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }
}
