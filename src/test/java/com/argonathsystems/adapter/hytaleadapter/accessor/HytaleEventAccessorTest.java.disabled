package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.EventAccessor;
import com.argonathsystems.framework.accessorapi.event.PlayerJoinEvent;
import com.hytale.api.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class HytaleEventAccessorTest {

    @Mock
    private Server mockServer;

    private HytaleEventAccessor eventAccessor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        eventAccessor = new HytaleEventAccessor(mockServer);
    }

    @Test
    void testEventRegistration() {
        List<PlayerJoinEvent> receivedEvents = new ArrayList<>();
        
        EventAccessor.EventRegistration registration = eventAccessor.register(
            PlayerJoinEvent.class,
            receivedEvents::add
        );
        
        assertThat(registration).isNotNull();
        assertThat(registration.getEventType()).isEqualTo(PlayerJoinEvent.class);
    }

    @Test
    void testEventEmission() {
        List<PlayerJoinEvent> receivedEvents = new ArrayList<>();
        
        eventAccessor.register(PlayerJoinEvent.class, receivedEvents::add);
        
        UUID playerId = UUID.randomUUID();
        PlayerJoinEvent event = new PlayerJoinEvent(playerId, "Welcome!");
        
        eventAccessor.emit(event);
        
        assertThat(receivedEvents).hasSize(1);
        assertThat(receivedEvents.get(0).getPlayerId()).isEqualTo(playerId);
        assertThat(receivedEvents.get(0).getJoinMessage()).isEqualTo("Welcome!");
    }

    @Test
    void testMultipleListeners() {
        List<PlayerJoinEvent> listener1Events = new ArrayList<>();
        List<PlayerJoinEvent> listener2Events = new ArrayList<>();
        
        eventAccessor.register(PlayerJoinEvent.class, listener1Events::add);
        eventAccessor.register(PlayerJoinEvent.class, listener2Events::add);
        
        UUID playerId = UUID.randomUUID();
        PlayerJoinEvent event = new PlayerJoinEvent(playerId, "Welcome!");
        
        eventAccessor.emit(event);
        
        assertThat(listener1Events).hasSize(1);
        assertThat(listener2Events).hasSize(1);
    }

    @Test
    void testEventUnregistration() {
        List<PlayerJoinEvent> receivedEvents = new ArrayList<>();
        
        EventAccessor.EventRegistration registration = eventAccessor.register(
            PlayerJoinEvent.class,
            receivedEvents::add
        );
        
        // First event should be received
        eventAccessor.emit(new PlayerJoinEvent(UUID.randomUUID(), "First"));
        assertThat(receivedEvents).hasSize(1);
        
        // Unregister
        registration.unregister();
        
        // Second event should not be received
        eventAccessor.emit(new PlayerJoinEvent(UUID.randomUUID(), "Second"));
        assertThat(receivedEvents).hasSize(1); // Still 1, not 2
    }

    @Test
    void testUnregisterAll() {
        List<PlayerJoinEvent> listener1Events = new ArrayList<>();
        List<PlayerJoinEvent> listener2Events = new ArrayList<>();
        
        eventAccessor.register(PlayerJoinEvent.class, listener1Events::add);
        eventAccessor.register(PlayerJoinEvent.class, listener2Events::add);
        
        // First event should reach both
        eventAccessor.emit(new PlayerJoinEvent(UUID.randomUUID(), "First"));
        assertThat(listener1Events).hasSize(1);
        assertThat(listener2Events).hasSize(1);
        
        // Unregister all
        eventAccessor.unregisterAll();
        
        // Second event should reach neither
        eventAccessor.emit(new PlayerJoinEvent(UUID.randomUUID(), "Second"));
        assertThat(listener1Events).hasSize(1);
        assertThat(listener2Events).hasSize(1);
    }

    @Test
    void testEventWithPriority() {
        List<PlayerJoinEvent> receivedEvents = new ArrayList<>();
        
        EventAccessor.EventRegistration registration = eventAccessor.register(
            PlayerJoinEvent.class,
            receivedEvents::add,
            100  // Priority (not yet implemented in HytaleEventAccessor, but API exists)
        );
        
        assertThat(registration).isNotNull();
        
        eventAccessor.emit(new PlayerJoinEvent(UUID.randomUUID(), "Priority test"));
        
        assertThat(receivedEvents).hasSize(1);
    }
}
