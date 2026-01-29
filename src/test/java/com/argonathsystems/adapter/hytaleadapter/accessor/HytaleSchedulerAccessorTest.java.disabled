package com.argonathsystems.adapter.hytaleadapter.accessor;

import com.argonathsystems.framework.accessorapi.SchedulerAccessor;
import com.hytale.api.Server;
import com.hytale.api.scheduler.Scheduler;
import com.hytale.api.scheduler.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for HytaleSchedulerAccessor.
 * 
 * <p>Verifies:
 * <ul>
 *   <li>Task scheduling (sync/async, delayed, repeating)</li>
 *   <li>Task cancellation</li>
 *   <li>Time unit conversion</li>
 *   <li>Runnable execution</li>
 * </ul>
 * 
 * @author Argonath Systems Team
 * @version 2.0.0
 */
@DisplayName("HytaleSchedulerAccessor Tests")
class HytaleSchedulerAccessorTest {

    @Mock
    private Server mockServer;

    @Mock
    private Scheduler mockScheduler;

    @Mock
    private Task mockTask;

    private HytaleSchedulerAccessor accessor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockServer.getScheduler()).thenReturn(mockScheduler);
        when(mockScheduler.runTask(any(Runnable.class))).thenReturn(mockTask);
        when(mockScheduler.runTaskLater(any(Runnable.class), anyLong())).thenReturn(mockTask);
        when(mockScheduler.runTaskTimer(any(Runnable.class), anyLong(), anyLong())).thenReturn(mockTask);
        when(mockScheduler.runTaskAsync(any(Runnable.class))).thenReturn(mockTask);
        when(mockTask.getTaskId()).thenReturn(123);

        accessor = new HytaleSchedulerAccessor(mockServer);
    }

    @Test
    @DisplayName("runTask() should schedule immediate synchronous task")
    void testRunTask() {
        // Arrange
        Runnable task = mock(Runnable.class);

        // Act
        int taskId = accessor.runTask(task);

        // Assert
        assertThat(taskId).isEqualTo(123);
        verify(mockScheduler).runTask(task);
    }

    @Test
    @DisplayName("runTaskLater() should schedule delayed synchronous task")
    void testRunTaskLater() {
        // Arrange
        Runnable task = mock(Runnable.class);
        long delayTicks = 20L;

        // Act
        int taskId = accessor.runTaskLater(task, delayTicks);

        // Assert
        assertThat(taskId).isEqualTo(123);
        verify(mockScheduler).runTaskLater(task, delayTicks);
    }

    @Test
    @DisplayName("runTaskTimer() should schedule repeating synchronous task")
    void testRunTaskTimer() {
        // Arrange
        Runnable task = mock(Runnable.class);
        long delayTicks = 20L;
        long periodTicks = 20L;

        // Act
        int taskId = accessor.runTaskTimer(task, delayTicks, periodTicks);

        // Assert
        assertThat(taskId).isEqualTo(123);
        verify(mockScheduler).runTaskTimer(task, delayTicks, periodTicks);
    }

    @Test
    @DisplayName("runTaskAsync() should schedule asynchronous task")
    void testRunTaskAsync() {
        // Arrange
        Runnable task = mock(Runnable.class);

        // Act
        int taskId = accessor.runTaskAsync(task);

        // Assert
        assertThat(taskId).isEqualTo(123);
        verify(mockScheduler).runTaskAsync(task);
    }

    @Test
    @DisplayName("cancelTask() should cancel task by ID")
    void testCancelTask() {
        // Arrange
        int taskId = 123;

        // Act
        accessor.cancelTask(taskId);

        // Assert
        verify(mockScheduler).cancelTask(taskId);
    }

    @Test
    @DisplayName("runDelayed() should convert seconds to ticks correctly")
    void testRunDelayed_SecondsToTicks() {
        // Arrange
        Runnable task = mock(Runnable.class);
        long delaySeconds = 5L;
        long expectedTicks = 100L; // 5 seconds * 20 ticks/second

        // Act
        int taskId = accessor.runDelayed(task, delaySeconds, TimeUnit.SECONDS);

        // Assert
        verify(mockScheduler).runTaskLater(task, expectedTicks);
    }

    @Test
    @DisplayName("runDelayed() should handle milliseconds correctly")
    void testRunDelayed_Milliseconds() {
        // Arrange
        Runnable task = mock(Runnable.class);
        long delayMs = 1000L;
        long expectedTicks = 20L; // 1000ms / 50ms per tick

        // Act
        int taskId = accessor.runDelayed(task, delayMs, TimeUnit.MILLISECONDS);

        // Assert
        verify(mockScheduler).runTaskLater(task, expectedTicks);
    }

    @Test
    @DisplayName("runRepeating() should convert period correctly")
    void testRunRepeating() {
        // Arrange
        Runnable task = mock(Runnable.class);
        long initialDelaySeconds = 1L;
        long periodSeconds = 5L;
        long expectedInitialTicks = 20L;
        long expectedPeriodTicks = 100L;

        // Act
        int taskId = accessor.runRepeating(task, initialDelaySeconds, periodSeconds, TimeUnit.SECONDS);

        // Assert
        verify(mockScheduler).runTaskTimer(task, expectedInitialTicks, expectedPeriodTicks);
    }

    @Test
    @DisplayName("runDelayedAsync() should schedule async delayed task")
    void testRunDelayedAsync() {
        // Arrange
        Runnable task = mock(Runnable.class);
        when(mockScheduler.runTaskLaterAsync(any(Runnable.class), anyLong())).thenReturn(mockTask);

        // Act
        int taskId = accessor.runDelayedAsync(task, 2L, TimeUnit.SECONDS);

        // Assert
        verify(mockScheduler).runTaskLaterAsync(task, 40L); // 2 seconds = 40 ticks
    }

    @Test
    @DisplayName("Runnable should be executed when scheduled")
    void testRunnableExecution() {
        // Arrange
        Runnable task = mock(Runnable.class);
        when(mockScheduler.runTask(any(Runnable.class))).thenAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return mockTask;
        });

        // Act
        accessor.runTask(task);

        // Assert
        verify(task).run();
    }

    @Test
    @DisplayName("Multiple tasks should have different IDs")
    void testMultipleTasks() {
        // Arrange
        when(mockTask.getTaskId()).thenReturn(1, 2, 3);

        // Act
        int id1 = accessor.runTask(() -> {});
        int id2 = accessor.runTask(() -> {});
        int id3 = accessor.runTask(() -> {});

        // Assert
        assertThat(id1).isEqualTo(1);
        assertThat(id2).isEqualTo(2);
        assertThat(id3).isEqualTo(3);
    }

    @Test
    @DisplayName("cancelTask() should handle invalid task ID gracefully")
    void testCancelTask_InvalidId() {
        // Act - Should not throw
        assertThatCode(() -> accessor.cancelTask(999))
            .doesNotThrowAnyException();

        // Assert
        verify(mockScheduler).cancelTask(999);
    }

    @Test
    @DisplayName("Zero delay should be handled correctly")
    void testRunDelayed_ZeroDelay() {
        // Arrange
        Runnable task = mock(Runnable.class);

        // Act
        accessor.runDelayed(task, 0L, TimeUnit.SECONDS);

        // Assert
        verify(mockScheduler).runTaskLater(task, 0L);
    }

    @Test
    @DisplayName("Very long delays should be converted correctly")
    void testRunDelayed_LongDelay() {
        // Arrange
        Runnable task = mock(Runnable.class);
        long delayMinutes = 60L;
        long expectedTicks = 72000L; // 60 minutes * 60 seconds * 20 ticks

        // Act
        accessor.runDelayed(task, delayMinutes, TimeUnit.MINUTES);

        // Assert
        verify(mockScheduler).runTaskLater(task, expectedTicks);
    }

    @Test
    @DisplayName("Repeating task with zero initial delay should start immediately")
    void testRunRepeating_ZeroInitialDelay() {
        // Arrange
        Runnable task = mock(Runnable.class);

        // Act
        accessor.runRepeating(task, 0L, 1L, TimeUnit.SECONDS);

        // Assert
        verify(mockScheduler).runTaskTimer(task, 0L, 20L);
    }
}
