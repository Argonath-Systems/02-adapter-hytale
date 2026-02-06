package com.argonathsystems.adapter.hytale.ecs;

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
 * Hytale ECS Component wrapper for player's guild membership.
 * 
 * <p>Stores the player's current guild affiliation, rank, permissions,
 * and contribution metrics. Note: Guild data itself is stored separately
 * (not per-player) - this component only stores the player's membership info.
 * 
 * <h2>Design Decision</h2>
 * <p>Guild-level data (name, level, bank) is persisted server-wide via
 * {@code GuildPersistenceManager}. This component only tracks the player's
 * relationship to their guild.
 * 
 * <h2>Specification Reference</h2>
 * <ul>
 *   <li>SF-ARCHITECTURE-028-ecs-persistence-bridge</li>
 *   <li>HLR-SOCIAL-004-guild-management-system</li>
 * </ul>
 * 
 * @author Argonath Systems Team
 * @version 1.0.0
 * @since 4.0.0
 */
public class ArgonathGuildMembershipComponent implements Component<EntityStore> {

    // ========== Serialized Fields ==========
    
    private UUID playerId;
    private String guildId;  // UUID string, null if not in guild
    private String rank;
    private Map<String, Boolean> permissions;
    private long contributedExperience;
    private long contributedCurrency;
    private int itemsDonated;
    private long joinedAtEpoch;
    private long lastActivityEpoch;
    private String lastActivityType;
    
    // ========== Guild Invite Tracking ==========
    
    private String pendingInviteGuildId;
    private long pendingInviteExpiresEpoch;

    // ========== CODEC Definition ==========
    
    public static final BuilderCodec<ArgonathGuildMembershipComponent> CODEC = 
        BuilderCodec.builder(ArgonathGuildMembershipComponent.class, ArgonathGuildMembershipComponent::new)
            .addField(new KeyedCodec<>("PlayerId", Codec.UUID_BINARY),
                (d, v) -> d.playerId = v, d -> d.playerId)
            .addField(new KeyedCodec<>("GuildId", Codec.STRING),
                (d, v) -> d.guildId = v, d -> d.guildId)
            .addField(new KeyedCodec<>("Rank", Codec.STRING),
                (d, v) -> d.rank = v, d -> d.rank)
            .addField(new KeyedCodec<>("Permissions", new MapCodec<>(Codec.BOOLEAN, HashMap::new, false)),
                (d, v) -> d.permissions = v, d -> d.permissions)
            .addField(new KeyedCodec<>("ContributedExperience", Codec.LONG),
                (d, v) -> d.contributedExperience = v, d -> d.contributedExperience)
            .addField(new KeyedCodec<>("ContributedCurrency", Codec.LONG),
                (d, v) -> d.contributedCurrency = v, d -> d.contributedCurrency)
            .addField(new KeyedCodec<>("ItemsDonated", Codec.INTEGER),
                (d, v) -> d.itemsDonated = v, d -> d.itemsDonated)
            .addField(new KeyedCodec<>("JoinedAt", Codec.LONG),
                (d, v) -> d.joinedAtEpoch = v, d -> d.joinedAtEpoch)
            .addField(new KeyedCodec<>("LastActivity", Codec.LONG),
                (d, v) -> d.lastActivityEpoch = v, d -> d.lastActivityEpoch)
            .addField(new KeyedCodec<>("LastActivityType", Codec.STRING),
                (d, v) -> d.lastActivityType = v, d -> d.lastActivityType)
            .addField(new KeyedCodec<>("PendingInviteGuildId", Codec.STRING),
                (d, v) -> d.pendingInviteGuildId = v, d -> d.pendingInviteGuildId)
            .addField(new KeyedCodec<>("PendingInviteExpires", Codec.LONG),
                (d, v) -> d.pendingInviteExpiresEpoch = v, d -> d.pendingInviteExpiresEpoch)
            .build();

    // ========== Constructors ==========
    
    public ArgonathGuildMembershipComponent() {
        this.permissions = new HashMap<>();
    }
    
