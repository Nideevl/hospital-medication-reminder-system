package com.medreminder.callservice.grpc;

import com.medreminder.common.grpc.GetSchedulesRequest;
import com.medreminder.common.grpc.ScheduleItem;
import com.medreminder.common.grpc.ScheduleServiceGrpc;
import com.medreminder.common.grpc.SchedulesResponse;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * gRPC Client wrapper for Schedule Service.
 * Provides methods to fetch schedule information from Schedule Service.
 */
@Component
public class ScheduleServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ScheduleServiceClient.class);
    private static final long TIMEOUT_SECONDS = 5;

    private final ScheduleServiceGrpc.ScheduleServiceBlockingStub scheduleServiceStub;

    @Autowired
    public ScheduleServiceClient(ScheduleServiceGrpc.ScheduleServiceBlockingStub scheduleServiceStub) {
        this.scheduleServiceStub = scheduleServiceStub;
    }

    public List<ScheduleItem> getSchedulesForToday(UUID patientId) {
        log.debug("Fetching todays schedules for patientId: {}", patientId);

        try {
            GetSchedulesRequest request = GetSchedulesRequest.newBuilder()
                    .setPatientId(patientId.toString())
                    .build();

            SchedulesResponse response = scheduleServiceStub
                    .withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .getSchedulesForToday(request);

            List<ScheduleItem> schedules = response.getSchedulesList();
            log.debug("Successfully fetched {} schedules for patientId: {}", schedules.size(), patientId);
            return schedules;

        } catch (StatusRuntimeException e) {
            log.error("Failed to fetch schedules for patientId: {}. Error: {}", 
                    patientId, e.getMessage(), e);
            throw e;
        }
    }
}
