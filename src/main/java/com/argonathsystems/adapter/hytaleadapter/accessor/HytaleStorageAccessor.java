package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.StorageAccessor;
import com.argonathsystems.framework.accessorapi.data.DataValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Hytale implementation of StorageAccessor using file-based key-value storage.
 * 
 * <p>The Hytale SDK provides entity-based PlayerStorage for player data, but no
 * general-purpose key-value store. This implementation provides file-backed
 * storage with in-memory caching for framework needs.
 * 
 * <h2>Storage Strategy</h2>
 * <ul>
 *   <li>Each namespace is stored as a separate properties file</li>
 *   <li>In-memory cache reduces disk I/O for frequent reads</li>
 *   <li>Write-through caching ensures durability</li>
 *   <li>Thread-safe with read-write locks per namespace</li>
 * </ul>
 * 
 * <h2>SDK Integration Note</h2>
 * <p>For player-specific data, prefer using the PlayerRef component system
 * directly in the adapter layer when available. This accessor is for
 * cross-player or system-level storage needs.
 * 
 * <h2>Specification Reference</h2>
 * <ul>
 *   <li>SF-ARCHITECTURE-004: Data Persistence</li>
 *   <li>SF-STORAGE-001: Storage Abstraction Layer</li>
 * </ul>
 * 
 * @author Argonath Systems Team
 * @version 1.1.0
 * @since 2.1.0
 */
