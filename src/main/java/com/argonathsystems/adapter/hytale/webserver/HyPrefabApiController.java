package com.argonathsystems.adapter.hytale.webserver;

import com.argonathsystems.adapter.hytale.webserver.PrefabService.PrefabData;
import com.argonathsystems.adapter.hytale.webserver.PrefabService.SpawnRequest;
import com.argonathsystems.adapter.hytale.webserver.PrefabService.SpawnResult;
import com.argonathsystems.framework.webserver.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HyPrefab WebUI integration controller.
 * 
 * <p>Exposes REST API endpoints for the HyPrefab visual prefab designer,
 * enabling create, read, update, and delete operations on prefab definitions.
 * 
 * <p>Uses the {@link WebServerAccessor} framework for platform-agnostic HTTP serving.
 * 
 * <h2>API Endpoints:</h2>
 * <pre>
 * GET    /api/v1/prefabs           - List all prefabs
 * GET    /api/v1/prefabs/{id}      - Get specific prefab
 * POST   /api/v1/prefabs           - Create new prefab
 * PUT    /api/v1/prefabs/{id}      - Update prefab
 * DELETE /api/v1/prefabs/{id}      - Delete prefab
 * POST   /api/v1/prefabs/{id}/spawn - Spawn prefab in world
 * </pre>
 * 
 * @author Argonath Systems
 * @version 1.1.0
 * @since 1.0.0
 */
public class HyPrefabApiController {
    
    private static final Logger LOGGER = Logger.getLogger(HyPrefabApiController.class.getName());
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private final WebServerAccessor webServer;
    private final PrefabService prefabService;
    
    /**
     * Creates a new HyPrefab API controller.
     * 
     * <p>Without a prefab service, all data operations will return 503 Service Unavailable.
     * 
     * @param webServer the web server accessor
     */
    public HyPrefabApiController(WebServerAccessor webServer) {
        this(webServer, null);
    }
    
    /**
     * Creates a new HyPrefab API controller with prefab service.
     * 
     * @param webServer the web server accessor
     * @param prefabService the prefab service (can be null)
     */
    public HyPrefabApiController(WebServerAccessor webServer, PrefabService prefabService) {
        this.webServer = webServer;
        this.prefabService = prefabService;
    }
    
    /**
     * Registers all API routes with the web server.
     */
    public void register() {
        webServer.registerRoute("/api/v1/prefabs", HttpMethod.GET, this::listPrefabs);
        webServer.registerRoute("/api/v1/prefabs/:id", HttpMethod.GET, this::getPrefab);
        webServer.registerRoute("/api/v1/prefabs", HttpMethod.POST, this::createPrefab);
        webServer.registerRoute("/api/v1/prefabs/:id", HttpMethod.PUT, this::updatePrefab);
        webServer.registerRoute("/api/v1/prefabs/:id", HttpMethod.DELETE, this::deletePrefab);
        webServer.registerRoute("/api/v1/prefabs/:id/spawn", HttpMethod.POST, this::spawnPrefab);
        
        LOGGER.log(Level.INFO, "Registered HyPrefab API endpoints at {0}/api/v1/prefabs",
            webServer.getBaseUrl());
    }
    
    /**
     * Unregisters all routes (cleanup).
     */
    public void unregister() {
        webServer.unregisterAllRoutes(this);
        LOGGER.log(Level.INFO, "Unregistered HyPrefab API endpoints");
    }
    
    private void listPrefabs(HttpRequest req, HttpResponse res) {
        try {
            if (!checkPermission(req, "argonath.hyprefab.web.read")) {
                res.writeError(403, "Insufficient permissions");
                return;
            }
            
            if (prefabService == null) {
                res.writeError(503, "Prefab service not available");
                return;
            }
            
            String searchTerm = req.getQueryParam("search").orElse(null);
            List<PrefabData> prefabs = prefabService.listAll(searchTerm);
            
            res.writeJson(GSON.toJson(prefabs));
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error listing prefabs", e);
            res.writeError(500, "Internal server error");
        }
    }
    
