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
        return server.getScheduler().callSync(task);
    }

    @Override
    public CompletableFuture<Void> execute(Runnable task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        server.getScheduler().runTask(() -> {
            try {
                task.run();
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    public boolean isMainThread() {
        // TODO: Check against Hytale server thread
        return true; 
    }
}