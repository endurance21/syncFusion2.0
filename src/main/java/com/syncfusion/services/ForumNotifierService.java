package com.syncfusion.services;

import com.syncfusion.brahmos.BrahmosClient;
import com.syncfusion.dto.hood.HoodResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class ForumNotifierService {
    Logger log = LoggerFactory.getLogger(ForumNotifierService.class);
    @Autowired
    private BrahmosClient brahmosClient;

    @Autowired
    private HoodService hoodService;

    public String notifyForNewThread(String societyId, String authorFbUserId, String emailText, String emailSubject) {
        log.info("notify brahmos for new thread authorUserId: {}, societyId: {}, emailText: {}, emailSubject: {}", authorFbUserId, societyId, emailText, emailSubject);
        HoodResponseDTO.ResponseData userData = hoodService.getUserDetails(authorFbUserId, societyId);
        if (userData == null) {
            log.error("user not found in hood for userID: {}", authorFbUserId);
            return null;
        }
        return brahmosClient.createNewForumPost(societyId, authorFbUserId, userData.getName(), emailText, emailSubject);
    }

    public String notifyForReplyToBasePost(String societyId, String authorFbUserId, String emailText, String forumPostId) {
        log.info("notify brahmos for reply to base post userId: {}, societyId: {}, emailText: {}, forumPostId: {}", authorFbUserId, societyId, emailText, forumPostId);
        HoodResponseDTO.ResponseData userData = hoodService.getUserDetails(authorFbUserId, societyId);
        if (userData == null) {
            log.error("user not found in hood for userId: {}", authorFbUserId);
            return null;
        }
        return brahmosClient.createNewReplyToForumPost(societyId, authorFbUserId,userData.getName(), userData.getProfilePic(),  emailText, forumPostId);
    }

    public String notifyForReplyToReply(String societyId, String authorFbUserId, String emailText, String forumPostId, String inReplyToCommentId) {
        log.info("notify brahmos for nested reply to post userId: {}, societyId: {}, emailText: {}, forumPostId: {}, inReplyToCommentId: {}", authorFbUserId, societyId, emailText, forumPostId, inReplyToCommentId);
        HoodResponseDTO.ResponseData userData = hoodService.getUserDetails(authorFbUserId, societyId);
        if (userData == null) {
            log.error("user not found in hood for userId: {}", authorFbUserId);
            return null;
        }
        return brahmosClient.createNewNestedReplyToForumPost(societyId, authorFbUserId,userData.getName(), userData.getProfilePic(),  emailText, forumPostId, inReplyToCommentId);
    }

}
