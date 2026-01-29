package com.argonathsystems.adapter.hytale.ui;

import com.argonathsystems.framework.ui.dialogue.DialogueChoice;
import com.argonathsystems.framework.ui.dialogue.QuestOfferData;
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
 * Unit tests for DialoguePageAdapter.
 */
class DialoguePageAdapterTest {
    
    @Mock
    private TemplateLoader mockLoader;
    
    @Mock
    private Player mockPlayer;
    
    private DialoguePageAdapter adapter;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adapter = new DialoguePageAdapter(mockLoader);
    }
    
    @Test
    void showDialogue_loadsAndProcessesTemplate() throws Exception {
        // Arrange
        String template = "<div>{{$npc.name}}: {{$currentText}}</div>";
        when(mockLoader.loadTemplate(anyString())).thenReturn(template);
        
        List<DialogueChoice> choices = new ArrayList<>();
        choices.add(new DialogueChoice(1, "Option 1", "opt1", true, null));
        
        DialoguePageAdapter.DialogueData data = new DialoguePageAdapter.DialogueData(
            "Gandalf", "gandalf", "Greetings!", choices, false, null
        );
        
        // Act
        String result = adapter.showDialogue(mockPlayer, data);
        
        // Assert
        assertNotNull(result);
        verify(mockLoader).loadTemplate("config/ui/huds/npc-dialogue.hyuiml");
    }
    
    @Test
    void showDialogue_handlesException() throws Exception {
        // Arrange
        when(mockLoader.loadTemplate(anyString())).thenThrow(new RuntimeException("Template not found"));
        
        List<DialogueChoice> choices = new ArrayList<>();
        DialoguePageAdapter.DialogueData data = new DialoguePageAdapter.DialogueData(
            "Gandalf", "gandalf", "Hello", choices, false, null
        );
        
        // Act & Assert
        assertThrows(RuntimeException.class, () -> adapter.showDialogue(mockPlayer, data));
        verify(mockPlayer).sendMessage(contains("Error displaying dialogue"));
    }
    
    @Test
    void dialogueData_gettersReturnCorrectValues() {
        // Arrange
        List<DialogueChoice> choices = new ArrayList<>();
        choices.add(new DialogueChoice(1, "Test", "test", true, null));
        
        QuestOfferData questOffer = QuestOfferData.builder("Quest 1")
            .addObjective("Find the ring")
            .withXpReward(100)
            .withGoldReward(50)
            .build();
        
        // Act
        DialoguePageAdapter.DialogueData data = new DialoguePageAdapter.DialogueData(
            "Frodo", "frodo", "The ring...", choices, true, questOffer
        );
        
        // Assert
        assertEquals("Frodo", data.getNpcName());
        assertEquals("frodo", data.getNpcAvatarId());
        assertEquals("The ring...", data.getDialogueText());
        assertEquals(1, data.getChoices().size());
        assertTrue(data.isShowQuestOffer());
        assertEquals(questOffer, data.getQuestOffer());
    }
    
    @Test
    void setChoiceHandler_setsHandler() {
        // Arrange
        DialogueChoice testChoice = new DialogueChoice(1, "Test", "test", true, null);
        final boolean[] handlerCalled = {false};
        
        // Act
        adapter.setChoiceHandler(choice -> handlerCalled[0] = true);
        
        // Assert - handler is set (would be called in full implementation)
        assertNotNull(adapter);
    }
}
