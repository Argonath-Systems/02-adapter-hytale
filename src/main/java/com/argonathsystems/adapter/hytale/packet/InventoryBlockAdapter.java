package com.argonathsystems.adapter.hytale.packet;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.inventory.MoveItemStack;
import com.hypixel.hytale.protocol.packets.inventory.SmartMoveItemStack;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
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
        boolean shouldBlock = false;
        
        // Handle MoveItemStack packet (drag-drop)
        if (packet instanceof MoveItemStack movePacket) {
            LOGGER.info("[INVENTORY-BLOCK] Processing MoveItemStack: player={}, toSection={}, toSlot={}", 
                playerId, movePacket.toSectionId, movePacket.toSlotId);
            shouldBlock = handleMoveItemStack(playerId, movePacket, filter);
        }
        
        // Handle SmartMoveItemStack packet (shift-click)
        if (packet instanceof SmartMoveItemStack smartMovePacket) {
            LOGGER.info("[INVENTORY-BLOCK] Processing SmartMoveItemStack: player={}, fromSection={}, fromSlot={}", 
                playerId, smartMovePacket.fromSectionId, smartMovePacket.fromSlotId);
            shouldBlock = handleSmartMoveItemStack(playerId, smartMovePacket, filter);
        }
        
        // When a packet is blocked, the client has already performed the action
        // locally. We must resync the client inventory to undo the visual change.
        // Without this, items appear to "disappear" from the client perspective.
        if (shouldBlock) {
            resyncClientInventory(playerRef);
        }
        
        return shouldBlock;
    }
    
    /**
     * Handle MoveItemStack packet (explicit drag-drop move).
     * 
     * <p>Blocks item placement into locked hotbar slots AND prevents
     * removing items from locked hotbar slots.</p>
     */
    private boolean handleMoveItemStack(UUID playerId, MoveItemStack packet,
                                         BiPredicate<UUID, Integer> filter) {
        // Check if target is in hotbar section - block placement INTO locked slots
        if (packet.toSectionId == Inventory.HOTBAR_SECTION_ID) {
            int targetSlot = packet.toSlotId;
            if (targetSlot >= 0 && targetSlot <= 8) {
                boolean shouldBlock = filter.test(playerId, targetSlot);
                LOGGER.info("[INVENTORY-BLOCK] MoveItemStack slot {} lock check: shouldBlock={}", targetSlot, shouldBlock);
                if (shouldBlock) {
                    LOGGER.info("[INVENTORY-BLOCK] BLOCKED MoveItemStack to slot {} for player {}", 
                        targetSlot, playerId);
                    return true;
                }
            }
        }
        
        // Check if source is FROM a locked hotbar slot - block removal FROM locked slots
        if (packet.fromSectionId == Inventory.HOTBAR_SECTION_ID) {
            int sourceSlot = packet.fromSlotId;
            if (sourceSlot >= 0 && sourceSlot <= 8) {
                boolean sourceBlocked = filter.test(playerId, sourceSlot);
                if (sourceBlocked) {
                    LOGGER.info("[INVENTORY-BLOCK] BLOCKED MoveItemStack from locked slot {} for player {}", 
                        sourceSlot, playerId);
                    return true;
                }
            }
        }
        
        return false;
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
        // If source is FROM a locked hotbar slot, block removal
        if (packet.fromSectionId == Inventory.HOTBAR_SECTION_ID) {
            int sourceSlot = packet.fromSlotId;
            if (sourceSlot >= 0 && sourceSlot <= 8) {
                boolean sourceBlocked = filter.test(playerId, sourceSlot);
                if (sourceBlocked) {
                    LOGGER.info("[INVENTORY-BLOCK] BLOCKED SmartMoveItemStack from locked slot {} for player {}", 
                        sourceSlot, playerId);
                    return true;
                }
            }
        }
        
        // If source is from inventory and target could be hotbar,
        // the server picks first available slot. We can't reliably block
        // SmartMove targeting without server-side hooks.
        // Server-side validation handles this via inventory event hooks.
        
        return false;
    }
    
    // =========================================================================
    // Client Inventory Resync
    // =========================================================================
    
    /**
     * Resync the client's inventory state after blocking an inventory packet.
     * 
     * <p>When we block a MoveItemStack or SmartMoveItemStack packet, the client
     * has already performed the item move locally. The client and server are now
     * desynced - the client thinks the item moved, but the server rejected it.</p>
     * 
     * <p>We must send the authoritative inventory state back to the client via
     * {@code player.sendInventory()} to fix this desync. Without this fix,
     * items appear to "disappear" from the client's perspective.</p>
     * 
     * <p><b>Thread Safety:</b> Uses {@code playerRef.getWorldUuid()} (thread-safe)
     * to obtain the World, then schedules all entity/store access on the world thread
     * via {@code world.execute()}. This avoids "Assert not in thread!" errors from
     * calling {@code playerRef.getReference()} on the network/packet filter thread.</p>
     * 
     * @param playerRef The player whose inventory should be resynced
     */
    private void resyncClientInventory(PlayerRef playerRef) {
        try {
            // getWorldUuid() is thread-safe and can be called from any thread
            java.util.UUID worldUuid = playerRef.getWorldUuid();
            if (worldUuid == null) {
                LOGGER.warn("[INVENTORY-BLOCK] Cannot resync inventory: player {} has no world UUID", 
                    playerRef.getUuid());
                return;
            }
            
            World world = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(worldUuid);
            if (world == null) {
                LOGGER.warn("[INVENTORY-BLOCK] Cannot resync inventory: world {} not found for player {}", 
                    worldUuid, playerRef.getUuid());
                return;
            }
            
            // Schedule ALL entity/store access on the world thread (SDK requirement)
            world.execute(() -> {
                try {
                    // Get entity reference ON the world thread where it's valid
                    Ref<EntityStore> entityRef = playerRef.getReference();
                    if (entityRef == null || !entityRef.isValid()) {
                        LOGGER.warn("[INVENTORY-BLOCK] Cannot resync inventory: invalid entity ref for player {} (on world thread)", 
                            playerRef.getUuid());
                        return;
                    }
                    
                    Store<EntityStore> store = entityRef.getStore();
                    Player playerComponent = store.getComponent(entityRef, Player.getComponentType());
                    if (playerComponent == null) {
                        LOGGER.warn("[INVENTORY-BLOCK] Cannot resync inventory: Player component null for {}", 
                            playerRef.getUuid());
                        return;
                    }
                    
                    // Resend the authoritative inventory state to the client
                    playerComponent.sendInventory();
                    
                    LOGGER.debug("[INVENTORY-BLOCK] Inventory resync sent for player {}", 
                        playerRef.getUuid());
                } catch (Exception e) {
                    LOGGER.error("[INVENTORY-BLOCK] Error during inventory resync for player {}", 
                        playerRef.getUuid(), e);
                }
            });
        } catch (Exception e) {
            LOGGER.error("[INVENTORY-BLOCK] Error scheduling inventory resync for player {}", 
                playerRef.getUuid(), e);
        }
    }
}
