package com.syncfusion.services.notification;

import com.syncfusion.dto.notification.EmailResponse;

import javax.mail.MessagingException;
import java.io.IOException;

public interface INotificationService {
    public EmailResponse sendEmail(String to, String fromName, String fromEmailAddress, String subject, String body, String replyToEmailAddress, String imposterEmail) throws MessagingException, IOException;

    public EmailResponse sendEmailInThread(String to, String fromName, String fromEmailAddress, String subject, String body, String inReplyToMessageId, String imposterEmail, String replyToEmailAddress) throws MessagingException, IOException;
}
