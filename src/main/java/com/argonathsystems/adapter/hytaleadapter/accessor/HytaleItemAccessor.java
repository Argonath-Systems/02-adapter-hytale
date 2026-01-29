package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.adapter.hytaleadapter.converter.ItemDataConverter;
import com.argonathsystems.framework.accessorapi.ItemAccessor;
import com.argonathsystems.framework.accessorapi.dto.ItemData;
import com.argonathsystems.framework.accessorapi.dto.ItemDefinitionData;
import com.argonathsystems.framework.accessorapi.dto.ItemDefinitionData.ItemRarity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Hytale implementation of ItemAccessor.
 * 
 * <p>Provides access to Hytale's item registry and allows creating
 * platform-agnostic ItemData instances from Hytale ItemStacks.
 * 
 * <p>Caches item definitions on first access for performance.
 * 
 * @author Argonath Systems Team
 * @version 1.0.0
 */
/**
 * <p><b>MIGRATION-001 Status:</b> BLOCKED - Requires Official Hytale SDK</p>
 * 
 * <p>This accessor implements Item creation functionality.</p>
 * <p>Implementation requires the official Hytale SDK (com.hypixel.hytale.*)
 * which is not available in the development environment.</p>
 * 
 * <p>All methods throw UnsupportedOperationException until the SDK is available.</p>
 * 
 * @see <a href="file://../../../docs/migration-001/PHASE-3-IMPLEMENTATION-STATUS.md">Phase 3 Status</a>
 */
public class HytaleItemAccessor implements ItemAccessor {
    
    private final Object /* Server */ server;
    
    /** Cache of item definitions - populated lazily from Hytale registry */
    private final Map<String, ItemDefinitionData> definitionCache = new ConcurrentHashMap<>();
    
    /** Cache of items by tag for faster lookups */
    private final Map<String, Set<String>> tagToItemsCache = new ConcurrentHashMap<>();
    
    /** Whether the cache has been initialized */
    private volatile boolean cacheInitialized = false;

    public HytaleItemAccessor(Object /* Server */ server) {
        this.server = server;
    }

    @Override
    public Optional<ItemDefinitionData> getItemDefinition(String itemId) {
        ensureCacheInitialized();
        return Optional.ofNullable(definitionCache.get(itemId));
    }

    @Override
    public Collection<ItemDefinitionData> getAllItemDefinitions() {
        ensureCacheInitialized();
        return Collections.unmodifiableCollection(definitionCache.values());
    }

    @Override
    public Collection<ItemDefinitionData> getItemsByTag(String tag) {
        ensureCacheInitialized();
        Set<String> itemIds = tagToItemsCache.getOrDefault(tag, Collections.emptySet());
        return itemIds.stream()
            .map(definitionCache::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Override
    public Set<String> getItemTags(String itemId) {
        ensureCacheInitialized();
        ItemDefinitionData def = definitionCache.get(itemId);
        return def != null ? def.tags() : Collections.emptySet();
    }

    @Override
    public ItemData createItem(String itemId, int amount) {
        // Validate item exists
        if (!itemExists(itemId)) {
            throw new IllegalArgumentException("Unknown item type: " + itemId);
        }
        
        // Create Hytale Object /* ItemStack */ and convert to our DTO
        Object /* ItemStack */ hytaleStack = server.createItemStack(itemId, amount);
        return ItemDataConverter.toDTO(hytaleStack);
    }

    // ========================
    // Cache Initialization
    // ========================

    /**
     * Ensures the definition cache is populated from Hytale's registry.
     * Thread-safe, only initializes once.
     */
    private void ensureCacheInitialized() {
        if (cacheInitialized) {
            return;
        }
        
        synchronized (this) {
            if (cacheInitialized) {
                return;
            }
            
            initializeCache();
            cacheInitialized = true;
        }
    }

    /**
     * Populates the cache from Hytale's item registry.
     */
    private void initializeCache() {
        ItemRegistry registry = server.getItemRegistry();
        if (registry == null) {
            return;
        }
        
        for (ItemType itemType : registry.getAllItems()) {
            String itemId = itemType.getId();
            ItemDefinitionData definition = convertToDefinition(itemType);
            
            definitionCache.put(itemId, definition);
            
            // Index by tags
            for (String tag : definition.tags()) {
                tagToItemsCache.computeIfAbsent(tag, k -> ConcurrentHashMap.newKeySet())
                    .add(itemId);
            }
        }
    }

    /**
     * Converts a Hytale ItemType to our platform-agnostic ItemDefinitionData.
     */
    private ItemDefinitionData convertToDefinition(ItemType itemType) {
        return new ItemDefinitionData(
            itemType.getId(),
            itemType.getDisplayName(),
            itemType.getDescription(),
            itemType.getMaxStackSize(),
            mapRarity(itemType.getRarity()),
            new HashSet<>(itemType.getTags())
        );
    }

    /**
     * Maps Hytale's rarity to our platform-agnostic ItemRarity enum.
     */
    private ItemRarity mapRarity(String hytaleRarity) {
        if (hytaleRarity == null) {
            return ItemRarity.COMMON;
        }
        
        return switch (hytaleRarity.toUpperCase()) {
            case "UNCOMMON" -> ItemRarity.UNCOMMON;
            case "RARE" -> ItemRarity.RARE;
            case "EPIC" -> ItemRarity.EPIC;
            case "LEGENDARY" -> ItemRarity.LEGENDARY;
            case "UNIQUE" -> ItemRarity.UNIQUE;
            default -> ItemRarity.COMMON;
        };
    }

    // ========================
    // Cache Management
    // ========================

    /**
     * Clears the cache. Call when item registry changes (e.g., datapack reload).
     */
    public void clearCache() {
        synchronized (this) {
            definitionCache.clear();
            tagToItemsCache.clear();
            cacheInitialized = false;
        }
    }

    /**
     * Forces cache re-initialization. Useful after datapack/mod changes.
     */
    public void refreshCache() {
        clearCache();
        ensureCacheInitialized();
    }
}