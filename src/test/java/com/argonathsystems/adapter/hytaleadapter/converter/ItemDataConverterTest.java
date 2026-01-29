package com.argonathsystems.adapter.hytaleadapter.converter;

import com.argonathsystems.framework.accessorapi.data.DataValue;
import com.argonathsystems.framework.accessorapi.dto.ItemData;
import com.hytale.api.Server;
import com.hytale.api.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Tests for ItemDataConverter.
 * 
 * <p>Verifies:
 * <ul>
 *   <li>Hytale ItemStack → ItemData DTO conversion</li>
 *   <li>ItemData DTO → Hytale ItemStack conversion</li>
 *   <li>Null safety</li>
 *   <li>Custom data handling (when SDK supports it)</li>
 * </ul>
 * 
 * @author Argonath Systems Team
 * @version 2.0.0
 */
@DisplayName("ItemDataConverter Tests")
class ItemDataConverterTest {

    @Mock
    private Server mockServer;

    @Mock
    private ItemStack mockItemStack;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("toDTO() should convert basic ItemStack to ItemData")
    void testToDTO_BasicConversion() {
        // Arrange
        when(mockItemStack.getType()).thenReturn("minecraft:diamond_sword");
        when(mockItemStack.getAmount()).thenReturn(1);

        // Act
        ItemData result = ItemDataConverter.toDTO(mockItemStack);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.itemId()).isEqualTo("minecraft:diamond_sword");
        assertThat(result.amount()).isEqualTo(1);
        assertThat(result.instanceId()).isNotNull(); // UUID generated
        
        // Current SDK stub doesn't support durability
        assertThat(result.durability()).isEqualTo(-1);
        assertThat(result.maxDurability()).isEqualTo(-1);
        assertThat(result.hasDurability()).isFalse();
        
