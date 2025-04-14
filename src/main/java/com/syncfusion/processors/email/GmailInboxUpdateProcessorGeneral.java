package com.syncfusion.processors.email;

import com.google.api.services.gmail.model.History;
import com.google.api.services.gmail.model.HistoryMessageAdded;
import com.google.api.services.gmail.model.Message;
import com.syncfusion.database.models.TJSGmailAccount;
import com.syncfusion.dto.gmail.TJSAttachment;
import com.syncfusion.dto.uniview.TJSEmailContent;
import com.syncfusion.services.TJSGmailAccountService;
import com.syncfusion.services.gmail.GmailClientProvider;
import com.syncfusion.uniview.UniviewApiClient;
import com.syncfusion.utils.UtilMethods;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

import static com.nobroker.tejas.utils.Constants.RedisKeys.LAST_GMAIL_HISTORY_ID;

@Component
public class GmailInboxUpdateProcessorGeneral implements GmailInboxUpdateProcessor {
    Logger log = LoggerFactory.getLogger(GmailInboxUpdateProcessorGeneral.class);
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private GmailClientProvider gmailClientProvider;
    @Autowired
    private TJSGmailAccountService gmailAccountService;

    @Autowired
    private UtilMethods utilMethods;

    @Autowired
    private UniviewApiClient univiewApiClient;
    @Value("${tejas.gmail.history.process.replay.allowed:false}")
    private boolean isHistoryProcessReplayAllowed;

