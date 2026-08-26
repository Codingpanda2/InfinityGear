package com.infinitypickaxes.config;

import com.infinitypickaxes.InfinityPickaxes;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class ConfigManager {

    private final InfinityPickaxes plugin;

    private File configFile;
    private FileConfiguration config;

    private File limitbreakFile;
    private FileConfiguration limitbreakConfig;

    private File enchantsFile;
    private FileConfiguration enchantsConfig;

    private File blocksFile;
    private FileConfiguration blocksConfig;

    private File mainMenuFile;
    private FileConfiguration mainMenuConfig;

    private File enchantsMenuFile;
    private FileConfiguration enchantsMenuConfig;

    private final Map<String, YamlConfiguration> localeConfigs = new HashMap<>();
    private String defaultLanguage = "en";

    public ConfigManager(InfinityPickaxes plugin) {
        this.plugin = plugin;
        loadAll();
    }

    public void loadAll() {
        // 1. Load and update config.yml
        this.configFile = loadAndSyncFile("config.yml");
        this.config = YamlConfiguration.loadConfiguration(configFile);
        updateMissingKeys(configFile, "config.yml", (YamlConfiguration) this.config);

        this.defaultLanguage = this.config.getString("language", "en").toLowerCase();

        // 2. Custom core files
        this.limitbreakFile = loadAndSyncFile("limitbreak.yml");
        this.limitbreakConfig = YamlConfiguration.loadConfiguration(limitbreakFile);
        updateMissingKeys(limitbreakFile, "limitbreak.yml", (YamlConfiguration) this.limitbreakConfig);

        this.enchantsFile = loadAndSyncFile("enchants.yml");
        this.enchantsConfig = YamlConfiguration.loadConfiguration(enchantsFile);
        updateMissingKeys(enchantsFile, "enchants.yml", (YamlConfiguration) this.enchantsConfig);

        this.blocksFile = loadAndSyncFile("blocks.yml");
        this.blocksConfig = YamlConfiguration.loadConfiguration(blocksFile);
        updateMissingKeys(blocksFile, "blocks.yml", (YamlConfiguration) this.blocksConfig);

        // 3. Locales
        loadLocales();

        // 4. Menus
        this.mainMenuFile = loadAndSyncFile("menus/main_menu.yml");
        this.mainMenuConfig = YamlConfiguration.loadConfiguration(mainMenuFile);
        updateMissingKeys(mainMenuFile, "menus/main_menu.yml", (YamlConfiguration) this.mainMenuConfig);

        this.enchantsMenuFile = loadAndSyncFile("menus/enchants_menu.yml");
        this.enchantsMenuConfig = YamlConfiguration.loadConfiguration(enchantsMenuFile);
        updateMissingKeys(enchantsMenuFile, "menus/enchants_menu.yml", (YamlConfiguration) this.enchantsMenuConfig);
        migrateEnchantsMenuInstructions((YamlConfiguration) this.enchantsMenuConfig);

    }

    private void loadLocales() {
        localeConfigs.clear();

        // Ensure default locales exist on disk
        loadAndSyncFile("locales/en.yml");
        loadAndSyncFile("locales/es.yml");

        File localesDir = new File(plugin.getDataFolder(), "locales");
        if (localesDir.exists() && localesDir.isDirectory()) {
            File[] files = localesDir.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files != null) {
                for (File f : files) {
                    String langKey = f.getName().replace(".yml", "").toLowerCase();
                    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
                    updateMissingKeys(f, "locales/" + f.getName(), yaml);
                    localeConfigs.put(langKey, yaml);
                }
            }
        }

        if (!localeConfigs.containsKey("en")) {
            File enFile = new File(plugin.getDataFolder(), "locales/en.yml");
            localeConfigs.put("en", YamlConfiguration.loadConfiguration(enFile));
        }
    }

    private File loadAndSyncFile(String resourcePath) {
        File file = new File(plugin.getDataFolder(), resourcePath);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                plugin.saveResource(resourcePath, false);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Could not save default resource: " + resourcePath, e);
            }
        }
        return file;
    }

    private void updateMissingKeys(File file, String resourcePath, YamlConfiguration currentConfig) {
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in == null) return;
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(reader);
                int missingCount = 0;

                for (String key : defaultConfig.getKeys(true)) {
                    if (!currentConfig.contains(key)) {
                        currentConfig.set(key, defaultConfig.get(key));
                        missingCount++;
                    }
                }

                if (defaultConfig.contains("config-version")) {
                    String defaultVer = defaultConfig.getString("config-version", "1.0.0");
                    String currentVer = currentConfig.getString("config-version", "");
                    if (!defaultVer.equalsIgnoreCase(currentVer)) {
                        currentConfig.set("config-version", defaultVer);
                        missingCount++;
                    }
                }

                if (missingCount > 0) {
                    currentConfig.save(file);
                }
            }
        } catch (Exception ignored) {}
    }

    private void migrateEnchantsMenuInstructions(YamlConfiguration menu) {
        boolean changed = false;

        List<String> socketLore = menu.getStringList("enchant-format.lore-unlocked");
        int dragLine = socketLore.indexOf("<green>▶ Drag & drop matching book or</green>");
        if (dragLine >= 0 && dragLine + 1 < socketLore.size()
                && socketLore.get(dragLine + 1).equals("<green>  click socket with book on cursor.</green>")) {
            socketLore.set(dragLine, "<green>▶ Click the book in your inventory, then</green>");
            socketLore.set(dragLine + 1, "<green>  click this socket with it on your cursor.</green>");
            socketLore.add(dragLine + 2,
                    "<dark_green>  Tip: shift-click the book to apply it directly.</dark_green>");
            menu.set("enchant-format.lore-unlocked", socketLore);
            changed = true;
        }

        List<String> infoLore = menu.getStringList("items.info-book.lore");
        int clickLine = infoLore.indexOf("<white>2.</white> <gray>Click the socket or drag the book");
        if (clickLine >= 0 && clickLine + 2 < infoLore.size()
                && infoLore.get(clickLine + 1).equals("   <gray>directly onto the desired slot.")
                && infoLore.get(clickLine + 2).equals(
                "<white>3.</white> <gray>The book will be consumed and pickaxe upgraded!")) {
            infoLore.set(clickLine, "<white>2.</white> <gray>Click the book in your inventory to pick it up.");
            infoLore.set(clickLine + 1,
                    "<white>3.</white> <gray>Click the matching socket with it on your cursor.");
            infoLore.set(clickLine + 2, "   <dark_gray>Or shift-click the book for quick apply.</dark_gray>");
            infoLore.add(clickLine + 3,
                    "<white>4.</white> <gray>The book will be consumed and pickaxe upgraded!");
            menu.set("items.info-book.lore", infoLore);
            changed = true;
        }

        if (changed) {
            try {
                menu.save(enchantsMenuFile);
            } catch (Exception exception) {
                plugin.getLogger().log(Level.WARNING,
                        "Could not migrate enchantment menu instructions", exception);
            }
        }
    }

    public void reload() {
        loadAll();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getLimitBreakConfig() {
        return limitbreakConfig;
    }

    public FileConfiguration getEnchantsConfig() {
        return enchantsConfig;
    }

    public void saveEnchantsConfig() {
        if (enchantsConfig == null || enchantsFile == null) return;
        try {
            enchantsConfig.save(enchantsFile);
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not save generated enchants.yml policy", exception);
        }
    }

    public FileConfiguration getBlocksConfig() {
        return blocksConfig;
    }

    public FileConfiguration getMessagesConfig() {
        return getLocaleConfig(defaultLanguage);
    }

    public FileConfiguration getLocaleConfig(String lang) {
        if (lang == null) lang = defaultLanguage;
        lang = lang.toLowerCase();
        if (lang.contains("_")) lang = lang.substring(0, lang.indexOf("_"));
        if (localeConfigs.containsKey(lang)) {
            return localeConfigs.get(lang);
        }
        return localeConfigs.getOrDefault(defaultLanguage, localeConfigs.getOrDefault("en", new YamlConfiguration()));
    }

    public FileConfiguration getMainMenuConfig() {
        return mainMenuConfig;
    }

    public FileConfiguration getEnchantsMenuConfig() {
        return enchantsMenuConfig;
    }

    public String getCurrentLanguage() {
        return defaultLanguage;
    }
}
