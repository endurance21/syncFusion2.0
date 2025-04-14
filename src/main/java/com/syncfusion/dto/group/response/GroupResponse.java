package com.syncfusion.dto.group.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GroupResponse<T> {
    private String message;
    private T data;
}
