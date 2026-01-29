package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.NotificationAccessor;

import java.util.UUID;

/**
 * <p><b>MIGRATION-001 Status:</b> BLOCKED - Requires Official Hytale SDK</p>
 * 
 * <p>This accessor implements Notifications functionality.</p>
 * <p>Implementation requires the official Hytale SDK (com.hypixel.hytale.*)
 * which is not available in the development environment.</p>
 * 
 * <p>All methods throw UnsupportedOperationException until the SDK is available.</p>
 * 
 * @see <a href="file://../../../docs/migration-001/PHASE-3-IMPLEMENTATION-STATUS.md">Phase 3 Status</a>
 */
public class HytaleNotificationAccessor implements NotificationAccessor {
    private final Object /* Server */ server;
    
    public HytaleNotificationAccessor(Object /* Server */ server) { 
        this.server = server; 
    }
    
    @Override 
    public void sendNotification(UUID playerId, String title, String message, String type) {
        throw new UnsupportedOperationException(
            "NotificationAccessor.sendNotification(UUID, String, String, String) requires Hytale SDK: " +
            "HytaleServer.get().getPlayer(playerId).sendNotification(title, message, type)"
        );
    }

    @Override
    public void sendNotification(UUID playerId, String title, String message) {
        throw new UnsupportedOperationException(
            "NotificationAccessor.sendNotification(UUID, String, String) requires Hytale SDK: " +
            "HytaleServer.get().getPlayer(playerId).sendNotification(title, message)"
        );
    }
}