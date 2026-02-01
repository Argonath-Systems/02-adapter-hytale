package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.adapter.hytaleadapter.converter.LocationConverter;
import com.argonathsystems.framework.accessorapi.EntityAccessor;
import com.argonathsystems.framework.accessorapi.data.DataValue;
import com.argonathsystems.framework.accessorapi.dto.EntityData;
import com.argonathsystems.framework.accessorapi.dto.LocationData;
import com.hypixel.hytale.builtin.mounts.MountedByComponent;
import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.protocol.MountController;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hytale implementation of EntityAccessor for NPC/Entity operations.
 * 
 * <p><b>MIGRATION-001 Status:</b> 🟡 PARTIAL IMPLEMENTATION</p>
 * 
 * <p><b>Deprecation Notes (2026-01-30):</b></p>
 * <p>Uses deprecated SDK methods marked for removal:</p>
 * <ul>
 *   <li>{@code entity.getUuid()} → Use ECS pattern: {@code entity.getReference()} + ComponentAccessor</li>
 *   <li>{@code entity.getTransformComponent()} → Use ECS pattern: component registry lookup</li>
 *   <li>{@code entity.getLegacyDisplayName()} → Use ECS pattern: Entity.DISPLAY_NAME codec</li>
 * </ul>
 * <p>Migration requires access to Store&lt;EntityStore&gt; and ComponentAccessor patterns.</p>
 * 
 * <p>SDK Classes Used:</p>
 * <ul>
 *   <li>{@code Entity} - Base entity with remove(), getWorld(), getUuid()</li>
 *   <li>{@code World} - getEntity(UUID), spawnEntity()</li>
 *   <li>{@code TransformComponent} - teleportPosition(), getPosition()</li>
 * </ul>
 * 
 * <p>Implemented Methods:</p>
 * <ul>
 *   <li>✅ getEntity() - via World.getEntity(UUID)</li>
 *   <li>✅ removeEntity() - via Entity.remove()</li>
 *   <li>✅ teleport() - via TransformComponent.teleportPosition()</li>
 *   <li>✅ damage() - via EntityStatMap.subtractStatValue()</li>
 *   <li>✅ heal() - via EntityStatMap.addStatValue()</li>
 *   <li>✅ getEntities() - via EntityStore iteration</li>
 *   <li>✅ getEntitiesNear() - via EntityStore iteration with distance filter</li>
 *   <li>✅ getMountedEntity() - via MountedComponent.getMountedToEntity()</li>
 *   <li>✅ getPassengers() - via MountedByComponent.getPassengers()</li>
 * </ul>
 * 
 * <p>Pending Methods:</p>
 * <ul>
 *   <li>⏳ spawnEntity() - needs entity factory lookup</li>
 *   <li>⏳ navigateTo() - no pathfinding API available</li>
 *   <li>⏳ setMetadata(), getMetadata() - needs BsonDocument integration</li>
 *   <li>⏳ mountEntity(), dismountEntity() - needs component mutation via CommandBuffer</li>
 * </ul>
 * 
 * @author Argonath Systems Team
 * @version 3.0.0
 * @since MIGRATION-001
 */
public class HytaleNPCEntityAccessor implements EntityAccessor {
    
    private final HytaleServer server;
    
    /**
     * Entity cache for quick UUID lookups.
     * In production, this would integrate with EntityStore.
     */
    private final Map<UUID, Entity> entityCache = new ConcurrentHashMap<>();
    
    /**
     * Metadata cache for entity custom data.
     * Maps entity UUID to key-value pairs.
     */
    private final Map<UUID, Map<String, DataValue>> metadataCache = new ConcurrentHashMap<>();
    
    /**
     * Current world context for entity operations.
     */
    private World currentWorld;

    public HytaleNPCEntityAccessor(Object server) {
        this.server = (HytaleServer) server;
    }
    
    /**
     * Set the current world context for entity operations.
     * 
     * @param world The world to use for entity lookups
     */
    public void setWorld(World world) {
        this.currentWorld = world;
    }
    
    /**
     * Get current world, falling back to default.
     */
    private World getWorld() {
        if (currentWorld != null) {
            return currentWorld;
        }
        return Universe.get().getDefaultWorld();
    }

