package com.medreminder.scheduleservice.grpc;

import com.medreminder.common.grpc.GetSchedulesRequest;
import com.medreminder.common.grpc.ScheduleItem;
import com.medreminder.common.grpc.ScheduleServiceGrpc;
import com.medreminder.common.grpc.SchedulesResponse;
import com.medreminder.scheduleservice.entity.Schedule;
import com.medreminder.scheduleservice.service.ScheduleService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
public class ScheduleGrpcService extends ScheduleServiceGrpc.ScheduleServiceImplBase {
    @Autowired
    private ScheduleService scheduleService;

    @Override
    public void getSchedulesForToday(GetSchedulesRequest request, StreamObserver<SchedulesResponse> responseObserver) {
        try {
            UUID patientId = UUID.fromString(request.getPatientId());
            List<Schedule> schedules = scheduleService.getSchedulesForToday(patientId);

            SchedulesResponse.Builder responseBuilder = SchedulesResponse.newBuilder();
            for (Schedule schedule : schedules) {
                ScheduleItem item = ScheduleItem.newBuilder()
                        .setScheduleId(schedule.getScheduleId().toString())
                        .setMedicationId(schedule.getMedicationId().toString())
                        .setScheduledTime(schedule.getDoseTimes())
                        .setStatus(schedule.getActive() ? "ACTIVE" : "INACTIVE")
                        .build();
                responseBuilder.addSchedules(item);
            }
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}