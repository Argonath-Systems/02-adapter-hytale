package com.argonathsystems.adapter.hytaleadapter.listener;

import com.argonathsystems.adapter.hytaleadapter.accessor.HytaleEventAccessor;
import com.argonathsystems.framework.accessorapi.event.PlayerJoinEvent;
import com.argonathsystems.framework.accessorapi.event.PlayerQuitEvent;
import com.hytale.api.event.EventListener;
import com.hytale.api.event.Handler;
import com.hytale.api.entity.Player;
import java.util.UUID;

public class HytaleAdapterEventListener implements EventListener {

    private final HytaleEventAccessor eventAccessor;

    public HytaleAdapterEventListener(HytaleEventAccessor eventAccessor) {
        this.eventAccessor = eventAccessor;
    }

    @Handler
    public void onPlayerJoin(com.hytale.api.event.PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Create framework event
        // Note: Join message handling might vary, passing null or default for now
        PlayerJoinEvent frameworkEvent = new PlayerJoinEvent(playerId, player.getName() + " joined the game");
        
        // Publish to framework
        eventAccessor.emit(frameworkEvent);
    }

    @Handler
    public void onPlayerQuit(com.hytale.api.event.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        PlayerQuitEvent frameworkEvent = new PlayerQuitEvent(playerId, player.getName() + " left the game");
        
        eventAccessor.emit(frameworkEvent);
    }
}