    @Override
    public Optional<EntityData> getEntity(UUID entityId) {
        if (entityId == null) {
            return Optional.empty();
        }
        
        World world = getWorld();
        if (world == null) {
            return Optional.empty();
        }
        
        // Lookup entity via World.getEntity(UUID)
        Entity entity = world.getEntity(entityId);
        if (entity == null || entity.wasRemoved()) {
            return Optional.empty();
        }
        
        return Optional.of(toEntityData(entity));
    }

    @Override
    public void removeEntity(UUID entityId) {
        if (entityId == null) {
            return;
        }
        
        World world = getWorld();
        if (world == null) {
            return;
        }
        
        Entity entity = world.getEntity(entityId);
        if (entity != null && !entity.wasRemoved()) {
            entity.remove();
            entityCache.remove(entityId);
        }
    }

    @Override
    public void teleport(UUID entityId, LocationData target) {
        if (entityId == null || target == null) {
            return;
        }
        
        World world = getWorld();
        if (world == null) {
            return;
        }
        
        Entity entity = world.getEntity(entityId);
        if (entity == null || entity.wasRemoved()) {
            return;
        }
        
        // Note: getTransformComponent() is deprecated - migrate to ECS pattern when available
        @SuppressWarnings("removal")
        TransformComponent transform = entity.getTransformComponent();
        if (transform == null) {
            return;
        }
        
        // Convert location to SDK types
        Vector3d position = LocationConverter.toVector3d(target);
        Vector3f rotation = new Vector3f(
            (float) target.pitch(),
            (float) target.yaw(),
            0.0f
        );
        
        // Teleport entity
        transform.teleportPosition(position);
        transform.teleportRotation(rotation);
    }

    @Override
    public Collection<EntityData> getEntities(String worldName) {
        World world = worldName != null ? Universe.get().getWorld(worldName) : getWorld();
        if (world == null) {
            return Collections.emptyList();
        }
        
        List<EntityData> entities = new ArrayList<>();
        EntityStore entityStore = world.getEntityStore();
        if (entityStore == null) {
            return Collections.emptyList();
        }
        
        Store<EntityStore> store = entityStore.getStore();
        store.forEachChunk((chunk, commandBuffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(i);
                if (ref != null && ref.isValid()) {
                    EntityData entityData = toEntityDataFromRef(ref, store, world);
                    if (entityData != null) {
                        entities.add(entityData);
                    }
                }
            }
        });
        
