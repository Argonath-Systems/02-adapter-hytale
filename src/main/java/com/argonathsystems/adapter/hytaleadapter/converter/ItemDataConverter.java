package com.argonathsystems.adapter.hytaleadapter.converter;

import com.argonathsystems.framework.accessorapi.dto.ItemData;
import com.argonathsystems.framework.accessorapi.data.DataValue;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * <p><b>MIGRATION-001 Status:</b> ✅ IMPLEMENTED</p>
 * 
 * <p>Converter for ItemStack (Hytale SDK) to ItemData (Platform-agnostic DTO).</p>
 * 
 * <h2>SDK Classes Used:</h2>
 * <ul>
 *   <li>{@code ItemStack} - Item with quantity, durability, metadata</li>
 *   <li>{@code BsonDocument} - Metadata storage format</li>
 * </ul>
 * 
 * @author Argonath Systems
 * @version 3.0.0
 * @since MIGRATION-001
 */
public class ItemDataConverter {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemDataConverter.class);
    
    /**
     * Convert from Hytale ItemStack to platform-agnostic ItemData.
     * 
     * @param itemStack Hytale SDK ItemStack object
     * @return ItemData DTO with item ID, amount, and metadata
     */
    public static ItemData toDTO(ItemStack itemStack) {
        if (ItemStack.isEmpty(itemStack)) {
            return null;
        }
        
        try {
            String itemId = itemStack.getItemId();
            int quantity = itemStack.getQuantity();
            // getDurability() returns double, safely convert to int
            int durability = (int) Math.round(itemStack.getDurability());
            int maxDurability = (int) Math.round(itemStack.getMaxDurability());
            
            // Extract metadata from BsonDocument
            Map<String, DataValue> customData = new HashMap<>();
            BsonDocument bsonMeta = itemStack.getMetadata();
            
            if (bsonMeta != null) {
                for (Map.Entry<String, BsonValue> entry : bsonMeta.entrySet()) {
                    DataValue value = bsonValueToDataValue(entry.getValue());
                    if (value != null) {
                        customData.put(entry.getKey(), value);
                    }
                }
            }
            
            // Use full 6-argument constructor with all fields
            return new ItemData(
                java.util.UUID.randomUUID(),  // Generate unique instance ID
                itemId,
                quantity,
                durability,
                maxDurability,
                customData
            );
            
        } catch (Exception e) {
            LOGGER.warn("Failed to convert ItemStack to DTO: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Convert from platform-agnostic ItemData to Hytale ItemStack.
     * 
     * @param dto ItemData DTO
     * @return Hytale SDK ItemStack object
     */
    public static ItemStack fromDTO(ItemData dto) {
        if (dto == null || dto.itemId() == null) {
            return null;
        }
        
        try {
            // Create base ItemStack
            ItemStack stack = new ItemStack(dto.itemId(), dto.amount());
            
            // Apply metadata if present
            if (dto.customData() != null && !dto.customData().isEmpty()) {
                BsonDocument bsonMeta = new BsonDocument();
                
                for (Map.Entry<String, DataValue> entry : dto.customData().entrySet()) {
                    BsonValue bsonValue = dataValueToBson(entry.getValue());
                    if (bsonValue != null) {
                        bsonMeta.put(entry.getKey(), bsonValue);
                    }
                }
                
                // Apply metadata via withMetadata (returns new ItemStack)
                if (!bsonMeta.isEmpty()) {
                    stack = stack.withMetadata(bsonMeta);
                }
            }
            
            return stack;
            
        } catch (Exception e) {
            LOGGER.warn("Failed to convert DTO to ItemStack: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Convert BsonValue to platform-agnostic DataValue.
     */
    private static DataValue bsonValueToDataValue(BsonValue bson) {
        if (bson == null) {
            return null;
        }
        
        switch (bson.getBsonType()) {
            case STRING:
                return DataValue.of(bson.asString().getValue());
            case INT32:
                return DataValue.of(bson.asInt32().getValue());
            case INT64:
                return DataValue.of(bson.asInt64().getValue());
            case DOUBLE:
                return DataValue.of(bson.asDouble().getValue());
            case BOOLEAN:
                return DataValue.of(bson.asBoolean().getValue());
            default:
                // For complex types, store as string representation
                return DataValue.of(bson.toString());
        }
    }
    
    /**
     * Convert platform-agnostic DataValue to BsonValue.
     */
    private static BsonValue dataValueToBson(DataValue value) {
        if (value == null) {
            return null;
        }
        
        return switch (value) {
            case DataValue.StringValue s -> new BsonString(s.value());
            case DataValue.IntValue i -> new BsonInt32(i.value());
            case DataValue.LongValue l -> new org.bson.BsonInt64(l.value());
            case DataValue.DoubleValue d -> new org.bson.BsonDouble(d.value());
            case DataValue.BoolValue b -> new org.bson.BsonBoolean(b.value());
            case DataValue.ListValue list -> {
                org.bson.BsonArray arr = new org.bson.BsonArray();
                for (DataValue item : list.value()) {
                    BsonValue bsonItem = dataValueToBson(item);
                    if (bsonItem != null) {
                        arr.add(bsonItem);
                    }
                }
                yield arr;
            }
            case DataValue.MapValue map -> {
                BsonDocument doc = new BsonDocument();
                for (var entry : map.value().entrySet()) {
                    BsonValue bsonVal = dataValueToBson(entry.getValue());
                    if (bsonVal != null) {
                        doc.put(entry.getKey(), bsonVal);
                    }
                }
                yield doc;
            }
        };
    }
    
    /**
     * Check if an ItemStack is empty or null.
     * 
     * @param stack The ItemStack to check
     * @return true if empty or null
     */
    public static boolean isEmpty(ItemStack stack) {
        return ItemStack.isEmpty(stack);
    }
    
    /**
     * Create an empty ItemStack.
     * 
     * @return Empty ItemStack
     */
    public static ItemStack empty() {
        return ItemStack.EMPTY;
    }
}
