package com.syncfusion.services.notification;

import com.google.api.services.gmail.model.Message;
import com.syncfusion.dto.nobrokerNotification.EmailResponse;
import com.syncfusion.services.gmail.GmailClientProvider;
import com.syncfusion.utils.UtilMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import java.io.IOException;

@Service
@Qualifier("GmailNotificationService")
public class GmailNotificationService implements INotificationService {

    Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private UtilMethods utilMethods;

    @Autowired
    private GmailClientProvider  gmailClientProvider;


    @Override
    public EmailResponse sendEmail(String to, String fromName, String fromEmailAddress, String subject, String body, String replyToEmailAddress, String imposterEmail) throws MessagingException, IOException {
        Message message = gmailClientProvider.getGmailClientForServiceAccount().sendEmail(to, fromName, subject, body, "text/html", imposterEmail, fromEmailAddress, replyToEmailAddress);
        String gmailMessageId = message.getId();
        Message fullMessage;
        // fetch whole message content from gmail server
        try {
            fullMessage = gmailClientProvider.getGmailClientForServiceAccount().getFullyQualifiedMessage(gmailMessageId, imposterEmail);
        } catch (IOException e) {
            log.error("Error while fetching message from gmail server for message id: {}", gmailMessageId, e);
            throw new RuntimeException(e);
        }
        String universalMessageId = utilMethods.extractMessageIdFromMessage(fullMessage.getPayload());

        return EmailResponse.builder().universalMessageId(universalMessageId).build();

    }

    @Override
    public EmailResponse sendEmailInThread(String to, String fromName, String fromEmailAddress, String subject, String body, String inReplyToMessageId, String imposterEmail, String replyToEmailAddress) throws MessagingException, IOException {
        Message message =  gmailClientProvider.getGmailClientForServiceAccount().sendEmailInThread(to, inReplyToMessageId, fromName, subject, body, "text/html", imposterEmail,  fromEmailAddress, replyToEmailAddress);
        String gmailMessageId = message.getId();
        Message fullMessage;
        // fetch whole message content from gmail server
        try {
            fullMessage = gmailClientProvider.getGmailClientForServiceAccount().getFullyQualifiedMessage(gmailMessageId, imposterEmail);
        } catch (IOException e) {
            log.error("Error while fetching message from gmail server for message id: {}", gmailMessageId, e);
            throw new RuntimeException(e);
        }
        String universalMessageId = utilMethods.extractMessageIdFromMessage(fullMessage.getPayload());

        return EmailResponse.builder().universalMessageId(universalMessageId).build();
    }
}
