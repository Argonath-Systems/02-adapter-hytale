package com.argonathsystems.adapter.hytale.ui;

import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.html.TemplateProcessor;
import com.argonathsystems.framework.ui.dialogue.DialoguePageBuilder;
import com.argonathsystems.framework.ui.template.TemplateLoader;
import hytale.player.PlayerRef;
import hytale.plugin.plugin.AssetStore;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adapter for rendering NPC Dialogue pages using HyUI.
 * 
 * <p>This adapter bridges the framework layer's {@link DialoguePageBuilder}
 * with HyUI's rendering capabilities. It handles:
 * <ul>
 *   <li>Template loading and caching</li>
 *   <li>Variable interpolation via HyUI's TemplateProcessor</li>
 *   <li>Page rendering via HyUI's PageBuilder</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Build dialogue data in framework layer
 * DialoguePageBuilder builder = new DialoguePageBuilder()
 *     .setNpcName("Gandalf")
 *     .setDialogueText("You shall not pass!")
 *     .addChoice(1, "Who are you?", "who", true)
 *     .addChoice(2, "Goodbye", "bye", true);
 * 
 * // Render via adapter
 * DialoguePageAdapter adapter = new DialoguePageAdapter(assetStore);
 * adapter.showDialogue(playerRef, builder);
 * }</pre>
 * 
 * @author Argonath Systems Team
 * @version 1.1.0
 * @since 1.1.0
 * @see DialoguePageBuilder
 */
public class DialoguePageAdapter {
    
    private static final Logger LOGGER = Logger.getLogger(DialoguePageAdapter.class.getName());
    private static final String TEMPLATE_NAME = "npc-dialogue";
    
    private final AssetStore assetStore;
    private final TemplateLoader templateLoader;
    private String cachedTemplate;
    
    /**
     * Creates a new DialoguePageAdapter.
     * 
     * @param assetStore the HyUI asset store for UI rendering
     */
    public DialoguePageAdapter(AssetStore assetStore) {
        this(assetStore, new TemplateLoader());
    }
    
    /**
     * Creates a new DialoguePageAdapter with a custom template loader.
     * 
     * @param assetStore the HyUI asset store
     * @param templateLoader the template loader for loading HYUIML
     */
    public DialoguePageAdapter(AssetStore assetStore, TemplateLoader templateLoader) {
        this.assetStore = assetStore;
        this.templateLoader = templateLoader;
    }
    
    /**
     * Shows an NPC dialogue page to a player.
     * 
     * @param player the target player
     * @param builder the dialogue data builder
     * @return true if the dialogue was shown successfully
     */
    public boolean showDialogue(PlayerRef player, DialoguePageBuilder builder) {
        try {
            String template = loadTemplate(builder);
            String processedHtml = processTemplate(template, builder);
            
            PageBuilder.pageForPlayer(player)
                .fromHtml(processedHtml)
                .open(assetStore);
            
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to show dialogue for " + player.getUsername(), e);
            return false;
        }
    }
    
    /**
     * Updates an existing NPC dialogue page.
     * 
     * <p>Use this for updating dialogue text or choices without closing
     * and reopening the page.
     * 
     * @param player the target player
     * @param builder the updated dialogue data
     * @return true if the update was successful
     */
    public boolean updateDialogue(PlayerRef player, DialoguePageBuilder builder) {
        // For now, just re-render the whole page
        // Future: Use HyUI's incremental update API if available
        return showDialogue(player, builder);
    }
    
    /**
     * Closes the dialogue page for a player.
     * 
     * @param player the target player
     */
    public void closeDialogue(PlayerRef player) {
        try {
            PageBuilder.closePageForPlayer(player, assetStore);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to close dialogue for " + player.getUsername(), e);
        }
    }
    
    /**
     * Loads the dialogue template.
     * 
     * <p>If the builder has a template supplier (for hot reload), uses that.
     * Otherwise, loads from the template loader.
     */
    private String loadTemplate(DialoguePageBuilder builder) {
        // Hot reload mode - use builder's supplier
        if (builder.hasTemplateSupplier()) {
            return builder.getTemplate();
        }
        
        // Normal mode - load from resources (with caching)
        if (cachedTemplate == null) {
            Optional<String> template = templateLoader.loadHudTemplate(TEMPLATE_NAME);
            cachedTemplate = template.orElseThrow(() -> 
                new IllegalStateException("Dialogue template not found: " + TEMPLATE_NAME));
        }
        return cachedTemplate;
    }
    
    /**
     * Processes the template with the builder's variables.
     */
    private String processTemplate(String template, DialoguePageBuilder builder) {
        TemplateProcessor processor = new TemplateProcessor();
        
        Map<String, Object> variables = builder.buildTemplateVariables();
        variables.forEach(processor::setVariable);
        
        return processor.process(template);
    }
    
    /**
     * Clears the cached template.
     * 
     * <p>Call this to force the template to be reloaded from disk.
     * Useful after a hot reload.
     */
    public void clearTemplateCache() {
        cachedTemplate = null;
    }
}
