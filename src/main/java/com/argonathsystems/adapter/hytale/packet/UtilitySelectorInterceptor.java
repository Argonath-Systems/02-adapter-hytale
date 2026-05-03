package com.argonathsystems.adapter.hytale.packet;

import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Packet filter that intercepts the native Utility Slot Selector (circle menu)
 * and dispatches a custom action instead.
 *
 * <p>When a player presses the Utility Slot Selector key (W on AZERTY / Z on QWERTY),
 * Hytale sends a {@link SyncInteractionChains} packet with {@link InteractionType#Use}.
 * This filter blocks that packet and invokes a registered callback, allowing the custom
 * Action Wheel to open instead of the native circle menu.</p>
 *
 * <h2>How It Works</h2>
 * <pre>
 * [Client] W key → SyncInteractionChains(Use, initial=true) → UtilitySelectorInterceptor
 *                                                                     ↓
 *                                                              BLOCK packet (return true)
 *                                                                     ↓
 *                                                              callback.accept(playerId, "argonath:OPEN_ACTION_WHEEL")
 *                                                                     ↓
 *                                                              HytaleInputAccessor.dispatchAction()
 *                                                                     ↓
 *                                                              ActionWheelInputListener → toggleWheel()
 * </pre>
 *
 * <h2>Safety: Avoiding False Positives</h2>
 * <p>The {@code InteractionType.Use} interaction is also generated for item use / F-key
 * interactions. To distinguish the Utility Selector from general Use interactions, we
 * check {@code chain.initial == true} (only the first Use event in a chain is the
 * selector trigger) and allow enabling/disabling per-player.</p>
 *
 * <p>If false positives are observed (e.g., item Use interactions are also blocked),
 * the filter can be refined with additional heuristics or completely disabled for
 * specific players via {@link #disableForPlayer(UUID)}.</p>
 *
 * @author Argonath Systems Team
 * @version 1.0.0
 * @since SM-UI-055
 * @see HotbarInteractionAdapter
 */
public class UtilitySelectorInterceptor implements PlayerPacketFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(UtilitySelectorInterceptor.class);

    /**
     * Callback invoked when the utility selector packet is intercepted.
     * BiConsumer<UUID playerId, String actionId>
     *
     * <p>Typically wired to {@code HytaleInputAccessor::dispatchAction}.</p>
     */
    private volatile BiConsumer<UUID, String> interceptCallback;

    /**
     * The action ID to dispatch when the utility selector is intercepted.
     */
    private static final String ACTION_ID = "argonath:OPEN_ACTION_WHEEL";

    /**
     * Players for whom interception is enabled.
     * By default, interception is enabled for ALL players. If you want per-player
     * control, add players to this set and check membership in test().
     *
     * <p>Current design: interception is globally enabled once a callback is set.
     * Per-player exclusions can be added via {@link #disableForPlayer(UUID)}.</p>
     */
    private final Set<UUID> excludedPlayers = ConcurrentHashMap.newKeySet();

    /**
     * Whether the interceptor is globally active. It becomes active once a
     * callback is registered via {@link #setInterceptCallback(BiConsumer)}.
     */
    private volatile boolean active = false;

    public UtilitySelectorInterceptor() {
        LOGGER.info("[UTILITY-INTERCEPT] UtilitySelectorInterceptor initialized");
    }

    /**
     * Set the callback to invoke when the utility selector packet is intercepted.
     *
     * <p>This callback receives (playerId, actionId) and should dispatch the action
     * to the input handler chain (i.e., call {@code dispatchAction(playerId, actionId)}).</p>
     *
     * @param callback The callback function. Setting this activates the interceptor.
     */
    public void setInterceptCallback(BiConsumer<UUID, String> callback) {
        this.interceptCallback = callback;
        this.active = (callback != null);
        LOGGER.info("[UTILITY-INTERCEPT] Intercept callback {}", callback != null ? "set (active)" : "cleared (inactive)");
    }

    /**
     * Exclude a specific player from utility selector interception.
     *
     * <p>Excluded players will have their native utility selector work normally.</p>
     *
     * @param playerId The player to exclude
     */
    public void disableForPlayer(UUID playerId) {
        excludedPlayers.add(playerId);
        LOGGER.debug("[UTILITY-INTERCEPT] Interception disabled for player {}", playerId);
    }

    /**
     * Re-enable utility selector interception for a previously excluded player.
     *
     * @param playerId The player to re-enable
     */
    public void enableForPlayer(UUID playerId) {
        excludedPlayers.remove(playerId);
        LOGGER.debug("[UTILITY-INTERCEPT] Interception re-enabled for player {}", playerId);
    }

    /**
     * Check if the interceptor is currently active.
     *
     * @return true if a callback is set and the interceptor is active
     */
    public boolean isActive() {
        return active;
    }

    @Override
    public boolean test(@Nonnull PlayerRef playerRef, @Nonnull Packet packet) {
        // Fast exit if not active or not the right packet type
        if (!active || !(packet instanceof SyncInteractionChains syncPacket)) {
            return false;
        }

        UUID playerId = playerRef.getUuid();

        // Don't intercept for excluded players
        if (excludedPlayers.contains(playerId)) {
            return false;
        }

        // Check each interaction chain for a Use interaction
        if (syncPacket.updates == null) {
            return false;
        }

        for (SyncInteractionChain chain : syncPacket.updates) {
            if (chain.interactionType == InteractionType.Use && chain.initial) {
                // This is the utility selector trigger!
                LOGGER.debug("[UTILITY-INTERCEPT] Intercepted Use interaction (initial=true) for player {}", playerId);

                // Dispatch the action wheel toggle via the registered callback
                BiConsumer<UUID, String> cb = interceptCallback;
                if (cb != null) {
                    try {
                        cb.accept(playerId, ACTION_ID);
                    } catch (Exception e) {
                        LOGGER.error("[UTILITY-INTERCEPT] Error in intercept callback for player {}", playerId, e);
                    }
                }

                // BLOCK the packet to prevent the native utility selector from opening
                return true;
            }
        }

        // Not a Use interaction — allow the packet through
        return false;
    }

    /**
     * Clean up player state on disconnect.
     *
     * @param playerId The player who disconnected
     */
    public void onPlayerDisconnect(UUID playerId) {
        excludedPlayers.remove(playerId);
    }
}
