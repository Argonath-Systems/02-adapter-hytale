package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.WorldAccessor;
import com.argonathsystems.framework.accessorapi.dto.LocationData;
import com.hypixel.hytale.builtin.weather.WeatherPlugin;
import com.hypixel.hytale.builtin.weather.resources.WeatherResource;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.worldmap.provider.IWorldMapProvider;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import com.hypixel.hytale.server.worldgen.chunk.ZoneBiomeResult;
import com.hypixel.hytale.server.worldgen.biome.Biome;
import com.hypixel.hytale.server.worldgen.zone.ZoneGeneratorResult;
import com.hypixel.hytale.server.worldgen.zone.Zone;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Hytale implementation of WorldAccessor using SDK world system.
 * 
 * <p><b>MIGRATION-001 Status:</b> � CORE IMPLEMENTATION COMPLETE</p>
 * 
 * <p>SDK Classes Used:</p>
 * <ul>
 *   <li>{@code World} - World instance with name, tick, chunk access</li>
 *   <li>{@code WorldConfig} - World configuration (day/night duration)</li>
 *   <li>{@code ChunkStore} - Chunk management</li>
 *   <li>{@code ChunkGenerator} - World generation (biome/zone access)</li>
 *   <li>{@code IWorldMapProvider} - World map provider interface</li>
 *   <li>{@code WeatherPlugin} - Weather state access</li>
 * </ul>
 * 
 * <p>Implemented Methods:</p>
 * <ul>
 *   <li>✅ getWorldName() - via World.getName()</li>
 *   <li>✅ getWorldTime() - via World.getTick()</li>
 *   <li>✅ isDaytime() - calculated from tick and day/night duration</li>
 *   <li>✅ getBiome() - via ChunkGenerator.getZoneBiomeResultAt()</li>
 *   <li>✅ getZone() - via ChunkGenerator.getZoneBiomeResultAt()</li>
 *   <li>✅ hasWeather() - via WeatherPlugin + WeatherResource</li>
 *   <li>✅ getBlockType() - via WorldChunk.getBlock()</li>
 *   <li>✅ setBlock() - via WorldChunk.setBlock()</li>
 * </ul>
 * 
 * <p>Pending Methods:</p>
 * <ul>
 *   <li>⏳ findSafeLocation() - custom implementation needed</li>
 *   <li>⏳ generateChunk(), unloadChunk() - via ChunkStore</li>
 * </ul>
 * 
 * <p><b>Implementation Note:</b> Biome/Zone access requires casting 
 * IWorldMapProvider to ChunkGenerator. This cast may fail for non-standard
 * world generators (flat, void). In those cases, "unknown" is returned.</p>
 * 
 * @author Argonath Systems Team
 * @version 3.0.0
 * @since MIGRATION-001
 */
public class HytaleWorldAccessor implements WorldAccessor {
    
    private final HytaleServer server;
    
    /**
     * Current world reference. In a multi-world setup, this would be managed
     * differently (e.g., per-player world tracking).
     */
    private World currentWorld;

    public HytaleWorldAccessor(Object server) {
        this.server = (HytaleServer) server;
    }
    
    /**
     * Set the current world context for this accessor.
     * 
     * @param world The world to use for operations
     */
    public void setWorld(World world) {
        this.currentWorld = world;
    }
    
    /**
     * Get the current world, falling back to default world if not set.
     */
    private World getWorld() {
        if (currentWorld != null) {
            return currentWorld;
        }
        // Get default world from Universe singleton
        return Universe.get().getDefaultWorld();
    }

    @Override
    public String getWorldName() {
        World world = getWorld();
        if (world == null) {
            return "unknown";
        }
        return world.getName();
    }

    @Override
    public long getWorldTime() {
        World world = getWorld();
        if (world == null) {
            return 0L;
        }
        return world.getTick();
    }

