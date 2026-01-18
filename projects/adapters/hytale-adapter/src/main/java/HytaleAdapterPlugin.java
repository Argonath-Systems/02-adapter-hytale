package com.argonathsystems.adapter.hytaleadapter;

// ✅ HYTALE IMPORTS ALLOWED IN ADAPTER MODULE
import com.hytale.api.Server;
import com.hytale.api.plugin.Plugin;
import com.hytale.api.plugin.PluginInfo;

/**
 * Main plugin entry point for Hytale Adapter.
 * 
 * <p>This adapter implements the Accessor API interfaces for Hytale.
 * It is the ONLY module in the codebase that imports Hytale classes.
 * 
 * @author LordOfTheTales Team
 * @version 1.0.0-SNAPSHOT
 */
@PluginInfo(
    id = "hytale-adapter",
    name = "Hytale Adapter",
    version = "1.0.0-SNAPSHOT"
)
public class HytaleAdapterPlugin implements Plugin {

    private Server server;
    private HytaleAdapterAdapterProvider adapterProvider;

    @Override
    public void onEnable(Server server) {
        this.server = server;
        
        // Create the adapter provider with all accessors
        this.adapterProvider = new HytaleAdapterAdapterProvider(server);
        
        // Register the provider so other mods can access it
        // AdapterRegistry.register(adapterProvider);
        
        server.getLogger().info("[Hytale Adapter] Adapter enabled!");
    }

    @Override
    public void onDisable() {
        // Cleanup
        if (adapterProvider != null) {
            adapterProvider.shutdown();
        }
        
        server.getLogger().info("[Hytale Adapter] Adapter disabled!");
    }
    
    /**
     * Get the accessor provider for dependency injection into mods.
     */
    public HytaleAdapterAdapterProvider getAdapterProvider() {
        return adapterProvider;
    }
}