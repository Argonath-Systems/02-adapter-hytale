package com.argonathsystems.adapter.hytaleadapter.accessor;

import au.ellie.hyui.builders.HudBuilder;
import au.ellie.hyui.builders.HyUIHud;
import au.ellie.hyui.builders.HyUIPage;
import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.html.TemplateProcessor;
import com.argonathsystems.framework.accessorapi.UIAccessor;
import com.argonathsystems.framework.accessorapi.ui.HudLayoutData;
import com.argonathsystems.framework.accessorapi.ui.UIContext;
import com.argonathsystems.framework.accessorapi.ui.UIEventBinding;
import com.argonathsystems.framework.accessorapi.ui.UIUpdateData;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hytale implementation of UIAccessor using HyUI (v0.5.11+).
 * 
 * <p><b>MIGRATION-001 Status:</b> ✅ IMPLEMENTED</p>
 * 
 * <p>SDK Classes Used:</p>
 * <ul>
 *   <li>{@code au.ellie.hyui.builders.HudBuilder} - HyUI HUD builder API</li>
 *   <li>{@code au.ellie.hyui.builders.PageBuilder} - HyUI Page builder API</li>
 *   <li>{@code au.ellie.hyui.builders.HyUIHud} - HUD instance for updates/removal</li>
 *   <li>{@code au.ellie.hyui.builders.HyUIPage} - Page instance for updates/close</li>
 *   <li>{@code Universe.get().getPlayer(UUID)} - Player lookup</li>
 * </ul>
 * 
 * <h2>HyUI Features Used (v0.5.11)</h2>
 * <ul>
 *   <li>Multi-HUD system - automatic HUD stacking</li>
 *   <li>HYUIML template processing</li>
 *   <li>Event listeners with UIContext</li>
 *   <li>Runtime template updates</li>
 * </ul>
 * 
 * @author Argonath Systems Team
 * @version 3.0.0
 * @since MIGRATION-001
 * @see <a href="https://hyui.gitbook.io/docs/">HyUI Documentation</a>
 */
