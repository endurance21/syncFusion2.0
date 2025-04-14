package com.syncfusion.services.credential;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.syncfusion.database.models.TJSGmailAccount;
import com.syncfusion.services.TJSGmailAccountService;
import com.syncfusion.services.gmail.TJSCredentialRefreshListener;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Component
public class NonServiceAccountCredentialProvider {
    Logger logger = org.slf4j.LoggerFactory.getLogger(NonServiceAccountCredentialProvider.class);
    private static final String CLIENT_SECRET_FILE = "gmail/gmail_client_secret.json"; // Path to your client_secret.json file
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private NetHttpTransport HTTP_TRANSPORT ;

    @Autowired
    private TJSGmailAccountService gmailAccountService;

    public Credential getCredential(TJSGmailAccount gmailAccount) throws IOException, GeneralSecurityException {
        logger.info("Getting credential for  non service account email : {}", gmailAccount.getEmail());
        HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                new InputStreamReader(getClass().getClassLoader().getResourceAsStream(CLIENT_SECRET_FILE)));

        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(HTTP_TRANSPORT)
                .setJsonFactory(JSON_FACTORY)
                .setClientSecrets(clientSecrets)
                .setRefreshListeners(Collections.singletonList(new TJSCredentialRefreshListener(gmailAccount, gmailAccountService)))
                .build()
                .setRefreshToken(gmailAccount.getRefreshToken());

        return credential;
    }

}
