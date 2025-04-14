package com.syncfusion.services.gmail;


import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.gmail.GmailScopes;
import com.syncfusion.database.models.TJSGmailAccount;
import com.syncfusion.services.TJSGmailAccountService;
import com.syncfusion.services.credential.NonServiceAccountCredentialProvider;
import com.syncfusion.services.credential.ServiceAccountCredentialProvider;
import com.syncfusion.utils.UtilMethods;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class GmailClientProvider {
    Map<String, GmailClient> gmailClientMap = new java.util.concurrent.ConcurrentHashMap<>();
    GmailClient gmailClientForServiceAccount;
    Logger logger = LoggerFactory.getLogger(GmailClientProvider.class);

    @Autowired
    private ServiceAccountCredentialProvider serviceAccountCredentialProvider;
    @Autowired
    private NonServiceAccountCredentialProvider nonServiceAccountCredentialProvider;

    @Autowired
    private TJSGmailAccountService gmailAccountService;

    @Autowired
    private UtilMethods utilMethods;


    public GmailClientProvider() {

    }

    @PostConstruct
    private void initAllClients() throws IOException, GeneralSecurityException {
        initNonServiceAccountGmailClients();
        initServiceAccountGmailClients();
    }

    private void initNonServiceAccountGmailClients() throws GeneralSecurityException, IOException {
        List<TJSGmailAccount> allAccounts = gmailAccountService.findAll();
        if (allAccounts == null || allAccounts.isEmpty()) {
            logger.error("No Gmail accounts found.");
            return;
        }
        gmailClientMap.clear();
        for (TJSGmailAccount account : allAccounts) {
            if(!account.isActive()) {
                logger.info("Skipping inactive account : {}", account.getEmail());
                continue;
            }
            Credential credential  =  nonServiceAccountCredentialProvider.getCredential(account);
            gmailClientMap.put(account.getEmail(), new GmailClient(account.getEmail(), credential));
        }
    }

    private void initServiceAccountGmailClients() throws IOException {
        List<String> SCOPES = new ArrayList<>();
        SCOPES.add(GmailScopes.GMAIL_READONLY);
        SCOPES.add(GmailScopes.GMAIL_SEND);
        SCOPES.add(GmailScopes.GMAIL_MODIFY);
        SCOPES.add(GmailScopes.MAIL_GOOGLE_COM);
        synchronized (this) {
            GoogleCredential googleCredential = (GoogleCredential) serviceAccountCredentialProvider.getCredential(SCOPES);
            GoogleCredential gmailCredentials = googleCredential.createDelegated(utilMethods.getImposterEmail());
            GmailClient gmailClient = new GmailClient(utilMethods.getImposterEmail(), gmailCredentials);
            this.gmailClientForServiceAccount = gmailClient;
        }
    }

    public GmailClient getGmailClient(String email) throws GeneralSecurityException, IOException {
        if (!gmailClientMap.containsKey(email)) {
            logger.warn("GmailClient not found for email: " + email + " Creating new one");
            TJSGmailAccount account = gmailAccountService.findById(email).get();
            if (account == null) {
                logger.error("No Gmail account found for email: " + email);
                throw new RuntimeException("No Gmail account found for email: " + email);
            }
            if(!account.isActive()){
                logger.error("Gmail account is inactive for email: " + email);
                throw new RuntimeException("Gmail account is inactive for email: " + email);
            }
            synchronized (this) {
                if (!gmailClientMap.containsKey(email)) {
                    Credential credential = nonServiceAccountCredentialProvider.getCredential(account);
                    gmailClientMap.put(email, new GmailClient(account.getEmail(),credential));
                }
            }
        }
        return gmailClientMap.get(email);
    }

    public GmailClient getGmailClientForServiceAccount() {
        return gmailClientForServiceAccount;
    }

    public void refreshNonServiceAccountClients() throws GeneralSecurityException, IOException {
        logger.info("Refreshing all non service account Gmail clients");
        initNonServiceAccountGmailClients();

    }

    public void subscribeToInboxUpdate(String email) throws GeneralSecurityException, IOException {
        logger.info("Subscribing to Gmail Inbox Updates for email : {}", email);
        GmailClient gmailClient = getGmailClient(email);
        if (gmailClient == null) {
            logger.error("No GmailClient found for email : {}", email);
            throw new RuntimeException("No GmailClient found for email : " + email);
        }
        try {
            gmailClient.watchForInboxUpdates();
            logger.info("Successfully subscribed to Gmail Inbox Updates for email : {}", email);
        } catch (IOException e) {
            logger.error("Error while subscribing to Gmail Inbox Updates for email : {}", email);
            throw new RuntimeException("Error while subscribing to Gmail Inbox Updates for email : " + email);
        }
    }

    public void subscribeToInboxUpdatesAll() throws GeneralSecurityException, IOException {
        logger.info("Subscribing to Gmail Inbox Updates for all accounts");
        initNonServiceAccountGmailClients();

        logger.info("Subscribing to Gmail Inbox Updates for all non service account");

        for (GmailClient gmailClient : gmailClientMap.values()) {
            try {
                gmailClient.watchForInboxUpdates();
                logger.info("Successfully subscribed to Gmail Inbox Updates for email : {}", gmailClient.getEmail());
            } catch (IOException e) {
                logger.error("Error while subscribing to Gmail Inbox Updates for email : {}", gmailClient.getEmail());
            }
        }

        logger.info("Subscribing to Gmail Inbox Updates for all Service Accounts");
        try {
            gmailClientForServiceAccount.watchForInboxUpdates();
            logger.info("Successfully subscribed to Gmail Inbox Updates for email : {}", gmailClientForServiceAccount.getEmail());
        } catch (IOException e) {
            logger.error("Error while subscribing to Gmail Inbox Updates for email : {}", gmailClientForServiceAccount.getEmail());
        }

    }
}
