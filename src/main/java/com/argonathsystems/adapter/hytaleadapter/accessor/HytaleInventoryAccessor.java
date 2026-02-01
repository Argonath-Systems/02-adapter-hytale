package com.argonathsystems.adapter.hytaleadapter.accessor;

import au.ellie.hyui.builders.ContainerBuilder;
import au.ellie.hyui.builders.HyUIPage;
import au.ellie.hyui.builders.ItemGridBuilder;
import au.ellie.hyui.builders.PageBuilder;
import com.argonathsystems.framework.accessorapi.InventoryAccessor;
import com.argonathsystems.framework.accessorapi.data.DataValue;
import com.argonathsystems.framework.accessorapi.dto.ItemData;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.ItemGridSlot;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import org.bson.BsonDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Hytale implementation of InventoryAccessor using SDK inventory system.
 * 
 * <p><b>MIGRATION-001 Status:</b> ✅ IMPLEMENTED</p>
 * 
 * <p>SDK Classes Used:</p>
 * <ul>
 *   <li>{@code Inventory} - Player inventory with sections (hotbar, storage, armor, etc.)</li>
 *   <li>{@code ItemContainer} - Abstract container for item slots</li>
 *   <li>{@code ItemStack} - Item with quantity, durability, metadata</li>
 * </ul>
 * 
 * <p>Inventory Sections:</p>
 * <ul>
 *   <li>Hotbar: Slots 0-9 (active hand)</li>
 *   <li>Storage: Main inventory (configurable rows/columns)</li>
 *   <li>Armor: Equipment slots</li>
 *   <li>Utility/Tools: Special slots</li>
 * </ul>
 * 
 * @author Argonath Systems Team
 * @version 3.0.0
 * @since MIGRATION-001
 */
