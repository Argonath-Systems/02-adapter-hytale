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
         // Requires Hytale API specific spawn logic which is likely on World
         return Optional.empty(); // TODO: Add spawn to World interface
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