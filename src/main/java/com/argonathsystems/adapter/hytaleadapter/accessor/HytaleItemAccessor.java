package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.ItemAccessor;
import com.argonathsystems.framework.accessorapi.data.DataValue;
import com.argonathsystems.framework.accessorapi.dto.ItemData;
import com.argonathsystems.framework.accessorapi.dto.ItemDefinitionData;
import com.argonathsystems.framework.accessorapi.dto.ItemDefinitionData.ItemRarity;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Hytale implementation of ItemAccessor using SDK asset registry.
 * 
 * <p><b>MIGRATION-001 Status:</b> ✅ IMPLEMENTED</p>
 * 
 * <p>SDK Classes Used:</p>
 * <ul>
 *   <li>{@code AssetRegistry} - Global registry for all asset types</li>
 *   <li>{@code AssetStore} - Store for specific asset types (Item, Block, etc.)</li>
 *   <li>{@code Item} - Item asset configuration with properties</li>
 * </ul>
 * 
 * <p>Item properties like categories, quality, and max stack are read from the
 * Item asset configuration.</p>
 * 
 * @author Argonath Systems Team
 * @version 3.0.0
 * @since MIGRATION-001
 */
public class HytaleItemAccessor implements ItemAccessor {
    
    private final Object server;

    public HytaleItemAccessor(Object server) {
        this.server = server;
    }

    @Override
    public Optional<ItemDefinitionData> getItemDefinition(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return Optional.empty();
        }
        
        // Get the Item asset store
        AssetStore<String, Item, DefaultAssetMap<String, Item>> itemStore = Item.getAssetStore();
        if (itemStore == null) {
            return Optional.empty();
        }
        
        // Get the asset map and lookup the item
        DefaultAssetMap<String, Item> assetMap = itemStore.getAssetMap();
        if (assetMap == null) {
            return Optional.empty();
        }
        
        Item item = assetMap.getAsset(itemId);
        if (item == null) {
            return Optional.empty();
        }
        
        return Optional.of(toItemDefinitionData(item));
    }

    @Override
    public Collection<ItemDefinitionData> getAllItemDefinitions() {
        AssetStore<String, Item, DefaultAssetMap<String, Item>> itemStore = Item.getAssetStore();
        if (itemStore == null) {
            return Collections.emptyList();
        }
        
        DefaultAssetMap<String, Item> assetMap = itemStore.getAssetMap();
        if (assetMap == null) {
            return Collections.emptyList();
        }
        
        // Iterate all items and convert to DTOs
        // SDK uses getAssetMap().keySet() for all keys
        return assetMap.getAssetMap().keySet().stream()
            .map(assetMap::getAsset)
            .filter(item -> item != null)
            .map(this::toItemDefinitionData)
            .collect(Collectors.toList());
    }

    @Override
    public Collection<ItemDefinitionData> getItemsByTag(String tag) {
        if (tag == null || tag.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Get all items and filter by tag/category
        return getAllItemDefinitions().stream()
            .filter(item -> item.hasTag(tag))
            .collect(Collectors.toList());
    }

    @Override
    public Set<String> getItemTags(String itemId) {
        return getItemDefinition(itemId)
            .map(ItemDefinitionData::tags)
            .orElse(Collections.emptySet());
    }

    @Override
    public ItemData createItem(String itemId, int amount) {
        if (itemId == null || itemId.isEmpty()) {
            throw new IllegalArgumentException("itemId cannot be null or empty");
        }
        
        // Validate the item exists
        Optional<ItemDefinitionData> definition = getItemDefinition(itemId);
        if (definition.isEmpty()) {
            throw new IllegalArgumentException("Unknown item: " + itemId);
        }
        
        // Create ItemData with default values
        return new ItemData(
            UUID.randomUUID(), // Unique instance ID
            itemId,
            Math.min(amount, definition.get().maxStackSize()),
            -1, // No durability by default
            -1, // No max durability by default
            Map.of() // Empty custom data
        );
    }
    
    // --- Helper Methods ---
    
    /**
     * Convert SDK Item asset to framework ItemDefinitionData DTO.
     */
    private ItemDefinitionData toItemDefinitionData(Item item) {
        String itemId = item.getId();
        
        // Get translation key for display name
        String displayName = item.getTranslationKey();
        if (displayName == null || displayName.isEmpty()) {
            displayName = itemId;
        }
        
        // Get description
        String description = item.getDescriptionTranslationKey();
        if (description == null) {
            description = "";
        }
        
        // Get max stack size
        int maxStackSize = item.getMaxStack();
        if (maxStackSize <= 0) {
            maxStackSize = 1; // Default to non-stackable
        }
        
        // Convert quality index to rarity
        // SDK uses getQualityIndex() which returns int
        ItemRarity rarity = convertQualityIndexToRarity(item.getQualityIndex());
        
        // Get categories as tags
        Set<String> tags = new HashSet<>();
        String[] categories = item.getCategories();
        if (categories != null) {
            tags.addAll(Arrays.asList(categories));
        }
        
        // Add item type tags based on properties
        if (item.getTool() != null) {
            tags.add("tool");
        }
        if (item.getWeapon() != null) {
            tags.add("weapon");
        }
        if (item.getArmor() != null) {
            tags.add("armor");
        }
        if (item.isConsumable()) {
            tags.add("consumable");
        }
        if (item.getBlockId() != null && !item.getBlockId().isEmpty()) {
            tags.add("block_item");
        }
        
        return new ItemDefinitionData(
            itemId,
            displayName,
            description,
            maxStackSize,
            rarity,
            tags
        );
    }
    
    /**
     * Map SDK quality index to framework rarity.
     * SDK uses getQualityIndex() which returns an integer.
     */
    private ItemRarity convertQualityIndexToRarity(int qualityIndex) {
        // Map quality index to rarity - indexes are SDK internal
        return switch (qualityIndex) {
            case 0 -> ItemRarity.COMMON;     // Assumed common
            case 1 -> ItemRarity.UNCOMMON;
            case 2 -> ItemRarity.RARE;
            case 3 -> ItemRarity.EPIC;
            case 4 -> ItemRarity.LEGENDARY;
            case 5 -> ItemRarity.UNIQUE;
            default -> ItemRarity.COMMON;
        };
    }
}
