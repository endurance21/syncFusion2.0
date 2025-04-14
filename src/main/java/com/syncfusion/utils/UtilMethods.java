package com.syncfusion.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.internal.text.StringEscapeUtils;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.syncfusion.dto.hood.BasePostDeepLinkData;
import com.syncfusion.dto.hood.CommentDeepLinkData;
import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMultipart;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.converter.EmailConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Component
public class UtilMethods {

    Logger log = LoggerFactory.getLogger(UtilMethods.class);
    @Autowired
    ResourceLoader resourceLoader;

    @Autowired
    private Environment environment;

    @Value("${hood.deeplink.baseUrl:''}")
    private String deepLinkBaseUrl;

    @Value("${email.domain:syncfusion.com}")
    private String emailDomain;

    @Value("${gmail.service.impersonate.user.email:service@syncfusion.com}")
    private String imposterEmail;


    @Value("${admin.emails:'admin@syncfusion.com,'}")
    private String adminEmails;

    public static String truncateToTwoChars(String input) {
        if (StringUtils.isBlank(input)) {
            return "";
        }
        return input.substring(0, Math.min(input.length(), 2));
    }

    private static String escapeSpaces(String input) {
        return input.trim().replaceAll("\r\n", "\n")
                .replaceAll(" ", "&nbsp;")
                .replaceAll("\n", "<br>");
    }

    public <T> String convertToJson(T object) {
        ObjectMapper mapper = new ObjectMapper();
        String json = null;
        try {
            json = mapper.writeValueAsString(object);
        } catch (IOException e) {
            log.error("Error while converting object to json", e);
        }
        return json;
    }

    public String getMessageId() {
        String messageId = new StringBuilder().append("<syncfusion-").append(System.currentTimeMillis()).append("@").append("syncfusion.com").append(">").toString();
        return messageId;
    }

    public String getEmailDomain() {
        return emailDomain;
    }
    public String getEmailDomain(String email){
        if(StringUtils.isBlank(email)){
            return "";
        }
        if(email.contains("@")){
            return email.split("@")[1];
        }
        return "";
    }

    public String getImposterEmail() {
        return imposterEmail;
    }

    public String removeReFromSubject(String subject) {
        String result = subject.replaceAll("^\\s*Re\\s*:", "").trim();
        return result;
    }

