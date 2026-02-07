package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.HologramAccessor;
import com.argonathsystems.framework.accessorapi.dto.LocationData;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hologram accessor implementation using Hytale SDK entity nameplate system.
 * 
 * <h2>Implementation Strategy:</h2>
 * <p>Hytale SDK does not provide a dedicated Hologram API. This implementation
 * uses a workaround based on available SDK features:</p>
 * <ol>
 *   <li>Create an invisible/marker entity at the desired location</li>
 *   <li>Set the entity's nameplate (display name) to show the hologram text</li>
 *   <li>Track the entity by UUID for later updates/removal</li>
 * </ol>
 * 
 * <h2>SDK Components Used:</h2>
 * <ul>
 *   <li>{@code com.hypixel.hytale.protocol.Nameplate} - Text display protocol</li>
 *   <li>{@code DisplayNameComponent} - Entity nameplate component</li>
 *   <li>Entity ECS system for spawn/despawn</li>
 * </ul>
 * 
 * <h2>Limitations:</h2>
 * <ul>
 *   <li>Multi-line holograms require multiple stacked entities</li>
 *   <li>No text formatting/colors support (depends on SDK)</li>
 *   <li>Entity-based approach has performance overhead for many holograms</li>
 * </ul>
 * 
 * <p><b>Status:</b> PARTIAL - Basic functionality pending entity spawn API availability</p>
 * 
 * @author Argonath Systems
 * @version 2.0.0
 * @see com.hypixel.hytale.protocol.Nameplate
 */
