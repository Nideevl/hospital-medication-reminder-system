package com.medreminder.reportservice.grpc;

import com.medreminder.common.grpc.GetSchedulesRequest;
import com.medreminder.common.grpc.ScheduleServiceGrpc;
import com.medreminder.common.grpc.SchedulesResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ScheduleServiceClient {

    private final ScheduleServiceGrpc.ScheduleServiceBlockingStub scheduleServiceStub;

    public ScheduleServiceClient(@Qualifier("scheduleServiceBlockingStub") ScheduleServiceGrpc.ScheduleServiceBlockingStub scheduleServiceStub) {
        this.scheduleServiceStub = scheduleServiceStub;
    }

    public SchedulesResponse getSchedules(String patientId) {
        GetSchedulesRequest request = GetSchedulesRequest.newBuilder()
                .setPatientId(patientId)
                .build();
        return scheduleServiceStub.getSchedulesForToday(request);
    }
}
