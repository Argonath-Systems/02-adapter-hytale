package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.adapter.hytaleadapter.converter.LocationConverter;
import com.argonathsystems.adapter.hytaleadapter.util.PlayerRefCache;
import com.argonathsystems.framework.accessorapi.SoundAccessor;
import com.argonathsystems.framework.accessorapi.dto.LocationData;

import java.util.UUID;

/**
 * <p><b>MIGRATION-001 Status:</b> BLOCKED - Requires Official Hytale SDK</p>
 * 
 * <p>This accessor implements Sound effects functionality.</p>
 * <p>Implementation requires the official Hytale SDK (com.hypixel.hytale.*)
 * which is not available in the development environment.</p>
 * 
 * <p>All methods throw UnsupportedOperationException until the SDK is available.</p>
 * 
 * @see <a href="file://../../../docs/migration-001/PHASE-3-IMPLEMENTATION-STATUS.md">Phase 3 Status</a>
 */
public class HytaleSoundAccessor implements SoundAccessor {
    private final Object /* Server */ server;
    public HytaleSoundAccessor(Object /* Server */ server) { this.server = server; }
    
    @Override 
    public void playSound(String soundId, LocationData location, float volume, float pitch) {
        World world = server.getWorld(location.world());
        if (world != null) {
            world.playSound(LocationConverter.fromDTO(location), soundId, volume, pitch);
        }
    }

    @Override 
    public void playSoundTo(UUID playerId, String soundId, float volume, float pitch) {
        Object /* Player */ player = PlayerRefCache.get(playerId); // Or use better lookup
        if (player != null) {
             player.playSound(soundId, volume, pitch);
        }
    }

    @Override 
    public void stopSound(UUID playerId, String soundId) {
        Object /* Player */ player = PlayerRefCache.get(playerId);
        if (player != null) {
            player.stopSound(soundId);
        }
    }
}