package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.StorageAccessor;
import com.hytale.api.Server;
import com.hytale.api.data.DataStorage;
import com.hytale.api.data.DataContainer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Hytale implementation of StorageAccessor.
 * 
 * <p>Provides persistent key-value storage using Hytale's data storage API.
 * Supports namespacing to prevent key collisions between different mods/systems.
 * 
 * <p>Data is persisted to disk by Hytale and survives server restarts.
 * Uses a hybrid approach: in-memory cache for fast reads, with periodic
 * flush to Hytale's persistent storage.
 * 
 * @author Argonath Systems Team
 * @version 1.0.0
 */
public class HytaleStorageAccessor implements StorageAccessor {
    
    private final Server server;
    
    /** In-memory cache for fast access. Maps namespace -> (key -> value) */
    private final Map<String, Map<String, Object>> cache = new ConcurrentHashMap<>();
    
    /** Tracks dirty namespaces that need to be flushed to persistent storage */
    private final Set<String> dirtyNamespaces = ConcurrentHashMap.newKeySet();
    
    /** Storage ID prefix to identify our data in Hytale's storage */
    private static final String STORAGE_PREFIX = "argonath_";

    public HytaleStorageAccessor(Server server) {
        this.server = server;
        loadAllFromPersistent();
    }

    @Override
    public void setString(String namespace, String key, String value) {
        validateParams(namespace, key);
        getOrCreateNamespace(namespace).put(key, value);
        markDirty(namespace);
    }

    @Override
    public Optional<String> getString(String namespace, String key) {
        validateParams(namespace, key);
        Object value = getNamespace(namespace).get(key);
        return value instanceof String s ? Optional.of(s) : Optional.empty();
    }

    @Override
    public void setInt(String namespace, String key, int value) {
        validateParams(namespace, key);
        getOrCreateNamespace(namespace).put(key, value);
        markDirty(namespace);
    }

    @Override
    public Optional<Integer> getInt(String namespace, String key) {
        validateParams(namespace, key);
        Object value = getNamespace(namespace).get(key);
        if (value instanceof Integer i) {
            return Optional.of(i);
        } else if (value instanceof Number n) {
            return Optional.of(n.intValue());
        }
        return Optional.empty();
    }

    @Override
    public void remove(String namespace, String key) {
        validateParams(namespace, key);
        Map<String, Object> ns = cache.get(namespace);
        if (ns != null && ns.remove(key) != null) {
            markDirty(namespace);
        }
    }

    @Override
    public boolean has(String namespace, String key) {
        validateParams(namespace, key);
        return getNamespace(namespace).containsKey(key);
    }

    @Override
    public Set<String> getKeys(String namespace) {
        validateNamespace(namespace);
        return Collections.unmodifiableSet(getNamespace(namespace).keySet());
    }

    // ========================
    // Extended Storage Methods
    // ========================

    /**
     * Sets a long value.
     */
    public void setLong(String namespace, String key, long value) {
        validateParams(namespace, key);
        getOrCreateNamespace(namespace).put(key, value);
        markDirty(namespace);
    }

    /**
     * Gets a long value.
     */
    public Optional<Long> getLong(String namespace, String key) {
        validateParams(namespace, key);
        Object value = getNamespace(namespace).get(key);
        if (value instanceof Long l) {
            return Optional.of(l);
        } else if (value instanceof Number n) {
            return Optional.of(n.longValue());
        }
        return Optional.empty();
    }

    /**
     * Sets a double value.
     */
    public void setDouble(String namespace, String key, double value) {
        validateParams(namespace, key);
        getOrCreateNamespace(namespace).put(key, value);
        markDirty(namespace);
    }

    /**
     * Gets a double value.
     */
    public Optional<Double> getDouble(String namespace, String key) {
        validateParams(namespace, key);
        Object value = getNamespace(namespace).get(key);
        if (value instanceof Double d) {
            return Optional.of(d);
        } else if (value instanceof Number n) {
            return Optional.of(n.doubleValue());
        }
        return Optional.empty();
    }

    /**
     * Sets a boolean value.
     */
    public void setBoolean(String namespace, String key, boolean value) {
        validateParams(namespace, key);
        getOrCreateNamespace(namespace).put(key, value);
        markDirty(namespace);
    }

