package com.argonathsystems.adapter.hytale.ui;

import au.ellie.hyui.html.TemplateProcessor;
import com.argonathsystems.framework.accessorapi.data.DataValue;
import com.argonathsystems.framework.ui.template.TemplateProcessorWrapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HyUI implementation of TemplateProcessorWrapper.
 * 
 * <p>This class bridges the Argonath type-safe DataValue system with HyUI's
 * template processing capabilities. It converts DataValue instances to their
 * underlying primitive types for use in HYUIML templates.
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * HyUITemplateProcessor processor = new HyUITemplateProcessor();
 * processor.setVariable("playerName", DataValue.of("Gandalf"))
 *          .setVariable("playerLevel", DataValue.of(60));
 * 
 * String processedHtml = processor.process(templateString);
 * }</pre>
 * 
 * @author Argonath Systems Team
 * @version 1.1.0
 * @since 1.1.0
 */
public class HyUITemplateProcessor implements TemplateProcessorWrapper {
    
    private final TemplateProcessor processor;
    
    /**
     * Creates a new HyUITemplateProcessor with a fresh HyUI TemplateProcessor.
     */
    public HyUITemplateProcessor() {
        this.processor = new TemplateProcessor();
    }
    
    @Override
    public TemplateProcessorWrapper setVariable(String name, DataValue value) {
        Object converted = convertToObject(value);
        processor.setVariable(name, converted);
        return this;
    }
    
    @Override
    public TemplateProcessorWrapper setVariable(String name, Object value) {
        processor.setVariable(name, value);
        return this;
    }
    
    @Override
    public TemplateProcessorWrapper setVariables(Map<String, DataValue> variables) {
        variables.forEach(this::setVariable);
        return this;
    }
    
    @Override
    public TemplateProcessorWrapper registerComponent(String name, String template) {
        processor.registerComponent(name, template);
        return this;
    }
    
    @Override
    public String process(String template) {
        return processor.process(template);
    }
    
    /**
     * Processes a template with the given variables.
     * 
     * @param template the template string
     * @param variables the variables to substitute
     * @return the processed template
     */
    public String process(String template, Map<String, Object> variables) {
        variables.forEach(processor::setVariable);
        return processor.process(template);
    }
    
    /**
     * Static helper to process a template with variables in one call.
     * 
     * @param template the template string
     * @param variables the variables to substitute
     * @return the processed template
     */
    public static String processStatic(String template, Map<String, Object> variables) {
        HyUITemplateProcessor processor = new HyUITemplateProcessor();
        return processor.process(template, variables);
    }
    
    /**
     * Gets the underlying HyUI TemplateProcessor for advanced usage.
     * 
     * @return the underlying TemplateProcessor
     */
    public TemplateProcessor getProcessor() {
        return processor;
    }
    
    /**
     * Converts a DataValue to its underlying Object representation.
     */
    private Object convertToObject(DataValue value) {
        return switch (value) {
            case DataValue.StringValue s -> s.value();
            case DataValue.IntValue i -> i.value();
            case DataValue.LongValue l -> l.value();
            case DataValue.DoubleValue d -> d.value();
            case DataValue.BoolValue b -> b.value();
            case DataValue.ListValue list -> convertList(list.value());
            case DataValue.MapValue map -> convertMap(map.value());
        };
    }
    
    /**
     * Converts a list of DataValues to a list of Objects.
     */
    private List<Object> convertList(List<DataValue> list) {
        return list.stream()
            .map(this::convertToObject)
            .collect(Collectors.toList());
    }
    
    /**
     * Converts a map of DataValues to a map of Objects.
     */
    private Map<String, Object> convertMap(Map<String, DataValue> map) {
        Map<String, Object> result = new HashMap<>();
        map.forEach((key, value) -> result.put(key, convertToObject(value)));
        return result;
    }
}
