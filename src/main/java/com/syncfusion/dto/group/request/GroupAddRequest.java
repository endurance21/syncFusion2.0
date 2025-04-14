package com.syncfusion.dto.group.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GroupAddRequest {
    @JsonProperty("society_id")
    private String societyId;
    @JsonProperty("society_name")
    private String societyName;
    @JsonProperty("society_seed_email_address")
    private String societySeedEmailAddress;
}
