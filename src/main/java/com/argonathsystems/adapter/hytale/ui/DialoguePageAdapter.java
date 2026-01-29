package com.argonathsystems.adapter.hytale.ui;

import au.ellie.hyui.builders.HyUIPage;
import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.html.TemplateProcessor;
import com.argonathsystems.framework.ui.dialogue.DialogueChoice;
import com.argonathsystems.framework.ui.dialogue.QuestOfferData;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Adapter for rendering NPC dialogue pages.
 * 
 * <p>This adapter bridges the platform-agnostic dialogue data models
 * with the Hytale platform's UI system.
 * 
 * <p><b>Architecture:</b>
 * <ul>
 *   <li>DialogueData → TemplateProcessor → HYUIML → Player UI</li>
 *   <li>Handles template variable interpolation and event handling</li>
 *   <li>ONLY module in adapter layer</li>
 * </ul>
 * 
 * @author Argonath Systems
 * @version 1.0.0-SNAPSHOT
 * @since 1.0.0
 */
public class DialoguePageAdapter {
    
    private static final String DIALOGUE_TEMPLATE_PATH = "config/ui/huds/npc-dialogue.hyuiml";
    
    private final TemplateLoader templateLoader;
    private final PlayerRefResolver playerRefResolver;
    private final StoreProvider storeProvider;
    private Consumer<DialogueChoice> choiceHandler;
    
    /**
     * Create a new dialogue page adapter.
     * 
     * @param templateLoader Template file loader
     * @param playerRefResolver Resolver to get PlayerRef from Player
     * @param storeProvider Provider for entity store
     */
    public DialoguePageAdapter(TemplateLoader templateLoader, PlayerRefResolver playerRefResolver, StoreProvider storeProvider) {
        this.templateLoader = templateLoader;
        this.playerRefResolver = playerRefResolver;
        this.storeProvider = storeProvider;
    }
    
    /**
     * Set the handler for dialogue choice selections.
     * 
     * @param handler Handler invoked when player selects a choice
     * @return this adapter
     */
    public DialoguePageAdapter setChoiceHandler(Consumer<DialogueChoice> handler) {
        this.choiceHandler = handler;
        return this;
    }
    
    /**
     * Show a dialogue page to a player.
     * 
     * @param player Player to show dialogue to
     * @param data Dialogue data to display
     * @return Processed HTML ready for rendering
     */
    public String showDialogue(Player player, DialogueData data) {
        try {
            String template = templateLoader.loadTemplate(DIALOGUE_TEMPLATE_PATH);
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
            
            // Add choice click handlers
            if (choiceHandler != null && data.getChoices() != null) {
                for (int i = 0; i < data.getChoices().size(); i++) {
                    final int choiceIndex = i;
                    builder.addEventListener("choice-" + i, CustomUIEventBindingType.Activating, (ctx) -> {
                        if (choiceIndex < data.getChoices().size()) {
                            choiceHandler.accept(data.getChoices().get(choiceIndex));
                        }
                    });
                }
            }
            
            // Add quest accept/decline handlers
            if (data.isShowQuestOffer()) {
                builder.addEventListener("accept-quest", CustomUIEventBindingType.Activating, (ctx) -> {
                    // Accept quest via the first choice (convention)
                    if (choiceHandler != null && !data.getChoices().isEmpty()) {
                        choiceHandler.accept(data.getChoices().get(0));
                    }
                });
                builder.addEventListener("decline-quest", CustomUIEventBindingType.Activating, (ctx) -> {
                    // Decline quest via closing the page
                    // The page will be closed automatically
                });
            }
            
            // Open the page
            builder.open(store);
            return processedHtml;
            
        } catch (Exception e) {
            player.sendMessage("§cError displaying dialogue: " + e.getMessage());
            throw new RuntimeException("Failed to show dialogue page", e);
        }
    }
    
    /**
     * Process HYUIML template with dialogue data.
     * 
     * @param template Raw HYUIML template string
     * @param data Dialogue data for variable substitution
     * @return Processed HTML ready for rendering
     */
    private String processTemplate(String template, DialogueData data) {
        TemplateProcessor processor = new TemplateProcessor();
        
        // NPC data
        Map<String, Object> npcData = new HashMap<>();
        npcData.put("name", data.getNpcName());
        npcData.put("avatarId", data.getNpcAvatarId());
        processor.setVariable("npc", npcData);
        
        // Current dialogue text
        processor.setVariable("currentText", data.getDialogueText());
        
        // Dialogue choices
        processor.setVariable("choices", data.getChoices());
        
        // Quest offer mode
        processor.setVariable("showQuestOffer", data.isShowQuestOffer());
        if (data.getQuestOffer() != null) {
            processor.setVariable("quest", data.getQuestOffer());
        }
        
        return processor.process(template);
    }
    
    /**
     * Data model for dialogue page rendering.
     */
    public static class DialogueData {
        private final String npcName;
        private final String npcAvatarId;
        private final String dialogueText;
        private final List<DialogueChoice> choices;
        private final boolean showQuestOffer;
        private final QuestOfferData questOffer;
        
        public DialogueData(String npcName, String npcAvatarId, String dialogueText,
                           List<DialogueChoice> choices, boolean showQuestOffer,
                           QuestOfferData questOffer) {
            this.npcName = npcName;
            this.npcAvatarId = npcAvatarId;
            this.dialogueText = dialogueText;
            this.choices = choices;
            this.showQuestOffer = showQuestOffer;
            this.questOffer = questOffer;
        }
        
        public String getNpcName() { return npcName; }
        public String getNpcAvatarId() { return npcAvatarId; }
        public String getDialogueText() { return dialogueText; }
        public List<DialogueChoice> getChoices() { return choices; }
        public boolean isShowQuestOffer() { return showQuestOffer; }
        public QuestOfferData getQuestOffer() { return questOffer; }
    }
}
