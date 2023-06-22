package com.composum.ai.composum.bundle;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Flow;

import javax.servlet.ServletOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.base.service.chat.GPTCompletionCallback;
import com.composum.ai.backend.base.service.chat.GPTFinishReason;
import com.composum.sling.core.servlet.Status;
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
    private final Queue<String> queue = new ArrayBlockingQueue<>(100);

    private final Gson gson = new Gson();

    public void setId(String id) {
        this.id = id;
    }

    public void writeTo(ServletOutputStream outputStream) {
        while (true) {
            String line = queue.poll();
            if (QUEUEEND.equals(line)) {
                LOG.debug("EventStream.writeTo finished for {}", id);
                return;
            }
            try {
                outputStream.println(line);
            } catch (IOException e) {
                LOG.error("Error writing to {} : {}", id, e.toString());
                onError(e);
                return;
            }
        }
    }

    @Override
    public void onFinish(GPTFinishReason finishReason) {
        LOG.debug("EventStream.onFinish");
        Status status = new Status(null, null, LOG);
        status.data(AIServlet.RESULTKEY).put(AIServlet.RESULTKEY_FINISHREASON, finishReason.name());
        queue.add("\n");
        queue.add("event: finish");
        queue.add("data: " + status.getJsonString());
        queue.add("");
        queue.add(QUEUEEND);
        if (subscription != null) {
            subscription.cancel();
        }
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        LOG.debug("EventStream.onSubscribe for {}", id);
        this.subscription = subscription;
        subscription.request(10000);
    }

    @Override
    public void onNext(String item) {
        LOG.debug("EventStream.onNext for {} : {}", id, item);
        queue.add("data: " + gson.toJson(item));
    }

    @Override
    public void onError(Throwable throwable) {
        LOG.error("EventStream.onError for {} : {}", id, throwable.toString(), throwable);
        if (subscription != null) {
            subscription.cancel();
        }
        Status status = new Status(null, null, LOG);
        status.error("Internal error: " + throwable.toString(), throwable);
        queue.add("\n");
        queue.add("event: error");
        queue.add("data: " + status.getJsonString());
        queue.add("");
        queue.add(QUEUEEND);
    }
}
