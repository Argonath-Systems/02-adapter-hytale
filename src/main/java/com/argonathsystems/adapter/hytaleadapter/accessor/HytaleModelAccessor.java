package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.ModelAccessor;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.core.universe.world.npc.INonPlayerCharacter;
import it.unimi.dsi.fastutil.Pair;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hytale implementation of ModelAccessor for entity appearance and animation.
 * 
 * <h2>SDK Integration:</h2>
 * <p>This accessor leverages available Hytale SDK features for model and animation control:</p>
 * <ul>
 *   <li>{@code AnimationUtils.playAnimation()} - Play named animations on entities</li>
 *   <li>{@code AnimationUtils.stopAnimation()} - Stop animations by slot</li>
 *   <li>{@code EntityScaleComponent} - Scale entities via ECS component</li>
 *   <li>{@code AnimationSlot} - Animation slot types (FULL_BODY, UPPER_BODY, etc.)</li>
 * </ul>
 * 
 * <h2>Limitations:</h2>
 * <ul>
 *   <li>Model spawning requires entity spawning API (complex ECS)</li>
 *   <li>Model/skin swapping not supported at runtime</li>
 *   <li>Glow effects not exposed in current SDK</li>
 *   <li>Equipment visualization requires separate EquipmentComponent</li>
 * </ul>
 * 
 * @author Argonath Systems
 * @since 2.0.0
 * @see com.hypixel.hytale.server.core.entity.AnimationUtils
 * @see com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent
 */
