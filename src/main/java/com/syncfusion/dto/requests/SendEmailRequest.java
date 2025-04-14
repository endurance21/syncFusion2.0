package com.syncfusion.dto.requests;

import lombok.Data;

@Data
public class SendEmailRequest {
    String fromName;
    String fromEmailAddress;
    String to;
    String subject;
    String body;
    String replyToEmailAddress;
    String inReplyTo;
}
