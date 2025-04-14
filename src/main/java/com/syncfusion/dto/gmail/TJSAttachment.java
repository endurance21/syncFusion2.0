package com.syncfusion.dto.gmail;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TJSAttachment {

    private String attachmentId;
    private String fileName;
    private String mimeType;
    private String size;
    private String gcpUrl;

}
