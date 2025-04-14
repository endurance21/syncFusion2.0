package com.syncfusion.services.credential;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.syncfusion.utils.UtilMethods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;


@Component
public class ServiceAccountCredentialProvider{
    @Value("${google.service.account.file.path:serviceAccountCredentials.json}")
    private String SERVICE_ACCOUNT_FILE_PATH;

    @Autowired
    private UtilMethods utilMethods;

    public Credential getCredential(List<String> scopes) {
        try {
            InputStream in = utilMethods.getFileAsStream(SERVICE_ACCOUNT_FILE_PATH);
            if (in == null) {
                throw new FileNotFoundException("Resource not found: " + SERVICE_ACCOUNT_FILE_PATH);
            }
            GoogleCredential credential = GoogleCredential.fromStream(in)
                    .createScoped(scopes);
            return credential;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
