package com.infinitygear.station;

import com.infinitypickaxes.InfinityPickaxes;
import com.infinitypickaxes.config.ConfigManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StationManagerTest {
    @Test void vanillaStationRequiresMatchingPhysicalBlockInRange() {
        StationManager manager = manager(config("VANILLA", "ENCHANTING_TABLE", null));
        World world = mock(World.class);
        Player player = mock(Player.class);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(new Location(world, 0, 64, 0));
        Block block = mock(Block.class);
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(new Location(world, 2, 64, 0));
        when(block.getType()).thenReturn(Material.ENCHANTING_TABLE);

        assertTrue(manager.authorized(StationType.RUNIC_TABLE, player, block));
        when(block.getType()).thenReturn(Material.CRAFTING_TABLE);
        assertFalse(manager.authorized(StationType.RUNIC_TABLE, player, block));
        when(block.getType()).thenReturn(Material.ENCHANTING_TABLE);
        when(block.getLocation()).thenReturn(new Location(world, 20, 64, 0));
        assertFalse(manager.authorized(StationType.RUNIC_TABLE, player, block));
    }

    @Test void unavailableNexoProviderDisablesStationInsteadOfGrantingAccess() {
        StationManager manager = manager(config("NEXO", null, "pack:runic_table"));
        assertNotNull(manager.definition(StationType.RUNIC_TABLE));
        assertFalse(manager.definition(StationType.RUNIC_TABLE).enabled());
    }

    private StationManager manager(YamlConfiguration yaml) {
        InfinityPickaxes plugin = mock(InfinityPickaxes.class);
        ConfigManager configs = mock(ConfigManager.class);
        Server server = mock(Server.class);
        PluginManager pluginManager = mock(PluginManager.class);
        when(plugin.getConfigManager()).thenReturn(configs);
        when(configs.getStationsConfig()).thenReturn(yaml);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(pluginManager.isPluginEnabled("Nexo")).thenReturn(false);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("StationManagerTest"));
        return new StationManager(plugin);
    }

    private YamlConfiguration config(String provider, String material, String nexoId) {
        YamlConfiguration yaml = new YamlConfiguration();
        String root = "stations.runic-table.";
        yaml.set(root + "enabled", true);
        yaml.set(root + "provider", provider);
        if (material != null) yaml.set(root + "material", material);
        if (nexoId != null) yaml.set(root + "nexo-id", nexoId);
        yaml.set(root + "interaction-distance", 6.0);
        yaml.set(root + "bypass-permission", "infinitygear.station.runic-table.bypass");
        return yaml;
    }
}
