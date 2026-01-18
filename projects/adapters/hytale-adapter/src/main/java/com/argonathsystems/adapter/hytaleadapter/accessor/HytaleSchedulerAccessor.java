package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.SchedulerAccessor;
import com.hytale.api.Server;
import com.hytale.api.scheduler.Task;

import java.util.concurrent.TimeUnit;

public class HytaleSchedulerAccessor implements SchedulerAccessor {
    private final Server server;

    public HytaleSchedulerAccessor(Server server) {
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