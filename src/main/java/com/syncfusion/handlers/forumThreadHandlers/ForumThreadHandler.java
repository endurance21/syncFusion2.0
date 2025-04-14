package com.syncfusion.handlers.forumThreadHandlers;

import com.syncfusion.brahmos.BrahmosClient;
import com.syncfusion.database.models.TJSGroup;
import com.syncfusion.database.models.TJSMessage;
import com.syncfusion.dto.brahmos.request.inBound.NewForumNestedReplyRequest;
import com.syncfusion.dto.brahmos.request.inBound.NewForumPostRequest;
import com.syncfusion.dto.brahmos.request.inBound.NewForumReplyRequest;
import com.syncfusion.dto.brahmos.response.outBound.ForumReplyResponse;
import com.syncfusion.dto.hood.HoodResponseDTO;
import com.syncfusion.dto.nobrokerNotification.EmailResponse;
import com.syncfusion.services.*;
import com.syncfusion.services.notification.SyncfusionNotificationService;
import com.syncfusion.utils.UtilMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ForumThreadHandler {

    Logger log = LoggerFactory.getLogger(ForumThreadHandler.class);
    @Autowired
    private GoogleService googleService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private UtilMethods utilMethods;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private HoodService hoodService;

    @Autowired
    private BrahmosClient brahmosClient;

    @Autowired
    @Qualifier("SyncfusionNotificationService")
    private SyncfusionNotificationService notificationService;

    public void handleNewPostCreation(NewForumPostRequest newForumPostRequest) {
        String forumPostId = newForumPostRequest.getForumPostId();
        String societyId = newForumPostRequest.getSocietyId();
        String subject = newForumPostRequest.getTitle();
        String emailBody = newForumPostRequest.getDescription();
        String authorName = newForumPostRequest.getAuthorName();
        String userId = newForumPostRequest.getUserId();

        TJSGroup googleGroup = groupService.getGroupFromSocietyId(societyId);
        String societyGroupEmail = googleGroup.getSocietyGroupEmail();

        if (googleGroup == null) {
            log.error("No google group found for group email: {}", societyGroupEmail);
            throw new RuntimeException("No google group found for group email: " + societyGroupEmail);
        }
        if(!googleGroup.isActive()){
            log.info("Google group is not active for group email: {}", societyGroupEmail);
            throw new RuntimeException("Google group is not active for group email: " + societyGroupEmail);
        }
        String imposterEmail = googleGroup.getImposterEmail();
        String emailBodyHtml;
        String senderDesignation = hoodService.getAuthorDesignation(societyId, userId);
        String createdOn = newForumPostRequest.getCreatedOn();
        try {
            emailBodyHtml = templateService.getHTMLBodyForBasePost(emailBody, subject,  authorName, senderDesignation, createdOn, societyId, forumPostId, googleGroup.getSocietyGroupEmail());
        } catch (IOException e) {
            log.error("Error while preparing email body html for new post creation, request: {}", newForumPostRequest.toString());
            throw new RuntimeException(e);
        }

        String sentUniversalMessageId;
        String providerMessageId;
        try {
            log.info("Sending email for new post creation, request: {}", newForumPostRequest.toString());
            EmailResponse emailResponse = notificationService.sendEmail(societyGroupEmail, authorName, societyGroupEmail, subject, emailBodyHtml, societyGroupEmail, imposterEmail);
            sentUniversalMessageId = emailResponse.getUniversalMessageId();
            providerMessageId = emailResponse.getProviderMessageId();
            log.info("Email sent successfully for new post creation, request: {}", newForumPostRequest.toString());
        } catch (Exception e) {
            log.error("Error while sending email for new post creation", e);
            throw new RuntimeException("Error while sending email for new post creation");
        }

        messageService.addNewMessage(forumPostId, providerMessageId, sentUniversalMessageId, null, true, false, societyId);
    }

    public void handleNewReplyCreation(NewForumReplyRequest newForumReplyRequest) {
        String forumPostId = newForumReplyRequest.getForumPostId();
        String societyId = newForumReplyRequest.getSocietyId();
        String subject = newForumReplyRequest.getTitle();
        String commentId = newForumReplyRequest.getReplyId();

        TJSGroup googleGroup = groupService.getGroupFromSocietyId(societyId);
        String societyGroupEmail = googleGroup.getSocietyGroupEmail();
        if (googleGroup == null) {
            log.info("No google group found for group email: {}", societyGroupEmail);
            throw new RuntimeException("No google group found for group email: " + societyGroupEmail);
        }
        if(!googleGroup.isActive()){
            log.info("Google group is not active for group email: {}", societyGroupEmail);
            throw new RuntimeException("Google group is not active for group email: " + societyGroupEmail);
        }

        String imposterEmail = googleGroup.getImposterEmail();

        boolean isForumExists = messageService.isForumPostExists(forumPostId);
        if (!isForumExists) {
            log.error("No forum post found for forum post id: {}", forumPostId);
            throw new RuntimeException("No forum post found for forum post id: " + forumPostId);
        }
        TJSMessage baseEmailMessage;
        try {
            baseEmailMessage = findBaseMessage(forumPostId);
        } catch (Exception e) {
            log.error("Error while finding base message id for thread id: {}", forumPostId, e);
            throw new RuntimeException(e);
        }

        String currentSenderText = newForumReplyRequest.getReplyText();
        String inReplyToMessageId = baseEmailMessage.getUniversalMessageId();
        String currentSenderFbId = newForumReplyRequest.getSenderId();
        String sentDate = newForumReplyRequest.getCreatedOn();

        HoodResponseDTO.ResponseData hoodUser = hoodService.getUserDetails(currentSenderFbId, societyId);
        String currentSenderName = hoodUser.getName();
        String currentSenderDesignation = hoodService.getAuthorDesignation(hoodUser);

        String newHTMLContent;

        try {
            newHTMLContent = templateService.getHTMLBodyForReply(currentSenderText, currentSenderName, currentSenderDesignation, sentDate, societyId, forumPostId, commentId, societyGroupEmail);
        } catch (IOException e) {
            log.error("Error while preparing email body html for new post creation, request: {}", newForumReplyRequest.toString());
            throw new RuntimeException(e);
        }

        String sentUniversalMessageId;
        String providerMessageId;
        try {
            log.info("Sending email for new reply creation to base email , request: {}", newForumReplyRequest.toString());
            EmailResponse emailResponse = notificationService.sendEmailInThread(societyGroupEmail, currentSenderName, societyGroupEmail, subject, newHTMLContent, inReplyToMessageId, imposterEmail, societyGroupEmail);
            sentUniversalMessageId = emailResponse.getUniversalMessageId();
            providerMessageId = emailResponse.getProviderMessageId();
            log.info("Email sent successfully for new reply to base email creation");
        } catch (Exception e) {
            log.error("Error while sending email for new reply creation", e);
            throw new RuntimeException(e);
        }
        messageService.addNewMessage(forumPostId, providerMessageId, sentUniversalMessageId, commentId, false, false, societyId);
    }

    public void handleNestedReplyCreation(NewForumNestedReplyRequest newForumNestedReplyRequest) {
        log.info("Handling new nested reply creation, request: {}", newForumNestedReplyRequest.toString());
        String forumPostId = newForumNestedReplyRequest.getForumPostId();
        String societyId = newForumNestedReplyRequest.getSocietyId();
        String subject = newForumNestedReplyRequest.getTitle();
        String commentId = newForumNestedReplyRequest.getReplyId();
        String senderFbUserId = newForumNestedReplyRequest.getSenderId();

        TJSGroup googleGroup = groupService.getGroupFromSocietyId(societyId);
        String societyGroupEmail = googleGroup.getSocietyGroupEmail();

        if (googleGroup == null) {
            log.error("No google group found for group email: {}", societyGroupEmail);
            throw new RuntimeException("No google group found for group email: " + societyGroupEmail);
        }
        if(!googleGroup.isActive()){
            log.info("Google group is not active for group email: {}", societyGroupEmail);
            throw new RuntimeException("Google group is not active for group email: " + societyGroupEmail);
        }
        boolean isForumExists = messageService.isForumPostExists(forumPostId);
        if (!isForumExists) {
            log.error("No forum post found for forum post id: {}", forumPostId);
            throw new RuntimeException("No forum post found for forum post id: " + forumPostId);
        }

        String imposterEmail = googleGroup.getImposterEmail();

        String parentCommentId = newForumNestedReplyRequest.getParentReplyId();
        TJSMessage parentMessage;
        try {
            parentMessage = findParentMessage(parentCommentId);
        } catch (Exception e) {
            log.error("Error while finding base message id for thread id: {}", forumPostId, e);
            throw new RuntimeException(e);
        }
        if (parentMessage == null) {
            log.error("No parent message found for parent comment id: {}", parentCommentId);
            throw new RuntimeException("No parent message found for parent comment id: " + parentCommentId);
        }
        String currentSenderText = newForumNestedReplyRequest.getReplyText();
        String sentDate = newForumNestedReplyRequest.getCreatedOn();
        HoodResponseDTO.ResponseData currentHoodUser = hoodService.getUserDetails(senderFbUserId, societyId);
        String currentSenderDesignation = hoodService.getAuthorDesignation(currentHoodUser);
        String currentSenderName = currentHoodUser.getName();


        String parentReplyId = newForumNestedReplyRequest.getParentReplyId();

        ForumReplyResponse.ForumReply parentForumReply = brahmosClient.getReplyMessage(societyId, forumPostId, parentReplyId);
        String parentForumReplyText = parentForumReply.getReplyText();
        String parentForumReplyAuthorName = parentForumReply.getSenderName();
        String parentAuthorFbUserId = parentForumReply.getSenderId();
        String parentSentDate = parentForumReply.getCreatedOn();
        String parentAuthorDesignation = hoodService.getAuthorDesignation(societyId, parentAuthorFbUserId);


        String newHTMLContent;
        try {
            newHTMLContent = templateService.getHTMLBodyForNestedReply(currentSenderText, currentSenderName, currentSenderDesignation, sentDate, parentForumReplyAuthorName, parentForumReplyText, parentAuthorDesignation, parentSentDate, societyId, forumPostId, commentId, societyGroupEmail);
        } catch (IOException e) {
            log.error("Error while fetching email html content from templateService for currentSenderText: {}, currentSenderName: {}, parentPostSenderName: {}, parentPostText: {}", currentSenderText, currentSenderName, parentForumReplyAuthorName, parentForumReplyText, e);
            throw new RuntimeException(e);
        }

        String inReplyToMessageId = parentMessage.getUniversalMessageId();
        String sentUniversalMessageId;
        String providerMessageId;
        try {
            log.info("Sending email for new reply creation to base email , request: {}", newForumNestedReplyRequest.toString());
            EmailResponse emailResponse = notificationService.sendEmailInThread(societyGroupEmail, currentSenderName, societyGroupEmail, subject, newHTMLContent, inReplyToMessageId, imposterEmail, societyGroupEmail);
            sentUniversalMessageId = emailResponse.getUniversalMessageId();
            providerMessageId = emailResponse.getProviderMessageId();
            log.info("Email sent successfully for new reply to base email creation");
        } catch (Exception e) {
            log.error("Error while sending email for new reply creation", e);
            throw new RuntimeException(e);
        }
        messageService.addNewMessage(forumPostId, providerMessageId, sentUniversalMessageId, commentId, false, false, societyId);
    }

    private TJSMessage findBaseMessage(String forumPostId) {
        TJSMessage baseMessage = messageService.findBaseMessageByForumPostId(forumPostId);
        return baseMessage;
    }

    private TJSMessage findParentMessage(String parentCommentId) {
        TJSMessage baseMessage = messageService.findByCommentId(parentCommentId);
        return baseMessage;
    }
}
