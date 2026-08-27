package com.infinitygear.station;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public record StationSession(StationType type, Location location, String nexoId, boolean administratorBypass) {
    public boolean revalidate(StationManager manager, Player player) {
        if (administratorBypass) return player.hasPermission("infinitygear.admin.station")
                || player.hasPermission("infinitygear.admin");
        if (location == null || location.getWorld() == null) return false;
        if (nexoId != null && !nexoId.isBlank()) {
            return manager.identifyNexo(player, nexoId, location).filter(type::equals).isPresent();
        }
        return manager.authorized(type, player, location.getBlock());
    }
}
