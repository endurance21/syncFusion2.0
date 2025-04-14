package com.syncfusion.controllers.webhook;

import com.syncfusion.dto.brahmos.request.inBound.NewForumNestedReplyRequest;
import com.syncfusion.dto.brahmos.request.inBound.NewForumPostRequest;
import com.syncfusion.dto.brahmos.request.inBound.NewForumReplyRequest;
import com.syncfusion.handlers.forumThreadHandlers.ForumThreadHandler;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook/forum")
public class ForumWebhookController {
    Logger log = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private ForumThreadHandler forumThreadHandler;

    @PostMapping("/new/post")
    public ResponseEntity<String> handleNewPostCreation(@Valid @RequestBody NewForumPostRequest newForumPostRequest) {
        log.info("webhook received for new post creation by forum request: {}", newForumPostRequest.toString());

        try {
            forumThreadHandler.handleNewPostCreation(newForumPostRequest);
        } catch (Exception e) {
            log.error("Error while handling new post creation", e);
            return ResponseEntity.status(500).body("Error while handling new post creation");
        }
        return ResponseEntity.ok("Email sent successfully");
    }

    @PostMapping("/new/reply")
    public ResponseEntity<String> handleNewReplyCreation(@Valid @RequestBody NewForumReplyRequest newForumReplyRequest) {
        log.info("webhook received for new reply to post creation by forum, request: {}",newForumReplyRequest.toString());

        try {
            forumThreadHandler.handleNewReplyCreation(newForumReplyRequest);
        } catch (Exception e) {
            log.error("Error while handling new reply creation", e);
            return ResponseEntity.status(500).body("Error while handling new reply creation");
        }
        return ResponseEntity.ok("Email sent successfully");
    }

    @PostMapping("/new/nestedReply")
    public ResponseEntity<String> handleNewNestedReplyCreation(@Valid @RequestBody NewForumNestedReplyRequest newForumNestedReplyRequest) {
        log.info("webhook received for new nested reply to post creation by forum, request: {}",newForumNestedReplyRequest.toString() );

        try {
            forumThreadHandler.handleNestedReplyCreation(newForumNestedReplyRequest);
        } catch (Exception e) {
            log.error("Error while handling new nested reply creation", e);
            return ResponseEntity.status(500).body("Error while handling new nested reply creation");
        }
        return ResponseEntity.ok("Email sent successfully");
    }
}
