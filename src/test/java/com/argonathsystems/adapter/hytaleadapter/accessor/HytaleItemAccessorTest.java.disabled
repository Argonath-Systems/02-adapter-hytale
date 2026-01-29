package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.dto.ItemData;
import com.argonathsystems.framework.accessorapi.dto.ItemDefinitionData;
import com.hytale.api.Server;
import com.hytale.api.inventory.ItemStack;
import com.hytale.api.registry.ItemRegistry;
import com.hytale.api.registry.ItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for HytaleItemAccessor.
 * 
 * <p>Verifies:
 * <ul>
 *   <li>Item definition retrieval from registry</li>
 *   <li>Item creation and conversion</li>
 *   <li>Tag-based item queries</li>
 *   <li>Cache initialization and performance</li>
 * </ul>
 * 
 * @author Argonath Systems Team
 * @version 2.0.0
 */
@DisplayName("HytaleItemAccessor Tests")
class HytaleItemAccessorTest {

    @Mock
    private Server mockServer;

    @Mock
    private ItemRegistry mockRegistry;

    @Mock
    private ItemType mockItemType1;

    @Mock
    private ItemType mockItemType2;

    @Mock
    private ItemStack mockItemStack;

    private HytaleItemAccessor accessor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup registry mock
        when(mockServer.getRegistry()).thenReturn(mockRegistry);
        
        // Setup item types
        when(mockItemType1.getId()).thenReturn("minecraft:diamond_sword");
        when(mockItemType1.getDisplayName()).thenReturn("Diamond Sword");
        when(mockItemType1.getMaxStackSize()).thenReturn(1);
        when(mockItemType1.getTags()).thenReturn(Set.of("weapon", "sword", "diamond"));
        
        when(mockItemType2.getId()).thenReturn("minecraft:iron_ingot");
        when(mockItemType2.getDisplayName()).thenReturn("Iron Ingot");
        when(mockItemType2.getMaxStackSize()).thenReturn(64);
        when(mockItemType2.getTags()).thenReturn(Set.of("material", "iron"));
        
        when(mockRegistry.getAllItemTypes()).thenReturn(List.of(mockItemType1, mockItemType2));
        when(mockRegistry.getItemType("minecraft:diamond_sword")).thenReturn(mockItemType1);
        when(mockRegistry.getItemType("minecraft:iron_ingot")).thenReturn(mockItemType2);
        
        when(mockServer.createItemStack(anyString(), anyInt())).thenReturn(mockItemStack);
        when(mockItemStack.getType()).thenReturn("minecraft:diamond_sword");
        when(mockItemStack.getAmount()).thenReturn(1);

