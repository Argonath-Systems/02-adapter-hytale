package com.argonathsystems.adapter.hytaleadapter;

import com.argonathsystems.platform.sdk.ArgonathPlugin;

/**
 * <p><b>MIGRATION-001 Status:</b> BLOCKED - Requires Official Hytale SDK</p>
 */
public class HytalePluginBridge {
    private final Object javaPlugin;
    private final ArgonathPlugin argonathPlugin;
    private final HytalePlatform platform;
    
    public HytalePluginBridge(Object javaPlugin, ArgonathPlugin argonathPlugin, Object server) {
        this.javaPlugin = javaPlugin;
        this.argonathPlugin = argonathPlugin;
        this.platform = new HytalePlatform(javaPlugin, server);
    }
    
    public void onEnable() {
        argonathPlugin.onEnable(platform.getAdapters());
    }
    
    public void onDisable() {
        argonathPlugin.onDisable();
    }
    
    public HytalePlatform getPlatform() {
        return platform;
    }
}
