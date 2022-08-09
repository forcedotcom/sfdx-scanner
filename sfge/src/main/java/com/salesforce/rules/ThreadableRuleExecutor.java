package com.salesforce.rules;

import com.salesforce.config.SfgeConfigProvider;
import com.salesforce.exception.SfgeRuntimeException;
import com.salesforce.graph.JustInTimeGraphProvider;
import com.salesforce.graph.ops.LogUtil;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.rules.ops.ProgressListener;
import com.salesforce.rules.ops.ProgressListenerImpl;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

public class ThreadableRuleExecutor {
    private static final Logger LOGGER = LogManager.getLogger(ThreadableRuleExecutor.class);
    /** Path-based rules will be killed after this long. */
    private static final long TIMEOUT = SfgeConfigProvider.get().getRuleThreadTimeout();

    /** This many threads can be running path-based rules at the same time. */
    private static final int THREAD_COUNT = SfgeConfigProvider.get().getRuleThreadCount();

    public static Set<Violation> run(List<ThreadableRuleSubmission> submissions) {
        // Create a threadpool and a completion service to monitor it.
        ExecutorService pool = Executors.newWorkStealingPool(THREAD_COUNT);
        CompletionService<Set<Violation>> completionService = new ExecutorCompletionService(pool);

        // Submit an appropriate amount of callables into the completion service.
        int submissionCount = submitRunners(completionService, submissions);

        // Create an array of results, and add the results into their slots as they come in.
        Set<Violation> allViolations = new TreeSet<>();
        int completedCount = 0;
        while (completedCount < submissionCount) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Beginning wait #" + (completedCount + 1));
            }
            // Get the next set of results.
            Set<Violation> violations = waitForCallable(completionService);
            int priorSize = allViolations.size();
            allViolations.addAll(violations);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(
                        "Wait #"
                                + (completedCount + 1)
                                + " finished, adding "
                                + (allViolations.size() - priorSize)
                                + " new entries");
            }
            completedCount += 1;
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Finishing waiting for futures");
            LOGGER.info("Shutting down");
        }
        pool.shutdown();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Finished shutdown");
        }
        return allViolations;
    }

    /**
     * Submits CallableExecutor instances to the provided CompletionService based on the given entry
     * points and rules. Returns the number of new runners submitted to the pool.
     */
    private static int submitRunners(
            CompletionService<Set<Violation>> completionService,
            List<ThreadableRuleSubmission> submissions) {
        int submissionCount = 0;
        final ProgressListener progressListener = new ProgressListenerImpl();
        for (ThreadableRuleSubmission submission : submissions) {
            completionService.submit(new CallableExecutor(submission, progressListener));
            submissionCount += 1;
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Queued " + submissionCount + " path-based rule executions");
        }
        return submissionCount;
    }

    /**
     * Waits for the next completion of a runner tied to the provided CompletionService, then
     * returns the violation set.
     *
     * @throws ThreadableRuleExecutorException - Thrown when a runner is unable to complete.
     */
    private static Set<Violation> waitForCallable(
            CompletionService<Set<Violation>> completionService) {
        try {
            long startTime = System.currentTimeMillis();
            Future<Set<Violation>> future = completionService.take();
            long endTime = System.currentTimeMillis();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Future returned after " + (endTime - startTime) + " ms");
            }
            return future.get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new ThreadableRuleExecutorException(ex);
        }
    }

    private static class CallableExecutor implements Callable<Set<Violation>> {
        private final ThreadableRuleSubmission submission;
        private final ProgressListener progressListener;
        private boolean timedOut;

        public CallableExecutor(
                ThreadableRuleSubmission submission, ProgressListener progressListener) {
            this.submission = submission;
            this.progressListener = progressListener;
        }

        public Set<Violation> call() {
            Timer timer = new Timer(this.getClass().getSimpleName() + " Timer");
            TreeSet<Violation> violations = new TreeSet<>();
            try (CloseableThreadContext.Instance closeable = LogUtil.startRuleRun()) {
                submission.initializeThreadLocals();
                submission.beforeRun();
                final Thread thread = Thread.currentThread();

                TimerTask task =
                        new TimerTask() {
                            @Override
                            public void run() {
                                if (thread.isAlive()) {
                                    String status = "Timed out after " + TIMEOUT + " ms";
                                    if (LOGGER.isInfoEnabled()) {
                                        LOGGER.info("Submission=" + submission + ", " + status);
                                    }
                                    // Mark the Executor as timed out and interrupt the thread.
                                    // Note: Calling thread.interrupt()
                                    // doesn't immediately interrupt the thread. Instead, the thread
                                    // will be interrupted when
                                    // something inside of it checks for Thread.isInterrupted().
                                    // This is most likely to happen
                                    // inside of a Gremlin query.
                                    timedOut = true;
                                    thread.interrupt();
                                }
                            }
                        };
                timer.schedule(task, TIMEOUT);
                violations.addAll(
                        new PathBasedRuleRunner(
                                        submission.getGraph(),
                                        submission.getRules(),
                                        submission.getPathEntry(),
                                        progressListener)
                                .runRules());
                timer.cancel();
            } catch (StackOverflowError | Exception ex) {
                // We don't want the timer to interrupt any of our exception handling, so
                // immediately cancel the timer.
                timer.cancel();
                // Check whether the thread is marked as having timed out.
                if (timedOut) {
                    // If the thread timed out, we should create a violation indicating that this
                    // occurred.
                    violations.add(
                            new Violation.TimeoutViolation(
                                    "Path evaluation timed out after " + TIMEOUT + " ms",
                                    submission.getPathEntry()));
                } else {
                    // If the thread threw another exception, wrap it in a violation, including the
                    // last line that executed
                    final String details =
                            ex.getClass().getSimpleName()
                                    + ": "
                                    + ex.getMessage()
                                    + ": "
                                    + ex.getStackTrace()[0];
                    violations.add(
                            new Violation.InternalErrorViolation(
                                    details, submission.getPathEntry()));
                }
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Error executing rule. submission=" + submission, ex);
                }
            } finally {
                // TODO: This should be in a method similar to initializeThreadLocals
                JustInTimeGraphProvider.remove();
            }
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Finished. method=" + submission.getPathEntry().toSimpleString());
            }
            submission.afterRun(violations);
            return violations;
        }
    }

    /**
     * Classes the wish to invoke `ThreadableRuleExecutor.run()` should supply a list of objects
     * that implement this interface.
     */
    public interface ThreadableRuleSubmission {
        /** The graph to be used when running rules. */
        GraphTraversalSource getGraph();

        /**
         * The MethodVertex at which path analysis should start. All paths starting at this vertex
         * will be evaluated against the rules.
         */
        MethodVertex getPathEntry();

        /** The list of rules to be executed. Only accepts path-based rules at this time. */
        List<AbstractPathBasedRule> getRules();

        /**
         * Invoked at the start of the Callable. Should do any initialization of thread-dependent
         * properties, e.g., JustInTime graphs.
         */
        // TODO: Consolidate with #beforeRun
        default void initializeThreadLocals() {
            // NO OP
        }

        /** Invoked in a Callable before the rules are evaluated. */
        default void beforeRun() {
            // NO OP
        }

        /**
         * Invoked in a Callable after the rules are evaluated
         *
         * @param violations - All violations that were created by this execution. TreeSet
         *     guarantees proper ordering.
         */
        default void afterRun(TreeSet<Violation> violations) {
            // NO OP
        }
    }

    /**
     * Exceptions thrown while evaluating rules are converted to violations if possible. If not
     * possible, then this exception will be thrown.
     */
    public static class ThreadableRuleExecutorException extends SfgeRuntimeException {
        public ThreadableRuleExecutorException(Throwable cause) {
            super(cause);
        }
    }
}
