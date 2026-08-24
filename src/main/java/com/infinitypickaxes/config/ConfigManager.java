package com.infinitypickaxes.config;

import com.infinitypickaxes.InfinityPickaxes;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

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

    private File configFile;
    private File enchantsFile;
    private File perksFile;
    private File blocksFile;
    private File messagesFile;
    private File mainMenuFile;
    private File enchantsMenuFile;
    private File perksMenuFile;

    private String currentLanguage = "en";

    public ConfigManager(InfinityPickaxes plugin) {
        this.plugin = plugin;
        loadAll();
    }

    public void loadAll() {
        // 1. Load and update config.yml
        this.configFile = loadAndSyncFile("config.yml");
        this.config = YamlConfiguration.loadConfiguration(configFile);
        updateMissingKeys(configFile, "config.yml", (YamlConfiguration) this.config);

        // Read active language
        this.currentLanguage = this.config.getString("language", "en").toLowerCase();

        // 2. Custom core files
        this.enchantsFile = loadAndSyncFile("enchants.yml");
        this.enchantsConfig = YamlConfiguration.loadConfiguration(enchantsFile);
        updateMissingKeys(enchantsFile, "enchants.yml", (YamlConfiguration) this.enchantsConfig);

        this.perksFile = loadAndSyncFile("perks.yml");
        this.perksConfig = YamlConfiguration.loadConfiguration(perksFile);
        updateMissingKeys(perksFile, "perks.yml", (YamlConfiguration) this.perksConfig);

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

        this.perksMenuFile = loadAndSyncFile("menus/perks_menu.yml");
        this.perksMenuConfig = YamlConfiguration.loadConfiguration(perksMenuFile);
        updateMissingKeys(perksMenuFile, "menus/perks_menu.yml", (YamlConfiguration) this.perksMenuConfig);
    }

    private void loadLocales() {
        // Ensure default locales exist on disk
        loadAndSyncFile("locales/en.yml");
        loadAndSyncFile("locales/es.yml");

        // Sync missing keys in default locales
        File enFile = new File(plugin.getDataFolder(), "locales/en.yml");
        if (enFile.exists()) {
            YamlConfiguration enYaml = YamlConfiguration.loadConfiguration(enFile);
            updateMissingKeys(enFile, "locales/en.yml", enYaml);
        }
        File esFile = new File(plugin.getDataFolder(), "locales/es.yml");
        if (esFile.exists()) {
            YamlConfiguration esYaml = YamlConfiguration.loadConfiguration(esFile);
            updateMissingKeys(esFile, "locales/es.yml", esYaml);
        }

        // Load active locale file
        String localePath = "locales/" + currentLanguage + ".yml";
        this.messagesFile = new File(plugin.getDataFolder(), localePath);

        if (!messagesFile.exists()) {
            // Fallback to en.yml if requested language does not exist
            plugin.getLogger().warning("Idioma '" + currentLanguage + "' no encontrado en locales/. Usando 'en.yml' por defecto.");
            this.messagesFile = new File(plugin.getDataFolder(), "locales/en.yml");
        }

        this.messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    private File loadAndSyncFile(String resourcePath) {
        File file = new File(plugin.getDataFolder(), resourcePath);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                plugin.saveResource(resourcePath, false);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "No se pudo guardar el recurso por defecto: " + resourcePath, e);
            }
        }
        return file;
    }

    /**
     * Recursively verifies and injects any missing default keys from embedded JAR resources into existing user configs.
     */
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

                // If default version differs or keys were added, save file
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
                    plugin.getLogger().info("Archivo '" + resourcePath + "' actualizado automáticamente con " + missingCount + " nuevas opciones por defecto.");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error al sincronizar claves de configuración para " + resourcePath, e);
        }
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

    public String getCurrentLanguage() {
        return currentLanguage;
    }
}
