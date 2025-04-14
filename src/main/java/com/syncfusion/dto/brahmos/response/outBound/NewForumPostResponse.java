package com.syncfusion.dto.brahmos.response.outBound;

import lombok.Data;

@Data
public class NewForumPostResponse {
   private String message;
   private ResponseData data;

   @Data
    public static class ResponseData{
        private String forumPostId;
    }
}


