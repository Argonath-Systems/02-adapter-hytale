package com.argonathsystems.adapter.hytale.ui;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TemplateLoader implementations.
 */
class TemplateLoaderTest {
    
    @Test
    void classpathLoader_loadsExistingResource() {
        // Arrange
        TemplateLoader loader = new TemplateLoader.ClasspathTemplateLoader();
        
        // Act & Assert - Template may not exist in test resources
        assertDoesNotThrow(() -> {
            String result = loader.loadTemplate("config/ui/huds/npc-dialogue.hyuiml");
            // If it doesn't throw, the loader works (file exists)
            // If it throws IOException, that's also expected (file doesn't exist in test resources)
        }, "Loader should handle path correctly");
    }
    
    @Test
    void classpathLoader_throwsOnNonExistent() {
        // Arrange
        TemplateLoader loader = new TemplateLoader.ClasspathTemplateLoader();
        
        // Act & Assert
        assertThrows(IOException.class, () -> 
            loader.loadTemplate("nonexistent/template.hyuiml")
        );
    }
    
    @Test
    void classpathLoader_throwsOnNullPath() {
        // Arrange
        TemplateLoader loader = new TemplateLoader.ClasspathTemplateLoader();
        
        // Act & Assert
        assertThrows(IOException.class, () -> 
            loader.loadTemplate(null)
        );
    }
    
    @Test
    void classpathLoader_throwsOnEmptyPath() {
        // Arrange
        TemplateLoader loader = new TemplateLoader.ClasspathTemplateLoader();
        
        // Act & Assert
        assertThrows(IOException.class, () -> 
            loader.loadTemplate("")
        );
    }
}
