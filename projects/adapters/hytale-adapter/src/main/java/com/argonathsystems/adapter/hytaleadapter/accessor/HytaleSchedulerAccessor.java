package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.SchedulerAccessor;
import com.hytale.api.Server;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class HytaleSchedulerAccessor implements SchedulerAccessor {
    private final Server server;
    private final AtomicInteger taskIdCounter = new AtomicInteger(0);

    public HytaleSchedulerAccessor(Server server) {
        this.server = server;
    }

    @Override
    public ScheduledTask runTask(Runnable task) {
        task.run(); // TODO: Use real scheduler
        return new DummyTask(taskIdCounter.incrementAndGet());
    }

    @Override
    public ScheduledTask runTaskLater(Runnable task, long delay, TimeUnit unit) {
        return new DummyTask(taskIdCounter.incrementAndGet());
    }

    @Override
    public ScheduledTask runTaskTimer(Runnable task, long initialDelay, long period, TimeUnit unit) {
        return new DummyTask(taskIdCounter.incrementAndGet());
    }

    @Override
    public ScheduledTask runTaskAsync(Runnable task) {
        new Thread(task).start();
        return new DummyTask(taskIdCounter.incrementAndGet());
    }

    @Override
    public ScheduledTask runTaskLaterAsync(Runnable task, long delay, TimeUnit unit) {
        return new DummyTask(taskIdCounter.incrementAndGet());
    }

    @Override
    public void cancelAll() {
    }

    private static class DummyTask implements ScheduledTask {
        private final int id;
        private volatile boolean cancelled = false;

        public DummyTask(int id) {
            this.id = id;
        }

        @Override
        public int getTaskId() {
            return id;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }
}