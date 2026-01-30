package com.argonathsystems.adapter.hytale.ui;

import au.ellie.hyui.builders.HudBuilder;
import au.ellie.hyui.html.TemplateProcessor;
import com.argonathsystems.framework.ui.combat.ActionBarBuilder;
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
 * Adapter for rendering Action Bar HUD using HyUI.
 * 
 * <p>This adapter bridges the framework layer's {@link ActionBarBuilder}
 * with HyUI's HUD rendering capabilities. It handles:
 * <ul>
 *   <li>Template loading and caching</li>
 *   <li>Variable interpolation via HyUI's TemplateProcessor</li>
 *   <li>HUD rendering via HyUI's HudBuilder</li>
 *   <li>Action slot updates and cooldown animations</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Build action bar data in framework layer
 * ActionBarBuilder builder = new ActionBarBuilder()
 *     .setSlot(1, ActionSlot.builder(1).withAction("fireball").build())
 *     .setHealth(100, 100)
 *     .setPrimaryResource("mana", 80, 100);
 * 
 * // Render via adapter
 * ActionBarAdapter adapter = new ActionBarAdapter(store);
 * adapter.showActionBar(playerRef, builder);
 * }</pre>
 * 
 * @author Argonath Systems Team
 * @version 1.1.0
 * @since 1.1.0
 * @see ActionBarBuilder
 */
public class ActionBarAdapter {
    
    private static final Logger LOGGER = Logger.getLogger(ActionBarAdapter.class.getName());
    private static final String TEMPLATE_NAME = "action-bar";
    
    private final Store<EntityStore> store;
    private final TemplateLoader templateLoader;
    private final Set<PlayerRef> activePlayers;
    private String cachedTemplate;
    
    /**
     * Creates a new ActionBarAdapter.
     * 
     * @param store the Hytale entity store for UI rendering
     */
    public ActionBarAdapter(Store<EntityStore> store) {
        this(store, new TemplateLoader());
    }
    
    /**
     * Creates a new ActionBarAdapter with a custom template loader.
     * 
     * @param store the Hytale entity store
     * @param templateLoader the template loader for loading HYUIML
     */
    public ActionBarAdapter(Store<EntityStore> store, TemplateLoader templateLoader) {
        this.store = store;
        this.templateLoader = templateLoader;
        this.activePlayers = ConcurrentHashMap.newKeySet();
    }
    
    /**
     * Shows the Action Bar HUD to a player.
     * 
     * <p>HyUI manages HUD lifecycle through its Multi-HUD system.
     * Calling show() multiple times will add to the existing HUD stack.
     * 
     * @param player the target player
     * @param dataBuilder the action bar data builder
     * @return true if the HUD was shown successfully
     */
    public boolean showActionBar(PlayerRef player, ActionBarBuilder dataBuilder) {
        try {
            String template = loadTemplate(dataBuilder);
            String processedHtml = processTemplate(template, dataBuilder);
            
            HudBuilder.hudForPlayer(player)
                .fromHtml(processedHtml)
                .show(store);
            
            activePlayers.add(player);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to show action bar for " + player.getUsername(), e);
            return false;
        }
    }
    
    /**
     * Updates the Action Bar HUD with new data.
     * 
     * <p>Use this for updating cooldowns, resource bars, or slot changes.
     * 
     * @param player the target player
     * @param dataBuilder the updated action bar data
     * @return true if the update was successful
     */
    public boolean updateActionBar(PlayerRef player, ActionBarBuilder dataBuilder) {
        // Re-render the HUD with new data
        // HyUI's Multi-HUD system handles this gracefully
        return showActionBar(player, dataBuilder);
    }
    
    /**
     * Marks the action bar as closed for tracking purposes.
     * 
     * <p>Note: HyUI manages HUD lifecycle through its Multi-HUD system.
     * To actually remove a HUD, use the HUD instance's remove() method.
     * 
     * @param player the target player
     */
    public void closeActionBar(PlayerRef player) {
        activePlayers.remove(player);
        // HyUI Multi-HUD system manages actual removal
    }
    
    /**
     * Checks if a player has an active Action Bar HUD.
     * 
     * @param player the player to check
     * @return true if the player has an active action bar
     */
    public boolean hasActiveActionBar(PlayerRef player) {
        return activePlayers.contains(player);
    }
    
    /**
     * Loads the action bar template.
     */
    private String loadTemplate(ActionBarBuilder dataBuilder) {
        // Hot reload mode - use builder's supplier
        if (dataBuilder.hasTemplateSupplier()) {
            return dataBuilder.getTemplate();
        }
        
        // Normal mode - load from resources (with caching)
        if (cachedTemplate == null) {
            Optional<String> template = templateLoader.loadHudTemplate(TEMPLATE_NAME);
            cachedTemplate = template.orElseThrow(() -> 
                new IllegalStateException("Action bar template not found: " + TEMPLATE_NAME));
        }
        return cachedTemplate;
    }
    
    /**
     * Processes the template with the builder's variables.
     */
    private String processTemplate(String template, ActionBarBuilder dataBuilder) {
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
