package com.argonathsystems.adapter.hytale.webserver;

import com.argonathsystems.framework.webserver.*;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.nitrado.hytale.plugins.webserver.WebServerPlugin;
import net.nitrado.hytale.plugins.webserver.authentication.HytaleUserPrincipal;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Nitrado WebServer adapter implementation of {@link WebServerAccessor}.
 * 
 * <p>This adapter bridges the Argonath WebServer Framework to the Nitrado WebServer Plugin,
 * enabling platform-agnostic HTTP API exposure from Hytale plugins.
 * 
 * <p><b>THIS IS THE ONLY CLASS IN THE PROJECT THAT IMPORTS NITRADO WEBSERVER CODE.</b>
 * All business logic uses {@link WebServerAccessor} interface.
 * 
 * @author Argonath Systems
 * @version 1.0.0
 * @since 1.0.0
 */
public class NitradoWebServerAdapter implements WebServerAccessor {
    
    private static final Logger LOGGER = Logger.getLogger(NitradoWebServerAdapter.class.getName());
    
    private final WebServerPlugin nitradoPlugin;
    private final PluginBase pluginOwner;
    private final String pluginGroup;
    private final String pluginName;
    private final Map<String, RegisteredRoute> routes = new ConcurrentHashMap<>();
    
    /**
     * Creates a new Nitrado WebServer adapter.
     * 
     * @param nitradoPlugin the Nitrado WebServer plugin instance
     * @param pluginOwner the owning plugin (for route cleanup)
     * @param pluginGroup the plugin group (e.g., "Argonath")
     * @param pluginName the plugin name (e.g., "QuestApi")
     */
    public NitradoWebServerAdapter(
        WebServerPlugin nitradoPlugin,
        PluginBase pluginOwner,
        String pluginGroup,
        String pluginName
    ) {
        this.nitradoPlugin = Objects.requireNonNull(nitradoPlugin, "nitradoPlugin cannot be null");
        this.pluginOwner = Objects.requireNonNull(pluginOwner, "pluginOwner cannot be null");
        this.pluginGroup = Objects.requireNonNull(pluginGroup, "pluginGroup cannot be null");
        this.pluginName = Objects.requireNonNull(pluginName, "pluginName cannot be null");
        
        LOGGER.log(Level.INFO, "Initialized Nitrado WebServer adapter for {0}/{1}", 
            new Object[]{pluginGroup, pluginName});
    }
    
    @Override
    public void registerRoute(String path, RouteHandler handler) {
        registerRoute(path, HttpMethod.GET, handler);
    }
    
    @Override
    public void registerRoute(String path, HttpMethod method, RouteHandler handler) {
        Objects.requireNonNull(path, "path cannot be null");
        Objects.requireNonNull(method, "method cannot be null");
        Objects.requireNonNull(handler, "handler cannot be null");
        
        if (!isAvailable()) {
            throw new IllegalStateException("Nitrado WebServer plugin is not available");
        }
        
        try {
            // Create servlet wrapper
            HttpServlet servlet = new RouteHandlerServlet(handler, method);
            
            // Register with Nitrado plugin
            nitradoPlugin.addServlet(pluginOwner, path, servlet);
            
            // Track route for cleanup
            String routeKey = method + ":" + path;
            routes.put(routeKey, new RegisteredRoute(path, method, servlet));
            
            LOGGER.log(Level.INFO, "Registered route: {0} {1} -> {2}/{3}{4}",
                new Object[]{method, path, pluginGroup, pluginName, path});
                
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to register route: " + method + " " + path, e);
            throw new RuntimeException("Failed to register route: " + path, e);
        }
    }
    
    @Override
    public void unregisterRoute(String path) {
        // Find and remove matching routes
        routes.entrySet().removeIf(entry -> {
            if (entry.getValue().path.equals(path)) {
                try {
                    nitradoPlugin.removeServlet(pluginOwner, path);
                    LOGGER.log(Level.INFO, "Unregistered route: {0}", path);
                    return true;
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to unregister route: " + path, e);
                    return false;
                }
            }
            return false;
        });
    }
    
