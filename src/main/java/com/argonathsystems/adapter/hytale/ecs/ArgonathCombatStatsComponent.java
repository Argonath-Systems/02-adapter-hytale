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
 * Hytale ECS Component wrapper for player's combat statistics.
 * 
 * <p>Tracks lifetime combat performance metrics including kills, deaths,
 * damage dealt/taken, and PvP statistics for leaderboards and achievements.
 * 
 * <h2>Specification Reference</h2>
 * <ul>
 *   <li>SF-ARCHITECTURE-028-ecs-persistence-bridge</li>
 *   <li>SM-COMBAT-011-combat-stats-persistence</li>
 * </ul>
 * 
 * @author Argonath Systems Team
 * @version 1.0.0
 * @since 4.0.0
 */
public class ArgonathCombatStatsComponent implements Component<EntityStore> {

    // ========== Serialized Fields - Kill/Death Tracking ==========
    
    private UUID playerId;
    private long totalKills;
    private long totalDeaths;
    private long playerKills;
    private long mobKills;
    private long bossKills;
    private int currentKillStreak;
    private int longestKillStreak;
    
    // ========== Damage Statistics ==========
    
    private long totalDamageDealt;
    private long totalDamageTaken;
    private long totalHealingDone;
    private long criticalHits;
    
    // ========== PvP/Arena Statistics ==========
    
    private int arenaWins;
    private int arenaLosses;
    private int duelWins;
    private int duelLosses;
    private int pvpRating;
    
    // ========== Breakdown Maps ==========
    
    private Map<String, Integer> killsByEnemyType;
    private Map<String, Integer> killsByWeaponType;
    
    // ========== Timestamps ==========
    
    private long lastKillAtEpoch;
    private long lastDeathAtEpoch;
    private long createdAtEpoch;
    private long lastUpdatedEpoch;

    // ========== CODEC Definition ==========
    
    public static final BuilderCodec<ArgonathCombatStatsComponent> CODEC = 
        BuilderCodec.builder(ArgonathCombatStatsComponent.class, ArgonathCombatStatsComponent::new)
            // Identity
            .addField(new KeyedCodec<>("PlayerId", Codec.UUID_BINARY),
                (d, v) -> d.playerId = v, d -> d.playerId)
            // Kill/Death
            .addField(new KeyedCodec<>("TotalKills", Codec.LONG),
                (d, v) -> d.totalKills = v, d -> d.totalKills)
            .addField(new KeyedCodec<>("TotalDeaths", Codec.LONG),
                (d, v) -> d.totalDeaths = v, d -> d.totalDeaths)
            .addField(new KeyedCodec<>("PlayerKills", Codec.LONG),
                (d, v) -> d.playerKills = v, d -> d.playerKills)
            .addField(new KeyedCodec<>("MobKills", Codec.LONG),
                (d, v) -> d.mobKills = v, d -> d.mobKills)
            .addField(new KeyedCodec<>("BossKills", Codec.LONG),
                (d, v) -> d.bossKills = v, d -> d.bossKills)
            .addField(new KeyedCodec<>("CurrentKillStreak", Codec.INTEGER),
                (d, v) -> d.currentKillStreak = v, d -> d.currentKillStreak)
            .addField(new KeyedCodec<>("LongestKillStreak", Codec.INTEGER),
                (d, v) -> d.longestKillStreak = v, d -> d.longestKillStreak)
            // Damage
            .addField(new KeyedCodec<>("TotalDamageDealt", Codec.LONG),
                (d, v) -> d.totalDamageDealt = v, d -> d.totalDamageDealt)
            .addField(new KeyedCodec<>("TotalDamageTaken", Codec.LONG),
                (d, v) -> d.totalDamageTaken = v, d -> d.totalDamageTaken)
            .addField(new KeyedCodec<>("TotalHealingDone", Codec.LONG),
                (d, v) -> d.totalHealingDone = v, d -> d.totalHealingDone)
            .addField(new KeyedCodec<>("CriticalHits", Codec.LONG),
                (d, v) -> d.criticalHits = v, d -> d.criticalHits)
            // PvP
            .addField(new KeyedCodec<>("ArenaWins", Codec.INTEGER),
                (d, v) -> d.arenaWins = v, d -> d.arenaWins)
            .addField(new KeyedCodec<>("ArenaLosses", Codec.INTEGER),
                (d, v) -> d.arenaLosses = v, d -> d.arenaLosses)
            .addField(new KeyedCodec<>("DuelWins", Codec.INTEGER),
                (d, v) -> d.duelWins = v, d -> d.duelWins)
            .addField(new KeyedCodec<>("DuelLosses", Codec.INTEGER),
                (d, v) -> d.duelLosses = v, d -> d.duelLosses)
            .addField(new KeyedCodec<>("PvpRating", Codec.INTEGER),
                (d, v) -> d.pvpRating = v, d -> d.pvpRating)
            // Maps
            .addField(new KeyedCodec<>("KillsByEnemyType", new MapCodec<>(Codec.INTEGER, HashMap::new, false)),
                (d, v) -> d.killsByEnemyType = v, d -> d.killsByEnemyType)
            .addField(new KeyedCodec<>("KillsByWeaponType", new MapCodec<>(Codec.INTEGER, HashMap::new, false)),
                (d, v) -> d.killsByWeaponType = v, d -> d.killsByWeaponType)
            // Timestamps
            .addField(new KeyedCodec<>("LastKillAt", Codec.LONG),
                (d, v) -> d.lastKillAtEpoch = v, d -> d.lastKillAtEpoch)
            .addField(new KeyedCodec<>("LastDeathAt", Codec.LONG),
                (d, v) -> d.lastDeathAtEpoch = v, d -> d.lastDeathAtEpoch)
            .addField(new KeyedCodec<>("CreatedAt", Codec.LONG),
                (d, v) -> d.createdAtEpoch = v, d -> d.createdAtEpoch)
            .addField(new KeyedCodec<>("LastUpdated", Codec.LONG),
                (d, v) -> d.lastUpdatedEpoch = v, d -> d.lastUpdatedEpoch)
            .build();