public class HytaleStorageAccessor implements StorageAccessor {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HytaleStorageAccessor.class);
    private static final String STORAGE_DIR = "argonath_data";
    private static final String OBJECT_STORAGE_DIR = "argonath_objects";
    private static final String FILE_EXTENSION = ".properties";
    private static final String JSON_EXTENSION = ".json";
    
    private final Object server;
    private final Path storagePath;
    private final Path objectStoragePath;
    private final Map<String, Map<String, String>> cache = new ConcurrentHashMap<>();
    private final Map<String, ReadWriteLock> namespaceLocks = new ConcurrentHashMap<>();
    private final Map<String, Boolean> dirtyNamespaces = new ConcurrentHashMap<>();
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "ArgonathStorage-AsyncWorker");
        t.setDaemon(true);
        return t;
    });
    
    public HytaleStorageAccessor(Object server) {
        this.server = server;
        
        // Determine storage path relative to server directory
        // SDK PATTERN: Universe.get().getPath() for world directory
        // For now, use working directory
        this.storagePath = Paths.get(STORAGE_DIR);
        this.objectStoragePath = Paths.get(OBJECT_STORAGE_DIR);
        
        try {
            Files.createDirectories(storagePath);
            Files.createDirectories(objectStoragePath);
            LOGGER.info("HytaleStorageAccessor initialized with storage paths: {}, {}", 
                storagePath.toAbsolutePath(), objectStoragePath.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Failed to create storage directories", e);
            throw new UnsupportedOperationException("Cannot initialize storage: " + e.getMessage(), e);
        }
    }
    
    // ==================== String Operations ====================
    
    @Override
    public void setString(String namespace, String key, String value) {
        Objects.requireNonNull(namespace, "namespace cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        
        ReadWriteLock lock = getNamespaceLock(namespace);
        lock.writeLock().lock();
        try {
            Map<String, String> namespaceData = getOrLoadNamespace(namespace);
            namespaceData.put(key, value);
            dirtyNamespaces.put(namespace, true);
            saveNamespace(namespace, namespaceData);
            
            LOGGER.trace("Set {}:{} = {}", namespace, key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public Optional<String> getString(String namespace, String key) {
        Objects.requireNonNull(namespace, "namespace cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
        
        ReadWriteLock lock = getNamespaceLock(namespace);
        lock.readLock().lock();
        try {
            Map<String, String> namespaceData = getOrLoadNamespace(namespace);
            return Optional.ofNullable(namespaceData.get(key));
        } finally {
            lock.readLock().unlock();
        }
    }
    
    // ==================== Integer Operations ====================
    
    @Override
    public void setInt(String namespace, String key, int value) {
        setString(namespace, key, String.valueOf(value));
    }
    
    @Override
    public Optional<Integer> getInt(String namespace, String key) {
        return getString(namespace, key).map(s -> {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid integer value for {}:{}: {}", namespace, key, s);
                return null;
            }
        });
    }
    
    // ==================== Long Operations ====================
    
    @Override
    public void setLong(String namespace, String key, long value) {
        setString(namespace, key, String.valueOf(value));
    }
    
    @Override
    public Optional<Long> getLong(String namespace, String key) {
        return getString(namespace, key).map(s -> {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid long value for {}:{}: {}", namespace, key, s);
                return null;
            }
        });
    }
    
    // ==================== Key Management ====================
    
    @Override
    public void remove(String namespace, String key) {
        Objects.requireNonNull(namespace, "namespace cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
        
        ReadWriteLock lock = getNamespaceLock(namespace);
        lock.writeLock().lock();
        try {
            Map<String, String> namespaceData = getOrLoadNamespace(namespace);
            if (namespaceData.remove(key) != null) {
                dirtyNamespaces.put(namespace, true);
                saveNamespace(namespace, namespaceData);
                LOGGER.trace("Removed {}:{}", namespace, key);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public boolean has(String namespace, String key) {
        Objects.requireNonNull(namespace, "namespace cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
        
        ReadWriteLock lock = getNamespaceLock(namespace);
        lock.readLock().lock();
        try {
            Map<String, String> namespaceData = getOrLoadNamespace(namespace);
            return namespaceData.containsKey(key);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public Set<String> getKeys(String namespace) {
        Objects.requireNonNull(namespace, "namespace cannot be null");
        
        ReadWriteLock lock = getNamespaceLock(namespace);
        lock.readLock().lock();
        try {
            Map<String, String> namespaceData = getOrLoadNamespace(namespace);
            return new HashSet<>(namespaceData.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    // ==================== Async Object Storage ====================
    
    @Override
    public <T> CompletableFuture<Void> saveAsync(String namespace, String key, T value, Function<T, String> serializer) {
        Objects.requireNonNull(namespace, "namespace cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        Objects.requireNonNull(serializer, "serializer cannot be null");
        
        return CompletableFuture.runAsync(() -> {
            Path namespacePath = objectStoragePath.resolve(sanitizeNamespace(namespace));
            try {
                Files.createDirectories(namespacePath);
                Path filePath = namespacePath.resolve(sanitizeKey(key) + JSON_EXTENSION);
                String content = serializer.apply(value);
                Files.writeString(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                LOGGER.trace("Saved object {}:{} to {}", namespace, key, filePath);
            } catch (IOException e) {
                LOGGER.error("Failed to save object {}:{}", namespace, key, e);
                throw new RuntimeException("Failed to save object: " + namespace + ":" + key, e);
            }
        }, asyncExecutor);
    }
    
    @Override
    public <T> CompletableFuture<Optional<T>> loadAsync(String namespace, String key, Function<String, T> deserializer) {
        Objects.requireNonNull(namespace, "namespace cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(deserializer, "deserializer cannot be null");
        
        return CompletableFuture.supplyAsync(() -> {
            Path filePath = objectStoragePath.resolve(sanitizeNamespace(namespace)).resolve(sanitizeKey(key) + JSON_EXTENSION);
            
            if (!Files.exists(filePath)) {
                LOGGER.trace("Object not found: {}:{}", namespace, key);
                return Optional.empty();
            }
            
            try {
                String content = Files.readString(filePath);
                T result = deserializer.apply(content);
                LOGGER.trace("Loaded object {}:{}", namespace, key);
                return Optional.ofNullable(result);
            } catch (IOException e) {
                LOGGER.error("Failed to load object {}:{}", namespace, key, e);
                return Optional.empty();
            } catch (Exception e) {
                LOGGER.error("Failed to deserialize object {}:{}", namespace, key, e);
                return Optional.empty();
            }
        }, asyncExecutor);
    }
    
    @Override
    public CompletableFuture<Boolean> deleteAsync(String namespace, String key) {
        Objects.requireNonNull(namespace, "namespace cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
        
        return CompletableFuture.supplyAsync(() -> {
            Path filePath = objectStoragePath.resolve(sanitizeNamespace(namespace)).resolve(sanitizeKey(key) + JSON_EXTENSION);
            
            try {
                boolean deleted = Files.deleteIfExists(filePath);
                if (deleted) {
                    LOGGER.trace("Deleted object {}:{}", namespace, key);
                }
                return deleted;
            } catch (IOException e) {
                LOGGER.error("Failed to delete object {}:{}", namespace, key, e);
                return false;
            }
        }, asyncExecutor);
    }
    
    @Override
    public CompletableFuture<Boolean> existsAsync(String namespace, String key) {
        Objects.requireNonNull(namespace, "namespace cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
        
        return CompletableFuture.supplyAsync(() -> {
            Path filePath = objectStoragePath.resolve(sanitizeNamespace(namespace)).resolve(sanitizeKey(key) + JSON_EXTENSION);
            return Files.exists(filePath);
        }, asyncExecutor);
    }
    
    @Override
    public CompletableFuture<List<String>> findKeysAsync(String namespace, String keyPrefix) {
        Objects.requireNonNull(namespace, "namespace cannot be null");
        Objects.requireNonNull(keyPrefix, "keyPrefix cannot be null");
        
        return CompletableFuture.supplyAsync(() -> {
            Path namespacePath = objectStoragePath.resolve(sanitizeNamespace(namespace));
            
            if (!Files.exists(namespacePath)) {
                return List.of();
            }
            
            String sanitizedPrefix = sanitizeKey(keyPrefix);
            try (var stream = Files.list(namespacePath)) {
                return stream
                    .filter(p -> p.getFileName().toString().startsWith(sanitizedPrefix))
                    .filter(p -> p.toString().endsWith(JSON_EXTENSION))
                    .map(p -> {
                        String name = p.getFileName().toString();
                        return name.substring(0, name.length() - JSON_EXTENSION.length());
                    })
                    .collect(Collectors.toList());
            } catch (IOException e) {
                LOGGER.error("Failed to find keys in namespace: {}", namespace, e);
                return List.of();
            }
        }, asyncExecutor);
    }
    
    // ==================== Internal Storage Operations ====================
    
    private ReadWriteLock getNamespaceLock(String namespace) {
        return namespaceLocks.computeIfAbsent(namespace, k -> new ReentrantReadWriteLock());
    }
    
    private Map<String, String> getOrLoadNamespace(String namespace) {
        return cache.computeIfAbsent(namespace, this::loadFromDisk);
    }
    
    private Map<String, String> loadFromDisk(String namespace) {
        Path filePath = storagePath.resolve(sanitizeNamespace(namespace) + FILE_EXTENSION);
        Map<String, String> data = new ConcurrentHashMap<>();
        
        if (Files.exists(filePath)) {
            Properties props = new Properties();
            try (InputStream is = Files.newInputStream(filePath)) {
                props.load(is);
                for (String key : props.stringPropertyNames()) {
                    data.put(key, props.getProperty(key));
                }
                LOGGER.debug("Loaded {} keys from namespace: {}", data.size(), namespace);
            } catch (IOException e) {
                LOGGER.error("Failed to load namespace: {}", namespace, e);
            }
        } else {
            LOGGER.debug("No existing data for namespace: {}", namespace);
        }
        
        return data;
    }
    
    private void saveNamespace(String namespace, Map<String, String> data) {
        Path filePath = storagePath.resolve(sanitizeNamespace(namespace) + FILE_EXTENSION);
        
        Properties props = new Properties();
        data.forEach(props::setProperty);
        
        try (OutputStream os = Files.newOutputStream(filePath, 
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            props.store(os, "Argonath Storage - " + namespace);
            dirtyNamespaces.put(namespace, false);
            LOGGER.trace("Saved namespace {} with {} keys", namespace, data.size());
        } catch (IOException e) {
            LOGGER.error("Failed to save namespace: {}", namespace, e);
        }
    }
    
    private String sanitizeNamespace(String namespace) {
        // Convert namespace to safe filename
        return namespace.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
    
    private String sanitizeKey(String key) {
        // Convert key to safe filename, replacing special chars
        return key.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }
    
    // ==================== Lifecycle Management ====================
    
    /**
     * Flushes all dirty namespaces to disk.
     * Call this periodically or on shutdown.
     */
    public void flushAll() {
        for (Map.Entry<String, Boolean> entry : dirtyNamespaces.entrySet()) {
            if (entry.getValue()) {
                String namespace = entry.getKey();
                ReadWriteLock lock = getNamespaceLock(namespace);
                lock.writeLock().lock();
                try {
                    Map<String, String> data = cache.get(namespace);
                    if (data != null) {
                        saveNamespace(namespace, data);
                    }
                } finally {
                    lock.writeLock().unlock();
                }
            }
        }
        LOGGER.info("Flushed all storage namespaces");
    }
    
    /**
     * Clears the in-memory cache (data remains on disk).
     */
    public void clearCache() {
        cache.clear();
        LOGGER.debug("Storage cache cleared");
    }
    
    /**
     * Deletes a namespace entirely (both cache and disk).
     */
    public void deleteNamespace(String namespace) {
        Objects.requireNonNull(namespace, "namespace cannot be null");
        
        ReadWriteLock lock = getNamespaceLock(namespace);
        lock.writeLock().lock();
        try {
            cache.remove(namespace);
            dirtyNamespaces.remove(namespace);
            
            Path filePath = storagePath.resolve(sanitizeNamespace(namespace) + FILE_EXTENSION);
            try {
                Files.deleteIfExists(filePath);
                LOGGER.info("Deleted namespace: {}", namespace);
            } catch (IOException e) {
                LOGGER.error("Failed to delete namespace file: {}", namespace, e);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Returns all namespace names that have been used.
     */
    public Set<String> getAllNamespaces() {
        Set<String> namespaces = new HashSet<>();
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(storagePath, "*" + FILE_EXTENSION)) {
            for (Path path : stream) {
                String filename = path.getFileName().toString();
                namespaces.add(filename.substring(0, filename.length() - FILE_EXTENSION.length()));
            }
        } catch (IOException e) {
            LOGGER.error("Failed to list namespaces", e);
        }
        
        return namespaces;
    }
}
