package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.adapter.hytale.packet.DebugPacketWatcher;
import com.argonathsystems.adapter.hytale.packet.HotbarInteractionAdapter;
import com.argonathsystems.adapter.hytale.packet.UtilitySelectorInterceptor;
import com.argonathsystems.framework.accessorapi.InputAccessor;
import com.hypixel.hytale.server.core.HytaleServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Hytale implementation of InputAccessor for key action handling.
 * 
 * <p><b>MIGRATION-001 Status:</b> ✅ IMPLEMENTED</p>
 * 
 * <h2>Architecture</h2>
 * <p>Hytale uses an action-based input system. Keys are mapped to "Actions" in the
 * asset pack's {@code input.json}. When a key is pressed, the client sends an action
 * event to the server rather than raw key data.</p>
 * 
 * <h2>Input Flow</h2>
 * <pre>
 * [Client] Key Press → [input.json] Action Mapping → [Server] Action Event
 *                                                            ↓
 *                                                    HytaleInputAccessor
 *                                                            ↓
 *                                                    KeyActionHandler
 * </pre>
 * 
 * <h2>Asset Pack Registration</h2>
 * <p>For custom actions, define them in your asset pack's {@code config/input.json}:
 * <pre>{@code
 * {
 *   "groups": [{
 *     "name": "Argonath Admin",
 *     "actions": [{
 *       "id": "npc_admin_toggle",
 *       "key": "N",
 *       "modifiers": ["SHIFT"],
 *       "context": "gameplay"
 *     }]
 *   }]
 * }
 * }</pre>
 * 
 * <h2>SDK Integration Notes</h2>
 * <p>The Hytale Server SDK does not directly expose input events. Input handling
 * is client-side, with actions translated to commands or UI events. This accessor
 * provides a framework for when/if input events become available.</p>
 * 
 * <h2>Hotbar-Based Cycling</h2>
 * <p>For hotbar-based cycling keybinds, see the {@code 06-mod-dual-hotbar} mod
 * which implements SF-ARCHITECTURE-028 as a standalone mod.</p>
 * 
 * <p>Current workarounds for input handling:</p>
 * <ul>
 *   <li>Use command-based triggers ({@code /anpc admin})</li>
 *   <li>Use item interactions (wand clicks)</li>
 *   <li>Use UI button events via HyUI</li>
 * </ul>
 * 
 * @author Argonath Systems Team
 * @version 1.2.0
 * @since SF-NPC-051
 */
