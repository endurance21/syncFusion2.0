package com.syncfusion.dto.brahmos.request.inBound;


import lombok.Data;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.nobroker.tejas.utils.Constants.DATE_TIME_FORMAT;

@Data
public class NewForumReplyRequest {
    private String societyId;
    private String forumPostId;
    private String replyText;
    private String title ;

    private String authorName;
    private String replyId;
    private String senderId;
    private String createdOn ;
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
