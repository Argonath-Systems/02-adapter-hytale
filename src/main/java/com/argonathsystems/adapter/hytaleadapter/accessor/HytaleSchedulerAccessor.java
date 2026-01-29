package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.SchedulerAccessor;

import java.util.concurrent.TimeUnit;

/**
 * <p><b>MIGRATION-001 Status:</b> BLOCKED - Requires Official Hytale SDK</p>
 * 
 * <p>This accessor implements Task scheduling functionality.</p>
 * <p>Implementation requires the official Hytale SDK (com.hypixel.hytale.*)
 * which is not available in the development environment.</p>
 * 
 * <p>All methods throw UnsupportedOperationException until the SDK is available.</p>
 * 
 * @see <a href="file://../../../docs/migration-001/PHASE-3-IMPLEMENTATION-STATUS.md">Phase 3 Status</a>
 */
public class HytaleSchedulerAccessor implements SchedulerAccessor {
    private final Object /* Server */ server;

    public HytaleSchedulerAccessor(Object /* Server */ server) {
        this.server = server;
    }

    private ScheduledTask wrap(Task task) {
        return new ScheduledTask() {
            @Override
            public int getTaskId() {
                return task.getTaskId();
            }

            @Override
            public boolean isCancelled() {
                return task.isCancelled();
            }

            @Override
            public void cancel() {
                task.cancel();
            }
        };
    }

    @Override
    public ScheduledTask runTask(Runnable task) {
        return wrap(server.getScheduler().runTask(task));
    }

    @Override
    public ScheduledTask runTaskLater(Runnable task, long delay, TimeUnit unit) {
        return wrap(server.getScheduler().runTaskLater(task, delay, unit));
    }

    @Override
    public ScheduledTask runTaskTimer(Runnable task, long initialDelay, long period, TimeUnit unit) {
        return wrap(server.getScheduler().runTaskTimer(task, initialDelay, period, unit));
    }

    @Override
    public ScheduledTask runTaskAsync(Runnable task) {
        return wrap(server.getScheduler().runTaskAsync(task));
    }

    @Override
    public ScheduledTask runTaskLaterAsync(Runnable task, long delay, TimeUnit unit) {
        return wrap(server.getScheduler().runTaskLater(task, delay, unit)); 
    }

    @Override
    public void cancelAll() {
        // Not supported by simple API yet
    }
}