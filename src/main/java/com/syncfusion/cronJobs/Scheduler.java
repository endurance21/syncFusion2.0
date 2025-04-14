package com.syncfusion.cronJobs;

import com.syncfusion.services.GoogleService;
import com.syncfusion.services.gmail.GmailClientProvider;
import com.syncfusion.services.notification.INotificationService;
import com.syncfusion.utils.UtilMethods;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Service
public class Scheduler {
    Logger logger = org.slf4j.LoggerFactory.getLogger(Scheduler.class);
    @Autowired
    private UtilMethods utilMethods;
    @Autowired
    private GoogleService googleService;

    @Autowired
    @Qualifier("SyncfusionNotificationService")
    private INotificationService notificationService;

    @Autowired
    private GmailClientProvider gmailClientProvider;

    @Scheduled(cron = "0 0 12 * * ?") // Run at 12 PM daily
    public void subscribeToGmailInboxUpdates() {
        logger.info("Scheduler started : subscribeToGmailInboxUpdates");

        try {
            logger.info("Subscribing to Gmail Inbox Updates ");
            subscribeForAllGmailClients();
        }catch (Exception e){
            logger.error("Error while subscribing to Gmail Inbox Updates for all service account emails");
            alertAdmins(e.getMessage());
            throw new RuntimeException(e);
        }
        logger.info("Scheduler ended : subscribeToGmailInboxUpdates");
    }
    private void subscribeForAllGmailClients() throws GeneralSecurityException, IOException {
        gmailClientProvider.subscribeToInboxUpdatesAll();
    }

    private void alertAdmins(String errorMessage){
        logger.info("Sending alert to admins : {}", errorMessage);
        String imposterEmail = utilMethods.getImposterEmail();
        List<String> adminEmails = utilMethods.getAdminEmails();

        StringBuilder  messageBuilder = new StringBuilder();
        messageBuilder.append("Hi Admins,<br><br>");
        messageBuilder.append("There was an error while subscribing to Gmail Inbox Updates for imposter email : ");
        messageBuilder.append(imposterEmail);
        messageBuilder.append("<br><br>");
        messageBuilder.append("<h1>Environment Properties </h1>");
        messageBuilder.append("Active profile : ");
        messageBuilder.append(utilMethods.getActiveProfile());
        messageBuilder.append("<br><br>");
        messageBuilder.append("Please check the logs for more details.<br><br>");
        messageBuilder.append("Error message : ");
        messageBuilder.append(errorMessage);

        String message = messageBuilder.toString();

        try {
            for(String adminEmail : adminEmails) {
               String messageId =  notificationService.sendEmail(adminEmail, "Syncfusion", imposterEmail, "[SYNCFUSION ALERT !] Error while subscribing to Gmail Inbox Updates", message, imposterEmail, imposterEmail).getProviderMessageId();
                logger.info("Alert sent to admin : {}, with provider messageID: {}", adminEmail, messageId);
            }
        } catch (Exception e) {
            logger.error("Error while sending alert to admins", e);
        }
    }
}
