package com.syncfusion.controllers.gmail;


import com.syncfusion.database.models.TJSGmailAccount;
import com.syncfusion.services.KafkaService;
import com.syncfusion.services.TJSGmailAccountService;
import com.syncfusion.services.gmail.GmailClient;
import com.syncfusion.services.gmail.GmailClientProvider;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/gmail/account")
public class GmailAccountController {

    @Autowired
    private TJSGmailAccountService accountService;

    @Autowired
    private GmailClientProvider gmailClientProvider;

    @Autowired
    private KafkaService kafkaService;

    @PostMapping("")
    public ResponseEntity<String> createAccount(@Valid @RequestBody TJSGmailAccount account) {
        try {
            TJSGmailAccount savedCredential = accountService.save(account);
            return new ResponseEntity<>("Credential created successfully for email: " + savedCredential.getEmail(), HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>("Error creating credential: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("")
    public ResponseEntity<TJSGmailAccount> getAccount(@Valid @RequestBody String email) {
        try {
            TJSGmailAccount savedCredential = accountService.findById(email).get();
            if (savedCredential == null) {
                return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity<>(savedCredential, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @DeleteMapping("")
    public ResponseEntity<String> deleteAccount(@RequestParam String email) {
        try {
           Optional<TJSGmailAccount> result =  accountService.findById(email);
           if(!result.isPresent()) {
               return new ResponseEntity<>("credential not found for email : " + email, HttpStatus.NOT_FOUND);
           }
            accountService.deleteById(email);
            return new ResponseEntity<>("credential deleted successfully for email : " + email, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deleting credential: " + e.getMessage());
        }
    }
    @PutMapping("/deactivate")
    public ResponseEntity<String> updateAccount(@RequestParam String email) {
        try {
            TJSGmailAccount account = accountService.findById(email).get();
            if(account == null) {
                return new ResponseEntity<>("account not found for email : " + email, HttpStatus.NOT_FOUND);
            }
            account.setActive(false);
            accountService.save(account);
            gmailClientProvider.refreshNonServiceAccountClients();
            return new ResponseEntity<>("Credential updated successfully for email: " + account.getEmail(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Error updating credential: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PostMapping("/watch/update/all")
    public ResponseEntity<String> refreshAllGmailClient() {
        try {
            gmailClientProvider.subscribeToInboxUpdatesAll();
            return new ResponseEntity<>("message sent to refresh server ", HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("error while  sending message to refresh server: " + e.getMessage());
        }
    }
    @PostMapping("/watch/update")
    public ResponseEntity<String> refreshAllGmailClient(@RequestParam("email") String email) {
        try {
            gmailClientProvider.subscribeToInboxUpdate(email);
            return new ResponseEntity<>("message sent to refresh server ", HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("error while  sending message to refresh server: " + e.getMessage());
        }
    }

    @GetMapping("/labels")
    public ResponseEntity<String> getLabels(@RequestParam("email") String email) {
        try {
            TJSGmailAccount account = accountService.findById(email).get();
            if (account == null) {
                return new ResponseEntity<>("No account found for email: " + email, HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity<>(gmailClientProvider.getGmailClient(email).getLabels().toString(), HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("error while  sending message to refresh server: " + e.getMessage());
        }
    }
    @GetMapping("/serviceAccount/labels")
    public ResponseEntity<String> getLabelsForServiceAccount(@RequestParam("email") String email) {
        try {
             GmailClient client  =  gmailClientProvider.getGmailClientForServiceAccount();
            return new ResponseEntity<>(client.getLabels().toString(), HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("error while  sending message to refresh server: " + e.getMessage());
        }
    }
    @PostMapping("/watch/update/stop")
    public ResponseEntity<String> stopInboxUpdate(@RequestParam("email") String email) {
        try {
            TJSGmailAccount account = accountService.findById(email).get();
            if (account == null) {
                return new ResponseEntity<>("No account found for email: " + email, HttpStatus.NOT_FOUND);
            }
            gmailClientProvider.getGmailClient(email).stopWatchingUpdates();
            return new ResponseEntity<>("message sent to stop inbox update ", HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("error while  sending message to stop inbox update: " + e.getMessage());
        }
    }
}
