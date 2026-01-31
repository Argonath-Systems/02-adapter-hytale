package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessor.MultiWorldAccessor;
import com.argonathsystems.framework.accessorapi.dto.LocationData;
import com.argonathsystems.framework.accessorapi.dto.TeleportResult;
import com.argonathsystems.framework.accessorapi.dto.WorldCreateConfig;
import com.argonathsystems.framework.accessorapi.dto.WorldData;
import com.argonathsystems.framework.accessorapi.dto.WorldState;
import com.argonathsystems.framework.accessorapi.dto.WorldType;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.universe.world.gen.DummyWorldGenProvider;
import com.hypixel.hytale.server.core.universe.world.gen.FlatWorldGenProvider;
import com.hypixel.hytale.server.core.universe.world.gen.VoidWorldGenProvider;
import com.hypixel.hytale.server.core.universe.world.gen.WorldGenProvider;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Hytale implementation of MultiWorldAccessor using Universe API.
 * 
 * <p>Provides multi-world management capabilities by wrapping the Hytale
 * Universe singleton and World API.
 * 
 * <h2>SDK Classes Used</h2>
 * <ul>
 *   <li>{@code Universe} - Singleton managing all worlds</li>
 *   <li>{@code World} - Individual world instance</li>
 *   <li>{@code WorldConfig} - World configuration builder</li>
 *   <li>{@code FlatWorldGenProvider} - Flat world generation</li>
 *   <li>{@code VoidWorldGenProvider} - Void/empty world generation</li>
 *   <li>{@code WorldGenProvider} - Custom world generation interface</li>
 * </ul>
 * 
 * <h2>Specification Reference</h2>
 * <ul>
 *   <li>SF-WORLD-050: World Manager Framework</li>
 *   <li>WM-L3-010: Platform Adapter</li>
 * </ul>
 * 
 * @author Argonath Systems Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class HytaleMultiWorldAccessor implements MultiWorldAccessor {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HytaleMultiWorldAccessor.class);
    
    private final HytaleServer server;
    
    /**
     * Create a new HytaleMultiWorldAccessor.
     * 
     * @param server The Hytale server instance
     */
    public HytaleMultiWorldAccessor(Object server) {
        this.server = (HytaleServer) server;
        LOGGER.info("HytaleMultiWorldAccessor initialized");
    }
    
    // ==================== World Lifecycle ====================
    
    @Override
    public CompletableFuture<WorldData> createWorld(WorldCreateConfig config) {
        LOGGER.info("Creating world: {} (type={})", config.name(), config.type());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Universe universe = Universe.get();
                
                // Build world configuration
                WorldConfig worldConfig = new WorldConfig();
                
                // Set world generation provider based on type
                WorldGenProvider genProvider = createWorldGenProvider(config);
                worldConfig.setWorldGenProvider(genProvider);
                
                // Apply game mode
                if (config.gameMode() != null) {
                    worldConfig.setGameMode(mapGameMode(config.gameMode()));
                }
                
                // Apply world rules from config
                applyWorldRules(worldConfig, config);
                
                // Determine world path
                Path worldPath = determineWorldPath(config);
                
                // Create the world using Universe API
                // Universe.makeWorld(name, path, config) creates a new world
                World world = universe.makeWorld(config.name(), worldPath, worldConfig);
                
                if (world == null) {
                    throw new RuntimeException("Universe.makeWorld returned null for: " + config.name());
                }
                
                // Add to universe if needed (some implementations require explicit add)
                // universe.addWorld(world);
                
                LOGGER.info("Successfully created world: {}", config.name());
                
                return convertToWorldData(world, WorldState.LOADED);
                
            } catch (Exception e) {
                LOGGER.error("Failed to create world: {}", config.name(), e);
                throw new RuntimeException("World creation failed: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> removeWorld(String name, boolean deleteFiles) {
        LOGGER.info("Removing world: {} (deleteFiles={})", name, deleteFiles);
        
        return CompletableFuture.runAsync(() -> {
            try {
                Universe universe = Universe.get();
                
                // Find the world
                Optional<World> worldOpt = findWorld(name);
                if (worldOpt.isEmpty()) {
                    LOGGER.warn("World not found for removal: {}", name);
                    return;
                }
                
                World world = worldOpt.get();
                
                // Remove from universe
                universe.removeWorld(world);
                
                // Delete files if requested
                if (deleteFiles) {
                    // TODO: Delete world directory from disk
                    LOGGER.info("File deletion requested but not implemented");
                }
                
                LOGGER.info("Successfully removed world: {}", name);
                
            } catch (Exception e) {
                LOGGER.error("Failed to remove world: {}", name, e);
                throw new RuntimeException("World removal failed: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public CompletableFuture<WorldData> loadWorld(String name) {
        LOGGER.info("Loading world: {}", name);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Universe universe = Universe.get();
                
                // Try to load from disk
                Path worldPath = Path.of("worlds", name);
                World world = universe.loadWorld(worldPath);
                
                if (world == null) {
                    throw new RuntimeException("Failed to load world: " + name);
                }
                
                // Add to universe
                universe.addWorld(world);
                
                LOGGER.info("Successfully loaded world: {}", name);
                
                return convertToWorldData(world, WorldState.LOADED);
                
            } catch (Exception e) {
                LOGGER.error("Failed to load world: {}", name, e);
                throw new RuntimeException("World load failed: " + e.getMessage(), e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> unloadWorld(String name) {
        LOGGER.info("Unloading world: {}", name);
        
        return CompletableFuture.runAsync(() -> {
            try {
                Universe universe = Universe.get();
                
                Optional<World> worldOpt = findWorld(name);
                if (worldOpt.isEmpty()) {
                    LOGGER.warn("World not found for unload: {}", name);
                    return;
                }
                
                World world = worldOpt.get();
                
                // Save world before unloading
                // world.save(); // If available in SDK
                
                // Remove from active worlds but keep on disk
                universe.removeWorld(world);
                
                LOGGER.info("Successfully unloaded world: {}", name);
                
            } catch (Exception e) {
                LOGGER.error("Failed to unload world: {}", name, e);
                throw new RuntimeException("World unload failed: " + e.getMessage(), e);
            }
        });
    }
    
    // ==================== World Query ====================
    
    @Override
    public Collection<WorldData> getWorlds() {
        return Universe.get().getWorlds().stream()
            .map(world -> convertToWorldData(world, WorldState.LOADED))
            .collect(Collectors.toList());
    }
    
    @Override
    public Optional<WorldData> getWorld(String name) {
        return findWorld(name)
            .map(world -> convertToWorldData(world, WorldState.LOADED));
    }
    
    @Override
    public boolean worldExists(String name) {
        return findWorld(name).isPresent();
    }
    
    @Override
    public boolean isWorldLoaded(String name) {
        return findWorld(name).isPresent();
    }
    
    // ==================== Teleportation ====================
    
    @Override
    public CompletableFuture<TeleportResult> teleportToWorld(UUID playerId, String worldName, LocationData location) {
        LOGGER.debug("Teleporting player {} to world {}", playerId, worldName);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Find target world
                Optional<World> worldOpt = findWorld(worldName);
                if (worldOpt.isEmpty()) {
                    return new TeleportResult(false, "World not found: " + worldName, null);
                }
                
                World targetWorld = worldOpt.get();
                
                // Find player reference
                PlayerRef playerRef = findPlayerRef(playerId);
                if (playerRef == null) {
                    return new TeleportResult(false, "Player not found", null);
                }
                
                // Calculate transform (position + rotation)
                Vector3f position;
                if (location != null) {
                    position = new Vector3f(
                        (float) location.x(),
                        (float) location.y(),
                        (float) location.z()
                    );
                } else {
                    // Use world spawn
                    // TODO: Get world spawn from configuration
                    position = new Vector3f(0, 64, 0);
                }
                
                // Teleport player to world
                // World.addPlayer(PlayerRef, Transform) adds player to world at position
                targetWorld.addPlayer(playerRef, createTransform(position));
                
                LocationData finalLocation = new LocationData(
                    worldName,
                    position.x, position.y, position.z,
                    0, 0 // pitch, yaw
                );
                
                LOGGER.info("Successfully teleported player {} to world {}", playerId, worldName);
                
                return new TeleportResult(true, "Teleported successfully", finalLocation);
                
            } catch (Exception e) {
                LOGGER.error("Failed to teleport player {} to world {}", playerId, worldName, e);
                return new TeleportResult(false, "Teleport failed: " + e.getMessage(), null);
            }
        });
    }
    
    // ==================== World Rules ====================
    
    @Override
    public void setWorldRule(String worldName, WorldRule rule, Object value) {
        LOGGER.debug("Setting world rule {}={} for world {}", rule, value, worldName);
        
        findWorld(worldName).ifPresent(world -> {
            WorldConfig config = world.getConfig();
            if (config == null) {
                LOGGER.warn("World config not available for: {}", worldName);
                return;
            }
            
            switch (rule) {
                case PVP_ENABLED -> config.setPvpEnabled((Boolean) value);
                case FALL_DAMAGE_ENABLED -> config.setFallDamageEnabled((Boolean) value);
                case MOB_SPAWNING -> config.setSpawningNPC((Boolean) value);
                case NPC_FROZEN -> config.setNPCFrozen((Boolean) value);
                case TIME_PAUSED -> config.setTimePaused((Boolean) value);
                default -> LOGGER.debug("Rule {} not mapped to Hytale config", rule);
            }
        });
    }
    
    @Override
    public void setGameMode(String worldName, GameModeType gameMode) {
        LOGGER.debug("Setting game mode {} for world {}", gameMode, worldName);
        
        findWorld(worldName).ifPresent(world -> {
            WorldConfig config = world.getConfig();
            if (config != null) {
                config.setGameMode(mapGameMode(gameMode.name()));
            }
        });
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * Find a world by name (case-insensitive).
     */
    private Optional<World> findWorld(String name) {
        return Universe.get().getWorlds().stream()
            .filter(w -> w.getName().equalsIgnoreCase(name))
            .findFirst();
    }
    
    /**
     * Convert Hytale World to WorldData DTO.
     */
    private WorldData convertToWorldData(World world, WorldState state) {
        // Determine world type based on generation provider
        WorldType type = determineWorldType(world);
        
        return new WorldData(
            world.getName(),
            type,
            state,
            countPlayers(world),
            "SURVIVAL", // Default game mode - would need to read from config
            getWorldSpawn(world)
        );
    }
    
    /**
     * Determine world type from world's generation provider.
     */
    private WorldType determineWorldType(World world) {
        WorldConfig config = world.getConfig();
        if (config == null) {
            return WorldType.NORMAL;
        }
        
        WorldGenProvider provider = config.getWorldGenProvider();
        if (provider instanceof FlatWorldGenProvider) {
            return WorldType.FLAT;
        } else if (provider instanceof VoidWorldGenProvider) {
            return WorldType.VOID;
        }
        
        return WorldType.NORMAL;
    }
    
    /**
     * Create world generation provider based on world type.
     */
    private WorldGenProvider createWorldGenProvider(WorldCreateConfig config) {
        return switch (config.type()) {
            case FLAT -> new FlatWorldGenProvider(); // Can configure layers
            case VOID -> new VoidWorldGenProvider();
            case NETHER, END -> new DummyWorldGenProvider(); // Placeholder
            default -> null; // Use default generation
        };
    }
    
    /**
     * Determine world storage path.
     */
    private Path determineWorldPath(WorldCreateConfig config) {
        // Use custom path if provided, otherwise default to worlds/<name>
        return Path.of("worlds", config.name());
    }
    
    /**
     * Apply world rules from config to WorldConfig.
     */
    private void applyWorldRules(WorldConfig worldConfig, WorldCreateConfig createConfig) {
        // Apply common defaults
        worldConfig.setPvpEnabled(false);
        worldConfig.setFallDamageEnabled(true);
        
        // TODO: Apply rules from createConfig.rules() map
    }
    
    /**
     * Map game mode string to Hytale game mode enum.
     */
    private Object mapGameMode(String gameMode) {
        // TODO: Map to actual Hytale GameMode enum when available
        // For now return the string - implementation depends on SDK
        return gameMode;
    }
    
    /**
     * Count players in a world.
     */
    private int countPlayers(World world) {
        // TODO: Implement when player tracking is available
        return 0;
    }
    
    /**
     * Get world spawn location.
     */
    private LocationData getWorldSpawn(World world) {
        // TODO: Get actual spawn from world config/spawn provider
        return new LocationData(world.getName(), 0, 64, 0, 0, 0);
    }
    
    /**
     * Find a PlayerRef for a given UUID.
     */
    private PlayerRef findPlayerRef(UUID playerId) {
        // TODO: Implement player lookup via server
        // server.getPlayer(playerId) or similar
        LOGGER.warn("STUB: Player lookup not fully implemented");
        return null;
    }
    
    /**
     * Create a transform for positioning.
     */
    private Object createTransform(Vector3f position) {
        // TODO: Create proper Transform object for Hytale
        // Transform.create(position, rotation)
        return position;
    }
}
