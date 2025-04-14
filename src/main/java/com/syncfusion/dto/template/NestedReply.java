package com.syncfusion.dto.template;

import lombok.Data;

@Data
public class NestedReply extends Reply {
    private Reply inReplyTo;

    public String toString() {
        return "NestedReply(" + super.toString() + ", inReplyTo=" + this.inReplyTo + ")";
    }
}
