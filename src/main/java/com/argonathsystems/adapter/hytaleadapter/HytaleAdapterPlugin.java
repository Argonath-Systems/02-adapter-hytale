package com.argonathsystems.adapter.hytaleadapter;

import com.argonathsystems.framework.core.ArgonathMod;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import java.util.ServiceLoader;
import java.util.ArrayList;
import java.util.List;
import com.argonathsystems.framework.accessorapi.AccessorRegistry;
import com.hytale.api.Server;
import com.hypixel.hytale.server.core.HytaleServer;

public class HytaleAdapterPlugin extends JavaPlugin {
    
    private final List<ArgonathMod> loadedMods = new ArrayList<>();

    public HytaleAdapterPlugin(JavaPluginInit init) {
        super(init);
        
        // Initialize EARLY in constructor
        try {
            // Initialize and register AccessorProvider EARLY
            Object coreServer = HytaleServer.get();
            if (coreServer instanceof Server) {
                HytaleAdapterProvider provider = new HytaleAdapterProvider((Server) coreServer);
                AccessorRegistry.registerProvider(provider);
                getLogger().info("HytaleAdapterProvider initialized and registered.");
            } else {
                 getLogger().error("HytaleServer instance does not implement com.hytale.api.Server! Provider registration failed.");
                 throw new IllegalStateException("Cannot register AccessorProvider - server type mismatch");
            }
            
            // Load Platform-Agnostic Mods via ServiceLoader
            ServiceLoader<ArgonathMod> loader = ServiceLoader.load(ArgonathMod.class, getClass().getClassLoader());
            int count = 0;
            for (ArgonathMod mod : loader) {
                 getLogger().info("Loading ArgonathMod: " + mod.getClass().getSimpleName());
                 try {
                     mod.onEnable();
                     loadedMods.add(mod);
                     count++;
                 } catch (Exception e) {
                     getLogger().error("Failed to enable mod: " + mod.getClass().getName(), e);
                 }
            }
            getLogger().info("Loaded " + count + " Argonath Mods.");

        } catch (Exception e) {
            getLogger().error("Failed to initialize HytaleAdapterPlugin", e);
            throw new RuntimeException("Critical setup failure", e);
        }
    }
        getLogger().info("Disabling Argonath Mods...");
        for (ArgonathMod mod : loadedMods) {
            try {
                mod.onDisable();
            } catch (Exception e) {
                getLogger().error("Error disabling mod: " + mod.getClass().getName(), e);
            }
        }
        loadedMods.clear();
        getLogger().info("HytaleAdapterPlugin disabled.");
    }
}
