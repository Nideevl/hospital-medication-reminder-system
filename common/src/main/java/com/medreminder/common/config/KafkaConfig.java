package com.medreminder.common.config;

import com.medreminder.common.util.Constants;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic medicationDueTopic() {
        return new NewTopic(
                Constants.KAFKA_TOPIC_MEDICATION_DUE,
                Constants.KAFKA_PARTITION_COUNT_MEDICATION_DUE,
                Constants.KAFKA_REPLICATION_FACTOR
        );
    }

    @Bean
    public NewTopic callResponseReceivedTopic() {
        return new NewTopic(
                Constants.KAFKA_TOPIC_CALL_RESPONSE_RECEIVED,
                Constants.KAFKA_PARTITION_COUNT_MEDICATION_DUE,
                Constants.KAFKA_REPLICATION_FACTOR
        );
    }

    @Bean
    public NewTopic doseMissedTopic() {
        return new NewTopic(
                Constants.KAFKA_TOPIC_DOSE_MISSED,
                Constants.KAFKA_PARTITION_COUNT_DOSE_MISSED,
                Constants.KAFKA_REPLICATION_FACTOR
        );
    }
}