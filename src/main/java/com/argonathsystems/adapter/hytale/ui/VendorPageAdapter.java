package com.argonathsystems.adapter.hytale.ui;

import au.ellie.hyui.builders.HyUIPage;
import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.html.TemplateProcessor;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Adapter for NPC vendor/shop page.
 * 
 * <p>This adapter bridges the platform-agnostic vendor data
 * with the Hytale platform's UI system.
 * 
 * <p><b>Architecture:</b>
 * <ul>
 *   <li>VendorData → TemplateProcessor → HYUIML → Player UI</li>
 *   <li>Provides buy/sell interface with item listings</li>
 *   <li>ONLY module in adapter layer</li>
 * </ul>
 * 
 * @author Argonath Systems
 * @version 1.0.0-SNAPSHOT
 * @since 1.0.0
 */
public class VendorPageAdapter {
    
    private static final String VENDOR_PAGE_TEMPLATE_PATH = "config/ui/pages/npc-vendor.hyuiml";
    
    private final TemplateLoader templateLoader;
    private final PlayerRefResolver playerRefResolver;
    private final StoreProvider storeProvider;
    private BiConsumer<String, Integer> purchaseHandler;
    private BiConsumer<String, Integer> sellHandler;
    
    /**
     * Create a new vendor page adapter.
     * 
     * @param templateLoader Template file loader
     * @param playerRefResolver Resolver to get PlayerRef from Player
     * @param storeProvider Provider for entity store
     */
    public VendorPageAdapter(TemplateLoader templateLoader, PlayerRefResolver playerRefResolver, StoreProvider storeProvider) {
        this.templateLoader = templateLoader;
        this.playerRefResolver = playerRefResolver;
        this.storeProvider = storeProvider;
    }
    
    /**
     * Set the handler for purchase events.
     * 
     * @param handler Handler invoked when player purchases an item
     * @return this adapter
     */
    public VendorPageAdapter setPurchaseHandler(BiConsumer<String, Integer> handler) {
        this.purchaseHandler = handler;
        return this;
    }
    
    /**
     * Set the handler for sell events.
     * 
     * @param handler Handler invoked when player sells an item
     * @return this adapter
     */
    public VendorPageAdapter setSellHandler(BiConsumer<String, Integer> handler) {
        this.sellHandler = handler;
        return this;
    }
    
    /**
     * Show vendor page for a player.
     * 
     * @param player Player to show vendor page to
     * @param data Vendor data to display
     * @return Processed HTML ready for rendering
     */
    public String showVendor(Player player, VendorData data) {
        try {
            String template = templateLoader.loadTemplate(VENDOR_PAGE_TEMPLATE_PATH);
            String processedHtml = processTemplate(template, data);
            
            // Get PlayerRef for HyUI
            Optional<PlayerRef> playerRefOpt = playerRefResolver.resolve(player);
            if (playerRefOpt.isEmpty()) {
                player.sendMessage("§cError: Could not resolve player reference");
                return processedHtml;
            }
            
            PlayerRef playerRef = playerRefOpt.get();
            Store<EntityStore> store = storeProvider.getStore();
            
            // Build page with event handlers
            PageBuilder builder = PageBuilder.pageForPlayer(playerRef)
                .fromHtml(processedHtml);
            
            // Add buy item handlers
            if (purchaseHandler != null && data.buyItems() != null) {
                for (VendorItem item : data.buyItems()) {
                    builder.addEventListener("buy-" + item.itemId(), CustomUIEventBindingType.Activating, (ctx) -> {
                        purchaseHandler.accept(item.itemId(), 1);
                    });
                }
            }
            
            // Add sell item handlers
            if (sellHandler != null && data.sellItems() != null) {
                for (VendorItem item : data.sellItems()) {
                    builder.addEventListener("sell-" + item.itemId(), CustomUIEventBindingType.Activating, (ctx) -> {
                        sellHandler.accept(item.itemId(), 1);
                    });
                }
            }
            
            // Add close button handler (optional, page can be dismissed)
            builder.addEventListener("btn-close", CustomUIEventBindingType.Activating, (ctx) -> {
                // Page will auto-close on button click
            });
            
            // Open the page
            builder.open(store);
            return processedHtml;
            
        } catch (Exception e) {
            player.sendMessage("§cError displaying vendor page: " + e.getMessage());
            throw new RuntimeException("Failed to show vendor page", e);
        }
    }
    
    /**
     * Process HYUIML template with vendor data.
     * 
     * @param template Raw HYUIML template string
     * @param data Vendor data for variable substitution
     * @return Processed HTML ready for rendering
     */
    private String processTemplate(String template, VendorData data) {
        TemplateProcessor processor = new TemplateProcessor();
        
        Map<String, Object> npcData = new HashMap<>();
        npcData.put("name", data.vendorName());
        npcData.put("greeting", data.greeting());
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("npc", npcData);
        variables.put("buyItems", data.buyItems());
        variables.put("sellItems", data.sellItems());
        variables.put("playerCurrency", data.playerCurrency());
        
        processor.setVariable("vendor", variables);
        return processor.process(template);
    }
    
    /**
     * Vendor page display data.
     * 
     * @param vendorName vendor NPC name
     * @param greeting vendor greeting message
     * @param buyItems items available for purchase
     * @param sellItems player items available to sell
     * @param playerCurrency player's current currency amount
     */
    public record VendorData(
        String vendorName,
        String greeting,
        List<VendorItem> buyItems,
        List<VendorItem> sellItems,
        int playerCurrency
    ) {}
    
    /**
     * Individual vendor item.
     * 
     * @param itemId item registry ID
     * @param name item display name
     * @param description item description
     * @param icon item icon path
     * @param price item price
     * @param stock available stock (-1 for infinite)
     * @param quantity player's owned quantity (for sell tab)
     */
    public record VendorItem(
        String itemId,
        String name,
        String description,
        String icon,
        int price,
        int stock,
        int quantity
    ) {}
}
