package com.argonathsystems.adapter.hytale.ui;

import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.html.TemplateProcessor;
import com.argonathsystems.framework.ui.menu.mount.MountCollectionDataProvider;
import com.argonathsystems.framework.ui.template.TemplateLoader;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adapter for rendering Mount Collection Page using HyUI.
 * 
 * <p>This adapter bridges the mod layer's {@link MountCollectionPageBuilder}
 * with HyUI's full page rendering capabilities. The collection page provides:
 * <ul>
 *   <li>Grid view of all mounts (owned, locked, hidden)</li>
 *   <li>Category filtering and sorting</li>
 *   <li>Search functionality</li>
 *   <li>Detailed mount inspection panel</li>
 *   <li>Favorite management</li>
 *   <li>Active mount selection</li>
 * </ul>
 * 
 * <h2>Page Layout</h2>
 * <pre>
 * ┌────────────────────────────────────────────────────┐
 * │ Header: Mount Collection              [Search] [X] │
 * ├────────────────────────────────────────────────────┤
 * │ Categories │ Mount Grid (4 columns)   │ Details   │
 * │            │ ┌──┐┌──┐┌──┐┌──┐        │ [Image]   │
 * │ All        │ │  ││  ││  ││  │        │ Stats     │
 * │ Ground     │ ├──┤├──┤├──┤├──┤        │ Abilities │
 * │ Flying     │ │  ││  ││  ││  │        │ Lore      │
 * │ Aquatic    │ └──┘└──┘└──┘└──┘        │           │
 * │ Exotic     │                          │ [Summon]  │
 * │            │ Showing 12/45 mounts     │ [Favorite]│
 * └────────────────────────────────────────────────────┘
 * </pre>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * MountCollectionPageBuilder builder = new MountCollectionPageBuilder()
 *     .setPlayerMounts(ownedMounts)
 *     .setAllMounts(allMounts)
 *     .setSelectedCategory("all")
 *     .setSelectedMount(selectedMountData);
 * 
 * MountCollectionPageAdapter adapter = new MountCollectionPageAdapter(store);
 * adapter.showCollectionPage(playerRef, builder);
 * }</pre>
 * 
 * @author Argonath Systems Team
 * @version 1.0.0
 * @since 1.0.0
 * @see MountCollectionPageBuilder
 * @see <a href="VDD-MISC-029-mount-collection.md">VDD-MISC-029: Mount Collection Page</a>
 */
public class MountCollectionPageAdapter {
    
    private static final Logger LOGGER = Logger.getLogger(MountCollectionPageAdapter.class.getName());
    private static final String TEMPLATE_PATH = "ui/page/mount-collection.hyuiml";
    
    private final Store<EntityStore> store;
    private final TemplateLoader templateLoader;
    private final Set<PlayerRef> activePlayers;
    private String cachedTemplate;
    
    /**
     * Creates a new MountCollectionPageAdapter.
     * 
     * @param store the Hytale entity store for UI rendering
     */
    public MountCollectionPageAdapter(Store<EntityStore> store) {
        this(store, new TemplateLoader());
    }
    
    /**
     * Creates a new MountCollectionPageAdapter with a custom template loader.
     * 
     * @param store the Hytale entity store
     * @param templateLoader the template loader for loading HYUIML
     */
    public MountCollectionPageAdapter(Store<EntityStore> store, TemplateLoader templateLoader) {
        this.store = store;
        this.templateLoader = templateLoader;
        this.activePlayers = ConcurrentHashMap.newKeySet();
    }
    
    /**
     * Shows the mount collection page to a player.
     * 
     * <p>This is a full-screen page that displays the player's mount collection.
     * It supports navigation, filtering, and mount management.
     * 
     * @param player the target player
     * @param dataProvider the collection page data provider (typically MountCollectionPageBuilder)
     * @return true if the page was shown successfully
     */
    public boolean showCollectionPage(PlayerRef player, MountCollectionDataProvider dataProvider) {
        try {
            String template = loadTemplate(dataProvider);
            String processedHtml = processTemplate(template, dataProvider.buildTemplateVariables());
            
            PageBuilder.pageForPlayer(player)
                .fromHtml(processedHtml)
                .open();
            
            activePlayers.add(player);
            LOGGER.fine("Mount collection page shown for player: " + player.getUsername());
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to show collection page for " + player.getUsername(), e);
            return false;
        }
    }
    
    /**
     * Updates the collection page with new data.
     * 
     * <p>Use this for:
     * <ul>
     *   <li>Category changes</li>
     *   <li>Search results</li>
     *   <li>Mount selection changes</li>
     *   <li>Sorting changes</li>
     * </ul>
     * 
     * @param player the target player
     * @param dataProvider updated collection data
     * @return true if the update was successful
     */
    public boolean updateCollectionPage(PlayerRef player, MountCollectionDataProvider dataProvider) {
        return showCollectionPage(player, dataProvider);
    }
    
    /**
     * Closes the collection page.
     * 
     * @param player the target player
     */
    public void closeCollectionPage(PlayerRef player) {
        activePlayers.remove(player);
        // HyUI page system manages actual removal
        LOGGER.fine("Mount collection page closed for player: " + player.getUsername());
    }
    
    /**
     * Checks if a player has the collection page open.
     * 
     * @param player the player to check
     * @return true if the player has the collection page open
     */
    public boolean isCollectionPageOpen(PlayerRef player) {
        return activePlayers.contains(player);
    }
    
    /**
     * Updates the selected mount in the details panel.
     * 
     * <p>This is a targeted update that only refreshes the right panel.
     * 
     * @param player the target player
     * @param dataProvider provider with new selected mount
     * @return true if the update was successful
     */
    public boolean updateSelectedMount(PlayerRef player, MountCollectionDataProvider dataProvider) {
        // For now, full page rebuild - HyUI may support partial updates later
        return updateCollectionPage(player, dataProvider);
    }
    
    /**
     * Updates the grid after a filter or search change.
     * 
     * @param player the target player
     * @param dataProvider provider with filtered mount list
     * @return true if the update was successful
     */
    public boolean updateMountGrid(PlayerRef player, MountCollectionDataProvider dataProvider) {
        return updateCollectionPage(player, dataProvider);
    }
    
    /**
     * Loads the collection page template.
     */
    private String loadTemplate(MountCollectionDataProvider dataProvider) {
        // Hot reload mode
        if (dataProvider.hasTemplateSupplier()) {
            return dataProvider.getTemplate();
        }
        
        // Cached mode
        if (cachedTemplate == null) {
            cachedTemplate = templateLoader.loadPageTemplate("mount-collection")
                .orElseThrow(() -> new IllegalStateException(
                    "Mount collection template not found"));
        }
        return cachedTemplate;
    }
    
    /**
     * Processes the template with data from the provider.
     */
    private String processTemplate(String template, Map<String, Object> variables) {
        
        TemplateProcessor processor = new TemplateProcessor();
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            processor.setVariable(entry.getKey(), entry.getValue());
        }
        
        return processor.process(template);
    }
    
    /**
     * Clears the template cache (for development hot reload).
     */
    public void clearTemplateCache() {
        cachedTemplate = null;
    }
}
