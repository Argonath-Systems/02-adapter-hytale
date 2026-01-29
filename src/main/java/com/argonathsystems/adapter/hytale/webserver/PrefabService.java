package com.argonathsystems.adapter.hytale.webserver;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for prefab operations.
 * 
 * <p>This interface defines the contract for prefab CRUD operations
 * used by the HyPrefab API controller. Implementations should handle
 * storage and retrieval of prefab definitions.
 * 
 * @author Argonath Systems
 * @version 1.0.0
 * @since 1.0.0
 */
public interface PrefabService {
    
    /**
     * Lists all prefabs, optionally filtered.
     * 
     * @param searchTerm optional search term
     * @return list of prefab data
     */
    List<PrefabData> listAll(String searchTerm);
    
    /**
     * Gets a specific prefab by ID.
     * 
     * @param prefabId the prefab ID
     * @return the prefab data, or empty if not found
     */
    Optional<PrefabData> getById(String prefabId);
    
    /**
     * Creates a new prefab.
     * 
     * @param prefab the prefab data to create
     * @return the created prefab with ID assigned
     */
    PrefabData create(PrefabData prefab);
    
    /**
     * Updates an existing prefab.
     * 
     * @param prefabId the prefab ID
     * @param prefab the updated prefab data
     * @return the updated prefab, or empty if not found
     */
    Optional<PrefabData> update(String prefabId, PrefabData prefab);
    
    /**
     * Deletes a prefab.
     * 
     * @param prefabId the prefab ID
     * @return true if deleted, false if not found
     */
    boolean delete(String prefabId);
    
    /**
     * Spawns a prefab at a specific location.
     * 
     * @param prefabId the prefab ID
     * @param spawnRequest the spawn request data
     * @return the spawn result
     */
    SpawnResult spawn(String prefabId, SpawnRequest spawnRequest);
    
    /**
     * Represents prefab data.
     */
    record PrefabData(
        String id,
        String name,
        String description,
        String category,
        Map<String, String> metadata
    ) {}
    
    /**
     * Represents a spawn request.
     */
    record SpawnRequest(
        String worldId,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
    ) {}
    
    /**
     * Represents a spawn result.
     */
    record SpawnResult(
        boolean success,
        String entityId,
        String message
    ) {
        public static SpawnResult success(String entityId) {
            return new SpawnResult(true, entityId, "Prefab spawned successfully");
        }
        
        public static SpawnResult failure(String message) {
            return new SpawnResult(false, null, message);
        }
    }
}
