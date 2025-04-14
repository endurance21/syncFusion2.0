package com.syncfusion.dto.group.request;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GoogleGroupMemberAddRequest {
    @JsonProperty("society_email")
    private String societyEmail;
    @JsonProperty("member_email")
    private String memberEmail;
}
