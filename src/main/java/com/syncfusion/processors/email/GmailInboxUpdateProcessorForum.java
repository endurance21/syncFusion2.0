package com.syncfusion.processors.email;

import com.google.api.services.gmail.model.*;
import com.syncfusion.database.models.TJSGroup;
import com.syncfusion.dto.nobrokerNotification.EmailResponse;
import com.syncfusion.handlers.emailThreadHandlers.IEmailThreadHandler;
import com.syncfusion.services.GoogleService;
import com.syncfusion.services.GroupService;
import com.syncfusion.services.gmail.GmailClientProvider;
import com.syncfusion.services.notification.NobrokerNotificationService;
import com.syncfusion.services.notification.SyncfusionNotificationService;
import com.syncfusion.utils.UtilMethods;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.nobroker.tejas.utils.Constants.GmailConstants.EMAIL_SOURCE_IDENTIFIER_HEADER_KEY;
import static com.nobroker.tejas.utils.Constants.GmailConstants.EMAIL_SOURCE_IDENTIFIER_HEADER_VALUE;
import static com.nobroker.tejas.utils.Constants.RedisKeys.LAST_GMAIL_HISTORY_ID;

@Component
public class GmailInboxUpdateProcessorForum implements GmailInboxUpdateProcessor  {
    Logger log = LoggerFactory.getLogger(GmailInboxUpdateProcessorForum.class);
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private GoogleService googleService;
    @Autowired
    private UtilMethods utilMethods;
    @Autowired
    private GroupService groupService;

    @Autowired
    private GmailClientProvider gmailClientProvider;

