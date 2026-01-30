package com.argonathsystems.adapter.hytale.ui;

import au.ellie.hyui.builders.HudBuilder;
import au.ellie.hyui.html.TemplateProcessor;
import com.argonathsystems.framework.ui.world.CompassBarBuilder;
import com.argonathsystems.framework.ui.template.TemplateLoader;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adapter for rendering Compass Bar HUD using HyUI.
 * 
 * <p>This adapter bridges the framework layer's {@link CompassBarBuilder}
 * with HyUI's HUD rendering capabilities. It handles:
 * <ul>
 *   <li>Skyrim-style horizontal compass</li>
 *   <li>Direction markers (N, E, S, W, etc)</li>
 *   <li>Quest objective markers with distance</li>
 *   <li>POI discovery markers</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Build compass data in framework layer
 * CompassBarBuilder builder = new CompassBarBuilder()
 *     .setPlayerHeading(45) // Facing NE
 *     .setPlayerPosition(100, 200)
 *     .addMarker(CompassMarker.builder("quest1", "main")
 *         .atPosition(150, 250).withDistance(70).tracked(true).build());
 * 
 * // Render via adapter
 * CompassBarAdapter adapter = new CompassBarAdapter(store);
 * adapter.showCompass(playerRef, builder);
 * }</pre>
 * 
 * @author Argonath Systems Team
 * @version 1.2.0
 * @since 1.1.0
 * @see CompassBarBuilder
 */
public class CompassBarAdapter {
    
    private static final Logger LOGGER = Logger.getLogger(CompassBarAdapter.class.getName());
    private static final String TEMPLATE_NAME = "compass-bar";
    
    private final Store<EntityStore> store;
    private final TemplateLoader templateLoader;
    private final Set<PlayerRef> activePlayers;
    private String cachedTemplate;
    
    /**
     * Creates a new CompassBarAdapter.
     * 
     * @param store the Hytale entity store for UI rendering
     */
    public CompassBarAdapter(Store<EntityStore> store) {
        this(store, new TemplateLoader());
    }
    
    /**
     * Creates a new CompassBarAdapter with a custom template loader.
     * 
     * @param store the Hytale entity store
     * @param templateLoader the template loader for loading HYUIML
     */
    public CompassBarAdapter(Store<EntityStore> store, TemplateLoader templateLoader) {
        this.store = store;
        this.templateLoader = templateLoader;
        this.activePlayers = ConcurrentHashMap.newKeySet();
    }
    
    /**
     * Shows the Compass Bar HUD to a player.
     * 
     * <p>Uses HyUI's multi-HUD system which automatically manages HUD stacking.
     * 
     * @param player the target player
     * @param dataBuilder the compass data builder
     * @return true if the HUD was shown successfully
     */
    public boolean showCompass(PlayerRef player, CompassBarBuilder dataBuilder) {
        try {
            String template = loadTemplate(dataBuilder);
            String processedHtml = processTemplate(template, dataBuilder);
            
            HudBuilder.hudForPlayer(player)
                .fromHtml(processedHtml)
                .show(store);
            
            activePlayers.add(player);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to show compass for " + player.getUsername(), e);
            return false;
        }
    }
    
    /**
     * Updates the Compass Bar HUD with new data.
     * 
     * <p>Use this for updating player heading, position, or markers.
     * Note: For high-frequency updates (every tick), consider using
     * a throttled update mechanism.
     * 
     * @param player the target player
     * @param dataBuilder the updated compass data
     * @return true if the update was successful
     */
    public boolean updateCompass(PlayerRef player, CompassBarBuilder dataBuilder) {
        return showCompass(player, dataBuilder);
    }
    
    /**
     * Marks compass as closed for tracking purposes.
     * 
     * <p>Note: HyUI's multi-HUD system manages HUD lifecycle automatically.
     * This method just updates our tracking state.
     * 
     * @param player the target player
     */
    public void closeCompass(PlayerRef player) {
        activePlayers.remove(player);
        // HyUI multi-HUD system handles actual HUD cleanup
    }
    
    /**
     * Checks if a player has an active Compass Bar HUD.
     * 
     * @param player the player to check
     * @return true if the player has an active compass
     */
    public boolean hasActiveCompass(PlayerRef player) {
        return activePlayers.contains(player);
    }
    
    /**
     * Loads the compass bar template.
     */
    private String loadTemplate(CompassBarBuilder dataBuilder) {
        if (dataBuilder.hasTemplateSupplier()) {
            return dataBuilder.getTemplate();
        }
        
        if (cachedTemplate == null) {
            Optional<String> template = templateLoader.loadHudTemplate(TEMPLATE_NAME);
            cachedTemplate = template.orElseThrow(() -> 
                new IllegalStateException("Compass bar template not found: " + TEMPLATE_NAME));
        }
        return cachedTemplate;
    }
    
    /**
     * Processes the template with the builder's variables.
     */
    private String processTemplate(String template, CompassBarBuilder dataBuilder) {
        TemplateProcessor processor = new TemplateProcessor();
        
        Map<String, Object> variables = dataBuilder.buildTemplateVariables();
        variables.forEach(processor::setVariable);
        
        return processor.process(template);
    }
    
    /**
     * Clears the cached template.
     */
    public void clearTemplateCache() {
        cachedTemplate = null;
    }
}