public class HytaleModelAccessor implements ModelAccessor {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HytaleModelAccessor.class);
    
    /** Cache of entity UUID to entity reference for quick lookups */
    private final Map<UUID, EntityReference> entityCache = new ConcurrentHashMap<>();
    
    /** Reference to the game server */
    private final Object server;
    
    /**
     * Internal wrapper for entity references needed for ECS operations.
     */
    private static class EntityReference {
        final UUID uuid;
        final String worldId;
        Ref<EntityStore> entityRef;
        
        EntityReference(UUID uuid, String worldId) {
            this.uuid = uuid;
            this.worldId = worldId;
        }
    }
    
    public HytaleModelAccessor(Object server) {
        this.server = server;
        LOGGER.info("HytaleModelAccessor initialized with SDK animation support");
    }
    
    @Override
    public UUID spawnModel(String modelId, double x, double y, double z) {
        // Delegate to NPCPlugin for entity creation, using the model ID as the NPC role.
        // This creates a visual-only entity — the NPC role determines the model appearance.
        // The spawned entity can then be animated via playAnimation()/stopAnimation().
        
        NPCPlugin npcPlugin = NPCPlugin.get();
        if (npcPlugin == null) {
            LOGGER.error("Cannot spawn model: NPCPlugin not available");
            throw new UnsupportedOperationException(
                "Model spawning requires NPCPlugin which is not loaded.");
        }
        
        // Find a world to spawn in — use the default world
        Universe universe = Universe.get();
        World defaultWorld = universe.getDefaultWorld();
        if (defaultWorld == null) {
            LOGGER.error("Cannot spawn model: no default world available");
            throw new UnsupportedOperationException(
                "Model spawning requires a world. No default world found.");
        }
        
        UUID entityUuid = UUID.randomUUID();
        
        // Parse modelId as "role" or "role:variant" pattern
        String role = modelId;
        String variant = null;
        if (modelId.contains(":")) {
            String[] parts = modelId.split(":", 2);
            role = parts[0];
            variant = parts[1];
        }
        
        final String npcRole = role;
        final String npcVariant = variant;
        
        defaultWorld.execute(() -> {
            try {
                Store<EntityStore> store = defaultWorld.getEntityStore().getStore();
                Vector3d position = new Vector3d(x, y, z);
                Vector3f rotation = new Vector3f(0.0f, 0.0f, 0.0f);
                
                Pair<Ref<EntityStore>, INonPlayerCharacter> result = 
                    npcPlugin.spawnNPC(
                        store, npcRole, npcVariant, position, rotation);
                
                if (result != null && result.left() != null && result.left().isValid()) {
                    Ref<EntityStore> ref = result.left();
                    
                    // Cache the entity reference for later animation/removal
                    EntityReference entityRef = new EntityReference(entityUuid, defaultWorld.getName());
                    entityRef.entityRef = ref;
                    entityCache.put(entityUuid, entityRef);
                    
                    LOGGER.info("Spawned model entity '{}' at ({}, {}, {}) -> UUID {}", 
                        modelId, x, y, z, entityUuid);
                } else {
                    LOGGER.error("NPCPlugin.spawnNPC returned null or invalid ref for model '{}'", modelId);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to spawn model '{}' at ({}, {}, {}): {}", 
                    modelId, x, y, z, e.getMessage(), e);
            }
        });
        
        return entityUuid;
    }
    
    @Override
    public void removeModel(UUID modelId) {
        EntityReference ref = entityCache.remove(modelId);
        if (ref == null) {
            LOGGER.warn("Model entity not found in cache: {}", modelId);
            return;
        }
        
        // Remove entity from world via ECS
        try {
            World world = getWorld(ref.worldId);
            if (world != null) {
                world.execute(() -> {
                    try {
                        Entity entity = world.getEntity(modelId);
                        if (entity != null) {
                            entity.remove();
                            LOGGER.debug("Removed model entity {} from world {}", modelId, ref.worldId);
                        } else {
                            LOGGER.debug("Entity {} already removed from world {}", modelId, ref.worldId);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to remove model entity {} in world thread: {}", 
                            modelId, e.getMessage(), e);
                    }
                });
            }
        } catch (Exception e) {
            LOGGER.error("Failed to remove model entity {}: {}", modelId, e.getMessage());
        }
    }
    
    @Override
    public void playAnimation(UUID entityId, String animationId) {
        playAnimation(entityId, animationId, false);
    }
    
    @Override
    public void playAnimation(UUID entityId, String animationId, boolean loop) {
        EntityReference ref = getOrLookupEntity(entityId);
        if (ref == null || ref.entityRef == null) {
            LOGGER.warn("Entity not found for animation: {}", entityId);
            return;
        }
        
        try {
            World world = getWorld(ref.worldId);
            if (world == null) {
                LOGGER.warn("World not found for entity {}: {}", entityId, ref.worldId);
                return;
            }
            
            Store<EntityStore> store = world.getEntityStore().getStore();
            // Store implements ComponentAccessor<EntityStore> directly
            
            // Use FULL_BODY slot by default for general animations
            AnimationUtils.playAnimation(
                ref.entityRef,
                AnimationSlot.Action,
                animationId,
                loop,
                store);
            
            LOGGER.debug("Playing animation '{}' on entity {} (loop={})", animationId, entityId, loop);
            
        } catch (Exception e) {
            LOGGER.error("Failed to play animation '{}' on entity {}: {}", 
                animationId, entityId, e.getMessage());
        }
    }
    
    @Override
    public void stopAnimation(UUID entityId, String animationId) {
        EntityReference ref = getOrLookupEntity(entityId);
        if (ref == null || ref.entityRef == null) {
            LOGGER.warn("Entity not found for stop animation: {}", entityId);
            return;
        }
        
        try {
            World world = getWorld(ref.worldId);
            if (world == null) {
                return;
            }
            
            Store<EntityStore> store = world.getEntityStore().getStore();
            // Store implements ComponentAccessor<EntityStore> directly
            
            // Stop animation by slot
            AnimationUtils.stopAnimation(
                ref.entityRef,
                AnimationSlot.Action,
                store);
            
            LOGGER.debug("Stopped animation on entity {}", entityId);
            
        } catch (Exception e) {
            LOGGER.error("Failed to stop animation on entity {}: {}", entityId, e.getMessage());
        }
    }
    
    @Override
    public void stopAllAnimations(UUID entityId) {
        EntityReference ref = getOrLookupEntity(entityId);
        if (ref == null || ref.entityRef == null) {
            LOGGER.warn("Entity not found for stop all animations: {}", entityId);
            return;
        }
        
        try {
            World world = getWorld(ref.worldId);
            if (world == null) {
                return;
            }
            
            Store<EntityStore> store = world.getEntityStore().getStore();
            // Store implements ComponentAccessor<EntityStore> directly
            
            // Stop animations on all slots
            for (AnimationSlot slot : AnimationSlot.values()) {
                try {
                    AnimationUtils.stopAnimation(ref.entityRef, slot, store);
                } catch (Exception e) {
                    // Ignore if slot has no active animation
                }
            }
            
            LOGGER.debug("Stopped all animations on entity {}", entityId);
            
        } catch (Exception e) {
            LOGGER.error("Failed to stop all animations on entity {}: {}", entityId, e.getMessage());
        }
    }
    
    @Override
    public void setModel(UUID entityId, String modelId) {
        // Model modification at runtime is not supported by Hytale SDK
        // Entity models are determined at spawn time
        throw new UnsupportedOperationException(
            "Runtime model modification not supported by Hytale SDK. " +
            "Entity models are determined at spawn time."
        );
    }
    
    @Override
    public void setSkin(UUID entityId, String skinId) {
        // Skin/texture modification not exposed in current API
        throw new UnsupportedOperationException(
            "Skin modification not supported by current Hytale SDK. " +
            "Requires skin/texture component API."
        );
    }
    
    @Override
    public void setModelVariant(UUID entityId, String variantId) {
        // Model variant control not exposed in current API
        throw new UnsupportedOperationException(
            "Model variant control not supported by current Hytale SDK."
        );
    }
    
    @Override
    public void setEquipment(UUID entityId, String slot, String itemId) {
        // Equipment visualization requires EquipmentComponent
        // This is handled by a separate system
        throw new UnsupportedOperationException(
            "Equipment visualization requires EquipmentComponent. " +
            "Use InventoryAccessor for equipment management."
        );
    }
    
    @Override
    public void clearEquipment(UUID entityId, String slot) {
        throw new UnsupportedOperationException(
            "Equipment clearing requires EquipmentComponent."
        );
    }
    
    @Override
    public void setScale(UUID entityId, float scale) {
        EntityReference ref = getOrLookupEntity(entityId);
        if (ref == null || ref.entityRef == null) {
            LOGGER.warn("Entity not found for scale: {}", entityId);
            return;
        }
        
        try {
            World world = getWorld(ref.worldId);
            if (world == null) {
                return;
            }
            
            Store<EntityStore> store = world.getEntityStore().getStore();
            
            // Get EntityScaleComponent via EntityStore
            EntityScaleComponent scaleComponent = store.getComponent(
                ref.entityRef,
                EntityScaleComponent.getComponentType()
            );
            
            if (scaleComponent != null) {
                scaleComponent.setScale(scale);
                LOGGER.debug("Set scale {} on entity {}", scale, entityId);
            } else {
                // Need to add component via CommandBuffer
                LOGGER.warn("EntityScaleComponent not found on entity {}. " +
                    "Scale can only be modified if component exists.", entityId);
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to set scale on entity {}: {}", entityId, e.getMessage());
        }
    }
    
    @Override
    public void setGlowing(UUID entityId, boolean glowing) {
        // Glow effect not exposed in current Hytale SDK
        throw new UnsupportedOperationException(
            "Glow effect not supported by current Hytale SDK. " +
            "Requires visual effects API extension."
        );
    }
    
    @Override
    public void setGlowColor(UUID entityId, int color) {
        // Glow color not exposed in current Hytale SDK
        throw new UnsupportedOperationException(
            "Glow color not supported by current Hytale SDK."
        );
    }
    
    // =====================================================
    // Private Helper Methods
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
     * Get or lookup an entity reference by UUID.
     */
    private EntityReference getOrLookupEntity(UUID entityId) {
        EntityReference cached = entityCache.get(entityId);
        if (cached != null && cached.entityRef != null) {
            return cached;
        }
        
        // Search for entity in all worlds
        try {
            Universe universe = Universe.get();
            if (universe == null) {
                return null;
            }
            
            // getWorlds() returns Map<String, World>, iterate over values
            for (World world : universe.getWorlds().values()) {
                Entity entity = world.getEntity(entityId);
                if (entity != null) {
                    String worldId = world.getName();
                    EntityReference ref = new EntityReference(entityId, worldId);
                    // Use World.getEntityRef(UUID) instead of entity.getRef()
                    ref.entityRef = world.getEntityRef(entityId);
                    entityCache.put(entityId, ref);
                    return ref;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error looking up entity {}: {}", entityId, e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Register an entity with this accessor for later reference.
     * Called by NPCAccessor or other entity creation systems.
     * 
     * @param entityId UUID of the entity
     * @param worldId World containing the entity
     * @param entityRef ECS reference to the entity
     */
    public void registerEntity(UUID entityId, String worldId, Ref<EntityStore> entityRef) {
        EntityReference ref = new EntityReference(entityId, worldId);
        ref.entityRef = entityRef;
        entityCache.put(entityId, ref);
        LOGGER.debug("Registered entity {} in world {}", entityId, worldId);
    }
    
    /**
     * Unregister an entity from the cache.
     * 
     * @param entityId UUID of the entity to unregister
     */
    public void unregisterEntity(UUID entityId) {
        entityCache.remove(entityId);
        LOGGER.debug("Unregistered entity {}", entityId);
    }
}
