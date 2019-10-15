package springdefflow;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.mail.internet.MimeMessage;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = App.class)
@SuppressWarnings("unused")
public class AppTest {

    static final Logger log = LoggerFactory.getLogger(AppTest.class);

    @Autowired
    @Qualifier("inputGoodChannel")
    MessageChannel goodChannel;

    @Autowired
    @Qualifier("inputFaultyChannel")
    MessageChannel faultyChannel;

    @Autowired
    ObjectMapper mapper;

    final String mimeFile = "example.mime";

    @Test
    public void testFailJsonMime() throws Exception {

        var mime = new MimeMessage(null, new ByteArrayInputStream(loadResource(mimeFile)));
        try {
            mapper.writer().writeValueAsString(mime);
            fail("Expected mime to JSON conversion failure.");
        } catch (Exception e) {
            log.info("Got expected exception when converting mime to json: " + e);
        }
    }

    @Test
    public void testAppFlowGood() throws Exception {

        assertNotNull(goodChannel);
        var msg = buildPayload();
        log.info("Sending message on good channel");
        goodChannel.send(MessageBuilder.withPayload(msg).build());
        assertTrue("Message on good channel processed.", msg.latch.await(3L, TimeUnit.SECONDS));
    }

    @Test
    public void testAppFlowBad() throws Exception {

        assertNotNull(faultyChannel);
        var msg = buildPayload();
        log.info("Sending message on faulty channel");
        faultyChannel.send(MessageBuilder.withPayload(msg).build());
        assertTrue("Message on faulty channel processed.", msg.latch.await(3L, TimeUnit.SECONDS));
    }

    App.InputPayload buildPayload() throws Exception {

        var msg = new App.InputPayload();
        msg.latch = new CountDownLatch(1);
        msg.mimeMessage = new MimeMessage(null, new ByteArrayInputStream(loadResource(mimeFile)));
        msg.subflow = true;
        return msg;
    }

    byte[] loadResource(String fileName) throws Exception {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName)) {
            return in.readAllBytes();
        }
    }

}
