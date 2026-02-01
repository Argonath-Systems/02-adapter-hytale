package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.CommandAccessor;
import com.argonathsystems.framework.accessorapi.command.CommandSender;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.command.system.CommandRegistration;
import com.hypixel.hytale.server.core.entity.entities.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hytale implementation of CommandAccessor using SDK CommandManager.
 * 
 * <p><b>MIGRATION-001 Status:</b> ✅ IMPLEMENTED</p>
 * 
 * <p>SDK Classes Used:</p>
 * <ul>
 *   <li>{@code CommandManager} - Singleton command registry</li>
 *   <li>{@code AbstractCommand} - Base class for commands with execute(CommandContext)</li>
 *   <li>{@code CommandContext} - Execution context with sender() and getInputString()</li>
 *   <li>{@code CommandRegistration} - Registration handle for unregistration</li>
 *   <li>{@code Message.raw(String)} - Raw text message creation</li>
 * </ul>
 * 
 * @author Argonath Systems Team
 * @version 3.0.0
 * @since MIGRATION-001
 */
public class HytaleCommandAccessor implements CommandAccessor {
    
    private final Object server;
    private final Map<String, CommandRegistration> registeredCommands = new ConcurrentHashMap<>();
    
    public HytaleCommandAccessor(Object server) { 
        this.server = server; 
    }
    
    @Override
    public void register(String command, CommandExecutor executor) {
        // Create a wrapper that adapts our CommandExecutor to Hytale's AbstractCommand
        AbstractCommand hytaleCommand = new ArgonathCommandWrapper(command, executor);
        
        // Register with Hytale's CommandManager
        CommandRegistration registration = CommandManager.get().register(hytaleCommand);
        registeredCommands.put(command, registration);
    }
    
    /**
     * Unregister a previously registered command.
     * 
     * @param command The command name to unregister
     * @return true if the command was unregistered, false if it wasn't registered
     */
    public boolean unregister(String command) {
        CommandRegistration registration = registeredCommands.remove(command);
        if (registration != null) {
            registration.unregister();
            return true;
        }
        return false;
    }
    
    /**
     * Unregister all commands registered through this accessor.
     */
    public void unregisterAll() {
        registeredCommands.values().forEach(CommandRegistration::unregister);
        registeredCommands.clear();
    }
    
    /**
     * Wrapper class that adapts our CommandExecutor interface to Hytale's AbstractCommand.
     */
    private static class ArgonathCommandWrapper extends AbstractCommand {
        private final String commandName;
        private final CommandExecutor executor;
        
        ArgonathCommandWrapper(String commandName, CommandExecutor executor) {
            super(commandName);
            this.commandName = commandName;
            this.executor = executor;
            // Allow commands to receive variable arguments without validation errors
            setAllowsExtraArguments(true);
        }
        
        @Override
        public String getName() {
            // Explicitly override to ensure name is returned correctly
            // This works around potential ASM compatibility issues with Java 25
            return commandName;
        }
        
        @Override
        protected CompletableFuture<Void> execute(CommandContext context) {
            // Convert Hytale context to our platform-agnostic format
            String[] args = parseArgs(context);
            
            // Create sender wrapper
            CommandSender sender = new HytaleCommandSenderWrapper(context);
            
            // Execute the command
            executor.execute(sender, args);
            
            return CompletableFuture.completedFuture(null);
        }
        
        private String[] parseArgs(CommandContext context) {
            // Get the raw command string and extract arguments
            String rawCommand = context.getInputString();
            if (rawCommand == null || rawCommand.isEmpty()) {
                return new String[0];
            }
            
            // Remove the command name from the start
            String argsString = rawCommand.trim();
            if (argsString.startsWith(commandName)) {
                argsString = argsString.substring(commandName.length()).trim();
            }
            
            if (argsString.isEmpty()) {
                return new String[0];
            }
            
            return argsString.split("\\s+");
        }
    }
    
