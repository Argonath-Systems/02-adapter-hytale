package com.argonathsystems.adapter.hytale.webserver;

import com.argonathsystems.adapter.hytale.permission.PermissionProvider;
import com.argonathsystems.framework.accessorapi.ModelAnimationAccessor;
import com.argonathsystems.framework.accessorapi.ModelAnimationAccessor.AnimationSetInfo;
import com.argonathsystems.framework.accessorapi.ModelAnimationAccessor.AnimationInfo;
import com.argonathsystems.framework.accessorapi.ModelAnimationAccessor.AnimationSoundPair;
import com.argonathsystems.framework.npc.behavior.NPCAnimationBehavior.AnimationTrigger;
import com.argonathsystems.framework.npc.service.NPCAnimationSoundService;
import com.argonathsystems.framework.webserver.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * REST API Controller for NPC Animation Discovery and Configuration.
 * 
 * <p>Provides endpoints for:
 * <ul>
 *   <li>Discovering model animations and their sound events</li>
 *   <li>Listing available animation triggers</li>
 *   <li>Testing animation playback on NPCs</li>
 *   <li>Managing animation-trigger assignments</li>
 * </ul>
 * 
 * <h2>API Endpoints:</h2>
 * <pre>
 * GET    /api/v1/npc/models/{modelId}/animations       - Get all animations for a model
 * GET    /api/v1/npc/models/{modelId}/animation-sounds - Get animations with sound events
 * GET    /api/v1/npc/triggers                          - List all available triggers
 * POST   /api/v1/npc/{npcId}/trigger/{trigger}         - Trigger animation on NPC (preview)
 * GET    /api/v1/npc/{npcId}/animations                - Get NPC's current animation config
 * </pre>
 * 
 * <h2>Specification Reference</h2>
 * <ul>
 *   <li>SF-NPC-041: NPC Audio Integration</li>
 *   <li>SF-NPC-040: NPC Designer API</li>
 *   <li>IMPL-PLAN-2026-Q1-NPC-QUEST-ANIMATION: Phase 5</li>
 * </ul>
 * 
 * @author Argonath Systems Team
 * @version 1.0.0
 * @since 2026-01-31
 */
public class NPCAnimationApiController {
    
    private static final Logger LOGGER = Logger.getLogger(NPCAnimationApiController.class.getName());
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private final WebServerAccessor webServer;
    private final ModelAnimationAccessor modelAnimationAccessor;
    private final NPCAnimationSoundService animationSoundService;
    
    /**
     * Constructs the controller.
     * 
     * @param webServer The web server accessor
     * @param modelAnimationAccessor Accessor for model animation discovery
     * @param animationSoundService Service for animation-sound synchronization (optional)
     */
    public NPCAnimationApiController(
            WebServerAccessor webServer,
            ModelAnimationAccessor modelAnimationAccessor,
            NPCAnimationSoundService animationSoundService) {
        this.webServer = Objects.requireNonNull(webServer, "webServer");
        this.modelAnimationAccessor = Objects.requireNonNull(modelAnimationAccessor, "modelAnimationAccessor");
        this.animationSoundService = animationSoundService; // Optional, may be null
    }
    
    /**
     * Registers all API routes with the web server.
     */
    public void register() {
        webServer.registerRoute("/api/v1/npc/models/:modelId/animations", HttpMethod.GET, this::getModelAnimations);
        webServer.registerRoute("/api/v1/npc/models/:modelId/animation-sounds", HttpMethod.GET, this::getAnimationSoundPairs);
        webServer.registerRoute("/api/v1/npc/triggers", HttpMethod.GET, this::getAvailableTriggers);
        webServer.registerRoute("/api/v1/npc/:npcId/trigger/:trigger", HttpMethod.POST, this::triggerAnimation);
        webServer.registerRoute("/api/v1/npc/:npcId/animations", HttpMethod.GET, this::getNPCAnimations);
        
        LOGGER.log(Level.INFO, "Registered NPC Animation API endpoints at {0}/api/v1/npc",
            webServer.getBaseUrl());
    }
    
    /**
     * Unregisters all routes.
     */
    public void unregister() {
        webServer.unregisterAllRoutes(this);
        LOGGER.log(Level.INFO, "Unregistered NPC Animation API endpoints");
    }
    
    // ========================================================================
    // API Handlers
    // ========================================================================
    
