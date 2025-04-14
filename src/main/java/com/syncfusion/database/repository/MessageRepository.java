package com.syncfusion.database.repository;

import com.syncfusion.database.models.TJSMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<TJSMessage, Long> {
    TJSMessage findByUniversalMessageId(String universalMessageId);
    @Query(value = "SELECT * FROM forum_message WHERE forum_post_id = ?1 AND is_base_message = true", nativeQuery = true)
    TJSMessage findBaseMessageByForumPostId(String forumPostId);

    TJSMessage findByCommentId(String commentId);
    boolean existsByForumPostId(String forumPostId);

}
