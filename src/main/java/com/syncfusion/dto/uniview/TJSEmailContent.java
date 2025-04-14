package com.syncfusion.dto.uniview;


import com.syncfusion.dto.gmail.TJSAttachment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class TJSEmailContent {
    @NonNull
    private String id;
    private Sender from;
    private List<Recipient> to;

    private List<Recipient> cc;
    private List<Recipient> bcc;
    private String subject;
    private String htmlText;
    private String plainText;
    private List<TJSAttachment> attachments;

    private String universalMessageId;

    private String inReplyToMessageId;

    @Builder
   static public class Sender{
        String name;
        String emailAddress;
    }
    @Builder
   static public  class Recipient{
        String name;
        String emailAddress;
        Type type;
       public static enum Type{
            TO,CC,BCC
        }
    }
}
