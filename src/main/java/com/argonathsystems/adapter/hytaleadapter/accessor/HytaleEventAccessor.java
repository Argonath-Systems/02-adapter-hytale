package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.EventAccessor;
import com.argonathsystems.framework.accessorapi.event.AccessorEvent;
import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.server.core.HytaleServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Hytale implementation of EventAccessor using SDK event system.
 * 
 * <p><b>MIGRATION-001 Status:</b> ✅ IMPLEMENTED</p>
 * 
 * <p>SDK Classes Used:</p>
 * <ul>
 *   <li>{@code EventBus} - Server's event bus instance</li>
 *   <li>{@code IEventRegistry} - Event registration interface</li>
 *   <li>{@code EventPriority} - Listener priority levels</li>
 *   <li>{@code EventRegistration} - Handle for unregistering</li>
 * </ul>
 * 
 * <p>Event Translation:</p>
 * <p>This accessor bridges between framework's {@link AccessorEvent} types and 
 * SDK's native event types. Implementations should use the event mapping registry
 * to translate between the two systems.</p>
 * 
 * <p>Note: For native SDK events (like PlayerConnectEvent), listeners should be
 * registered directly via the SDK's EventBus. This accessor is for framework-level
 * custom events.</p>
 * 
 * @author Argonath Systems Team
 * @version 3.0.0
 * @since MIGRATION-001
 */
public class HytaleEventAccessor implements EventAccessor {
    
    private final HytaleServer server;
    private final EventBus eventBus;
    
    /**
     * Map of all active registrations for cleanup.
     * Key is our EventRegistration wrapper, value is the SDK registration.
     */
    private final Map<EventRegistration, com.hypixel.hytale.event.EventRegistration<?, ?>> registrations = new ConcurrentHashMap<>();
    
    /**
     * List of all registrations for bulk unregister.
     */
    private final List<EventRegistration> allRegistrations = new ArrayList<>();

    public HytaleEventAccessor(Object server) {
        this.server = (HytaleServer) server;
        this.eventBus = this.server.getEventBus();
    }

    @Override
    public <T extends AccessorEvent> EventRegistration register(Class<T> eventType, Consumer<T> listener) {
        return register(eventType, listener, 0);
    }

    @Override
    public <T extends AccessorEvent> EventRegistration register(Class<T> eventType, Consumer<T> listener, int priority) {
        // For custom AccessorEvents, we use a simple in-memory event dispatch
        // since AccessorEvents are framework-level and don't map directly to SDK events.
        //
        // For SDK-native events, use the SDK's EventBus directly through specialized methods.
        
        HytaleEventRegistration registration = new HytaleEventRegistration(eventType, listener, priority);
        
        // Register with internal dispatcher
        getCustomEventDispatcher(eventType).addListener(registration);
        
        allRegistrations.add(registration);
        return registration;
    }

    @Override
    public void unregister(EventRegistration registration) {
        if (registration instanceof HytaleEventRegistration hytaleReg) {
            getCustomEventDispatcher(hytaleReg.eventType).removeListener(hytaleReg);
            allRegistrations.remove(registration);
        }
        
        // If it's an SDK registration, unregister from the event bus
        com.hypixel.hytale.event.EventRegistration<?, ?> sdkReg = registrations.remove(registration);
        if (sdkReg != null) {
            sdkReg.unregister();
        }
    }

