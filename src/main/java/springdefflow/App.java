package springdefflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import java.io.ByteArrayOutputStream;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import javax.mail.internet.MimeMessage;

@Configuration
@EnableAutoConfiguration(exclude = { JmxAutoConfiguration.class })
@EnableBinding(RabbitBindings.class) // <-- this is what causes the exception in the faulty flow.
public class App {

    public static void main(String[] args) {
        new SpringApplication(App.class).run(args);
    }

    static final Logger log = LoggerFactory.getLogger(App.class);

    public static class InputPayload {

        public CountDownLatch latch;
        public MimeMessage mimeMessage;
        public boolean subflow;
    }

    public static final String DONE_LATCH = "doneLatch";

    @Bean
    IntegrationFlow goodFlow() {

        return IntegrationFlows.from(inputGoodChannel())
            .enrichHeaders(m -> m.headerExpressions(h -> h.put(DONE_LATCH, "payload.latch")))
            .handle(parseMime())
            // subflow without the use of defaultOutputToParentFlow
            .<InputPayload, Boolean>route(p -> p.subflow,
                m -> m.subFlowMapping(false, f -> f.transform(p -> false))
                    .subFlowMapping(true, f -> f.transform(p -> 1)))
            .handle(logBitPayload())
            .handle(latchCountDown())
            .log().get();
    }

    @Bean
    IntegrationFlow faultyFlow() {

        return IntegrationFlows.from(inputFaultyChannel())
            .enrichHeaders(m -> m.headerExpressions(h -> h.put(DONE_LATCH, "payload.latch")))
            .handle(parseMime())
            // subflow with the use of defaultOutputToParentFlow
            .<InputPayload, Boolean>route(p -> p.subflow,
                m -> m.subFlowMapping(false, f -> f.transform(p -> false))
                    .resolutionRequired(false)
                    .defaultOutputToParentFlow())
            .handle(logBitPayload())
            .handle(latchCountDown())
            .log().get();
    }

    @Bean
    MessageChannel inputGoodChannel() {
        return MessageChannels.direct().get();
    }

    @Bean
    MessageChannel inputFaultyChannel() {
        return MessageChannels.direct().get();
    }

    @Bean
    ParseMime parseMime() {
        return new ParseMime();
    }

    class ParseMime {

        public Message<InputPayload> handle(Message<InputPayload> message) throws Exception {

            var bout = new ByteArrayOutputStream();
            message.getPayload().mimeMessage.writeTo(bout);
            log.info("Mime message written to {} bytes.", bout.size());
            return message;
        }
    }

    @Bean
    LogBitPayload logBitPayload() {
        return new LogBitPayload();
    }

    class LogBitPayload {

        public Message<Object> handle(Message<Object> message) {

            log.info("Payload value set to {}", message.getPayload());
            return message;
        }
    }

    @Bean
    LatchCountDown latchCountDown() {
        return new LatchCountDown();
    }

    class LatchCountDown {

        public Message<?> handle(Message<?> message) {

            var latch = Optional.ofNullable(message.getHeaders().get(DONE_LATCH, CountDownLatch.class));
            if (latch.isPresent()) {
                log.info("Releasing latch for message {}", message);
                latch.get().countDown();
            }
            return message;
        }
    }

}
