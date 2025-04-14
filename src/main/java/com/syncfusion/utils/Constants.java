package com.syncfusion.utils;

public class Constants {
    public static final String MESSAGE_ID = "Message-ID";
    public static final String REQUEST_ID = "request-id";
    public static final String X_REQUEST_ID = "X-Request-ID";
    public static final String X_REQUEST_HOST = "X-Request-Host";
    public static final String X_REAL_IP = "X-Real-IP";
    public static final String REFERER = "Referer";
    public static final String USER_AGENT = "User-agent";
    public static final String HOST = "Host";
    public static final String ACCESS_LOG = "access.log";
    public static final String LOG_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss,SSS";
    public static final String HYPHEN = "-";
    public static final String SINGLE_WHITESPACE = "\\s";
    public static final String DATE_TIME_FORMAT = "dd/MM/yyyy HH:mm";

    public static final String KAFKA_TOPIC_FOR_INBOX_UPDATE = "tejas_inbox_update";
    public static final String KAFKA_TOPIC_FOR_INBOX_UPDATE_SERVICE_ACCOUNT = "tejas_inbox_update_service_account";
    public static final String KAFKA_GROUP_ID = "group-id";


    private Constants() throws IllegalAccessException {
        throw new IllegalAccessException("Constants class cannot be accessed");
    }

    public static class RedisKeys {
        public static final String LAST_GMAIL_HISTORY_ID = "last_gmail_history_id";
    }

    public static class GmailConstants {
        public static final String TOKENS_DIRECTORY_PATH = "tokens";
        public static final String EMAIL_SOURCE_IDENTIFIER_HEADER_KEY = "X-Email-Source";
        public static final String EMAIL_SOURCE_IDENTIFIER_HEADER_VALUE = "TEJAS";

        public static final String IN_REPLY_TO_HEADER_KEY = "In-Reply-To";
        public static final String REFERENCES_HEADER_KEY = "References";
        public static final String REPLY_TO_HEADER_KEY = "Reply-To";
    }

    public enum EmailDomain {
        SYNCFUSION("syncfusion.com"),SYNCFUSION_FORUM("syncfusion-forum.com");
        private String domain;
        EmailDomain(String domain) {
            this.domain = domain;
        }
        public String getDomain() {
            return domain;
        }
    }

    public enum AUTHENTICATION_MODE{
            OAUTH2,SERVICE_ACCOUNT
    }
}
