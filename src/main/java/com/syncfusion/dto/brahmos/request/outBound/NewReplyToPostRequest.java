package com.syncfusion.dto.brahmos.request.outBound;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NewReplyToPostRequest {
    String postId, replyText, societyId,replierName, replierFirebaseId, replierProfilePic;
}
