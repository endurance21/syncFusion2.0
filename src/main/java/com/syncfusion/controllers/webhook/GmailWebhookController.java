package com.syncfusion.controllers.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncfusion.dto.gmail.GmailDTO;
import com.syncfusion.kafka.EmailUpdateMessage;
import com.syncfusion.processors.email.GmailInboxUpdateProcessorForum;
import com.syncfusion.services.KafkaService;
import com.syncfusion.utils.Constants;
import com.syncfusion.utils.UtilMethods;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook/gmail")
public class GmailWebhookController {
    Logger logger = org.slf4j.LoggerFactory.getLogger(GmailWebhookController.class);
    @Autowired
    private GmailInboxUpdateProcessorForum inboxProcessor;

    @Autowired
    private UtilMethods utilMethods;

    @Autowired
    private KafkaService kafkaService;

    @PostMapping("/inbox/update")
    public ResponseEntity<String> handleInboxUpdate(@RequestBody GmailDTO.GmailPayload payload) throws JsonProcessingException {
        String emailAddress = payload.getEmailAddress();
        String historyId = payload.getHistoryId();
        EmailUpdateMessage emailUpdateMessage = new EmailUpdateMessage(emailAddress, historyId);
        String message = new ObjectMapper().writeValueAsString(emailUpdateMessage);
        // produce with email address as key so that all messages for a particular email address are processed in order
        logger.info("Producing message for email: {} with historyId: {}", emailAddress, historyId);
        String imposterEmail = utilMethods.getImposterEmail();
        if (imposterEmail != null && imposterEmail.equals(emailAddress)) {
            logger.info("Imposter email detected, producing for service account topic");
            kafkaService.produceMessage(Constants.KAFKA_TOPIC_FOR_INBOX_UPDATE_SERVICE_ACCOUNT, emailAddress, message);
        }else{
            kafkaService.produceMessage(Constants.KAFKA_TOPIC_FOR_INBOX_UPDATE, emailAddress, message);
        }
        return new ResponseEntity<>("Request Processed successfully ", HttpStatus.OK);
    }
    

}
