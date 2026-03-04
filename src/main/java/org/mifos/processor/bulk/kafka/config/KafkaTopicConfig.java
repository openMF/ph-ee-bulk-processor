package org.mifos.processor.bulk.kafka.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
public class KafkaTopicConfig {

    @Value(value = "${kafka.bootstrapAddress}")
    private String bootstrapAddress;

    @Value(value = "${kafka.topic.gsma.name}")
    private String gsmaTopicName;

    @Value(value = "${kafka.topic.slcb.name}")
    private String slcbTopicName;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic gsmaTopic() {
        return new NewTopic(gsmaTopicName, 1, (short) 1);
    }

    @Bean
    public NewTopic slcbTopic() {
        return new NewTopic(slcbTopicName, 1, (short) 1);
    }
}
