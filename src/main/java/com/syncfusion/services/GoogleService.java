package com.syncfusion.services;


import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.directory.Directory;
import com.google.api.services.directory.DirectoryScopes;
import com.google.api.services.directory.model.Group;
import com.google.api.services.directory.model.Member;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.groupssettings.Groupssettings;
import com.google.api.services.groupssettings.GroupssettingsScopes;
import com.google.api.services.groupssettings.model.Groups;
import com.syncfusion.services.credential.ServiceAccountCredentialProvider;
import com.syncfusion.utils.UtilMethods;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;


@Service
public class GoogleService {
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = new ArrayList<>();
    Logger log = LoggerFactory.getLogger(GoogleService.class);
    @Value("${application.name:tejas}")
    private String APPLICATION_NAME;

    @Value("${gmail.service.impersonate.user.email:nbh@syncfusion.com}")
    private String IMPERSONATE_USER;
    @Autowired
    private UtilMethods utilMethods;
    @Autowired
    private ServiceAccountCredentialProvider serviceAccountCredentialProvider;

    private Groupssettings groupsSettingsAPI;
    private Directory googleDirectory;
    private Directory.Groups groupsAPI;
    private NetHttpTransport HTTP_TRANSPORT;



    @PostConstruct
    private void init() throws GeneralSecurityException, IOException {
        HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        SCOPES.add(GmailScopes.GMAIL_READONLY);
        SCOPES.add(GmailScopes.GMAIL_SEND);
        SCOPES.add(GmailScopes.GMAIL_MODIFY);
        SCOPES.add(GmailScopes.MAIL_GOOGLE_COM);

        SCOPES.add(GroupssettingsScopes.APPS_GROUPS_SETTINGS);
        SCOPES.add(DirectoryScopes.ADMIN_DIRECTORY_GROUP);

        GoogleCredential googleCredential = (GoogleCredential) serviceAccountCredentialProvider.getCredential(SCOPES);

        log.info("intilizating google group settings with :{}", IMPERSONATE_USER);
        GoogleCredential groupsSettingsCredentials = googleCredential.createDelegated(IMPERSONATE_USER);
        initGoogleGroupSettingClient(groupsSettingsCredentials);
        log.info("intilized google group settings");

        log.info("intilizating google directory with :{}", IMPERSONATE_USER);
        GoogleCredential googleDirectoryCredentials = googleCredential.createDelegated(IMPERSONATE_USER);
        initGoogleDirectoryClient(googleDirectoryCredentials);
        log.info("intilized google directory ");
    }

    private void initGoogleDirectoryClient(Credential credential) throws IOException {
        // Build the Google Groups Settings API client
        googleDirectory = new Directory.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
        log.info("Google directory service initialized");
        Group group = getGroupsAPI().get("bren-mercury@syncfusion.com").execute();

        log.info(group.toString());

    }

    private Directory getDirectory() {
        if (googleDirectory == null) {
            throw new RuntimeException("Google directory service not initialized");
        }
        return googleDirectory;
    }

    private Directory.Groups getGroupsAPI() {
        if (googleDirectory == null) {
            throw new RuntimeException("Google directory service not initialized");
        }
        if (groupsAPI == null) {
            groupsAPI = googleDirectory.groups();
        }
        return groupsAPI;
    }

    private void initGoogleGroupSettingClient(Credential credential) throws IOException {
        // Build the Google Groups Settings API client
        groupsSettingsAPI = new Groupssettings.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
        log.info("Google group settings service initialized");

        Groups groupSettings = getGroupsSettingsAPI().groups().get("test_group@syncfusion.com").execute();
        log.info(groupSettings.toString());

    }

    private Groupssettings getGroupsSettingsAPI() {
        if (groupsSettingsAPI == null) {
            throw new RuntimeException("Groups settings service not initialized");
        }
        return groupsSettingsAPI;
    }




    public Group createNewGoogleGroup(String groupEmail, String groupName) throws IOException {
        log.info("Creating new google group with email : {}", groupEmail);
        Group googleGroup = new Group();
        googleGroup.setEmail(groupEmail);
        googleGroup.setName(groupName);
        googleGroup.setAdminCreated(true);
        googleGroup.setDescription("This is a group created by Syncfusion");
        try {
            googleGroup = getGroupsAPI().insert(googleGroup).execute();
        } catch (Exception e) {
            log.error("Error while creating new google group with email : {}", groupEmail, e);
            throw e;
        }
        log.info("Successfully created new google group with email : {}, id : {} ", groupEmail, googleGroup.getId());
        return googleGroup;
    }

    public Member addMemberToGoogleGroup(String groupEmail, String memberEmail, String role) throws IOException {
        Member member = new Member();
        member.setEmail(memberEmail);
        member.setRole(role);
        member = getDirectory().members().insert(groupEmail, member).execute();
        return member;
    }

    public void deleteMemberFromGoogleGroup(String groupEmail, String memberEmail) throws IOException {
        getDirectory().members().delete(groupEmail, memberEmail).execute();
    }

    public Groups updateGoogleGroupSettings(String groupEmail) throws IOException {
        Groups updatedGroupSettings = new Groups();
        updatedGroupSettings.setWhoCanPostMessage("ANYONE_CAN_POST");
        updatedGroupSettings.setWhoCanJoin("INVITED_CAN_JOIN");
        updatedGroupSettings.setAllowExternalMembers("true");
        updatedGroupSettings.setAllowWebPosting("true");
        updatedGroupSettings.setDefaultSender("GROUP");
        updatedGroupSettings.setMessageModerationLevel("MODERATE_ALL_MESSAGES");
        updatedGroupSettings.setSendMessageDenyNotification("false");
        updatedGroupSettings.setIncludeCustomFooter("false");
        updatedGroupSettings.setReplyTo("REPLY_TO_CUSTOM");
        updatedGroupSettings.setCustomReplyTo(groupEmail);
        updatedGroupSettings.setMembersCanPostAsTheGroup("true");
        return getGroupsSettingsAPI().groups().update(groupEmail, updatedGroupSettings).execute();
    }

    public Groups getGoogleGroupSettings(String groupEmail) throws IOException {
        return getGroupsSettingsAPI().groups().get(groupEmail).execute();
    }




}