public class HytaleUIAccessor implements UIAccessor {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HytaleUIAccessor.class);
    
    /** Registered UI templates: uiId -> HYUIML content */
    private final Map<String, String> registeredUIs = new ConcurrentHashMap<>();
    
    /** Active HUD instances per player: "playerId-hudId" -> HyUIHud */
    private final Map<String, HyUIHud> activeHuds = new ConcurrentHashMap<>();
    
    /** Active Page instances per player: "playerId-pageId" -> HyUIPage */
    private final Map<String, HyUIPage> activePages = new ConcurrentHashMap<>();
    
    /** Track which UI is currently open per player for hasUIOpen checks */
    private final Map<UUID, String> openUIByPlayer = new ConcurrentHashMap<>();

    public HytaleUIAccessor(Object server) {
        LOGGER.info("HytaleUIAccessor initialized with HyUI v0.5.11+ integration");
    }

    // ========================================================================
    // Core UI Registration and Opening
    // ========================================================================

    @Override
    public void registerUI(String uiId, String uiDef) {
        if (uiId == null || uiId.isBlank()) {
            throw new IllegalArgumentException("uiId cannot be null or blank");
        }
        if (uiDef == null || uiDef.isBlank()) {
            throw new IllegalArgumentException("uiDef cannot be null or blank");
        }
        
        registeredUIs.put(uiId, uiDef);
        LOGGER.debug("Registered UI template: {}", uiId);
    }

    @Override
    public void openUI(UUID playerId, String uiId, UIContext context) {
        PlayerRef playerRef = getPlayerRef(playerId);
        if (playerRef == null) {
            LOGGER.warn("Cannot open UI {}: player {} not found", uiId, playerId);
            return;
        }
        
        String template = registeredUIs.get(uiId);
        if (template == null) {
            LOGGER.warn("Cannot open UI {}: not registered", uiId);
            return;
        }
        
        // Get the World from PlayerRef.getWorldUuid() - this is thread-safe and doesn't trigger
        // Store assertions. Then we schedule the entire store access + UI opening on the world thread.
        UUID worldUuid = playerRef.getWorldUuid();
        if (worldUuid == null) {
            LOGGER.warn("Cannot open UI {}: player {} has no world UUID", uiId, playerId);
            return;
        }
        
        World world = Universe.get().getWorld(worldUuid);
        if (world == null) {
            LOGGER.warn("Cannot open UI {}: world {} not found for player {}", uiId, worldUuid, playerId);
            return;
        }
        
        // Process template before scheduling (can be done on any thread)
        String processedHtml = processTemplateWithContext(template, context);
        
        // Schedule UI opening on world thread using World as Executor
        // CRITICAL: getPlayerStore() and PageBuilder.open() MUST run on the WorldThread
        // to avoid "Assert not in thread!" errors from Store.getComponent()
        CompletableFuture.runAsync(() -> {
            try {
                // Get store ON the world thread - this is where thread assertion is enforced
                Store<EntityStore> store = getPlayerStore(playerRef);
                if (store == null) {
                    LOGGER.warn("Cannot open UI {}: player {} has no store context (on world thread)", uiId, playerId);
                    return;
                }
                
                // Open as a Page (full-screen UI) - MUST run on WorldThread
                HyUIPage page = PageBuilder.pageForPlayer(playerRef)
                    .fromHtml(processedHtml)
                    .open(store);
                
                activePages.put(buildKey(playerId, uiId), page);
                openUIByPlayer.put(playerId, uiId);
                LOGGER.debug("Opened UI {} for player {}", uiId, playerId);
                
            } catch (Exception e) {
                LOGGER.error("Failed to open UI {} for player {}", uiId, playerId, e);
            }
        }, world);
    }
    
    @Override
    public void openUIWithEvents(UUID playerId, String uiId, UIContext context, List<UIEventBinding> eventBindings) {
        PlayerRef playerRef = getPlayerRef(playerId);
        if (playerRef == null) {
            LOGGER.warn("Cannot open UI {}: player {} not found", uiId, playerId);
            return;
        }
        
        String template = registeredUIs.get(uiId);
        if (template == null) {
            LOGGER.warn("Cannot open UI {}: not registered", uiId);
            return;
        }
        
        UUID worldUuid = playerRef.getWorldUuid();
        if (worldUuid == null) {
            LOGGER.warn("Cannot open UI {}: player {} has no world UUID", uiId, playerId);
            return;
        }
        
        World world = Universe.get().getWorld(worldUuid);
        if (world == null) {
            LOGGER.warn("Cannot open UI {}: world {} not found for player {}", uiId, worldUuid, playerId);
            return;
        }
        
        // Process template before scheduling (can be done on any thread)
        String processedHtml = processTemplateWithContext(template, context);
        
        // Schedule UI opening on world thread using World as Executor
        CompletableFuture.runAsync(() -> {
            try {
                Store<EntityStore> store = getPlayerStore(playerRef);
                if (store == null) {
                    LOGGER.warn("Cannot open UI {}: player {} has no store context", uiId, playerId);
                    return;
                }
                
                // Build the page with event listeners
                PageBuilder builder = PageBuilder.pageForPlayer(playerRef)
                    .fromHtml(processedHtml);
                
                // Register event bindings
                if (eventBindings != null) {
                    for (UIEventBinding binding : eventBindings) {
                        CustomUIEventBindingType hyuiEventType = mapEventType(binding.eventType());
                        
                        builder.addEventListener(binding.elementId(), hyuiEventType, (data, ctx) -> {
                            // Create event context wrapper
                            UIEventBinding.UIEventContext eventContext = new HyUIEventContext(ctx, playerId);
                            binding.handler().accept(playerId, eventContext);
                        });
                        LOGGER.trace("Registered event listener: {} -> {}", binding.elementId(), binding.eventType());
                    }
                }
                
                HyUIPage page = builder.open(store);
                
                activePages.put(buildKey(playerId, uiId), page);
                openUIByPlayer.put(playerId, uiId);
                LOGGER.debug("Opened UI {} with {} event bindings for player {}", 
                    uiId, eventBindings != null ? eventBindings.size() : 0, playerId);
                
            } catch (Exception e) {
                LOGGER.error("Failed to open UI {} for player {}", uiId, playerId, e);
            }
        }, world);
    }
    
    /**
     * Maps accessor UIEventType to HyUI CustomUIEventBindingType.
     */
    private CustomUIEventBindingType mapEventType(UIEventBinding.UIEventType eventType) {
        return switch (eventType) {
            case CLICK -> CustomUIEventBindingType.Activating;
            case VALUE_CHANGED -> CustomUIEventBindingType.ValueChanged;
            case FOCUS_GAINED -> CustomUIEventBindingType.FocusGained;
            case FOCUS_LOST -> CustomUIEventBindingType.FocusLost;
            case SELECTION_CHANGED -> CustomUIEventBindingType.SelectedTabChanged;
            case HOVER_ENTER -> CustomUIEventBindingType.MouseEntered;
            case HOVER_EXIT -> CustomUIEventBindingType.MouseExited;
        };
    }
    
    /**
     * Wrapper around HyUI's UIContext for the accessor API.
     */
    private class HyUIEventContext implements UIEventBinding.UIEventContext {
        private final au.ellie.hyui.events.UIContext hyuiContext;
        private final UUID playerId;
        
        HyUIEventContext(au.ellie.hyui.events.UIContext hyuiContext, UUID playerId) {
            this.hyuiContext = hyuiContext;
            this.playerId = playerId;
        }
        
        @Override
        public String getValue(String elementId) {
            return hyuiContext.getValue(elementId, String.class).orElse(null);
        }
        
        @Override
        public <T> T getValue(String elementId, Class<T> type) {
            return hyuiContext.getValue(elementId, type).orElse(null);
        }
        
        @Override
        public void closeUI() {
            hyuiContext.getPage().ifPresent(HyUIPage::close);
            openUIByPlayer.remove(playerId);
        }
        
        @Override
        public void updateElement(String elementId, UIUpdateData data) {
            hyuiContext.getPage().ifPresent(page -> applyUIUpdate(page, elementId, data));
        }
    }

    @Override
    public void closeUI(UUID playerId) {
        String currentUI = openUIByPlayer.remove(playerId);
        if (currentUI != null) {
            String key = buildKey(playerId, currentUI);
            HyUIPage page = activePages.remove(key);
            if (page != null) {
                page.close();
                LOGGER.debug("Closed UI {} for player {}", currentUI, playerId);
            }
        }
    }

    @Override
    public boolean hasUIOpen(UUID playerId, String uiId) {
        String currentUI = openUIByPlayer.get(playerId);
        return uiId.equals(currentUI);
    }

    @Override
    public void sendUIUpdate(UUID playerId, String elementId, UIUpdateData data) {
        // Find any active page for this player and update the element
        String currentUI = openUIByPlayer.get(playerId);
        if (currentUI != null) {
            String key = buildKey(playerId, currentUI);
            HyUIPage page = activePages.get(key);
            if (page != null) {
                applyUIUpdate(page, elementId, data);
            }
        }
    }

    // ========================================================================
    // HUD Methods (Persistent UI Elements)
    // ========================================================================

    @Override
    public void addHud(UUID playerId, String hudId, String content) {
        PlayerRef playerRef = getPlayerRef(playerId);
        if (playerRef == null) {
            LOGGER.warn("Cannot add HUD {}: player {} not found", hudId, playerId);
            return;
        }
        
        // HyUI .show(store) MUST be called on the world thread with a valid Store.
        // See: https://hyui.gitbook.io/docs - "HUD Building" section.
        UUID worldUuid = playerRef.getWorldUuid();
        if (worldUuid == null) {
            LOGGER.warn("Cannot add HUD {}: player {} has no world UUID", hudId, playerId);
            return;
        }
        
        World world = Universe.get().getWorld(worldUuid);
        if (world == null) {
            LOGGER.warn("Cannot add HUD {}: world {} not found for player {}", hudId, worldUuid, playerId);
            return;
        }
        
        // Schedule HUD creation on world thread (CRITICAL: .show(store) requires world thread)
        CompletableFuture.runAsync(() -> {
            try {
                Store<EntityStore> store = getPlayerStore(playerRef);
                if (store == null) {
                    LOGGER.warn("Cannot add HUD {}: player {} has no store context (on world thread)", hudId, playerId);
                    return;
                }
                
                HyUIHud hud = HudBuilder.hudForPlayer(playerRef)
                    .fromHtml(content)
                    .show(store);
                
                activeHuds.put(buildKey(playerId, hudId), hud);
                LOGGER.debug("Added HUD {} for player {}", hudId, playerId);
                
            } catch (Exception e) {
                LOGGER.error("Failed to add HUD {} for player {}", hudId, playerId, e);
            }
        }, world);
    }

    @Override
    public void removeHud(UUID playerId, String hudId) {
        String key = buildKey(playerId, hudId);
        HyUIHud hud = activeHuds.remove(key);
        if (hud != null) {
            hud.remove();
            LOGGER.debug("Removed HUD {} for player {}", hudId, playerId);
        }
    }

    @Override
    public void updateHud(UUID playerId, String hudId, String content) {
        String key = buildKey(playerId, hudId);
        HyUIHud existingHud = activeHuds.get(key);
        
        if (existingHud != null) {
            // Remove the old HUD and create a fresh one with the new content.
            // HyUI's updateExisting() via detachedHud() is unreliable for full content replacement.
            // Instead, we remove + re-add which guarantees the new HyUIML is rendered correctly.
            existingHud.remove();
            activeHuds.remove(key);
            addHud(playerId, hudId, content);
        } else {
            // HUD doesn't exist, create it
            addHud(playerId, hudId, content);
        }
    }

    @Override
    public void updateHudLayout(UUID playerId, HudLayoutData layoutData) {
        // Apply layout to each tracked HUD element
        layoutData.elements().forEach((hudId, pos) -> {
            String key = buildKey(playerId, hudId);
            HyUIHud hud = activeHuds.get(key);
            if (hud != null) {
                // HyUI doesn't have direct position setters on HUD
                // Would need to rebuild with updated anchor positions
                LOGGER.debug("Layout update for HUD {} - position ({}, {}), visible: {}", 
                    hudId, pos.x(), pos.y(), pos.visible());
            }
        });
    }

    @Override
    public void openHudEditor(UUID playerId) {
        String editorHtml = buildHudEditorHtml();
        addHud(playerId, "hud_editor", editorHtml);
    }

    @Override
    public void closeHudEditor(UUID playerId) {
        removeHud(playerId, "hud_editor");
    }

    @Override
    public boolean isInHudEditMode(UUID playerId) {
        return activeHuds.containsKey(buildKey(playerId, "hud_editor"));
    }

    // ========================================================================
    // Native HUD Control Methods
    // ========================================================================
    
    /** Track hidden native HUD components per player: "playerId-componentId" -> true */
    private final Map<String, Boolean> hiddenNativeHuds = new ConcurrentHashMap<>();
    
    /** Map of component IDs to HudComponent enum values */
    private static final Map<String, com.hypixel.hytale.protocol.packets.interface_.HudComponent> HUD_COMPONENT_MAP;
    
    static {
        HUD_COMPONENT_MAP = new java.util.HashMap<>();
        HUD_COMPONENT_MAP.put("Hotbar", com.hypixel.hytale.protocol.packets.interface_.HudComponent.Hotbar);
        HUD_COMPONENT_MAP.put("hotbar", com.hypixel.hytale.protocol.packets.interface_.HudComponent.Hotbar);
        HUD_COMPONENT_MAP.put("StatusIcons", com.hypixel.hytale.protocol.packets.interface_.HudComponent.StatusIcons);
        HUD_COMPONENT_MAP.put("Reticle", com.hypixel.hytale.protocol.packets.interface_.HudComponent.Reticle);
        HUD_COMPONENT_MAP.put("Chat", com.hypixel.hytale.protocol.packets.interface_.HudComponent.Chat);
        HUD_COMPONENT_MAP.put("Requests", com.hypixel.hytale.protocol.packets.interface_.HudComponent.Requests);
        HUD_COMPONENT_MAP.put("Notifications", com.hypixel.hytale.protocol.packets.interface_.HudComponent.Notifications);
        HUD_COMPONENT_MAP.put("KillFeed", com.hypixel.hytale.protocol.packets.interface_.HudComponent.KillFeed);
        HUD_COMPONENT_MAP.put("InputBindings", com.hypixel.hytale.protocol.packets.interface_.HudComponent.InputBindings);
        HUD_COMPONENT_MAP.put("PlayerList", com.hypixel.hytale.protocol.packets.interface_.HudComponent.PlayerList);
        HUD_COMPONENT_MAP.put("EventTitle", com.hypixel.hytale.protocol.packets.interface_.HudComponent.EventTitle);
        HUD_COMPONENT_MAP.put("Compass", com.hypixel.hytale.protocol.packets.interface_.HudComponent.Compass);
        HUD_COMPONENT_MAP.put("ObjectivePanel", com.hypixel.hytale.protocol.packets.interface_.HudComponent.ObjectivePanel);
        HUD_COMPONENT_MAP.put("PortalPanel", com.hypixel.hytale.protocol.packets.interface_.HudComponent.PortalPanel);
        HUD_COMPONENT_MAP.put("BuilderToolsLegend", com.hypixel.hytale.protocol.packets.interface_.HudComponent.BuilderToolsLegend);
        HUD_COMPONENT_MAP.put("Speedometer", com.hypixel.hytale.protocol.packets.interface_.HudComponent.Speedometer);
        HUD_COMPONENT_MAP.put("UtilitySlotSelector", com.hypixel.hytale.protocol.packets.interface_.HudComponent.UtilitySlotSelector);
        HUD_COMPONENT_MAP.put("BlockVariantSelector", com.hypixel.hytale.protocol.packets.interface_.HudComponent.BlockVariantSelector);
        HUD_COMPONENT_MAP.put("BuilderToolsMaterialSlotSelector", com.hypixel.hytale.protocol.packets.interface_.HudComponent.BuilderToolsMaterialSlotSelector);
        HUD_COMPONENT_MAP.put("Stamina", com.hypixel.hytale.protocol.packets.interface_.HudComponent.Stamina);
        HUD_COMPONENT_MAP.put("AmmoIndicator", com.hypixel.hytale.protocol.packets.interface_.HudComponent.AmmoIndicator);
        HUD_COMPONENT_MAP.put("Health", com.hypixel.hytale.protocol.packets.interface_.HudComponent.Health);
        HUD_COMPONENT_MAP.put("Mana", com.hypixel.hytale.protocol.packets.interface_.HudComponent.Mana);
        HUD_COMPONENT_MAP.put("Oxygen", com.hypixel.hytale.protocol.packets.interface_.HudComponent.Oxygen);
        HUD_COMPONENT_MAP.put("Sleep", com.hypixel.hytale.protocol.packets.interface_.HudComponent.Sleep);
    }

    @Override
    public void hideNativeHud(UUID playerId, String componentId) {
        if (playerId == null || componentId == null || componentId.isBlank()) {
            LOGGER.warn("hideNativeHud called with invalid parameters: player={}, component={}", playerId, componentId);
            return;
        }
        
        String key = buildKey(playerId, componentId);
        hiddenNativeHuds.put(key, true);
        
        PlayerRef playerRef = getPlayerRef(playerId);
        if (playerRef == null) {
            LOGGER.debug("Cannot hide native HUD {}: player {} not found (may have disconnected)", componentId, playerId);
            return;
        }
        
        // Check if player reference is still valid before proceeding
        var entityRef = playerRef.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            LOGGER.debug("Cannot hide native HUD {}: player {} entity reference invalid (may be disconnecting)", 
                componentId, playerId);
            return;
        }
        
        // Map componentId to HudComponent enum
        com.hypixel.hytale.protocol.packets.interface_.HudComponent hudComponent = HUD_COMPONENT_MAP.get(componentId);
        if (hudComponent == null) {
            LOGGER.warn("Unknown HUD component: {}. Available components: {}", componentId, HUD_COMPONENT_MAP.keySet());
            return;
        }
        
        // Get the World and schedule on world thread
        UUID worldUuid = playerRef.getWorldUuid();
        if (worldUuid == null) {
            LOGGER.debug("Cannot hide native HUD {}: player {} has no world UUID (may be disconnecting)", componentId, playerId);
            return;
        }
        
        World world = Universe.get().getWorld(worldUuid);
        if (world == null) {
            LOGGER.debug("Cannot hide native HUD {}: world {} not found", componentId, worldUuid);
            return;
        }
        
        // Schedule on world thread
        CompletableFuture.runAsync(() -> {
            try {
                // Re-check entity reference validity on world thread (may have changed)
                var entityRefLocal = playerRef.getReference();
                if (entityRefLocal == null || !entityRefLocal.isValid()) {
                    LOGGER.debug("Cannot hide native HUD: player entity reference became invalid");
                    return;
                }
                
                Store<EntityStore> store = entityRefLocal.getStore();
                if (store == null) {
                    LOGGER.debug("Cannot hide native HUD: player store not available");
                    return;
                }
                
                // Get Player entity to access HudManager using correct API: getComponent(ref, componentType)
                Player player = store.getComponent(entityRefLocal, Player.getComponentType());
                if (player == null) {
                    LOGGER.debug("Cannot hide native HUD: Player entity not found for {} (may be disconnecting)", playerId);
                    return;
                }
                
                com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager hudManager = player.getHudManager();
                hudManager.hideHudComponents(playerRef, hudComponent);
                LOGGER.debug("Hidden native HUD '{}' for player {}", componentId, playerId);
                
            } catch (IllegalStateException e) {
                // Expected during player disconnect - entity reference becomes invalid
                LOGGER.debug("Could not hide native HUD {} for player {} (entity likely disconnecting): {}", 
                    componentId, playerId, e.getMessage());
            } catch (Exception e) {
                LOGGER.error("Failed to hide native HUD {} for player {}", componentId, playerId, e);
            }
        }, world);
    }

    @Override
    public void showNativeHud(UUID playerId, String componentId) {
        if (playerId == null || componentId == null || componentId.isBlank()) {
            LOGGER.warn("showNativeHud called with invalid parameters: player={}, component={}", playerId, componentId);
            return;
        }
        
        String key = buildKey(playerId, componentId);
        hiddenNativeHuds.remove(key);
        
        PlayerRef playerRef = getPlayerRef(playerId);
        if (playerRef == null) {
            LOGGER.debug("Cannot show native HUD {}: player {} not found (may have disconnected)", componentId, playerId);
            return;
        }
        
        // Check if player reference is still valid before proceeding
        var entityRef = playerRef.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            LOGGER.debug("Cannot show native HUD {}: player {} entity reference invalid (may be disconnecting)", 
                componentId, playerId);
            return;
        }
        
        // Map componentId to HudComponent enum
        com.hypixel.hytale.protocol.packets.interface_.HudComponent hudComponent = HUD_COMPONENT_MAP.get(componentId);
        if (hudComponent == null) {
            LOGGER.warn("Unknown HUD component: {}. Available components: {}", componentId, HUD_COMPONENT_MAP.keySet());
            return;
        }
        
        // Get the World and schedule on world thread
        UUID worldUuid = playerRef.getWorldUuid();
        if (worldUuid == null) {
            LOGGER.debug("Cannot show native HUD {}: player {} has no world UUID (may be disconnecting)", componentId, playerId);
            return;
        }
        
        World world = Universe.get().getWorld(worldUuid);
        if (world == null) {
            LOGGER.debug("Cannot show native HUD {}: world {} not found", componentId, worldUuid);
            return;
        }
        
        // Schedule on world thread
        CompletableFuture.runAsync(() -> {
            try {
                // Re-check entity reference validity on world thread (may have changed)
                var entityRefLocal = playerRef.getReference();
                if (entityRefLocal == null || !entityRefLocal.isValid()) {
                    LOGGER.debug("Cannot show native HUD: player entity reference became invalid");
                    return;
                }
                
                Store<EntityStore> store = entityRefLocal.getStore();
                if (store == null) {
                    LOGGER.debug("Cannot show native HUD: player store not available");
                    return;
                }
                
                // Get Player entity to access HudManager using correct API: getComponent(ref, componentType)
                Player player = store.getComponent(entityRefLocal, Player.getComponentType());
                if (player == null) {
                    LOGGER.debug("Cannot show native HUD: Player entity not found for {} (may be disconnecting)", playerId);
                    return;
                }
                
                com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager hudManager = player.getHudManager();
                hudManager.showHudComponents(playerRef, hudComponent);
                LOGGER.debug("Restored native HUD '{}' for player {}", componentId, playerId);
                
            } catch (IllegalStateException e) {
                // Expected during player disconnect - entity reference becomes invalid
                LOGGER.debug("Could not show native HUD {} for player {} (entity likely disconnecting): {}", 
                    componentId, playerId, e.getMessage());
            } catch (Exception e) {
                LOGGER.error("Failed to show native HUD {} for player {}", componentId, playerId, e);
            }
        }, world);
    }

    @Override
    public boolean isNativeHudHidden(UUID playerId, String componentId) {
        if (playerId == null || componentId == null) {
            return false;
        }
        
        String key = buildKey(playerId, componentId);
        return hiddenNativeHuds.getOrDefault(key, false);
    }

    // ========================================================================
    // Modal Methods
    // ========================================================================
    
    @Override
    public void openModal(UUID playerId, String modalId, String content, UIContext context) {
        // Modals in HyUI are Pages with decorated-container styling
        PlayerRef playerRef = getPlayerRef(playerId);
        if (playerRef == null) {
            LOGGER.warn("Cannot open modal {}: player {} not found", modalId, playerId);
            return;
        }
        
        // Get the World from PlayerRef.getWorldUuid() - this is thread-safe and doesn't trigger
        // Store assertions. Then we schedule the entire store access + UI opening on the world thread.
        UUID worldUuid = playerRef.getWorldUuid();
        if (worldUuid == null) {
            LOGGER.warn("Cannot open modal {}: player {} has no world UUID", modalId, playerId);
            return;
        }
        
        World world = Universe.get().getWorld(worldUuid);
        if (world == null) {
            LOGGER.warn("Cannot open modal {}: world {} not found for player {}", modalId, worldUuid, playerId);
            return;
        }
        
        // Process template before scheduling (can be done on any thread)
        String processedHtml = wrapAsModal(content, context);
        
        // Schedule on world thread using World as Executor
        // CRITICAL: getPlayerStore() and PageBuilder.open() MUST run on the WorldThread
        CompletableFuture.runAsync(() -> {
            try {
                // Get store ON the world thread - this is where thread assertion is enforced
                Store<EntityStore> store = getPlayerStore(playerRef);
                if (store == null) {
                    LOGGER.warn("Cannot open modal {}: player {} has no store context (on world thread)", modalId, playerId);
                    return;
                }
                
                HyUIPage page = PageBuilder.pageForPlayer(playerRef)
                    .fromHtml(processedHtml)
                    .open(store);
                
                activePages.put(buildKey(playerId, modalId), page);
                LOGGER.debug("Opened modal {} for player {}", modalId, playerId);
                
            } catch (Exception e) {
                LOGGER.error("Failed to open modal {} for player {}", modalId, playerId, e);
            }
        }, world);
    }
    
    @Override
    public void closeModal(UUID playerId, String modalId) {
        String key = buildKey(playerId, modalId);
        HyUIPage page = activePages.remove(key);
        if (page != null) {
            page.close();
            LOGGER.debug("Closed modal {} for player {}", modalId, playerId);
        }
    }
    
    @Override
    public void updateModal(UUID playerId, String modalId, String elementId, UIUpdateData data) {
        String key = buildKey(playerId, modalId);
        HyUIPage page = activePages.get(key);
        if (page != null) {
            applyUIUpdate(page, elementId, data);
        }
    }
    
    @Override
    public boolean hasModalOpen(UUID playerId, String modalId) {
        return activePages.containsKey(buildKey(playerId, modalId));
    }
    
    // ========================================================================
    // Page Methods
    // ========================================================================
    
    @Override
    public void openPage(UUID playerId, String pageId, String content, UIContext context) {
        PlayerRef playerRef = getPlayerRef(playerId);
        if (playerRef == null) {
            LOGGER.warn("Cannot open page {}: player {} not found", pageId, playerId);
            return;
        }
        
        // Get the World from PlayerRef.getWorldUuid() - this is thread-safe and doesn't trigger
        // Store assertions. Then we schedule the entire store access + UI opening on the world thread.
        UUID worldUuid = playerRef.getWorldUuid();
        if (worldUuid == null) {
            LOGGER.warn("Cannot open page {}: player {} has no world UUID", pageId, playerId);
            return;
        }
        
        World world = Universe.get().getWorld(worldUuid);
        if (world == null) {
            LOGGER.warn("Cannot open page {}: world {} not found for player {}", pageId, worldUuid, playerId);
            return;
        }
        
        // Process template before scheduling (can be done on any thread)
        String processedHtml = wrapAsPage(content, context);
        
        // Schedule on world thread using World as Executor
        // CRITICAL: getPlayerStore() and PageBuilder.open() MUST run on the WorldThread
        CompletableFuture.runAsync(() -> {
            try {
                // Get store ON the world thread - this is where thread assertion is enforced
                Store<EntityStore> store = getPlayerStore(playerRef);
                if (store == null) {
                    LOGGER.warn("Cannot open page {}: player {} has no store context (on world thread)", pageId, playerId);
                    return;
                }
                
                HyUIPage page = PageBuilder.pageForPlayer(playerRef)
                    .fromHtml(processedHtml)
                    .open(store);
                
                activePages.put(buildKey(playerId, pageId), page);
                LOGGER.debug("Opened page {} for player {}", pageId, playerId);
                
            } catch (Exception e) {
                LOGGER.error("Failed to open page {} for player {}", pageId, playerId, e);
            }
        }, world);
    }
    
    @Override
    public void openPageWithEvents(UUID playerId, String pageId, String content, UIContext context, List<UIEventBinding> eventBindings) {
        PlayerRef playerRef = getPlayerRef(playerId);
        if (playerRef == null) {
            LOGGER.warn("Cannot open page {}: player {} not found", pageId, playerId);
            return;
        }
        
        UUID worldUuid = playerRef.getWorldUuid();
        if (worldUuid == null) {
            LOGGER.warn("Cannot open page {}: player {} has no world UUID", pageId, playerId);
            return;
        }
        
        World world = Universe.get().getWorld(worldUuid);
        if (world == null) {
            LOGGER.warn("Cannot open page {}: world {} not found for player {}", pageId, worldUuid, playerId);
            return;
        }
        
        // Process template before scheduling (can be done on any thread)
        String processedHtml = wrapAsPage(content, context);
        
        // Schedule on world thread using World as Executor
        CompletableFuture.runAsync(() -> {
            try {
                Store<EntityStore> store = getPlayerStore(playerRef);
                if (store == null) {
                    LOGGER.warn("Cannot open page {}: player {} has no store context (on world thread)", pageId, playerId);
                    return;
                }
                
                // Build page with inline content AND event bindings in a single builder call
                PageBuilder builder = PageBuilder.pageForPlayer(playerRef)
                    .fromHtml(processedHtml);
                
                // Attach event listeners to the same builder instance
                if (eventBindings != null) {
                    for (UIEventBinding binding : eventBindings) {
                        CustomUIEventBindingType hyuiEventType = mapEventType(binding.eventType());
                        
                        builder.addEventListener(binding.elementId(), hyuiEventType, (data, ctx) -> {
                            UIEventBinding.UIEventContext eventContext = new HyUIEventContext(ctx, playerId);
                            binding.handler().accept(playerId, eventContext);
                        });
                        LOGGER.trace("Registered page event listener: {} -> {}", binding.elementId(), binding.eventType());
                    }
                }
                
                HyUIPage page = builder.open(store);
                
                activePages.put(buildKey(playerId, pageId), page);
                LOGGER.debug("Opened page {} with {} event bindings for player {}", 
                    pageId, eventBindings != null ? eventBindings.size() : 0, playerId);
                
            } catch (Exception e) {
                LOGGER.error("Failed to open page {} for player {}", pageId, playerId, e);
            }
        }, world);
    }

    @Override
    public void closePage(UUID playerId, String pageId) {
        String key = buildKey(playerId, pageId);
        HyUIPage page = activePages.remove(key);
        if (page != null) {
            page.close();
            LOGGER.debug("Closed page {} for player {}", pageId, playerId);
        }
    }
    
    @Override
    public void updatePage(UUID playerId, String pageId, String elementId, UIUpdateData data) {
        String key = buildKey(playerId, pageId);
        HyUIPage page = activePages.get(key);
        if (page != null) {
            applyUIUpdate(page, elementId, data);
        }
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Get PlayerRef from Universe by UUID.
     */
    private PlayerRef getPlayerRef(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        return Universe.get().getPlayer(playerId);
    }
    
    /**
     * Get Store from PlayerRef's player reference.
     * This is the correct way to get the store for HyUI operations.
     * Uses the pattern from HyUI's own commands (e.g., HyUITestGuiCommand).
     */
    private Store<EntityStore> getPlayerStore(PlayerRef playerRef) {
        if (playerRef == null) {
            return null;
        }
        var ref = playerRef.getReference();
        if (ref != null && ref.isValid()) {
            return ref.getStore();
        }
        return null;
    }
    
    /**
     * Get the World instance for a player.
     * Required for scheduling tasks on the world thread via World.execute().
     */
    private World getPlayerWorld(PlayerRef playerRef) {
        UUID worldUuid = playerRef.getWorldUuid();
        if (worldUuid != null) {
            return Universe.get().getWorld(worldUuid);
        }
        // Fallback to default world
        return Universe.get().getDefaultWorld();
    }
    
    /**
     * Get Store (ComponentAccessor) from PlayerRef's world.
     * EntityStore.getStore() returns Store<EntityStore> which implements ComponentAccessor.
     */
    private Store<EntityStore> getEntityStoreAccessor(PlayerRef playerRef) {
        UUID worldUuid = playerRef.getWorldUuid();
        if (worldUuid != null) {
            var world = Universe.get().getWorld(worldUuid);
            if (world != null) {
                var entityStore = world.getEntityStore();
                if (entityStore != null) {
                    return entityStore.getStore();
                }
            }
        }
        // Fallback to default world
        var defaultWorld = Universe.get().getDefaultWorld();
        if (defaultWorld != null) {
            var entityStore = defaultWorld.getEntityStore();
            if (entityStore != null) {
                return entityStore.getStore();
            }
        }
        LOGGER.warn("Could not resolve EntityStore for player {} (worldUuid={}, defaultWorld={})",
            playerRef.getUuid(), worldUuid, defaultWorld);
        return null;
    }
    
    /**
     * Build cache key from player UUID and UI ID.
     */
    private String buildKey(UUID playerId, String uiId) {
        return playerId.toString() + "-" + uiId;
    }
    
    /**
     * Process template with UIContext variables using HyUI TemplateProcessor.
     * 
     * <p>Extracts data from UIContext implementations (typically records) via reflection
     * and populates a TemplateProcessor with variables for {{$variableName}} substitution.
     * 
     * <p>Supported context types:
     * <ul>
     *   <li>Java Records: All record components become template variables</li>
     *   <li>Map-based contexts: If context has a getData() method returning Map</li>
     *   <li>Other objects: Public getter methods (getXxx/isXxx) become variables</li>
     * </ul>
     * 
     * @param template The HyUIML template with {{$variable}} placeholders
     * @param context The UIContext containing variable values
     * @return Processed template with variables substituted
     */
    private String processTemplateWithContext(String template, UIContext context) {
        if (context == null) {
            return template;
        }
        
        TemplateProcessor processor = new TemplateProcessor();
        
        try {
            Class<?> contextClass = context.getClass();
            
            // Handle Java Records (preferred for UIContext implementations)
            if (contextClass.isRecord()) {
                for (java.lang.reflect.RecordComponent component : contextClass.getRecordComponents()) {
                    String name = component.getName();
                    Object value = component.getAccessor().invoke(context);
                    processor.setVariable(name, value != null ? value : "");
                    LOGGER.trace("Template variable set: {} = {}", name, value);
                }
            } else {
                // Handle regular classes via getter methods
                for (java.lang.reflect.Method method : contextClass.getMethods()) {
                    String methodName = method.getName();
                    if (method.getParameterCount() == 0 && !methodName.equals("getClass")) {
                        String varName = null;
                        
                        if (methodName.startsWith("get") && methodName.length() > 3) {
                            varName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
                        } else if (methodName.startsWith("is") && methodName.length() > 2 
                                   && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)) {
                            varName = Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
                        }
                        
                        if (varName != null) {
                            Object value = method.invoke(context);
                            processor.setVariable(varName, value != null ? value : "");
                            LOGGER.trace("Template variable set: {} = {}", varName, value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to extract variables from UIContext: {}", e.getMessage());
            // Return unprocessed template if extraction fails
            return template;
        }
        
        // Process the template using HyUI's TemplateProcessor
        return processor.process(template);
    }
    
    /**
     * Wrap content in standard modal structure.
     */
    private String wrapAsModal(String content, UIContext context) {
        // UIContext is a marker interface - title extraction needs proper implementation
        String title = "";
        
        String html = """
            <div class="page-overlay">
                <div class="decorated-container" data-hyui-title="%s">
                    <div class="container-contents">
                        %s
                    </div>
                </div>
            </div>
            """.formatted(title, content);
        
        return processTemplateWithContext(html, context);
    }
    
    /**
     * Wrap content in standard page structure.
     */
    private String wrapAsPage(String content, UIContext context) {
        // UIContext is a marker interface - title extraction needs proper implementation
        String title = "";
        
        String html = """
            <div class="page-overlay">
                <div class="container" data-hyui-title="%s">
                    <div class="container-contents">
                        %s
                    </div>
                </div>
            </div>
            """.formatted(title, content);
        
        return processTemplateWithContext(html, context);
    }
    
    /**
     * Apply UI update to a page element.
     */
    private void applyUIUpdate(HyUIPage page, String elementId, UIUpdateData data) {
        // HyUI element updates require accessing the element builder
        // This is a simplified implementation - full implementation needs
        // to track element builders or use page.getById()
        switch (data) {
            case UIUpdateData.Visibility v -> 
                LOGGER.debug("Update visibility for {}: visible={}, enabled={}", 
                    elementId, v.visible(), v.enabled());
            case UIUpdateData.Text t -> 
                LOGGER.debug("Update text for {}: {}", elementId, t.value());
            case UIUpdateData.Value v -> 
                LOGGER.debug("Update value for {}: {}", elementId, v.value());
            case UIUpdateData.Progress p -> 
                LOGGER.debug("Update progress for {}: {}/{}", elementId, p.current(), p.max());
            case UIUpdateData.ListData l -> 
                LOGGER.debug("Update list for {}: {} items", elementId, l.items().size());
            case UIUpdateData.MapData m -> 
                LOGGER.debug("Update map for {}: {} entries", elementId, m.data().size());
            case UIUpdateData.HtmlContent h -> 
                LOGGER.debug("Update HTML for {}: {} chars", elementId, h.html().length());
        }
    }
    
    /**
     * Build HUD editor HTML.
     */
    private String buildHudEditorHtml() {
        return """
            <div style="anchor-top: 0; anchor-bottom: 0; anchor-left: 0; anchor-right: 0;">
                <div style="anchor-top: 10; anchor-left: 10; anchor-right: 10; height: 40;">
                    <p style="color: white; font-size: 16;">HUD Layout Editor</p>
                    <p style="color: gray; font-size: 12;">Drag elements to reposition. Press F7 to save and exit.</p>
                </div>
            </div>
            """;
    }
    
    /**
     * Cleanup player data on disconnect.
     * Should be called from player quit event handler.
     */
    public void cleanupPlayer(UUID playerId) {
        openUIByPlayer.remove(playerId);
        
        // Remove all HUDs for this player
        activeHuds.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(playerId.toString())) {
                entry.getValue().remove();
                return true;
            }
            return false;
        });
        
        // Remove all pages for this player
        activePages.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(playerId.toString())) {
                entry.getValue().close();
                return true;
            }
            return false;
        });
        
        // Remove all hidden native HUD state for this player
        hiddenNativeHuds.entrySet().removeIf(entry -> 
            entry.getKey().startsWith(playerId.toString()));
    }
    
    /**
     * Get registered UI template by ID.
     */
    public Optional<String> getRegisteredUI(String uiId) {
        return Optional.ofNullable(registeredUIs.get(uiId));
    }
    
    // ========================================================================
    // Player Readiness Check
    // ========================================================================
    
    @Override
    public boolean isPlayerReadyForUI(UUID playerId) {
        PlayerRef playerRef = getPlayerRef(playerId);
        if (playerRef == null) {
            return false;
        }
        
        // Check if player has a world UUID - this indicates they've completed 
        // the world join process and are ready for UI operations
        UUID worldUuid = playerRef.getWorldUuid();
        return worldUuid != null;
    }
}
