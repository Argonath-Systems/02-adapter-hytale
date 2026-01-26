package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.ConfigAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hytale.api.Server;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class HytaleConfigAccessor implements ConfigAccessor {
    private final Server server;
    private final ObjectMapper mapper;
    private final Path configDir;

    public HytaleConfigAccessor(Server server) {
        this.server = server;
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        // Use server's config directory or default to ./config
        this.configDir = Paths.get("config");
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create config directory", e);
        }
    }

    @Override
    public <T> Optional<T> load(String name, Class<T> type) {
        Path configFile = configDir.resolve(name + ".json");
        
        if (!Files.exists(configFile)) {
            return Optional.empty();
        }
        
        try {
            T config = mapper.readValue(configFile.toFile(), type);
            return Optional.of(config);
        } catch (IOException e) {
            System.err.println("Failed to load config " + name + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public <T> void save(String name, T config) {
        Path configFile = configDir.resolve(name + ".json");
        
        try {
            mapper.writeValue(configFile.toFile(), config);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config " + name, e);
        }
    }
}
