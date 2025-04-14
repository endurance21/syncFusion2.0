package com.syncfusion.handlers.emailThreadHandlers;

import com.syncfusion.brahmos.BrahmosClient;
import com.syncfusion.database.models.TJSGroup;
import com.syncfusion.database.models.TJSGroupMember;
import com.syncfusion.database.models.TJSMessage;
import com.syncfusion.dto.brahmos.response.outBound.ForumReplyResponse;
import com.syncfusion.dto.hood.HoodResponseDTO;
import com.syncfusion.dto.nobrokerNotification.EmailResponse;
import com.syncfusion.services.*;
import com.syncfusion.services.notification.SyncfusionNotificationService;
import com.syncfusion.utils.UtilMethods;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Qualifier("GmailThreadHandler")
public class GmailThreadHandler implements IEmailThreadHandler {
    Logger log = LoggerFactory.getLogger(GmailThreadHandler.class);
    @Autowired
    private UtilMethods utilMethods;

    @Autowired
    private GroupService groupService;

    @Autowired
    private GoogleService googleService;

    @Autowired
    private ForumNotifierService forumNotifierService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private HoodService hoodService;

    @Autowired
    private BrahmosClient brahmosClient;

    @Autowired
    private GroupMemberService groupMemberService;

    @Autowired
    @Qualifier("SyncfusionNotificationService")
    private SyncfusionNotificationService notificationService;

