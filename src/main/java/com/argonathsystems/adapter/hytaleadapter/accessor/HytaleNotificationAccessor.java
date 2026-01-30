package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.NotificationAccessor;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.util.NotificationUtil;

import java.util.UUID;

/**
 * Hytale implementation of NotificationAccessor using SDK notification system.
 * 
 * <p><b>MIGRATION-001 Status:</b> ✅ IMPLEMENTED</p>
 * 
 * <p>SDK Classes Used:</p>
 * <ul>
 *   <li>{@code NotificationUtil} - Static utility for sending notifications</li>
 *   <li>{@code NotificationStyle} - Notification appearance (Default, Danger, Warning, Success)</li>
 *   <li>{@code Universe.get().getPlayer(UUID)} - Get PlayerRef for player lookup</li>
 *   <li>{@code PlayerRef.getPacketHandler()} - Get PacketHandler for sending packets</li>
 *   <li>{@code Message.raw(String)} - Raw text message wrapper</li>
 * </ul>
 * 
 * @author Argonath Systems Team
 * @version 3.0.0
 * @since MIGRATION-001
 */
public class HytaleNotificationAccessor implements NotificationAccessor {
    
    public HytaleNotificationAccessor(Object server) { 
        // Server instance not needed - use Universe.get() for player lookup
    }
    
    @Override 
    public void sendNotification(UUID playerId, String title, String message, String type) {
        if (playerId == null || title == null) {
            return;
        }
        
        PlayerRef playerRef = getPlayerRef(playerId);
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }
        
        PacketHandler connection = playerRef.getPacketHandler();
        if (connection == null) {
            return;
        }
        
        // Convert type string to NotificationStyle
        NotificationStyle style = parseNotificationStyle(type);
        
        // Create messages using Message.raw() for raw text
        Message titleMsg = Message.raw(title);
        Message msgBody = message != null ? Message.raw(message) : null;
        
        // Send notification via NotificationUtil
        if (msgBody != null) {
            NotificationUtil.sendNotification(connection, titleMsg, msgBody, style);
        } else {
            NotificationUtil.sendNotification(connection, titleMsg, style);
        }
    }

    @Override
    public void sendNotification(UUID playerId, String title, String message) {
        sendNotification(playerId, title, message, "default");
    }
    
    // --- Helper Methods ---
    
    /**
     * Get PlayerRef from Universe by UUID.
     * 
     * @param playerId Player's UUID
     * @return PlayerRef or null if not found
     */
    private PlayerRef getPlayerRef(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        return Universe.get().getPlayer(playerId);
    }
    
    /**
     * Parse notification type string to SDK NotificationStyle enum.
     * 
     * @param type Type string: "default", "danger", "warning", "success"
     * @return NotificationStyle enum value
     */
    private NotificationStyle parseNotificationStyle(String type) {
        if (type == null) {
            return NotificationStyle.Default;
        }
        
        return switch (type.toLowerCase()) {
            case "danger", "error" -> NotificationStyle.Danger;
            case "warning", "warn" -> NotificationStyle.Warning;
            case "success", "ok" -> NotificationStyle.Success;
            default -> NotificationStyle.Default;
        };
    }
}