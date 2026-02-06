package com.argonathsystems.adapter.hytaleadapter;

import com.argonathsystems.adapter.hytale.ecs.ArgonathComponentSyncService;
import com.argonathsystems.framework.accessorapi.EventAccessor;
import com.argonathsystems.framework.accessorapi.event.PlayerJoinEvent;
import com.argonathsystems.framework.accessorapi.event.PlayerQuitEvent;
import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Bridges Hytale SDK native events to Argonath Framework AccessorEvents.
 * 
 * <p>This bridge listens for SDK-level events (like {@code PlayerConnectEvent}) and
 * emits corresponding framework-level events (like {@code PlayerJoinEvent}) through
 * the {@link EventAccessor}. This allows platform-agnostic mods to listen for player
 * lifecycle events without direct SDK dependencies.</p>
 * 
 * <h2>Bridged Events</h2>
 * <table>
 *   <tr><th>SDK Event</th><th>Framework Event</th><th>Trigger</th></tr>
 *   <tr><td>{@code PlayerConnectEvent}</td><td>{@code PlayerJoinEvent}</td><td>Player connected to server</td></tr>
 *   <tr><td>{@code PlayerDisconnectEvent}</td><td>{@code PlayerQuitEvent}</td><td>Player disconnected</td></tr>
 * </table>
 * 
 * <h2>Event Timing</h2>
 * <p>{@code PlayerConnectEvent} fires when the player connects and their entity is created.
 * The player object should be available via {@code getPlayer()}. For UI operations that
 * require the player to be fully in-world, consider adding a small delay or using
 * scheduler to defer HUD creation.</p>
 * 
 * @author Argonath Systems
 * @version 1.0.0
 * @since 2026-02-01
 * @see EventAccessor
 * @see PlayerJoinEvent
 * @see PlayerQuitEvent
 */
