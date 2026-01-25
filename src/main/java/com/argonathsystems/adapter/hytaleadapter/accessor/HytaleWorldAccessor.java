package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.adapter.hytaleadapter.converter.LocationConverter;
import com.argonathsystems.framework.accessorapi.WorldAccessor;
import com.argonathsystems.framework.accessorapi.dto.LocationData;
import com.hytale.api.Location;
import com.hytale.api.Server;
import com.hytale.api.block.Block;
import com.hytale.api.world.Biome;
import com.hytale.api.world.World;
import com.hytale.api.world.Zone;

import java.util.Optional;

/**
 * Hytale implementation of WorldAccessor.
 * 
 * <p>Provides access to Hytale's world state including biomes, zones,
 * time, weather, and block information. Uses thread-safe access patterns
 * as Hytale world data may require main thread access.
 * 
 * @author Argonath Systems Team
 * @version 1.0.0
 */
public class HytaleWorldAccessor implements WorldAccessor {
    
    private final Server server;
    
    /** Ticks representing daytime range (0-12000 = day, 12000-24000 = night) */
    private static final long DAY_START = 0;
    private static final long DAY_END = 12000;
    
    /** Default search parameters for safe location */
    private static final int DEFAULT_VERTICAL_SEARCH = 10;

    public HytaleWorldAccessor(Server server) {
        this.server = server;
    }

    @Override
    public String getWorldName() {
        World world = server.getDefaultWorld();
        return world != null ? world.getName() : "unknown";
    }

    @Override
    public String getBiome(LocationData location) {
        World world = getWorld(location.world());
        if (world == null) {
            return "unknown";
        }
        
        Biome biome = world.getBiomeAt(
            (int) location.x(),
            (int) location.y(),
            (int) location.z()
        );
        
        return biome != null ? biome.getId() : "unknown";
    }

    @Override
    public Optional<String> getZone(LocationData location) {
        World world = getWorld(location.world());
        if (world == null) {
            return Optional.empty();
        }
        
        Zone zone = world.getZoneAt(
            (int) location.x(),
            (int) location.y(),
            (int) location.z()
        );
        
        return zone != null ? Optional.of(zone.getId()) : Optional.empty();
    }

    @Override
    public long getWorldTime() {
        World world = server.getDefaultWorld();
        return world != null ? world.getTime() : 0;
    }

    @Override
    public boolean isDaytime() {
        long time = getWorldTime() % 24000; // Normalize to day cycle
        return time >= DAY_START && time < DAY_END;
    }

    @Override
    public boolean hasWeather() {
        World world = server.getDefaultWorld();
        if (world == null) {
            return false;
        }
        
        // Check for rain, storm, or other weather conditions
        return world.isRaining() || world.isThundering();
    }

    @Override
    public String getBlockType(LocationData location) {
        World world = getWorld(location.world());
        if (world == null) {
            return "air";
        }
        
        Block block = world.getBlockAt(
            (int) location.x(),
            (int) location.y(),
            (int) location.z()
        );
        
        return block != null ? block.getType().getId() : "air";
    }

    @Override
    public boolean isSafeLocation(LocationData location) {
        World world = getWorld(location.world());
        if (world == null) {
            return false;
        }
        
        int x = (int) location.x();
        int y = (int) location.y();
        int z = (int) location.z();
        
        // Check block at feet and head are passable
        Block feetBlock = world.getBlockAt(x, y, z);
        Block headBlock = world.getBlockAt(x, y + 1, z);
        Block groundBlock = world.getBlockAt(x, y - 1, z);
        
        if (feetBlock == null || headBlock == null || groundBlock == null) {
            return false;
        }
        
        boolean feetClear = feetBlock.getType().isPassable();
        boolean headClear = headBlock.getType().isPassable();
        boolean groundSolid = groundBlock.getType().isSolid();
        
        // Also check for hazards (lava, fire, etc.)
        boolean noHazard = !isHazardBlock(feetBlock) && !isHazardBlock(groundBlock);
        
        return feetClear && headClear && groundSolid && noHazard;
    }

    @Override
    public Optional<LocationData> findSafeLocation(LocationData near, int radius) {
        World world = getWorld(near.world());
        if (world == null) {
            return Optional.empty();
        }
        
        int centerX = (int) near.x();
        int centerY = (int) near.y();
        int centerZ = (int) near.z();
        
        // Search in expanding circles
        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    // Only check edge of current radius ring
                    if (Math.abs(dx) != r && Math.abs(dz) != r) {
                        continue;
                    }
                    
                    int x = centerX + dx;
                    int z = centerZ + dz;
                    
                    // Search vertically around the center Y
                    Optional<LocationData> safe = searchVertically(world, x, centerY, z, near.world());
                    if (safe.isPresent()) {
                        return safe;
                    }
                }
            }
        }
        
        return Optional.empty();
    }

    // ========================
    // Helper Methods
    // ========================

    /**
     * Gets a world by name.
     */
    private World getWorld(String worldName) {
        if (worldName == null || worldName.isEmpty()) {
            return server.getDefaultWorld();
        }
        return server.getWorld(worldName);
    }

    /**
     * Searches vertically for a safe location at the given x,z coordinates.
     */
    private Optional<LocationData> searchVertically(World world, int x, int centerY, int z, String worldName) {
        // Search up first, then down
        for (int dy = 0; dy <= DEFAULT_VERTICAL_SEARCH; dy++) {
            // Check up
            LocationData upLocation = new LocationData(worldName, x, centerY + dy, z);
            if (isSafeLocation(upLocation)) {
                return Optional.of(upLocation);
            }
            
            // Check down
            if (dy > 0) {
                LocationData downLocation = new LocationData(worldName, x, centerY - dy, z);
                if (isSafeLocation(downLocation)) {
                    return Optional.of(downLocation);
                }
            }
        }
        
        return Optional.empty();
    }

    /**
     * Checks if a block is a hazard (lava, fire, cactus, etc.).
     */
    private boolean isHazardBlock(Block block) {
        if (block == null) {
            return false;
        }
        
        String typeId = block.getType().getId().toLowerCase();
        return typeId.contains("lava") 
            || typeId.contains("fire") 
            || typeId.contains("cactus")
            || typeId.contains("magma")
            || typeId.contains("wither_rose");
    }

    // ========================
    // Extended Methods
    // ========================

    /**
     * Gets the current weather type.
     * 
     * @return Weather type identifier
     */
    public String getWeatherType() {
        World world = server.getDefaultWorld();
        if (world == null) {
            return "clear";
        }
        
        if (world.isThundering()) {
            return "thunder";
        } else if (world.isRaining()) {
            return "rain";
        } else {
            return "clear";
        }
    }

    /**
     * Gets the world spawn location.
     * 
     * @param worldName World to get spawn for
     * @return Spawn location, or empty if world not found
     */
    public Optional<LocationData> getSpawnLocation(String worldName) {
        World world = getWorld(worldName);
        if (world == null) {
            return Optional.empty();
        }
        
        Location spawn = world.getSpawnLocation();
        return Optional.ofNullable(LocationConverter.toDTO(spawn));
    }

    /**
     * Gets the world's difficulty level.
     * 
     * @return Difficulty identifier
     */
    public String getDifficulty() {
        World world = server.getDefaultWorld();
        if (world == null) {
            return "normal";
        }
        
        return world.getDifficulty().name().toLowerCase();
    }

    @Override
    public boolean unloadChunk(int x, int z) {
        return false;
    }

    @Override
    public java.util.concurrent.CompletableFuture<Boolean> generateChunk(int x, int z) {
        return java.util.concurrent.CompletableFuture.completedFuture(false);
    }
}