public class HytaleInputAccessor implements InputAccessor {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HytaleInputAccessor.class);
    
    /** Counter for generating unique registration IDs */
    private static final AtomicLong REGISTRATION_ID_COUNTER = new AtomicLong(0);
    
    /** Map of action ID -> list of handlers (sorted by priority) */
    private final Map<String, List<HandlerEntry>> handlersByAction = new ConcurrentHashMap<>();
    
    /** Map of registration ID -> handler entry for fast unregistration */
    private final Map<Long, HandlerEntry> registrationsById = new ConcurrentHashMap<>();
    
    /** Hotbar interaction adapter for slot filtering */
    private final HotbarInteractionAdapter hotbarInteractionAdapter;
    
    /** Utility selector interceptor for blocking native circle menu and dispatching custom action */
    private final UtilitySelectorInterceptor utilitySelectorInterceptor;
    
    /** Debug packet watcher for discovering packet types (temporary development tool) */
    private final DebugPacketWatcher debugPacketWatcher;
    
    /** List of active hotbar slot filters */
    private final List<HotbarSlotFilterEntry> hotbarSlotFilters = new CopyOnWriteArrayList<>();
    
    /** Reference to HytaleServer for potential future SDK integration */
    @SuppressWarnings("unused")
    private final HytaleServer server;

    public HytaleInputAccessor(Object server) {
        this.server = (HytaleServer) server;
        this.hotbarInteractionAdapter = new HotbarInteractionAdapter();
        this.utilitySelectorInterceptor = new UtilitySelectorInterceptor();
        this.debugPacketWatcher = new DebugPacketWatcher();
        
        // Wire the composite filter to the adapter
        hotbarInteractionAdapter.registerSlotFilter(this::processHotbarSlotFilters);
        
        // Wire the utility selector interceptor callback to dispatchAction()
        // When the interceptor blocks the native utility selector packet,
        // it calls this callback which dispatches the action to registered handlers
        // (e.g., ActionWheelInputListener's "argonath:OPEN_ACTION_WHEEL" handler)
        utilitySelectorInterceptor.setInterceptCallback((playerId, actionId) -> {
            LOGGER.info("Utility selector intercepted for player {} — dispatching action: {}", playerId, actionId);
            dispatchAction(playerId, actionId);
        });
        
        LOGGER.info("HytaleInputAccessor initialized - ready for input action handling");
    }
    
    /**
     * Get the hotbar interaction adapter for packet registration.
     * 
     * <p>This adapter should be registered with the packet adapter system
     * during server initialization.</p>
     * 
     * @return The hotbar interaction adapter
     */
    public HotbarInteractionAdapter getHotbarInteractionAdapter() {
        return hotbarInteractionAdapter;
    }
    
    /**
     * Get the utility selector interceptor for packet registration.
     *
     * <p>This interceptor should be registered with the packet adapter system
     * during server initialization. It blocks the native Hytale utility slot
     * selector (circle menu) and dispatches a custom action instead.</p>
     *
     * @return The utility selector interceptor
     */
    public UtilitySelectorInterceptor getUtilitySelectorInterceptor() {
        return utilitySelectorInterceptor;
    }
    
    /**
     * Get the debug packet watcher for packet registration.
     *
     * <p>This watcher logs inbound packets for debugging purposes.
     * It is an observe-only watcher that does NOT block any packets.</p>
     *
     * @return The debug packet watcher
     */
    public DebugPacketWatcher getDebugPacketWatcher() {
        return debugPacketWatcher;
    }
    
    /**
     * Process all registered hotbar slot filters.
     * Returns true if ANY filter wants to block the slot switch.
     */
    private boolean processHotbarSlotFilters(UUID playerId, Integer slotIndex) {
        for (HotbarSlotFilterEntry entry : hotbarSlotFilters) {
            try {
                if (entry.filter.test(playerId, slotIndex)) {
                    LOGGER.trace("Hotbar slot {} blocked for player {} by filter {}", 
                        slotIndex, playerId, entry.registrationId);
                    return true; // Block
                }
            } catch (Exception e) {
                LOGGER.error("Error in hotbar slot filter (id={}): {}", 
                    entry.registrationId, e.getMessage(), e);
            }
        }
        // No filter blocked — track the new current slot so previousSlot is correct
        // in subsequent HotbarSlotContext instances
        updateCurrentSlot(playerId, slotIndex);
        return false; // Allow
    }

    // =========================================================================
    // InputAccessor Implementation
    // =========================================================================

    @Override
    public InputRegistration registerKeyAction(String actionId, Consumer<UUID> handler) {
        return registerKeyAction(actionId, handler, 0);
    }

    @Override
    public InputRegistration registerKeyAction(String actionId, Consumer<UUID> handler, int priority) {
        Objects.requireNonNull(actionId, "actionId cannot be null");
        Objects.requireNonNull(handler, "handler cannot be null");
        if (actionId.isBlank()) {
            throw new IllegalArgumentException("actionId cannot be blank");
        }
        
        // Wrap simple consumer in KeyActionHandler
        KeyActionHandler wrappedHandler = (playerId, context) -> handler.accept(playerId);
        
        return registerKeyActionWithContext(actionId, wrappedHandler, priority);
    }

    @Override
    public InputRegistration registerKeyActionWithContext(String actionId, KeyActionHandler handler) {
        return registerKeyActionWithContext(actionId, handler, 0);
    }
    
    /**
     * Internal registration with priority support.
     */
    private InputRegistration registerKeyActionWithContext(String actionId, KeyActionHandler handler, int priority) {
        Objects.requireNonNull(actionId, "actionId cannot be null");
        Objects.requireNonNull(handler, "handler cannot be null");
        if (actionId.isBlank()) {
            throw new IllegalArgumentException("actionId cannot be blank");
        }
        
        long registrationId = REGISTRATION_ID_COUNTER.incrementAndGet();
        HandlerEntry entry = new HandlerEntry(registrationId, actionId, handler, priority);
        
        // Add to action handlers list
        handlersByAction.computeIfAbsent(actionId, k -> new CopyOnWriteArrayList<>())
                .add(entry);
        
        // Sort by priority (descending - higher priority first)
        List<HandlerEntry> handlers = handlersByAction.get(actionId);
        handlers.sort(Comparator.comparingInt(HandlerEntry::priority).reversed());
        
        // Track by ID for unregistration
        registrationsById.put(registrationId, entry);
        
        LOGGER.debug("Registered key action handler: {} (priority={}, id={})", actionId, priority, registrationId);
        
        return new HytaleInputRegistration(registrationId, actionId, this);
    }

    @Override
    public void unregisterKeyAction(InputRegistration registration) {
        if (registration == null || !registration.isActive()) {
            return;
        }
        
        if (registration instanceof HytaleInputRegistration hytaleReg) {
            HandlerEntry entry = registrationsById.remove(hytaleReg.registrationId);
            if (entry != null) {
                List<HandlerEntry> handlers = handlersByAction.get(entry.actionId());
                if (handlers != null) {
                    handlers.remove(entry);
                    if (handlers.isEmpty()) {
                        handlersByAction.remove(entry.actionId());
                    }
                }
                hytaleReg.markInactive();
                LOGGER.debug("Unregistered key action handler: {} (id={})", entry.actionId(), hytaleReg.registrationId);
            }
        }
    }

    @Override
    public void unregisterAllForAction(String actionId) {
        List<HandlerEntry> handlers = handlersByAction.remove(actionId);
        if (handlers != null) {
            for (HandlerEntry entry : handlers) {
                registrationsById.remove(entry.registrationId());
            }
            LOGGER.debug("Unregistered all handlers for action: {} (count={})", actionId, handlers.size());
        }
    }

    @Override
    public void unregisterAll() {
        int count = registrationsById.size();
        handlersByAction.clear();
        registrationsById.clear();
        LOGGER.debug("Unregistered all key action handlers (count={})", count);
    }

    @Override
    public boolean hasHandlers(String actionId) {
        List<HandlerEntry> handlers = handlersByAction.get(actionId);
        return handlers != null && !handlers.isEmpty();
    }

    // =========================================================================
    // Action Dispatch (called by platform integration)
    // =========================================================================

    /**
     * Dispatch a key action event to registered handlers.
     * 
     * <p>This method is called by the platform integration layer when a key action
     * is received from the client. It invokes handlers in priority order, stopping
     * if an event is consumed.</p>
     * 
     * @param playerId Player who triggered the action
     * @param actionId Action identifier
     * @param modifiers Active modifier keys
     * @return true if the event was handled (consumed or had handlers)
     */
    public boolean dispatchAction(UUID playerId, String actionId, Set<ModifierKey> modifiers) {
        List<HandlerEntry> handlers = handlersByAction.get(actionId);
        if (handlers == null || handlers.isEmpty()) {
            return false;
        }
        
        HytaleInputEventContext context = new HytaleInputEventContext(actionId, modifiers);
        
        for (HandlerEntry entry : handlers) {
            try {
                entry.handler().handle(playerId, context);
                
                if (context.isConsumed()) {
                    LOGGER.trace("Action {} consumed by handler (id={})", actionId, entry.registrationId());
                    break;
                }
            } catch (Exception e) {
                LOGGER.error("Error in key action handler for action {}: {}", actionId, e.getMessage(), e);
            }
        }
        
        return true;
    }
    
    /**
     * Simplified dispatch without modifiers.
     */
    public boolean dispatchAction(UUID playerId, String actionId) {
        return dispatchAction(playerId, actionId, Set.of());
    }

    // =========================================================================
    // Modifier Key Enum
    // =========================================================================
    
    /**
     * Modifier keys that can be held during an action.
     */
    public enum ModifierKey {
        SHIFT, CTRL, ALT
    }

    // =========================================================================
    // Internal Types
    // =========================================================================

    /**
     * Internal record for tracking handler entries.
     */
    private record HandlerEntry(
        long registrationId,
        String actionId,
        KeyActionHandler handler,
        int priority
    ) {}

    /**
     * Implementation of InputRegistration.
     */
    private static class HytaleInputRegistration implements InputRegistration {
        private final long registrationId;
        private final String actionId;
        private final HytaleInputAccessor accessor;
        private volatile boolean active = true;
        
        HytaleInputRegistration(long registrationId, String actionId, HytaleInputAccessor accessor) {
            this.registrationId = registrationId;
            this.actionId = actionId;
            this.accessor = accessor;
        }
        
        @Override
        public String getActionId() {
            return actionId;
        }
        
        @Override
        public void unregister() {
            if (active) {
                accessor.unregisterKeyAction(this);
            }
        }
        
        @Override
        public boolean isActive() {
            return active;
        }
        
        void markInactive() {
            active = false;
        }
    }

    /**
     * Implementation of InputEventContext.
     */
    private static class HytaleInputEventContext implements InputEventContext {
        private final String actionId;
        private final long timestamp;
        private final Set<ModifierKey> modifiers;
        private volatile boolean consumed = false;
        
        HytaleInputEventContext(String actionId, Set<ModifierKey> modifiers) {
            this.actionId = actionId;
            this.timestamp = System.currentTimeMillis();
            this.modifiers = modifiers != null ? Set.copyOf(modifiers) : Set.of();
        }
        
        @Override
        public String getActionId() {
            return actionId;
        }
        
        @Override
        public long getTimestamp() {
            return timestamp;
        }
        
        @Override
        public boolean isShiftHeld() {
            return modifiers.contains(ModifierKey.SHIFT);
        }
        
        @Override
        public boolean isCtrlHeld() {
            return modifiers.contains(ModifierKey.CTRL);
        }
        
        @Override
        public boolean isAltHeld() {
            return modifiers.contains(ModifierKey.ALT);
        }
        
        @Override
        public void consume() {
            consumed = true;
        }
        
        @Override
        public boolean isConsumed() {
            return consumed;
        }
    }
    
    // =========================================================================
    // Hotbar Slot Filtering (SM-UI-050)
    // =========================================================================
    
    @Override
    public HotbarFilterRegistration registerHotbarSlotFilter(BiPredicate<UUID, Integer> filter) {
        Objects.requireNonNull(filter, "filter cannot be null");
        
        long registrationId = REGISTRATION_ID_COUNTER.incrementAndGet();
        HotbarSlotFilterEntry entry = new HotbarSlotFilterEntry(registrationId, filter);
        hotbarSlotFilters.add(entry);
        
        LOGGER.debug("Registered hotbar slot filter (id={})", registrationId);
        
        return new HytaleHotbarFilterRegistration(registrationId, this);
    }
    
    @Override
    public HotbarFilterRegistration registerHotbarSlotHandler(HotbarSlotHandler handler) {
        Objects.requireNonNull(handler, "handler cannot be null");
        
        // Wrap the HotbarSlotHandler in a BiPredicate<UUID, Integer> filter
        // that creates a HotbarSlotContext for each invocation
        BiPredicate<UUID, Integer> wrappedFilter = (playerId, slotIndex) -> {
            HotbarSlotContext context = new DefaultHotbarSlotContext(
                playerId, slotIndex,
                currentSlots.getOrDefault(playerId, 0),
                System.currentTimeMillis(),
                true // isInitialInteraction
            );
            return handler.handle(context);
        };
        
        long registrationId = REGISTRATION_ID_COUNTER.incrementAndGet();
        HotbarSlotFilterEntry entry = new HotbarSlotFilterEntry(registrationId, wrappedFilter);
        hotbarSlotFilters.add(entry);
        
        LOGGER.info("Registered hotbar slot handler (id={})", registrationId);
        
        return new HytaleHotbarFilterRegistration(registrationId, this);
    }
    
    /** Track currently selected slots per player for context building */
    private final Map<UUID, Integer> currentSlots = new ConcurrentHashMap<>();
    
    /**
     * Update the tracked current slot for a player.
     * Called when a slot selection is allowed through.
     */
    public void updateCurrentSlot(UUID playerId, int slotIndex) {
        currentSlots.put(playerId, slotIndex);
    }
    
    /**
     * Remove a hotbar slot filter by registration ID.
     */
    private void removeHotbarSlotFilter(long registrationId) {
        hotbarSlotFilters.removeIf(entry -> entry.registrationId == registrationId);
        LOGGER.debug("Unregistered hotbar slot filter (id={})", registrationId);
    }
    
    /**
     * Entry for tracking hotbar slot filters.
     */
    private record HotbarSlotFilterEntry(
        long registrationId,
        BiPredicate<UUID, Integer> filter
    ) {}
    
    /**
     * Implementation of HotbarFilterRegistration.
     */
    private static class HytaleHotbarFilterRegistration implements HotbarFilterRegistration {
        private final long registrationId;
        private final HytaleInputAccessor accessor;
        private volatile boolean active = true;
        
        HytaleHotbarFilterRegistration(long registrationId, HytaleInputAccessor accessor) {
            this.registrationId = registrationId;
            this.accessor = accessor;
        }
        
        @Override
        public void unregister() {
            if (active) {
                accessor.removeHotbarSlotFilter(registrationId);
                active = false;
            }
        }
        
        @Override
        public boolean isActive() {
            return active;
        }
    }
    
    /**
     * Default implementation of HotbarSlotContext for the handler pattern.
     */
    private static class DefaultHotbarSlotContext implements HotbarSlotContext {
        private final UUID playerId;
        private final int targetSlot;
        private final int previousSlot;
        private final long timestamp;
        private final boolean initialInteraction;
        private volatile boolean consumed = false;
        
        DefaultHotbarSlotContext(UUID playerId, int targetSlot, int previousSlot, 
                long timestamp, boolean initialInteraction) {
            this.playerId = playerId;
            this.targetSlot = targetSlot;
            this.previousSlot = previousSlot;
            this.timestamp = timestamp;
            this.initialInteraction = initialInteraction;
        }
        
        @Override
        public UUID getPlayerId() {
            return playerId;
        }
        
        @Override
        public int getTargetSlot() {
            return targetSlot;
        }
        
        @Override
        public int getPreviousSlot() {
            return previousSlot;
        }
        
        @Override
        public long getTimestamp() {
            return timestamp;
        }
        
        @Override
        public boolean isInitialInteraction() {
            return initialInteraction;
        }
        
        @Override
        public void consume() {
            consumed = true;
        }
        
        @Override
        public boolean isConsumed() {
            return consumed;
        }
    }
}
