package com.syncfusion.dto.group.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GroupDeleteRequest {

    @JsonProperty("society_id")
    private String societyId;
}
