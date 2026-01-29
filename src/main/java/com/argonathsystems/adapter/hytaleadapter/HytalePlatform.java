package com.argonathsystems.adapter.hytaleadapter;

/**
 * <p><b>MIGRATION-001 Status:</b> BLOCKED - Requires Official Hytale SDK</p>
 */
public class HytalePlatform {
    private final Object plugin;
    private final HytaleAdapterProvider adapters;
    
    public HytalePlatform(Object plugin, Object server) {
        this.plugin = plugin;
        this.adapters = new HytaleAdapterProvider(server);
    }
    
    public HytaleAdapterProvider getAdapters() {
        return adapters;
    }
    
    public Object getPlugin() {
        return plugin;
    }
}
