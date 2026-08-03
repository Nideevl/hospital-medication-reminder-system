package com.medreminder.reportservice.grpc;

import com.medreminder.scheduleservice.grpc.ScheduleServiceGrpc;
import com.medreminder.scheduleservice.grpc.ScheduleServiceOuterClass.*;
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

    /**
     * Fetches all schedules for today for a given patient.
     *
     * @param patientId UUID of the patient
     * @return List of ScheduleResponse containing todays schedules
     * @throws StatusRuntimeException if gRPC call fails
     */
    public List<ScheduleResponse> getSchedulesForToday(UUID patientId) {
        log.debug("Fetching todays schedules for patientId: {}", patientId);

        try {
            GetSchedulesRequest request = GetSchedulesRequest.newBuilder()
                    .setPatientId(patientId.toString())
                    .build();

            GetSchedulesResponse response = scheduleServiceStub
                    .withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .getSchedulesForToday(request);

            List<ScheduleResponse> schedules = response.getSchedulesList();
            log.debug("Successfully fetched {} schedules for patientId: {}", schedules.size(), patientId);
            return schedules;

        } catch (StatusRuntimeException e) {
            log.error("Failed to fetch schedules for patientId: {}. Error: {}", 
                    patientId, e.getMessage(), e);
            throw e;
        }
    }
}
