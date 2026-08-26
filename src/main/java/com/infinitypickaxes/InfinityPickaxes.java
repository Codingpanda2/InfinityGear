package com.infinitypickaxes;

import com.infinitypickaxes.commands.InfinityPickaxeCommand;
import com.infinitypickaxes.config.ConfigManager;
import com.infinitypickaxes.config.MessageManager;
import com.infinitypickaxes.core.enchant.EnchantManager;
import com.infinitypickaxes.core.duplicate.PickaxeDuplicateService;
import com.infinitypickaxes.core.level.LevelManager;
import com.infinitypickaxes.core.limitbreak.LimitBreakManager;
import com.infinitypickaxes.core.pickaxe.PickaxeManager;
import com.infinitypickaxes.gui.CustomGui;
import com.infinitypickaxes.gui.GuiManager;
import com.infinitypickaxes.hooks.PlaceholderAPIHook;
import com.infinitypickaxes.listeners.*;
import com.infinitypickaxes.utils.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class InfinityPickaxes extends JavaPlugin {

    private static InfinityPickaxes instance;

    private ConfigManager configManager;
    private MessageManager messageManager;
    private LevelManager levelManager;
    private EnchantManager enchantManager;
    private LimitBreakManager limitBreakManager;
    private PickaxeManager pickaxeManager;
    private GuiManager guiManager;
    private PickaxeHeldListener heldListener;
    private PlaceholderAPIHook papiHook;
    private PickaxeDuplicateService duplicateService;
    private DuplicateDetectionListener duplicateListener;

    @Override
    public void onEnable() {
        instance = this;
        long start = System.currentTimeMillis();
        ConsoleCommandSender console = Bukkit.getConsoleSender();

        // Print header banner
        console.sendMessage(TextUtil.parse(""));
        console.sendMessage(TextUtil.parse("<gradient:#00E5FF:#0077FE>  ██╗███╗   ██╗███████╗██╗███╗   ██╗██╗████████╗██╗   ██╗</gradient>"));
        console.sendMessage(TextUtil.parse("<gradient:#00E5FF:#0077FE>  ██║████╗  ██║██╔════╝██║████╗  ██║██║╚══██╔══╝╚██╗ ██╔╝</gradient>"));
        console.sendMessage(TextUtil.parse("<gradient:#00E5FF:#0077FE>  ██║██╔██╗ ██║█████╗  ██║██╔██╗ ██║██║   ██║    ╚████╔╝ </gradient>"));
        console.sendMessage(TextUtil.parse("<gradient:#00E5FF:#0077FE>  ██║██║╚██╗██║██╔══╝  ██║██║╚██╗██║██║   ██║     ╚██╔╝  </gradient>"));
        console.sendMessage(TextUtil.parse("<gradient:#00E5FF:#0077FE>  ██║██║ ╚████║██║     ██║██║ ╚████║██║   ██║      ██║   </gradient>"));
        console.sendMessage(TextUtil.parse("<gradient:#0077FE:#00E5FF>  ╚═╝╚═╝  ╚═══╝╚═╝     ╚═╝╚═╝  ╚═══╝╚═╝   ╚═╝      ╚═╝   </gradient>"));
        console.sendMessage(TextUtil.parse("<gradient:#00FF88:#00E5FF><b>       ⛏️ INFINITY PICKAXES </b></gradient><gray>v<yellow>" + getDescription().getVersion() + "</yellow> <dark_gray>┃</dark_gray> <gray>Paper 26.2"));
        console.sendMessage(TextUtil.parse("<dark_gray>  ─────────────────────────────────────────────────────────────</dark_gray>"));

        // 1. Initialize Configuration & Locales
        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);
        console.sendMessage(TextUtil.parse("<green>  ✔ <dark_gray>[1/6]</dark_gray> <white>Configuration & Locales:</white> <green>Loaded (Default: " + configManager.getCurrentLanguage() + ")</green>"));

        // 2. Initialize Core Subsystems & LimitBreak
        this.levelManager = new LevelManager(this);
        this.enchantManager = new EnchantManager(this);
        this.limitBreakManager = new LimitBreakManager(this);
        try {
            this.duplicateService = new PickaxeDuplicateService(this);
        } catch (Exception exception) {
            getLogger().severe("Could not initialize duplicate protection: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.pickaxeManager = new PickaxeManager(this);
        this.guiManager = new GuiManager(this);

        int socketsCount = enchantManager.getAllSockets().size();
        boolean ecoPresent = enchantManager.getEcoHook().isEcoEnchantsPresent();
        console.sendMessage(TextUtil.parse("<green>  ✔ <dark_gray>[2/6]</dark_gray> <white>EcoEnchants & LimitBreak:</white> " +
                (ecoPresent ? "<green>Connected </green>" : "<red>Unavailable </red>") +
                "<dark_gray>(" + socketsCount + " sockets, LimitBreak +" + limitBreakManager.getMaxExtraLevels() + ")</dark_gray>"));

        console.sendMessage(TextUtil.parse("<green>  ✔ <dark_gray>[3/6]</dark_gray> <white>Leveling System:</white> <green>Ready </green><dark_gray>(Max Level: " + levelManager.getMaxLevel() + ")</dark_gray>"));
        console.sendMessage(TextUtil.parse("<green>  ✔ <dark_gray>[4/6]</dark_gray> <white>Duplicate Protection:</white> <green>Ready</green>"));

        // 3. Register Event Listeners
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new QuarantineListener(this), this);
        BlockPlaceListener placeListener = new BlockPlaceListener(this);
        pm.registerEvents(placeListener, this);
        pm.registerEvents(new BlockBreakListener(this, placeListener), this);
        pm.registerEvents(new PickaxeInteractListener(this), this);
        pm.registerEvents(this.guiManager, this);

        this.heldListener = new PickaxeHeldListener(this);
        pm.registerEvents(this.heldListener, this);
        this.duplicateListener = new DuplicateDetectionListener(this);
        pm.registerEvents(this.duplicateListener, this);

        // 4. Register PlaceholderAPI Hook if present
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            this.papiHook = new PlaceholderAPIHook(this);
            this.papiHook.register();
            console.sendMessage(TextUtil.parse("<green>  ✔ <dark_gray>[5/6]</dark_gray> <white>PlaceholderAPI Bridge:</white> <green>Expansion registered for zMenu/DeluxeMenus.</green>"));
        } else {
            console.sendMessage(TextUtil.parse("<yellow>  ℹ <dark_gray>[5/6]</dark_gray> <white>PlaceholderAPI Bridge:</white> <dark_gray>Not detected (Optional).</dark_gray>"));
        }

        // 5. Register Commands
        PluginCommand cmd = getCommand("infinitypickaxes");
        if (cmd != null) {
            InfinityPickaxeCommand executor = new InfinityPickaxeCommand(this);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
            console.sendMessage(TextUtil.parse("<green>  ✔ <dark_gray>[6/6]</dark_gray> <white>Commands & Events:</white> <green>Registered (/ipickaxe, /ipickaxe book, /ipickaxe reload).</green>"));
        }

        long elapsed = System.currentTimeMillis() - start;
        console.sendMessage(TextUtil.parse("<dark_gray>  ─────────────────────────────────────────────────────────────</dark_gray>"));
        console.sendMessage(TextUtil.parse("<gradient:#00FF88:#00E5FF><b>  ✨ InfinityPickaxes enabled and ready in " + elapsed + "ms! ✨</b></gradient>"));
        console.sendMessage(TextUtil.parse(""));
    }

    /**
     * Completely safe reload method without memory leaks or duplicate tasks.
     */
    public void reloadPlugin(CommandSender sender) {
        long start = System.currentTimeMillis();

        // 1. Close open CustomGui inventories
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof CustomGui) {
                p.closeInventory();
            }
        }

        // 2. Reload configurations and locales
        this.configManager.reload();

        // 3. Reload core subsystems
        this.levelManager.loadConfig();
        this.enchantManager.loadConfig();
        if (this.duplicateListener != null) this.duplicateListener.reload();

        // 4. Refresh all pickaxes currently held by players
        if (this.heldListener != null) {
            this.heldListener.refreshAllHeldPickaxes();
        }

        // 5. Ensure PlaceholderAPI hook is registered
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI") && papiHook == null) {
            this.papiHook = new PlaceholderAPIHook(this);
            this.papiHook.register();
        }

        long elapsed = System.currentTimeMillis() - start;
        messageManager.sendMessage(sender, "messages.reload-success");
        getLogger().info("InfinityPickaxes reloaded in " + elapsed + "ms. Active language: " + configManager.getCurrentLanguage());
    }

    @Override
    public void onDisable() {
        if (duplicateListener != null) duplicateListener.stop();

        // 1. Close any open CustomGui inventories
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof CustomGui) {
                p.closeInventory();
            }
        }

        // 2. Cancel all bukkit scheduler tasks for this plugin
        Bukkit.getScheduler().cancelTasks(this);

        // 3. Unregister PlaceholderAPI
        if (papiHook != null) {
            try {
                papiHook.unregister();
            } catch (Throwable ignored) {}
            papiHook = null;
        }
        if (duplicateService != null) {
            try {
                duplicateService.close();
            } catch (Exception exception) {
                getLogger().warning("Could not close duplicate registry cleanly: " + exception.getMessage());
            }
        }

        ConsoleCommandSender console = Bukkit.getConsoleSender();
        console.sendMessage(TextUtil.parse("<dark_gray>  ─────────────────────────────────────────────────────────────</dark_gray>"));
        console.sendMessage(TextUtil.parse("<red>  ✖ </red><gradient:#FF5555:#FF0055><b>INFINITY PICKAXES</b></gradient> <dark_gray>»</dark_gray> <gray>Plugin disabled safely and all tasks terminated.</gray>"));
        console.sendMessage(TextUtil.parse("<dark_gray>  ─────────────────────────────────────────────────────────────</dark_gray>"));
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

    public LimitBreakManager getLimitBreakManager() {
        return limitBreakManager;
    }

    public PickaxeManager getPickaxeManager() {
        return pickaxeManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public PickaxeDuplicateService getDuplicateService() {
        return duplicateService;
    }
}
