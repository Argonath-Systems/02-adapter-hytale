package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.InstanceAccessor;
import com.argonathsystems.framework.accessorapi.data.DataValue;
import com.argonathsystems.framework.accessorapi.dto.InstanceData;
import com.argonathsystems.framework.accessorapi.dto.InstanceData.InstanceState;
import com.argonathsystems.framework.accessorapi.dto.LocationData;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hytale implementation of InstanceAccessor for instanced world management.
 * 
 * <p>Provides dynamic instance creation using Hytale's Universe world management APIs.
 * Instances are backed by dynamically created/loaded worlds with tracking for metadata,
 * player assignments, and lifecycle management.
 * 
 * <h2>SDK PATTERN: Universe World Management</h2>
 * <pre>
 * // Create instance world:
 * Universe.get().addWorld(instanceId).thenAccept(world -> {
 *     // World is ready for players
 * });
 * 
 * // Load existing instance:
 * Universe.get().loadWorld(instanceId).thenAccept(world -> {
 *     // Restored from disk
 * });
 * 
 * // Remove instance:
 * Universe.get().removeWorld(instanceId);
 * </pre>
 * 
 * <h2>Specification Reference</h2>
 * <ul>
 *   <li>DRS-L1-001: Instanced Content Engine</li>
 *   <li>DRS-L3-001: Instance Lifecycle Manager</li>
 * </ul>
 * 
 * @author Argonath Systems Team
 * @version 1.1.0
 * @since 2.1.0
 */
