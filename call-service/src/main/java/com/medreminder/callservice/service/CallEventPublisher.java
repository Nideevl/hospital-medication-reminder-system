package com.medreminder.callservice.service;

import com.medreminder.callservice.entity.CallLog;
import com.medreminder.common.dto.CallResponseEvent;
import com.medreminder.common.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CallEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CallEventPublisher.class);

    private final KafkaTemplate<String, CallResponseEvent> kafkaTemplate;

    public CallEventPublisher(KafkaTemplate<String, CallResponseEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishCallResponse(CallLog callLog, String response) {
        CallResponseEvent event = buildCallResponseEvent(callLog, response);
        log.info("Publishing CallResponseEvent to topic {}: {}", Constants.KAFKA_TOPIC_CALL_RESPONSE, event);
        kafkaTemplate.send(Constants.KAFKA_TOPIC_CALL_RESPONSE, callLog.getScheduleId().toString(), event);
    }

    private CallResponseEvent buildCallResponseEvent(CallLog callLog, String response) {
        return CallResponseEvent.builder()
                .callLogId(callLog.getCallId())
                .scheduleId(callLog.getScheduleId())
                .callStatus(callLog.getCallStatus())
                .ivrResponse(response != null ? response : callLog.getIvrResponse())
                .timestamp(LocalDateTime.now())
                .build();
    }
}
