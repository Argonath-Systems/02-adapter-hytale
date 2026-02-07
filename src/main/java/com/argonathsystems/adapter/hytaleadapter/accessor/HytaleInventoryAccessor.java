package com.argonathsystems.adapter.hytaleadapter.accessor;

import au.ellie.hyui.builders.ContainerBuilder;
import au.ellie.hyui.builders.HyUIPage;
import au.ellie.hyui.builders.ItemGridBuilder;
import au.ellie.hyui.builders.LabelBuilder;
import au.ellie.hyui.builders.PageBuilder;
import com.argonathsystems.adapter.hytale.packet.InventoryBlockAdapter;
import com.argonathsystems.framework.accessorapi.InventoryAccessor;
import com.argonathsystems.framework.accessorapi.data.DataValue;
import com.argonathsystems.framework.accessorapi.dto.ItemData;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.ItemGridSlot;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiPredicate;

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
    
    /** Counter for generating unique registration IDs */
    private static final AtomicLong REGISTRATION_ID_COUNTER = new AtomicLong(0);
    
    private final HytaleServer server;
    
    /** Inventory block adapter for slot filtering */
    private final InventoryBlockAdapter inventoryBlockAdapter;
    
    /** List of active slot block filters */
    private final List<SlotBlockFilterEntry> slotBlockFilters = new CopyOnWriteArrayList<>();

    public HytaleInventoryAccessor(Object server) {
        this.server = (HytaleServer) server;
        this.inventoryBlockAdapter = new InventoryBlockAdapter();
        
        // Wire the composite filter to the adapter
        inventoryBlockAdapter.registerSlotBlockFilter(this::processSlotBlockFilters);
        
        LOGGER.info("HytaleInventoryAccessor initialized with slot blocking support");
    }
    
    /**
     * Get the inventory block adapter for packet registration.
     * 
     * <p>This adapter should be registered with the packet adapter system
     * during server initialization.</p>
     * 
     * @return The inventory block adapter
     */
    public InventoryBlockAdapter getInventoryBlockAdapter() {
        return inventoryBlockAdapter;
    }
    
    /**
     * Process all registered slot block filters.
     * Returns true if ANY filter wants to block the placement.
     */
    private boolean processSlotBlockFilters(UUID playerId, Integer slotIndex) {
        LOGGER.debug("Processing {} slot block filters for player {} slot {}", 
            slotBlockFilters.size(), playerId, slotIndex);
        
        for (SlotBlockFilterEntry entry : slotBlockFilters) {
            try {
                boolean shouldBlock = entry.filter.test(playerId, slotIndex);
                LOGGER.debug("Filter {} for slot {}: shouldBlock={}", 
                    entry.registrationId, slotIndex, shouldBlock);
                if (shouldBlock) {
                    LOGGER.info("Slot {} blocked for player {} by filter {}", 
                        slotIndex, playerId, entry.registrationId);
                    return true; // Block
                }
            } catch (Exception e) {
                LOGGER.error("Error in slot block filter (id={}): {}", 
                    entry.registrationId, e.getMessage(), e);
            }
        }
        return false; // Allow
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
    public Optional<ItemData> getHotbarItem(UUID playerId, int hotbarSlot) {
        if (hotbarSlot < 0 || hotbarSlot > 8) {
            LOGGER.debug("Hotbar slot {} out of range (0-8)", hotbarSlot);
            return Optional.empty();
        }
        
        Player player = getPlayer(playerId);
        if (player == null) {
            return Optional.empty();
        }
        
        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return Optional.empty();
        }
        
        // Get hotbar container specifically
        ItemContainer hotbar = inventory.getHotbar();
        if (hotbar == null) {
            LOGGER.debug("Player {} has no hotbar container", playerId);
            return Optional.empty();
        }
        
        ItemStack stack = hotbar.getItemStack((short) hotbarSlot);
        if (ItemStack.isEmpty(stack)) {
            return Optional.empty();
        }
        
        return Optional.of(toItemData(stack, hotbarSlot));
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
        
        // Refresh open container UI if the player has one open
        HyUIPage existingPage = openContainerPages.get(playerId);
        if (existingPage != null) {
            // Re-open the container with updated contents
            // The openContainer() method will close the existing page first
            int size = (items != null) ? items.size() : 0;
            String title = containerId; // Preserve title from cached state
            openContainer(playerId, containerId, title, Math.max(size, 9));
            LOGGER.debug("Refreshed container UI for {} - {} with {} items", playerId, containerId, size);
        } else {
            LOGGER.debug("Container contents cached for {} - {} (no UI open)", playerId, containerId);
        }
    }

    /**
     * Open a container UI for the player using HyUI PageBuilder + ItemGridBuilder.
     * 
     * <p>Creates a custom page with an ItemGrid displaying the container contents.
     * The grid is populated with ItemGridSlot entries converted from the cached
     * {@link ItemData} entries for this container. Slot click events are logged
     * for future interaction handling.</p>
     * 
     * <h2>SDK + HyUI Integration</h2>
     * <ol>
     *   <li>Get PlayerRef from Universe</li>
     *   <li>Build ItemGridBuilder with slots from cached container contents</li>
     *   <li>Create PageBuilder with fromHtml() for container layout</li>
     *   <li>Add ItemGrid element to the page</li>
     *   <li>Open page via PageBuilder.open(playerRef, store)</li>
     * </ol>
     * 
     * @param playerId    the player UUID
     * @param containerId the container identifier
     * @param title       the container title displayed to the player
     * @param size        number of slots (used to compute rows)
     * @see <a href="https://hyui.gitbook.io/docs/">HyUI Documentation</a>
     */
    @Override
    public void openContainer(UUID playerId, String containerId, String title, int size) {
        // 1. Get PlayerRef
        PlayerRef playerRef = Universe.get().getPlayer(playerId);
        if (playerRef == null || !playerRef.isValid()) {
            LOGGER.warn("Cannot open container: player {} not found or offline", playerId);
            return;
        }
        
        // Close any existing container page for this player
        closeContainer(playerId);
        
        // 2. Retrieve cached container contents
        String cacheKey = buildContainerCacheKey(playerId, containerId);
        List<ItemData> contents = containerContentsCache.getOrDefault(cacheKey, Collections.emptyList());
        
        // 3. Calculate grid dimensions
        int slotsPerRow = 9; // Standard container width
        int rows = Math.max(1, (int) Math.ceil((double) size / slotsPerRow));
        
        // 4. Build ItemGrid with container contents
        ItemGridBuilder gridBuilder = ItemGridBuilder.itemGrid()
            .withId("container_" + containerId)
            .withSlotsPerRow(slotsPerRow)
            .withAreItemsDraggable(false)
            .withRenderItemQualityBackground(true);
        
        // Populate slots from container contents
        for (int i = 0; i < size; i++) {
            ItemGridSlot slot = new ItemGridSlot();
            if (i < contents.size()) {
                ItemData item = contents.get(i);
                if (item != null && item.itemId() != null) {
                    // Convert ItemData to SDK ItemStack for the slot
                    ItemStack stack = new ItemStack(item.itemId(), item.amount());
                    slot.setItemStack(stack);
                    
                    // Set display name if available from custom data
                    if (item.customData() != null && item.customData().containsKey("displayName")) {
                        slot.setName(item.customData().get("displayName").toString());
                    }
                }
            }
            // Empty slots are added as-is (no ItemStack set)
            gridBuilder.addSlot(slot);
        }
        
        // 5. Build the container page HTML template
        String containerHtml = buildContainerTemplate(title, containerId, rows, slotsPerRow);
        
        // 6. Get the world + store for the player
        try {
            UUID worldUuid = playerRef.getWorldUuid();
            World world = Universe.get().getWorld(worldUuid);
            if (world == null) {
                LOGGER.warn("Cannot open container: world not found for player {}", playerId);
                return;
            }
            
            // Execute on world thread for thread-safe store access
            world.execute(() -> {
                try {
                    Store<EntityStore> store = world.getEntityStore().getStore();
                    
                    HyUIPage page = PageBuilder.pageForPlayer(playerRef)
                        .fromHtml(containerHtml)
                        .addElement(gridBuilder)
                        .open(playerRef, store);
                    
                    openContainerPages.put(playerId, page);
                    LOGGER.debug("Opened container '{}' ({} slots) for player {}", 
                        containerId, size, playerId);
                } catch (Exception e) {
                    LOGGER.error("Failed to open container page for player {}: {}", 
                        playerId, e.getMessage(), e);
                }
            });
            
        } catch (Exception e) {
            LOGGER.error("Failed to open container '{}' for player {}: {}", 
                containerId, playerId, e.getMessage(), e);
        }
    }

    /**
     * Builds a minimal HyUIML template for a container page.
     * 
     * @param title       container title
     * @param containerId container ID for element targeting
     * @param rows        number of grid rows
     * @param slotsPerRow slots per row
     * @return HyUIML template string
     */
    private String buildContainerTemplate(String title, String containerId, int rows, int slotsPerRow) {
        return """
            <div id="container-page-%s" style="width: 100%%; height: 100%%;">
                <div class="container-header" style="text-align: center; padding: 8px;">
                    <span class="container-title">%s</span>
                </div>
                <div class="container-body" style="padding: 4px;">
                    <div id="container_%s" class="item-grid"
                         data-hyui-slots-per-row="%d"
                         data-hyui-are-items-draggable="false">
                    </div>
                </div>
            </div>
            """.formatted(containerId, title, containerId, slotsPerRow);
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
    
    // ============================================================
    // Slot Blocking (SM-UI-050)
    // ============================================================
    
    @Override
    public SlotBlockRegistration registerHotbarSlotBlockFilter(BiPredicate<UUID, Integer> filter) {
        if (filter == null) {
            return null;
        }
        
        long registrationId = REGISTRATION_ID_COUNTER.incrementAndGet();
        SlotBlockFilterEntry entry = new SlotBlockFilterEntry(registrationId, filter);
        slotBlockFilters.add(entry);
        
        LOGGER.debug("Registered slot block filter (id={})", registrationId);
        
        return new HytaleSlotBlockRegistration(registrationId, this);
    }
    
    /**
     * Remove a slot block filter by registration ID.
     */
    private void removeSlotBlockFilter(long registrationId) {
        slotBlockFilters.removeIf(entry -> entry.registrationId == registrationId);
        LOGGER.debug("Unregistered slot block filter (id={})", registrationId);
    }
    
    /**
     * Entry for tracking slot block filters.
     */
    private record SlotBlockFilterEntry(
        long registrationId,
        BiPredicate<UUID, Integer> filter
    ) {}
    
    /**
     * Implementation of SlotBlockRegistration.
     */
    private static class HytaleSlotBlockRegistration implements SlotBlockRegistration {
        private final long registrationId;
        private final HytaleInventoryAccessor accessor;
        private volatile boolean active = true;
        
        HytaleSlotBlockRegistration(long registrationId, HytaleInventoryAccessor accessor) {
            this.registrationId = registrationId;
            this.accessor = accessor;
        }
        
        @Override
        public void unregister() {
            if (active) {
                accessor.removeSlotBlockFilter(registrationId);
                active = false;
            }
        }
        
        @Override
        public boolean isActive() {
            return active;
        }
    }
}
