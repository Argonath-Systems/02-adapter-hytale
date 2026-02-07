package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.adapter.hytaleadapter.converter.LocationConverter;
import com.argonathsystems.adapter.hytaleadapter.converter.PlayerConverter;
import com.argonathsystems.adapter.hytale.permission.PermissionProvider;
import com.argonathsystems.framework.accessorapi.PlayerAccessor;
import com.argonathsystems.framework.accessorapi.dto.LocationData;
import com.argonathsystems.framework.accessorapi.dto.PlayerData;
import com.argonathsystems.framework.accessorapi.platform.PlatformEntity;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Hytale implementation of PlayerAccessor using official Hytale SDK.
 * 
 * <p><b>MIGRATION-001 Status:</b> ✅ IMPLEMENTED</p>
 * 
 * <p>SDK Classes Used:</p>
 * <ul>
 *   <li>{@code Universe} - Singleton for player/world lookup via Universe.get()</li>
 *   <li>{@code PlayerRef} - Player reference with getUuid(), getUsername(), getTransform()</li>
 *   <li>{@code Transform} - Position and rotation data</li>
 *   <li>{@code Message.raw(String)} - Raw text message wrapper for sendMessage()</li>
 * </ul>
 * 
 * @author Argonath Systems Team
 * @version 3.0.0
 * @since MIGRATION-001
 */
public class HytalePlayerAccessor implements PlayerAccessor {
    
    public HytalePlayerAccessor(Object server) {
        // Server instance not needed - use Universe.get() for player lookup
    }

    /**
     * Wrapper for Hytale's PlayerRef to implement PlatformEntity.
     */
    public record HytalePlayerEntity(UUID playerId) implements PlatformEntity {
        @Override
        public UUID getEntityId() {
            return playerId;
        }
    }

    @Override
    public Optional<PlayerData> getPlayer(UUID playerId) {
        PlayerRef playerRef = getPlayerRefById(playerId);
        if (playerRef == null || !playerRef.isValid()) {
            return Optional.empty();
        }
        
        return Optional.ofNullable(PlayerConverter.toDTO(playerRef));
    }

    @Override
    public Collection<PlayerData> getOnlinePlayers() {
        return Universe.get().getPlayers().stream()
            .filter(PlayerRef::isValid)
            .map(PlayerConverter::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public void teleport(UUID playerId, LocationData location) {
        PlayerRef playerRef = getPlayerRefById(playerId);
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }
        
        // Convert location to SDK types
        Vector3d position = LocationConverter.toVector3d(location);
        
        // Create rotation from yaw/pitch (pitch, yaw, roll)
        Vector3f rotation = new Vector3f(
            (float) location.pitch(),
            (float) location.yaw(),
            0.0f // roll
        );
        
        // Get the target world
        var world = Universe.get().getWorld(location.world());
        if (world == null) {
            world = Universe.get().getDefaultWorld();
        }
        
        // Create transform and update position
        Transform transform = new Transform(position, rotation);
        playerRef.updatePosition(world, transform, rotation);
    }

    @Override
    public void sendMessage(UUID playerId, String message) {
        PlayerRef playerRef = getPlayerRefById(playerId);
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }
        
        // SDK uses Message.raw() for raw text messages
        Message msg = Message.raw(message);
        playerRef.sendMessage(msg);
    }

    @Override
    public Optional<LocationData> getLocation(UUID playerId) {
        PlayerRef playerRef = getPlayerRefById(playerId);
        if (playerRef == null || !playerRef.isValid()) {
            return Optional.empty();
        }
        
        Transform transform = playerRef.getTransform();
        if (transform == null) {
            return Optional.empty();
        }
        
        // Get position and rotation from Transform
        Vector3d position = transform.getPosition();
        Vector3f rotation = transform.getRotation();
        
        // Get world name from player's current world
        String worldName = getWorldName(playerRef);
        
        return Optional.of(new LocationData(
            worldName,
            position.getX(),
            position.getY(),
            position.getZ(),
            rotation.getY(), // yaw
            rotation.getX()  // pitch
        ));
    }

    @Override
    public void setHealth(UUID playerId, int health) {
        PlayerRef playerRef = getPlayerRefById(playerId);
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }
        
        // Health is managed via EntityStatMap component in ECS.
        // Must execute on world thread for thread safety (ECS store access).
        // Pattern: PlayerRef → getWorldUuid() → Universe.getWorld() → world.execute()
        UUID worldUuid = playerRef.getWorldUuid();
        if (worldUuid == null) {
            return;
        }
        
        World world = Universe.get().getWorld(worldUuid);
        if (world == null) {
            return;
        }
        
        world.execute(() -> {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null || !ref.isValid()) {
                return;
            }
            
            Store<EntityStore> store = ref.getStore();
            if (store == null) {
                return;
            }
            
            EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
            if (statMap == null) {
                return;
            }
            
            int healthIndex = DefaultEntityStatTypes.getHealth();
            statMap.setStatValue(healthIndex, (float) Math.max(0, health));
        });
    }
    
    @Override
    public boolean hasPermission(UUID playerId, String permission) {
        PlayerRef playerRef = getPlayerRefById(playerId);
        if (playerRef == null || !playerRef.isValid()) {
            return false;
        }
        
        // Hytale SDK does not expose a native permission API.
        // Delegate to custom PermissionProvider (config-based role→permission mapping).
        // See: SA-ADAPTER-001 (HA-004), SF-ACCESSOR-API-010 (AA-020)
        return PermissionProvider.getInstance().hasPermission(playerId, permission);
    }
    
    @Override
    public UUID getPlayerId(PlatformEntity platformEntity) {
        return platformEntity.getEntityId();
    }
    
    // --- Helper Methods ---
    
    /**
     * Get PlayerRef from Universe by UUID.
     * 
     * @param playerId Player's UUID
     * @return PlayerRef or null if not found
     */
    private PlayerRef getPlayerRefById(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        return Universe.get().getPlayer(playerId);
    }
    
    /**
     * Get world name from PlayerRef.
     * 
     * @param playerRef the player reference
     * @return world name or "world" as fallback
     */
    private String getWorldName(PlayerRef playerRef) {
        UUID worldUuid = playerRef.getWorldUuid();
        if (worldUuid != null) {
            var world = Universe.get().getWorld(worldUuid);
            if (world != null) {
                return world.getName();
            }
        }
        return "world"; // Default fallback
    }
}