    // ========== Constructors ==========
    
    public ArgonathCombatStatsComponent() {
        this.killsByEnemyType = new HashMap<>();
        this.killsByWeaponType = new HashMap<>();
        this.pvpRating = 1000; // Default ELO
        this.createdAtEpoch = Instant.now().toEpochMilli();
        this.lastUpdatedEpoch = Instant.now().toEpochMilli();
    }
    
    public ArgonathCombatStatsComponent(ArgonathCombatStatsComponent clone) {
        this.playerId = clone.playerId;
        this.totalKills = clone.totalKills;
        this.totalDeaths = clone.totalDeaths;
        this.playerKills = clone.playerKills;
        this.mobKills = clone.mobKills;
        this.bossKills = clone.bossKills;
        this.currentKillStreak = clone.currentKillStreak;
        this.longestKillStreak = clone.longestKillStreak;
        this.totalDamageDealt = clone.totalDamageDealt;
        this.totalDamageTaken = clone.totalDamageTaken;
        this.totalHealingDone = clone.totalHealingDone;
        this.criticalHits = clone.criticalHits;
        this.arenaWins = clone.arenaWins;
        this.arenaLosses = clone.arenaLosses;
        this.duelWins = clone.duelWins;
        this.duelLosses = clone.duelLosses;
        this.pvpRating = clone.pvpRating;
        this.killsByEnemyType = new HashMap<>(clone.killsByEnemyType);
        this.killsByWeaponType = new HashMap<>(clone.killsByWeaponType);
        this.lastKillAtEpoch = clone.lastKillAtEpoch;
        this.lastDeathAtEpoch = clone.lastDeathAtEpoch;
        this.createdAtEpoch = clone.createdAtEpoch;
        this.lastUpdatedEpoch = clone.lastUpdatedEpoch;
    }

    // ========== Component Interface ==========
    
    @Nonnull
    @Override
    public Component<EntityStore> clone() {
        return new ArgonathCombatStatsComponent(this);
    }

    // ========== Combat Recording Methods ==========
    
