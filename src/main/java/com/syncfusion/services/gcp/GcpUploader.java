package com.syncfusion.services.gcp;

import com.github.jknack.handlebars.internal.lang3.StringUtils;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;

@Service
public class GcpUploader {

    Logger logger = org.slf4j.LoggerFactory.getLogger(GcpUploader.class);
    @Value("${gcp.bucket.name:'tejas_bucket'}")
    private String bucketName;
    @Value("${gcp.service.account.file.path:gcp/gcp_service_account.json}")
    private String serviceAccountFilePath;

    private volatile Storage storage;

    private Storage initializeStorage() {
        try {
            // Authenticate using the service account key JSON file
            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(new FileInputStream(serviceAccountFilePath))
                    .createScoped("https://www.googleapis.com/auth/cloud-platform");

            // Set up the storage options
            StorageOptions storageOptions = StorageOptions.newBuilder()
                    .setCredentials(credentials)
                    .build();

            // Create the storage client
            this.storage = storageOptions.getService();
            return storageOptions.getService();
        } catch (IOException e) {
            throw new RuntimeException("Error initializing Google Cloud Storage", e);
        }
    }

    private Storage geStorage() {
        if (storage == null) {
            synchronized (GcpUploader.class) {
                if (storage == null)
                    storage = initializeStorage();
                return storage;
            }
        }
        return storage;
    }

    public String upload(byte[] bytes, String fileName, String contentType) {
        if (StringUtils.isBlank(contentType)) {
            logger.error("Content type is blank for file name - {}", fileName);
            contentType = "unclear";
        }
        BlobId blobId = BlobId.of(bucketName, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .setCacheControl("public, max-age=31536000")
                .build();
        Blob blob = geStorage().create(blobInfo, bytes);
        logger.info("File Saved in GS, FileName - {}", fileName);
        return blob.getBlobId().toString();
    }

}
