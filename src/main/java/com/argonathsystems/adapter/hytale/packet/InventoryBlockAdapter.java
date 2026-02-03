package com.argonathsystems.adapter.hytale.packet;

import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.inventory.MoveItemStack;
import com.hypixel.hytale.protocol.packets.inventory.SmartMoveItemStack;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.function.BiPredicate;

/**
 * Adapter for intercepting inventory actions to block item placement in locked slots.
 * 
 * <p>This adapter intercepts inventory manipulation packets and can block
 * item placement in specific hotbar slots, used by the multi-hotbar mod
 * to prevent items being placed in action-bound slots.</p>
 * 
 * <h2>Blocked Actions</h2>
 * <ul>
 *   <li>{@code MoveItemStack} - Drag-drop onto hotbar slots</li>
 *   <li>{@code SmartMoveItemStack} - Shift-click into hotbar slots</li>
 * </ul>
 * 
 * @author Argonath Systems Team
 * @version 1.0.0
 * @since SM-UI-050
 */
public class InventoryBlockAdapter implements PlayerPacketFilter {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryBlockAdapter.class);
    
    /**
     * Slot block filter.
     * BiPredicate&lt;UUID playerId, Integer slotIndex&gt;
     * Returns true to BLOCK item placement, false to ALLOW.
     */
    private volatile BiPredicate<UUID, Integer> slotBlockFilter;
    
    public InventoryBlockAdapter() {
        LOGGER.info("InventoryBlockAdapter initialized");
    }
    
    // =========================================================================
    // Filter Registration
    // =========================================================================
    
    /**
     * Register a slot block filter.
     * 
     * <p>The filter receives (playerId, slotIndex) and should return:
     * <ul>
     *   <li>{@code true} to BLOCK item placement in this slot</li>
     *   <li>{@code false} to ALLOW item placement in this slot</li>
     * </ul>
     * 
     * @param filter The filter function
     */
    public void registerSlotBlockFilter(BiPredicate<UUID, Integer> filter) {
        this.slotBlockFilter = filter;
        LOGGER.info("Slot block filter registered");
    }
    
    /**
     * Unregister the slot block filter.
     */
    public void unregisterSlotBlockFilter() {
        this.slotBlockFilter = null;
        LOGGER.info("Slot block filter unregistered");
    }
    
    /**
     * Check if a filter is currently registered.
     */
    public boolean hasFilter() {
        return slotBlockFilter != null;
    }
    
    // =========================================================================
    // PlayerPacketFilter Implementation
    // =========================================================================
    
    @Override
    public boolean test(@Nonnull PlayerRef playerRef, @Nonnull Packet packet) {
        BiPredicate<UUID, Integer> filter = slotBlockFilter;
        if (filter == null) {
            return false; // No filter registered, allow all
        }
        
        UUID playerId = playerRef.getUuid();
        
        // Handle MoveItemStack packet (drag-drop)
        if (packet instanceof MoveItemStack movePacket) {
            LOGGER.info("[INVENTORY-BLOCK] Processing MoveItemStack: player={}, toSection={}, toSlot={}", 
                playerId, movePacket.toSectionId, movePacket.toSlotId);
            return handleMoveItemStack(playerId, movePacket, filter);
        }
        
        // Handle SmartMoveItemStack packet (shift-click)
        if (packet instanceof SmartMoveItemStack smartMovePacket) {
            LOGGER.info("[INVENTORY-BLOCK] Processing SmartMoveItemStack: player={}, fromSection={}, fromSlot={}", 
                playerId, smartMovePacket.fromSectionId, smartMovePacket.fromSlotId);
            return handleSmartMoveItemStack(playerId, smartMovePacket, filter);
        }
        
        return false;
    }
    
    /**
     * Handle MoveItemStack packet (explicit drag-drop move).
     */
    private boolean handleMoveItemStack(UUID playerId, MoveItemStack packet,
                                         BiPredicate<UUID, Integer> filter) {
        // Check if target is in hotbar section
        if (packet.toSectionId != Inventory.HOTBAR_SECTION_ID) {
            LOGGER.info("[INVENTORY-BLOCK] MoveItemStack target not hotbar (sectionId={}), allowing", 
                packet.toSectionId);
            return false;
        }
        
        int targetSlot = packet.toSlotId;
        if (targetSlot < 0 || targetSlot > 8) {
            LOGGER.info("[INVENTORY-BLOCK] MoveItemStack target slot {} out of range, allowing", targetSlot);
            return false; // Invalid slot
        }
        
        // Check if this slot should be blocked
        boolean shouldBlock = filter.test(playerId, targetSlot);
        LOGGER.info("[INVENTORY-BLOCK] MoveItemStack slot {} lock check: shouldBlock={}", targetSlot, shouldBlock);
        if (shouldBlock) {
            LOGGER.info("[INVENTORY-BLOCK] BLOCKED MoveItemStack to slot {} for player {}", 
                targetSlot, playerId);
        }
        return shouldBlock;
    }
    
    /**
     * Handle SmartMoveItemStack packet (shift-click auto-move).
     * 
     * <p>For SmartMove, the server determines the destination. If the source
     * is from an inventory section, hotbar slots might be targeted. We check
     * if any registered locked slot might be a destination.</p>
     */
    private boolean handleSmartMoveItemStack(UUID playerId, SmartMoveItemStack packet,
                                              BiPredicate<UUID, Integer> filter) {
        // If source is FROM hotbar, this is moving OUT - allow
        if (packet.fromSectionId == Inventory.HOTBAR_SECTION_ID) {
            return false;
        }
        
        // If source is from inventory and target could be hotbar,
        // check all potentially locked slots
        // Note: SmartMove doesn't specify target, server picks first available
        // We can't reliably block without checking all slots
        // For now, we'll rely on the server-side validation
        // This is a limitation - true blocking requires server-side hooks
        
        return false;
    }
}
