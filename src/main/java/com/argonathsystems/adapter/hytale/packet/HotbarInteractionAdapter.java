package com.argonathsystems.adapter.hytale.packet;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.protocol.packets.inventory.SetActiveSlot;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;

/**
 * Adapter for intercepting hotbar interaction packets (SyncInteractionChains).
 * 
 * <p>This adapter provides a platform-agnostic way for mods to intercept and
 * optionally block hotbar slot selection packets. It bridges between the
 * Hytale SDK packet system and the accessor API.</p>
 * 
 * <h2>Supported Interactions</h2>
 * <ul>
 *   <li>{@code SwapTo} - Player pressing a hotbar key (1-9, 0)</li>
 *   <li>{@code SwapFrom} - Player swapping from current slot</li>
 *   <li>{@code Primary} - Primary interaction (left click)</li>
 *   <li>{@code Secondary} - Secondary interaction (right click)</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <pre>{@code
 * // In the accessor implementation
 * HotbarInteractionAdapter adapter = new HotbarInteractionAdapter();
 * 
 * // Register filter from mod layer
 * adapter.registerSlotFilter((playerId, slotIndex) -> {
 *     // Return true to BLOCK the slot switch
 *     // Return false to ALLOW the slot switch
 *     return multiHotbarMod.processSlotPress(playerId, slotIndex);
 * });
 * }</pre>
 * 
 * @author Argonath Systems Team
 * @version 1.0.0
 * @since SM-UI-050
 */
