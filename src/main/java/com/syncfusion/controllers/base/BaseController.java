package com.syncfusion.controllers.base;


import com.google.api.services.gmail.model.Message;
import com.syncfusion.dto.requests.SendEmailRequest;
import com.syncfusion.dto.response.TJSResponse;
import com.syncfusion.services.TemplateService;
import com.syncfusion.services.gmail.GmailClientProvider;
import com.syncfusion.services.notification.SyncfusionNotificationService;
import com.syncfusion.utils.UtilMethods;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.mail.MessagingException;
import java.io.IOException;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/")
public class BaseController {
    @Autowired
    private GmailClientProvider gmailClientProvider;

    @Autowired
    private TemplateService templateService;

    @Autowired
    @Qualifier("SyncfusionNotificationService")
    private SyncfusionNotificationService notificationService;

    @Autowired
    private UtilMethods utilMethods;

    @GetMapping("/test")
    public String test() throws IOException {
        return "hello from TEJAS !";

    }

    @GetMapping("/gmail/send/{to}/{subject}/{body}/{name}/{inreplyto}/{imposterEmail}")
    public String sendEmail(@PathVariable String to, @PathVariable String subject, @PathVariable String body, @PathVariable String name, @PathVariable String inreplyto, @PathVariable String imposterEmail) throws IOException, MessagingException {
        if (!StringUtils.isBlank(inreplyto)) {
            Message messaage = gmailClientProvider.getGmailClientForServiceAccount().sendEmailInThread(to, inreplyto, name, subject, body, "text/html");
            return messaage.getId();
        }
        Message message = gmailClientProvider.getGmailClientForServiceAccount().sendEmail(to, name, subject, body, "text/html", imposterEmail, imposterEmail, imposterEmail);
        return message.getId();
    }

    @PostMapping("/email")
    public ResponseEntity<TJSResponse<Object,Object>> sendEmailViaNotificationService(@Validated @RequestBody SendEmailRequest emailRequest) throws IOException, MessagingException {
        String messageId;
        try {
            if (StringUtils.isBlank(emailRequest.getInReplyTo())) {
                messageId = notificationService.sendEmail(emailRequest.getTo(), emailRequest.getFromName(), emailRequest.getFromEmailAddress(), emailRequest.getSubject(), emailRequest.getBody(), emailRequest.getReplyToEmailAddress(), emailRequest.getFromEmailAddress()).getUniversalMessageId();
            }else{
                messageId = notificationService.sendEmailInThread(emailRequest.getTo(), emailRequest.getFromName(), emailRequest.getFromEmailAddress(), emailRequest.getSubject(), emailRequest.getBody(), emailRequest.getInReplyTo(), emailRequest.getFromEmailAddress(), emailRequest.getReplyToEmailAddress()).getUniversalMessageId();
            }
            return utilMethods.getResponseEntity(TJSResponse.builder().message("Email sent successfully ").data(messageId).status(OK).build(), OK);
        } catch (Exception e) {
            return utilMethods.getResponseEntity(TJSResponse.builder().data(null).status(INTERNAL_SERVER_ERROR).message(e.getMessage()).build(), INTERNAL_SERVER_ERROR);

        }
    }

    @GetMapping("/email/hbs")
    public String getEmailContent() throws IOException {
        return templateService.getSampleHtmlContent();
    }

    @GetMapping("/email/hbs/reply")
    public String getEmailReplyContent() throws IOException {
        return templateService.getSampleReplyHtmlContent();
    }
    @GetMapping("/email/hbs/reply/nested")
    public String getEmailNestedReplyContent() throws IOException {
        return templateService.getSampleNestedRelytHtmlContent();
    }

    @GetMapping("/health")
    public String health() {
        return "hello from SYNCFUSION !";
    }

    @PostMapping("/send-email")
    public ResponseEntity<TJSResponse<Object,Object>> sendEmailViaSyncfusionNotificationService(@Validated @RequestBody SendEmailRequest emailRequest) throws IOException, MessagingException {
        String messageId;
        try {
            if (StringUtils.isBlank(emailRequest.getInReplyTo())) {
                messageId = notificationService.sendEmail(emailRequest.getTo(), emailRequest.getFromName(), emailRequest.getFromEmailAddress(), emailRequest.getSubject(), emailRequest.getBody(), emailRequest.getReplyToEmailAddress(), emailRequest.getFromEmailAddress()).getUniversalMessageId();
            }else{
                messageId = notificationService.sendEmailInThread(emailRequest.getTo(), emailRequest.getFromName(), emailRequest.getFromEmailAddress(), emailRequest.getSubject(), emailRequest.getBody(), emailRequest.getInReplyTo(), emailRequest.getFromEmailAddress(), emailRequest.getReplyToEmailAddress()).getUniversalMessageId();
            }
            return utilMethods.getResponseEntity(TJSResponse.builder().message("Email sent successfully ").data(messageId).status(OK).build(), OK);
        } catch (Exception e) {
            return utilMethods.getResponseEntity(TJSResponse.builder().data(null).status(INTERNAL_SERVER_ERROR).message(e.getMessage()).build(), INTERNAL_SERVER_ERROR);

        }
    }
}

