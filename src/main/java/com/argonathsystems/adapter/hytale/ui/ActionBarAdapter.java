package com.argonathsystems.adapter.hytale.ui;

import au.ellie.hyui.builders.HudBuilder;
import au.ellie.hyui.builders.HyUIHud;
import au.ellie.hyui.html.TemplateProcessor;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Adapter for action bar HUD element.
 * 
 * <p>This adapter bridges the platform-agnostic action bar data
 * with the Hytale platform's UI system.
 * 
 * <p><b>Architecture:</b>
 * <ul>
 *   <li>ActionBarData → TemplateProcessor → HYUIML → Player UI</li>
 *   <li>Provides hotbar with ability slots, consumables, and mount control</li>
 *   <li>ONLY module in adapter layer</li>
 * </ul>
 * 
 * @author Argonath Systems
 * @version 1.0.0-SNAPSHOT
 * @since 1.0.0
 */
public class ActionBarAdapter {
    
    private static final String ACTION_BAR_TEMPLATE_PATH = "config/ui/huds/action-bar.hyuiml";
    private static final long REFRESH_RATE_MS = 100; // 100ms refresh for responsive action bar
    
    private final TemplateLoader templateLoader;
    private final PlayerRefResolver playerRefResolver;
    private final Map<UUID, HyUIHud> activeHuds = new ConcurrentHashMap<>();
    private Consumer<String> slotActivationHandler;
    
    /**
     * Create a new action bar adapter.
     * 
     * @param templateLoader Template file loader
     * @param playerRefResolver Resolver to get PlayerRef from Player
     */
    public ActionBarAdapter(TemplateLoader templateLoader, PlayerRefResolver playerRefResolver) {
        this.templateLoader = templateLoader;
        this.playerRefResolver = playerRefResolver;
    }
    
    /**
     * Set the handler for slot activation events.
     * 
     * @param handler Handler invoked when player activates a slot
     * @return this adapter
     */
    public ActionBarAdapter setSlotActivationHandler(Consumer<String> handler) {
        this.slotActivationHandler = handler;
        return this;
    }
    
    /**
     * Show action bar HUD for a player.
     * 
     * @param player Player to show action bar to
     * @param data Action bar data to display
     * @return Processed HTML ready for rendering
     */
    public String showActionBar(Player player, ActionBarData data) {
        try {
            String template = templateLoader.loadTemplate(ACTION_BAR_TEMPLATE_PATH);
            String processedHtml = processTemplate(template, data);
            
            // Get PlayerRef for HyUI
            Optional<PlayerRef> playerRefOpt = playerRefResolver.resolve(player);
            if (playerRefOpt.isEmpty()) {
                player.sendMessage("§cError: Could not resolve player reference");
                return processedHtml;
            }
            
            PlayerRef playerRef = playerRefOpt.get();
            UUID playerId = player.getUUID();
            
            // Remove existing HUD if present
            HyUIHud existingHud = activeHuds.get(playerId);
            if (existingHud != null) {
                existingHud.remove();
            }
            
            // Build and show HUD using HyUI
            HyUIHud hud = HudBuilder.hudForPlayer(playerRef)
                .fromHtml(processedHtml)
                .withRefreshRate(REFRESH_RATE_MS)
                .addEventListener("slot1", CustomUIEventBindingType.Activating, (ctx) -> {
                    if (slotActivationHandler != null) slotActivationHandler.accept("slot1");
                })
                .addEventListener("slot2", CustomUIEventBindingType.Activating, (ctx) -> {
                    if (slotActivationHandler != null) slotActivationHandler.accept("slot2");
                })
                .addEventListener("slot3", CustomUIEventBindingType.Activating, (ctx) -> {
                    if (slotActivationHandler != null) slotActivationHandler.accept("slot3");
                })
                .addEventListener("slot4", CustomUIEventBindingType.Activating, (ctx) -> {
                    if (slotActivationHandler != null) slotActivationHandler.accept("slot4");
                })
                .addEventListener("mount", CustomUIEventBindingType.Activating, (ctx) -> {
                    if (slotActivationHandler != null) slotActivationHandler.accept("mount");
                })
                .show();
            
            activeHuds.put(playerId, hud);
            return processedHtml;
            
        } catch (Exception e) {
            player.sendMessage("§cError displaying action bar: " + e.getMessage());
            throw new RuntimeException("Failed to show action bar", e);
        }
    }
    
    /**
     * Hide action bar HUD for a player.
     * 
     * @param player Player to hide action bar from
     */
    public void hideActionBar(Player player) {
        HyUIHud hud = activeHuds.remove(player.getUUID());
        if (hud != null) {
            hud.remove();
        }
    }
    
    /**
     * Update action bar data without recreating the HUD.
     * 
     * @param player Player to update
     * @param data New action bar data
     */
    public void updateActionBar(Player player, ActionBarData data) {
        HyUIHud hud = activeHuds.get(player.getUUID());
        if (hud != null) {
            String template = templateLoader.loadTemplate(ACTION_BAR_TEMPLATE_PATH);
            String processedHtml = processTemplate(template, data);
            hud.updateHtml(processedHtml);
        } else {
            showActionBar(player, data);
        }
    }
    
    /**
     * Process HYUIML template with action bar data.
     * 
     * @param template Raw HYUIML template string
     * @param data Action bar data for variable substitution
     * @return Processed HTML ready for rendering
     */
    private String processTemplate(String template, ActionBarData data) {
        TemplateProcessor processor = new TemplateProcessor();
        
        // Convert data to map for HyUI processor
        Map<String, Object> variables = new HashMap<>();
        variables.put("slot1", data.slot1());
        variables.put("slot2", data.slot2());
        variables.put("slot3", data.slot3());
        variables.put("slot4", data.slot4());
        variables.put("mountIcon", data.mountIcon());
        variables.put("hasMountActive", data.hasMountActive());
        
        processor.setVariable("actionBar", variables);
        return processor.process(template);
    }
    
    /**
     * Action bar display data.
     * 
     * @param slot1 ability in slot 1
     * @param slot2 ability in slot 2
     * @param slot3 consumable in slot 3
     * @param slot4 consumable in slot 4
     * @param mountIcon mount icon path
     * @param hasMountActive whether mount is active
     */
    public record ActionBarData(
        SlotData slot1,
        SlotData slot2,
        SlotData slot3,
        SlotData slot4,
        String mountIcon,
        boolean hasMountActive
    ) {}
    
    /**
     * Individual slot data.
     * 
     * @param icon slot icon path
     * @param cooldown cooldown percentage (0.0-1.0)
     * @param charges remaining charges
     * @param keybind keyboard shortcut
     */
    public record SlotData(
        String icon,
        float cooldown,
        int charges,
        String keybind
    ) {}
}
