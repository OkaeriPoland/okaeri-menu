package eu.okaeri.menu.bukkit.unit.async;

import eu.okaeri.menu.async.AsyncExecutor;
import eu.okaeri.menu.async.BukkitAsyncExecutor;
import lombok.NonNull;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AsyncExecutor implementations.
 * Tests Bukkit scheduler integration and async task execution.
 */
class AsyncExecutorTest {

    private static ServerMock server;
    private JavaPlugin plugin;

    @BeforeAll
    static void setUpServer() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void tearDownServer() {
        MockBukkit.unmock();
    }

    @BeforeEach
    void setUp() {
        this.plugin = MockBukkit.createMockPlugin();
    }

    // ========================================
    // BUKKIT ASYNC EXECUTOR
    // ========================================

    @Test
    @DisplayName("Should execute task asynchronously")
    void testAsyncExecution() throws Exception {
        AsyncExecutor executor = AsyncExecutor.bukkit(this.plugin);
        AtomicBoolean executed = new AtomicBoolean(false);

        CompletableFuture<String> future = executor.execute(() -> {
            executed.set(true);
            return "result";
        });

        // Wait for async task to complete (MockBukkit executes on actual thread pool)
        String result = future.get(1, java.util.concurrent.TimeUnit.SECONDS);

        assertThat(executed.get()).isTrue();
        assertThat(result).isEqualTo("result");
        assertThat(future).isCompletedWithValue("result");
    }

    @Test
    @DisplayName("Should return completed future with result")
    void testFutureCompletion() throws Exception {
        AsyncExecutor executor = AsyncExecutor.bukkit(this.plugin);

        CompletableFuture<Integer> future = executor.execute(() -> 42);

        Integer result = future.get(1, java.util.concurrent.TimeUnit.SECONDS);

        assertThat(result).isEqualTo(42);
        assertThat(future).isCompletedWithValue(42);
    }

