package com.syncfusion.services;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncfusion.kafka.EmailUpdateMessage;
import com.syncfusion.kafka.KafkaCommonConfig;
import com.syncfusion.processors.email.GmailInboxUpdateProcessorForum;
import com.syncfusion.processors.email.GmailInboxUpdateProcessorGeneral;
import com.syncfusion.utils.UtilMethods;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static com.syncfusion.utils.Constants.*;

@Service("kafkaService")
public class KafkaService {
    private static Logger logger = org.slf4j.LoggerFactory.getLogger(KafkaService.class);

    @Autowired
    private KafkaCommonConfig kafkaCommonConfig;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private UtilMethods utilsMethod;


    @Autowired
    private GmailInboxUpdateProcessorGeneral gmailInboxUpdateProcessorGeneral;
    @Autowired
    private GmailInboxUpdateProcessorForum gmailInboxUpdateProcessorForum;

    public void produceMessage(String topicName, String msg) {
        kafkaTemplate.send(topicName, msg);
    }

    public void produceMessage(String topicName, String key, String msg) {
        kafkaTemplate.send(topicName, key, msg);
    }

    @KafkaListener(topics = KAFKA_TOPIC_FOR_INBOX_UPDATE, groupId = KAFKA_GROUP_ID)
    public void consumeInboxUpdateRecord(String message) {
        logger.info("Received Message in group foo: " + message);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            EmailUpdateMessage updateMessage = objectMapper.readValue(message, EmailUpdateMessage.class);
            handleMessage(updateMessage);
        } catch (Exception e) {
            logger.error("Error in parsing message: " + message, e);
        }
    }
    @KafkaListener(topics = KAFKA_TOPIC_FOR_INBOX_UPDATE_SERVICE_ACCOUNT, groupId = KAFKA_GROUP_ID)
    public void consumeInboxUpdateRecordForServiceAccount(String message) {
        logger.info("Received Message in group foo: " + message);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            EmailUpdateMessage updateMessage = objectMapper.readValue(message, EmailUpdateMessage.class);
            handleMessageForServiceAccount(updateMessage);
        } catch (Exception e) {
            logger.error("Error in parsing message: " + message, e);
        }
    }

    private void handleMessageForServiceAccount(EmailUpdateMessage message) {
        String emailAddress = message.getEmailAddress();
        String historyId = message.getHistoryId();

        if (emailAddress == null || historyId == null) {
            logger.error("Invalid message received: " + message);
            return;
        }
        String imposterEmail = utilsMethod.getImposterEmail();

        if(imposterEmail !=null && imposterEmail.equals(emailAddress)) {
            logger.info("Imposter email detected, processing for service account");
            gmailInboxUpdateProcessorForum.processHistoryId(historyId, emailAddress);
        }else{
            logger.error("Unknown imposter email found : {}", emailAddress);
        }
    }
    private void handleMessage(EmailUpdateMessage message) {
        logger.info("Received Message in group foo: " + message);

        String emailAddress = message.getEmailAddress();
        String historyId = message.getHistoryId();

        if (emailAddress == null || historyId == null) {
            logger.error("Invalid message received: " + message);
            return;
        }
        gmailInboxUpdateProcessorGeneral.processHistoryId(historyId, emailAddress);
    }
}


