package com.syncfusion.dto.template;

import com.github.jknack.handlebars.Handlebars;
import lombok.Data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.nobroker.tejas.utils.Constants.DATE_TIME_FORMAT;

@Data
public class Reply {
    private String authorName = "Divyanshu";
    private Handlebars.SafeString emailText = new Handlebars.SafeString("reply by divyanshu");
    private String authorDesignation = "Member @ NoBroker";
    private String authorInitials = "D";
    private String sentDate;
    private Handlebars.SafeString replyToCommentDeepLink;
    private Handlebars.SafeString viewDiscussionDeepLink;
    private String societyEmail;

    public void setReplyToCommentDeepLink(String deepLink) {
        this.replyToCommentDeepLink =  setAsSafeString(deepLink);
    }
    public void setViewDiscussionDeepLink(String deepLink) {
        this.viewDiscussionDeepLink =  setAsSafeString(deepLink);
    }
    public void setEmailText(String emailText) {
        this.emailText = setAsSafeString(emailText);
    }
    private Handlebars.SafeString setAsSafeString(String string) {
        return new Handlebars.SafeString(string);
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
