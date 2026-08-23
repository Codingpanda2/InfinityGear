package com.infinitypickaxes.config;

import com.infinitypickaxes.InfinityPickaxes;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ConfigManager {

    private final InfinityPickaxes plugin;

    private FileConfiguration config;
    private FileConfiguration enchantsConfig;
    private FileConfiguration perksConfig;
    private FileConfiguration blocksConfig;
    private FileConfiguration messagesConfig;
    private FileConfiguration mainMenuConfig;
    private FileConfiguration enchantsMenuConfig;
    private FileConfiguration perksMenuConfig;

    private File enchantsFile;
    private File perksFile;
    private File blocksFile;
    private File messagesFile;
    private File mainMenuFile;
    private File enchantsMenuFile;
    private File perksMenuFile;

    public ConfigManager(InfinityPickaxes plugin) {
        this.plugin = plugin;
        loadAll();
    }

    public void loadAll() {
        // 1. config.yml
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        // 2. Custom files
        this.enchantsFile = loadResourceFile("enchants.yml");
        this.enchantsConfig = YamlConfiguration.loadConfiguration(enchantsFile);

        this.perksFile = loadResourceFile("perks.yml");
        this.perksConfig = YamlConfiguration.loadConfiguration(perksFile);

        this.blocksFile = loadResourceFile("blocks.yml");
        this.blocksConfig = YamlConfiguration.loadConfiguration(blocksFile);

        this.messagesFile = loadResourceFile("messages.yml");
        this.messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // 3. Menus
        this.mainMenuFile = loadResourceFile("menus/main_menu.yml");
        this.mainMenuConfig = YamlConfiguration.loadConfiguration(mainMenuFile);

        this.enchantsMenuFile = loadResourceFile("menus/enchants_menu.yml");
        this.enchantsMenuConfig = YamlConfiguration.loadConfiguration(enchantsMenuFile);

        this.perksMenuFile = loadResourceFile("menus/perks_menu.yml");
        this.perksMenuConfig = YamlConfiguration.loadConfiguration(perksMenuFile);
    }

    private File loadResourceFile(String path) {
        File file = new File(plugin.getDataFolder(), path);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            plugin.saveResource(path, false);
        }
        return file;
    }

    public void reload() {
        loadAll();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getEnchantsConfig() {
        return enchantsConfig;
    }

    public FileConfiguration getPerksConfig() {
        return perksConfig;
    }

    public FileConfiguration getBlocksConfig() {
        return blocksConfig;
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    public FileConfiguration getMainMenuConfig() {
        return mainMenuConfig;
    }

    public FileConfiguration getEnchantsMenuConfig() {
        return enchantsMenuConfig;
    }

    public FileConfiguration getPerksMenuConfig() {
        return perksMenuConfig;
    }
}