public class HytaleInstanceAccessor implements InstanceAccessor {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HytaleInstanceAccessor.class);
    
    // Internal tracking for instances (since Hytale worlds don't have instance-specific metadata)
    private final Map<UUID, InstanceRecord> instances = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerToInstance = new ConcurrentHashMap<>();
    private final Map<UUID, LocationData> playerReturnLocations = new ConcurrentHashMap<>();
    
    // Reference to server (for Universe access via reflection or SDK)
    private final Object server;
    
    /**
     * Internal record tracking instance state beyond what Hytale World provides.
     */
    private static class InstanceRecord {
        final UUID instanceId;
        final String instanceType;
        final String worldName;
        final UUID ownerId;
        final int maxPlayers;
        final LocationData spawnLocation;
        final LocationData returnLocation;
        final Instant createdAt;
        Instant expiresAt;
        InstanceState state = InstanceState.ACTIVE;
        final Map<String, DataValue> metadata = new ConcurrentHashMap<>();
        final Set<UUID> players = ConcurrentHashMap.newKeySet();
        
        InstanceRecord(UUID instanceId, String instanceType, String worldName, UUID ownerId,
                       int maxPlayers, LocationData spawnLocation, LocationData returnLocation,
                       Duration timeout, Map<String, DataValue> initialMetadata) {
            this.instanceId = instanceId;
            this.instanceType = instanceType;
            this.worldName = worldName;
            this.ownerId = ownerId;
            this.maxPlayers = maxPlayers;
            this.spawnLocation = spawnLocation;
            this.returnLocation = returnLocation;
            this.createdAt = Instant.now();
            this.expiresAt = timeout != null ? createdAt.plus(timeout) : null;
            if (initialMetadata != null) {
                this.metadata.putAll(initialMetadata);
            }
        }
        
        InstanceData toInstanceData() {
            return new InstanceData(
                instanceId,
                worldName,
                instanceType,
                ownerId,
                state,
                new HashSet<>(players),
                maxPlayers,
                createdAt,
                expiresAt,
                spawnLocation,
                returnLocation,
                new HashMap<>(metadata)
            );
        }
    }
    
    public HytaleInstanceAccessor(Object server) {
        this.server = server;
        LOGGER.info("HytaleInstanceAccessor initialized with Universe world management integration");
    }
    
    // ==================== Instance Lifecycle ====================
    
    @Override
    public CompletableFuture<Optional<InstanceData>> createInstance(InstanceConfig config) {
        UUID instanceId = UUID.randomUUID();
        String worldName = "instance_" + config.instanceType() + "_" + instanceId.toString().substring(0, 8);
        
        LOGGER.info("Creating instance: type={}, worldName={}", config.instanceType(), worldName);
        
        try {
            Universe universe = Universe.get();
            
            // Create world configuration for instance
            WorldConfig worldConfig = new WorldConfig();
            
            // Use Universe.makeWorld() to create a new world for this instance
            java.nio.file.Path worldPath = java.nio.file.Path.of("worlds", "instances", worldName);
            
            return universe.makeWorld(worldName, worldPath, worldConfig)
                .thenApply(world -> {
                    if (world == null) {
                        LOGGER.error("Universe.makeWorld returned null for instance: {}", worldName);
                        return Optional.<InstanceData>empty();
                    }
                    
                    InstanceRecord record = new InstanceRecord(
                        instanceId, config.instanceType(), worldName,
                        config.ownerId(), config.maxPlayers(),
                        config.spawnLocation(), config.returnLocation(),
                        config.duration(), config.metadata()
                    );
                    instances.put(instanceId, record);
                    
                    LOGGER.info("Instance created successfully: id={}, world={}", instanceId, worldName);
                    return Optional.of(record.toInstanceData());
                })
                .exceptionally(ex -> {
                    LOGGER.error("Failed to create instance world: {}", worldName, ex);
                    return Optional.empty();
                });
        } catch (Exception e) {
            LOGGER.error("Failed to access Universe for instance creation: {}", worldName, e);
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }
    
    @Override
    public CompletableFuture<Void> destroyInstance(UUID instanceId) {
        InstanceRecord record = instances.get(instanceId);
        if (record == null) {
            LOGGER.warn("Attempted to destroy non-existent instance: {}", instanceId);
            return CompletableFuture.completedFuture(null);
        }
        
        LOGGER.info("Destroying instance: id={}, worldName={}", instanceId, record.worldName);
        
        // Teleport all players out first
        Set<UUID> playersToRemove = new HashSet<>(record.players);
        for (UUID playerId : playersToRemove) {
            teleportFromInstance(playerId);
        }
        
        // Remove instance world from Universe
        try {
            Universe universe = Universe.get();
            universe.removeWorld(record.worldName);
            LOGGER.info("Instance world removed from Universe: {}", record.worldName);
        } catch (Exception e) {
            LOGGER.error("Failed to remove instance world from Universe: {}", record.worldName, e);
        }
        
        instances.remove(instanceId);
        record.state = InstanceState.DESTROYED;
        LOGGER.info("Instance destroyed: id={}, world={}", instanceId, record.worldName);
        
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public Optional<InstanceData> getInstance(UUID instanceId) {
        InstanceRecord record = instances.get(instanceId);
        if (record == null) {
            return Optional.empty();
        }
        
        // Check expiration
        if (record.expiresAt != null && Instant.now().isAfter(record.expiresAt)) {
            LOGGER.debug("Instance {} has expired, scheduling cleanup", instanceId);
            // Schedule async cleanup
            CompletableFuture.runAsync(() -> destroyInstance(instanceId));
            return Optional.empty();
        }
        
        return Optional.of(record.toInstanceData());
    }
    
    @Override
    public Collection<InstanceData> getAllInstances() {
        return instances.values().stream()
            .filter(r -> r.expiresAt == null || Instant.now().isBefore(r.expiresAt))
            .map(InstanceRecord::toInstanceData)
            .toList();
    }
    
    @Override
    public Collection<InstanceData> getInstancesByType(String instanceType) {
        return instances.values().stream()
            .filter(r -> r.instanceType.equals(instanceType))
            .filter(r -> r.expiresAt == null || Instant.now().isBefore(r.expiresAt))
            .map(InstanceRecord::toInstanceData)
            .toList();
    }
    
    // ==================== Player Management ====================
    
    @Override
    public boolean teleportToInstance(UUID playerId, UUID instanceId) {
        return teleportToInstance(playerId, instanceId, null);
    }
    
    @Override
    public boolean teleportToInstance(UUID playerId, UUID instanceId, LocationData spawnLocation) {
        InstanceRecord record = instances.get(instanceId);
        if (record == null) {
            LOGGER.warn("Cannot teleport player {} to non-existent instance: {}", playerId, instanceId);
            return false;
        }
        
        try {
            Universe universe = Universe.get();
            
            // Find the instance world
            World instanceWorld = null;
            for (World w : universe.getWorlds().values()) {
                if (w.getName().equalsIgnoreCase(record.worldName)) {
                    instanceWorld = w;
                    break;
                }
            }
            if (instanceWorld == null) {
                LOGGER.warn("Instance world not loaded: {}", record.worldName);
                return false;
            }
            
            // Get player reference
            PlayerRef playerRef = universe.getPlayer(playerId);
            if (playerRef == null || !playerRef.isValid()) {
                LOGGER.warn("Player not found or offline: {}", playerId);
                return false;
            }
            
            // Save return location before teleporting
            UUID currentWorldUuid = playerRef.getWorldUuid();
            if (currentWorldUuid != null) {
                World currentWorld = universe.getWorld(currentWorldUuid);
                if (currentWorld != null) {
                    Transform currentTransform = playerRef.getTransform();
                    if (currentTransform != null) {
                        var pos = currentTransform.getPosition();
                        playerReturnLocations.put(playerId, new LocationData(
                            currentWorld.getName(),
                            pos.getX(), pos.getY(), pos.getZ(),
                            0, 0
                        ));
                    }
                }
            }
            
            // Create spawn transform
            LocationData spawn = spawnLocation != null ? spawnLocation : record.spawnLocation;
            Vector3f position;
            if (spawn != null) {
                position = new Vector3f((float) spawn.x(), (float) spawn.y(), (float) spawn.z());
            } else {
                position = new Vector3f(0, 64, 0);
            }
            
            Transform spawnTransform = new Transform(position.x, position.y, position.z);
            
            // Teleport player to instance world
            instanceWorld.addPlayer(playerRef, spawnTransform);
            
            // Update tracking
            playerToInstance.put(playerId, instanceId);
            record.players.add(playerId);
            
            LOGGER.info("Player {} teleported to instance {} (world={})", playerId, instanceId, record.worldName);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to teleport player {} to instance {}: {}", playerId, instanceId, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean teleportFromInstance(UUID playerId) {
        UUID instanceId = playerToInstance.get(playerId);
        if (instanceId == null) {
            LOGGER.debug("Player {} is not in an instance", playerId);
            return false;
        }
        
        InstanceRecord record = instances.get(instanceId);
        
        try {
            Universe universe = Universe.get();
            PlayerRef playerRef = universe.getPlayer(playerId);
            
            if (playerRef != null && playerRef.isValid()) {
                // Determine return destination
                LocationData returnLoc = playerReturnLocations.get(playerId);
                World targetWorld = null;
                Vector3f targetPosition;
                
                if (returnLoc != null) {
                    // Try to find the saved return world
                    for (World w : universe.getWorlds().values()) {
                        if (w.getName().equalsIgnoreCase(returnLoc.world())) {
                            targetWorld = w;
                            break;
                        }
                    }
                }
                
                if (targetWorld == null) {
                    // Fallback to default world
                    targetWorld = universe.getDefaultWorld();
                }
                
                if (targetWorld != null) {
                    if (returnLoc != null) {
                        targetPosition = new Vector3f(
                            (float) returnLoc.x(), (float) returnLoc.y(), (float) returnLoc.z()
                        );
                    } else {
                        targetPosition = new Vector3f(0, 64, 0);
                    }
                    
                    Transform returnTransform = new Transform(
                        targetPosition.x, targetPosition.y, targetPosition.z
                    );
                    targetWorld.addPlayer(playerRef, returnTransform);
                    LOGGER.info("Player {} returned from instance {} to world {}", 
                        playerId, instanceId, targetWorld.getName());
                } else {
                    LOGGER.warn("No target world available for player {} return from instance {}", 
                        playerId, instanceId);
                }
            } else {
                LOGGER.debug("Player {} offline during instance return, cleaning up tracking only", playerId);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to teleport player {} from instance {}: {}", 
                playerId, instanceId, e.getMessage(), e);
        }
        
        // Always update tracking regardless of teleport success
        playerToInstance.remove(playerId);
        playerReturnLocations.remove(playerId);
        if (record != null) {
            record.players.remove(playerId);
        }
        
        return true;
    }
    
    @Override
    public boolean isInInstance(UUID playerId) {
        return playerToInstance.containsKey(playerId);
    }
    
    @Override
    public Optional<UUID> getPlayerInstance(UUID playerId) {
        return Optional.ofNullable(playerToInstance.get(playerId));
    }
    
    @Override
    public Collection<UUID> getInstancePlayers(UUID instanceId) {
        InstanceRecord record = instances.get(instanceId);
        if (record == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(record.players);
    }
    
    // ==================== Instance State ====================
    
    @Override
    public void setInstanceMetadata(UUID instanceId, String key, DataValue value) {
        InstanceRecord record = instances.get(instanceId);
        if (record != null) {
            record.metadata.put(key, value);
            LOGGER.trace("Set metadata for instance {}: {}={}", instanceId, key, value);
        } else {
            LOGGER.warn("Cannot set metadata for non-existent instance: {}", instanceId);
        }
    }
    
    @Override
    public Optional<DataValue> getInstanceMetadata(UUID instanceId, String key) {
        InstanceRecord record = instances.get(instanceId);
        if (record != null) {
            return Optional.ofNullable(record.metadata.get(key));
        }
        return Optional.empty();
    }
    
    @Override
    public Map<String, DataValue> getAllInstanceMetadata(UUID instanceId) {
        InstanceRecord record = instances.get(instanceId);
        if (record != null) {
            return Collections.unmodifiableMap(record.metadata);
        }
        return Collections.emptyMap();
    }
    
    @Override
    public void extendInstanceDuration(UUID instanceId, Duration additionalTime) {
        InstanceRecord record = instances.get(instanceId);
        if (record != null && record.expiresAt != null) {
            record.expiresAt = record.expiresAt.plus(additionalTime);
            LOGGER.info("Extended instance {} duration by {}, new expiry: {}", 
                instanceId, additionalTime, record.expiresAt);
        }
    }
    
    @Override
    public Optional<Duration> getRemainingDuration(UUID instanceId) {
        InstanceRecord record = instances.get(instanceId);
        if (record != null && record.expiresAt != null) {
            Duration remaining = Duration.between(Instant.now(), record.expiresAt);
            return Optional.of(remaining.isNegative() ? Duration.ZERO : remaining);
        }
        return Optional.empty();
    }
    
    // ==================== Lifecycle Management ====================
    
    /**
     * Cleans up expired instances. Should be called periodically.
     */
    public void cleanupExpiredInstances() {
        Instant now = Instant.now();
        List<UUID> expired = instances.entrySet().stream()
            .filter(e -> e.getValue().expiresAt != null && now.isAfter(e.getValue().expiresAt))
            .map(Map.Entry::getKey)
            .toList();
        
        for (UUID instanceId : expired) {
            LOGGER.info("Cleaning up expired instance: {}", instanceId);
            destroyInstance(instanceId);
        }
    }
    
    /**
     * Handles player disconnect - removes from instance tracking.
     */
    public void onPlayerDisconnect(UUID playerId) {
        UUID instanceId = playerToInstance.remove(playerId);
        playerReturnLocations.remove(playerId);
        
        if (instanceId != null) {
            InstanceRecord record = instances.get(instanceId);
            if (record != null) {
                record.players.remove(playerId);
                LOGGER.debug("Player {} disconnected from instance {}", playerId, instanceId);
            }
        }
    }
}