    /**
     * Gets a boolean value.
     */
    public Optional<Boolean> getBoolean(String namespace, String key) {
        validateParams(namespace, key);
        Object value = getNamespace(namespace).get(key);
        return value instanceof Boolean b ? Optional.of(b) : Optional.empty();
    }

    /**
     * Sets a string list value.
     */
    public void setStringList(String namespace, String key, List<String> value) {
        validateParams(namespace, key);
        getOrCreateNamespace(namespace).put(key, new ArrayList<>(value));
        markDirty(namespace);
    }

    /**
     * Gets a string list value.
     */
    @SuppressWarnings("unchecked")
    public Optional<List<String>> getStringList(String namespace, String key) {
        validateParams(namespace, key);
        Object value = getNamespace(namespace).get(key);
        if (value instanceof List<?> list) {
            List<String> result = list.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .collect(Collectors.toList());
            return Optional.of(result);
        }
        return Optional.empty();
    }

    // ========================
    // Namespace Operations
    // ========================

    /**
     * Clears all data in a namespace.
     */
    public void clearNamespace(String namespace) {
        validateNamespace(namespace);
        cache.remove(namespace);
        markDirty(namespace);
    }

    /**
     * Gets all namespaces that have data.
     */
    public Set<String> getNamespaces() {
        return Collections.unmodifiableSet(cache.keySet());
    }

    /**
     * Gets the count of keys in a namespace.
     */
    public int getKeyCount(String namespace) {
        validateNamespace(namespace);
        return getNamespace(namespace).size();
    }

    // ========================
    // Persistence
    // ========================

    /**
     * Flushes all dirty data to Hytale's persistent storage.
     * Call this periodically or on server shutdown.
     */
    public void flush() {
        Set<String> toFlush = new HashSet<>(dirtyNamespaces);
        dirtyNamespaces.clear();
        
        DataStorage storage = server.getDataStorage();
        if (storage == null) {
            return;
        }
        
        for (String namespace : toFlush) {
            String storageKey = STORAGE_PREFIX + namespace;
            Map<String, Object> data = cache.get(namespace);
            
            if (data == null || data.isEmpty()) {
                // Delete empty namespaces
                storage.delete(storageKey);
            } else {
                // Save to persistent storage
                DataContainer container = storage.getOrCreate(storageKey);
                container.clear();
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    container.set(entry.getKey(), entry.getValue());
                }
                container.save();
            }
        }
    }

    /**
     * Forces an immediate save of all data.
     * Use sparingly as it may impact performance.
     */
    public void forceSave() {
        // Mark all namespaces as dirty to ensure complete save
        dirtyNamespaces.addAll(cache.keySet());
        flush();
    }

    // ========================
    // Private Helpers
    // ========================

    private void loadAllFromPersistent() {
        DataStorage storage = server.getDataStorage();
        if (storage == null) {
            return;
        }
        
        // Load all containers with our prefix
        for (String storageKey : storage.getAllKeys()) {
            if (storageKey.startsWith(STORAGE_PREFIX)) {
                String namespace = storageKey.substring(STORAGE_PREFIX.length());
                DataContainer container = storage.get(storageKey);
                
                if (container != null) {
                    Map<String, Object> namespaceData = new ConcurrentHashMap<>();
                    for (String key : container.getKeys()) {
                        namespaceData.put(key, container.get(key));
                    }
                    cache.put(namespace, namespaceData);
                }
            }
        }
    }

    private Map<String, Object> getNamespace(String namespace) {
        return cache.getOrDefault(namespace, Collections.emptyMap());
    }

    private Map<String, Object> getOrCreateNamespace(String namespace) {
        return cache.computeIfAbsent(namespace, k -> new ConcurrentHashMap<>());
    }

    private void markDirty(String namespace) {
        dirtyNamespaces.add(namespace);
    }

    private void validateParams(String namespace, String key) {
        validateNamespace(namespace);
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
    }

    private void validateNamespace(String namespace) {
        if (namespace == null || namespace.isEmpty()) {
            throw new IllegalArgumentException("Namespace cannot be null or empty");
        }
    }
}