package com.syncfusion.database.models;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Getter
@Setter
@Table(name = "tejas_gmail_account")
public class TJSGmailAccount {
    @Column(name = "email")
    @Id
    @NonNull
    private String email;

    @Column(name = "access_token")
    @NonNull
    private String accessToken;

    @Column(name = "refresh_token")
    @NonNull
    private String refreshToken;

    @Column(name = "expires_in")
    @NonNull
    private long expiresIn;

    @Column(name = "active", columnDefinition = "boolean default false")
    private boolean active;

    @UpdateTimestamp
    private java.sql.Timestamp updatedOn;

    @CreationTimestamp
    private java.sql.Timestamp createdOn;

}