    @Override
    public void unregisterAll() {
        // Unregister all SDK registrations
        for (com.hypixel.hytale.event.EventRegistration<?, ?> sdkReg : registrations.values()) {
            sdkReg.unregister();
        }
        registrations.clear();
        
        // Clear all custom event dispatchers
        for (EventRegistration reg : new ArrayList<>(allRegistrations)) {
            if (reg instanceof HytaleEventRegistration hytaleReg) {
                getCustomEventDispatcher(hytaleReg.eventType).removeListener(hytaleReg);
            }
        }
        allRegistrations.clear();
        customEventDispatchers.clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AccessorEvent> T emit(T event) {
        // Dispatch to registered listeners
        CustomEventDispatcher<T> dispatcher = (CustomEventDispatcher<T>) customEventDispatchers.get(event.getClass());
        if (dispatcher != null) {
            dispatcher.dispatch(event);
        }
        return event;
    }
    
    // --- SDK Native Event Registration ---
    
    /**
     * Register a listener for a native SDK event.
     * Use this for events like PlayerConnectEvent, BlockBreakEvent, etc.
     * 
     * <p><b>Note:</b> Uses register() instead of registerGlobal() due to
     * complex type constraints in the SDK API.</p>
     * 
     * @param sdkEventType The SDK event class
     * @param listener The event handler
     * @return Registration handle
     */
    @SuppressWarnings("unchecked")
    public <E extends com.hypixel.hytale.event.IBaseEvent<Void>> EventRegistration registerNative(
            Class<E> sdkEventType, 
            Consumer<E> listener) {
        
        com.hypixel.hytale.event.EventRegistration<Void, E> sdkReg = 
            eventBus.register(sdkEventType, listener);
        
        NativeEventRegistration wrapper = new NativeEventRegistration(sdkEventType, sdkReg);
        registrations.put(wrapper, sdkReg);
        allRegistrations.add(wrapper);
        
        return wrapper;
    }
    
    /**
     * Register a listener for a native SDK event with priority.
     */
    @SuppressWarnings("unchecked") 
    public <E extends com.hypixel.hytale.event.IBaseEvent<Void>> EventRegistration registerNative(
            Class<E> sdkEventType,
            Consumer<E> listener,
            EventPriority priority) {
        
        com.hypixel.hytale.event.EventRegistration<Void, E> sdkReg = 
            eventBus.register(priority, sdkEventType, listener);
        
        NativeEventRegistration wrapper = new NativeEventRegistration(sdkEventType, sdkReg);
        registrations.put(wrapper, sdkReg);
        allRegistrations.add(wrapper);
        
        return wrapper;
    }
    
    // --- Custom Event Dispatcher ---
    
    private final Map<Class<?>, CustomEventDispatcher<?>> customEventDispatchers = new ConcurrentHashMap<>();
    
    @SuppressWarnings("unchecked")
    private <T extends AccessorEvent> CustomEventDispatcher<T> getCustomEventDispatcher(Class<T> eventType) {
        return (CustomEventDispatcher<T>) customEventDispatchers.computeIfAbsent(
            eventType, 
            k -> new CustomEventDispatcher<>()
        );
    }
    
    /**
     * Simple dispatcher for custom AccessorEvents.
     */
    private static class CustomEventDispatcher<T extends AccessorEvent> {
        private final List<HytaleEventRegistration> listeners = new ArrayList<>();
        
        void addListener(HytaleEventRegistration listener) {
            listeners.add(listener);
            // Sort by priority (higher first)
            listeners.sort((a, b) -> Integer.compare(b.priority, a.priority));
        }
        
        void removeListener(HytaleEventRegistration listener) {
            listeners.remove(listener);
        }
        
        @SuppressWarnings("unchecked")
        void dispatch(T event) {
            for (HytaleEventRegistration listener : new ArrayList<>(listeners)) {
                try {
                    ((Consumer<T>) listener.listener).accept(event);
                } catch (Exception e) {
                    // Log error but continue dispatching to other listeners
                    e.printStackTrace();
                }
            }
        }
    }
    
    // --- Registration Wrappers ---
    
    /**
     * Registration for custom AccessorEvents.
     */
    private class HytaleEventRegistration implements EventRegistration {
        final Class<? extends AccessorEvent> eventType;
        final Consumer<?> listener;
        final int priority;
        
        HytaleEventRegistration(Class<? extends AccessorEvent> eventType, Consumer<?> listener, int priority) {
            this.eventType = eventType;
            this.listener = listener;
            this.priority = priority;
        }
        
        @Override
        public Class<? extends AccessorEvent> getEventType() {
            return eventType;
        }
        
        @Override
        public void unregister() {
            HytaleEventAccessor.this.unregister(this);
        }
    }
    
    /**
     * Registration wrapper for native SDK events.
     */
    private class NativeEventRegistration implements EventRegistration {
        final Class<?> sdkEventType;
        final com.hypixel.hytale.event.EventRegistration<?, ?> sdkRegistration;
        
        NativeEventRegistration(Class<?> sdkEventType, com.hypixel.hytale.event.EventRegistration<?, ?> sdkRegistration) {
            this.sdkEventType = sdkEventType;
            this.sdkRegistration = sdkRegistration;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends AccessorEvent> getEventType() {
            // Native SDK events don't implement AccessorEvent.
            // Return the SDK event class cast as a best-effort — callers should
            // check instanceof before using. This avoids returning null which
            // violates the contract and can cause NPEs downstream.
            // See: IMPL-ADAPTER-PLAN-001, NULL-001
            return (Class<? extends AccessorEvent>) sdkEventType;
        }
        
        @Override
        public void unregister() {
            HytaleEventAccessor.this.unregister(this);
        }
    }
}