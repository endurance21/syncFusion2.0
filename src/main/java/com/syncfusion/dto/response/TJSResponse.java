package com.syncfusion.dto.response;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
@Builder
public class TJSResponse<T,k> {
    private HttpStatus status;
    private String message;
    private T data;
    private k errors;
    public  int getStatus(){
        return status.value();
    }
}
