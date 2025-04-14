package com.syncfusion.dto.syncfusionNotification;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmailResponse {
    private String universalMessageId;
    private String providerMessageId;
    private boolean success = true;
}
