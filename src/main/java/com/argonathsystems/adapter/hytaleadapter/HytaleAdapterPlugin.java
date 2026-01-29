package com.argonathsystems.adapter.hytaleadapter;

import com.argonathsystems.framework.core.ArgonathMod;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import java.util.ServiceLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import com.argonathsystems.framework.accessorapi.AccessorRegistry;
import com.hypixel.hytale.server.core.HytaleServer;

public class HytaleAdapterPlugin extends JavaPlugin {
    
    private final List<ArgonathMod> loadedMods = new ArrayList<>();

    public HytaleAdapterPlugin(JavaPluginInit init) {
        super(init);
        
        // Initialize EARLY in constructor
        try {
            // Initialize and register AccessorProvider EARLY
            Object coreServer = HytaleServer.get();
            HytaleAdapterProvider provider = new HytaleAdapterProvider(coreServer);
            AccessorRegistry.registerProvider(provider);
            getLogger().at(Level.INFO).log("HytaleAdapterProvider initialized and registered.");
            
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
    
    public void onDisable() {
        getLogger().at(Level.INFO).log("Disabling Argonath Mods...");
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
