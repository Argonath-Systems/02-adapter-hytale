package com.argonathsystems.adapter.hytaleadapter;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;

public class HytaleAdapterPlugin extends JavaPlugin {
    
    @Override
    public void onEnable() {
        getLogger().info("Initializing HytaleAdapterPlugin...");
        
        try {
            // Disabled for initial validation pass to verify plugin loading mechanism
            getLogger().info("HytaleAdapterProvider initialized (Stubbed).");
             
        } catch (Exception e) {
            getLogger().error("Failed to initialize HytaleAdapterPlugin", e);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("HytaleAdapterPlugin disabled.");
    }
}
