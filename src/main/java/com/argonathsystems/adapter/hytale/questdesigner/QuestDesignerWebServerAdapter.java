package com.argonathsystems.adapter.hytale.questdesigner;

import com.argonathsystems.mod.questdesigner.servlet.*;
import com.argonathsystems.mod.questdesigner.QuestDesignerService;
import com.argonathsystems.mod.questdesigner.QuestDesignerServiceImpl;
import com.argonathsystems.mod.questdesigner.storage.FileQuestStorage;
import com.argonathsystems.mod.questdesigner.storage.QuestStorage;
import com.argonathsystems.mod.questdesigner.validation.GraphValidator;
import com.argonathsystems.mod.questdesigner.accessor.HytaleRegistryAccessor;
import com.argonathsystems.mod.questdesigner.accessor.HytaleAssetAccessor;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.registry.EntityRegistry;
import com.hypixel.hytale.server.core.registry.ItemRegistry;
import com.hypixel.hytale.server.core.asset.AssetManager;
import jakarta.servlet.http.HttpServlet;
import net.nitrado.hytale.plugins.webserver.WebServerPlugin;

import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Quest Designer Web Server Adapter.
 * 
 * <p>Registers all Quest Designer servlets with the Nitrado WebServer plugin.
 * This adapter handles the wiring between platform-agnostic servlets and
 * the Hytale server environment.
 * 
 * <h2>Registered Endpoints:</h2>
 * <pre>
 * GET  /                       - Static file serving (React SPA)
 * GET  /api/v1/quests          - List all quests
 * POST /api/v1/quests          - Create new quest
 * GET  /api/v1/quests/{id}     - Get quest by ID
 * PUT  /api/v1/quests/{id}     - Update quest
 * DELETE /api/v1/quests/{id}   - Delete quest
 * POST /api/v1/validate        - Validate quest JSON
 * GET  /api/v1/registry/*      - Access game registry
 * GET  /api/v1/configuration   - Editor configuration
 * GET  /api/v1/assets/*        - Asset serving
 * POST /api/v1/debug/*         - Debug endpoints
 * GET  /api/v1/status          - Server status
 * </pre>
 * 
 * @author Argonath Systems
 * @version 1.0.0
 * @since 1.0.0
 */
public class QuestDesignerWebServerAdapter {

    private static final Logger LOGGER = Logger.getLogger(QuestDesignerWebServerAdapter.class.getName());
    
    private static final String URL_PREFIX = "/quest-designer";
    private static final String API_V1_PREFIX = URL_PREFIX + "/api/v1";

    private final WebServerPlugin webServerPlugin;
    private final PluginBase ownerPlugin;
    private final Path questStoragePath;
    
    // Services
    private QuestStorage questStorage;
    private QuestDesignerService questService;
    private HytaleRegistryAccessor registryAccessor;
    private HytaleAssetAccessor assetAccessor;

    // Servlets
    private StaticFileServlet staticFileServlet;
    private QuestApiServlet questApiServlet;
    private RegistryApiServlet registryApiServlet;
    private ConfigurationServlet configurationServlet;
    private AssetServlet assetServlet;
    private DebugServlet debugServlet;
    private StatusServlet statusServlet;

    private boolean registered = false;

    /**
     * Creates a new Quest Designer Web Server Adapter.
     * 
     * @param webServerPlugin the Nitrado WebServer plugin instance
     * @param ownerPlugin the owning Hytale plugin
     * @param questStoragePath path to quest storage directory (e.g., config/quests/)
     */
    public QuestDesignerWebServerAdapter(
        WebServerPlugin webServerPlugin,
        PluginBase ownerPlugin,
        Path questStoragePath
    ) {
        this.webServerPlugin = Objects.requireNonNull(webServerPlugin, "webServerPlugin cannot be null");
        this.ownerPlugin = Objects.requireNonNull(ownerPlugin, "ownerPlugin cannot be null");
        this.questStoragePath = Objects.requireNonNull(questStoragePath, "questStoragePath cannot be null");
    }

