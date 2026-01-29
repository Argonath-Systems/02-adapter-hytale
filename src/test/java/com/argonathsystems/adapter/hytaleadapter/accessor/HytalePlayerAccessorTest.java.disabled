package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.adapter.hytaleadapter.converter.LocationConverter;
import com.argonathsystems.adapter.hytaleadapter.util.PlayerRefCache;
import com.argonathsystems.framework.accessorapi.dto.LocationData;
import com.argonathsystems.framework.accessorapi.dto.PlayerData;
import com.hytale.api.Location;
import com.hytale.api.Server;
import com.hytale.api.entity.Player;
import com.hytale.api.world.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for HytalePlayerAccessor.
 * 
 * <p>Verifies:
 * <ul>
 *   <li>Player data retrieval and conversion</li>
 *   <li>Online players collection</li>
 *   <li>Teleportation</li>
 *   <li>Messaging</li>
 *   <li>Health/hunger/experience manipulation</li>
 *   <li>Cache integration</li>
 * </ul>
 * 
 * @author Argonath Systems Team
 * @version 2.0.0
 */
@DisplayName("HytalePlayerAccessor Tests")
class HytalePlayerAccessorTest {

    @Mock
    private Server mockServer;

    @Mock
    private World mockWorld;

    @Mock
    private Player mockPlayer;

    @Mock
    private Location mockLocation;

    private HytalePlayerAccessor accessor;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        accessor = new HytalePlayerAccessor(mockServer);
        playerId = UUID.randomUUID();

        // Setup default mock behavior
        when(mockPlayer.getUniqueId()).thenReturn(playerId);
        when(mockPlayer.getName()).thenReturn("TestPlayer");
        when(mockPlayer.getHealth()).thenReturn(20.0);
        when(mockPlayer.getMaxHealth()).thenReturn(20.0);
        when(mockPlayer.getLocation()).thenReturn(mockLocation);
        
        when(mockLocation.getWorldName()).thenReturn("world");
        when(mockLocation.getX()).thenReturn(0.0);
        when(mockLocation.getY()).thenReturn(64.0);
        when(mockLocation.getZ()).thenReturn(0.0);
        when(mockLocation.getYaw()).thenReturn(0.0f);
        when(mockLocation.getPitch()).thenReturn(0.0f);

        when(mockWorld.getPlayers()).thenReturn(List.of(mockPlayer));
        when(mockServer.getWorlds()).thenReturn(List.of(mockWorld));
        
