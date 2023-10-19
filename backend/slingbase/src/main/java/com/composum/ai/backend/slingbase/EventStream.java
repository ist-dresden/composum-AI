package com.composum.ai.backend.slingbase;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.base.service.StringstreamSlowdown;
import com.composum.ai.backend.base.service.chat.GPTCompletionCallback;
import com.composum.ai.backend.base.service.chat.GPTFinishReason;
import com.google.gson.Gson;

public class EventStream implements GPTCompletionCallback {

    private static final Logger LOG = LoggerFactory.getLogger(EventStream.class);

    /**
     * Special message in the {@link #queue} that signals that we are done. Impossible in the normal stream.
     */
    public static final String QUEUEEND = ":queueend";

    private String id;

    private volatile Flow.Subscription subscription;

    /**
     * concurrent queue with strings as lines to write, already in SSE format.
     */
    final BlockingQueue<String> queue = new ArrayBlockingQueue<>(100);

    final StringBuilder wholeResponse = new StringBuilder();

    final List<Consumer<String>> wholeResponseListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    private volatile GPTFinishReason finishReason;

    private final Gson gson = new Gson();

    private final StringstreamSlowdown slowdown = new StringstreamSlowdown(this::writeData, 250);

    public void setId(String id) {
        this.id = id;
    }

    public void writeTo(PrintWriter writer) throws InterruptedException {
        while (true) {
            String line;
            try {
                line = queue.poll(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                onError(e);
                throw e;
            }
            if (line == null) {
                LOG.error("EventStream.writeTo timed out for {}", id);
                onError(new IOException("timed out"));
                return;
            }
            LOG.trace("EventStream.writeTo {} line {}", id, line);
            if (QUEUEEND.equals(line)) {
                LOG.debug("EventStream.writeTo finished for {}", id);
                return;
            }
            try {
                writer.println(line);
                writer.flush();
            } catch (RuntimeException e) {
                LOG.error("Error writing to {} : {}", id, e.toString());
                subscription.cancel();
                throw e;
            }
        }
    }

    /**
     * Successfull conclusion; puts event 'finished' into the stream with data JSON like this:
     * {"success":true,"data":{"result":{"finishreason":"STOP"}}}
     */
    @Override
    public void onFinish(GPTFinishReason finishReason) {
        LOG.debug("EventStream.onFinish for {} : {}", id, finishReason);
        slowdown.flush();
        this.finishReason = finishReason;
        Map<String, Object> status = Map.of("success", true,
                "data", Map.of(
                        "result", Map.of("finishreason", finishReason.name())));
        queue.add("");
        queue.add("event: finished");
        queue.add("data: " + gson.toJson(status));
        queue.add("");
        queue.add("");
        queue.add(QUEUEEND);
        if (null != getWholeResponse()) {
            wholeResponseListeners.forEach(listener -> listener.accept(getWholeResponse()));
        }
    }

    @Override
    public void setLoggingId(String loggingId) {
        LOG.debug("EventStream.setLoggingId for {} : {}", id, loggingId);
    }

    public void addWholeResponseListener(Consumer<String> listener) {
        wholeResponseListeners.add(listener);
    }

    /**
     * Returns the whole response, but only if it was received completely, otherwise null.
     */
    @Nullable
    public String getWholeResponse() {
        if (finishReason == GPTFinishReason.STOP) {
            return wholeResponse.toString();
        } else {
            return null;
        }
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        LOG.debug("EventStream.onSubscribe for {}", id);
        this.subscription = subscription;
        subscription.request(10000);
    }

    @Override
    public void onNext(String data) {
        LOG.trace("EventStream.onNext for {} : {}", id, data);
        slowdown.accept(data);
    }

    protected void writeData(String data) {
        // data = XSS.filter(data); // OUCH - that breaks things sometimes and doesn't really work as the troublesome
        // stuff could be spread out...
        // TODO: find a better way to filter the output
        queue.add("data: " + gson.toJson(data));
        queue.add(""); // empty line to separate events and force processing of this event
        wholeResponse.append(data);
    }

    /**
     * Puts an 'exception' event into the stream with data JSON like this: {"success":false,"title":"Internal error","messages":[{"level":"error","text":"something happened"}]}
     */
    @Override
    public void onError(Throwable throwable) {
        LOG.error("EventStream.onError for {} : {}", id, throwable.toString(), throwable);
        if (subscription != null) {
            subscription.cancel();
        }
        String errorDescription = throwable.toString();
        Map<String, Object> status = Map.of("success", false,
                "title", "Internal error",
                "messages", List.of(Map.of("level", "error", "text", errorDescription)));
        queue.add("");
        queue.add("event: exception"); // do not use 'error' as event name as that is received when the connection is closed.
        queue.add("data: " + gson.toJson(status));
        queue.add("");
        queue.add("");
        queue.add(QUEUEEND);
    }
}
