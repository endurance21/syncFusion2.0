package com.syncfusion.kafka;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EmailUpdateMessage {
    String emailAddress;
    String historyId;

    public EmailUpdateMessage(String emailAddress, String historyId) {
        this.emailAddress = emailAddress;
        this.historyId = historyId;
    }
}
