package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.GuildAccessor;
import com.argonathsystems.framework.accessorapi.GuildData;
import com.argonathsystems.framework.accessorapi.GuildRank;
import com.argonathsystems.framework.accessorapi.MemberData;
import com.argonathsystems.framework.accessorapi.TransactionData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
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
    private static final String MEMBERS_NAMESPACE = "guild_members";
    private static final String PLAYER_GUILDS_NAMESPACE = "player_guilds";

    private final Map<UUID, GuildData> guilds = new ConcurrentHashMap<>();
    private final Map<UUID, List<MemberData>> guildMembers = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerGuilds = new ConcurrentHashMap<>();
    private final Map<UUID, Invitation> invitations = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Double>> zoneInfluence = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> bankBalances = new ConcurrentHashMap<>();
    private final Map<UUID, List<TransactionData>> transactions = new ConcurrentHashMap<>();
    
    // Optional storage accessor for persistence
    private HytaleStorageAccessor storageAccessor;
    
    // JSON serialization
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    private record Invitation(UUID guildId, UUID inviterId, UUID inviteeId, long expiresAt) {}
    
    // Persistence data wrappers for clean serialization
    private record PersistedGuildData(
        String id, String name, String tag, String founderId,
        String motd, long createdAt, int level, String description,
        String recruitmentStatus, String emblemShape, String emblemIcon,
        String emblemPrimaryColor, String emblemSecondaryColor
    ) {
        static PersistedGuildData from(GuildData g) {
            return new PersistedGuildData(
                g.id().toString(), g.name(), g.tag(), g.founderId().toString(),
                g.motd(), g.createdAt(), g.level(), g.description(),
                g.recruitmentStatus(), g.emblemShape(), g.emblemIcon(),
                g.emblemPrimaryColor(), g.emblemSecondaryColor()
            );
        }
        
        GuildData toGuildData() {
            return new GuildData(
                UUID.fromString(id), name, tag, UUID.fromString(founderId),
                motd, createdAt, level, description,
                recruitmentStatus, emblemShape, emblemIcon,
                emblemPrimaryColor, emblemSecondaryColor
            );
        }
    }
    
    private record PersistedMemberData(
        String guildId, String playerId, String rank, long joinedAt,
        long lastActiveAt, String playerName, boolean isOnline, int level, long contribution
    ) {
        static PersistedMemberData from(MemberData m) {
            return new PersistedMemberData(
                m.guildId().toString(), m.playerId().toString(), m.rank().name(),
                m.joinedAt(), m.lastActiveAt(), m.playerName(), m.isOnline(),
                m.level(), m.contribution()
            );
        }
        
        MemberData toMemberData() {
            return new MemberData(
                UUID.fromString(guildId), UUID.fromString(playerId),
                GuildRank.valueOf(rank), joinedAt, lastActiveAt,
                playerName, isOnline, level, contribution
            );
        }
    }
    
    public HytaleGuildAccessor() {
        LOGGER.info("HytaleGuildAccessor initialized (in-memory mode)");
    }
    
    /**
     * Enables persistence by providing a storage accessor.
     * Also triggers loading of persisted data.
     */
    public void setStorageAccessor(HytaleStorageAccessor storageAccessor) {
        this.storageAccessor = storageAccessor;
        LOGGER.info("HytaleGuildAccessor persistence enabled via StorageAccessor");
        loadAllFromStorage();
    }
    
    // ==================== Persistence Methods ====================
    
    private void loadAllFromStorage() {
        if (storageAccessor == null) return;
        
        LOGGER.info("Loading guild data from persistent storage...");
        
        try {
            // Load all guilds
            storageAccessor.findKeysAsync(GUILD_NAMESPACE, "").thenAccept(keys -> {
                for (String key : keys) {
                    loadGuildFromStorage(UUID.fromString(key));
                }
                LOGGER.info("Loaded {} guilds from storage", guilds.size());
            }).exceptionally(ex -> {
                LOGGER.error("Failed to load guilds from storage", ex);
                return null;
            });
            
            // Load player -> guild mappings
            storageAccessor.findKeysAsync(PLAYER_GUILDS_NAMESPACE, "").thenAccept(keys -> {
                for (String playerId : keys) {
                    storageAccessor.loadAsync(PLAYER_GUILDS_NAMESPACE, playerId, s -> s)
                        .thenAccept(opt -> opt.ifPresent(guildId -> 
                            playerGuilds.put(UUID.fromString(playerId), UUID.fromString(guildId))
                        ));
                }
                LOGGER.info("Loaded {} player-guild mappings from storage", playerGuilds.size());
            }).exceptionally(ex -> {
                LOGGER.error("Failed to load player-guild mappings from storage", ex);
                return null;
            });
            
        } catch (Exception e) {
            LOGGER.error("Error during storage load", e);
        }
    }
    
    private void loadGuildFromStorage(UUID guildId) {
        if (storageAccessor == null) return;
        
        String key = guildId.toString();
        
        // Load guild data
        storageAccessor.loadAsync(GUILD_NAMESPACE, key, json -> GSON.fromJson(json, PersistedGuildData.class))
            .thenAccept(opt -> opt.ifPresent(persisted -> {
                guilds.put(guildId, persisted.toGuildData());
                
                // Load members for this guild
                loadMembersFromStorage(guildId);
            }))
            .exceptionally(ex -> {
                LOGGER.error("Failed to load guild {}", guildId, ex);
                return null;
            });
    }
    
    private void loadMembersFromStorage(UUID guildId) {
        if (storageAccessor == null) return;
        
        String key = guildId.toString();
        Type listType = new TypeToken<List<PersistedMemberData>>(){}.getType();
        
        storageAccessor.loadAsync(MEMBERS_NAMESPACE, key, json -> {
            List<PersistedMemberData> persisted = GSON.fromJson(json, listType);
            return persisted;
        }).thenAccept(opt -> opt.ifPresent(persistedList -> {
            List<MemberData> members = new ArrayList<>();
            for (PersistedMemberData pm : persistedList) {
                members.add(pm.toMemberData());
            }
            guildMembers.put(guildId, members);
        })).exceptionally(ex -> {
            LOGGER.error("Failed to load members for guild {}", guildId, ex);
            return null;
        });
    }
    
    private void saveGuildToStorage(UUID guildId) {
        if (storageAccessor == null) return;
        
        GuildData guild = guilds.get(guildId);
        if (guild == null) return;
        
        String key = guildId.toString();
        PersistedGuildData persisted = PersistedGuildData.from(guild);
        
        storageAccessor.saveAsync(GUILD_NAMESPACE, key, persisted, GSON::toJson)
            .exceptionally(ex -> {
                LOGGER.error("Failed to save guild {}", guildId, ex);
                return null;
            });
    }
    
    private void deleteGuildFromStorage(UUID guildId) {
        if (storageAccessor == null) return;
        
        String key = guildId.toString();
        
        storageAccessor.deleteAsync(GUILD_NAMESPACE, key);
        storageAccessor.deleteAsync(MEMBERS_NAMESPACE, key);
    }
    
    private void saveMembersToStorage(UUID guildId) {
        if (storageAccessor == null) return;
        
        List<MemberData> members = guildMembers.get(guildId);
        if (members == null) return;
        
        String key = guildId.toString();
        List<PersistedMemberData> persisted = members.stream()
            .map(PersistedMemberData::from)
            .toList();
        
        storageAccessor.saveAsync(MEMBERS_NAMESPACE, key, persisted, GSON::toJson)
            .exceptionally(ex -> {
                LOGGER.error("Failed to save members for guild {}", guildId, ex);
                return null;
            });
    }
    
    private void savePlayerGuildMapping(UUID playerId, UUID guildId) {
        if (storageAccessor == null) return;
        
        storageAccessor.saveAsync(PLAYER_GUILDS_NAMESPACE, playerId.toString(), guildId.toString(), s -> s)
            .exceptionally(ex -> {
                LOGGER.error("Failed to save player-guild mapping for {}", playerId, ex);
                return null;
            });
    }
    
    private void deletePlayerGuildMapping(UUID playerId) {
        if (storageAccessor == null) return;
        
        storageAccessor.deleteAsync(PLAYER_GUILDS_NAMESPACE, playerId.toString());
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
        
        // Persist to storage
        saveGuildToStorage(guildId);
        
        LOGGER.info("Created guild '{}' [{}] with ID {}", name, tag, guildId);
        return true;
    }

    @Override
    public void addMember(UUID guildId, UUID playerId, GuildRank rank) {
        if (!guilds.containsKey(guildId)) return;

        MemberData member = new MemberData(guildId, playerId, rank, System.currentTimeMillis(), System.currentTimeMillis(), "Player", false, 1, 0L);
        guildMembers.computeIfAbsent(guildId, k -> new ArrayList<>()).add(member);
        playerGuilds.put(playerId, guildId);
        
        // Persist to storage
        saveMembersToStorage(guildId);
        savePlayerGuildMapping(playerId, guildId);
        
        LOGGER.debug("Added player {} to guild {} with rank {}", playerId, guildId, rank);
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
            
            // Persist changes
            saveMembersToStorage(guildId);
            deletePlayerGuildMapping(playerId);
            
            LOGGER.debug("Removed player {} from guild {}", playerId, guildId);
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
                
                // Persist rank change
                saveMembersToStorage(guildId);
                
                LOGGER.debug("Updated rank for player {} in guild {} to {}", playerId, guildId, newRank);
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
                deletePlayerGuildMapping(m.playerId());
            }
        }
        guilds.remove(guildId);
        
        // Remove from persistent storage
        deleteGuildFromStorage(guildId);
        
        LOGGER.info("Deleted guild {}", guildId);
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
            // currency param filters by transaction type (e.g., "GOLD", "SILVER")
            .filter(tx -> currency == null || tx.type().equals(currency))
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
            existing.id(), existing.name(), existing.tag(), existing.founderId(),
            motd, // Updated MOTD
            existing.createdAt(), existing.level(), existing.description(),
            existing.recruitmentStatus(), existing.emblemShape(), existing.emblemIcon(),
            existing.emblemPrimaryColor(), existing.emblemSecondaryColor()
        );
        guilds.put(guildId, updated);
        saveGuildToStorage(guildId);
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
            existing.id(), existing.name(), existing.tag(), existing.founderId(),
            existing.motd(), existing.createdAt(), existing.level(),
            description, // Updated description
            existing.recruitmentStatus(), existing.emblemShape(), existing.emblemIcon(),
            existing.emblemPrimaryColor(), existing.emblemSecondaryColor()
        );
        guilds.put(guildId, updated);
        saveGuildToStorage(guildId);
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
            existing.id(), existing.name(), existing.tag(), existing.founderId(),
            existing.motd(), existing.createdAt(), existing.level(), existing.description(),
            status, // Updated recruitment status
            existing.emblemShape(), existing.emblemIcon(),
            existing.emblemPrimaryColor(), existing.emblemSecondaryColor()
        );
        guilds.put(guildId, updated);
        saveGuildToStorage(guildId);
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
            existing.id(), existing.name(), existing.tag(), existing.founderId(),
            existing.motd(), existing.createdAt(), existing.level(), existing.description(),
            existing.recruitmentStatus(),
            shape, icon, primaryColor, secondaryColor // Updated emblem
        );
        guilds.put(guildId, updated);
        saveGuildToStorage(guildId);
        LOGGER.debug("Set emblem for guild {}: shape={}, icon={}, colors={}/{}", 
            guildId, shape, icon, primaryColor, secondaryColor);
    }
}
