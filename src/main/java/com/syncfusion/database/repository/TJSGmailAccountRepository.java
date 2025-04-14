package com.syncfusion.database.repository;

import com.syncfusion.database.models.TJSGmailAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TJSGmailAccountRepository extends JpaRepository<TJSGmailAccount, String> {
    // You can add custom query methods if needed
}
