package com.syncfusion.services.gmail;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;
import com.google.common.collect.Lists;
import com.syncfusion.dto.gmail.TJSAttachment;
import com.syncfusion.services.MyApplicationContextProvider;
import com.syncfusion.services.gcp.GcpUploader;
import com.syncfusion.utils.UtilMethods;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;

import static com.nobroker.tejas.utils.Constants.GmailConstants.*;

public class GmailClient {
    Logger logger = LoggerFactory.getLogger(GmailClient.class);
    private Gmail gmail;
    private Credential credential;
    private String email ;
    private NetHttpTransport HTTP_TRANSPORT ;
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();



    public GmailClient(String email, Credential credential) {
        this.email = email;
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            initGmail(credential);
        } catch (Exception e) {
            throw new RuntimeException("Error initilizing gmail client using service account", e);
        }

    }

    public String getEmail(){
        return email;
    }
    private  void initGmail(Credential credential) {
        synchronized (this) {
            try {
                gmail = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                        .setApplicationName("Tejas")
                        .build();
                printLabels();
            } catch (Exception e) {
                throw new RuntimeException("Error while creating gmail client  for email: " + email, e);
            }
        }

    }



    private Gmail getGmail() throws IOException {
        if (gmail == null) {
          initGmail(credential);
        }
        return gmail;
    }
    public List<Label> getLabels() throws IOException {
        logger.info("Getting gmail history");
        String user = email;
        ListLabelsResponse listResponse = gmail.users().labels().list(user).execute();
        List<Label> labels = listResponse.getLabels();
        return labels;
    }

    public void printLabels(){
        try {
            List<Label> labels = getLabels();
            for (Label label : labels) {
                logger.info("Label: " + label.getName());
            }
        } catch (IOException e) {
            logger.error("Error getting labels", e);
        }
    }
    public String watchForInboxUpdates() throws IOException {
        List<String> labelIds = new ArrayList<>();
        labelIds.add("INBOX");
        String labelFilterAction = "INCLUDE";
        WatchResponse response = getGmail().users().watch(email, new WatchRequest().setTopicName("projects/nobrokerhood-in/topics/forum-email-push").setLabelIds(labelIds).setLabelFilterAction(labelFilterAction)).execute();

        if (response.isEmpty()) {
            logger.error("Error while watching for inbox updates");
            throw new RuntimeException("Error while watching for inbox updates");
        }
        logger.info("successfully watching inbox updates, response : {}", response);
        return response.getHistoryId().toString();
    }
    public List<History> getHistory(String historyId, String imposterEmail) throws IOException {
        logger.info("Getting gmail history for history id : {} and imposterEmail :{} ", historyId, imposterEmail);
        List<History> history = new ArrayList<>();

        String nextPageToken = null;

        do {
            logger.info("Getting history for history id : {} and nextPageToken : {}", historyId, nextPageToken);
            ListHistoryResponse response;
            if (StringUtils.isBlank(nextPageToken)) {
                response = getGmail().users().history().list(imposterEmail).setStartHistoryId(BigInteger.valueOf(Long.parseLong(historyId))).execute();
            } else {
                response = getGmail().users().history().list(imposterEmail).setStartHistoryId(BigInteger.valueOf(Long.parseLong(historyId))).setPageToken(nextPageToken).execute();
            }
            if (response.getHistory() == null) {
                logger.warn("No history found for history id : {}", historyId);
                break;
            }
            logger.info("History found for history id: {} with total size: {}, with nextPageToken: {}", historyId, response.getHistory().size(), response.getNextPageToken());
            history.addAll(response.getHistory());
            nextPageToken = response.getNextPageToken();
        } while (nextPageToken != null);

        logger.info("Total history found for history id : {} is : {}", historyId, history.size());
        return history;

    }

    public Message getFullyQualifiedMessage(String messageId, String imposterEmail) throws IOException {
        logger.info("Getting message from id : {}", messageId);
        if (StringUtils.isBlank(imposterEmail)) {
            imposterEmail = email;
        }
        return getGmail().users().messages().get(imposterEmail, messageId).execute();
    }

    public List<Message> getFullyQualifiedMessageBatch(LinkedHashSet<String> idsSet){
        List<String> ids = new ArrayList<>(idsSet);
        long totalIds = ids.size();
        logger.info("total size of message ids: {}",  totalIds);
        if(totalIds > 100) {
            logger.info("total size of message ids: {} is greater than 100, so splitting into batches",  totalIds);
            List<Message> fullyQualifiedMessageList = new ArrayList<>();
            List<List<String>> partition = Lists.partition(ids, 100);
            for(List<String> batch : partition) {
                fullyQualifiedMessageList.addAll(getFullyQualifiedMessageBatch(new LinkedHashSet<>(batch)));
            }
            return fullyQualifiedMessageList;
        }
        logger.info("total size of message ids: {} is less than 100, so not splitting into batches",  totalIds);

        List<Message> fullyQualifiedMessageList = new ArrayList<>();
        try {

            final JsonBatchCallback<Message> callback = new JsonBatchCallback<Message>() {
                public void onSuccess(Message message, HttpHeaders responseHeaders) {
                    logger.info("Message retrieved successfully from batch request for id: {}", message.getId());
                    fullyQualifiedMessageList.add(message);
                }

                public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) {
                    logger.error("Error occurred while getting message from batch request : {}", e.getMessage());
                }
            };

            BatchRequest batch = getGmail().batch();
            for (String id : ids) {
                getGmail().users().messages().get(email, id).setFormat("full").queue(batch, callback);
            }
            batch.execute();

        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info( "Total message size: {}  " , fullyQualifiedMessageList.size());

        return fullyQualifiedMessageList;
    }
    public String getEmlAttachment(String messageId, String attachmentId) throws IOException {
        logger.info("Getting attachment for message id : {} and attachment id : {}", messageId, attachmentId);

        try {
            MessagePartBody messagePartBody = getGmail().users().messages().attachments().get(email, messageId, attachmentId).execute();
            String attachmentDataEncoded = messagePartBody.getData();
            UtilMethods utilMethods = MyApplicationContextProvider.getContext().getBean(UtilMethods.class);
            return utilMethods.decodeBase64(attachmentDataEncoded);
        } catch (Exception err) {
            logger.error("Error while getting attachment for message id : {} and attachment id : {}", messageId, attachmentId, err);
            throw err;
        }
    }
    public String getAttachment(String messageId, String attachmentId) throws IOException {
        logger.info("Getting attachment for message id : {} and attachment id : {}", messageId, attachmentId);

        try {
            MessagePartBody messagePartBody = getGmail().users().messages().attachments().get(email, messageId, attachmentId).execute();
            String attachmentDataEncoded = messagePartBody.getData();
            UtilMethods utilMethods = MyApplicationContextProvider.getContext().getBean(UtilMethods.class);
            return utilMethods.decodeBase64(attachmentDataEncoded);
        } catch (Exception err) {
            logger.error("Error while getting attachment for message id : {} and attachment id : {}", messageId, attachmentId, err);
            throw err;
        }
    }
    public List<TJSAttachment> extractAttachment(Message message) throws IOException {
        String messageId = message.getId();
        List<TJSAttachment> allAttachments = new ArrayList<>();
        if(message.getPayload() !=null && message.getPayload().getParts() != null)
        {
            List<MessagePart> messagePart = message.getPayload().getParts();
            for(MessagePart part : messagePart)
            {
                if(part.getBody() !=null && part.getBody().getAttachmentId()!=null){
                    String attachmentId = part.getBody().getAttachmentId();
                    String data = getAttachment(messageId, attachmentId);
                    String fileName = part.getFilename();
                    String mimeType = part.getMimeType();
                    String size = part.getBody().getSize().toString();
                    GcpUploader  gcpUploader = MyApplicationContextProvider.getContext().getBean(GcpUploader.class);
                    try {
                        String gcpURI = gcpUploader.upload(data.getBytes(), fileName, mimeType);
                        TJSAttachment tjsAttachment =  TJSAttachment.builder().fileName(fileName).attachmentId(attachmentId).gcpUrl(gcpURI).mimeType(mimeType).size(size).build();
                        allAttachments.add(tjsAttachment);
                    }catch (Exception e){
                        logger.error("Error while uploading attachment to gcp for message id : {} and attachment id : {}", messageId, attachmentId, e);
                    }


                }
            }
        }
        return  allAttachments;
    }

    public Message sendEmailInThread(String to, String inReplyToMessageId, String fromName, String subject, String content, String contentType, String impersonateEmail, String fromEmailAddress, String repltyToEmailAddress, String threadId) throws MessagingException, IOException {
        logger.info("Sending email in thread to : {} with subject: {}, with inReplyToMessageId: {}, content: {}, fromEmail: {}, replyTo: {}", to, subject, inReplyToMessageId, content, fromEmailAddress, repltyToEmailAddress);

        MimeMessage email = createEmail(to, fromEmailAddress, fromName, subject, content, contentType);
        // this is important to identify the source of the email and to filter out the emails sent by this application and hence stop loop of sending emails
        email.setHeader(EMAIL_SOURCE_IDENTIFIER_HEADER_KEY, EMAIL_SOURCE_IDENTIFIER_HEADER_VALUE);
        email.setHeader(IN_REPLY_TO_HEADER_KEY, inReplyToMessageId);
        email.setHeader(REFERENCES_HEADER_KEY, inReplyToMessageId);

        if (StringUtils.isBlank(repltyToEmailAddress)) {
            repltyToEmailAddress = fromEmailAddress;
        }
        email.setHeader(REPLY_TO_HEADER_KEY, repltyToEmailAddress);

        Message message;
        if (!StringUtils.isBlank(threadId)) {
            message = createMessageWithEmail(email, threadId);
        } else {
            message = createMessageWithEmail(email);
        }
        message = getGmail().users().messages().send(impersonateEmail, message).execute();
        logger.info("Gmail message send successfully with gmail id : {}", message.getId());
        return message;
    }

    public Message sendEmail(String to, String fromName, String subject, String content, String contentType, String impersonateEmail, String fromEmail, String replyToEmail) throws IOException, MessagingException {
        logger.info("Sending email to : {} with subject: {}", to, subject);
        MimeMessage email = createEmail(to, fromEmail, fromName, subject, content, contentType);
        // this is important to identify the source of the email and to filter out the emails sent by this application and hence stop loop of sending emails
        email.setHeader(EMAIL_SOURCE_IDENTIFIER_HEADER_KEY, EMAIL_SOURCE_IDENTIFIER_HEADER_VALUE);
//        String principalEmailAddress = getService().users().getProfile("me").execute().getEmailAddress();
        email.setHeader(REPLY_TO_HEADER_KEY, replyToEmail);
//        email.setHeader("Content-Transfer-Encoding", "base64");
        Message message = createMessageWithEmail(email);
        message = getGmail().users().messages().send(impersonateEmail, message).execute();
        logger.info("Gmail message sent successfully with id : {}", message.getId());
        return message;
    }

    public Message sendEmailInThread(String to, String inReplyToMessageId, String fromName, String subject, String content, String contentType, String impersonateEmail, String fromEmailAddress, String repltyToEmailAddress) throws MessagingException, IOException {
        return sendEmailInThread(to, inReplyToMessageId, fromName, subject, content, contentType, impersonateEmail, fromEmailAddress, repltyToEmailAddress, null);
    }

    public void stopWatchingUpdates(){
        try {
            getGmail().users().stop(email).execute();
            logger.info("Successfully stopped watching updates for email: {}", email);
        } catch (IOException e) {
            logger.error("Error while stopping watching updates for email: {}", email, e);
        }
    }


    public Message sendEmailInThread(String to, String inReplyToMessageId, String fromName, String subject, String content, String contentType) throws MessagingException, IOException {
        UtilMethods utilMethods  = MyApplicationContextProvider.getContext().getBean(UtilMethods.class);
        return this.sendEmailInThread(to, inReplyToMessageId, fromName, subject, content, contentType, "me", utilMethods.getImposterEmail() , null);
    }
    public static MimeMessage createEmailWithHtml(String toEmailAddress,
                                                  String fromEmailAddress,
                                                  String fromName,
                                                  String subject,
                                                  String htmlContent)
            throws MessagingException, UnsupportedEncodingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage message = new MimeMessage(session);

        // Set the sender address.
        message.setFrom(new InternetAddress(fromEmailAddress, fromName));

        // Set the recipient address.
        message.setRecipients(javax.mail.Message.RecipientType.TO, InternetAddress.parse(toEmailAddress, false));

        // Set the subject.
        message.setSubject(subject);

        // Create a MimeMultipart to hold both the HTML and plain text parts.
        MimeMultipart multipart = new MimeMultipart("alternative");

        // Create a body part for the HTML content.
        BodyPart htmlBodyPart = new MimeBodyPart();
        htmlBodyPart.setContent(htmlContent, "text/html; charset=utf-8");


        // Add the HTML part to the multipart.
        multipart.addBodyPart(htmlBodyPart);

        // Set the multipart as the message's content.
        message.setContent(multipart);


        // Return the prepared MimeMessage.
        return message;
    }

    public static MimeMessage createEmail(String toEmailAddress,
                                          String fromEmailAddress,
                                          String fromName,
                                          String subject,
                                          String bodyText, String contentType)
            throws MessagingException, UnsupportedEncodingException {
        if ("text/html".equalsIgnoreCase(contentType)) {
            return createEmailWithHtml(toEmailAddress, fromEmailAddress, fromName, subject, bodyText);
        }
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress(fromEmailAddress, fromName));
        email.addRecipient(javax.mail.Message.RecipientType.TO,
                new InternetAddress(toEmailAddress));
        email.setSubject(subject);
        email.setText(bodyText);
        return email;
    }

    public static Message createMessageWithEmail(MimeMessage emailContent)
            throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    public static Message createMessageWithEmail(MimeMessage emailContent, String threadId)
            throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        message.setThreadId(threadId);
        return message;
    }

}
