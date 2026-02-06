package com.argonathsystems.adapter.hytale.ecs;

import com.argonathsystems.framework.stats.persistence.PlayerStatsData;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.MapCodec;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Component;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Hytale ECS Component wrapper for player statistics.
 * 
 * <p>This component wraps {@link PlayerStatsData} for automatic persistence
 * through Hytale's EntityStore system. Use {@code putComponent()} for persistent
 * storage that survives server restarts.
 * 
 * <h2>Usage</h2>
 * <pre>{@code
 * // On player join - load/create component
 * ArgonathPlayerStatsComponent comp = store.ensureAndGetComponent(
 *     ref, ArgonathComponentRegistry.playerStats()
 * );
 * PlayerStatsData pojo = comp.toPojo();
 * 
 * // On player quit - save component
 * ArgonathPlayerStatsComponent updated = ArgonathPlayerStatsComponent.fromPojo(pojo);
 * store.putComponent(ref, ArgonathComponentRegistry.playerStats(), updated);
 * }</pre>
 * 
 * <h2>Specification</h2>
 * <p>SF-ARCHITECTURE-028-ecs-persistence-bridge, Section 4.1
 * 
 * @author Argonath Systems Team
 * @version 1.0.0
 * @since 4.0.0
 * @see PlayerStatsData
 * @see ArgonathComponentRegistry
 */
public class ArgonathPlayerStatsComponent implements Component<EntityStore> {

    // ========== Serialized Fields ==========
    
    private UUID playerId;
    private int level;
    private long experience;
    private Map<String, Double> baseStats;
    private long lastUpdatedEpoch;
    private long version;

    // ========== CODEC Definition ==========
    
    /**
     * BuilderCodec for BSON serialization to Hytale's EntityStore.
     * 
     * <p>Each field is mapped with a KeyedCodec specifying:
     * <ol>
     *   <li>Key name in BSON</li>
     *   <li>Codec type (STRING, INTEGER, LONG, etc.)</li>
     *   <li>Setter lambda</li>
     *   <li>Getter lambda</li>
     * </ol>
     */
    public static final BuilderCodec<ArgonathPlayerStatsComponent> CODEC = 
        BuilderCodec.builder(ArgonathPlayerStatsComponent.class, ArgonathPlayerStatsComponent::new)
            .addField(new KeyedCodec<>("PlayerId", Codec.UUID_BINARY),
                (data, v) -> data.playerId = v, 
                data -> data.playerId)
            .addField(new KeyedCodec<>("Level", Codec.INTEGER),
                (data, v) -> data.level = v, 
                data -> data.level)
            .addField(new KeyedCodec<>("Experience", Codec.LONG),
                (data, v) -> data.experience = v, 
                data -> data.experience)
            .addField(new KeyedCodec<>("BaseStats", new MapCodec<>(Codec.DOUBLE, HashMap::new, false)),
                (data, v) -> data.baseStats = v, 
                data -> data.baseStats)
            .addField(new KeyedCodec<>("LastUpdated", Codec.LONG),
                (data, v) -> data.lastUpdatedEpoch = v, 
                data -> data.lastUpdatedEpoch)
            .addField(new KeyedCodec<>("Version", Codec.LONG),
                (data, v) -> data.version = v, 
                data -> data.version)
            .build();

    // ========== Constructors ==========
    
    /**
     * Default constructor for deserialization.
     */
    public ArgonathPlayerStatsComponent() {
        this.baseStats = new HashMap<>();
        this.level = 1;
        this.experience = 0L;
        this.lastUpdatedEpoch = Instant.now().toEpochMilli();
        this.version = 1L;
    }
    
    /**
     * Copy constructor for cloning.
     * 
     * @param clone Component to clone
     */
    public ArgonathPlayerStatsComponent(ArgonathPlayerStatsComponent clone) {
        this.playerId = clone.playerId;
        this.level = clone.level;
        this.experience = clone.experience;
        this.baseStats = new HashMap<>(clone.baseStats);
        this.lastUpdatedEpoch = clone.lastUpdatedEpoch;
        this.version = clone.version;
    }

    // ========== Component Interface ==========
    
    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new ArgonathPlayerStatsComponent(this);
    }

    // ========== Conversion Methods ==========
    
    /**
     * Create component from platform-agnostic POJO.
     * 
     * @param pojo The PlayerStatsData to wrap
     * @return New component containing the POJO data
     */
    public static ArgonathPlayerStatsComponent fromPojo(PlayerStatsData pojo) {
        ArgonathPlayerStatsComponent comp = new ArgonathPlayerStatsComponent();
        comp.playerId = pojo.getPlayerId();
        comp.level = pojo.getLevel();
        comp.experience = pojo.getExperience();
        comp.baseStats = new HashMap<>(pojo.getBaseStats());
        comp.lastUpdatedEpoch = pojo.getLastUpdated() != null 
            ? pojo.getLastUpdated().toEpochMilli() 
            : Instant.now().toEpochMilli();
        comp.version = pojo.getVersion();
        return comp;
    }
    
    /**
     * Convert to platform-agnostic POJO.
     * 
     * @return PlayerStatsData containing this component's data
     */
    public PlayerStatsData toPojo() {
        PlayerStatsData pojo = new PlayerStatsData(playerId);
        pojo.setLevel(level);
        pojo.setExperience(experience);
        pojo.setBaseStats(new HashMap<>(baseStats));
        pojo.setLastUpdated(Instant.ofEpochMilli(lastUpdatedEpoch));
        pojo.setVersion(version);
        return pojo;
    }

    // ========== Getters ==========
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public int getLevel() {
        return level;
    }
    
    public long getExperience() {
        return experience;
    }
    
    public Map<String, Double> getBaseStats() {
        return new HashMap<>(baseStats);
    }
    
    public Instant getLastUpdated() {
        return Instant.ofEpochMilli(lastUpdatedEpoch);
    }
    
    public long getVersion() {
        return version;
    }

    // ========== Setters ==========
    
    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }
    
    public void setLevel(int level) {
        this.level = level;
    }
    
    public void setExperience(long experience) {
        this.experience = experience;
    }
    
    public void setBaseStats(Map<String, Double> baseStats) {
        this.baseStats = baseStats != null ? new HashMap<>(baseStats) : new HashMap<>();
    }
    
    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdatedEpoch = lastUpdated != null ? lastUpdated.toEpochMilli() : Instant.now().toEpochMilli();
    }
    
    public void setVersion(long version) {
        this.version = version;
    }

    // ========== Utility Methods ==========
    
    /**
     * Update from a POJO without creating a new instance.
     * 
     * @param pojo Source data
     */
    public void updateFromPojo(PlayerStatsData pojo) {
        this.playerId = pojo.getPlayerId();
        this.level = pojo.getLevel();
        this.experience = pojo.getExperience();
        this.baseStats = new HashMap<>(pojo.getBaseStats());
        this.lastUpdatedEpoch = Instant.now().toEpochMilli();
        this.version = pojo.getVersion() + 1;
    }

    @Override
    public String toString() {
        return "ArgonathPlayerStatsComponent{" +
            "playerId=" + playerId +
            ", level=" + level +
            ", experience=" + experience +
            ", statsCount=" + (baseStats != null ? baseStats.size() : 0) +
            ", version=" + version +
            '}';
    }
}
