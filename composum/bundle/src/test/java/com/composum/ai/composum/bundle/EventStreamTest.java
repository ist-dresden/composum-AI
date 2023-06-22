package com.composum.ai.composum.bundle;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.Flow;

import javax.servlet.ServletOutputStream;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.sling.xss.XSSFilter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.composum.ai.backend.base.service.chat.GPTFinishReason;
import com.composum.sling.core.util.ServiceHandle;

@RunWith(MockitoJUnitRunner.class)
public class EventStreamTest {

    @Rule
    public ErrorCollector ec = new ErrorCollector();

    @Mock
    private ServletOutputStream outputStream;

    @Mock
    private Flow.Subscription subscription;

    @Mock
    private XSSFilter xssFilter;

    private final StringBuilder buf = new StringBuilder();

    @Before
    public void setup() throws IOException, IllegalAccessException {
        Mockito.doAnswer(
                invocation -> {
                    String line = invocation.getArgument(0);
                    buf.append(line).append("\n");
                    return null;
                }
        ).when(outputStream).println(anyString());
        when(xssFilter.filter(anyString())).thenAnswer(invocation -> (String) invocation.getArgument(0));
        ServiceHandle xssfilterhandle =
                (ServiceHandle) FieldUtils.readStaticField(com.composum.sling.core.util.XSS.class, "XSSFilter_HANDLE", true);
        FieldUtils.writeField(xssfilterhandle, "service", xssFilter, true);
    }

    @Test(timeout = 1000)
    public void testGoodFlow() throws InterruptedException {
        EventStream eventStream = new EventStream();
        eventStream.setId("testId");
        eventStream.onSubscribe(subscription);
        eventStream.onNext("testItem1 ");
        eventStream.onNext("testItem2 ");
        eventStream.onNext("testItem3.");
        eventStream.onFinish(GPTFinishReason.STOP);
        eventStream.onComplete();
        eventStream.writeTo(outputStream);
        String expected = "data: \"testItem1 \"\n" +
                "data: \"testItem2 \"\n" +
                "data: \"testItem3.\"\n" +
                "\n" +
                "\n" +
                "event: finish\n" +
                "data: {\"status\":200,\"success\":true,\"warning\":false,\"data\":{\"result\":{\"finishreason\":\"STOP\"}}}\n" +
                "\n";
        ec.checkThat(buf.toString(), is(expected));
        ec.checkThat(eventStream.getWholeResponse(), is("testItem1 testItem2 testItem3."));
    }

    @Test
    public void onErrorTest() throws InterruptedException {
        EventStream eventStream = new EventStream();
        eventStream.setId("testId");

        eventStream.onSubscribe(subscription);

        Throwable throwable = new Throwable("testError");
        eventStream.onError(throwable);

        verify(subscription).cancel();
        ec.checkThat(eventStream.queue.contains("event: error"), is(true));
        eventStream.writeTo(outputStream);
        ec.checkThat(buf.toString().replaceAll("\\d{13}", "<timestamp>")
                , is(("\n" +
                        "\n" +
                        "event: error\n" +
                        "data: {\"status\":400,\"success\":false,\"warning\":false,\"title\":\"Error\",\"messages\":[{\"level\":\"error\",\"text\":\"Internal error: java.lang.Throwable: testError\",\"rawText\":\"Internal error: java.lang.Throwable: testError\",\"arguments\":[\"testError\"],\"timestamp\":1687439360950}]}\n" +
                        "\n").replaceAll("\\d{13}", "<timestamp>")));
    }
}
