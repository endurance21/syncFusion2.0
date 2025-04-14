package com.syncfusion.handlers.emailThreadHandlers;

public interface IEmailThreadHandler {
    void handleNewThread(String senderEmail, String senderName, String groupEmail, String emailContent, String subject, String universalMessageId, String imposterEmail);
    void handleExistingThread(String senderEmail, String senderName, String groupEmail, String emailContent, String subject, String universalMessageId, String inReplyToUniversalMessageId, String imposterEmail);
}
