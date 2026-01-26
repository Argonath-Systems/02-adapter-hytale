package com.argonathsystems.adapter.hytale.api;

import com.argonathsystems.hyquest.model.QuestDefinition;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.nio.file.Path;
import java.util.List;

/**
 * HTTP REST controller for HyQuestUI integration.
 * 
 * <p>Exposes {@link HyQuestApiService} as REST endpoints matching the
 * OpenAPI specification defined in {@code 99-HyQuest-WebUI/swagger.yaml}.
 * 
 * <p>Uses Javalin lightweight HTTP framework for simplicity and performance.
 * 
 * <h2>Endpoints:</h2>
 * <pre>
 * GET    /api/v1/quests           - List all quests
 * GET    /api/v1/quests/{id}      - Get specific quest
 * POST   /api/v1/quests           - Create new quest
 * PUT    /api/v1/quests/{id}      - Update quest
 * DELETE /api/v1/quests/{id}      - Delete quest
 * </pre>
 * 
 * <h2>CORS Configuration:</h2>
 * CORS is enabled for all origins to allow the WebUI to connect.
 * 
 * @author Argonath Systems
 * @version 1.0.0
 * @since 1.0.0
 */
public class HyQuestHttpController {
    
    private final HyQuestApiService apiService;
    private final Javalin app;
    private final int port;
    
    /**
     * Create a new HTTP controller.
     * 
     * @param dataDirectory Server data directory for quest storage
     * @param port HTTP port to listen on (default: 19133)
     */
    public HyQuestHttpController(Path dataDirectory, int port) {
        this.apiService = new HyQuestApiService(dataDirectory);
        this.port = port;
        this.app = createJavalinApp();
        registerRoutes();
    }
    
    /**
     * Create Javalin app with CORS configuration.
     */
    private Javalin createJavalinApp() {
        return Javalin.create(config -> {
            // Enable CORS for WebUI
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.anyHost(); // Allow any origin (WebUI running on different port)
                });
            });
            
            // Disable auto-generation of 404 responses
            config.http.prefer405over404 = true;
            
            // Set default content type
            config.http.defaultContentType = "application/json";
        });
    }
    
    /**
     * Register all REST API routes.
     */
    private void registerRoutes() {
        // GET /api/v1/quests - List all quests
        app.get("/api/v1/quests", this::getAllQuests);
        
        // GET /api/v1/quests/{id} - Get specific quest
        app.get("/api/v1/quests/{id}", this::getQuest);
        
        // POST /api/v1/quests - Create new quest
        app.post("/api/v1/quests", this::createQuest);
        
        // PUT /api/v1/quests/{id} - Update quest
        app.put("/api/v1/quests/{id}", this::updateQuest);
        
        // DELETE /api/v1/quests/{id} - Delete quest
        app.delete("/api/v1/quests/{id}", this::deleteQuest);
        
        // Health check endpoint
        app.get("/api/v1/health", ctx -> {
            ctx.json(new HealthResponse("healthy", apiService.getQuestCount()));
        });
        
        // Exception handlers
        app.exception(IllegalArgumentException.class, (e, ctx) -> {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(new ErrorResponse("bad_request", e.getMessage()));
        });
        
        app.exception(Exception.class, (e, ctx) -> {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(new ErrorResponse("internal_error", e.getMessage()));
        });
    }
    
    /**
     * Start the HTTP server.
     * 
     * @return This controller instance for chaining
     */
    public HyQuestHttpController start() {
        app.start(port);
        System.out.println("HyQuest API Server started on http://localhost:" + port);
        System.out.println("  - Quest count: " + apiService.getQuestCount());
        System.out.println("  - Storage: " + apiService.getQuestStoragePath());
        return this;
    }
    
    /**
     * Stop the HTTP server.
     */
    public void stop() {
        app.stop();
        System.out.println("HyQuest API Server stopped");
    }
    
    /**
     * Check if server is running.
     */
    public boolean isRunning() {
        return app.jettyServer() != null && app.jettyServer().server().isStarted();
    }
    
    /**
     * Get the API service (for advanced usage).
     */
    public HyQuestApiService getApiService() {
        return apiService;
    }
    
    // --- Route Handlers ---
    
    private void getAllQuests(Context ctx) {
        List<QuestDefinition> quests = apiService.getAllQuests();
        
        // Support for pagination (simplified)
        Integer page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
        Integer pageSize = ctx.queryParamAsClass("pageSize", Integer.class).getOrDefault(20);
        
        // Simple pagination
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, quests.size());
        
        if (start >= quests.size()) {
            ctx.json(new PaginatedResponse<>(List.of(), quests.size(), page, pageSize));
        } else {
            List<QuestDefinition> pageQuests = quests.subList(start, end);
            ctx.json(new PaginatedResponse<>(pageQuests, quests.size(), page, pageSize));
        }
    }
    
    private void getQuest(Context ctx) {
        String questId = ctx.pathParam("id");
        QuestDefinition quest = apiService.getQuest(questId);
        
        if (quest == null) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ErrorResponse("not_found", "Quest not found: " + questId));
        } else {
            ctx.json(quest);
        }
    }
    
    private void createQuest(Context ctx) {
        QuestDefinition quest = ctx.bodyAsClass(QuestDefinition.class);
        
        try {
            QuestDefinition created = apiService.createQuest(quest);
            ctx.status(HttpStatus.CREATED);
            ctx.json(created);
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.CONFLICT);
            ctx.json(new ErrorResponse("conflict", e.getMessage()));
        }
    }
    
    private void updateQuest(Context ctx) {
        String questId = ctx.pathParam("id");
        QuestDefinition quest = ctx.bodyAsClass(QuestDefinition.class);
        
        try {
            QuestDefinition updated = apiService.updateQuest(questId, quest);
            ctx.json(updated);
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ErrorResponse("not_found", e.getMessage()));
        }
    }
    
    private void deleteQuest(Context ctx) {
        String questId = ctx.pathParam("id");
        boolean deleted = apiService.deleteQuest(questId);
        
        if (deleted) {
            ctx.status(HttpStatus.NO_CONTENT);
        } else {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(new ErrorResponse("not_found", "Quest not found: " + questId));
        }
    }
    
    // --- Response DTOs ---
    
    private record PaginatedResponse<T>(
        List<T> items,
        int total,
        int page,
        int pageSize
    ) {
        public int totalPages() {
            return (int) Math.ceil((double) total / pageSize);
        }
    }
    
    private record ErrorResponse(
        String error,
        String message
    ) {}
    
    private record HealthResponse(
        String status,
        int questCount
    ) {}
}
