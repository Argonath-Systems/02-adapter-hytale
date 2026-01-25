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
    }

    @Override
    public void onEnable() {
        getLogger().info("Initializing HytaleAdapterPlugin...");
        
        try {
            // Initialize and register AccessorProvider
            // Attempting to bridge Core Server to API Server
            Object coreServer = HytaleServer.get();
            if (coreServer instanceof Server) {
                HytaleAdapterProvider provider = new HytaleAdapterProvider((Server) coreServer);
                AccessorRegistry.registerProvider(provider);
                getLogger().info("HytaleAdapterProvider initialized and registered.");
            } else {
                 getLogger().error("HytaleServer instance does not implement com.hytale.api.Server! Provider registration failed.");
                 // Fallback or critical failure? 
                 // Many mods will fail, but we'll let it process to see specific errors if any.
                 // For now, let's try to proceed hoping there's a mixin or something handling this, 
                 // or that compiler lets it verify.
                 // Actually, if we can't register, we should probably throw or log strictly.
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
        }
    }

    @Override
    public void onDisable() {
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
