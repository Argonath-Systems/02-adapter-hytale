package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.SchedulerAccessor;
import com.hypixel.hytale.server.core.task.TaskRegistration;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Hytale implementation of SchedulerAccessor using SDK task system.
 * 
 * <p><b>MIGRATION-001 Status:</b> ✅ IMPLEMENTED</p>
 * 
 * <p>SDK Classes Used:</p>
 * <ul>
 *   <li>{@code com.hypixel.hytale.server.core.task.TaskRegistration} - Wraps ScheduledFuture</li>
 *   <li>{@code com.hypixel.hytale.server.core.universe.world.World} - For world.execute() thread safety</li>
 * </ul>
 * 
 * <p>Implementation uses Java's ScheduledExecutorService for task scheduling,
 * wrapped with Hytale's TaskRegistration for compatibility.</p>
 * 
 * @author Argonath Systems Team
 * @version 3.0.0
 * @since MIGRATION-001
 */
public class HytaleSchedulerAccessor implements SchedulerAccessor {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HytaleSchedulerAccessor.class);
    
    private static final ScheduledExecutorService SYNC_EXECUTOR = 
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Argonath-Scheduler-Sync");
            t.setDaemon(true);
            return t;
        });
    
    private static final ScheduledExecutorService ASYNC_EXECUTOR = 
        Executors.newScheduledThreadPool(4, r -> {
            Thread t = new Thread(r, "Argonath-Scheduler-Async");
            t.setDaemon(true);
            return t;
        });
    
    private final AtomicLong taskIdCounter = new AtomicLong(0);
    private final Map<Long, ScheduledFuture<?>> activeTasks = new ConcurrentHashMap<>();
    private final Object server;
    
    public HytaleSchedulerAccessor(Object server) { 
        this.server = server; 
    }
    
    @Override
    public ScheduledTask runTask(Runnable task) {
        ScheduledFuture<?> future = SYNC_EXECUTOR.schedule(task, 0, TimeUnit.MILLISECONDS);
        return wrapTask(future);
    }
    
    @Override
    public ScheduledTask runTaskLater(Runnable task, long delay, TimeUnit unit) {
        ScheduledFuture<?> future = SYNC_EXECUTOR.schedule(task, delay, unit);
        return wrapTask(future);
    }
    
    @Override
    public ScheduledTask runTaskTimer(Runnable task, long initialDelay, long period, TimeUnit unit) {
        ScheduledFuture<?> future = SYNC_EXECUTOR.scheduleAtFixedRate(task, initialDelay, period, unit);
        return wrapTask(future);
    }
    
    @Override
    public ScheduledTask runTaskAsync(Runnable task) {
        ScheduledFuture<?> future = ASYNC_EXECUTOR.schedule(task, 0, TimeUnit.MILLISECONDS);
        return wrapTask(future);
    }
    
    @Override
    public ScheduledTask runTaskLaterAsync(Runnable task, long delay, TimeUnit unit) {
        ScheduledFuture<?> future = ASYNC_EXECUTOR.schedule(task, delay, unit);
        return wrapTask(future);
    }
    
    @Override
    public void runOnWorldThread(UUID playerId, Runnable task) {
        if (playerId == null || task == null) {
            LOGGER.warn("[SCHEDULER] Cannot run task on world thread: playerId or task is null");
            return;
        }
        
        PlayerRef playerRef = Universe.get().getPlayer(playerId);
        if (playerRef == null || !playerRef.isValid()) {
            LOGGER.info("[SCHEDULER] Cannot run task on world thread: player {} not found or invalid", playerId);
            return;
        }
        
        UUID worldUuid = playerRef.getWorldUuid();
        if (worldUuid == null) {
            LOGGER.info("[SCHEDULER] Cannot run task on world thread: player {} has no world", playerId);
            return;
        }
        
        World world = Universe.get().getWorld(worldUuid);
        if (world == null) {
            LOGGER.info("[SCHEDULER] Cannot run task on world thread: world {} not found", worldUuid);
            return;
        }
        
        LOGGER.info("[SCHEDULER] Executing task on world thread for player {}", playerId);
        // Execute on the world thread - this is the proper way to access player components
        world.execute(task);
    }
    
    @Override
    public void cancelAll() {
        activeTasks.values().forEach(future -> future.cancel(false));
        activeTasks.clear();
    }
    
    /**
     * Wrap a ScheduledFuture into our ScheduledTask interface.
     * Also integrates with Hytale's TaskRegistration for SDK compatibility.
     */
    private ScheduledTask wrapTask(ScheduledFuture<?> future) {
        long taskId = taskIdCounter.incrementAndGet();
        activeTasks.put(taskId, future);
        
        // Create Hytale TaskRegistration for SDK compatibility
        TaskRegistration registration = new TaskRegistration(future);
        
        return new HytaleScheduledTask(taskId, future, registration);
    }
    
    /**
     * Implementation of ScheduledTask that wraps Hytale's TaskRegistration.
     */
    private class HytaleScheduledTask implements ScheduledTask {
        private final long taskId;
        private final ScheduledFuture<?> future;
        private final TaskRegistration registration;
        
        HytaleScheduledTask(long taskId, ScheduledFuture<?> future, TaskRegistration registration) {
            this.taskId = taskId;
            this.future = future;
            this.registration = registration;
        }
        
        @Override
        public int getTaskId() {
            return (int) taskId;
        }
        
        @Override
        public void cancel() {
            future.cancel(false);
            activeTasks.remove(taskId);
        }
        
        @Override
        public boolean isCancelled() {
            return future.isCancelled();
        }
        
        /**
         * Check if the task is complete.
         * @return true if the task has completed
         */
        public boolean isDone() {
            return future.isDone();
        }
        
        /**
         * Get the underlying Hytale TaskRegistration.
         * @return The TaskRegistration for SDK interoperability
         */
        public TaskRegistration getRegistration() {
            return registration;
        }
    }
    
    /**
     * Shutdown the scheduler executors gracefully.
     * Should be called when the plugin is disabled.
     */
    public void shutdown() {
        cancelAll();
        SYNC_EXECUTOR.shutdown();
        ASYNC_EXECUTOR.shutdown();
        try {
            if (!SYNC_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                SYNC_EXECUTOR.shutdownNow();
            }
            if (!ASYNC_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                ASYNC_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            SYNC_EXECUTOR.shutdownNow();
            ASYNC_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
