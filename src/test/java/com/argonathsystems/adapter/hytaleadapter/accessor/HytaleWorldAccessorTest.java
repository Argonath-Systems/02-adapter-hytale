package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.dto.LocationData;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HytaleWorldAccessor.
 * 
 * <p>Tests the SDK integration patterns for world/block operations using mocked
 * Hytale SDK components.</p>
 * 
 * @author Argonath Systems Team
 * @since Phase 5 Implementation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HytaleWorldAccessor Tests")
class HytaleWorldAccessorTest {

    @Mock
    private HytaleServer mockServer;

    @Mock
    private World mockWorld;

    @Mock
    private WorldChunk mockChunk;

    private HytaleWorldAccessor accessor;

    @BeforeEach
    void setUp() {
        accessor = new HytaleWorldAccessor(mockServer);
    }

    // ========================================================================
    // Batch 5.5: Block Operations Tests
    // ========================================================================

    @Nested
    @DisplayName("Block Operations")
    class BlockOperationsTests {

        @Test
        @DisplayName("getBlockType() should return air for null location")
        void getBlockType_shouldReturnAirForNullLocation() {
            // When
            String result = accessor.getBlockType(null);

            // Then
            assertThat(result).isEqualTo("minecraft:air");
        }

        @Test
        @DisplayName("getBlockType() should return air when world not found")
        void getBlockType_shouldReturnAirWhenWorldNotFound() {
            // Given
            LocationData location = new LocationData("nonexistent", 0, 64, 0, 0, 0);

            try (MockedStatic<Universe> universeMock = mockStatic(Universe.class)) {
                Universe mockUniverse = mock(Universe.class);
                universeMock.when(Universe::get).thenReturn(mockUniverse);
                when(mockUniverse.getWorld("nonexistent")).thenReturn(null);
                when(mockUniverse.getDefaultWorld()).thenReturn(null);

                // When
                String result = accessor.getBlockType(location);

                // Then
                assertThat(result).isEqualTo("minecraft:air");
            }
        }

        @Test
        @DisplayName("getBlockType() should return air when chunk not loaded")
        void getBlockType_shouldReturnAirWhenChunkNotLoaded() {
            // Given
            LocationData location = new LocationData("world", 100, 64, 200, 0, 0);

            try (MockedStatic<Universe> universeMock = mockStatic(Universe.class)) {
                Universe mockUniverse = mock(Universe.class);
                universeMock.when(Universe::get).thenReturn(mockUniverse);
                when(mockUniverse.getWorld("world")).thenReturn(mockWorld);
                when(mockWorld.getChunkIfLoaded(anyLong())).thenReturn(null);

                // When
                String result = accessor.getBlockType(location);

                // Then
                assertThat(result).isEqualTo("minecraft:air");
            }
        }

        @Test
        @DisplayName("getBlockType() should return block ID when chunk is loaded")
        void getBlockType_shouldReturnBlockIdWhenChunkLoaded() {
            // Given
            LocationData location = new LocationData("world", 100, 64, 200, 0, 0);
            int expectedBlockId = 42;

            try (MockedStatic<Universe> universeMock = mockStatic(Universe.class)) {
                Universe mockUniverse = mock(Universe.class);
                universeMock.when(Universe::get).thenReturn(mockUniverse);
                when(mockUniverse.getWorld("world")).thenReturn(mockWorld);
                when(mockWorld.getChunkIfLoaded(anyLong())).thenReturn(mockChunk);
                
                // Local coords: 100 & 15 = 4, 200 & 15 = 8
                when(mockChunk.getBlock(4, 64, 8)).thenReturn(expectedBlockId);

                // When
                String result = accessor.getBlockType(location);

                // Then
                assertThat(result).isEqualTo("block:" + expectedBlockId);
            }
        }

