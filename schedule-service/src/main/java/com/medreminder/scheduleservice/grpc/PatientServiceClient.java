package com.medreminder.scheduleservice.grpc;

// UPDATED IMPORTS
import com.medreminder.common.grpc.GetCaregiverPhoneRequest;
import com.medreminder.common.grpc.GetCaregiverPhoneResponse;
import com.medreminder.common.grpc.GetPatientRequest;
import com.medreminder.common.grpc.PatientInfo;
import com.medreminder.common.grpc.PatientServiceGrpc;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * gRPC Client wrapper for Patient Service.
 * Provides methods to fetch patient data from Patient Service.
 */
@Component
public class PatientServiceClient {

    private static final Logger log = LoggerFactory.getLogger(PatientServiceClient.class);
    private static final long TIMEOUT_SECONDS = 5;

    private final PatientServiceGrpc.PatientServiceBlockingStub patientServiceStub;

    @Autowired
    public PatientServiceClient(PatientServiceGrpc.PatientServiceBlockingStub patientServiceStub) {
        this.patientServiceStub = patientServiceStub;
    }

    /**
     * Fetches patient details by patient ID.
     *
     * @param patientId UUID of the patient
     * @return PatientInfo containing patient details (Updated return type)
     * @throws StatusRuntimeException if gRPC call fails
     */
    public PatientInfo getPatientById(UUID patientId) {
        log.debug("Fetching patient details for patientId: {}", patientId);

        try {
            GetPatientRequest request = GetPatientRequest.newBuilder()
                    .setPatientId(patientId.toString())
                    .build();

            // Updated response type to match proto
            PatientInfo response = patientServiceStub
                    .withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .getPatientById(request);

            log.debug("Successfully fetched patient details for patientId: {}", patientId);
            return response;

        } catch (StatusRuntimeException e) {
            log.error("Failed to fetch patient details for patientId: {}. Error: {}", 
                    patientId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Fetches caregiver phone number for a patient.
     *
     * @param patientId UUID of the patient
     * @return String caregiver phone number
     * @throws StatusRuntimeException if gRPC call fails or patient has no caregiver
     */
    public String getCaregiverPhone(UUID patientId) {
        log.debug("Fetching caregiver phone for patientId: {}", patientId);

        try {
            GetCaregiverPhoneRequest request = GetCaregiverPhoneRequest.newBuilder()
                    .setPatientId(patientId.toString())
                    .build();

            GetCaregiverPhoneResponse response = patientServiceStub
                    .withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .getCaregiverPhone(request);

            // Updated to getCaregiverPhone() to match the proto property name
            String phone = response.getCaregiverPhone(); 
            log.debug("Successfully fetched caregiver phone for patientId: {}", patientId);
            return phone;

        } catch (StatusRuntimeException e) {
            log.error("Failed to fetch caregiver phone for patientId: {}. Error: {}", 
                    patientId, e.getMessage(), e);
            throw e;
        }
    }
}