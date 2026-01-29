package com.argonathsystems.adapter.hytaleadapter.converter;

import com.argonathsystems.framework.accessorapi.data.DataValue;
import com.argonathsystems.framework.accessorapi.dto.ItemData;
import com.hytale.api.inventory.ItemStack;
import com.hytale.api.Server;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Converts between Hytale ItemStack and platform-agnostic ItemData DTO.
 * 
 * <p>Handles:
 * <ul>
 *   <li>Basic item properties (type, amount)</li>
 *   <li>Durability (when supported by Hytale API)</li>
 *   <li>Custom NBT/metadata mapped to type-safe DataValue</li>
 *   <li>Bidirectional conversion</li>
 * </ul>
 * 
 * <p><strong>Note:</strong> Current Hytale SDK stub is minimal. Full implementation
 * requires actual ItemStack API with:
 * <ul>
 *   <li>getDurability() / setDurability()</li>
 *   <li>getMaxDurability()</li>
 *   <li>getCustomData() / setCustomData() for NBT access</li>
 * </ul>
 * 
 * @author Argonath Systems Team
 * @version 2.0.0
 * @since 1.0.0
 */
public class ItemDataConverter {
    
    /**
     * Convert Hytale ItemStack to platform-agnostic ItemData.
     * 
     * @param hytaleItemStack The Hytale item stack
     * @return ItemData DTO, or null if input is null
     */
    public static ItemData toDTO(ItemStack hytaleItemStack) {
        if (hytaleItemStack == null) return null;
        
        // Extract basic properties
        String itemId = hytaleItemStack.getType();
        int amount = hytaleItemStack.getAmount();
        
        // TODO: When Hytale SDK expands, extract these from actual API:
        // int durability = hytaleItemStack.getDurability();
        // int maxDurability = hytaleItemStack.getMaxDurability();
        // Object rawNBT = hytaleItemStack.getCustomData();
        
        int durability = -1;  // -1 = not applicable
        int maxDurability = -1;
        
        // Convert custom data to type-safe DataValue map
        Map<String, DataValue> customData = extractCustomData(hytaleItemStack);
        
        return new ItemData(
            UUID.randomUUID(), // Generate instance ID (Hytale may not have per-item UUIDs)
            itemId,
            amount,
            durability,
            maxDurability,
            customData
        );
    }
    
    /**
     * Convert platform-agnostic ItemData to Hytale ItemStack.
     * 
     * @param dto The ItemData DTO
     * @param server The Hytale server instance
     * @return Hytale ItemStack, or null if input is null
     */
    public static ItemStack fromDTO(ItemData dto, Server server) {
        if (dto == null) return null;
        
        // Create basic item stack
        ItemStack stack = server.createItemStack(dto.itemId(), dto.amount());
        
        // TODO: When Hytale SDK expands, apply these to the stack:
        // if (dto.hasDurability()) {
        //     stack.setDurability(dto.durability());
        // }
        // applyCustomData(stack, dto.customData());
        
        return stack;
    }
    
    /**
     * Extract custom NBT/metadata from Hytale ItemStack and convert to type-safe DataValue map.
     * 
     * <p><strong>Current Implementation:</strong> Returns empty map due to SDK stub limitations.
     * <p><strong>Future Implementation:</strong> Will extract from {@code itemStack.getCustomData()}
     * and convert BSON/NBT to DataValue using recursive mapping.
     * 
     * @param itemStack The Hytale item stack
     * @return Map of custom data as DataValue
     */
    private static Map<String, DataValue> extractCustomData(ItemStack itemStack) {
        // TODO: When Hytale provides NBT/metadata API:
        // Object rawData = itemStack.getCustomData();
        // if (rawData instanceof BsonDocument bson) {
        //     return convertBsonToDataValue(bson);
        // } else if (rawData instanceof Map) {
        //     return convertMapToDataValue((Map<String, Object>) rawData);
        // }
        
        return Map.of(); // Empty until SDK supports metadata
    }
    
