package com.syncfusion.database.repository;


import com.syncfusion.database.models.TJSGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<TJSGroup, Long> {
    TJSGroup findBySocietyId(String societyId);
    TJSGroup findBySocietyGroupEmail(String groupEmail);

    @Query("SELECT COUNT(gg) > 0 FROM TJSGroup gg WHERE gg.imposterEmail = :imposterEmail")
    boolean doesImposterEmailExists(String imposterEmail);

    @Query("SELECT COUNT(gg) > 0 FROM TJSGroup gg, TJSGroupMember gm " +
            "WHERE gg.societyGroupEmail = :groupEmail " +
            "AND gm.memberEmail = :memberEmail " +
            "AND gg.societyId = gm.societyId")
    boolean isMemberBelongsToSociety(String groupEmail, String memberEmail);
}
