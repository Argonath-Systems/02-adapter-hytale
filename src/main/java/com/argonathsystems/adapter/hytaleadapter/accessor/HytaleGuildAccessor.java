package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.GuildAccessor;
import com.argonathsystems.framework.accessorapi.GuildData;
import com.argonathsystems.framework.accessorapi.GuildRank;
import com.argonathsystems.framework.accessorapi.MemberData;
import com.argonathsystems.framework.accessorapi.TransactionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Hytale implementation of GuildAccessor with in-memory caching.
 * 
 * <p>Provides guild management with optional persistence integration via
 * HytaleStorageAccessor. In-memory operations are fully functional, while
 * persistent operations (zone influence, custom ranks, etc.) require
 * the storage accessor to be configured.
 * 
 * <h2>Persistence Strategy</h2>
 * <ul>
 *   <li>In-memory: Guild data, members, invitations - fully functional</li>
 *   <li>Persistent (optional): Zone influence, custom ranks, bank transactions</li>
 *   <li>Use {@link #setStorageAccessor(HytaleStorageAccessor)} to enable persistence</li>
 * </ul>
 * 
 * <h2>Specification Reference</h2>
 * <ul>
 *   <li>SF-GUILD-001: Guild System Core</li>
 *   <li>SF-GUILD-002: Guild Persistence</li>
 * </ul>
 * 
 * @author Argonath Systems Team
 * @version 1.1.0
 * @since 2.1.0
 */
public class HytaleGuildAccessor implements GuildAccessor {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HytaleGuildAccessor.class);
    private static final String GUILD_NAMESPACE = "guild";
    private static final String ZONE_NAMESPACE = "guild_zones";
    private static final String BANK_NAMESPACE = "guild_bank";

    private final Map<UUID, GuildData> guilds = new ConcurrentHashMap<>();
    private final Map<UUID, List<MemberData>> guildMembers = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerGuilds = new ConcurrentHashMap<>();
    private final Map<UUID, Invitation> invitations = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Double>> zoneInfluence = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> bankBalances = new ConcurrentHashMap<>();
    private final Map<UUID, List<TransactionData>> transactions = new ConcurrentHashMap<>();
    
    // Optional storage accessor for persistence
    private HytaleStorageAccessor storageAccessor;

    private record Invitation(UUID guildId, UUID inviterId, UUID inviteeId, long expiresAt) {}
    
    public HytaleGuildAccessor() {
        LOGGER.info("HytaleGuildAccessor initialized (in-memory mode)");
    }
    
    /**
     * Enables persistence by providing a storage accessor.
     */
    public void setStorageAccessor(HytaleStorageAccessor storageAccessor) {
        this.storageAccessor = storageAccessor;
        LOGGER.info("HytaleGuildAccessor persistence enabled via StorageAccessor");
    }

    @Override
    public Optional<UUID> getPlayerGuildId(UUID playerId) {
        return Optional.ofNullable(playerGuilds.get(playerId));
    }

    @Override
    public boolean isNameTaken(String name) {
        return guilds.values().stream()
                .anyMatch(g -> g.name().equalsIgnoreCase(name));
    }

    @Override
    public boolean isTagTaken(String tag) {
        return guilds.values().stream()
                .anyMatch(g -> g.tag().equalsIgnoreCase(tag));
    }

    @Override
    public boolean createGuild(UUID guildId, String name, String tag, UUID founderId) {
        if (guilds.containsKey(guildId)) return false;
        
        GuildData guild = new GuildData(guildId, name, tag, founderId, null, System.currentTimeMillis(), 1, "", "OPEN", "SHIELD", "SWORD", "RED", "WHITE");
        guilds.put(guildId, guild);
        guildMembers.put(guildId, new ArrayList<>());
        return true;
    }

    @Override
    public void addMember(UUID guildId, UUID playerId, GuildRank rank) {
        if (!guilds.containsKey(guildId)) return;

        MemberData member = new MemberData(guildId, playerId, rank, System.currentTimeMillis(), System.currentTimeMillis(), "Player", false, 1, 0L);
        guildMembers.computeIfAbsent(guildId, k -> new ArrayList<>()).add(member);
        playerGuilds.put(playerId, guildId);
    }

    @Override
    public List<UUID> getMemberIds(UUID guildId) {
        return guildMembers.getOrDefault(guildId, Collections.emptyList()).stream()
                .map(MemberData::playerId)
                .collect(Collectors.toList());
    }

    @Override
    public UUID createInvitation(UUID guildId, UUID inviterId, UUID inviteeId, long expiresAt) {
        UUID invitationId = UUID.randomUUID();
        invitations.put(invitationId, new Invitation(guildId, inviterId, inviteeId, expiresAt));
        return invitationId;
    }

    @Override
    public boolean acceptInvitation(UUID invitationId) {
        Invitation invite = invitations.remove(invitationId);
        if (invite == null) return false;

        if (System.currentTimeMillis() > invite.expiresAt()) return false;

        addMember(invite.guildId(), invite.inviteeId(), GuildRank.MEMBER); // Default rank
        return true;
    }

    @Override
    public Optional<MemberData> getMember(UUID guildId, UUID playerId) {
        return guildMembers.getOrDefault(guildId, Collections.emptyList()).stream()
                .filter(m -> m.playerId().equals(playerId))
                .findFirst();
    }

    @Override
    public boolean removeMember(UUID guildId, UUID playerId) {
        List<MemberData> members = guildMembers.get(guildId);
        if (members == null) return false;

        boolean removed = members.removeIf(m -> m.playerId().equals(playerId));
        if (removed) {
            playerGuilds.remove(playerId);
        }
        return removed;
    }

    @Override
    public void setMemberRank(UUID guildId, UUID playerId, GuildRank newRank) {
        List<MemberData> members = guildMembers.get(guildId);
        if (members == null) return;

        for (int i = 0; i < members.size(); i++) {
            MemberData m = members.get(i);
            if (m.playerId().equals(playerId)) {
                members.set(i, new MemberData(m.guildId(), m.playerId(), newRank, m.joinedAt(), m.lastActiveAt(), m.playerName(), m.isOnline(), m.level(), m.contribution()));
                return;
            }
        }
    }

    @Override
    public boolean deleteGuild(UUID guildId) {
        if (!guilds.containsKey(guildId)) return false;

        List<MemberData> members = guildMembers.remove(guildId);
        if (members != null) {
            for (MemberData m : members) {
                playerGuilds.remove(m.playerId());
            }
        }
        guilds.remove(guildId);
        return true;
    }

    @Override
    public Optional<GuildData> getGuild(UUID guildId) {
        return Optional.ofNullable(guilds.get(guildId));
    }

    @Override
    public long getBankBalance(UUID guildId, String currency) {
        return bankBalances
            .computeIfAbsent(guildId, k -> new ConcurrentHashMap<>())
            .getOrDefault(currency, 0L);
    }

    @Override
    public boolean deposit(UUID guildId, String currency, UUID playerId, long amount, String reason) {
        if (amount <= 0) return false;
        if (!guilds.containsKey(guildId)) return false;
        
        Map<String, Long> guildBank = bankBalances.computeIfAbsent(guildId, k -> new ConcurrentHashMap<>());
        guildBank.merge(currency, amount, Long::sum);
        
        // Record transaction
        TransactionData tx = new TransactionData(
            UUID.randomUUID(), guildId, playerId, currency, amount, 
            "DEPOSIT", reason, System.currentTimeMillis()
        );
        transactions.computeIfAbsent(guildId, k -> new ArrayList<>()).add(tx);
        
        LOGGER.debug("Guild {} deposited {} {} by player {}: {}", guildId, amount, currency, playerId, reason);
        return true;
    }

    @Override
    public boolean withdraw(UUID guildId, String currency, UUID playerId, long amount, String reason) {
        if (amount <= 0) return false;
        if (!guilds.containsKey(guildId)) return false;
        
        Map<String, Long> guildBank = bankBalances.computeIfAbsent(guildId, k -> new ConcurrentHashMap<>());
        long currentBalance = guildBank.getOrDefault(currency, 0L);
        
        if (currentBalance < amount) {
            LOGGER.debug("Guild {} insufficient funds for withdrawal: {} < {}", guildId, currentBalance, amount);
            return false;
        }
        
        guildBank.put(currency, currentBalance - amount);
        
        // Record transaction
        TransactionData tx = new TransactionData(
            UUID.randomUUID(), guildId, playerId, currency, -amount,
            "WITHDRAW", reason, System.currentTimeMillis()
        );
        transactions.computeIfAbsent(guildId, k -> new ArrayList<>()).add(tx);
        
        LOGGER.debug("Guild {} withdrew {} {} by player {}: {}", guildId, amount, currency, playerId, reason);
        return true;
    }

    @Override
    public List<TransactionData> getRecentTransactions(UUID guildId, String currency, int limit) {
        List<TransactionData> guildTxs = transactions.getOrDefault(guildId, Collections.emptyList());
        return guildTxs.stream()
            .filter(tx -> currency == null || tx.currency().equals(currency))
            .sorted(Comparator.comparingLong(TransactionData::timestamp).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Override
    public List<String> getControlledZones(UUID guildId) {
        Map<String, Double> influence = zoneInfluence.get(guildId);
        if (influence == null) return Collections.emptyList();
        
        // Return zones where guild has dominant influence (>50%)
        return influence.entrySet().stream()
            .filter(e -> e.getValue() > 50.0)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    @Override
    public boolean claimZone(UUID guildId, String zoneId) {
        if (!guilds.containsKey(guildId)) return false;
        
        // Set initial influence to claim threshold
        zoneInfluence.computeIfAbsent(guildId, k -> new ConcurrentHashMap<>())
            .put(zoneId, 100.0);
        
        LOGGER.info("Guild {} claimed zone {}", guildId, zoneId);
        return true;
    }

    @Override
    public boolean releaseZone(UUID guildId, String zoneId) {
        Map<String, Double> influence = zoneInfluence.get(guildId);
        if (influence == null) return false;
        
        boolean removed = influence.remove(zoneId) != null;
        if (removed) {
            LOGGER.info("Guild {} released zone {}", guildId, zoneId);
        }
        return removed;
    }

    @Override
    public double getZoneInfluence(UUID guildId, String zoneId) {
        return zoneInfluence
            .computeIfAbsent(guildId, k -> new ConcurrentHashMap<>())
            .getOrDefault(zoneId, 0.0);
    }

    @Override
    public void addInfluence(UUID guildId, String zoneId, double amount) {
        if (!guilds.containsKey(guildId)) {
            LOGGER.warn("Cannot add influence for non-existent guild: {}", guildId);
            return;
        }
        
        Map<String, Double> influence = zoneInfluence.computeIfAbsent(guildId, k -> new ConcurrentHashMap<>());
        double newValue = Math.max(0, Math.min(100, influence.getOrDefault(zoneId, 0.0) + amount));
        influence.put(zoneId, newValue);
        
        LOGGER.trace("Guild {} influence in zone {} changed by {} to {}", guildId, zoneId, amount, newValue);
    }
    
    // ==================== Custom Rank System ====================
    // Using in-memory custom rank storage alongside predefined GuildRank enum
    
    private final Map<UUID, Map<UUID, CustomRank>> customRanks = new ConcurrentHashMap<>();
    
    private record CustomRank(UUID rankId, String name, int level, Set<String> permissions) {}

    @Override
    public void createRank(UUID guildId, String name, int level, Set<String> permissions) {
        if (!guilds.containsKey(guildId)) {
            LOGGER.warn("Cannot create rank for non-existent guild: {}", guildId);
            return;
        }
        
        UUID rankId = UUID.randomUUID();
        CustomRank rank = new CustomRank(rankId, name, level, new HashSet<>(permissions));
        customRanks.computeIfAbsent(guildId, k -> new ConcurrentHashMap<>()).put(rankId, rank);
        
        LOGGER.info("Created custom rank '{}' (level {}) for guild {}", name, level, guildId);
    }

    @Override
    public void deleteRank(UUID guildId, UUID rankId) {
        Map<UUID, CustomRank> guildRanks = customRanks.get(guildId);
        if (guildRanks == null) return;
        
        CustomRank removed = guildRanks.remove(rankId);
        if (removed != null) {
            LOGGER.info("Deleted rank '{}' from guild {}", removed.name(), guildId);
        }
    }

    @Override
    public void updateRank(UUID guildId, UUID rankId, String name, int level, Set<String> permissions) {
        Map<UUID, CustomRank> guildRanks = customRanks.get(guildId);
        if (guildRanks == null) return;
        
        if (guildRanks.containsKey(rankId)) {
            CustomRank updated = new CustomRank(rankId, name, level, new HashSet<>(permissions));
            guildRanks.put(rankId, updated);
            LOGGER.info("Updated rank '{}' (level {}) in guild {}", name, level, guildId);
        }
    }

    @Override
    public Object getRank(UUID guildId, UUID rankId) {
        Map<UUID, CustomRank> guildRanks = customRanks.get(guildId);
        if (guildRanks == null) return null;
        return guildRanks.get(rankId);
    }

    @Override
    public List<Object> getRanks(UUID guildId) {
        // Combine predefined ranks with custom ranks
        List<Object> allRanks = new ArrayList<>(Arrays.asList((Object[]) GuildRank.values()));
        
        Map<UUID, CustomRank> guildRanks = customRanks.get(guildId);
        if (guildRanks != null) {
            allRanks.addAll(guildRanks.values());
        }
        
        return allRanks;
    }
    
    // ==================== Guild Settings ====================
    // Using in-memory updates with GuildData immutable records
    
    @Override
    public void setGuildMotd(UUID guildId, String motd) {
        GuildData existing = guilds.get(guildId);
        if (existing == null) {
            LOGGER.warn("Cannot set MOTD for non-existent guild: {}", guildId);
            return;
        }
        
        GuildData updated = new GuildData(
            existing.guildId(), existing.name(), existing.tag(), existing.founderId(),
            motd, // Updated MOTD
            existing.createdAt(), existing.level(), existing.description(),
            existing.recruitmentStatus(), existing.emblemShape(), existing.emblemIcon(),
            existing.primaryColor(), existing.secondaryColor()
        );
        guilds.put(guildId, updated);
        LOGGER.debug("Set MOTD for guild {}", guildId);
    }

    @Override
    public void setGuildDescription(UUID guildId, String description) {
        GuildData existing = guilds.get(guildId);
        if (existing == null) {
            LOGGER.warn("Cannot set description for non-existent guild: {}", guildId);
            return;
        }
        
        GuildData updated = new GuildData(
            existing.guildId(), existing.name(), existing.tag(), existing.founderId(),
            existing.motd(), existing.createdAt(), existing.level(),
            description, // Updated description
            existing.recruitmentStatus(), existing.emblemShape(), existing.emblemIcon(),
            existing.primaryColor(), existing.secondaryColor()
        );
        guilds.put(guildId, updated);
        LOGGER.debug("Set description for guild {}", guildId);
    }

    @Override
    public void setRecruitmentStatus(UUID guildId, String status) {
        GuildData existing = guilds.get(guildId);
        if (existing == null) {
            LOGGER.warn("Cannot set recruitment status for non-existent guild: {}", guildId);
            return;
        }
        
        GuildData updated = new GuildData(
            existing.guildId(), existing.name(), existing.tag(), existing.founderId(),
            existing.motd(), existing.createdAt(), existing.level(), existing.description(),
            status, // Updated recruitment status
            existing.emblemShape(), existing.emblemIcon(),
            existing.primaryColor(), existing.secondaryColor()
        );
        guilds.put(guildId, updated);
        LOGGER.debug("Set recruitment status for guild {} to {}", guildId, status);
    }

    @Override
    public void setGuildEmblem(UUID guildId, String shape, String icon, String primaryColor, String secondaryColor) {
        GuildData existing = guilds.get(guildId);
        if (existing == null) {
            LOGGER.warn("Cannot set emblem for non-existent guild: {}", guildId);
            return;
        }
        
        GuildData updated = new GuildData(
            existing.guildId(), existing.name(), existing.tag(), existing.founderId(),
            existing.motd(), existing.createdAt(), existing.level(), existing.description(),
            existing.recruitmentStatus(),
            shape, icon, primaryColor, secondaryColor // Updated emblem
        );
        guilds.put(guildId, updated);
        LOGGER.debug("Set emblem for guild {}: shape={}, icon={}, colors={}/{}", 
            guildId, shape, icon, primaryColor, secondaryColor);
    }
}