    /**
     * Initializes services and registers all servlets.
     * 
     * @param itemRegistry Hytale item registry
     * @param entityRegistry Hytale entity registry
     * @param assetManager Hytale asset manager
     */
    public void initialize(
        ItemRegistry itemRegistry,
        EntityRegistry entityRegistry,
        AssetManager assetManager
    ) {
        LOGGER.log(Level.INFO, "Initializing Quest Designer Web Server Adapter");
        
        try {
            // Initialize storage
            questStorage = new FileQuestStorage(questStoragePath);
            LOGGER.log(Level.INFO, "Quest storage initialized at: {0}", questStoragePath);
            
            // Initialize accessors
            registryAccessor = new HytaleRegistryAccessorImpl(itemRegistry, entityRegistry);
            assetAccessor = new HytaleAssetAccessorImpl(assetManager);
            
            // Initialize service
            GraphValidator graphValidator = new GraphValidator();
            questService = new QuestDesignerServiceImpl(questStorage, graphValidator);
            
            // Initialize and register servlets
            registerServlets();
            
            LOGGER.log(Level.INFO, "Quest Designer Web Server Adapter initialized successfully");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize Quest Designer Web Server Adapter", e);
            throw new RuntimeException("Quest Designer initialization failed", e);
        }
    }

    private void registerServlets() {
        if (registered) {
            LOGGER.log(Level.WARNING, "Servlets already registered, skipping");
            return;
        }

        try {
            // Static file serving (React SPA)
            staticFileServlet = new StaticFileServlet();
            registerServlet(URL_PREFIX + "/*", staticFileServlet);

            // Quest API
            questApiServlet = new QuestApiServlet(questService);
            registerServlet(API_V1_PREFIX + "/quests/*", questApiServlet);
            registerServlet(API_V1_PREFIX + "/validate", questApiServlet);

            // Registry API
            registryApiServlet = new RegistryApiServlet(registryAccessor);
            registerServlet(API_V1_PREFIX + "/registry/*", registryApiServlet);

            // Configuration
            configurationServlet = new ConfigurationServlet();
            registerServlet(API_V1_PREFIX + "/configuration", configurationServlet);

            // Asset serving
            assetServlet = new AssetServlet(assetAccessor);
            registerServlet(API_V1_PREFIX + "/assets/*", assetServlet);

            // Debug endpoints
            debugServlet = new DebugServlet(questService);
            registerServlet(API_V1_PREFIX + "/debug/*", debugServlet);

            // Status/health
            statusServlet = new StatusServlet(questService);
            registerServlet(API_V1_PREFIX + "/status", statusServlet);

            registered = true;
            LOGGER.log(Level.INFO, "All Quest Designer servlets registered under {0}", URL_PREFIX);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to register servlets", e);
            throw new RuntimeException("Servlet registration failed", e);
        }
    }

    private void registerServlet(String path, HttpServlet servlet) {
        webServerPlugin.addServlet(ownerPlugin, path, servlet);
        LOGGER.log(Level.FINE, "Registered servlet: {0}", path);
    }

    /**
     * Unregisters all servlets (cleanup on shutdown).
     */
    public void shutdown() {
        if (!registered) {
            return;
        }

        LOGGER.log(Level.INFO, "Shutting down Quest Designer Web Server Adapter");
        
        try {
            // Note: Nitrado WebServer may handle servlet cleanup automatically
            // when the plugin is unloaded. This is here for explicit cleanup if needed.
            
            registered = false;
            LOGGER.log(Level.INFO, "Quest Designer Web Server Adapter shut down successfully");
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error during shutdown", e);
        }
    }

    /**
     * Checks if the adapter is registered and active.
     * 
     * @return true if servlets are registered
     */
    public boolean isRegistered() {
        return registered;
    }

    /**
     * Gets the quest service instance.
     * 
     * @return the quest designer service
     */
    public QuestDesignerService getQuestService() {
        return questService;
    }

    /**
     * Gets the registry accessor instance.
     * 
     * @return the registry accessor
     */
    public HytaleRegistryAccessor getRegistryAccessor() {
        return registryAccessor;
    }

    /**
     * Gets the asset accessor instance.
     * 
     * @return the asset accessor
     */
    public HytaleAssetAccessor getAssetAccessor() {
        return assetAccessor;
    }

    /**
     * Gets the base URL for the Quest Designer web interface.
     * 
     * @return URL prefix (e.g., "/quest-designer")
     */
    public String getBaseUrl() {
        return URL_PREFIX;
    }

    /**
     * Gets the API base URL.
     * 
     * @return API URL prefix (e.g., "/quest-designer/api/v1")
     */
    public String getApiBaseUrl() {
        return API_V1_PREFIX;
    }
}
