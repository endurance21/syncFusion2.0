package com.syncfusion.dto.group.response;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GoogleGroupResponse {
    String groupEmail;
    String groupId;
    String groupName;
}
