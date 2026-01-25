package com.argonathsystems.adapter.hytaleadapter;

import com.argonathsystems.adapter.hytaleadapter.listener.HytaleAdapterEventListener;
import com.argonathsystems.framework.accessorapi.AccessorRegistry;
import com.hytale.api.Server;
import com.hytale.api.plugin.Plugin;

import com.argonathsystems.adapter.hytaleadapter.accessor.HytaleEventAccessor;

public class HytalePlatform implements Plugin {
    private Server server;
    private HytaleAdapterProvider provider;
    private HytaleAdapterEventListener eventListener;

    @Override
    public void onEnable(Server server) {
        this.server = server;
        this.provider = new HytaleAdapterProvider(server);
        
        // Register Provider
        AccessorRegistry.registerProvider(provider);
        
        // Register Events
        this.eventListener = new HytaleAdapterEventListener((HytaleEventAccessor) provider.getEventAccessor());
        server.getEventBus().register(eventListener);
        
        server.getLogger().info("Hytale Adapter (LordOfTheTales) Enabled");
    }

    @Override
    public void onDisable() {
        if (server != null) {
            server.getLogger().info("Hytale Adapter Disabled");
        }
    }
}