    public boolean isProd() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if (profile.equalsIgnoreCase("prod")) {
                return true;
            }
        }
        return false;
    }
    public boolean isTestProfile(){
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if (profile.equalsIgnoreCase("test")) {
                return true;
            }
        }
        return false;
    }

    public String getActiveProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        return activeProfiles[0];
    }

    public String proposeEmailFromSocietyName(String groupName) {
        // replace longest sequence of  contagous whitespace with "-"
        String email = groupName.replaceAll("\\s+", "-").toLowerCase() + "@" + emailDomain;
        log.info("Proposing email for group name: {}, as : {} ", groupName, email);
        return email;

    }

    public InputStream getFileAsStream(String filePath) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:" + filePath);
        return resource.getInputStream();
    }

    public String extractEmail(String input) {
        Pattern pattern = Pattern.compile(".*?<(.*?)>|([^<>\\s]+)");
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            String email = matcher.group(1);
            if (email == null) {
                email = matcher.group(2);
            }
            return email;
        }
        return null;
    }

    public String decodeBase64(String encodedString) {
        // Replace "-" with "+" and "_" with "/"
        String replacedString = encodedString.replace("-", "+").replace("_", "/");
        byte[] decodedBytes = java.util.Base64.getDecoder().decode(replacedString);
        return new String(decodedBytes);
    }

    public String encodeBase64(String originalString) {
        byte[] bytes = originalString.getBytes();
        byte[] encodedBytes = Base64.getEncoder().encode(bytes);
        return new String(encodedBytes);
    }

    public String prepareSenderDesignation(String owner, String tenant) {
        if (StringUtils.isBlank(owner) && StringUtils.isBlank(tenant)) {
            return "";
        }
        if (StringUtils.isBlank(owner)) {
            return new StringBuilder().append("Tenant@ ").append(tenant).toString();
        }
        return new StringBuilder().append("Owner@ ").append(owner).toString();
    }

    public String extractHeader(MessagePart messagePart, String headerKey) {
        Optional<MessagePartHeader> messagePartHeader = messagePart.getHeaders().stream().filter(header -> header.getName().equalsIgnoreCase(headerKey)).findFirst();

        if (messagePartHeader.isPresent()) {
            return messagePartHeader.get().getValue();
        }
        return null;
    }

    public List<String> extractCCHeader(MessagePart messagePart){
        String ccString =  extractHeader(messagePart, "Cc");
        if(StringUtils.isBlank(ccString)){
            return new ArrayList<>();
        }
        String[] ccEmails = ccString.split(",");
        return Arrays.asList(ccEmails);
    }

    public String extractSubjectFromMessage(MessagePart message) {
        return extractHeader(message, "Subject");

    }

    public String findEmailBodyHTML(Message message) {
        Optional<MessagePart> messagePart = message.getPayload().getParts().stream().filter(p -> p.getMimeType().equalsIgnoreCase("text/html")).findFirst();
        if (messagePart.isPresent()) {
            return decodeBase64(messagePart.get().getBody().getData());
        }
        throw new RuntimeException("No HTML body found in email message");
    }

    public String extractMessageIdFromMessage(MessagePart message) {
        return extractHeader(message, "Message-Id");
    }

    public String extractFromHeaderFromMessage(MessagePart message) {
        return extractHeader(message, "From");
    }

    public String extractNameFromEmail(String email) {
        if (StringUtils.isBlank(email)) {
            return "";
        }

        Pattern pattern = Pattern.compile("^(.*?)\\s*<.*>$"); // Matches "some name" before the angle brackets
        Matcher matcher = pattern.matcher(email);

        if (matcher.find()) {
            return matcher.group(1).trim();
        } else {
            pattern = Pattern.compile("(.+)@.*");
            matcher = pattern.matcher(email);
            if (matcher.matches()) {
                // The name will be in the first capturing group (index 1)
                return matcher.group(1);
            }
            // If no name is found, return the whole email
            return email;
        }
    }

    public String extractInReplyToHeaderFromMessage(MessagePart message) {
        return extractHeader(message, "In-Reply-To");
    }

    public String extractToHeaderFromMessage(MessagePart message) {
        return extractHeader(message, "To");
    }

    public String extractAddressFromEmail(String emailWithName) {
        return extractEmail(emailWithName);
    }

    public String removeEmailHistoryText(String emailText) {
        String gmailSanitized = removeGmailEmailHistory(emailText);
        String outlookSanitized = removeEmailHistoryForOutLookClient(gmailSanitized);
        String yahooSanitized = removeEmailHistoryForYahooClient(outlookSanitized);
        return yahooSanitized;
    }

    public String removeGmailEmailHistory(String emailText) {
        String regexPattern = "(On (Sun|Mon|Tue|Wed|Thu|Fri|Sat), ([0-9]{0,2}[\\s]{0,1})(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec)[,]{0,1} ([0-9]{0,2}, )?[0-9]{4}( at|,) [0-9]{1,2}:[0-9]{1,2})[\\s\\S]*";
        Pattern pattern = Pattern.compile(regexPattern, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(emailText);
        if (matcher.find()) {
            log.info("Found email history text in email, removing it");
            return matcher.replaceAll("");
        }
        log.info("No email history text found in email");
        return emailText;
    }

    public String removeEmailHistoryForOutLookClient(String emailText) {
        //  remove everything starts with Get Outlook for Android or Get Outlook for iOS
        String regexPattern = "(Get Outlook for Android|Get Outlook for iOS)[\\s\\S]*";
        Pattern pattern = Pattern.compile(regexPattern, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(emailText);
        if (matcher.find()) {
            log.info("[OUTLOOK CLIENT FOUND ], removing EMAIL HISTORY AND SIGNATURE");
            return matcher.replaceAll("");
        }
        return emailText;
    }

    public String removeEmailHistoryForYahooClient(String emailText) {
        String regexPattern = "(Sent from Yahoo)[\\s\\S]*";
        String signatureSanatized = removeEmailHistoryForYahooClient(emailText, regexPattern);
        regexPattern = "(On [0-9]{1,2}-[A-Za-z]{1,5}-[0-9]{1,4} [0-9]{1,2}:[0-9]{1,2}[,]{0,1})[\\s\\S]*";
        String historySanatized = removeEmailHistoryForYahooClient(signatureSanatized, regexPattern);
        return historySanatized;
    }

    private String removeEmailHistoryForYahooClient(String emailText, String regexPattern) {
        Pattern pattern = Pattern.compile(regexPattern, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(emailText);
        if (matcher.find()) {
            log.info("[Yahoo CLIENT FOUND ], removing EMAIL HISTORY AND SIGNATURE");
            return matcher.replaceAll("");
        }
        return emailText;
    }

    public String removeEmailSignature(String emailText) {
        String regexPattern = "(--\n[\\s\\S]*)";
        Pattern pattern = Pattern.compile(regexPattern, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(emailText);
        if (matcher.find()) {
            log.info("Found email signature text in email, removing it");
            return matcher.replaceAll("");
        }
        log.info("No email signature text found in email");
        return emailText;
    }

    public <T> ResponseEntity<T> getResponseEntity(T t, HttpStatus httpStatus) {
        return new ResponseEntity<T>(t, httpStatus);
    }

    public String getInitialFromName(String name) {
        if (StringUtils.isBlank(name)) {
            return "";
        }
        String[] names = name.split(" ");
        String initial = "";
        for (String n : names) {
            initial += n.charAt(0);
        }
        return truncateToTwoChars(initial);
    }

    public String createDeepLinkForBasePost(String societyId, String forumId) {
        return createDeepLink(societyId, forumId, null);
    }

    public String createDeepLinkForSpecificComment(String societyId, String forumId, String commentId) {
        return createDeepLink(societyId, forumId, commentId);
    }

    private String createDeepLink(String societyId, String forumId, String commentId) {
        log.info("Creating deep link for societyId: {}, forumId: {}, commentId: {}", societyId, forumId, commentId);
        if (StringUtils.isBlank(societyId) || StringUtils.isBlank(forumId)) {
            throw new RuntimeException("SocietyId or ForumId cannot be null");
        }
        if (StringUtils.isBlank(deepLinkBaseUrl)) {
            log.error("DeepLinkBaseUrl is not configured");
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(deepLinkBaseUrl).append("/");


        if (StringUtils.isBlank(commentId)) {
            BasePostDeepLinkData basePostDeepLinkData = new BasePostDeepLinkData();
            basePostDeepLinkData.setType("forum");
            basePostDeepLinkData.setSocietyId(societyId);
            basePostDeepLinkData.setForumId(forumId);

            String basePostDeepLinkDataJson = convertToJson(basePostDeepLinkData);
            log.info("BasePostDeepLinkDataJson: {}", basePostDeepLinkDataJson);
            String encodedBasePostDeepLinkData = encodeBase64(basePostDeepLinkDataJson);
            log.info("EncodedBasePostDeepLinkData: {}", encodedBasePostDeepLinkData);
            sb.append(encodedBasePostDeepLinkData);
            return sb.toString();

        } else {
            CommentDeepLinkData commentDeepLinkData = new CommentDeepLinkData();
            commentDeepLinkData.setType("forum");
            commentDeepLinkData.setSocietyId(societyId);
            commentDeepLinkData.setForumId(forumId);
            commentDeepLinkData.setCommentId(commentId);

            String commentDeepLinkJson = convertToJson(commentDeepLinkData);
            log.info("CommentDeepLinkJson: {}", commentDeepLinkJson);
            String encodedCommentDeepLinkData = encodeBase64(commentDeepLinkJson);
            log.info("EncodedCommentDeepLinkData: {}", encodedCommentDeepLinkData);
            sb.append(encodedCommentDeepLinkData);
            return sb.toString();

        }
    }

    public List<String> getAdminEmails() {
        String[] emails = adminEmails.split(",");
        return Arrays.stream(emails).toList();
    }

    public String convertUnicodeToHTML(String input) {
        String decodedString = StringEscapeUtils.unescapeJava(input);
        // Encode the decoded string as HTML entities
        String htmlString = StringEscapeUtils.escapeHtml4(decodedString);
        return escapeSpaces(htmlString);
    }

    public MessagePart parseBase64Eml(String data) throws IOException, jakarta.mail.MessagingException {
        Email email = EmailConverter.emlToEmail(data);
        MessagePart messagePart = new MessagePart();
        Map<String, Collection<String>> headers = email.getHeaders();
        List<MessagePartHeader> messagePartHeaders = new ArrayList<>();
        for (Map.Entry<String, Collection<String>> header : headers.entrySet()) {
            if(header.getValue() instanceof List){
                MessagePartHeader messagePartHeader = new MessagePartHeader();
                messagePartHeader.setName(header.getKey());
                messagePartHeader.setValue(((ArrayList<String>)header.getValue()).get(0));
                messagePartHeaders.add(messagePartHeader);
            }else{
                log.warn("Header value is not a list, header: {}", header);
            }

        }
        MessagePartHeader subjectHeader = new MessagePartHeader();
        subjectHeader.setName("Subject");
        subjectHeader.setValue(email.getSubject());
        messagePartHeaders.add(subjectHeader);

        MessagePartHeader toHeader = new MessagePartHeader();
        toHeader.setName("To");
        toHeader.setValue(email.getRecipients().get(0).getName()+" <"+email.getRecipients().get(0).getAddress()+">");
        messagePartHeaders.add(toHeader);

        MessagePartHeader fromHeader = new MessagePartHeader();
        fromHeader.setName("From");
        fromHeader.setValue(email.getFromRecipient().getName()+"<"+email.getFromRecipient().getAddress()+">");
        messagePartHeaders.add(fromHeader);

        messagePart.setHeaders(messagePartHeaders);
        String plainText = email.getPlainText();
        String htmlText = email.getHTMLText();
        String message;
        if(StringUtils.isNotBlank(plainText)) {
            message = plainText;
        }else if(StringUtils.isNotBlank(htmlText)) {
           message = htmlText;
           try{
               String yahooEmailBody = getYahooEmailBodyFromHtml(htmlText);
               log.info("Yahoo email body: {}", yahooEmailBody);
               message =  yahooEmailBody;
           }catch (Exception e){
               if(e.getMessage().equalsIgnoreCase("No yahoo email body found")){
                   log.error("No yahoo body found");
               }else{
                   log.error("Error while parsing yahoo email body", e);

               }
           }
        }else{
            message="Sender's Email Client is not supported, please contact at support@syncfusion.com";
        }

        messagePart.setBody(new MessagePartBody().setData(encodeBase64(message)));
        return messagePart;
    }
    private String getYahooEmailBodyFromHtml(String htmlbody){
       Document document =  Jsoup.parse(htmlbody);
        Element firstDiv = document.select("div").first();
       if(firstDiv!=null && firstDiv.hasAttr("dir") && "auto".equalsIgnoreCase(firstDiv.attr("dir"))  && StringUtils.isNotBlank(firstDiv.text())){
           return firstDiv.text();
       }
       throw new RuntimeException("No yahoo email body found");
    }

    private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws IOException, jakarta.mail.MessagingException {
        int count = mimeMultipart.getCount();
        if (count == 0) {
            return "";
        } else if (count == 1) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(0);
            return getTextFromBodyPart(bodyPart);
        } else {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < count; i++) {
                BodyPart bodyPart = mimeMultipart.getBodyPart(i);
                result.append(getTextFromBodyPart(bodyPart));
            }
            return result.toString();
        }
    }

    private String getTextFromBodyPart(BodyPart bodyPart) throws IOException, MessagingException {
        Object content = bodyPart.getContent();
        if (content instanceof MimeMultipart) {
            return getTextFromMimeMultipart((MimeMultipart) content);
        } else if (content instanceof String) {
            return (String) content;
        } else {
            return "";
        }
    }

    public String getUnsubsribeListHeaderKey() {
        return "List-Unsubscribe";
    }
    public String getUnsubsribeListHeaderValue(String groupEmail) {
        if(StringUtils.isEmpty(groupEmail)){
            return "";
        }
        //remove emial domain with @
        groupEmail = groupEmail.split("@")[0];
        return String.format("<mailto:%s+unsubscribe@syncfusion.com>", groupEmail);
    }

    public String findEmailBodyText(MessagePart message) {
        List<MessagePart> parts = message.getParts();
        if(parts == null){
            log.info("No parts found in message");
            return findEmailBodyText(message.getBody());
        }
        Optional<MessagePart> multipart = parts.stream().filter(p -> p.getMimeType().equalsIgnoreCase("multipart/alternative")).findFirst() ;
        if(multipart.isPresent()) {
            log.debug("multipart/alternative part found in message");
            MessagePart nestedMessagePart = multipart.get();
            return findEmailBodyText(nestedMessagePart);
        }
        Optional<MessagePart> textPlainPart = parts.stream().filter(p -> p.getMimeType().equalsIgnoreCase("text/plain")).findFirst() ;
        if(textPlainPart.isPresent()){
            log.info("text/plain part found in message");
            return findEmailBodyText(textPlainPart.get().getBody());
        }
        return null;
    }
    public String findEmailBodyHtmlText(MessagePart message){
        List<MessagePart> parts = message.getParts();
        if(parts == null){
            log.info("No parts found in message");
            return findEmailBodyText(message.getBody());
        }
        Optional<MessagePart> multipart = parts.stream().filter(p -> p.getMimeType().equalsIgnoreCase("multipart/alternative")).findFirst() ;
        if(multipart.isPresent()) {
            log.debug("multipart/alternative part found in message");
            MessagePart nestedMessagePart = multipart.get();
            return findEmailBodyText(nestedMessagePart);
        }
        Optional<MessagePart> htmlText = parts.stream().filter(p -> p.getMimeType().equalsIgnoreCase("text/html")).findFirst() ;
        if(htmlText.isPresent()){
            log.info("text/html part found in message");
            return findEmailBodyText(htmlText.get().getBody());
        }
        return null;
    }

    public String findEmailBodyText(MessagePartBody messagePartBody){
        if(messagePartBody == null){
            log.info("No messagePartBody found in message: {}", messagePartBody);
            return null;
        }
        String encodedBody = messagePartBody.getData();
        String emailText = decodeBase64(encodedBody);
        String emailHistorySanatized = removeEmailHistoryText(emailText);
        String emailSignatureSanatized = removeEmailSignature(emailHistorySanatized);
        return emailSignatureSanatized!=null ? emailSignatureSanatized.trim() : null;
    }

    private void validateEmailClient(String emailClient) {
        if (emailClient == null || emailClient.isEmpty()) {
            message="Sender's Email Client is not supported, please contact at support@syncfusion.com";
            throw new TJSException(message, HttpStatus.BAD_REQUEST);
        }
    }

    public String getUnsubscribeLink(String groupEmail) {
        return String.format("<mailto:%s+unsubscribe@syncfusion.com>", groupEmail);
    }
}
