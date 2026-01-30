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
            context.sendMessage(Message.raw(message));
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