public class HytaleInventoryAccessor implements InventoryAccessor {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HytaleInventoryAccessor.class);
    
    private final HytaleServer server;

    public HytaleInventoryAccessor(Object server) {
        this.server = (HytaleServer) server;
    }

    @Override
    public List<ItemData> getInventoryContents(UUID playerId) {
        Player player = getPlayer(playerId);
        if (player == null) {
            return Collections.emptyList();
        }
        
        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return Collections.emptyList();
        }
        
        List<ItemData> contents = new ArrayList<>();
        
        // Get combined container (everything)
        ItemContainer combined = inventory.getCombinedEverything();
        short capacity = combined.getCapacity();
        
        for (short slot = 0; slot < capacity; slot++) {
            ItemStack stack = combined.getItemStack(slot);
            if (!ItemStack.isEmpty(stack)) {
                contents.add(toItemData(stack, slot));
            }
        }
        
        return contents;
    }

    @Override
    public Optional<ItemData> getItem(UUID playerId, int slot) {
        Player player = getPlayer(playerId);
        if (player == null) {
            return Optional.empty();
        }
        
        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return Optional.empty();
        }
        
        // Use combined container for unified slot access
        ItemContainer combined = inventory.getCombinedEverything();
        ItemStack stack = combined.getItemStack((short) slot);
        
        if (ItemStack.isEmpty(stack)) {
            return Optional.empty();
        }
        
        return Optional.of(toItemData(stack, slot));
    }

    @Override
    public void setItem(UUID playerId, int slot, ItemData item) {
        Player player = getPlayer(playerId);
        if (player == null) {
            return;
        }
        
        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return;
        }
        
        ItemContainer combined = inventory.getCombinedEverything();
        ItemStack stack = fromItemData(item);
        
        // Set item in slot (replaces existing)
        combined.setItemStackForSlot((short) slot, stack);
        
        // Notify client of inventory change
        player.sendInventory();
    }

    @Override
    public Optional<ItemData> addItem(UUID playerId, ItemData item) {
        Player player = getPlayer(playerId);
        if (player == null) {
            return Optional.of(item); // Return full item as overflow
        }
        
        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return Optional.of(item);
        }
        
        ItemStack stack = fromItemData(item);
        
        // Add to hotbar first, then storage
        ItemContainer combined = inventory.getCombinedHotbarFirst();
        var transaction = combined.addItemStack(stack);
        
        player.sendInventory();
        
        // If transaction has remaining items, return as overflow
        // SDK uses getRemainder() for remaining ItemStack
        ItemStack remaining = transaction.getRemainder();
        if (!ItemStack.isEmpty(remaining)) {
            return Optional.of(toItemData(remaining, -1));
        }
        
        return Optional.empty();
    }

    @Override
    public boolean removeItem(UUID playerId, String itemId, int amount) {
        Player player = getPlayer(playerId);
        if (player == null) {
            return false;
        }
        
        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return false;
        }
        
        ItemContainer combined = inventory.getCombinedEverything();
        int remaining = amount;
        
        // Iterate through slots and remove items
        for (short slot = 0; slot < combined.getCapacity() && remaining > 0; slot++) {
            ItemStack stack = combined.getItemStack(slot);
            if (!ItemStack.isEmpty(stack) && itemId.equals(stack.getItemId())) {
                int toRemove = Math.min(remaining, stack.getQuantity());
                combined.removeItemStackFromSlot(slot, toRemove);
                remaining -= toRemove;
            }
        }
        
        if (remaining < amount) {
            player.sendInventory();
        }
        
        return remaining == 0;
    }

    @Override
    public boolean hasItem(UUID playerId, String itemId, int amount) {
        return countItem(playerId, itemId) >= amount;
    }

    @Override
    public int countItem(UUID playerId, String itemId) {
        Player player = getPlayer(playerId);
        if (player == null) {
            return 0;
        }
        
        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return 0;
        }
        
        ItemContainer combined = inventory.getCombinedEverything();
        int count = 0;
        
        for (short slot = 0; slot < combined.getCapacity(); slot++) {
            ItemStack stack = combined.getItemStack(slot);
            if (!ItemStack.isEmpty(stack) && itemId.equals(stack.getItemId())) {
                count += stack.getQuantity();
            }
        }
        
        return count;
    }

    @Override
    public Optional<ItemData> getMainHandItem(UUID playerId) {
        Player player = getPlayer(playerId);
        if (player == null) {
            return Optional.empty();
        }
        
        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return Optional.empty();
        }
        
        // Get active hotbar item
        ItemStack stack = inventory.getItemInHand();
        if (ItemStack.isEmpty(stack)) {
            return Optional.empty();
        }
        
        return Optional.of(toItemData(stack, inventory.getActiveHotbarSlot()));
    }

    @Override
    public int getEmptySlots(UUID playerId) {
        Player player = getPlayer(playerId);
        if (player == null) {
            return 0;
        }
        
        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return 0;
        }
        
        // Count empty slots in storage and hotbar
        ItemContainer combined = inventory.getCombinedHotbarFirst();
        int emptyCount = 0;
        
        for (short slot = 0; slot < combined.getCapacity(); slot++) {
            if (ItemStack.isEmpty(combined.getItemStack(slot))) {
                emptyCount++;
            }
        }
        
        return emptyCount;
    }

    @Override
    public void clearInventory(UUID playerId) {
        Player player = getPlayer(playerId);
        if (player == null) {
            return;
        }
        
        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return;
        }
        
        inventory.clear();
        player.sendInventory();
    }

    // ============================================================
    // Container Operations (for instanced loot)
    // ============================================================

    // Thread-safe storage for per-player container contents
    private final java.util.concurrent.ConcurrentHashMap<String, List<ItemData>> containerContentsCache = 
        new java.util.concurrent.ConcurrentHashMap<>();
    
    // Track open container pages per player
    private final java.util.concurrent.ConcurrentHashMap<UUID, HyUIPage> openContainerPages = 
        new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public List<ItemData> getContainerContents(UUID playerId, String containerId) {
        String cacheKey = buildContainerCacheKey(playerId, containerId);
        return containerContentsCache.getOrDefault(cacheKey, Collections.emptyList());
    }

    @Override
    public void setContainerContents(UUID playerId, String containerId, List<ItemData> items) {
        String cacheKey = buildContainerCacheKey(playerId, containerId);
        if (items == null || items.isEmpty()) {
            containerContentsCache.remove(cacheKey);
        } else {
            containerContentsCache.put(cacheKey, new ArrayList<>(items));
        }
        
        // TODO: Refresh open container with updated contents when HyUI API is clarified
        LOGGER.debug("Container contents cached for {} - {}", playerId, containerId);
    }

    /**
     * Open a container UI for the player.
     * 
     * <p><b>SDK Research Required:</b></p>
     * <ul>
     *   <li>ItemGridSlot requires ItemStack, not (String, int) - need ItemStack.Builder pattern</li>
     *   <li>PageBuilder.withId() method not found in HyUI docs</li>
     *   <li>HyUIPage.show() method not found - use .open(Store) pattern instead?</li>
     *   <li>Need to research proper HyUI container/inventory grid creation</li>
     * </ul>
     * 
     * @see <a href="https://hyui.gitbook.io/docs/">HyUI Documentation</a>
     */
    @Override
    public void openContainer(UUID playerId, String containerId, String title, int size) {
        // TODO: Implement using HyUI ItemGridBuilder once API is clarified
        // Required SDK research:
        // 1. How to create ItemStack from item ID string
        // 2. Correct PageBuilder API for container UI
        // 3. How to handle drag-and-drop slot events
        throw new UnsupportedOperationException(
            "Container UI implementation requires HyUI ItemGrid API research. " +
            "SDK: ItemGridSlot(ItemStack), PageBuilder, ItemGridBuilder");
    }

    @Override
    public void closeContainer(UUID playerId) {
        if (playerId == null) {
            return;
        }
        
        HyUIPage existingPage = openContainerPages.remove(playerId);
        if (existingPage != null) {
            existingPage.close();
            LOGGER.debug("Closed container for player {}", playerId);
        }
    }

    /**
     * Build a cache key for container contents.
     */
    private String buildContainerCacheKey(UUID playerId, String containerId) {
        return playerId.toString() + ":" + containerId;
    }
    
    // --- Helper Methods ---
    
    /**
     * Get Player component from UUID using SDK Universe and ECS pattern.
     * 
     * <p><b>SDK Pattern:</b></p>
     * <ol>
     *   <li>Get PlayerRef via Universe.get().getPlayer(UUID)</li>
     *   <li>Get Player component via PlayerRef.getComponent(Player.getComponentType())</li>
     * </ol>
     * 
     * @param playerId The player's UUID
     * @return Player component, or null if not found/offline
     */
    private Player getPlayer(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        
        // Use Universe singleton to get player reference
        PlayerRef playerRef = Universe.get().getPlayer(playerId);
        if (playerRef == null || !playerRef.isValid()) {
            LOGGER.debug("Player not found or offline: {}", playerId);
            return null;
        }
        
        // Get the Player component via ECS pattern
        // Player is a Component<EntityStore>, use getComponent(ComponentType)
        Player player = playerRef.getComponent(Player.getComponentType());
        if (player == null) {
            LOGGER.debug("PlayerRef valid but Player component unavailable: {}", playerId);
            return null;
        }
        
        return player;
    }
    
    /**
     * Convert SDK ItemStack to framework ItemData DTO.
     */
    private ItemData toItemData(ItemStack stack, int slot) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        
        // Convert BsonDocument metadata to DataValue map
        Map<String, DataValue> customData = new HashMap<>();
        BsonDocument metadata = stack.getMetadata();
        if (metadata != null) {
            customData = convertBsonToDataValue(metadata);
        }
        
        return new ItemData(
            UUID.randomUUID(), // Generate unique instance ID
            stack.getItemId(),
            stack.getQuantity(),
            (int) stack.getDurability(),
            (int) stack.getMaxDurability(),
            customData
        );
    }
    
    /**
     * Convert framework ItemData DTO to SDK ItemStack.
     */
    private ItemStack fromItemData(ItemData item) {
        if (item == null) {
            return ItemStack.EMPTY;
        }
        
        // Create ItemStack with item ID and quantity
        ItemStack stack = new ItemStack(item.itemId(), item.amount());
        
        // Apply durability if specified
        if (item.durability() > 0) {
            stack = stack.withDurability(item.durability());
        }
        if (item.maxDurability() > 0) {
            stack = stack.withMaxDurability(item.maxDurability());
        }
        
        // Apply metadata if present
        if (item.customData() != null && !item.customData().isEmpty()) {
            BsonDocument metadata = convertDataValueToBson(item.customData());
            stack = stack.withMetadata(metadata);
        }
        
        return stack;
    }
    
    /**
     * Convert BSON document to DataValue map.
     */
    private Map<String, DataValue> convertBsonToDataValue(BsonDocument doc) {
        Map<String, DataValue> result = new HashMap<>();
        for (String key : doc.keySet()) {
            org.bson.BsonValue bsonValue = doc.get(key);
            DataValue dataValue = bsonToDataValue(bsonValue);
            if (dataValue != null) {
                result.put(key, dataValue);
            }
        }
        return result;
    }
    
    /**
     * Convert a single BSON value to DataValue.
     */
    private DataValue bsonToDataValue(org.bson.BsonValue bsonValue) {
        if (bsonValue == null || bsonValue.isNull()) {
            return null;
        }
        if (bsonValue.isString()) {
            return DataValue.of(bsonValue.asString().getValue());
        }
        if (bsonValue.isInt32()) {
            return DataValue.of(bsonValue.asInt32().getValue());
        }
        if (bsonValue.isInt64()) {
            return DataValue.of(bsonValue.asInt64().getValue());
        }
        if (bsonValue.isDouble()) {
            return DataValue.of(bsonValue.asDouble().getValue());
        }
        if (bsonValue.isBoolean()) {
            return DataValue.of(bsonValue.asBoolean().getValue());
        }
        // For complex types, store as string representation
        return DataValue.of(bsonValue.toString());
    }
    
    /**
     * Convert DataValue map to BSON document.
     */
    private BsonDocument convertDataValueToBson(Map<String, DataValue> data) {
        BsonDocument doc = new BsonDocument();
        for (Map.Entry<String, DataValue> entry : data.entrySet()) {
            org.bson.BsonValue bsonValue = dataValueToBson(entry.getValue());
            if (bsonValue != null) {
                doc.put(entry.getKey(), bsonValue);
            }
        }
        return doc;
    }
    
    /**
     * Convert a single DataValue to BSON value.
     */
    private org.bson.BsonValue dataValueToBson(DataValue value) {
        return switch (value) {
            case DataValue.StringValue sv -> new org.bson.BsonString(sv.value());
            case DataValue.IntValue iv -> new org.bson.BsonInt32(iv.value());
            case DataValue.LongValue lv -> new org.bson.BsonInt64(lv.value());
            case DataValue.DoubleValue dv -> new org.bson.BsonDouble(dv.value());
            case DataValue.BoolValue bv -> new org.bson.BsonBoolean(bv.value());
            case DataValue.ListValue lv -> {
                org.bson.BsonArray arr = new org.bson.BsonArray();
                for (DataValue dv : lv.value()) {
                    arr.add(dataValueToBson(dv));
                }
                yield arr;
            }
            case DataValue.MapValue mv -> convertDataValueToBson(mv.value());
        };
    }
}
