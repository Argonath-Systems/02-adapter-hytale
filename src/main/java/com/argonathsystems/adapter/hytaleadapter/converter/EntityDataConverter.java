package com.argonathsystems.adapter.hytaleadapter.converter;

import com.argonathsystems.framework.accessorapi.dto.EntityData;
import com.argonathsystems.framework.accessorapi.dto.LocationData;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * <p><b>MIGRATION-001 Status:</b> ✅ IMPLEMENTED</p>
 * 
 * <p>Converter for Entity (Hytale SDK) to EntityData (Platform-agnostic DTO).</p>
 * 
 * <h2>SDK Classes Used:</h2>
 * <ul>
 *   <li>{@code Entity} - Base entity from ECS system</li>
 *   <li>{@code TransformComponent} - Position/rotation data</li>
 *   <li>{@code UUIDComponent} - Entity UUID accessor</li>
 *   <li>{@code EntityStore} - World entity storage</li>
 * </ul>
 * 
 * @author Argonath Systems
 * @version 3.0.0
 * @since MIGRATION-001
 */
public class EntityDataConverter {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityDataConverter.class);
    
    /**
     * Convert from Hytale Entity to platform-agnostic EntityData.
     * 
     * @param entity Hytale SDK Entity object
     * @return EntityData DTO with position, type, and metadata
     */
    public static EntityData toDTO(Entity entity) {
        if (entity == null || entity.wasRemoved()) {
            return null;
        }
        
        try {
            // Get UUID from entity (deprecated but functional)
            @SuppressWarnings("removal")
            UUID uuid = entity.getUuid();
            
            // Get transform for position/rotation
            @SuppressWarnings("removal")
            TransformComponent transform = entity.getTransformComponent();
            
            double x = 0, y = 0, z = 0;
            float yaw = 0, pitch = 0;
            String worldName = null;
            
            if (transform != null) {
                Vector3d pos = transform.getPosition();
                x = pos.getX();
                y = pos.getY();
                z = pos.getZ();
                
                Vector3f rot = transform.getRotation();
                if (rot != null) {
                    yaw = rot.getY();
                    pitch = rot.getX();
                }
            }
            
            // Get world name
            World world = entity.getWorld();
            if (world != null) {
                worldName = world.getName();
            }
            
            // Get entity type/name (use display name or type ID)
            @SuppressWarnings("removal")
            String displayName = entity.getLegacyDisplayName();
            String entityType = displayName != null ? displayName : "entity";
            
            // Build location
            LocationData location = new LocationData(worldName, x, y, z, yaw, pitch);
            
            // Get health data (defaults if not available)
            int health = 100;
            int maxHealth = 100;
            
            return new EntityData(uuid, entityType, null, location, health, maxHealth);
            
        } catch (Exception e) {
            LOGGER.warn("Failed to convert entity to DTO: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Convert from Hytale Entity using ECS component store.
     * This is the preferred ECS-compliant method.
     * 
     * @param ref Entity reference in the component store
     * @param store The component store containing entity data
     * @param world The world the entity belongs to
     * @return EntityData DTO
     */
    public static EntityData toDTO(Ref<EntityStore> ref, Store<EntityStore> store, World world) {
        if (ref == null || !ref.isValid() || store == null) {
            return null;
        }
        
        try {
            // Get UUID component
            UUIDComponent uuidComp = store.getComponent(ref, UUIDComponent.getComponentType());
            UUID uuid = uuidComp != null ? uuidComp.getUuid() : UUID.randomUUID();
            
            // Get transform component
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            
            double x = 0, y = 0, z = 0;
            float yaw = 0, pitch = 0;
            
            if (transform != null) {
                Vector3d pos = transform.getPosition();
                x = pos.getX();
                y = pos.getY();
                z = pos.getZ();
                
                Vector3f rot = transform.getRotation();
                if (rot != null) {
                    yaw = rot.getY();
                    pitch = rot.getX();
                }
            }
            
            String worldName = world != null ? world.getName() : null;
            LocationData location = new LocationData(worldName, x, y, z, yaw, pitch);
            
            // Get health data (defaults if not available)
            int health = 100;
            int maxHealth = 100;
            
            return new EntityData(uuid, "entity", null, location, health, maxHealth);
            
        } catch (Exception e) {
            LOGGER.warn("Failed to convert entity ref to DTO: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Convert location data to Hytale SDK Vector3d.
     * 
     * @param location Platform-agnostic location
     * @return Hytale SDK position vector
     */
    public static Vector3d toVector3d(LocationData location) {
        if (location == null) {
            return new Vector3d(0, 0, 0);
        }
        return new Vector3d(location.x(), location.y(), location.z());
    }
    
    /**
     * Convert location data to Hytale SDK rotation Vector3f.
     * 
     * @param location Platform-agnostic location with yaw/pitch
     * @return Hytale SDK rotation vector (pitch, yaw, roll)
     */
    public static Vector3f toRotation(LocationData location) {
        if (location == null) {
            return new Vector3f(0, 0, 0);
        }
        return new Vector3f((float) location.pitch(), (float) location.yaw(), 0.0f);
    }
    
    /**
     * Get world by name or UUID string.
     * 
     * @param worldId World name or UUID
     * @return World instance or null
     */
    public static World getWorld(String worldId) {
        if (worldId == null || worldId.isEmpty()) {
            return Universe.get().getDefaultWorld();
        }
        
        // Try name lookup
        var worlds = Universe.get().getWorlds();
        for (World world : worlds.values()) {
            if (worldId.equals(world.getName())) {
                return world;
            }
        }
        
        // Try UUID lookup
        try {
            UUID uuid = UUID.fromString(worldId);
            return Universe.get().getWorld(uuid);
        } catch (IllegalArgumentException ignored) {
            // Not a UUID
        }
        
        return Universe.get().getDefaultWorld();
    }
}
