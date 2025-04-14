package com.syncfusion.dto.brahmos.response.outBound;

import lombok.Data;

@Data
public class NewReplyToPostResponse {

    private String message;
    private ResponseData data;

    @Data
    public static class ResponseData{
        private String replyId;
    }
}
