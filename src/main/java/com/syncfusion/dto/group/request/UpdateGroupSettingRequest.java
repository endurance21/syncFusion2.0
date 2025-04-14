package com.syncfusion.dto.group.request;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateGroupSettingRequest {
    @NotBlank(message = "society_id cannot be blank")
    @JsonProperty("society_id")
    String societyId;
    @NotBlank(message = "active field  cannot be blank")
    String active;
}
