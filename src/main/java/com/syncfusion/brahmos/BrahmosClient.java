package com.syncfusion.brahmos;

import com.syncfusion.dto.brahmos.request.outBound.ForumReplyDataRequest;
import com.syncfusion.dto.brahmos.request.outBound.NewForumPostRequest;
import com.syncfusion.dto.brahmos.request.outBound.NewNestedReplyRequest;
import com.syncfusion.dto.brahmos.request.outBound.NewReplyToPostRequest;
import com.syncfusion.dto.brahmos.response.outBound.ForumReplyResponse;
import com.syncfusion.dto.brahmos.response.outBound.NewForumPostResponse;
import com.syncfusion.dto.brahmos.response.outBound.NewNestedReplyResponse;
import com.syncfusion.dto.brahmos.response.outBound.NewReplyToPostResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class BrahmosClient {

    Logger log = LoggerFactory.getLogger(BrahmosClient.class);
    @Autowired
    private RestTemplate restTemplate;
    @Value("${brahmos.cloufunctions.createnewpost.url:http://localhost:5001/nobrokerhood-stage/us-central1/createnewpost}")
    private String createNewPostUrl;
    @Value("${brahmos.cloufunctions.createnewreply.url:http://localhost:5001/nobrokerhood-stage/us-central1/addreplytopost}")
    private String createNewReplyUrl;
    @Value("${brahmos.cloufunctions.createnewnestedreply.url:http://localhost:5001/nobrokerhood-stage/us-central1/addnestedreply}")
    private String createNewNestedReplyUrl;

    @Value("${brahmos.cloufunctions.getforumreplies.url:http://localhost:5001/nobrokerhood-stage/us-central1/getreplydata}")
    private String getForumRepliesUrl;
    @Value("${brahmos.cloufunctions.authorization.token:some_token}")
    private String authorizationtoken;

    public String createNewForumPost(String societyId, String authorFirebaseUserId, String authorName, String emailText, String emailSubject) {
        log.info("calling cloud function to create new forum post, with societyId: {}, authorName: {}, firebaseUerId: {}, emailText: {}, emailSubject: {}",
                societyId, authorName,  authorFirebaseUserId, emailText, emailSubject);

        NewForumPostRequest newForumPostBody = new NewForumPostRequest(societyId,authorName,  authorFirebaseUserId, emailText, emailSubject);
        HttpEntity<NewForumPostRequest> httpEntity = createNewHTTPEntity(newForumPostBody);
        ResponseEntity<NewForumPostResponse> responseEntity = restTemplate.postForEntity(createNewPostUrl, httpEntity,  NewForumPostResponse.class);
        log.info("response from cloud function: {}", responseEntity);
        return responseEntity.getBody().getData().getForumPostId();
    }

    public String createNewReplyToForumPost(String societyId, String authorFirebaseId, String authorName, String authorProfilePic,  String replyText, String forumPostId) {
        log.info("calling cloud function to create new reply to forum post, with societyId: {}, authorFirebaseUserId: {}, replyText: {}, forumPostId: {}", societyId, authorFirebaseId, replyText, forumPostId);
        NewReplyToPostRequest newReplyToPostRequest = new NewReplyToPostRequest(forumPostId, replyText, societyId, authorName, authorFirebaseId, authorProfilePic);

        HttpEntity<NewReplyToPostRequest> httpEntity = createNewHTTPEntity(newReplyToPostRequest);
        ResponseEntity<NewReplyToPostResponse> responseEntity = restTemplate.postForEntity(createNewReplyUrl, httpEntity, NewReplyToPostResponse.class);
        log.info("response from cloud function: {}", responseEntity.toString());
        return responseEntity.getBody().getData().getReplyId();
    }

    public String createNewNestedReplyToForumPost(String societyId, String authorFirebaseId, String authorName, String authorProfilePic,  String replyText, String forumPostId,  String parentReplyId) {
        log.info("calling cloud function to create new nested reply to forum post, with societyId: {}, authorFirebaseUserId: {}, replyText: {}, forumPostId: {}, parentReplyId: {}", societyId, authorFirebaseId, replyText, forumPostId, parentReplyId);
        NewNestedReplyRequest newNestedReplyRequest = new NewNestedReplyRequest(forumPostId, replyText, societyId, authorName, authorFirebaseId, authorProfilePic, parentReplyId);
        HttpEntity<NewNestedReplyRequest> httpEntity = createNewHTTPEntity(newNestedReplyRequest);
        ResponseEntity<NewNestedReplyResponse> responseEntity = restTemplate.postForEntity(createNewNestedReplyUrl, httpEntity, NewNestedReplyResponse.class);
        log.info("response from cloud function: {}", responseEntity.toString());
        return responseEntity.getBody().getData().getReplyId();
    }

    public ForumReplyResponse.ForumReply getReplyMessage(String societyId, String forumId, String commentId){
        log.info("Calling cloud function to get reply data for societyId :{}, forumId: {}, commentId: {}", societyId, forumId, commentId);
        ForumReplyDataRequest forumReplyDataRequest = new ForumReplyDataRequest();
        forumReplyDataRequest.setReplyId(commentId);
        forumReplyDataRequest.setForumId(forumId);
        forumReplyDataRequest.setSocietyId(societyId);
        HttpEntity<ForumReplyDataRequest> httpEntity = createNewHTTPEntity(forumReplyDataRequest);
        ResponseEntity<ForumReplyResponse> responseEntity = restTemplate.postForEntity(getForumRepliesUrl, httpEntity, ForumReplyResponse.class);
        log.info("response from cloud function: {}", responseEntity.toString());
        return responseEntity.getBody().getData();
    }
    private <T> HttpEntity<T> createNewHTTPEntity( T requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("authorization", authorizationtoken);
        return new HttpEntity<>(requestBody, headers);
    }
}
