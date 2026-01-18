package com.argonathsystems.adapter.hytaleadapter.thread;

import com.argonathsystems.framework.accessorapi.thread.WorldExecutor;
import com.hytale.api.Server;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

public class HytaleWorldExecutor implements WorldExecutor {
    private final Server server;

    public HytaleWorldExecutor(Server server) {
        this.server = server;
    }

    @Override
    public <T> CompletableFuture<T> execute(Callable<T> task) {
        // TODO: Implement actual Hytale scheduler execution
        CompletableFuture<T> future = new CompletableFuture<>();
        try {
            future.complete(task.call());
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public CompletableFuture<Void> execute(Runnable task) {
        // TODO: Implement actual Hytale scheduler execution
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            task.run();
            future.complete(null);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public boolean isMainThread() {
        // TODO: Check against Hytale server thread
        return true; 
    }
}