package com.syncfusion.dto.brahmos.request.outBound;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NewForumPostRequest {
    private String societyId, authorName, authorFirebaseUserId, content, subject ;

}
