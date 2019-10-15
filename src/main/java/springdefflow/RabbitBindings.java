package springdefflow;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.MessageChannel;

public interface RabbitBindings {

    @Input("rabbitChannel")
    MessageChannel rabbitChannel();
}
