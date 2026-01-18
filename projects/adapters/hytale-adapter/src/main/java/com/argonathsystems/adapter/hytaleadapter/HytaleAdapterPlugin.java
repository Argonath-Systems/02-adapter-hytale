package com.argonathsystems.adapter.hytaleadapter;

import com.argonathsystems.framework.core.ArgonathMod;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import java.util.ServiceLoader;
import java.util.ArrayList;
import java.util.List;

public class HytaleAdapterPlugin extends JavaPlugin {
    
    private final List<ArgonathMod> loadedMods = new ArrayList<>();

    public HytaleAdapterPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    public void onEnable() {
        getLogger().info("Initializing HytaleAdapterPlugin...");
        
        try {
            // Disabled for initial validation pass to verify plugin loading mechanism
            getLogger().info("HytaleAdapterProvider initialized (Stubbed).");

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
