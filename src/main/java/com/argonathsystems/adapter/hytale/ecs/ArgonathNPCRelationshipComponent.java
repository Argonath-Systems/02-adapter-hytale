package com.argonathsystems.adapter.hytale.ecs;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.ListCodec;
import com.hypixel.hytale.codec.codecs.MapCodec;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Component;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Hytale ECS Component wrapper for player-NPC relationships.
 * 
 * <p>Stores all relationship data between a player and NPCs they've interacted with,
 * including reputation, dialogue progress, quest states, and trade history.
 * 
 * <h2>Data Structure</h2>
 * <p>Relationships are stored as a list of entries, each keyed by NPC template ID.
 * 
 * <h2>Specification Reference</h2>
 * <ul>
 *   <li>SF-ARCHITECTURE-028-ecs-persistence-bridge</li>
 *   <li>SF-ARCHITECTURE-006-npc-framework</li>
 * </ul>
 * 
 * @author Argonath Systems Team
 * @version 1.0.0
 * @since 4.0.0
 */
public class ArgonathNPCRelationshipComponent implements Component<EntityStore> {

    // ========== Serialized Fields ==========
    
    private UUID playerId;
    private List<NPCRelationshipEntry> relationships;
    private long lastUpdatedEpoch;

    // ========== Nested Relationship Entry ==========
    
    /**
     * Represents a single player-NPC relationship.
     */
    public static class NPCRelationshipEntry {
        public String npcTemplateId;
        public int reputationLevel;
        public String reputationTier;
        public int totalInteractions;
        public int questsCompleted;
        public int questsFailed;
        public int dialogueStage;
        public Map<String, Boolean> dialogueFlags;
        public Map<String, String> questStates;
        public Map<String, String> customFlags;
        public int itemsBought;
        public int itemsSold;
        public long totalCurrencySpent;
        public long firstInteractionEpoch;
        public long lastInteractionEpoch;
        public boolean isRomantic;
        public boolean isRival;
        public boolean isAlly;
        
        public NPCRelationshipEntry() {
            this.dialogueFlags = new HashMap<>();
            this.questStates = new HashMap<>();
            this.customFlags = new HashMap<>();
            this.reputationLevel = 0;
            this.reputationTier = "NEUTRAL";
        }
        
        /**
         * BuilderCodec for serialization.
         */
        public static final BuilderCodec<NPCRelationshipEntry> CODEC = 
            BuilderCodec.builder(NPCRelationshipEntry.class, NPCRelationshipEntry::new)
                .addField(new KeyedCodec<>("NpcTemplateId", Codec.STRING),
                    (e, v) -> e.npcTemplateId = v, e -> e.npcTemplateId)
                .addField(new KeyedCodec<>("ReputationLevel", Codec.INTEGER),
                    (e, v) -> e.reputationLevel = v, e -> e.reputationLevel)
                .addField(new KeyedCodec<>("ReputationTier", Codec.STRING),
                    (e, v) -> e.reputationTier = v, e -> e.reputationTier)
                .addField(new KeyedCodec<>("TotalInteractions", Codec.INTEGER),
                    (e, v) -> e.totalInteractions = v, e -> e.totalInteractions)
                .addField(new KeyedCodec<>("QuestsCompleted", Codec.INTEGER),
                    (e, v) -> e.questsCompleted = v, e -> e.questsCompleted)
                .addField(new KeyedCodec<>("QuestsFailed", Codec.INTEGER),
                    (e, v) -> e.questsFailed = v, e -> e.questsFailed)
                .addField(new KeyedCodec<>("DialogueStage", Codec.INTEGER),
                    (e, v) -> e.dialogueStage = v, e -> e.dialogueStage)
                .addField(new KeyedCodec<>("DialogueFlags", new MapCodec<>(Codec.BOOLEAN, HashMap::new, false)),
                    (e, v) -> e.dialogueFlags = v, e -> e.dialogueFlags)
                .addField(new KeyedCodec<>("QuestStates", new MapCodec<>(Codec.STRING, HashMap::new, false)),
                    (e, v) -> e.questStates = v, e -> e.questStates)
                .addField(new KeyedCodec<>("CustomFlags", new MapCodec<>(Codec.STRING, HashMap::new, false)),
                    (e, v) -> e.customFlags = v, e -> e.customFlags)
                .addField(new KeyedCodec<>("ItemsBought", Codec.INTEGER),
                    (e, v) -> e.itemsBought = v, e -> e.itemsBought)
                .addField(new KeyedCodec<>("ItemsSold", Codec.INTEGER),
                    (e, v) -> e.itemsSold = v, e -> e.itemsSold)
                .addField(new KeyedCodec<>("TotalCurrencySpent", Codec.LONG),
                    (e, v) -> e.totalCurrencySpent = v, e -> e.totalCurrencySpent)
                .addField(new KeyedCodec<>("FirstInteraction", Codec.LONG),
                    (e, v) -> e.firstInteractionEpoch = v, e -> e.firstInteractionEpoch)
                .addField(new KeyedCodec<>("LastInteraction", Codec.LONG),
                    (e, v) -> e.lastInteractionEpoch = v, e -> e.lastInteractionEpoch)
                .addField(new KeyedCodec<>("IsRomantic", Codec.BOOLEAN),
                    (e, v) -> e.isRomantic = v, e -> e.isRomantic)
                .addField(new KeyedCodec<>("IsRival", Codec.BOOLEAN),
                    (e, v) -> e.isRival = v, e -> e.isRival)
                .addField(new KeyedCodec<>("IsAlly", Codec.BOOLEAN),
                    (e, v) -> e.isAlly = v, e -> e.isAlly)
                .build();
        
