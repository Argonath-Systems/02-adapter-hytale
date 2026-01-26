package com.argonathsystems.adapter.hytale.plugin;

import com.argonathsystems.adapter.hytale.api.HyQuestHttpController;
import com.argonathsystems.adapter.hytale.integration.QuestFormatConverter;
import com.argonathsystems.hyquest.model.QuestDefinition;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * Integration manager for HyQuestUI ↔ Hytale Server communication.
 * 
 * <p>This class is responsible for:
 * <ul>
 *   <li>Starting the HTTP REST API server</li>
 *   <li>Syncing quests between HyQuestUI and Quest Framework</li>
 *   <li>Converting quest formats</li>
 *   <li>Managing quest lifecycle</li>
 * </ul>
 * 
 * <h2>Usage in Hytale Plugin:</h2>
 * <pre>{@code
 * public class MyHytalePlugin extends JavaPlugin {
 *     private HyQuestIntegration hyquestIntegration;
 *     
 *     @Override
 *     public void onEnable() {
 *         Path dataDir = getDataFolder().toPath();
 *         hyquestIntegration = new HyQuestIntegration(dataDir, 19133);
 *         hyquestIntegration.start();
 *         
 *         // Sync quests to framework
 *         hyquestIntegration.syncToFramework(questFramework);
 *     }
 *     
 *     @Override
 *     public void onDisable() {
 *         hyquestIntegration.stop();
 *     }
 * }
 * }</pre>
 * 
 * @author Argonath Systems
 * @version 1.0.0
 * @since 1.0.0
 */
public class HyQuestIntegration {
    
    private static final Logger LOGGER = Logger.getLogger(HyQuestIntegration.class.getName());
    
    private final HyQuestHttpController httpController;
    private final QuestFormatConverter converter;
    private final Path dataDirectory;
    
    /**
     * Create HyQuest integration instance.
     * 
     * @param dataDirectory Server data directory
     * @param apiPort HTTP API port (default: 19133)
     */
    public HyQuestIntegration(Path dataDirectory, int apiPort) {
        this.dataDirectory = dataDirectory;
        this.httpController = new HyQuestHttpController(dataDirectory, apiPort);
        this.converter = new QuestFormatConverter();
        
        LOGGER.info("HyQuest Integration initialized");
        LOGGER.info("  Data directory: " + dataDirectory);
        LOGGER.info("  API port: " + apiPort);
    }
    
    /**
     * Create with default port (19133).
     */
    public HyQuestIntegration(Path dataDirectory) {
        this(dataDirectory, 19133);
    }
    
    /**
     * Start the HTTP API server.
     * 
     * @return This instance for chaining
     */
    public HyQuestIntegration start() {
        httpController.start();
        LOGGER.info("HyQuest Integration started successfully");
        return this;
    }
    
    /**
     * Stop the HTTP API server.
     */
    public void stop() {
        httpController.stop();
        LOGGER.info("HyQuest Integration stopped");
    }
    
    /**
     * Sync all quests from HyQuestUI to Quest Framework.
     * 
     * <p>This method:
     * <ol>
     *   <li>Fetches all quests from HyQuestUI API service</li>
     *   <li>Converts them to Quest Framework format</li>
     *   <li>Registers them with the quest framework</li>
     * </ol>
     * 
     * @param questFramework Quest framework instance to sync to
     * @return Number of quests synced
     */
    public int syncToFramework(Object questFramework) {
        // TODO: Implement actual Quest Framework integration
        // For now, just log the conversion
        
        var allQuests = getHttpController().getApiService().getAllQuests();
        int synced = 0;
        
        for (com.argonathsystems.hyquest.model.QuestDefinition hyQuest : allQuests) {
            try {
                var frameworkQuest = converter.toFrameworkFormat(hyQuest);
                
                // TODO: Register with quest framework
                // questFramework.registerQuest(frameworkQuest);
                
                LOGGER.info("Synced quest: " + frameworkQuest.getId());
                synced++;
            } catch (Exception e) {
                LOGGER.warning("Failed to sync quest " + hyQuest.getId() + ": " + e.getMessage());
            }
        }
        
        LOGGER.info("Synced " + synced + " quests to Quest Framework");
        return synced;
    }
    
    /**
     * Export a Quest Framework quest to HyQuestUI.
     * 
     * @param frameworkQuest Quest from Quest Framework
     * @return true if exported successfully
     */
    public boolean exportToHyQuest(com.lordofthetales.framework.quest.model.QuestDefinition frameworkQuest) {
        try {
            com.argonathsystems.hyquest.model.QuestDefinition hyQuest = converter.toHyQuestFormat(frameworkQuest);
            getHttpController().getApiService().createQuest(hyQuest);
            
            LOGGER.info("Exported quest to HyQuestUI: " + frameworkQuest.getId());
            return true;
        } catch (Exception e) {
            LOGGER.warning("Failed to export quest " + frameworkQuest.getId() + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if the integration is running.
     */
    public boolean isRunning() {
        return httpController.isRunning();
    }
    
    /**
     * Get the HTTP controller (for advanced usage).
     */
    public HyQuestHttpController getHttpController() {
        return httpController;
    }
    
    /**
     * Get the format converter (for advanced usage).
     */
    public QuestFormatConverter getConverter() {
        return converter;
    }
    
    // --- Static Factory Methods ---
    
    /**
     * Create and start integration with default settings.
     * 
     * @param serverDataPath Server data directory path
     * @return Started integration instance
     */
    public static HyQuestIntegration startDefault(String serverDataPath) {
        Path dataDir = Paths.get(serverDataPath);
        return new HyQuestIntegration(dataDir).start();
    }
}