    public ArgonathGuildMembershipComponent(ArgonathGuildMembershipComponent clone) {
        this.playerId = clone.playerId;
        this.guildId = clone.guildId;
        this.rank = clone.rank;
        this.permissions = new HashMap<>(clone.permissions);
        this.contributedExperience = clone.contributedExperience;
        this.contributedCurrency = clone.contributedCurrency;
        this.itemsDonated = clone.itemsDonated;
        this.joinedAtEpoch = clone.joinedAtEpoch;
        this.lastActivityEpoch = clone.lastActivityEpoch;
        this.lastActivityType = clone.lastActivityType;
        this.pendingInviteGuildId = clone.pendingInviteGuildId;
        this.pendingInviteExpiresEpoch = clone.pendingInviteExpiresEpoch;
    }

    // ========== Component Interface ==========
    
    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new ArgonathGuildMembershipComponent(this);
    }

    // ========== Guild Operations ==========
    
    /**
     * Check if player is in a guild.
     */
    public boolean isInGuild() {
        return guildId != null && !guildId.isBlank();
    }
    
    /**
     * Join a guild.
     */
    public void joinGuild(String guildId, String rank) {
        this.guildId = guildId;
        this.rank = rank;
        this.joinedAtEpoch = Instant.now().toEpochMilli();
        this.lastActivityEpoch = Instant.now().toEpochMilli();
        this.lastActivityType = "JOINED";
        this.contributedExperience = 0;
        this.contributedCurrency = 0;
        this.itemsDonated = 0;
        this.permissions.clear();
        clearPendingInvite();
    }
    
    /**
     * Leave current guild.
     */
    public void leaveGuild() {
        this.guildId = null;
        this.rank = null;
        this.permissions.clear();
        this.lastActivityEpoch = Instant.now().toEpochMilli();
        this.lastActivityType = "LEFT";
    }
    
    /**
     * Record activity.
     */
    public void recordActivity(String activityType) {
        this.lastActivityEpoch = Instant.now().toEpochMilli();
        this.lastActivityType = activityType;
    }
    
    /**
     * Add contribution.
     */
    public void addContribution(long experience, long currency, int items) {
        this.contributedExperience += experience;
        this.contributedCurrency += currency;
        this.itemsDonated += items;
        recordActivity("CONTRIBUTION");
    }
    
    /**
     * Check if player has a specific permission.
     */
    public boolean hasPermission(String permission) {
        return permissions.getOrDefault(permission, false);
    }
    
    /**
     * Set a pending guild invite.
     */
    public void setPendingInvite(String guildId, long expiresInSeconds) {
        this.pendingInviteGuildId = guildId;
        this.pendingInviteExpiresEpoch = Instant.now().plusSeconds(expiresInSeconds).toEpochMilli();
    }
    
    /**
     * Clear pending invite.
     */
    public void clearPendingInvite() {
        this.pendingInviteGuildId = null;
        this.pendingInviteExpiresEpoch = 0;
    }
    
    /**
     * Check if pending invite is valid.
     */
    public boolean hasPendingInvite() {
        if (pendingInviteGuildId == null) return false;
        return Instant.now().toEpochMilli() < pendingInviteExpiresEpoch;
    }

    // ========== Getters ==========
    
    public UUID getPlayerId() { return playerId; }
    public String getGuildId() { return guildId; }
    public String getRank() { return rank; }
    public Map<String, Boolean> getPermissions() { return new HashMap<>(permissions); }
    public long getContributedExperience() { return contributedExperience; }
    public long getContributedCurrency() { return contributedCurrency; }
    public int getItemsDonated() { return itemsDonated; }
    public Instant getJoinedAt() { return joinedAtEpoch > 0 ? Instant.ofEpochMilli(joinedAtEpoch) : null; }
    public Instant getLastActivity() { return lastActivityEpoch > 0 ? Instant.ofEpochMilli(lastActivityEpoch) : null; }
    public String getLastActivityType() { return lastActivityType; }
    public String getPendingInviteGuildId() { return pendingInviteGuildId; }

    // ========== Setters ==========
    
    public void setPlayerId(UUID playerId) { this.playerId = playerId; }
    public void setGuildId(String guildId) { this.guildId = guildId; }
    public void setRank(String rank) { this.rank = rank; }
    public void setPermissions(Map<String, Boolean> permissions) { 
        this.permissions = permissions != null ? new HashMap<>(permissions) : new HashMap<>(); 
    }

    @Override
    public String toString() {
        return "ArgonathGuildMembershipComponent{" +
            "playerId=" + playerId +
            ", guildId='" + guildId + '\'' +
            ", rank='" + rank + '\'' +
            ", inGuild=" + isInGuild() +
            '}';
    }
}
