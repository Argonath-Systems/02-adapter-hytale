package com.argonathsystems.adapter.hytaleadapter;

import com.argonathsystems.platform.sdk.ArgonathPlugin;
import com.argonathsystems.platform.sdk.PluginContext;

/**
 * <p><b>MIGRATION-001 Status:</b> BLOCKED - Requires Official Hytale SDK</p>
 */
public class HytalePluginContextImpl implements PluginContext {
    private final Object plugin;
    private final Object server;
    
    public HytalePluginContextImpl(Object plugin, Object server) {
        this.plugin = plugin;
        this.server = server;
    }
    
    @Override
    public String getPluginName() {
        throw new UnsupportedOperationException(
            "HytalePluginContextImpl.getPluginName() requires official Hytale SDK Plugin class"
        );
    }
    
    @Override
    public String getPluginVersion() {
        throw new UnsupportedOperationException(
            "HytalePluginContextImpl.getPluginVersion() requires official Hytale SDK Plugin class"
        );
    }
    
    @Override
    public Object getRawPlatformPlugin() {
        return plugin;
    }
}
