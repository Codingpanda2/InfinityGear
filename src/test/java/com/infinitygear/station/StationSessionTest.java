package com.infinitygear.station;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class StationSessionTest {
    @Test void commandBypassSessionAcceptsConfiguredPerStationPermission() {
        StationManager manager = mock(StationManager.class);
        Player player = mock(Player.class);
        when(manager.hasBypass(StationType.FUSION_ALTAR, player)).thenReturn(true);

        assertTrue(new StationSession(StationType.FUSION_ALTAR, null, null, true)
                .revalidate(manager, player));
    }

    @Test void commandBypassSessionRetainsLegacyAdministratorPermission() {
        StationManager manager = mock(StationManager.class);
        Player player = mock(Player.class);
        when(player.hasPermission("infinitypickaxes.admin")).thenReturn(true);

        assertTrue(new StationSession(StationType.GEAR_FORGE, null, null, true)
                .revalidate(manager, player));
    }
}
