package com.medreminder.patient.grpc;

import com.medreminder.common.grpc.GetPatientRequest;
import com.medreminder.common.grpc.PatientInfo;
import com.medreminder.common.grpc.PatientServiceGrpc;
import com.medreminder.patient.entity.Patient;
import com.medreminder.patient.service.PatientService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class PatientGrpcService extends PatientServiceGrpc.PatientServiceImplBase {

    @Autowired
    private PatientService patientService;

    @Override
    public void getPatient(GetPatientRequest request, StreamObserver<PatientInfo> responseObserver) {
        try {
            log.info("gRPC: Fetching patient with ID: {}", request.getPatientId());
            UUID patientId = UUID.fromString(request.getPatientId());
            Patient patient = patientService.getPatientById(patientId);
            
            PatientInfo patientInfo = PatientInfo.newBuilder()
                    .setPatientId(patient.getPatientId().toString())
                    .setFirstName(patient.getFirstName())
                    .setLastName(patient.getLastName())
                    .setEmail(patient.getEmail())
                    .setPhoneNumber(patient.getPhoneNumber())
                    .setMedicalRecordNumber(patient.getMedicalRecordNumber())
                    .setActive(patient.getActive())
                    .build();
            
            responseObserver.onNext(patientInfo);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            log.error("Invalid patient ID format: {}", request.getPatientId());
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid patient ID format")
                    .asException());
        } catch (Exception e) {
            log.error("Error fetching patient", e);
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Patient not found")
                    .asException());
        }
    }
}
