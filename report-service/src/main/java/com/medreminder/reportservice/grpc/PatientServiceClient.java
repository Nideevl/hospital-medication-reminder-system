package com.medreminder.reportservice.grpc;

import com.medreminder.common.grpc.GetPatientRequest;
import com.medreminder.common.grpc.PatientInfo;
import com.medreminder.common.grpc.PatientServiceGrpc;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class PatientServiceClient {

    private final PatientServiceGrpc.PatientServiceBlockingStub patientServiceStub;

    public PatientServiceClient(@Qualifier("patientServiceBlockingStub") PatientServiceGrpc.PatientServiceBlockingStub patientServiceStub) {
        this.patientServiceStub = patientServiceStub;
    }

    public PatientInfo getPatient(String patientId) {
        GetPatientRequest request = GetPatientRequest.newBuilder()
                .setPatientId(patientId)
                .build();
        return patientServiceStub.getPatientById(request);
    }
}