    /**
     * This method handles the new thread. It prepares a new email that will be sent to the google group based on the message recieved by sender member.
     * This results, in two messages apperaring in sender's inbox, one sent by sender and other sent by google group.
     *
     * @param senderEmail
     * @param senderName
     * @param groupEmail
     * @param emailText
     * @param subject
     * @param universalMessageId
     * @param imposterEmail
     */
    @Override
    public void handleNewThread(String senderEmail, String senderName, String groupEmail, String emailText, String subject, String universalMessageId, String imposterEmail) {
        log.info("Handling new  thread with, senderEmail: {}, groupEmail: {}, emailText: {}, universalMessageId: {}", senderEmail, groupEmail, emailText, universalMessageId);
        String emailHTMLContent;
        String sentUniversalMessageId;
        TJSGroup googleGroup = groupService.getGroupFromGroupEmail(groupEmail);
        String senderEmailAddress = utilMethods.extractEmail(senderEmail);
        TJSGroupMember googleGroupMember = groupMemberService.getTJSGroupFromMemberEmailAndSocietyId(googleGroup.getSocietyId(), senderEmailAddress);
        String senderFbUserId = googleGroupMember.getFirebaseUserId();
        String createdForumId;
        String providerMessageId;
        try {
            createdForumId = forumNotifierService.notifyForNewThread(googleGroup.getSocietyId(), senderFbUserId, emailText, subject);
        } catch (Exception e) {
            log.error("Error while notifying BRAHMOS for new thread", e);
            throw new RuntimeException(e);
        }

        HoodResponseDTO.ResponseData hoodUser = hoodService.getUserDetails(googleGroupMember.getFirebaseUserId(), googleGroupMember.getSocietyId());
        if(!StringUtils.isBlank(hoodUser.getName())){
            senderName = hoodUser.getName();
        }

        String senderDesignation = utilMethods.prepareSenderDesignation(hoodUser.getOwner(), hoodUser.getTenant());

        String sentDate = null;
        try {
            emailHTMLContent = templateService.getHTMLBodyForBasePost(emailText,  subject, senderName, senderDesignation, sentDate, googleGroup.getSocietyId(), createdForumId, googleGroup.getSocietyGroupEmail());
        } catch (IOException e) {
            log.error("Error while preparing email body content from handlebars");
            throw new RuntimeException(e);
        }

        try {
            log.info("Sending email to google group to start a new thread  from tejas: {}, sucject: {} ", groupEmail, subject);
            EmailResponse emailResponse = notificationService.sendEmailInThread(googleGroup.getSocietyGroupEmail(), senderName, groupEmail, subject, emailHTMLContent, universalMessageId, imposterEmail, groupEmail);
            sentUniversalMessageId = emailResponse.getUniversalMessageId();
            providerMessageId = emailResponse.getProviderMessageId();
            log.info("Successfully sent email to google group :{}", groupEmail);
        } catch (Exception e) {
            log.error("Error while sending email to google group: {}", groupEmail, e);
            throw new RuntimeException(e);
        }

        try {
            log.info("Adding new message in DB for societyId: {} and forumId:{} ", googleGroup.getSocietyId(), createdForumId);
            messageService.addNewMessage(createdForumId, providerMessageId, sentUniversalMessageId, null, true, true, googleGroup.getSocietyId());
            log.info("Successfully added new message in DB for new thread: {}", sentUniversalMessageId);
        } catch (Exception e) {
            log.error("Error while processing new thread: {}", sentUniversalMessageId, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * This method handles the existing thread. It prepares a new email that will be sent to the google group based on the message received by sender member.
     *
     * @param senderEmail
     * @param senderName
     * @param groupEmail
     * @param emailText
     * @param subject
     * @param universalMessageId
     * @param inReplyToUniversalMessageId
     * @param imposterEmail
     */

    @Override
    public void handleExistingThread(String senderEmail, String senderName, String groupEmail, String emailText, String subject, String universalMessageId, String inReplyToUniversalMessageId, String imposterEmail) {
        log.info("Existing thread found, senderEmail: {}, groupEmail: {}, emailText: {}, messageId: {}, inReplyTo: {}", senderEmail, groupEmail, emailText, universalMessageId, inReplyToUniversalMessageId);
        TJSMessage parentMessage = messageService.findByUniversalMessageId(inReplyToUniversalMessageId);

        if (parentMessage == null) {
            log.info("Message is not found  in DB, this is invalid request so not processing further");
            throw new RuntimeException("Message is not found  in DB, this is invalid request so not processing further");
        }
        String createdCommentId;
        String forumPostId = parentMessage.getForumPostId();
        TJSGroup googleGroup = groupService.getGroupFromGroupEmail(groupEmail);
        String societyId = googleGroup.getSocietyId();
        String parentForumReplyId = parentMessage.getCommentId();
        String currentSenderEmailAddress = utilMethods.extractEmail(senderEmail);

        TJSGroupMember googleGroupMember = groupMemberService.getTJSGroupFromMemberEmailAndSocietyId(googleGroup.getSocietyId(), currentSenderEmailAddress);
        String currentSenderFbUserId = googleGroupMember.getFirebaseUserId();

        HoodResponseDTO.ResponseData hoodUser = hoodService.getUserDetails(currentSenderFbUserId, societyId);



        String currentSenderName = hoodUser.getName();
        if(StringUtils.isBlank(currentSenderName)) {
        	currentSenderName = senderName;
        }
        String currentSenderText = emailText;
        String currentSentDate = null;
        String currentSenderDesignation = utilMethods.prepareSenderDesignation(hoodUser.getOwner(), hoodUser.getTenant());

        String sentUniversalMessageId;
        String providerMessageId;

        if (parentMessage.isBaseMessage()) {
            // this is reply to base post
            try {
                createdCommentId = forumNotifierService.notifyForReplyToBasePost(googleGroup.getSocietyId(), currentSenderFbUserId, emailText, forumPostId);
                log.info("successfully processed  for reply to existing thread and notified brahmos {}", forumPostId);
            } catch (Exception e) {
                log.error("Error while notifying brahmos for  reply to base post: {}", forumPostId, e);
                throw new RuntimeException(e);
            }

            try {
                String emailHTMLContent = templateService.getHTMLBodyForReply(currentSenderText, currentSenderName, currentSenderDesignation, currentSentDate, societyId, forumPostId, createdCommentId, groupEmail);

                log.info("Sending email to google group  in thread as sender has already started the thread from theier end , so append new email into that thread,  from tejas: {}", groupEmail);
                EmailResponse emailResponse = notificationService.sendEmailInThread(groupEmail, currentSenderName, groupEmail, subject, emailHTMLContent, inReplyToUniversalMessageId, imposterEmail, groupEmail);
                sentUniversalMessageId = emailResponse.getUniversalMessageId();
                providerMessageId = emailResponse.getProviderMessageId();
                log.info("Successfully sent email to google group :{}", groupEmail);
            } catch (Exception e) {
                log.error("Error while sending email to google group: {}", groupEmail, e);
                throw new RuntimeException(e);
            }


        } else {
            // this is reply to reply
            try {
                createdCommentId = forumNotifierService.notifyForReplyToReply(googleGroup.getSocietyId(), currentSenderFbUserId, emailText, forumPostId, parentForumReplyId);
                log.info("successfully processed  for nested reply to existing thread and notified brahmos {}", forumPostId);
            } catch (Exception e) {
                log.error("Error while notifying brahmos for reply to reply: {}", forumPostId, e);
                throw new RuntimeException(e);
            }
            try {
                ForumReplyResponse.ForumReply parentForumReply = brahmosClient.getReplyMessage(societyId, forumPostId, parentForumReplyId);
                String parentForumReplyText = parentForumReply.getReplyText().trim();
                String parentForumReplyAuthorName = parentForumReply.getSenderName();
                String parentAuthorFbUserId = parentForumReply.getSenderId();
                String parentSentDate = parentForumReply.getCreatedOn();
                String parentAuthorDesignation = hoodService.getAuthorDesignation(societyId, parentAuthorFbUserId);


                String emailHTMLContent = templateService.getHTMLBodyForNestedReply(currentSenderText, currentSenderName, currentSenderDesignation, currentSentDate, parentForumReplyAuthorName, parentForumReplyText, parentAuthorDesignation, parentSentDate, societyId, forumPostId, createdCommentId, groupEmail);

                log.info("Sending email to google group  in thread as sender has already started the thread from theier end , so append new email into that thread,  from tejas: {}", groupEmail);
                EmailResponse emailResponse = notificationService.sendEmailInThread(groupEmail, currentSenderName, groupEmail, subject, emailHTMLContent, inReplyToUniversalMessageId, imposterEmail, groupEmail);
                sentUniversalMessageId =  emailResponse.getUniversalMessageId();
                providerMessageId = emailResponse.getProviderMessageId();
                log.info("Successfully sent email to google group from tejas: {}", groupEmail);
            } catch (Exception e) {
                log.error("Error while sending email to google group: {}", groupEmail, e);
                throw new RuntimeException(e);
            }

        }
        // save this message in DB with comment id;
        log.info("Saving message in DB for forumPostId: {}, gmailMessageId: {}, univerSalMessageId : {}, societyId: {}", forumPostId, null, sentUniversalMessageId, societyId);
        messageService.addNewMessage(forumPostId, providerMessageId, sentUniversalMessageId, createdCommentId, false, true, societyId);
    }

}
