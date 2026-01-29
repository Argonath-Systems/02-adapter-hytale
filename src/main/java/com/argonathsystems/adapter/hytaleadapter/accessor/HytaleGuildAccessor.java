package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.GuildAccessor;
import com.argonathsystems.framework.accessorapi.GuildData;
import com.argonathsystems.framework.accessorapi.GuildRank;
import com.argonathsystems.framework.accessorapi.MemberData;
import com.argonathsystems.framework.accessorapi.TransactionData;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class HytaleGuildAccessor implements GuildAccessor {

    private final Map<UUID, GuildData> guilds = new ConcurrentHashMap<>();
    private final Map<UUID, List<MemberData>> guildMembers = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerGuilds = new ConcurrentHashMap<>();
    private final Map<UUID, Invitation> invitations = new ConcurrentHashMap<>();

    private record Invitation(UUID guildId, UUID inviterId, UUID inviteeId, long expiresAt) {}

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
        return 0;
    }

    @Override
    public boolean deposit(UUID guildId, String currency, UUID playerId, long amount, String reason) {
        return true;
    }

    @Override
    public boolean withdraw(UUID guildId, String currency, UUID playerId, long amount, String reason) {
        return true;
    }

    @Override
    public List<TransactionData> getRecentTransactions(UUID guildId, String currency, int limit) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getControlledZones(UUID guildId) {
        return Collections.emptyList();
    }

    @Override
    public boolean claimZone(UUID guildId, String zoneId) {
        return true;
    }

    @Override
    public boolean releaseZone(UUID guildId, String zoneId) {
        return true;
    }

    @Override
    public double getZoneInfluence(UUID guildId, String zoneId) {
        // In-memory implementation returns 0.0 for untracked zones
        return 0.0;
    }

    @Override
    public void addInfluence(UUID guildId, String zoneId, double amount) {
        // MIGRATION-001: Zone influence tracking requires persistent storage
        throw new UnsupportedOperationException(
            "HytaleGuildAccessor.addInfluence() requires persistent zone influence tracking. " +
            "In-memory implementation does not support zone influence. " +
            "See MIGRATION-001 for SDK integration requirements."
        );
    }

    @Override
    public void createRank(UUID guildId, String name, int level, Set<String> permissions) {
        // MIGRATION-001: Custom rank creation requires persistent storage
        throw new UnsupportedOperationException(
            "HytaleGuildAccessor.createRank() requires persistent guild rank storage. " +
            "In-memory implementation uses predefined GuildRank enum. " +
            "See MIGRATION-001 for SDK integration requirements."
        );
    }

    @Override
    public void deleteRank(UUID guildId, UUID rankId) {
        // MIGRATION-001: Custom rank deletion requires persistent storage
        throw new UnsupportedOperationException(
            "HytaleGuildAccessor.deleteRank() requires persistent guild rank storage. " +
            "In-memory implementation uses predefined GuildRank enum. " +
            "See MIGRATION-001 for SDK integration requirements."
        );
    }

    @Override
    public void updateRank(UUID guildId, UUID rankId, String name, int level, Set<String> permissions) {
        // MIGRATION-001: Custom rank updates require persistent storage
        throw new UnsupportedOperationException(
            "HytaleGuildAccessor.updateRank() requires persistent guild rank storage. " +
            "In-memory implementation uses predefined GuildRank enum. " +
            "See MIGRATION-001 for SDK integration requirements."
        );
    }

    @Override
    public Object getRank(UUID guildId, UUID rankId) {
        // MIGRATION-001: Custom rank retrieval requires persistent storage
        throw new UnsupportedOperationException(
            "HytaleGuildAccessor.getRank() requires persistent guild rank storage. " +
            "In-memory implementation uses predefined GuildRank enum. " +
            "See MIGRATION-001 for SDK integration requirements."
        );
    }

    @Override
    public List<Object> getRanks(UUID guildId) {
        // Return predefined ranks as a list - this is valid for in-memory impl
        return Arrays.asList(GuildRank.values());
    }

    @Override
    public void setGuildMotd(UUID guildId, String motd) {
        // MIGRATION-001: Guild MOTD updates require persistent storage
        throw new UnsupportedOperationException(
            "HytaleGuildAccessor.setGuildMotd() requires persistent guild storage. " +
            "In-memory implementation does not support MOTD updates. " +
            "See MIGRATION-001 for SDK integration requirements."
        );
    }

    @Override
    public void setGuildDescription(UUID guildId, String description) {
        // MIGRATION-001: Guild description updates require persistent storage
        throw new UnsupportedOperationException(
            "HytaleGuildAccessor.setGuildDescription() requires persistent guild storage. " +
            "In-memory implementation does not support description updates. " +
            "See MIGRATION-001 for SDK integration requirements."
        );
    }

    @Override
    public void setRecruitmentStatus(UUID guildId, String status) {
        // MIGRATION-001: Recruitment status updates require persistent storage
        throw new UnsupportedOperationException(
            "HytaleGuildAccessor.setRecruitmentStatus() requires persistent guild storage. " +
            "In-memory implementation does not support recruitment status updates. " +
            "See MIGRATION-001 for SDK integration requirements."
        );
    }

    @Override
    public void setGuildEmblem(UUID guildId, String shape, String icon, String primaryColor, String secondaryColor) {
        // MIGRATION-001: Guild emblem updates require persistent storage
        throw new UnsupportedOperationException(
            "HytaleGuildAccessor.setGuildEmblem() requires persistent guild storage. " +
            "In-memory implementation does not support emblem customization. " +
            "See MIGRATION-001 for SDK integration requirements."
        );
    }
}
