package springdefflow;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.MessageChannel;

/**
 * This class only exists so that EnableBinding can be used.
 * It has no other function.
 */
public interface RabbitBindings {

    @Input("rabbitChannel")
    MessageChannel rabbitChannel();
}
