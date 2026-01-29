package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.adapter.hytaleadapter.util.PlayerRefCache;
import com.argonathsystems.framework.accessorapi.UIAccessor;
import com.argonathsystems.framework.accessorapi.ui.HudLayoutData;
import com.argonathsystems.framework.accessorapi.ui.UIContext;
import com.argonathsystems.framework.accessorapi.ui.UIUpdateData;
import com.hytale.api.Server;
import com.hytale.api.entity.Player;
import com.hytale.api.entity.PlayerRef;
import com.hytale.ui.HudBuilder;
import com.hytale.ui.HyUIHud;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hytale implementation of UIAccessor.
 * 
 * @version 2.0.0 - Updated to use type-safe UI types
 */
public class HytaleUIAccessor implements UIAccessor {
    private final Server server;
    private final Map<String, String> registeredUIs = new ConcurrentHashMap<>();
    /** Active HUD instances per player, keyed by (playerId + "-" + hudId) */
    private final Map<String, HyUIHud> activeHuds = new ConcurrentHashMap<>();
    
    public HytaleUIAccessor(Server server) { 
        this.server = server; 
    }
    
    @Override
    public void registerUI(String uiId, String uiDef) {
        registeredUIs.put(uiId, uiDef);
        // TODO In a real implementation, we would register this with the Hytale server
        // so it knows about the UI ID and its definition.
        // server.registerUI(uiId, uiDef);
    }
    
    @Override 
    public void openUI(UUID playerId, String uiId, UIContext context) {
        Player player = PlayerRefCache.get(playerId);
        if (player != null) {
            // If we have a registered definition and no context, pass the definition
            // This mimics the behavior in addHud where content is passed as the object
            if (context == null && registeredUIs.containsKey(uiId)) {
                player.openUI(uiId, registeredUIs.get(uiId));
            } else {
                // Note: Hytale SDK still expects Object, so we pass the UIContext as-is
                player.openUI(uiId, context);
            }
        }
    }

    @Override 
    public void closeUI(UUID playerId) {
        Player player = PlayerRefCache.get(playerId);
        if (player != null) {
            player.closeUI();
        }
    }

    @Override
    public boolean hasUIOpen(UUID playerId, String uiId) {
        Player player = PlayerRefCache.get(playerId);
        if (player != null) {
            return player.hasUIOpen(uiId);
        }
        return false;
    }

    @Override
    public void sendUIUpdate(UUID playerId, String elementId, UIUpdateData data) {
        Player player = PlayerRefCache.get(playerId);
        if (player != null) {
            // Convert UIUpdateData to platform-appropriate format
            Object platformData = convertUIUpdateData(data);
            player.sendUIUpdate(elementId, platformData);
        }
    }
    
    /**
     * Convert type-safe UIUpdateData to platform Object format.
     */
    private Object convertUIUpdateData(UIUpdateData data) {
        return switch (data) {
            case UIUpdateData.Visibility v -> Map.of("visible", v.visible(), "enabled", v.enabled());
            case UIUpdateData.Text t -> t.value();
            case UIUpdateData.Value v -> v.value();
            case UIUpdateData.Progress p -> Map.of("current", p.current(), "max", p.max());
            case UIUpdateData.ListData l -> l.items();
            case UIUpdateData.MapData m -> m.data();
        };
    }
    
    // === HUD-specific method implementations ===
    
    @Override
    public void addHud(UUID playerId, String hudId, String content) {
        PlayerRef playerRef = PlayerRefCache.getRef(playerId);
        if (playerRef != null) {
            // Build and display HUD using HyUI HudBuilder API
            HyUIHud hud = HudBuilder.hudForPlayer(playerRef)
                .fromHtml(content)
                .withRefreshRate(100)
                .show();
            
            // Track active HUD for later update/removal
            activeHuds.put(playerId + "-" + hudId, hud);
        }
    }
    
    @Override
    public void removeHud(UUID playerId, String hudId) {
        String hudKey = playerId + "-" + hudId;
        HyUIHud hud = activeHuds.remove(hudKey);
        if (hud != null) {
            hud.hide();
        }
    }
    
    @Override
    public void updateHud(UUID playerId, String hudId, String content) {
        String hudKey = playerId + "-" + hudId;
        HyUIHud existingHud = activeHuds.get(hudKey);
        if (existingHud != null) {
            // Update existing HUD content
            existingHud.updateContent(content);
        } else {
            // HUD doesn't exist, create it
            addHud(playerId, hudId, content);
        }
    }
    
    @Override
    public void updateHudLayout(UUID playerId, HudLayoutData layoutData) {
        // Update layout for each tracked HUD element
        layoutData.elements().forEach((hudId, pos) -> {
            String hudKey = playerId + "-" + hudId;
            HyUIHud hud = activeHuds.get(hudKey);
            if (hud != null) {
                // Apply position from layout data
                hud.setPosition(pos.x(), pos.y());
                hud.setVisible(pos.visible());
            }
        });
    }
    
    @Override
    public void openHudEditor(UUID playerId) {
        PlayerRef playerRef = PlayerRefCache.getRef(playerId);
        if (playerRef != null) {
            // Open HUD layout editor UI
            addHud(playerId, "hud_editor", buildHudEditorHtml());
        }
    }
    
    @Override
    public void closeHudEditor(UUID playerId) {
        removeHud(playerId, "hud_editor");
    }
    
    @Override
    public boolean isInHudEditMode(UUID playerId) {
        return activeHuds.containsKey(playerId + "-" + "hud_editor");
    }
    
    /**
     * Builds the HUD editor HTML content.
     */
    private String buildHudEditorHtml() {
        return """
            <div class="hud-editor-overlay">
                <div class="hud-editor-header">HUD Layout Editor</div>
                <div class="hud-editor-hint">Drag elements to reposition. Press F7 to save and exit.</div>
            </div>
            """;
    }
}
