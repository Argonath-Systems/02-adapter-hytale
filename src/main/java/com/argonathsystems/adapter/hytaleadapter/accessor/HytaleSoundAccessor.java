package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.adapter.hytaleadapter.converter.LocationConverter;
import com.argonathsystems.adapter.hytaleadapter.util.PlayerRefCache;
import com.argonathsystems.framework.accessorapi.SoundAccessor;
import com.argonathsystems.framework.accessorapi.dto.LocationData;
import com.hytale.api.Server;
import com.hytale.api.world.World;
import com.hytale.api.entity.Player;

import java.util.UUID;

public class HytaleSoundAccessor implements SoundAccessor {
    private final Server server;
    public HytaleSoundAccessor(Server server) { this.server = server; }
    
    @Override 
    public void playSound(String soundId, LocationData location, float volume, float pitch) {
        World world = server.getWorld(location.world());
        if (world != null) {
            world.playSound(LocationConverter.fromDTO(location), soundId, volume, pitch);
        }
    }

    @Override 
    public void playSoundTo(UUID playerId, String soundId, float volume, float pitch) {
        Player player = PlayerRefCache.get(playerId); // Or use better lookup
        if (player != null) {
             player.playSound(soundId, volume, pitch);
        }
    }

    @Override 
    public void stopSound(UUID playerId, String soundId) {
        Player player = PlayerRefCache.get(playerId);
        if (player != null) {
            player.stopSound(soundId);
        }
    }
}