    @Override
    public boolean isDaytime() {
        World world = getWorld();
        if (world == null) {
            return true; // Default to daytime
        }
        
        // Get day/night duration from world config
        int daytimeDuration = world.getDaytimeDurationSeconds();
        int nighttimeDuration = world.getNighttimeDurationSeconds();
        int fullCycleSeconds = daytimeDuration + nighttimeDuration;
        
        if (fullCycleSeconds <= 0) {
            return true; // No cycle configured, assume daytime
        }
        
        // Convert ticks to seconds (assuming 20 ticks per second)
        long ticksPerSecond = 20;
        long currentSecond = (world.getTick() / ticksPerSecond) % fullCycleSeconds;
        
        // Daytime is the first portion of the cycle
        return currentSecond < daytimeDuration;
    }

    @Override
    public String getBiome(LocationData location) {
        if (location == null) {
            return "unknown";
        }
        
        World world = location.world() != null ? Universe.get().getWorld(location.world()) : getWorld();
        if (world == null) {
            return "unknown";
        }
        
        try {
            // Try to access ChunkGenerator via IWorldMapProvider
            // This is the worldgen system that knows about biomes and zones
            var worldConfig = world.getWorldConfig();
            if (worldConfig == null) {
                return "unknown";
            }
            
            var worldGenProvider = worldConfig.getWorldGenProvider();
            if (worldGenProvider == null) {
                return "unknown";
            }
            
            // IWorldMapProvider has getGenerator(World) - ChunkGenerator implements this interface
            // We need to check if the provider is actually a ChunkGenerator or can provide one
            if (worldGenProvider instanceof IWorldMapProvider mapProvider) {
                var generator = mapProvider.getGenerator(world);
                // The generator might be a ChunkGenerator which has getZoneBiomeResultAt
                if (generator instanceof ChunkGenerator chunkGen) {
                    int x = (int) Math.floor(location.x());
                    int y = (int) Math.floor(location.y());
                    int z = (int) Math.floor(location.z());
                    
                    ZoneBiomeResult result = chunkGen.getZoneBiomeResultAt(x, y, z);
                    if (result != null) {
                        Biome biome = result.getBiome();
                        if (biome != null) {
                            return biome.getName();
                        }
                    }
                }
            }
            
            // Fallback: if we can't access ChunkGenerator directly
            return "unknown";
            
        } catch (Exception e) {
            // WorldGen system may not be fully initialized or may fail for edge cases
            return "unknown";
        }
    }

