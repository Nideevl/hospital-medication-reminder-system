package com.medreminder.patient.grpc;

import com.medreminder.common.grpc.GetPatientByIdRequest;
import com.medreminder.common.grpc.GetPatientByIdResponse;
import com.medreminder.common.grpc.GetCaregiverPhoneRequest;
import com.medreminder.common.grpc.GetCaregiverPhoneResponse;
import com.medreminder.common.grpc.PatientServiceGrpc;
import com.medreminder.patient.entity.Patient;
import com.medreminder.patient.exception.ResourceNotFoundException;
import com.medreminder.patient.service.PatientService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PatientGrpcService extends PatientServiceGrpc.PatientServiceImplBase {

    @Autowired
    private PatientService patientService;

    @Override
    public void getPatientById(GetPatientByIdRequest request,
                               StreamObserver<GetPatientByIdResponse> responseObserver) {
        try {
            String patientIdStr = request.getPatientId();
            UUID patientId = UUID.fromString(patientIdStr);

            Patient patient = patientService.getPatientById(patientId);

            GetPatientByIdResponse response = GetPatientByIdResponse.newBuilder()
                    .setPatientId(patient.getPatientId().toString())
                    .setFirstName(patient.getFirstName())
                    .setLastName(patient.getLastName())
                    .setPhoneNumber(patient.getPhoneNumber())
                    .setLocation(patient.getLocation())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (ResourceNotFoundException e) {
            responseObserver.onError(
                    Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(
                    Status.INTERNAL.withDescription("Error retrieving patient: " + e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getCaregiverPhone(GetCaregiverPhoneRequest request,
                                  StreamObserver<GetCaregiverPhoneResponse> responseObserver) {
        try {
            String patientIdStr = request.getPatientId();
            UUID patientId = UUID.fromString(patientIdStr);

            Patient patient = patientService.getPatientById(patientId);

            // For now, returning patient's phone as caregiver phone (placeholder)
            GetCaregiverPhoneResponse response = GetCaregiverPhoneResponse.newBuilder()
                    .setCaregiverPhone(patient.getPhoneNumber())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (ResourceNotFoundException e) {
            responseObserver.onError(
                    Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(
                    Status.INTERNAL.withDescription("Error retrieving caregiver phone: " + e.getMessage()).asRuntimeException());
        }
    }
}