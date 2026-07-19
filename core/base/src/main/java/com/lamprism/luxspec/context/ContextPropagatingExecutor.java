package com.lamprism.luxspec.context;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;

/**
 * Captures the complete active ExecutionContext when work is submitted to another Executor.
 *
 * @author RollW
 */
public final class ContextPropagatingExecutor implements Executor {
    private final Executor delegate;

    /**
     * Creates an executor wrapper that propagates the submitting thread's active context.
     *
     * @param delegate the executor that runs submitted work
     */
    public ContextPropagatingExecutor(Executor delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public void execute(Runnable command) {
        Runnable nonNullCommand = Objects.requireNonNull(command, "command");
        Optional<ExecutionContext> captured = ExecutionContexts.snapshot();
        delegate.execute(() -> run(captured, nonNullCommand));
    }

    /**
     * Executes work inside a new empty root context instead of inheriting the submitting context.
     *
     * @param command the detached work to run
     */
    public void executeDetached(Runnable command) {
        Runnable nonNullCommand = Objects.requireNonNull(command, "command");
        delegate.execute(() -> run(ExecutionContext.empty(), nonNullCommand));
    }

    private void run(Optional<ExecutionContext> captured, Runnable command) {
        if (captured.isEmpty()) {
            command.run();
            return;
        }
        run(captured.orElseThrow(), command);
    }

    private void run(ExecutionContext context, Runnable command) {
        try (ExecutionContexts.Scope ignored = ExecutionContexts.open(context)) {
            command.run();
        }
    }
}
