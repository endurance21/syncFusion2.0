package com.syncfusion.services;


import com.syncfusion.database.models.TJSMessage;
import com.syncfusion.database.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    public void saveMessage(String forumPostId, String providerMessageId, String universalMessageId, String commentId, boolean isBaseMessage, boolean isCreatedByEmail, String societyId) {
        TJSMessage newMessage = new TJSMessage();
        newMessage.setBaseMessage(isBaseMessage);
        newMessage.setCommentId(commentId);
        newMessage.setForumPostId(forumPostId);
        newMessage.setProviderMessageId(providerMessageId);
        newMessage.setUniversalMessageId(universalMessageId);
        newMessage.setCreatedByEmail(isCreatedByEmail);
        newMessage.setSocietyId(societyId);
        messageRepository.save(newMessage);
    }

    public void addNewMessage(String forumPostId, String providerMessageId, String universalMessageId, String commentId, boolean isBaseMessage, boolean isCreatedByEmail, String societyId ){
        saveMessage(forumPostId, providerMessageId, universalMessageId, commentId, isBaseMessage, isCreatedByEmail, societyId );
    }

    public TJSMessage findByUniversalMessageId(String universalMessageId) {
        return messageRepository.findByUniversalMessageId(universalMessageId);
    }

    public TJSMessage findBaseMessageByForumPostId(String forumPostId) {
        return messageRepository.findBaseMessageByForumPostId(forumPostId);
    }

    public  boolean isForumPostExists(String forumPostId){
        return messageRepository.existsByForumPostId(forumPostId);
    }
    public TJSMessage findByCommentId(String commentId) {
        return messageRepository.findByCommentId(commentId);
    }
}
