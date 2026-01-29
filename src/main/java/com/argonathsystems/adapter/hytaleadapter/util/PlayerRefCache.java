package com.argonathsystems.adapter.hytaleadapter.util;

import com.hytale.api.entity.Player;
import com.hytale.api.entity.PlayerRef;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerRefCache {
    private static final Map<UUID, Player> cache = new ConcurrentHashMap<>();
    private static final Map<UUID, PlayerRef> refCache = new ConcurrentHashMap<>();

    public static void add(Player player) {
        cache.put(player.getUniqueId(), player);
    }
    
    public static void addRef(UUID playerId, PlayerRef ref) {
        refCache.put(playerId, ref);
    }

    public static void remove(UUID uuid) {
        cache.remove(uuid);
        refCache.remove(uuid);
    }

    public static Player get(UUID uuid) {
        return cache.get(uuid);
    }
    
    /**
     * Gets the PlayerRef for a player.
     * Returns null if the player is not cached.
     * 
     * @param playerId The player's UUID
     * @return PlayerRef or null if not found
     */
    public static PlayerRef getRef(UUID playerId) {
        return refCache.get(playerId);
    }

    public static void clear() {
        cache.clear();
        refCache.clear();
    }
}