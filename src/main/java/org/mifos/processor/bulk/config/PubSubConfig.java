package org.mifos.processor.bulk.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PubSubConfig {

    @Value("${pubsub.room.code}")
    public String roomCode;

    @Value("${pubsub.room.class}")
    public String roomClass;

    @Value("${pubsub.event.type}")
    public String eventType;

}
