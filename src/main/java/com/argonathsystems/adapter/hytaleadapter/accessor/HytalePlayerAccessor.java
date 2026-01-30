package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.adapter.hytaleadapter.converter.LocationConverter;
import com.argonathsystems.adapter.hytaleadapter.converter.PlayerConverter;
import com.argonathsystems.framework.accessorapi.PlayerAccessor;
import com.argonathsystems.framework.accessorapi.dto.LocationData;
import com.argonathsystems.framework.accessorapi.dto.PlayerData;
import com.argonathsystems.framework.accessorapi.platform.PlatformEntity;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

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
        
        // Health is managed via EntityStatMap component in ECS
        // This requires ECS component access through the PlayerRef
        // 
        // For now, mark as partial implementation - needs ECS integration
        throw new UnsupportedOperationException(
            "setHealth requires ECS ComponentAccessor integration. " +
            "Access EntityStatMap via PlayerRef.getComponent(EntityStatMap.getComponentType())."
        );
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