        @Test
        @DisplayName("getBlockType() should calculate correct local coordinates")
        void getBlockType_shouldCalculateCorrectLocalCoordinates() {
            // Given - coords that cross chunk boundaries
            LocationData location = new LocationData("world", 31, 128, 47, 0, 0);
            // 31 & 15 = 15, 47 & 15 = 15

            try (MockedStatic<Universe> universeMock = mockStatic(Universe.class)) {
                Universe mockUniverse = mock(Universe.class);
                universeMock.when(Universe::get).thenReturn(mockUniverse);
                when(mockUniverse.getWorld("world")).thenReturn(mockWorld);
                when(mockWorld.getChunkIfLoaded(anyLong())).thenReturn(mockChunk);
                when(mockChunk.getBlock(15, 128, 15)).thenReturn(100);

                // When
                String result = accessor.getBlockType(location);

                // Then
                verify(mockChunk).getBlock(15, 128, 15);
                assertThat(result).isEqualTo("block:100");
            }
        }

        @Test
        @DisplayName("getBlockType() should handle negative coordinates")
        void getBlockType_shouldHandleNegativeCoordinates() {
            // Given
            LocationData location = new LocationData("world", -10, 64, -20, 0, 0);
            // -10 & 15 = 6 (Java behavior for negative numbers with bitwise AND)
            // Actually: -10 in binary with & 15 = 6

            try (MockedStatic<Universe> universeMock = mockStatic(Universe.class)) {
                Universe mockUniverse = mock(Universe.class);
                universeMock.when(Universe::get).thenReturn(mockUniverse);
                when(mockUniverse.getWorld("world")).thenReturn(mockWorld);
                when(mockWorld.getChunkIfLoaded(anyLong())).thenReturn(mockChunk);
                when(mockChunk.getBlock(anyInt(), anyInt(), anyInt())).thenReturn(55);

                // When
                String result = accessor.getBlockType(location);

                // Then
                assertThat(result).startsWith("block:");
            }
        }

        @Test
        @DisplayName("setBlock() should not throw for null world")
        void setBlock_shouldNotThrowForNullWorld() {
            // Given
            try (MockedStatic<Universe> universeMock = mockStatic(Universe.class)) {
                Universe mockUniverse = mock(Universe.class);
                universeMock.when(Universe::get).thenReturn(mockUniverse);
                when(mockUniverse.getDefaultWorld()).thenReturn(null);

                // When & Then - should not throw
                accessor.setBlock(null, 0, 64, 0, "block:1");
            }
        }

        @Test
        @DisplayName("setBlock() should not throw for null blockId")
        void setBlock_shouldNotThrowForNullBlockId() {
            // Given
            try (MockedStatic<Universe> universeMock = mockStatic(Universe.class)) {
                Universe mockUniverse = mock(Universe.class);
                universeMock.when(Universe::get).thenReturn(mockUniverse);
                when(mockUniverse.getDefaultWorld()).thenReturn(mockWorld);

                // When & Then - should not throw
                accessor.setBlock(mockWorld, 0, 64, 0, null);
            }
        }

        @Test
        @DisplayName("setBlock() should execute on world thread")
        void setBlock_shouldExecuteOnWorldThread() {
            // Given
            try (MockedStatic<Universe> universeMock = mockStatic(Universe.class)) {
                Universe mockUniverse = mock(Universe.class);
                universeMock.when(Universe::get).thenReturn(mockUniverse);
                when(mockUniverse.getDefaultWorld()).thenReturn(mockWorld);
                
                doNothing().when(mockWorld).execute(any(Runnable.class));

                // When
                accessor.setBlock(mockWorld, 100, 64, 200, "block:1");

                // Then
                verify(mockWorld).execute(any(Runnable.class));
            }
        }

        @Test
        @DisplayName("setBlock() should parse numeric block ID")
        void setBlock_shouldParseNumericBlockId() {
            // Given
            try (MockedStatic<Universe> universeMock = mockStatic(Universe.class)) {
                Universe mockUniverse = mock(Universe.class);
                universeMock.when(Universe::get).thenReturn(mockUniverse);
                when(mockUniverse.getDefaultWorld()).thenReturn(mockWorld);
                
                doAnswer(invocation -> {
                    Runnable runnable = invocation.getArgument(0);
                    runnable.run();
                    return null;
                }).when(mockWorld).execute(any(Runnable.class));
                
                when(mockWorld.getChunkIfLoaded(anyLong())).thenReturn(mockChunk);
                when(mockChunk.setBlock(anyInt(), anyInt(), anyInt(), anyInt(), any(), anyInt(), anyInt(), anyInt()))
                    .thenReturn(true);

                // When
                accessor.setBlock(mockWorld, 4, 64, 8, "block:42");

                // Then
                verify(mockChunk).setBlock(eq(4), eq(64), eq(8), eq(42), any(), eq(0), eq(0), eq(0));
            }
        }

