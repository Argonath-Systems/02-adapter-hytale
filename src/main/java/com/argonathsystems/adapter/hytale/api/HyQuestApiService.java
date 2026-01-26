package com.argonathsystems.adapter.hytale.api;

import com.argonathsystems.hyquest.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * REST API implementation for HyQuestUI integration.
 * 
 * <p>This service exposes HTTP endpoints that match the OpenAPI specification
 * defined in {@code 99-HyQuest-WebUI/swagger.yaml}, allowing the Quest Designer
 * WebUI to communicate with the Hytale server.
 * 
 * <p><b>Note:</b> This is a simple file-based implementation. For production,
 * consider using a proper HTTP framework like Javalin, Spark, or embedded Jetty.
 * 
 * <h2>Supported Endpoints:</h2>
 * <ul>
 *   <li>GET /quests - List all quests</li>
 *   <li>GET /quests/{id} - Get specific quest</li>
 *   <li>POST /quests - Create new quest</li>
 *   <li>PUT /quests/{id} - Update quest</li>
 *   <li>DELETE /quests/{id} - Delete quest</li>
 * </ul>
 * 
 * <h2>Storage:</h2>
 * Quests are stored as JSON files in: {@code server/data/quests/}
 * 
 * <h2>Authentication:</h2>
 * Authentication is skipped for now (as per user request).
 * 
 * @author Argonath Systems
 * @version 1.0.0
 * @since 1.0.0
 */
public class HyQuestApiService {
    
    private final Path questStoragePath;
    private final Gson gson;
    private final Map<String, QuestDefinition> questCache = new ConcurrentHashMap<>();
    
    /**
     * Create a new HyQuest API service.
     * 
     * @param dataDirectory Root data directory for the server
     */
    public HyQuestApiService(Path dataDirectory) {
        this.questStoragePath = dataDirectory.resolve("quests");
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
        
        // Create storage directory if it doesn't exist
        try {
            Files.createDirectories(questStoragePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create quest storage directory", e);
        }
        
        // Load existing quests
        loadAllQuests();
    }
    
    /**
     * Get all quests (simplified - no pagination/filtering for now).
     * 
     * <p>Endpoint: {@code GET /quests}
     * 
     * @return List of all quests
     */
    public List<QuestDefinition> getAllQuests() {
        return new ArrayList<>(questCache.values());
    }
    
    /**
     * Get a specific quest by ID.
     * 
     * <p>Endpoint: {@code GET /quests/{questId}}
     * 
     * @param questId Quest ID
     * @return Quest definition, or null if not found
     */
    public QuestDefinition getQuest(String questId) {
        return questCache.get(questId);
    }
    
    /**
     * Create a new quest.
     * 
     * <p>Endpoint: {@code POST /quests}
     * 
     * @param quest Quest definition to create
     * @return Created quest
     * @throws IllegalArgumentException if quest with same ID already exists
     */
    public QuestDefinition createQuest(QuestDefinition quest) {
        if (questCache.containsKey(quest.getId())) {
            throw new IllegalArgumentException("Quest already exists: " + quest.getId());
        }
        
        // Save to file
        saveQuestToFile(quest);
        
        // Cache
        questCache.put(quest.getId(), quest);
        
        return quest;
    }
    
    /**
     * Update an existing quest.
     * 
     * <p>Endpoint: {@code PUT /quests/{questId}}
     * 
     * @param questId Quest ID
     * @param quest Updated quest definition
     * @return Updated quest
     * @throws IllegalArgumentException if quest doesn't exist
     */
    public QuestDefinition updateQuest(String questId, QuestDefinition quest) {
        if (!questCache.containsKey(questId)) {
            throw new IllegalArgumentException("Quest not found: " + questId);
        }
        
        // Ensure ID matches
        quest.setId(questId);
        
        // Save to file
        saveQuestToFile(quest);
        
        // Update cache
        questCache.put(questId, quest);
        
        return quest;
    }
    
    /**
     * Delete a quest.
     * 
     * <p>Endpoint: {@code DELETE /quests/{questId}}
     * 
     * @param questId Quest ID to delete
     * @return true if deleted, false if not found
     */
    public boolean deleteQuest(String questId) {
        if (!questCache.containsKey(questId)) {
            return false;
        }
        
        // Delete file
        Path questFile = questStoragePath.resolve(questId + ".json");
        try {
            Files.deleteIfExists(questFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete quest file", e);
        }
        
        // Remove from cache
        questCache.remove(questId);
        
        return true;
    }
    
    /**
     * Search quests by filter criteria (simplified implementation).
     * 
     * @param searchTerm Search term (searches in ID only for now)
     * @param category Category filter
     * @param minLevel Minimum level
     * @param maxLevel Maximum level
     * @return Filtered quests
     */
    public List<QuestDefinition> searchQuests(
        String searchTerm,
        String category,
        Integer minLevel,
        Integer maxLevel
    ) {
        return questCache.values().stream()
            .filter(quest -> {
                // Simple search filter
                if (searchTerm != null && !quest.getId().contains(searchTerm)) {
                    return false;
                }
                
                // TODO: Add metadata-based filtering when metadata is populated
                
                return true;
            })
            .collect(Collectors.toList());
    }
    
    // --- Private Helper Methods ---
    
    /**
     * Load all quests from the storage directory.
     */
    private void loadAllQuests() {
        try {
            if (!Files.exists(questStoragePath)) {
                return;
            }
            
            Files.list(questStoragePath)
                .filter(path -> path.toString().endsWith(".json"))
                .forEach(this::loadQuestFromFile);
                
        } catch (IOException e) {
            throw new RuntimeException("Failed to load quests from storage", e);
        }
    }
    
    /**
     * Load a single quest from a file.
     */
    private void loadQuestFromFile(Path questFile) {
        try (Reader reader = Files.newBufferedReader(questFile)) {
            QuestDefinition quest = gson.fromJson(reader, QuestDefinition.class);
            questCache.put(quest.getId(), quest);
        } catch (IOException e) {
            System.err.println("Failed to load quest from " + questFile + ": " + e.getMessage());
        }
    }
    
    /**
     * Save a quest to a JSON file.
     */
    private void saveQuestToFile(QuestDefinition quest) {
        Path questFile = questStoragePath.resolve(quest.getId() + ".json");
        try (Writer writer = Files.newBufferedWriter(questFile)) {
            gson.toJson(quest, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save quest to file", e);
        }
    }
    
    /**
     * Get quest storage path (for debugging/testing).
     */
    public Path getQuestStoragePath() {
        return questStoragePath;
    }
    
    /**
     * Get number of cached quests.
     */
    public int getQuestCount() {
        return questCache.size();
    }
}
