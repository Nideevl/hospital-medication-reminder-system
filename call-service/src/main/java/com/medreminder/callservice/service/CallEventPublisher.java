package com.medreminder.callservice.service;

import com.medreminder.callservice.entity.CallLog;
import com.medreminder.common.dto.CallResponseEvent;
import com.medreminder.common.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kafka Event Publisher for Call Service.
 * Publishes call-response-received events when call responses are received.
 */
@Component
public class CallEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CallEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public CallEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes a call-response-received event based on call log.
     *
     * @param callLog CallLog entity containing call details
     */
    public void publishCallResponse(CallLog callLog) {
        try {
            CallResponseEvent event = buildCallResponseEvent(callLog, null);
            
            Message<CallResponseEvent> message = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, Constants.KAFKA_TOPIC_CALL_RESPONSE)
                    .setHeader(KafkaHeaders.KEY, callLog.getCallId().toString())
                    .build();

            kafkaTemplate.send(message);

            log.info("Successfully published call-response-received event for callId: {}, patientId: {}, scheduleId: {}",
                    callLog.getCallId(), callLog.getPatientId(), callLog.getScheduleId());

        } catch (Exception e) {
            log.error("Failed to publish call-response-received event for callId: {}, error: {}",
                    callLog.getCallId(), e.getMessage(), e);
            throw new RuntimeException("Failed to publish call response event", e);
        }
    }

    /**
     * Publishes a call-response-received event with additional response status.
     *
     * @param callLog CallLog entity containing call details
     * @param response Response status (TAKEN/NOT_TAKEN/NO_RESPONSE)
     */
    public void publishCallResponseWithStatus(CallLog callLog, String response) {
        try {
            CallResponseEvent event = buildCallResponseEvent(callLog, response);
            
            Message<CallResponseEvent> message = MessageBuilder
                    .withPayload(event)
                    .setHeader(KafkaHeaders.TOPIC, Constants.KAFKA_TOPIC_CALL_RESPONSE)
                    .setHeader(KafkaHeaders.KEY, callLog.getCallId().toString())
                    .build();

            kafkaTemplate.send(message);

            log.info("Successfully published call-response-received event with status {} for callId: {}, patientId: {}, scheduleId: {}",
                    response, callLog.getCallId(), callLog.getPatientId(), callLog.getScheduleId());

        } catch (Exception e) {
            log.error("Failed to publish call-response-received event with status {} for callId: {}, error: {}",
                    response, callLog.getCallId(), e.getMessage(), e);
            throw new RuntimeException("Failed to publish call response event with status", e);
        }
    }

    /**
     * Builds a CallResponseEvent from CallLog entity.
     *
     * @param callLog CallLog entity
     * @param response Optional response status
     * @return CallResponseEvent
     */
    private CallResponseEvent buildCallResponseEvent(CallLog callLog, String response) {
        return CallResponseEvent.builder()
                .callId(callLog.getCallId().toString())
                .patientId(callLog.getPatientId().toString())
                .scheduleId(callLog.getScheduleId().toString())
                .responseReceived(callLog.isResponseReceived())
                .response(response)
                .responseTime(LocalDateTime.now())
                .build();
    }
}