public class HytaleEventBridge {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HytaleEventBridge.class);
    
    private final HytaleServer server;
    private final EventAccessor eventAccessor;
    private final List<EventRegistration<?, ?>> registrations = new ArrayList<>();
    
    /**
     * Create a new event bridge.
     * 
     * @param server Hytale server instance
     * @param eventAccessor Framework event accessor for emitting events
     */
    public HytaleEventBridge(HytaleServer server, EventAccessor eventAccessor) {
        this.server = server;
        this.eventAccessor = eventAccessor;
    }
    
    /**
     * Register all SDK event listeners and start bridging to framework events.
     * 
     * <p>Should be called during framework initialization, after the EventAccessor
     * is available.</p>
     * 
     * <p><b>Note:</b> PlayerReadyEvent is registered in HytaleAdapterPlugin using
     * getEventRegistry().registerGlobal() because PlayerReadyEvent extends PlayerEvent,
     * not IBaseEvent, and cannot be registered via EventBus.register().</p>
     */
    public void registerBridges() {
        LOGGER.info("Registering SDK → Framework event bridges...");
        
        EventBus eventBus = server.getEventBus();
        
        // Bridge PlayerConnectEvent → PlayerJoinEvent
        // PlayerConnectEvent implements IEvent<Void> and fires when player connects
        // Note: Player may still be loading, but getPlayer() is available
        EventRegistration<?, ?> connectReg = eventBus.register(
            PlayerConnectEvent.class,
            this::handlePlayerConnect
        );
        registrations.add(connectReg);
        LOGGER.debug("  ✓ PlayerConnectEvent → PlayerJoinEvent bridge registered");
        
        // Note: PlayerReadyEvent bridge is registered in HytaleAdapterPlugin
        // because it extends PlayerEvent (not IBaseEvent) and requires
        // JavaPlugin.getEventRegistry().registerGlobal() for registration.
        
        // Bridge PlayerDisconnectEvent → PlayerQuitEvent
        // PlayerDisconnectEvent extends PlayerRefEvent<Void>
        EventRegistration<?, ?> disconnectReg = eventBus.register(
            PlayerDisconnectEvent.class,
            this::handlePlayerDisconnect
        );
        registrations.add(disconnectReg);
        LOGGER.debug("  ✓ PlayerDisconnectEvent → PlayerQuitEvent bridge registered");
        
        LOGGER.info("SDK → Framework event bridges registered: {} bridges active", registrations.size());
    }
    
    /**
     * Unregister all event bridges.
     * 
     * <p>Should be called during framework shutdown.</p>
     */
    public void unregisterBridges() {
        LOGGER.info("Unregistering SDK → Framework event bridges...");
        
        for (EventRegistration<?, ?> registration : registrations) {
            try {
                registration.unregister();
            } catch (Exception e) {
                LOGGER.warn("Failed to unregister bridge: {}", e.getMessage());
            }
        }
        registrations.clear();
        
        LOGGER.info("SDK → Framework event bridges unregistered");
    }
    
    // === Event Handlers ===
    
    /**
     * Handle PlayerConnectEvent from SDK, emit PlayerJoinEvent to framework.
     * 
     * <p>Note: ECS component sync is NOT done here because the player's EntityStore
     * is not fully ready until PlayerReadyEvent. The sync is triggered from
     * FrameworkLoaderPlugin's PlayerReadyEvent handler.</p>
     */
    private void handlePlayerConnect(PlayerConnectEvent event) {
        try {
            Player player = event.getPlayer();
            if (player == null) {
                LOGGER.warn("PlayerConnectEvent received with null player, skipping bridge");
                return;
            }
            
            UUID playerId = player.getUuid();
            String playerName = player.getDisplayName();
            
            LOGGER.debug("Bridging PlayerConnectEvent → PlayerJoinEvent for {} ({})", playerName, playerId);
            
            // Note: ECS sync happens in PlayerReadyEvent when store is fully available
            
            // Create and emit framework event
            PlayerJoinEvent joinEvent = new PlayerJoinEvent(playerId, playerName);
            eventAccessor.emit(joinEvent);
            
            LOGGER.debug("PlayerJoinEvent emitted for player {}", playerName);
            
        } catch (Exception e) {
            LOGGER.error("Failed to bridge PlayerConnectEvent to PlayerJoinEvent", e);
        }
    }
    
    /**
     * Handle PlayerDisconnectEvent from SDK, emit PlayerQuitEvent to framework.
     * Also triggers ECS component sync to save player data to EntityStore.
     */
    private void handlePlayerDisconnect(PlayerDisconnectEvent event) {
        try {
            PlayerRef playerRef = event.getPlayerRef();
            if (playerRef == null) {
                LOGGER.warn("PlayerDisconnectEvent received with null playerRef, skipping bridge");
                return;
            }
            
            UUID playerId = playerRef.getUuid();
            String playerName = playerRef.getUsername();
            String disconnectReason = event.getDisconnectReason() != null 
                ? event.getDisconnectReason().toString() 
                : "Unknown";
            
            LOGGER.debug("Bridging PlayerDisconnectEvent → PlayerQuitEvent for {} ({}), reason: {}", 
                playerName, playerId, disconnectReason);
            
            // Sync POJOs back to ECS components before player leaves
            // This saves updated data to EntityStore for persistence
            try {
                ArgonathComponentSyncService.onPlayerQuit(playerId);
                LOGGER.debug("ECS component sync (save) completed for player {}", playerName);
            } catch (Exception e) {
                LOGGER.warn("ECS component sync (save) failed for player {} (non-fatal): {}", playerName, e.getMessage());
                // Non-fatal - continue with event emission
            }
            
            // Create and emit framework event
            PlayerQuitEvent quitEvent = new PlayerQuitEvent(playerId, playerName + " left the game");
            eventAccessor.emit(quitEvent);
            
            LOGGER.debug("PlayerQuitEvent emitted for player {}", playerName);
            
        } catch (Exception e) {
            LOGGER.error("Failed to bridge PlayerDisconnectEvent to PlayerQuitEvent", e);
        }
    }
}