        accessor = new HytaleItemAccessor(mockServer);
    }

    @Test
    @DisplayName("getItemDefinition() should return item definition from cache")
    void testGetItemDefinition() {
        // Act
        Optional<ItemDefinitionData> result = accessor.getItemDefinition("minecraft:diamond_sword");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().itemId()).isEqualTo("minecraft:diamond_sword");
        assertThat(result.get().displayName()).isEqualTo("Diamond Sword");
        assertThat(result.get().maxStackSize()).isEqualTo(1);
        assertThat(result.get().tags()).containsExactlyInAnyOrder("weapon", "sword", "diamond");
    }

    @Test
    @DisplayName("getItemDefinition() should return empty for unknown items")
    void testGetItemDefinition_NotFound() {
        // Act
        Optional<ItemDefinitionData> result = accessor.getItemDefinition("unknown:item");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getAllItemDefinitions() should return all cached definitions")
    void testGetAllItemDefinitions() {
        // Act
        Collection<ItemDefinitionData> result = accessor.getAllItemDefinitions();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ItemDefinitionData::itemId)
            .containsExactlyInAnyOrder("minecraft:diamond_sword", "minecraft:iron_ingot");
    }

    @Test
    @DisplayName("getItemsByTag() should return items with matching tag")
    void testGetItemsByTag() {
        // Act
        Collection<ItemDefinitionData> result = accessor.getItemsByTag("weapon");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).extracting(ItemDefinitionData::itemId)
            .containsExactly("minecraft:diamond_sword");
    }

    @Test
    @DisplayName("getItemsByTag() should return multiple items for common tag")
    void testGetItemsByTag_MultipleResults() {
        // Arrange - Add more items with 'material' tag
        ItemType mockItemType3 = mock(ItemType.class);
        when(mockItemType3.getId()).thenReturn("minecraft:gold_ingot");
        when(mockItemType3.getDisplayName()).thenReturn("Gold Ingot");
        when(mockItemType3.getMaxStackSize()).thenReturn(64);
        when(mockItemType3.getTags()).thenReturn(Set.of("material", "gold"));
        
        when(mockRegistry.getAllItemTypes()).thenReturn(List.of(mockItemType1, mockItemType2, mockItemType3));
        accessor = new HytaleItemAccessor(mockServer); // Reinitialize to reload cache

        // Act
        Collection<ItemDefinitionData> result = accessor.getItemsByTag("material");

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ItemDefinitionData::itemId)
            .containsExactlyInAnyOrder("minecraft:iron_ingot", "minecraft:gold_ingot");
    }

    @Test
    @DisplayName("getItemsByTag() should return empty for unknown tag")
    void testGetItemsByTag_NotFound() {
        // Act
        Collection<ItemDefinitionData> result = accessor.getItemsByTag("nonexistent_tag");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getItemTags() should return all tags for an item")
    void testGetItemTags() {
        // Act
        Set<String> result = accessor.getItemTags("minecraft:diamond_sword");

        // Assert
        assertThat(result).containsExactlyInAnyOrder("weapon", "sword", "diamond");
    }

    @Test
    @DisplayName("getItemTags() should return empty set for unknown item")
    void testGetItemTags_NotFound() {
        // Act
        Set<String> result = accessor.getItemTags("unknown:item");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("createItem() should create ItemData with correct properties")
    void testCreateItem() {
        // Act
        ItemData result = accessor.createItem("minecraft:diamond_sword", 1);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.itemId()).isEqualTo("minecraft:diamond_sword");
        assertThat(result.amount()).isEqualTo(1);
        assertThat(result.instanceId()).isNotNull();
    }

    @Test
    @DisplayName("createItem() should throw for unknown item type")
    void testCreateItem_UnknownItem() {
        // Act & Assert
        assertThatThrownBy(() -> accessor.createItem("unknown:item", 1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown item type");
    }

    @Test
    @DisplayName("createItem() should create stacked items")
    void testCreateItem_Stacked() {
        // Act
        ItemData result = accessor.createItem("minecraft:iron_ingot", 64);

        // Assert
        assertThat(result.itemId()).isEqualTo("minecraft:iron_ingot");
        assertThat(result.amount()).isEqualTo(64);
    }

    @Test
    @DisplayName("itemExists() should return true for registered items")
    void testItemExists() {
        // Act
        boolean result = accessor.itemExists("minecraft:diamond_sword");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("itemExists() should return false for unknown items")
    void testItemExists_NotFound() {
        // Act
        boolean result = accessor.itemExists("unknown:item");

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("fromHytaleItem() should convert ItemStack to ItemData")
    void testFromHytaleItem() {
        // Act
        ItemData result = accessor.fromHytaleItem(mockItemStack);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.itemId()).isEqualTo("minecraft:diamond_sword");
        assertThat(result.amount()).isEqualTo(1);
    }

    @Test
    @DisplayName("fromHytaleItem() should handle null gracefully")
    void testFromHytaleItem_Null() {
        // Act
        ItemData result = accessor.fromHytaleItem(null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toHytaleItem() should convert ItemData to ItemStack")
    void testToHytaleItem() {
        // Arrange
        ItemData itemData = new ItemData("minecraft:diamond_sword", 1);

        // Act
        ItemStack result = accessor.toHytaleItem(itemData);

        // Assert
        assertThat(result).isNotNull();
        verify(mockServer).createItemStack("minecraft:diamond_sword", 1);
    }

    @Test
    @DisplayName("toHytaleItem() should handle null gracefully")
    void testToHytaleItem_Null() {
        // Act
        ItemStack result = accessor.toHytaleItem(null);

        // Assert
        assertThat(result).isNull();
        verify(mockServer, never()).createItemStack(anyString(), anyInt());
    }

    @Test
    @DisplayName("Cache should be initialized lazily on first access")
    void testCacheInitialization() {
        // Arrange - Create new accessor (cache not initialized)
        HytaleItemAccessor newAccessor = new HytaleItemAccessor(mockServer);

        // Act - First access initializes cache
        newAccessor.getItemDefinition("minecraft:diamond_sword");

        // Assert - Registry was queried
        verify(mockRegistry).getAllItemTypes();

        // Act - Second access uses cache (no additional registry calls)
        clearInvocations(mockRegistry);
        newAccessor.getItemDefinition("minecraft:iron_ingot");

        // Assert - No additional registry calls
        verify(mockRegistry, never()).getAllItemTypes();
    }

    @Test
    @DisplayName("Tag cache should enable efficient tag-based queries")
    void testTagCache_Performance() {
        // Act - Multiple queries for same tag
        accessor.getItemsByTag("weapon");
        accessor.getItemsByTag("weapon");
        accessor.getItemsByTag("weapon");

        // Assert - Cache was built once during initialization
        verify(mockRegistry, times(1)).getAllItemTypes();
    }

    @Test
    @DisplayName("getAllItemDefinitions() should return unmodifiable collection")
    void testGetAllItemDefinitions_Unmodifiable() {
        // Act
        Collection<ItemDefinitionData> result = accessor.getAllItemDefinitions();

        // Assert - Attempting to modify should fail
        assertThatThrownBy(() -> result.clear())
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("getItemTags() should return unmodifiable set for unknown items")
    void testGetItemTags_UnmodifiableEmpty() {
        // Act
        Set<String> result = accessor.getItemTags("unknown:item");

        // Assert
        assertThat(result).isEmpty();
        assertThatThrownBy(() -> result.add("test"))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
