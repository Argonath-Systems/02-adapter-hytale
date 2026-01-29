package com.argonathsystems.adapter.hytaleadapter.thread;

/**
 * <p><b>MIGRATION-001 Status:</b> BLOCKED - Requires Official Hytale SDK</p>
 */
public class HytaleWorldExecutor {
    private final Object world;
    
    public HytaleWorldExecutor(Object world) {
        this.world = world;
    }
    
    public void execute(Runnable task) {
        throw new UnsupportedOperationException(
            "HytaleWorldExecutor.execute() requires official Hytale SDK World thread-safety system"
        );
    }
    
    public void executeLater(Runnable task, long delayTicks) {
        throw new UnsupportedOperationException(
            "HytaleWorldExecutor.executeLater() requires official Hytale SDK Scheduler"
        );
    }
}
