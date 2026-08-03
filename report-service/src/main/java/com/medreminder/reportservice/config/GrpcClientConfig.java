package com.medreminder.reportservice.config;

import com.medreminder.patientservice.grpc.PatientServiceGrpc;
import com.medreminder.scheduleservice.grpc.ScheduleServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * gRPC Client Configuration for Report Service.
 * Creates channels and stubs for communicating with other microservices.
 */
@Configuration
public class GrpcClientConfig {

    private static final String PATIENT_SERVICE_HOST = "localhost";
    private static final int PATIENT_SERVICE_PORT = 9001;
    private static final String SCHEDULE_SERVICE_HOST = "localhost";
    private static final int SCHEDULE_SERVICE_PORT = 9002;

    /**
     * Creates a ManagedChannel for Patient Service.
     * Port 9001 matches PatientServices GrpcServerConfig port.
     *
     * @return ManagedChannel connected to Patient Service
     */
    @Bean
    public ManagedChannel patientServiceChannel() {
        return ManagedChannelBuilder.forAddress(PATIENT_SERVICE_HOST, PATIENT_SERVICE_PORT)
                .usePlaintext()
                .build();
    }

    /**
     * Creates a blocking stub for Patient Service.
     *
     * @param channel ManagedChannel for Patient Service
     * @return PatientServiceBlockingStub
     */
    @Bean
    public PatientServiceGrpc.PatientServiceBlockingStub patientServiceBlockingStub(
            ManagedChannel patientServiceChannel) {
        return PatientServiceGrpc.newBlockingStub(patientServiceChannel);
    }

    /**
     * Creates a ManagedChannel for Schedule Service.
     * Port 9002 matches ScheduleServices GrpcServerConfig port.
     *
     * @return ManagedChannel connected to Schedule Service
     */
    @Bean
    public ManagedChannel scheduleServiceChannel() {
        return ManagedChannelBuilder.forAddress(SCHEDULE_SERVICE_HOST, SCHEDULE_SERVICE_PORT)
                .usePlaintext()
                .build();
    }

    /**
     * Creates a blocking stub for Schedule Service.
     *
     * @param channel ManagedChannel for Schedule Service
     * @return ScheduleServiceBlockingStub
     */
    @Bean
    public ScheduleServiceGrpc.ScheduleServiceBlockingStub scheduleServiceBlockingStub(
            ManagedChannel scheduleServiceChannel) {
        return ScheduleServiceGrpc.newBlockingStub(scheduleServiceChannel);
    }
}
