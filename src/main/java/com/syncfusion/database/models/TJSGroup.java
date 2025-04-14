package com.syncfusion.database.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "google_group")
public class TJSGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "society_id")
    @NonNull
    private String societyId;

    @Column(name = "society_group_email")
    @NonNull
    private String societyGroupEmail;

    @Column(name = "imposter_email")
    @NonNull
    private String imposterEmail;

    @Column(name = "active", columnDefinition = "boolean default true")
    private boolean active = true;
}
