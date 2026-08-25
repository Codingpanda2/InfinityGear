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

        this.enchantsMenuFile = loadAndSyncFile("menus/enchants_menu.yml");
        this.enchantsMenuConfig = YamlConfiguration.loadConfiguration(enchantsMenuFile);

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
