package com.syncfusion.services;


import com.syncfusion.dto.hood.HoodResponseDTO;
import com.syncfusion.utils.UtilMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class HoodService {

    Logger log = LoggerFactory.getLogger(HoodService.class);
    @Autowired
    private RestTemplate restTemplate;

    @Value("${hood.baseurl:https://bb-12231.hood.blackbox.syncfusion.com/}")
    private String hoodBaseUrl;

    @Value("${hood.getuserdetailsfromemail:internal/api/v1/firebase/get/details/}")
    private String getUserDetailsFromEmailPath;

    @Autowired
    private UtilMethods utilMethods;

    public HoodResponseDTO.ResponseData getUserDetails(String fbUserId, String societyId) {
        String url = new StringBuilder(hoodBaseUrl).append(getUserDetailsFromEmailPath).append(fbUserId).append("/").append(societyId).toString();
        log.info("calling hood for user details at  url: {}", url);
        ResponseEntity<HoodResponseDTO> responseEntity = null;
        try {
            responseEntity = restTemplate.getForEntity(url, HoodResponseDTO.class);
        } catch (Exception e) {
            log.error("error while calling hood for user details at  url: {}", url, e);
        } finally {
            if (responseEntity == null || responseEntity.getBody().getData() == null) {
                log.info("no data found for email on hood so returning dummy data for userId: {}", fbUserId);
                HoodResponseDTO.ResponseData res = new HoodResponseDTO.ResponseData();
                res.setFbUserId(fbUserId);
                String name = "N/A";
                res.setName(name);
                res.setOwner("A1");
                res.setProfilePic("https://picsum.photos/200");
                return res;
            }
            log.info("response from hood for user details: {}", responseEntity.getBody().getData());
            return responseEntity.getBody().getData().get(0);
        }


    }

    public String getAuthorDesignation(String societyId, String fbUserId) {
        HoodResponseDTO.ResponseData hoodUser = getUserDetails(fbUserId, societyId);
        if (hoodUser == null) {
            return null;
        }
        String owner = hoodUser.getOwner();
        String tenant = hoodUser.getTenant();

        return utilMethods.prepareSenderDesignation(owner, tenant);
    }
    public  String getAuthorDesignation(HoodResponseDTO.ResponseData hoodUser) {
        if (hoodUser == null) {
            return null;
        }
        String owner = hoodUser.getOwner();
        String tenant = hoodUser.getTenant();

        return utilMethods.prepareSenderDesignation(owner, tenant);
    }
}
