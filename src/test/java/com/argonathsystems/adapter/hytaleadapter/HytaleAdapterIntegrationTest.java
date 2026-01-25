package com.argonathsystems.adapter.hytaleadapter;

import com.argonathsystems.adapter.hytaleadapter.accessor.HytaleEventAccessor;
import com.argonathsystems.adapter.hytaleadapter.listener.HytaleAdapterEventListener;
import com.argonathsystems.framework.accessorapi.event.PlayerJoinEvent;
import com.hytale.api.Logger;
import com.hytale.api.Server;
import com.hytale.api.entity.Player;
// import com.hytale.api.event.EventBus; // Not strictly needed for this unit test as we call methods directly
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class HytaleAdapterIntegrationTest {

    @Mock Server server;
    @Mock Logger logger;
    @Mock Player player;

    HytaleEventAccessor eventAccessor;
    HytaleAdapterEventListener eventListener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(server.getLogger()).thenReturn(logger);

        eventAccessor = new HytaleEventAccessor(server);
        eventListener = new HytaleAdapterEventListener(eventAccessor);
    }

    @Test
    void testPlayerJoinEventFlow() {
        UUID playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getName()).thenReturn("TestPlayer");

        AtomicBoolean eventReceived = new AtomicBoolean(false);
        
        // Register a listener on the Framework side
        eventAccessor.register(PlayerJoinEvent.class, event -> {
            assertEquals(playerId, event.getPlayerId());
            assertEquals("TestPlayer joined the game", event.getJoinMessage());
            eventReceived.set(true);
        });

        // Simulate Hytale Event being triggered (manually calling the handler)
        com.hytale.api.event.PlayerJoinEvent hytaleEvent = new com.hytale.api.event.PlayerJoinEvent(player);
        eventListener.onPlayerJoin(hytaleEvent);

        assertTrue(eventReceived.get(), "Framework listener should have received the event");
    }
}