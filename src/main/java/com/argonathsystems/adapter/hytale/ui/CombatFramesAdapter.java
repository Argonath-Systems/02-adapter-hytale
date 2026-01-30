package com.argonathsystems.adapter.hytale.ui;

import au.ellie.hyui.builders.HudBuilder;
import au.ellie.hyui.html.TemplateProcessor;
import com.argonathsystems.framework.ui.combat.CombatFramesBuilder;
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
 * Adapter for rendering Combat Frames HUD using HyUI.
 * 
 * <p>This adapter bridges the framework layer's {@link CombatFramesBuilder}
 * with HyUI's HUD rendering capabilities. It handles:
 * <ul>
 *   <li>Player frame (top-left)</li>
 *   <li>Target frame (top-center)</li>
 *   <li>Target-of-target frame</li>
 *   <li>Party/raid frames</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Build combat frames data in framework layer
 * CombatFramesBuilder builder = new CombatFramesBuilder()
 *     .setPlayerFrame(UnitFrame.builder("player", "Hero")
 *         .withHealth(95, 100).asPlayer().build())
 *     .setTargetFrame(UnitFrame.builder("mob", "Goblin")
 *         .withHealth(50, 75).asHostile().build());
 * 
 * // Render via adapter
 * CombatFramesAdapter adapter = new CombatFramesAdapter(store);
 * adapter.showCombatFrames(playerRef, builder);
 * }</pre>
 * 
 * @author Argonath Systems Team
 * @version 1.2.0
 * @since 1.1.0
 * @see CombatFramesBuilder
 */
public class CombatFramesAdapter {
    
    private static final Logger LOGGER = Logger.getLogger(CombatFramesAdapter.class.getName());
    private static final String TEMPLATE_NAME = "combat-frames";
    
    private final Store<EntityStore> store;
    private final TemplateLoader templateLoader;
    private final Set<PlayerRef> activePlayers;
    private String cachedTemplate;
    
    /**
     * Creates a new CombatFramesAdapter.
     * 
     * @param store the Hytale entity store for UI rendering
     */
    public CombatFramesAdapter(Store<EntityStore> store) {
        this(store, new TemplateLoader());
    }
    
    /**
     * Creates a new CombatFramesAdapter with a custom template loader.
     * 
     * @param store the Hytale entity store
     * @param templateLoader the template loader for loading HYUIML
     */
    public CombatFramesAdapter(Store<EntityStore> store, TemplateLoader templateLoader) {
        this.store = store;
        this.templateLoader = templateLoader;
        this.activePlayers = ConcurrentHashMap.newKeySet();
    }
    
    /**
     * Shows the Combat Frames HUD to a player.
     * 
     * <p>Uses HyUI's multi-HUD system which automatically manages HUD stacking.
     * 
     * @param player the target player
     * @param builder the combat frames data builder
     * @return true if the HUD was shown successfully
     */
    public boolean showCombatFrames(PlayerRef player, CombatFramesBuilder builder) {
        try {
            String template = loadTemplate(builder);
            String processedHtml = processTemplate(template, builder);
            
            HudBuilder.hudForPlayer(player)
                .fromHtml(processedHtml)
                .show(store);
            
            activePlayers.add(player);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to show combat frames for " + player.getUsername(), e);
            return false;
        }
    }
    
    /**
     * Updates the Combat Frames HUD with new data.
     * 
     * <p>Use this for updating health bars, buffs/debuffs, or target changes.
     * 
     * @param player the target player
     * @param builder the updated combat frames data
     * @return true if the update was successful
     */
    public boolean updateCombatFrames(PlayerRef player, CombatFramesBuilder builder) {
        return showCombatFrames(player, builder);
    }
    
    /**
     * Marks player as no longer having combat frames.
     * 
     * <p>Note: HyUI's multi-HUD system manages HUD lifecycle automatically.
     * This method just updates our tracking state.
     * 
     * @param player the target player
     */
    public void closeCombatFrames(PlayerRef player) {
        activePlayers.remove(player);
        // HyUI multi-HUD system handles actual HUD cleanup
    }
    
    /**
     * Checks if a player has active Combat Frames HUD.
     * 
     * @param player the player to check
     * @return true if the player has active combat frames
     */
    public boolean hasActiveCombatFrames(PlayerRef player) {
        return activePlayers.contains(player);
    }
    
    /**
     * Loads the combat frames template.
     */
    private String loadTemplate(CombatFramesBuilder builder) {
        if (builder.hasTemplateSupplier()) {
            return builder.getTemplate();
        }
        
        if (cachedTemplate == null) {
            Optional<String> template = templateLoader.loadHudTemplate(TEMPLATE_NAME);
            cachedTemplate = template.orElseThrow(() -> 
                new IllegalStateException("Combat frames template not found: " + TEMPLATE_NAME));
        }
        return cachedTemplate;
    }
    
    /**
     * Processes the template with the builder's variables.
     */
    private String processTemplate(String template, CombatFramesBuilder builder) {
        TemplateProcessor processor = new TemplateProcessor();
        
        Map<String, Object> variables = builder.buildTemplateVariables();
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
