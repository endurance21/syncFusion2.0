package com.syncfusion.dto.brahmos.response.outBound;

import lombok.Data;

@Data
public class NewNestedReplyResponse {
    private  String message;
    private  NestedReplyData  data;

    @Data
    public static class NestedReplyData{
        public String replyId;
    }
}
