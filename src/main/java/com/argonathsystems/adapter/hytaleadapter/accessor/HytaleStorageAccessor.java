package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.StorageAccessor;
import com.argonathsystems.framework.accessorapi.data.DataValue;

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
/**
 * <p><b>MIGRATION-001 Status:</b> BLOCKED - Requires Official Hytale SDK</p>
 * 
 * <p>This accessor implements Data persistence functionality.</p>
 * <p>Implementation requires the official Hytale SDK (com.hypixel.hytale.*)
 * which is not available in the development environment.</p>
 * 
 * <p>All methods throw UnsupportedOperationException until the SDK is available.</p>
 * 
 * @see <a href="file://../../../docs/migration-001/PHASE-3-IMPLEMENTATION-STATUS.md">Phase 3 Status</a>
 */
public class HytaleStorageAccessor implements StorageAccessor {
    
    private final Object /* Server */ server;
    
    /** In-memory cache for fast access. Maps namespace -> (key -> value) */
    private final Map<String, Map<String, DataValue>> cache = new ConcurrentHashMap<>();
    
    /** Tracks dirty namespaces that need to be flushed to persistent storage */
    private final Set<String> dirtyNamespaces = ConcurrentHashMap.newKeySet();
    
    /** Storage ID prefix to identify our data in Hytale's storage */
    private static final String STORAGE_PREFIX = "argonath_";

    public HytaleStorageAccessor(Object /* Server */ server) {
        this.server = server;
        loadAllFromPersistent();
    }

    @Override
    public void setString(String namespace, String key, String value) {
        validateParams(namespace, key);
        getOrCreateNamespace(namespace).put(key, DataValue.of(value));
        markDirty(namespace);
    }

    @Override
    public Optional<String> getString(String namespace, String key) {
        validateParams(namespace, key);
        DataValue value = getNamespace(namespace).get(key);
        return value != null ? value.asString() : Optional.empty();
    }

    @Override
    public void setInt(String namespace, String key, int value) {
        validateParams(namespace, key);
        getOrCreateNamespace(namespace).put(key, DataValue.of(value));
        markDirty(namespace);
    }

    @Override
    public Optional<Integer> getInt(String namespace, String key) {
        validateParams(namespace, key);
        DataValue value = getNamespace(namespace).get(key);
        return value != null ? value.asInt() : Optional.empty();
    }

    @Override
    public void remove(String namespace, String key) {
        validateParams(namespace, key);
        Map<String, DataValue> ns = cache.get(namespace);
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
        getOrCreateNamespace(namespace).put(key, DataValue.of(value));
        markDirty(namespace);
    }

    /**
     * Gets a long value.
     */
    public Optional<Long> getLong(String namespace, String key) {
        validateParams(namespace, key);
        DataValue value = getNamespace(namespace).get(key);
        return value != null ? value.asLong() : Optional.empty();
    }

    /**
     * Sets a double value.
     */
    public void setDouble(String namespace, String key, double value) {
        validateParams(namespace, key);
        getOrCreateNamespace(namespace).put(key, DataValue.of(value));
        markDirty(namespace);
    }

    /**
     * Gets a double value.
     */
    public Optional<Double> getDouble(String namespace, String key) {
        validateParams(namespace, key);
        DataValue value = getNamespace(namespace).get(key);
        return value != null ? value.asDouble() : Optional.empty();
    }

    /**
     * Sets a boolean value.
     */
    public void setBoolean(String namespace, String key, boolean value) {
        validateParams(namespace, key);
        getOrCreateNamespace(namespace).put(key, DataValue.of(value));
        markDirty(namespace);
    }

    /**
     * Gets a boolean value.
     */
    public Optional<Boolean> getBoolean(String namespace, String key) {
        validateParams(namespace, key);
        DataValue value = getNamespace(namespace).get(key);
        return value != null ? value.asBool() : Optional.empty();
    }

    /**
     * Sets a string list value.
     */
    public void setStringList(String namespace, String key, List<String> value) {
        validateParams(namespace, key);
        List<DataValue> dataValues = value.stream()
            .map(DataValue::of)
            .collect(Collectors.toList());
        getOrCreateNamespace(namespace).put(key, DataValue.of(dataValues));
        markDirty(namespace);
    }

    /**
     * Gets a string list value.
     */
    public Optional<List<String>> getStringList(String namespace, String key) {
        validateParams(namespace, key);
        DataValue value = getNamespace(namespace).get(key);
        if (value != null) {
            return value.asList().map(list ->
                list.stream()
                    .map(DataValue::asString)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList())
            );
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
            Map<String, DataValue> data = cache.get(namespace);
            
            if (data == null || data.isEmpty()) {
                // Delete empty namespaces
                storage.delete(storageKey);
            } else {
                // Save to persistent storage - convert DataValue to platform Object
                DataContainer container = storage.getOrCreate(storageKey);
                container.clear();
                for (Map.Entry<String, DataValue> entry : data.entrySet()) {
                    container.set(entry.getKey(), convertToObject(entry.getValue()));
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
                    Map<String, DataValue> namespaceData = new ConcurrentHashMap<>();
                    for (String key : container.getKeys()) {
                        namespaceData.put(key, convertFromObject(container.get(key)));
                    }
                    cache.put(namespace, namespaceData);
                }
            }
        }
    }

    private Map<String, DataValue> getNamespace(String namespace) {
        return cache.getOrDefault(namespace, Collections.emptyMap());
    }

    private Map<String, DataValue> getOrCreateNamespace(String namespace) {
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

    /**
     * Converts DataValue to platform Object for persistence.
     */
    private Object convertToObject(DataValue dataValue) {
        return switch (dataValue) {
            case DataValue.StringValue(String value) -> value;
            case DataValue.IntValue(int value) -> value;
            case DataValue.LongValue(long value) -> value;
            case DataValue.DoubleValue(double value) -> value;
            case DataValue.BoolValue(boolean value) -> value;
            case DataValue.ListValue(List<DataValue> list) ->
                list.stream().map(this::convertToObject).collect(Collectors.toList());
            case DataValue.MapValue(Map<String, DataValue> map) -> {
                Map<String, Object> result = new HashMap<>();
                map.forEach((k, v) -> result.put(k, convertToObject(v)));
                yield result;
            }
        };
    }

    /**
     * Converts platform Object to DataValue.
     */
    @SuppressWarnings("unchecked")
    private DataValue convertFromObject(Object obj) {
        if (obj instanceof String s) return DataValue.of(s);
        if (obj instanceof Integer i) return DataValue.of(i);
        if (obj instanceof Long l) return DataValue.of(l);
        if (obj instanceof Double d) return DataValue.of(d);
        if (obj instanceof Boolean b) return DataValue.of(b);
        if (obj instanceof List<?> list) {
            List<DataValue> values = list.stream()
                .map(this::convertFromObject)
                .collect(Collectors.toList());
            return DataValue.of(values);
        }
        if (obj instanceof Map<?, ?> map) {
            Map<String, DataValue> values = new HashMap<>();
            map.forEach((k, v) -> {
                if (k instanceof String key) {
                    values.put(key, convertFromObject(v));
                }
            });
            return DataValue.of(values);
        }
        // Fallback for unknown types - convert to string
        return DataValue.of(obj.toString());
    }
}