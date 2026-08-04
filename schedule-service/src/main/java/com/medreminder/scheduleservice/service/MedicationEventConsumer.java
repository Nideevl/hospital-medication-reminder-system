package com.medreminder.scheduleservice.service;
import com.medreminder.common.dto.CallResponseEvent;
import com.medreminder.common.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
@Service
public class MedicationEventConsumer {
    private static final Logger logger = LoggerFactory.getLogger(MedicationEventConsumer.class);
    
    @Autowired
    private ScheduleService scheduleService;
    
    @KafkaListener(topics = Constants.KAFKA_TOPIC_CALL_RESPONSE, groupId = "${spring.kafka.consumer.group-id:schedule-service-group}")
    public void consumeCallResponseEvent(CallResponseEvent event) {
        logger.info("Received CallResponseEvent: {}", event);
        if (event != null && event.getScheduleId() != null) {
            try {
                logger.info("Processing event for scheduleId: {}, ivrResponse: {}", event.getScheduleId(), event.getIvrResponse());
                boolean active = "TAKEN".equalsIgnoreCase(event.getIvrResponse()) ? false : true;
                scheduleService.updateScheduleStatus(event.getScheduleId(), active);
                logger.info("Successfully updated schedule {} to active={}", event.getScheduleId(), active);
            } catch (Exception e) {
                logger.error("Error processing CallResponseEvent for scheduleId: {}", event.getScheduleId(), e);
            }
        } else {
            logger.warn("Received null event or null scheduleId");
        }
    }
}