    @Override
    public void unregisterAllRoutes(Object owner) {
        if (!(owner instanceof PluginBase) || owner != pluginOwner) {
            LOGGER.log(Level.WARNING, "Attempted to unregister routes with different owner");
            return;
        }
        
        try {
            nitradoPlugin.removeServlets(pluginOwner);
            routes.clear();
            LOGGER.log(Level.INFO, "Unregistered all routes for {0}/{1}",
                new Object[]{pluginGroup, pluginName});
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to unregister all routes", e);
        }
    }
    
    @Override
    public boolean isAvailable() {
        return nitradoPlugin != null;
    }
    
    @Override
    public int getPort() {
        if (!isAvailable()) {
            throw new IllegalStateException("Nitrado WebServer plugin is not available");
        }
        // Nitrado plugin doesn't expose port directly - use default or config
        return 7003; // Default Nitrado WebServer port (game port + 3)
    }
    
    @Override
    public String getBaseUrl() {
        if (!isAvailable()) {
            throw new IllegalStateException("Nitrado WebServer plugin is not available");
        }
        return String.format("https://localhost:%d/%s/%s", getPort(), pluginGroup, pluginName);
    }
    
    /**
     * Internal servlet that wraps a RouteHandler.
     */
    private static class RouteHandlerServlet extends HttpServlet {
        private final RouteHandler handler;
        private final HttpMethod allowedMethod;
        
        public RouteHandlerServlet(RouteHandler handler, HttpMethod allowedMethod) {
            this.handler = handler;
            this.allowedMethod = allowedMethod;
        }
        
        @Override
        protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Check HTTP method
            if (!req.getMethod().equalsIgnoreCase(allowedMethod.name())) {
                resp.setStatus(405); // Method Not Allowed
                resp.getWriter().write("{\"error\":\"Method not allowed\"}");
                return;
            }
            
