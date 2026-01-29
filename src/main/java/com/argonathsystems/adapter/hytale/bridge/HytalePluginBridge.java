package com.argonathsystems.adapter.hytale.bridge;

import com.argonathsystems.adapter.api.ArgonathPlugin;
import com.argonathsystems.adapter.api.ArgonathPluginContext;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

/**
 * Bridge between Hytale's JavaPlugin and Argonath's ArgonathPlugin.
 * 
 * <p>This class acts as an adapter, allowing platform-agnostic {@link ArgonathPlugin}
 * instances to run on the Hytale platform. All mods should extend {@link ArgonathPlugin},
 * not this class directly.</p>
 * 
 * <h2>Architecture:</h2>
 * <pre>
 * JavaPlugin (Hytale SDK)
 *     ↑
 *     | extends
 *     |
 * HytalePluginBridge (Adapter Layer - ONLY imports Hytale SDK)
 *     ↓
 *     | delegates to
 *     |
 * ArgonathPlugin (Platform-Agnostic - NO Hytale imports)
 *     ↑
 *     | extends
 *     |
 * MyMod (User Code - NO Hytale imports)
 * </pre>
 * 
 * <h2>Lifecycle:</h2>
 * <ol>
 *   <li>Hytale loads HytalePluginBridge via JavaPlugin</li>
 *   <li>HytalePluginBridge creates HytalePluginContextImpl</li>
 *   <li>HytalePluginBridge calls argonathPlugin.initialize(context)</li>
 *   <li>ArgonathPlugin calls onEnable() on user's mod</li>
 * </ol>
 * 
 * @since MIGRATION-001
 */
public final class HytalePluginBridge extends JavaPlugin {
    private final ArgonathPlugin argonathPlugin;
    private final ArgonathPluginContext context;
    
    /**
     * Create a new HytalePluginBridge.
     * 
     * @param init Hytale plugin initialization data
     * @param argonathPlugin The platform-agnostic plugin to wrap
     */
    public HytalePluginBridge(JavaPluginInit init, ArgonathPlugin argonathPlugin) {
        super(init);
        this.argonathPlugin = argonathPlugin;
        this.context = new HytalePluginContextImpl(this);
    }
    
    /**
     * Called by Hytale when the plugin is enabled.
     * Delegates to the ArgonathPlugin's initialize method.
     */
    @Override
    protected void onEnable() {
        try {
            argonathPlugin.initialize(context);
            getLogger().info("Argonath plugin enabled: {}", argonathPlugin.getClass().getSimpleName());
        } catch (Exception e) {
            getLogger().error("Failed to enable Argonath plugin: {}", argonathPlugin.getClass().getSimpleName(), e);
            throw e;
        }
    }
    
    /**
     * Called by Hytale when the plugin is disabled.
     * Delegates to the ArgonathPlugin's onDisable method.
     */
    @Override
    protected void onDisable() {
        try {
            argonathPlugin.onDisable();
            getLogger().info("Argonath plugin disabled: {}", argonathPlugin.getClass().getSimpleName());
        } catch (Exception e) {
            getLogger().error("Failed to disable Argonath plugin: {}", argonathPlugin.getClass().getSimpleName(), e);
        }
    }
}
