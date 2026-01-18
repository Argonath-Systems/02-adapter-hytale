package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.hytale.api.Server;
import com.argonathsystems.framework.accessorapi.EventAccessor;

import java.util.function.Consumer;

public class HytaleEventAccessor implements EventAccessor {
    private final Server server;

    public HytaleEventAccessor(Server server) {
        this.server = server;
    }

    @Override
    public <T> EventRegistration register(Class<T> eventType, Consumer<T> listener) {
        return null;
    }

    @Override
    public <T> EventRegistration register(Class<T> eventType, Consumer<T> listener, int priority) {
        return null;
    }

    @Override
    public void unregister(EventRegistration registration) {
    }

    @Override
    public void unregisterAll() {
    }

    @Override
    public <T> T emit(T event) {
        return event;
    }
}