package com.syncfusion.dto.syncfusionNotification;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;


@Data
@Builder
public class EmailRequest {

    private Template template;
    private Metadata metadata;

    @Data
    @Builder
    public static class Template {
        @JsonProperty("crmUserId")
        private String crmUserId;

        @JsonProperty("provider")
        private String provider;

        @JsonProperty("body")
        private String body;

        @JsonProperty("subject")
        private String subject;

        @JsonProperty("messageType")
        private String messageType;

        @JsonProperty("accountLabel")
        private String accountLabel;

        @JsonProperty("headers")
        private Map<String, String> headers;

        @JsonProperty("fromEmail")
        private FromEmail fromEmail;

        @JsonProperty("replyTo")
        private ReplyTo replyTo;

        @JsonProperty("recipients")
        private List<Recipient> recipients;

        @JsonProperty("additionalProperties")
        private Map<String, String> additionalProperties;

        @JsonProperty("attachments")
        private List<Object> attachments;

        @Data
        @Builder
        public static class FromEmail {
            @JsonProperty("name")
            private String name;

            @JsonProperty("email")
            private String email;

            public String getName() {
                return name;
            }
            public void setName(String name) {
                if(StringUtils.isNotBlank(name)){
                    this.name = name;
                }else{
                    this.name = "";
                }
            }

            // Getters and setters
        }

        @Data
        @Builder
        public static class ReplyTo {
            @JsonProperty("name")
            private String name;

            @JsonProperty("email")
            private String email;
            public String getName() {
                return name;
            }
            public void setName(String name) {
                if(StringUtils.isNotBlank(name)){
                    this.name = name;
                }else{
                    this.name = "";
                }
            }
            // Getters and setters
        }

        @Data
        @Builder
        public static class Recipient {
            @JsonProperty("name")
            private String name;

            @JsonProperty("email")
            private String email;

            @JsonProperty("type")
            private String type;

            // Getters and setters
            public String getName() {
                return name;
            }
            public void setName(String name) {
                if(StringUtils.isNotBlank(name)){
                    this.name = name;
                }else{
                    this.name = "";
                }
            }
        }
    }

    @Data
    @Builder
    public static class Metadata {
        @JsonProperty("business")
        private String business;

        @JsonProperty("apiVersion")
        private String apiVersion;

        @JsonProperty("notificationType")
        private String notificationType;

        @JsonProperty("authKey")
        private String authKey;
    }
}
