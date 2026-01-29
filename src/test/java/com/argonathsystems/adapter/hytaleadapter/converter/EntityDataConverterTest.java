package com.argonathsystems.adapter.hytaleadapter.converter;

import com.argonathsystems.framework.accessorapi.dto.EntityData;
import com.argonathsystems.framework.accessorapi.dto.LocationData;
import com.hytale.api.Location;
import com.hytale.api.entity.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for EntityDataConverter.
 * 
 * <p>Verifies:
 * <ul>
 *   <li>Hytale Entity → EntityData DTO conversion</li>
 *   <li>EntityData DTO application to existing entities</li>
 *   <li>Null safety</li>
 *   <li>ECS component access patterns</li>
 * </ul>
 * 
 * @author Argonath Systems Team
 * @version 2.0.0
 */
@DisplayName("EntityDataConverter Tests")
class EntityDataConverterTest {

    @Mock
    private Entity mockEntity;

    @Mock
    private Location mockLocation;

    private UUID entityId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        entityId = UUID.randomUUID();
        
        // Setup default mock behavior
        when(mockEntity.getUniqueId()).thenReturn(entityId);
        when(mockEntity.getType()).thenReturn("minecraft:zombie");
        when(mockEntity.getName()).thenReturn("Zombie");
        when(mockEntity.getLocation()).thenReturn(mockLocation);
        when(mockEntity.getHealth()).thenReturn(20.0);
        when(mockEntity.getMaxHealth()).thenReturn(20.0);
        
