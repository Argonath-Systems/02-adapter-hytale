package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.adapter.hytaleadapter.converter.LocationConverter;
import com.argonathsystems.adapter.hytaleadapter.converter.PlayerConverter;
import com.argonathsystems.adapter.hytaleadapter.util.PlayerRefCache;
import com.argonathsystems.framework.accessorapi.PlayerAccessor;
import com.argonathsystems.framework.accessorapi.dto.LocationData;
import com.argonathsystems.framework.accessorapi.dto.PlayerData;
import com.hytale.api.Server;
import com.hytale.api.entity.Player;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class HytalePlayerAccessor implements PlayerAccessor {
    private final Server server;

    public HytalePlayerAccessor(Server server) {
        this.server = server;
    }

    private Player getPlayerRef(UUID playerId) {
        Player player = PlayerRefCache.get(playerId);
        if (player != null) return player;
        
        // Fallback or full scan if cache missed (though cache should be kept up to date by events)
        for (com.hytale.api.world.World world : server.getWorlds()) {
            for (Player p : world.getPlayers()) {
                if (p.getUniqueId().equals(playerId)) {
                    PlayerRefCache.add(p);
                    return p;
                }
            }
        }
        return null;
    }

    @Override
    public Optional<PlayerData> getPlayer(UUID playerId) {
        Player player = getPlayerRef(playerId);
        return Optional.ofNullable(PlayerConverter.toDTO(player));
    }

    @Override
    public Collection<PlayerData> getOnlinePlayers() {
        // Collect from all worlds
        return server.getWorlds().stream()
            .flatMap(w -> w.getPlayers().stream())
            .map(PlayerConverter::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public void teleport(UUID playerId, LocationData location) {
         Player player = getPlayerRef(playerId);
         if (player != null) {
             player.teleport(LocationConverter.fromDTO(location));
         }
    }

    @Override
    public void sendMessage(UUID playerId, String message) {
        Player player = getPlayerRef(playerId);
        if (player != null) {
            player.sendMessage(message);
        }
    }

    @Override
    public Optional<LocationData> getLocation(UUID playerId) {
        Player player = getPlayerRef(playerId);
        if (player != null) {
            return Optional.ofNullable(LocationConverter.toDTO(player.getLocation()));
        }
        return Optional.empty();
    }

    @Override
    public void setHealth(UUID playerId, int health) {
        Player player = getPlayerRef(playerId);
        if (player != null) {
            player.setHealth(health);
        }
    }
    
    @Override
    public UUID getPlayerId(Object platformObject) {
         if (platformObject instanceof Player) {
             return ((Player) platformObject).getUniqueId();
         }
         return null;
    }
}