        /**
         * Adjust reputation and update tier.
         */
        public void adjustReputation(int delta) {
            this.reputationLevel = Math.max(-1000, Math.min(1000, this.reputationLevel + delta));
            updateReputationTier();
        }
        
        private void updateReputationTier() {
            if (reputationLevel >= 850) {
                reputationTier = "EXALTED";
            } else if (reputationLevel >= 500) {
                reputationTier = "REVERED";
            } else if (reputationLevel >= 250) {
                reputationTier = "HONORED";
            } else if (reputationLevel >= 100) {
                reputationTier = "FRIENDLY";
            } else if (reputationLevel >= -100) {
                reputationTier = "NEUTRAL";
            } else if (reputationLevel >= -500) {
                reputationTier = "UNFRIENDLY";
            } else {
                reputationTier = "HOSTILE";
            }
        }
    }

    // ========== CODEC Definition ==========
    
    public static final BuilderCodec<ArgonathNPCRelationshipComponent> CODEC = 
        BuilderCodec.builder(ArgonathNPCRelationshipComponent.class, ArgonathNPCRelationshipComponent::new)
            .addField(new KeyedCodec<>("PlayerId", Codec.UUID_BINARY),
                (d, v) -> d.playerId = v, d -> d.playerId)
            .addField(new KeyedCodec<>("Relationships", new ListCodec<>(NPCRelationshipEntry.CODEC, ArrayList::new)),
                (d, v) -> d.relationships = v, d -> d.relationships)
            .addField(new KeyedCodec<>("LastUpdated", Codec.LONG),
                (d, v) -> d.lastUpdatedEpoch = v, d -> d.lastUpdatedEpoch)
            .build();

    // ========== Constructors ==========
    
    public ArgonathNPCRelationshipComponent() {
        this.relationships = new ArrayList<>();
        this.lastUpdatedEpoch = Instant.now().toEpochMilli();
    }
    
    public ArgonathNPCRelationshipComponent(ArgonathNPCRelationshipComponent clone) {
        this.playerId = clone.playerId;
        this.relationships = new ArrayList<>(clone.relationships);
        this.lastUpdatedEpoch = clone.lastUpdatedEpoch;
    }

    // ========== Component Interface ==========
    
    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new ArgonathNPCRelationshipComponent(this);
    }

    // ========== Relationship Access ==========
    
    /**
     * Get or create a relationship entry for an NPC.
     */
    public NPCRelationshipEntry getOrCreateRelationship(String npcTemplateId) {
        return relationships.stream()
            .filter(r -> r.npcTemplateId.equals(npcTemplateId))
            .findFirst()
            .orElseGet(() -> {
                NPCRelationshipEntry entry = new NPCRelationshipEntry();
                entry.npcTemplateId = npcTemplateId;
                entry.firstInteractionEpoch = Instant.now().toEpochMilli();
                relationships.add(entry);
                return entry;
            });
    }
    
    /**
     * Get reputation level with a specific NPC.
     */
    public int getReputation(String npcTemplateId) {
        return relationships.stream()
            .filter(r -> r.npcTemplateId.equals(npcTemplateId))
            .mapToInt(r -> r.reputationLevel)
            .findFirst()
            .orElse(0);
    }

    // ========== Getters and Setters ==========
    
    public UUID getPlayerId() { return playerId; }
    public void setPlayerId(UUID playerId) { this.playerId = playerId; }
    
    public List<NPCRelationshipEntry> getRelationships() { return relationships; }
    public void setRelationships(List<NPCRelationshipEntry> relationships) { 
        this.relationships = relationships != null ? new ArrayList<>(relationships) : new ArrayList<>(); 
    }
    
    public Instant getLastUpdated() { return Instant.ofEpochMilli(lastUpdatedEpoch); }
    public void setLastUpdated(Instant lastUpdated) { 
        this.lastUpdatedEpoch = lastUpdated != null ? lastUpdated.toEpochMilli() : Instant.now().toEpochMilli(); 
    }

    @Override
    public String toString() {
        return "ArgonathNPCRelationshipComponent{" +
            "playerId=" + playerId +
            ", relationshipCount=" + relationships.size() +
            '}';
    }
}
