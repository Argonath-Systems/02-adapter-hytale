package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.BlockAccessor;
import com.argonathsystems.framework.accessorapi.dto.LocationData;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Hytale implementation of BlockAccessor using SDK block/voxel system.
 * 
 * <p>Provides platform-specific implementation for block operations:
 * <ul>
 *   <li>Block type queries and modifications</li>
 *   <li>Container detection (chests, barrels, etc.)</li>
 *   <li>Spatial queries for container locations</li>
 * </ul>
 * 
 * <h2>SDK Classes Used</h2>
 * <ul>
 *   <li>{@code World} - World instance with chunk access</li>
 *   <li>{@code WorldChunk} - Chunk containing blocks</li>
 *   <li>{@code Block} - Individual block data</li>
 * </ul>
 * 
 * <h2>Container Types</h2>
 * The following block types are recognized as containers:
 * <ul>
 *   <li>chest - Standard chest (27 slots)</li>
 *   <li>large_chest - Double chest (54 slots)</li>
 *   <li>barrel - Barrel container (27 slots)</li>
 *   <li>shulker_box - Shulker box (27 slots)</li>
 *   <li>ender_chest - Player-specific ender chest (27 slots)</li>
 * </ul>
 * 
 * @author Argonath Systems Team
 * @version 1.0.0
 * @since 2.4.0
 */