    private void getPrefab(HttpRequest req, HttpResponse res) {
        try {
            if (!checkPermission(req, "argonath.hyprefab.web.read")) {
                res.writeError(403, "Insufficient permissions");
                return;
            }
            
            String prefabId = req.getPathParam("id");
            if (prefabId == null || prefabId.isBlank()) {
                res.writeError(400, "Missing prefab ID");
                return;
            }
            
            if (prefabService == null) {
                res.writeError(503, "Prefab service not available");
                return;
            }
            
            Optional<PrefabData> prefab = prefabService.getById(prefabId);
            if (prefab.isPresent()) {
                res.writeJson(GSON.toJson(prefab.get()));
            } else {
                res.writeError(404, "Prefab not found: " + prefabId);
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting prefab", e);
            res.writeError(500, "Internal server error");
        }
    }
    
    private void createPrefab(HttpRequest req, HttpResponse res) {
        try {
            if (!checkPermission(req, "argonath.hyprefab.web.write")) {
                res.writeError(403, "Insufficient permissions");
                return;
            }
            
            if (prefabService == null) {
                res.writeError(503, "Prefab service not available");
                return;
            }
            
            PrefabData newPrefab = parseBody(req, PrefabData.class);
            if (newPrefab == null) {
                res.writeError(400, "Invalid request body");
                return;
            }
            
            PrefabData created = prefabService.create(newPrefab);
            
            res.setStatus(201); // Created
            res.writeJson(GSON.toJson(created));
            
        } catch (JsonSyntaxException e) {
            res.writeError(400, "Invalid JSON: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating prefab", e);
            res.writeError(500, "Internal server error");
        }
    }
    
    private void updatePrefab(HttpRequest req, HttpResponse res) {
        try {
            if (!checkPermission(req, "argonath.hyprefab.web.write")) {
                res.writeError(403, "Insufficient permissions");
                return;
            }
            
            String prefabId = req.getPathParam("id");
            if (prefabId == null || prefabId.isBlank()) {
                res.writeError(400, "Missing prefab ID");
                return;
            }
            
            if (prefabService == null) {
                res.writeError(503, "Prefab service not available");
                return;
            }
            
            PrefabData updatedPrefab = parseBody(req, PrefabData.class);
            if (updatedPrefab == null) {
                res.writeError(400, "Invalid request body");
                return;
            }
            
            Optional<PrefabData> result = prefabService.update(prefabId, updatedPrefab);
            if (result.isPresent()) {
                res.writeJson(GSON.toJson(result.get()));
            } else {
                res.writeError(404, "Prefab not found: " + prefabId);
            }
            
        } catch (JsonSyntaxException e) {
            res.writeError(400, "Invalid JSON: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating prefab", e);
            res.writeError(500, "Internal server error");
        }
    }
    
    private void deletePrefab(HttpRequest req, HttpResponse res) {
        try {
            if (!checkPermission(req, "argonath.hyprefab.web.write")) {
                res.writeError(403, "Insufficient permissions");
                return;
            }
            
            String prefabId = req.getPathParam("id");
            if (prefabId == null || prefabId.isBlank()) {
                res.writeError(400, "Missing prefab ID");
                return;
            }
            
            if (prefabService == null) {
                res.writeError(503, "Prefab service not available");
                return;
            }
            
            boolean deleted = prefabService.delete(prefabId);
            if (deleted) {
                res.setStatus(204); // No Content
            } else {
                res.writeError(404, "Prefab not found: " + prefabId);
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting prefab", e);
            res.writeError(500, "Internal server error");
        }
    }
    
    private void spawnPrefab(HttpRequest req, HttpResponse res) {
        try {
            if (!checkPermission(req, "argonath.hyprefab.admin")) {
                res.writeError(403, "Insufficient permissions");
                return;
            }
            
            String prefabId = req.getPathParam("id");
            if (prefabId == null || prefabId.isBlank()) {
                res.writeError(400, "Missing prefab ID");
                return;
            }
            
            if (prefabService == null) {
                res.writeError(503, "Prefab service not available");
                return;
            }
            
            SpawnRequest spawnRequest = parseBody(req, SpawnRequest.class);
            if (spawnRequest == null) {
                res.writeError(400, "Invalid spawn request");
                return;
            }
            
            SpawnResult result = prefabService.spawn(prefabId, spawnRequest);
            
            if (result.success()) {
                res.writeJson(GSON.toJson(result));
            } else {
                res.writeError(400, result.message());
            }
            
        } catch (JsonSyntaxException e) {
            res.writeError(400, "Invalid JSON: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error spawning prefab", e);
            res.writeError(500, "Internal server error");
        }
    }
    
    /**
     * Parses the request body as the specified type.
     * 
     * @param req the HTTP request
     * @param type the type to parse as
     * @return the parsed object, or null if body is empty
     */
    private <T> T parseBody(HttpRequest req, Class<T> type) {
        String body = req.getBody();
        if (body == null || body.isBlank()) {
            return null;
        }
        return GSON.fromJson(body, type);
    }
    
    /**
     * Checks if the current user has the required permission.
     */
    private boolean checkPermission(HttpRequest req, String permission) {
        return req.getUser()
            .map(user -> user.hasPermission(permission))
            .orElse(false); // Require authentication for all endpoints
    }
}