    @Test
    @DisplayName("Should handle different result types")
    void testDifferentResultTypes() throws Exception {
        AsyncExecutor executor = AsyncExecutor.bukkit(this.plugin);

        // String
        CompletableFuture<String> stringFuture = executor.execute(() -> "text");
        assertThat(stringFuture.get(1, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo("text");

        // Integer
        CompletableFuture<Integer> intFuture = executor.execute(() -> 123);
        assertThat(intFuture.get(1, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(123);

        // Boolean
        CompletableFuture<Boolean> boolFuture = executor.execute(() -> true);
        assertThat(boolFuture.get(1, java.util.concurrent.TimeUnit.SECONDS)).isTrue();

        // Null
        CompletableFuture<Object> nullFuture = executor.execute(() -> null);
        assertThat(nullFuture.get(1, java.util.concurrent.TimeUnit.SECONDS)).isNull();
    }

    @Test
    @DisplayName("Should handle task exceptions")
    void testTaskException() {
        AsyncExecutor executor = AsyncExecutor.bukkit(this.plugin);

        CompletableFuture<String> future = executor.execute(() -> {
            throw new RuntimeException("Task failed");
        });

        // Wait for future to complete
        assertThat(future)
            .failsWithin(java.time.Duration.ofSeconds(1))
            .withThrowableOfType(java.util.concurrent.ExecutionException.class)
            .withMessageContaining("Task failed");
    }

    @Test
    @DisplayName("Should handle different exception types")
    void testDifferentExceptionTypes() {
        AsyncExecutor executor = AsyncExecutor.bukkit(this.plugin);

        // RuntimeException
        CompletableFuture<String> runtimeFuture = executor.execute(() -> {
            throw new RuntimeException("Runtime error");
        });
        assertThat(runtimeFuture)
            .failsWithin(java.time.Duration.ofSeconds(1))
            .withThrowableOfType(java.util.concurrent.ExecutionException.class);

        // IllegalArgumentException
        CompletableFuture<String> illegalArgFuture = executor.execute(() -> {
            throw new IllegalArgumentException("Invalid argument");
        });
        assertThat(illegalArgFuture)
            .failsWithin(java.time.Duration.ofSeconds(1))
            .withThrowableOfType(java.util.concurrent.ExecutionException.class);

        // NullPointerException
        CompletableFuture<String> npeFuture = executor.execute(() -> {
            throw new NullPointerException("Null value");
        });
        assertThat(npeFuture)
            .failsWithin(java.time.Duration.ofSeconds(1))
            .withThrowableOfType(java.util.concurrent.ExecutionException.class);
    }

    @Test
    @DisplayName("Should execute multiple tasks independently")
    void testMultipleTasksIndependently() throws Exception {
        AsyncExecutor executor = AsyncExecutor.bukkit(this.plugin);
        AtomicInteger counter = new AtomicInteger(0);

        CompletableFuture<Integer> future1 = executor.execute(() -> {
            counter.incrementAndGet();
            return 1;
        });

        CompletableFuture<Integer> future2 = executor.execute(() -> {
            counter.incrementAndGet();
            return 2;
        });

        CompletableFuture<Integer> future3 = executor.execute(() -> {
            counter.incrementAndGet();
            return 3;
        });

        // Wait for all futures
        future1.get(1, java.util.concurrent.TimeUnit.SECONDS);
        future2.get(1, java.util.concurrent.TimeUnit.SECONDS);
        future3.get(1, java.util.concurrent.TimeUnit.SECONDS);

        assertThat(counter.get()).isEqualTo(3);
        assertThat(future1).isCompletedWithValue(1);
        assertThat(future2).isCompletedWithValue(2);
        assertThat(future3).isCompletedWithValue(3);
    }

    @Test
    @DisplayName("Should create executor using static factory")
    void testStaticFactoryMethod() {
        AsyncExecutor executor = AsyncExecutor.bukkit(this.plugin);

        assertThat(executor).isNotNull();
        assertThat(executor).isInstanceOf(BukkitAsyncExecutor.class);
    }

    @Test
    @DisplayName("Should support custom executor implementations")
    void testCustomExecutorImplementation() {
        // Create custom executor that executes synchronously
        AsyncExecutor customExecutor = new AsyncExecutor() {
            @Override
            public <T> CompletableFuture<T> execute(java.util.function.@NonNull Supplier<T> task) {
                try {
                    return CompletableFuture.completedFuture(task.get());
                } catch (Exception e) {
                    CompletableFuture<T> future = new CompletableFuture<>();
                    future.completeExceptionally(e);
                    return future;
                }
            }
        };

        CompletableFuture<String> future = customExecutor.execute(() -> "custom");

        assertThat(future).isCompletedWithValue("custom");
    }

    @Test
    @DisplayName("Should handle long-running tasks")
    void testLongRunningTask() throws Exception {
        AsyncExecutor executor = AsyncExecutor.bukkit(this.plugin);
        AtomicInteger progress = new AtomicInteger(0);

        CompletableFuture<Integer> future = executor.execute(() -> {
            for (int i = 0; i < 5; i++) {
                progress.incrementAndGet();
            }
            return progress.get();
        });

        // Wait for completion
        Integer result = future.get(1, java.util.concurrent.TimeUnit.SECONDS);

        assertThat(progress.get()).isEqualTo(5);
        assertThat(result).isEqualTo(5);
        assertThat(future).isCompletedWithValue(5);
    }

    @Test
    @DisplayName("Should allow chaining with whenComplete")
    void testWhenCompleteChaining() {
        AsyncExecutor executor = AsyncExecutor.bukkit(this.plugin);
        AtomicBoolean callbackExecuted = new AtomicBoolean(false);

        CompletableFuture<String> future = executor.execute(() -> "data");

        future.whenComplete((result, error) -> {
            callbackExecuted.set(true);
            assertThat(result).isEqualTo("data");
            assertThat(error).isNull();
        });

        server.getScheduler().performTicks(1);

        assertThat(callbackExecuted.get()).isTrue();
    }

    @Test
    @DisplayName("Should allow chaining with thenApply")
    void testThenApplyChaining() throws Exception {
        AsyncExecutor executor = AsyncExecutor.bukkit(this.plugin);

        CompletableFuture<String> future = executor.execute(() -> 42)
            .thenApply(num -> "Value: " + num);

        String result = future.get(1, java.util.concurrent.TimeUnit.SECONDS);

        assertThat(result).isEqualTo("Value: 42");
        assertThat(future).isCompletedWithValue("Value: 42");
    }

    @Test
    @DisplayName("Should propagate exceptions through chain")
    void testExceptionPropagationThroughChain() throws Exception {
        AsyncExecutor executor = AsyncExecutor.bukkit(this.plugin);

        CompletableFuture<String> future = executor.<String>execute(() -> {
            throw new RuntimeException("Initial error");
        }).exceptionally(ex -> "Recovered: " + ex.getMessage());

        String result = future.get(1, java.util.concurrent.TimeUnit.SECONDS);

        assertThat(result).startsWith("Recovered:");
        assertThat(result).contains("Initial error");
    }
}
