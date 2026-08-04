package com.medreminder.callservice.config;

import com.medreminder.common.grpc.PatientServiceGrpc;
import com.medreminder.common.grpc.ScheduleServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * gRPC Client Configuration for Call Service.
 * Creates channels and stubs for communicating with other microservices.
 */
@Configuration
public class GrpcClientConfig {

    private static final String PATIENT_SERVICE_HOST = "localhost";
    private static final int PATIENT_SERVICE_PORT = 9001;
    private static final String SCHEDULE_SERVICE_HOST = "localhost";
    private static final int SCHEDULE_SERVICE_PORT = 9002;

    @Bean
    public ManagedChannel patientServiceChannel() {
        return ManagedChannelBuilder.forAddress(PATIENT_SERVICE_HOST, PATIENT_SERVICE_PORT)
                .usePlaintext()
                .build();
    }

    @Bean
    public PatientServiceGrpc.PatientServiceBlockingStub patientServiceBlockingStub(
            ManagedChannel patientServiceChannel) {
        return PatientServiceGrpc.newBlockingStub(patientServiceChannel);
    }

    @Bean
    public ManagedChannel scheduleServiceChannel() {
        return ManagedChannelBuilder.forAddress(SCHEDULE_SERVICE_HOST, SCHEDULE_SERVICE_PORT)
                .usePlaintext()
                .build();
    }

    @Bean
    public ScheduleServiceGrpc.ScheduleServiceBlockingStub scheduleServiceBlockingStub(
            ManagedChannel scheduleServiceChannel) {
        return ScheduleServiceGrpc.newBlockingStub(scheduleServiceChannel);
    }
}
