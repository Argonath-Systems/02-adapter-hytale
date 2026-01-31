package com.argonathsystems.adapter.hytaleadapter.util;

import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.bson.BsonArray;
import org.bson.BsonString;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.BsonDouble;
import org.bson.BsonBoolean;
import org.bson.BsonNull;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility for converting between BSON Documents and Java Maps.
 * Uses MongoDB's BSON library for proper type handling.
 * 
 * <p>Hytale SDK uses BSON extensively for entity metadata, config storage,
 * and network serialization. This utility provides bidirectional conversion.</p>
 * 
 * @author Argonath Systems Team
 * @version 1.1.0
 * @since MIGRATION-001
 */
public class BsonConverter {
    
    /**
     * Converts a BSON Document to a Java Map.
     * 
     * @param bsonDocument The BSON document (org.bson.Document or BsonDocument)
     * @return Map representation of the document
     */
    public static Map<String, Object> bsonToMap(Object bsonDocument) {
        if (bsonDocument == null) {
            return Collections.emptyMap();
        }
        
        if (bsonDocument instanceof Document doc) {
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<String, Object> entry : doc.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Document nestedDoc) {
                    map.put(entry.getKey(), bsonToMap(nestedDoc));
                } else if (value instanceof List<?> list) {
                    map.put(entry.getKey(), convertList(list));
                } else {
                    map.put(entry.getKey(), value);
                }
            }
            return map;
        } else if (bsonDocument instanceof BsonDocument bsonDoc) {
            return bsonDocumentToMap(bsonDoc);
        } else {
            throw new IllegalArgumentException(
                "Expected org.bson.Document or BsonDocument but got " + bsonDocument.getClass().getName()
            );
        }
    }
    
    /**
     * Converts a BsonDocument to a Java Map.
     */
    private static Map<String, Object> bsonDocumentToMap(BsonDocument doc) {
        Map<String, Object> map = new HashMap<>();
        for (Map.Entry<String, BsonValue> entry : doc.entrySet()) {
            map.put(entry.getKey(), bsonValueToJava(entry.getValue()));
        }
        return map;
    }
    
    /**
     * Converts a BsonValue to its Java equivalent.
     */
    private static Object bsonValueToJava(BsonValue value) {
        if (value == null || value.isNull()) {
            return null;
        }
        return switch (value.getBsonType()) {
            case STRING -> value.asString().getValue();
            case INT32 -> value.asInt32().getValue();
            case INT64 -> value.asInt64().getValue();
            case DOUBLE -> value.asDouble().getValue();
            case BOOLEAN -> value.asBoolean().getValue();
            case DOCUMENT -> bsonDocumentToMap(value.asDocument());
            case ARRAY -> {
                List<Object> list = new ArrayList<>();
                for (BsonValue item : value.asArray()) {
                    list.add(bsonValueToJava(item));
                }
                yield list;
            }
            default -> value.toString();
        };
    }
    
    /**
     * Converts a List, recursively handling nested documents.
     */
    private static List<Object> convertList(List<?> list) {
        List<Object> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Document doc) {
                result.add(bsonToMap(doc));
            } else if (item instanceof List<?> nestedList) {
                result.add(convertList(nestedList));
            } else {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Converts a Java Map to a BSON Document.
     * 
     * @param map The map to convert
     * @return BSON Document (org.bson.Document)
     */
    public static Document mapToBson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return new Document();
        }
        
        Document doc = new Document();
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            doc.put(entry.getKey(), convertToSupportedType(value));
        }
        
        return doc;
    }
    
    /**
     * Converts a Java Map to a BsonDocument.
     * 
     * @param map The map to convert
     * @return BsonDocument
     */
    public static BsonDocument mapToBsonDocument(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return new BsonDocument();
        }
        
        BsonDocument doc = new BsonDocument();
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            doc.put(entry.getKey(), javaToBsonValue(entry.getValue()));
        }
        
        return doc;
    }
    
    /**
     * Converts a Java object to a BsonValue.
     */
    private static BsonValue javaToBsonValue(Object value) {
        if (value == null) {
            return BsonNull.VALUE;
        }
        return switch (value) {
            case String s -> new BsonString(s);
            case Integer i -> new BsonInt32(i);
            case Long l -> new BsonInt64(l);
            case Double d -> new BsonDouble(d);
            case Float f -> new BsonDouble(f.doubleValue());
            case Boolean b -> new BsonBoolean(b);
            case Map<?, ?> m -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> typedMap = (Map<String, Object>) m;
                yield mapToBsonDocument(typedMap);
            }
            case List<?> list -> {
                BsonArray array = new BsonArray();
                for (Object item : list) {
                    array.add(javaToBsonValue(item));
                }
                yield array;
            }
            default -> new BsonString(value.toString());
        };
    }
    
    /**
     * Converts a value to a type supported by org.bson.Document.
     */
    @SuppressWarnings("unchecked")
    private static Object convertToSupportedType(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> m) {
            return mapToBson((Map<String, Object>) m);
        }
        if (value instanceof List<?> list) {
            List<Object> converted = new ArrayList<>();
            for (Object item : list) {
                converted.add(convertToSupportedType(item));
            }
            return converted;
        }
        return value; // Primitives, Strings, etc. are supported directly
    }
}