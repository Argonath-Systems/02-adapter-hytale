package com.argonathsystems.adapter.hytale.ecs;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.ListCodec;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Component;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Hytale ECS Component wrapper for player's mount collection.
 * 
 * <p>This component stores the player's tamed mounts, favorites, and active mounts
 * for automatic persistence through Hytale's EntityStore system.
 * 
 * <h2>Data Structure</h2>
 * <ul>
 *   <li>List of owned mount UUIDs (detailed mount data stored separately)</li>
 *   <li>Primary and secondary mount IDs for quick summon</li>
 *   <li>Favorite mount IDs list</li>
 * </ul>
 * 
 * <h2>Specification</h2>
 * <p>SF-ARCHITECTURE-028-ecs-persistence-bridge, Section 4.1
 * 
 * @author Argonath Systems Team
 * @version 1.0.0
 * @since 4.0.0
 */
public class ArgonathMountCollectionComponent implements Component<EntityStore> {

    // ========== Serialized Fields ==========
    
    private UUID ownerId;
    private List<MountDataEntry> mounts;
    private String primaryMountId;
    private String secondaryMountId;
    private List<String> favoriteIds;
    private long lastUpdatedEpoch;

    // ========== Nested Mount Data ==========
    
    /**
     * Serializable mount entry for the collection.
     */
    public static class MountDataEntry {
        public String mountId;
        public String creatureType;
        public String customName;
        public int level;
        public long experience;
        public List<String> unlockedAbilities;
        public List<String> equippedAbilities;
        public long tamedAtEpoch;
        public long totalDistance;
        public long totalTimeRidden;
        
        public MountDataEntry() {
            this.unlockedAbilities = new ArrayList<>();
            this.equippedAbilities = new ArrayList<>();
        }
        
        /**
         * BuilderCodec for MountDataEntry serialization.
         */
        public static final BuilderCodec<MountDataEntry> CODEC = 
            BuilderCodec.builder(MountDataEntry.class, MountDataEntry::new)
                .addField(new KeyedCodec<>("MountId", Codec.STRING),
                    (e, v) -> e.mountId = v, e -> e.mountId)
                .addField(new KeyedCodec<>("CreatureType", Codec.STRING),
                    (e, v) -> e.creatureType = v, e -> e.creatureType)
                .addField(new KeyedCodec<>("CustomName", Codec.STRING),
                    (e, v) -> e.customName = v, e -> e.customName)
                .addField(new KeyedCodec<>("Level", Codec.INTEGER),
                    (e, v) -> e.level = v, e -> e.level)
                .addField(new KeyedCodec<>("Experience", Codec.LONG),
                    (e, v) -> e.experience = v, e -> e.experience)
                .addField(new KeyedCodec<>("UnlockedAbilities", new ListCodec<>(Codec.STRING, ArrayList::new)),
                    (e, v) -> e.unlockedAbilities = v, e -> e.unlockedAbilities)
                .addField(new KeyedCodec<>("EquippedAbilities", new ListCodec<>(Codec.STRING, ArrayList::new)),
                    (e, v) -> e.equippedAbilities = v, e -> e.equippedAbilities)
                .addField(new KeyedCodec<>("TamedAt", Codec.LONG),
                    (e, v) -> e.tamedAtEpoch = v, e -> e.tamedAtEpoch)
                .addField(new KeyedCodec<>("TotalDistance", Codec.LONG),
                    (e, v) -> e.totalDistance = v, e -> e.totalDistance)
                .addField(new KeyedCodec<>("TotalTimeRidden", Codec.LONG),
                    (e, v) -> e.totalTimeRidden = v, e -> e.totalTimeRidden)
                .build();
    }

    // ========== CODEC Definition ==========
    