            try {
                // Convert Jakarta Servlet objects to framework interfaces
                HttpRequest request = new ServletHttpRequest(req);
                HttpResponse response = new ServletHttpResponse(resp);
                
                // Call the route handler
                handler.handle(request, response);
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error in route handler", e);
                resp.setStatus(500);
                resp.setContentType("application/json");
                resp.getWriter().write(String.format(
                    "{\"error\":\"Internal server error: %s\"}", 
                    e.getMessage()
                ));
            }
        }
    }
    
    /**
     * Adapter from Jakarta HttpServletRequest to framework HttpRequest.
     */
    private static class ServletHttpRequest implements HttpRequest {
        private final HttpServletRequest servletRequest;
        private final Map<String, String> pathParams = new HashMap<>();
        
        public ServletHttpRequest(HttpServletRequest servletRequest) {
            this.servletRequest = servletRequest;
            
            // Extract path parameters from request attributes
            // Nitrado plugin stores matched path parameters in request attributes
            // Standard servlet pattern is to use request attributes with "pathParam." prefix
            var attributeNames = servletRequest.getAttributeNames();
            while (attributeNames.hasMoreElements()) {
                String attrName = attributeNames.nextElement();
                if (attrName.startsWith("pathParam.")) {
                    String paramName = attrName.substring("pathParam.".length());
                    Object value = servletRequest.getAttribute(attrName);
                    if (value != null) {
                        pathParams.put(paramName, value.toString());
                    }
                }
            }
            
            // Also check for common servlet path variable patterns
            // Some implementations use different attribute names
            Object pathVariables = servletRequest.getAttribute("org.springframework.web.servlet.HandlerMapping.uriTemplateVariables");
            if (pathVariables instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> vars = (Map<String, Object>) pathVariables;
                vars.forEach((key, value) -> {
                    if (value != null) {
                        pathParams.put(key, value.toString());
                    }
                });
            }
        }
        
        @Override
        public HttpMethod getMethod() {
            return HttpMethod.valueOf(servletRequest.getMethod().toUpperCase());
        }
        
        @Override
        public String getPath() {
            return servletRequest.getPathInfo() != null ? servletRequest.getPathInfo() : "/";
        }
        
        @Override
        public Optional<String> getQueryParam(String name) {
            return Optional.ofNullable(servletRequest.getParameter(name));
        }
        
        @Override
        public Map<String, String> getQueryParams() {
            Map<String, String> params = new HashMap<>();
            servletRequest.getParameterMap().forEach((key, values) -> {
                if (values != null && values.length > 0) {
                    params.put(key, values[0]);
                }
            });
            return params;
        }
        
        @Override
        public String getPathParam(String name) {
            return pathParams.get(name);
        }
        
        @Override
        public Map<String, String> getPathParams() {
            return new HashMap<>(pathParams);
        }
        
        @Override
        public Optional<String> getHeader(String name) {
            return Optional.ofNullable(servletRequest.getHeader(name));
        }
        
        @Override
        public Map<String, String> getHeaders() {
            Map<String, String> headers = new HashMap<>();
            servletRequest.getHeaderNames().asIterator().forEachRemaining(name ->
                headers.put(name, servletRequest.getHeader(name))
            );
            return headers;
        }
        
        @Override
        public String getBody() {
            try {
                return new String(servletRequest.getInputStream().readAllBytes());
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to read request body", e);
                return "";
            }
        }
        
        @Override
        public Optional<UserPrincipal> getUser() {
            var principal = servletRequest.getUserPrincipal();
            if (principal instanceof HytaleUserPrincipal hytaleUser) {
                return Optional.of(new NitradoUserPrincipal(hytaleUser));
            }
            return Optional.empty();
        }
        
        @Override
        public Optional<String> getContentType() {
            return Optional.ofNullable(servletRequest.getContentType());
        }
    }
    
    /**
     * Adapter from Jakarta HttpServletResponse to framework HttpResponse.
     */
    private static class ServletHttpResponse implements HttpResponse {
        private final HttpServletResponse servletResponse;
        
        public ServletHttpResponse(HttpServletResponse servletResponse) {
            this.servletResponse = servletResponse;
        }
        
        @Override
        public void setStatus(int code) {
            servletResponse.setStatus(code);
        }
        
        @Override
        public void setHeader(String name, String value) {
            servletResponse.setHeader(name, value);
        }
        
        @Override
        public void setContentType(String contentType) {
            servletResponse.setContentType(contentType);
        }
        
        @Override
        public void write(String content) {
            try {
                servletResponse.getWriter().write(content);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to write response", e);
                throw new RuntimeException("Failed to write response", e);
            }
        }
    }
    
    /**
     * Adapter from Nitrado HytaleUserPrincipal to framework UserPrincipal.
     */
    private static class NitradoUserPrincipal implements UserPrincipal {
        private final HytaleUserPrincipal nitradoUser;
        
        public NitradoUserPrincipal(HytaleUserPrincipal nitradoUser) {
            this.nitradoUser = nitradoUser;
        }
        
        @Override
        public String getName() {
            return nitradoUser.getName();
        }
        
        @Override
        public boolean hasPermission(String permission) {
            return nitradoUser.hasPermission(permission);
        }
        
        @Override
        public Set<String> getPermissions() {
            // Nitrado doesn't expose all permissions directly
            // Return empty set - permissions are checked individually
            return Collections.emptySet();
        }
        
        @Override
        public boolean isPlayer() {
            // Service accounts have names starting with "serviceaccount."
            return !nitradoUser.getName().startsWith("serviceaccount.");
        }
    }
    
    /**
     * Internal record for tracking registered routes.
     */
    private record RegisteredRoute(String path, HttpMethod method, HttpServlet servlet) {}
}
