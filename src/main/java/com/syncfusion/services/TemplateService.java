package com.syncfusion.services;


import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.syncfusion.dto.template.BasePost;
import com.syncfusion.dto.template.NestedReply;
import com.syncfusion.dto.template.Reply;
import com.syncfusion.utils.UtilMethods;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class TemplateService {

    Logger log = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private UtilMethods utilMethods;

    private Handlebars handlebars;

    public Handlebars getHandlebars() {
        if(handlebars == null)
            init();
        return handlebars;
    }

    @PostConstruct
    public void init() {
        TemplateLoader loader = new ClassPathTemplateLoader("/templates", ".hbs");
        handlebars = new Handlebars(loader);
        log.info("Handlebars initialized");
    }

    private Template getEmailTemplate(String templateName) throws IOException {
        Template template = getHandlebars().compile(templateName);
        return template;
    }

    private Template getNestedReplyTemplate() throws IOException {
        return getEmailTemplate("email_reply_to_reply_template");
    }

    private Template getReplyToBasePostTemplate() throws IOException {
        return getEmailTemplate("email_reply_to_base_post_template");
    }

    private Template getBasePostTemplate() throws IOException {
        return getEmailTemplate("email_base_post_template");
    }

    public String getSampleHtmlContent() throws IOException {
        return getHTMLBodyForBasePost("Hi, This is a sample email \\uD83D\\uDC4D\\uD83D\\uDC4D\n", "subject", "Tejas", "Owner@102 ", null, "someSocietyId", "someForumId", "some email");
    }

    public String getSampleReplyHtmlContent() throws IOException {
        return getHTMLBodyForReply("Hi, This is a sample reply email", "Tejas", "Tenant @ 101", null, "someSocietyId", "someForumId", "someCommentId", "some-email");
    }
    public String getSampleNestedRelytHtmlContent() throws IOException {
        return getHTMLBodyForNestedReply("Hi, This is a sample nested reply email", "Tejas", "Tenant @ 101", null, "nithin kumar", "parent comment text", "tenant @ 109", null, "societyId","forumId","some id", "some-email");
    }

    public String getHTMLBodyForBasePost(String emailText, String subject, String senderName, String senderDesignation, String sentDate, String societyId, String forumId, String societyEmail) throws IOException {
        Template template = getBasePostTemplate();
        BasePost basePostEmailContent = new BasePost();
        basePostEmailContent.setAuthorName(senderName);
        basePostEmailContent.setSubject(subject);
        basePostEmailContent.setEmailText(utilMethods.convertUnicodeToHTML(emailText));
        basePostEmailContent.setSentDate(sentDate);
        basePostEmailContent.setAuthorInitials(utilMethods.getInitialFromName(senderName));
        basePostEmailContent.setAuthorDesignation(senderDesignation);
        basePostEmailContent.setViewDiscussionDeepLink(utilMethods.createDeepLinkForBasePost(societyId, forumId));
        basePostEmailContent.setReplyToCommentDeepLink(null);
        basePostEmailContent.setSocietyEmail(societyEmail);
        log.info("emailContent: {}", basePostEmailContent);

        return template.apply(basePostEmailContent);
    }

    public String getHTMLBodyForReply(String emailText, String senderName, String authorDesignation, String sentDate, String societyId, String forumId, String commentId, String societyEmail) throws IOException {
        Template template = getReplyToBasePostTemplate();
        Reply replyEmailContent = new Reply();
        replyEmailContent.setAuthorName(senderName);
        replyEmailContent.setEmailText(utilMethods.convertUnicodeToHTML(emailText));
        replyEmailContent.setSentDate(sentDate);
        replyEmailContent.setAuthorDesignation(authorDesignation);
        replyEmailContent.setAuthorInitials(utilMethods.getInitialFromName(senderName));
        replyEmailContent.setReplyToCommentDeepLink(utilMethods.createDeepLinkForSpecificComment(societyId, forumId, commentId));
        replyEmailContent.setViewDiscussionDeepLink(utilMethods.createDeepLinkForBasePost(societyId, forumId));
        replyEmailContent.setSocietyEmail(societyEmail);

        log.info("emailContent: {}", replyEmailContent);
        return template.apply(replyEmailContent);
    }

    public String getHTMLBodyForNestedReply(String emailText, String senderName, String authorDesignation, String sentDate, String parentAuthorName, String parentCommentText, String parentAuthorDesignation, String parentSentDate, String societyId, String forumId, String commentId, String societyEmail) throws IOException {
        NestedReply nestedReply = new NestedReply();
        nestedReply.setAuthorName(senderName);
        nestedReply.setEmailText(utilMethods.convertUnicodeToHTML(emailText));
        nestedReply.setAuthorDesignation(authorDesignation);
        nestedReply.setAuthorInitials(utilMethods.getInitialFromName(senderName));
        nestedReply.setReplyToCommentDeepLink(utilMethods.createDeepLinkForSpecificComment(societyId, forumId, commentId));
        nestedReply.setViewDiscussionDeepLink(utilMethods.createDeepLinkForBasePost(societyId, forumId));
        nestedReply.setSentDate(sentDate);
        nestedReply.setSocietyEmail(societyEmail);

        Reply parentReply = new Reply();
        parentReply.setAuthorName(parentAuthorName);
        parentReply.setAuthorInitials(utilMethods.getInitialFromName(parentAuthorName));
        parentReply.setAuthorDesignation(parentAuthorDesignation);
        parentReply.setSentDate(parentSentDate);
        parentReply.setEmailText(utilMethods.convertUnicodeToHTML(parentCommentText));

        nestedReply.setInReplyTo(parentReply);

        Template nestedReplyTemplate = getNestedReplyTemplate();

        log.info("emailContent: {}", nestedReply);

        return nestedReplyTemplate.apply(nestedReply);
    }
}


