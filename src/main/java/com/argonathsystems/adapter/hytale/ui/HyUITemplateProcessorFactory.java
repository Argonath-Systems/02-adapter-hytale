package com.argonathsystems.adapter.hytale.ui;

import com.argonathsystems.framework.ui.template.TemplateLoader;
import com.argonathsystems.framework.ui.template.TemplateProcessorFactory;
import com.argonathsystems.framework.ui.template.TemplateProcessorWrapper;

import java.util.Map;
import java.util.Optional;

/**
 * HyUI implementation of TemplateProcessorFactory.
 * 
 * <p>This factory creates HyUITemplateProcessor instances for use in the
 * UI framework layer. It also provides component set preloading support.
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * HyUITemplateProcessorFactory factory = new HyUITemplateProcessorFactory();
 * TemplateProcessorWrapper processor = factory.create();
 * }</pre>
 * 
 * <h2>Component Sets</h2>
 * <p>Component sets are predefined groups of reusable UI components:
 * <ul>
 *   <li>"lotr" - LOTR-themed components (stat-card, feature-item, etc.)</li>
 *   <li>"combat" - Combat UI components</li>
 *   <li>"dialogue" - Dialogue system components</li>
 * </ul>
 * 
 * @author Argonath Systems Team
 * @version 1.1.0
 * @since 1.1.0
 */
public class HyUITemplateProcessorFactory implements TemplateProcessorFactory {
    
    private final TemplateLoader componentLoader;
    private final Map<String, ComponentSet> componentSets;
    
    /**
     * Creates a new factory with default component loader.
     */
    public HyUITemplateProcessorFactory() {
        this(new TemplateLoader());
    }
    
    /**
     * Creates a new factory with a custom template loader.
     * 
     * @param componentLoader the loader for component templates
     */
    public HyUITemplateProcessorFactory(TemplateLoader componentLoader) {
        this.componentLoader = componentLoader;
        this.componentSets = initializeComponentSets();
    }
    
    @Override
    public TemplateProcessorWrapper create() {
        return new HyUITemplateProcessor();
    }
    
    @Override
    public TemplateProcessorWrapper createWithComponents(String componentSet) {
        HyUITemplateProcessor processor = new HyUITemplateProcessor();
        
        ComponentSet set = componentSets.get(componentSet);
        if (set != null) {
            set.registerComponents(processor, componentLoader);
        }
        
        return processor;
    }
    
    /**
     * Initializes the predefined component sets.
     */
    private Map<String, ComponentSet> initializeComponentSets() {
        return Map.of(
            "lotr", new LotrComponentSet(),
            "combat", new CombatComponentSet(),
            "dialogue", new DialogueComponentSet()
        );
    }
    
    /**
     * Interface for component sets that can register components with processors.
     */
    interface ComponentSet {
        void registerComponents(HyUITemplateProcessor processor, TemplateLoader loader);
    }
    
    /**
     * LOTR-themed UI component set.
     */
    private static class LotrComponentSet implements ComponentSet {
        @Override
        public void registerComponents(HyUITemplateProcessor processor, TemplateLoader loader) {
            // Stat Card - displays a labeled value
            processor.registerComponent("statCard", """
                <div style="background-color: rgba(45, 36, 30, 0.9); anchor-width: 120; anchor-height: 60;">
                    <p style="color: #a89885; font-size: 11;">{{$label}}</p>
                    <p style="color: #f4e8d0; font-size: 18; font-weight: bold;">{{$value}}</p>
                </div>
                """);
            
            // Feature Item - bullet point in a list
            processor.registerComponent("featureItem", """
                <div style="layout-mode: Left; anchor-height: 24;">
                    <p style="color: #d4a017; font-size: 14;">•</p>
                    <p style="color: #f4e8d0; font-size: 13;">{{$text}}</p>
                </div>
                """);
            
            // Gold Amount - formatted gold display
            processor.registerComponent("goldAmount", """
                <p style="color: #d4a017; font-size: 14; font-weight: bold;">💰 {{$amount}}g</p>
                """);
            
            // XP Amount - formatted XP display
            processor.registerComponent("xpAmount", """
                <p style="color: #90ee90; font-size: 14; font-weight: bold;">+{{$amount}} XP</p>
                """);
        }
    }
    
    /**
     * Combat UI component set.
     */
    private static class CombatComponentSet implements ComponentSet {
        @Override
        public void registerComponents(HyUITemplateProcessor processor, TemplateLoader loader) {
            // Health Bar
            processor.registerComponent("healthBar", """
                <div style="anchor-height: 20; background-color: rgba(0, 0, 0, 0.6);">
                    <div style="anchor-width: {{$percent}}%; anchor-height: 100%; background-color: #dc143c;"></div>
                    <p style="font-size: 11; color: #ffffff;">{{$current}}/{{$max}}</p>
                </div>
                """);
            
            // Mana Bar
            processor.registerComponent("manaBar", """
                <div style="anchor-height: 20; background-color: rgba(0, 0, 0, 0.6);">
                    <div style="anchor-width: {{$percent}}%; anchor-height: 100%; background-color: #4169e1;"></div>
                    <p style="font-size: 11; color: #ffffff;">{{$current}}/{{$max}}</p>
                </div>
                """);
            
            // Buff Icon
            processor.registerComponent("buffIcon", """
                <img src="{{$icon}}" style="anchor-width: 20; anchor-height: 20; background-color: rgba(144, 238, 144, 0.3);"
                     data-hyui-tooltiptext="{{$name}} - {{$duration}}s">
                """);
        }
    }
    
    /**
     * Dialogue system component set.
     */
    private static class DialogueComponentSet implements ComponentSet {
        @Override
        public void registerComponents(HyUITemplateProcessor processor, TemplateLoader loader) {
            // Dialogue Choice
            processor.registerComponent("dialogueChoice", """
                <button id="choice-{{$id}}" 
                        class="custom-textbutton dialogue-choice"
                        data-choice-id="{{$id}}"
                        data-hyui-default-label-style="color: #f4e8d0;"
                        data-hyui-hovered-label-style="color: #ffffff; font-weight: bold;">
                    <span style="color: #d4a017; font-weight: bold; font-size: 13;">[{{$index}}]</span>
                    <span style="color: #f4e8d0; font-size: 13; flex-weight: 1;">{{$text}}</span>
                </button>
                """);
            
            // NPC Name Header
            processor.registerComponent("npcHeader", """
                <div style="layout-mode: Left;">
                    <p style="font-size: 18; font-weight: bold; color: #d4a017; text-transform: uppercase;">{{$name}}</p>
                    {{#if level}}<p style="font-size: 11; color: #a89885;">Level {{$level}}</p>{{/if}}
                </div>
                """);
            
            // Load external dialogue choice component if available
            Optional<String> choiceTemplate = loader.loadComponentTemplate("dialogue-choice");
            choiceTemplate.ifPresent(template -> processor.registerComponent("dialogueChoiceExternal", template));
        }
    }
}
