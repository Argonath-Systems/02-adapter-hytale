package com.argonathsystems.adapter.hytale.ui;

import au.ellie.hyui.builders.HudBuilder;
import au.ellie.hyui.builders.HyUIHud;
import au.ellie.hyui.html.TemplateProcessor;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hytale.api.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapter for combat party/raid frames HUD.
 * 
 * <p>This adapter bridges the platform-agnostic combat frames data
 * with the Hytale platform's UI system.
 * 
 * <p><b>Architecture:</b>
 * <ul>
 *   <li>CombatFramesData → TemplateProcessor → HYUIML → Player UI</li>
 *   <li>Displays health/mana bars for party and raid members</li>
 *   <li>ONLY module in adapter layer</li>
 * </ul>
 * 
 * @author Argonath Systems
 * @version 1.0.0-SNAPSHOT
 * @since 1.0.0
 */
public class CombatFramesAdapter {
    
    private static final String COMBAT_FRAMES_TEMPLATE_PATH = "config/ui/huds/combat-frames.hyuiml";
    private static final long REFRESH_RATE_MS = 200; // 200ms refresh for combat frames
    
    private final TemplateLoader templateLoader;
    private final PlayerRefResolver playerRefResolver;
    private final Map<UUID, HyUIHud> activeHuds = new ConcurrentHashMap<>();
    
    /**
     * Create a new combat frames adapter.
     * 
     * @param templateLoader Template file loader
     * @param playerRefResolver Resolver to get PlayerRef from Player
     */
    public CombatFramesAdapter(TemplateLoader templateLoader, PlayerRefResolver playerRefResolver) {
        this.templateLoader = templateLoader;
        this.playerRefResolver = playerRefResolver;
    }
    
    /**
     * Show combat frames HUD for a player.
     * 
     * @param player Player to show combat frames to
     * @param data Combat frames data to display
     * @return Processed HTML ready for rendering
     */
    public String showCombatFrames(Player player, CombatFramesData data) {
        try {
            String template = templateLoader.loadTemplate(COMBAT_FRAMES_TEMPLATE_PATH);
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
                .show();
            
            activeHuds.put(playerId, hud);
            return processedHtml;
            
        } catch (Exception e) {
            player.sendMessage("§cError displaying combat frames: " + e.getMessage());
            throw new RuntimeException("Failed to show combat frames", e);
        }
    }
    
    /**
     * Hide combat frames HUD for a player.
     * 
     * @param player Player to hide combat frames from
     */
    public void hideCombatFrames(Player player) {
        HyUIHud hud = activeHuds.remove(player.getUUID());
        if (hud != null) {
            hud.remove();
        }
    }
    
    /**
     * Update combat frames data without recreating the HUD.
     * 
     * @param player Player to update
     * @param data New combat frames data
     */
    public void updateCombatFrames(Player player, CombatFramesData data) {
        HyUIHud hud = activeHuds.get(player.getUUID());
        if (hud != null) {
            String template = templateLoader.loadTemplate(COMBAT_FRAMES_TEMPLATE_PATH);
            String processedHtml = processTemplate(template, data);
            hud.updateHtml(processedHtml);
        } else {
            showCombatFrames(player, data);
        }
    }
    
    /**
     * Process HYUIML template with combat frames data.
     * 
     * @param template Raw HYUIML template string
     * @param data Combat frames data for variable substitution
     * @return Processed HTML ready for rendering
     */
    private String processTemplate(String template, CombatFramesData data) {
        TemplateProcessor processor = new TemplateProcessor();
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("mode", data.mode());
        variables.put("members", data.members());
        
        processor.setVariable("combat", variables);
        return processor.process(template);
    }
    
    /**
     * Combat frames display data.
     * 
     * @param mode display mode ("solo", "party", "raid")
     * @param members list of party/raid members
     */
    public record CombatFramesData(
        String mode,
        List<MemberFrame> members
    ) {}
    
    /**
     * Individual member frame data.
     * 
     * @param name member name
     * @param health current health percentage (0.0-1.0)
     * @param maxHealth maximum health value
     * @param mana current mana percentage (0.0-1.0)
     * @param maxMana maximum mana value
     * @param classType member class
     * @param level member level
     * @param isDead whether member is dead
     * @param isLeader whether member is party leader
     */
    public record MemberFrame(
        String name,
        float health,
        int maxHealth,
        float mana,
        int maxMana,
        String classType,
        int level,
        boolean isDead,
        boolean isLeader
    ) {}
}
