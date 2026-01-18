package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.NotificationAccessor;
import com.hytale.api.Server;
import java.util.UUID;

public class HytaleNotificationAccessor implements NotificationAccessor {
    private final Server server;
    public HytaleNotificationAccessor(Server server) { this.server = server; }
    
    @Override public void sendNotification(UUID playerId, String title, String message, String icon) {}

    @Override
    public void sendNotification(UUID playerId, String title, String message) {
    }
}