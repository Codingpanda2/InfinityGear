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

    private File perksFile;
    private FileConfiguration perksConfig;

    private File blocksFile;
    private FileConfiguration blocksConfig;

    private File mainMenuFile;
    private FileConfiguration mainMenuConfig;

    private File enchantsMenuFile;
    private FileConfiguration enchantsMenuConfig;

    private File perksMenuFile;
    private FileConfiguration perksMenuConfig;

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

        // Auto-migrate lore template if contains outdated Spanish/ownership keys
        migratePickaxeLore();

        this.defaultLanguage = this.config.getString("language", "en").toLowerCase();

        // 2. Custom core files
        this.limitbreakFile = loadAndSyncFile("limitbreak.yml");
        this.limitbreakConfig = YamlConfiguration.loadConfiguration(limitbreakFile);
        updateMissingKeys(limitbreakFile, "limitbreak.yml", (YamlConfiguration) this.limitbreakConfig);

        this.enchantsFile = loadAndSyncFile("enchants.yml");
        this.enchantsConfig = YamlConfiguration.loadConfiguration(enchantsFile);
        migrateEnchantsFile();
        updateMissingKeys(enchantsFile, "enchants.yml", (YamlConfiguration) this.enchantsConfig);

        this.perksFile = loadAndSyncFile("perks.yml");
        this.perksConfig = YamlConfiguration.loadConfiguration(perksFile);
        migratePerksFile();
        updateMissingKeys(perksFile, "perks.yml", (YamlConfiguration) this.perksConfig);

        this.blocksFile = loadAndSyncFile("blocks.yml");
        this.blocksConfig = YamlConfiguration.loadConfiguration(blocksFile);
        updateMissingKeys(blocksFile, "blocks.yml", (YamlConfiguration) this.blocksConfig);

        // 3. Locales
        loadLocales();

        // 4. Menus
        this.mainMenuFile = loadAndSyncFile("menus/main_menu.yml");
        this.mainMenuConfig = YamlConfiguration.loadConfiguration(mainMenuFile);
        migrateMenuFile(mainMenuFile, "menus/main_menu.yml", (YamlConfiguration) this.mainMenuConfig, "Sockets de Encantamientos");

        this.enchantsMenuFile = loadAndSyncFile("menus/enchants_menu.yml");
        this.enchantsMenuConfig = YamlConfiguration.loadConfiguration(enchantsMenuFile);
        migrateMenuFile(enchantsMenuFile, "menus/enchants_menu.yml", (YamlConfiguration) this.enchantsMenuConfig, "¿Cómo Mejorar Sockets?");

        this.perksMenuFile = loadAndSyncFile("menus/perks_menu.yml");
        this.perksMenuConfig = YamlConfiguration.loadConfiguration(perksMenuFile);
        migrateMenuFile(perksMenuFile, "menus/perks_menu.yml", (YamlConfiguration) this.perksMenuConfig, "Ranuras de Habilidades");
    }

    private void migratePickaxeLore() {
        List<String> currentLore = this.config.getStringList("pickaxe-lore.lore");
        boolean needsMigration = false;
        for (String line : currentLore) {
            if (line.contains("Dueño") || line.contains("%player%") || line.contains("ENCANTAMIENTOS")
                    || line.contains("PERKS ACTIVOS") || line.contains("Progreso XP:") || line.contains("Nivel del Pico")) {
                needsMigration = true;
                break;
            }
        }
        if (needsMigration || currentLore.isEmpty()) {
            List<String> newLore = Arrays.asList(
                    "%enchants_list%",
                    "<dark_gray>---------------------</dark_gray>",
                    "<gray>Level: <gold><b>%level%</b></gold></gray>",
                    "<gray>Progress: %xp_bar%</gray>",
                    "<gray>Mined Blocks: <yellow>%blocks_mined%</yellow></gray>",
                    "<gray>Perks: <gold>%perks_count%</gold><gray>/</gray><gold>%max_perks%</gold></gray>"
            );
            this.config.set("pickaxe-lore.lore", newLore);
            try {
                ((YamlConfiguration) this.config).save(configFile);
                plugin.getLogger().info("Pickaxe lore template migrated automatically to new clean English layout.");
            } catch (Exception ignored) {}
        }
    }

    private void migrateEnchantsFile() {
        String effName = this.enchantsConfig.getString("enchants.efficiency.display-name", "");
        if (effName.contains("Eficiencia") || effName.contains("Fortuna")) {
            overwriteFromResource(enchantsFile, "enchants.yml");
            this.enchantsConfig = YamlConfiguration.loadConfiguration(enchantsFile);
            plugin.getLogger().info("enchants.yml migrated to standard English.");
        }
    }

    private void migratePerksFile() {
        String pName = this.perksConfig.getString("perks.haste_surge.display-name", "");
        if (pName.contains("Furia de Prisa") || pName.contains("Fundición")) {
            overwriteFromResource(perksFile, "perks.yml");
            this.perksConfig = YamlConfiguration.loadConfiguration(perksFile);
            plugin.getLogger().info("perks.yml migrated to standard English.");
        }
    }

    private void migrateMenuFile(File file, String resourcePath, YamlConfiguration menuYaml, String spanishKeyword) {
        String raw = menuYaml.saveToString();
        if (raw.contains(spanishKeyword)) {
            overwriteFromResource(file, resourcePath);
            plugin.getLogger().info(resourcePath + " migrated to standard English.");
        }
    }

    private void overwriteFromResource(File targetFile, String resourcePath) {
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in == null) return;
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                YamlConfiguration def = YamlConfiguration.loadConfiguration(reader);
                def.save(targetFile);
            }
        } catch (Exception ignored) {}
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

    public FileConfiguration getPerksConfig() {
        return perksConfig;
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

    public FileConfiguration getPerksMenuConfig() {
        return perksMenuConfig;
    }

    public String getCurrentLanguage() {
        return defaultLanguage;
    }
}
