package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.MultiWorldAccessor;
import com.argonathsystems.framework.accessorapi.MultiWorldAccessor.GameModeType;
import com.argonathsystems.framework.accessorapi.dto.LocationData;
import com.argonathsystems.framework.accessorapi.dto.TeleportResult;
import com.argonathsystems.framework.accessorapi.dto.WorldCreateConfig;
import com.argonathsystems.framework.accessorapi.dto.WorldData;
import com.argonathsystems.framework.accessorapi.dto.WorldState;
import com.argonathsystems.framework.accessorapi.dto.WorldType;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.universe.world.worldgen.provider.DummyWorldGenProvider;
import com.hypixel.hytale.server.core.universe.world.worldgen.provider.FlatWorldGenProvider;
import com.hypixel.hytale.server.core.universe.world.worldgen.provider.IWorldGenProvider;
import com.hypixel.hytale.math.vector.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
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
        
        Universe universe = Universe.get();
        
        // Build world configuration
        WorldConfig worldConfig = new WorldConfig();
        
        // Set world generation provider based on type
        IWorldGenProvider genProvider = createWorldGenProvider(config);
        worldConfig.setWorldGenProvider(genProvider);
        
        // Apply game mode
        if (config.gameMode() != null) {
            worldConfig.setGameMode(mapGameMode(config.gameMode()));
        }
        
        // Apply world rules from config
        applyWorldRules(worldConfig, config);
        
        // Determine world path
        Path worldPath = determineWorldPath(config);
        
        // Create the world using Universe API (returns CompletableFuture)
        return universe.makeWorld(config.name(), worldPath, worldConfig)
            .thenApply(world -> {
                if (world == null) {
                    throw new RuntimeException("Universe.makeWorld returned null for: " + config.name());
                }
                
                LOGGER.info("Successfully created world: {}", config.name());
                
                return convertToWorldData(world, WorldState.ACTIVE);
            })
            .exceptionally(e -> {
                LOGGER.error("Failed to create world: {}", config.name(), e);
                throw new RuntimeException("World creation failed: " + e.getMessage(), e);
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
                
                // Remove from universe (SDK takes world name)
                universe.removeWorld(world.getName());
                
                // Delete files if requested
                if (deleteFiles) {
                    Path worldDir = Path.of("worlds", name);
                    if (Files.exists(worldDir)) {
                        deleteDirectoryRecursively(worldDir);
                        LOGGER.info("Deleted world directory: {}", worldDir);
                    }
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
        
        Universe universe = Universe.get();
        
        // loadWorld(String) handles loading from disk and adding to universe
        return universe.loadWorld(name)
            .thenApply(world -> {
                if (world == null) {
                    throw new RuntimeException("Failed to load world: " + name);
                }
                
                LOGGER.info("Successfully loaded world: {}", name);
                
                return convertToWorldData(world, WorldState.ACTIVE);
            })
            .exceptionally(e -> {
                LOGGER.error("Failed to load world: {}", name, e);
                throw new RuntimeException("World load failed: " + e.getMessage(), e);
            });
    }
    
    @Override
    public CompletableFuture<Void> unloadWorld(String name, boolean save) {
        LOGGER.info("Unloading world: {} (save={})", name, save);
        
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
                universe.removeWorld(world.getName());
                
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
        return Universe.get().getWorlds().values().stream()
            .map(world -> convertToWorldData(world, WorldState.ACTIVE))
            .collect(Collectors.toList());
    }
    
    @Override
    public Collection<WorldData> getLoadedWorlds() {
        // In Hytale SDK, all worlds in Universe.getWorlds() are loaded
        // There's no distinction between loaded and registered worlds
        return getWorlds();
    }
    
    @Override
    public Optional<WorldData> getWorld(String name) {
        return findWorld(name)
            .map(world -> convertToWorldData(world, WorldState.ACTIVE));
    }
    
    @Override
    public Optional<WorldData> getWorldByUuid(UUID uuid) {
        Universe universe = Universe.get();
        World world = universe.getWorld(uuid);
        if (world == null) {
            return Optional.empty();
        }
        return Optional.of(convertToWorldData(world, WorldState.ACTIVE));
    }
    
    @Override
    public String getDefaultWorldName() {
        Universe universe = Universe.get();
        World defaultWorld = universe.getDefaultWorld();
        return defaultWorld != null ? defaultWorld.getName() : "world";
    }
    
    @Override
    public boolean worldExists(String name) {
        return findWorld(name).isPresent();
    }
    
    @Override
    public boolean isWorldLoaded(String name) {
        return findWorld(name).isPresent();
    }
    
    @Override
    public void setSpawnLocation(String worldName, LocationData location) {
        findWorld(worldName).ifPresentOrElse(
            world -> {
                // SDK doesn't expose direct spawn setting on WorldConfig
                // This would typically be stored in world metadata and used when spawning players
                LOGGER.info("Set spawn location for world {} to ({}, {}, {})", 
                    worldName, location.x(), location.y(), location.z());
                // TODO: Store spawn location in world metadata or custom storage
                // when SDK provides proper spawn location management API
            },
            () -> LOGGER.warn("Cannot set spawn for non-existent world: {}", worldName)
        );
    }
    
    @Override
    public Optional<LocationData> getSpawnLocation(String worldName) {
        return findWorld(worldName)
            .map(world -> {
                // Get spawn location from world config if set
                WorldConfig config = world.getWorldConfig();
                // WorldConfig doesn't expose spawn directly, use (0,64,0) as default
                // In actual implementation, spawn would be stored in world metadata
                return new LocationData(
                    worldName,
                    0.0, 64.0, 0.0,  // Default spawn at world center, sea level
                    0.0f, 0.0f       // Default yaw/pitch
                );
            });
    }
    
    @Override
    public GameModeType getGameMode(String worldName) {
        return findWorld(worldName)
            .map(world -> {
                GameMode mode = world.getWorldConfig().getGameMode();
                return reverseMapGameMode(mode);
            })
            .orElse(GameModeType.ADVENTURE);
    }
    
    /**
     * Reverse map from Hytale GameMode to framework GameModeType.
     */
    private GameModeType reverseMapGameMode(GameMode mode) {
        if (mode == null) {
            return GameModeType.ADVENTURE;
        }
        return switch (mode) {
            case Creative -> GameModeType.CREATIVE;
            case Adventure -> GameModeType.ADVENTURE;
            default -> GameModeType.ADVENTURE;
        };
    }
    
    @Override
    public Optional<String> getPlayerWorld(UUID playerId) {
        Universe universe = Universe.get();
        PlayerRef playerRef = universe.getPlayer(playerId);
        if (playerRef == null) {
            return Optional.empty();
        }
        
        // Get the world UUID from the player and find the world by UUID
        UUID worldUuid = playerRef.getWorldUuid();
        if (worldUuid == null) {
            return Optional.empty();
        }
        
        // Universe.getWorld(UUID) returns the world for that UUID
        World world = universe.getWorld(worldUuid);
        if (world == null) {
            return Optional.empty();
        }
        
        return Optional.of(world.getName());
    }
    
    @Override
    public Collection<UUID> getWorldPlayers(String worldName) {
        Optional<World> worldOpt = findWorld(worldName);
        if (worldOpt.isEmpty()) {
            LOGGER.debug("World not found for getWorldPlayers: {}", worldName);
            return java.util.Collections.emptyList();
        }
        
        World world = worldOpt.get();
        
        // World has getPlayerRefs() which returns players in that specific world
        return world.getPlayerRefs().stream()
            .map(PlayerRef::getUuid)
            .collect(Collectors.toList());
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
                    return TeleportResult.failure("World not found: " + worldName);
                }
                
                World targetWorld = worldOpt.get();
                
                // Find player reference
                PlayerRef playerRef = findPlayerRef(playerId);
                if (playerRef == null) {
                    return TeleportResult.failure("Player not found");
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
                
                return TeleportResult.success(finalLocation, "Teleported successfully");
                
            } catch (Exception e) {
                LOGGER.error("Failed to teleport player {} to world {}", playerId, worldName, e);
                return TeleportResult.failure("Teleport failed: " + e.getMessage());
            }
        });
    }
    
    // ==================== World Rules ====================
    
    @Override
    public void setWorldRule(String worldName, String rule, Object value) {
        LOGGER.debug("Setting world rule {}={} for world {}", rule, value, worldName);
        
        findWorld(worldName).ifPresent(world -> {
            WorldConfig config = world.getWorldConfig();
            if (config == null) {
                LOGGER.warn("World config not available for: {}", worldName);
                return;
            }
            
            // Map string rule names to SDK config methods
            switch (rule.toLowerCase()) {
                case "pvp_enabled", "pvp" -> config.setPvpEnabled((Boolean) value);
                // Note: Fall damage is read-only in SDK - no setter available
                case "fall_damage_enabled", "fall_damage" -> 
                    LOGGER.debug("Fall damage setting not configurable via SDK");
                case "mob_spawning", "npc_spawning" -> config.setSpawningNPC((Boolean) value);
                case "npc_frozen", "freeze_npc" -> config.setIsAllNPCFrozen((Boolean) value);
                case "time_paused", "game_time_paused" -> config.setGameTimePaused((Boolean) value);
                case "block_ticking" -> config.setBlockTicking((Boolean) value);
                case "compass_updating" -> config.setCompassUpdating((Boolean) value);
                default -> LOGGER.debug("Rule {} not mapped to Hytale config", rule);
            }
        });
    }
    
    @Override
    public Optional<Object> getWorldRule(String worldName, String rule) {
        return findWorld(worldName)
            .map(world -> {
                WorldConfig config = world.getWorldConfig();
                if (config == null) {
                    return null;
                }
                
                // Map string rule names to SDK config getters
                return switch (rule.toLowerCase()) {
                    case "pvp_enabled", "pvp" -> config.isPvpEnabled();
                    case "mob_spawning", "npc_spawning" -> config.isSpawningNPC();
                    case "npc_frozen", "freeze_npc" -> config.isAllNPCFrozen();
                    case "time_paused", "game_time_paused" -> config.isGameTimePaused();
                    case "block_ticking" -> config.isBlockTicking();
                    default -> null;
                };
            });
    }
    
    @Override
    public Map<String, Object> getWorldRules(String worldName) {
        return findWorld(worldName)
            .map(world -> {
                WorldConfig config = world.getWorldConfig();
                if (config == null) {
                    return Map.<String, Object>of();
                }
                
                return Map.<String, Object>of(
                    "pvp_enabled", config.isPvpEnabled(),
                    "mob_spawning", config.isSpawningNPC(),
                    "npc_frozen", config.isAllNPCFrozen(),
                    "time_paused", config.isGameTimePaused(),
                    "block_ticking", config.isBlockTicking()
                );
            })
            .orElse(Map.of());
    }
    
    @Override
    public void setGameMode(String worldName, GameModeType gameMode) {
        LOGGER.debug("Setting game mode {} for world {}", gameMode, worldName);
        
        findWorld(worldName).ifPresent(world -> {
            WorldConfig config = world.getWorldConfig();
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
        return Universe.get().getWorlds().values().stream()
            .filter(w -> w.getName().equalsIgnoreCase(name))
            .findFirst();
    }
    
    /**
     * Convert Hytale World to WorldData DTO.
     */
    private WorldData convertToWorldData(World world, WorldState state) {
        // Determine world type based on generation provider
        WorldType type = determineWorldType(world);
        WorldConfig config = world.getWorldConfig();
        
        // Extract game mode from config (default to SURVIVAL if not set)
        String gameMode = "SURVIVAL";
        if (config != null && config.getGameMode() != null) {
            gameMode = config.getGameMode().name();
        }
        
        // Get spawn location
        LocationData spawnLocation = getWorldSpawn(world);
        
        // Get world seed
        long seed = config != null ? config.getSeed() : 0L;
        
        return new WorldData(
            world.getWorldConfig() != null ? world.getWorldConfig().getUuid() : UUID.randomUUID(),
            world.getName(),
            config != null ? config.getDisplayName() : world.getName(),
            type,
            state,
            seed,
            gameMode,
            spawnLocation,
            Map.of(), // rules - would need to extract from config
            countPlayers(world),
            Instant.now(), // createdAt - not available from SDK, use now
            Instant.now(), // lastAccessedAt
            Map.of()  // metadata
        );
    }
    
    /**
     * Determine world type from world's generation provider.
     */
    private WorldType determineWorldType(World world) {
        WorldConfig config = world.getWorldConfig();
        if (config == null) {
            return WorldType.NORMAL;
        }
        
        IWorldGenProvider provider = config.getWorldGenProvider();
        if (provider instanceof FlatWorldGenProvider) {
            return WorldType.FLAT;
        } else if (provider instanceof DummyWorldGenProvider) {
            // SDK uses DummyWorldGenProvider for void-like empty worlds
            return WorldType.VOID;
        }
        
        return WorldType.NORMAL;
    }
    
    /**
     * Create world generation provider based on world type.
     * 
     * <p>SDK providers available:
     * <ul>
     *   <li>FlatWorldGenProvider - Flat superflat worlds with configurable layers</li>
     *   <li>DummyWorldGenProvider - Empty/void worlds with no generation</li>
     * </ul>
     */
    private IWorldGenProvider createWorldGenProvider(WorldCreateConfig config) {
        return switch (config.type()) {
            case FLAT -> new FlatWorldGenProvider(); // Can configure layers
            case VOID -> new DummyWorldGenProvider(); // Empty world with no generation
            case NETHER, END -> new DummyWorldGenProvider(); // Placeholder for other dimensions
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
        // Note: Fall damage cannot be configured via SDK - isFallDamageEnabled is read-only
        // Fall damage settings may be controlled via world template or gameplay config
        
        // TODO: Apply rules from createConfig.rules() map
    }
    
    /**
     * Map game mode string to Hytale GameMode enum.
     * 
     * <p>SDK GameMode enum only supports: Adventure, Creative
     * Maps other modes appropriately:
     * <ul>
     *   <li>SURVIVAL, ADVENTURE -> Adventure</li>
     *   <li>CREATIVE -> Creative</li>
     *   <li>SPECTATOR -> Creative (no spectator in SDK)</li>
     * </ul>
     */
    private com.hypixel.hytale.protocol.GameMode mapGameMode(String gameMode) {
        if (gameMode == null) {
            return com.hypixel.hytale.protocol.GameMode.Adventure;
        }
        return switch (gameMode.toUpperCase()) {
            case "CREATIVE" -> com.hypixel.hytale.protocol.GameMode.Creative;
            case "SURVIVAL", "ADVENTURE", "SPECTATOR" -> com.hypixel.hytale.protocol.GameMode.Adventure;
            default -> {
                LOGGER.warn("Unknown game mode: {}, defaulting to Adventure", gameMode);
                yield com.hypixel.hytale.protocol.GameMode.Adventure;
            }
        };
    }
    
    /**
     * Count players in a world.
     * 
     * SDK Pattern: Iterate through Universe.getPlayers() and filter by world
     */
    private int countPlayers(World world) {
        if (world == null) {
            return 0;
        }
        try {
            // Universe.get().getPlayers() returns all connected players
            // Filter by checking each player's current world
            int count = 0;
            // Note: Actual implementation would be:
            // for (PlayerRef player : Universe.get().getPlayers()) {
            //     if (player.getWorld().equals(world)) count++;
            // }
            // For now, return 0 as player iteration API needs verification
            return count;
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Get world spawn location.
     * 
     * SDK Pattern: WorldConfig contains spawn provider/position
     */
    private LocationData getWorldSpawn(World world) {
        if (world == null) {
            return new LocationData("unknown", 0, 64, 0, 0, 0);
        }
        try {
            WorldConfig config = world.getWorldConfig();
            if (config != null) {
                // SDK provides spawn via WorldConfig
                // SpawnProvider spawnProvider = config.getSpawnProvider();
                // Vector3d spawnPos = spawnProvider.getSpawnPosition(world);
                // return new LocationData(world.getName(), spawnPos.x, spawnPos.y, spawnPos.z, 0, 0);
            }
            // Default fallback - center of world at Y=64
            return new LocationData(world.getName(), 0, 64, 0, 0, 0);
        } catch (Exception e) {
            return new LocationData(world.getName(), 0, 64, 0, 0, 0);
        }
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
    private com.hypixel.hytale.math.vector.Transform createTransform(Vector3f position) {
        return new com.hypixel.hytale.math.vector.Transform(
            position.x, position.y, position.z
        );
    }
    
    /**
     * Recursively deletes a directory and all its contents.
     * 
     * @param directory The directory to delete
     * @throws RuntimeException if deletion fails
     */
    private void deleteDirectoryRecursively(Path directory) {
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc != null) {
                        throw exc;
                    }
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOGGER.error("Failed to delete directory: {}", directory, e);
            throw new RuntimeException("Failed to delete world directory: " + directory, e);
        }
    }
}
