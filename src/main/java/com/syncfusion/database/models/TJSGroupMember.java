package com.syncfusion.database.models;


import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Getter
@Setter
@Table(name = "google_group_member")
@IdClass(TJSGroupMemberID.class)
public class TJSGroupMember{

    @Column(name = "society_id")
    @NonNull
    @Id
    private String societyId;

    @Column(name = "member_email")
    @NonNull
    @Id
    private String memberEmail;

    @Id
    @Column(name = "firebase_user_id")
    @NonNull
    private String firebaseUserId;

}


@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
class TJSGroupMemberID implements Serializable {
    private String memberEmail;
    private String societyId;
    private String firebaseUserId;

}
