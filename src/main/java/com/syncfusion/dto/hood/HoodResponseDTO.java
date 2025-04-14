package com.syncfusion.dto.hood;


import lombok.Data;

import java.util.List;

@Data
public class HoodResponseDTO {

    private String msg;
    private String sts;

    private List<ResponseData> data;

    private String timeZone;

    @Data
    public static class ResponseData {
       private String fbUserId;
       private String name;
       private String profilePic;
       private String tenant;
       private String owner;
    }
}
