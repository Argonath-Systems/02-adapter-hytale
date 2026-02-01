package com.argonathsystems.adapter.hytale.util;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache for PlayerRef instances to avoid repeated lookups.
 * 
 * <p><b>MIGRATION-001 Status:</b> ✅ IMPLEMENTED</p>
 * 
 * <p>The official Hytale SDK uses PlayerRef instead of direct Player objects.
 * This cache maintains mappings from UUID to PlayerRef for efficient access.</p>
 * 
 * <h2>ECS Pattern Compliance:</h2>
 * PlayerRef is the ECS-safe way to reference players. Store refs, not entities.
 * 
 * <h2>Thread Safety:</h2>
 * Uses ConcurrentHashMap for thread-safe cache operations.
 * 
 * @author Argonath Systems
 * @version 3.0.0
 * @since MIGRATION-001
 */
public class PlayerRefCache {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerRefCache.class);
    
    private final JavaPlugin plugin;
    private final ConcurrentHashMap<UUID, PlayerRef> cache = new ConcurrentHashMap<>();
    
    /**
     * Create a new PlayerRefCache.
     * 
     * @param plugin The plugin instance for server access
     */
    public PlayerRefCache(JavaPlugin plugin) {
        this.plugin = plugin;
        LOGGER.debug("PlayerRefCache initialized");
    }
    
    /**
     * Get a PlayerRef for the given UUID.
     * Caches the result for future lookups.
     * 
     * @param uuid Player UUID
     * @return PlayerRef or null if player not found/offline
     */
    public PlayerRef get(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        
        // Check cache first
        PlayerRef cached = cache.get(uuid);
        if (cached != null && cached.isValid()) {
            return cached;
        }
        
        // Remove invalid entry if present
        if (cached != null) {
            cache.remove(uuid);
        }
        
        // Lookup from Universe
        PlayerRef playerRef = Universe.get().getPlayer(uuid);
        if (playerRef != null && playerRef.isValid()) {
            cache.put(uuid, playerRef);
            LOGGER.trace("Cached PlayerRef for UUID: {}", uuid);
            return playerRef;
        }
        
        return null;
    }
    
    /**
     * Get or create a PlayerRef entry.
     * Same as get() but with clearer semantics for cache usage.
     * 
     * @param uuid Player UUID
     * @return PlayerRef or null if player not found
     */
    public PlayerRef getOrCreate(UUID uuid) {
        return get(uuid);
    }
    
    /**
     * Check if a player is cached and valid.
     * 
     * @param uuid Player UUID
     * @return true if cached and valid
     */
    public boolean isCached(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        PlayerRef ref = cache.get(uuid);
        return ref != null && ref.isValid();
    }
    
    /**
     * Refresh a cached PlayerRef.
     * Forces a fresh lookup from Universe.
     * 
     * @param uuid Player UUID
     * @return Fresh PlayerRef or null
     */
    public PlayerRef refresh(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        
        cache.remove(uuid);
        return get(uuid);
    }
    
    /**
     * Invalidate a PlayerRef from the cache.
     * Call this when a player disconnects.
     * 
     * @param uuid Player UUID
     */
    public void invalidate(UUID uuid) {
        if (uuid != null) {
            cache.remove(uuid);
            LOGGER.trace("Invalidated PlayerRef cache for UUID: {}", uuid);
        }
    }
    
    /**
     * Clear all cached PlayerRefs.
     */
    public void clear() {
        int size = cache.size();
        cache.clear();
        LOGGER.debug("Cleared PlayerRefCache ({} entries)", size);
    }
    
    /**
     * Remove all invalid entries from the cache.
     * Call periodically to clean up stale references.
     * 
     * @return Number of entries removed
     */
    public int cleanupInvalid() {
        int removed = 0;
        var iterator = cache.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue() == null || !entry.getValue().isValid()) {
                iterator.remove();
                removed++;
            }
        }
        if (removed > 0) {
            LOGGER.debug("Cleaned up {} invalid PlayerRef entries", removed);
        }
        return removed;
    }
    
    /**
     * Get the current cache size.
     * 
     * @return Number of cached PlayerRefs
     */
    public int size() {
        return cache.size();
    }
}
