package com.argonathsystems.adapter.hytale.questdesigner;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.registry.EntityRegistry;
import com.hypixel.hytale.server.core.registry.ItemRegistry;
import com.hypixel.hytale.server.core.asset.AssetManager;
import net.nitrado.hytale.plugins.webserver.WebServerPlugin;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hytale Plugin for the Quest Designer tool.
 * 
 * <p>This plugin initializes the Quest Designer web-based tool and registers
 * all HTTP endpoints with the Nitrado WebServer plugin.
 * 
 * <h2>Features:</h2>
 * <ul>
 *   <li>Visual quest graph editor (React SPA)</li>
 *   <li>Quest CRUD API endpoints</li>
 *   <li>Registry access for items, entities, NPCs</li>
 *   <li>Asset serving for icons and textures</li>
 *   <li>Quest validation and debugging</li>
 * </ul>
 * 
 * <h2>Dependencies:</h2>
 * <ul>
 *   <li>{@code Nitrado:WebServer} - HTTP server infrastructure</li>
 *   <li>{@code ArgonathSystems:QuestFramework} - Quest runtime (optional)</li>
 * </ul>
 * 
 * <h2>Configuration:</h2>
 * <ul>
 *   <li>Quest storage: {@code config/quests/}</li>
 *   <li>Web interface: {@code /quest-designer/}</li>
 *   <li>API endpoints: {@code /quest-designer/api/v1/}</li>
 * </ul>
 * 
 * @author Argonath Systems
 * @version 1.0.0
 * @since 1.0.0
 */
public class QuestDesignerPlugin extends JavaPlugin {

    private static final Logger LOGGER = Logger.getLogger(QuestDesignerPlugin.class.getName());
    
    private static final String PLUGIN_NAME = "QuestDesigner";
    private static final String PLUGIN_VERSION = "1.0.0";
    private static final String WEBSERVER_PLUGIN_ID = "Nitrado:WebServer";
    
    private QuestDesignerWebServerAdapter webServerAdapter;
    private boolean initialized = false;

    /**
     * Constructs the Quest Designer plugin.
     * 
     * @param init Hytale plugin initialization context
     */
    public QuestDesignerPlugin(JavaPluginInit init) {
        super(init);
    }

    /**
     * Called during plugin setup phase.
     * 
     * <p>Verifies dependencies are available (Nitrado WebServer).
     */
    public void setup() {
        LOGGER.log(Level.INFO, "[{0}] Setting up v{1}", new Object[]{PLUGIN_NAME, PLUGIN_VERSION});
        
        // Check for WebServer dependency
        PluginManager pluginManager = getPluginManager();
        if (!pluginManager.isPluginLoaded(WEBSERVER_PLUGIN_ID)) {
            LOGGER.log(Level.SEVERE, "[{0}] FATAL: Nitrado WebServer plugin not found!", PLUGIN_NAME);
            LOGGER.log(Level.SEVERE, "[{0}] Please ensure {1} is installed and loaded", 
                new Object[]{PLUGIN_NAME, WEBSERVER_PLUGIN_ID});
            return;
        }
        
        LOGGER.log(Level.INFO, "[{0}] Dependencies verified", PLUGIN_NAME);
    }

    /**
     * Called when the plugin starts.
     * 
     * <p>Initializes the web server adapter and registers all endpoints.
     */
    public void start() {
        LOGGER.log(Level.INFO, "[{0}] Starting...", PLUGIN_NAME);
        
        try {
            // Get WebServer plugin
            PluginManager pluginManager = getPluginManager();
            WebServerPlugin webServerPlugin = (WebServerPlugin) pluginManager.getPlugin(WEBSERVER_PLUGIN_ID);
            
            if (webServerPlugin == null) {
                LOGGER.log(Level.SEVERE, "[{0}] Failed to get WebServer plugin instance", PLUGIN_NAME);
                return;
            }
            
            // Get Hytale registries
            ItemRegistry itemRegistry = getItemRegistry();
            EntityRegistry entityRegistry = getEntityRegistry();
            AssetManager assetManager = getAssetManager();
            
            // Configure storage path
            Path questStoragePath = Paths.get(getDataFolder().getAbsolutePath(), "quests");
            
            // Create and initialize adapter
            webServerAdapter = new QuestDesignerWebServerAdapter(
                webServerPlugin,
                this,
                questStoragePath
            );
            
            webServerAdapter.initialize(itemRegistry, entityRegistry, assetManager);
            
            initialized = true;
            LOGGER.log(Level.INFO, "[{0}] Started successfully!", PLUGIN_NAME);
            LOGGER.log(Level.INFO, "[{0}] Web interface available at: {1}", 
                new Object[]{PLUGIN_NAME, webServerAdapter.getBaseUrl()});
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[" + PLUGIN_NAME + "] Failed to start", e);
        }
    }

    /**
     * Called during plugin shutdown.
     * 
     * <p>Unregisters endpoints and releases resources.
     */
    public void shutdown() {
        LOGGER.log(Level.INFO, "[{0}] Shutting down...", PLUGIN_NAME);
        
        if (webServerAdapter != null) {
            webServerAdapter.shutdown();
        }
        
        initialized = false;
        LOGGER.log(Level.INFO, "[{0}] Shutdown complete", PLUGIN_NAME);
    }

    /**
     * Checks if the plugin is fully initialized and ready.
     * 
     * @return true if initialized and web server is registered
     */
    public boolean isReady() {
        return initialized && webServerAdapter != null && webServerAdapter.isRegistered();
    }

    /**
     * Gets the web server adapter.
     * 
     * @return the adapter, or null if not initialized
     */
    public QuestDesignerWebServerAdapter getWebServerAdapter() {
        return webServerAdapter;
    }

    // ==== Stub methods for Hytale API access ====
    // These would be replaced with actual API calls when Hytale SDK is available

    private PluginManager getPluginManager() {
        // TODO: Replace with actual Hytale API
        throw new UnsupportedOperationException("Hytale API not available - stub implementation");
    }

    private ItemRegistry getItemRegistry() {
        // TODO: Replace with actual Hytale API
        throw new UnsupportedOperationException("Hytale API not available - stub implementation");
    }

    private EntityRegistry getEntityRegistry() {
        // TODO: Replace with actual Hytale API
        throw new UnsupportedOperationException("Hytale API not available - stub implementation");
    }

    private AssetManager getAssetManager() {
        // TODO: Replace with actual Hytale API
        throw new UnsupportedOperationException("Hytale API not available - stub implementation");
    }
}
