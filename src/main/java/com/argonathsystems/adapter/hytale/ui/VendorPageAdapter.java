package com.argonathsystems.adapter.hytale.ui;

import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.builders.HyUIPage;
import au.ellie.hyui.html.TemplateProcessor;
import com.argonathsystems.framework.ui.vendor.VendorPageBuilder;
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
 * Adapter for rendering NPC Vendor pages using HyUI.
 * 
 * <p>This adapter bridges the framework layer's {@link VendorPageBuilder}
 * with HyUI's rendering capabilities. It handles:
 * <ul>
 *   <li>Template loading and caching</li>
 *   <li>Variable interpolation via HyUI's TemplateProcessor</li>
 *   <li>Page rendering via HyUI's PageBuilder</li>
 *   <li>Buy/Sell/Buyback tab switching</li>
 *   <li>Transaction processing</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Build vendor data in framework layer
 * VendorPageBuilder builder = new VendorPageBuilder()
 *     .setVendorName("General Merchant")
 *     .setPlayerGold(1500)
 *     .addBuyItem(VendorItem.builder("sword", "Iron Sword")
 *         .withBuyPrice(100).build())
 *     .setActiveTab("buy");
 * 
 * // Render via adapter
 * VendorPageAdapter adapter = new VendorPageAdapter(store);
 * adapter.showVendor(playerRef, builder);
 * }</pre>
 * 
 * @author Argonath Systems Team
 * @version 1.1.0
 * @since 1.1.0
 * @see VendorPageBuilder
 */
public class VendorPageAdapter {
    
    private static final Logger LOGGER = Logger.getLogger(VendorPageAdapter.class.getName());
    private static final String TEMPLATE_NAME = "npc-vendor";
    
    private final Store<EntityStore> store;
    private final TemplateLoader templateLoader;
    private final Map<PlayerRef, HyUIPage> activePages;
    private final Map<PlayerRef, VendorPageBuilder> activeBuilders;
    private String cachedTemplate;
    
    /**
     * Creates a new VendorPageAdapter.
     * 
     * @param store the Hytale entity store for UI rendering
     */
    public VendorPageAdapter(Store<EntityStore> store) {
        this(store, new TemplateLoader());
    }
    
    /**
     * Creates a new VendorPageAdapter with a custom template loader.
     * 
     * @param store the Hytale entity store
     * @param templateLoader the template loader for loading HYUIML
     */
    public VendorPageAdapter(Store<EntityStore> store, TemplateLoader templateLoader) {
        this.store = store;
        this.templateLoader = templateLoader;
        this.activePages = new ConcurrentHashMap<>();
        this.activeBuilders = new ConcurrentHashMap<>();
    }
    
    /**
     * Shows the Vendor page to a player.
     * 
     * @param player the target player
     * @param builder the vendor data builder
     * @return true if the vendor page was shown successfully
     */
    public boolean showVendor(PlayerRef player, VendorPageBuilder builder) {
        try {
            String template = loadTemplate(builder);
            String processedHtml = processTemplate(template, builder);
            
            HyUIPage page = PageBuilder.pageForPlayer(player)
                .fromHtml(processedHtml)
                .open(player, store);
            
            activePages.put(player, page);
            activeBuilders.put(player, builder);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to show vendor for " + player.getUsername(), e);
            return false;
        }
    }
    
    /**
     * Updates the Vendor page with new data.
     * 
     * <p>Use this for tab switching, item selection, or gold updates
     * without closing and reopening the page.
     * 
     * @param player the target player
     * @param builder the updated vendor data
     * @return true if the update was successful
     */
    public boolean updateVendor(PlayerRef player, VendorPageBuilder builder) {
        // For now, just re-render the whole page
        return showVendor(player, builder);
    }
    
    /**
     * Closes the Vendor page for a player.
     * 
     * @param player the target player
     */
    public void closeVendor(PlayerRef player) {
        HyUIPage page = activePages.remove(player);
        activeBuilders.remove(player);
        if (page != null) {
            try {
                page.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to close vendor for " + player.getUsername(), e);
            }
        }
    }
    
    /**
     * Checks if a player has an active Vendor page.
     * 
     * @param player the player to check
     * @return true if the player has an active vendor page
     */
    public boolean hasActiveVendor(PlayerRef player) {
        return activePages.containsKey(player);
    }
    
    /**
     * Gets the current builder for a player's vendor session.
     * 
     * @param player the player
     * @return the builder, or null if no active session
     */
    public VendorPageBuilder getActiveBuilder(PlayerRef player) {
        return activeBuilders.get(player);
    }
    
    /**
     * Switches the active tab in the Vendor page.
     * 
     * @param player the target player
     * @param tab the tab to switch to ("buy", "sell", or "buyback")
     * @return true if the switch was successful
     */
    public boolean switchTab(PlayerRef player, String tab) {
        VendorPageBuilder builder = activeBuilders.get(player);
        if (builder != null) {
            builder.setActiveTab(tab);
            return updateVendor(player, builder);
        }
        return false;
    }
    
    /**
     * Selects an item in the vendor interface.
     * 
     * @param player the target player
     * @param itemId the item to select
     * @return true if the selection was successful
     */
    public boolean selectItem(PlayerRef player, String itemId) {
        VendorPageBuilder builder = activeBuilders.get(player);
        if (builder != null) {
            builder.setSelectedItem(itemId);
            builder.setSelectedQuantity(1);
            return updateVendor(player, builder);
        }
        return false;
    }
    
    /**
     * Updates the transaction quantity.
     * 
     * @param player the target player
     * @param quantity the new quantity
     * @return true if the update was successful
     */
    public boolean setQuantity(PlayerRef player, int quantity) {
        VendorPageBuilder builder = activeBuilders.get(player);
        if (builder != null) {
            builder.setSelectedQuantity(quantity);
            return updateVendor(player, builder);
        }
        return false;
    }
    
    /**
     * Loads the vendor template.
     * 
     * <p>If the builder has a template supplier (for hot reload), uses that.
     * Otherwise, loads from the template loader.
     */
    private String loadTemplate(VendorPageBuilder builder) {
        // Hot reload mode - use builder's supplier
        if (builder.hasTemplateSupplier()) {
            return builder.getTemplate();
        }
        
        // Normal mode - load from resources (with caching)
        if (cachedTemplate == null) {
            Optional<String> template = templateLoader.loadPageTemplate(TEMPLATE_NAME);
            cachedTemplate = template.orElseThrow(() -> 
                new IllegalStateException("Vendor template not found: " + TEMPLATE_NAME));
        }
        return cachedTemplate;
    }
    
    /**
     * Processes the template with the builder's variables.
     */
    private String processTemplate(String template, VendorPageBuilder builder) {
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
