package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.SchedulerAccessor;
import java.util.concurrent.TimeUnit;

/**
 * <p><b>MIGRATION-001 Status:</b> BLOCKED - Requires Official Hytale SDK</p>
 * @see <a href="file://../../../docs/migration-001/PHASE-3-IMPLEMENTATION-STATUS.md">Phase 3 Status</a>
 */
public class HytaleSchedulerAccessor implements SchedulerAccessor {
    private final Object server;
    public HytaleSchedulerAccessor(Object server) { this.server = server; }
    
    @Override
    public ScheduledTask runTask(Runnable task) {
        throw new UnsupportedOperationException("HytaleSchedulerAccessor.runTask() requires official Hytale SDK Scheduler");
    }
    @Override
    public ScheduledTask runTaskLater(Runnable task, long delay, TimeUnit unit) {
        throw new UnsupportedOperationException("HytaleSchedulerAccessor.runTaskLater() requires official Hytale SDK");
    }
    @Override
    public ScheduledTask runTaskTimer(Runnable task, long initialDelay, long period, TimeUnit unit) {
        throw new UnsupportedOperationException("HytaleSchedulerAccessor.runTaskTimer() requires official Hytale SDK");
    }
    @Override
    public ScheduledTask runTaskAsync(Runnable task) {
        throw new UnsupportedOperationException("HytaleSchedulerAccessor.runTaskAsync() requires official Hytale SDK");
    }
    @Override
    public ScheduledTask runTaskLaterAsync(Runnable task, long delay, TimeUnit unit) {
        throw new UnsupportedOperationException("HytaleSchedulerAccessor.runTaskLaterAsync() requires official Hytale SDK");
    }
    @Override
    public void cancelAll() {
        throw new UnsupportedOperationException("HytaleSchedulerAccessor.cancelAll() requires official Hytale SDK");
    }
}
