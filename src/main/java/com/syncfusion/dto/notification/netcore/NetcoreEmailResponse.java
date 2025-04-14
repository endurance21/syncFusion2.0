package com.syncfusion.dto.syncfusionNotification.netcore;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class NetcoreEmailResponse {
    @JsonProperty("event_log")
    private  EventLog eventLog;
    private String status;

    @Data
    public class EventLog{
        private  String messageId;
    }

}
