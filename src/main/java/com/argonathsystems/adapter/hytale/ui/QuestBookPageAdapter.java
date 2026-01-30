package com.argonathsystems.adapter.hytale.ui;

import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.builders.HyUIPage;
import au.ellie.hyui.html.TemplateProcessor;
import com.argonathsystems.framework.ui.quest.QuestBookPageBuilder;
import com.argonathsystems.framework.ui.template.TemplateLoader;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adapter for rendering Quest Book pages using HyUI.
 * 
 * <p>This adapter bridges the framework layer's {@link QuestBookPageBuilder}
 * with HyUI's rendering capabilities. It handles:
 * <ul>
 *   <li>Template loading and caching</li>
 *   <li>Variable interpolation via HyUI's TemplateProcessor</li>
 *   <li>Page rendering via HyUI's PageBuilder</li>
 *   <li>Tab switching between Active/Complete/Failed quests</li>
 *   <li>Quest selection and detail panel updates</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Build quest book data in framework layer
 * QuestCategory mainQuests = new QuestCategory("Main Story");
 * mainQuests.addQuest(new QuestListItem("q1", "A Shadow Falls", 10, "main", "icon.png"));
 * 
 * QuestBookPageBuilder builder = new QuestBookPageBuilder()
 *     .setActiveTab("active")
 *     .addCategory(mainQuests)
 *     .setSelectedQuest(questDetail);
 * 
 * // Render via adapter
 * QuestBookPageAdapter adapter = new QuestBookPageAdapter(store);
 * adapter.showQuestBook(playerRef, builder);
 * }</pre>
 * 
 * @author Argonath Systems Team
 * @version 1.1.0
 * @since 1.1.0
 * @see QuestBookPageBuilder
 */
public class QuestBookPageAdapter {
    
    private static final Logger LOGGER = Logger.getLogger(QuestBookPageAdapter.class.getName());
    private static final String TEMPLATE_NAME = "quest-book";
    
    private final Store<EntityStore> store;
    private final TemplateLoader templateLoader;
    private final Map<PlayerRef, HyUIPage> activePages;
    private String cachedTemplate;
    
    /**
     * Creates a new QuestBookPageAdapter.
     * 
     * @param store the Hytale entity store for UI rendering
     */
    public QuestBookPageAdapter(Store<EntityStore> store) {
        this(store, new TemplateLoader());
    }
    
    /**
     * Creates a new QuestBookPageAdapter with a custom template loader.
     * 
     * @param store the Hytale entity store
     * @param templateLoader the template loader for loading HYUIML
     */
    public QuestBookPageAdapter(Store<EntityStore> store, TemplateLoader templateLoader) {
        this.store = store;
        this.templateLoader = templateLoader;
        this.activePages = new ConcurrentHashMap<>();
    }
    
    /**
     * Shows the Quest Book page to a player.
     * 
     * @param player the target player
     * @param builder the quest book data builder
     * @return true if the quest book was shown successfully
     */
    public boolean showQuestBook(PlayerRef player, QuestBookPageBuilder builder) {
        try {
            String template = loadTemplate(builder);
            String processedHtml = processTemplate(template, builder);
            
            HyUIPage page = PageBuilder.pageForPlayer(player)
                .fromHtml(processedHtml)
                .open(player, store);
            
            activePages.put(player, page);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to show quest book for " + player.getUsername(), e);
            return false;
        }
    }
    
    /**
     * Updates the Quest Book page with new data.
     * 
     * <p>Use this for tab switching, quest selection, or refreshing quest status
     * without closing and reopening the page.
     * 
     * @param player the target player
     * @param builder the updated quest book data
     * @return true if the update was successful
     */
    public boolean updateQuestBook(PlayerRef player, QuestBookPageBuilder builder) {
        // For now, just re-render the whole page
        // Future: Use HyUI's incremental update API if available
        return showQuestBook(player, builder);
    }
    
    /**
     * Closes the Quest Book page for a player.
     * 
     * @param player the target player
     */
    public void closeQuestBook(PlayerRef player) {
        HyUIPage page = activePages.remove(player);
        if (page != null) {
            try {
                page.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to close quest book for " + player.getUsername(), e);
            }
        }
    }
    
    /**
     * Checks if a player has an active Quest Book page.
     * 
     * @param player the player to check
     * @return true if the player has an active quest book
     */
    public boolean hasActiveQuestBook(PlayerRef player) {
        return activePages.containsKey(player);
    }
    
    /**
     * Switches the active tab in the Quest Book.
     * 
     * <p>Convenience method for tab switching that updates the builder
     * and refreshes the page.
     * 
     * @param player the target player
     * @param builder the quest book builder
     * @param tab the tab to switch to ("active", "complete", or "failed")
     * @return true if the switch was successful
     */
    public boolean switchTab(PlayerRef player, QuestBookPageBuilder builder, String tab) {
        builder.setActiveTab(tab);
        return updateQuestBook(player, builder);
    }
    
    /**
     * Loads the quest book template.
     * 
     * <p>If the builder has a template supplier (for hot reload), uses that.
     * Otherwise, loads from the template loader.
     */
    private String loadTemplate(QuestBookPageBuilder builder) {
        // Hot reload mode - use builder's supplier
        if (builder.hasTemplateSupplier()) {
            return builder.getTemplate();
        }
        
        // Normal mode - load from resources (with caching)
        if (cachedTemplate == null) {
            Optional<String> template = templateLoader.loadPageTemplate(TEMPLATE_NAME);
            cachedTemplate = template.orElseThrow(() -> 
                new IllegalStateException("Quest book template not found: " + TEMPLATE_NAME));
        }
        return cachedTemplate;
    }
    
    /**
     * Processes the template with the builder's variables.
     */
    private String processTemplate(String template, QuestBookPageBuilder builder) {
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
