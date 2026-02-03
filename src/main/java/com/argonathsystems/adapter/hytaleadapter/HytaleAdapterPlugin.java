package com.argonathsystems.adapter.hytaleadapter;

import com.argonathsystems.adapter.hytale.packet.HotbarInteractionAdapter;
import com.argonathsystems.adapter.hytale.packet.InventoryBlockAdapter;
import com.argonathsystems.adapter.hytaleadapter.accessor.HytaleInputAccessor;
import com.argonathsystems.adapter.hytaleadapter.accessor.HytaleInventoryAccessor;
import com.argonathsystems.framework.core.ArgonathMod;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import java.util.ServiceLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import com.argonathsystems.framework.accessorapi.AccessorRegistry;
import com.argonathsystems.framework.accessorapi.InputAccessor;
import com.argonathsystems.framework.accessorapi.InventoryAccessor;
import com.hypixel.hytale.server.core.HytaleServer;

/**
 * Hytale Adapter Plugin - Alternative entry point for standalone deployment.
 * 
 * <p><b>NOTE:</b> This plugin is typically NOT used when deploying via FrameworkLoader.
 * The FrameworkLoaderPlugin initializes the HytaleAdapterProvider and event bridges
 * via reflection. This class exists for standalone adapter deployment scenarios only.</p>
 * 
 * <p>When deployed through FrameworkLoader (the normal case), the following occurs:</p>
 * <ul>
 *   <li>FrameworkLoaderPlugin creates HytaleAdapterProvider via reflection</li>
 *   <li>FrameworkLoaderPlugin creates HytaleEventBridge for SDK→Framework events</li>
 *   <li>FrameworkLoaderPlugin registers PlayerReadyEvent bridge via getEventRegistry()</li>
 * </ul>
 * 
 * @see com.argonathsystems.framework.loader.FrameworkLoaderPlugin
 */
public class HytaleAdapterPlugin extends JavaPlugin {
    
    private final List<ArgonathMod> loadedMods = new ArrayList<>();
    
    /** Packet filter registrations for cleanup */
    private PacketFilter hotbarFilterRegistration;
    private PacketFilter inventoryFilterRegistration;
    
    /** The accessor provider for framework access */
    private HytaleAdapterProvider provider;

    public HytaleAdapterPlugin(JavaPluginInit init) {
        super(init);
        
        // NOTE: When deployed via FrameworkLoader, this plugin is NOT loaded.
        // FrameworkLoaderPlugin handles all initialization including:
        // - HytaleAdapterProvider creation and registration
        // - HytaleEventBridge setup (PlayerJoinEvent, PlayerQuitEvent)
        // - PlayerReadyEvent bridge registration (requires JavaPlugin context)
        //
        // This constructor is only used for standalone adapter deployment.
        
        try {
            // Initialize and register AccessorProvider EARLY
            Object coreServer = HytaleServer.get();
            provider = new HytaleAdapterProvider(coreServer);
            AccessorRegistry.registerProvider(provider);
            getLogger().at(Level.INFO).log("HytaleAdapterProvider initialized and registered.");
            
            // Register packet adapters for hotbar and inventory interception
            registerPacketAdapters(provider);
            
            // NOTE: PlayerReadyEvent bridge is NOT registered here.
            // When using this plugin standalone, PlayerReadyEvent requires
            // getEventRegistry().registerGlobal() which is available here.
            // TODO: Add PlayerReadyEvent registration if standalone deployment is needed.
            
            // Load Platform-Agnostic Mods via ServiceLoader
            ServiceLoader<ArgonathMod> loader = ServiceLoader.load(ArgonathMod.class, getClass().getClassLoader());
            int count = 0;
            for (ArgonathMod mod : loader) {
                 getLogger().at(Level.INFO).log("Loading ArgonathMod: " + mod.getClass().getSimpleName());
                 try {
                     mod.onEnable();
                     loadedMods.add(mod);
                     count++;
                 } catch (Exception e) {
                     getLogger().at(Level.SEVERE).withCause(e).log("Failed to enable mod: " + mod.getClass().getName());
                 }
            }
            getLogger().at(Level.INFO).log("Loaded " + count + " Argonath Mods.");

        } catch (Exception e) {
            getLogger().at(Level.SEVERE).withCause(e).log("Failed to initialize HytaleAdapterPlugin");
            throw new RuntimeException("Critical setup failure", e);
        }
    }
    
    /**
     * Register packet adapters for hotbar and inventory interception.
     * 
     * <p>This wires the accessor layer's packet filters into Hytale's 
     * packet system so they can actually intercept packets.</p>
     */
    private void registerPacketAdapters(HytaleAdapterProvider provider) {
        try {
            // Register hotbar interaction adapter
            InputAccessor inputAccessor = provider.getInputAccessor();
            if (inputAccessor instanceof HytaleInputAccessor hytaleInputAccessor) {
                HotbarInteractionAdapter hotbarAdapter = hytaleInputAccessor.getHotbarInteractionAdapter();
                hotbarFilterRegistration = PacketAdapters.registerInbound(hotbarAdapter);
                getLogger().at(Level.INFO).log("Registered HotbarInteractionAdapter for packet filtering");
            }
            
            // Register inventory block adapter
            InventoryAccessor inventoryAccessor = provider.getInventoryAccessor();
            if (inventoryAccessor instanceof HytaleInventoryAccessor hytaleInventoryAccessor) {
                InventoryBlockAdapter inventoryAdapter = hytaleInventoryAccessor.getInventoryBlockAdapter();
                inventoryFilterRegistration = PacketAdapters.registerInbound(inventoryAdapter);
                getLogger().at(Level.INFO).log("Registered InventoryBlockAdapter for slot blocking");
            }
        } catch (Exception e) {
            getLogger().at(Level.WARNING).withCause(e).log("Failed to register packet adapters (non-fatal)");
        }
    }
    
    public void onDisable() {
        getLogger().at(Level.INFO).log("Disabling Argonath Mods...");
        
        // Unregister packet adapters
        try {
            if (hotbarFilterRegistration != null) {
                PacketAdapters.deregisterInbound(hotbarFilterRegistration);
            }
            if (inventoryFilterRegistration != null) {
                PacketAdapters.deregisterInbound(inventoryFilterRegistration);
            }
        } catch (Exception e) {
            getLogger().at(Level.WARNING).withCause(e).log("Failed to deregister packet adapters");
        }
        
        for (ArgonathMod mod : loadedMods) {
            try {
                mod.onDisable();
            } catch (Exception e) {
                getLogger().at(Level.SEVERE).withCause(e).log("Error disabling mod: " + mod.getClass().getName());
            }
        }
        loadedMods.clear();
        getLogger().at(Level.INFO).log("HytaleAdapterPlugin disabled.");
    }
}
