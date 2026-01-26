package com.argonathsystems.adapter.hytaleadapter.util;

// import com.fasterxml.jackson.databind.ObjectMapper;
// import org.bson.Document;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility for converting between BSON Documents and Java Maps.
 * Uses MongoDB's BSON library for proper type handling.
 * 
 * TODO: Add org.bson dependency to enable this utility
 */
public class BsonConverter {
    // private static final ObjectMapper mapper = new ObjectMapper();
    
    /**
     * Converts a BSON Document to a Java Map.
     * 
     * @param bsonDocument The BSON document (must be org.bson.Document)
     * @return Map representation of the document
     */
    public static Map<String, Object> bsonToMap(Object bsonDocument) {
        throw new UnsupportedOperationException("BSON support not yet enabled - add org.bson dependency");
        /* if (bsonDocument == null) {
            return Collections.emptyMap();
        }
        
        if (!(bsonDocument instanceof Document)) {
            throw new IllegalArgumentException(
                "Expected org.bson.Document but got " + bsonDocument.getClass().getName()
            );
        }
        
        Document doc = (Document) bsonDocument;
        Map<String, Object> map = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : doc.entrySet()) {
            Object value = entry.getValue();
            
            // Recursively convert nested documents
            if (value instanceof Document) {
                map.put(entry.getKey(), bsonToMap(value));
            } else {
                map.put(entry.getKey(), value);
            }
        }
        
        return map; */
    }

    /**
     * Converts a Java Map to a BSON Document.
     * 
     * @param map The map to convert
     * @return BSON Document
     */
    public static Object mapToBson(Map<String, Object> map) {
        throw new UnsupportedOperationException("BSON support not yet enabled - add org.bson dependency");
        /* if (map == null || map.isEmpty()) {
            return new Document();
        }
        
        /* Document doc = new Document();
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            
            // Recursively convert nested maps
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                doc.put(entry.getKey(), mapToBson(nestedMap));
            } else {
                doc.put(entry.getKey(), value);
            }
        }
        
        return doc; */
    }
}