package com.argonathsystems.adapter.hytaleadapter.ui;

import au.ellie.hyui.builders.HudBuilder;
import au.ellie.hyui.builders.HyUIHud;
import au.ellie.hyui.builders.LabelBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic action bar HUD adapter for the Hytale platform.
 * Action bars are non-intrusive HUD elements displayed near the bottom of the player's screen.
 * 
 * <p><b>Thread Safety:</b> This adapter uses ConcurrentHashMap for thread-safe player tracking.
 * HUD operations should be executed on the world thread via world.execute().</p>
 * 
 * <p><b>Architectural Note:</b> This adapter remains in the adapter layer (not moved to framework)
 * because it contains HyUI imports which are platform-specific. The adapter layer is the ONLY place
 * for Hytale/HyUI platform dependencies. While this is a generic utility usable by multiple mods,
 * it must stay in the adapter to maintain platform agnosticism in the framework layer.</p>
 * 
 * @since 2.1.0
 */
public class ActionBarAdapter {
    private final Map<UUID, HyUIHud> activeActionBars = new ConcurrentHashMap<>();
    
    /**
     * Shows an action bar message to the specified player.
     * Creates a new HUD element anchored near the bottom center of the screen.
     * 
     * @param playerRef the player to show the action bar to
     * @param message the message to display
     */
    public void showActionBar(PlayerRef playerRef, String message) {
        // Remove existing action bar if present
        hideActionBar(playerRef);
        
        // Create new action bar HUD
        HyUIHud hud = HudBuilder.detachedHud()
            .fromHtml("""
                <div style="anchor-bottom: 60; anchor-center-horizontal: true;">
                    <label id="action-text">%s</label>
                </div>
                """.formatted(escapeHtml(message)))
            .show(playerRef);
        
        activeActionBars.put(playerRef.getUuid(), hud);
    }
    
    /**
     * Hides the action bar for the specified player.
     * 
     * @param playerRef the player whose action bar should be hidden
     */
    public void hideActionBar(PlayerRef playerRef) {
        HyUIHud hud = activeActionBars.remove(playerRef.getUuid());
        if (hud != null) {
            hud.remove();
        }
    }
    
    /**
     * Updates the message of an existing action bar.
     * If no action bar is currently shown, this method has no effect.
     * 
     * @param playerRef the player whose action bar should be updated
     * @param message the new message to display
     */
    public void updateActionBar(PlayerRef playerRef, String message) {
        HyUIHud hud = activeActionBars.get(playerRef.getUuid());
        if (hud != null) {
            hud.getById("action-text", LabelBuilder.class).ifPresent(label -> {
                label.withText(escapeHtml(message));
            });
        }
    }
    
    /**
     * Escapes HTML special characters to prevent injection.
     * 
     * @param text the text to escape
     * @return the escaped text
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
