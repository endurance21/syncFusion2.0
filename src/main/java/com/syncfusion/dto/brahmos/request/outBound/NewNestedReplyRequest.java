package com.syncfusion.dto.brahmos.request.outBound;

public class NewNestedReplyRequest extends NewReplyToPostRequest{
    String parentReplyId;

    public NewNestedReplyRequest(String postId, String content, String societyId, String replierName, String replierFirebaseUserId, String replierProfilePic, String parentReplyId){
        super(postId, content, societyId, replierName, replierFirebaseUserId, replierProfilePic);
        this.parentReplyId =  parentReplyId;
    }

    public void setParentReplyId(String parentReplyId) {
        this.parentReplyId = parentReplyId;
    }

    public String getParentReplyId() {
        return parentReplyId;
    }
}
