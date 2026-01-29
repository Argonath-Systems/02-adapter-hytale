package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.HologramAccessor;
import com.argonathsystems.framework.accessorapi.dto.LocationData;

import java.util.UUID;

/**
 * <p><b>MIGRATION-001 Status:</b> BLOCKED - Requires Official Hytale SDK</p>
 * 
 * <p>This accessor implements Hologram display functionality.</p>
 * <p>Implementation requires the official Hytale SDK (com.hypixel.hytale.*)
 * which is not available in the development environment.</p>
 * 
 * <p>All methods throw UnsupportedOperationException until the SDK is available.</p>
 * 
 * @see <a href="file://../../../docs/migration-001/PHASE-3-IMPLEMENTATION-STATUS.md">Phase 3 Status</a>
 */
public class HytaleHologramAccessor implements HologramAccessor {
    private final Object /* Server */ server;
    
    public HytaleHologramAccessor(Object /* Server */ server) { 
        this.server = server; 
    }
    
    @Override 
    public UUID createHologram(LocationData location, String... lines) {
        throw new UnsupportedOperationException(
            "HytaleHologramAccessor.createHologram() requires official Hytale SDK: " +
            "HytaleServer.get().getWorld(worldName).spawnHologram(location, lines)"
        );
    }

    @Override 
    public void updateHologram(UUID hologramId, String... lines) {
        throw new UnsupportedOperationException(
            "HytaleHologramAccessor.updateHologram() requires official Hytale SDK: " +
            "Hologram entity lookup and setLines()"
        );
    }

    @Override 
    public void moveHologram(UUID hologramId, LocationData newLocation) {
        throw new UnsupportedOperationException(
            "HytaleHologramAccessor.moveHologram() requires official Hytale SDK: " +
            "Entity.teleport(location)"
        );
    }

    @Override 
    public void removeHologram(UUID hologramId) {
        throw new UnsupportedOperationException(
            "HytaleHologramAccessor.removeHologram() requires official Hytale SDK: " +
            "Entity.remove()"
        );
    }
}