        @Test
        @DisplayName("setBlock() should handle string-only block ID format")
        void setBlock_shouldHandleStringOnlyBlockId() {
            // Given
            try (MockedStatic<Universe> universeMock = mockStatic(Universe.class)) {
                Universe mockUniverse = mock(Universe.class);
                universeMock.when(Universe::get).thenReturn(mockUniverse);
                when(mockUniverse.getDefaultWorld()).thenReturn(mockWorld);
                
                doAnswer(invocation -> {
                    Runnable runnable = invocation.getArgument(0);
                    runnable.run();
                    return null;
                }).when(mockWorld).execute(any(Runnable.class));
                
                when(mockWorld.getChunkIfLoaded(anyLong())).thenReturn(mockChunk);
                when(mockChunk.setBlock(anyInt(), anyInt(), anyInt(), anyInt(), any(), anyInt(), anyInt(), anyInt()))
                    .thenReturn(true);

                // When - named block defaults to 0 (air)
                accessor.setBlock(mockWorld, 4, 64, 8, "minecraft:stone");

                // Then - should set block ID 0 as fallback
                verify(mockChunk).setBlock(eq(4), eq(64), eq(8), eq(0), any(), eq(0), eq(0), eq(0));
            }
        }

        @Test
        @DisplayName("setBlock() should accept World object directly")
        void setBlock_shouldAcceptWorldObject() {
            // Given
            try (MockedStatic<Universe> universeMock = mockStatic(Universe.class)) {
                doAnswer(invocation -> {
                    Runnable runnable = invocation.getArgument(0);
                    runnable.run();
                    return null;
                }).when(mockWorld).execute(any(Runnable.class));
                
                when(mockWorld.getChunkIfLoaded(anyLong())).thenReturn(mockChunk);

                // When
                accessor.setBlock(mockWorld, 0, 64, 0, "123");

                // Then
                verify(mockWorld).execute(any(Runnable.class));
            }
        }

        @Test
        @DisplayName("setBlock() should accept world name as String")
        void setBlock_shouldAcceptWorldNameString() {
            // Given
            try (MockedStatic<Universe> universeMock = mockStatic(Universe.class)) {
                Universe mockUniverse = mock(Universe.class);
                universeMock.when(Universe::get).thenReturn(mockUniverse);
                when(mockUniverse.getWorld("test_world")).thenReturn(mockWorld);
                
                doNothing().when(mockWorld).execute(any(Runnable.class));

                // When
                accessor.setBlock("test_world", 0, 64, 0, "block:1");

                // Then
                verify(mockUniverse).getWorld("test_world");
                verify(mockWorld).execute(any(Runnable.class));
            }
        }
    }

    // ========================================================================
    // Chunk Key Calculation Tests
    // ========================================================================

    @Nested
    @DisplayName("Chunk Key Calculation")
    class ChunkKeyCalculationTests {

        @Test
        @DisplayName("should calculate correct chunk key for positive coordinates")
        void shouldCalculateCorrectChunkKeyForPositiveCoords() {
            // Given
            LocationData location = new LocationData("world", 32, 64, 48, 0, 0);
            // chunkX = 32 >> 4 = 2
            // chunkZ = 48 >> 4 = 3
            // Expected key = (2L << 32) | (3 & 0xFFFFFFFFL)

            try (MockedStatic<Universe> universeMock = mockStatic(Universe.class)) {
                Universe mockUniverse = mock(Universe.class);
                universeMock.when(Universe::get).thenReturn(mockUniverse);
                when(mockUniverse.getWorld("world")).thenReturn(mockWorld);
                when(mockWorld.getChunkIfLoaded(anyLong())).thenReturn(null);

                // When
                accessor.getBlockType(location);

                // Then
                long expectedKey = ((long) 2 << 32) | (3 & 0xFFFFFFFFL);
                verify(mockWorld).getChunkIfLoaded(expectedKey);
            }
        }
    }
}
