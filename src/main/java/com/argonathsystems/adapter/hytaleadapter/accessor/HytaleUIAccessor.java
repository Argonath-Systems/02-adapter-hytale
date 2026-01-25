package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.adapter.hytaleadapter.util.PlayerRefCache;
import com.argonathsystems.framework.accessorapi.UIAccessor;
import com.hytale.api.Server;
import com.hytale.api.entity.Player;

import java.util.Map;
import java.util.UUID;

public class HytaleUIAccessor implements UIAccessor {
    private final Server server;
    
    public HytaleUIAccessor(Server server) { 
        this.server = server; 
    }
    
    @Override 
    public void openUI(UUID playerId, String uiId, Object context) {
        Player player = PlayerRefCache.get(playerId);
        if (player != null) {
            player.openUI(uiId, context);
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
    public void sendUIUpdate(UUID playerId, String elementId, Object data) {
        Player player = PlayerRefCache.get(playerId);
        if (player != null) {
            player.sendUIUpdate(elementId, data);
        }
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
    public void updateHudLayout(UUID playerId, Map<String, Object> layoutData) {
        Player player = PlayerRefCache.get(playerId);
        if (player != null) {
            // TODO: Implement HUD layout updates when Hytale API supports it
            player.sendUIUpdate("hud_layout", layoutData);
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
