package com.syncfusion.services.notification;

import com.syncfusion.dto.nobrokerNotification.EmailRequest;
import com.syncfusion.dto.nobrokerNotification.EmailResponse;
import com.syncfusion.dto.nobrokerNotification.netcore.NetcoreEmailResponse;
import com.syncfusion.exceptions.TJSException;
import com.syncfusion.utils.UtilMethods;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.syncfusion.utils.Constants.GmailConstants.*;
import static com.syncfusion.utils.Constants.MESSAGE_ID;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.OK;

@Service
@Qualifier("SyncfusionNotificationService")
public class SyncfusionNotificationService implements INotificationService {
    private static Logger log = LoggerFactory.getLogger(SyncfusionNotificationService.class);

    @Value("${syncfusion.notification.authkey:****}")
    private String authKey;

    @Value("${syncfusion.notification.url:https://www.syncfusion.com/notification-api/send}")
    private String syncfusionNotificationUrl;

    @Value("${syncfusion.notification.crmUserId:****}")
    private String crmUserId;

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private UtilMethods utilMethods;

    @Autowired
    private Environment environment;

    @Override
    public EmailResponse sendEmail(String to, String fromName, String fromEmailAddress, String subject, String body, String replyToEmailAddress, String imposterEmail) throws MessagingException, IOException {
        String universalMessageID = utilMethods.prepareRandomMessageId();
        EmailRequest emailRequest = prepareEmailRequest(to, fromName, fromEmailAddress, subject, body, replyToEmailAddress, universalMessageID, null);
        log.info("Sending email request: {}", emailRequest);
        EmailResponse emailResponse = sendEmail(emailRequest);
        log.info("Successfully sent email with provider id ID: {}", emailResponse.getProviderMessageId());
        return emailResponse;
    }

    @Override
    public EmailResponse sendEmailInThread(String to, String fromName, String fromEmailAddress, String subject, String body, String inReplyToMessageId, String imposterEmail, String replyToEmailAddress) throws MessagingException, IOException {
        String universalMessageID = utilMethods.prepareRandomMessageId();
        EmailRequest emailRequest = prepareEmailRequest(to, fromName, fromEmailAddress, subject, body, replyToEmailAddress, universalMessageID, inReplyToMessageId);
        log.info("Sending email request: {}", emailRequest);
        EmailResponse responseBody = sendEmail(emailRequest);
        log.info("Successfully sent email with provider id ID: {}", responseBody.getProviderMessageId());

        return responseBody;
    }

    private EmailResponse sendEmail(EmailRequest emailRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<EmailRequest> entity = new HttpEntity<>(emailRequest, headers);

        ResponseEntity<NetcoreEmailResponse> response = restTemplate.exchange(syncfusionNotificationUrl, POST, entity, NetcoreEmailResponse.class);

        if (response.getStatusCode() == OK) {
            log.info("Successfully sent email. Status code: {}", response.getStatusCode());
            NetcoreEmailResponse responseBody = response.getBody();
            String providerMessageId = responseBody.getEventLog().getMessageId();
            EmailResponse emailResponse = EmailResponse.builder()
                    .providerMessageId(providerMessageId)
                    .success(true)
                    .universalMessageId(emailRequest.getTemplate().getHeaders().get(MESSAGE_ID))
                    .build();
            return emailResponse;
        } else {
            log.info("Failed to send email. Status code: {}", response.getStatusCode());
            throw new TJSException("Error while calling syncfusion notification server ;)", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private EmailRequest prepareEmailRequest(String to, String fromName, String fromEmailAddress, String subject, String body, String replyToEmailAddress, String universalMessageId, String inReplyTo) {
        EmailRequest.Template.FromEmail fromEmail = EmailRequest.Template.FromEmail.builder().name(fromName).email(fromEmailAddress).build();
        EmailRequest.Template.Recipient recipient = EmailRequest.Template.Recipient.builder().email(to).type("TO").name("").build();

        String provider = getProvider();
        EmailRequest.Template.ReplyTo replyTo = EmailRequest.Template.ReplyTo.builder().email(replyToEmailAddress).name("").build();

        Map<String, String> headers = new HashMap<>();
        headers.put(MESSAGE_ID, universalMessageId);
        headers.put(EMAIL_SOURCE_IDENTIFIER_HEADER_KEY, EMAIL_SOURCE_IDENTIFIER_HEADER_VALUE);
        headers.put(utilMethods.getUnsubsribeListHeaderKey(), utilMethods.getUnsubsribeListHeaderValue(fromEmailAddress));

        if (StringUtils.isNotBlank(inReplyTo)) {
            headers.put(IN_REPLY_TO_HEADER_KEY, inReplyTo);
        }
        EmailRequest.Template template = EmailRequest.Template.builder()
                .fromEmail(fromEmail)
                .subject(subject)
                .body(body)
                .provider(provider)
                .recipients(Collections.singletonList(recipient))
                .replyTo(replyTo)
                .messageType("SYNCFUSION_FORUM_SYNC")
                .headers(headers)
                .crmUserId(crmUserId)
                .build();

        EmailRequest.Metadata metadata = EmailRequest.Metadata.builder().
                authKey(authKey)
                .business("hood")
                .apiVersion("v1")
                .notificationType("EMAIL")
                .build();
        EmailRequest emailRequest = EmailRequest.builder()
                .template(template)
                .metadata(metadata)
                .build();
        return emailRequest;
    }

    private String getProvider(){
        String provider = environment.getProperty("syncfusion.notification.provider","NETCORE");
        return provider;
    }
}
