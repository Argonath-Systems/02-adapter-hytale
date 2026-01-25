package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.adapter.hytaleadapter.converter.LocationConverter;
import com.argonathsystems.framework.accessorapi.HologramAccessor;
import com.argonathsystems.framework.accessorapi.dto.LocationData;
import com.hytale.api.Server;
import com.hytale.api.world.World;
import com.hytale.api.entity.Entity;
import com.hytale.api.entity.Hologram;

import java.util.UUID;

public class HytaleHologramAccessor implements HologramAccessor {
    private final Server server;
    public HytaleHologramAccessor(Server server) { this.server = server; }
    
    @Override 
    public UUID createHologram(LocationData location, String... lines) {
        World world = server.getWorld(location.world());
        if (world != null) {
            Hologram hologram = world.spawnHologram(LocationConverter.fromDTO(location), lines);
            if (hologram != null) {
                return hologram.getUniqueId();
            }
        }
        return null;
    }

    @Override 
    public void updateHologram(UUID hologramId, String... lines) {
        // Since we don't know the world, we might have to search worlds or rely on global lookup if supported.
        // Assuming Server has global entity lookup or we iterate worlds. 
        // For simplicity/stub, let's assume we can find it via first world or we need to look it up.
        // The SDK stub for Server doesn't have searching all worlds yet.
        // BUT, usually accessor implementations are better if they know the context.
        // Let's assume server.getWorlds() exists or we just fail gracefully.
        
        // BETTER: Implementation should probably store world in a map if server doesn't support global lookup.
        // Or iterate.
        // Let's iterate if server allows.
        
        Entity entity = findEntity(hologramId);
        if (entity instanceof Hologram) {
            ((Hologram) entity).setLines(lines);
        }
    }

    @Override 
    public void moveHologram(UUID hologramId, LocationData newLocation) {
        Entity entity = findEntity(hologramId);
        if (entity != null) {
             entity.teleport(LocationConverter.fromDTO(newLocation));
        }
    }

    @Override 
    public void removeHologram(UUID hologramId) {
        Entity entity = findEntity(hologramId);
        if (entity != null) {
            entity.remove();
        }
    }
    
    private Entity findEntity(UUID uuid) {
        // Naive iteration if server supports getting worlds, otherwise we are stuck.
        // The SDK Server interface:
        // collection<World> getWorlds();
        // Let's check if Server has getWorlds()
        for (World world : server.getWorlds()) {
            Entity e = world.getEntity(uuid);
            if (e != null) return e;
        }
        return null;
    }
}