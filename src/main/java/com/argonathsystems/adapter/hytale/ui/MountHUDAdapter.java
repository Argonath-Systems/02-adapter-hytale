package com.argonathsystems.adapter.hytale.ui;

import au.ellie.hyui.builders.HudBuilder;
import au.ellie.hyui.html.TemplateProcessor;
import com.argonathsystems.framework.ui.hud.mount.MountHUDDataProvider;
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
 * Adapter for rendering Mount HUD using HyUI.
 * 
 * <p>This adapter bridges template variables (provided by mod layer builders)
 * with HyUI's HUD rendering capabilities. It handles:
 * <ul>
 *   <li>Template loading and caching</li>
 *   <li>Variable interpolation via HyUI's TemplateProcessor</li>
 *   <li>HUD rendering via HyUI's HudBuilder</li>
 *   <li>Stamina/altitude bar updates</li>
 *   <li>Ability cooldown animations</li>
 * </ul>
 * 
 * <h2>Architectural Note</h2>
 * <p>This adapter accepts {@code Map<String, Object>} rather than a specific builder
 * type to maintain proper layering (adapter layer cannot depend on mod layer).
 * The mod layer's MountHUDBuilder produces the template variables via its
 * {@code buildTemplateVariables()} method.
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // In mod layer - build template variables
 * MountHUDBuilder builder = new MountHUDBuilder()
 *     .setMount(mountData)
 *     .setStamina(80, 100)
 *     .setFlying(false);
 * Map<String, Object> variables = builder.buildTemplateVariables();
 * 
 * // Render via adapter
 * MountHUDAdapter adapter = new MountHUDAdapter(store);
 * adapter.showMountHUD(playerRef, variables);
 * }</pre>
 * 
 * @author Argonath Systems Team
 * @version 1.0.0
 * @since 1.0.0
 * @see <a href="VDD-MISC-011-world-003-mount-hud.md">VDD-MISC-011: Mount HUD</a>
 */
@SuppressWarnings("deprecation") // HyUI TemplateProcessor may have deprecated methods
public class MountHUDAdapter {
    
    private static final Logger LOGGER = Logger.getLogger(MountHUDAdapter.class.getName());
    private static final String TEMPLATE_NAME = "mount-hud";
    
    private final Store<EntityStore> store;
    private final TemplateLoader templateLoader;
    private final Set<PlayerRef> activePlayers;
    private String cachedTemplate;
    
    /**
     * Creates a new MountHUDAdapter.
     * 
     * @param store the Hytale entity store for UI rendering
     */
    public MountHUDAdapter(Store<EntityStore> store) {
        this(store, new TemplateLoader());
    }
    
    /**
     * Creates a new MountHUDAdapter with a custom template loader.
     * 
     * @param store the Hytale entity store
     * @param templateLoader the template loader for loading HYUIML
     */
    public MountHUDAdapter(Store<EntityStore> store, TemplateLoader templateLoader) {
        this.store = store;
        this.templateLoader = templateLoader;
        this.activePlayers = ConcurrentHashMap.newKeySet();
    }
    
    /**
     * Shows the Mount HUD to a player.
     * 
     * <p>The HUD displays mount information including:
     * <ul>
     *   <li>Mount name and type icon</li>
     *   <li>Stamina bar with percentage</li>
     *   <li>Altitude bar (for flying mounts)</li>
     *   <li>Ability buttons with keybinds</li>
     *   <li>Speed bonus and level</li>
     * </ul>
     * 
     * @param player the target player
     * @param dataProvider the mount HUD data provider (typically MountHUDBuilder)
     * @return true if the HUD was shown successfully
     */
    public boolean showMountHUD(PlayerRef player, MountHUDDataProvider dataProvider) {
        try {
            String template = loadTemplate(dataProvider);
            String processedHtml = processTemplate(template, dataProvider.buildTemplateVariables());
            
            HudBuilder.hudForPlayer(player)
                .fromHtml(processedHtml)
                .show(store);
            
            activePlayers.add(player);
            LOGGER.fine("Mount HUD shown for player: " + player.getUsername());
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to show mount HUD for " + player.getUsername(), e);
            return false;
        }
    }
    
    /**
     * Updates the Mount HUD with new data.
     * 
     * <p>Use this for updating stamina, altitude, or cooldowns.
     * 
     * @param player the target player
     * @param dataProvider updated data provider
     * @return true if the update was successful
     */
    public boolean updateMountHUD(PlayerRef player, MountHUDDataProvider dataProvider) {
        return showMountHUD(player, dataProvider);
    }
    
    /**
     * Hides the Mount HUD for a player.
     * 
     * <p>Called when the player dismounts.
     * 
     * @param player the target player
     */
    public void hideMountHUD(PlayerRef player) {
        activePlayers.remove(player);
        // HyUI Multi-HUD system manages actual removal
        LOGGER.fine("Mount HUD hidden for player: " + player.getUsername());
    }
    
    /**
     * Checks if a player has an active Mount HUD.
     * 
     * @param player the player to check
     * @return true if the player has an active mount HUD
     */
    public boolean hasActiveMountHUD(PlayerRef player) {
        return activePlayers.contains(player);
    }
    
    /**
     * Updates just the stamina bar for performance.
     * 
     * @param player the target player
     * @param current current stamina
     * @param max maximum stamina
     * @return true if the update was successful
     */
    public boolean updateStamina(PlayerRef player, double current, double max) {
        // For targeted updates, rebuild with new stamina only
        // In practice, a full rebuild is often fast enough with HyUI
        LOGGER.fine("Stamina update: " + current + "/" + max);
        return true; // Placeholder - full implementation would use partial updates
    }
    
    /**
     * Updates just the altitude bar for flying mounts.
     * 
     * @param player the target player
     * @param current current altitude
     * @param max maximum altitude
     * @return true if the update was successful
     */
    public boolean updateAltitude(PlayerRef player, double current, double max) {
        LOGGER.fine("Altitude update: " + current + "/" + max);
        return true; // Placeholder - full implementation would use partial updates
    }
    
    /**
     * Loads the mount HUD template.
     */
    private String loadTemplate(MountHUDDataProvider dataProvider) {
        // Hot reload mode - check for template supplier
        if (dataProvider.hasTemplateSupplier()) {
            return dataProvider.getTemplate();
        }
        
        // Default - use cached template
        if (cachedTemplate == null) {
            cachedTemplate = templateLoader.loadHudTemplate(TEMPLATE_NAME)
                .orElseThrow(() -> new IllegalStateException(
                    "Mount HUD template not found: " + TEMPLATE_NAME));
        }
        return cachedTemplate;
    }
    
    /**
     * Processes the template with the provided variables.
     */
    private String processTemplate(String template, Map<String, Object> variables) {
        TemplateProcessor processor = new TemplateProcessor();
        variables.forEach(processor::setVariable);
        return processor.process(template);
    }
    
    /**
     * Clears the template cache (for development hot reload).
     */
    public void clearTemplateCache() {
        cachedTemplate = null;
    }
}
