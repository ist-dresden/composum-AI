package com.composum.ai.backend.base.service.chat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Some POC using {@link CompletableFuture}.
 */
public class CheckCompletableFutureTest {

    private static ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    @AfterClass
    public static void tearDown() {
        scheduledExecutor.shutdownNow();
    }


    @Test
    public void testDelay1() throws ExecutionException, InterruptedException {
        CompletableFuture<String> result = new CompletableFuture<>();
        retry(result, 3);
        System.out.println("result = " + result.get());
    }

    CompletableFuture<String> retry(CompletableFuture<String> result, int count) {
        if (count > 0) {
            scheduledExecutor.schedule(() -> {
                        System.out.println("retrying " + count);
                        retry(result, count - 1);
                    }, 20, TimeUnit.MILLISECONDS
            );
        } else {
            result.complete("done");
        }
        return result;
    }

    /**
     * We simulate an asynchronous call that fails 2 times.
     */
    @Test
    public void testDelay2() throws ExecutionException, InterruptedException {
        CompletableFuture<String> result = new CompletableFuture<>();
        asyncRetry(result, 5);
        System.out.println("result = " + result.get());
    }

    void asyncRetry(CompletableFuture<String> result, int count) {
        CompletableFuture<String> callresult = call();
        callresult
                .thenAccept(result::complete)
                .exceptionally(e -> {
                    System.out.println("exception: " + e.getMessage());
                    if (count > 0) {
                        asyncRetry(result, count - 1);
                    } else {
                        result.completeExceptionally(e);
                    }
                    return null;
                });
    }

    private int count = 3;

    private CompletableFuture<String> call() {
        CompletableFuture<String> callresult = new CompletableFuture<>();
        scheduledExecutor.schedule(() -> {
            if (count-- > 0) {
                System.out.println("call failed " + count);
                callresult.completeExceptionally(new RuntimeException("Please retry"));
            } else {
                System.out.println("call succeeded");
                callresult.complete("DONE");
            }
        }, 20, TimeUnit.MILLISECONDS);
        return callresult;
    }

}