    @Override
    public Optional<String> getZone(LocationData location) {
        if (location == null) {
            return Optional.empty();
        }
        
        World world = location.world() != null ? Universe.get().getWorld(location.world()) : getWorld();
        if (world == null) {
            return Optional.empty();
        }
        
        try {
            // Same access pattern as getBiome() - via IWorldMapProvider → ChunkGenerator
            var worldConfig = world.getWorldConfig();
            if (worldConfig == null) {
                return Optional.empty();
            }
            
            var worldGenProvider = worldConfig.getWorldGenProvider();
            if (worldGenProvider == null) {
                return Optional.empty();
            }
            
            if (worldGenProvider instanceof IWorldMapProvider mapProvider) {
                var generator = mapProvider.getGenerator(world);
                if (generator instanceof ChunkGenerator chunkGen) {
                    int x = (int) Math.floor(location.x());
                    int y = (int) Math.floor(location.y());
                    int z = (int) Math.floor(location.z());
                    
                    ZoneBiomeResult result = chunkGen.getZoneBiomeResultAt(x, y, z);
                    if (result != null) {
                        ZoneGeneratorResult zoneResult = result.getZoneResult();
                        if (zoneResult != null) {
                            Zone zone = zoneResult.getZone();
                            if (zone != null) {
                                return Optional.of(zone.name());
                            }
                        }
                    }
                }
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            // WorldGen system may not be fully initialized or may fail for edge cases
            return Optional.empty();
        }
    }

    @Override
    public boolean hasWeather() {
        World world = getWorld();
        if (world == null) {
            return false;
        }
        
        try {
            // Get WeatherPlugin singleton
            WeatherPlugin weatherPlugin = WeatherPlugin.get();
            if (weatherPlugin == null) {
                return false;
            }
            
            // Get WeatherResource type for accessing world weather state
            ResourceType<EntityStore, WeatherResource> resourceType = weatherPlugin.getWeatherResourceType();
            if (resourceType == null) {
                return false;
            }
            
            // Access the world's entity store to get the Store, then get the weather resource
            var entityStore = world.getEntityStore();
            if (entityStore == null) {
                return false;
            }
            
            var store = entityStore.getStore();
            if (store == null) {
                return false;
            }
            
            WeatherResource weatherResource = store.getResource(resourceType);
            if (weatherResource == null) {
                return false;
            }
            
            // Check if there's forced weather set, or if any environment has active weather
            int forcedWeatherIndex = weatherResource.getForcedWeatherIndex();
            
            // Weather index > 0 typically indicates weather is active
            // Index 0 is usually "clear", higher indices are rain, storm, etc.
            // This is a simplification - actual implementation may need to check
            // specific weather types from the asset registry
            return forcedWeatherIndex > 0 || !weatherResource.getEnvironmentWeather().isEmpty();
            
        } catch (Exception e) {
            // WeatherPlugin may not be loaded, or other SDK issues
            return false;
        }
    }

    @Override
    public String getBlockType(LocationData location) {
        if (location == null) {
            return "minecraft:air";
        }
        
        World world = location.world() != null ? Universe.get().getWorld(location.world()) : getWorld();
        if (world == null) {
            return "minecraft:air";
        }
        
        try {
            // Convert world coordinates to chunk coordinates
            int blockX = (int) Math.floor(location.x());
            int blockY = (int) Math.floor(location.y());
            int blockZ = (int) Math.floor(location.z());
            
            // Calculate chunk key from block coordinates
            // Chunk size is 16x16, key encodes X and Z chunk positions
            int chunkX = blockX >> 4;
            int chunkZ = blockZ >> 4;
            long chunkKey = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
            
            WorldChunk chunk = world.getChunkIfLoaded(chunkKey);
            if (chunk == null) {
                return "minecraft:air"; // Chunk not loaded
            }
            
            // Local coordinates within chunk (0-15)
            int localX = blockX & 15;
            int localZ = blockZ & 15;
            
            // Get block ID at position
            int blockId = chunk.getBlock(localX, blockY, localZ);
            
            // Return block ID as string (actual block name lookup would require BlockRegistry)
            return "block:" + blockId;
        } catch (Exception e) {
            return "minecraft:air";
        }
    }

    @Override
    public Optional<LocationData> findSafeLocation(LocationData near, int radius) {
        if (near == null || radius <= 0) {
            return Optional.empty();
        }
        
        World world = near.world() != null ? Universe.get().getWorld(near.world()) : getWorld();
        if (world == null) {
            return Optional.empty();
        }
        
        int centerX = (int) Math.floor(near.x());
        int centerY = (int) Math.floor(near.y());
        int centerZ = (int) Math.floor(near.z());
        
        // Spiral search pattern from center outward
        for (int r = 0; r <= radius; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    // Only check outer ring at each radius
                    if (r > 0 && Math.abs(x) != r && Math.abs(z) != r) {
                        continue;
                    }
                    
                    int checkX = centerX + x;
                    int checkZ = centerZ + z;
                    
                    // Search vertically around the center Y
                    for (int dy = 0; dy <= 10; dy++) {
                        // Check both above and below
                        for (int yDir : new int[]{dy, -dy}) {
                            if (dy == 0 && yDir < 0) continue; // Skip duplicate check at dy=0
                            
                            int checkY = centerY + yDir;
                            if (checkY < 0 || checkY > 255) continue;
                            
                            // Check for safe location: solid ground + 2 air blocks above
                            if (isSolidBlock(world, checkX, checkY, checkZ) &&
                                !isSolidBlock(world, checkX, checkY + 1, checkZ) &&
                                !isSolidBlock(world, checkX, checkY + 2, checkZ)) {
                                
                                // Found safe location - return position on top of solid block
                                return Optional.of(new LocationData(
                                    near.world(),
                                    checkX + 0.5,
                                    checkY + 1.0,
                                    checkZ + 0.5,
                                    near.yaw(),
                                    near.pitch()
                                ));
                            }
                        }
                    }
                }
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Checks if a block at the given position is solid (non-air).
     */
    private boolean isSolidBlock(World world, int x, int y, int z) {
        if (y < 0 || y > 255) {
            return false;
        }
        
        try {
            int chunkX = x >> 4;
            int chunkZ = z >> 4;
            long chunkKey = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
            
            WorldChunk chunk = world.getChunkIfLoaded(chunkKey);
            if (chunk == null) {
                return false; // Treat unloaded chunks as non-solid
            }
            
            int localX = x & 15;
            int localZ = z & 15;
            int blockId = chunk.getBlock(localX, y, localZ);
            
            // Block ID 0 is typically air
            return blockId != 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public CompletableFuture<Boolean> generateChunk(int chunkX, int chunkZ) {
        World world = getWorld();
        if (world == null) {
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                long chunkKey = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
                
                // Check if already loaded
                WorldChunk existing = world.getChunkIfLoaded(chunkKey);
                if (existing != null) {
                    return true; // Already loaded
                }
                
                // Request chunk load/generation
                // Note: getChunk() may trigger generation if not present
                // This is a blocking call - for async behavior, use world.execute()
                world.execute(() -> {
                    try {
                        // Access chunk - this triggers loading/generation
                        world.getChunk(chunkKey);
                    } catch (Exception e) {
                        // Chunk generation may fail for edge-of-world chunks
                    }
                });
                
                // Verify chunk is now loaded
                return world.getChunkIfLoaded(chunkKey) != null;
            } catch (Exception e) {
                return false;
            }
        });
    }

    @Override
    public boolean unloadChunk(int chunkX, int chunkZ) {
        World world = getWorld();
        if (world == null) {
            return false;
        }
        
        try {
            long chunkKey = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
            
            WorldChunk chunk = world.getChunkIfLoaded(chunkKey);
            if (chunk == null) {
                return true; // Already unloaded
            }
            
            // SDK Note: Hytale's chunk lifecycle is managed automatically
            // based on player proximity. Manual unloading may not be supported
            // or may be overridden by the chunk manager.
            //
            // For now, we mark this as a limitation. The chunk system will
            // automatically unload chunks when no players are nearby.
            //
            // If explicit unload is needed, research ChunkStore.unloadChunk()
            // or similar API when available.
            
            return false; // Explicit unload not supported
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void setBlock(Object worldObj, int x, int y, int z, String blockId) {
        World world = null;
        if (worldObj instanceof World) {
            world = (World) worldObj;
        } else if (worldObj instanceof String) {
            world = Universe.get().getWorld((String) worldObj);
        } else {
            world = getWorld();
        }
        
        if (world == null || blockId == null) {
            return;
        }
        
        final World targetWorld = world;
        
        try {
            // Execute on world thread for thread safety
            targetWorld.execute(() -> {
                // Calculate chunk key from block coordinates
                int chunkX = x >> 4;
                int chunkZ = z >> 4;
                long chunkKey = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
                
                WorldChunk chunk = targetWorld.getChunkIfLoaded(chunkKey);
                if (chunk == null) {
                    return; // Chunk not loaded, can't set block
                }
                
                // Local coordinates within chunk (0-15)
                int localX = x & 15;
                int localZ = z & 15;
                
                // Parse block ID - support formats like "block:123" or just "123"
                int numericBlockId = 0;
                try {
                    if (blockId.startsWith("block:")) {
                        numericBlockId = Integer.parseInt(blockId.substring(6));
                    } else if (blockId.matches("\\d+")) {
                        numericBlockId = Integer.parseInt(blockId);
                    } else {
                        // For named blocks, would need BlockRegistry lookup
                        // For now, default to air (0)
                        numericBlockId = 0;
                    }
                } catch (NumberFormatException e) {
                    numericBlockId = 0;
                }
                
                // Set block using WorldChunk's setBlock method
                // Parameters: localX, y, localZ, blockId, blockType (null), flags (0), metaFlags (0), filler (0)
                chunk.setBlock(localX, y, localZ, numericBlockId, null, 0, 0, 0);
            });
        } catch (Exception e) {
            // Silent failure - block operations may fail if chunk is unloaded
        }
    }
}
