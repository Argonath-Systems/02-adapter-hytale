package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.adapter.hytaleadapter.util.PlayerRefCache;
import com.argonathsystems.framework.accessorapi.NotificationAccessor;
import com.hytale.api.Server;
import com.hytale.api.entity.Player;

import java.util.UUID;

public class HytaleNotificationAccessor implements NotificationAccessor {
    private final Server server;
    public HytaleNotificationAccessor(Server server) { this.server = server; }
    
    @Override 
    public void sendNotification(UUID playerId, String title, String message, String icon) {
        Player player = PlayerRefCache.get(playerId);
        if (player != null) {
            player.sendNotification(title, message, icon);
        }
    }

    @Override
    public void sendNotification(UUID playerId, String title, String message) {
        sendNotification(playerId, title, message, null); // Default or null icon
    }
}