    /**
     * Wrapper for Hytale's command sender to our platform-agnostic interface.
     */
    private static class HytaleCommandSenderWrapper implements CommandSender {
        private final CommandContext context;
        
        HytaleCommandSenderWrapper(CommandContext context) {
            this.context = context;
        }
        
        @Override
        public void sendMessage(String message) {
            context.sendMessage(parseColorCodes(message));
        }
        
        /**
         * Parse Minecraft-style § color codes and convert to Hytale Message API.
         * Supports: §0-9, §a-f (colors), §l (bold), §o (italic), §r (reset)
         */
        private Message parseColorCodes(String text) {
            if (text == null || !text.contains("§")) {
                return Message.raw(text);
            }
            
            // Map § codes to hex colors
            java.util.Map<Character, String> colorMap = java.util.Map.ofEntries(
                java.util.Map.entry('0', "#000000"), // Black
                java.util.Map.entry('1', "#0000AA"), // Dark Blue
                java.util.Map.entry('2', "#00AA00"), // Dark Green
                java.util.Map.entry('3', "#00AAAA"), // Dark Aqua
                java.util.Map.entry('4', "#AA0000"), // Dark Red
                java.util.Map.entry('5', "#AA00AA"), // Dark Purple
                java.util.Map.entry('6', "#FFAA00"), // Gold
                java.util.Map.entry('7', "#AAAAAA"), // Gray
                java.util.Map.entry('8', "#555555"), // Dark Gray
                java.util.Map.entry('9', "#5555FF"), // Blue
                java.util.Map.entry('a', "#55FF55"), // Green
                java.util.Map.entry('b', "#55FFFF"), // Aqua
                java.util.Map.entry('c', "#FF5555"), // Red
                java.util.Map.entry('d', "#FF55FF"), // Light Purple
                java.util.Map.entry('e', "#FFFF55"), // Yellow
                java.util.Map.entry('f', "#FFFFFF")  // White
            );
            
            // Parse segments and build message
            java.util.List<Message> segments = new java.util.ArrayList<>();
            StringBuilder currentText = new StringBuilder();
            String currentColor = null;
            boolean bold = false;
            boolean italic = false;
            
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '§' && i + 1 < text.length()) {
                    // Flush current segment
                    if (currentText.length() > 0) {
                        Message seg = Message.raw(currentText.toString());
                        if (currentColor != null) seg = seg.color(currentColor);
                        if (bold) seg = seg.bold(true);
                        if (italic) seg = seg.italic(true);
                        segments.add(seg);
                        currentText.setLength(0);
                    }
                    
                    char code = Character.toLowerCase(text.charAt(i + 1));
                    i++; // Skip the code character
                    
                    if (colorMap.containsKey(code)) {
                        currentColor = colorMap.get(code);
                        bold = false;
                        italic = false;
                    } else if (code == 'l') {
                        bold = true;
                    } else if (code == 'o') {
                        italic = true;
                    } else if (code == 'r') {
                        currentColor = null;
                        bold = false;
                        italic = false;
                    }
                } else {
                    currentText.append(c);
                }
            }
            
            // Flush remaining text
            if (currentText.length() > 0) {
                Message seg = Message.raw(currentText.toString());
                if (currentColor != null) seg = seg.color(currentColor);
                if (bold) seg = seg.bold(true);
                if (italic) seg = seg.italic(true);
                segments.add(seg);
            }
            
            if (segments.isEmpty()) {
                return Message.raw("");
            } else if (segments.size() == 1) {
                return segments.get(0);
            } else {
                return Message.join(segments.toArray(new Message[0]));
            }
        }
        
        @Override
        public boolean hasPermission(String permission) {
            return context.sender().hasPermission(permission);
        }
        
        @Override
        public String getName() {
            return context.sender().getDisplayName();
        }
        
        @Override
        public boolean isPlayer() {
            return context.isPlayer();
        }
        
        @Override
        public Optional<UUID> getPlayerId() {
            if (context.isPlayer()) {
                return Optional.ofNullable(context.sender().getUuid());
            }
            return Optional.empty();
        }
    }
}
