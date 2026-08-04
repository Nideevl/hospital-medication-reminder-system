package com.medreminder.common.util;

public final class Constants {

    private Constants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // Kafka Topics
    public static final String KAFKA_TOPIC_CALL_RESPONSE = "call-response-topic";
    public static final String KAFKA_TOPIC_MEDICATION_DUE = "medication-due";
    public static final String KAFKA_TOPIC_DOSE_MISSED = "dose-missed";

    // Kafka Partitions & Replication
    public static final int KAFKA_PARTITION_COUNT_MEDICATION_DUE = 10;
    public static final int KAFKA_PARTITION_COUNT_DOSE_MISSED = 5;
    public static final short KAFKA_REPLICATION_FACTOR = 1;

    // RabbitMQ Queues
    public static final String RABBITMQ_QUEUE_ESCALATION = "escalation-queue";
    public static final String RABBITMQ_QUEUE_EMAIL = "email-queue";
    public static final String RABBITMQ_QUEUE_SMS = "sms-queue";

    // RabbitMQ Exchanges & Routing Keys
    public static final String RABBITMQ_EXCHANGE_DEFAULT = "med-reminder-exchange";
    public static final String RABBITMQ_ROUTING_KEY_ESCALATION = "escalation.route";
    public static final String RABBITMQ_ROUTING_KEY_EMAIL = "email.route";
    public static final String RABBITMQ_ROUTING_KEY_SMS = "sms.route";

    // gRPC Services
    public static final int GRPC_SERVICE_PATIENT_PORT = 9001;
    public static final int GRPC_SERVICE_SCHEDULE_PORT = 9002;
    public static final int GRPC_SERVICE_CALL_PORT = 9003;
}