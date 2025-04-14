package com.syncfusion.dto.brahmos.response.outBound;


import lombok.Data;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.syncfusion.utils.Constants.DATE_TIME_FORMAT;

@Data
public class ForumReplyResponse {
    private String message;
    private ForumReply data;

    @Data
    public static class ForumReply {
        private String createdBy;
        private String messageType;
        private String replyText;
        private String senderId;
        private String senderName;
        private String senderProfilePic;
        private String updatedOn;
        private String createdOn;

        public String getCreatedOn() {
            // convert createdOn in millisecond to format date time
            if (createdOn == null) {
                return null;
            }
            long epochTimeMillis = Long.parseLong(createdOn);
            String formattedDate = convertEpochToFormattedDate(epochTimeMillis);

            return formattedDate;
        }
        private static String convertEpochToFormattedDate(long epochTimeMillis) {
            Date date = new Date(epochTimeMillis);
            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_TIME_FORMAT);
            return dateFormat.format(date);
        }

    }

}