public class HotbarInteractionAdapter implements PlayerPacketFilter {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HotbarInteractionAdapter.class);
    
    /**
     * Slot selection filter.
     * BiPredicate&lt;UUID playerId, Integer slotIndex&gt;
     * Returns true to BLOCK the native slot switch, false to ALLOW.
     */
    private volatile BiPredicate<UUID, Integer> slotSelectionFilter;
    
    /**
     * Interaction filter for primary/secondary clicks while holding a slot.
     * BiPredicate&lt;UUID playerId, InteractionData&gt;
     * Returns true to BLOCK the interaction, false to ALLOW.
     */
    private volatile BiPredicate<UUID, InteractionData> interactionFilter;
    
    /** Track currently selected slots per player for SwapFrom detection */
    private final Map<UUID, Integer> currentSlots = new ConcurrentHashMap<>();
    
    public HotbarInteractionAdapter() {
        LOGGER.info("HotbarInteractionAdapter initialized");
    }
    
    // =========================================================================
    // Filter Registration (Called from accessor layer)
    // =========================================================================
    
    /**
     * Register a slot selection filter.
     * 
     * <p>The filter receives (playerId, slotIndex) and should return:
     * <ul>
     *   <li>{@code true} to BLOCK the native slot switch</li>
     *   <li>{@code false} to ALLOW the native slot switch</li>
     * </ul>
     * 
     * @param filter The filter function
     */
    public void registerSlotFilter(BiPredicate<UUID, Integer> filter) {
        this.slotSelectionFilter = filter;
        LOGGER.info("Slot selection filter registered");
    }
    
    /**
     * Unregister the slot selection filter.
     */
    public void unregisterSlotFilter() {
        this.slotSelectionFilter = null;
        LOGGER.info("Slot selection filter unregistered");
    }
    
    /**
     * Register an interaction filter for primary/secondary actions.
     * 
     * @param filter The filter function
     */
    public void registerInteractionFilter(BiPredicate<UUID, InteractionData> filter) {
        this.interactionFilter = filter;
        LOGGER.info("Interaction filter registered");
    }
    
    /**
     * Unregister the interaction filter.
     */
    public void unregisterInteractionFilter() {
        this.interactionFilter = null;
        LOGGER.info("Interaction filter unregistered");
    }
    
    // =========================================================================
    // PlayerPacketFilter Implementation
    // =========================================================================
    
    @Override
    public boolean test(@Nonnull PlayerRef playerRef, @Nonnull Packet packet) {
        // Only interested in SyncInteractionChains packets
        if (!(packet instanceof SyncInteractionChains syncPacket)) {
            return false; // Don't block, not our packet type
        }
        
        UUID playerId = playerRef.getUuid();
        boolean shouldBlock = false;
        
        // INFO level for visibility - critical for debugging slot interception
        LOGGER.info("[HOTBAR-INTERCEPT] Received SyncInteractionChains from player {}, updates: {}, filter: {}", 
            playerId, 
            syncPacket.updates != null ? syncPacket.updates.length : 0,
            slotSelectionFilter != null ? "present" : "null");
        
        // Capture the previous slot BEFORE processing chains
        int previousSlot = currentSlots.getOrDefault(playerId, 0);
        
        // Check each interaction chain
        for (SyncInteractionChain chain : syncPacket.updates) {
            LOGGER.info("[HOTBAR-INTERCEPT] Processing chain: type={}, activeSlot={}", 
                chain.interactionType, chain.activeHotbarSlot);
            boolean chainBlocked = processChain(playerId, chain);
            if (chainBlocked) {
                shouldBlock = true;
            }
        }
        
        // If we blocked the packet, fix client desync by sending SetActiveSlot
        // The client has already switched slots locally before sending the packet,
        // so we must tell it to go back to the correct slot.
        // See: SDK docs "Customizing Hotbar Actions" - Part 4: The Client Desync Problem
        if (shouldBlock) {
            int resyncSlot = currentSlots.getOrDefault(playerId, previousSlot);
            // Clamp to valid hotbar range [0, 8] — negative values from SwapFrom
            // chains (e.g. -5) would crash Inventory.setActiveHotbarSlot()
            if (resyncSlot < 0 || resyncSlot > 8) {
                LOGGER.warn("[HOTBAR-INTERCEPT] Invalid resync slot {} for player {}, clamping to 0", 
                    resyncSlot, playerId);
                resyncSlot = 0;
            }
            LOGGER.info("[HOTBAR-INTERCEPT] Sending SetActiveSlot resync to slot {} for player {}", 
                resyncSlot, playerId);
            resyncClientSlot(playerRef, resyncSlot);
        }
        
        LOGGER.info("[HOTBAR-INTERCEPT] Final decision: shouldBlock={}", shouldBlock);
        return shouldBlock;
    }
    
    /**
     * Process a single interaction chain.
     * 
     * @param playerId The player ID
     * @param chain The interaction chain
     * @return true to block this chain's native behavior
     */
    private boolean processChain(UUID playerId, SyncInteractionChain chain) {
        InteractionType type = chain.interactionType;
        
        switch (type) {
            case SwapTo -> {
                // Player pressing a hotbar key (slot index is in activeHotbarSlot)
                int slotIndex = chain.activeHotbarSlot;
                return handleSlotSelection(playerId, slotIndex);
            }
            case SwapFrom -> {
                // Player swapping from current slot (check data for target)
                if (chain.data != null) {
                    int targetSlot = chain.data.targetSlot;
                    return handleSlotSelection(playerId, targetSlot);
                }
            }
            case Primary, Secondary -> {
                // Player clicking while on a slot
                return handleInteraction(playerId, chain);
            }
            default -> {
                // Other interaction types - don't block
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Handle slot selection events (number keys 1-9, 0).
     */
    private boolean handleSlotSelection(UUID playerId, int slotIndex) {
        BiPredicate<UUID, Integer> filter = slotSelectionFilter;
        LOGGER.info("[HOTBAR-INTERCEPT] handleSlotSelection: player={}, slot={}, filterRegistered={}", 
            playerId, slotIndex, filter != null);
        
        if (filter == null) {
            LOGGER.info("[HOTBAR-INTERCEPT] No slot filter registered, allowing slot selection");
            return false; // No filter registered, allow all
        }
        
        try {
            boolean shouldBlock = filter.test(playerId, slotIndex);
            LOGGER.info("[HOTBAR-INTERCEPT] Filter result for slot {}: shouldBlock={}", slotIndex, shouldBlock);
            if (shouldBlock) {
                LOGGER.info("[HOTBAR-INTERCEPT] BLOCKED slot {} selection for player {}", slotIndex, playerId);
            } else {
                // Track the new current slot — only store valid hotbar indices (0-8)
                // Negative values (e.g. -5 from SwapFrom chain.data.targetSlot) are
                // internal Hytale representations, NOT valid hotbar slots.
                if (slotIndex >= 0 && slotIndex < 9) {
                    currentSlots.put(playerId, slotIndex);
                    LOGGER.info("[HOTBAR-INTERCEPT] ALLOWED slot {} selection, tracking new slot", slotIndex);
                } else {
                    LOGGER.info("[HOTBAR-INTERCEPT] ALLOWED slot {} selection (not tracked — out of valid range 0-8)", slotIndex);
                }
            }
            return shouldBlock;
        } catch (Exception e) {
            LOGGER.error("[HOTBAR-INTERCEPT] Error in slot selection filter for player {} slot {}", 
                playerId, slotIndex, e);
            return false; // On error, allow the action
        }
    }
    
    /**
     * Handle interaction events (primary/secondary click).
     */
    private boolean handleInteraction(UUID playerId, SyncInteractionChain chain) {
        BiPredicate<UUID, InteractionData> filter = interactionFilter;
        if (filter == null) {
            return false; // No filter registered, allow all
        }
        
        try {
            int currentSlot = currentSlots.getOrDefault(playerId, chain.activeHotbarSlot);
            InteractionData data = new InteractionData(
                currentSlot,
                chain.interactionType == InteractionType.Primary,
                chain.initial
            );
            
            return filter.test(playerId, data);
        } catch (Exception e) {
            LOGGER.error("Error in interaction filter for player {}", playerId, e);
            return false; // On error, allow the action
        }
    }
    
    // =========================================================================
    // Client Desync Correction
    // =========================================================================
    
    /**
     * Resync the client's active hotbar slot after blocking a slot switch.
     * 
     * <p>When we block a SyncInteractionChains packet, the client has already
     * performed the slot switch locally. We must send a SetActiveSlot packet
     * to force the client back to the correct slot.</p>
     * 
     * <p>Per SDK docs: "The client performs this action locally *before* the 
     * server confirms it. [...] send SetActiveSlot (packet 177), which explicitly
     * tells the client which slot to select."</p>
     * 
     * <p>Also updates the server-side inventory active slot and resyncs inventory
     * to prevent item desync (items appearing to disappear).</p>
     * 
     * <p><b>Thread Safety:</b> Uses {@code playerRef.getWorldUuid()} (thread-safe)
     * to obtain the World, then schedules all entity/store access on the world thread
     * via {@code world.execute()}. This avoids "Assert not in thread!" errors from
     * calling {@code playerRef.getReference()} on the network/packet filter thread.</p>
     * 
     * @param playerRef The player ref
     * @param correctSlot The slot the player should remain on
     */
    private void resyncClientSlot(PlayerRef playerRef, int correctSlot) {
        // Final safety net: ensure slot is in valid range before any Hytale API call
        if (correctSlot < 0 || correctSlot > 8) {
            LOGGER.error("[HOTBAR-INTERCEPT] resyncClientSlot called with invalid slot {}, aborting", correctSlot);
            return;
        }
        
        try {
            // getWorldUuid() is thread-safe and can be called from any thread
            UUID worldUuid = playerRef.getWorldUuid();
            if (worldUuid == null) {
                LOGGER.warn("[HOTBAR-INTERCEPT] Cannot resync: player {} has no world UUID", 
                    playerRef.getUuid());
                return;
            }
            
            World world = Universe.get().getWorld(worldUuid);
            if (world == null) {
                LOGGER.warn("[HOTBAR-INTERCEPT] Cannot resync: world {} not found for player {}", 
                    worldUuid, playerRef.getUuid());
                return;
            }
            
            // Schedule ALL entity/store access on the world thread (SDK requirement)
            world.execute(() -> {
                try {
                    // Get entity reference ON the world thread where it's valid
                    Ref<EntityStore> entityRef = playerRef.getReference();
                    if (entityRef == null || !entityRef.isValid()) {
                        LOGGER.warn("[HOTBAR-INTERCEPT] Cannot resync: invalid entity ref for player {} (on world thread)", 
                            playerRef.getUuid());
                        return;
                    }
                    
                    Store<EntityStore> store = entityRef.getStore();
                    Player playerComponent = store.getComponent(entityRef, Player.getComponentType());
                    if (playerComponent == null) {
                        LOGGER.warn("[HOTBAR-INTERCEPT] Cannot resync: Player component null for {}", 
                            playerRef.getUuid());
                        return;
                    }
                    
                    // Update server-side active slot state
                    playerComponent.getInventory().setActiveHotbarSlot((byte) correctSlot);
                    
                    // Send SetActiveSlot packet to fix client desync
                    SetActiveSlot setActiveSlotPacket = new SetActiveSlot(
                        Inventory.HOTBAR_SECTION_ID,  // -1 indicates the hotbar
                        correctSlot                    // The slot index to select
                    );
                    playerRef.getPacketHandler().write(setActiveSlotPacket);
                    
                    LOGGER.debug("[HOTBAR-INTERCEPT] Resync complete: player={}, slot={}", 
                        playerRef.getUuid(), correctSlot);
                } catch (Exception e) {
                    LOGGER.error("[HOTBAR-INTERCEPT] Error during slot resync for player {}", 
                        playerRef.getUuid(), e);
                }
            });
        } catch (Exception e) {
            LOGGER.error("[HOTBAR-INTERCEPT] Error scheduling slot resync for player {}", 
                playerRef.getUuid(), e);
        }
    }
    
    // =========================================================================
    // Player Lifecycle
    // =========================================================================
    
    /**
     * Clean up player state when they disconnect.
     */
    public void onPlayerDisconnect(UUID playerId) {
        currentSlots.remove(playerId);
    }
    
    // =========================================================================
    // Data Classes
    // =========================================================================
    
    /**
     * Data class for interaction events.
     */
    public record InteractionData(
        int slotIndex,
        boolean isPrimary,
        boolean isInitial
    ) {}
}