    /**
     * Apply custom data from DataValue map to Hytale ItemStack.
     * 
     * <p><strong>Current Implementation:</strong> No-op due to SDK stub limitations.
     * <p><strong>Future Implementation:</strong> Will convert DataValue to BSON/NBT
     * and call {@code itemStack.setCustomData()}.
     * 
     * @param itemStack The target item stack
     * @param customData Custom data to apply
     */
    @SuppressWarnings("unused")
    private static void applyCustomData(ItemStack itemStack, Map<String, DataValue> customData) {
        // TODO: When Hytale provides NBT/metadata API:
        // if (customData.isEmpty()) return;
        // Object bson = convertDataValueToBson(customData);
        // itemStack.setCustomData(bson);
    }
    
    /**
     * Recursively convert Map<String, Object> to Map<String, DataValue>.
     * 
     * <p>Used for converting raw NBT/BSON data to type-safe DataValue.
     * 
     * @param map Raw data map
     * @return Type-safe DataValue map
     */
    @SuppressWarnings("unused")
    private static Map<String, DataValue> convertMapToDataValue(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return Map.of();
        
        Map<String, DataValue> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            DataValue dataValue = objectToDataValue(value);
            if (dataValue != null) {
                result.put(entry.getKey(), dataValue);
            }
        }
        return result;
    }
    
    /**
     * Convert a raw Object to type-safe DataValue.
     * 
     * @param obj The object to convert
     * @return DataValue, or null if type not supported
     */
    @SuppressWarnings("unchecked")
    private static DataValue objectToDataValue(Object obj) {
        if (obj == null) return null;
        
        return switch (obj) {
            case String s -> DataValue.of(s);
            case Integer i -> DataValue.of(i);
            case Long l -> DataValue.of(l);
            case Double d -> DataValue.of(d);
            case Float f -> DataValue.of((double) f);
            case Boolean b -> DataValue.of(b);
            case Map<?, ?> m -> {
                if (m.isEmpty()) yield DataValue.of(Map.of());
                // Recursively convert nested map
                Map<String, DataValue> nested = convertMapToDataValue((Map<String, Object>) m);
                yield DataValue.of(nested);
            }
            case Iterable<?> list -> {
                // Convert list elements to DataValue
                var dataList = new java.util.ArrayList<DataValue>();
                for (Object item : list) {
                    DataValue dv = objectToDataValue(item);
                    if (dv != null) dataList.add(dv);
                }
                yield DataValue.of(dataList);
            }
            default -> {
                // Log warning for unsupported type
                System.err.println("Warning: Unsupported data type " + obj.getClass().getName() + 
                                   " in ItemData custom data, skipping");
                yield null;
            }
        };
    }
    
    /**
     * Convert DataValue map back to raw Map<String, Object> for Hytale API.
     * 
     * @param dataMap Type-safe DataValue map
     * @return Raw object map for Hytale NBT/BSON
     */
    @SuppressWarnings("unused")
    private static Map<String, Object> convertDataValueToMap(Map<String, DataValue> dataMap) {
        if (dataMap == null || dataMap.isEmpty()) return Map.of();
        
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, DataValue> entry : dataMap.entrySet()) {
            Object value = dataValueToObject(entry.getValue());
            if (value != null) {
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }
    
    /**
     * Convert DataValue back to raw Object.
     * 
     * @param dataValue The DataValue to convert
     * @return Raw object
     */
    private static Object dataValueToObject(DataValue dataValue) {
        if (dataValue == null) return null;
        
        return switch (dataValue) {
            case DataValue.StringValue s -> s.value();
            case DataValue.IntValue i -> i.value();
            case DataValue.LongValue l -> l.value();
            case DataValue.DoubleValue d -> d.value();
            case DataValue.BoolValue b -> b.value();
            case DataValue.ListValue list -> {
                var result = new java.util.ArrayList<>();
                for (DataValue item : list.value()) {
                    result.add(dataValueToObject(item));
                }
                yield result;
            }
            case DataValue.MapValue map -> {
                var result = new HashMap<String, Object>();
                for (Map.Entry<String, DataValue> entry : map.value().entrySet()) {
                    result.put(entry.getKey(), dataValueToObject(entry.getValue()));
                }
                yield result;
            }
        };
    }
}