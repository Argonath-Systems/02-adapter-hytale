package com.argonathsystems.adapter.hytaleadapter.ui;

import au.ellie.hyui.builders.HudBuilder;
import au.ellie.hyui.builders.HyUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapter for managing combat frame UI elements using HyUI.
 * Combat frames are HUD elements displayed when a player enters combat mode.
 * 
 * <p><b>Thread Safety:</b> This adapter uses ConcurrentHashMap for thread-safe player tracking.
 * HUD operations should be executed on the world thread via world.execute().</p>
 * 
 * @since 2.1.0
 */
public class CombatFramesAdapter {
    private final Map<UUID, HyUIHud> activeCombatFrames = new ConcurrentHashMap<>();
    
    /**
     * Shows a combat frame indicator to the specified player.
     * Creates a new HUD element anchored to the top-right of the screen.
     * 
     * @param playerRef the player to show the combat frame to
     */
    public void showCombatFrame(PlayerRef playerRef) {
        // Remove existing combat frame if present
        hideCombatFrame(playerRef);
        
        // Create new combat frame HUD
        HyUIHud hud = HudBuilder.detachedHud()
            .fromHtml("""
                <div style="anchor-top: 100; anchor-right: 10;">
                    <div id="combat-frame" style="background-color: #1a1a1a; padding: 10;">
                        <label id="combat-status">⚔ In Combat</label>
                    </div>
                </div>
                """)
            .show(playerRef);
        
        activeCombatFrames.put(playerRef.getUuid(), hud);
    }
    
    /**
     * Hides the combat frame for the specified player.
     * 
     * @param playerRef the player whose combat frame should be hidden
     */
    public void hideCombatFrame(PlayerRef playerRef) {
        HyUIHud hud = activeCombatFrames.remove(playerRef.getUuid());
        if (hud != null) {
            hud.remove();
        }
    }
}
