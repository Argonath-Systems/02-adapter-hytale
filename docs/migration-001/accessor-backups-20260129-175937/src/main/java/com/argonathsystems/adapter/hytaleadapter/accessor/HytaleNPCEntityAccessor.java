package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.adapter.hytaleadapter.converter.LocationConverter;
import com.argonathsystems.framework.accessorapi.EntityAccessor;
import com.argonathsystems.framework.accessorapi.data.DataValue;
import com.argonathsystems.framework.accessorapi.dto.EntityData;
import com.argonathsystems.framework.accessorapi.dto.LocationData;
import com.hytale.api.Location;
import com.hytale.api.Server;
import com.hytale.api.entity.Entity;
import com.hytale.api.world.World;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Hytale implementation of EntityAccessor.
 * 
 * <p>Provides entity management using the Hytale API.
 * 
 * <p><strong>API Limitations:</strong>
 * <ul>
 *   <li>Entity spawning is not supported (only Holograms can be spawned via World.spawnHologram())</li>
 *   <li>Navigation/pathfinding is not exposed in the basic Hytale API</li>
 *   <li>Metadata storage must use an external storage mechanism</li>
 * </ul>
 * 
 * @author Argonath Systems
 * @since 1.1.0
 * @version 2.0.0 - Updated to use DataValue for metadata
 */
public class HytaleNPCEntityAccessor implements EntityAccessor {
    
    private final Server server;
    private final Map<UUID, Entity> entityCache = new ConcurrentHashMap<>();
    private final Map<String, Map<String, DataValue>> entityMetadata = new ConcurrentHashMap<>();
    
    public HytaleNPCEntityAccessor(Server server) {
        this.server = server;
    }
    
    @Override
    public Optional<EntityData> getEntity(UUID entityId) {
        Entity entity = findEntity(entityId);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(toEntityData(entity));
    }
    
    @Override
    public Collection<EntityData> getEntities(String worldName) {
        World world = server.getWorld(worldName);
        if (world == null) {
            return Collections.emptyList();
        }
        
        return world.getEntities().stream()
                .map(this::toEntityData)
                .collect(Collectors.toList());
    }
    
    @Override
    public Collection<EntityData> getEntitiesNear(LocationData location, double radius) {
        World world = server.getWorld(location.world());
        if (world == null) {
            return Collections.emptyList();
        }
        
        double radiusSquared = radius * radius;
        return world.getEntities().stream()
                .filter(entity -> {
                    Location entityLoc = entity.getLocation();
                    double dx = entityLoc.getX() - location.x();
                    double dy = entityLoc.getY() - location.y();
                    double dz = entityLoc.getZ() - location.z();
                    return (dx * dx + dy * dy + dz * dz) <= radiusSquared;
                })
                .map(this::toEntityData)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<EntityData> spawnEntity(String type, LocationData location) {
        // NOTE: Current Hytale API does not support spawning arbitrary entities.
        // Only Holograms can be spawned via World.spawnHologram().
        throw new UnsupportedOperationException(
            "Entity spawning not yet supported by Hytale API. " +
            "Only Holograms can be spawned via World.spawnHologram(). " +
            "Custom entity spawning requires future API extensions or command-based workarounds."
        );
    }
    
    @Override
    public void removeEntity(UUID entityId) {
        Entity entity = findEntity(entityId);
        if (entity != null) {
            entity.remove();
            entityCache.remove(entityId);
            entityMetadata.remove(entityId.toString());
        }
    }
    
    @Override
    public void damage(UUID entityId, int amount) {
        Entity entity = findEntity(entityId);
        if (entity == null) return;
        
        double newHealth = Math.max(0, entity.getHealth() - amount);
        entity.setHealth(newHealth);
    }
    
    @Override
    public void heal(UUID entityId, int amount) {
        Entity entity = findEntity(entityId);
        if (entity == null) return;
        
        double newHealth = Math.min(entity.getMaxHealth(), entity.getHealth() + amount);
        entity.setHealth(newHealth);
    }
    
    @Override
    public void navigateTo(UUID entityId, LocationData target) {
        // Navigation/pathfinding requires API support that doesn't exist in current Hytale API.
        // Workaround: Use teleport() for instant movement, or implement custom pathfinding.
        throw new UnsupportedOperationException(
            "NPC navigation not yet supported by Hytale API. " +
            "Requires pathfinding or AI control extensions. " +
            "Workaround: Use teleport() for instant movement."
        );
    }
    
    @Override
    public void teleport(UUID entityId, LocationData target) {
        Entity entity = findEntity(entityId);
        if (entity == null) return;
        
        Location hytaleLocation = LocationConverter.fromDTO(target);
        entity.teleport(hytaleLocation);
    }
    
    @Override
    public void setMetadata(UUID entityId, String key, DataValue value) {
        entityMetadata
                .computeIfAbsent(entityId.toString(), k -> new ConcurrentHashMap<>())
                .put(key, value);
    }
    
    @Override
    public Optional<DataValue> getMetadata(UUID entityId, String key) {
        Map<String, DataValue> metadata = entityMetadata.get(entityId.toString());
        if (metadata == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(metadata.get(key));
    }
    
    // === Helper Methods ===
    
    /**
     * Convert a Hytale Entity to EntityData DTO.
     */
    private EntityData toEntityData(Entity entity) {
        return new EntityData(
                entity.getUniqueId(),
                entity.getType(),
                entity.getName(),
                LocationConverter.toDTO(entity.getLocation()),
                (int) entity.getHealth(),
                (int) entity.getMaxHealth()
        );
    }
    
    /**
     * Find an entity by UUID, using cache first, then searching all worlds.
     */
    private Entity findEntity(UUID entityId) {
        // Check cache first
        Entity cached = entityCache.get(entityId);
        if (cached != null) {
            return cached;
        }
        
        // Search all worlds for the entity
        for (World world : server.getWorlds()) {
            Entity entity = world.getEntity(entityId);
            if (entity != null) {
                entityCache.put(entityId, entity);
                return entity;
            }
        }
        
        return null;
    }
    
    /**
     * Clears the entity cache. Call periodically to prevent memory leaks from despawned entities.
     */
    public void clearCache() {
        // Remove invalid entities from cache
        entityCache.entrySet().removeIf(entry -> {
            try {
                Entity entity = entry.getValue();
                // Try accessing the entity - if it's despawned, this might fail or return invalid data
                entity.getUniqueId();
                return false; // Keep valid entities
            } catch (Exception e) {
                return true; // Remove invalid entities
            }
        });
    }
}
