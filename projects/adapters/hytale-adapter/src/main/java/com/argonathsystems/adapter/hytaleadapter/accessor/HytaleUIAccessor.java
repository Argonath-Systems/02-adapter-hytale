package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.adapter.hytaleadapter.util.PlayerRefCache;
import com.argonathsystems.framework.accessorapi.UIAccessor;
import com.hytale.api.Server;
import com.hytale.api.entity.Player;

import java.util.UUID;

public class HytaleUIAccessor implements UIAccessor {
    private final Server server; // Kept for consistency, though unused directly if using cache
    public HytaleUIAccessor(Server server) { this.server = server; }
    
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
}