        return entities;
    }

    @Override
    public Collection<EntityData> getEntitiesNear(LocationData location, double radius) {
        if (location == null || radius <= 0) {
            return Collections.emptyList();
        }
        
        World world = location.world() != null ? Universe.get().getWorld(location.world()) : getWorld();
        if (world == null) {
            return Collections.emptyList();
        }
        
        double radiusSquared = radius * radius;
        List<EntityData> nearbyEntities = new ArrayList<>();
        EntityStore entityStore = world.getEntityStore();
        if (entityStore == null) {
            return Collections.emptyList();
        }
        
        Store<EntityStore> store = entityStore.getStore();
        store.forEachChunk((chunk, commandBuffer) -> {
            for (int i = 0; i < chunk.size(); i++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(i);
                if (ref == null || !ref.isValid()) {
                    continue;
                }
                
                // Get transform component to check distance
                TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
                if (transform == null) {
                    continue;
                }
                
                Vector3d pos = transform.getPosition();
                double dx = pos.getX() - location.x();
                double dy = pos.getY() - location.y();
                double dz = pos.getZ() - location.z();
                double distanceSquared = dx * dx + dy * dy + dz * dz;
                
                if (distanceSquared <= radiusSquared) {
                    EntityData entityData = toEntityDataFromRef(ref, store, world);
                    if (entityData != null) {
                        nearbyEntities.add(entityData);
                    }
                }
            }
        });
        
        return nearbyEntities;
    }

    @Override
    public Optional<EntityData> spawnEntity(String type, LocationData location) {
        if (type == null || location == null) {
            return Optional.empty();
        }
        
        World world = location.world() != null ? Universe.get().getWorld(location.world()) : getWorld();
        if (world == null) {
            return Optional.empty();
        }
        
        try {
            EntityStore entityStore = world.getEntityStore();
            if (entityStore == null) {
                return Optional.empty();
            }
            
            Store<EntityStore> store = entityStore.getStore();
            Vector3d position = new Vector3d(location.x(), location.y(), location.z());
            Vector3f rotation = new Vector3f((float) location.pitch(), (float) location.yaw(), 0.0f);
            
            // Use NPCPlugin for NPC spawning with role-based type
            NPCPlugin npcPlugin = NPCPlugin.get();
            if (npcPlugin == null) {
                return Optional.empty();
            }
            
            // Parse type as "role:variant" or just "role"
            String role = type;
            String variant = null;
            if (type.contains(":")) {
                String[] parts = type.split(":", 2);
                role = parts[0];
                variant = parts[1];
            }
            
            // Spawn NPC using NPCPlugin
            var result = npcPlugin.spawnNPC(store, role, variant, position, rotation);
            if (result == null) {
                return Optional.empty();
            }
            
            Ref<EntityStore> ref = result.left();
            if (ref == null || !ref.isValid()) {
                return Optional.empty();
            }
            
            // Get UUID from the spawned entity
            UUID entityUuid = getUuidFromRef(ref, entityStore);
            if (entityUuid == null) {
                // Generate new UUID if not found
                entityUuid = UUID.randomUUID();
            }
            
            // Build EntityData for the spawned entity
            EntityData entityData = new EntityData(
                entityUuid,
                type,
                null, // displayName
                location,
                20, // default health
                20  // default maxHealth
            );
            
            return Optional.of(entityData);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public void damage(UUID entityId, int amount) {
        if (entityId == null || amount <= 0) {
            return;
        }
        
        World world = getWorld();
        if (world == null) {
            return;
        }
        
        // Execute on world thread for thread safety
        world.execute(() -> {
            EntityStore entityStore = world.getEntityStore();
            if (entityStore == null) {
                return;
            }
            
            Ref<EntityStore> ref = entityStore.getRefFromUUID(entityId);
            if (ref == null || !ref.isValid()) {
                return;
            }
            
            Store<EntityStore> store = entityStore.getStore();
            EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
            if (statMap == null) {
                return;
            }
            
            // Get health stat index from DefaultEntityStatTypes
            int healthIndex = DefaultEntityStatTypes.getHealth();
            statMap.subtractStatValue(healthIndex, (float) amount);
        });
    }

    @Override
    public void heal(UUID entityId, int amount) {
        if (entityId == null || amount <= 0) {
            return;
        }
        
        World world = getWorld();
        if (world == null) {
            return;
        }
        
        // Execute on world thread for thread safety
        world.execute(() -> {
            EntityStore entityStore = world.getEntityStore();
            if (entityStore == null) {
                return;
            }
            
            Ref<EntityStore> ref = entityStore.getRefFromUUID(entityId);
            if (ref == null || !ref.isValid()) {
                return;
            }
            
            Store<EntityStore> store = entityStore.getStore();
            EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
            if (statMap == null) {
                return;
            }
            
            // Get health stat index from DefaultEntityStatTypes
            int healthIndex = DefaultEntityStatTypes.getHealth();
            statMap.addStatValue(healthIndex, (float) amount);
        });
    }

    @Override
    public void navigateTo(UUID entityId, LocationData target) {
        // TODO: Implement via pathfinding API
        throw new UnsupportedOperationException(
            "HytaleNPCEntityAccessor.navigateTo() requires pathfinding integration. " +
            "Research needed: PathfindingComponent, MovementController patterns."
        );
    }

    @Override
    public void setMetadata(UUID entityId, String key, DataValue value) {
        if (entityId == null || key == null) {
            return;
        }
        
        // Store in internal metadata cache
        // Production would use entity component system or BsonDocument
        Map<String, DataValue> entityMeta = metadataCache.computeIfAbsent(entityId, k -> new ConcurrentHashMap<>());
        
        if (value == null) {
            entityMeta.remove(key);
        } else {
            entityMeta.put(key, value);
        }
    }

    @Override
    public Optional<DataValue> getMetadata(UUID entityId, String key) {
        if (entityId == null || key == null) {
            return Optional.empty();
        }
        
        Map<String, DataValue> entityMeta = metadataCache.get(entityId);
        if (entityMeta == null) {
            return Optional.empty();
        }
        
        return Optional.ofNullable(entityMeta.get(key));
    }
    
    // ========================================================================
    // Mount/Riding Operations - Using Hytale's builtin.mounts ECS components
    // ========================================================================
    
    @Override
    public boolean mountEntity(UUID riderId, UUID mountId) {
        if (riderId == null || mountId == null) {
            return false;
        }
        
        World world = getWorld();
        if (world == null) {
            return false;
        }
        
        try {
            // Execute on world thread for thread safety
            world.execute(() -> {
                EntityStore entityStore = world.getEntityStore();
                if (entityStore == null) {
                    return;
                }
                
                Ref<EntityStore> riderRef = entityStore.getRefFromUUID(riderId);
                Ref<EntityStore> mountRef = entityStore.getRefFromUUID(mountId);
                if (riderRef == null || !riderRef.isValid() || mountRef == null || !mountRef.isValid()) {
                    return;
                }
                
                Store<EntityStore> store = entityStore.getStore();
                
                // Create MountedComponent for the rider pointing to mount
                Vector3f attachmentOffset = new Vector3f(0, 0, 0); // Default offset
                MountedComponent mountedComp = new MountedComponent(mountRef, attachmentOffset, MountController.Minecart);
                store.addComponent(riderRef, MountedComponent.getComponentType(), mountedComp);
                
                // Update MountedByComponent on the mount to include rider as passenger
                MountedByComponent mountedBy = store.getComponent(mountRef, MountedByComponent.getComponentType());
                if (mountedBy == null) {
                    // Create new MountedByComponent with rider as first passenger
                    MountedByComponent newMountedBy = new MountedByComponent();
                    newMountedBy.addPassenger(riderRef);
                    store.addComponent(mountRef, MountedByComponent.getComponentType(), newMountedBy);
                } else {
                    // Add rider to existing passenger list
                    mountedBy.addPassenger(riderRef);
                }
            });
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean dismountEntity(UUID riderId) {
        if (riderId == null) {
            return false;
        }
        
        World world = getWorld();
        if (world == null) {
            return false;
        }
        
        try {
            // Execute on world thread for thread safety
            world.execute(() -> {
                EntityStore entityStore = world.getEntityStore();
                if (entityStore == null) {
                    return;
                }
                
                Ref<EntityStore> riderRef = entityStore.getRefFromUUID(riderId);
                if (riderRef == null || !riderRef.isValid()) {
                    return;
                }
                
                Store<EntityStore> store = entityStore.getStore();
                
                // Get the mount reference from rider's MountedComponent before removing
                MountedComponent mounted = store.getComponent(riderRef, MountedComponent.getComponentType());
                if (mounted == null) {
                    return; // Rider is not mounted
                }
                
                Ref<EntityStore> mountRef = mounted.getMountedToEntity();
                
                // Remove MountedComponent from rider
                store.removeComponentIfExists(riderRef, MountedComponent.getComponentType());
                
                // Remove rider from mount's passenger list if mount exists
                if (mountRef != null && mountRef.isValid()) {
                    MountedByComponent mountedBy = store.getComponent(mountRef, MountedByComponent.getComponentType());
                    if (mountedBy != null) {
                        mountedBy.removePassenger(riderRef);
                    }
                }
            });
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public Optional<UUID> getMountedEntity(UUID riderId) {
        if (riderId == null) {
            return Optional.empty();
        }
        
        World world = getWorld();
        if (world == null) {
            return Optional.empty();
        }
        
        EntityStore entityStore = world.getEntityStore();
        if (entityStore == null) {
            return Optional.empty();
        }
        
        Ref<EntityStore> riderRef = entityStore.getRefFromUUID(riderId);
        if (riderRef == null || !riderRef.isValid()) {
            return Optional.empty();
        }
        
        Store<EntityStore> store = entityStore.getStore();
        MountedComponent mounted = store.getComponent(riderRef, MountedComponent.getComponentType());
        if (mounted == null) {
            return Optional.empty();
        }
        
        Ref<EntityStore> mountRef = mounted.getMountedToEntity();
        if (mountRef == null || !mountRef.isValid()) {
            return Optional.empty();
        }
        
        // Get UUID from mount reference
        UUID mountUuid = getUuidFromRef(mountRef, entityStore);
        return Optional.ofNullable(mountUuid);
    }
    
    @Override
    public Collection<UUID> getPassengers(UUID mountId) {
        if (mountId == null) {
            return Collections.emptyList();
        }
        
        World world = getWorld();
        if (world == null) {
            return Collections.emptyList();
        }
        
        EntityStore entityStore = world.getEntityStore();
        if (entityStore == null) {
            return Collections.emptyList();
        }
        
        Ref<EntityStore> mountRef = entityStore.getRefFromUUID(mountId);
        if (mountRef == null || !mountRef.isValid()) {
            return Collections.emptyList();
        }
        
        Store<EntityStore> store = entityStore.getStore();
        MountedByComponent mountedBy = store.getComponent(mountRef, MountedByComponent.getComponentType());
        if (mountedBy == null) {
            return Collections.emptyList();
        }
        
        List<Ref<EntityStore>> passengerRefs = mountedBy.getPassengers();
        if (passengerRefs == null || passengerRefs.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<UUID> passengerIds = new ArrayList<>();
        for (Ref<EntityStore> passengerRef : passengerRefs) {
            if (passengerRef != null && passengerRef.isValid()) {
                UUID passengerId = getUuidFromRef(passengerRef, entityStore);
                if (passengerId != null) {
                    passengerIds.add(passengerId);
                }
            }
        }
        
        return passengerIds;
    }
    
    // --- Helper Methods ---
    
    /**
     * Convert SDK Entity to framework EntityData DTO.
     * 
     * <p>Note: Uses deprecated SDK methods (getUuid, getLegacyDisplayName, getTransformComponent)
     * until ECS migration is complete.</p>
     */
    @SuppressWarnings("removal")
    private EntityData toEntityData(Entity entity) {
        if (entity == null) {
            return null;
        }
        
        UUID uuid = entity.getUuid();
        String displayName = entity.getLegacyDisplayName();
        String entityType = entity.getClass().getSimpleName();
        
        // Get location from TransformComponent
        LocationData location = null;
        TransformComponent transform = entity.getTransformComponent();
        if (transform != null) {
            Vector3d pos = transform.getPosition();
            Vector3f rot = transform.getRotation();
            String worldName = entity.getWorld() != null ? entity.getWorld().getName() : "world";
            location = new LocationData(
                worldName,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                rot.getY(), // yaw
                rot.getX()  // pitch
            );
        }
        
        // Get health from EntityStatMap if available
        int health = 20;
        int maxHealth = 20;
        
        return new EntityData(uuid, entityType, displayName, location, health, maxHealth);
    }
    
    /**
     * Convert entity reference to EntityData using ECS component access.
     * 
     * @param ref The entity reference
     * @param store The entity store
     * @param world The world context
     * @return EntityData or null if entity is invalid
     */
    private EntityData toEntityDataFromRef(Ref<EntityStore> ref, Store<EntityStore> store, World world) {
        if (ref == null || !ref.isValid()) {
            return null;
        }
        
        // Get transform component for position
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        LocationData location = null;
        if (transform != null) {
            Vector3d pos = transform.getPosition();
            Vector3f rot = transform.getRotation();
            String worldName = world != null ? world.getName() : "world";
            location = new LocationData(
                worldName,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                rot.getY(), // yaw
                rot.getX()  // pitch
            );
        }
        
        // Get health from EntityStatMap
        int health = 20;
        int maxHealth = 20;
        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap != null) {
            int healthIndex = DefaultEntityStatTypes.getHealth();
            EntityStatValue healthStat = statMap.get(healthIndex);
            if (healthStat != null) {
                health = (int) healthStat.get();
                maxHealth = (int) healthStat.getMax();
            }
        }
        
        // Get UUID from EntityStore
        UUID uuid = getUuidFromRef(ref, world.getEntityStore());
        if (uuid == null) {
            // Fallback: use ref hash as pseudo-UUID (not ideal but allows iteration)
            uuid = new UUID(0, ref.hashCode());
        }
        
        // Entity type is harder to get from ECS - use generic type
        String entityType = "Entity";
        String displayName = null;
        
        return new EntityData(uuid, entityType, displayName, location, health, maxHealth);
    }
    
    /**
     * Get UUID from entity reference.
     * 
     * <p>Uses UUIDComponent to extract UUID from entity reference.</p>
     * 
     * @param ref The entity reference
     * @param entityStore The entity store for lookup
     * @return UUID or null if not found
     */
    private UUID getUuidFromRef(Ref<EntityStore> ref, EntityStore entityStore) {
        if (ref == null || !ref.isValid() || entityStore == null) {
            return null;
        }
        
        try {
            Store<EntityStore> store = entityStore.getStore();
            // Use UUIDComponent to get entity UUID - this is the ECS-compliant approach
            UUIDComponent uuidComp = store.getComponent(ref, UUIDComponent.getComponentType());
            if (uuidComp != null) {
                return uuidComp.getUuid();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