        when(mockLocation.getWorldName()).thenReturn("world");
        when(mockLocation.getX()).thenReturn(100.5);
        when(mockLocation.getY()).thenReturn(64.0);
        when(mockLocation.getZ()).thenReturn(200.5);
        when(mockLocation.getYaw()).thenReturn(180.0f);
        when(mockLocation.getPitch()).thenReturn(0.0f);
    }

    @Test
    @DisplayName("toDTO() should convert basic Entity to EntityData")
    void testToDTO_BasicConversion() {
        // Act
        EntityData result = EntityDataConverter.toDTO(mockEntity);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(entityId);
        assertThat(result.type()).isEqualTo("minecraft:zombie");
        assertThat(result.customName()).isEqualTo("Zombie");
        assertThat(result.health()).isEqualTo(20);
        assertThat(result.maxHealth()).isEqualTo(20);
        assertThat(result.isDead()).isFalse();
        assertThat(result.healthPercent()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("toDTO() should handle null input gracefully")
    void testToDTO_NullInput() {
        // Act
        EntityData result = EntityDataConverter.toDTO(null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toDTO() should convert location data correctly")
    void testToDTO_LocationConversion() {
        // Act
        EntityData result = EntityDataConverter.toDTO(mockEntity);

        // Assert
        assertThat(result.location()).isNotNull();
        assertThat(result.location().world()).isEqualTo("world");
        assertThat(result.location().x()).isEqualTo(100.5);
        assertThat(result.location().y()).isEqualTo(64.0);
        assertThat(result.location().z()).isEqualTo(200.5);
        assertThat(result.location().yaw()).isEqualTo(180.0f);
        assertThat(result.location().pitch()).isEqualTo(0.0f);
    }

    @Test
    @DisplayName("toDTO() should handle damaged entity")
    void testToDTO_DamagedEntity() {
        // Arrange
        when(mockEntity.getHealth()).thenReturn(5.0);
        when(mockEntity.getMaxHealth()).thenReturn(20.0);

        // Act
        EntityData result = EntityDataConverter.toDTO(mockEntity);

        // Assert
        assertThat(result.health()).isEqualTo(5);
        assertThat(result.maxHealth()).isEqualTo(20);
        assertThat(result.healthPercent()).isEqualTo(0.25);
        assertThat(result.isDead()).isFalse();
    }

    @Test
    @DisplayName("toDTO() should handle dead entity")
    void testToDTO_DeadEntity() {
        // Arrange
        when(mockEntity.getHealth()).thenReturn(0.0);

        // Act
        EntityData result = EntityDataConverter.toDTO(mockEntity);

        // Assert
        assertThat(result.health()).isEqualTo(0);
        assertThat(result.isDead()).isTrue();
        assertThat(result.healthPercent()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("toDTO() should handle entity with custom name")
    void testToDTO_CustomName() {
        // Arrange
        when(mockEntity.getName()).thenReturn("Lord Zombie");

        // Act
        EntityData result = EntityDataConverter.toDTO(mockEntity);

        // Assert
        assertThat(result.customName()).isEqualTo("Lord Zombie");
        assertThat(result.hasCustomName()).isTrue();
    }

    @Test
    @DisplayName("toDTO() should handle entity without custom name")
    void testToDTO_NoCustomName() {
        // Arrange
        when(mockEntity.getName()).thenReturn(null);

        // Act
        EntityData result = EntityDataConverter.toDTO(mockEntity);

        // Assert
        assertThat(result.customName()).isNull();
        assertThat(result.hasCustomName()).isFalse();
    }

    @Test
    @DisplayName("applyToEntity() should update entity health")
    void testApplyToEntity_UpdateHealth() {
        // Arrange
        EntityData dto = new EntityData(
            entityId,
            "minecraft:zombie",
            "Zombie",
            new LocationData("world", 100.5, 64.0, 200.5, 180.0f, 0.0f),
            15,
            20
        );

        // Act
        EntityDataConverter.applyToEntity(dto, mockEntity);

        // Assert
        verify(mockEntity).setHealth(15);
    }

    @Test
    @DisplayName("applyToEntity() should teleport entity to new location")
    void testApplyToEntity_Teleport() {
        // Arrange
        LocationData newLocation = new LocationData("world", 50.0, 70.0, 150.0, 90.0f, 45.0f);
        EntityData dto = new EntityData(
            entityId,
            "minecraft:zombie",
            "Zombie",
            newLocation,
            20,
            20
        );

        // Act
        EntityDataConverter.applyToEntity(dto, mockEntity);

        // Assert
        verify(mockEntity).teleport(any(Location.class));
    }

    @Test
    @DisplayName("applyToEntity() should throw when DTO is null")
    void testApplyToEntity_NullDTO() {
        // Act & Assert
        assertThatThrownBy(() -> EntityDataConverter.applyToEntity(null, mockEntity))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Both DTO and existing entity must be non-null");
    }

    @Test
    @DisplayName("applyToEntity() should throw when entity is null")
    void testApplyToEntity_NullEntity() {
        // Arrange
        EntityData dto = new EntityData(
            entityId,
            "minecraft:zombie",
            "Zombie",
            new LocationData("world", 100.5, 64.0, 200.5, 180.0f, 0.0f),
            20,
            20
        );

        // Act & Assert
        assertThatThrownBy(() -> EntityDataConverter.applyToEntity(dto, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Both DTO and existing entity must be non-null");
    }

    @Test
    @DisplayName("applyToEntity() should throw when UUID mismatch")
    void testApplyToEntity_UUIDMismatch() {
        // Arrange
        UUID differentId = UUID.randomUUID();
        EntityData dto = new EntityData(
            differentId,
            "minecraft:zombie",
            "Zombie",
            new LocationData("world", 100.5, 64.0, 200.5, 180.0f, 0.0f),
            20,
            20
        );

        // Act & Assert
        assertThatThrownBy(() -> EntityDataConverter.applyToEntity(dto, mockEntity))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Entity UUID mismatch");
    }

    @Test
    @DisplayName("applyToEntity() should handle null location gracefully")
    void testApplyToEntity_NullLocation() {
        // Arrange
        EntityData dto = new EntityData(
            entityId,
            "minecraft:zombie",
            "Zombie",
            null,  // null location
            20,
            20
        );

        // Act
        EntityDataConverter.applyToEntity(dto, mockEntity);

        // Assert
        verify(mockEntity).setHealth(20);
        verify(mockEntity, never()).teleport(any(Location.class));
    }

    @Test
    @DisplayName("EntityData helper methods should work correctly")
    void testEntityData_HelperMethods() {
        // Arrange
        EntityData alive = new EntityData(
            UUID.randomUUID(),
            "player",
            "Steve",
            new LocationData("world", 0, 64, 0, 0f, 0f),
            10,
            20
        );
        
        EntityData dead = new EntityData(
            UUID.randomUUID(),
            "zombie",
            null,
            new LocationData("world", 0, 64, 0, 0f, 0f),
            0,
            20
        );

        // Assert
        assertThat(alive.isDead()).isFalse();
        assertThat(alive.healthPercent()).isEqualTo(0.5);
        assertThat(alive.hasCustomName()).isTrue();
        
        assertThat(dead.isDead()).isTrue();
        assertThat(dead.healthPercent()).isEqualTo(0.0);
        assertThat(dead.hasCustomName()).isFalse();
    }

    @Test
    @DisplayName("toDTO() should handle entity with fractional health")
    void testToDTO_FractionalHealth() {
        // Arrange
        when(mockEntity.getHealth()).thenReturn(15.7);
        when(mockEntity.getMaxHealth()).thenReturn(20.0);

        // Act
        EntityData result = EntityDataConverter.toDTO(mockEntity);

        // Assert - Should cast to int
        assertThat(result.health()).isEqualTo(15);
        assertThat(result.maxHealth()).isEqualTo(20);
    }

    @Test
    @DisplayName("Bidirectional conversion should preserve entity identity")
    void testBidirectionalConversion() {
        // Act
        EntityData dto = EntityDataConverter.toDTO(mockEntity);
        
        // Clear invocations from toDTO
        clearInvocations(mockEntity);
        
        // Apply back to entity
        EntityDataConverter.applyToEntity(dto, mockEntity);

        // Assert
        assertThat(dto.id()).isEqualTo(entityId);
        verify(mockEntity).setHealth(20);  // Health was preserved
        verify(mockEntity).teleport(any(Location.class));  // Location was preserved
    }
}
