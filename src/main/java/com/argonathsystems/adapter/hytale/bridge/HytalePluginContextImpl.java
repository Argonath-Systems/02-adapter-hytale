package com.argonathsystems.adapter.hytale.bridge;

import com.argonathsystems.adapter.api.ArgonathPluginContext;
import com.argonathsystems.adapter.hytale.accessor.*;
import com.argonathsystems.framework.accessor.accessorapi.*;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import org.slf4j.Logger;

/**
 * Hytale-specific implementation of {@link ArgonathPluginContext}.
 * 
 * <p>This class provides concrete Hytale accessor implementations while
 * maintaining the platform-agnostic interface contract.</p>
 * 
 * <h2>Accessor Initialization:</h2>
 * All accessors are eagerly initialized during context construction to avoid
 * null pointer exceptions and ensure consistent behavior.
 * 
 * @since MIGRATION-001
 */
public class HytalePluginContextImpl implements ArgonathPluginContext {
    private final JavaPlugin plugin;
    private final PlayerAccessor playerAccessor;
    private final EventAccessor eventAccessor;
    private final SchedulerAccessor schedulerAccessor;
    private final CommandAccessor commandAccessor;
    private final ConfigAccessor configAccessor;
    private final StorageAccessor storageAccessor;
    private final TextStylingAccessor textStylingAccessor;
    
    /**
     * Create a new HytalePluginContextImpl.
     * 
     * @param plugin The JavaPlugin instance providing access to Hytale SDK
     */
    public HytalePluginContextImpl(JavaPlugin plugin) {
        this.plugin = plugin;
        
        // Initialize all accessors with Hytale-specific implementations
        this.playerAccessor = new HytalePlayerAccessor(plugin);
        this.eventAccessor = new HytaleEventAccessor(plugin);
        this.schedulerAccessor = new HytaleSchedulerAccessor(plugin);
        this.commandAccessor = new HytaleCommandAccessor(plugin);
        this.configAccessor = new HytaleConfigAccessor(plugin);
        this.storageAccessor = new HytaleStorageAccessor(plugin);
        this.textStylingAccessor = new HytaleTextStylingAccessor(plugin);
    }
    
    @Override
    public Logger getLogger() {
        return plugin.getLogger();
    }
    
    @Override
    public PlayerAccessor getPlayerAccessor() {
        return playerAccessor;
    }
    
    @Override
    public EventAccessor getEventAccessor() {
        return eventAccessor;
    }
    
    @Override
    public SchedulerAccessor getSchedulerAccessor() {
        return schedulerAccessor;
    }
    
    @Override
    public CommandAccessor getCommandAccessor() {
        return commandAccessor;
    }
    
    @Override
    public ConfigAccessor getConfigAccessor() {
        return configAccessor;
    }
    
    @Override
    public StorageAccessor getStorageAccessor() {
        return storageAccessor;
    }
    
    @Override
    public TextStylingAccessor getTextStylingAccessor() {
        return textStylingAccessor;
    }
}
