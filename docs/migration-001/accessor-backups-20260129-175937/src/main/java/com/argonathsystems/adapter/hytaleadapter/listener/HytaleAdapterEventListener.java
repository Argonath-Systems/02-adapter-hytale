package com.argonathsystems.adapter.hytaleadapter.listener;

import com.argonathsystems.adapter.hytaleadapter.accessor.HytaleEventAccessor;
import com.argonathsystems.framework.accessorapi.event.PlayerJoinEvent;
import com.argonathsystems.framework.accessorapi.event.PlayerQuitEvent;
import com.hytale.api.event.EventListener;
import com.hytale.api.event.Handler;
import com.hytale.api.entity.Player;
import java.util.UUID;

import com.argonathsystems.adapter.hytaleadapter.util.PlayerRefCache;

public class HytaleAdapterEventListener implements EventListener {

    private final HytaleEventAccessor eventAccessor;

    public HytaleAdapterEventListener(HytaleEventAccessor eventAccessor) {
        this.eventAccessor = eventAccessor;
    }

    @Handler
    public void onPlayerJoin(com.hytale.api.event.PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        PlayerRefCache.add(player);

        // Create framework event
        PlayerJoinEvent frameworkEvent = new PlayerJoinEvent(playerId, player.getName() + " joined the game");
        
        // Publish to framework
        eventAccessor.emit(frameworkEvent);
    }

    @Handler
    public void onPlayerQuit(com.hytale.api.event.PlayerQuitEvent event) {
         Player player = event.getPlayer();
         UUID playerId = player.getUniqueId();
         
         PlayerRefCache.remove(playerId);

         // Create framework event
         PlayerQuitEvent frameworkEvent = new PlayerQuitEvent(playerId, player.getName() + " left the game");
         
         // Publish to framework
         eventAccessor.emit(frameworkEvent);
    }
    
    @Handler
    public void onEntityDeath(com.hytale.api.event.EntityDeathEvent event) {
        com.hytale.api.entity.Entity victim = event.getEntity();
        com.hytale.api.entity.Entity killer = event.getKiller();
        
        UUID killerId = killer != null ? killer.getUniqueId() : null;
        
        // Create framework event
        com.argonathsystems.framework.accessorapi.event.EntityKillEvent frameworkEvent = 
            new com.argonathsystems.framework.accessorapi.event.EntityKillEvent(
                com.argonathsystems.adapter.hytaleadapter.converter.EntityDataConverter.toDTO(victim),
                killerId
            );
            
        // Publish to framework
        eventAccessor.emit(frameworkEvent);
    }
}