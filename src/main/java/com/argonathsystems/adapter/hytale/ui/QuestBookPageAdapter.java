package com.argonathsystems.adapter.hytale.ui;

import au.ellie.hyui.builders.HyUIPage;
import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.html.TemplateProcessor;
import com.argonathsystems.framework.ui.quest.QuestCategory;
import com.argonathsystems.framework.ui.quest.QuestDetail;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hytale.api.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Adapter for rendering quest book pages using HyUI.
 * 
 * <p>This adapter bridges the platform-agnostic quest data models
 * with the Hytale platform's UI system.
 * 
 * <p><b>Architecture:</b>
 * <ul>
 *   <li>QuestBookData → TemplateProcessor → HYUIML → Player UI</li>
 *   <li>Handles template variable interpolation and event handling</li>
 *   <li>ONLY module in adapter layer</li>
 * </ul>
 * 
 * @author Argonath Systems
 * @version 1.0.0-SNAPSHOT
 * @since 1.0.0
 */
public class QuestBookPageAdapter {
    
    private static final String QUEST_BOOK_TEMPLATE_PATH = "config/ui/pages/quest-book.hyuiml";
    
    private final TemplateLoader templateLoader;
    private final PlayerRefResolver playerRefResolver;
    private final StoreProvider storeProvider;
    private Consumer<String> questSelectHandler;
    private Consumer<String> questTrackHandler;
    private Consumer<String> questAbandonHandler;
    
    /**
     * Create a new quest book page adapter.
     * 
     * @param templateLoader Template file loader
     * @param playerRefResolver Resolver to get PlayerRef from Player
     * @param storeProvider Provider for entity store
     */
    public QuestBookPageAdapter(TemplateLoader templateLoader, PlayerRefResolver playerRefResolver, StoreProvider storeProvider) {
        this.templateLoader = templateLoader;
        this.playerRefResolver = playerRefResolver;
        this.storeProvider = storeProvider;
    }
    
    /**
     * Set the handler for quest selection events.
     * 
     * @param handler Handler invoked when player selects a quest
     * @return this adapter
     */
    public QuestBookPageAdapter setQuestSelectHandler(Consumer<String> handler) {
        this.questSelectHandler = handler;
        return this;
    }
    
    /**
     * Set the handler for quest tracking events.
     * 
     * @param handler Handler invoked when player tracks/untracks a quest
     * @return this adapter
     */
    public QuestBookPageAdapter setQuestTrackHandler(Consumer<String> handler) {
        this.questTrackHandler = handler;
        return this;
    }
    
    /**
     * Set the handler for quest abandon events.
     * 
     * @param handler Handler invoked when player abandons a quest
     * @return this adapter
     */
    public QuestBookPageAdapter setQuestAbandonHandler(Consumer<String> handler) {
        this.questAbandonHandler = handler;
        return this;
    }
    
    /**
     * Show the quest book page to a player.
     * 
     * @param player Player to show quest book to
     * @param data Quest book data to display
     * @return Processed HTML ready for rendering
     */
    public String showQuestBook(Player player, QuestBookData data) {
        try {
            String template = templateLoader.loadTemplate(QUEST_BOOK_TEMPLATE_PATH);
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
            
            // Add quest select handlers for each quest
            if (questSelectHandler != null && data.getCategories() != null) {
                for (QuestCategory category : data.getCategories()) {
                    for (var quest : category.getQuests()) {
                        builder.addEventListener("quest-" + quest.getId(), CustomUIEventBindingType.Activating, (ctx) -> {
                            questSelectHandler.accept(quest.getId());
                        });
                    }
                }
            }
            
            // Add track/untrack handler
            if (questTrackHandler != null) {
                builder.addEventListener("btn-track", CustomUIEventBindingType.Activating, (ctx) -> {
                    if (data.getSelectedQuest() != null) {
                        questTrackHandler.accept(data.getSelectedQuest().getId());
                    }
                });
            }
            
            // Add abandon handler
            if (questAbandonHandler != null) {
                builder.addEventListener("btn-abandon", CustomUIEventBindingType.Activating, (ctx) -> {
                    if (data.getSelectedQuest() != null) {
                        questAbandonHandler.accept(data.getSelectedQuest().getId());
                    }
                });
            }
            
            // Open the page
            builder.open(store);
            return processedHtml;
            
        } catch (Exception e) {
            player.sendMessage("§cError displaying quest book: " + e.getMessage());
            throw new RuntimeException("Failed to show quest book", e);
        }
    }
    
    /**
     * Process HYUIML template with quest book data.
     * 
     * @param template Raw HYUIML template string
     * @param data Quest book data for variable substitution
     * @return Processed HTML ready for rendering
     */
    private String processTemplate(String template, QuestBookData data) {
        TemplateProcessor processor = new TemplateProcessor();
        
        processor.setVariable("activeTab", data.getActiveTab());
        processor.setVariable("categories", data.getCategories());
        processor.setVariable("selectedQuest", data.getSelectedQuest());
        
        return processor.process(template);
    }
    
    /**
     * Data model for quest book page rendering.
     */
    public static class QuestBookData {
        private final String activeTab;
        private final List<QuestCategory> categories;
        private final QuestDetail selectedQuest;
        
        public QuestBookData(String activeTab, List<QuestCategory> categories, QuestDetail selectedQuest) {
            this.activeTab = activeTab;
            this.categories = categories;
            this.selectedQuest = selectedQuest;
        }
        
        public String getActiveTab() { return activeTab; }
        public List<QuestCategory> getCategories() { return categories; }
        public QuestDetail getSelectedQuest() { return selectedQuest; }
        
        public QuestBookData withActiveTab(String tab) {
            return new QuestBookData(tab, categories, selectedQuest);
        }
        
        public QuestBookData withSelectedQuest(QuestDetail quest) {
            return new QuestBookData(activeTab, categories, quest);
        }
    }
}
