package com.argonathsystems.adapter.hytale.ui;

import au.ellie.hyui.builders.HudBuilder;
import au.ellie.hyui.html.TemplateProcessor;
import com.argonathsystems.framework.ui.hud.mount.MountRadialDataProvider;
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
 * Adapter for rendering Mount Summon Radial Menu using HyUI.
 * 
 * <p>This adapter bridges the mod layer's {@link MountSummonRadialBuilder}
 * with HyUI's modal rendering capabilities. The radial menu provides:
 * <ul>
 *   <li>Quick access to favorite mounts</li>
 *   <li>Sector-based selection (8 slots)</li>
 *   <li>Visual feedback for selection</li>
 *   <li>Keybind display and hover tooltips</li>
 * </ul>
 * 
 * <h2>UI Flow</h2>
 * <ol>
 *   <li>Player presses radial menu key (default: G)</li>
 *   <li>Menu opens with current favorites</li>
 *   <li>Player moves cursor to select mount sector</li>
 *   <li>Player releases key or clicks to summon</li>
 * </ol>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * MountSummonRadialBuilder builder = new MountSummonRadialBuilder()
 *     .addFavorite(0, favoriteMount1)
 *     .addFavorite(1, favoriteMount2)
 *     .setSelectedSector(0);
 * 
 * MountSummonRadialAdapter adapter = new MountSummonRadialAdapter(store);
 * adapter.showRadialMenu(playerRef, builder);
 * }</pre>
 * 
 * @author Argonath Systems Team
 * @version 1.0.0
 * @since 1.0.0
 * @see MountSummonRadialBuilder
 * @see <a href="VDD-MISC-012-mount-summon-radial.md">VDD-MISC-012: Mount Summon Radial</a>
 */
public class MountSummonRadialAdapter {
    
    private static final Logger LOGGER = Logger.getLogger(MountSummonRadialAdapter.class.getName());
    private static final String TEMPLATE_PATH = "ui/modal/mount-summon-radial.hyuiml";
    
    private final Store<EntityStore> store;
    private final TemplateLoader templateLoader;
    private final Set<PlayerRef> activePlayers;
    private String cachedTemplate;
    
    /**
     * Creates a new MountSummonRadialAdapter.
     * 
     * @param store the Hytale entity store for UI rendering
     */
    public MountSummonRadialAdapter(Store<EntityStore> store) {
        this(store, new TemplateLoader());
    }
    
    /**
     * Creates a new MountSummonRadialAdapter with a custom template loader.
     * 
     * @param store the Hytale entity store
     * @param templateLoader the template loader for loading HYUIML
     */
    public MountSummonRadialAdapter(Store<EntityStore> store, TemplateLoader templateLoader) {
        this.store = store;
        this.templateLoader = templateLoader;
        this.activePlayers = ConcurrentHashMap.newKeySet();
    }
    
    /**
     * Shows the radial menu to a player.
     * 
     * <p>The radial menu displays up to 8 favorite mounts in a circular layout.
     * Each sector shows the mount's icon and name, with visual feedback
     * for the currently selected sector.
     * 
     * @param player the target player
     * @param dataProvider the radial menu data provider (typically MountSummonRadialBuilder)
     * @return true if the menu was shown successfully
     */
    public boolean showRadialMenu(PlayerRef player, MountRadialDataProvider dataProvider) {
        try {
            String template = loadTemplate(dataProvider);
            String processedHtml = processTemplate(template, dataProvider.buildTemplateVariables());
            
            HudBuilder.hudForPlayer(player)
                .fromHtml(processedHtml)
                .show(store);
            
            activePlayers.add(player);
            LOGGER.fine("Mount radial menu shown for player: " + player.getUsername());
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to show radial menu for " + player.getUsername(), e);
            return false;
        }
    }
    
    /**
     * Updates the radial menu selection.
     * 
     * <p>Call this when the player moves their cursor to a different sector.
     * 
     * @param player the target player
     * @param dataProvider updated provider with new selected sector
     * @return true if the update was successful
     */
    public boolean updateSelection(PlayerRef player, MountRadialDataProvider dataProvider) {
        return showRadialMenu(player, dataProvider);
    }
    
    /**
     * Hides the radial menu.
     * 
     * <p>Called when:
     * <ul>
     *   <li>Player releases the radial key</li>
     *   <li>Player selects a mount</li>
     *   <li>Player presses escape</li>
     * </ul>
     * 
     * @param player the target player
     */
    public void hideRadialMenu(PlayerRef player) {
        activePlayers.remove(player);
        // HyUI modal system manages actual removal
        LOGGER.fine("Mount radial menu hidden for player: " + player.getUsername());
    }
    
    /**
     * Checks if a player has the radial menu open.
     * 
     * @param player the player to check
     * @return true if the player has the radial menu open
     */
    public boolean isRadialMenuOpen(PlayerRef player) {
        return activePlayers.contains(player);
    }
    
    /**
     * Gets the selected sector based on cursor angle.
     * 
     * @param cursorX cursor X position relative to center
     * @param cursorY cursor Y position relative to center
     * @return the sector index (0-7), or -1 if in center dead zone
     */
    public int calculateSelectedSector(double cursorX, double cursorY) {
        double distance = Math.sqrt(cursorX * cursorX + cursorY * cursorY);
        
        // Center dead zone (30px radius)
        if (distance < 30) {
            return -1;
        }
        
        // Calculate angle (0 = right, counter-clockwise)
        double angle = Math.atan2(-cursorY, cursorX);
        if (angle < 0) {
            angle += 2 * Math.PI;
        }
        
        // Convert to sector (8 sectors, 45 degrees each)
        // Offset by 22.5 degrees so sector 0 is at top
        double offsetAngle = angle + Math.PI / 8;
        int sector = (int) (offsetAngle / (Math.PI / 4)) % 8;
        
        return sector;
    }
    
    /**
     * Loads the radial menu template.
     */
    private String loadTemplate(MountRadialDataProvider dataProvider) {
        // Hot reload mode
        if (dataProvider.hasTemplateSupplier()) {
            return dataProvider.getTemplate();
        }
        
        // Cached mode
        if (cachedTemplate == null) {
            cachedTemplate = templateLoader.loadHudTemplate("mount-summon-radial")
                .orElseThrow(() -> new IllegalStateException(
                    "Mount radial template not found"));
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
