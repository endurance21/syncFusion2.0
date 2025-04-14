package com.syncfusion.services;

import com.syncfusion.database.models.TJSGmailAccount;
import com.syncfusion.database.repository.TJSGmailAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TJSGmailAccountService {
    private final TJSGmailAccountRepository credentialRepository;

    @Autowired
    public TJSGmailAccountService(TJSGmailAccountRepository credentialRepository) {
        this.credentialRepository = credentialRepository;
    }

    public TJSGmailAccount save(TJSGmailAccount credential) {
        return credentialRepository.save(credential);
    }

    public Optional<TJSGmailAccount> findById(String email) {
        return credentialRepository.findById(email);
    }

    public List<TJSGmailAccount> findAll() {
        return credentialRepository.findAll();
    }

    public void deleteById(String email) {
        credentialRepository.deleteById(email);
    }

    // Add other custom service methods if needed
}
