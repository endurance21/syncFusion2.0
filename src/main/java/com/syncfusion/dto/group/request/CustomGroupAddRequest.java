package com.syncfusion.dto.group.request;

import lombok.Data;

@Data
public class CustomGroupAddRequest {
    private String societyId;
    private String groupName;
    private String groupEmail;
    private String imposterEmail;
}
