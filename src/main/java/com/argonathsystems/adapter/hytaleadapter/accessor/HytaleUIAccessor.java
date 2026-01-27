package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.adapter.hytaleadapter.util.PlayerRefCache;
import com.argonathsystems.framework.accessorapi.UIAccessor;
import com.argonathsystems.framework.accessorapi.ui.HudLayoutData;
import com.argonathsystems.framework.accessorapi.ui.UIContext;
import com.argonathsystems.framework.accessorapi.ui.UIUpdateData;
import com.hytale.api.Server;
import com.hytale.api.entity.Player;

import java.util.Map;
import java.util.UUID;

/**
 * Hytale implementation of UIAccessor.
 * 
 * @version 2.0.0 - Updated to use type-safe UI types
 */
public class HytaleUIAccessor implements UIAccessor {
    private final Server server;
    private final Map<String, String> registeredUIs = new java.util.concurrent.ConcurrentHashMap<>();
    
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
        Player player = PlayerRefCache.get(playerId);
        if (player != null) {
            // TODO: Implement using actual Hytale HUD API when available
            // For now, use generic UI system as fallback
            player.openUI(hudId, content);
        }
    }
    
    @Override
    public void removeHud(UUID playerId, String hudId) {
        Player player = PlayerRefCache.get(playerId);
        if (player != null) {
            // TODO: Implement using actual Hytale HUD API when available
            player.closeUI();
        }
    }
    
    @Override
    public void updateHud(UUID playerId, String hudId, String content) {
        Player player = PlayerRefCache.get(playerId);
        if (player != null) {
            // TODO: Implement using actual Hytale HUD API when available
            player.sendUIUpdate(hudId, content);
        }
    }
    
    @Override
    public void updateHudLayout(UUID playerId, HudLayoutData layoutData) {
        Player player = PlayerRefCache.get(playerId);
        if (player != null) {
            // Convert HudLayoutData to Map<String, Object> for platform
            Map<String, Object> platformData = new java.util.HashMap<>();
            layoutData.elements().forEach((id, pos) -> {
                platformData.put(id, Map.of(
                    "x", pos.x(),
                    "y", pos.y(),
                    "width", pos.width(),
                    "height", pos.height(),
                    "anchor", pos.anchor(),
                    "visible", pos.visible()
                ));
            });
            
            // TODO: Implement HUD layout updates when Hytale API supports it
            player.sendUIUpdate("hud_layout", platformData);
        }
    }
    
    @Override
    public void openHudEditor(UUID playerId) {
        Player player = PlayerRefCache.get(playerId);
        if (player != null) {
            // TODO: Implement HUD editor when Hytale API supports it
            player.openUI("hud_editor", null);
        }
    }
    
    @Override
    public void closeHudEditor(UUID playerId) {
        Player player = PlayerRefCache.get(playerId);
        if (player != null) {
            // TODO: Close HUD editor when implemented
            player.closeUI();
        }
    }
    
    @Override
    public boolean isInHudEditMode(UUID playerId) {
        Player player = PlayerRefCache.get(playerId);
        if (player != null) {
            // TODO: Track HUD edit mode state when implemented
            return player.hasUIOpen("hud_editor");
        }
        return false;
    }
}