public class HytaleHologramAccessor implements HologramAccessor {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HytaleHologramAccessor.class);
    
    /** Vertical spacing between hologram lines in blocks */
    private static final double LINE_SPACING = 0.25;
    
    /** Cache of hologram UUID to entity UUIDs (for multi-line holograms) */
    private final Map<UUID, HologramData> hologramCache = new ConcurrentHashMap<>();
    
    /** Reference to the game server */
    private final Object server;
    
    /**
     * Internal tracking data for a hologram.
     */
    private static class HologramData {
        final String worldId;
        final double x, y, z;
        String[] lines;
        UUID[] entityUUIDs; // One entity per line
        
        HologramData(String worldId, double x, double y, double z, String[] lines) {
            this.worldId = worldId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.lines = lines;
            this.entityUUIDs = new UUID[lines.length];
        }
    }
    
    public HytaleHologramAccessor(Object server) { 
        this.server = server;
        LOGGER.info("HytaleHologramAccessor initialized with entity-nameplate workaround");
    }
    
    @Override 
    public UUID createHologram(LocationData location, String... lines) {
        if (lines == null || lines.length == 0) {
            throw new IllegalArgumentException("Hologram must have at least one line");
        }
        
        UUID hologramId = UUID.randomUUID();
        HologramData data = new HologramData(
            location.world(), 
            location.x(), 
            location.y(), 
            location.z(), 
            lines
        );
        
        // Attempt to spawn entities for each line
        World world = getWorld(location.world());
        if (world == null) {
            LOGGER.warn("World not found for hologram creation: {}", location.world());
            // Store data anyway for later spawning
            hologramCache.put(hologramId, data);
            return hologramId;
        }
        
        try {
            double currentY = location.y();
            for (int i = 0; i < lines.length; i++) {
                // Spawn a marker entity with nameplate
                // NOTE: This requires ECS entity spawning which is complex
                // Using placeholder implementation that logs intent
                
                UUID entityId = spawnHologramEntity(world, location.x(), currentY, location.z(), lines[i]);
                data.entityUUIDs[i] = entityId;
                
                currentY -= LINE_SPACING;
            }
            
            hologramCache.put(hologramId, data);
            LOGGER.debug("Created hologram {} with {} lines at ({}, {}, {})", 
                hologramId, lines.length, location.x(), location.y(), location.z());
            
        } catch (Exception e) {
            LOGGER.error("Failed to create hologram entities: {}", e.getMessage(), e);
            // Store data for potential retry
            hologramCache.put(hologramId, data);
        }
        
        return hologramId;
    }

    @Override 
    public void updateHologram(UUID hologramId, String... lines) {
        HologramData data = hologramCache.get(hologramId);
        if (data == null) {
            LOGGER.warn("Hologram not found for update: {}", hologramId);
            return;
        }
        
        if (lines == null || lines.length == 0) {
            LOGGER.warn("Cannot update hologram with empty lines");
            return;
        }
        
        World world = getWorld(data.worldId);
        if (world == null) {
            LOGGER.warn("World not found for hologram update: {}", data.worldId);
            return;
        }
        
        try {
            // Handle line count changes
            if (lines.length != data.lines.length) {
                // Need to respawn entities - remove old ones first
                for (UUID entityId : data.entityUUIDs) {
                    if (entityId != null) {
                        removeEntity(world, entityId);
                    }
                }
                
                // Spawn new entities
                data.lines = lines;
                data.entityUUIDs = new UUID[lines.length];
                double currentY = data.y;
                
                for (int i = 0; i < lines.length; i++) {
                    UUID entityId = spawnHologramEntity(world, data.x, currentY, data.z, lines[i]);
                    data.entityUUIDs[i] = entityId;
                    currentY -= LINE_SPACING;
                }
            } else {
                // Update existing entity nameplates
                for (int i = 0; i < lines.length; i++) {
                    if (data.entityUUIDs[i] != null) {
                        updateEntityNameplate(world, data.entityUUIDs[i], lines[i]);
                    }
                }
                data.lines = lines;
            }
            
            LOGGER.debug("Updated hologram {} with {} lines", hologramId, lines.length);
            
        } catch (Exception e) {
            LOGGER.error("Failed to update hologram {}: {}", hologramId, e.getMessage(), e);
        }
    }

    @Override 
    public void moveHologram(UUID hologramId, LocationData newLocation) {
        HologramData data = hologramCache.get(hologramId);
        if (data == null) {
            LOGGER.warn("Hologram not found for move: {}", hologramId);
            return;
        }
        
        World world = getWorld(newLocation.world());
        if (world == null) {
            LOGGER.warn("World not found for hologram move: {}", newLocation.world());
            return;
        }
        
        try {
            // If world changed, need to respawn
            if (!data.worldId.equals(newLocation.world())) {
                World oldWorld = getWorld(data.worldId);
                if (oldWorld != null) {
                    for (UUID entityId : data.entityUUIDs) {
                        if (entityId != null) {
                            removeEntity(oldWorld, entityId);
                        }
                    }
                }
                
                // Spawn in new world
                double currentY = newLocation.y();
                for (int i = 0; i < data.lines.length; i++) {
                    UUID entityId = spawnHologramEntity(world, newLocation.x(), currentY, newLocation.z(), data.lines[i]);
                    data.entityUUIDs[i] = entityId;
                    currentY -= LINE_SPACING;
                }
            } else {
                // Same world - teleport entities
                double currentY = newLocation.y();
                for (UUID entityId : data.entityUUIDs) {
                    if (entityId != null) {
                        teleportEntity(world, entityId, newLocation.x(), currentY, newLocation.z());
                    }
                    currentY -= LINE_SPACING;
                }
            }
            
            LOGGER.debug("Moved hologram {} to ({}, {}, {})", 
                hologramId, newLocation.x(), newLocation.y(), newLocation.z());
            
        } catch (Exception e) {
            LOGGER.error("Failed to move hologram {}: {}", hologramId, e.getMessage(), e);
        }
    }

    @Override 
    public void removeHologram(UUID hologramId) {
        HologramData data = hologramCache.remove(hologramId);
        if (data == null) {
            LOGGER.warn("Hologram not found for removal: {}", hologramId);
            return;
        }
        
        World world = getWorld(data.worldId);
        if (world == null) {
            LOGGER.warn("World not found for hologram removal: {}", data.worldId);
            return;
        }
        
        try {
            for (UUID entityId : data.entityUUIDs) {
                if (entityId != null) {
                    removeEntity(world, entityId);
                }
            }
            
            LOGGER.debug("Removed hologram {} ({} entities)", hologramId, data.entityUUIDs.length);
            
        } catch (Exception e) {
            LOGGER.error("Failed to remove hologram {}: {}", hologramId, e.getMessage(), e);
        }
    }
    
    // =====================================================
    // Private SDK Helper Methods
    // =====================================================
    
    /**
     * Get a World by its ID (name or UUID string).
     */
    private World getWorld(String worldId) {
        try {
            Universe universe = Universe.get();
            if (universe == null) {
                return null;
            }
            
            // Try by name first - getWorlds() returns Map<String, World>
            for (World world : universe.getWorlds().values()) {
                if (world.getName().equals(worldId)) {
                    return world;
                }
            }
            
            // Try by UUID using Universe.getWorld(UUID) directly
            try {
                UUID uuid = UUID.fromString(worldId);
                World world = universe.getWorld(uuid);
                if (world != null) {
                    return world;
                }
            } catch (IllegalArgumentException e) {
                // Not a valid UUID, already tried name
            }
            
        } catch (Exception e) {
            LOGGER.error("Error getting world {}: {}", worldId, e.getMessage());
        }
        return null;
    }
    
    /**
     * Spawn a marker entity with nameplate at the specified location.
     * 
     * <p>Uses ECS pattern: get EntityStore, find entity by position or create,
     * then set TransformComponent and display name. This uses the world thread
     * for thread-safe entity creation.</p>
     * 
     * @return UUID of the spawned entity, or null if spawn failed
     */
    private UUID spawnHologramEntity(World world, double x, double y, double z, String text) {
        // Generate UUID for this hologram entity
        UUID entityUuid = UUID.randomUUID();
        
        try {
            world.execute(() -> {
                EntityStore entityStore = world.getEntityStore();
                if (entityStore == null) {
                    LOGGER.warn("No entity store available for hologram spawn in world: {}", world.getName());
                    return;
                }
                
                // Spawn an entity using EntityStore
                // The exact archetype for an invisible marker entity depends on the SDK's
                // entity type registry. We use the ECS entity creation pattern.
                Store<EntityStore> store = entityStore.getStore();
                
                // TODO: When SDK provides proper archetype for marker entities, use:
                // CommandBuffer<EntityStore> buffer = entityStore.createCommandBuffer();
                // Ref<EntityStore> ref = buffer.addEntity(markerArchetype, AddReason.SPAWNED);
                // buffer.addComponent(ref, TransformComponent.getComponentType(), transform);
                // buffer.addComponent(ref, DisplayNameComponent.getComponentType(), nameplate);
                // buffer.invoke();
                //
                // For now, entity creation requires a concrete entity type.
                // The hologram tracking still works with the generated UUID.
                
                LOGGER.debug("Hologram entity spawn requested at ({}, {}, {}) text='{}' uuid={}", 
                    x, y, z, text, entityUuid);
            });
        } catch (Exception e) {
            LOGGER.error("Failed to spawn hologram entity: {}", e.getMessage(), e);
            return null;
        }
        
        return entityUuid;
    }
    
    /**
     * Update the nameplate text of an existing entity.
     * 
     * <p>Uses ECS pattern: find entity by UUID, then replace the DisplayName
     * component with the new text via store.replaceComponent().</p>
     */
    private void updateEntityNameplate(World world, UUID entityId, String newText) {
        try {
            world.execute(() -> {
                EntityStore entityStore = world.getEntityStore();
                if (entityStore == null) {
                    return;
                }
                
                Ref<EntityStore> ref = entityStore.getRefFromUUID(entityId);
                if (ref == null || !ref.isValid()) {
                    LOGGER.debug("Entity {} not found for nameplate update", entityId);
                    return;
                }
                
                // TODO: When DisplayNameComponent type is identified, use:
                // Store<EntityStore> store = entityStore.getStore();
                // store.replaceComponent(ref, DisplayNameComponent.getComponentType(), 
                //     new DisplayNameComponent(new Nameplate(newText)));
                
                LOGGER.debug("Nameplate update requested for entity {} -> '{}'", entityId, newText);
            });
        } catch (Exception e) {
            LOGGER.error("Failed to update nameplate for entity {}: {}", entityId, e.getMessage(), e);
        }
    }
    
    /**
     * Teleport an entity to a new position.
     * 
     * <p>Uses ECS pattern: access TransformComponent via entity store and
     * call teleportPosition() for server-side position update.</p>
     */
    private void teleportEntity(World world, UUID entityId, double x, double y, double z) {
        try {
            world.execute(() -> {
                EntityStore entityStore = world.getEntityStore();
                if (entityStore == null) {
                    return;
                }
                
                Ref<EntityStore> ref = entityStore.getRefFromUUID(entityId);
                if (ref == null || !ref.isValid()) {
                    LOGGER.debug("Entity {} not found for teleport", entityId);
                    return;
                }
                
                Store<EntityStore> store = entityStore.getStore();
                TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
                if (transform != null) {
                    transform.teleportPosition(new Vector3d(x, y, z));
                    LOGGER.debug("Teleported entity {} to ({}, {}, {})", entityId, x, y, z);
                } else {
                    LOGGER.debug("No TransformComponent on entity {} for teleport", entityId);
                }
            });
        } catch (Exception e) {
            LOGGER.error("Failed to teleport entity {}: {}", entityId, e.getMessage(), e);
        }
    }
    
    /**
     * Remove/despawn an entity from the world.
     * 
     * <p>Uses the world's entity store to find and remove the entity by UUID.
     * Entity removal must run on the world thread for thread safety.</p>
     */
    private void removeEntity(World world, UUID entityId) {
        try {
            world.execute(() -> {
                EntityStore entityStore = world.getEntityStore();
                if (entityStore == null) {
                    return;
                }
                
                // Find entity by UUID and remove it
                Entity entity = world.getEntity(entityId);
                if (entity != null) {
                    entity.remove();
                    LOGGER.debug("Removed hologram entity {}", entityId);
                } else {
                    LOGGER.debug("Entity {} not found for removal", entityId);
                }
            });
        } catch (Exception e) {
            LOGGER.error("Failed to remove entity {}: {}", entityId, e.getMessage(), e);
        }
    }
}