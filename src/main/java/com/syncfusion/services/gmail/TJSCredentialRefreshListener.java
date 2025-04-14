package com.syncfusion.services.gmail;


import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialRefreshListener;
import com.google.api.client.auth.oauth2.TokenErrorResponse;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.syncfusion.database.models.TJSGmailAccount;
import com.syncfusion.services.TJSGmailAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public  class TJSCredentialRefreshListener implements CredentialRefreshListener {
    Logger logger = LoggerFactory.getLogger(TJSCredentialRefreshListener.class);
    ;
    private TJSGmailAccount account;

    private TJSGmailAccountService gmailAccountService;

    public TJSCredentialRefreshListener(TJSGmailAccount account, TJSGmailAccountService gmailAccountService) {
        this.account = account;
        this.gmailAccountService = gmailAccountService;
    }

    @Override
    public void onTokenResponse(Credential credential, TokenResponse tokenResponse) throws IOException {
        account.setAccessToken(credential.getAccessToken());
        account.setRefreshToken(credential.getRefreshToken());
        account.setExpiresIn(credential.getExpirationTimeMilliseconds());
        gmailAccountService.save(account);
    }

    @Override
    public void onTokenErrorResponse(Credential credential, TokenErrorResponse tokenErrorResponse) throws IOException {
        if(tokenErrorResponse!=null)
            logger.error("Error refreshing token for email: " + account.getEmail() + " Error: " + tokenErrorResponse.getErrorDescription());
        else
            logger.error("Error refreshing token for email: " + account.getEmail());
    }


}