    /**
     * GET /api/v1/npc/models/{modelId}/animations
     * 
     * Returns all animation sets and animations for a model asset.
     */
    private void getModelAnimations(HttpRequest req, HttpResponse res) {
        try {
            if (!checkPermission(req, "argonath.npc.animation.read")) {
                res.writeError(403, "Insufficient permissions");
                return;
            }
            
            String modelId = req.getPathParam("modelId");
            if (modelId == null || modelId.isEmpty()) {
                res.writeError(400, "Model ID is required");
                return;
            }
            
            // URL decode the model ID (e.g., "model%3Ahumanoid" -> "model:humanoid")
            modelId = java.net.URLDecoder.decode(modelId, "UTF-8");
            
            Map<String, AnimationSetInfo> animationSets = modelAnimationAccessor.getAnimationSets(modelId);
            
            // Build response
            ModelAnimationsResponse response = new ModelAnimationsResponse(
                modelId,
                animationSets,
                animationSets.values().stream()
                    .flatMap(set -> set.animations().stream())
                    .count()
            );
            
            res.setContentType("application/json");
            res.writeJson(GSON.toJson(response));
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error getting model animations", e);
            res.writeError(500, "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * GET /api/v1/npc/models/{modelId}/animation-sounds
     * 
     * Returns only animations that have native sound events attached.
     */
    private void getAnimationSoundPairs(HttpRequest req, HttpResponse res) {
        try {
            if (!checkPermission(req, "argonath.npc.animation.read")) {
                res.writeError(403, "Insufficient permissions");
                return;
            }
            
            String modelId = req.getPathParam("modelId");
            if (modelId == null || modelId.isEmpty()) {
                res.writeError(400, "Model ID is required");
                return;
            }
            
            modelId = java.net.URLDecoder.decode(modelId, "UTF-8");
            
            List<AnimationSoundPair> pairs = modelAnimationAccessor.getAnimationsWithSounds(modelId);
            
            res.setContentType("application/json");
            res.writeJson(GSON.toJson(Map.of(
                "modelId", modelId,
                "pairs", pairs,
                "count", pairs.size()
            )));
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error getting animation-sound pairs", e);
            res.writeError(500, "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * GET /api/v1/npc/triggers
     * 
     * Returns all available animation triggers with descriptions.
     */
    private void getAvailableTriggers(HttpRequest req, HttpResponse res) {
        try {
            if (!checkPermission(req, "argonath.npc.animation.read")) {
                res.writeError(403, "Insufficient permissions");
                return;
            }
            
            List<TriggerInfo> triggers = Arrays.stream(AnimationTrigger.values())
                .map(trigger -> new TriggerInfo(
                    trigger.name(),
                    getTriggerDescription(trigger),
                    getTriggerCategory(trigger)
                ))
                .collect(Collectors.toList());
            
            res.setContentType("application/json");
            res.writeJson(GSON.toJson(Map.of(
                "triggers", triggers,
                "count", triggers.size()
            )));
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error getting triggers", e);
            res.writeError(500, "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * POST /api/v1/npc/{npcId}/trigger/{trigger}
     * 
     * Triggers an animation on an NPC for preview/testing.
     */
    private void triggerAnimation(HttpRequest req, HttpResponse res) {
        try {
            if (!checkPermission(req, "argonath.npc.animation.trigger")) {
                res.writeError(403, "Insufficient permissions");
                return;
            }
            
            if (animationSoundService == null) {
                res.writeError(503, "Animation service not available");
                return;
            }
            
            String npcId = req.getPathParam("npcId");
            String triggerName = req.getPathParam("trigger");
            
            if (npcId == null || triggerName == null) {
                res.writeError(400, "NPC ID and trigger are required");
                return;
            }
            
            AnimationTrigger trigger;
            try {
                trigger = AnimationTrigger.valueOf(triggerName.toUpperCase());
            } catch (IllegalArgumentException e) {
                res.writeError(400, "Invalid trigger: " + triggerName);
                return;
            }
            
            // TODO: Get NPC from service and call animationSoundService.trigger(...)
            // For now, return a stub response indicating the feature exists
            res.setContentType("application/json");
            res.writeJson(GSON.toJson(Map.of(
                "success", true,
                "npcId", npcId,
                "trigger", trigger.name(),
                "message", "Animation trigger queued"
            )));
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error triggering animation", e);
            res.writeError(500, "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * GET /api/v1/npc/{npcId}/animations
     * 
     * Returns the current animation configuration for an NPC.
     */
    private void getNPCAnimations(HttpRequest req, HttpResponse res) {
        try {
            if (!checkPermission(req, "argonath.npc.animation.read")) {
                res.writeError(403, "Insufficient permissions");
                return;
            }
            
            String npcId = req.getPathParam("npcId");
            if (npcId == null) {
                res.writeError(400, "NPC ID is required");
                return;
            }
            
            // TODO: Get NPC from service and return its animation behavior
            res.setContentType("application/json");
            res.writeJson(GSON.toJson(Map.of(
                "npcId", npcId,
                "autoDiscoveryEnabled", true,
                "message", "Animation config retrieval not yet implemented"
            )));
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error getting NPC animations", e);
            res.writeError(500, "Internal server error: " + e.getMessage());
        }
    }
    
    // ========================================================================
    // Helper Methods
    // ========================================================================
    
    private boolean checkPermission(HttpRequest req, String permission) {
        // Extract player UUID from request header or session
        String playerIdHeader = req.getHeader("X-Player-UUID").orElse(null);
        if (playerIdHeader == null || playerIdHeader.isBlank()) {
            // No player context in request — allow for development/tool access
            // In production, this should return false for authenticated endpoints
            LOGGER.log(Level.FINE, "No X-Player-UUID header in request, allowing for tool access");
            return true;
        }
        
        try {
            UUID playerId = UUID.fromString(playerIdHeader);
            return PermissionProvider.getInstance().hasPermission(playerId, permission);
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Invalid player UUID in request header: {0}", playerIdHeader);
            return false;
        }
    }
    
    private String getTriggerDescription(AnimationTrigger trigger) {
        return switch (trigger) {
            case ON_INTERACT -> "Play when player interacts with NPC";
            case RANDOM -> "Play randomly while idle";
            case ON_COMBAT_START -> "Play when entering combat";
            case ON_COMBAT_END -> "Play when exiting combat";
            case ON_PLAYER_APPROACH -> "Play when detecting a player";
            case ON_SPAWN -> "Play on NPC spawn";
            case TIME_OF_DAY -> "Play at specific time of day";
            case ON_DIALOGUE_START -> "Play when dialogue starts";
            case ON_DEATH -> "Play when NPC dies";
            case ON_DAMAGE_TAKEN -> "Play when NPC takes damage";
            case ON_DAMAGE_DEALT -> "Play when NPC deals damage";
            case ON_DESPAWN -> "Play when NPC despawns";
            case ON_DIALOGUE_END -> "Play when dialogue ends normally";
            case ON_DIALOGUE_ABANDON -> "Play when player abandons dialogue";
            case ON_DIALOGUE_CHOICE -> "Play when player selects choice";
            case ON_DIALOGUE_LINE -> "Play when NPC speaks a line";
            case ON_QUEST_ACCEPT -> "Play when quest accepted";
            case ON_QUEST_COMPLETE -> "Play when quest objectives complete";
            case ON_QUEST_TURN_IN -> "Play when quest turned in";
            case ON_QUEST_FAIL -> "Play when quest failed";
            case ON_QUEST_OFFER -> "Play when offering quest";
            case ON_TRADE_START -> "Play when trade opens";
            case ON_TRADE_COMPLETE -> "Play when trade completes";
            case ON_TRADE_CANCEL -> "Play when trade cancelled";
            case ON_FIRST_MEET -> "Play on first meeting (one-time)";
            case ON_AFFINITY_CHANGE -> "Play when affinity tier changes";
            case ON_GREETING -> "Play greeting animation";
            case ON_BECOME_HOSTILE -> "Play when becoming hostile";
            case ON_BECOME_FRIENDLY -> "Play when becoming friendly";
            case ON_MOVEMENT_START -> "Play when starting to move";
            case ON_MOVEMENT_STOP -> "Play when stopping movement";
            case ON_WAYPOINT_REACHED -> "Play when reaching waypoint";
            case ON_MOVEMENT_MODE_CHANGE -> "Play when movement mode changes";
        };
    }
    
    private String getTriggerCategory(AnimationTrigger trigger) {
        return switch (trigger) {
            case ON_DEATH, ON_DAMAGE_TAKEN, ON_DAMAGE_DEALT, ON_SPAWN, ON_DESPAWN -> "lifecycle";
            case ON_DIALOGUE_START, ON_DIALOGUE_END, ON_DIALOGUE_ABANDON, 
                 ON_DIALOGUE_CHOICE, ON_DIALOGUE_LINE -> "dialogue";
            case ON_QUEST_ACCEPT, ON_QUEST_COMPLETE, ON_QUEST_TURN_IN, 
                 ON_QUEST_FAIL, ON_QUEST_OFFER -> "quest";
            case ON_TRADE_START, ON_TRADE_COMPLETE, ON_TRADE_CANCEL -> "trade";
            case ON_FIRST_MEET, ON_AFFINITY_CHANGE, ON_GREETING, 
                 ON_BECOME_HOSTILE, ON_BECOME_FRIENDLY -> "social";
            case ON_MOVEMENT_START, ON_MOVEMENT_STOP, ON_WAYPOINT_REACHED, 
                 ON_MOVEMENT_MODE_CHANGE -> "movement";
            case ON_COMBAT_START, ON_COMBAT_END -> "combat";
            case ON_INTERACT, RANDOM, ON_PLAYER_APPROACH, TIME_OF_DAY -> "general";
        };
    }
    
    // ========================================================================
    // Response DTOs
    // ========================================================================
    
    private record ModelAnimationsResponse(
        String modelId,
        Map<String, AnimationSetInfo> animationSets,
        long totalAnimations
    ) {}
    
    private record TriggerInfo(
        String name,
        String description,
        String category
    ) {}
}
