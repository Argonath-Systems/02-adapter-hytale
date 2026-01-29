package com.argonathsystems.adapter.hytale.ui;

import com.argonathsystems.framework.ui.quest.QuestCategory;
import com.argonathsystems.framework.ui.quest.QuestDetail;
import com.argonathsystems.framework.ui.quest.QuestListItem;
import com.hytale.api.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for QuestBookPageAdapter.
 */
class QuestBookPageAdapterTest {
    
    @Mock
    private TemplateLoader mockLoader;
    
    @Mock
    private Player mockPlayer;
    
    private QuestBookPageAdapter adapter;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adapter = new QuestBookPageAdapter(mockLoader);
    }
    
    @Test
    void showQuestBook_loadsAndProcessesTemplate() throws Exception {
        // Arrange
        String template = "<div>Quest Book: {{$activeTab}}</div>";
        when(mockLoader.loadTemplate(anyString())).thenReturn(template);
        
        List<QuestCategory> categories = new ArrayList<>();
        QuestCategory category = new QuestCategory("Main Quests");
        category.addQuest(new QuestListItem("q1", "Quest 1", 50, "Main", "icon.png"));
        categories.add(category);
        
        QuestBookPageAdapter.QuestBookData data = new QuestBookPageAdapter.QuestBookData(
            "active", categories, null
        );
        
        // Act
        String result = adapter.showQuestBook(mockPlayer, data);
        
        // Assert
        assertNotNull(result);
        verify(mockLoader).loadTemplate("config/ui/pages/quest-book.hyuiml");
    }
    
    @Test
    void questBookData_withActiveTab_createsNewInstance() {
        // Arrange
        List<QuestCategory> categories = new ArrayList<>();
        QuestBookPageAdapter.QuestBookData original = new QuestBookPageAdapter.QuestBookData(
            "active", categories, null
        );
        
        // Act
        QuestBookPageAdapter.QuestBookData updated = original.withActiveTab("complete");
        
        // Assert
        assertEquals("active", original.getActiveTab());
        assertEquals("complete", updated.getActiveTab());
        assertSame(categories, updated.getCategories());
    }
    
    @Test
    void questBookData_withSelectedQuest_createsNewInstance() {
        // Arrange
        List<QuestCategory> categories = new ArrayList<>();
        QuestBookPageAdapter.QuestBookData original = new QuestBookPageAdapter.QuestBookData(
            "active", categories, null
        );
        
        QuestDetail quest = QuestDetail.builder("q1", "Quest 1")
            .withDescription("Description")
            .withType("Main")
            .withLevel(50)
            .build();
        
        // Act
        QuestBookPageAdapter.QuestBookData updated = original.withSelectedQuest(quest);
        
        // Assert
        assertNull(original.getSelectedQuest());
        assertEquals(quest, updated.getSelectedQuest());
    }
}
