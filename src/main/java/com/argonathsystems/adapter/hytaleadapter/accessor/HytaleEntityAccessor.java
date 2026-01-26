package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.EntityAccessor;
import com.argonathsystems.framework.accessorapi.dto.EntityData;
import com.argonathsystems.framework.accessorapi.dto.LocationData;
import com.argonathsystems.adapter.hytaleadapter.converter.EntityDataConverter;
import com.argonathsystems.adapter.hytaleadapter.converter.LocationConverter;
import com.hytale.api.Server;
import com.hytale.api.world.World;
import com.hytale.api.entity.Entity;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class HytaleEntityAccessor implements EntityAccessor {
    private final Server server;

    public HytaleEntityAccessor(Server server) {
        this.server = server;
    }

    @Override
    public Optional<EntityData> getEntity(UUID entityId) {
        // Search in all worlds as we don't know the world
        // Optimization: Use a cache or Hytale API global lookup if available
        for (World world : server.getWorlds()) {
            Entity entity = world.getEntity(entityId);
            if (entity != null) {
                return Optional.ofNullable(EntityDataConverter.toDTO(entity));
            }
        }
        return Optional.empty();
    }

    @Override
    public Collection<EntityData> getEntities(String worldName) {
        World world = server.getWorld(worldName);
        if (world == null) return Collections.emptyList();
        
        return world.getEntities().stream()
                .map(EntityDataConverter::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<EntityData> getEntitiesNear(LocationData location, double radius) {
        World world = server.getWorld(location.world());
        if (world == null) return Collections.emptyList();
        
        // This is naive implementation. Should use spatial lookup
        return world.getEntities().stream()
                .filter(e -> {
                    // Quick check
                    if (e.getLocation() == null) return false;
                    double distSq = Math.pow(e.getLocation().getX() - location.x(), 2) +
                                    Math.pow(e.getLocation().getY() - location.y(), 2) +
                                    Math.pow(e.getLocation().getZ() - location.z(), 2);
                    return distSq <= radius * radius;
                })
                .map(EntityDataConverter::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<EntityData> spawnEntity(String type, LocationData location) {
        // TODO: Hytale API doesn't have spawnEntity(type, x, y, z) yet
        // Once available, implement entity spawning
        throw new UnsupportedOperationException("Entity spawning not yet available in Hytale API");
        
        /* // Find the world for this location
        World world = null;
        if (location.worldId() != null) {
            world = server.getWorld(UUID.fromString(location.worldId()));
        }
        
        // Fallback to first world if none specified
        if (world == null && !server.getWorlds().isEmpty()) {
            world = server.getWorlds().iterator().next();
        }
        
        if (world == null) {
            System.err.println("No world available for entity spawn");
            return Optional.empty();
        }
        
        try {
            // Spawn entity using World API
            // Note: This assumes Hytale API provides world.spawnEntity(type, x, y, z)
            // The exact method may vary based on final Hytale SDK
            Entity entity = world.spawnEntity(type, location.x(), location.y(), location.z());
            
            if (entity != null) {
                return Optional.of(EntityDataConverter.toDTO(entity));
            }
        } catch (Exception e) {
            System.err.println("Failed to spawn entity '" + type + "': " + e.getMessage());
        }
        
        return Optional.empty(); */
    }

    @Override
    public void removeEntity(UUID entityId) {
        for (World world : server.getWorlds()) {
            Entity entity = world.getEntity(entityId);
            if (entity != null) {
                entity.remove();
                return;
            }
        }
    }

    @Override
    public void damage(UUID entityId, int amount) {
         for (World world : server.getWorlds()) {
            Entity entity = world.getEntity(entityId);
            if (entity != null) {
                entity.setHealth(Math.max(0, entity.getHealth() - amount));
                return;
            }
        }
    }

    @Override
    public void heal(UUID entityId, int amount) {
         for (World world : server.getWorlds()) {
            Entity entity = world.getEntity(entityId);
            if (entity != null) {
                 entity.setHealth(Math.min(entity.getMaxHealth(), entity.getHealth() + amount));
                return;
            }
        }
    }
}