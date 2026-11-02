package com.edu.registration.repository;

import com.edu.registration.entity.ThongBao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ThongBaoRepository extends JpaRepository<ThongBao, Long> {
    Page<ThongBao> findByRecipientId(Long recipientId, Pageable pageable);
    Page<ThongBao> findByRecipientIdAndReadAtIsNull(Long recipientId, Pageable pageable);
    Optional<ThongBao> findByIdAndRecipientId(Long id, Long recipientId);
    long countByRecipientIdAndReadAtIsNull(Long recipientId);
    boolean existsByRecipientIdAndIdempotencyKey(Long recipientId, String idempotencyKey);

    @Modifying
    @Query("update ThongBao t set t.readAt = :readAt where t.id = :id and t.recipient.id = :recipientId and t.readAt is null")
    int markRead(@Param("id") Long id, @Param("recipientId") Long recipientId,
                 @Param("readAt") LocalDateTime readAt);

    @Modifying
    @Query("update ThongBao t set t.readAt = :readAt where t.recipient.id = :recipientId and t.readAt is null")
    int markAllRead(@Param("recipientId") Long recipientId, @Param("readAt") LocalDateTime readAt);
}