        // No custom data in stub
        assertThat(result.customData()).isEmpty();
    }

    @Test
    @DisplayName("toDTO() should handle null input gracefully")
    void testToDTO_NullInput() {
        // Act
        ItemData result = ItemDataConverter.toDTO(null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toDTO() should handle multi-stack items")
    void testToDTO_StackedItems() {
        // Arrange
        when(mockItemStack.getType()).thenReturn("minecraft:golden_apple");
        when(mockItemStack.getAmount()).thenReturn(64);

        // Act
        ItemData result = ItemDataConverter.toDTO(mockItemStack);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.amount()).isEqualTo(64);
    }

    @Test
    @DisplayName("fromDTO() should convert ItemData to ItemStack")
    void testFromDTO_BasicConversion() {
        // Arrange
        ItemData dto = new ItemData(
            UUID.randomUUID(),
            "minecraft:iron_ingot",
            16,
            -1,
            -1,
            Map.of()
        );
        
        when(mockServer.createItemStack(any(), anyInt())).thenReturn(mockItemStack);
        when(mockItemStack.getType()).thenReturn("minecraft:iron_ingot");
        when(mockItemStack.getAmount()).thenReturn(16);

        // Act
        ItemStack result = ItemDataConverter.fromDTO(dto, mockServer);

        // Assert
        assertThat(result).isNotNull();
        verify(mockServer).createItemStack("minecraft:iron_ingot", 16);
    }

    @Test
    @DisplayName("fromDTO() should handle null input gracefully")
    void testFromDTO_NullInput() {
        // Act
        ItemStack result = ItemDataConverter.fromDTO(null, mockServer);

        // Assert
        assertThat(result).isNull();
        verifyNoInteractions(mockServer);
    }

    @Test
    @DisplayName("fromDTO() should create item with simple constructor")
    void testFromDTO_SimpleConstructor() {
        // Arrange
        ItemData dto = new ItemData("minecraft:bread", 8);
        
        when(mockServer.createItemStack(any(), anyInt())).thenReturn(mockItemStack);

        // Act
        ItemStack result = ItemDataConverter.fromDTO(dto, mockServer);

        // Assert
        assertThat(result).isNotNull();
        verify(mockServer).createItemStack("minecraft:bread", 8);
    }

    @Test
    @DisplayName("toDTO() should generate unique instance IDs")
    void testToDTO_UniqueInstanceIds() {
        // Arrange
        when(mockItemStack.getType()).thenReturn("minecraft:stone");
        when(mockItemStack.getAmount()).thenReturn(1);

        // Act
        ItemData result1 = ItemDataConverter.toDTO(mockItemStack);
        ItemData result2 = ItemDataConverter.toDTO(mockItemStack);

        // Assert
        assertThat(result1.instanceId()).isNotEqualTo(result2.instanceId());
    }

    @Test
    @DisplayName("Bidirectional conversion should preserve item type and amount")
    void testBidirectionalConversion() {
        // Arrange
        when(mockItemStack.getType()).thenReturn("minecraft:emerald");
        when(mockItemStack.getAmount()).thenReturn(5);
        when(mockServer.createItemStack(any(), anyInt())).thenReturn(mockItemStack);

        // Act
        ItemData dto = ItemDataConverter.toDTO(mockItemStack);
        ItemStack reconstructed = ItemDataConverter.fromDTO(dto, mockServer);

        // Assert
        assertThat(dto.itemId()).isEqualTo("minecraft:emerald");
        assertThat(dto.amount()).isEqualTo(5);
        verify(mockServer).createItemStack("minecraft:emerald", 5);
    }

    @Test
    @DisplayName("toDTO() should handle items with durability when SDK supports it")
    void testToDTO_WithDurability_FutureImplementation() {
        // This test documents expected behavior when Hytale SDK adds durability support
        // Currently returns -1 for both durability and maxDurability
        
        // Arrange
        when(mockItemStack.getType()).thenReturn("minecraft:diamond_pickaxe");
        when(mockItemStack.getAmount()).thenReturn(1);
        // TODO: When SDK adds getDurability()/getMaxDurability(), add:
        // when(mockItemStack.getDurability()).thenReturn(1200);
        // when(mockItemStack.getMaxDurability()).thenReturn(1561);

        // Act
        ItemData result = ItemDataConverter.toDTO(mockItemStack);

        // Assert - Current behavior
        assertThat(result.hasDurability()).isFalse();
        
        // TODO: When SDK supports durability, update assertions to:
        // assertThat(result.hasDurability()).isTrue();
        // assertThat(result.durability()).isEqualTo(1200);
        // assertThat(result.maxDurability()).isEqualTo(1561);
    }

    @Test
    @DisplayName("toDTO() should handle items with custom data when SDK supports it")
    void testToDTO_WithCustomData_FutureImplementation() {
        // This test documents expected behavior when Hytale SDK adds NBT/custom data support
        
        // Arrange
        when(mockItemStack.getType()).thenReturn("minecraft:enchanted_book");
        when(mockItemStack.getAmount()).thenReturn(1);
        // TODO: When SDK adds getCustomData(), add mock behavior

        // Act
        ItemData result = ItemDataConverter.toDTO(mockItemStack);

        // Assert - Current behavior
        assertThat(result.customData()).isEmpty();
        
        // TODO: When SDK supports custom data, update assertions to verify:
        // - BSON/NBT to DataValue conversion
        // - Nested structures
        // - Type-safe mapping
    }

    @Test
    @DisplayName("ItemData helper methods should work correctly")
    void testItemData_HelperMethods() {
        // Arrange
        ItemData noDurability = new ItemData("item1", 1);
        ItemData withDurability = new ItemData(
            UUID.randomUUID(),
            "item2",
            1,
            50,
            100,
            Map.of()
        );

        // Assert
        assertThat(noDurability.hasDurability()).isFalse();
        assertThat(noDurability.isDamaged()).isFalse();
        
        assertThat(withDurability.hasDurability()).isTrue();
        assertThat(withDurability.isDamaged()).isTrue();
        assertThat(withDurability.durabilityPercent()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("ItemData with type-safe custom data should be constructible")
    void testItemData_WithTypeSafeCustomData() {
        // Arrange
        Map<String, DataValue> customData = Map.of(
            "enchantments", DataValue.of(List.of(
                DataValue.of("sharpness"),
                DataValue.of("unbreaking")
            )),
            "enchant_level", DataValue.of(3),
            "unbreakable", DataValue.of(true),
            "lore", DataValue.of("A legendary sword")
        );

        ItemData dto = new ItemData(
            UUID.randomUUID(),
            "minecraft:diamond_sword",
            1,
            -1,
            -1,
            customData
        );

        // Assert
        assertThat(dto.customData()).hasSize(4);
        assertThat(dto.customData().get("enchant_level").asInt()).contains(3);
        assertThat(dto.customData().get("unbreakable").asBool()).contains(true);
        assertThat(dto.customData().get("lore").asString()).contains("A legendary sword");
    }
}
