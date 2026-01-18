package com.argonathsystems.adapter.hytaleadapter;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

public class HytaleAdapterPlugin extends JavaPlugin {
    
    public HytaleAdapterPlugin(JavaPluginInit init) {
        super(init);
    }

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
