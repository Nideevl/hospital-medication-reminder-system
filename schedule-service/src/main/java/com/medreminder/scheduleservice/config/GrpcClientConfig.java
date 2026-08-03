package com.medreminder.scheduleservice.config;

import com.medreminder.patientservice.grpc.PatientServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * gRPC Client Configuration for Schedule Service.
 * Creates channels and stubs for communicating with other microservices.
 */
@Configuration
public class GrpcClientConfig {

    private static final String PATIENT_SERVICE_HOST = "localhost";
    private static final int PATIENT_SERVICE_PORT = 9001;

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
     * Used for synchronous gRPC calls to Patient Service.
     *
     * @param channel ManagedChannel for Patient Service
     * @return PatientServiceBlockingStub
     */
    @Bean
    public PatientServiceGrpc.PatientServiceBlockingStub patientServiceBlockingStub(
            ManagedChannel patientServiceChannel) {
        return PatientServiceGrpc.newBlockingStub(patientServiceChannel);
    }
}
