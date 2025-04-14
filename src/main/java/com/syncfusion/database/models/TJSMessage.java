package com.syncfusion.database.models;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;


@Entity
@Getter
@Setter
@Table(name = "forum_message")
public class TJSMessage {

    @Column(name = "society_id")
    private String societyId;

    @Column(name = "forum_post_id")
    private String forumPostId;

    @Column(name = "provider_message_id")
    private String providerMessageId;

    @Id
    @Column(name = "universal_message_id")
    private String universalMessageId;

    @Column(name = "comment_id")
    private String commentId;

    @Column(name = "is_base_message")
    private boolean isBaseMessage;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "creation_date")
    private Date creationDate;

    @Column(name = "is_created_by_email", columnDefinition = "boolean default false")
    private boolean isCreatedByEmail = false;

    @PrePersist
    public void prePersist() {
        this.creationDate = new Date();
    }
}

