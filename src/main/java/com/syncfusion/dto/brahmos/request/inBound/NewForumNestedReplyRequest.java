package com.syncfusion.dto.brahmos.request.inBound;

import lombok.Data;

@Data
public class NewForumNestedReplyRequest extends NewForumReplyRequest {
    private String parentReplyId;
//    private String parentReplyText;
//    private String parentReplyAuthorName;
//    private String parentReplyAuthorUserId;
//    private String parentReplyAuthorProfilePic;
//    private String parentReplyCreatedOn;

    public String toString(){
        return "parentReplyId:" +parentReplyId +" " + super.toString();
    }

}
