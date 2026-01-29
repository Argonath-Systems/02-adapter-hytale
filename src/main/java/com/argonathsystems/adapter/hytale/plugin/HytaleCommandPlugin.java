package com.argonathsystems.adapter.hytale.plugin;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import java.util.logging.Logger;

/**
 * Hytale adapter plugin for the Command Framework.
 * 
 * <p>This plugin bridges the platform-agnostic Command Framework 
 * ({@code 03-framework-command}) to the Hytale server environment.
 * It handles lifecycle events and initializes the command registry
 * when the server loads.</p>
 * 
 * <p>The Command Framework itself contains ZERO Hytale imports - all
 * platform-specific integration is handled here in the adapter layer.</p>
 * 
 * @author Argonath Systems Team
 * @version 1.1.0
 * @since 1.0.0
 */
public class HytaleCommandPlugin extends JavaPlugin {
    
    private static final Logger LOGGER = Logger.getLogger(HytaleCommandPlugin.class.getName());
    
    /**
     * Construct the command plugin with Hytale initialization context.
     *
     * @param init the Hytale plugin initialization context
     */
    public HytaleCommandPlugin(JavaPluginInit init) {
        super(init);
    }
    
    /**
     * Called during plugin setup phase.
     * Initializes the command registry and prepares for command registration.
     */
    public void setup() {
        LOGGER.info("[CommandAdapter] Command Framework adapter loaded.");
        // Command registry is available as a singleton from the framework
        // Actual command registration happens when mods call CommandRegistry.register()
    }
    
    /**
     * Called when the plugin starts.
     * The command system is now active and ready to process commands.
     */
    public void start() {
        LOGGER.info("[CommandAdapter] Command Framework adapter active.");
    }
    
    /**
     * Called during plugin shutdown.
     * Cleans up command registrations and releases resources.
     */
    public void shutdown() {
        LOGGER.info("[CommandAdapter] Command Framework adapter shutting down.");
    }
}