    /**
     * BuilderCodec for BSON serialization to Hytale's EntityStore.
     */
    public static final BuilderCodec<ArgonathMountCollectionComponent> CODEC = 
        BuilderCodec.builder(ArgonathMountCollectionComponent.class, ArgonathMountCollectionComponent::new)
            .addField(new KeyedCodec<>("OwnerId", Codec.UUID_BINARY),
                (data, v) -> data.ownerId = v, 
                data -> data.ownerId)
            .addField(new KeyedCodec<>("Mounts", new ListCodec<>(MountDataEntry.CODEC, ArrayList::new)),
                (data, v) -> data.mounts = v, 
                data -> data.mounts)
            .addField(new KeyedCodec<>("PrimaryMountId", Codec.STRING),
                (data, v) -> data.primaryMountId = v, 
                data -> data.primaryMountId)
            .addField(new KeyedCodec<>("SecondaryMountId", Codec.STRING),
                (data, v) -> data.secondaryMountId = v, 
                data -> data.secondaryMountId)
            .addField(new KeyedCodec<>("FavoriteIds", new ListCodec<>(Codec.STRING, ArrayList::new)),
                (data, v) -> data.favoriteIds = v, 
                data -> data.favoriteIds)
            .addField(new KeyedCodec<>("LastUpdated", Codec.LONG),
                (data, v) -> data.lastUpdatedEpoch = v, 
                data -> data.lastUpdatedEpoch)
            .build();

    // ========== Constructors ==========
    
    /**
     * Default constructor for deserialization.
     */
    public ArgonathMountCollectionComponent() {
        this.mounts = new ArrayList<>();
        this.favoriteIds = new ArrayList<>();
        this.lastUpdatedEpoch = Instant.now().toEpochMilli();
    }
    
    /**
     * Copy constructor for cloning.
     */
    public ArgonathMountCollectionComponent(ArgonathMountCollectionComponent clone) {
        this.ownerId = clone.ownerId;
        this.mounts = new ArrayList<>(clone.mounts);
        this.primaryMountId = clone.primaryMountId;
        this.secondaryMountId = clone.secondaryMountId;
        this.favoriteIds = new ArrayList<>(clone.favoriteIds);
        this.lastUpdatedEpoch = clone.lastUpdatedEpoch;
    }

    // ========== Component Interface ==========
    
    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new ArgonathMountCollectionComponent(this);
    }

    // ========== Getters and Setters ==========
    
    public UUID getOwnerId() {
        return ownerId;
    }
    
    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }
    
    public List<MountDataEntry> getMounts() {
        return mounts;
    }
    
    public void setMounts(List<MountDataEntry> mounts) {
        this.mounts = mounts != null ? new ArrayList<>(mounts) : new ArrayList<>();
    }
    
    public String getPrimaryMountId() {
        return primaryMountId;
    }
    
    public void setPrimaryMountId(String primaryMountId) {
        this.primaryMountId = primaryMountId;
    }
    
    public String getSecondaryMountId() {
        return secondaryMountId;
    }
    
    public void setSecondaryMountId(String secondaryMountId) {
        this.secondaryMountId = secondaryMountId;
    }
    
    public List<String> getFavoriteIds() {
        return favoriteIds;
    }
    
    public void setFavoriteIds(List<String> favoriteIds) {
        this.favoriteIds = favoriteIds != null ? new ArrayList<>(favoriteIds) : new ArrayList<>();
    }
    
    public Instant getLastUpdated() {
        return Instant.ofEpochMilli(lastUpdatedEpoch);
    }
    
    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdatedEpoch = lastUpdated != null ? lastUpdated.toEpochMilli() : Instant.now().toEpochMilli();
    }

    // ========== Utility Methods ==========
    
    public int getMountCount() {
        return mounts != null ? mounts.size() : 0;
    }

    @Override
    public String toString() {
        return "ArgonathMountCollectionComponent{" +
            "ownerId=" + ownerId +
            ", mountCount=" + getMountCount() +
            ", primaryMountId='" + primaryMountId + '\'' +
            '}';
    }
}