    @Value("${tejas.gmail.history.process.replay.allowed:false}")
    private boolean isHistoryProcessReplayAllowed;
    @Autowired
    @Qualifier("GmailThreadHandler")
    private IEmailThreadHandler emailThreadHandler;
    @Autowired
    @Qualifier("SyncfusionNotificationService")
    private SyncfusionNotificationService notificationService;
    public  void processHistoryId(String historyId, String imposterEmail) {
        log.info("Received request to process historyId: {} for imposterEmail :{}", historyId, imposterEmail);
        if (!isValidImposterEmail(imposterEmail)) {
            log.info("[STOP XXX] Invalid imposterEmail: {} received, not processing further", imposterEmail);
            return;
        }
        long currentTime = System.currentTimeMillis();
        String lastProcessedHistoryId = getLastProcessedHistoryId(imposterEmail);

        String newLastProcessedHistoryId;
        String historyIdTobeProcessed;
        if (StringUtils.isBlank(lastProcessedHistoryId)) {
            historyIdTobeProcessed = historyId;
        } else {
            historyIdTobeProcessed = lastProcessedHistoryId;
        }


        if(isHistoryProcessReplayAllowed){
            historyIdTobeProcessed = historyId;
            log.info("history replaying is allowed, hence not checking redis keys", historyId);
        }
        if (!isHistoryProcessReplayAllowed && hasAlreadyProcessed(historyId, lastProcessedHistoryId)) {
            log.info("[STOP XXX] HistoryId: {} is already processed,  New HistoryId should be greater than equal to: {}, not processing further", historyId, lastProcessedHistoryId);
            return;
        }
        log.info("[OK] HistoryId: {} is not processed yet, so processing further", historyId);

        try {
            newLastProcessedHistoryId = _processHistoryId(historyIdTobeProcessed, imposterEmail);
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
        setNewLastProcessedHistoryId(newLastProcessedHistoryId, imposterEmail);
        log.info("[DONE] Processing historyId: {}, in : {} ms, for imposterEmail: {} ", historyId, System.currentTimeMillis() - currentTime, imposterEmail);

    }

    private String _processHistoryId(String historyId, String imposterEmail) throws IOException, GeneralSecurityException {
        List<History> history = gmailClientProvider.getGmailClientForServiceAccount().getHistory(historyId, imposterEmail);
        if (history == null || history.isEmpty()) {
            return historyId;
        }

        Collections.sort(history, Comparator.comparing(History::getId));
        History lastHistory = history.get(history.size() - 1);
        String newLastProcessedHistoryId = lastHistory.getId().toString();

        long totalGenuineMessageProcessed = 0;

        for (History historyItem : history) {
            log.info("[STARTING] to process historyItem: {}", historyItem.getId());
            totalGenuineMessageProcessed += processHistoryItem(historyItem, imposterEmail);
        }
        log.info("[COMPLETED] Processing of  historyId: {}, new Last processed historyId is: {}, total genuine emails processed: {}", historyId, newLastProcessedHistoryId, totalGenuineMessageProcessed);
        return newLastProcessedHistoryId;
    }

    private long processHistoryItem(History historyItem, String imposterEmail) throws GeneralSecurityException, IOException {
        List<HistoryMessageAdded> messages = historyItem.getMessagesAdded();
        if (messages == null) {
            log.info("[STOP XXX] No messages found in historyItem: {}", historyItem.getId());
            return 0;
        }
        long successfullProcessedMessages = 0;
        List<String> messageIds = messages.stream().map(HistoryMessageAdded::getMessage).map(Message::getId).collect(java.util.stream.Collectors.toList());
        LinkedHashSet<String> orderedMessageId = new LinkedHashSet<>();
        orderedMessageId.addAll(messageIds);

        log.info("Processing total  messages: {}, imposterEmail: {}", messageIds.size(), imposterEmail);
        List<Message> messagesToBeProcessed = gmailClientProvider.getGmailClientForServiceAccount().getFullyQualifiedMessageBatch(orderedMessageId);
        messagesToBeProcessed.sort(Comparator.comparing(Message::getInternalDate));

        log.info("Processing total  messages: {}, imposterEmail: {}", messagesToBeProcessed.size(), imposterEmail);
        for (Message message : messagesToBeProcessed) {
            successfullProcessedMessages += processMessageAdded(message, imposterEmail);
        }
        return successfullProcessedMessages;
    }

    private int processMessageAdded(Message fullMessage, String imposterEmail) {
        String id = fullMessage.getId();
        log.info("Processing message: {}, imposterEmail: {}", id, imposterEmail);
        try {
            if (!canProcessEmailMessage(fullMessage, imposterEmail)) {
                log.info("[STOP XXX] Either Email is not sent to a society group or User is not attached to this group hence not processing further ;) message id: {}", fullMessage.getId());
                return 0;
            }
            log.info("[OK] Email is sent by group moderation hence processing further :), message :{}", id);

            MessagePart actualWrappedMessage = getWrappedMessage(fullMessage);
            if (actualWrappedMessage == null) {
                log.info("No wrapped message found for message id: {}", fullMessage.getId());
                return 0;
            }
            String subject = utilMethods.extractSubjectFromMessage(fullMessage.getPayload());
            log.info("Received request for subject: {}", subject);
            if (isMessageSentByImposter(actualWrappedMessage)) {
                log.info("Email is sent by imposter  message id: {}", fullMessage.getId());
                handleMessageSentByImposter(fullMessage, imposterEmail);
                return 1;
            }
            log.info("Email is not sent by imposter hence processing further :),  message id: {}", fullMessage.getId());

            String fromEmail = utilMethods.extractFromHeaderFromMessage(actualWrappedMessage);
            String fromEmailAddress = utilMethods.extractEmail(fromEmail);
            String toEmail = utilMethods.extractToHeaderFromMessage(actualWrappedMessage);
            String toEmailAddress = utilMethods.extractEmail(toEmail);

            if (!isValidSocietyEmailAndSender(toEmailAddress, fromEmailAddress)) {
                log.info("[STOP XXX] Email is not sent by society email or sender is not valid hence not processing further :),  message id: {}", fullMessage.getId());
                return 0;
            }

            if (isNewThread(actualWrappedMessage)) {
                log.info("New thread found hence processing new thread");
                return processNewThread(actualWrappedMessage, imposterEmail);
            }
            return processExistingThread(actualWrappedMessage, imposterEmail);

        } catch (Exception e) {
            log.error("Error while fetching message: {}", id, e);
            return 0;
        }
    }

    private boolean isNewThread(MessagePart actualWrappedMessage) {
        String inReplyToHeader = utilMethods.extractInReplyToHeaderFromMessage(actualWrappedMessage);
        if (StringUtils.isBlank(inReplyToHeader)) {
            return true;
        }
        return false;
    }

    private void handleMessageSentByImposter(Message message, String imposterEmail) {
        log.info("Message sent by imposter hence, approving this message by sending a reply this thread");
        String subject = utilMethods.extractSubjectFromMessage(message.getPayload());
        String messageId = utilMethods.extractMessageIdFromMessage(message.getPayload());
        String fromEmail = utilMethods.extractFromHeaderFromMessage(message.getPayload());
        String fromEmailAddress = utilMethods.extractEmail(fromEmail);
        String approvedMessage = "Approved";


        try {
            log.info("Sending email to APPROVE imposter email : {}, subject: {}, messageId: {}, fromEmail: {}, approvedMessage: {}", null, subject, messageId, fromEmail, approvedMessage);
            EmailResponse response =  notificationService.sendEmailInThread(fromEmailAddress,"TEJAS", imposterEmail, subject, approvedMessage, messageId, imposterEmail, imposterEmail);
            log.info("Email sent successfully to APPROVE imposter email threadId: {}, subject: {}, messageId: {}, fromEmail: {}, approvedMessage: {}, providerMessageId: {}", null, subject, messageId, fromEmail, approvedMessage, response.getProviderMessageId());
        } catch (Exception e) {
            log.error("Error while sending email to threadId: {}, subject: {}, messageId: {}, fromEmail: {}, approvedMessage: {}", null, subject, messageId, fromEmail, approvedMessage, e);
        }

    }

    private MessagePart getWrappedMessage(Message message) {
        Optional<MessagePart> wrappedMessagePart = message.getPayload().getParts().stream().filter(part -> part.getMimeType().equalsIgnoreCase("message/rfc822")).findFirst();
        if(!wrappedMessagePart.isPresent()){
            throw  new RuntimeException(String.format("No wrapped message found for message id: %s ",message.getId()));
        }
        MessagePart messagePart  = wrappedMessagePart.get();
        if(messagePart.getParts()==null){
            log.info("No parts found so, might be .eml file");
            if(checkIfMessageIsEMLFile(messagePart)){
                 try {
                     messagePart = getMessageFromEmlFile(message.getId(), messagePart);
                     return messagePart;
                 }catch (Exception e){
                        log.error("Error while fetching message from eml file for message id: {}", message.getId(), e);
                        throw  new RuntimeException(String.format("Error while fetching message from eml file for message id: %s ",message.getId()));
                 }
            }else{
                log.error("message is not of correct format, message id: {}", message.getId());
            }
        }
        return messagePart.getParts().get(0);
    }

    private boolean checkIfMessageIsEMLFile(MessagePart messagePart) {
        if(StringUtils.isNotBlank(messagePart.getBody().getAttachmentId())){
            log.info("Message is EML file");
            return true;
        }else{
            return false;
        }

    }
    private MessagePart getMessageFromEmlFile(String messageId, MessagePart messagePart) throws IOException, MessagingException, jakarta.mail.MessagingException {
        String decodedEmlData  =  gmailClientProvider.getGmailClientForServiceAccount().getEmlAttachment(messageId, messagePart.getBody().getAttachmentId());
        return utilMethods.parseBase64Eml(decodedEmlData);
    }

    public boolean isValidImposterEmail(String imposterEmail) {
        if (StringUtils.isBlank(imposterEmail)) {
            return false;
        }
        return groupService.doesImposterEmailExists(imposterEmail);
    }

    public boolean isValidSocietyEmailAndSender(String groupEmail, String senderEmail) {
        log.info("Checking if groupEmail: {} and senderEmail: {} are valid", groupEmail, senderEmail);
        if (groupEmail == null || senderEmail == null) {
            return false;
        }
        return groupService.isMemberBelongsToSociety(groupEmail, senderEmail);
    }


    private boolean isMessageSentByImposter(MessagePart message) {
        String emailSourceHeader = getEmailSourceHeader(message);
        if (EMAIL_SOURCE_IDENTIFIER_HEADER_VALUE.equalsIgnoreCase(emailSourceHeader)) {
            return true;
        }
        return false;
    }

    private String getEmailSourceHeader(MessagePart message) {
        Optional<MessagePartHeader> messagePartHeader = message.getHeaders().stream().filter(header -> header.getName().equalsIgnoreCase(EMAIL_SOURCE_IDENTIFIER_HEADER_KEY)).findFirst();
        if (messagePartHeader.isPresent()) {
            return messagePartHeader.get().getValue();
        }
        return null;
    }

    private String prepareValidFromEmail(String societyEmail) {
        String modifiedEmail = insertTextBeforeAtSymbol(societyEmail, "+msgappr");
        return modifiedEmail;
    }

    private String insertTextBeforeAtSymbol(String email, String text) {
        String[] split = email.split("@");
        return split[0] + text + "@" + split[1];
    }

    private boolean acquireLock(String messageId) {
        Boolean isLockAcquired = redisTemplate.opsForValue().setIfAbsent(getAlreadyProcessedRedisKey(messageId), "locked");
        if (isLockAcquired != null && isLockAcquired) {
            redisTemplate.expire(getAlreadyProcessedRedisKey((messageId)), 10000, TimeUnit.MILLISECONDS);
        }
        return isLockAcquired != null && isLockAcquired;
    }

    private boolean canProcessEmailMessage(Message message, String imposterEmail) {
        String fromEmail = utilMethods.extractFromHeaderFromMessage(message.getPayload());
        String fromEmailAddress = utilMethods.extractEmail(fromEmail);
        String toEmail = utilMethods.extractToHeaderFromMessage(message.getPayload());
        String substringToRemove = "+msgappr";

        if (!fromEmailAddress.contains(substringToRemove)) {
            log.info("Email is not sent by Email Group moderation system hence not processing further ;) email: {}", message.getId());
            return false;
        }
        String extractedSocietyEmail = fromEmailAddress.replace(substringToRemove, "");
        String validImposterEmail = findImposterEmail(extractedSocietyEmail);

        TJSGroup tjsGroup = getTJSGroup(extractedSocietyEmail);
        if(tjsGroup == null) {
            log.info("No Group Record found for this  group email: {}", extractedSocietyEmail);
            return false;
        }
        if(Boolean.valueOf(tjsGroup.isActive()).equals(Boolean.FALSE)){
            log.info("Group is not active hence not processing further ;) email: {}", message.getId());
            return false;
        }
        log.info("Email is sent to: {}, from: {}, validImposter: {}, givenImposter :{} ", toEmail, fromEmail, validImposterEmail, imposterEmail);
        if (!imposterEmail.equalsIgnoreCase(validImposterEmail)) {
            log.info("Email is not sent by Email Group moderation system hence not processing further ;) email: {}", message.getId());
            return false;
        }
        if (acquireLock(message.getId())) {
            log.info("Lock acquired for message id: {}", message.getId());
            return true;
        } else {
            log.info("Lock not acquired for message id: {}", message.getId());
            return false;
        }
    }

    private String getAlreadyProcessedRedisKey(String messageId) {
        return "tejas.gmail.history.processed." + messageId;
    }
    private TJSGroup getTJSGroup(String extractedSocietyEmail) {
        return groupService.getGroupFromGroupEmail(extractedSocietyEmail);
    }

    private int processNewThread(MessagePart message, String imposterEmail) {
        String senderEmail = utilMethods.extractFromHeaderFromMessage(message);
        String senderEmailAddress = utilMethods.extractEmail(senderEmail);
        String senderName = utilMethods.extractNameFromEmail(senderEmail);
        String groupEmail = utilMethods.extractToHeaderFromMessage(message);
        String groupEmailAddress = utilMethods.extractAddressFromEmail(groupEmail);
        // this messageId smtp id and it is different from "id" of message which is id of message in gmail;
        String universalMessageId = utilMethods.extractMessageIdFromMessage(message);
        String emailText = utilMethods.findEmailBodyText(message);
        String emailSubject = utilMethods.extractSubjectFromMessage(message);
        if (StringUtils.isBlank(emailText)) {
            log.info("Email text is blank for subject: {}", emailSubject);
            return 0;
        }
        try {
            emailThreadHandler.handleNewThread(senderEmailAddress, senderName, groupEmailAddress, emailText, emailSubject, universalMessageId, imposterEmail);
            return 1;
        } catch (Exception e) {
            log.error("Error while processing new thread for messageId: {}", universalMessageId, e);
            return 0;
        }
    }

    private int processExistingThread(MessagePart message, String imposterEmail) {
        String senderEmail = utilMethods.extractFromHeaderFromMessage(message);
        String groupEmail = utilMethods.extractToHeaderFromMessage(message);
        String groupEmailAddress = utilMethods.extractAddressFromEmail(groupEmail);
        String universalMessageId = utilMethods.extractMessageIdFromMessage(message);
        String emailText = utilMethods.findEmailBodyText(message);
        String emailSubject = utilMethods.extractSubjectFromMessage(message);
        if (StringUtils.isBlank(emailText)) {
            log.info("Email text is blank for subject: {}", emailSubject);
            return 0;
        }
        String subjectWithoutRe = utilMethods.removeReFromSubject(emailSubject);
        String senderName = utilMethods.extractNameFromEmail(senderEmail);
        String inReplyTo = utilMethods.extractInReplyToHeaderFromMessage(message);
        try {
            emailThreadHandler.handleExistingThread(senderEmail, senderName, groupEmailAddress, emailText, subjectWithoutRe, universalMessageId, inReplyTo, imposterEmail);
            return 1;
        } catch (Exception e) {
            log.error("Error while processing existing thread for messageId: {}", universalMessageId, e);
            return 0;
        }
    }


    private String findEmailBodyText(MessagePartBody messagePartBody){
        if(messagePartBody == null){
            log.info("No messagePartBody found in message: {}", messagePartBody);
            return null;
        }
        String encodedBody = messagePartBody.getData();
        String emailText = utilMethods.decodeBase64(encodedBody);
        String emailHistorySanatized = utilMethods.removeEmailHistoryText(emailText);
        String emailSignatureSanatized = utilMethods.removeEmailSignature(emailHistorySanatized);
        return emailSignatureSanatized!=null ? emailSignatureSanatized.trim() : null;
    }

    private boolean hasAlreadyProcessed(String historyId, String lastProcessedHistoryId) {
        if (StringUtils.isBlank(lastProcessedHistoryId)) {
            return false;
        }
        return Long.parseLong(historyId) < Long.parseLong(lastProcessedHistoryId);
    }

    private String getLastProcessedHistoryId(String imposterEmail) {
        String key = getRedisForLastProcessedHistoryId(imposterEmail);
        return (String) redisTemplate.opsForValue().get(key);
    }

    private String getRedisForLastProcessedHistoryId(String imposterEmail) {
        return LAST_GMAIL_HISTORY_ID + imposterEmail;
    }

    private void setNewLastProcessedHistoryId(String newLastHistoryId, String imposterEmail) {
        String key = getRedisForLastProcessedHistoryId(imposterEmail);
        redisTemplate.opsForValue().set(key, newLastHistoryId);
    }

    private String findImposterEmail(String extractedSocietyEmail) {
        TJSGroup googleGroup = groupService.getGroupFromGroupEmail(extractedSocietyEmail);
        if (googleGroup == null) {
            throw new RuntimeException("No google group found for societyEmail: " + extractedSocietyEmail);
        }
        return googleGroup.getImposterEmail();
    }
}
