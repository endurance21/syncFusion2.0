package com.syncfusion.uniview;

import com.syncfusion.dto.uniview.TJSEmailContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class UniviewApiClient {
    Logger log = LoggerFactory.getLogger(UniviewApiClient.class);
    @Autowired
    private RestTemplate restTemplate;

    @Value("${uniview.baseurl:''}")
    private String univiewBaseUrl;

    @Value("${uniview.healthcheck.url:''}")
    private String univiewHealthCheckUrl;

    @Value("${uniview.notify.route:''}")
    private String unviewNotifyRoute;

    @Value("${uniview.authorization.token:''}")
    private String authorizationtoken;

    public boolean healthCheck() {
        String url = String.format("%s%s", univiewBaseUrl, univiewHealthCheckUrl);
        HttpEntity requestEntity = createNewHTTPEntity(null);
        ResponseEntity<String> response = null;
        try {
            response = restTemplate.getForEntity(url, String.class, requestEntity);
        } catch (Exception e) {
            log.error("error while calling uniview healthcheck url: {}", url, e);
        } finally {
            if (response == null) {
                log.error("error while calling uniview healthcheck url: {}", url);
                return false;
            }
            if (response.getStatusCode().value() == 200) {
                return true;
            }
            return false;
        }
    }

    public void notifyForIncomingEmail(TJSEmailContent emailContent) {
        log.info("calling uniview api to notify for new thread");
        if (emailContent == null) {
            log.error("emailContent is null, not calling uniview api");
            return;
        }
        String url = getURI(unviewNotifyRoute);
        HttpEntity<TJSEmailContent> httpEntity = createNewHTTPEntity(emailContent);
        try {
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, httpEntity, String.class);
            log.info("successfully called uniview api to notify for new thread, response: {}", responseEntity);
        } catch (Exception e) {
            log.error("error while calling uniview api to notify for new thread", e);
        }
    }
    private RestTemplate getRestTemplate() {
        return restTemplate;
    }

    private String getBaseUrl() {
        return univiewBaseUrl;
    }

    private String getURI(String url) {
        return String.format("%s%s", getBaseUrl(), url);
    }

    private <T> HttpEntity<T> createNewHTTPEntity(T requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("authorization", authorizationtoken);
        return new HttpEntity<>(requestBody, headers);
    }
}