public class HytaleBlockAccessor implements BlockAccessor {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HytaleBlockAccessor.class);
    
    private static final Set<String> CONTAINER_TYPES = Set.of(
        "chest", "large_chest", "barrel", "shulker_box", "ender_chest",
        "trapped_chest", "hopper", "dispenser", "dropper",
        "furnace", "blast_furnace", "smoker"
    );
    
    private static final Map<String, Integer> CONTAINER_SIZES = Map.ofEntries(
        Map.entry("chest", 27),
        Map.entry("large_chest", 54),
        Map.entry("barrel", 27),
        Map.entry("shulker_box", 27),
        Map.entry("ender_chest", 27),
        Map.entry("trapped_chest", 27),
        Map.entry("hopper", 5),
        Map.entry("dispenser", 9),
        Map.entry("dropper", 9),
        Map.entry("furnace", 3),
        Map.entry("blast_furnace", 3),
        Map.entry("smoker", 3)
    );
    
    private final HytaleServer server;
    
    public HytaleBlockAccessor(Object server) {
        this.server = (HytaleServer) server;
    }
    
    // ============================================================
    // Legacy API (coordinate-only, uses default world)
    // ============================================================
    
    @Override
    public String getBlockType(int x, int y, int z) {
        World world = getDefaultWorld();
        if (world == null) {
            return "air";
        }
        return getBlockType(world.getName(), x, y, z);
    }
    
    @Override
    public void setBlockType(int x, int y, int z, String blockType) {
        World world = getDefaultWorld();
        if (world != null) {
            setBlock(world.getName(), x, y, z, blockType);
        }
    }
    
    @Override
    public void breakBlock(int x, int y, int z) {
        World world = getDefaultWorld();
        if (world != null) {
            setBlock(world.getName(), x, y, z, "air");
        }
    }
    
    // ============================================================
    // World-Aware API
    // ============================================================
    
    @Override
    public String getBlockType(String worldId, int x, int y, int z) {
        World world = getWorld(worldId);
        if (world == null) {
            LOGGER.warn("World not found: {}", worldId);
            return "air";
        }
        
        try {
            // Get chunk index (packed long from x,z coordinates)
            int chunkX = x >> 4;
            int chunkZ = z >> 4;
            long chunkIndex = packChunkIndex(chunkX, chunkZ);
            
            // Get chunk from world using packed index
            WorldChunk chunk = world.getChunk(chunkIndex);
            if (chunk == null) {
                return "air";
            }
            
            // Get block type using BlockAccessor interface
            // Local coordinates within chunk (0-15)
            int localX = x & 15;
            int localZ = z & 15;
            
            var blockType = chunk.getBlockType(localX, y, localZ);
            if (blockType != null) {
                // BlockType uses getId() for the identifier
                return (String) blockType.getId();
            }
            return "air";
        } catch (Exception e) {
            LOGGER.debug("Failed to get block type at ({}, {}, {}): {}", x, y, z, e.getMessage());
            return "air";
        }
    }
    
    @Override
    public boolean setBlock(String worldId, int x, int y, int z, String blockType) {
        World world = getWorld(worldId);
        if (world == null) {
            LOGGER.warn("World not found: {}", worldId);
            return false;
        }
        
        try {
            // Get chunk index (packed long from x,z coordinates)
            int chunkX = x >> 4;
            int chunkZ = z >> 4;
            long chunkIndex = packChunkIndex(chunkX, chunkZ);
            
            // Get chunk from world using packed index
            WorldChunk chunk = world.getChunk(chunkIndex);
            if (chunk == null) {
                LOGGER.warn("Chunk not loaded at ({}, {})", chunkX, chunkZ);
                return false;
            }
            
            // Local coordinates within chunk (0-15)
            int localX = x & 15;
            int localZ = z & 15;
            
            // Use BlockAccessor.setBlock(x, y, z, blockTypeString) method
            return chunk.setBlock(localX, y, localZ, blockType);
        } catch (Exception e) {
            LOGGER.debug("Failed to set block at ({}, {}, {}): {}", x, y, z, e.getMessage());
            return false;
        }
    }
    
    /**
     * Pack chunk X,Z coordinates into a single long index.
     * SDK uses packed long for chunk lookup.
     */
    private long packChunkIndex(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }
    
    // ============================================================
    // Container Operations
    // ============================================================
    
    @Override
    public boolean isContainer(String worldId, int x, int y, int z) {
        // TODO: Implement when block type query is available
        // For now, check against known container types
        String blockType = getBlockTypeInternal(worldId, x, y, z);
        return CONTAINER_TYPES.contains(blockType);
    }
    
    @Override
    public Optional<String> getContainerType(String worldId, int x, int y, int z) {
        String blockType = getBlockTypeInternal(worldId, x, y, z);
        if (CONTAINER_TYPES.contains(blockType)) {
            return Optional.of(blockType);
        }
        return Optional.empty();
    }
    
    @Override
    public int getContainerSize(String worldId, int x, int y, int z) {
        String blockType = getBlockTypeInternal(worldId, x, y, z);
        return CONTAINER_SIZES.getOrDefault(blockType, 0);
    }
    
    @Override
    public List<LocationData> getContainersInChunk(String worldId, int chunkX, int chunkZ) {
        // TODO: Implement chunk scanning when block iteration API is available
        // SDK PATTERN: Iterate blocks in WorldChunk, check for container types
        LOGGER.debug("getContainersInChunk called for world={}, chunk=({}, {})", worldId, chunkX, chunkZ);
        return List.of(); // Return empty until SDK support
    }
    
    @Override
    public List<LocationData> getContainersInRadius(String worldId, int centerX, int centerY, int centerZ, int radius) {
        // TODO: Implement spatial search when block API is available
        // This could be optimized with spatial indexing
        LOGGER.debug("getContainersInRadius called for world={}, center=({}, {}, {}), radius={}", 
            worldId, centerX, centerY, centerZ, radius);
        return List.of(); // Return empty until SDK support
    }
    
    @Override
    public Set<String> getContainerTypes() {
        return Collections.unmodifiableSet(CONTAINER_TYPES);
    }
    
    @Override
    public boolean isAir(String worldId, int x, int y, int z) {
        String blockType = getBlockTypeInternal(worldId, x, y, z);
        return "air".equals(blockType) || "void_air".equals(blockType) || "cave_air".equals(blockType);
    }
    
    @Override
    public boolean isSolid(String worldId, int x, int y, int z) {
        // TODO: Implement solid block detection via SDK
        // SDK PATTERN: Block.isSolid() or similar property
        String blockType = getBlockTypeInternal(worldId, x, y, z);
        // For now, assume non-air blocks are solid (simplistic)
        return !isAir(worldId, x, y, z);
    }
    
    // ============================================================
    // Internal Helpers
    // ============================================================
    
    /**
     * Internal method that returns a fallback value instead of throwing.
     * Used for container checks - now uses SDK block API.
     */
    private String getBlockTypeInternal(String worldId, int x, int y, int z) {
        try {
            return getBlockType(worldId, x, y, z);
        } catch (Exception e) {
            LOGGER.trace("getBlockTypeInternal failed for ({}, {}, {}): {}", x, y, z, e.getMessage());
            return "unknown";
        }
    }
    
    private World getDefaultWorld() {
        // SDK PATTERN: Get default world from Universe singleton
        return Universe.get().getDefaultWorld();
    }
    
    private World getWorld(String worldId) {
        if (worldId == null || worldId.isEmpty()) {
            return getDefaultWorld();
        }
        
        // Try to find world by name in all loaded worlds
        var worldsMap = Universe.get().getWorlds();
        for (var entry : worldsMap.entrySet()) {
            World world = entry.getValue();
            if (worldId.equals(world.getName())) {
                return world;
            }
        }
        
        // Try UUID-based lookup if worldId looks like a UUID
        try {
            java.util.UUID uuid = java.util.UUID.fromString(worldId);
            World world = Universe.get().getWorld(uuid);
            if (world != null) {
                return world;
            }
        } catch (IllegalArgumentException ignored) {
            // Not a UUID, ignore
        }
        
        // Fallback to default world
        World defaultWorld = getDefaultWorld();
        if (defaultWorld != null && worldId.equals(defaultWorld.getName())) {
            return defaultWorld;
        }
        
        LOGGER.debug("World not found: {}, using default", worldId);
        return defaultWorld;
    }
}
