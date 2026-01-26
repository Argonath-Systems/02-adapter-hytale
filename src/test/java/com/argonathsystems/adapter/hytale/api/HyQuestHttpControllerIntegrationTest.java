package com.argonathsystems.adapter.hytale.api;

import com.argonathsystems.hyquest.model.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Integration test for HyQuest HTTP API.
 * 
 * <p>Tests the complete flow:
 * <ol>
 *   <li>Start HTTP server</li>
 *   <li>Create quest via POST</li>
 *   <li>Retrieve quest via GET</li>
 *   <li>Update quest via PUT</li>
 *   <li>Delete quest via DELETE</li>
 * </ol>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HyQuestHttpControllerIntegrationTest {
    
    private static HyQuestHttpController controller;
    private static Path testDataDir;
    private static HttpClient httpClient;
    private static Gson gson;
    private static final String BASE_URL = "http://localhost:19134"; // Different port for testing
    
    @BeforeAll
    static void setUp() throws IOException {
        // Create temporary test directory
        testDataDir = Files.createTempDirectory("hyquest-test");
        
        // Initialize controller on test port
        controller = new HyQuestHttpController(testDataDir, 19134);
        controller.start();
        
        // Wait for server to start
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        httpClient = HttpClient.newHttpClient();
        gson = new Gson();
        
        System.out.println("Test server started at: " + BASE_URL);
    }
    
    @AfterAll
    static void tearDown() throws IOException {
        controller.stop();
        
        // Clean up test directory
        if (Files.exists(testDataDir)) {
            Files.walk(testDataDir)
                .sorted((a, b) -> -a.compareTo(b))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        // Ignore
                    }
                });
        }
        
        System.out.println("Test cleanup complete");
    }
    
    @Test
    @Order(1)
    void testHealthEndpoint() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/v1/health"))
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("healthy"));
        
        System.out.println("✓ Health check passed");
    }
    
    @Test
    @Order(2)
    void testCreateQuest() throws Exception {
        // Create test quest
        QuestDefinition quest = createTestQuest("test_quest_1");
        String questJson = gson.toJson(quest);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/v1/quests"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(questJson))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(201, response.statusCode());
        
        QuestDefinition created = gson.fromJson(response.body(), QuestDefinition.class);
        assertEquals("test_quest_1", created.getId());
        
        System.out.println("✓ Quest created successfully");
    }
    
    @Test
    @Order(3)
    void testGetQuest() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/v1/quests/test_quest_1"))
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode());
        
        QuestDefinition quest = gson.fromJson(response.body(), QuestDefinition.class);
        assertEquals("test_quest_1", quest.getId());
        
        System.out.println("✓ Quest retrieved successfully");
    }
    
    @Test
    @Order(4)
    void testGetAllQuests() throws Exception {
        // Create another quest
        QuestDefinition quest2 = createTestQuest("test_quest_2");
        String questJson = gson.toJson(quest2);
        
        HttpRequest createRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/v1/quests"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(questJson))
            .build();
        
        httpClient.send(createRequest, HttpResponse.BodyHandlers.ofString());
        
        // Get all quests
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/v1/quests"))
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("test_quest_1"));
        assertTrue(response.body().contains("test_quest_2"));
        
        System.out.println("✓ Quest list retrieved successfully");
    }
    
    @Test
    @Order(5)
    void testUpdateQuest() throws Exception {
        QuestDefinition quest = createTestQuest("test_quest_1");
        // Modify quest (would need to update nodes/edges in real scenario)
        String questJson = gson.toJson(quest);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/v1/quests/test_quest_1"))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(questJson))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode());
        
        System.out.println("✓ Quest updated successfully");
    }
    
    @Test
    @Order(6)
    void testDeleteQuest() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/v1/quests/test_quest_2"))
            .DELETE()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(204, response.statusCode());
        
        // Verify it's deleted
        HttpRequest getRequest = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/v1/quests/test_quest_2"))
            .GET()
            .build();
        
        HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, getResponse.statusCode());
        
        System.out.println("✓ Quest deleted successfully");
    }
    
    @Test
    @Order(7)
    void testQuestNotFound() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/v1/quests/nonexistent_quest"))
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(404, response.statusCode());
        assertTrue(response.body().contains("not_found"));
        
        System.out.println("✓ 404 error handled correctly");
    }
    
    @Test
    @Order(8)
    void testQuestConflict() throws Exception {
        // Try to create quest with ID that already exists
        QuestDefinition quest = createTestQuest("test_quest_1");
        String questJson = gson.toJson(quest);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/v1/quests"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(questJson))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(409, response.statusCode());
        assertTrue(response.body().contains("conflict"));
        
        System.out.println("✓ Conflict error handled correctly");
    }
    
    // --- Helper Methods ---
    
    private QuestDefinition createTestQuest(String id) {
        QuestDefinition quest = new QuestDefinition();
        quest.setId(id);
        
        // Create simple quest structure
        QuestDefinitionNodesInner rootNode = new QuestDefinitionNodesInner();
        rootNode.setId("root");
        
        QuestDefinitionNodesInner stageNode = new QuestDefinitionNodesInner();
        stageNode.setId("stage_1");
        
        quest.setNodes(List.of(rootNode, stageNode));
        
        GraphEdge edge = new GraphEdge();
        edge.setId("edge_1");
        edge.setSource("root");
        edge.setTarget("stage_1");
        
        quest.setEdges(List.of(edge));
        
        return quest;
    }
}
