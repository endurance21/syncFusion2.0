package com.syncfusion.dto.gmail;

import lombok.Data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.syncfusion.utils.Constants.DATE_TIME_FORMAT;

public class GmailDTO {
    public record PubSubRecord(String emailAddress, String historyId) {
    }

    @Data
    public static class GmailPayload {
        private String emailAddress;
        private String historyId;


    }

    @Data
    public static class EmailContent {
        private String authorName = "Divyanshu";
        private String emailText = "reply by divyanshu";
        private String authorDesignation = "Member @ Syncfusion";
        private String subject = "Syncfusion Forum";
        private String authorInitials = "D";
        private String sentDate;
        private String replyToCommentDeepLink;
        private String viewDiscussionDeepLink;
        private Boolean isThread= false;

        private EmailContent inReplyTo;

        public EmailContent() {
        }

        public void setSentDate(Date sentDate) {
            if (sentDate == null) {
                sentDate = new Date();
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_TIME_FORMAT);
            String dateString = dateFormat.format(sentDate);
            this.sentDate = dateString;
        }

        public void setSentDate(String sentDateString) {
            SimpleDateFormat defaultDateFormat = new SimpleDateFormat(DATE_TIME_FORMAT);
            Date sentDate ;
            if(sentDateString == null) {
                sentDate = new Date();
            }else {
                try {
                    sentDate = defaultDateFormat.parse(sentDateString);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
            SimpleDateFormat requiredDateFormat = new SimpleDateFormat(DATE_TIME_FORMAT);
            String dateString = requiredDateFormat.format(sentDate);
            this.sentDate = dateString;
        }
    }
}