    public void processHistoryId(String historyId, String email) {
        log.info("Received request to process historyId: {} for email :{}", historyId, email);
        if (!isValidEmail(email)) {
            log.info("[STOP XXX] Invalid request, for email : {} received, not processing further", email);
            return;
        }
        long currentTime = System.currentTimeMillis();
        String lastProcessedHistoryId = getLastProcessedHistoryId(email);

        String newLastProcessedHistoryId;
        String historyIdTobeProcessed;
        if (StringUtils.isBlank(lastProcessedHistoryId)) {
            historyIdTobeProcessed = historyId;
        } else {
            historyIdTobeProcessed = lastProcessedHistoryId;
        }
        if (isHistoryProcessReplayAllowed) {
            historyIdTobeProcessed = historyId;
            log.info("history replaying is allowed, hence not checking redis keys", historyId);
        }
        if (!isHistoryProcessReplayAllowed && hasAlreadyProcessed(historyId, lastProcessedHistoryId)) {
            log.info("[STOP XXX] HistoryId: {} is already processed,  New HistoryId should be greater than equal to: {}, not processing further", historyId, lastProcessedHistoryId);
            return;
        }
        log.info("[OK] HistoryId: {} is not processed yet, so processing further", historyId);

        try {
            newLastProcessedHistoryId = _processHistoryId(historyIdTobeProcessed, email);
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
        setNewLastProcessedHistoryId(newLastProcessedHistoryId, email);
        log.info("[DONE] Processing historyId: {}, in : {} ms, for imposterEmail: {} ", historyId, System.currentTimeMillis() - currentTime, email);

    }

    private String _processHistoryId(String historyId, String email) throws IOException, GeneralSecurityException {
        List<History> history = gmailClientProvider.getGmailClient(email).getHistory(historyId, email);
        if (history == null || history.isEmpty()) {
            return historyId;
        }

        Collections.sort(history, Comparator.comparing(History::getId));
        History lastHistory = history.get(history.size() - 1);
        String newLastProcessedHistoryId = lastHistory.getId().toString();

        long totalGenuineMessageProcessed = 0;

        for (History historyItem : history) {
            log.info("[STARTING] to process historyItem: {}", historyItem.getId());
            totalGenuineMessageProcessed += processHistoryItem(historyItem, email);
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
        LinkedHashSet<String> orderedMessageIds = new LinkedHashSet<>();
        orderedMessageIds.addAll(messageIds);

        log.info("Processing total  messages: {}, imposterEmail: {}", messageIds.size(), imposterEmail);
        List<Message> messagesToBeProcessed = gmailClientProvider.getGmailClient(imposterEmail).getFullyQualifiedMessageBatch(orderedMessageIds);
        messagesToBeProcessed.sort(Comparator.comparing(Message::getInternalDate));
        log.info("Processing total  messages: {}, imposterEmail: {}", messagesToBeProcessed.size(), imposterEmail);
        for (Message message : messagesToBeProcessed) {
            successfullProcessedMessages += processMessageAdded(message, imposterEmail);
        }
        return successfullProcessedMessages;
    }

    private int processMessageAdded(Message message, String imposterEmail) throws GeneralSecurityException, IOException {
        log.info("Processing message: {}, imposterEmail: {}", message.getId(), imposterEmail);
        List<TJSAttachment> attachments = gmailClientProvider.getGmailClient(imposterEmail).extractAttachment(message);
        String subject = utilMethods.extractSubjectFromMessage(message.getPayload());
        String inReplyTo = utilMethods.extractInReplyToHeaderFromMessage(message.getPayload());
        String universalMessageId = utilMethods.extractMessageIdFromMessage(message.getPayload());
        String from = utilMethods.extractFromHeaderFromMessage(message.getPayload());
        String to = utilMethods.extractToHeaderFromMessage(message.getPayload());
        String plainText = utilMethods.findEmailBodyText(message.getPayload());
        String htmlText = utilMethods.findEmailBodyHtmlText(message.getPayload());
        List<String> CC = utilMethods.extractCCHeader(message.getPayload());
        String fromName = utilMethods.extractNameFromEmail(from);
        String fromEmailAddress = utilMethods.extractAddressFromEmail(from);
        TJSEmailContent.Sender sender = TJSEmailContent.Sender.builder().name(fromName).emailAddress(fromEmailAddress).build();
        String toEmails[] = to.split(",");
        List<TJSEmailContent.Recipient> toRecepients = new ArrayList<>();
        for (String toEmail : toEmails) {
            String name = utilMethods.extractNameFromEmail(toEmail);
            String address = utilMethods.extractAddressFromEmail(toEmail);
            TJSEmailContent.Recipient recipient = TJSEmailContent.Recipient.builder().name(name).emailAddress(address).type(TJSEmailContent.Recipient.Type.TO).build();
            toRecepients.add(recipient);
        }
        List<TJSEmailContent.Recipient> ccRecepients = new ArrayList<>();
        for (String cc : CC) {
            String name = utilMethods.extractNameFromEmail(cc);
            String address = utilMethods.extractAddressFromEmail(cc);

            TJSEmailContent.Recipient ccRecipient = TJSEmailContent.Recipient.builder().name(name).emailAddress(address).type(TJSEmailContent.Recipient.Type.CC).build();
            ccRecepients.add(ccRecipient);
        }
        TJSEmailContent emailContent = TJSEmailContent.builder()
                .subject(subject)
                .inReplyToMessageId(inReplyTo)
                .from(sender)
                .cc(ccRecepients)
                .to(toRecepients)
                .attachments(attachments)
                .build();
        if (StringUtils.isNotBlank(inReplyTo)) {
            log.info("This is reply to existing thread, notiying for reply to thread, subject: {}, plainText: {}, htmlBody: {}, CC: {}", subject, plainText, htmlText, CC.toString());
        } else if (StringUtils.isNotBlank(universalMessageId)) {
            log.info("New thread found for universalMessageId: {}, subject: {}, plainText: {}, htmlText: {}", universalMessageId, subject, plainText, htmlText);
        } else {
            log.error("Neither inReplyTo nor universalMessageId found, subject: {}, body: {}", subject, plainText);
        }
        try {
            univiewApiClient.notifyForIncomingEmail(emailContent);
            return 1;
        } catch (Exception e) {
            log.error("Error while calling uniview api to notify for new thread", e);
            return 0;
        }finally {
            log.info("Processed message for imposterEmail: {}, messageId: {}, subject: {}, from: {}, to: {}, plainText: {}, attachments: {}", imposterEmail, universalMessageId, subject, from, to, plainText, attachments.toString());
        }


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

    public boolean isValidEmail(String email) {
        if (StringUtils.isBlank(email)) {
            return false;
        }
        TJSGmailAccount gmailAccount = gmailAccountService.findById(email).orElse(null);
        if (gmailAccount == null) {
            return false;
        }
        if (!gmailAccount.isActive()) {
            log.info("GmailCredential: {} is not active, hence not processing further", email);
            return false;
        }
        return true;
    }

}
