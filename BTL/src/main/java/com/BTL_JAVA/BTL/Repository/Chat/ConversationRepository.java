package com.BTL_JAVA.BTL.Repository.Chat;

import com.BTL_JAVA.BTL.Entity.Chat.Conversation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.BTL_JAVA.BTL.enums.ChatConversationType;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Integer> {
    Optional<Conversation> findByUser_IdAndAdmin_Id(int user_id, int admin_id);

    Optional<Conversation> findByUser_IdAndAdmin_IdAndConversationType(
            int userId,
            int adminId,
            ChatConversationType conversationType
    );

    @Query("""
            select c from Conversation c
            where c.user.id = :userId
              and c.admin.id = :adminId
              and (c.conversationType = :conversationType or c.conversationType is null)
            """)
    Optional<Conversation> findByUserAndAdminAndConversationTypeOrNull(
            int userId,
            int adminId,
            ChatConversationType conversationType
    );

    List<Conversation> findByUser_IdOrderByUpdatedAtDesc(int userId);

    // lấy các conversation của viewer là ADMIN
    @EntityGraph(attributePaths = {"user", "admin"})
    List<Conversation> findByAdmin_IdOrderByUpdatedAtDesc(int adminId);
}