        // Pre-populate cache
        PlayerRefCache.add(mockPlayer);
    }

    @AfterEach
    void tearDown() {
        PlayerRefCache.clear();
    }

    @Test
    @DisplayName("getPlayer() should return PlayerData when player exists in cache")
    void testGetPlayer_FromCache() {
        // Act
        Optional<PlayerData> result = accessor.getPlayer(playerId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(playerId);
        assertThat(result.get().name()).isEqualTo("TestPlayer");
        assertThat(result.get().health()).isEqualTo(20);
        assertThat(result.get().maxHealth()).isEqualTo(20);
    }

    @Test
    @DisplayName("getPlayer() should return empty when player not found")
    void testGetPlayer_NotFound() {
        // Arrange
        UUID unknownId = UUID.randomUUID();

        // Act
        Optional<PlayerData> result = accessor.getPlayer(unknownId);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getPlayer() should fall back to world scan if not in cache")
    void testGetPlayer_FallbackToWorldScan() {
        // Arrange
        UUID newPlayerId = UUID.randomUUID();
        Player newPlayer = mock(Player.class);
        when(newPlayer.getUniqueId()).thenReturn(newPlayerId);
        when(newPlayer.getName()).thenReturn("NewPlayer");
        when(newPlayer.getHealth()).thenReturn(15.0);
        when(newPlayer.getMaxHealth()).thenReturn(20.0);
        when(newPlayer.getLocation()).thenReturn(mockLocation);
        
        when(mockWorld.getPlayers()).thenReturn(List.of(mockPlayer, newPlayer));

        // Act
        Optional<PlayerData> result = accessor.getPlayer(newPlayerId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(newPlayerId);
        assertThat(result.get().name()).isEqualTo("NewPlayer");
        
        // Verify player was added to cache
        assertThat(PlayerRefCache.get(newPlayerId)).isEqualTo(newPlayer);
    }

    @Test
    @DisplayName("getOnlinePlayers() should return all online players")
    void testGetOnlinePlayers() {
        // Arrange
        Player player2 = mock(Player.class);
        when(player2.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player2.getName()).thenReturn("Player2");
        when(player2.getHealth()).thenReturn(18.0);
        when(player2.getMaxHealth()).thenReturn(20.0);
        when(player2.getLocation()).thenReturn(mockLocation);

        when(mockWorld.getPlayers()).thenReturn(List.of(mockPlayer, player2));

        // Act
        Collection<PlayerData> result = accessor.getOnlinePlayers();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(PlayerData::name)
            .containsExactlyInAnyOrder("TestPlayer", "Player2");
    }

    @Test
    @DisplayName("getOnlinePlayers() should collect from multiple worlds")
    void testGetOnlinePlayers_MultipleWorlds() {
        // Arrange
        World world2 = mock(World.class);
        Player player2 = mock(Player.class);
        when(player2.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player2.getName()).thenReturn("Player2");
        when(player2.getHealth()).thenReturn(20.0);
        when(player2.getMaxHealth()).thenReturn(20.0);
        when(player2.getLocation()).thenReturn(mockLocation);
        
        when(world2.getPlayers()).thenReturn(List.of(player2));
        when(mockServer.getWorlds()).thenReturn(List.of(mockWorld, world2));

        // Act
        Collection<PlayerData> result = accessor.getOnlinePlayers();

        // Assert
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("teleport() should call player.teleport() with converted location")
    void testTeleport() {
        // Arrange
        LocationData destination = new LocationData("world_nether", 100.0, 70.0, 200.0, 90.0f, 0.0f);

        // Act
        accessor.teleport(playerId, destination);

        // Assert
        verify(mockPlayer).teleport(any(Location.class));
    }

    @Test
    @DisplayName("teleport() should handle non-existent player gracefully")
    void testTeleport_PlayerNotFound() {
        // Arrange
        UUID unknownId = UUID.randomUUID();
        LocationData destination = new LocationData("world", 0, 64, 0, 0f, 0f);

        // Act - Should not throw
        assertThatCode(() -> accessor.teleport(unknownId, destination))
            .doesNotThrowAnyException();

        // Assert
        verify(mockPlayer, never()).teleport(any());
    }

    @Test
    @DisplayName("sendMessage() should call player.sendMessage()")
    void testSendMessage() {
        // Act
        accessor.sendMessage(playerId, "Hello, player!");

        // Assert
        verify(mockPlayer).sendMessage("Hello, player!");
    }

    @Test
    @DisplayName("sendMessage() should handle non-existent player gracefully")
    void testSendMessage_PlayerNotFound() {
        // Arrange
        UUID unknownId = UUID.randomUUID();

        // Act - Should not throw
        assertThatCode(() -> accessor.sendMessage(unknownId, "Test"))
            .doesNotThrowAnyException();

        // Assert
        verify(mockPlayer, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("getLocation() should return player's location")
    void testGetLocation() {
        // Act
        Optional<LocationData> result = accessor.getLocation(playerId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().world()).isEqualTo("world");
        assertThat(result.get().x()).isEqualTo(0.0);
        assertThat(result.get().y()).isEqualTo(64.0);
        assertThat(result.get().z()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getLocation() should return empty when player not found")
    void testGetLocation_PlayerNotFound() {
        // Arrange
        UUID unknownId = UUID.randomUUID();

        // Act
        Optional<LocationData> result = accessor.getLocation(unknownId);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("setHealth() should update player health")
    void testSetHealth() {
        // Act
        accessor.setHealth(playerId, 10);

        // Assert
        verify(mockPlayer).setHealth(10);
    }

    @Test
    @DisplayName("setHealth() should handle non-existent player gracefully")
    void testSetHealth_PlayerNotFound() {
        // Arrange
        UUID unknownId = UUID.randomUUID();

        // Act - Should not throw
        assertThatCode(() -> accessor.setHealth(unknownId, 10))
            .doesNotThrowAnyException();

        // Assert
        verify(mockPlayer, never()).setHealth(anyDouble());
    }

    @Test
    @DisplayName("getHealth() should return player's current health")
    void testGetHealth() {
        // Act
        int result = accessor.getHealth(playerId);

        // Assert
        assertThat(result).isEqualTo(20);
    }

    @Test
    @DisplayName("getHealth() should return 0 when player not found")
    void testGetHealth_PlayerNotFound() {
        // Arrange
        UUID unknownId = UUID.randomUUID();

        // Act
        int result = accessor.getHealth(unknownId);

        // Assert
        assertThat(result).isEqualTo(0);
    }

    @Test
    @DisplayName("HytalePlayerEntity wrapper should implement PlatformEntity correctly")
    void testHytalePlayerEntity_Wrapper() {
        // Arrange
        HytalePlayerAccessor.HytalePlayerEntity wrapper = 
            new HytalePlayerAccessor.HytalePlayerEntity(mockPlayer);

        // Assert
        assertThat(wrapper.getEntityId()).isEqualTo(playerId);
        assertThat(wrapper.player()).isEqualTo(mockPlayer);
    }

    @Test
    @DisplayName("getOnlinePlayers() should return empty collection when no players online")
    void testGetOnlinePlayers_Empty() {
        // Arrange
        when(mockWorld.getPlayers()).thenReturn(List.of());

        // Act
        Collection<PlayerData> result = accessor.getOnlinePlayers();

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("PlayerRefCache integration should improve lookup performance")
    void testPlayerRefCache_Integration() {
        // Arrange - Clear cache to force world scan first time
        PlayerRefCache.clear();

        // Act - First call should scan worlds
        Optional<PlayerData> result1 = accessor.getPlayer(playerId);
        
        // Assert - Player found and added to cache
        assertThat(result1).isPresent();
        assertThat(PlayerRefCache.get(playerId)).isEqualTo(mockPlayer);

        // Act - Second call should use cache (no world scan)
        Optional<PlayerData> result2 = accessor.getPlayer(playerId);

        // Assert - Same result
        assertThat(result2).isPresent();
        assertThat(result2.get().id()).isEqualTo(result1.get().id());
    }
}
