package com.syncfusion.dto.group.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CustomGroupMemberAddRequest {
    @JsonProperty("society_id")
    private String societyId;
    @JsonProperty("member_email")
    private String memberEmail;
    @JsonProperty("firebase_user_id")
    private String firebaseUserId;
}
