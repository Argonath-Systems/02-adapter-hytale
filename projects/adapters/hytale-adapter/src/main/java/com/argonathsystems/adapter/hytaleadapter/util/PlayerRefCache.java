package com.argonathsystems.adapter.hytaleadapter.util;

import com.hytale.api.entity.Player;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerRefCache {
    private static final Map<UUID, Player> cache = new ConcurrentHashMap<>();

    public static void add(Player player) {
        // cache.put(player.getUniqueId(), player);
    }

    public static void remove(UUID uuid) {
        cache.remove(uuid);
    }

    public static Player get(UUID uuid) {
        return cache.get(uuid);
    }

    public static void clear() {
        cache.clear();
    }
}