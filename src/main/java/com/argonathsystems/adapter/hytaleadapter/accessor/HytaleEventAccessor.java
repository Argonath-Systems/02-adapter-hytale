package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.EventAccessor;
import com.argonathsystems.framework.accessorapi.event.AccessorEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * <p><b>MIGRATION-001 Status:</b> BLOCKED - Requires Official Hytale SDK</p>
 * 
 * <p>This accessor implements Event registration and handling functionality.</p>
 * <p>Implementation requires the official Hytale SDK (com.hypixel.hytale.*)
 * which is not available in the development environment.</p>
 * 
 * <p>All methods throw UnsupportedOperationException until the SDK is available.</p>
 * 
 * @see <a href="file://../../../docs/migration-001/PHASE-3-IMPLEMENTATION-STATUS.md">Phase 3 Status</a>
 */
public class HytaleEventAccessor implements EventAccessor {
    private final Object /* Server */ server;
    private final Map<Class<? extends AccessorEvent>, List<Consumer<? extends AccessorEvent>>> listeners = new ConcurrentHashMap<>();

    public HytaleEventAccessor(Object /* Server */ server) {
        this.server = server;
    }

    @Override
    public <T extends AccessorEvent> EventRegistration register(Class<T> eventType, Consumer<T> listener) {
        return register(eventType, listener, 0);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AccessorEvent> EventRegistration register(Class<T> eventType, Consumer<T> listener, int priority) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add((Consumer<AccessorEvent>) listener);
        return new EventRegistration() {
            @Override
            public Class<? extends AccessorEvent> getEventType() {
                return eventType;
            }

            @Override
            public void unregister() {
                List<Consumer<? extends AccessorEvent>> list = listeners.get(eventType);
                if (list != null) {
                    list.remove(listener);
                }
            }
        };
    }

    @Override
    public void unregister(EventRegistration registration) {
        registration.unregister();
    }

    @Override
    public void unregisterAll() {
        listeners.clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends AccessorEvent> T emit(T event) {
        List<Consumer<? extends AccessorEvent>> list = listeners.get(event.getClass());
        if (list != null) {
            // Create a copy to avoid concurrent modification issues during iteration
            for (Consumer<? extends AccessorEvent> consumer : new ArrayList<>(list)) {
                try {
                    ((Consumer<T>) consumer).accept(event);
                } catch (Exception e) {
                    server.getLogger().error("Error dispatching event " + event.getClass().getSimpleName(), e);
                }
            }
        }
        return event;
    }
}