package com.argonathsystems.adapter.hytaleadapter.converter;

import com.argonathsystems.framework.accessorapi.dto.LocationData;
import com.hytale.api.Location;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for LocationConverter.
 * 
 * <p>Verifies:
 * <ul>
 *   <li>Hytale Location → LocationData DTO conversion</li>
 *   <li>LocationData DTO → Hytale Location conversion</li>
 *   <li>Null safety</li>
 *   <li>Bidirectional conversion accuracy</li>
 * </ul>
 * 
 * @author Argonath Systems Team
 * @version 2.0.0
 */
@DisplayName("LocationConverter Tests")
class LocationConverterTest {

    @Test
    @DisplayName("toDTO() should convert Location to LocationData")
    void testToDTO_BasicConversion() {
        // Arrange
        Location hytaleLocation = new Location("world", 100.5, 64.0, 200.5, 180.0f, 45.0f);

        // Act
        LocationData result = LocationConverter.toDTO(hytaleLocation);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.world()).isEqualTo("world");
        assertThat(result.x()).isEqualTo(100.5);
        assertThat(result.y()).isEqualTo(64.0);
        assertThat(result.z()).isEqualTo(200.5);
        assertThat(result.yaw()).isEqualTo(180.0f);
        assertThat(result.pitch()).isEqualTo(45.0f);
    }

    @Test
    @DisplayName("toDTO() should handle null input gracefully")
    void testToDTO_NullInput() {
        // Act
        LocationData result = LocationConverter.toDTO(null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toDTO() should handle spawn location (0,0,0)")
    void testToDTO_SpawnLocation() {
        // Arrange
        Location spawn = new Location("world", 0.0, 0.0, 0.0, 0.0f, 0.0f);

        // Act
        LocationData result = LocationConverter.toDTO(spawn);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.x()).isEqualTo(0.0);
        assertThat(result.y()).isEqualTo(0.0);
        assertThat(result.z()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("toDTO() should handle negative coordinates")
    void testToDTO_NegativeCoordinates() {
        // Arrange
        Location location = new Location("world", -500.5, 10.0, -300.75, 270.0f, -30.0f);

        // Act
        LocationData result = LocationConverter.toDTO(location);

        // Assert
        assertThat(result.x()).isEqualTo(-500.5);
        assertThat(result.z()).isEqualTo(-300.75);
        assertThat(result.yaw()).isEqualTo(270.0f);
        assertThat(result.pitch()).isEqualTo(-30.0f);
    }

    @Test
    @DisplayName("toDTO() should handle different world names")
    void testToDTO_DifferentWorlds() {
        // Arrange
        Location nether = new Location("world_nether", 50.0, 64.0, 100.0, 0.0f, 0.0f);
        Location end = new Location("world_the_end", 0.0, 48.0, 0.0, 0.0f, 0.0f);

        // Act
        LocationData netherData = LocationConverter.toDTO(nether);
        LocationData endData = LocationConverter.toDTO(end);

        // Assert
        assertThat(netherData.world()).isEqualTo("world_nether");
        assertThat(endData.world()).isEqualTo("world_the_end");
    }

    @Test
    @DisplayName("fromDTO() should convert LocationData to Location")
    void testFromDTO_BasicConversion() {
        // Arrange
        LocationData dto = new LocationData("world", 100.5, 64.0, 200.5, 180.0f, 45.0f);

        // Act
        Location result = LocationConverter.fromDTO(dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getWorldName()).isEqualTo("world");
        assertThat(result.getX()).isEqualTo(100.5);
        assertThat(result.getY()).isEqualTo(64.0);
        assertThat(result.getZ()).isEqualTo(200.5);
        assertThat(result.getYaw()).isEqualTo(180.0f);
        assertThat(result.getPitch()).isEqualTo(45.0f);
    }

    @Test
    @DisplayName("fromDTO() should handle null input gracefully")
    void testFromDTO_NullInput() {
        // Act
        Location result = LocationConverter.fromDTO(null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Bidirectional conversion should preserve all data")
    void testBidirectionalConversion_ToAndFrom() {
        // Arrange
        Location original = new Location("orbis", 123.456, 78.9, 987.654, 90.5f, -15.25f);

        // Act
        LocationData dto = LocationConverter.toDTO(original);
        Location reconstructed = LocationConverter.fromDTO(dto);

        // Assert
        assertThat(reconstructed.getWorldName()).isEqualTo(original.getWorldName());
        assertThat(reconstructed.getX()).isEqualTo(original.getX());
        assertThat(reconstructed.getY()).isEqualTo(original.getY());
        assertThat(reconstructed.getZ()).isEqualTo(original.getZ());
        assertThat(reconstructed.getYaw()).isEqualTo(original.getYaw());
        assertThat(reconstructed.getPitch()).isEqualTo(original.getPitch());
    }

    @Test
    @DisplayName("Bidirectional conversion should preserve all data (reverse)")
    void testBidirectionalConversion_FromAndTo() {
        // Arrange
        LocationData original = new LocationData("custom_world", -99.99, 256.0, 0.01, 359.9f, 89.5f);

        // Act
        Location hytaleLocation = LocationConverter.fromDTO(original);
        LocationData reconstructed = LocationConverter.toDTO(hytaleLocation);

        // Assert
        assertThat(reconstructed.world()).isEqualTo(original.world());
        assertThat(reconstructed.x()).isEqualTo(original.x());
        assertThat(reconstructed.y()).isEqualTo(original.y());
        assertThat(reconstructed.z()).isEqualTo(original.z());
        assertThat(reconstructed.yaw()).isEqualTo(original.yaw());
        assertThat(reconstructed.pitch()).isEqualTo(original.pitch());
    }

    @Test
    @DisplayName("Should handle extreme coordinate values")
    void testExtremeCoordinates() {
        // Arrange
        Location extreme = new Location(
            "world",
            Double.MAX_VALUE / 2,
            -Double.MAX_VALUE / 2,
            Double.MIN_VALUE,
            360.0f,
            -90.0f
        );

        // Act
        LocationData dto = LocationConverter.toDTO(extreme);
        Location reconstructed = LocationConverter.fromDTO(dto);

        // Assert
        assertThat(reconstructed.getX()).isEqualTo(extreme.getX());
        assertThat(reconstructed.getY()).isEqualTo(extreme.getY());
        assertThat(reconstructed.getZ()).isEqualTo(extreme.getZ());
    }

    @Test
    @DisplayName("Should handle high precision decimal coordinates")
    void testHighPrecisionCoordinates() {
        // Arrange
        Location precise = new Location(
            "world",
            100.123456789,
            64.987654321,
            200.555666777,
            123.456f,
            -78.901f
        );

        // Act
        LocationData dto = LocationConverter.toDTO(precise);
        Location reconstructed = LocationConverter.fromDTO(dto);

        // Assert - Verify precision is preserved
        assertThat(reconstructed.getX()).isEqualTo(100.123456789);
        assertThat(reconstructed.getY()).isEqualTo(64.987654321);
        assertThat(reconstructed.getZ()).isEqualTo(200.555666777);
        assertThat(reconstructed.getYaw()).isEqualTo(123.456f);
        assertThat(reconstructed.getPitch()).isEqualTo(-78.901f);
    }

    @Test
    @DisplayName("Multiple conversions should be idempotent")
    void testMultipleConversions_Idempotent() {
        // Arrange
        Location original = new Location("world", 50.0, 100.0, 150.0, 45.0f, 30.0f);

        // Act - Convert multiple times
        LocationData dto1 = LocationConverter.toDTO(original);
        Location loc1 = LocationConverter.fromDTO(dto1);
        LocationData dto2 = LocationConverter.toDTO(loc1);
        Location loc2 = LocationConverter.fromDTO(dto2);
        LocationData dto3 = LocationConverter.toDTO(loc2);

        // Assert - All conversions should be identical
        assertThat(dto1).isEqualTo(dto2);
        assertThat(dto2).isEqualTo(dto3);
        assertThat(loc1.getX()).isEqualTo(loc2.getX());
        assertThat(loc1.getY()).isEqualTo(loc2.getY());
        assertThat(loc1.getZ()).isEqualTo(loc2.getZ());
    }

    @Test
    @DisplayName("Should handle world names with special characters")
    void testWorldNamesWithSpecialCharacters() {
        // Arrange
        Location specialWorld1 = new Location("world_123-test.v2", 0, 0, 0, 0f, 0f);
        Location specialWorld2 = new Location("Orbis:Terra:Main", 0, 0, 0, 0f, 0f);

        // Act
        LocationData dto1 = LocationConverter.toDTO(specialWorld1);
        LocationData dto2 = LocationConverter.toDTO(specialWorld2);

        // Assert
        assertThat(dto1.world()).isEqualTo("world_123-test.v2");
        assertThat(dto2.world()).isEqualTo("Orbis:Terra:Main");
    }
}