    /**
     * Records a kill event.
     */
    public void recordKill(boolean isPlayer, boolean isBoss, String enemyType, String weaponType) {
        this.totalKills++;
        this.currentKillStreak++;
        
        if (this.currentKillStreak > this.longestKillStreak) {
            this.longestKillStreak = this.currentKillStreak;
        }
        
        if (isPlayer) {
            this.playerKills++;
        } else {
            this.mobKills++;
            if (isBoss) {
                this.bossKills++;
            }
        }
        
        if (enemyType != null) {
            this.killsByEnemyType.merge(enemyType, 1, Integer::sum);
        }
        if (weaponType != null) {
            this.killsByWeaponType.merge(weaponType, 1, Integer::sum);
        }
        
        this.lastKillAtEpoch = Instant.now().toEpochMilli();
        this.lastUpdatedEpoch = Instant.now().toEpochMilli();
    }
    
    /**
     * Records a death event.
     */
    public void recordDeath() {
        this.totalDeaths++;
        this.currentKillStreak = 0;
        this.lastDeathAtEpoch = Instant.now().toEpochMilli();
        this.lastUpdatedEpoch = Instant.now().toEpochMilli();
    }
    
    /**
     * Records damage dealt.
     */
    public void recordDamageDealt(long amount, boolean wasCritical) {
        this.totalDamageDealt += amount;
        if (wasCritical) {
            this.criticalHits++;
        }
        this.lastUpdatedEpoch = Instant.now().toEpochMilli();
    }
    
    /**
     * Records damage taken.
     */
    public void recordDamageTaken(long amount) {
        this.totalDamageTaken += amount;
        this.lastUpdatedEpoch = Instant.now().toEpochMilli();
    }
    
    /**
     * Records healing done.
     */
    public void recordHealing(long amount) {
        this.totalHealingDone += amount;
        this.lastUpdatedEpoch = Instant.now().toEpochMilli();
    }

    // ========== Calculated Stats ==========
    
    public double getKillDeathRatio() {
        if (totalDeaths == 0) return totalKills;
        return (double) totalKills / totalDeaths;
    }
    
    public int getArenaWinRate() {
        int total = arenaWins + arenaLosses;
        if (total == 0) return 0;
        return (int) ((arenaWins * 100.0) / total);
    }

    // ========== Getters ==========
    
    public UUID getPlayerId() { return playerId; }
    public long getTotalKills() { return totalKills; }
    public long getTotalDeaths() { return totalDeaths; }
    public long getPlayerKills() { return playerKills; }
    public long getMobKills() { return mobKills; }
    public long getBossKills() { return bossKills; }
    public int getCurrentKillStreak() { return currentKillStreak; }
    public int getLongestKillStreak() { return longestKillStreak; }
    public long getTotalDamageDealt() { return totalDamageDealt; }
    public long getTotalDamageTaken() { return totalDamageTaken; }
    public long getTotalHealingDone() { return totalHealingDone; }
    public long getCriticalHits() { return criticalHits; }
    public int getArenaWins() { return arenaWins; }
    public int getArenaLosses() { return arenaLosses; }
    public int getDuelWins() { return duelWins; }
    public int getDuelLosses() { return duelLosses; }
    public int getPvpRating() { return pvpRating; }
    public Map<String, Integer> getKillsByEnemyType() { return new HashMap<>(killsByEnemyType); }
    public Map<String, Integer> getKillsByWeaponType() { return new HashMap<>(killsByWeaponType); }

    // ========== Setters ==========
    
    public void setPlayerId(UUID playerId) { this.playerId = playerId; }
    public void setPvpRating(int pvpRating) { this.pvpRating = pvpRating; }
    public void setArenaWins(int arenaWins) { this.arenaWins = arenaWins; }
    public void setArenaLosses(int arenaLosses) { this.arenaLosses = arenaLosses; }
    public void setDuelWins(int duelWins) { this.duelWins = duelWins; }
    public void setDuelLosses(int duelLosses) { this.duelLosses = duelLosses; }

    @Override
    public String toString() {
        return "ArgonathCombatStatsComponent{" +
            "playerId=" + playerId +
            ", kills=" + totalKills +
            ", deaths=" + totalDeaths +
            ", kdr=" + String.format("%.2f", getKillDeathRatio()) +
            ", pvpRating=" + pvpRating +
            '}';
    }
}
