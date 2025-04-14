package com.syncfusion.database.repository;

import com.syncfusion.database.models.TJSGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupMemberRepository  extends JpaRepository<TJSGroupMember, Long> {
    TJSGroupMember findByMemberEmail(String memberEmail);
    List<TJSGroupMember> findBySocietyIdAndMemberEmail(String societyId, String memberEmail);
    TJSGroupMember findBySocietyIdAndMemberEmailAndFirebaseUserId(String societyId, String memberEmail, String fbUserId);
}
